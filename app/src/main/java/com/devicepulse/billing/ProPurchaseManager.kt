package com.devicepulse.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow

interface ProPurchaseManager {
    val isProUser: Flow<Boolean>
    suspend fun getFormattedPrice(): String?
    fun launchPurchaseFlow(activity: Activity)
}
