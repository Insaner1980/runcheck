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
import com.runcheck.billing.ProPurchaseRefreshResult
import com.runcheck.billing.ProPurchaseManager
import com.runcheck.billing.PurchaseEvent
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
class BillingManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PurchasesUpdatedListener,
    com.runcheck.domain.repository.ProStatusProvider,
    ProPurchaseManager {

    private val scopeExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        ReleaseSafeLog.error(TAG, "Billing coroutine failed", throwable)
    }
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.Main + scopeExceptionHandler)

    private val _isProState = MutableStateFlow(false)
    override val isProUser: Flow<Boolean> = _isProState.asStateFlow()
    override val proState: Flow<com.runcheck.pro.ProState>
        get() = _isProState.asStateFlow().map { isPro ->
            if (isPro) com.runcheck.pro.ProState(status = com.runcheck.pro.ProStatus.PRO_PURCHASED)
            else com.runcheck.pro.ProState()
        }
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
    private val _initComplete = CompletableDeferred<Unit>()

    override fun isPro(): Boolean = _isProState.value

    suspend fun awaitInitialized() = _initComplete.await()

    fun initialize() {
        // Debug builds always have Pro enabled for development
        if (BuildConfig.DEBUG) {
            updateProState(true)
            _billingAvailable.value = true
            _initComplete.complete(Unit)
            return
        }

        if (billingClient?.isReady == true) {
            _initComplete.complete(Unit)
            return
        }

        _billingAvailable.value = false
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    reconnectAttempts = 0
                    _billingAvailable.value = true
                    scope.launch {
                        queryExistingPurchases()
                        queryProductDetails()
                        _initComplete.complete(Unit)
                    }
                } else {
                    _billingAvailable.value = false
                    _initComplete.complete(Unit)
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingAvailable.value = false
                if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    val delayMs = RECONNECT_BASE_DELAY_MS * (1L shl reconnectAttempts)
                    reconnectAttempts++
                    scope.launch {
                        delay(delayMs)
                        reconnect()
                    }
                } else {
                    _initComplete.complete(Unit)
                }
            }
        })
    }

    private fun reconnect() {
        billingClient?.endConnection()
        billingClient = null
        initialize()
    }

    private suspend fun queryExistingPurchases(): ProPurchaseRefreshResult {
        val client = billingClient ?: return ProPurchaseRefreshResult.UNAVAILABLE
        if (!client.isReady) return ProPurchaseRefreshResult.UNAVAILABLE
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = client.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            return syncPurchases(result.purchasesList, emitEvents = false)
        }
        return ProPurchaseRefreshResult.UNAVAILABLE
    }

    private suspend fun queryProductDetails(): com.android.billingclient.api.ProductDetails? {
        val client = billingClient ?: return null
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID_PRO)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        val result: ProductDetailsResult = client.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            cachedProductDetails = result.productDetailsList?.firstOrNull()
            cachedFormattedPrice = cachedProductDetails?.oneTimePurchaseOfferDetails?.formattedPrice
            _billingAvailable.value = cachedProductDetails != null
        } else {
            cachedProductDetails = null
            _billingAvailable.value = false
        }
        return cachedProductDetails
    }

    override suspend fun getFormattedPrice(): String? {
        cachedFormattedPrice?.let { return it }
        return queryProductDetails()?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    override suspend fun refreshPurchaseStatus(): ProPurchaseRefreshResult {
        return queryExistingPurchases()
    }

    override fun launchPurchaseFlow(activity: Activity) {
        val productDetails = cachedProductDetails
        val client = billingClient
        if (productDetails == null || client == null || !client.isReady) {
            _purchaseEvents.tryEmit(PurchaseEvent.Error("Billing not ready"))
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = client.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseEvents.tryEmit(PurchaseEvent.Error("Could not start purchase"))
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    scope.launch { syncPurchases(purchases, emitEvents = true) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseEvents.tryEmit(PurchaseEvent.Canceled)
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _purchaseEvents.tryEmit(PurchaseEvent.AlreadyOwned)
                scope.launch { queryExistingPurchases() }
            }
            else -> {
                _purchaseEvents.tryEmit(
                    PurchaseEvent.Error(billingResult.debugMessage)
                )
            }
        }
    }

    private suspend fun syncPurchases(
        purchases: List<Purchase>,
        emitEvents: Boolean
    ): ProPurchaseRefreshResult {
        val proPurchases = purchases.filter { it.products.contains(PRODUCT_ID_PRO) }
        val purchased = proPurchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        val pending = proPurchases.filter {
            it.purchaseState == Purchase.PurchaseState.PENDING
        }

        _hasPendingPurchase.value = pending.isNotEmpty()

        return when {
            purchased.isNotEmpty() -> {
                updateProState(true)
                purchased
                    .filter { !it.isAcknowledged }
                    .forEach { acknowledgePurchaseWithRetry(it) }
                ProPurchaseRefreshResult.ACTIVE
            }
            pending.isNotEmpty() -> {
                if (emitEvents) _purchaseEvents.tryEmit(PurchaseEvent.Pending)
                ProPurchaseRefreshResult.NOT_ACTIVE
            }
            else -> {
                updateProState(false)
                ProPurchaseRefreshResult.NOT_ACTIVE
            }
        }
    }

    private suspend fun acknowledgePurchaseWithRetry(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        repeat(MAX_ACK_RETRIES) { attempt ->
            val client = billingClient ?: return
            try {
                val result = suspendCancellableCoroutine { cont ->
                    client.acknowledgePurchase(params) { billingResult ->
                        cont.resume(billingResult)
                    }
                }
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    return
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Fall through to retry
            }
            if (attempt < MAX_ACK_RETRIES - 1) {
                delay(ACK_RETRY_BASE_DELAY_MS * (1L shl attempt))
            }
        }
        ReleaseSafeLog.error(TAG, "Failed to acknowledge purchase after $MAX_ACK_RETRIES attempts")
    }

    private fun updateProState(isPro: Boolean) {
        _isProState.value = isPro
    }

    fun destroy() {
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
