package com.runcheck.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryPeriodSelectorPolicyTest {
    @Test
    fun `fifth period remains reachable and announced at compact width with large text`() {
        val policy =
            historyPeriodSelectorPolicy(
                optionLabels = listOf("24H", "7D", "30D", "90D", "1Y"),
                selectedIndex = 4,
                viewportWidthDp = 411,
                fontScale = 2f,
                reducedMotion = false,
            )

        assertTrue(policy.isScrollable)
        assertEquals(4, policy.selectedItemScrollTarget)
        assertTrue(policy.animateSelectedItemScroll)
        assertEquals(5, policy.selectedItemPosition)
        assertEquals(5, policy.optionCount)
        assertTrue(policy.announcesSelectedState)
        assertFalse(policy.allowsLabelTruncation)
    }

    @Test
    fun `reduced motion keeps the selected period reachable without animation`() {
        val policy =
            historyPeriodSelectorPolicy(
                optionLabels = listOf("24H", "7D", "30D", "90D", "1Y"),
                selectedIndex = 4,
                viewportWidthDp = 411,
                fontScale = 2f,
                reducedMotion = true,
            )

        assertEquals(4, policy.selectedItemScrollTarget)
        assertFalse(policy.animateSelectedItemScroll)
    }
}
