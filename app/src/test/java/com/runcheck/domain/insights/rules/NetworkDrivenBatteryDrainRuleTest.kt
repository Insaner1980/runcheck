package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.analysis.TimeWindowAligner
import com.runcheck.domain.insights.model.InsightPriority
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

    @Test
    fun `returns empty when battery and network history is too sparse`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val batteryReadings = batteryDrainReadings(now, listOf(80, 79, 78))
            val networkReadings = weakCellularDrainReadings(now).take(3)

            val rule =
                NetworkDrivenBatteryDrainRule(
                    batteryRepository = TestBatteryRepository(batteryReadings),
                    networkRepository = TestNetworkRepository(networkReadings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                    timeWindowAligner = TimeWindowAligner(),
                )

            assertTrue(rule.evaluate(now).isEmpty())
        }

    @Test
    fun `returns medium priority for moderate weak signal drain increase`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val batteryReadings = batteryDrainReadings(now, listOf(100, 95, 90, 85, 80, 74, 68, 62, 56))
            val networkReadings = moderateWeakCellularDrainReadings(now)

            val rule =
                NetworkDrivenBatteryDrainRule(
                    batteryRepository = TestBatteryRepository(batteryReadings),
                    networkRepository = TestNetworkRepository(networkReadings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                    timeWindowAligner = TimeWindowAligner(),
                )

            val insight = rule.evaluate(now).single()

            assertEquals("cellular_drain:20plus", insight.dedupeKey)
            assertEquals(InsightPriority.MEDIUM, insight.priority)
            assertEquals("20", insight.bodyArgs[0])
            assertEquals("-110", insight.bodyArgs[1])
        }

    @Test
    fun `returns empty when network context is not cellular signal data`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val batteryReadings = batteryDrainReadings(now, listOf(80, 79, 78, 77, 76, 73, 70, 67, 64))
            val wifiReadings =
                List(8) { index ->
                    networkReading(
                        timestamp = now - (7L - index) * 6L * INSIGHT_TEST_HOUR_MS,
                        type = "WIFI",
                        signalDbm = null,
                        latencyMs = 40,
                    )
                }

            val rule =
                NetworkDrivenBatteryDrainRule(
                    batteryRepository = TestBatteryRepository(batteryReadings),
                    networkRepository = TestNetworkRepository(wifiReadings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                    timeWindowAligner = TimeWindowAligner(),
                )

            assertTrue(rule.evaluate(now).isEmpty())
        }

    private fun moderateWeakCellularDrainReadings(now: Long) =
        listOf(
            networkReading(now - 42L * INSIGHT_TEST_HOUR_MS, "CELLULAR", -96, 100),
            networkReading(now - 36L * INSIGHT_TEST_HOUR_MS, "CELLULAR", -98, 100),
            networkReading(now - 30L * INSIGHT_TEST_HOUR_MS, "CELLULAR", -97, 100),
            networkReading(now - 24L * INSIGHT_TEST_HOUR_MS, "CELLULAR", -95, 100),
            networkReading(now - 18L * INSIGHT_TEST_HOUR_MS, "CELLULAR", -110, 100),
            networkReading(now - 12L * INSIGHT_TEST_HOUR_MS, "CELLULAR", -110, 100),
            networkReading(now - 6L * INSIGHT_TEST_HOUR_MS, "CELLULAR", -110, 100),
            networkReading(now, "CELLULAR", -110, 100),
        )
}
