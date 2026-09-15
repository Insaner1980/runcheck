package com.runcheck.domain.model

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
    val plugType: String,
)

fun ChargingSession.reconstructedAveragePowerMw(): Int? {
    val averageCurrentMa = avgCurrentMa ?: return null
    val averageVoltageMv = avgVoltageMv ?: return null
    val powerMw = averageCurrentMa.toLong() * averageVoltageMv.toLong() / 1000L

    return powerMw
        .takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }
        ?.toInt()
}
