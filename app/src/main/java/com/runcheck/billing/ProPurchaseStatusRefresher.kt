package com.runcheck.billing

fun interface ProPurchaseStatusRefresher {
    suspend fun refreshPurchaseStatusAfterInitialization(): ProPurchaseRefreshResult
}
