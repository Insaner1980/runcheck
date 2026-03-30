package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.analysis.TimeWindowAligner
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.ThermalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatAcceleratedBatteryWearRuleTest {
    @Test
    fun `returns heat insight when hot windows drain faster`() =
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
            val thermalReadings =
                listOf(
                    thermal(now - 42L * hourMs, 34.0f, 1),
                    thermal(now - 36L * hourMs, 33.7f, 0),
                    thermal(now - 30L * hourMs, 34.4f, 1),
                    thermal(now - 24L * hourMs, 34.2f, 1),
                    thermal(now - 18L * hourMs, 41.0f, 3),
                    thermal(now - 12L * hourMs, 42.5f, 4),
                    thermal(now - 6L * hourMs, 43.2f, 4),
                    thermal(now, 42.8f, 3),
                )

            val rule =
                HeatAcceleratedBatteryWearRule(
                    batteryRepository = FakeHeatBatteryRepository(batteryReadings),
                    thermalRepository = FakeHeatThermalRepository(thermalReadings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                    timeWindowAligner = TimeWindowAligner(),
                )

            val insights = rule.evaluate(now)

            assertEquals(1, insights.size)
            val insight = insights.single()
            assertEquals(HeatAcceleratedBatteryWearRule.RULE_ID, insight.ruleId)
            assertEquals("heat_drain:60plus", insight.dedupeKey)
            assertEquals("200", insight.bodyArgs[0])
            assertEquals("43", insight.bodyArgs[1])
        }

    @Test
    fun `returns empty when hot intervals do not worsen drain`() =
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
            val thermalReadings =
                listOf(
                    thermal(now - 42L * hourMs, 34.0f, 1),
                    thermal(now - 36L * hourMs, 33.7f, 0),
                    thermal(now - 30L * hourMs, 34.4f, 1),
                    thermal(now - 24L * hourMs, 34.2f, 1),
                    thermal(now - 18L * hourMs, 41.0f, 3),
                    thermal(now - 12L * hourMs, 42.5f, 4),
                    thermal(now - 6L * hourMs, 43.2f, 4),
                    thermal(now, 42.8f, 3),
                )

            val rule =
                HeatAcceleratedBatteryWearRule(
                    batteryRepository = FakeHeatBatteryRepository(batteryReadings),
                    thermalRepository = FakeHeatThermalRepository(thermalReadings),
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

    private fun thermal(
        timestamp: Long,
        batteryTempC: Float,
        thermalStatus: Int,
    ) = ThermalReading(
        timestamp = timestamp,
        batteryTempC = batteryTempC,
        cpuTempC = 72f,
        thermalStatus = thermalStatus,
        throttling = thermalStatus >= 3,
    )
}

private class FakeHeatBatteryRepository(
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

private class FakeHeatThermalRepository(
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
