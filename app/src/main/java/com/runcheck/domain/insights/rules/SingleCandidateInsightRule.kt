package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.model.InsightCandidate

/** Shared orchestration for rules that can emit at most one candidate per evaluation. */
abstract class SingleCandidateInsightRule(
    final override val ruleId: String,
) : InsightRule {
    final override suspend fun evaluate(now: Long): List<InsightCandidate> =
        buildCandidate(now)?.let(::listOf).orEmpty()

    protected abstract suspend fun buildCandidate(now: Long): InsightCandidate?
}
