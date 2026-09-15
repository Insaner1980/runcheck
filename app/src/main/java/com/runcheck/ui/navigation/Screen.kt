package com.runcheck.ui.navigation

sealed class Screen(
    val route: String,
) {
    data object Home : Screen("home")

    data object Insights : Screen("insights")

    data object Battery : Screen("battery")

    data object Network : Screen("network")

    data object SpeedTest : Screen("speed_test")

    data object Thermal : Screen("thermal")

    data object Storage : Screen("storage")

    data object Settings : Screen("settings")

    data object Charger : Screen("charger")

    data object AppUsage : Screen("app_usage")

    data object ProUpgrade : Screen("pro_upgrade")

    data class Cleanup(
        val type: String,
    ) : Screen("cleanup/$type") {
        companion object {
            const val ARG_TYPE = "type"
            const val ROUTE = "cleanup/{$ARG_TYPE}"
        }
    }

    data object Learn : Screen("learn")

    data class LearnArticle(
        val articleId: String,
    ) : Screen("learn/$articleId") {
        companion object {
            const val ARG_ARTICLE_ID = "articleId"
            const val ROUTE = "learn/{$ARG_ARTICLE_ID}"
        }
    }

    data class FullscreenChart(
        val source: String,
        val metric: String,
        val period: String,
    ) : Screen("fullscreen_chart/$source/$metric/$period") {
        companion object {
            const val ARG_SOURCE = "source"
            const val ARG_METRIC = "metric"
            const val ARG_PERIOD = "period"
            const val ROUTE = "fullscreen_chart/{$ARG_SOURCE}/{$ARG_METRIC}/{$ARG_PERIOD}"
        }
    }

    companion object {
        // Routes in this set can be opened directly from external entry points such as
        // notifications without needing additional arguments.
        private val directRoutes: Set<String> by lazy {
            setOf(
                Home.route,
                Insights.route,
                Battery.route,
                Network.route,
                SpeedTest.route,
                Thermal.route,
                Storage.route,
                Settings.route,
                Charger.route,
                AppUsage.route,
                ProUpgrade.route,
                Learn.route,
            )
        }

        val learnCrossLinkRoutes: Set<String>
            get() = directRoutes

        fun isDirectRoute(route: String): Boolean = route in directRoutes

        fun isValidLearnCrossLinkRoute(route: String): Boolean = isDirectRoute(route)
    }
}
