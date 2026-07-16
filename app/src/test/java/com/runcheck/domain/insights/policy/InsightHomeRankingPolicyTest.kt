package com.runcheck.domain.insights.policy

import com.runcheck.domain.insights.engine.InsightHomeRankingPolicy
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

    @Test
    fun `caps selection at three even when requested limit is higher`() {
        val insights =
            InsightTarget.entries
                .filterNot { it == InsightTarget.NONE }
                .mapIndexed { index, target ->
                    insight(id = index + 1L, target = target, type = InsightType.CROSS_CATEGORY)
                }

        val selected = policy.selectHomeInsights(insights, limit = 10)

        assertEquals(3, selected.size)
    }

    @Test
    fun `orders by ranking fields and uses id as deterministic final tie breaker`() {
        val insights =
            listOf(
                insight(id = 4, priority = InsightPriority.MEDIUM, confidence = 1f, generatedAt = 500L),
                insight(id = 3, priority = InsightPriority.HIGH, confidence = 0.8f, generatedAt = 500L),
                insight(id = 2, priority = InsightPriority.HIGH, confidence = 0.9f, generatedAt = 400L),
                insight(id = 1, priority = InsightPriority.HIGH, confidence = 0.9f, generatedAt = 400L),
            )

        val selected = policy.selectHomeInsights(insights, limit = 3)

        assertEquals(listOf(1L, 2L, 3L), selected.map { it.id })
    }

    private fun insight(
        id: Long,
        target: InsightTarget = InsightTarget.BATTERY,
        type: InsightType = InsightType.BATTERY,
        priority: InsightPriority = InsightPriority.HIGH,
        confidence: Float = 0.9f,
        generatedAt: Long = 100L - id,
    ) = Insight(
        id = id,
        ruleId = "rule-$id",
        type = type,
        priority = priority,
        confidence = confidence,
        titleKey = "title_$id",
        bodyKey = "body_$id",
        bodyArgs = emptyList(),
        generatedAt = generatedAt,
        expiresAt = 1_000L,
        target = target,
        seen = false,
        dismissed = false,
    )
}
