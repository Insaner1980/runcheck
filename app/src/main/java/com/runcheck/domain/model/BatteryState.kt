package com.runcheck.domain.model

import androidx.annotation.IntRange

data class BatteryState(
    @param:IntRange(from = 0, to = 100) val level: Int,
    val voltageMv: Int,
    val temperatureC: Float,
    val currentMa: MeasuredValue<Int>,
    val chargingStatus: ChargingStatus,
    val plugType: PlugType,
    val health: BatteryHealth,
    val technology: String,
    val cycleCount: Int? = null,
    @param:IntRange(from = 0, to = 100) val healthPercent: Int? = null,
    val remainingMah: Int? = null,
    val designCapacityMah: Int? = null,
    val estimatedCapacityMah: Int? = null,
)
