package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.repository.ThermalRepository
import javax.inject.Inject
import kotlin.math.roundToInt

class ThermalPatternDetectionRule
    @Inject
    constructor(
        private val thermalRepository: ThermalRepository,
    ) : InsightRule {
        override val ruleId: String = RULE_ID

        override suspend fun evaluate(now: Long): List<InsightCandidate> = buildCandidate(now)?.let(::listOf).orEmpty()

        private suspend fun buildCandidate(now: Long): InsightCandidate? {
            val readings = thermalRepository.getReadingsSinceSync(now - LOOKBACK_MS)
            if (readings.size < MINIMUM_READING_COUNT) return null

            val hotReadings = readings.filter(::isHotReading)
            if (hotReadings.size < MINIMUM_HOT_READING_COUNT) return null

            val summary = hotReadings.toThermalPatternSummary(totalReadingCount = readings.size) ?: return null
            if (summary.hotRatio < MINIMUM_HOT_RATIO) return null

            return summary.toCandidate(now)
        }

        private fun isHotReading(reading: ThermalReading): Boolean =
            reading.batteryTempC >= HOT_BATTERY_TEMP_C ||
                parseThermalStatus(reading.thermalStatus) >= ThermalStatus.MODERATE

        private fun List<ThermalReading>.toThermalPatternSummary(totalReadingCount: Int): ThermalPatternSummary? {
            val hotRatio = size / totalReadingCount.toFloat()
            val peakTemp = maxOfOrNull { it.batteryTempC }
            val peakStatus = maxOfOrNull { parseThermalStatus(it.thermalStatus) }

            return if (peakTemp != null && peakStatus != null) {
                ThermalPatternSummary(
                    hotRatio = hotRatio,
                    averageHotTemp = map { it.batteryTempC }.average().toFloat(),
                    peakTemp = peakTemp,
                    peakStatus = peakStatus,
                    confidence = (totalReadingCount / CONFIDENCE_SAMPLE_COUNT.toFloat()).coerceIn(0f, 1f),
                )
            } else {
                null
            }
        }

        private fun ThermalPatternSummary.toCandidate(now: Long): InsightCandidate {
            val ratioPercent = (hotRatio * 100f).roundToInt()
            return InsightCandidate(
                ruleId = ruleId,
                dedupeKey = "hot_pattern:${ratioPercent.toRatioBucket()}",
                type = InsightType.THERMAL,
                priority = resolvePriority(),
                confidence = confidence,
                titleKey = TITLE_KEY,
                bodyKey = BODY_KEY,
                bodyArgs = listOf(ratioPercent.toString(), averageHotTemp.roundToInt().toString()),
                generatedAt = now,
                expiresAt = now + TTL_MS,
                dataWindowStart = now - LOOKBACK_MS,
                dataWindowEnd = now,
                target = InsightTarget.THERMAL,
            )
        }

        private fun ThermalPatternSummary.resolvePriority(): InsightPriority =
            when {
                peakStatus >= ThermalStatus.CRITICAL ||
                    peakTemp >= HIGH_PRIORITY_TEMP_C ||
                    hotRatio >= HIGH_PRIORITY_HOT_RATIO -> InsightPriority.HIGH

                else -> InsightPriority.MEDIUM
            }

        private fun Int.toRatioBucket(): String =
            when {
                this >= 80 -> "80plus"
                this >= 70 -> "70plus"
                else -> "60plus"
            }

        private fun parseThermalStatus(raw: Int): ThermalStatus =
            ThermalStatus.entries.getOrElse(raw) { ThermalStatus.NONE }

        companion object {
            const val RULE_ID = "thermal_pattern_detection"

            private const val TITLE_KEY = "insight_thermal_pattern_title"
            private const val BODY_KEY = "insight_thermal_pattern_body"
            private const val LOOKBACK_MS = 48L * 60L * 60L * 1000L
            private const val TTL_MS = 12L * 60L * 60L * 1000L
            private const val HOT_BATTERY_TEMP_C = 39.5f
            private const val HIGH_PRIORITY_TEMP_C = 43f
            private const val MINIMUM_READING_COUNT = 6
            private const val MINIMUM_HOT_READING_COUNT = 4
            private const val MINIMUM_HOT_RATIO = 0.6f
            private const val HIGH_PRIORITY_HOT_RATIO = 0.75f
            private const val CONFIDENCE_SAMPLE_COUNT = 8
        }
    }

private data class ThermalPatternSummary(
    val hotRatio: Float,
    val averageHotTemp: Float,
    val peakTemp: Float,
    val peakStatus: ThermalStatus,
    val confidence: Float,
)
