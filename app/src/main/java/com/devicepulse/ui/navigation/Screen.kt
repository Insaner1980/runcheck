package com.devicepulse.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Battery : Screen("battery")
    data object Network : Screen("network")
    data object SpeedTest : Screen("speed_test")
    data object Thermal : Screen("thermal")
    data object Storage : Screen("storage")
    data object Settings : Screen("settings")
    data object Charger : Screen("charger")
    data object AppUsage : Screen("app_usage")
}
