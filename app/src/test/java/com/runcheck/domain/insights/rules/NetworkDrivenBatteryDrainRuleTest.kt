package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.analysis.TimeWindowAligner
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkDrivenBatteryDrainRuleTest {
    @Test
    fun `returns network drain insight when weak cellular windows drain faster`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val batteryReadings = batteryDrainReadings(now, listOf(80, 79, 78, 77, 76, 73, 70, 67, 64))
            val networkReadings = weakCellularDrainReadings(now)

            val rule =
                NetworkDrivenBatteryDrainRule(
                    batteryRepository = TestBatteryRepository(batteryReadings),
                    networkRepository = TestNetworkRepository(networkReadings),
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
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val batteryReadings = batteryDrainReadings(now, listOf(80, 79, 78, 77, 76, 75, 74, 73, 72))
            val networkReadings = weakCellularDrainReadings(now)

            val rule =
                NetworkDrivenBatteryDrainRule(
                    batteryRepository = TestBatteryRepository(batteryReadings),
                    networkRepository = TestNetworkRepository(networkReadings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                    timeWindowAligner = TimeWindowAligner(),
                )

            val insights = rule.evaluate(now)

            assertTrue(insights.isEmpty())
        }
}
