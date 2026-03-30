package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.analysis.DrainSample
import com.runcheck.domain.insights.analysis.TimeInterval
import com.runcheck.domain.insights.analysis.TimeWindowAligner
import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.ThermalRepository
import javax.inject.Inject
import kotlin.math.roundToInt

class HeatAcceleratedBatteryWearRule
    @Inject
    constructor(
        private val batteryRepository: BatteryRepository,
        private val thermalRepository: ThermalRepository,
        private val batteryDrainAnalyzer: BatteryDrainAnalyzer,
        private val timeWindowAligner: TimeWindowAligner,
    ) : InsightRule {
        override val ruleId: String = RULE_ID

        override suspend fun evaluate(now: Long): List<InsightCandidate> {
            val batteryReadings = batteryRepository.getReadingsSinceSync(now - LOOKBACK_MS)
            val thermalReadings = thermalRepository.getReadingsSinceSync(now - LOOKBACK_MS)
            if (batteryReadings.size < MINIMUM_BATTERY_READING_COUNT ||
                thermalReadings.size < MINIMUM_THERMAL_READING_COUNT
            ) {
                return emptyList()
            }

            val dischargingPairs =
                batteryReadings
                    .sortedBy { it.timestamp }
                    .zipWithNext()
                    .filter { (_, current) ->
                        current.status in BatteryDrainAnalyzer.DEFAULT_DISCHARGING_STATUSES
                    }
            if (dischargingPairs.size < MINIMUM_TOTAL_INTERVAL_COUNT) return emptyList()

            val alignedIntervals =
                timeWindowAligner.alignLatestContext(
                    intervals =
                        dischargingPairs.map { (previous, current) ->
                            TimeInterval(
                                startTime = previous.timestamp,
                                endTime = current.timestamp,
                            )
                        },
                    contexts = thermalReadings,
                    contextTimestamp = ThermalReading::timestamp,
                )

            val hotDrainSamples = mutableListOf<DrainSample>()
            val coolDrainSamples = mutableListOf<DrainSample>()
            val hotTemperatures = mutableListOf<Float>()

            dischargingPairs.zip(alignedIntervals).forEach { (pair, aligned) ->
                val thermalReading = aligned.context ?: return@forEach
                val sample = pair.toDrainSample() ?: return@forEach
                val thermalStatus = parseThermalStatus(thermalReading.thermalStatus)

                when {
                    thermalReading.batteryTempC >= HOT_BATTERY_TEMP_C || thermalStatus >= ThermalStatus.SEVERE -> {
                        hotDrainSamples += sample
                        hotTemperatures += thermalReading.batteryTempC
                    }

                    thermalReading.batteryTempC <= COOL_BATTERY_TEMP_C && thermalStatus <= ThermalStatus.LIGHT -> {
                        coolDrainSamples += sample
                    }
                }
            }

            if (hotDrainSamples.size < MINIMUM_CLASSIFIED_INTERVAL_COUNT ||
                coolDrainSamples.size < MINIMUM_CLASSIFIED_INTERVAL_COUNT
            ) {
                return emptyList()
            }

            val comparison =
                batteryDrainAnalyzer.compareAverageDrainRates(
                    currentSamples = hotDrainSamples,
                    previousSamples = coolDrainSamples,
                ) ?: return emptyList()
            if (comparison.changeRatio < MINIMUM_DRAIN_RATIO) return emptyList()

            val peakHotTemperature = hotTemperatures.maxOrNull() ?: return emptyList()
            val priority =
                when {
                    comparison.changeRatio >= HIGH_PRIORITY_DRAIN_RATIO ||
                        peakHotTemperature >= HIGH_PRIORITY_TEMP_C -> {
                        InsightPriority.HIGH
                    }

                    else -> {
                        InsightPriority.MEDIUM
                    }
                }
            val confidence =
                (minOf(hotDrainSamples.size, coolDrainSamples.size) / CONFIDENCE_INTERVAL_COUNT.toFloat())
                    .coerceIn(0f, 1f)
            val increaseBucket =
                when {
                    comparison.percentIncrease >= 60 -> "60plus"
                    comparison.percentIncrease >= 40 -> "40plus"
                    else -> "20plus"
                }

            return listOf(
                InsightCandidate(
                    ruleId = ruleId,
                    dedupeKey = "heat_drain:$increaseBucket",
                    type = InsightType.BATTERY,
                    priority = priority,
                    confidence = confidence,
                    titleKey = TITLE_KEY,
                    bodyKey = BODY_KEY,
                    bodyArgs =
                        listOf(
                            comparison.percentIncrease.coerceAtLeast(1).toString(),
                            peakHotTemperature.roundToInt().toString(),
                        ),
                    generatedAt = now,
                    expiresAt = now + TTL_MS,
                    dataWindowStart = now - LOOKBACK_MS,
                    dataWindowEnd = now,
                    target = InsightTarget.BATTERY,
                ),
            )
        }

        private fun parseThermalStatus(raw: Int): ThermalStatus =
            ThermalStatus.entries.getOrElse(raw) { ThermalStatus.NONE }

        private fun Pair<BatteryReading, BatteryReading>.toDrainSample(): DrainSample? {
            val (previous, current) = this
            val levelDrop = (previous.level - current.level).coerceAtLeast(0)
            val durationMs = (current.timestamp - previous.timestamp).coerceAtLeast(0L)
            if (levelDrop <= 0 || durationMs <= 0L) return null

            return DrainSample(
                levelDropPct = levelDrop.toFloat(),
                durationMs = durationMs,
            )
        }

        companion object {
            const val RULE_ID = "heat_accelerated_battery_wear"

            private const val TITLE_KEY = "insight_heat_battery_wear_title"
            private const val BODY_KEY = "insight_heat_battery_wear_body"
            private const val LOOKBACK_MS = 48L * 60L * 60L * 1000L
            private const val TTL_MS = 12L * 60L * 60L * 1000L
            private const val HOT_BATTERY_TEMP_C = 40f
            private const val COOL_BATTERY_TEMP_C = 35f
            private const val HIGH_PRIORITY_TEMP_C = 43f
            private const val MINIMUM_BATTERY_READING_COUNT = 9
            private const val MINIMUM_THERMAL_READING_COUNT = 8
            private const val MINIMUM_TOTAL_INTERVAL_COUNT = 8
            private const val MINIMUM_CLASSIFIED_INTERVAL_COUNT = 3
            private const val MINIMUM_DRAIN_RATIO = 1.2f
            private const val HIGH_PRIORITY_DRAIN_RATIO = 1.4f
            private const val CONFIDENCE_INTERVAL_COUNT = 5
        }
    }
