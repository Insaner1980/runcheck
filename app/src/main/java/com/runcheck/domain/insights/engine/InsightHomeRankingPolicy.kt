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

            val selected = mutableListOf<Insight>()
            val usedBuckets = mutableSetOf<String>()

            for (insight in insights) {
                val bucket = bucketFor(insight)
                if (!usedBuckets.add(bucket)) continue

                selected += insight
                if (selected.size == limit) return selected
            }

            if (selected.size == insights.size || selected.size == limit) return selected

            for (insight in insights) {
                if (insight in selected) continue
                selected += insight
                if (selected.size == limit) break
            }

            return selected
        }

        private fun bucketFor(insight: Insight): String =
            when (insight.target) {
                InsightTarget.NONE -> "type:${insight.type.name}"
                else -> "target:${insight.target.name}"
            }
    }
