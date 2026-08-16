package com.runcheck.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface PurchaseEvent {
    data object Success : PurchaseEvent

    data object Pending : PurchaseEvent

    data object AlreadyOwned : PurchaseEvent

    data object Canceled : PurchaseEvent

    data class Error(
        val debugMessage: String,
    ) : PurchaseEvent
}

enum class ProPurchaseRefreshResult {
    ACTIVE,
    NOT_ACTIVE,
    UNAVAILABLE,
}

interface ProPurchaseManager {
    val isProUser: StateFlow<Boolean>
    val billingAvailable: Flow<Boolean>
    val purchaseEvents: SharedFlow<PurchaseEvent>
    val hasPendingPurchase: Flow<Boolean>

    suspend fun awaitPurchaseStatusReady() = Unit

    suspend fun getFormattedPrice(): String?

    suspend fun refreshPurchaseStatus(): ProPurchaseRefreshResult

    fun launchPurchaseFlow(activity: Activity)
}
