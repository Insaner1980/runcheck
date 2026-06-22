package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.model.BatteryReading
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryDegradationTrendRuleTest {
    @Test
    fun `emits high priority battery insight when current week drains faster than previous week`() =
        runTest {
            val now = WINDOW_MS * 3
            val previousWindowStart = now - (WINDOW_MS * 2)
            val currentWindowStart = now - WINDOW_MS
            val readings =
                windowReadings(previousWindowStart, startLevel = 100, dropPerSample = 1) +
                    windowReadings(currentWindowStart, startLevel = 100, dropPerSample = 3)
            val rule =
                BatteryDegradationTrendRule(
                    batteryRepository = TestBatteryRepository(readings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                )

            val result = rule.evaluate(now)

            assertEquals(1, result.size)
            val insight = result.single()
            assertEquals(BatteryDegradationTrendRule.RULE_ID, insight.ruleId)
            assertEquals(InsightType.BATTERY, insight.type)
            assertEquals(InsightPriority.HIGH, insight.priority)
            assertEquals(InsightTarget.BATTERY, insight.target)
            assertEquals(listOf("200"), insight.bodyArgs)
            assertTrue(insight.confidence >= 0.5f)
        }

    @Test
    fun `returns empty when either comparison window has too few discharging readings`() =
        runTest {
            val now = WINDOW_MS * 3
            val previousWindowStart = now - (WINDOW_MS * 2)
            val currentWindowStart = now - WINDOW_MS
            val readings =
                windowReadings(previousWindowStart, startLevel = 100, dropPerSample = 1, count = 20) +
                    windowReadings(currentWindowStart, startLevel = 100, dropPerSample = 3, count = 10)
            val rule =
                BatteryDegradationTrendRule(
                    batteryRepository = TestBatteryRepository(readings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                )

            assertTrue(rule.evaluate(now).isEmpty())
        }

    @Test
    fun `returns empty when drain increase is below threshold`() =
        runTest {
            val now = WINDOW_MS * 3
            val previousWindowStart = now - (WINDOW_MS * 2)
            val currentWindowStart = now - WINDOW_MS
            val readings =
                windowReadings(previousWindowStart, startLevel = 100, dropPerSample = 2) +
                    windowReadings(currentWindowStart, startLevel = 100, dropPerSample = 2)
            val rule =
                BatteryDegradationTrendRule(
                    batteryRepository = TestBatteryRepository(readings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                )

            assertTrue(rule.evaluate(now).isEmpty())
        }

    private fun windowReadings(
        start: Long,
        startLevel: Int,
        dropPerSample: Int,
        count: Int = 20,
    ): List<BatteryReading> {
        val interval = WINDOW_MS / (count + 1)
        return (0 until count).map { index ->
            batteryReading(
                timestamp = start + ((index + 1) * interval),
                level = startLevel - (index * dropPerSample),
            )
        }
    }

    private companion object {
        private const val WINDOW_MS = 7L * 24L * 60L * 60L * 1000L
    }
}
