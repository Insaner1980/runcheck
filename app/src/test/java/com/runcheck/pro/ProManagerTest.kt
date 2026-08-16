package com.runcheck.pro

import com.runcheck.billing.ProPurchaseManager
import com.runcheck.util.AppDispatchers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
    private val proPurchaseManager: ProPurchaseManager = mockk(relaxed = true)
    private val isProUserFlow = MutableStateFlow(false)

    private lateinit var proManager: ProManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { proPurchaseManager.isProUser } returns isProUserFlow
        coEvery { proPurchaseManager.awaitPurchaseStatusReady() } returns Unit
        proManager = ProManager(proPurchaseManager, AppDispatchers())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state before purchase status is ready is free`() {
        assertEquals(ProStatus.FREE, proManager.proState.value.status)
        assertFalse(proManager.proState.value.isPro)
        assertFalse(proManager.isProStatusReady)
    }

    @Test
    fun `fresh install without a purchase remains free`() =
        runTest(testDispatcher) {
            proManager.initialize()
            advanceUntilIdle()

            assertEquals(ProStatus.FREE, proManager.proState.value.status)
            assertFalse(proManager.isPro())
            assertTrue(proManager.isProStatusReady)
        }

    @Test
    fun `initialize waits for purchase status before marking access ready`() =
        runTest(testDispatcher) {
            val purchaseStatusReady = CompletableDeferred<Unit>()
            coEvery { proPurchaseManager.awaitPurchaseStatusReady() } coAnswers {
                purchaseStatusReady.await()
            }

            proManager.initialize()
            advanceUntilIdle()

            assertFalse(proManager.isProStatusReady)

            purchaseStatusReady.complete(Unit)
            advanceUntilIdle()

            assertTrue(proManager.isProStatusReady)
            assertEquals(ProStatus.FREE, proManager.proState.value.status)
        }

    @Test
    fun `verified purchase grants permanent pro access`() =
        runTest(testDispatcher) {
            isProUserFlow.value = true

            proManager.initialize()
            advanceUntilIdle()

            val state = proManager.proState.value
            assertEquals(ProStatus.PRO_PURCHASED, state.status)
            assertTrue(state.isPro)
            assertTrue(proManager.isProUser.first())
        }

    @Test
    fun `purchase status failure falls back to ready free access`() =
        runTest(testDispatcher) {
            coEvery { proPurchaseManager.awaitPurchaseStatusReady() } throws IllegalStateException("unavailable")

            proManager.initialize()
            advanceUntilIdle()

            assertTrue(proManager.isProStatusReady)
            assertEquals(ProStatus.FREE, proManager.proState.value.status)
        }

    @Test
    fun `purchase status changes update access in both directions`() =
        runTest(testDispatcher) {
            proManager.initialize()
            advanceUntilIdle()
            assertEquals(ProStatus.FREE, proManager.proState.value.status)

            isProUserFlow.value = true
            advanceUntilIdle()
            assertEquals(ProStatus.PRO_PURCHASED, proManager.proState.value.status)

            isProUserFlow.value = false
            advanceUntilIdle()
            assertEquals(ProStatus.FREE, proManager.proState.value.status)
            assertFalse(proManager.isPro())
        }

    @Test
    fun `feature access follows purchased pro status`() =
        runTest(testDispatcher) {
            proManager.initialize()
            advanceUntilIdle()
            assertFalse(proManager.hasFeature(ProFeature.EXTENDED_HISTORY))
            assertFalse(proManager.hasFeature(ProFeature.CSV_EXPORT))

            isProUserFlow.value = true
            advanceUntilIdle()
            assertTrue(proManager.hasFeature(ProFeature.EXTENDED_HISTORY))
            assertTrue(proManager.hasFeature(ProFeature.CSV_EXPORT))
        }

    @Test
    fun `initialize is idempotent`() =
        runTest(testDispatcher) {
            proManager.initialize()
            proManager.initialize()
            advanceUntilIdle()

            coVerify(exactly = 1) { proPurchaseManager.awaitPurchaseStatusReady() }

            isProUserFlow.value = true
            advanceUntilIdle()
            assertEquals(ProStatus.PRO_PURCHASED, proManager.proState.value.status)
        }
}
