package com.runcheck.ui.navigation

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
    data object ProUpgrade : Screen("pro_upgrade")
    data class Cleanup(val type: String) : Screen("cleanup/$type") {
        companion object {
            const val ROUTE = "cleanup/{type}"
        }
    }
    data object Learn : Screen("learn")
    data class LearnArticle(val articleId: String) : Screen("learn/$articleId") {
        companion object {
            const val ROUTE = "learn/{articleId}"
        }
    }
    data class FullscreenChart(val source: String, val metric: String, val period: String)
        : Screen("fullscreen_chart/$source/$metric/$period") {
        companion object {
            const val ROUTE = "fullscreen_chart/{source}/{metric}/{period}"
        }
    }
}
