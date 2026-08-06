package com.runcheck.data.billing

import com.runcheck.testutil.findRootDir
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class BillingManagerApiContractTest {
    private val billingManagerSource: String =
        findRootDir()
            .resolve("app/src/main/java/com/runcheck/data/billing/BillingManager.kt")
            .readText()

    @Test
    fun `BillingClient builder enables Play Billing auto service reconnection`() {
        assertTrue(
            "BillingClient builder should call enableAutoServiceReconnection() for disconnected-call handling",
            billingManagerSource.contains(".enableAutoServiceReconnection()"),
        )
    }

    @Test
    fun `auto reconnect is not raced by a manual disconnect reconnect`() {
        val disconnectCallback =
            Regex(
                "override fun onBillingServiceDisconnected\\(\\) \\{(?<body>.*?)\\n\\s*}",
                RegexOption.DOT_MATCHES_ALL,
            ).find(billingManagerSource)?.groups?.get("body")?.value.orEmpty()

        assertFalse(
            "Automatic service reconnection should own disconnect recovery without a competing delayed startConnection",
            disconnectCallback.contains("scheduleReconnect()"),
        )
    }

    @Test
    fun `initialization keeps one BillingClient while connection is in progress`() {
        assertTrue(
            "Repeated initialization should reuse the existing BillingClient instead of registering another purchase listener",
            billingManagerSource.contains("@Synchronized\n        fun initialize()") &&
                billingManagerSource.contains("if (billingClient != null)"),
        )
    }

    @Test
    fun `purchase refresh can trigger automatic service reconnection`() {
        val queryFunction =
            billingManagerSource
                .substringAfter("private suspend fun queryExistingPurchases()")
                .substringBefore("private suspend fun queryProductDetails()")

        assertFalse(
            "queryPurchasesAsync must be called while disconnected so BillingClient auto reconnection can run",
            queryFunction.contains("!client.isReady"),
        )
        assertTrue(
            "A refresh may defer while the initial BillingClient connection is still being established",
            queryFunction.contains("BillingClient.ConnectionState.CONNECTING"),
        )
        assertTrue(queryFunction.contains("client.queryPurchasesAsync(params)"))
    }

    @Test
    fun `only one fallback reconnect can be scheduled at a time`() {
        assertTrue(
            "Concurrent Billing failures should not queue competing BillingClient replacements",
            billingManagerSource.contains("@Synchronized\n        private fun scheduleReconnect()") &&
                billingManagerSource.contains("if (reconnectJob?.isActive == true) return"),
        )
    }

    @Test
    fun `production BillingManager does not suppress all Billing deprecations`() {
        assertFalse(
            "BillingManager should not hide future Billing API drift with a class-level DEPRECATION suppression",
            billingManagerSource.contains("@Suppress(\"DEPRECATION\")"),
        )
    }

    @Test
    fun `purchase flow errors preserve Billing 9 sub response details`() {
        assertTrue(
            "Both launch and purchase-update BillingResults must pass the Billing 9 sub response code to user-safe mapping",
            billingManagerSource.contains("result.onPurchasesUpdatedSubResponseCode") &&
                billingManagerSource.contains("billingResult.onPurchasesUpdatedSubResponseCode"),
        )
    }

    @Test
    fun `unacknowledged purchases do not unlock pro before acknowledgement succeeds`() {
        val syncPurchasesSource =
            billingManagerSource
                .substringAfter("internal suspend fun syncPurchases(")
                .substringBefore("private suspend fun acknowledgePurchaseWithRetry")

        assertTrue(syncPurchasesSource.contains("acknowledgementResults"))
        assertTrue(syncPurchasesSource.contains("acknowledgePurchaseWithRetry(purchase)"))
        assertTrue(
            "Pro state must be updated only inside the successful acknowledgement branch",
            syncPurchasesSource.indexOf("acknowledgementResults.any") <
                syncPurchasesSource.indexOf("updateProState(true)"),
        )
        assertTrue(syncPurchasesSource.contains("ProPurchaseRefreshResult.UNAVAILABLE"))
    }
}
