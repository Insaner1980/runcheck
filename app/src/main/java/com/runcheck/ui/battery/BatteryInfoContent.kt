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
}
