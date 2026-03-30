package com.runcheck.domain.usecase

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.repository.BatteryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetBatteryStatisticsUseCase
    @Inject
    constructor(
        private val batteryRepository: BatteryRepository,
        private val batteryDrainAnalyzer: BatteryDrainAnalyzer,
    ) {
        suspend operator fun invoke(periodDays: Int = DEFAULT_PERIOD_DAYS): BatteryStatistics? {
            return withContext(Dispatchers.Default) {
                val since = System.currentTimeMillis() - periodDays * DAY_MS
                val readings = batteryRepository.getReadingsSinceSync(since)
                if (readings.size < 2) return@withContext null

                val sorted = readings.sortedBy { it.timestamp }
                val chargeSummary = calculateChargeSummary(sorted)
                val avgDrainRate = batteryDrainAnalyzer.calculateAverageDrainRate(sorted)
                val fullChargeEstimateHours = avgDrainRate?.takeIf { it > MIN_DRAIN_RATE }?.let { 100f / it }

                BatteryStatistics(
                    periodDays = periodDays,
                    totalChargedPct = chargeSummary.totalCharged,
                    totalDischargedPct = chargeSummary.totalDischarged,
                    chargeSessions = chargeSummary.chargeSessions,
                    avgDrainRatePctPerHour = avgDrainRate,
                    fullChargeEstimateHours = fullChargeEstimateHours,
                    readingCount = sorted.size,
                )
            }
        }

        private fun calculateChargeSummary(readings: List<com.runcheck.domain.model.BatteryReading>): ChargeSummary {
            var totalCharged = 0f
            var totalDischarged = 0f
            var chargeSessions = 0
            var wasCharging = false

            for (index in 1 until readings.size) {
                val previous = readings[index - 1]
                val current = readings[index]
                val levelDiff = current.level - previous.level
                val isCharging = current.status == "CHARGING"

                when {
                    levelDiff > 0 -> totalCharged += levelDiff
                    levelDiff < 0 -> totalDischarged += -levelDiff
                }

                if (isCharging && !wasCharging) {
                    chargeSessions++
                }
                wasCharging = isCharging
            }

            return ChargeSummary(
                totalCharged = totalCharged,
                totalDischarged = totalDischarged,
                chargeSessions = chargeSessions,
            )
        }

        companion object {
            const val DEFAULT_PERIOD_DAYS = 10
            private const val DAY_MS = 24 * 60 * 60 * 1000L
            private const val MIN_DRAIN_RATE = 0.1f
        }
    }

private data class ChargeSummary(
    val totalCharged: Float,
    val totalDischarged: Float,
    val chargeSessions: Int,
)

data class BatteryStatistics(
    val periodDays: Int,
    val totalChargedPct: Float,
    val totalDischargedPct: Float,
    val chargeSessions: Int,
    val avgDrainRatePctPerHour: Float?,
    val fullChargeEstimateHours: Float?,
    val readingCount: Int,
)
