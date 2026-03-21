package com.runcheck.domain.model

data class CleanupGroupSummary(
    val category: MediaCategory,
    val itemCount: Int,
    val totalBytes: Long
)

data class CleanupSummary(
    val groups: List<CleanupGroupSummary>,
    val totalCount: Int,
    val totalBytes: Long,
    val maxFileSizeBytes: Long
)

enum class CleanupScanSource {
    LARGE_FILES,
    OLD_DOWNLOADS,
    APK_FILES
}

data class CleanupScanQuery(
    val source: CleanupScanSource,
    val filterValue: Long
)
