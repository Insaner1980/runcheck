package com.runcheck.testutil

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType

fun insightFixture(
    id: Long = 1L,
    target: InsightTarget = InsightTarget.BATTERY,
    seen: Boolean = true,
): Insight =
    Insight(
        id = id,
        ruleId = "rule_$id",
        type = InsightType.BATTERY,
        priority = InsightPriority.HIGH,
        confidence = 0.9f,
        titleKey = "title",
        bodyKey = "body",
        bodyArgs = emptyList(),
        generatedAt = 0L,
        expiresAt = Long.MAX_VALUE,
        target = target,
        seen = seen,
        dismissed = false,
    )
