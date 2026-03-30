package com.runcheck.domain.insights.rules

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
            val rule = RecurringThermalThrottlingRule(FakeThrottlingRepository(events))

            val insights = rule.evaluate(now)

            assertEquals(1, insights.size)
            val insight = insights.single()
            assertEquals(RecurringThermalThrottlingRule.RULE_ID, insight.ruleId)
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
            val rule = RecurringThermalThrottlingRule(FakeThrottlingRepository(events))

            val insights = rule.evaluate(now)

            assertTrue(insights.isEmpty())
        }
}

private class FakeThrottlingRepository(
    private val events: List<ThrottlingEvent>,
) : ThrottlingRepository {
    override fun getRecentEvents(limit: Int): Flow<List<ThrottlingEvent>> = emptyFlow()

    override suspend fun getEventsSinceSync(since: Long): List<ThrottlingEvent> =
        events.filter { it.timestamp >= since }

    override suspend fun insert(event: ThrottlingEvent): Long = 0L

    override suspend fun updateSnapshot(
        id: Long,
        thermalStatus: String,
        batteryTempC: Float,
        cpuTempC: Float?,
        foregroundApp: String?,
    ) = Unit

    override suspend fun updateDuration(
        id: Long,
        durationMs: Long,
    ) = Unit

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}
