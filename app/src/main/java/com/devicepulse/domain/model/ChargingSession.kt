package com.devicepulse.domain.model

data class ChargingSession(
    val id: Long = 0,
    val chargerId: Long,
    val startTime: Long,
    val endTime: Long?,
    val startLevel: Int,
    val endLevel: Int?,
    val avgCurrentMa: Int?,
    val maxCurrentMa: Int?,
    val avgVoltageMv: Int?,
    val avgPowerMw: Int?,
    val plugType: String
)
