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

        override suspend fun evaluate(now: Long): List<InsightCandidate> {
            val batteryReadings = batteryRepository.getReadingsSinceSync(now - LOOKBACK_MS)
            val networkReadings = networkRepository.getReadingsSinceSync(now - LOOKBACK_MS)
            if (batteryReadings.size < MINIMUM_BATTERY_READING_COUNT ||
                networkReadings.size < MINIMUM_NETWORK_READING_COUNT
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
                    contexts = networkReadings,
                    contextTimestamp = NetworkReading::timestamp,
                )

            val weakDrainSamples = mutableListOf<DrainSample>()
            val strongDrainSamples = mutableListOf<DrainSample>()
            val weakSignals = mutableListOf<Int>()

            dischargingPairs.zip(alignedIntervals).forEach { (pair, aligned) ->
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

            if (weakDrainSamples.size < MINIMUM_CLASSIFIED_INTERVAL_COUNT ||
                strongDrainSamples.size < MINIMUM_CLASSIFIED_INTERVAL_COUNT
            ) {
                return emptyList()
            }

            val comparison =
                batteryDrainAnalyzer.compareAverageDrainRates(
                    currentSamples = weakDrainSamples,
                    previousSamples = strongDrainSamples,
                ) ?: return emptyList()
            if (comparison.changeRatio < MINIMUM_DRAIN_RATIO) return emptyList()

            val averageWeakSignal = weakSignals.average().roundToInt()
            val priority =
                when {
                    comparison.changeRatio >= HIGH_PRIORITY_DRAIN_RATIO ||
                        averageWeakSignal <= HIGH_PRIORITY_SIGNAL_DBM -> {
                        InsightPriority.HIGH
                    }

                    else -> {
                        InsightPriority.MEDIUM
                    }
                }
            val confidence =
                (minOf(weakDrainSamples.size, strongDrainSamples.size) / CONFIDENCE_INTERVAL_COUNT.toFloat())
                    .coerceIn(0f, 1f)
            val increaseBucket =
                when {
                    comparison.percentIncrease >= 50 -> "50plus"
                    comparison.percentIncrease >= 30 -> "30plus"
                    else -> "20plus"
                }

            return listOf(
                InsightCandidate(
                    ruleId = ruleId,
                    dedupeKey = "cellular_drain:$increaseBucket",
                    type = InsightType.NETWORK,
                    priority = priority,
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
                ),
            )
        }

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
