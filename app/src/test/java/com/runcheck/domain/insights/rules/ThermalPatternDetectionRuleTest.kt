package com.runcheck.domain.insights.rules

import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.repository.ThermalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalPatternDetectionRuleTest {
    @Test
    fun `returns thermal pattern insight when heat stays elevated`() =
        runTest {
            val hourMs = 60L * 60L * 1000L
            val now = 100L * hourMs
            val readings =
                listOf(
                    thermal(now - 42L * hourMs, 34.0f, 1),
                    thermal(now - 36L * hourMs, 33.5f, 0),
                    thermal(now - 30L * hourMs, 40.2f, 2),
                    thermal(now - 24L * hourMs, 41.0f, 3),
                    thermal(now - 18L * hourMs, 42.5f, 4),
                    thermal(now - 12L * hourMs, 43.0f, 4),
                    thermal(now - 6L * hourMs, 42.2f, 3),
                    thermal(now, 41.8f, 3),
                )

            val rule = ThermalPatternDetectionRule(FakeThermalPatternRepository(readings))

            val insights = rule.evaluate(now)

            assertEquals(1, insights.size)
            val insight = insights.single()
            assertEquals(ThermalPatternDetectionRule.RULE_ID, insight.ruleId)
            assertEquals("hot_pattern:70plus", insight.dedupeKey)
            assertEquals("75", insight.bodyArgs[0])
            assertEquals("42", insight.bodyArgs[1])
        }

    @Test
    fun `returns empty when hot readings are isolated`() =
        runTest {
            val hourMs = 60L * 60L * 1000L
            val now = 100L * hourMs
            val readings =
                listOf(
                    thermal(now - 42L * hourMs, 34.0f, 1),
                    thermal(now - 36L * hourMs, 33.5f, 0),
                    thermal(now - 30L * hourMs, 35.2f, 1),
                    thermal(now - 24L * hourMs, 36.0f, 1),
                    thermal(now - 18L * hourMs, 39.0f, 1),
                    thermal(now - 12L * hourMs, 40.5f, 2),
                )

            val rule = ThermalPatternDetectionRule(FakeThermalPatternRepository(readings))

            val insights = rule.evaluate(now)

            assertTrue(insights.isEmpty())
        }

    private fun thermal(
        timestamp: Long,
        batteryTempC: Float,
        thermalStatus: Int,
    ) = ThermalReading(
        timestamp = timestamp,
        batteryTempC = batteryTempC,
        cpuTempC = 70f,
        thermalStatus = thermalStatus,
        throttling = thermalStatus >= 3,
    )
}

private class FakeThermalPatternRepository(
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
