package com.runcheck.ui.home.insights

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.policy.requiresProAccess

data class InsightNavigationAction(
    val onClick: (() -> Unit)?,
)

data class InsightNavigationHandlers(
    val onNavigateToBattery: () -> Unit,
    val onNavigateToNetwork: () -> Unit,
    val onNavigateToThermal: () -> Unit,
    val onNavigateToStorage: () -> Unit,
    val onNavigateToCharger: () -> Unit,
    val onNavigateToAppUsage: () -> Unit,
    val onNavigateToProUpgrade: () -> Unit,
)

fun resolveInsightNavigationAction(
    insight: Insight,
    isPro: Boolean,
    navigationHandlers: InsightNavigationHandlers,
): InsightNavigationAction {
    val onClick =
        if (!isPro && insight.target.requiresProAccess()) {
            navigationHandlers.onNavigateToProUpgrade
        } else {
            when (insight.target) {
                InsightTarget.BATTERY -> {
                    navigationHandlers.onNavigateToBattery
                }

                InsightTarget.THERMAL -> {
                    navigationHandlers.onNavigateToThermal
                }

                InsightTarget.NETWORK -> {
                    navigationHandlers.onNavigateToNetwork
                }

                InsightTarget.STORAGE -> {
                    navigationHandlers.onNavigateToStorage
                }

                InsightTarget.CHARGER -> {
                    navigationHandlers.onNavigateToCharger
                }

                InsightTarget.APP_USAGE -> {
                    navigationHandlers.onNavigateToAppUsage
                }

                InsightTarget.NONE -> {
                    null
                }
            }
        }

    return InsightNavigationAction(onClick = onClick)
}
