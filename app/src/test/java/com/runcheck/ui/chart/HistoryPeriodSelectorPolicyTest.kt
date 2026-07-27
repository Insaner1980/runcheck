package com.runcheck.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryPeriodSelectorPolicyTest {
    @Test
    fun `empty selector has no scroll target announcement or animation`() {
        val policy =
            historyPeriodSelectorPolicy(
                optionLabels = emptyList(),
                selectedIndex = 4,
                viewportWidthDp = 411,
                fontScale = 2f,
                reducedMotion = false,
            )

        assertFalse(policy.isScrollable)
        assertEquals(0, policy.selectedItemScrollTarget)
        assertEquals(0, policy.selectedItemPosition)
        assertEquals(0, policy.optionCount)
        assertFalse(policy.announcesSelectedState)
        assertFalse(policy.animateSelectedItemScroll)
    }

    @Test
    fun `out of range selection clamps to the final available period`() {
        val policy =
            historyPeriodSelectorPolicy(
                optionLabels = listOf("24H", "7D", "30D"),
                selectedIndex = 99,
                viewportWidthDp = 411,
                fontScale = 1f,
                reducedMotion = false,
            )

        assertEquals(2, policy.selectedItemScrollTarget)
        assertEquals(3, policy.selectedItemPosition)
        assertEquals(3, policy.optionCount)
    }

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
