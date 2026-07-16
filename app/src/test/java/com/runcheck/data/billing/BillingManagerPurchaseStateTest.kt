package com.runcheck.data.billing

import android.content.Context
import com.android.billingclient.api.Purchase
import com.runcheck.util.AppDispatchers
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingManagerPurchaseStateTest {
    @Test
    fun `pending purchase replaces stale purchased entitlement without unlocking pro`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val manager = createManager(dispatcher)
            val purchased = purchaseWithState(Purchase.PurchaseState.PURCHASED)
            val pending = purchaseWithState(Purchase.PurchaseState.PENDING)

            manager.syncPurchases(listOf(purchased), emitEvents = false)
            assertTrue(manager.isPro())

            manager.syncPurchases(listOf(pending), emitEvents = false)
            assertFalse(manager.isPro())

            manager.destroy()
        }

    @Test
    fun `pending purchase completes and later cancellation clears tracked state`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val manager = createManager(dispatcher)
            val pending = purchaseWithState(Purchase.PurchaseState.PENDING)
            val purchased = purchaseWithState(Purchase.PurchaseState.PURCHASED)

            manager.syncPurchases(listOf(pending), emitEvents = false)
            assertFalse(manager.isPro())
            assertTrue(manager.hasPendingPurchase.first())

            manager.syncPurchases(listOf(purchased), emitEvents = false)
            assertTrue(manager.isPro())
            assertFalse(manager.hasPendingPurchase.first())

            manager.syncPurchases(listOf(pending), emitEvents = false)
            manager.syncPurchases(emptyList(), emitEvents = false)
            assertFalse(manager.isPro())
            assertFalse(manager.hasPendingPurchase.first())

            manager.destroy()
        }

    @Test
    fun `purchase state transitions update the cached entitlement including revocations`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val proStatusCache = mockk<ProStatusCache>(relaxed = true)
            val manager = createManager(dispatcher, proStatusCache)
            val purchased = purchaseWithState(Purchase.PurchaseState.PURCHASED)
            val pending = purchaseWithState(Purchase.PurchaseState.PENDING)

            manager.syncPurchases(listOf(purchased), emitEvents = false)
            manager.syncPurchases(listOf(pending), emitEvents = false)
            manager.syncPurchases(listOf(purchased), emitEvents = false)
            manager.syncPurchases(emptyList(), emitEvents = false)

            verifySequence {
                proStatusCache.setCachedProStatus(true)
                proStatusCache.setCachedProStatus(false)
                proStatusCache.setCachedProStatus(true)
                proStatusCache.setCachedProStatus(false)
            }

            manager.destroy()
        }

    private fun createManager(
        dispatcher: CoroutineDispatcher,
        proStatusCache: ProStatusCache = mockk(relaxed = true),
    ): BillingManager {
        val dispatchers = mockk<AppDispatchers>()
        every { dispatchers.main } returns dispatcher
        return BillingManager(
            context = mockk<Context>(relaxed = true),
            proStatusCache = proStatusCache,
            dispatchers = dispatchers,
        )
    }

    private fun purchaseWithState(state: Int): Purchase =
        mockk {
            every { products } returns listOf(BillingManager.PRODUCT_ID_PRO)
            every { purchaseState } returns state
            every { isAcknowledged } returns true
        }
}
