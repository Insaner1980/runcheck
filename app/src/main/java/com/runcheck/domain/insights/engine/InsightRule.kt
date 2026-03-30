package com.runcheck.domain.insights.engine

import com.runcheck.domain.insights.model.InsightCandidate

interface InsightRule {
    val ruleId: String

    suspend fun evaluate(now: Long): List<InsightCandidate>
}
