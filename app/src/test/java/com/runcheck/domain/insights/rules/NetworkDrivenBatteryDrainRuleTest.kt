package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.analysis.TimeWindowAligner
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.model.NetworkReading
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkDrivenBatteryDrainRuleTest {
    @Test
    fun `returns network drain insight when weak cellular windows drain faster`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val insight = evaluate(now, listOf(80, 79, 78, 77, 76, 73, 70, 67, 64)).single()
            assertEquals(NetworkDrivenBatteryDrainRule.RULE_ID, insight.ruleId)
            assertEquals("cellular_drain:50plus", insight.dedupeKey)
            assertEquals("200", insight.bodyArgs[0])
            assertEquals("-115", insight.bodyArgs[1])
        }

    @Test
    fun `returns empty when weak signal does not correlate with higher drain`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            assertTrue(evaluate(now, listOf(80, 79, 78, 77, 76, 75, 74, 73, 72)).isEmpty())
        }

    @Test
    fun `returns empty when battery and network history is too sparse`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val networkReadings = weakCellularDrainReadings(now).take(3)

            assertTrue(evaluate(now, listOf(80, 79, 78), networkReadings).isEmpty())
        }

    @Test
    fun `returns medium priority for moderate weak signal drain increase`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val networkReadings = weakCellularDrainReadings(now, weakSignalDbm = -110)
            val insight = evaluate(now, listOf(100, 95, 90, 85, 80, 74, 68, 62, 56), networkReadings).single()

            assertEquals("cellular_drain:20plus", insight.dedupeKey)
            assertEquals(InsightPriority.MEDIUM, insight.priority)
            assertEquals("20", insight.bodyArgs[0])
            assertEquals("-110", insight.bodyArgs[1])
        }

    @Test
    fun `returns empty when network context is not cellular signal data`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val wifiReadings =
                List(8) { index ->
                    networkReading(
                        timestamp = now - (7L - index) * 6L * INSIGHT_TEST_HOUR_MS,
                        type = "WIFI",
                        signalDbm = null,
                        latencyMs = 40,
                    )
                }

            assertTrue(evaluate(now, listOf(80, 79, 78, 77, 76, 73, 70, 67, 64), wifiReadings).isEmpty())
        }

    private suspend fun evaluate(
        now: Long,
        levels: List<Int>,
        networkReadings: List<NetworkReading> = weakCellularDrainReadings(now),
    ) = NetworkDrivenBatteryDrainRule(
        batteryRepository = TestBatteryRepository(batteryDrainReadings(now, levels)),
        networkRepository = TestNetworkRepository(networkReadings),
        batteryDrainAnalyzer = BatteryDrainAnalyzer(),
        timeWindowAligner = TimeWindowAligner(),
    ).evaluate(now)
}
