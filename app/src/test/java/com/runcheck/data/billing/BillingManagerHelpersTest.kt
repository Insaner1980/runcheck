package com.runcheck.data.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.runcheck.R
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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

    @Test
    fun `billing message mapping handles known purchase sub response codes`() {
        assertEquals(
            R.string.billing_insufficient_funds,
            billingMessageResFor(
                responseCode = BillingClient.BillingResponseCode.ERROR,
                subResponseCode =
                    BillingClient.OnPurchasesUpdatedSubResponseCode
                        .PAYMENT_DECLINED_DUE_TO_INSUFFICIENT_FUNDS,
            ),
        )
        assertEquals(
            R.string.billing_user_ineligible,
            billingMessageResFor(
                responseCode = BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                subResponseCode = BillingClient.OnPurchasesUpdatedSubResponseCode.USER_INELIGIBLE,
            ),
        )
    }

    @Test
    fun `unknown purchase sub response code uses response code or generic fallback`() {
        assertEquals(
            R.string.billing_network_error,
            billingMessageResFor(
                responseCode = BillingClient.BillingResponseCode.NETWORK_ERROR,
                subResponseCode = Int.MAX_VALUE,
            ),
        )
        assertNull(
            billingMessageResFor(
                responseCode = BillingClient.BillingResponseCode.ERROR,
                subResponseCode = Int.MAX_VALUE,
            ),
        )
    }

    @Test
    fun `billing acknowledgement retries until success`() =
        runTest {
            var attempts = 0

            val acknowledged =
                retryBillingAcknowledgement(maxRetries = 3, retryBaseDelayMs = 0L) {
                    attempts++
                    billingResult(
                        if (attempts == 3) {
                            BillingClient.BillingResponseCode.OK
                        } else {
                            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE
                        },
                    )
                }

            assertTrue(acknowledged)
            assertEquals(3, attempts)
        }

    @Test
    fun `billing acknowledgement remains inactive after retries are exhausted`() =
        runTest {
            var attempts = 0

            val acknowledged =
                retryBillingAcknowledgement(maxRetries = 3, retryBaseDelayMs = 0L) {
                    attempts++
                    billingResult(BillingClient.BillingResponseCode.ERROR)
                }

            assertFalse(acknowledged)
            assertEquals(3, attempts)
        }
}

private fun billingResult(responseCode: Int): BillingResult =
    BillingResult
        .newBuilder()
        .setResponseCode(responseCode)
        .build()
