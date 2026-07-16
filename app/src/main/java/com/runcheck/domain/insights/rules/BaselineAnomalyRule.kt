package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.analysis.dischargingPairs
import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.repository.BatteryRepository
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class BaselineAnomalyRule
    @Inject
    constructor(
        private val batteryRepository: BatteryRepository,
        private val batteryDrainAnalyzer: BatteryDrainAnalyzer,
    ) : InsightRule {
        override val ruleId: String = RULE_ID

        override suspend fun evaluate(now: Long): List<InsightCandidate> = buildCandidate(now)?.let(::listOf).orEmpty()

        private suspend fun buildCandidate(now: Long): InsightCandidate? {
            val currentWindowStart = now - CURRENT_WINDOW_MS
            val baselineStart = currentWindowStart - BASELINE_WINDOW_MS
            val readings =
                batteryRepository
                    .getReadingsSinceSync(baselineStart)
                    .takeIf { it.size >= MINIMUM_TOTAL_READING_COUNT }
                    ?: return null
            val currentDrainWindow = currentDrainWindow(readings, currentWindowStart, now) ?: return null

            val baselineRates =
                (0 until BASELINE_DAYS).mapNotNull { day ->
                    val dayStart = baselineStart + (day * DAY_MS)
                    val dayEnd = dayStart + DAY_MS
                    val dayReadings = readings.filter { it.timestamp >= dayStart && it.timestamp < dayEnd }
                    if (dayReadings.dischargingPairCount() < MINIMUM_BASELINE_SAMPLE_COUNT) {
                        null
                    } else {
                        batteryDrainAnalyzer.calculateAverageDrainRate(dayReadings)
                    }
                }
            if (baselineRates.size < MINIMUM_BASELINE_DAY_COUNT) return null

            val anomalyScore = anomalyScore(currentDrainWindow.currentRate, baselineRates) ?: return null

            return InsightCandidate(
                ruleId = ruleId,
                dedupeKey = "battery_drain:${anomalyScore.rateRatio.toRatioBucket()}",
                type = InsightType.BATTERY,
                priority = resolvePriority(anomalyScore.zScore, anomalyScore.rateRatio),
                confidence = resolveConfidence(baselineRates.size, currentDrainWindow.dischargingPairs),
                titleKey = TITLE_KEY,
                bodyKey = BODY_KEY,
                bodyArgs =
                    listOf(
                        currentDrainWindow.currentRate
                            .roundToInt()
                            .coerceAtLeast(1)
                            .toString(),
                        anomalyScore.baselineRate
                            .roundToInt()
                            .coerceAtLeast(1)
                            .toString(),
                    ),
                generatedAt = now,
                expiresAt = now + TTL_MS,
                dataWindowStart = baselineStart,
                dataWindowEnd = now,
                target = InsightTarget.BATTERY,
            )
        }

        private fun currentDrainWindow(
            readings: List<BatteryReading>,
            currentWindowStart: Long,
            now: Long,
        ): CurrentDrainWindow? {
            val currentReadings = readings.filter { it.timestamp in currentWindowStart..now }
            val currentDischargingPairs = currentReadings.dischargingPairCount()
            if (currentDischargingPairs < MINIMUM_CURRENT_SAMPLE_COUNT) return null

            val currentRate = batteryDrainAnalyzer.calculateAverageDrainRate(currentReadings) ?: return null
            if (currentRate < MINIMUM_CURRENT_DRAIN_RATE) return null

            return CurrentDrainWindow(currentRate, currentDischargingPairs)
        }

        private fun anomalyScore(
            currentRate: Float,
            baselineRates: List<Float>,
        ): AnomalyScore? {
            val baselineRate = baselineRates.average().toFloat()
            if (baselineRate <= 0f) return null

            val standardDeviation = baselineRates.sampleStandardDeviation().coerceAtLeast(MINIMUM_STANDARD_DEVIATION)
            val zScore = (currentRate - baselineRate) / standardDeviation
            val rateRatio = currentRate / baselineRate

            return when {
                zScore < MINIMUM_Z_SCORE || rateRatio < MINIMUM_RATE_RATIO -> null
                else -> AnomalyScore(baselineRate, zScore, rateRatio)
            }
        }

        private fun List<BatteryReading>.dischargingPairCount(): Int = dischargingPairs().size

        private fun List<Float>.sampleStandardDeviation(): Float {
            if (size < 2) return 0f
            val mean = average()
            val squaredDeviationSum = sumOf { rate -> (rate - mean).pow(2) }
            return sqrt(squaredDeviationSum / (size - 1)).toFloat()
        }

        private fun resolvePriority(
            zScore: Float,
            rateRatio: Float,
        ): InsightPriority =
            when {
                zScore >= HIGH_PRIORITY_Z_SCORE ||
                    rateRatio >= HIGH_PRIORITY_RATE_RATIO -> InsightPriority.HIGH

                else -> InsightPriority.MEDIUM
            }

        private fun resolveConfidence(
            baselineDayCount: Int,
            currentSampleCount: Int,
        ): Float {
            val baselineConfidence = baselineDayCount / BASELINE_DAYS.toFloat()
            val currentConfidence = currentSampleCount / CONFIDENCE_CURRENT_SAMPLE_COUNT.toFloat()
            return ((baselineConfidence + currentConfidence) / 2f).coerceIn(0f, 1f)
        }

        private fun Float.toRatioBucket(): String =
            when {
                this >= 3f -> "3xplus"
                this >= 2.5f -> "25xplus"
                this >= 2f -> "2xplus"
                else -> "175xplus"
            }

        private data class CurrentDrainWindow(
            val currentRate: Float,
            val dischargingPairs: Int,
        )

        private data class AnomalyScore(
            val baselineRate: Float,
            val zScore: Float,
            val rateRatio: Float,
        )

        companion object {
            const val RULE_ID = "battery_baseline_anomaly"

            private const val TITLE_KEY = "insight_battery_baseline_anomaly_title"
            private const val BODY_KEY = "insight_battery_baseline_anomaly_body"
            private const val HOUR_MS = 60L * 60L * 1000L
            private const val DAY_MS = 24L * HOUR_MS
            private const val CURRENT_WINDOW_MS = DAY_MS
            private const val BASELINE_DAYS = 14
            private const val BASELINE_WINDOW_MS = BASELINE_DAYS * DAY_MS
            private const val TTL_MS = DAY_MS
            private const val MINIMUM_TOTAL_READING_COUNT = 40
            private const val MINIMUM_CURRENT_SAMPLE_COUNT = 4
            private const val MINIMUM_BASELINE_SAMPLE_COUNT = 4
            private const val MINIMUM_BASELINE_DAY_COUNT = 7
            private const val CONFIDENCE_CURRENT_SAMPLE_COUNT = 6
            private const val MINIMUM_CURRENT_DRAIN_RATE = 3f
            private const val MINIMUM_STANDARD_DEVIATION = 0.5f
            private const val MINIMUM_Z_SCORE = 2.5f
            private const val MINIMUM_RATE_RATIO = 1.75f
            private const val HIGH_PRIORITY_Z_SCORE = 4f
            private const val HIGH_PRIORITY_RATE_RATIO = 3f
        }
    }
