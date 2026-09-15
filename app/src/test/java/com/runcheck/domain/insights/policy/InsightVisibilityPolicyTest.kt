package com.runcheck.domain.insights.policy

import com.runcheck.domain.insights.model.InsightTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightVisibilityPolicyTest {
    @Test
    fun `defines Pro access for every Insight target`() {
        val expected =
            mapOf(
                InsightTarget.NONE to false,
                InsightTarget.BATTERY to false,
                InsightTarget.THERMAL to false,
                InsightTarget.NETWORK to false,
                InsightTarget.STORAGE to false,
                InsightTarget.APP_USAGE to true,
                InsightTarget.CHARGER to true,
            )

        assertEquals(InsightTarget.entries.toSet(), expected.keys)
        expected.forEach { (target, requiresProAccess) ->
            assertEquals(target.name, requiresProAccess, target.requiresProAccess())
        }
    }
}
