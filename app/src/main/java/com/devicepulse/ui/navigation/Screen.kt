package com.devicepulse.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Battery : Screen("battery")
    data object Network : Screen("network")
    data object Thermal : Screen("thermal")
    data object Storage : Screen("storage")
    data object Settings : Screen("settings")
}
