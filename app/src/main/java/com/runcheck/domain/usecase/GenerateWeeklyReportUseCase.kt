package com.runcheck.domain.usecase

import com.runcheck.domain.model.WeeklyAppUsageSummary
import com.runcheck.domain.model.WeeklyBatterySummary
import com.runcheck.domain.model.WeeklyReport
import com.runcheck.domain.model.WeeklyReportAvailability
import com.runcheck.domain.model.WeeklyReportCoverage
import com.runcheck.domain.model.WeeklyReportPeriod
import com.runcheck.domain.model.WeeklyReportSourceData
import com.runcheck.domain.model.WeeklySpeedSummary
import com.runcheck.domain.model.WeeklyStorageSummary
import com.runcheck.domain.model.WeeklyThermalSummary
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.WeeklyReportRepository
import java.time.Instant
import javax.inject.Inject
import kotlin.math.abs

sealed interface WeeklyReportGenerationResult {
    data object Locked : WeeklyReportGenerationResult

    data class Available(
        val report: WeeklyReport,
    ) : WeeklyReportGenerationResult
}

class GenerateWeeklyReportUseCase
    @Inject
    constructor(
        private val repository: WeeklyReportRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        suspend operator fun invoke(period: WeeklyReportPeriod): WeeklyReportGenerationResult {
            if (!proStatusProvider.isPro()) return WeeklyReportGenerationResult.Locked
            return WeeklyReportGenerationResult.Available(
                aggregateWeeklyReport(period, repository.loadPeriod(period)),
            )
        }
    }

internal fun aggregateWeeklyReport(
    period: WeeklyReportPeriod,
    source: WeeklyReportSourceData,
): WeeklyReport {
    val battery = aggregateBattery(source)
    val storage = aggregateStorage(source)
    val thermal = aggregateThermal(source)
    val speed = aggregateSpeed(source)
    val topApps =
        source.appUsage
            .groupBy { it.packageName }
            .map { (packageName, rows) ->
                WeeklyAppUsageSummary(
                    packageName = packageName,
                    appLabel = rows.lastOrNull()?.appLabel,
                    foregroundTimeMs = rows.sumOf { it.foregroundTimeMs },
                    availability = WeeklyReportAvailability.ESTIMATED,
                )
            }.sortedByDescending { it.foregroundTimeMs }
            .take(MAX_TOP_APPS)

    val sampleTimes =
        buildList {
            addAll(source.batteryReadings.map { it.timestamp })
            addAll(source.storageReadings.map { it.timestamp })
            addAll(source.thermalReadings.map { it.timestamp })
            addAll(source.throttlingEventTimestamps)
            addAll(source.speedTests.map { it.timestamp })
            addAll(source.appUsage.map { it.timestamp })
        }
    val monitoredDays =
        sampleTimes
            .asSequence()
            .map { Instant.ofEpochMilli(it).atZone(period.zoneId).toLocalDate() }
            .distinct()
            .count()
    val coverageAvailability =
        when {
            sampleTimes.isEmpty() -> WeeklyReportAvailability.UNAVAILABLE
            monitoredDays >= DAYS_IN_WEEK && source.appUsage.isEmpty() ->
                WeeklyReportAvailability.AVAILABLE
            else -> WeeklyReportAvailability.ESTIMATED
        }
    val coverage =
        WeeklyReportCoverage(
            monitoredDays = monitoredDays,
            sampleCount = sampleTimes.size,
            availability = coverageAvailability,
        )
    return WeeklyReport(
        period = period,
        coverage = coverage,
        battery = battery,
        storage = storage,
        thermal = thermal,
        speed = speed,
        topApps = topApps,
        availability = coverageAvailability,
    )
}

private fun aggregateBattery(source: WeeklyReportSourceData): WeeklyBatterySummary {
    val validSegments =
        source.batteryReadings
            .sortedBy { it.timestamp }
            .zipWithNext()
            .filter { (first, second) ->
                val gap = second.timestamp - first.timestamp
                gap in 1..MAX_BATTERY_SAMPLE_GAP_MS
            }
    val dischargeSegments = validSegments.filter { (first, second) -> second.level < first.level }
    val dischargeChange =
        dischargeSegments.sumOf { (first, second) -> (second.level - first.level).toDouble() }
    val chargeChange =
        validSegments
            .filter { (first, second) -> second.level > first.level }
            .sumOf { (first, second) -> (second.level - first.level).toDouble() }
    val dischargeHours =
        dischargeSegments.sumOf { (first, second) ->
            (second.timestamp - first.timestamp).toDouble() / MILLIS_PER_HOUR
        }
    val healthValues = source.batteryReadings.mapNotNull { it.healthPct }
    val healthChange =
        if (healthValues.size >= 2) {
            healthValues.last() - healthValues.first()
        } else {
            null
        }
    val availability =
        if (validSegments.isEmpty()) {
            WeeklyReportAvailability.UNAVAILABLE
        } else {
            WeeklyReportAvailability.ESTIMATED
        }
    return WeeklyBatterySummary(
        averageDischargePercentPerHour =
            if (dischargeHours > 0.0) abs(dischargeChange) / dischargeHours else null,
        dischargePercentChange = dischargeChange,
        chargePercentChange = chargeChange,
        healthPercentChange = healthChange,
        availability = availability,
        validSegmentCount = validSegments.size,
    )
}

private fun aggregateStorage(source: WeeklyReportSourceData): WeeklyStorageSummary {
    val readings = source.storageReadings.sortedBy { it.timestamp }
    val change =
        if (readings.size >= 2) {
            readings.last().availableBytes - readings.first().availableBytes
        } else {
            null
        }
    return WeeklyStorageSummary(
        availableBytesChange = change,
        availability =
            if (change == null) WeeklyReportAvailability.UNAVAILABLE else WeeklyReportAvailability.ESTIMATED,
    )
}

private fun aggregateThermal(source: WeeklyReportSourceData): WeeklyThermalSummary {
    val throttlingCount = source.throttlingEventTimestamps.size
    return WeeklyThermalSummary(
        throttlingEventCount = throttlingCount,
        highestThermalStatus = source.thermalReadings.maxOfOrNull { it.thermalStatus },
        availability =
            if (source.thermalReadings.isEmpty() && source.throttlingEventTimestamps.isEmpty()) {
                WeeklyReportAvailability.UNAVAILABLE
            } else {
                WeeklyReportAvailability.ESTIMATED
            },
    )
}

private fun aggregateSpeed(source: WeeklyReportSourceData): WeeklySpeedSummary =
    WeeklySpeedSummary(
        testCount = source.speedTests.size,
        medianDownloadMbps = median(source.speedTests.map { it.downloadMbps }),
        medianUploadMbps = median(source.speedTests.map { it.uploadMbps }),
        medianLatencyMs = median(source.speedTests.map { it.pingMs.toDouble() }),
        availability =
            if (source.speedTests.isEmpty()) {
                WeeklyReportAvailability.UNAVAILABLE
            } else {
                WeeklyReportAvailability.AVAILABLE
            },
    )

private fun median(values: List<Double>): Double? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 0) {
        (sorted[middle - 1] + sorted[middle]) / 2.0
    } else {
        sorted[middle]
    }
}

private const val MILLIS_PER_HOUR = 60.0 * 60.0 * 1000.0
private const val MAX_BATTERY_SAMPLE_GAP_MS = 4L * 60L * 60L * 1000L
private const val DAYS_IN_WEEK = 7
private const val MAX_TOP_APPS = 5
