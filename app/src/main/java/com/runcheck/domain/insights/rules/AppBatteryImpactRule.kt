package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.repository.AppBatteryUsageRepository
import javax.inject.Inject
import kotlin.math.roundToInt

class AppBatteryImpactRule
    @Inject
    constructor(
        private val appBatteryUsageRepository: AppBatteryUsageRepository,
    ) : InsightRule {
        override val ruleId: String = RULE_ID

        override suspend fun evaluate(now: Long): List<InsightCandidate> {
            val readings = appBatteryUsageRepository.getUsageSinceSync(now - LOOKBACK_MS)
            if (readings.isEmpty()) return emptyList()

            val entriesWithDrain = readings.filter { it.estimatedDrainMah != null }
            if (entriesWithDrain.size < MINIMUM_DRAIN_ENTRY_COUNT) return emptyList()

            val aggregated =
                entriesWithDrain
                    .groupBy { it.packageName }
                    .values
                    .mapNotNull { entries ->
                        val latest = entries.maxByOrNull { it.timestamp } ?: return@mapNotNull null
                        AggregatedDrain(
                            packageName = latest.packageName,
                            appLabel = latest.appLabel,
                            totalDrainMah = entries.sumOf { it.estimatedDrainMah?.toDouble() ?: 0.0 }.toFloat(),
                            sampleCount = entries.size,
                        )
                    }
            val topApp = aggregated.maxByOrNull { it.totalDrainMah } ?: return emptyList()
            if (topApp.totalDrainMah < MINIMUM_TOTAL_DRAIN_MAH) return emptyList()

            val totalDrainMah = aggregated.sumOf { it.totalDrainMah.toDouble() }.toFloat().coerceAtLeast(0.1f)
            val drainSharePercent = ((topApp.totalDrainMah * 100f) / totalDrainMah).roundToInt()
            if (drainSharePercent < MINIMUM_DRAIN_SHARE_PERCENT) return emptyList()

            val completeness = entriesWithDrain.size / readings.size.toFloat()
            val confidence =
                ((topApp.sampleCount / CONFIDENCE_SAMPLE_COUNT.toFloat()) * completeness)
                    .coerceIn(0f, 1f)
            val priority =
                when {
                    topApp.totalDrainMah >= HIGH_PRIORITY_DRAIN_MAH ||
                        drainSharePercent >= HIGH_PRIORITY_DRAIN_SHARE_PERCENT -> {
                        InsightPriority.HIGH
                    }

                    else -> {
                        InsightPriority.MEDIUM
                    }
                }
            val shareBucket =
                when {
                    drainSharePercent >= 70 -> "70plus"
                    drainSharePercent >= 50 -> "50plus"
                    else -> "35plus"
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
                    bodyArgs =
                        listOf(
                            topApp.appLabel,
                            topApp.totalDrainMah.roundToInt().toString(),
                            drainSharePercent.toString(),
                        ),
                    generatedAt = now,
                    expiresAt = now + TTL_MS,
                    dataWindowStart = now - LOOKBACK_MS,
                    dataWindowEnd = now,
                    target = InsightTarget.APP_USAGE,
                ),
            )
        }

        private data class AggregatedDrain(
            val packageName: String,
            val appLabel: String,
            val totalDrainMah: Float,
            val sampleCount: Int,
        )

        companion object {
            const val RULE_ID = "app_battery_impact"

            private const val TITLE_KEY = "insight_app_battery_impact_title"
            private const val BODY_KEY = "insight_app_battery_impact_body"
            private const val LOOKBACK_MS = 24L * 60L * 60L * 1000L
            private const val TTL_MS = 12L * 60L * 60L * 1000L
            private const val MINIMUM_DRAIN_ENTRY_COUNT = 3
            private const val MINIMUM_TOTAL_DRAIN_MAH = 150f
            private const val HIGH_PRIORITY_DRAIN_MAH = 250f
            private const val MINIMUM_DRAIN_SHARE_PERCENT = 35
            private const val HIGH_PRIORITY_DRAIN_SHARE_PERCENT = 55
            private const val CONFIDENCE_SAMPLE_COUNT = 4
        }
    }
