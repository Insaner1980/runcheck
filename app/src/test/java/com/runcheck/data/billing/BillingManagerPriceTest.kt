package com.runcheck.data.billing

import android.content.Context
import com.android.billingclient.api.ProductDetails
import com.runcheck.billing.ProPurchaseManager
import com.runcheck.util.AppDispatchers
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BillingManagerPriceTest {
    @Test
    fun `price follows product replacement and invalidation without separate price updates`() =
        runTest {
            val dispatchers = mockk<AppDispatchers>()
            every { dispatchers.main } returns StandardTestDispatcher(testScheduler)
            val manager =
                BillingManager(
                    context = mockk<Context>(relaxed = true),
                    proStatusCache = mockk(relaxed = true),
                    dispatchers = dispatchers,
                )
            val api: ProPurchaseManager = manager
            try {
                assertNull(api.getFormattedPrice())

                cacheProduct(manager, productWithPrice("€4.99"))
                assertEquals("€4.99", api.getFormattedPrice())

                cacheProduct(manager, productWithPrice("1 299,00 kr"))
                assertEquals("1 299,00 kr", api.getFormattedPrice())

                cacheProduct(manager, mockk { every { oneTimePurchaseOfferDetails } returns null })
                assertNull(api.getFormattedPrice())

                cacheProduct(manager, productWithPrice("€4.99"))
                assertEquals("€4.99", api.getFormattedPrice())
                cacheProduct(manager, null)
                assertNull(api.getFormattedPrice())

                cacheProduct(manager, productWithPrice("€4.99"))
                manager.destroy()
                assertNull(api.getFormattedPrice())
            } finally {
                manager.destroy()
            }
        }

    private fun productWithPrice(price: String): ProductDetails =
        mockk {
            every { oneTimePurchaseOfferDetails } returns
                mockk { every { formattedPrice } returns price }
        }

    private fun cacheProduct(
        manager: BillingManager,
        product: ProductDetails?,
    ) {
        // Seed only the canonical cache; assertions exercise the public purchase API.
        BillingManager::class.java.getDeclaredField("cachedProductDetails").apply {
            isAccessible = true
            set(manager, product)
        }
    }
}
