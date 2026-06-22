package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.analysis.TimeWindowAligner
import com.runcheck.domain.insights.model.InsightPriority
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatAcceleratedBatteryWearRuleTest {
    @Test
    fun `returns heat insight when hot windows drain faster`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val batteryReadings = batteryDrainReadings(now, listOf(80, 79, 78, 77, 76, 73, 70, 67, 64))
            val thermalReadings = heatDrainThermalReadings(now)

            val rule =
                HeatAcceleratedBatteryWearRule(
                    batteryRepository = TestBatteryRepository(batteryReadings),
                    thermalRepository = TestThermalRepository(thermalReadings),
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
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val batteryReadings = batteryDrainReadings(now, listOf(80, 79, 78, 77, 76, 75, 74, 73, 72))
            val thermalReadings = heatDrainThermalReadings(now)

            val rule =
                HeatAcceleratedBatteryWearRule(
                    batteryRepository = TestBatteryRepository(batteryReadings),
                    thermalRepository = TestThermalRepository(thermalReadings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                    timeWindowAligner = TimeWindowAligner(),
                )

            val insights = rule.evaluate(now)

            assertTrue(insights.isEmpty())
        }

    @Test
    fun `returns empty when heat and battery history is too sparse`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val batteryReadings = batteryDrainReadings(now, listOf(80, 79, 78))
            val thermalReadings = heatDrainThermalReadings(now).take(3)

            val rule =
                HeatAcceleratedBatteryWearRule(
                    batteryRepository = TestBatteryRepository(batteryReadings),
                    thermalRepository = TestThermalRepository(thermalReadings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                    timeWindowAligner = TimeWindowAligner(),
                )

            assertTrue(rule.evaluate(now).isEmpty())
        }

    @Test
    fun `returns lower heat drain buckets without high temperature priority`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val mediumThermalReadings = moderateHeatDrainThermalReadings(now)

            val mediumInsight =
                HeatAcceleratedBatteryWearRule(
                    batteryRepository =
                        TestBatteryRepository(
                            batteryDrainReadings(now, listOf(100, 96, 92, 88, 84, 79, 74, 69, 64)),
                        ),
                    thermalRepository = TestThermalRepository(mediumThermalReadings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                    timeWindowAligner = TimeWindowAligner(),
                ).evaluate(now)
                    .single()
            val fortyPlusInsight =
                HeatAcceleratedBatteryWearRule(
                    batteryRepository =
                        TestBatteryRepository(
                            batteryDrainReadings(now, listOf(100, 96, 92, 88, 84, 78, 72, 66, 60)),
                        ),
                    thermalRepository = TestThermalRepository(mediumThermalReadings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                    timeWindowAligner = TimeWindowAligner(),
                ).evaluate(now)
                    .single()

            assertEquals("heat_drain:20plus", mediumInsight.dedupeKey)
            assertEquals(InsightPriority.MEDIUM, mediumInsight.priority)
            assertEquals("heat_drain:40plus", fortyPlusInsight.dedupeKey)
        }

    @Test
    fun `returns empty when thermal context cannot classify hot or cool drain windows`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val batteryReadings = batteryDrainReadings(now, listOf(80, 79, 78, 77, 76, 73, 70, 67, 64))
            val neutralThermalReadings =
                List(8) { index ->
                    thermalReading(
                        timestamp = now - (7L - index) * 6L * INSIGHT_TEST_HOUR_MS,
                        batteryTempC = 37f,
                        thermalStatus = 1,
                    )
                }

            val rule =
                HeatAcceleratedBatteryWearRule(
                    batteryRepository = TestBatteryRepository(batteryReadings),
                    thermalRepository = TestThermalRepository(neutralThermalReadings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                    timeWindowAligner = TimeWindowAligner(),
                )

            assertTrue(rule.evaluate(now).isEmpty())
        }

    private fun moderateHeatDrainThermalReadings(now: Long) =
        listOf(
            thermalReading(now - 42L * INSIGHT_TEST_HOUR_MS, 34.0f, 1),
            thermalReading(now - 36L * INSIGHT_TEST_HOUR_MS, 33.7f, 0),
            thermalReading(now - 30L * INSIGHT_TEST_HOUR_MS, 34.4f, 1),
            thermalReading(now - 24L * INSIGHT_TEST_HOUR_MS, 34.2f, 1),
            thermalReading(now - 18L * INSIGHT_TEST_HOUR_MS, 40.5f, 2),
            thermalReading(now - 12L * INSIGHT_TEST_HOUR_MS, 40.8f, 2),
            thermalReading(now - 6L * INSIGHT_TEST_HOUR_MS, 41.0f, 2),
            thermalReading(now, 41.2f, 2),
        )
}
