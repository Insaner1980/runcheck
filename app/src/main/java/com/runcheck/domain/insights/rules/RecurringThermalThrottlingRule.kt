package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.repository.ThrottlingRepository
import javax.inject.Inject

class RecurringThermalThrottlingRule
    @Inject
    constructor(
        private val throttlingRepository: ThrottlingRepository,
    ) : InsightRule {
        override val ruleId: String = RULE_ID

        override suspend fun evaluate(now: Long): List<InsightCandidate> {
            val events = throttlingRepository.getEventsSinceSync(now - LOOKBACK_MS)
            if (events.isEmpty()) return emptyList()

            val severeEvents =
                events.filter { event ->
                    parseThermalStatus(event.thermalStatus) >= ThermalStatus.SEVERE
                }
            if (severeEvents.size < MINIMUM_EVENT_COUNT) return emptyList()

            val peakStatus =
                severeEvents.maxOfOrNull { event ->
                    parseThermalStatus(event.thermalStatus)
                } ?: return emptyList()
            val peakTemp = severeEvents.maxOfOrNull { it.batteryTempC } ?: return emptyList()
            val totalDurationMs = severeEvents.sumOf { it.durationMs ?: 0L }
            val confidence = (severeEvents.size / CONFIDENCE_EVENT_COUNT.toFloat()).coerceIn(0f, 1f)
            val priority =
                when {
                    peakStatus >= ThermalStatus.CRITICAL ||
                        totalDurationMs >= HIGH_PRIORITY_DURATION_MS -> InsightPriority.HIGH

                    else -> InsightPriority.MEDIUM
                }

            return listOf(
                InsightCandidate(
                    ruleId = ruleId,
                    dedupeKey = buildDedupeKey(severeEvents.size, peakStatus),
                    type = InsightType.THERMAL,
                    priority = priority,
                    confidence = confidence,
                    titleKey = TITLE_KEY,
                    bodyKey = BODY_KEY,
                    bodyArgs =
                        listOf(
                            severeEvents.size.toString(),
                            peakStatus.name.lowercase(),
                            peakTemp.toInt().toString(),
                        ),
                    generatedAt = now,
                    expiresAt = now + TTL_MS,
                    dataWindowStart = now - LOOKBACK_MS,
                    dataWindowEnd = now,
                    target = InsightTarget.THERMAL,
                ),
            )
        }

        private fun buildDedupeKey(
            count: Int,
            peakStatus: ThermalStatus,
        ): String {
            val countBucket =
                when {
                    count >= 6 -> "6plus"
                    count >= 4 -> "4plus"
                    else -> "3plus"
                }
            return "${peakStatus.name.lowercase()}:$countBucket"
        }

        private fun parseThermalStatus(raw: String): ThermalStatus =
            ThermalStatus.entries.firstOrNull { it.name == raw } ?: ThermalStatus.NONE

        companion object {
            const val RULE_ID = "recurring_thermal_throttling"

            private const val TITLE_KEY = "insight_thermal_throttling_title"
            private const val BODY_KEY = "insight_thermal_throttling_body"
            private const val LOOKBACK_MS = 7L * 24L * 60L * 60L * 1000L
            private const val TTL_MS = 24L * 60L * 60L * 1000L
            private const val MINIMUM_EVENT_COUNT = 3
            private const val CONFIDENCE_EVENT_COUNT = 5
            private const val HIGH_PRIORITY_DURATION_MS = 15L * 60L * 1000L
        }
    }
