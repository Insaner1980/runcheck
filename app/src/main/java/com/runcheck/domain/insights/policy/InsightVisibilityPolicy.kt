package com.runcheck.domain.insights.policy

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightTarget

fun List<Insight>.visibleForProAccess(isPro: Boolean): List<Insight> =
    if (isPro) {
        this
    } else {
        filterNot { it.target.requiresProAccess() }
    }

fun InsightTarget.requiresProAccess(): Boolean =
    when (this) {
        InsightTarget.APP_USAGE,
        InsightTarget.CHARGER,
        -> true

        InsightTarget.NONE,
        InsightTarget.BATTERY,
        InsightTarget.THERMAL,
        InsightTarget.NETWORK,
        InsightTarget.STORAGE,
        -> false
    }
