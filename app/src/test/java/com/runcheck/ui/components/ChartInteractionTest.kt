package com.runcheck.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartInteractionTest {
    @Test
    fun `axis labels wider than a narrow chart stay at its left edge`() {
        assertEquals(40f, chartXLabelLeft(60f, 80f, 40f, 40f), 0f)
        assertEquals(40f, chartXLabelLeft(60f, 40f, 40f, 40f), 0f)
    }

    @Test
    fun `axis labels fit at both edges and remain centered inside the chart`() {
        assertEquals(10f, chartXLabelLeft(10f, 20f, 10f, 100f), 0f)
        assertEquals(50f, chartXLabelLeft(60f, 20f, 10f, 100f), 0f)
        assertEquals(90f, chartXLabelLeft(110f, 20f, 10f, 100f), 0f)
    }

    @Test
    fun `nearest chart point rounds correctly and includes both edges`() {
        assertEquals(0, nearestChartPointIndex(-10f, 10f, 100f, 5))
        assertEquals(2, nearestChartPointIndex(59f, 10f, 100f, 5))
        assertEquals(4, nearestChartPointIndex(120f, 10f, 100f, 5))
    }

    @Test
    fun `appended point count handles trimming and multiple arrivals`() {
        val previous = (1..60).map(Int::toFloat)
        val current = (4..63).map(Int::toFloat)

        assertEquals(3, appendedPointCount(previous, current))
    }
}
