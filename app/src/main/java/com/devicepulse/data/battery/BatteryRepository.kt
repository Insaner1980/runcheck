package com.devicepulse.data.battery

import com.devicepulse.data.db.dao.BatteryReadingDao
import com.devicepulse.data.db.entity.BatteryReadingEntity
import com.devicepulse.data.device.DeviceProfileRepository
import com.devicepulse.domain.model.BatteryReading
import com.devicepulse.domain.model.BatteryState
import com.devicepulse.domain.model.Confidence
import com.devicepulse.domain.model.MeasuredValue
import com.devicepulse.domain.repository.BatteryRepository as BatteryRepositoryContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryRepository @Inject constructor(
    private val batteryDataSourceFactory: BatteryDataSourceFactory,
    private val deviceProfileRepository: DeviceProfileRepository,
    private val batteryReadingDao: BatteryReadingDao
) : BatteryRepositoryContract {

    override fun getBatteryState(): Flow<BatteryState> = flow {
        val profile = deviceProfileRepository.ensureProfileInternal()
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

        val extraFlow = combine(
            source.getPlugType(),
            source.getHealth(),
            source.getTechnology(),
            source.getCycleCount(),
            source.getHealthPercent()
        ) { plug, health, tech, cycle, healthPct ->
            BatteryStateExtra(plug, health, tech, cycle, healthPct)
        }

        combine(stateFlow, extraFlow) { partial, extra ->
            BatteryState(
                level = partial.level,
                voltageMv = partial.voltage,
                temperatureC = partial.temp,
                currentMa = partial.current,
                chargingStatus = partial.status,
                plugType = extra.plug,
                health = extra.health,
                technology = extra.tech,
                cycleCount = extra.cycle,
                healthPercent = extra.healthPct
            )
        }.collect { emit(it) }
    }

    override fun getReadingsSince(since: Long): Flow<List<BatteryReading>> {
        return batteryReadingDao.getReadingsSince(since).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveReading(state: BatteryState) {
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

    override suspend fun getAllReadings(): List<BatteryReading> {
        return batteryReadingDao.getAll().map { it.toDomain() }
    }

    override suspend fun deleteOlderThan(cutoff: Long) {
        batteryReadingDao.deleteOlderThan(cutoff)
    }

    private data class BatteryStatePartial(
        val level: Int,
        val voltage: Int,
        val temp: Float,
        val current: MeasuredValue<Int>,
        val status: com.devicepulse.domain.model.ChargingStatus
    )

    private data class BatteryStateExtra(
        val plug: com.devicepulse.domain.model.PlugType,
        val health: com.devicepulse.domain.model.BatteryHealth,
        val tech: String,
        val cycle: Int?,
        val healthPct: Int?
    )
}

private fun BatteryReadingEntity.toDomain() = BatteryReading(
    id = id,
    timestamp = timestamp,
    level = level,
    voltageMv = voltageMv,
    temperatureC = temperatureC,
    currentMa = currentMa,
    currentConfidence = currentConfidence,
    status = status,
    plugType = plugType,
    health = health,
    cycleCount = cycleCount,
    healthPct = healthPct
)
