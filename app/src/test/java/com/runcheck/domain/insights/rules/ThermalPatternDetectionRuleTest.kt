package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.model.InsightPriority
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalPatternDetectionRuleTest {
    @Test
    fun `returns thermal pattern insight when heat stays elevated`() =
        runTest {
            val readings =
                listOf(
                    thermalReading(NOW - 42L * INSIGHT_TEST_HOUR_MS, 34.0f, 1),
                    thermalReading(NOW - 36L * INSIGHT_TEST_HOUR_MS, 33.5f, 0),
                    thermalReading(NOW - 30L * INSIGHT_TEST_HOUR_MS, 40.2f, 2),
                    thermalReading(NOW - 24L * INSIGHT_TEST_HOUR_MS, 41.0f, 3),
                    thermalReading(NOW - 18L * INSIGHT_TEST_HOUR_MS, 42.5f, 4),
                    thermalReading(NOW - 12L * INSIGHT_TEST_HOUR_MS, 43.0f, 4),
                    thermalReading(NOW - 6L * INSIGHT_TEST_HOUR_MS, 42.2f, 3),
                    thermalReading(NOW, 41.8f, 3),
                )

            val rule = ThermalPatternDetectionRule(TestThermalRepository(readings))

            val insights = rule.evaluate(NOW)

            assertEquals(1, insights.size)
            val insight = insights.single()
            assertEquals(ThermalPatternDetectionRule.RULE_ID, insight.ruleId)
            assertEquals("hot_pattern:70plus", insight.dedupeKey)
            assertEquals(InsightPriority.HIGH, insight.priority)
            assertEquals("75", insight.bodyArgs[0])
            assertEquals("42", insight.bodyArgs[1])
        }

    @Test
    fun `returns empty when hot readings are isolated`() =
        runTest {
            val readings =
                listOf(
                    thermalReading(NOW - 42L * INSIGHT_TEST_HOUR_MS, 34.0f, 1),
                    thermalReading(NOW - 36L * INSIGHT_TEST_HOUR_MS, 33.5f, 0),
                    thermalReading(NOW - 30L * INSIGHT_TEST_HOUR_MS, 35.2f, 1),
                    thermalReading(NOW - 24L * INSIGHT_TEST_HOUR_MS, 36.0f, 1),
                    thermalReading(NOW - 18L * INSIGHT_TEST_HOUR_MS, 39.0f, 1),
                    thermalReading(NOW - 12L * INSIGHT_TEST_HOUR_MS, 40.5f, 2),
                )

            val rule = ThermalPatternDetectionRule(TestThermalRepository(readings))

            val insights = rule.evaluate(NOW)

            assertTrue(insights.isEmpty())
        }

    @Test
    fun `returns medium priority insight when heat ratio is moderate and peak stays below high threshold`() =
        runTest {
            val readings = moderateReadings()

            val rule = ThermalPatternDetectionRule(TestThermalRepository(readings))

            val insight = rule.evaluate(NOW).single()

            assertEquals("hot_pattern:60plus", insight.dedupeKey)
            assertEquals(InsightPriority.MEDIUM, insight.priority)
            assertEquals("67", insight.bodyArgs[0])
            assertEquals("40", insight.bodyArgs[1])
        }

    @Test
    fun `returns empty when thermal history is too short`() =
        runTest {
            val readings =
                listOf(
                    thermalReading(NOW - 12L * INSIGHT_TEST_HOUR_MS, 40.5f, 2),
                    thermalReading(NOW - 6L * INSIGHT_TEST_HOUR_MS, 41.0f, 3),
                    thermalReading(NOW, 41.5f, 3),
                )

            val rule = ThermalPatternDetectionRule(TestThermalRepository(readings))

            assertTrue(rule.evaluate(NOW).isEmpty())
        }

    @Test
    fun `does not count future reading toward thermal pattern minimum`() =
        runTest {
            val readings = moderateReadings(lastTimestamp = NOW + 1L)
            val rule = ThermalPatternDetectionRule(TestThermalRepository(readings))

            val insights = rule.evaluate(NOW)

            assertTrue(insights.isEmpty())
        }

    private fun moderateReadings(lastTimestamp: Long = NOW) =
        listOf(
            thermalReading(NOW - 30L * INSIGHT_TEST_HOUR_MS, 34.0f, 1),
            thermalReading(NOW - 24L * INSIGHT_TEST_HOUR_MS, 35.0f, 1),
            thermalReading(NOW - 18L * INSIGHT_TEST_HOUR_MS, 39.6f, 2),
            thermalReading(NOW - 12L * INSIGHT_TEST_HOUR_MS, 39.8f, 2),
            thermalReading(NOW - 6L * INSIGHT_TEST_HOUR_MS, 40.0f, 2),
            thermalReading(lastTimestamp, 40.2f, 2),
        )

    private companion object {
        const val NOW = 100L * INSIGHT_TEST_HOUR_MS
    }
}
