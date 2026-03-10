package com.devicepulse.domain.model

data class StorageState(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val usagePercent: Float,
    val appsBytes: Long,
    val mediaBytes: Long,
    val sdCardAvailable: Boolean = false,
    val sdCardTotalBytes: Long? = null,
    val sdCardAvailableBytes: Long? = null,
    val fillRateEstimate: String? = null
)
