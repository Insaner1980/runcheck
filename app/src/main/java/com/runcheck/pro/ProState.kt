package com.runcheck.pro

import androidx.compose.runtime.Immutable

enum class ProStatus {
    FREE,
    PRO_PURCHASED,
}

enum class ProFeature {
    EXTENDED_HISTORY,
    CHARGER_COMPARISON,
    PER_APP_BATTERY,
    WIDGETS,
    CSV_EXPORT,
    THERMAL_LOGS,
    REMAINING_CHARGE_TIME,
    STORAGE_CLEANUP,
}

@Immutable
data class ProState(
    val status: ProStatus = ProStatus.FREE,
    val purchaseTimestamp: Long = 0L,
) {
    val isPro: Boolean
        get() = status == ProStatus.PRO_PURCHASED

    @Suppress("UnusedParameter") // All features gated behind single Pro status; param kept for per-feature gating
    fun hasFeature(feature: ProFeature): Boolean = isPro
}
