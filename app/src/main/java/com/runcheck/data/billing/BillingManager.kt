package com.runcheck.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.runcheck.BuildConfig
import com.runcheck.R
import com.runcheck.billing.ProPurchaseManager
import com.runcheck.billing.ProPurchaseRefreshResult
import com.runcheck.billing.ProPurchaseStatusRefresher
import com.runcheck.billing.PurchaseEvent
import com.runcheck.util.AppDispatchers
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages the Google Play Billing lifecycle: connection, purchase flow,
 * acknowledgement, and pro status state. This is a lifecycle-aware service,
 * not a data repository — it must be explicitly initialized and destroyed.
 */
@Singleton
class BillingManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val proStatusCache: ProStatusCache,
        private val dispatchers: AppDispatchers,
    ) : PurchasesUpdatedListener,
        com.runcheck.domain.repository.ProStatusProvider,
        ProPurchaseManager,
        ProPurchaseStatusRefresher {
        private val scopeExceptionHandler =
            CoroutineExceptionHandler { _, throwable ->
                ReleaseSafeLog.error(TAG, "Billing coroutine failed", throwable)
            }
        private val scopeJob = SupervisorJob()
        private val scope = CoroutineScope(scopeJob + dispatchers.main + scopeExceptionHandler)

        private val _isProUser = MutableStateFlow(false)
        override val isProUser: Flow<Boolean> = _isProUser.asStateFlow()
        override val isProStatusReady: Boolean
            get() = initComplete.isCompleted
        private val _billingAvailable = MutableStateFlow(false)
        override val billingAvailable: Flow<Boolean> = _billingAvailable.asStateFlow()

        private val _purchaseEvents = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 1)
        override val purchaseEvents: SharedFlow<PurchaseEvent> = _purchaseEvents.asSharedFlow()

        private val _hasPendingPurchase = MutableStateFlow(false)
        override val hasPendingPurchase: Flow<Boolean> = _hasPendingPurchase.asStateFlow()

        private var billingClient: BillingClient? = null
        private var cachedProductDetails: com.android.billingclient.api.ProductDetails? = null
        private var cachedFormattedPrice: String? = null
        private var reconnectAttempts = 0
        private var reconnectJob: Job? = null
        private val initComplete = CompletableDeferred<Unit>()

        override fun isPro(): Boolean = _isProUser.value

        override suspend fun awaitPurchaseStatusReady() = awaitInitialized()

        suspend fun awaitInitialized() = initComplete.await()

        @Synchronized
        fun initialize() {
            // Debug builds always have Pro enabled for development
            if (BuildConfig.DEBUG) {
                _isProUser.value = true
                _billingAvailable.value = true
                initComplete.complete(Unit)
                return
            }

            if (billingClient != null) {
                if (billingClient?.isReady == true) {
                    initComplete.complete(Unit)
                }
                return
            }

            // Restore cached Pro state so Pro users don't see free-tier flash
            // while the async billing query runs
            if (proStatusCache.getCachedProStatus()) {
                updateProState(true)
            }

            _billingAvailable.value = false
            billingClient =
                BillingClient
                    .newBuilder(context)
                    .setListener(this)
                    .enableAutoServiceReconnection()
                    .enablePendingPurchases(
                        PendingPurchasesParams
                            .newBuilder()
                            .enableOneTimeProducts()
                            .build(),
                    ).build()

            billingClient?.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        when (billingResult.responseCode) {
                            BillingClient.BillingResponseCode.OK -> {
                                synchronized(this@BillingManager) {
                                    reconnectAttempts = 0
                                    reconnectJob?.cancel()
                                    reconnectJob = null
                                }
                                _billingAvailable.value = true
                                scope.launch {
                                    queryExistingPurchases()
                                    queryProductDetails()
                                    initComplete.complete(Unit)
                                }
                            }

                            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                            BillingClient.BillingResponseCode.NETWORK_ERROR,
                            BillingClient.BillingResponseCode.ERROR,
                            -> {
                                _billingAvailable.value = false
                                scheduleReconnect()
                                initComplete.complete(Unit)
                            }

                            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
                            BillingClient.BillingResponseCode.DEVELOPER_ERROR,
                            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
                            BillingClient.BillingResponseCode.ITEM_NOT_OWNED,
                            BillingClient.BillingResponseCode.USER_CANCELED,
                            -> {
                                _billingAvailable.value = false
                                initComplete.complete(Unit)
                            }

                            else -> {
                                _billingAvailable.value = false
                                initComplete.complete(Unit)
                            }
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        _billingAvailable.value = false
                    }
                },
            )
        }

        private fun reconnect() {
            billingClient?.endConnection()
            billingClient = null
            initialize()
        }

        private suspend fun queryExistingPurchases(): ProPurchaseRefreshResult {
            val client = billingClient ?: return ProPurchaseRefreshResult.UNAVAILABLE
            if (client.connectionState == BillingClient.ConnectionState.CONNECTING) {
                return ProPurchaseRefreshResult.UNAVAILABLE
            }
            val params =
                QueryPurchasesParams
                    .newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            val result = client.queryPurchasesAsync(params)
            return when (result.billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    syncPurchases(result.purchasesList, emitEvents = false)
                }

                in reconnectableBillingResponseCodes() -> {
                    _billingAvailable.value = false
                    scheduleReconnect()
                    ProPurchaseRefreshResult.UNAVAILABLE
                }

                in nonReadyBillingResponseCodes() -> {
                    _billingAvailable.value = false
                    ProPurchaseRefreshResult.UNAVAILABLE
                }

                else -> {
                    ProPurchaseRefreshResult.UNAVAILABLE
                }
            }
        }

        private suspend fun queryProductDetails(): com.android.billingclient.api.ProductDetails? {
            val client = billingClient ?: return null
            val product =
                QueryProductDetailsParams.Product
                    .newBuilder()
                    .setProductId(PRODUCT_ID_PRO)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            val params =
                QueryProductDetailsParams
                    .newBuilder()
                    .setProductList(listOf(product))
                    .build()
            val result: ProductDetailsResult = client.queryProductDetails(params)
            when (result.billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    cachedProductDetails = result.productDetailsList?.firstOrNull()
                    cachedFormattedPrice = cachedProductDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                    _billingAvailable.value = cachedProductDetails != null
                }

                in reconnectableBillingResponseCodes() -> {
                    _billingAvailable.value = cachedProductDetails != null
                    scheduleReconnect()
                }

                in nonReadyBillingResponseCodes() -> {
                    cachedProductDetails = null
                    cachedFormattedPrice = null
                    _billingAvailable.value = false
                }

                else -> {
                    cachedProductDetails = null
                    cachedFormattedPrice = null
                    _billingAvailable.value = false
                }
            }
            return cachedProductDetails
        }

        override suspend fun getFormattedPrice(): String? {
            cachedFormattedPrice?.let { return it }
            return queryProductDetails()?.oneTimePurchaseOfferDetails?.formattedPrice
        }

        override suspend fun refreshPurchaseStatus(): ProPurchaseRefreshResult = queryExistingPurchases()

        override suspend fun refreshPurchaseStatusAfterInitialization(): ProPurchaseRefreshResult {
            initialize()
            awaitInitialized()
            if (isPro()) return ProPurchaseRefreshResult.ACTIVE
            return refreshPurchaseStatus()
        }

        override fun launchPurchaseFlow(activity: Activity) {
            val productDetails = cachedProductDetails
            val client = billingClient
            if (productDetails == null || client == null || !client.isReady) {
                _purchaseEvents.tryEmit(
                    PurchaseEvent.Error(context.getString(R.string.billing_purchase_not_ready)),
                )
                return
            }

            val productDetailsParams =
                BillingFlowParams.ProductDetailsParams
                    .newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            val billingFlowParams =
                BillingFlowParams
                    .newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))
                    .build()

            val result = client.launchBillingFlow(activity, billingFlowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                    _purchaseEvents.tryEmit(PurchaseEvent.AlreadyOwned)
                    scope.launch { queryExistingPurchases() }
                } else {
                    _purchaseEvents.tryEmit(
                        PurchaseEvent.Error(
                            billingMessageFor(
                                responseCode = result.responseCode,
                                subResponseCode = result.onPurchasesUpdatedSubResponseCode,
                            ),
                        ),
                    )
                    maybeReconnectAfterFailure(result.responseCode)
                }
            }
        }

        override fun onPurchasesUpdated(
            billingResult: BillingResult,
            purchases: List<Purchase>?,
        ) {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    if (!purchases.isNullOrEmpty()) {
                        scope.launch { syncPurchases(purchases, emitEvents = true) }
                    } else {
                        scope.launch { queryExistingPurchases() }
                    }
                }

                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    _purchaseEvents.tryEmit(PurchaseEvent.Canceled)
                }

                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    _purchaseEvents.tryEmit(PurchaseEvent.AlreadyOwned)
                    scope.launch { queryExistingPurchases() }
                }

                in reconnectableBillingResponseCodes(),
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                BillingClient.BillingResponseCode.ITEM_NOT_OWNED,
                BillingClient.BillingResponseCode.DEVELOPER_ERROR,
                -> {
                    _purchaseEvents.tryEmit(
                        PurchaseEvent.Error(
                            billingMessageFor(
                                responseCode = billingResult.responseCode,
                                subResponseCode = billingResult.onPurchasesUpdatedSubResponseCode,
                            ),
                        ),
                    )
                    maybeReconnectAfterFailure(billingResult.responseCode)
                }

                else -> {
                    _purchaseEvents.tryEmit(
                        PurchaseEvent.Error(
                            billingMessageFor(
                                responseCode = billingResult.responseCode,
                                subResponseCode = billingResult.onPurchasesUpdatedSubResponseCode,
                            ),
                        ),
                    )
                }
            }
        }

        internal suspend fun syncPurchases(
            purchases: List<Purchase>,
            emitEvents: Boolean,
        ): ProPurchaseRefreshResult {
            val proPurchases = purchases.filter { it.products.contains(PRODUCT_ID_PRO) }
            val purchased =
                proPurchases.filter {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
            val pending =
                proPurchases.filter {
                    it.purchaseState == Purchase.PurchaseState.PENDING
                }

            _hasPendingPurchase.value = pending.isNotEmpty()

            return when {
                purchased.isNotEmpty() -> {
                    val acknowledgementResults =
                        purchased.map { purchase ->
                            purchase.isAcknowledged || acknowledgePurchaseWithRetry(purchase)
                        }
                    if (acknowledgementResults.any { it }) {
                        updateProState(true)
                        if (emitEvents) _purchaseEvents.tryEmit(PurchaseEvent.Success)
                        ProPurchaseRefreshResult.ACTIVE
                    } else {
                        updateProState(false)
                        if (emitEvents) {
                            _purchaseEvents.tryEmit(
                                PurchaseEvent.Error(context.getString(R.string.billing_purchase_error)),
                            )
                        }
                        ProPurchaseRefreshResult.UNAVAILABLE
                    }
                }

                pending.isNotEmpty() -> {
                    updateProState(false)
                    if (emitEvents) _purchaseEvents.tryEmit(PurchaseEvent.Pending)
                    ProPurchaseRefreshResult.NOT_ACTIVE
                }

                else -> {
                    updateProState(false)
                    ProPurchaseRefreshResult.NOT_ACTIVE
                }
            }
        }

        private suspend fun acknowledgePurchaseWithRetry(purchase: Purchase): Boolean {
            val params =
                AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            val client = billingClient ?: return false
            val acknowledged =
                retryBillingAcknowledgement(
                    maxRetries = MAX_ACK_RETRIES,
                    retryBaseDelayMs = ACK_RETRY_BASE_DELAY_MS,
                ) {
                    suspendCancellableCoroutine { cont ->
                        client.acknowledgePurchase(params) { billingResult ->
                            cont.resume(billingResult)
                        }
                    }
                }
            if (!acknowledged) {
                ReleaseSafeLog.error(TAG, "Failed to acknowledge purchase after $MAX_ACK_RETRIES attempts")
            }
            return acknowledged
        }

        private fun updateProState(isPro: Boolean) {
            _isProUser.value = isPro
            proStatusCache.setCachedProStatus(isPro)
        }

        @Synchronized
        private fun scheduleReconnect() {
            if (reconnectJob?.isActive == true) return

            if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                val delayMs = RECONNECT_BASE_DELAY_MS * (1L shl reconnectAttempts)
                reconnectAttempts++
                reconnectJob =
                    scope.launch {
                        delay(delayMs)
                        synchronized(this@BillingManager) {
                            reconnectJob = null
                        }
                        reconnect()
                    }
            } else {
                initComplete.complete(Unit)
            }
        }

        private fun maybeReconnectAfterFailure(responseCode: Int) {
            if (responseCode in reconnectableBillingResponseCodes()) {
                _billingAvailable.value = false
                scheduleReconnect()
            }
        }

        private fun billingMessageFor(
            responseCode: Int,
            subResponseCode: Int,
        ): String {
            val messageRes = billingMessageResFor(responseCode, subResponseCode)
            return context.getString(messageRes ?: R.string.billing_purchase_error)
        }

        @Synchronized
        fun destroy() {
            reconnectJob?.cancel()
            reconnectJob = null
            scope.cancel()
            billingClient?.endConnection()
            billingClient = null
            cachedProductDetails = null
            cachedFormattedPrice = null
            _billingAvailable.value = false
        }

        companion object {
            private const val TAG = "BillingManager"
            const val PRODUCT_ID_PRO = BuildConfig.PRO_PRODUCT_ID
            private const val MAX_RECONNECT_ATTEMPTS = 3
            private const val RECONNECT_BASE_DELAY_MS = 2_000L
            private const val MAX_ACK_RETRIES = 3
            private const val ACK_RETRY_BASE_DELAY_MS = 2_000L
        }
    }

internal fun reconnectableBillingResponseCodes(): Set<Int> =
    setOf(
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        BillingClient.BillingResponseCode.NETWORK_ERROR,
        BillingClient.BillingResponseCode.ERROR,
    )

internal suspend fun retryBillingAcknowledgement(
    maxRetries: Int,
    retryBaseDelayMs: Long,
    acknowledge: suspend () -> BillingResult,
): Boolean {
    require(maxRetries > 0) { "maxRetries must be positive" }
    require(retryBaseDelayMs >= 0L) { "retryBaseDelayMs must not be negative" }

    repeat(maxRetries) { attempt ->
        try {
            if (acknowledge().responseCode == BillingClient.BillingResponseCode.OK) {
                return true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Retry transient acknowledgement failures below.
        }
        if (attempt < maxRetries - 1) {
            delay(retryBaseDelayMs * (1L shl attempt))
        }
    }
    return false
}

internal fun nonReadyBillingResponseCodes(): Set<Int> =
    setOf(
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
        BillingClient.BillingResponseCode.DEVELOPER_ERROR,
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
        BillingClient.BillingResponseCode.ITEM_NOT_OWNED,
        BillingClient.BillingResponseCode.USER_CANCELED,
    )

internal fun billingMessageResFor(
    responseCode: Int,
    subResponseCode: Int = BillingClient.OnPurchasesUpdatedSubResponseCode.NO_APPLICABLE_SUB_RESPONSE_CODE,
): Int? =
    billingSubResponseMessageResFor(subResponseCode)
        ?: billingResponseMessageResFor(responseCode)

private fun billingSubResponseMessageResFor(subResponseCode: Int): Int? =
    when (subResponseCode) {
        BillingClient.OnPurchasesUpdatedSubResponseCode.PAYMENT_DECLINED_DUE_TO_INSUFFICIENT_FUNDS -> {
            R.string.billing_insufficient_funds
        }

        BillingClient.OnPurchasesUpdatedSubResponseCode.USER_INELIGIBLE -> {
            R.string.billing_user_ineligible
        }

        else -> {
            null
        }
    }

private fun billingResponseMessageResFor(responseCode: Int): Int? =
    when (responseCode) {
        BillingClient.BillingResponseCode.NETWORK_ERROR -> R.string.billing_network_error

        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
        -> R.string.billing_service_unavailable

        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
        -> R.string.billing_item_unavailable

        else -> null
    }
