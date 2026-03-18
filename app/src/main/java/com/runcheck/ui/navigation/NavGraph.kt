package com.runcheck.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                onNavigateToProUpgrade = { navController.navigateSingleTop(Screen.ProUpgrade.route) }
            )
        }
        composable(Screen.Battery.route) {
            BatteryDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCharger = { navController.navigateSingleTop(Screen.Charger.route) },
                onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) }
            )
        }
        composable(Screen.Network.route) {
            NetworkDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSpeedTest = { navController.navigateSingleTop(Screen.SpeedTest.route) }
            )
        }
        composable(Screen.Thermal.route) {
            ThermalDetailScreen(
                onBack = { navController.popBackStack() },
                onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) }
            )
        }
        composable(Screen.Storage.route) {
            StorageDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCleanup = { type ->
                    navController.navigateSingleTop(Screen.Cleanup(type.name).route)
                },
                onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) }
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
                onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) }
            )
        }
        composable(Screen.AppUsage.route) {
            AppUsageScreen(
                onBack = { navController.popBackStack() },
                onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) }
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
