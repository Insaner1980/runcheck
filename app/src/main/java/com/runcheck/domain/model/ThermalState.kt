package com.runcheck.domain.model

data class ThermalState(
    val batteryTempC: Float,
    val cpuTempC: Float? = null,
    val thermalHeadroom: Float? = null,
    val thermalStatus: ThermalStatus,
    val isThrottling: Boolean,
)
