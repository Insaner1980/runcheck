package com.runcheck.domain.model


data class StorageState(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val usagePercent: Float,
    val appsBytes: Long? = null,
    val totalCacheBytes: Long? = null,
    val appCount: Int? = null,
    val mediaBreakdown: MediaBreakdown? = null,
    val trashInfo: TrashInfo? = null,
    val sdCardAvailable: Boolean = false,
    val sdCardTotalBytes: Long? = null,
    val sdCardAvailableBytes: Long? = null,
    val fillRateBytesPerDay: Long? = null,
    val fillRateEstimate: String? = null
)
