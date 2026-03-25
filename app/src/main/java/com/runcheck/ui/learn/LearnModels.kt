package com.runcheck.ui.learn

import androidx.annotation.StringRes
import com.runcheck.R

enum class LearnTopic(@param:StringRes val labelRes: Int) {
    BATTERY(R.string.learn_topic_battery),
    TEMPERATURE(R.string.learn_topic_temperature),
    NETWORK(R.string.learn_topic_network),
    STORAGE(R.string.learn_topic_storage),
    GENERAL(R.string.learn_topic_general)
}

object LearnArticleIds {
    const val BATTERY_HEALTH = "battery_health"
    const val BATTERY_DRAIN = "battery_drain"
    const val BATTERY_CHARGING = "battery_charging"
    const val BATTERY_CURRENT_POWER = "battery_current_power"
    const val THERMAL_NORMAL_TEMPS = "thermal_normal_temps"
    const val THERMAL_THROTTLING = "thermal_throttling"
    const val THERMAL_FEEDBACK = "thermal_feedback"
    const val NETWORK_SIGNAL = "network_signal"
    const val NETWORK_WIFI_BANDS = "network_wifi_bands"
    const val NETWORK_SPEED_TESTS = "network_speed_tests"
    const val STORAGE_SLOWDOWN = "storage_slowdown"
    const val STORAGE_BREAKDOWN = "storage_breakdown"
    const val HEALTH_SCORE = "health_score"
    const val SOFTWARE_VS_HARDWARE = "sw_vs_hw"
}

data class LearnArticle(
    val id: String,
    val topic: LearnTopic,
    @param:StringRes val titleRes: Int,
    @param:StringRes val previewRes: Int,
    @param:StringRes val bodyRes: Int,
    val readTimeMinutes: Int,
    val crossLinkRoute: String?,
    val legacyIds: Set<String> = emptySet()
)

data class LearnTopicSection(
    val topic: LearnTopic,
    val articles: List<LearnArticle>
)
