package com.devicepulse.data.battery

import com.devicepulse.data.db.dao.BatteryReadingDao
import com.devicepulse.data.db.entity.BatteryReadingEntity
import com.devicepulse.data.device.DeviceProfileRepository
import com.devicepulse.domain.model.BatteryState
import com.devicepulse.domain.model.Confidence
import com.devicepulse.domain.model.MeasuredValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryRepository @Inject constructor(
    private val batteryDataSourceFactory: BatteryDataSourceFactory,
    private val deviceProfileRepository: DeviceProfileRepository,
    private val batteryReadingDao: BatteryReadingDao
) {

    fun getBatteryState(): Flow<BatteryState> = flow {
        val profile = deviceProfileRepository.ensureProfile()
        val source = batteryDataSourceFactory.create(profile)

        val stateFlow = combine(
            source.getLevel(),
            source.getVoltage(),
            source.getTemperature(),
            source.getCurrentNow(),
            source.getChargingStatus()
        ) { level, voltage, temp, current, status ->
            BatteryStatePartial(level, voltage, temp, current, status)
        }

        combine(
            stateFlow,
            source.getPlugType(),
            source.getHealth(),
            source.getTechnology(),
            source.getCycleCount()
        ) { partial, plug, health, tech, cycle ->
            BatteryState(
                level = partial.level,
                voltageMv = partial.voltage,
                temperatureC = partial.temp,
                currentMa = partial.current,
                chargingStatus = partial.status,
                plugType = plug,
                health = health,
                technology = tech,
                cycleCount = cycle
            )
        }.collect { emit(it) }
    }

    fun getReadingsSince(since: Long): Flow<List<BatteryReadingEntity>> {
        return batteryReadingDao.getReadingsSince(since)
    }

    suspend fun saveReading(state: BatteryState) {
        val entity = BatteryReadingEntity(
            timestamp = System.currentTimeMillis(),
            level = state.level,
            voltageMv = state.voltageMv,
            temperatureC = state.temperatureC,
            currentMa = if (state.currentMa.confidence != Confidence.UNAVAILABLE) {
                state.currentMa.value
            } else null,
            currentConfidence = state.currentMa.confidence.name,
            status = state.chargingStatus.name,
            plugType = state.plugType.name,
            health = state.health.name,
            cycleCount = state.cycleCount,
            healthPct = state.healthPercent
        )
        batteryReadingDao.insert(entity)
    }

    private data class BatteryStatePartial(
        val level: Int,
        val voltage: Int,
        val temp: Float,
        val current: MeasuredValue<Int>,
        val status: com.devicepulse.domain.model.ChargingStatus
    )
}
