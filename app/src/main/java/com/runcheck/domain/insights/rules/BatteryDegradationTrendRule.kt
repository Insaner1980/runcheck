package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.repository.BatteryRepository
import javax.inject.Inject

class BatteryDegradationTrendRule
    @Inject
    constructor(
        private val batteryRepository: BatteryRepository,
        private val batteryDrainAnalyzer: BatteryDrainAnalyzer,
    ) : InsightRule {
        override val ruleId: String = RULE_ID

        override suspend fun evaluate(now: Long): List<InsightCandidate> {
            val currentWindowStart = now - WINDOW_MS
            val previousWindowStart = now - (WINDOW_MS * 2)
            val allReadings = batteryRepository.getReadingsSinceSync(previousWindowStart)
            if (allReadings.size < MINIMUM_READING_COUNT_PER_WINDOW * 2) return emptyList()

            val previousWindow =
                allReadings.filter { reading ->
                    reading.timestamp in previousWindowStart until currentWindowStart
                }
            val currentWindow =
                allReadings.filter { reading ->
                    reading.timestamp in currentWindowStart..now
                }

            val previousDischargingCount =
                previousWindow.count { it.status in BatteryDrainAnalyzer.DEFAULT_DISCHARGING_STATUSES }
            val currentDischargingCount =
                currentWindow.count { it.status in BatteryDrainAnalyzer.DEFAULT_DISCHARGING_STATUSES }
            if (previousDischargingCount < MINIMUM_READING_COUNT_PER_WINDOW ||
                currentDischargingCount < MINIMUM_READING_COUNT_PER_WINDOW
            ) {
                return emptyList()
            }

            val comparison =
                batteryDrainAnalyzer.compareAverageDrainRates(
                    currentWindow = currentWindow,
                    previousWindow = previousWindow,
                ) ?: return emptyList()

            if (comparison.changeRatio <= MINIMUM_DEGRADATION_RATIO) {
                return emptyList()
            }

            val confidence =
                (minOf(currentDischargingCount, previousDischargingCount) / CONFIDENCE_SAMPLE_SIZE.toFloat())
                    .coerceIn(0f, 1f)
            val percentIncrease = comparison.percentIncrease.coerceAtLeast(1).toString()

            return listOf(
                InsightCandidate(
                    ruleId = ruleId,
                    dedupeKey = "$currentWindowStart:$now",
                    type = InsightType.BATTERY,
                    priority = InsightPriority.HIGH,
                    confidence = confidence,
                    titleKey = TITLE_KEY,
                    bodyKey = BODY_KEY,
                    bodyArgs = listOf(percentIncrease),
                    generatedAt = now,
                    expiresAt = now + TTL_MS,
                    dataWindowStart = previousWindowStart,
                    dataWindowEnd = now,
                    target = InsightTarget.BATTERY,
                ),
            )
        }

        companion object {
            const val RULE_ID = "battery_degradation_trend"

            private const val TITLE_KEY = "insight_battery_degradation_title"
            private const val BODY_KEY = "insight_battery_degradation_body"
            private const val WINDOW_MS = 7L * 24L * 60L * 60L * 1000L
            private const val TTL_MS = 24L * 60L * 60L * 1000L
            private const val MINIMUM_READING_COUNT_PER_WINDOW = 20
            private const val CONFIDENCE_SAMPLE_SIZE = 40
            private const val MINIMUM_DEGRADATION_RATIO = 1.15f
        }
    }
