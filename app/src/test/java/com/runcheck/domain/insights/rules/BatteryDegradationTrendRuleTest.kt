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
            val rule = degradationRule(comparisonReadings(previousDrop = 1, currentDrop = 3))

            val result = rule.evaluate(NOW)

            assertEquals(1, result.size)
            val insight = result.single()
            assertEquals(BatteryDegradationTrendRule.RULE_ID, insight.ruleId)
            assertEquals(InsightType.BATTERY, insight.type)
            assertEquals(InsightPriority.HIGH, insight.priority)
            assertEquals(InsightTarget.BATTERY, insight.target)
            assertEquals("battery_degradation:200plus", insight.dedupeKey)
            assertEquals(listOf("200"), insight.bodyArgs)
            assertTrue(insight.confidence >= 0.5f)
        }

    @Test
    fun `keeps dedupe key stable when evaluation time moves within the same finding`() =
        runTest {
            val rule = degradationRule(comparisonReadings(previousDrop = 1, currentDrop = 3))

            val firstKey = rule.evaluate(NOW).single().dedupeKey
            val nextRunKey = rule.evaluate(NOW + 1).single().dedupeKey

            assertEquals(firstKey, nextRunKey)
        }

    @Test
    fun `changes dedupe key when degradation moves to another severity bucket`() =
        runTest {
            val moderateRule = degradationRule(comparisonReadings(previousDrop = 1, currentDrop = 2))
            val severeRule = degradationRule(comparisonReadings(previousDrop = 1, currentDrop = 3))

            assertEquals("battery_degradation:100plus", moderateRule.evaluate(NOW).single().dedupeKey)
            assertEquals("battery_degradation:200plus", severeRule.evaluate(NOW).single().dedupeKey)
        }

    @Test
    fun `returns empty when either comparison window has too few discharging readings`() =
        runTest {
            val readings = comparisonReadings(previousDrop = 1, currentDrop = 3, currentCount = 10)
            val rule = degradationRule(readings)

            assertTrue(rule.evaluate(NOW).isEmpty())
        }

    @Test
    fun `returns empty when drain increase is below threshold`() =
        runTest {
            val rule = degradationRule(comparisonReadings(previousDrop = 2, currentDrop = 2))

            assertTrue(rule.evaluate(NOW).isEmpty())
        }

    @Test
    fun `does not treat charging transition as battery drain`() =
        runTest {
            val currentReadings = windowReadings(CURRENT_WINDOW_START, startLevel = 50, dropPerSample = 0)
            val readings =
                windowReadings(PREVIOUS_WINDOW_START, startLevel = 100, dropPerSample = 1) +
                    batteryReading(
                        timestamp = CURRENT_WINDOW_START + 1,
                        level = 100,
                    ).copy(status = "CHARGING") +
                    currentReadings
            val rule = degradationRule(readings)

            assertTrue(rule.evaluate(NOW).isEmpty())
        }

    private fun comparisonReadings(
        previousDrop: Int,
        currentDrop: Int,
        currentCount: Int = 20,
    ) = windowReadings(PREVIOUS_WINDOW_START, 100, previousDrop) +
        windowReadings(CURRENT_WINDOW_START, 100, currentDrop, currentCount)

    private fun degradationRule(readings: List<BatteryReading>) =
        BatteryDegradationTrendRule(TestBatteryRepository(readings), BatteryDrainAnalyzer())

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
        private const val NOW = WINDOW_MS * 3
        private const val PREVIOUS_WINDOW_START = NOW - (WINDOW_MS * 2)
        private const val CURRENT_WINDOW_START = NOW - WINDOW_MS
    }
}
