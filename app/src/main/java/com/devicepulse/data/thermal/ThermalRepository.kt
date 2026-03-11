package com.devicepulse.data.thermal

import com.devicepulse.data.db.dao.ThermalReadingDao
import com.devicepulse.data.db.entity.ThermalReadingEntity
import com.devicepulse.data.device.DeviceProfileRepository
import com.devicepulse.domain.model.ThermalState
import com.devicepulse.domain.model.ThermalStatus
import com.devicepulse.domain.repository.ThermalReadingData
import com.devicepulse.domain.repository.ThermalRepository as ThermalRepositoryContract
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
) : ThermalRepositoryContract {

    override fun getThermalState(): Flow<ThermalState> = flow {
        val profile = deviceProfileRepository.ensureProfileInternal()

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
}

private fun ThermalReadingEntity.toDomain() = ThermalReadingData(
    timestamp = timestamp,
    batteryTempC = batteryTempC,
    cpuTempC = cpuTempC,
    thermalStatus = thermalStatus,
    throttling = throttling
)
