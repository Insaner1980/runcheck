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
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isProState = MutableStateFlow(false)
    override val isProUser: Flow<Boolean> = _isProState.asStateFlow()
    override val proState: Flow<com.runcheck.pro.ProState>
        get() = _isProState.asStateFlow().map { isPro ->
            if (isPro) com.runcheck.pro.ProState(status = com.runcheck.pro.ProStatus.PRO_PURCHASED)
            else com.runcheck.pro.ProState()
        }
    private val _billingAvailable = MutableStateFlow(false)
    override val billingAvailable: Flow<Boolean> = _billingAvailable.asStateFlow()

    private var billingClient: BillingClient? = null
    private var cachedProductDetails: com.android.billingclient.api.ProductDetails? = null

    init {
        scope.launch(Dispatchers.IO) {
            runCatching {
                ProStatusCache.isPro(context)
            }.onSuccess { isPro ->
                _isProState.value = isPro
            }.onFailure { error ->
                ReleaseSafeLog.error(TAG, "Failed to load persisted pro state", error)
            }
        }
    }

    override fun isPro(): Boolean = _isProState.value

    fun initialize() {
        if (billingClient?.isReady == true) return

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
                    _billingAvailable.value = true
                    scope.launch {
                        queryExistingPurchases()
                        queryProductDetails()
                    }
                } else {
                    _billingAvailable.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingAvailable.value = false
                scope.launch { reconnect() }
            }
        })
    }

    private suspend fun reconnect() {
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
            return if (syncPurchases(result.purchasesList)) {
                ProPurchaseRefreshResult.ACTIVE
            } else {
                ProPurchaseRefreshResult.NOT_ACTIVE
            }
        }
        return ProPurchaseRefreshResult.UNAVAILABLE
    }

    suspend fun queryProductDetails(): com.android.billingclient.api.ProductDetails? {
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
            _billingAvailable.value = cachedProductDetails != null
        } else {
            cachedProductDetails = null
            _billingAvailable.value = false
        }
        return cachedProductDetails
    }

    override suspend fun getFormattedPrice(): String? {
        return queryProductDetails()?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    override suspend fun refreshPurchaseStatus(): ProPurchaseRefreshResult {
        return queryExistingPurchases()
    }

    override fun launchPurchaseFlow(activity: Activity) {
        val productDetails = cachedProductDetails ?: return
        val client = billingClient ?: return

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        client.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            syncPurchases(purchases)
        }
    }

    private fun syncPurchases(purchases: List<Purchase>): Boolean {
        val proPurchases = purchases
            .filter(::isEligibleProPurchase)

        updateProState(proPurchases.isNotEmpty())

        proPurchases
            .filter { !it.isAcknowledged }
            .forEach { acknowledgePurchase(it) }
        return proPurchases.isNotEmpty()
    }

    private fun isEligibleProPurchase(purchase: Purchase): Boolean {
        return purchase.products.contains(PRODUCT_ID_PRO) &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val client = billingClient ?: return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                updateProState(true)
            }
        }
    }

    private fun updateProState(isPro: Boolean) {
        _isProState.value = isPro
        scope.launch(Dispatchers.IO) {
            runCatching {
                ProStatusCache.setPro(context, isPro)
            }.onFailure { error ->
                ReleaseSafeLog.error(TAG, "Failed to persist pro state", error)
            }
        }
    }

    fun destroy() {
        scope.cancel()
        billingClient?.endConnection()
        billingClient = null
        cachedProductDetails = null
        _billingAvailable.value = false
    }

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_ID_PRO = BuildConfig.PRO_PRODUCT_ID
    }
}
