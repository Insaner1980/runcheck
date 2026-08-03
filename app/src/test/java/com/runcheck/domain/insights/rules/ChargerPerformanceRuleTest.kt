package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.model.ChargerProfile
import com.runcheck.domain.model.ChargingSession
import com.runcheck.domain.repository.ChargerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargerPerformanceRuleTest {
    @Test
    fun `returns charger insight when one saved charger is much slower`() =
        runTest {
            val sessions =
                listOf(
                    session(1L, NOW - 12L * DAY_MS, 31_000),
                    session(1L, NOW - 8L * DAY_MS, 30_000),
                    session(2L, NOW - 6L * DAY_MS, 18_000),
                    session(2L, NOW - 2L * DAY_MS, 17_000),
                )

            val insights = evaluate(sessions)

            assertEquals(1, insights.size)
            val insight = insights.single()
            assertEquals(ChargerPerformanceRule.RULE_ID, insight.ruleId)
            assertEquals("charger:2:35plus", insight.dedupeKey)
            assertEquals("Desk Charger", insight.bodyArgs[0])
            assertEquals("43", insight.bodyArgs[1])
        }

    @Test
    fun `uses current and voltage samples and returns high priority for very slow charger`() =
        runTest {
            val sessions =
                listOf(
                    session(1L, NOW - 12L * DAY_MS, avgPowerMw = 31_000),
                    session(1L, NOW - 8L * DAY_MS, avgPowerMw = 30_000),
                    session(2L, NOW - 6L * DAY_MS, avgCurrentMa = 1_000, avgVoltageMv = 5_000),
                    session(2L, NOW - 2L * DAY_MS, avgCurrentMa = 900, avgVoltageMv = 5_000),
                )

            val insight = evaluate(sessions, secondChargerName = "Travel Charger").single()

            assertEquals("charger:2:50plus", insight.dedupeKey)
            assertEquals(InsightPriority.HIGH, insight.priority)
            assertEquals("Travel Charger", insight.bodyArgs[0])
            assertEquals("84", insight.bodyArgs[1])
        }

    @Test
    fun `returns medium priority for moderately slower charger`() =
        runTest {
            val sessions =
                listOf(
                    session(1L, NOW - 12L * DAY_MS, 30_000),
                    session(1L, NOW - 8L * DAY_MS, 30_000),
                    session(2L, NOW - 6L * DAY_MS, 23_000),
                    session(2L, NOW - 2L * DAY_MS, 22_000),
                )

            val insight = evaluate(sessions).single()

            assertEquals("charger:2:20plus", insight.dedupeKey)
            assertEquals(InsightPriority.MEDIUM, insight.priority)
            assertEquals("25", insight.bodyArgs[1])
        }

    @Test
    fun `returns empty when there are not enough saved chargers`() =
        runTest {
            val chargers =
                listOf(
                    ChargerProfile(id = 1L, name = "Only Charger", created = NOW - 30L * DAY_MS),
                )

            val rule = ChargerPerformanceRule(FakeChargerRepository(chargers, sessions = emptyList()))

            assertTrue(rule.evaluate(NOW).isEmpty())
        }

    @Test
    fun `returns empty when only one charger has enough samples`() =
        runTest {
            val sessions =
                listOf(
                    session(1L, NOW - 12L * DAY_MS, avgPowerMw = 31_000),
                    session(1L, NOW - 8L * DAY_MS, avgPowerMw = 30_000),
                    session(1L, NOW - 6L * DAY_MS, avgPowerMw = 29_000),
                    session(1L, NOW - 2L * DAY_MS, avgPowerMw = 28_000),
                )

            assertTrue(evaluate(sessions).isEmpty())
        }

    @Test
    fun `returns empty when charger speeds are similar`() =
        runTest {
            val sessions =
                listOf(
                    session(1L, NOW - 12L * DAY_MS, 30_000),
                    session(1L, NOW - 8L * DAY_MS, 29_000),
                    session(2L, NOW - 6L * DAY_MS, 28_000),
                    session(2L, NOW - 2L * DAY_MS, 27_000),
                )

            val insights = evaluate(sessions)

            assertTrue(insights.isEmpty())
        }

    @Test
    fun `returns empty when mixed charging modes make the observed power ranges overlap`() =
        runTest {
            val sessions =
                listOf(
                    session(1L, NOW - 12L * DAY_MS, 40_000),
                    session(1L, NOW - 8L * DAY_MS, 10_000),
                    session(2L, NOW - 6L * DAY_MS, 20_000),
                    session(2L, NOW - 2L * DAY_MS, 20_000),
                )

            assertTrue(
                evaluate(sessions, firstChargerName = "Mixed Mode", secondChargerName = "Stable Charger").isEmpty(),
            )
        }

    @Test
    fun `ignores non-positive power samples instead of dividing by zero`() =
        runTest {
            val sessions =
                listOf(
                    session(1L, NOW - 12L * DAY_MS, 30_000),
                    session(1L, NOW - 8L * DAY_MS, 30_000),
                    session(2L, NOW - 6L * DAY_MS, 0),
                    session(2L, NOW - 2L * DAY_MS, -1_000),
                )

            assertTrue(
                evaluate(
                    sessions,
                    firstChargerName = "Valid Charger",
                    secondChargerName = "Invalid Samples",
                ).isEmpty(),
            )
        }

    private suspend fun evaluate(
        sessions: List<ChargingSession>,
        firstChargerName: String = "Fast Brick",
        secondChargerName: String = "Desk Charger",
    ) = ChargerPerformanceRule(
        FakeChargerRepository(
            chargers =
                listOf(
                    ChargerProfile(1L, firstChargerName, NOW - 30L * DAY_MS),
                    ChargerProfile(2L, secondChargerName, NOW - 25L * DAY_MS),
                ),
            sessions = sessions,
        ),
    ).evaluate(NOW)

    private fun session(
        chargerId: Long,
        endTime: Long,
        avgPowerMw: Int? = null,
        avgCurrentMa: Int? = null,
        avgVoltageMv: Int? = null,
    ) = ChargingSession(
        chargerId = chargerId,
        startTime = endTime - (50L * 60L * 1000L),
        endTime = endTime,
        startLevel = 25,
        endLevel = 80,
        avgCurrentMa = avgCurrentMa,
        maxCurrentMa = null,
        avgVoltageMv = avgVoltageMv,
        avgPowerMw = avgPowerMw,
        plugType = "AC",
    )

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L
        const val NOW = 60L * DAY_MS
    }
}

private class FakeChargerRepository(
    private val chargers: List<ChargerProfile>,
    private val sessions: List<ChargingSession>,
) : ChargerRepository {
    override fun getChargerProfiles(): Flow<List<ChargerProfile>> = emptyFlow()

    override fun getAllSessions(): Flow<List<ChargingSession>> = emptyFlow()

    override suspend fun getChargerProfilesSync(): List<ChargerProfile> = chargers

    override suspend fun getAllSessionsSync(): List<ChargingSession> = sessions

    override suspend fun insertCharger(name: String): Long = 0L

    override suspend fun deleteChargerById(id: Long) = Unit

    override suspend fun insertSession(session: ChargingSession): Long = 0L

    override suspend fun completeSession(
        id: Long,
        endTime: Long,
        endLevel: Int,
        avgCurrentMa: Int?,
        maxCurrentMa: Int?,
        avgVoltageMv: Int?,
        avgPowerMw: Int?,
    ) = Unit

    override suspend fun getActiveSession(): ChargingSession? = null

    override suspend fun deleteSessionsOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}
