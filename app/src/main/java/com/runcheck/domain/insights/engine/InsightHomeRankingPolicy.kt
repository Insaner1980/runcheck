package com.runcheck.domain.insights.engine

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightTarget
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightHomeRankingPolicy
    @Inject
    constructor() {
        fun selectHomeInsights(
            insights: List<Insight>,
            limit: Int,
        ): List<Insight> {
            if (limit <= 0 || insights.isEmpty()) return emptyList()

            val effectiveLimit = minOf(limit, MAX_HOME_INSIGHTS)
            val rankedInsights = insights.sortedWith(HOME_RANKING)
            val selected = mutableListOf<Insight>()
            val usedBuckets = mutableSetOf<String>()

            for (insight in rankedInsights) {
                val bucket = bucketFor(insight)
                if (!usedBuckets.add(bucket)) continue

                selected += insight
                if (selected.size == effectiveLimit) return selected
            }

            if (selected.size == rankedInsights.size || selected.size == effectiveLimit) return selected

            for (insight in rankedInsights) {
                if (insight in selected) continue
                selected += insight
                if (selected.size == effectiveLimit) break
            }

            return selected
        }

        private fun bucketFor(insight: Insight): String =
            when (insight.target) {
                InsightTarget.NONE -> "type:${insight.type.name}"
                else -> "target:${insight.target.name}"
            }

        private companion object {
            const val MAX_HOME_INSIGHTS = 3

            val HOME_RANKING: Comparator<Insight> =
                compareBy<Insight> { it.priority.sortOrder }
                    .thenByDescending { it.confidence }
                    .thenByDescending { it.generatedAt }
                    .thenBy { it.id }
        }
    }
