package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.model.InsightMessageId
import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.domain.repository.ThrottlingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringThermalThrottlingRuleTest {
    @Test
    fun `unknown identifiers do not contribute to recurrence or candidate values`() =
        runTest {
            val now = 14L * 24L * INSIGHT_TEST_HOUR_MS
            val valid = List(3) { index -> throttlingEvent(index.toLong(), now - index) }
            val unknown =
                listOf("UNKNOWN", "severe", "Severe", " SEVERE ").mapIndexed { index, status ->
                    throttlingEvent(10L + index, now).copy(
                        thermalStatus = status,
                        batteryTempC = 99f,
                        durationMs = 60L * 60L * 1000L,
                    )
                }
            val expected = RecurringThermalThrottlingRule(TestThrottlingRepository(valid)).evaluate(now).single()
            val actual =
                RecurringThermalThrottlingRule(TestThrottlingRepository(valid + unknown)).evaluate(now).single()

            assertEquals(expected, actual)
            assertEquals(0.6f, actual.confidence, 0f)
            assertTrue(
                RecurringThermalThrottlingRule(
                    TestThrottlingRepository(valid.take(2) + unknown),
                ).evaluate(now).isEmpty(),
            )
        }

    @Test
    fun `returns thermal insight when severe events recur`() =
        runTest {
            val dayMs = 24L * 60L * 60L * 1000L
            val now = 14L * dayMs
            val events =
                listOf(
                    ThrottlingEvent(
                        id = 1L,
                        timestamp = now - 6L * dayMs,
                        thermalStatus = "SEVERE",
                        batteryTempC = 43f,
                        cpuTempC = null,
                        foregroundApp = "camera",
                        durationMs =
                            4L * 60L * 1000L,
                    ),
                    ThrottlingEvent(
                        id = 2L,
                        timestamp = now - 4L * dayMs,
                        thermalStatus = "SEVERE",
                        batteryTempC = 44f,
                        cpuTempC = null,
                        foregroundApp = "maps",
                        durationMs =
                            5L * 60L * 1000L,
                    ),
                    ThrottlingEvent(
                        id = 3L,
                        timestamp = now - 2L * dayMs,
                        thermalStatus = "CRITICAL",
                        batteryTempC = 46f,
                        cpuTempC = null,
                        foregroundApp = "game",
                        durationMs =
                            8L * 60L * 1000L,
                    ),
                )
            val rule = RecurringThermalThrottlingRule(TestThrottlingRepository(events))

            val insights = rule.evaluate(now)

            assertEquals(1, insights.size)
            val insight = insights.single()
            assertEquals(RecurringThermalThrottlingRule.RULE_ID, insight.ruleId)
            assertEquals(InsightMessageId.RECURRING_THERMAL_THROTTLING, insight.messageId)
            assertEquals("critical:3plus", insight.dedupeKey)
            assertEquals("3", insight.bodyArgs[0])
            assertEquals("critical", insight.bodyArgs[1])
            assertEquals("46", insight.bodyArgs[2])
        }

    @Test
    fun `returns empty when only isolated or light events exist`() =
        runTest {
            val dayMs = 24L * 60L * 60L * 1000L
            val now = 14L * dayMs
            val events =
                listOf(
                    ThrottlingEvent(
                        id = 1L,
                        timestamp = now - 3L * dayMs,
                        thermalStatus = "LIGHT",
                        batteryTempC = 36f,
                        cpuTempC = null,
                        foregroundApp = null,
                        durationMs =
                            2L * 60L * 1000L,
                    ),
                    ThrottlingEvent(
                        id = 2L,
                        timestamp = now - 1L * dayMs,
                        thermalStatus = "SEVERE",
                        batteryTempC = 41f,
                        cpuTempC = null,
                        foregroundApp = null,
                        durationMs =
                            1L * 60L * 1000L,
                    ),
                )
            val rule = RecurringThermalThrottlingRule(TestThrottlingRepository(events))

            val insights = rule.evaluate(now)

            assertTrue(insights.isEmpty())
        }

    @Test
    fun `does not count future event toward recurrence minimum`() =
        runTest {
            val dayMs = 24L * 60L * 60L * 1000L
            val now = 14L * dayMs
            val events =
                listOf(
                    throttlingEvent(id = 1L, timestamp = now - 7L * dayMs),
                    throttlingEvent(id = 2L, timestamp = now - 1L * dayMs),
                    throttlingEvent(id = 3L, timestamp = now + 1L),
                )
            val rule = RecurringThermalThrottlingRule(TestThrottlingRepository(events))

            val insights = rule.evaluate(now)

            assertTrue(insights.isEmpty())
        }
}

private fun throttlingEvent(
    id: Long,
    timestamp: Long,
) = ThrottlingEvent(
    id = id,
    timestamp = timestamp,
    thermalStatus = "SEVERE",
    batteryTempC = 43f,
    cpuTempC = null,
    foregroundApp = null,
    durationMs = 60L * 1000L,
)
