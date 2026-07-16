package com.runcheck.domain.insights.engine

import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.rules.RecurringThermalThrottlingRule
import com.runcheck.domain.insights.rules.ThermalPatternDetectionRule
import com.runcheck.domain.repository.InsightRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightEngine
    @Inject
    constructor(
        private val rules: Set<@JvmSuppressWildcards InsightRule>,
        private val insightRepository: InsightRepository,
    ) {
        suspend fun generateInsights(now: Long = System.currentTimeMillis()) {
            val candidatesByRule =
                rules.associate { rule ->
                    val candidates =
                        rule
                            .evaluate(now)
                            .filter { it.confidence >= MINIMUM_CONFIDENCE }
                    rule.ruleId to candidates
                }
            insightRepository.replaceGenerationResults(candidatesByRule.withoutOverlappingThermalPattern(), now)
        }

        private fun Map<String, List<InsightCandidate>>.withoutOverlappingThermalPattern():
            Map<String, List<InsightCandidate>> {
            val recurringThrottling = this[RecurringThermalThrottlingRule.RULE_ID]
            val thermalPattern = this[ThermalPatternDetectionRule.RULE_ID]
            if (recurringThrottling.isNullOrEmpty() || thermalPattern.isNullOrEmpty()) return this

            return this + (ThermalPatternDetectionRule.RULE_ID to emptyList())
        }

        companion object {
            private const val MINIMUM_CONFIDENCE = 0.6f
        }
    }
