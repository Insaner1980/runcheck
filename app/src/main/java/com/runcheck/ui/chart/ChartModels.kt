package com.runcheck.ui.chart

enum class BatteryHistoryMetric {
    LEVEL,
    TEMPERATURE,
    CURRENT,
    VOLTAGE,
}

enum class SessionGraphMetric {
    CURRENT,
    POWER,
}

enum class SessionGraphWindow(
    val durationMs: Long?,
) {
    FIFTEEN_MINUTES(15 * 60_000L),
    THIRTY_MINUTES(30 * 60_000L),
    ALL(null),
}

enum class NetworkHistoryMetric {
    SIGNAL,
    LATENCY,
}

enum class ThermalHistoryMetric {
    BATTERY_TEMP,
    CPU_TEMP,
}

enum class StorageHistoryMetric {
    USED_SPACE,
    AVAILABLE_SPACE,
}

enum class FullscreenChartSource {
    BATTERY_HISTORY,
    BATTERY_SESSION,
    NETWORK_HISTORY,
}

data class ChargingSessionSummary(
    val startLevel: Int,
    val gainPercent: Int,
    val durationMs: Long,
    val peakTemperatureC: Float,
    val averageCurrentMa: Int?,
    val deliveredMah: Int?,
    val averagePowerW: Float?,
    val averageSpeedPctPerHour: Float?,
    val recentSpeedPctPerHour: Float?,
    val remainingTo80Ms: Long?,
    val remainingTo100Ms: Long?,
    val readings: List<com.runcheck.domain.model.BatteryReading>,
)

const val MAX_HISTORY_CHART_POINTS = 300
const val MAX_SESSION_CHART_POINTS = 240
const val MAX_NETWORK_HISTORY_POINTS = 300
const val MAX_THERMAL_HISTORY_POINTS = 300
const val MAX_STORAGE_HISTORY_POINTS = 300
const val MAX_FULLSCREEN_CHART_POINTS = 600
const val MAX_FULLSCREEN_SESSION_POINTS = 480
