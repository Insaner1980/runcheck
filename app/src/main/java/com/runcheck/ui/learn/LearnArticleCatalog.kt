package com.runcheck.ui.learn

import com.runcheck.R
import com.runcheck.ui.navigation.Screen

object LearnArticleCatalog {
    private val topicOrder =
        listOf(
            LearnTopic.BATTERY,
            LearnTopic.TEMPERATURE,
            LearnTopic.NETWORK,
            LearnTopic.STORAGE,
            LearnTopic.GENERAL,
        )

    val articles: List<LearnArticle> =
        listOf(
            LearnArticle(
                id = LearnArticleIds.BATTERY_HEALTH,
                topic = LearnTopic.BATTERY,
                titleRes = R.string.learn_battery_health_title,
                previewRes = R.string.learn_battery_health_preview,
                bodyRes = R.string.learn_battery_health_body,
                readTimeMinutes = 3,
                crossLinkRoute = Screen.Battery.route,
            ),
            LearnArticle(
                id = LearnArticleIds.BATTERY_DRAIN,
                topic = LearnTopic.BATTERY,
                titleRes = R.string.learn_battery_drain_title,
                previewRes = R.string.learn_battery_drain_preview,
                bodyRes = R.string.learn_battery_drain_body,
                readTimeMinutes = 3,
                crossLinkRoute = Screen.Battery.route,
            ),
            LearnArticle(
                id = LearnArticleIds.BATTERY_CHARGING,
                topic = LearnTopic.BATTERY,
                titleRes = R.string.learn_battery_charging_title,
                previewRes = R.string.learn_battery_charging_preview,
                bodyRes = R.string.learn_battery_charging_body,
                readTimeMinutes = 3,
                crossLinkRoute = Screen.Battery.route,
            ),
            LearnArticle(
                id = LearnArticleIds.BATTERY_CURRENT_POWER,
                topic = LearnTopic.BATTERY,
                titleRes = R.string.learn_battery_current_power_title,
                previewRes = R.string.learn_battery_current_power_preview,
                bodyRes = R.string.learn_battery_current_power_body,
                readTimeMinutes = 3,
                crossLinkRoute = Screen.Battery.route,
            ),
            LearnArticle(
                id = LearnArticleIds.THERMAL_NORMAL_TEMPS,
                topic = LearnTopic.TEMPERATURE,
                titleRes = R.string.learn_thermal_normal_temps_title,
                previewRes = R.string.learn_thermal_normal_temps_preview,
                bodyRes = R.string.learn_thermal_normal_temps_body,
                readTimeMinutes = 2,
                crossLinkRoute = Screen.Thermal.route,
            ),
            LearnArticle(
                id = LearnArticleIds.THERMAL_THROTTLING,
                topic = LearnTopic.TEMPERATURE,
                titleRes = R.string.learn_thermal_throttling_title,
                previewRes = R.string.learn_thermal_throttling_preview,
                bodyRes = R.string.learn_thermal_throttling_body,
                readTimeMinutes = 3,
                crossLinkRoute = Screen.Thermal.route,
            ),
            LearnArticle(
                id = LearnArticleIds.THERMAL_FEEDBACK,
                topic = LearnTopic.TEMPERATURE,
                titleRes = R.string.learn_thermal_feedback_title,
                previewRes = R.string.learn_thermal_feedback_preview,
                bodyRes = R.string.learn_thermal_feedback_body,
                readTimeMinutes = 2,
                crossLinkRoute = Screen.Thermal.route,
            ),
            LearnArticle(
                id = LearnArticleIds.NETWORK_SIGNAL,
                topic = LearnTopic.NETWORK,
                titleRes = R.string.learn_network_signal_title,
                previewRes = R.string.learn_network_signal_preview,
                bodyRes = R.string.learn_network_signal_body,
                readTimeMinutes = 3,
                crossLinkRoute = Screen.Network.route,
            ),
            LearnArticle(
                id = LearnArticleIds.NETWORK_WIFI_BANDS,
                topic = LearnTopic.NETWORK,
                titleRes = R.string.learn_network_wifi_bands_title,
                previewRes = R.string.learn_network_wifi_bands_preview,
                bodyRes = R.string.learn_network_wifi_bands_body,
                readTimeMinutes = 3,
                crossLinkRoute = Screen.Network.route,
            ),
            LearnArticle(
                id = LearnArticleIds.NETWORK_SPEED_TESTS,
                topic = LearnTopic.NETWORK,
                titleRes = R.string.learn_network_speed_tests_title,
                previewRes = R.string.learn_network_speed_tests_preview,
                bodyRes = R.string.learn_network_speed_tests_body,
                readTimeMinutes = 2,
                crossLinkRoute = Screen.Network.route,
            ),
            LearnArticle(
                id = LearnArticleIds.STORAGE_SLOWDOWN,
                topic = LearnTopic.STORAGE,
                titleRes = R.string.learn_storage_slowdown_title,
                previewRes = R.string.learn_storage_slowdown_preview,
                bodyRes = R.string.learn_storage_slowdown_body,
                readTimeMinutes = 2,
                crossLinkRoute = Screen.Storage.route,
            ),
            LearnArticle(
                id = LearnArticleIds.STORAGE_BREAKDOWN,
                topic = LearnTopic.STORAGE,
                titleRes = R.string.learn_storage_breakdown_title,
                previewRes = R.string.learn_storage_breakdown_preview,
                bodyRes = R.string.learn_storage_breakdown_body,
                readTimeMinutes = 2,
                crossLinkRoute = Screen.Storage.route,
            ),
            LearnArticle(
                id = LearnArticleIds.HEALTH_SCORE,
                topic = LearnTopic.GENERAL,
                titleRes = R.string.learn_health_score_title,
                previewRes = R.string.learn_health_score_preview,
                bodyRes = R.string.learn_health_score_body,
                readTimeMinutes = 3,
                crossLinkRoute = null,
            ),
            LearnArticle(
                id = LearnArticleIds.SOFTWARE_VS_HARDWARE,
                topic = LearnTopic.GENERAL,
                titleRes = R.string.learn_sw_vs_hw_title,
                previewRes = R.string.learn_sw_vs_hw_preview,
                bodyRes = R.string.learn_sw_vs_hw_body,
                readTimeMinutes = 3,
                crossLinkRoute = null,
            ),
            LearnArticle(
                id = LearnArticleIds.BACKGROUND_MONITORING,
                topic = LearnTopic.GENERAL,
                titleRes = R.string.learn_background_monitoring_title,
                previewRes = R.string.learn_background_monitoring_preview,
                bodyRes = R.string.learn_background_monitoring_body,
                readTimeMinutes = 4,
                crossLinkRoute = Screen.Settings.route,
            ),
        )

    val sections: List<LearnTopicSection> =
        topicOrder.mapNotNull { topic ->
            articles
                .filter { it.topic == topic }
                .takeIf { it.isNotEmpty() }
                ?.let { topicArticles -> LearnTopicSection(topic = topic, articles = topicArticles) }
        }

    init {
        articles.forEach { article ->
            check(
                article.crossLinkRoute == null ||
                    Screen.isValidLearnCrossLinkRoute(article.crossLinkRoute),
            ) {
                "Unknown learn cross-link route for ${article.id}: ${article.crossLinkRoute}"
            }
            article.legacyIds.forEach { legacyId ->
                check(legacyId != article.id) {
                    "Legacy article id must differ from canonical id for ${article.id}"
                }
            }
        }
        check(sections.flatMap(LearnTopicSection::articles) == articles) {
            "Learn topic sections are out of sync with the article catalog"
        }
    }

    private val articlesById: Map<String, LearnArticle> =
        buildMap {
            articles.forEach { article ->
                check(put(article.id, article) == null) {
                    "Duplicate learn article id: ${article.id}"
                }
            }
        }

    private val legacyAliases: Map<String, String> =
        buildMap {
            articles.forEach { article ->
                article.legacyIds.forEach { legacyId ->
                    check(put(legacyId, article.id) == null) {
                        "Duplicate legacy learn article id: $legacyId"
                    }
                }
            }
        }

    private val articlesByTopic: Map<LearnTopic, List<LearnArticle>> =
        sections.associate { section -> section.topic to section.articles }

    fun resolveArticleId(id: String): String? =
        when {
            id in articlesById -> id
            else -> legacyAliases[id]?.takeIf { alias -> alias in articlesById }
        }

    fun findById(id: String): LearnArticle? = resolveArticleId(id)?.let(articlesById::get)

    fun containsId(id: String): Boolean = resolveArticleId(id) != null

    fun articlesForTopic(topic: LearnTopic): List<LearnArticle> = articlesByTopic[topic].orEmpty()

    fun findAllByIds(ids: Iterable<String>): List<LearnArticle> =
        ids.mapNotNull(::findById).distinctBy(LearnArticle::id)
}
