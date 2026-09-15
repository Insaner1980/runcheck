package com.runcheck.ui.home.insights

import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.testutil.insightFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InsightNavigationTest {
    @Test
    fun `resolves navigation at the Insight Pro boundary`() {
        val cases =
            listOf(
                NavigationCase(InsightTarget.BATTERY, isPro = false, expectedDestination = "battery"),
                NavigationCase(InsightTarget.APP_USAGE, isPro = false, expectedDestination = "pro_upgrade"),
                NavigationCase(InsightTarget.CHARGER, isPro = false, expectedDestination = "pro_upgrade"),
                NavigationCase(InsightTarget.APP_USAGE, isPro = true, expectedDestination = "app_usage"),
                NavigationCase(InsightTarget.CHARGER, isPro = true, expectedDestination = "charger"),
                NavigationCase(InsightTarget.NONE, isPro = false, expectedDestination = null),
            )

        cases.forEach { case ->
            var actualDestination: String? = null
            val action =
                resolveInsightNavigationAction(
                    insight = insightFixture(target = case.target),
                    isPro = case.isPro,
                    navigationHandlers =
                        InsightNavigationHandlers(
                            onNavigateToBattery = { actualDestination = "battery" },
                            onNavigateToNetwork = { actualDestination = "network" },
                            onNavigateToThermal = { actualDestination = "thermal" },
                            onNavigateToStorage = { actualDestination = "storage" },
                            onNavigateToCharger = { actualDestination = "charger" },
                            onNavigateToAppUsage = { actualDestination = "app_usage" },
                            onNavigateToProUpgrade = { actualDestination = "pro_upgrade" },
                        ),
                )

            if (case.expectedDestination == null) {
                assertNull(case.target.name, action.onClick)
            } else {
                assertNotNull(case.target.name, action.onClick)
                requireNotNull(action.onClick).invoke()
                assertEquals(case.target.name, case.expectedDestination, actualDestination)
            }
        }
    }

    private data class NavigationCase(
        val target: InsightTarget,
        val isPro: Boolean,
        val expectedDestination: String?,
    )
}
