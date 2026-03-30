package com.runcheck.domain.insights.rules

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
            val dayMs = 24L * 60L * 60L * 1000L
            val now = 60L * dayMs
            val chargers =
                listOf(
                    ChargerProfile(id = 1L, name = "Fast Brick", created = now - 30L * dayMs),
                    ChargerProfile(id = 2L, name = "Desk Charger", created = now - 25L * dayMs),
                )
            val sessions =
                listOf(
                    session(1L, now - 12L * dayMs, 31_000),
                    session(1L, now - 8L * dayMs, 30_000),
                    session(2L, now - 6L * dayMs, 18_000),
                    session(2L, now - 2L * dayMs, 17_000),
                )

            val rule = ChargerPerformanceRule(FakeChargerRepository(chargers, sessions))

            val insights = rule.evaluate(now)

            assertEquals(1, insights.size)
            val insight = insights.single()
            assertEquals(ChargerPerformanceRule.RULE_ID, insight.ruleId)
            assertEquals("charger:2:35plus", insight.dedupeKey)
            assertEquals("Desk Charger", insight.bodyArgs[0])
            assertEquals("43", insight.bodyArgs[1])
        }

    @Test
    fun `returns empty when charger speeds are similar`() =
        runTest {
            val dayMs = 24L * 60L * 60L * 1000L
            val now = 60L * dayMs
            val chargers =
                listOf(
                    ChargerProfile(id = 1L, name = "Fast Brick", created = now - 30L * dayMs),
                    ChargerProfile(id = 2L, name = "Desk Charger", created = now - 25L * dayMs),
                )
            val sessions =
                listOf(
                    session(1L, now - 12L * dayMs, 30_000),
                    session(1L, now - 8L * dayMs, 29_000),
                    session(2L, now - 6L * dayMs, 28_000),
                    session(2L, now - 2L * dayMs, 27_000),
                )

            val rule = ChargerPerformanceRule(FakeChargerRepository(chargers, sessions))

            val insights = rule.evaluate(now)

            assertTrue(insights.isEmpty())
        }

    private fun session(
        chargerId: Long,
        endTime: Long,
        avgPowerMw: Int,
    ) = ChargingSession(
        chargerId = chargerId,
        startTime = endTime - (50L * 60L * 1000L),
        endTime = endTime,
        startLevel = 25,
        endLevel = 80,
        avgCurrentMa = null,
        maxCurrentMa = null,
        avgVoltageMv = null,
        avgPowerMw = avgPowerMw,
        plugType = "AC",
    )
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
}
