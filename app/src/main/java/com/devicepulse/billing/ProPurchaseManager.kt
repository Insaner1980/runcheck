package com.devicepulse.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow

enum class ProPurchaseRefreshResult {
    ACTIVE,
    NOT_ACTIVE,
    UNAVAILABLE
}

interface ProPurchaseManager {
    val isProUser: Flow<Boolean>
    val billingAvailable: Flow<Boolean>
    suspend fun getFormattedPrice(): String?
    suspend fun refreshPurchaseStatus(): ProPurchaseRefreshResult
    fun launchPurchaseFlow(activity: Activity)
}
