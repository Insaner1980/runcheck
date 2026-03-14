package com.runcheck.domain.model

data class ChargerSummary(
    val chargerId: Long,
    val chargerName: String,
    val sessionCount: Int,
    val avgChargingSpeedMa: Int?,
    val avgTimeToFullMinutes: Int?,
    val lastUsed: Long?
)
