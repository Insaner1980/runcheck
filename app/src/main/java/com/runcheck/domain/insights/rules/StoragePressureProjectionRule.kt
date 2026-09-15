package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.StorageFillProjection
import com.runcheck.domain.insights.analysis.StorageGrowthAnalyzer
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightMessageId
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.repository.StorageRepository
import javax.inject.Inject

class StoragePressureProjectionRule
    @Inject
    constructor(
        storageRepository: StorageRepository,
        storageGrowthAnalyzer: StorageGrowthAnalyzer,
    ) : StoragePressureInsightRule(
            ruleId = RULE_ID,
            repository = storageRepository,
            analyzer = storageGrowthAnalyzer,
            maxDaysUntilFull = MAX_DAYS_UNTIL_FULL,
        ) {
        override fun buildStorageCandidate(
            now: Long,
            readingCount: Int,
            projection: StorageFillProjection,
        ): InsightCandidate = projection.toCandidate(now, readingCount)

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
                messageId = InsightMessageId.STORAGE_PRESSURE_PROJECTION,
                bodyArgs = listOf(estimate),
                generatedAt = now,
                expiresAt = now + TTL_MS,
                dataWindowStart = now - STORAGE_PRESSURE_LOOKBACK_MS,
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

            private const val TTL_MS = 24L * 60L * 60L * 1000L
            private const val MAX_DAYS_UNTIL_FULL = 30L
            private const val HIGH_PRIORITY_DAYS = 14L
            private const val HIGH_PRIORITY_USED_PERCENT = 90
            private const val CONFIDENCE_SAMPLE_COUNT = 10
        }
    }
