package com.runcheck.domain.insights.model

/** Stable persisted message keys, including messages retained only for historical rows. */
enum class InsightMessageId(
    val titleKey: String,
    val bodyKey: String,
) {
    BATTERY_DEGRADATION(
        "insight_battery_degradation_title",
        "insight_battery_degradation_body",
    ),
    BATTERY_BASELINE_ANOMALY(
        "insight_battery_baseline_anomaly_title",
        "insight_battery_baseline_anomaly_body",
    ),
    CHARGER_PERFORMANCE(
        "insight_charger_performance_title",
        "insight_charger_performance_body",
    ),
    HEAVY_APP_USAGE(
        "insight_app_usage_title",
        "insight_app_usage_body",
    ),
    NETWORK_SIGNAL_PATTERN(
        "insight_network_signal_pattern_title",
        "insight_network_signal_pattern_body",
    ),
    NETWORK_DRIVEN_BATTERY_DRAIN(
        "insight_network_drain_title",
        "insight_network_drain_body",
    ),
    HEAT_ACCELERATED_BATTERY_WEAR(
        "insight_heat_battery_wear_title",
        "insight_heat_battery_wear_body",
    ),
    STORAGE_PRESSURE_PROJECTION(
        "insight_storage_pressure_title",
        "insight_storage_pressure_body",
    ),
    STORAGE_PRESSURE_IMPACT(
        "insight_storage_impact_title",
        "insight_storage_impact_body",
    ),
    RECURRING_THERMAL_THROTTLING(
        "insight_thermal_throttling_title",
        "insight_thermal_throttling_body",
    ),
    THERMAL_PATTERN(
        "insight_thermal_pattern_title",
        "insight_thermal_pattern_body",
    ),
    LEGACY_APP_BATTERY_IMPACT(
        "insight_app_battery_impact_title",
        "insight_app_battery_impact_body",
    ),
    ;

    companion object {
        fun fromKeys(
            titleKey: String,
            bodyKey: String,
        ): InsightMessageId? = entries.firstOrNull { it.titleKey == titleKey && it.bodyKey == bodyKey }
    }
}
