package com.devicepulse.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devicepulse.ui.appusage.AppUsageScreen
import com.devicepulse.ui.battery.BatteryDetailScreen
import com.devicepulse.ui.charger.ChargerComparisonScreen
import com.devicepulse.ui.dashboard.DashboardScreen
import com.devicepulse.ui.home.HomeScreen
import com.devicepulse.ui.network.NetworkDetailScreen
import com.devicepulse.ui.network.SpeedTestScreen
import com.devicepulse.ui.settings.SettingsScreen
import com.devicepulse.ui.storage.StorageDetailScreen
import com.devicepulse.ui.thermal.ThermalDetailScreen

@Composable
fun DevicePulseNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val topLevelRoutes = setOf(
        Screen.Home.route,
        Screen.Health.route,
        Screen.Network.route,
        Screen.More.route
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
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
                    onNavigateToHealth = {
                        navController.navigate(Screen.Health.route)
                    },
                    onNavigateToNetwork = {
                        navController.navigate(Screen.Network.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToMore = {
                        navController.navigate(Screen.More.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Health.route) {
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
            composable(Screen.Network.route) {
                NetworkDetailScreen(
                    onNavigateToSpeedTest = { navController.navigate(Screen.SpeedTest.route) }
                )
            }
            composable(Screen.More.route) {
                MoreScreen(
                    onNavigateToThermal = { navController.navigate(Screen.Thermal.route) },
                    onNavigateToStorage = { navController.navigate(Screen.Storage.route) },
                    onNavigateToCharger = { navController.navigate(Screen.Charger.route) },
                    onNavigateToAppUsage = { navController.navigate(Screen.AppUsage.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Battery.route) {
                BatteryDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Thermal.route) {
                ThermalDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Storage.route) {
                StorageDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Charger.route) {
                ChargerComparisonScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.AppUsage.route) {
                AppUsageScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SpeedTest.route) {
                SpeedTestScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
