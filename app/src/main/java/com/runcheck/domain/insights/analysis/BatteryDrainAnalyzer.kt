package com.runcheck.domain.insights.analysis

import com.runcheck.domain.model.BatteryReading
import javax.inject.Inject
import kotlin.math.roundToInt

class BatteryDrainAnalyzer
    @Inject
    constructor() {
        fun calculateAverageDrainRate(samples: List<DrainSample>): Float? {
            if (samples.isEmpty()) return null

            val totalDrainPct = samples.sumOf { it.levelDropPct.coerceAtLeast(0f).toDouble() }.toFloat()
            val totalDrainMs = samples.sumOf { it.durationMs.coerceAtLeast(0L) }
            if (totalDrainMs <= 0L || totalDrainPct <= 0f) return null

            return totalDrainPct / (totalDrainMs / HOUR_MS.toFloat())
        }

        fun calculateAverageDrainRate(
            readings: List<BatteryReading>,
            dischargingStatuses: Set<String> = DEFAULT_DISCHARGING_STATUSES,
        ): Float? {
            val dischargingPairs =
                readings
                    .sortedBy { it.timestamp }
                    .zipWithNext()
                    .filter { (_, current) -> current.status in dischargingStatuses }
            if (dischargingPairs.isEmpty()) return null

            val totalDrainPct =
                dischargingPairs
                    .sumOf { (previous, current) ->
                        (previous.level - current.level).coerceAtLeast(0).toDouble()
                    }.toFloat()
            val totalDrainMs =
                dischargingPairs.sumOf { (previous, current) ->
                    (current.timestamp - previous.timestamp).coerceAtLeast(0L)
                }
            if (totalDrainMs <= 0L) return null

            return totalDrainPct / (totalDrainMs / HOUR_MS.toFloat())
        }

        fun compareAverageDrainRates(
            currentSamples: List<DrainSample>,
            previousSamples: List<DrainSample>,
        ): DrainRateComparison? {
            val currentRate = calculateAverageDrainRate(currentSamples) ?: return null
            val previousRate = calculateAverageDrainRate(previousSamples) ?: return null
            if (currentRate <= 0f || previousRate <= 0f) return null

            val changeRatio = currentRate / previousRate
            return DrainRateComparison(
                currentRatePctPerHour = currentRate,
                previousRatePctPerHour = previousRate,
                changeRatio = changeRatio,
                percentIncrease = ((changeRatio - 1f) * 100f).roundToInt(),
                currentSampleCount = currentSamples.size,
                previousSampleCount = previousSamples.size,
            )
        }

        fun compareAverageDrainRates(
            currentWindow: List<BatteryReading>,
            previousWindow: List<BatteryReading>,
            dischargingStatuses: Set<String> = DEFAULT_DISCHARGING_STATUSES,
        ): DrainRateComparison? {
            val currentRate = calculateAverageDrainRate(currentWindow, dischargingStatuses) ?: return null
            val previousRate = calculateAverageDrainRate(previousWindow, dischargingStatuses) ?: return null
            if (currentRate <= 0f || previousRate <= 0f) return null

            val changeRatio = currentRate / previousRate
            return DrainRateComparison(
                currentRatePctPerHour = currentRate,
                previousRatePctPerHour = previousRate,
                changeRatio = changeRatio,
                percentIncrease = ((changeRatio - 1f) * 100f).roundToInt(),
                currentSampleCount = currentWindow.size,
                previousSampleCount = previousWindow.size,
            )
        }

        companion object {
            private const val HOUR_MS = 3_600_000L

            val DEFAULT_DISCHARGING_STATUSES: Set<String> =
                setOf("DISCHARGING", "NOT_CHARGING")
        }
    }

data class DrainRateComparison(
    val currentRatePctPerHour: Float,
    val previousRatePctPerHour: Float,
    val changeRatio: Float,
    val percentIncrease: Int,
    val currentSampleCount: Int,
    val previousSampleCount: Int,
)

data class DrainSample(
    val levelDropPct: Float,
    val durationMs: Long,
)
