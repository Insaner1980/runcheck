package com.runcheck.data.thermal

import com.runcheck.data.db.dao.ThermalReadingDao
import com.runcheck.data.db.entity.ThermalReadingEntity
import com.runcheck.data.device.DeviceProfileProvider
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.repository.ThermalReadingData
import com.runcheck.domain.repository.ThermalRepository as ThermalRepositoryContract
import com.runcheck.domain.usecase.TrackThrottlingEventsUseCase
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermalRepositoryImpl @Inject constructor(
    private val thermalDataSource: ThermalDataSource,
    private val deviceProfileProvider: DeviceProfileProvider,
    private val thermalReadingDao: ThermalReadingDao,
    private val trackThrottlingEvents: TrackThrottlingEventsUseCase
) : ThermalRepositoryContract {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val thermalStateFlow: Flow<ThermalState> by lazy {
        flow {
            val profile = deviceProfileProvider.getDeviceProfile()
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
                        isThrottling = thermalStatus >= ThermalStatus.SEVERE
                    )
                }.onEach { state ->
                    trackThrottlingEvents(state)
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

    private companion object {
        const val STOP_TIMEOUT_MS = 0L
    }
}

private fun ThermalReadingEntity.toDomain() = ThermalReadingData(
    timestamp = timestamp,
    batteryTempC = batteryTempC,
    cpuTempC = cpuTempC,
    thermalStatus = thermalStatus,
    throttling = throttling
)
