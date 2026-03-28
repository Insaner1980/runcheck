package com.runcheck.domain.model

import androidx.annotation.IntRange

data class BatteryReading(
    val id: Long = 0,
    val timestamp: Long,
    @param:IntRange(from = 0, to = 100) val level: Int,
    val voltageMv: Int,
    val temperatureC: Float,
    val currentMa: Int?,
    val currentConfidence: String,
    val status: String,
    val plugType: String,
    val health: String,
    val cycleCount: Int?,
    @param:IntRange(from = 0, to = 100) val healthPct: Int?,
)
