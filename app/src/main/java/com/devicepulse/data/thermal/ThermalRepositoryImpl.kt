package com.devicepulse.data.thermal

import com.devicepulse.data.appusage.AppUsageDataSource
import com.devicepulse.data.db.dao.ThermalReadingDao
import com.devicepulse.data.db.entity.ThermalReadingEntity
import com.devicepulse.data.device.DeviceProfileRepositoryImpl
import com.devicepulse.domain.model.ThermalState
import com.devicepulse.domain.model.ThrottlingEvent
import com.devicepulse.domain.model.ThermalStatus
import com.devicepulse.domain.repository.ThrottlingRepository
import com.devicepulse.domain.repository.ThermalReadingData
import com.devicepulse.domain.repository.ThermalRepository as ThermalRepositoryContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermalRepositoryImpl @Inject constructor(
    private val thermalDataSource: ThermalDataSource,
    private val deviceProfileRepository: DeviceProfileRepositoryImpl,
    private val thermalReadingDao: ThermalReadingDao,
    private val throttlingRepository: ThrottlingRepository,
    private val appUsageDataSource: AppUsageDataSource
) : ThermalRepositoryContract {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val throttlingMutex = Mutex()
    private var activeThrottlingEvent: ActiveThrottlingEvent? = null

    private val thermalStateFlow: Flow<ThermalState> by lazy {
        flow {
            val profile = deviceProfileRepository.ensureProfileInternal()
            emitAll(
                combine(
                    thermalDataSource.getBatteryTemperature(),
                    thermalDataSource.getCpuTemperature(profile.thermalZonesAvailable),
                    thermalDataSource.getThermalStatus(),
                    thermalDataSource.getThermalHeadroom()
                ) { batteryTemp, cpuTemp, thermalStatus, headroom ->
                    ThermalState(
                        batteryTempC = batteryTemp,
                        cpuTempC = cpuTemp,
                        thermalHeadroom = headroom,
                        thermalStatus = thermalStatus,
                        isThrottling = thermalStatus >= THROTTLING_THRESHOLD
                    )
                }.onEach { state ->
                    processThrottlingTransition(state)
                }
            )
        }.shareIn(
            scope = repositoryScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MS),
            replay = 1
        )
    }

    override fun getThermalState(): Flow<ThermalState> = thermalStateFlow

    override suspend fun saveReading(state: ThermalState) {
        val entity = ThermalReadingEntity(
            timestamp = System.currentTimeMillis(),
            batteryTempC = state.batteryTempC,
            cpuTempC = state.cpuTempC,
            thermalStatus = state.thermalStatus.ordinal,
            throttling = state.isThrottling
        )
        thermalReadingDao.insert(entity)
    }

    override suspend fun getAllReadings(): List<ThermalReadingData> {
        return thermalReadingDao.getAll().map { it.toDomain() }
    }

    override suspend fun deleteOlderThan(cutoff: Long) {
        thermalReadingDao.deleteOlderThan(cutoff)
    }

    private suspend fun processThrottlingTransition(state: ThermalState) {
        throttlingMutex.withLock {
            val currentEvent = activeThrottlingEvent
            when {
                state.thermalStatus >= THROTTLING_THRESHOLD && currentEvent == null -> {
                    val startTimeMs = System.currentTimeMillis()
                    val eventId = throttlingRepository.insert(
                        ThrottlingEvent(
                            timestamp = startTimeMs,
                            thermalStatus = state.thermalStatus.name,
                            batteryTempC = state.batteryTempC,
                            cpuTempC = state.cpuTempC,
                            foregroundApp = appUsageDataSource.getCurrentForegroundApp(),
                            durationMs = null
                        )
                    )
                    activeThrottlingEvent = ActiveThrottlingEvent(
                        id = eventId,
                        startTimeMs = startTimeMs,
                        peakStatus = state.thermalStatus
                    )
                }

                state.thermalStatus >= THROTTLING_THRESHOLD &&
                    currentEvent != null &&
                    state.thermalStatus > currentEvent.peakStatus -> {
                    throttlingRepository.updateSnapshot(
                        id = currentEvent.id,
                        thermalStatus = state.thermalStatus.name,
                        batteryTempC = state.batteryTempC,
                        cpuTempC = state.cpuTempC,
                        foregroundApp = appUsageDataSource.getCurrentForegroundApp()
                    )
                    activeThrottlingEvent = currentEvent.copy(peakStatus = state.thermalStatus)
                }

                state.thermalStatus < THROTTLING_THRESHOLD && currentEvent != null -> {
                    throttlingRepository.updateDuration(
                        id = currentEvent.id,
                        durationMs = (System.currentTimeMillis() - currentEvent.startTimeMs)
                            .coerceAtLeast(0L)
                    )
                    activeThrottlingEvent = null
                }
            }
        }
    }

    private data class ActiveThrottlingEvent(
        val id: Long,
        val startTimeMs: Long,
        val peakStatus: ThermalStatus
    )

    companion object {
        private val THROTTLING_THRESHOLD = ThermalStatus.SEVERE
        private const val STOP_TIMEOUT_MS = 0L
    }
}

private fun ThermalReadingEntity.toDomain() = ThermalReadingData(
    timestamp = timestamp,
    batteryTempC = batteryTempC,
    cpuTempC = cpuTempC,
    thermalStatus = thermalStatus,
    throttling = throttling
)
