package com.runcheck.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.runcheck.pro.ProStateProvider
import com.runcheck.ui.appusage.AppUsageScreen
import com.runcheck.ui.battery.BatteryDetailScreen
import com.runcheck.ui.charger.ChargerComparisonScreen
import com.runcheck.ui.fullscreen.FullscreenChartResult
import com.runcheck.ui.fullscreen.FullscreenChartScreen
import com.runcheck.ui.home.HomeScreen
import com.runcheck.ui.home.insights.InsightNavigationHandlers
import com.runcheck.ui.insights.InsightsScreen
import com.runcheck.ui.learn.LearnArticleDetailScreen
import com.runcheck.ui.learn.LearnScreen
import com.runcheck.ui.network.NetworkDetailScreen
import com.runcheck.ui.network.NetworkViewModel
import com.runcheck.ui.network.SpeedTestScreen
import com.runcheck.ui.pro.ProUpgradeScreen
import com.runcheck.ui.settings.SettingsScreen
import com.runcheck.ui.storage.StorageDetailScreen
import com.runcheck.ui.storage.cleanup.CleanupScreen
import com.runcheck.ui.theme.LocalReducedMotion
import com.runcheck.ui.theme.MotionTokens
import com.runcheck.ui.thermal.ThermalDetailScreen

@Composable
fun RuncheckNavHost(
    proStateProvider: ProStateProvider,
    modifier: Modifier = Modifier,
    deepLinkRoute: String? = null,
    onConsumeDeepLink: () -> Unit = {},
) {
    val navController = rememberNavController()
    val reducedMotion = LocalReducedMotion.current
    val proState by proStateProvider.proState.collectAsStateWithLifecycle()
    val proAccessReady by proStateProvider.proAccessReady.collectAsStateWithLifecycle()
    val insightNavigationHandlers =
        remember(navController) {
            InsightNavigationHandlers(
                onNavigateToBattery = { navController.navigateSingleTop(Screen.Battery.route) },
                onNavigateToNetwork = { navController.navigateSingleTop(Screen.Network.route) },
                onNavigateToThermal = { navController.navigateSingleTop(Screen.Thermal.route) },
                onNavigateToStorage = { navController.navigateSingleTop(Screen.Storage.route) },
                onNavigateToCharger = {
                    navController.navigateNested(
                        parentRoute = Screen.Battery.route,
                        childRoute = Screen.Charger.route,
                    )
                },
                onNavigateToAppUsage = { navController.navigateSingleTop(Screen.AppUsage.route) },
                onNavigateToProUpgrade = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
            )
        }
    val navigateToFullscreen: (String, String, String) -> Unit =
        remember(navController) {
            { source, metric, period ->
                navController.navigateSingleTop(Screen.FullscreenChart(source, metric, period).route)
            }
        }
    val navigateToLearnArticle: (String) -> Unit =
        remember(navController) {
            { articleId -> navController.navigateSingleTop(Screen.LearnArticle(articleId).route) }
        }

    // Navigate to the deep-link screen and consume so it doesn't re-fire
    val currentOnConsumeDeepLink by rememberUpdatedState(onConsumeDeepLink)
    LaunchedEffect(deepLinkRoute, proAccessReady, proState.isPro) {
        val resolvedRoute =
            deepLinkRoute?.let { route ->
                resolveProRoute(route, proAccessReady, proState.isPro)
            } ?: return@LaunchedEffect
        navController.navigateNotificationRoute(resolvedRoute)
        currentOnConsumeDeepLink()
    }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            if (reducedMotion) {
                EnterTransition.None
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(MotionTokens.MEDIUM),
                ) + fadeIn(animationSpec = tween(MotionTokens.MEDIUM))
            }
        },
        exitTransition = {
            if (reducedMotion) {
                ExitTransition.None
            } else {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(MotionTokens.MEDIUM),
                ) + fadeOut(animationSpec = tween(MotionTokens.MEDIUM))
            }
        },
        popEnterTransition = {
            if (reducedMotion) {
                EnterTransition.None
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(MotionTokens.MEDIUM),
                ) + fadeIn(animationSpec = tween(MotionTokens.MEDIUM))
            }
        },
        popExitTransition = {
            if (reducedMotion) {
                ExitTransition.None
            } else {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(MotionTokens.MEDIUM),
                ) + fadeOut(animationSpec = tween(MotionTokens.MEDIUM))
            }
        },
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToBattery = insightNavigationHandlers.onNavigateToBattery,
                onNavigateToNetwork = insightNavigationHandlers.onNavigateToNetwork,
                onNavigateToThermal = insightNavigationHandlers.onNavigateToThermal,
                onNavigateToStorage = insightNavigationHandlers.onNavigateToStorage,
                onNavigateToCharger = insightNavigationHandlers.onNavigateToCharger,
                onNavigateToSpeedTest = {
                    navController.navigateNested(
                        parentRoute = Screen.Network.route,
                        childRoute = Screen.SpeedTest.route,
                    )
                },
                onNavigateToAppUsage = insightNavigationHandlers.onNavigateToAppUsage,
                onNavigateToInsights = { navController.navigateSingleTop(Screen.Insights.route) },
                onNavigateToSettings = { navController.navigateSingleTop(Screen.Settings.route) },
                onNavigateToProUpgrade = insightNavigationHandlers.onNavigateToProUpgrade,
                onNavigateToLearn = { navController.navigateSingleTop(Screen.Learn.route) },
                onNavigateToLearnArticle = navigateToLearnArticle,
            )
        }
        composable(Screen.Insights.route) {
            InsightsScreen(
                onBack = { navController.popBackStack() },
                navigationHandlers = insightNavigationHandlers,
            )
        }
        composable(Screen.Battery.route) { entry ->
            val fullscreenResult = entry.rememberFullscreenChartResult()
            BatteryDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCharger = { navController.navigateSingleTop(Screen.Charger.route) },
                onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                onNavigateToFullscreen = navigateToFullscreen,
                onNavigateToLearnArticle = navigateToLearnArticle,
                fullscreenResultSource = fullscreenResult.source,
                fullscreenResultMetric = fullscreenResult.metric,
                fullscreenResultPeriod = fullscreenResult.period,
                onConsumeFullscreenResult = entry::consumeFullscreenChartResult,
            )
        }
        composable(Screen.Network.route) { entry ->
            val networkViewModel: NetworkViewModel = hiltViewModel(entry)
            val fullscreenResult = entry.rememberFullscreenChartResult()
            NetworkDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSpeedTest = { navController.navigateSingleTop(Screen.SpeedTest.route) },
                onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                onNavigateToFullscreen = navigateToFullscreen,
                onNavigateToLearnArticle = navigateToLearnArticle,
                fullscreenResultMetric = fullscreenResult.metric,
                fullscreenResultPeriod = fullscreenResult.period,
                onConsumeFullscreenResult = entry::consumeFullscreenChartResult,
                viewModelProvider = { networkViewModel },
            )
        }
        composable(Screen.Thermal.route) {
            ThermalDetailScreen(
                onBack = { navController.popBackStack() },
                onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                onNavigateToLearnArticle = { articleId ->
                    navController.navigateSingleTop(Screen.LearnArticle(articleId).route)
                },
            )
        }
        composable(Screen.Storage.route) {
            StorageDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCleanup = { type ->
                    navController.navigateSingleTop(Screen.Cleanup(type.name).route)
                },
                onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                onNavigateToLearnArticle = { articleId ->
                    navController.navigateSingleTop(Screen.LearnArticle(articleId).route)
                },
            )
        }
        composable(
            route = Screen.Cleanup.ROUTE,
            arguments = listOf(navArgument("type") { type = NavType.StringType }),
        ) {
            CleanupScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Charger.route) {
            ProRouteGate(
                route = Screen.Charger.route,
                proAccessReady = proAccessReady,
                isPro = proState.isPro,
                onRedirect = { navController.redirectToProUpgrade(Screen.Charger.route) },
            ) {
                ChargerComparisonScreen(
                    onBack = { navController.popBackStack() },
                    onUpgradeToPro = {
                        navController.redirectToProUpgrade(Screen.Charger.route)
                    },
                )
            }
        }
        composable(Screen.AppUsage.route) {
            ProRouteGate(
                route = Screen.AppUsage.route,
                proAccessReady = proAccessReady,
                isPro = proState.isPro,
                onRedirect = { navController.redirectToProUpgrade(Screen.AppUsage.route) },
            ) {
                AppUsageScreen(
                    onBack = { navController.popBackStack() },
                    onUpgradeToPro = {
                        navController.redirectToProUpgrade(Screen.AppUsage.route)
                    },
                )
            }
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToLearnArticle = { articleId ->
                    navController.navigateSingleTop(Screen.LearnArticle(articleId).route)
                },
            )
        }
        composable(Screen.SpeedTest.route) {
            val networkParentEntry =
                remember(navController) {
                    runCatching { navController.getBackStackEntry(Screen.Network.route) }.getOrNull()
                }
            val networkViewModel: NetworkViewModel =
                if (networkParentEntry != null) {
                    hiltViewModel(networkParentEntry)
                } else {
                    hiltViewModel()
                }
            SpeedTestScreen(
                onBack = { navController.popBackStack() },
                viewModelProvider = { networkViewModel },
            )
        }
        composable(Screen.ProUpgrade.route) {
            ProUpgradeScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Learn.route) {
            LearnScreen(
                onBack = { navController.popBackStack() },
                onNavigateToArticle = { articleId ->
                    navController.navigateSingleTop(Screen.LearnArticle(articleId).route)
                },
            )
        }
        composable(
            route = Screen.LearnArticle.ROUTE,
            arguments = listOf(navArgument("articleId") { type = NavType.StringType }),
        ) {
            val articleId = it.arguments?.getString("articleId") ?: ""
            LearnArticleDetailScreen(
                articleId = articleId,
                onBack = { navController.popBackStack() },
                onNavigateToRoute = { route -> navController.navigateSingleTop(route) },
            )
        }
        composable(
            route = Screen.FullscreenChart.ROUTE,
            arguments =
                listOf(
                    navArgument("source") { type = NavType.StringType },
                    navArgument("metric") { type = NavType.StringType },
                    navArgument("period") { type = NavType.StringType },
                ),
            enterTransition = {
                if (reducedMotion) {
                    EnterTransition.None
                } else {
                    scaleIn(
                        initialScale = 0.94f,
                        animationSpec = tween(MotionTokens.FULLSCREEN_ENTER_SCALE),
                    ) + fadeIn(animationSpec = tween(MotionTokens.FULLSCREEN_ENTER_FADE))
                }
            },
            exitTransition = {
                if (reducedMotion) {
                    ExitTransition.None
                } else {
                    scaleOut(
                        targetScale = 1.02f,
                        animationSpec = tween(MotionTokens.FULLSCREEN_EXIT),
                    ) + fadeOut(animationSpec = tween(MotionTokens.FULLSCREEN_EXIT))
                }
            },
            popEnterTransition = {
                if (reducedMotion) {
                    EnterTransition.None
                } else {
                    fadeIn(animationSpec = tween(MotionTokens.FULLSCREEN_EXIT))
                }
            },
            popExitTransition = {
                if (reducedMotion) {
                    ExitTransition.None
                } else {
                    scaleOut(
                        targetScale = 0.96f,
                        animationSpec = tween(MotionTokens.FULLSCREEN_ENTER_FADE),
                    ) + fadeOut(animationSpec = tween(MotionTokens.FULLSCREEN_ENTER_FADE))
                }
            },
        ) {
            FullscreenChartScreen(
                onBack = { navController.popBackStack() },
                onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                onSelectionChange = { source, metric, period ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set(FullscreenChartResult.KEY_SOURCE, source)
                        set(FullscreenChartResult.KEY_METRIC, metric)
                        set(FullscreenChartResult.KEY_PERIOD, period)
                    }
                },
            )
        }
    }
}

private data class FullscreenChartResultState(
    val source: String?,
    val metric: String?,
    val period: String?,
)

@Composable
private fun NavBackStackEntry.rememberFullscreenChartResult(): FullscreenChartResultState {
    val source by savedStateHandle
        .getStateFlow<String?>(FullscreenChartResult.KEY_SOURCE, null)
        .collectAsStateWithLifecycle()
    val metric by savedStateHandle
        .getStateFlow<String?>(FullscreenChartResult.KEY_METRIC, null)
        .collectAsStateWithLifecycle()
    val period by savedStateHandle
        .getStateFlow<String?>(FullscreenChartResult.KEY_PERIOD, null)
        .collectAsStateWithLifecycle()
    return FullscreenChartResultState(source, metric, period)
}

private fun NavBackStackEntry.consumeFullscreenChartResult() {
    savedStateHandle.remove<String>(FullscreenChartResult.KEY_SOURCE)
    savedStateHandle.remove<String>(FullscreenChartResult.KEY_METRIC)
    savedStateHandle.remove<String>(FullscreenChartResult.KEY_PERIOD)
}

@Composable
private fun ProRouteGate(
    route: String,
    proAccessReady: Boolean,
    isPro: Boolean,
    onRedirect: () -> Unit,
    content: @Composable () -> Unit,
) {
    val currentOnRedirect by rememberUpdatedState(onRedirect)
    when (resolveProRoute(route, proAccessReady, isPro)) {
        route -> {
            content()
        }

        Screen.ProUpgrade.route -> {
            LaunchedEffect(route) { currentOnRedirect() }
        }

        null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

internal fun resolveProRoute(
    route: String,
    proAccessReady: Boolean,
    isPro: Boolean,
): String? =
    when {
        route !in proOnlyRoutes -> route
        !proAccessReady -> null
        !isPro -> Screen.ProUpgrade.route
        else -> route
    }

private val proOnlyRoutes = setOf(Screen.Charger.route, Screen.AppUsage.route)

private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

private fun NavHostController.navigateNotificationRoute(route: String) {
    if (currentBackStackEntry?.destination?.route == route) {
        return
    }
    navigate(route) {
        launchSingleTop = true
        popUpTo(graph.startDestinationId)
    }
}

private fun NavHostController.redirectToProUpgrade(restrictedRoute: String) {
    navigate(Screen.ProUpgrade.route) {
        launchSingleTop = true
        popUpTo(restrictedRoute) {
            inclusive = true
        }
    }
}

private fun NavHostController.navigateNested(
    parentRoute: String,
    childRoute: String,
) {
    if (currentBackStackEntry?.destination?.route == childRoute) {
        return
    }
    navigateSingleTop(parentRoute)
    navigateSingleTop(childRoute)
}
