package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightMessageId
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.model.ThermalStatusPersistence
import com.runcheck.domain.repository.ThrottlingRepository
import javax.inject.Inject

class RecurringThermalThrottlingRule
    @Inject
    constructor(
        private val throttlingRepository: ThrottlingRepository,
    ) : InsightRule {
        override val ruleId: String = RULE_ID

        override suspend fun evaluate(now: Long): List<InsightCandidate> {
            val windowStart = now - LOOKBACK_MS
            val events =
                throttlingRepository
                    .getEventsSinceSync(windowStart)
                    .filter { event -> event.timestamp <= now }
            if (events.isEmpty()) return emptyList()

            val severeEvents =
                events.filter { event ->
                    ThermalStatusPersistence.fromId(event.thermalStatus)?.let { it >= ThermalStatus.SEVERE } == true
                }
            if (severeEvents.size < MINIMUM_EVENT_COUNT) return emptyList()

            val peakStatus =
                severeEvents
                    .mapNotNull { event -> ThermalStatusPersistence.fromId(event.thermalStatus) }
                    .maxOrNull() ?: return emptyList()
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
                    messageId = InsightMessageId.RECURRING_THERMAL_THROTTLING,
                    bodyArgs =
                        listOf(
                            severeEvents.size.toString(),
                            ThermalStatusPersistence.toId(peakStatus).lowercase(),
                            peakTemp.toInt().toString(),
                        ),
                    generatedAt = now,
                    expiresAt = now + TTL_MS,
                    dataWindowStart = windowStart,
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
            return "${ThermalStatusPersistence.toId(peakStatus).lowercase()}:$countBucket"
        }

        companion object {
            const val RULE_ID = "recurring_thermal_throttling"

            private const val LOOKBACK_MS = 7L * 24L * 60L * 60L * 1000L
            private const val TTL_MS = 24L * 60L * 60L * 1000L
            private const val MINIMUM_EVENT_COUNT = 3
            private const val CONFIDENCE_EVENT_COUNT = 5
            private const val HIGH_PRIORITY_DURATION_MS = 15L * 60L * 1000L
        }
    }
