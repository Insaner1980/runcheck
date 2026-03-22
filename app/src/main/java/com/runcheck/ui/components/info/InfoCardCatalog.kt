package com.runcheck.ui.components.info

import androidx.annotation.StringRes
import com.runcheck.R

enum class InfoCardScreen {
    BATTERY_DETAIL,
    NETWORK_DETAIL,
    THERMAL_DETAIL,
    STORAGE_DETAIL
}

data class DismissibleInfoCardDefinition(
    val key: String,
    val version: Int,
    val screen: InfoCardScreen,
    @param:StringRes val headlineRes: Int,
    @param:StringRes val bodyRes: Int,
    val learnArticleId: String?,
    val purpose: String
) {
    init {
        require(version > 0) { "Info card version must be positive" }
    }

    val id: String = "${key}_v$version"
}

object InfoCardCatalog {
    val BatteryHealthDegraded = DismissibleInfoCardDefinition(
        key = "battery_health_80",
        version = 2,
        screen = InfoCardScreen.BATTERY_DETAIL,
        headlineRes = R.string.info_card_health_80_headline,
        bodyRes = R.string.info_card_health_80_body,
        learnArticleId = "battery_health",
        purpose = "Explain that battery health below 90% reflects normal capacity loss."
    )

    val BatteryDiesBeforeZero = DismissibleInfoCardDefinition(
        key = "battery_dies_before_zero",
        version = 2,
        screen = InfoCardScreen.BATTERY_DETAIL,
        headlineRes = R.string.info_card_dies_before_zero_headline,
        bodyRes = R.string.info_card_dies_before_zero_body,
        learnArticleId = "battery_health",
        purpose = "Explain why worn batteries can shut down before the gauge reaches 0%."
    )

    val BatteryChargingHabits = DismissibleInfoCardDefinition(
        key = "battery_charging_habits",
        version = 2,
        screen = InfoCardScreen.BATTERY_DETAIL,
        headlineRes = R.string.info_card_charging_habits_headline,
        bodyRes = R.string.info_card_charging_habits_body,
        learnArticleId = "battery_charging",
        purpose = "Teach healthier charging habits while the device is charging."
    )

    val BatteryScreenOffDrain = DismissibleInfoCardDefinition(
        key = "battery_screen_off_drain",
        version = 2,
        screen = InfoCardScreen.BATTERY_DETAIL,
        headlineRes = R.string.info_card_screen_off_drain_headline,
        bodyRes = R.string.info_card_screen_off_drain_body,
        learnArticleId = "battery_drain",
        purpose = "Warn that background activity is causing unusually high screen-off drain."
    )

    val NetworkWeakSignalDrain = DismissibleInfoCardDefinition(
        key = "network_weak_signal_drain",
        version = 2,
        screen = InfoCardScreen.NETWORK_DETAIL,
        headlineRes = R.string.info_card_weak_signal_headline,
        bodyRes = R.string.info_card_weak_signal_body,
        learnArticleId = "network_signal",
        purpose = "Explain that poor cellular or Wi-Fi signal increases radio power usage."
    )

    val NetworkSpeedTestScope = DismissibleInfoCardDefinition(
        key = "network_speed_test_info",
        version = 1,
        screen = InfoCardScreen.NETWORK_DETAIL,
        headlineRes = R.string.info_card_speed_test_info_headline,
        bodyRes = R.string.info_card_speed_test_info_body,
        learnArticleId = "network_speed_tests",
        purpose = "Clarify what a speed test measures and why results vary."
    )

    val ThermalThrottlingExplainer = DismissibleInfoCardDefinition(
        key = "thermal_throttling_explainer",
        version = 1,
        screen = InfoCardScreen.THERMAL_DETAIL,
        headlineRes = R.string.info_card_thermal_throttling_headline,
        bodyRes = R.string.info_card_thermal_throttling_body,
        learnArticleId = "thermal_throttling",
        purpose = "Explain why Android throttles performance to avoid heat damage."
    )

    val ThermalHeatBatteryLoop = DismissibleInfoCardDefinition(
        key = "thermal_heat_battery_loop",
        version = 1,
        screen = InfoCardScreen.THERMAL_DETAIL,
        headlineRes = R.string.info_card_heat_battery_headline,
        bodyRes = R.string.info_card_heat_battery_body,
        learnArticleId = "thermal_feedback",
        purpose = "Explain the feedback loop between high heat and battery aging."
    )

    val StorageFullSlowsPhone = DismissibleInfoCardDefinition(
        key = "storage_full_slow",
        version = 1,
        screen = InfoCardScreen.STORAGE_DETAIL,
        headlineRes = R.string.info_card_full_storage_headline,
        bodyRes = R.string.info_card_full_storage_body,
        learnArticleId = "storage_slowdown",
        purpose = "Explain why storage above roughly 75% can make the phone feel slower."
    )

    val StorageOverview = DismissibleInfoCardDefinition(
        key = "storage_overview",
        version = 1,
        screen = InfoCardScreen.STORAGE_DETAIL,
        headlineRes = R.string.info_card_storage_overview_headline,
        bodyRes = R.string.info_card_storage_overview_body,
        learnArticleId = "storage_breakdown",
        purpose = "Explain how storage is typically distributed across apps, media, and system data."
    )

    val BatteryLiveNotification = DismissibleInfoCardDefinition(
        key = "battery_live_notification",
        version = 1,
        screen = InfoCardScreen.BATTERY_DETAIL,
        headlineRes = R.string.info_card_live_notif_headline,
        bodyRes = R.string.info_card_live_notif_body,
        learnArticleId = null,
        purpose = "Explain the live notification feature and how to configure it in Settings."
    )

    val all = listOf(
        BatteryHealthDegraded,
        BatteryDiesBeforeZero,
        BatteryChargingHabits,
        BatteryScreenOffDrain,
        BatteryLiveNotification,
        NetworkWeakSignalDrain,
        NetworkSpeedTestScope,
        ThermalThrottlingExplainer,
        ThermalHeatBatteryLoop,
        StorageFullSlowsPhone,
        StorageOverview
    )
}
