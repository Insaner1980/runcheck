package com.devicepulse.domain.model

data class BatteryReading(
    val id: Long = 0,
    val timestamp: Long,
    val level: Int,
    val voltageMv: Int,
    val temperatureC: Float,
    val currentMa: Int?,
    val currentConfidence: String,
    val status: String,
    val plugType: String,
    val health: String,
    val cycleCount: Int?,
    val healthPct: Int?
)
