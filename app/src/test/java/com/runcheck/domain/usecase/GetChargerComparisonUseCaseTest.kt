package com.runcheck.domain.usecase

import com.runcheck.domain.model.ChargerProfile
import com.runcheck.domain.model.ChargerSummary
import com.runcheck.domain.model.ChargingSession
import com.runcheck.domain.repository.ChargerRepository
import com.runcheck.domain.repository.ProStatusProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetChargerComparisonUseCaseTest {

    private lateinit var useCase: GetChargerComparisonUseCase
    private lateinit var chargerRepository: ChargerRepository
    private lateinit var proStatusProvider: ProStatusProvider

    @Before
    fun setup() {
        chargerRepository = mockk()
        proStatusProvider = mockk()
    }

    private fun createUseCase(): GetChargerComparisonUseCase =
        GetChargerComparisonUseCase(chargerRepository, proStatusProvider)

    private fun charger(id: Long, name: String) = ChargerProfile(
        id = id,
        name = name,
        created = 1_000_000L
    )

    private fun session(
        id: Long = 0,
        chargerId: Long,
        startTime: Long,
        endTime: Long? = null,
        startLevel: Int = 20,
        endLevel: Int? = null,
        avgCurrentMa: Int? = null
    ) = ChargingSession(
        id = id,
        chargerId = chargerId,
        startTime = startTime,
        endTime = endTime,
        startLevel = startLevel,
        endLevel = endLevel,
        avgCurrentMa = avgCurrentMa,
        maxCurrentMa = null,
        avgVoltageMv = null,
        avgPowerMw = null,
        plugType = "USB"
    )

    @Test
    fun `non-Pro user returns empty flow`() = runTest {
        every { proStatusProvider.isPro() } returns false

        useCase = createUseCase()
        val result = useCase().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `Pro user gets charger data`() = runTest {
        every { proStatusProvider.isPro() } returns true
        every { chargerRepository.getChargerProfiles() } returns flowOf(
            listOf(charger(1L, "Wall Charger"))
        )
        every { chargerRepository.getAllSessions() } returns flowOf(
            listOf(
                session(
                    chargerId = 1L,
                    startTime = 1000L,
                    endTime = 3_600_000L + 1000L, // 1 hour
                    startLevel = 20,
                    endLevel = 80,
                    avgCurrentMa = 2000
                )
            )
        )

        useCase = createUseCase()
        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals("Wall Charger", result[0].chargerName)
        assertEquals(1, result[0].sessionCount)
        assertNotNull(result[0].avgChargingSpeedMa)
        assertEquals(2000, result[0].avgChargingSpeedMa)
    }

    @Test
    fun `two chargers with sessions are sorted by last usage descending`() = runTest {
        every { proStatusProvider.isPro() } returns true
        every { chargerRepository.getChargerProfiles() } returns flowOf(
            listOf(
                charger(1L, "Old Charger"),
                charger(2L, "New Charger")
            )
        )
        every { chargerRepository.getAllSessions() } returns flowOf(
            listOf(
                session(
                    chargerId = 1L,
                    startTime = 1_000_000L,
                    endTime = 4_600_000L,
                    startLevel = 30,
                    endLevel = 90,
                    avgCurrentMa = 1500
                ),
                session(
                    chargerId = 2L,
                    startTime = 5_000_000L,
                    endTime = 8_600_000L,
                    startLevel = 10,
                    endLevel = 85,
                    avgCurrentMa = 2500
                )
            )
        )

        useCase = createUseCase()
        val result = useCase().first()

        assertEquals(2, result.size)
        // New Charger was used more recently (startTime=5_000_000)
        assertEquals("New Charger", result[0].chargerName)
        assertEquals("Old Charger", result[1].chargerName)
    }

    @Test
    fun `avg charging speed calculated correctly from multiple sessions`() = runTest {
        every { proStatusProvider.isPro() } returns true
        every { chargerRepository.getChargerProfiles() } returns flowOf(
            listOf(charger(1L, "USB-C"))
        )
        every { chargerRepository.getAllSessions() } returns flowOf(
            listOf(
                session(
                    chargerId = 1L,
                    startTime = 1000L,
                    endTime = 3_601_000L,
                    startLevel = 20,
                    endLevel = 80,
                    avgCurrentMa = 2000
                ),
                session(
                    chargerId = 1L,
                    startTime = 5_000_000L,
                    endTime = 8_600_000L,
                    startLevel = 30,
                    endLevel = 90,
                    avgCurrentMa = 3000
                )
            )
        )

        useCase = createUseCase()
        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals(2, result[0].sessionCount)
        // Average of 2000 and 3000 = 2500
        assertEquals(2500, result[0].avgChargingSpeedMa)
    }

    @Test
    fun `session with zero percent level gain does not cause division by zero`() = runTest {
        every { proStatusProvider.isPro() } returns true
        every { chargerRepository.getChargerProfiles() } returns flowOf(
            listOf(charger(1L, "Bad Charger"))
        )
        every { chargerRepository.getAllSessions() } returns flowOf(
            listOf(
                session(
                    chargerId = 1L,
                    startTime = 1000L,
                    endTime = 3_601_000L,
                    startLevel = 50,
                    endLevel = 50, // 0% gain
                    avgCurrentMa = 100
                )
            )
        )

        useCase = createUseCase()
        val result = useCase().first()

        // Should not crash; avgTimeToFull should be null since no gain
        assertEquals(1, result.size)
        assertNull(result[0].avgTimeToFullMinutes)
    }

    @Test
    fun `no sessions for a charger shows zero session count`() = runTest {
        every { proStatusProvider.isPro() } returns true
        every { chargerRepository.getChargerProfiles() } returns flowOf(
            listOf(charger(1L, "Unused Charger"))
        )
        every { chargerRepository.getAllSessions() } returns flowOf(emptyList())

        useCase = createUseCase()
        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals(0, result[0].sessionCount)
        assertNull(result[0].avgChargingSpeedMa)
        assertNull(result[0].avgTimeToFullMinutes)
        assertNull(result[0].lastUsed)
    }

    @Test
    fun `incomplete sessions without endTime are not used for avg calculations`() = runTest {
        every { proStatusProvider.isPro() } returns true
        every { chargerRepository.getChargerProfiles() } returns flowOf(
            listOf(charger(1L, "Active Charger"))
        )
        every { chargerRepository.getAllSessions() } returns flowOf(
            listOf(
                session(
                    chargerId = 1L,
                    startTime = 1000L,
                    endTime = null, // still active
                    startLevel = 20,
                    endLevel = null,
                    avgCurrentMa = null
                )
            )
        )

        useCase = createUseCase()
        val result = useCase().first()

        assertEquals(1, result.size)
        // Session counts include incomplete ones
        assertEquals(1, result[0].sessionCount)
        // But speed/time averages are from completed sessions only
        assertNull(result[0].avgChargingSpeedMa)
        assertNull(result[0].avgTimeToFullMinutes)
        // lastUsed should still be set (based on all sessions)
        assertEquals(1000L, result[0].lastUsed)
    }

    @Test
    fun `avgTimeToFull calculated correctly`() = runTest {
        every { proStatusProvider.isPro() } returns true
        every { chargerRepository.getChargerProfiles() } returns flowOf(
            listOf(charger(1L, "Fast Charger"))
        )
        // Session: 60 minutes, 20% -> 80% = 60% gain
        // Time to full: 60min * 100 / 60 = 100 min
        val sixtyMinMs = 60 * 60_000L
        every { chargerRepository.getAllSessions() } returns flowOf(
            listOf(
                session(
                    chargerId = 1L,
                    startTime = 0L,
                    endTime = sixtyMinMs,
                    startLevel = 20,
                    endLevel = 80,
                    avgCurrentMa = 2000
                )
            )
        )

        useCase = createUseCase()
        val result = useCase().first()

        assertEquals(1, result.size)
        // durationMinutes = 60_000_000 / 60_000 = 60 (actually sixtyMinMs/60000 = 60)
        // Wait: endTime=3600000, startTime=0, duration = 3600000ms = 60min
        // levelGain = 80 - 20 = 60
        // avgTimeToFull = 60 * 100 / 60 = 100
        assertEquals(100, result[0].avgTimeToFullMinutes)
    }

    @Test
    fun `sessions with null avgCurrentMa are excluded from speed average`() = runTest {
        every { proStatusProvider.isPro() } returns true
        every { chargerRepository.getChargerProfiles() } returns flowOf(
            listOf(charger(1L, "Mixed"))
        )
        every { chargerRepository.getAllSessions() } returns flowOf(
            listOf(
                session(
                    chargerId = 1L,
                    startTime = 1000L,
                    endTime = 3_601_000L,
                    startLevel = 20,
                    endLevel = 80,
                    avgCurrentMa = 2000
                ),
                session(
                    chargerId = 1L,
                    startTime = 5_000_000L,
                    endTime = 8_600_000L,
                    startLevel = 30,
                    endLevel = 90,
                    avgCurrentMa = null // no data
                )
            )
        )

        useCase = createUseCase()
        val result = useCase().first()

        assertEquals(1, result.size)
        // Only the first session's 2000 mA contributes
        assertEquals(2000, result[0].avgChargingSpeedMa)
    }

    @Test
    fun `charger with no completed sessions but has active session`() = runTest {
        every { proStatusProvider.isPro() } returns true
        every { chargerRepository.getChargerProfiles() } returns flowOf(
            listOf(charger(1L, "New Charger"))
        )
        every { chargerRepository.getAllSessions() } returns flowOf(
            listOf(
                session(
                    chargerId = 1L,
                    startTime = 5_000_000L,
                    endTime = null,
                    startLevel = 10,
                    endLevel = null,
                    avgCurrentMa = null
                )
            )
        )

        useCase = createUseCase()
        val result = useCase().first()

        assertEquals(1, result.size)
        val summary = result[0]
        assertEquals(1, summary.sessionCount)
        assertNull(summary.avgChargingSpeedMa)
        assertNull(summary.avgTimeToFullMinutes)
        assertEquals(5_000_000L, summary.lastUsed)
    }
}
