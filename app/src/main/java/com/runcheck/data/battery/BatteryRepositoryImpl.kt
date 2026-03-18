package com.runcheck.data.battery

import com.runcheck.data.db.dao.BatteryReadingDao
import com.runcheck.data.db.entity.BatteryReadingEntity
import com.runcheck.data.device.DeviceProfileRepositoryImpl
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.repository.BatteryRepository as BatteryRepositoryContract
import com.runcheck.util.TimestampSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryRepositoryImpl @Inject constructor(
    private val batteryDataSourceFactory: BatteryDataSourceFactory,
    private val deviceProfileRepository: DeviceProfileRepositoryImpl,
    private val batteryReadingDao: BatteryReadingDao,
    private val batteryCapacityReader: BatteryCapacityReader
) : BatteryRepositoryContract {

    override fun getBatteryState(): Flow<BatteryState> = flow {
        val profile = withContext(Dispatchers.IO) {
            deviceProfileRepository.ensureProfileInternal()
        }
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

        val chargeCounterFlow = source.getChargeCounter()
        val designCapacityMah = batteryCapacityReader.getDesignCapacityMah()

        combine(stateFlow, extraFlow, chargeCounterFlow) { partial, extra, chargeCounterMah ->
            val estimatedCapacityMah = if (designCapacityMah != null && extra.healthPct != null) {
                (designCapacityMah * extra.healthPct / 100)
            } else null

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
                healthPercent = extra.healthPct,
                remainingMah = chargeCounterMah,
                designCapacityMah = designCapacityMah,
                estimatedCapacityMah = estimatedCapacityMah
            )
        }.collect { emit(it) }
    }

    override fun getReadingsSince(since: Long, limit: Int?): Flow<List<BatteryReading>> {
        val now = System.currentTimeMillis()
        val readingsFlow = if (limit != null) {
            batteryReadingDao.getReadingsSinceLimited(since, now, limit)
        } else {
            batteryReadingDao.getReadingsSince(since, now)
        }
        return readingsFlow.map { entities ->
            entities.map { it.toDomain() }
                .filter { TimestampSanitizer.isUsable(it.timestamp) }
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

    override suspend fun getReadingsSinceSync(since: Long): List<BatteryReading> {
        return batteryReadingDao.getReadingsSinceSync(since).map { it.toDomain() }
    }

    override suspend fun deleteOlderThan(cutoff: Long) {
        batteryReadingDao.deleteOlderThan(cutoff)
    }

    override suspend fun getLastChargingTimestamp(): Long? {
        return batteryReadingDao.getLastChargingTimestamp()
    }

    private data class BatteryStatePartial(
        val level: Int,
        val voltage: Int,
        val temp: Float,
        val current: MeasuredValue<Int>,
        val status: com.runcheck.domain.model.ChargingStatus
    )

    private data class BatteryStateExtra(
        val plug: com.runcheck.domain.model.PlugType,
        val health: com.runcheck.domain.model.BatteryHealth,
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
