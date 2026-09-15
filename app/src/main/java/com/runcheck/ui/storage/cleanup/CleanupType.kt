package com.runcheck.ui.storage.cleanup

import android.os.Build
import androidx.annotation.StringRes
import com.runcheck.R

private const val BYTES_PER_MEGABYTE = 1_000_000L

enum class CleanupType(
    @get:StringRes val titleRes: Int,
    val filterOptions: List<FilterOption>,
    val defaultFilterIndex: Int,
    val preselectAll: Boolean,
    // Null means no additional platform restriction.
    val minimumSupportedApi: Int?,
) {
    LARGE_FILES(
        titleRes = R.string.cleanup_large_files_title,
        filterOptions =
            listOf(
                FilterOption(R.string.cleanup_filter_10mb, 10L * BYTES_PER_MEGABYTE),
                FilterOption(R.string.cleanup_filter_50mb, 50L * BYTES_PER_MEGABYTE),
                FilterOption(R.string.cleanup_filter_100mb, 100L * BYTES_PER_MEGABYTE),
                FilterOption(R.string.cleanup_filter_500mb, 500L * BYTES_PER_MEGABYTE),
            ),
        defaultFilterIndex = 1,
        preselectAll = false,
        minimumSupportedApi = null,
    ),
    OLD_DOWNLOADS(
        titleRes = R.string.cleanup_old_downloads_title,
        filterOptions =
            listOf(
                FilterOption(R.string.cleanup_filter_30d, 30L * 86_400_000),
                FilterOption(R.string.cleanup_filter_60d, 60L * 86_400_000),
                FilterOption(R.string.cleanup_filter_90d, 90L * 86_400_000),
                FilterOption(R.string.cleanup_filter_1y, 365L * 86_400_000),
            ),
        defaultFilterIndex = 0,
        preselectAll = false,
        minimumSupportedApi = Build.VERSION_CODES.R,
    ),
    APK_FILES(
        titleRes = R.string.cleanup_apk_files_title,
        filterOptions = emptyList(),
        defaultFilterIndex = 0,
        preselectAll = true,
        minimumSupportedApi = Build.VERSION_CODES.R,
    ),
}

data class FilterOption(
    @param:StringRes val labelRes: Int,
    val value: Long,
)
