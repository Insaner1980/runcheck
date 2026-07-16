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
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val readings =
                listOf(
                    thermalReading(now - 42L * INSIGHT_TEST_HOUR_MS, 34.0f, 1),
                    thermalReading(now - 36L * INSIGHT_TEST_HOUR_MS, 33.5f, 0),
                    thermalReading(now - 30L * INSIGHT_TEST_HOUR_MS, 40.2f, 2),
                    thermalReading(now - 24L * INSIGHT_TEST_HOUR_MS, 41.0f, 3),
                    thermalReading(now - 18L * INSIGHT_TEST_HOUR_MS, 42.5f, 4),
                    thermalReading(now - 12L * INSIGHT_TEST_HOUR_MS, 43.0f, 4),
                    thermalReading(now - 6L * INSIGHT_TEST_HOUR_MS, 42.2f, 3),
                    thermalReading(now, 41.8f, 3),
                )

            val rule = ThermalPatternDetectionRule(TestThermalRepository(readings))

            val insights = rule.evaluate(now)

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
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val readings =
                listOf(
                    thermalReading(now - 42L * INSIGHT_TEST_HOUR_MS, 34.0f, 1),
                    thermalReading(now - 36L * INSIGHT_TEST_HOUR_MS, 33.5f, 0),
                    thermalReading(now - 30L * INSIGHT_TEST_HOUR_MS, 35.2f, 1),
                    thermalReading(now - 24L * INSIGHT_TEST_HOUR_MS, 36.0f, 1),
                    thermalReading(now - 18L * INSIGHT_TEST_HOUR_MS, 39.0f, 1),
                    thermalReading(now - 12L * INSIGHT_TEST_HOUR_MS, 40.5f, 2),
                )

            val rule = ThermalPatternDetectionRule(TestThermalRepository(readings))

            val insights = rule.evaluate(now)

            assertTrue(insights.isEmpty())
        }

    @Test
    fun `returns medium priority insight when heat ratio is moderate and peak stays below high threshold`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val readings =
                listOf(
                    thermalReading(now - 30L * INSIGHT_TEST_HOUR_MS, 34.0f, 1),
                    thermalReading(now - 24L * INSIGHT_TEST_HOUR_MS, 35.0f, 1),
                    thermalReading(now - 18L * INSIGHT_TEST_HOUR_MS, 39.6f, 2),
                    thermalReading(now - 12L * INSIGHT_TEST_HOUR_MS, 39.8f, 2),
                    thermalReading(now - 6L * INSIGHT_TEST_HOUR_MS, 40.0f, 2),
                    thermalReading(now, 40.2f, 2),
                )

            val rule = ThermalPatternDetectionRule(TestThermalRepository(readings))

            val insight = rule.evaluate(now).single()

            assertEquals("hot_pattern:60plus", insight.dedupeKey)
            assertEquals(InsightPriority.MEDIUM, insight.priority)
            assertEquals("67", insight.bodyArgs[0])
            assertEquals("40", insight.bodyArgs[1])
        }

    @Test
    fun `returns empty when thermal history is too short`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val readings =
                listOf(
                    thermalReading(now - 12L * INSIGHT_TEST_HOUR_MS, 40.5f, 2),
                    thermalReading(now - 6L * INSIGHT_TEST_HOUR_MS, 41.0f, 3),
                    thermalReading(now, 41.5f, 3),
                )

            val rule = ThermalPatternDetectionRule(TestThermalRepository(readings))

            assertTrue(rule.evaluate(now).isEmpty())
        }

    @Test
    fun `does not count future reading toward thermal pattern minimum`() =
        runTest {
            val now = 100L * INSIGHT_TEST_HOUR_MS
            val readings =
                listOf(
                    thermalReading(now - 30L * INSIGHT_TEST_HOUR_MS, 34.0f, 1),
                    thermalReading(now - 24L * INSIGHT_TEST_HOUR_MS, 35.0f, 1),
                    thermalReading(now - 18L * INSIGHT_TEST_HOUR_MS, 39.6f, 2),
                    thermalReading(now - 12L * INSIGHT_TEST_HOUR_MS, 39.8f, 2),
                    thermalReading(now - 6L * INSIGHT_TEST_HOUR_MS, 40.0f, 2),
                    thermalReading(now + 1L, 40.2f, 2),
                )
            val rule = ThermalPatternDetectionRule(TestThermalRepository(readings))

            val insights = rule.evaluate(now)

            assertTrue(insights.isEmpty())
        }
}
