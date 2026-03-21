package com.runcheck.ui.thermal

import com.runcheck.R
import com.runcheck.ui.components.info.InfoSheetContent

object ThermalInfoContent {
    val cpuTemp = InfoSheetContent(
        title = R.string.info_thermal_cpu_temp_title,
        explanation = R.string.info_thermal_cpu_temp_explanation,
        normalRange = R.string.info_thermal_cpu_temp_range,
        whyItMatters = R.string.info_thermal_cpu_temp_matters,
        deeperDetail = R.string.info_thermal_cpu_temp_detail
    )
    val thermalHeadroom = InfoSheetContent(
        title = R.string.info_thermal_headroom_title,
        explanation = R.string.info_thermal_headroom_explanation,
        normalRange = R.string.info_thermal_headroom_range,
        whyItMatters = R.string.info_thermal_headroom_matters,
        deeperDetail = R.string.info_thermal_headroom_detail
    )
    val thermalStatus = InfoSheetContent(
        title = R.string.info_thermal_status_title,
        explanation = R.string.info_thermal_status_explanation,
        normalRange = R.string.info_thermal_status_range,
        whyItMatters = R.string.info_thermal_status_matters
    )
    val throttling = InfoSheetContent(
        title = R.string.info_thermal_throttling_title,
        explanation = R.string.info_thermal_throttling_explanation,
        normalRange = R.string.info_thermal_throttling_range,
        whyItMatters = R.string.info_thermal_throttling_matters
    )
}
