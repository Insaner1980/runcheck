package com.runcheck.ui.learn

import com.runcheck.R
import com.runcheck.ui.navigation.Screen

object LearnArticleCatalog {

    val sections: List<LearnTopicSection> = listOf(
        LearnTopicSection(
            topic = LearnTopic.BATTERY,
            articles = listOf(
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
                )
            )
        ),
        LearnTopicSection(
            topic = LearnTopic.TEMPERATURE,
            articles = listOf(
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
                )
            )
        ),
        LearnTopicSection(
            topic = LearnTopic.NETWORK,
            articles = listOf(
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
                )
            )
        ),
        LearnTopicSection(
            topic = LearnTopic.STORAGE,
            articles = listOf(
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
                )
            )
        ),
        LearnTopicSection(
            topic = LearnTopic.GENERAL,
            articles = listOf(
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
        )
    )

    val articles: List<LearnArticle> = sections.flatMap(LearnTopicSection::articles)

    init {
        articles.forEach { article ->
            check(
                article.crossLinkRoute == null ||
                    Screen.isValidLearnCrossLinkRoute(article.crossLinkRoute)
            ) {
                "Unknown learn cross-link route for ${article.id}: ${article.crossLinkRoute}"
            }
        }
    }

    private val articlesById: Map<String, LearnArticle> = buildMap {
        articles.forEach { article ->
            check(put(article.id, article) == null) {
                "Duplicate learn article id: ${article.id}"
            }
        }
    }

    private val articlesByTopic: Map<LearnTopic, List<LearnArticle>> =
        sections.associate { section -> section.topic to section.articles }

    fun findById(id: String): LearnArticle? = articlesById[id]

    fun containsId(id: String): Boolean = id in articlesById

    fun articlesForTopic(topic: LearnTopic): List<LearnArticle> = articlesByTopic[topic].orEmpty()
}
