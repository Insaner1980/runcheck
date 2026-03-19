package com.runcheck.domain.usecase

import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.domain.repository.ThrottlingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrackThrottlingEventsUseCaseTest {

    private lateinit var useCase: TrackThrottlingEventsUseCase
    private lateinit var throttlingRepository: ThrottlingRepository
    private lateinit var foregroundAppProvider: TrackThrottlingEventsUseCase.ForegroundAppProvider

    @Before
    fun setup() {
        throttlingRepository = mockk(relaxed = true)
        foregroundAppProvider = mockk()

        coEvery { foregroundAppProvider.getCurrentForegroundApp() } returns "com.example.app"
        coEvery { throttlingRepository.insert(any()) } returns 1L

        useCase = TrackThrottlingEventsUseCase(throttlingRepository, foregroundAppProvider)
    }

    private fun thermalState(
        status: ThermalStatus,
        batteryTempC: Float = 40f,
        cpuTempC: Float? = 75f
    ) = ThermalState(
        batteryTempC = batteryTempC,
        cpuTempC = cpuTempC,
        thermalStatus = status,
        isThrottling = status >= ThermalStatus.SEVERE
    )

    @Test
    fun `SEVERE thermal state opens throttling event`() = runTest {
        val state = thermalState(ThermalStatus.SEVERE)

        useCase(state)

        val eventSlot = slot<ThrottlingEvent>()
        coVerify(exactly = 1) { throttlingRepository.insert(capture(eventSlot)) }
        val inserted = eventSlot.captured
        assertEquals("SEVERE", inserted.thermalStatus)
        assertEquals(40f, inserted.batteryTempC, 0.01f)
        assertEquals(75f, inserted.cpuTempC)
        assertEquals("com.example.app", inserted.foregroundApp)
        // Duration should be null for an open event
        assertEquals(null, inserted.durationMs)
    }

    @Test
    fun `NONE after SEVERE closes event with duration`() = runTest {
        // Open the event
        useCase(thermalState(ThermalStatus.SEVERE))

        // Close it
        useCase(thermalState(ThermalStatus.NONE))

        coVerify(exactly = 1) { throttlingRepository.insert(any()) }
        coVerify(exactly = 1) {
            throttlingRepository.updateDuration(
                id = 1L,
                durationMs = any()
            )
        }
    }

    @Test
    fun `duration is non-negative when closing event`() = runTest {
        useCase(thermalState(ThermalStatus.SEVERE))
        useCase(thermalState(ThermalStatus.NONE))

        val durationSlot = slot<Long>()
        coVerify {
            throttlingRepository.updateDuration(
                id = 1L,
                durationMs = capture(durationSlot)
            )
        }
        assertTrue(
            "Duration should be >= 0, got ${durationSlot.captured}",
            durationSlot.captured >= 0
        )
    }

    @Test
    fun `consecutive SEVERE calls do not create duplicate events`() = runTest {
        useCase(thermalState(ThermalStatus.SEVERE))
        useCase(thermalState(ThermalStatus.SEVERE))
        useCase(thermalState(ThermalStatus.SEVERE))

        // Only one insert, and no snapshot updates since status did not worsen
        coVerify(exactly = 1) { throttlingRepository.insert(any()) }
        coVerify(exactly = 0) { throttlingRepository.updateSnapshot(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `peak status updates when thermal worsens from SEVERE to CRITICAL`() = runTest {
        useCase(thermalState(ThermalStatus.SEVERE))
        useCase(thermalState(ThermalStatus.CRITICAL, batteryTempC = 45f, cpuTempC = 90f))

        coVerify(exactly = 1) { throttlingRepository.insert(any()) }
        coVerify(exactly = 1) {
            throttlingRepository.updateSnapshot(
                id = 1L,
                thermalStatus = "CRITICAL",
                batteryTempC = 45f,
                cpuTempC = 90f,
                foregroundApp = "com.example.app"
            )
        }
    }

    @Test
    fun `peak status updates through multiple escalations`() = runTest {
        useCase(thermalState(ThermalStatus.SEVERE))
        useCase(thermalState(ThermalStatus.CRITICAL))
        useCase(thermalState(ThermalStatus.EMERGENCY))

        coVerify(exactly = 1) { throttlingRepository.insert(any()) }
        // Two escalations: SEVERE->CRITICAL and CRITICAL->EMERGENCY
        coVerify(exactly = 2) {
            throttlingRepository.updateSnapshot(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `NONE when no active event is a no-op`() = runTest {
        useCase(thermalState(ThermalStatus.NONE))

        coVerify(exactly = 0) { throttlingRepository.insert(any()) }
        coVerify(exactly = 0) { throttlingRepository.updateDuration(any(), any()) }
        coVerify(exactly = 0) { throttlingRepository.updateSnapshot(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `LIGHT when no active event is a no-op`() = runTest {
        useCase(thermalState(ThermalStatus.LIGHT))

        coVerify(exactly = 0) { throttlingRepository.insert(any()) }
    }

    @Test
    fun `MODERATE when no active event is a no-op`() = runTest {
        useCase(thermalState(ThermalStatus.MODERATE))

        coVerify(exactly = 0) { throttlingRepository.insert(any()) }
    }

    @Test
    fun `downgrade from CRITICAL to SEVERE does not close or update`() = runTest {
        useCase(thermalState(ThermalStatus.SEVERE))
        useCase(thermalState(ThermalStatus.CRITICAL))
        // Go back to SEVERE (still >= threshold but not higher than peak CRITICAL)
        useCase(thermalState(ThermalStatus.SEVERE))

        coVerify(exactly = 1) { throttlingRepository.insert(any()) }
        // Only one snapshot update: SEVERE -> CRITICAL
        coVerify(exactly = 1) {
            throttlingRepository.updateSnapshot(any(), any(), any(), any(), any())
        }
        // No close because status is still >= SEVERE
        coVerify(exactly = 0) { throttlingRepository.updateDuration(any(), any()) }
    }

    @Test
    fun `can open new event after closing previous one`() = runTest {
        coEvery { throttlingRepository.insert(any()) } returnsMany listOf(1L, 2L)

        // First event
        useCase(thermalState(ThermalStatus.SEVERE))
        useCase(thermalState(ThermalStatus.NONE))

        // Second event
        useCase(thermalState(ThermalStatus.CRITICAL))
        useCase(thermalState(ThermalStatus.NONE))

        coVerify(exactly = 2) { throttlingRepository.insert(any()) }
        coVerify(exactly = 2) { throttlingRepository.updateDuration(any(), any()) }
    }

    @Test
    fun `EMERGENCY directly opens event`() = runTest {
        useCase(thermalState(ThermalStatus.EMERGENCY))

        val eventSlot = slot<ThrottlingEvent>()
        coVerify(exactly = 1) { throttlingRepository.insert(capture(eventSlot)) }
        assertEquals("EMERGENCY", eventSlot.captured.thermalStatus)
    }

    @Test
    fun `foreground app is captured on insert and update`() = runTest {
        coEvery { foregroundAppProvider.getCurrentForegroundApp() } returnsMany
            listOf("com.app.one", "com.app.two")

        useCase(thermalState(ThermalStatus.SEVERE))
        useCase(thermalState(ThermalStatus.CRITICAL))

        val insertSlot = slot<ThrottlingEvent>()
        coVerify { throttlingRepository.insert(capture(insertSlot)) }
        assertEquals("com.app.one", insertSlot.captured.foregroundApp)

        coVerify {
            throttlingRepository.updateSnapshot(
                id = any(),
                thermalStatus = any(),
                batteryTempC = any(),
                cpuTempC = any(),
                foregroundApp = "com.app.two"
            )
        }
    }

    @Test
    fun `null foreground app is handled gracefully`() = runTest {
        coEvery { foregroundAppProvider.getCurrentForegroundApp() } returns null

        useCase(thermalState(ThermalStatus.SEVERE))

        val eventSlot = slot<ThrottlingEvent>()
        coVerify { throttlingRepository.insert(capture(eventSlot)) }
        assertEquals(null, eventSlot.captured.foregroundApp)
    }
}
