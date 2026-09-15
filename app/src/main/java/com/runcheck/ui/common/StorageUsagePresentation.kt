package com.runcheck.ui.common

import com.runcheck.domain.model.HealthStatus

/** Storage-used presentation severity; independent of scoring, alerts, and measurement normalization. */
object StorageUsagePresentation {
    const val FAIR_START_PERCENT = 75f
    const val POOR_START_PERCENT = 85f
    const val CRITICAL_START_PERCENT = 95f

    fun classify(usedPercent: Float): HealthStatus =
        when {
            usedPercent >= CRITICAL_START_PERCENT -> HealthStatus.CRITICAL
            usedPercent >= POOR_START_PERCENT -> HealthStatus.POOR
            usedPercent >= FAIR_START_PERCENT -> HealthStatus.FAIR
            else -> HealthStatus.HEALTHY
        }
}
