package com.runcheck.domain.insights.engine

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
            insightRepository.deleteExpired(now)

            rules.forEach { rule ->
                val candidates =
                    rule
                        .evaluate(now)
                        .filter { it.confidence >= MINIMUM_CONFIDENCE }
                insightRepository.replaceRuleResults(rule.ruleId, candidates)
            }

            insightRepository.deleteExpired(now)
        }

        companion object {
            private const val MINIMUM_CONFIDENCE = 0.6f
        }
    }
