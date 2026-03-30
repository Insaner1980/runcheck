package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.StorageGrowthAnalyzer
import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.scoring.HealthScoreCalculator
import javax.inject.Inject

class StoragePressureImpactRule
    @Inject
    constructor(
        private val storageRepository: StorageRepository,
        private val storageGrowthAnalyzer: StorageGrowthAnalyzer,
        private val healthScoreCalculator: HealthScoreCalculator,
    ) : InsightRule {
        override val ruleId: String = RULE_ID

        override suspend fun evaluate(now: Long): List<InsightCandidate> {
            val readings = storageRepository.getReadingsSinceSync(now - LOOKBACK_MS)
            if (readings.size < MINIMUM_READING_COUNT) return emptyList()

            val latest = readings.maxByOrNull { it.timestamp } ?: return emptyList()
            val fillRateBytesPerDay = storageGrowthAnalyzer.calculateFillRateBytesPerDay(readings) ?: return emptyList()
            if (fillRateBytesPerDay <= 0L) return emptyList()

            val availableBytes = latest.availableBytes.coerceAtLeast(0L)
            if (availableBytes <= 0L) return emptyList()

            val estimate =
                storageGrowthAnalyzer.formatEstimate(availableBytes, fillRateBytesPerDay) ?: return emptyList()
            val storageState =
                StorageState(
                    totalBytes = latest.totalBytes,
                    availableBytes = latest.availableBytes,
                    usedBytes = latest.totalBytes - latest.availableBytes,
                    usagePercent =
                        (
                            (
                                (latest.totalBytes - latest.availableBytes).toDouble() /
                                    latest.totalBytes.coerceAtLeast(1).toDouble()
                            ) *
                                100.0
                        ).toFloat(),
                    appsBytes = latest.appsBytes,
                    fillRateBytesPerDay = fillRateBytesPerDay,
                    fillRateEstimate = estimate,
                )
            val storageScore = healthScoreCalculator.calculateStorageScore(storageState)
            if (storageScore > MAX_STORAGE_SCORE_FOR_INSIGHT) return emptyList()

            val daysUntilFull = availableBytes / fillRateBytesPerDay
            if (daysUntilFull > MAX_DAYS_UNTIL_FULL) return emptyList()

            val priority =
                when {
                    storageScore <= HIGH_PRIORITY_STORAGE_SCORE ||
                        daysUntilFull <= HIGH_PRIORITY_DAYS -> InsightPriority.HIGH

                    else -> InsightPriority.MEDIUM
                }
            val confidence = (readings.size / CONFIDENCE_SAMPLE_COUNT.toFloat()).coerceIn(0f, 1f)
            val scoreBucket =
                when {
                    storageScore <= 20 -> "critical"
                    storageScore <= 45 -> "poor"
                    else -> "fair"
                }

            return listOf(
                InsightCandidate(
                    ruleId = ruleId,
                    dedupeKey = "storage_score:$scoreBucket",
                    type = InsightType.STORAGE,
                    priority = priority,
                    confidence = confidence,
                    titleKey = TITLE_KEY,
                    bodyKey = BODY_KEY,
                    bodyArgs = listOf(storageScore.toString(), estimate),
                    generatedAt = now,
                    expiresAt = now + TTL_MS,
                    dataWindowStart = now - LOOKBACK_MS,
                    dataWindowEnd = latest.timestamp,
                    target = InsightTarget.STORAGE,
                ),
            )
        }

        companion object {
            const val RULE_ID = "storage_pressure_impact"

            private const val TITLE_KEY = "insight_storage_impact_title"
            private const val BODY_KEY = "insight_storage_impact_body"
            private const val LOOKBACK_MS = 14L * 24L * 60L * 60L * 1000L
            private const val TTL_MS = 24L * 60L * 60L * 1000L
            private const val MINIMUM_READING_COUNT = 4
            private const val MAX_DAYS_UNTIL_FULL = 45L
            private const val HIGH_PRIORITY_DAYS = 14L
            private const val MAX_STORAGE_SCORE_FOR_INSIGHT = 65
            private const val HIGH_PRIORITY_STORAGE_SCORE = 45
            private const val CONFIDENCE_SAMPLE_COUNT = 8
        }
    }
