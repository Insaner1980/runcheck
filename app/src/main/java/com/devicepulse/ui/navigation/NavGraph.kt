package com.devicepulse.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devicepulse.ui.battery.BatteryDetailScreen
import com.devicepulse.ui.dashboard.DashboardScreen
import com.devicepulse.ui.network.NetworkDetailScreen
import com.devicepulse.ui.settings.SettingsScreen
import com.devicepulse.ui.storage.StorageDetailScreen
import com.devicepulse.ui.thermal.ThermalDetailScreen

@Composable
fun DevicePulseNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showMoreMenu by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            Box {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route == "more") {
                            showMoreMenu = true
                        } else {
                            navController.navigate(route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
                MoreMenu(
                    expanded = showMoreMenu,
                    onDismiss = { showMoreMenu = false },
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
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
            startDestination = Screen.Dashboard.route,
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
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
