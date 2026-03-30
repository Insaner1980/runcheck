package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.repository.NetworkRepository
import javax.inject.Inject
import kotlin.math.roundToInt

class NetworkSignalPatternRule
    @Inject
    constructor(
        private val networkRepository: NetworkRepository,
    ) : InsightRule {
        override val ruleId: String = RULE_ID

        override suspend fun evaluate(now: Long): List<InsightCandidate> {
            val readings = networkRepository.getReadingsSinceSync(now - LOOKBACK_MS)
            val cellularSamples =
                readings.filter { reading ->
                    reading.type == ConnectionType.CELLULAR.name && reading.signalDbm != null
                }
            if (cellularSamples.size < MINIMUM_SAMPLE_COUNT) return emptyList()

            val weakSamples = cellularSamples.filter { it.signalDbm != null && it.signalDbm <= WEAK_SIGNAL_DBM }
            if (weakSamples.size < MINIMUM_WEAK_SAMPLE_COUNT) return emptyList()

            val weakRatio = weakSamples.size / cellularSamples.size.toFloat()
            if (weakRatio < MINIMUM_WEAK_RATIO) return emptyList()

            val averageWeakSignal =
                weakSamples
                    .mapNotNull { it.signalDbm }
                    .average()
                    .roundToInt()
            val weakPercent = (weakRatio * 100f).roundToInt()
            val priority =
                when {
                    weakRatio >= HIGH_PRIORITY_RATIO || averageWeakSignal <= HIGH_PRIORITY_DBM -> InsightPriority.HIGH
                    else -> InsightPriority.MEDIUM
                }
            val confidence = (cellularSamples.size / CONFIDENCE_SAMPLE_COUNT.toFloat()).coerceIn(0f, 1f)
            val bucket =
                when {
                    weakPercent >= 90 -> "90plus"
                    weakPercent >= 75 -> "75plus"
                    else -> "60plus"
                }

            return listOf(
                InsightCandidate(
                    ruleId = ruleId,
                    dedupeKey = "cellular:$bucket",
                    type = InsightType.NETWORK,
                    priority = priority,
                    confidence = confidence,
                    titleKey = TITLE_KEY,
                    bodyKey = BODY_KEY,
                    bodyArgs = listOf(weakPercent.toString(), averageWeakSignal.toString()),
                    generatedAt = now,
                    expiresAt = now + TTL_MS,
                    dataWindowStart = now - LOOKBACK_MS,
                    dataWindowEnd = now,
                    target = InsightTarget.NETWORK,
                ),
            )
        }

        companion object {
            const val RULE_ID = "network_signal_pattern"

            private const val TITLE_KEY = "insight_network_signal_pattern_title"
            private const val BODY_KEY = "insight_network_signal_pattern_body"
            private const val LOOKBACK_MS = 3L * 24L * 60L * 60L * 1000L
            private const val TTL_MS = 12L * 60L * 60L * 1000L
            private const val WEAK_SIGNAL_DBM = -110
            private const val HIGH_PRIORITY_DBM = -115
            private const val MINIMUM_SAMPLE_COUNT = 6
            private const val MINIMUM_WEAK_SAMPLE_COUNT = 4
            private const val MINIMUM_WEAK_RATIO = 0.6f
            private const val HIGH_PRIORITY_RATIO = 0.75f
            private const val CONFIDENCE_SAMPLE_COUNT = 10
        }
    }
