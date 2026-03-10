package com.devicepulse.domain.model

data class ThermalState(
    val batteryTempC: Float,
    val cpuTempC: Float? = null,
    val thermalStatus: ThermalStatus,
    val isThrottling: Boolean
)
