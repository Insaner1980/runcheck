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

        override suspend fun evaluate(now: Long): List<InsightCandidate> = buildCandidate(now)?.let(::listOf).orEmpty()

        private suspend fun buildCandidate(now: Long): InsightCandidate? {
            val summaries = loadSummaries(now) ?: return null
            val comparison = summaries.findComparison() ?: return null
            if (comparison.percentSlower < MINIMUM_SLOWER_PERCENT) return null

            return comparison.toCandidate(now)
        }

        private suspend fun loadSummaries(now: Long): List<ChargerPerformanceSummary>? {
            val chargers = chargerRepository.getChargerProfilesSync()
            val sessions = chargerRepository.getAllSessionsSync()
            if (chargers.size < MINIMUM_CHARGER_COUNT || sessions.isEmpty()) return null

            val completedRecentSessions =
                sessions.filter { session ->
                    session.endTime != null && session.endTime >= now - LOOKBACK_MS
                }
            if (completedRecentSessions.size < MINIMUM_TOTAL_COMPLETED_SESSIONS) return null

            return chargers
                .mapNotNull { charger ->
                    buildSummary(
                        charger = charger,
                        sessions = completedRecentSessions.filter { it.chargerId == charger.id },
                    )
                }.filter { it.sampleCount >= MINIMUM_SESSION_COUNT_PER_CHARGER }
                .takeIf { it.size >= MINIMUM_CHARGER_COUNT }
        }

        private fun List<ChargerPerformanceSummary>.findComparison(): ChargerPerformanceComparison? {
            val bestCharger = maxByOrNull { it.avgPowerMw }
            val weakestCharger =
                bestCharger?.let { best ->
                    filter { it.chargerId != best.chargerId }.maxByOrNull { it.lastUsed ?: 0L }
                }

            return if (
                bestCharger != null &&
                weakestCharger != null &&
                bestCharger.minPowerMw > 0 &&
                percentSlower(weakestCharger.maxPowerMw, bestCharger.minPowerMw) >= MINIMUM_SLOWER_PERCENT
            ) {
                ChargerPerformanceComparison(
                    bestCharger = bestCharger,
                    weakestCharger = weakestCharger,
                    percentSlower = percentSlower(weakestCharger.avgPowerMw, bestCharger.avgPowerMw),
                )
            } else {
                null
            }
        }

        private fun percentSlower(
            slowerPowerMw: Int,
            fasterPowerMw: Int,
        ): Int =
            if (fasterPowerMw > 0) {
                ((1.0 - slowerPowerMw.toDouble() / fasterPowerMw.toDouble()) * 100.0).roundToInt()
            } else {
                0
            }

        private fun ChargerPerformanceComparison.toCandidate(now: Long): InsightCandidate =
            InsightCandidate(
                ruleId = ruleId,
                dedupeKey = "charger:${weakestCharger.chargerId}:${percentSlower.toSpeedBucket()}",
                type = InsightType.CHARGER,
                priority = percentSlower.toPriority(),
                confidence =
                    (
                        minOf(bestCharger.sampleCount, weakestCharger.sampleCount) /
                            CONFIDENCE_SAMPLE_COUNT.toFloat()
                    ).coerceIn(0f, 1f),
                titleKey = TITLE_KEY,
                bodyKey = BODY_KEY,
                bodyArgs = listOf(weakestCharger.name, percentSlower.toString()),
                generatedAt = now,
                expiresAt = now + TTL_MS,
                dataWindowStart = now - LOOKBACK_MS,
                dataWindowEnd = weakestCharger.lastUsed ?: now,
                target = InsightTarget.CHARGER,
            )

        private fun Int.toPriority(): InsightPriority =
            when {
                this >= HIGH_PRIORITY_SLOWER_PERCENT -> InsightPriority.HIGH
                else -> InsightPriority.MEDIUM
            }

        private fun Int.toSpeedBucket(): String =
            when {
                this >= 50 -> "50plus"
                this >= 35 -> "35plus"
                else -> "20plus"
            }

        private fun buildSummary(
            charger: ChargerProfile,
            sessions: List<ChargingSession>,
        ): ChargerPerformanceSummary? {
            val powerSamples =
                sessions.mapNotNull { it.positivePowerMw() }
            if (powerSamples.size < MINIMUM_SESSION_COUNT_PER_CHARGER) return null

            return ChargerPerformanceSummary(
                chargerId = charger.id,
                name = charger.name,
                avgPowerMw = powerSamples.average().roundToInt(),
                minPowerMw = powerSamples.min(),
                maxPowerMw = powerSamples.max(),
                sampleCount = powerSamples.size,
                lastUsed = sessions.maxOfOrNull { it.endTime ?: it.startTime },
            )
        }

        private fun ChargingSession.positivePowerMw(): Int? =
            avgPowerMw
                ?.takeIf { it > 0 }
                ?: avgCurrentMa?.let { currentMa ->
                    avgVoltageMv?.let { voltageMv ->
                        (currentMa.toLong() * voltageMv.toLong() / 1000L)
                            .takeIf { it in 1..Int.MAX_VALUE.toLong() }
                            ?.toInt()
                    }
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
    val minPowerMw: Int,
    val maxPowerMw: Int,
    val sampleCount: Int,
    val lastUsed: Long?,
)

private data class ChargerPerformanceComparison(
    val bestCharger: ChargerPerformanceSummary,
    val weakestCharger: ChargerPerformanceSummary,
    val percentSlower: Int,
)
