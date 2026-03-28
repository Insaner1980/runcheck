package com.runcheck.data.billing

import com.android.billingclient.api.BillingClient
import com.runcheck.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("DEPRECATION")
class BillingManagerHelpersTest {
    @Test
    fun `reconnectable response codes are marked reconnectable`() {
        val codes =
            listOf(
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponseCode.NETWORK_ERROR,
                BillingClient.BillingResponseCode.ERROR,
            )

        codes.forEach { code ->
            assertTrue(code in reconnectableBillingResponseCodes())
        }
    }

    @Test
    fun `deprecated service timeout is not treated as reconnectable`() {
        assertFalse(
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT in reconnectableBillingResponseCodes(),
        )
    }

    @Test
    fun `terminal non-ready response codes are not marked reconnectable`() {
        val codes =
            listOf(
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
                BillingClient.BillingResponseCode.DEVELOPER_ERROR,
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
                BillingClient.BillingResponseCode.ITEM_NOT_OWNED,
                BillingClient.BillingResponseCode.USER_CANCELED,
            )

        codes.forEach { code ->
            assertTrue(code in nonReadyBillingResponseCodes())
            assertFalse(code in reconnectableBillingResponseCodes())
        }
    }

    @Test
    fun `billing message mapping returns targeted strings for known cases`() {
        assertEquals(
            R.string.billing_network_error,
            billingMessageResFor(BillingClient.BillingResponseCode.NETWORK_ERROR),
        )
        assertEquals(
            R.string.billing_service_unavailable,
            billingMessageResFor(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE),
        )
        assertEquals(
            R.string.billing_service_unavailable,
            billingMessageResFor(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE),
        )
        assertEquals(
            R.string.billing_item_unavailable,
            billingMessageResFor(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE),
        )
    }

    @Test
    fun `billing message mapping falls back for generic codes`() {
        assertNull(billingMessageResFor(BillingClient.BillingResponseCode.OK))
        assertNull(billingMessageResFor(BillingClient.BillingResponseCode.DEVELOPER_ERROR))
        assertNull(billingMessageResFor(BillingClient.BillingResponseCode.ITEM_NOT_OWNED))
    }
}
