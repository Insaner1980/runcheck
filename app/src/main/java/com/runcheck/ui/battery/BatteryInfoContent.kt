package com.runcheck.ui.battery

import com.runcheck.R
import com.runcheck.ui.components.info.InfoSheetContent

object BatteryInfoContent {
    val voltage = InfoSheetContent(
        title = R.string.info_battery_voltage_title,
        explanation = R.string.info_battery_voltage_explanation,
        normalRange = R.string.info_battery_voltage_range,
        whyItMatters = R.string.info_battery_voltage_matters,
        deeperDetail = R.string.info_battery_voltage_detail
    )
    val temperature = InfoSheetContent(
        title = R.string.info_battery_temperature_title,
        explanation = R.string.info_battery_temperature_explanation,
        normalRange = R.string.info_battery_temperature_range,
        whyItMatters = R.string.info_battery_temperature_matters,
        deeperDetail = R.string.info_battery_temperature_detail
    )
    val healthStatus = InfoSheetContent(
        title = R.string.info_battery_health_status_title,
        explanation = R.string.info_battery_health_status_explanation,
        normalRange = R.string.info_battery_health_status_range,
        whyItMatters = R.string.info_battery_health_status_matters
    )
    val cycleCount = InfoSheetContent(
        title = R.string.info_battery_cycle_count_title,
        explanation = R.string.info_battery_cycle_count_explanation,
        normalRange = R.string.info_battery_cycle_count_range,
        whyItMatters = R.string.info_battery_cycle_count_matters,
        deeperDetail = R.string.info_battery_cycle_count_detail
    )
    val healthPercent = InfoSheetContent(
        title = R.string.info_battery_health_percent_title,
        explanation = R.string.info_battery_health_percent_explanation,
        normalRange = R.string.info_battery_health_percent_range,
        whyItMatters = R.string.info_battery_health_percent_matters
    )
    val capacity = InfoSheetContent(
        title = R.string.info_battery_capacity_title,
        explanation = R.string.info_battery_capacity_explanation,
        normalRange = R.string.info_battery_capacity_range,
        whyItMatters = R.string.info_battery_capacity_matters
    )
    val currentMa = InfoSheetContent(
        title = R.string.info_battery_current_title,
        explanation = R.string.info_battery_current_explanation,
        normalRange = R.string.info_battery_current_range,
        whyItMatters = R.string.info_battery_current_matters,
        deeperDetail = R.string.info_battery_current_detail
    )
    val powerW = InfoSheetContent(
        title = R.string.info_battery_power_title,
        explanation = R.string.info_battery_power_explanation,
        normalRange = R.string.info_battery_power_range,
        whyItMatters = R.string.info_battery_power_matters
    )
    val drainRate = InfoSheetContent(
        title = R.string.info_battery_drain_rate_title,
        explanation = R.string.info_battery_drain_rate_explanation,
        normalRange = R.string.info_battery_drain_rate_range,
        whyItMatters = R.string.info_battery_drain_rate_matters
    )
    val confidence = InfoSheetContent(
        title = R.string.info_battery_confidence_title,
        explanation = R.string.info_battery_confidence_explanation,
        normalRange = R.string.info_battery_confidence_range,
        whyItMatters = R.string.info_battery_confidence_matters
    )
    val screenOnOff = InfoSheetContent(
        title = R.string.info_battery_screen_on_off_title,
        explanation = R.string.info_battery_screen_on_off_explanation,
        normalRange = R.string.info_battery_screen_on_off_range,
        whyItMatters = R.string.info_battery_screen_on_off_matters
    )
    val deepSleep = InfoSheetContent(
        title = R.string.info_battery_deep_sleep_title,
        explanation = R.string.info_battery_deep_sleep_explanation,
        normalRange = R.string.info_battery_deep_sleep_range,
        whyItMatters = R.string.info_battery_deep_sleep_matters
    )
    val remaining = InfoSheetContent(
        title = R.string.info_battery_remaining_title,
        explanation = R.string.info_battery_remaining_explanation,
        normalRange = R.string.info_battery_remaining_range,
        whyItMatters = R.string.info_battery_remaining_matters
    )
    val technology = InfoSheetContent(
        title = R.string.info_battery_technology_title,
        explanation = R.string.info_battery_technology_explanation,
        normalRange = R.string.info_battery_technology_range,
        whyItMatters = R.string.info_battery_technology_matters
    )
    val plugType = InfoSheetContent(
        title = R.string.info_battery_plug_type_title,
        explanation = R.string.info_battery_plug_type_explanation,
        normalRange = R.string.info_battery_plug_type_range,
        whyItMatters = R.string.info_battery_plug_type_matters
    )
    val currentStats = InfoSheetContent(
        title = R.string.info_battery_current_stats_title,
        explanation = R.string.info_battery_current_stats_explanation,
        normalRange = R.string.info_battery_current_stats_range,
        whyItMatters = R.string.info_battery_current_stats_matters
    )
    val statsCharged = InfoSheetContent(
        title = R.string.info_battery_stats_charged_title,
        explanation = R.string.info_battery_stats_charged_explanation,
        normalRange = R.string.info_battery_stats_charged_range,
        whyItMatters = R.string.info_battery_stats_charged_matters
    )
    val statsDischarged = InfoSheetContent(
        title = R.string.info_battery_stats_discharged_title,
        explanation = R.string.info_battery_stats_discharged_explanation,
        normalRange = R.string.info_battery_stats_discharged_range,
        whyItMatters = R.string.info_battery_stats_discharged_matters
    )
    val statsSessions = InfoSheetContent(
        title = R.string.info_battery_stats_sessions_title,
        explanation = R.string.info_battery_stats_sessions_explanation,
        normalRange = R.string.info_battery_stats_sessions_range,
        whyItMatters = R.string.info_battery_stats_sessions_matters
    )
    val statsAvgUsage = InfoSheetContent(
        title = R.string.info_battery_stats_avg_usage_title,
        explanation = R.string.info_battery_stats_avg_usage_explanation,
        normalRange = R.string.info_battery_stats_avg_usage_range,
        whyItMatters = R.string.info_battery_stats_avg_usage_matters
    )
    val statsFullChargeEst = InfoSheetContent(
        title = R.string.info_battery_stats_full_charge_est_title,
        explanation = R.string.info_battery_stats_full_charge_est_explanation,
        normalRange = R.string.info_battery_stats_full_charge_est_range,
        whyItMatters = R.string.info_battery_stats_full_charge_est_matters
    )
}
