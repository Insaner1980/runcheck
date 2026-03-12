package com.devicepulse.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devicepulse.ui.appusage.AppUsageScreen
import com.devicepulse.ui.battery.BatteryDetailScreen
import com.devicepulse.ui.charger.ChargerComparisonScreen
import com.devicepulse.ui.home.HomeScreen
import com.devicepulse.ui.network.NetworkDetailScreen
import com.devicepulse.ui.network.SpeedTestScreen
import com.devicepulse.ui.settings.SettingsScreen
import com.devicepulse.ui.storage.StorageDetailScreen
import com.devicepulse.ui.thermal.ThermalDetailScreen

@Composable
fun DevicePulseNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToBattery = { navController.navigate(Screen.Battery.route) },
                onNavigateToNetwork = { navController.navigate(Screen.Network.route) },
                onNavigateToThermal = { navController.navigate(Screen.Thermal.route) },
                onNavigateToStorage = { navController.navigate(Screen.Storage.route) },
                onNavigateToCharger = { navController.navigate(Screen.Charger.route) },
                onNavigateToSpeedTest = { navController.navigate(Screen.SpeedTest.route) },
                onNavigateToAppUsage = { navController.navigate(Screen.AppUsage.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProUpgrade = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Battery.route) {
            BatteryDetailScreen(
                onBack = { navController.popBackStack() },
                onUpgradeToPro = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Network.route) {
            NetworkDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSpeedTest = { navController.navigate(Screen.SpeedTest.route) }
            )
        }
        composable(Screen.Thermal.route) {
            ThermalDetailScreen(
                onBack = { navController.popBackStack() },
                onUpgradeToPro = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Storage.route) {
            StorageDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Charger.route) {
            ChargerComparisonScreen(
                onBack = { navController.popBackStack() },
                onUpgradeToPro = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.AppUsage.route) {
            AppUsageScreen(
                onBack = { navController.popBackStack() },
                onUpgradeToPro = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SpeedTest.route) {
            SpeedTestScreen(onBack = { navController.popBackStack() })
        }
    }
}
