package com.runcheck.ui.pro

import com.runcheck.billing.ProPurchaseManager
import com.runcheck.billing.ProPurchaseRefreshResult
import com.runcheck.billing.PurchaseEvent
import com.runcheck.pro.ProState
import com.runcheck.pro.ProStateProvider
import com.runcheck.pro.ProStatus
import com.runcheck.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProUpgradeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `pending UI state follows tracked purchase and clears after cancellation`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val hasPendingPurchase = MutableStateFlow(false)
            val purchaseManager = mockk<ProPurchaseManager>()
            every { purchaseManager.billingAvailable } returns MutableStateFlow(true)
            every { purchaseManager.hasPendingPurchase } returns hasPendingPurchase
            every { purchaseManager.purchaseEvents } returns MutableSharedFlow<PurchaseEvent>()
            coEvery { purchaseManager.getFormattedPrice() } returns null
            coEvery { purchaseManager.refreshPurchaseStatus() } returns ProPurchaseRefreshResult.NOT_ACTIVE
            val proStateProvider = mockk<ProStateProvider>()
            every { proStateProvider.proState } returns MutableStateFlow(ProState())
            val viewModel = ProUpgradeViewModel(proStateProvider, purchaseManager)
            runCurrent()

            hasPendingPurchase.value = true
            runCurrent()
            assertTrue(viewModel.uiState.value.purchasePending)

            hasPendingPurchase.value = false
            runCurrent()
            assertFalse(viewModel.uiState.value.purchasePending)
        }

    @Test
    fun `price loads when billing becomes available after view model creation`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val billingAvailable = MutableStateFlow(false)
            val purchaseManager = mockk<ProPurchaseManager>()
            every { purchaseManager.billingAvailable } returns billingAvailable
            every { purchaseManager.hasPendingPurchase } returns MutableStateFlow(false)
            every { purchaseManager.purchaseEvents } returns MutableSharedFlow<PurchaseEvent>()
            coEvery { purchaseManager.getFormattedPrice() } returns "€4.99"
            val proStateProvider = mockk<ProStateProvider>()
            every { proStateProvider.proState } returns MutableStateFlow(ProState())
            val viewModel = ProUpgradeViewModel(proStateProvider, purchaseManager)
            runCurrent()

            assertNull(viewModel.uiState.value.formattedPrice)

            billingAvailable.value = true
            runCurrent()

            assertEquals("€4.99", viewModel.uiState.value.formattedPrice)
        }

    @Test
    fun `restored pro state does not show purchase thank you`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val proState = MutableStateFlow(ProState(status = ProStatus.PRO_PURCHASED))
            val purchaseManager = mockk<ProPurchaseManager>()
            every { purchaseManager.billingAvailable } returns MutableStateFlow(false)
            every { purchaseManager.hasPendingPurchase } returns MutableStateFlow(false)
            every { purchaseManager.purchaseEvents } returns MutableSharedFlow<PurchaseEvent>()
            val proStateProvider = mockk<ProStateProvider>()
            every { proStateProvider.proState } returns proState
            val viewModel = ProUpgradeViewModel(proStateProvider, purchaseManager)
            runCurrent()

            assertTrue(viewModel.uiState.value.proState.isPro)
            assertFalse(viewModel.uiState.value.purchaseCompleted)
        }

    @Test
    fun `successful purchase shows dismissible thank you`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val (viewModel, purchaseEvents) = createPurchaseEventViewModel()
            runCurrent()

            purchaseEvents.emit(PurchaseEvent.Success)
            runCurrent()

            assertTrue(viewModel.uiState.value.purchaseCompleted)

            viewModel.dismissThankYou()

            assertFalse(viewModel.uiState.value.purchaseCompleted)
        }

    @Test
    fun `canceled purchase clears pending state and error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val (viewModel, purchaseEvents) = createPurchaseEventViewModel()
            runCurrent()

            purchaseEvents.emit(PurchaseEvent.Pending)
            purchaseEvents.emit(PurchaseEvent.Error("Canceled purchase"))
            runCurrent()
            assertTrue(viewModel.uiState.value.purchasePending)

            purchaseEvents.emit(PurchaseEvent.Canceled)
            runCurrent()

            assertFalse(viewModel.uiState.value.purchasePending)
            assertNull(viewModel.uiState.value.purchaseError)
        }

    private fun createPurchaseEventViewModel(): Pair<ProUpgradeViewModel, MutableSharedFlow<PurchaseEvent>> {
        val purchaseEvents = MutableSharedFlow<PurchaseEvent>()
        val purchaseManager = mockk<ProPurchaseManager>()
        every { purchaseManager.billingAvailable } returns MutableStateFlow(false)
        every { purchaseManager.hasPendingPurchase } returns MutableStateFlow(false)
        every { purchaseManager.purchaseEvents } returns purchaseEvents
        val proStateProvider = mockk<ProStateProvider>()
        every { proStateProvider.proState } returns MutableStateFlow(ProState())
        return ProUpgradeViewModel(proStateProvider, purchaseManager) to purchaseEvents
    }
}
