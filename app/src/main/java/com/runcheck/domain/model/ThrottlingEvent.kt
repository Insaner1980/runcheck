package com.runcheck.domain.model

data class ThrottlingEvent(
    val id: Long = 0,
    val timestamp: Long,
    val thermalStatus: String,
    val batteryTempC: Float,
    val cpuTempC: Float?,
    val foregroundApp: String?,
    val durationMs: Long?,
)
