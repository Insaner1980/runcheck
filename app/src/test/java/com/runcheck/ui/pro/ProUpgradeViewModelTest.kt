package com.runcheck.ui.pro

import com.runcheck.billing.ProPurchaseManager
import com.runcheck.billing.ProPurchaseRefreshResult
import com.runcheck.billing.PurchaseEvent
import com.runcheck.pro.ProState
import com.runcheck.pro.ProStateProvider
import com.runcheck.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
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
}
