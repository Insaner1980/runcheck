package com.runcheck.ui.common

import com.runcheck.domain.model.HealthStatus

/** Presentation only: classify the original Celsius measurement before display conversion or rounding. */
object BatteryTemperaturePresentation {
    const val NORMAL_START_C = 25f
    const val FAIR_START_C = 35f
    const val POOR_START_C = 40f
    const val CRITICAL_START_C = 45f

    enum class Band(
        val severity: HealthStatus,
    ) {
        COOL(HealthStatus.HEALTHY),
        NORMAL(HealthStatus.HEALTHY),
        WARM(HealthStatus.FAIR),
        HOT(HealthStatus.POOR),
        CRITICAL(HealthStatus.CRITICAL),
    }

    fun classify(celsius: Float): Band =
        when {
            celsius >= CRITICAL_START_C -> Band.CRITICAL
            celsius >= POOR_START_C -> Band.HOT
            celsius >= FAIR_START_C -> Band.WARM
            celsius >= NORMAL_START_C -> Band.NORMAL
            else -> Band.COOL
        }
}
