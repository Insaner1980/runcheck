package com.runcheck.ui.storage.cleanup

import androidx.annotation.StringRes
import com.runcheck.R

enum class CleanupType {
    LARGE_FILES,
    OLD_DOWNLOADS,
    APK_FILES;

    @get:StringRes val titleRes: Int get() = when (this) {
        LARGE_FILES -> R.string.cleanup_large_files_title
        OLD_DOWNLOADS -> R.string.cleanup_old_downloads_title
        APK_FILES -> R.string.cleanup_apk_files_title
    }

    val filterOptions: List<FilterOption> get() = when (this) {
        LARGE_FILES -> listOf(
            FilterOption(R.string.cleanup_filter_10mb, 10L * 1024 * 1024),
            FilterOption(R.string.cleanup_filter_50mb, 50L * 1024 * 1024),
            FilterOption(R.string.cleanup_filter_100mb, 100L * 1024 * 1024),
            FilterOption(R.string.cleanup_filter_500mb, 500L * 1024 * 1024)
        )
        OLD_DOWNLOADS -> listOf(
            FilterOption(R.string.cleanup_filter_30d, 30L * 86_400_000),
            FilterOption(R.string.cleanup_filter_60d, 60L * 86_400_000),
            FilterOption(R.string.cleanup_filter_90d, 90L * 86_400_000),
            FilterOption(R.string.cleanup_filter_1y, 365L * 86_400_000)
        )
        APK_FILES -> emptyList()
    }

    val defaultFilterIndex: Int get() = when (this) {
        LARGE_FILES -> 1  // 50 MB
        OLD_DOWNLOADS -> 0  // 30 days
        APK_FILES -> 0
    }

    val preselectAll: Boolean get() = this == APK_FILES
}

data class FilterOption(
    @param:StringRes val labelRes: Int,
    val value: Long
)
