package com.devicepulse.domain.model

data class AppBatteryUsage(
    val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val appLabel: String,
    val foregroundTimeMs: Long,
    val estimatedDrainMah: Float?
)
