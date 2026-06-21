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
    this == InsightTarget.APP_USAGE ||
        this == InsightTarget.CHARGER
