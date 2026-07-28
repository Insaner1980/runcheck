package com.runcheck.ui.insights

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsPresentationTest {
    @Test
    fun `all filter separates needs-attention insights from lower-priority insights`() {
        val high = insight(id = 1L, priority = InsightPriority.HIGH)
        val medium = insight(id = 2L, priority = InsightPriority.MEDIUM)
        val low = insight(id = 3L, priority = InsightPriority.LOW)

        val sections = groupInsights(listOf(low, medium, high), InsightFilter.ALL)

        assertEquals(listOf(medium, high), sections.needsAttention)
        assertEquals(listOf(low), sections.other)
    }

    @Test
    fun `important filter excludes low-priority insights`() {
        val high = insight(id = 1L, priority = InsightPriority.HIGH)
        val low = insight(id = 2L, priority = InsightPriority.LOW)

        val sections = groupInsights(listOf(low, high), InsightFilter.IMPORTANT)

        assertEquals(listOf(high), sections.needsAttention)
        assertEquals(emptyList<Insight>(), sections.other)
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
        titleKey = "insight_battery_degradation_title",
        bodyKey = "insight_battery_degradation_body",
        bodyArgs = listOf("90", "80"),
        generatedAt = 0L,
        expiresAt = Long.MAX_VALUE,
        target = InsightTarget.BATTERY,
        seen = false,
        dismissed = false,
    )
}
