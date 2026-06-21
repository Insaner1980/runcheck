package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.StorageFillProjection
import com.runcheck.domain.insights.analysis.StorageGrowthAnalyzer
import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.repository.StorageRepository
import javax.inject.Inject

class StoragePressureProjectionRule
    @Inject
    constructor(
        private val storageRepository: StorageRepository,
        private val storageGrowthAnalyzer: StorageGrowthAnalyzer,
    ) : InsightRule {
        override val ruleId: String = RULE_ID

        override suspend fun evaluate(now: Long): List<InsightCandidate> = buildCandidate(now)?.let(::listOf).orEmpty()

        private suspend fun buildCandidate(now: Long): InsightCandidate? {
            val readings = storageRepository.getReadingsSinceSync(now - LOOKBACK_MS)
            if (readings.size < MINIMUM_READING_COUNT) return null

            val projection = storageGrowthAnalyzer.calculateProjection(readings) ?: return null
            if (projection.daysUntilFull > MAX_DAYS_UNTIL_FULL) return null

            return projection.toCandidate(now, readings.size)
        }

        private fun StorageFillProjection.toCandidate(
            now: Long,
            readingCount: Int,
        ): InsightCandidate =
            InsightCandidate(
                ruleId = ruleId,
                dedupeKey = daysUntilFull.toBucket(),
                type = InsightType.STORAGE,
                priority = resolvePriority(daysUntilFull, usedPercent),
                confidence = (readingCount / CONFIDENCE_SAMPLE_COUNT.toFloat()).coerceIn(0f, 1f),
                titleKey = TITLE_KEY,
                bodyKey = BODY_KEY,
                bodyArgs = listOf(estimate),
                generatedAt = now,
                expiresAt = now + TTL_MS,
                dataWindowStart = now - LOOKBACK_MS,
                dataWindowEnd = latest.timestamp,
                target = InsightTarget.STORAGE,
            )

        private fun resolvePriority(
            daysUntilFull: Long,
            usedPercent: Int,
        ): InsightPriority =
            when {
                daysUntilFull <= HIGH_PRIORITY_DAYS ||
                    usedPercent >= HIGH_PRIORITY_USED_PERCENT -> InsightPriority.HIGH

                else -> InsightPriority.MEDIUM
            }

        private fun Long.toBucket(): String =
            when {
                this <= 7 -> "7d"
                this <= 14 -> "14d"
                else -> "30d"
            }

        companion object {
            const val RULE_ID = "storage_pressure_projection"

            private const val TITLE_KEY = "insight_storage_pressure_title"
            private const val BODY_KEY = "insight_storage_pressure_body"
            private const val LOOKBACK_MS = 14L * 24L * 60L * 60L * 1000L
            private const val TTL_MS = 24L * 60L * 60L * 1000L
            private const val MINIMUM_READING_COUNT = 4
            private const val MAX_DAYS_UNTIL_FULL = 30L
            private const val HIGH_PRIORITY_DAYS = 14L
            private const val HIGH_PRIORITY_USED_PERCENT = 90
            private const val CONFIDENCE_SAMPLE_COUNT = 10
        }
    }
