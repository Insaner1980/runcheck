package com.runcheck.domain.model

data class StorageState(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val usagePercent: Float,
    val appsBytes: Long? = null,
    val mediaBytes: Long? = null,
    val sdCardAvailable: Boolean = false,
    val sdCardTotalBytes: Long? = null,
    val sdCardAvailableBytes: Long? = null,
    val fillRateEstimate: String? = null
)
