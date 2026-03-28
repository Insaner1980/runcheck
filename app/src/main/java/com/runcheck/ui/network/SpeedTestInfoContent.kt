package com.runcheck.ui.network

import com.runcheck.R
import com.runcheck.ui.components.info.InfoSheetContent

object SpeedTestInfoContent {
    val download =
        InfoSheetContent(
            title = R.string.info_speedtest_download_title,
            explanation = R.string.info_speedtest_download_explanation,
            normalRange = R.string.info_speedtest_download_range,
            whyItMatters = R.string.info_speedtest_download_matters,
        )
    val upload =
        InfoSheetContent(
            title = R.string.info_speedtest_upload_title,
            explanation = R.string.info_speedtest_upload_explanation,
            normalRange = R.string.info_speedtest_upload_range,
            whyItMatters = R.string.info_speedtest_upload_matters,
        )
    val ping =
        InfoSheetContent(
            title = R.string.info_speedtest_ping_title,
            explanation = R.string.info_speedtest_ping_explanation,
            normalRange = R.string.info_speedtest_ping_range,
            whyItMatters = R.string.info_speedtest_ping_matters,
        )
    val jitter =
        InfoSheetContent(
            title = R.string.info_speedtest_jitter_title,
            explanation = R.string.info_speedtest_jitter_explanation,
            normalRange = R.string.info_speedtest_jitter_range,
            whyItMatters = R.string.info_speedtest_jitter_matters,
        )
}
