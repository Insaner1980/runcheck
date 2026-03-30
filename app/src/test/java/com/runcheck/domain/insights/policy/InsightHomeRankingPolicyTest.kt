package com.runcheck.domain.insights.policy

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightHomeRankingPolicyTest {
    private val policy = InsightHomeRankingPolicy()

    @Test
    fun `keeps one strongest insight per target before filling remaining slots`() {
        val insights =
            listOf(
                insight(id = 1, target = InsightTarget.BATTERY, type = InsightType.BATTERY),
                insight(id = 2, target = InsightTarget.BATTERY, type = InsightType.CROSS_CATEGORY),
                insight(id = 3, target = InsightTarget.THERMAL, type = InsightType.THERMAL),
                insight(id = 4, target = InsightTarget.NETWORK, type = InsightType.NETWORK),
            )

        val selected = policy.selectHomeInsights(insights, limit = 3)

        assertEquals(listOf(1L, 3L, 4L), selected.map { it.id })
    }

    @Test
    fun `uses type as fallback bucket for non navigable insights`() {
        val insights =
            listOf(
                insight(id = 1, target = InsightTarget.NONE, type = InsightType.CROSS_CATEGORY),
                insight(id = 2, target = InsightTarget.NONE, type = InsightType.CROSS_CATEGORY),
                insight(id = 3, target = InsightTarget.NONE, type = InsightType.STORAGE),
            )

        val selected = policy.selectHomeInsights(insights, limit = 3)

        assertEquals(listOf(1L, 3L, 2L), selected.map { it.id })
    }

    private fun insight(
        id: Long,
        target: InsightTarget,
        type: InsightType,
    ) = Insight(
        id = id,
        ruleId = "rule-$id",
        type = type,
        priority = InsightPriority.HIGH,
        confidence = 0.9f,
        titleKey = "title_$id",
        bodyKey = "body_$id",
        bodyArgs = emptyList(),
        generatedAt = 100L - id,
        expiresAt = 1_000L,
        target = target,
        seen = false,
        dismissed = false,
    )
}
