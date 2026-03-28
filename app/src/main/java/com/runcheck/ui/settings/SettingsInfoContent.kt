package com.runcheck.ui.settings

import com.runcheck.R
import com.runcheck.ui.components.info.InfoSheetContent

object SettingsInfoContent {
    val currentReading =
        InfoSheetContent(
            title = R.string.info_settings_current_reading_title,
            explanation = R.string.info_settings_current_reading_explanation,
            normalRange = R.string.info_settings_current_reading_range,
            whyItMatters = R.string.info_settings_current_reading_matters,
            deeperDetail = R.string.info_settings_current_reading_detail,
        )
    val cycleCount =
        InfoSheetContent(
            title = R.string.info_settings_cycle_count_title,
            explanation = R.string.info_settings_cycle_count_explanation,
            normalRange = R.string.info_settings_cycle_count_range,
            whyItMatters = R.string.info_settings_cycle_count_matters,
        )
    val thermalZones =
        InfoSheetContent(
            title = R.string.info_settings_thermal_zones_title,
            explanation = R.string.info_settings_thermal_zones_explanation,
            normalRange = R.string.info_settings_thermal_zones_range,
            whyItMatters = R.string.info_settings_thermal_zones_matters,
        )
}
