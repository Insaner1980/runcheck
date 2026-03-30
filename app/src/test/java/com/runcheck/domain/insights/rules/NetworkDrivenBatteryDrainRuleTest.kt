package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.analysis.TimeWindowAligner
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkDrivenBatteryDrainRuleTest {
    @Test
    fun `returns network drain insight when weak cellular windows drain faster`() =
        runTest {
            val hourMs = 60L * 60L * 1000L
            val now = 100L * hourMs
            val batteryReadings =
                listOf(
                    reading(now - 48L * hourMs, 80),
                    reading(now - 42L * hourMs, 79),
                    reading(now - 36L * hourMs, 78),
                    reading(now - 30L * hourMs, 77),
                    reading(now - 24L * hourMs, 76),
                    reading(now - 18L * hourMs, 73),
                    reading(now - 12L * hourMs, 70),
                    reading(now - 6L * hourMs, 67),
                    reading(now, 64),
                )
            val networkReadings =
                listOf(
                    network(now - 42L * hourMs, -96),
                    network(now - 36L * hourMs, -98),
                    network(now - 30L * hourMs, -97),
                    network(now - 24L * hourMs, -95),
                    network(now - 18L * hourMs, -113),
                    network(now - 12L * hourMs, -116),
                    network(now - 6L * hourMs, -118),
                    network(now, -115),
                )

            val rule =
                NetworkDrivenBatteryDrainRule(
                    batteryRepository = FakeBatteryRepository(batteryReadings),
                    networkRepository = FakeBatteryDrainNetworkRepository(networkReadings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                    timeWindowAligner = TimeWindowAligner(),
                )

            val insights = rule.evaluate(now)

            assertEquals(1, insights.size)
            val insight = insights.single()
            assertEquals(NetworkDrivenBatteryDrainRule.RULE_ID, insight.ruleId)
            assertEquals("cellular_drain:50plus", insight.dedupeKey)
            assertEquals("200", insight.bodyArgs[0])
            assertEquals("-115", insight.bodyArgs[1])
        }

    @Test
    fun `returns empty when weak signal does not correlate with higher drain`() =
        runTest {
            val hourMs = 60L * 60L * 1000L
            val now = 100L * hourMs
            val batteryReadings =
                listOf(
                    reading(now - 48L * hourMs, 80),
                    reading(now - 42L * hourMs, 79),
                    reading(now - 36L * hourMs, 78),
                    reading(now - 30L * hourMs, 77),
                    reading(now - 24L * hourMs, 76),
                    reading(now - 18L * hourMs, 75),
                    reading(now - 12L * hourMs, 74),
                    reading(now - 6L * hourMs, 73),
                    reading(now, 72),
                )
            val networkReadings =
                listOf(
                    network(now - 42L * hourMs, -96),
                    network(now - 36L * hourMs, -98),
                    network(now - 30L * hourMs, -97),
                    network(now - 24L * hourMs, -95),
                    network(now - 18L * hourMs, -113),
                    network(now - 12L * hourMs, -116),
                    network(now - 6L * hourMs, -118),
                    network(now, -115),
                )

            val rule =
                NetworkDrivenBatteryDrainRule(
                    batteryRepository = FakeBatteryRepository(batteryReadings),
                    networkRepository = FakeBatteryDrainNetworkRepository(networkReadings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                    timeWindowAligner = TimeWindowAligner(),
                )

            val insights = rule.evaluate(now)

            assertTrue(insights.isEmpty())
        }

    private fun reading(
        timestamp: Long,
        level: Int,
    ) = BatteryReading(
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

    private fun network(
        timestamp: Long,
        signalDbm: Int,
    ) = NetworkReading(
        timestamp = timestamp,
        type = "CELLULAR",
        signalDbm = signalDbm,
        wifiSpeedMbps = null,
        wifiFrequency = null,
        carrier = "Carrier",
        networkSubtype = "LTE",
        latencyMs = 100,
    )
}

private class FakeBatteryRepository(
    private val readings: List<BatteryReading>,
) : BatteryRepository {
    override fun getBatteryState() = emptyFlow<com.runcheck.domain.model.BatteryState>()

    override fun getReadingsSince(
        since: Long,
        limit: Int?,
    ): Flow<List<BatteryReading>> = emptyFlow()

    override suspend fun saveReading(state: com.runcheck.domain.model.BatteryState) = Unit

    override suspend fun getAllReadings(): List<BatteryReading> = readings

    override suspend fun getReadingsSinceSync(since: Long): List<BatteryReading> =
        readings.filter { it.timestamp >= since }

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit

    override suspend fun getLastChargingTimestamp(): Long? = null

    override suspend fun getLatestReadingTimestamp(): Long? = readings.maxOfOrNull { it.timestamp }
}

private class FakeBatteryDrainNetworkRepository(
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
