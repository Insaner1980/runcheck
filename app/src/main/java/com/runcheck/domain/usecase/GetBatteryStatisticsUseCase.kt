package com.runcheck.domain.usecase

import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.repository.BatteryRepository
import javax.inject.Inject

class GetBatteryStatisticsUseCase @Inject constructor(
    private val batteryRepository: BatteryRepository
) {
    suspend operator fun invoke(periodDays: Int = DEFAULT_PERIOD_DAYS): BatteryStatistics? {
        val since = System.currentTimeMillis() - periodDays * DAY_MS
        val readings = batteryRepository.getReadingsSince(since)
        if (readings.size < 2) return null

        val sorted = readings.sortedBy { it.timestamp }

        // Calculate total charged and discharged percentages
        var totalCharged = 0f
        var totalDischarged = 0f
        var chargeSessions = 0
        var wasCharging = false

        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val curr = sorted[i]
            val levelDiff = curr.level - prev.level
            val isCharging = curr.status == "CHARGING"

            if (levelDiff > 0) {
                totalCharged += levelDiff
            } else if (levelDiff < 0) {
                totalDischarged += -levelDiff
            }

            // Count charge session starts
            if (isCharging && !wasCharging) {
                chargeSessions++
            }
            wasCharging = isCharging
        }

        // Calculate average drain rates from discharging-only readings
        val dischargingPairs = sorted.zipWithNext().filter { (_, curr) ->
            curr.status == "DISCHARGING" || curr.status == "NOT_CHARGING"
        }

        val avgDrainRate = if (dischargingPairs.isNotEmpty()) {
            val totalDrainPct = dischargingPairs.sumOf { (prev, curr) ->
                (prev.level - curr.level).coerceAtLeast(0).toDouble()
            }.toFloat()
            val totalDrainMs = dischargingPairs.sumOf { (prev, curr) ->
                curr.timestamp - prev.timestamp
            }
            if (totalDrainMs > 0) {
                totalDrainPct / (totalDrainMs / 3_600_000f)
            } else null
        } else null

        // Full charge estimate (hours)
        val fullChargeEstimateHours = avgDrainRate?.let {
            if (it > 0.1f) 100f / it else null
        }

        return BatteryStatistics(
            periodDays = periodDays,
            totalChargedPct = totalCharged,
            totalDischargedPct = totalDischarged,
            chargeSessions = chargeSessions,
            avgDrainRatePctPerHour = avgDrainRate,
            fullChargeEstimateHours = fullChargeEstimateHours,
            readingCount = sorted.size
        )
    }

    companion object {
        const val DEFAULT_PERIOD_DAYS = 10
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}

/**
 * Suspend version of getReadingsSince that returns a list directly (not Flow).
 */
private suspend fun com.runcheck.domain.repository.BatteryRepository.getReadingsSince(since: Long): List<BatteryReading> {
    val readings = mutableListOf<BatteryReading>()
    // Use getAllReadings and filter, since the Flow-based version doesn't suit one-shot queries
    return getAllReadings().filter { it.timestamp >= since }
}

data class BatteryStatistics(
    val periodDays: Int,
    val totalChargedPct: Float,
    val totalDischargedPct: Float,
    val chargeSessions: Int,
    val avgDrainRatePctPerHour: Float?,
    val fullChargeEstimateHours: Float?,
    val readingCount: Int
)
