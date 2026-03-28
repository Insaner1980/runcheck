package com.runcheck.domain.model

data class ThermalReading(
    val timestamp: Long,
    val batteryTempC: Float,
    val cpuTempC: Float?,
    val thermalStatus: Int,
    val throttling: Boolean,
)
