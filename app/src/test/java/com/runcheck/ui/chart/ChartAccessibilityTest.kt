package com.runcheck.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ChartAccessibilityTest {
    @Test
    fun `buildChartAccessibilitySnapshot formats values and detects increasing trend`() {
        val snapshot =
            buildChartAccessibilitySnapshot(
                chartData = listOf(35f, 52f, 89f),
                unit = "%",
                decimals = 0,
            )

        assertNotNull(snapshot)
        assertEquals("35%", snapshot?.minimumValue)
        assertEquals("89%", snapshot?.maximumValue)
        assertEquals("89%", snapshot?.latestValue)
        assertEquals(ChartTrendDirection.INCREASING, snapshot?.trendDirection)
    }

    @Test
    fun `buildChartAccessibilitySnapshot detects decreasing trend`() {
        val snapshot =
            buildChartAccessibilitySnapshot(
                chartData = listOf(84f, 62f, 41f),
                unit = " mA",
                decimals = 0,
            )

        assertEquals(ChartTrendDirection.DECREASING, snapshot?.trendDirection)
    }

    @Test
    fun `buildChartAccessibilitySnapshot treats small changes as stable`() {
        val snapshot =
            buildChartAccessibilitySnapshot(
                chartData = listOf(4.12f, 4.09f, 4.11f),
                unit = " V",
                decimals = 2,
            )

        assertEquals("4.09 V", snapshot?.minimumValue)
        assertEquals("4.12 V", snapshot?.maximumValue)
        assertEquals("4.11 V", snapshot?.latestValue)
        assertEquals(ChartTrendDirection.STABLE, snapshot?.trendDirection)
    }
}
