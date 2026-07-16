package com.runcheck.domain.model

data class HealthScore(
    val overallScore: Int,
    val batteryScore: Int,
    val networkScore: Int,
    val thermalScore: Int,
    val storageScore: Int,
    val status: HealthStatus,
    val diagnostics: HealthScoreDiagnostics,
) {
    companion object {
        fun statusFromScore(score: Int): HealthStatus =
            when {
                score >= 75 -> HealthStatus.HEALTHY
                score >= 50 -> HealthStatus.FAIR
                score >= 25 -> HealthStatus.POOR
                else -> HealthStatus.CRITICAL
            }
    }
}

data class HealthScoreDiagnostics(
    val battery: BatteryScoreDiagnostics,
    val network: NetworkScoreDiagnostics,
    val thermal: ThermalScoreDiagnostics,
    val storage: StorageScoreDiagnostics,
)

data class BatteryScoreDiagnostics(
    val healthPenalty: Int,
    val temperaturePenalty: Int,
    val voltagePenalty: Int,
    val capacityPenalty: Int,
)

data class NetworkScoreDiagnostics(
    val mode: NetworkScoreMode,
    val signalScore: Int,
    val liveLatencyPenalty: Int?,
    val speedTestPingScore: Int?,
    val speedTestDownloadScore: Int?,
    val speedTestJitterScore: Int?,
    val speedTestAgeMillis: Long?,
    val speedTestWeightPercent: Int,
)

enum class NetworkScoreMode {
    DISCONNECTED,
    LIVE,
    SPEED_TEST,
    FADING_SPEED_TEST,
}

data class ThermalScoreDiagnostics(
    val batteryTemperaturePenalty: Int,
    val cpuTemperaturePenalty: Int,
    val statusPenalty: Int,
)

data class StorageScoreDiagnostics(
    val usagePenalty: Int,
)
