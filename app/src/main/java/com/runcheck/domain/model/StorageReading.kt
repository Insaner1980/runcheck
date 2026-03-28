package com.runcheck.domain.model

data class StorageReading(
    val timestamp: Long,
    val totalBytes: Long,
    val availableBytes: Long,
    val appsBytes: Long,
    val mediaBytes: Long,
)
