package com.runcheck.ui.learn

import com.runcheck.R
import com.runcheck.ui.navigation.Screen

object LearnArticleCatalog {

    val articles: List<LearnArticle> = listOf(
        // Battery
        LearnArticle(
            id = "battery_health",
            topic = LearnTopic.BATTERY,
            titleRes = R.string.learn_battery_health_title,
            previewRes = R.string.learn_battery_health_preview,
            bodyRes = R.string.learn_battery_health_body,
            readTimeMinutes = 3,
            crossLinkRoute = Screen.Battery.route
        ),
        LearnArticle(
            id = "battery_drain",
            topic = LearnTopic.BATTERY,
            titleRes = R.string.learn_battery_drain_title,
            previewRes = R.string.learn_battery_drain_preview,
            bodyRes = R.string.learn_battery_drain_body,
            readTimeMinutes = 3,
            crossLinkRoute = Screen.Battery.route
        ),
        LearnArticle(
            id = "battery_charging",
            topic = LearnTopic.BATTERY,
            titleRes = R.string.learn_battery_charging_title,
            previewRes = R.string.learn_battery_charging_preview,
            bodyRes = R.string.learn_battery_charging_body,
            readTimeMinutes = 3,
            crossLinkRoute = Screen.Battery.route
        ),
        LearnArticle(
            id = "battery_current_power",
            topic = LearnTopic.BATTERY,
            titleRes = R.string.learn_battery_current_power_title,
            previewRes = R.string.learn_battery_current_power_preview,
            bodyRes = R.string.learn_battery_current_power_body,
            readTimeMinutes = 3,
            crossLinkRoute = Screen.Battery.route
        ),
        // Temperature
        LearnArticle(
            id = "thermal_normal_temps",
            topic = LearnTopic.TEMPERATURE,
            titleRes = R.string.learn_thermal_normal_temps_title,
            previewRes = R.string.learn_thermal_normal_temps_preview,
            bodyRes = R.string.learn_thermal_normal_temps_body,
            readTimeMinutes = 2,
            crossLinkRoute = Screen.Thermal.route
        ),
        LearnArticle(
            id = "thermal_throttling",
            topic = LearnTopic.TEMPERATURE,
            titleRes = R.string.learn_thermal_throttling_title,
            previewRes = R.string.learn_thermal_throttling_preview,
            bodyRes = R.string.learn_thermal_throttling_body,
            readTimeMinutes = 3,
            crossLinkRoute = Screen.Thermal.route
        ),
        LearnArticle(
            id = "thermal_feedback",
            topic = LearnTopic.TEMPERATURE,
            titleRes = R.string.learn_thermal_feedback_title,
            previewRes = R.string.learn_thermal_feedback_preview,
            bodyRes = R.string.learn_thermal_feedback_body,
            readTimeMinutes = 2,
            crossLinkRoute = Screen.Thermal.route
        ),
        // Network
        LearnArticle(
            id = "network_signal",
            topic = LearnTopic.NETWORK,
            titleRes = R.string.learn_network_signal_title,
            previewRes = R.string.learn_network_signal_preview,
            bodyRes = R.string.learn_network_signal_body,
            readTimeMinutes = 3,
            crossLinkRoute = Screen.Network.route
        ),
        LearnArticle(
            id = "network_wifi_bands",
            topic = LearnTopic.NETWORK,
            titleRes = R.string.learn_network_wifi_bands_title,
            previewRes = R.string.learn_network_wifi_bands_preview,
            bodyRes = R.string.learn_network_wifi_bands_body,
            readTimeMinutes = 3,
            crossLinkRoute = Screen.Network.route
        ),
        LearnArticle(
            id = "network_speed_tests",
            topic = LearnTopic.NETWORK,
            titleRes = R.string.learn_network_speed_tests_title,
            previewRes = R.string.learn_network_speed_tests_preview,
            bodyRes = R.string.learn_network_speed_tests_body,
            readTimeMinutes = 2,
            crossLinkRoute = Screen.Network.route
        ),
        // Storage
        LearnArticle(
            id = "storage_slowdown",
            topic = LearnTopic.STORAGE,
            titleRes = R.string.learn_storage_slowdown_title,
            previewRes = R.string.learn_storage_slowdown_preview,
            bodyRes = R.string.learn_storage_slowdown_body,
            readTimeMinutes = 2,
            crossLinkRoute = Screen.Storage.route
        ),
        LearnArticle(
            id = "storage_breakdown",
            topic = LearnTopic.STORAGE,
            titleRes = R.string.learn_storage_breakdown_title,
            previewRes = R.string.learn_storage_breakdown_preview,
            bodyRes = R.string.learn_storage_breakdown_body,
            readTimeMinutes = 2,
            crossLinkRoute = Screen.Storage.route
        ),
        // General
        LearnArticle(
            id = "health_score",
            topic = LearnTopic.GENERAL,
            titleRes = R.string.learn_health_score_title,
            previewRes = R.string.learn_health_score_preview,
            bodyRes = R.string.learn_health_score_body,
            readTimeMinutes = 3,
            crossLinkRoute = null
        ),
        LearnArticle(
            id = "sw_vs_hw",
            topic = LearnTopic.GENERAL,
            titleRes = R.string.learn_sw_vs_hw_title,
            previewRes = R.string.learn_sw_vs_hw_preview,
            bodyRes = R.string.learn_sw_vs_hw_body,
            readTimeMinutes = 3,
            crossLinkRoute = null
        )
    )

    fun findById(id: String): LearnArticle? = articles.find { it.id == id }
}
