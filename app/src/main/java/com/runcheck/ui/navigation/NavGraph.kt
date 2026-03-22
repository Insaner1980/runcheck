package com.runcheck.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.runcheck.ui.appusage.AppUsageScreen
import com.runcheck.ui.battery.BatteryDetailScreen
import com.runcheck.ui.charger.ChargerComparisonScreen
import com.runcheck.ui.home.HomeScreen
import com.runcheck.ui.network.NetworkDetailScreen
import com.runcheck.ui.network.SpeedTestScreen
import com.runcheck.ui.pro.ProUpgradeScreen
import com.runcheck.ui.settings.SettingsScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.runcheck.ui.fullscreen.FullscreenChartResult
import com.runcheck.ui.fullscreen.FullscreenChartScreen
import com.runcheck.ui.learn.LearnArticleDetailScreen
import com.runcheck.ui.learn.LearnScreen
import com.runcheck.ui.storage.StorageDetailScreen
import com.runcheck.ui.storage.cleanup.CleanupScreen
import com.runcheck.ui.thermal.ThermalDetailScreen
import com.runcheck.ui.theme.LocalReducedMotion

@Composable
fun RuncheckNavHost(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val reducedMotion = LocalReducedMotion.current

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
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            }
        },
        exitTransition = {
            if (reducedMotion) {
                ExitTransition.None
            } else {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        },
        popEnterTransition = {
            if (reducedMotion) {
                EnterTransition.None
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            }
        },
        popExitTransition = {
            if (reducedMotion) {
                ExitTransition.None
            } else {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToBattery = { navController.navigateSingleTop(Screen.Battery.route) },
                onNavigateToNetwork = { navController.navigateSingleTop(Screen.Network.route) },
                onNavigateToThermal = { navController.navigateSingleTop(Screen.Thermal.route) },
                onNavigateToStorage = { navController.navigateSingleTop(Screen.Storage.route) },
                onNavigateToCharger = {
                    navController.navigateNested(
                        parentRoute = Screen.Battery.route,
                        childRoute = Screen.Charger.route
                    )
                },
                onNavigateToSpeedTest = {
                    navController.navigateNested(
                        parentRoute = Screen.Network.route,
                        childRoute = Screen.SpeedTest.route
                    )
                },
                onNavigateToAppUsage = { navController.navigateSingleTop(Screen.AppUsage.route) },
                onNavigateToSettings = { navController.navigateSingleTop(Screen.Settings.route) },
                onNavigateToProUpgrade = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                onNavigateToLearn = { navController.navigateSingleTop(Screen.Learn.route) }
            )
        }
        composable(Screen.Battery.route) { entry ->
            val resultSource by entry.savedStateHandle
                .getStateFlow<String?>(FullscreenChartResult.KEY_SOURCE, null)
                .collectAsStateWithLifecycle()
            val resultMetric by entry.savedStateHandle
                .getStateFlow<String?>(FullscreenChartResult.KEY_METRIC, null)
                .collectAsStateWithLifecycle()
            val resultPeriod by entry.savedStateHandle
                .getStateFlow<String?>(FullscreenChartResult.KEY_PERIOD, null)
                .collectAsStateWithLifecycle()
            BatteryDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCharger = { navController.navigateSingleTop(Screen.Charger.route) },
                onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                onNavigateToFullscreen = { source, metric, period ->
                    navController.navigateSingleTop(Screen.FullscreenChart(source, metric, period).route)
                },
                onNavigateToLearnArticle = { articleId ->
                    navController.navigateSingleTop(Screen.LearnArticle(articleId).route)
                },
                fullscreenResultSource = resultSource,
                fullscreenResultMetric = resultMetric,
                fullscreenResultPeriod = resultPeriod,
                onFullscreenResultConsumed = {
                    entry.savedStateHandle.remove<String>(FullscreenChartResult.KEY_SOURCE)
                    entry.savedStateHandle.remove<String>(FullscreenChartResult.KEY_METRIC)
                    entry.savedStateHandle.remove<String>(FullscreenChartResult.KEY_PERIOD)
                }
            )
        }
        composable(Screen.Network.route) { entry ->
            val resultSource by entry.savedStateHandle
                .getStateFlow<String?>(FullscreenChartResult.KEY_SOURCE, null)
                .collectAsStateWithLifecycle()
            val resultMetric by entry.savedStateHandle
                .getStateFlow<String?>(FullscreenChartResult.KEY_METRIC, null)
                .collectAsStateWithLifecycle()
            val resultPeriod by entry.savedStateHandle
                .getStateFlow<String?>(FullscreenChartResult.KEY_PERIOD, null)
                .collectAsStateWithLifecycle()
            NetworkDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSpeedTest = { navController.navigateSingleTop(Screen.SpeedTest.route) },
                onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                onNavigateToFullscreen = { source, metric, period ->
                    navController.navigateSingleTop(Screen.FullscreenChart(source, metric, period).route)
                },
                onNavigateToLearnArticle = { articleId ->
                    navController.navigateSingleTop(Screen.LearnArticle(articleId).route)
                },
                fullscreenResultMetric = resultMetric,
                fullscreenResultPeriod = resultPeriod,
                onFullscreenResultConsumed = {
                    entry.savedStateHandle.remove<String>(FullscreenChartResult.KEY_SOURCE)
                    entry.savedStateHandle.remove<String>(FullscreenChartResult.KEY_METRIC)
                    entry.savedStateHandle.remove<String>(FullscreenChartResult.KEY_PERIOD)
                }
            )
        }
        composable(Screen.Thermal.route) {
            ThermalDetailScreen(
                onBack = { navController.popBackStack() },
                onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                onNavigateToLearnArticle = { articleId ->
                    navController.navigateSingleTop(Screen.LearnArticle(articleId).route)
                }
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
                }
            )
        }
        composable(
            route = Screen.Cleanup.ROUTE,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) {
            CleanupScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Charger.route) {
            ChargerComparisonScreen(
                onBack = { navController.popBackStack() },
                onUpgradeToPro = {
                    navController.navigate(Screen.ProUpgrade.route) {
                        launchSingleTop = true
                        popUpTo(Screen.Charger.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(Screen.AppUsage.route) {
            AppUsageScreen(
                onBack = { navController.popBackStack() },
                onUpgradeToPro = {
                    navController.navigate(Screen.ProUpgrade.route) {
                        launchSingleTop = true
                        popUpTo(Screen.AppUsage.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SpeedTest.route) {
            SpeedTestScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ProUpgrade.route) {
            ProUpgradeScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Learn.route) {
            LearnScreen(
                onBack = { navController.popBackStack() },
                onNavigateToArticle = { articleId ->
                    navController.navigateSingleTop(Screen.LearnArticle(articleId).route)
                }
            )
        }
        composable(
            route = Screen.LearnArticle.ROUTE,
            arguments = listOf(navArgument("articleId") { type = NavType.StringType })
        ) {
            val articleId = it.arguments?.getString("articleId") ?: ""
            LearnArticleDetailScreen(
                articleId = articleId,
                onBack = { navController.popBackStack() },
                onNavigateToRoute = { route -> navController.navigateSingleTop(route) }
            )
        }
        composable(
            route = Screen.FullscreenChart.ROUTE,
            arguments = listOf(
                navArgument("source") { type = NavType.StringType },
                navArgument("metric") { type = NavType.StringType },
                navArgument("period") { type = NavType.StringType }
            )
        ) {
            FullscreenChartScreen(
                onBack = { navController.popBackStack() },
                onSelectionChanged = { source, metric, period ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set(FullscreenChartResult.KEY_SOURCE, source)
                        set(FullscreenChartResult.KEY_METRIC, metric)
                        set(FullscreenChartResult.KEY_PERIOD, period)
                    }
                }
            )
        }
    }
}

private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

private fun NavHostController.navigateNested(parentRoute: String, childRoute: String) {
    navigateSingleTop(parentRoute)
    navigateSingleTop(childRoute)
}
