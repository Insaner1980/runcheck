package com.runcheck.pro

import androidx.compose.runtime.Immutable

enum class ProStatus {
    TRIAL_ACTIVE,
    TRIAL_EXPIRED,
    PRO_PURCHASED
}

enum class ProFeature {
    EXTENDED_HISTORY,
    CHARGER_COMPARISON,
    PER_APP_BATTERY,
    WIDGETS,
    CSV_EXPORT,
    THERMAL_LOGS,
    AD_FREE
}

@Immutable
data class ProState(
    val status: ProStatus = ProStatus.TRIAL_EXPIRED,
    val trialDaysRemaining: Int = 0,
    val trialStartTimestamp: Long = 0L,
    val purchaseTimestamp: Long = 0L
) {
    val isPro: Boolean
        get() = status == ProStatus.PRO_PURCHASED || status == ProStatus.TRIAL_ACTIVE

    @Suppress("UnusedParameter") // All features gated behind single Pro status; param kept for per-feature gating
    fun hasFeature(feature: ProFeature): Boolean = isPro
}
