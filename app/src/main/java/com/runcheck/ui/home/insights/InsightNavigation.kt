package com.runcheck.ui.home.insights

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightTarget

data class InsightNavigationAction(
    val onClick: (() -> Unit)?,
)

fun resolveInsightNavigationAction(
    insight: Insight,
    isPro: Boolean,
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
): InsightNavigationAction {
    val onClick =
        when (insight.target) {
            InsightTarget.BATTERY -> onNavigateToBattery
            InsightTarget.THERMAL -> onNavigateToThermal
            InsightTarget.NETWORK -> onNavigateToNetwork
            InsightTarget.STORAGE -> onNavigateToStorage
            InsightTarget.CHARGER -> if (isPro) onNavigateToCharger else onNavigateToProUpgrade
            InsightTarget.APP_USAGE -> if (isPro) onNavigateToAppUsage else onNavigateToProUpgrade
            InsightTarget.NONE -> null
        }

    return InsightNavigationAction(onClick = onClick)
}
