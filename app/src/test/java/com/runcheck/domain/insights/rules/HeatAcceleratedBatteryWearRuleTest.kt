package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.analysis.TimeWindowAligner
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
}
