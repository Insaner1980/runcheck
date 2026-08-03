package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.DrainRateComparison
import com.runcheck.domain.insights.model.InsightCandidate

abstract class ContextualBatteryDrainRule<Samples>(
    ruleId: String,
) : SingleCandidateInsightRule(ruleId) {
    protected abstract suspend fun loadSamples(now: Long): Samples?

    protected abstract fun compareSamples(samples: Samples): DrainRateComparison?

    protected abstract fun createCandidate(
        now: Long,
        comparison: DrainRateComparison,
        samples: Samples,
    ): InsightCandidate?

    final override suspend fun buildCandidate(now: Long): InsightCandidate? {
        val samples = loadSamples(now) ?: return null
        val comparison = compareSamples(samples) ?: return null
        return createCandidate(now, comparison, samples)
    }
}
