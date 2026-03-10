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
import com.devicepulse.ui.dashboard.DashboardScreen
import com.devicepulse.ui.home.HomeScreen
import com.devicepulse.ui.network.NetworkDetailScreen
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
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route)
                },
                onNavigateToBattery = {
                    navController.navigate(Screen.Battery.route)
                },
                onNavigateToNetwork = {
                    navController.navigate(Screen.Network.route)
                },
                onNavigateToThermal = {
                    navController.navigate(Screen.Thermal.route)
                },
                onNavigateToCharger = {
                    navController.navigate(Screen.Charger.route)
                },
                onNavigateToAppUsage = {
                    navController.navigate(Screen.AppUsage.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToBattery = {
                    navController.navigate(Screen.Battery.route)
                },
                onNavigateToNetwork = {
                    navController.navigate(Screen.Network.route)
                },
                onNavigateToThermal = {
                    navController.navigate(Screen.Thermal.route)
                },
                onNavigateToStorage = {
                    navController.navigate(Screen.Storage.route)
                }
            )
        }
        composable(Screen.Battery.route) {
            BatteryDetailScreen()
        }
        composable(Screen.Network.route) {
            NetworkDetailScreen()
        }
        composable(Screen.Thermal.route) {
            ThermalDetailScreen()
        }
        composable(Screen.Storage.route) {
            StorageDetailScreen()
        }
        composable(Screen.Charger.route) {
            ChargerComparisonScreen()
        }
        composable(Screen.AppUsage.route) {
            AppUsageScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
