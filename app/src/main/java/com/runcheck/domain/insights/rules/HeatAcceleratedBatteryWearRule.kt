package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.analysis.DrainRateComparison
import com.runcheck.domain.insights.analysis.DrainSample
import com.runcheck.domain.insights.analysis.TimeWindowAligner
import com.runcheck.domain.insights.analysis.dischargingPairs
import com.runcheck.domain.insights.analysis.toDrainSample
import com.runcheck.domain.insights.analysis.toTimeIntervals
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

        override suspend fun evaluate(now: Long): List<InsightCandidate> = buildCandidate(now)?.let(::listOf).orEmpty()

        private suspend fun buildCandidate(now: Long): InsightCandidate? {
            val input = loadInput(now) ?: return null
            val samples = classifyDrainSamples(input)
            val comparison = samples.compareDrainRates() ?: return null
            val peakHotTemperature = samples.peakHotTemperature ?: return null

            return buildCandidate(
                now = now,
                comparison = comparison,
                peakHotTemperature = peakHotTemperature,
                confidence = samples.confidence,
            )
        }

        private suspend fun loadInput(now: Long): HeatDrainInput? {
            val batteryReadings = batteryRepository.getReadingsSinceSync(now - LOOKBACK_MS)
            val thermalReadings = thermalRepository.getReadingsSinceSync(now - LOOKBACK_MS)
            if (batteryReadings.size < MINIMUM_BATTERY_READING_COUNT ||
                thermalReadings.size < MINIMUM_THERMAL_READING_COUNT
            ) {
                return null
            }

            val dischargingPairs = batteryReadings.dischargingPairs()
            return HeatDrainInput(
                dischargingPairs = dischargingPairs,
                thermalReadings = thermalReadings,
            ).takeIf { dischargingPairs.size >= MINIMUM_TOTAL_INTERVAL_COUNT }
        }

        private fun classifyDrainSamples(input: HeatDrainInput): HeatDrainSamples {
            val alignedIntervals =
                timeWindowAligner.alignLatestContext(
                    intervals = input.dischargingPairs.toTimeIntervals(),
                    contexts = input.thermalReadings,
                    contextTimestamp = ThermalReading::timestamp,
                )
            val hotDrainSamples = mutableListOf<DrainSample>()
            val coolDrainSamples = mutableListOf<DrainSample>()
            val hotTemperatures = mutableListOf<Float>()

            input.dischargingPairs.zip(alignedIntervals).forEach { (pair, aligned) ->
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

            return HeatDrainSamples(
                hotDrainSamples = hotDrainSamples,
                coolDrainSamples = coolDrainSamples,
                hotTemperatures = hotTemperatures,
            )
        }

        private fun HeatDrainSamples.compareDrainRates(): DrainRateComparison? =
            takeIf { it.hasEnoughSamples }
                ?.let {
                    batteryDrainAnalyzer.compareAverageDrainRates(
                        currentSamples = hotDrainSamples,
                        previousSamples = coolDrainSamples,
                    )
                }?.takeIf { it.changeRatio >= MINIMUM_DRAIN_RATIO }

        private fun buildCandidate(
            now: Long,
            comparison: DrainRateComparison,
            peakHotTemperature: Float,
            confidence: Float,
        ): InsightCandidate =
            InsightCandidate(
                ruleId = ruleId,
                dedupeKey = "heat_drain:${comparison.percentIncrease.toIncreaseBucket()}",
                type = InsightType.BATTERY,
                priority = resolvePriority(comparison, peakHotTemperature),
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
            )

        private fun resolvePriority(
            comparison: DrainRateComparison,
            peakHotTemperature: Float,
        ): InsightPriority =
            when {
                comparison.changeRatio >= HIGH_PRIORITY_DRAIN_RATIO ||
                    peakHotTemperature >= HIGH_PRIORITY_TEMP_C -> InsightPriority.HIGH

                else -> InsightPriority.MEDIUM
            }

        private fun Int.toIncreaseBucket(): String =
            when {
                this >= 60 -> "60plus"
                this >= 40 -> "40plus"
                else -> "20plus"
            }

        private val HeatDrainSamples.hasEnoughSamples: Boolean
            get() =
                hotDrainSamples.size >= MINIMUM_CLASSIFIED_INTERVAL_COUNT &&
                    coolDrainSamples.size >= MINIMUM_CLASSIFIED_INTERVAL_COUNT

        private val HeatDrainSamples.confidence: Float
            get() =
                (minOf(hotDrainSamples.size, coolDrainSamples.size) / CONFIDENCE_INTERVAL_COUNT.toFloat())
                    .coerceIn(0f, 1f)

        private val HeatDrainSamples.peakHotTemperature: Float?
            get() = hotTemperatures.maxOrNull()

        private fun parseThermalStatus(raw: Int): ThermalStatus =
            ThermalStatus.entries.getOrElse(raw) { ThermalStatus.NONE }

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

private data class HeatDrainInput(
    val dischargingPairs: List<Pair<BatteryReading, BatteryReading>>,
    val thermalReadings: List<ThermalReading>,
)

private data class HeatDrainSamples(
    val hotDrainSamples: List<DrainSample>,
    val coolDrainSamples: List<DrainSample>,
    val hotTemperatures: List<Float>,
)
