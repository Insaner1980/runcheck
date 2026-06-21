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
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.NetworkRepository
import javax.inject.Inject
import kotlin.math.roundToInt

class NetworkDrivenBatteryDrainRule
    @Inject
    constructor(
        private val batteryRepository: BatteryRepository,
        private val networkRepository: NetworkRepository,
        private val batteryDrainAnalyzer: BatteryDrainAnalyzer,
        private val timeWindowAligner: TimeWindowAligner,
    ) : InsightRule {
        override val ruleId: String = RULE_ID

        override suspend fun evaluate(now: Long): List<InsightCandidate> = buildCandidate(now)?.let(::listOf).orEmpty()

        private suspend fun buildCandidate(now: Long): InsightCandidate? {
            val input = loadInput(now) ?: return null
            val samples = classifyDrainSamples(input)
            val comparison = samples.compareDrainRates() ?: return null

            return buildCandidate(
                now = now,
                comparison = comparison,
                averageWeakSignal = samples.averageWeakSignal,
                confidence = samples.confidence,
            )
        }

        private suspend fun loadInput(now: Long): NetworkDrainInput? {
            val batteryReadings = batteryRepository.getReadingsSinceSync(now - LOOKBACK_MS)
            val networkReadings = networkRepository.getReadingsSinceSync(now - LOOKBACK_MS)
            if (batteryReadings.size < MINIMUM_BATTERY_READING_COUNT ||
                networkReadings.size < MINIMUM_NETWORK_READING_COUNT
            ) {
                return null
            }

            val dischargingPairs = batteryReadings.dischargingPairs()
            return NetworkDrainInput(
                dischargingPairs = dischargingPairs,
                networkReadings = networkReadings,
            ).takeIf { dischargingPairs.size >= MINIMUM_TOTAL_INTERVAL_COUNT }
        }

        private fun classifyDrainSamples(input: NetworkDrainInput): NetworkDrainSamples {
            val alignedIntervals =
                timeWindowAligner.alignLatestContext(
                    intervals = input.dischargingPairs.toTimeIntervals(),
                    contexts = input.networkReadings,
                    contextTimestamp = NetworkReading::timestamp,
                )
            val weakDrainSamples = mutableListOf<DrainSample>()
            val strongDrainSamples = mutableListOf<DrainSample>()
            val weakSignals = mutableListOf<Int>()

            input.dischargingPairs.zip(alignedIntervals).forEach { (pair, aligned) ->
                val networkReading = aligned.context ?: return@forEach
                if (networkReading.type != ConnectionType.CELLULAR.name) return@forEach

                val signalDbm = networkReading.signalDbm ?: return@forEach
                val sample = pair.toDrainSample() ?: return@forEach

                when {
                    signalDbm <= WEAK_SIGNAL_DBM -> {
                        weakDrainSamples += sample
                        weakSignals += signalDbm
                    }

                    signalDbm >= STRONG_SIGNAL_DBM -> {
                        strongDrainSamples += sample
                    }
                }
            }

            return NetworkDrainSamples(
                weakDrainSamples = weakDrainSamples,
                strongDrainSamples = strongDrainSamples,
                weakSignals = weakSignals,
            )
        }

        private fun NetworkDrainSamples.compareDrainRates(): DrainRateComparison? =
            takeIf { it.hasEnoughSamples }
                ?.let {
                    batteryDrainAnalyzer.compareAverageDrainRates(
                        currentSamples = weakDrainSamples,
                        previousSamples = strongDrainSamples,
                    )
                }?.takeIf { it.changeRatio >= MINIMUM_DRAIN_RATIO }

        private fun buildCandidate(
            now: Long,
            comparison: DrainRateComparison,
            averageWeakSignal: Int,
            confidence: Float,
        ): InsightCandidate =
            InsightCandidate(
                ruleId = ruleId,
                dedupeKey = "cellular_drain:${comparison.percentIncrease.toIncreaseBucket()}",
                type = InsightType.NETWORK,
                priority = resolvePriority(comparison, averageWeakSignal),
                confidence = confidence,
                titleKey = TITLE_KEY,
                bodyKey = BODY_KEY,
                bodyArgs =
                    listOf(
                        comparison.percentIncrease.coerceAtLeast(1).toString(),
                        averageWeakSignal.toString(),
                    ),
                generatedAt = now,
                expiresAt = now + TTL_MS,
                dataWindowStart = now - LOOKBACK_MS,
                dataWindowEnd = now,
                target = InsightTarget.NETWORK,
            )

        private fun resolvePriority(
            comparison: DrainRateComparison,
            averageWeakSignal: Int,
        ): InsightPriority =
            when {
                comparison.changeRatio >= HIGH_PRIORITY_DRAIN_RATIO ||
                    averageWeakSignal <= HIGH_PRIORITY_SIGNAL_DBM -> InsightPriority.HIGH

                else -> InsightPriority.MEDIUM
            }

        private fun Int.toIncreaseBucket(): String =
            when {
                this >= 50 -> "50plus"
                this >= 30 -> "30plus"
                else -> "20plus"
            }

        private val NetworkDrainSamples.hasEnoughSamples: Boolean
            get() =
                weakDrainSamples.size >= MINIMUM_CLASSIFIED_INTERVAL_COUNT &&
                    strongDrainSamples.size >= MINIMUM_CLASSIFIED_INTERVAL_COUNT

        private val NetworkDrainSamples.averageWeakSignal: Int
            get() = weakSignals.average().roundToInt()

        private val NetworkDrainSamples.confidence: Float
            get() =
                (minOf(weakDrainSamples.size, strongDrainSamples.size) / CONFIDENCE_INTERVAL_COUNT.toFloat())
                    .coerceIn(0f, 1f)

        companion object {
            const val RULE_ID = "network_driven_battery_drain"

            private const val TITLE_KEY = "insight_network_drain_title"
            private const val BODY_KEY = "insight_network_drain_body"
            private const val LOOKBACK_MS = 48L * 60L * 60L * 1000L
            private const val TTL_MS = 12L * 60L * 60L * 1000L
            private const val WEAK_SIGNAL_DBM = -110
            private const val STRONG_SIGNAL_DBM = -100
            private const val MINIMUM_BATTERY_READING_COUNT = 9
            private const val MINIMUM_NETWORK_READING_COUNT = 8
            private const val MINIMUM_TOTAL_INTERVAL_COUNT = 8
            private const val MINIMUM_CLASSIFIED_INTERVAL_COUNT = 3
            private const val MINIMUM_DRAIN_RATIO = 1.2f
            private const val HIGH_PRIORITY_DRAIN_RATIO = 1.5f
            private const val HIGH_PRIORITY_SIGNAL_DBM = -115
            private const val CONFIDENCE_INTERVAL_COUNT = 5
        }
    }

private data class NetworkDrainInput(
    val dischargingPairs: List<Pair<BatteryReading, BatteryReading>>,
    val networkReadings: List<NetworkReading>,
)

private data class NetworkDrainSamples(
    val weakDrainSamples: List<DrainSample>,
    val strongDrainSamples: List<DrainSample>,
    val weakSignals: List<Int>,
)
