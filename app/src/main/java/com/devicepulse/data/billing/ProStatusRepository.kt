package com.devicepulse.data.billing

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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProStatusRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isProState = MutableStateFlow(false)
    val isProUser: Flow<Boolean> = _isProState.asStateFlow()

    private var billingClient: BillingClient? = null
    private var cachedProductDetails: com.android.billingclient.api.ProductDetails? = null

    fun isPro(): Boolean = _isProState.value

    fun initialize() {
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
                    scope.launch { queryExistingPurchases() }
                }
            }

            override fun onBillingServiceDisconnected() {
                // BillingClient reconnects automatically
            }
        })
    }

    private suspend fun queryExistingPurchases() {
        val client = billingClient ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = client.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val hasPro = result.purchasesList.any { purchase ->
                purchase.products.contains(PRODUCT_ID_PRO) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            _isProState.value = hasPro
        }
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
        }
        return cachedProductDetails
    }

    fun launchPurchaseFlow(activity: Activity): BillingResult? {
        val productDetails = cachedProductDetails ?: return null
        val client = billingClient ?: return null

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        return client.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    _isProState.value = true
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val client = billingClient ?: return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params) { /* acknowledged */ }
    }

    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }

    companion object {
        const val PRODUCT_ID_PRO = "devicepulse_pro"
    }
}
