package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.model.ChargerProfile
import com.runcheck.domain.model.ChargingSession
import com.runcheck.domain.repository.ChargerRepository
import javax.inject.Inject
import kotlin.math.roundToInt

class ChargerPerformanceRule
    @Inject
    constructor(
        private val chargerRepository: ChargerRepository,
    ) : InsightRule {
        override val ruleId: String = RULE_ID

        override suspend fun evaluate(now: Long): List<InsightCandidate> {
            val chargers = chargerRepository.getChargerProfilesSync()
            val sessions = chargerRepository.getAllSessionsSync()
            if (chargers.size < MINIMUM_CHARGER_COUNT || sessions.isEmpty()) return emptyList()

            val completedRecentSessions =
                sessions.filter { session ->
                    session.endTime != null && session.endTime >= now - LOOKBACK_MS
                }
            if (completedRecentSessions.size < MINIMUM_TOTAL_COMPLETED_SESSIONS) return emptyList()

            val summaries =
                chargers
                    .mapNotNull { charger ->
                        buildSummary(
                            charger = charger,
                            sessions = completedRecentSessions.filter { it.chargerId == charger.id },
                        )
                    }.filter { it.sampleCount >= MINIMUM_SESSION_COUNT_PER_CHARGER }
            if (summaries.size < MINIMUM_CHARGER_COUNT) return emptyList()

            val bestCharger = summaries.maxByOrNull { it.avgPowerMw } ?: return emptyList()
            val weakestCharger =
                summaries
                    .filter { it.chargerId != bestCharger.chargerId }
                    .maxByOrNull { it.lastUsed ?: 0L } ?: return emptyList()

            val powerGapRatio = weakestCharger.avgPowerMw.toFloat() / bestCharger.avgPowerMw.toFloat()
            val percentSlower = ((1f - powerGapRatio) * 100f).roundToInt()
            if (percentSlower < MINIMUM_SLOWER_PERCENT) return emptyList()

            val priority =
                when {
                    percentSlower >= HIGH_PRIORITY_SLOWER_PERCENT -> InsightPriority.HIGH
                    else -> InsightPriority.MEDIUM
                }
            val confidence =
                (
                    minOf(bestCharger.sampleCount, weakestCharger.sampleCount) /
                        CONFIDENCE_SAMPLE_COUNT.toFloat()
                ).coerceIn(0f, 1f)
            val speedBucket =
                when {
                    percentSlower >= 50 -> "50plus"
                    percentSlower >= 35 -> "35plus"
                    else -> "20plus"
                }

            return listOf(
                InsightCandidate(
                    ruleId = ruleId,
                    dedupeKey = "charger:${weakestCharger.chargerId}:$speedBucket",
                    type = InsightType.CHARGER,
                    priority = priority,
                    confidence = confidence,
                    titleKey = TITLE_KEY,
                    bodyKey = BODY_KEY,
                    bodyArgs = listOf(weakestCharger.name, percentSlower.toString()),
                    generatedAt = now,
                    expiresAt = now + TTL_MS,
                    dataWindowStart = now - LOOKBACK_MS,
                    dataWindowEnd = weakestCharger.lastUsed ?: now,
                    target = InsightTarget.CHARGER,
                ),
            )
        }

        private fun buildSummary(
            charger: ChargerProfile,
            sessions: List<ChargingSession>,
        ): ChargerPerformanceSummary? {
            val powerSamples =
                sessions.mapNotNull { session ->
                    session.avgPowerMw ?: session.avgCurrentMa?.let { currentMa ->
                        session.avgVoltageMv?.let { voltageMv ->
                            (currentMa * voltageMv) / 1000
                        }
                    }
                }
            if (powerSamples.size < MINIMUM_SESSION_COUNT_PER_CHARGER) return null

            return ChargerPerformanceSummary(
                chargerId = charger.id,
                name = charger.name,
                avgPowerMw = powerSamples.average().roundToInt(),
                sampleCount = powerSamples.size,
                lastUsed = sessions.maxOfOrNull { it.endTime ?: it.startTime },
            )
        }

        companion object {
            const val RULE_ID = "charger_performance"

            private const val TITLE_KEY = "insight_charger_performance_title"
            private const val BODY_KEY = "insight_charger_performance_body"
            private const val LOOKBACK_MS = 45L * 24L * 60L * 60L * 1000L
            private const val TTL_MS = 24L * 60L * 60L * 1000L
            private const val MINIMUM_CHARGER_COUNT = 2
            private const val MINIMUM_TOTAL_COMPLETED_SESSIONS = 4
            private const val MINIMUM_SESSION_COUNT_PER_CHARGER = 2
            private const val MINIMUM_SLOWER_PERCENT = 20
            private const val HIGH_PRIORITY_SLOWER_PERCENT = 35
            private const val CONFIDENCE_SAMPLE_COUNT = 4
        }
    }

private data class ChargerPerformanceSummary(
    val chargerId: Long,
    val name: String,
    val avgPowerMw: Int,
    val sampleCount: Int,
    val lastUsed: Long?,
)
