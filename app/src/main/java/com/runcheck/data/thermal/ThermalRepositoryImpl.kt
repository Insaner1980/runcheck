package com.runcheck.data.thermal

import android.os.SystemClock
import com.runcheck.data.db.dao.ThermalReadingDao
import com.runcheck.data.db.entity.ThermalReadingEntity
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatusPersistence
import com.runcheck.domain.usecase.TrackThrottlingEventsUseCase
import com.runcheck.util.AppDispatchers
import com.runcheck.util.ReleaseSafeLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton
import com.runcheck.domain.repository.ThermalRepository as ThermalRepositoryContract

@Singleton
class ThermalRepositoryImpl
    @Inject
    constructor(
        private val thermalDataSource: ThermalDataSource,
        private val thermalReadingDao: ThermalReadingDao,
        private val trackThrottlingEvents: TrackThrottlingEventsUseCase,
        private val dispatchers: AppDispatchers,
    ) : ThermalRepositoryContract {
        @Suppress("TooGenericExceptionCaught")
        private fun observeThermalState(bestEffortTracking: Boolean): Flow<ThermalState> =
            flow {
                emitAll(
                    combine(
                        thermalDataSource.getBatteryTemperature(),
                        thermalDataSource.getThermalStatus(),
                        thermalDataSource.getThermalHeadroom(),
                    ) { batteryTemp, thermalStatus, headroom ->
                        ThermalState(
                            batteryTempC = batteryTemp,
                            cpuTempC = null,
                            thermalHeadroom = headroom,
                            thermalStatus = thermalStatus,
                            isThrottling = thermalStatus.isThrottling,
                        )
                    }.onEach { state ->
                        try {
                            trackThrottlingEvents(
                                state = state,
                                wallClockMillis = System.currentTimeMillis(),
                                elapsedRealtimeMillis = SystemClock.elapsedRealtime(),
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            if (!bestEffortTracking) throw error
                            ReleaseSafeLog.error("ThermalRepository", "Throttling event tracking failed", error)
                        }
                    },
                )
            }.flowOn(dispatchers.io)

        override fun getThermalState(): Flow<ThermalState> = observeThermalState(bestEffortTracking = false)

        override fun getLiveThermalState(): Flow<ThermalState> = observeThermalState(bestEffortTracking = true)

        override fun getReadingsSince(
            since: Long,
            limit: Int?,
        ): Flow<List<ThermalReading>> {
            val source =
                if (limit != null) {
                    thermalReadingDao.getReadingsSinceLimited(since, limit)
                } else {
                    thermalReadingDao.getReadingsSince(since)
                }
            return source
                .map { entities -> entities.map { it.toDomain() } }
                .flowOn(dispatchers.io)
        }

        override suspend fun getReadingsSinceSync(since: Long): List<ThermalReading> =
            thermalReadingDao.getReadingsSinceSync(since).map { it.toDomain() }

        override suspend fun saveReading(state: ThermalState) {
            val entity =
                ThermalReadingEntity(
                    timestamp = System.currentTimeMillis(),
                    batteryTempC = state.batteryTempC,
                    cpuTempC = state.cpuTempC,
                    thermalStatus = ThermalStatusPersistence.toCode(state.thermalStatus),
                    throttling = state.isThrottling,
                )
            thermalReadingDao.insert(entity)
        }

        override suspend fun getAllReadings(): List<ThermalReading> = thermalReadingDao.getAll().map { it.toDomain() }

        override suspend fun deleteOlderThan(cutoff: Long) = thermalReadingDao.deleteOlderThan(cutoff)

        override suspend fun deleteAll() = thermalReadingDao.deleteAll()
    }

private fun ThermalReadingEntity.toDomain() =
    ThermalReading(
        timestamp = timestamp,
        batteryTempC = batteryTempC,
        cpuTempC = cpuTempC,
        thermalStatus = thermalStatus,
        throttling = throttling,
    )
