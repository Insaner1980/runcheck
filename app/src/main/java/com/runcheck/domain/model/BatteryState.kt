package com.runcheck.domain.model

data class BatteryState(
    val level: Int,
    val voltageMv: Int,
    val temperatureC: Float,
    val currentMa: MeasuredValue<Int>,
    val chargingStatus: ChargingStatus,
    val plugType: PlugType,
    val health: BatteryHealth,
    val technology: String,
    val cycleCount: Int? = null,
    val healthPercent: Int? = null
)
