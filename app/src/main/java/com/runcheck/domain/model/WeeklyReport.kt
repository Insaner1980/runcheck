package com.runcheck.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class WeeklyReportPeriod(
    val startInclusive: Instant,
    val endExclusive: Instant,
    val zoneId: ZoneId,
) {
    init {
        require(startInclusive < endExclusive)
    }

    companion object {
        fun previousCompleted(
            now: Instant,
            zoneId: ZoneId,
        ): WeeklyReportPeriod {
            val currentWeekStart =
                now
                    .atZone(zoneId)
                    .toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay(zoneId)
            return WeeklyReportPeriod(
                startInclusive = currentWeekStart.minusWeeks(1).toInstant(),
                endExclusive = currentWeekStart.toInstant(),
                zoneId = zoneId,
            )
        }
    }
}

enum class WeeklyReportAvailability {
    AVAILABLE,
    ESTIMATED,
    UNAVAILABLE,
}

data class WeeklyReportMetric<T>(
    val value: T?,
    val availability: WeeklyReportAvailability,
    val sampleCount: Int,
)

data class WeeklyReportCoverage(
    val monitoredDays: Int,
    val sampleCount: Int,
    val availability: WeeklyReportAvailability,
)

data class WeeklyBatterySummary(
    val averageDischargePercentPerHour: Double?,
    val dischargePercentChange: Double,
    val chargePercentChange: Double,
    val healthPercentChange: Int?,
    val availability: WeeklyReportAvailability,
    val validSegmentCount: Int,
)

data class WeeklyStorageSummary(
    val availableBytesChange: Long?,
    val availability: WeeklyReportAvailability,
)

data class WeeklyThermalSummary(
    val throttlingEventCount: Int,
    val highestThermalStatus: Int?,
    val availability: WeeklyReportAvailability,
)

data class WeeklySpeedSummary(
    val testCount: Int,
    val medianDownloadMbps: Double?,
    val medianUploadMbps: Double?,
    val medianLatencyMs: Double?,
    val availability: WeeklyReportAvailability,
)

data class WeeklyAppUsageSummary(
    val packageName: String,
    val appLabel: String?,
    val foregroundTimeMs: Long,
    val availability: WeeklyReportAvailability,
)

data class WeeklyReport(
    val period: WeeklyReportPeriod,
    val coverage: WeeklyReportCoverage,
    val battery: WeeklyBatterySummary,
    val storage: WeeklyStorageSummary,
    val thermal: WeeklyThermalSummary,
    val speed: WeeklySpeedSummary,
    val topApps: List<WeeklyAppUsageSummary>,
    val availability: WeeklyReportAvailability,
)

data class WeeklyReportSourceData(
    val batteryReadings: List<BatteryReading> = emptyList(),
    val storageReadings: List<StorageReading> = emptyList(),
    val thermalReadings: List<ThermalReading> = emptyList(),
    val throttlingEventTimestamps: List<Long> = emptyList(),
    val speedTests: List<SpeedTestResult> = emptyList(),
    val appUsage: List<AppBatteryUsage> = emptyList(),
)
