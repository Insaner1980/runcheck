package com.devicepulse.data.thermal

import com.devicepulse.data.db.dao.ThermalReadingDao
import com.devicepulse.data.db.entity.ThermalReadingEntity
import com.devicepulse.data.device.DeviceProfileRepository
import com.devicepulse.domain.model.ThermalState
import com.devicepulse.domain.model.ThermalStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermalRepository @Inject constructor(
    private val thermalDataSource: ThermalDataSource,
    private val deviceProfileRepository: DeviceProfileRepository,
    private val thermalReadingDao: ThermalReadingDao
) {

    fun getThermalState(): Flow<ThermalState> = flow {
        val profile = deviceProfileRepository.ensureProfile()

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
                isThrottling = thermalStatus >= ThermalStatus.MODERATE
            )
        }.collect { emit(it) }
    }

    fun getReadingsSince(since: Long): Flow<List<ThermalReadingEntity>> {
        return thermalReadingDao.getReadingsSince(since)
    }

    suspend fun saveReading(state: ThermalState) {
        val entity = ThermalReadingEntity(
            timestamp = System.currentTimeMillis(),
            batteryTempC = state.batteryTempC,
            cpuTempC = state.cpuTempC,
            thermalStatus = state.thermalStatus.ordinal,
            throttling = state.isThrottling
        )
        thermalReadingDao.insert(entity)
    }
}
