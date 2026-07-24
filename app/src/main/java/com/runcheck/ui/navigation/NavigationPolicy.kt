package com.runcheck.ui.navigation

import com.runcheck.ui.storage.cleanup.CleanupType

enum class TopLevelNavigationAction {
    SWITCH,
    RESELECT,
}

enum class ProtectedFeatureAccessState {
    WAITING_FOR_PRO_STATUS,
    LOCKED,
    AVAILABLE,
}

fun protectedFeatureAccessState(
    proStatusReady: Boolean,
    hasProAccess: Boolean,
): ProtectedFeatureAccessState =
    when {
        !proStatusReady -> ProtectedFeatureAccessState.WAITING_FOR_PRO_STATUS
        hasProAccess -> ProtectedFeatureAccessState.AVAILABLE
        else -> ProtectedFeatureAccessState.LOCKED
    }

fun toolsStorageCleanupRoute(): String = Screen.Cleanup(CleanupType.LARGE_FILES.name).route

fun topLevelDestinationFor(route: String?): TopLevelDestination? =
    topLevelDestinations.firstOrNull { it.screen.route == route }

fun coldStartParentFor(route: String): TopLevelDestination? =
    topLevelDestinationFor(route)
        ?: when {
            route in
                setOf(
                    Screen.Battery.route,
                    Screen.Network.route,
                    Screen.Thermal.route,
                    Screen.Storage.route,
                ) ||
                route.matchesRoutePattern(Screen.FullscreenChart.ROUTE) -> TopLevelDestination.Home

            route in
                setOf(
                    Screen.SpeedTest.route,
                    Screen.Charger.route,
                    Screen.AppUsage.route,
                    Screen.WeeklyReport.route,
                    Screen.Learn.route,
                    Screen.Export.route,
                ) ||
                route.matchesRoutePattern(Screen.Cleanup.ROUTE) ||
                route.matchesRoutePattern(Screen.LearnArticle.ROUTE) -> TopLevelDestination.Tools

            route == Screen.ProUpgrade.route -> TopLevelDestination.Settings

            else -> null
        }

fun externalNavigationPlan(route: String): List<String> {
    val parent = coldStartParentFor(route) ?: return emptyList()
    return if (parent.screen.route == route) {
        listOf(route)
    } else {
        listOf(parent.screen.route, route)
    }
}

fun topLevelNavigationAction(
    currentRoute: String?,
    destination: TopLevelDestination,
): TopLevelNavigationAction =
    if (currentRoute == destination.screen.route) {
        TopLevelNavigationAction.RESELECT
    } else {
        TopLevelNavigationAction.SWITCH
    }

fun String.requiresReadyProStatus(): Boolean =
    when {
        this == Screen.Charger.route ||
            this == Screen.AppUsage.route ||
            this == Screen.Export.route -> {
            true
        }

        matchesRoutePattern(Screen.Cleanup.ROUTE) -> {
            true
        }

        matchesRoutePattern(Screen.FullscreenChart.ROUTE) -> {
            split('/').getOrNull(1) in proFullscreenSources
        }

        else -> {
            false
        }
    }

internal fun String.matchesRoutePattern(pattern: String): Boolean {
    val routeSegments = split('/')
    val patternSegments = pattern.split('/')
    if (routeSegments.size != patternSegments.size) return false
    return routeSegments.zip(patternSegments).all { (routeSegment, patternSegment) ->
        if (patternSegment.startsWith('{') && patternSegment.endsWith('}')) {
            routeArgumentPattern.matches(routeSegment)
        } else {
            routeSegment == patternSegment
        }
    }
}

private val proFullscreenSources = setOf("BATTERY_HISTORY", "NETWORK_HISTORY")
private val routeArgumentPattern = Regex("[A-Za-z0-9_-]+")
