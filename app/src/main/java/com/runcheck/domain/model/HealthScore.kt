package com.runcheck.domain.model

import androidx.annotation.IntRange

data class HealthScore(
    @param:IntRange(from = 0, to = 100) val overallScore: Int,
    @param:IntRange(from = 0, to = 100) val batteryScore: Int,
    @param:IntRange(from = 0, to = 100) val networkScore: Int,
    @param:IntRange(from = 0, to = 100) val thermalScore: Int,
    @param:IntRange(from = 0, to = 100) val storageScore: Int,
    val status: HealthStatus
) {
    companion object {
        fun statusFromScore(@IntRange(from = 0, to = 100) score: Int): HealthStatus = when {
            score >= 75 -> HealthStatus.HEALTHY
            score >= 50 -> HealthStatus.FAIR
            score >= 25 -> HealthStatus.POOR
            else -> HealthStatus.CRITICAL
        }
    }
}
