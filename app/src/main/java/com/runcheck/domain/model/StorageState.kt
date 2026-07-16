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
    val removableStorageAvailable: Boolean = false,
    val removableStorageTotalBytes: Long? = null,
    val removableStorageAvailableBytes: Long? = null,
    val fillRateBytesPerDay: Long? = null,
    val fillRateEstimate: String? = null,
    val fileSystemType: String? = null,
    val encryptionStatus: String? = null,
    val storageVolumes: Int = 0,
)
