package com.runcheck.ui.home.insights

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeInsightsContentPolicyTest {
    @Test
    fun `empty insight list keeps the section reachable through its empty state`() {
        assertTrue(homeInsightsSectionShowsEmptyState(emptyList()))
    }

    @Test
    fun `non-empty insight list renders rows instead of the empty state`() {
        assertFalse(homeInsightsSectionShowsEmptyState(listOf(testInsight())))
    }

    private fun testInsight(): Insight =
        Insight(
            id = 1L,
            ruleId = "rule",
            type = InsightType.BATTERY,
            priority = InsightPriority.HIGH,
            confidence = 0.9f,
            titleKey = "title",
            bodyKey = "body",
            bodyArgs = emptyList(),
            generatedAt = 0L,
            expiresAt = Long.MAX_VALUE,
            target = InsightTarget.BATTERY,
            seen = false,
            dismissed = false,
        )
}
