package com.runcheck.domain.insights.rules

import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.ThermalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal const val INSIGHT_TEST_HOUR_MS = 60L * 60L * 1000L

internal class TestBatteryRepository(
    private val readings: List<BatteryReading>,
) : BatteryRepository {
    override fun getBatteryState(): Flow<BatteryState> = emptyFlow()

    override fun getReadingsSince(
        since: Long,
        limit: Int?,
    ): Flow<List<BatteryReading>> = emptyFlow()

    override suspend fun saveReading(state: BatteryState) = Unit

    override suspend fun getAllReadings(): List<BatteryReading> = readings

    override suspend fun getReadingsSinceSync(since: Long): List<BatteryReading> =
        readings.filter { it.timestamp >= since }

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit

    override suspend fun getLastChargingTimestamp(): Long? = null

    override suspend fun getLatestReadingTimestamp(): Long? = readings.maxOfOrNull { it.timestamp }
}

internal class TestNetworkRepository(
    private val readings: List<NetworkReading>,
) : NetworkRepository {
    override fun getNetworkState(): Flow<NetworkState> = emptyFlow()

    override suspend fun measureLatency(): Int? = null

    override suspend fun saveReading(state: NetworkState) = Unit

    override suspend fun getAllReadings(): List<NetworkReading> = readings

    override fun getReadingsSince(
        since: Long,
        limit: Int?,
    ): Flow<List<NetworkReading>> = emptyFlow()

    override suspend fun getReadingsSinceSync(since: Long): List<NetworkReading> =
        readings.filter { it.timestamp >= since }

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}

internal class TestThermalRepository(
    private val readings: List<ThermalReading>,
) : ThermalRepository {
    override fun getThermalState(): Flow<ThermalState> = emptyFlow()

    override fun getReadingsSince(
        since: Long,
        limit: Int?,
    ): Flow<List<ThermalReading>> = emptyFlow()

    override suspend fun getReadingsSinceSync(since: Long): List<ThermalReading> =
        readings.filter { it.timestamp >= since }

    override suspend fun saveReading(state: ThermalState) = Unit

    override suspend fun getAllReadings(): List<ThermalReading> = readings

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}

internal fun batteryReading(
    timestamp: Long,
    level: Int,
): BatteryReading =
    BatteryReading(
        timestamp = timestamp,
        level = level,
        voltageMv = 4000,
        temperatureC = 30f,
        currentMa = -400,
        currentConfidence = "HIGH",
        status = "DISCHARGING",
        plugType = "NONE",
        health = "GOOD",
        cycleCount = 300,
        healthPct = 90,
    )

internal fun batteryDrainReadings(
    now: Long,
    levels: List<Int>,
): List<BatteryReading> {
    val offsets = listOf(48L, 42L, 36L, 30L, 24L, 18L, 12L, 6L, 0L)
    return offsets.zip(levels).map { (offsetHours, level) ->
        batteryReading(now - offsetHours * INSIGHT_TEST_HOUR_MS, level)
    }
}

internal fun thermalReading(
    timestamp: Long,
    batteryTempC: Float,
    thermalStatus: Int,
    cpuTempC: Float = 70f,
): ThermalReading =
    ThermalReading(
        timestamp = timestamp,
        batteryTempC = batteryTempC,
        cpuTempC = cpuTempC,
        thermalStatus = thermalStatus,
        throttling = thermalStatus >= 3,
    )

internal fun heatDrainThermalReadings(now: Long): List<ThermalReading> =
    listOf(
        thermalReading(now - 42L * INSIGHT_TEST_HOUR_MS, 34.0f, 1),
        thermalReading(now - 36L * INSIGHT_TEST_HOUR_MS, 33.7f, 0),
        thermalReading(now - 30L * INSIGHT_TEST_HOUR_MS, 34.4f, 1),
        thermalReading(now - 24L * INSIGHT_TEST_HOUR_MS, 34.2f, 1),
        thermalReading(now - 18L * INSIGHT_TEST_HOUR_MS, 41.0f, 3),
        thermalReading(now - 12L * INSIGHT_TEST_HOUR_MS, 42.5f, 4),
        thermalReading(now - 6L * INSIGHT_TEST_HOUR_MS, 43.2f, 4),
        thermalReading(now, 42.8f, 3),
    )

internal fun networkReading(
    timestamp: Long,
    type: String,
    signalDbm: Int?,
    latencyMs: Int?,
    wifiSpeedMbps: Int? = null,
    wifiFrequency: Int? = null,
): NetworkReading =
    NetworkReading(
        timestamp = timestamp,
        type = type,
        signalDbm = signalDbm,
        wifiSpeedMbps = wifiSpeedMbps,
        wifiFrequency = wifiFrequency,
        carrier = if (type == "CELLULAR") "Carrier" else null,
        networkSubtype = if (type == "CELLULAR") "LTE" else null,
        latencyMs = latencyMs,
    )

internal fun weakCellularDrainReadings(now: Long): List<NetworkReading> =
    listOf(
        networkReading(now - 42L * INSIGHT_TEST_HOUR_MS, "CELLULAR", -96, 100),
        networkReading(now - 36L * INSIGHT_TEST_HOUR_MS, "CELLULAR", -98, 100),
        networkReading(now - 30L * INSIGHT_TEST_HOUR_MS, "CELLULAR", -97, 100),
        networkReading(now - 24L * INSIGHT_TEST_HOUR_MS, "CELLULAR", -95, 100),
        networkReading(now - 18L * INSIGHT_TEST_HOUR_MS, "CELLULAR", -113, 100),
        networkReading(now - 12L * INSIGHT_TEST_HOUR_MS, "CELLULAR", -116, 100),
        networkReading(now - 6L * INSIGHT_TEST_HOUR_MS, "CELLULAR", -118, 100),
        networkReading(now, "CELLULAR", -115, 100),
    )
