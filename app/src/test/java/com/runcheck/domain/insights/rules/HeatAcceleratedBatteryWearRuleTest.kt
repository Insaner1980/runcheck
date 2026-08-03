package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.analysis.TimeWindowAligner
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.model.ThermalReading
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatAcceleratedBatteryWearRuleTest {
    @Test
    fun `returns heat insight when hot windows drain faster`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val insight = evaluate(now, listOf(80, 79, 78, 77, 76, 73, 70, 67, 64)).single()
            assertEquals(HeatAcceleratedBatteryWearRule.RULE_ID, insight.ruleId)
            assertEquals("heat_drain:60plus", insight.dedupeKey)
            assertEquals("200", insight.bodyArgs[0])
            assertEquals("43", insight.bodyArgs[1])
        }

    @Test
    fun `returns empty when hot intervals do not worsen drain`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            assertTrue(evaluate(now, listOf(80, 79, 78, 77, 76, 75, 74, 73, 72)).isEmpty())
        }

    @Test
    fun `returns empty when heat and battery history is too sparse`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val thermalReadings = heatDrainThermalReadings(now).take(3)

            assertTrue(evaluate(now, listOf(80, 79, 78), thermalReadings).isEmpty())
        }

    @Test
    fun `returns lower heat drain buckets without high temperature priority`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val mediumThermalReadings = heatDrainThermalReadings(now, moderate = true)

            val mediumInsight =
                evaluate(now, listOf(100, 96, 92, 88, 84, 79, 74, 69, 64), mediumThermalReadings).single()
            val fortyPlusInsight =
                evaluate(now, listOf(100, 96, 92, 88, 84, 78, 72, 66, 60), mediumThermalReadings).single()

            assertEquals("heat_drain:20plus", mediumInsight.dedupeKey)
            assertEquals(InsightPriority.MEDIUM, mediumInsight.priority)
            assertEquals("heat_drain:40plus", fortyPlusInsight.dedupeKey)
        }

    @Test
    fun `returns empty when thermal context cannot classify hot or cool drain windows`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val neutralThermalReadings =
                List(8) { index ->
                    thermalReading(
                        timestamp = now - (7L - index) * 6L * INSIGHT_TEST_HOUR_MS,
                        batteryTempC = 37f,
                        thermalStatus = 1,
                    )
                }

            assertTrue(
                evaluate(now, listOf(80, 79, 78, 77, 76, 73, 70, 67, 64), neutralThermalReadings).isEmpty(),
            )
        }

    private suspend fun evaluate(
        now: Long,
        levels: List<Int>,
        thermalReadings: List<ThermalReading> = heatDrainThermalReadings(now),
    ) = HeatAcceleratedBatteryWearRule(
        batteryRepository = TestBatteryRepository(batteryDrainReadings(now, levels)),
        thermalRepository = TestThermalRepository(thermalReadings),
        batteryDrainAnalyzer = BatteryDrainAnalyzer(),
        timeWindowAligner = TimeWindowAligner(),
    ).evaluate(now)
}
