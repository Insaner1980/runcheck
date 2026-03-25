package com.runcheck.ui.storage

import com.runcheck.R
import com.runcheck.ui.components.info.InfoSheetContent

object StorageInfoContent {
    val usagePercent = InfoSheetContent(
        title = R.string.info_storage_usage_title,
        explanation = R.string.info_storage_usage_explanation,
        normalRange = R.string.info_storage_usage_range,
        whyItMatters = R.string.info_storage_usage_matters
    )
    val fillRate = InfoSheetContent(
        title = R.string.info_storage_fill_rate_title,
        explanation = R.string.info_storage_fill_rate_explanation,
        normalRange = R.string.info_storage_fill_rate_range,
        whyItMatters = R.string.info_storage_fill_rate_matters
    )
    val cache = InfoSheetContent(
        title = R.string.info_storage_cache_title,
        explanation = R.string.info_storage_cache_explanation,
        normalRange = R.string.info_storage_cache_range,
        whyItMatters = R.string.info_storage_cache_matters
    )
    val appsTotal = InfoSheetContent(
        title = R.string.info_storage_apps_title,
        explanation = R.string.info_storage_apps_explanation,
        normalRange = R.string.info_storage_apps_range,
        whyItMatters = R.string.info_storage_apps_matters
    )
    val filesystem = InfoSheetContent(
        title = R.string.info_storage_filesystem_title,
        explanation = R.string.info_storage_filesystem_explanation,
        normalRange = R.string.info_storage_filesystem_range,
        whyItMatters = R.string.info_storage_filesystem_matters
    )
    val encryption = InfoSheetContent(
        title = R.string.info_storage_encryption_title,
        explanation = R.string.info_storage_encryption_explanation,
        normalRange = R.string.info_storage_encryption_range,
        whyItMatters = R.string.info_storage_encryption_matters
    )
}
