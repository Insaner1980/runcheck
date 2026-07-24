package com.runcheck.ui.insights

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightFilterTest {
    @Test
    fun `all filter keeps every visible insight`() {
        val insights = listOf(insight(1, InsightPriority.HIGH), insight(2, InsightPriority.LOW))

        assertEquals(insights, InsightFilter.ALL.applyTo(insights))
    }

    @Test
    fun `important filter keeps high and medium priorities`() {
        val high = insight(1, InsightPriority.HIGH)
        val medium = insight(2, InsightPriority.MEDIUM)
        val low = insight(3, InsightPriority.LOW)

        assertEquals(
            listOf(high, medium),
            InsightFilter.IMPORTANT.applyTo(listOf(high, medium, low)),
        )
    }

    private fun insight(
        id: Long,
        priority: InsightPriority,
    ) = Insight(
        id = id,
        ruleId = "rule-$id",
        type = InsightType.BATTERY,
        priority = priority,
        confidence = 0.9f,
        titleKey = "title",
        bodyKey = "body",
        bodyArgs = emptyList(),
        generatedAt = id,
        expiresAt = Long.MAX_VALUE,
        target = InsightTarget.BATTERY,
        seen = false,
        dismissed = false,
    )
}
