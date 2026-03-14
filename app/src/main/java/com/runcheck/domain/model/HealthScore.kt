package com.runcheck.domain.model

data class HealthScore(
    val overallScore: Int,
    val batteryScore: Int,
    val networkScore: Int,
    val thermalScore: Int,
    val storageScore: Int,
    val status: HealthStatus
) {
    companion object {
        fun statusFromScore(score: Int): HealthStatus = when {
            score >= 75 -> HealthStatus.HEALTHY
            score >= 50 -> HealthStatus.FAIR
            score >= 25 -> HealthStatus.POOR
            else -> HealthStatus.CRITICAL
        }
    }
}
