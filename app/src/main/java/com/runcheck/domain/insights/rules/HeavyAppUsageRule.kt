package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.domain.repository.AppBatteryUsageRepository
import javax.inject.Inject

class HeavyAppUsageRule
    @Inject
    constructor(
        private val appBatteryUsageRepository: AppBatteryUsageRepository,
    ) : InsightRule {
        override val ruleId: String = RULE_ID

        override suspend fun evaluate(now: Long): List<InsightCandidate> {
            val readings = appBatteryUsageRepository.getUsageSinceSync(now - LOOKBACK_MS)
            if (readings.isEmpty()) return emptyList()

            val byPackage = readings.groupBy { it.packageName }
            val aggregated =
                byPackage.values.mapNotNull { entries ->
                    val totalForegroundTimeMs = entries.sumOf { it.foregroundTimeMs }
                    val latest = entries.maxByOrNull { it.timestamp } ?: return@mapNotNull null
                    AggregatedUsage(
                        packageName = latest.packageName,
                        appLabel = latest.appLabel,
                        totalForegroundTimeMs = totalForegroundTimeMs,
                    )
                }
            val topApp = aggregated.maxByOrNull { it.totalForegroundTimeMs } ?: return emptyList()
            if (topApp.totalForegroundTimeMs < MIN_FOREGROUND_TIME_MS) return emptyList()

            val totalForegroundTimeMs = aggregated.sumOf { it.totalForegroundTimeMs }.coerceAtLeast(1L)
            val sharePercent = ((topApp.totalForegroundTimeMs * 100.0) / totalForegroundTimeMs).toInt()
            if (sharePercent < MIN_SHARE_PERCENT) return emptyList()

            val durationLabel = formatDuration(topApp.totalForegroundTimeMs)
            val priority =
                when {
                    topApp.totalForegroundTimeMs >= HIGH_PRIORITY_TIME_MS ||
                        sharePercent >= HIGH_PRIORITY_SHARE_PERCENT -> InsightPriority.HIGH

                    else -> InsightPriority.MEDIUM
                }
            val confidence = (aggregated.size / CONFIDENCE_APP_COUNT.toFloat()).coerceIn(0f, 1f)
            val shareBucket =
                when {
                    sharePercent >= 80 -> "80plus"
                    sharePercent >= 70 -> "70plus"
                    else -> "60plus"
                }

            return listOf(
                InsightCandidate(
                    ruleId = ruleId,
                    dedupeKey = "${topApp.packageName}:$shareBucket",
                    type = InsightType.APP_USAGE,
                    priority = priority,
                    confidence = confidence,
                    titleKey = TITLE_KEY,
                    bodyKey = BODY_KEY,
                    bodyArgs = listOf(topApp.appLabel, sharePercent.toString(), durationLabel),
                    generatedAt = now,
                    expiresAt = now + TTL_MS,
                    dataWindowStart = now - LOOKBACK_MS,
                    dataWindowEnd = now,
                    target = InsightTarget.APP_USAGE,
                ),
            )
        }

        private fun formatDuration(durationMs: Long): String {
            val totalMinutes = durationMs / 60_000L
            val hours = totalMinutes / 60L
            val minutes = totalMinutes % 60L
            return if (hours > 0L) {
                if (minutes > 0L) {
                    "${hours}h ${minutes}m"
                } else {
                    "${hours}h"
                }
            } else {
                "${minutes}m"
            }
        }

        private data class AggregatedUsage(
            val packageName: String,
            val appLabel: String,
            val totalForegroundTimeMs: Long,
        )

        companion object {
            const val RULE_ID = "heavy_app_usage"

            private const val TITLE_KEY = "insight_app_usage_title"
            private const val BODY_KEY = "insight_app_usage_body"
            private const val LOOKBACK_MS = 24L * 60L * 60L * 1000L
            private const val TTL_MS = 12L * 60L * 60L * 1000L
            private const val MIN_FOREGROUND_TIME_MS = 2L * 60L * 60L * 1000L
            private const val HIGH_PRIORITY_TIME_MS = 4L * 60L * 60L * 1000L
            private const val MIN_SHARE_PERCENT = 60
            private const val HIGH_PRIORITY_SHARE_PERCENT = 75
            private const val CONFIDENCE_APP_COUNT = 5
        }
    }
