package com.runcheck.pro

import com.runcheck.billing.ProPurchaseManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProManagerTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var trialManager: TrialManager
    private lateinit var proPurchaseManager: ProPurchaseManager
    private lateinit var proManager: ProManager

    private val trialStateFlow = MutableStateFlow(TrialState())
    private val isProUserFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        trialManager = mockk(relaxed = true)
        proPurchaseManager = mockk(relaxed = true)

        every { trialManager.trialState } returns trialStateFlow
        every { proPurchaseManager.isProUser } returns isProUserFlow

        proManager = ProManager(trialManager, proPurchaseManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state before initialize is default ProState`() {
        val state = proManager.proState.value
        assertEquals(ProStatus.TRIAL_EXPIRED, state.status)
        assertEquals(0, state.trialDaysRemaining)
        assertFalse(state.isPro)
    }

    @Test
    fun `fresh install with no trial and no purchase emits TRIAL_EXPIRED`() =
        runTest(testDispatcher) {
            // Simulate fresh install: trial not active, no purchase
            coEvery { trialManager.initialize() } returns true
            trialStateFlow.value =
                TrialState(
                    isActive = false,
                    daysRemaining = 0,
                    startTimestamp = 0L,
                    isFirstLaunch = true,
                )
            isProUserFlow.value = false

            proManager.initialize()
            advanceUntilIdle()

            val state = proManager.proState.value
            assertEquals(ProStatus.TRIAL_EXPIRED, state.status)
            assertEquals(0, state.trialDaysRemaining)
            assertFalse(state.isPro)
        }

    @Test
    fun `trial started and within period emits TRIAL_ACTIVE`() =
        runTest(testDispatcher) {
            val trialStart = System.currentTimeMillis()
            coEvery { trialManager.initialize() } returns true
            trialStateFlow.value =
                TrialState(
                    isActive = true,
                    daysRemaining = 5,
                    startTimestamp = trialStart,
                    isFirstLaunch = false,
                )
            isProUserFlow.value = false

            proManager.initialize()
            advanceUntilIdle()

            val state = proManager.proState.value
            assertEquals(ProStatus.TRIAL_ACTIVE, state.status)
            assertEquals(5, state.trialDaysRemaining)
            assertEquals(trialStart, state.trialStartTimestamp)
            assertTrue(state.isPro)
        }

    @Test
    fun `trial expired emits TRIAL_EXPIRED`() =
        runTest(testDispatcher) {
            val trialStart = System.currentTimeMillis() - 8 * 24 * 60 * 60 * 1000L // 8 days ago
            coEvery { trialManager.initialize() } returns false
            trialStateFlow.value =
                TrialState(
                    isActive = false,
                    daysRemaining = 0,
                    startTimestamp = trialStart,
                    isFirstLaunch = false,
                )
            isProUserFlow.value = false

            proManager.initialize()
            advanceUntilIdle()

            val state = proManager.proState.value
            assertEquals(ProStatus.TRIAL_EXPIRED, state.status)
            assertEquals(0, state.trialDaysRemaining)
            assertEquals(trialStart, state.trialStartTimestamp)
            assertFalse(state.isPro)
        }

    @Test
    fun `already purchased emits PRO_PURCHASED`() =
        runTest(testDispatcher) {
            val trialStart = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L // 30 days ago
            coEvery { trialManager.initialize() } returns false
            trialStateFlow.value =
                TrialState(
                    isActive = false,
                    daysRemaining = 0,
                    startTimestamp = trialStart,
                    isFirstLaunch = false,
                )
            isProUserFlow.value = true

            proManager.initialize()
            advanceUntilIdle()

            val state = proManager.proState.value
            assertEquals(ProStatus.PRO_PURCHASED, state.status)
            assertEquals(0, state.trialDaysRemaining)
            assertTrue(state.isPro)
        }

    @Test
    fun `purchase during active trial transitions to PRO_PURCHASED`() =
        runTest(testDispatcher) {
            val trialStart = System.currentTimeMillis()
            coEvery { trialManager.initialize() } returns true
            trialStateFlow.value =
                TrialState(
                    isActive = true,
                    daysRemaining = 5,
                    startTimestamp = trialStart,
                    isFirstLaunch = false,
                )
            isProUserFlow.value = false

            proManager.initialize()
            advanceUntilIdle()

            // Verify trial is active first
            assertEquals(ProStatus.TRIAL_ACTIVE, proManager.proState.value.status)
            assertTrue(proManager.isPro())

            // Simulate purchase
            isProUserFlow.value = true
            advanceUntilIdle()

            val state = proManager.proState.value
            assertEquals(ProStatus.PRO_PURCHASED, state.status)
            assertTrue(state.isPro)
            assertTrue(proManager.isPro())
        }

    @Test
    fun `isPro returns true for TRIAL_ACTIVE`() =
        runTest(testDispatcher) {
            coEvery { trialManager.initialize() } returns true
            trialStateFlow.value =
                TrialState(
                    isActive = true,
                    daysRemaining = 3,
                    startTimestamp = System.currentTimeMillis(),
                )
            isProUserFlow.value = false

            proManager.initialize()
            advanceUntilIdle()

            assertTrue(proManager.isPro())
        }

    @Test
    fun `isProUser flow emits true for active trial users`() =
        runTest(testDispatcher) {
            coEvery { trialManager.initialize() } returns true
            trialStateFlow.value =
                TrialState(
                    isActive = true,
                    daysRemaining = 4,
                    startTimestamp = System.currentTimeMillis(),
                )
            isProUserFlow.value = false

            proManager.initialize()
            advanceUntilIdle()

            assertTrue(proManager.isProUser.first())
        }

    @Test
    fun `isPro returns false for TRIAL_EXPIRED`() =
        runTest(testDispatcher) {
            coEvery { trialManager.initialize() } returns false
            trialStateFlow.value =
                TrialState(
                    isActive = false,
                    daysRemaining = 0,
                    startTimestamp = System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000L,
                )
            isProUserFlow.value = false

            proManager.initialize()
            advanceUntilIdle()

            assertFalse(proManager.isPro())
        }

    @Test
    fun `isPro returns true for PRO_PURCHASED`() =
        runTest(testDispatcher) {
            coEvery { trialManager.initialize() } returns false
            trialStateFlow.value =
                TrialState(
                    isActive = false,
                    daysRemaining = 0,
                    startTimestamp = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L,
                )
            isProUserFlow.value = true

            proManager.initialize()
            advanceUntilIdle()

            assertTrue(proManager.isPro())
        }

    @Test
    fun `hasFeature returns true when pro is active`() =
        runTest(testDispatcher) {
            coEvery { trialManager.initialize() } returns false
            trialStateFlow.value =
                TrialState(
                    isActive = false,
                    daysRemaining = 0,
                    startTimestamp = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L,
                )
            isProUserFlow.value = true

            proManager.initialize()
            advanceUntilIdle()

            assertTrue(proManager.hasFeature(ProFeature.EXTENDED_HISTORY))
            assertTrue(proManager.hasFeature(ProFeature.CSV_EXPORT))
            assertTrue(proManager.hasFeature(ProFeature.WIDGETS))
        }

    @Test
    fun `hasFeature returns false when trial expired and not purchased`() =
        runTest(testDispatcher) {
            coEvery { trialManager.initialize() } returns false
            trialStateFlow.value =
                TrialState(
                    isActive = false,
                    daysRemaining = 0,
                    startTimestamp = System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000L,
                )
            isProUserFlow.value = false

            proManager.initialize()
            advanceUntilIdle()

            assertFalse(proManager.hasFeature(ProFeature.EXTENDED_HISTORY))
            assertFalse(proManager.hasFeature(ProFeature.CSV_EXPORT))
            assertFalse(proManager.hasFeature(ProFeature.WIDGETS))
        }

    @Test
    fun `initialize is idempotent and only runs once`() =
        runTest(testDispatcher) {
            coEvery { trialManager.initialize() } returns true
            trialStateFlow.value =
                TrialState(
                    isActive = true,
                    daysRemaining = 7,
                    startTimestamp = System.currentTimeMillis(),
                )
            isProUserFlow.value = false

            proManager.initialize()
            advanceUntilIdle()

            assertEquals(ProStatus.TRIAL_ACTIVE, proManager.proState.value.status)

            // Change flows — but calling initialize() again should not re-subscribe
            // because ProManager guards with `initialized` flag.
            // The existing combine subscription still processes new flow values though.
            proManager.initialize() // Should be a no-op (guard flag)
            advanceUntilIdle()

            // The combine is still active from the first initialize, so it processes updates
            isProUserFlow.value = true
            advanceUntilIdle()

            assertEquals(ProStatus.PRO_PURCHASED, proManager.proState.value.status)
        }

    @Test
    fun `purchase takes priority over active trial`() =
        runTest(testDispatcher) {
            val trialStart = System.currentTimeMillis()
            coEvery { trialManager.initialize() } returns true
            // Trial is active AND user has purchased — purchase should win
            trialStateFlow.value =
                TrialState(
                    isActive = true,
                    daysRemaining = 5,
                    startTimestamp = trialStart,
                )
            isProUserFlow.value = true

            proManager.initialize()
            advanceUntilIdle()

            val state = proManager.proState.value
            assertEquals(
                "Purchase should take priority over active trial",
                ProStatus.PRO_PURCHASED,
                state.status,
            )
            assertTrue(state.isPro)
        }

    @Test
    fun `proState flow emits updates when trial state changes`() =
        runTest(testDispatcher) {
            val trialStart = System.currentTimeMillis()
            coEvery { trialManager.initialize() } returns true
            trialStateFlow.value =
                TrialState(
                    isActive = true,
                    daysRemaining = 5,
                    startTimestamp = trialStart,
                )
            isProUserFlow.value = false

            proManager.initialize()
            advanceUntilIdle()

            assertEquals(ProStatus.TRIAL_ACTIVE, proManager.proState.value.status)
            assertEquals(5, proManager.proState.value.trialDaysRemaining)

            // Trial expires
            trialStateFlow.value =
                TrialState(
                    isActive = false,
                    daysRemaining = 0,
                    startTimestamp = trialStart,
                )
            advanceUntilIdle()

            assertEquals(ProStatus.TRIAL_EXPIRED, proManager.proState.value.status)
            assertEquals(0, proManager.proState.value.trialDaysRemaining)
            assertFalse(proManager.isPro())
        }
}
