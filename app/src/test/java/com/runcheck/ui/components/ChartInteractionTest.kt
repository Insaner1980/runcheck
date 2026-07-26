package com.runcheck.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartInteractionTest {
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
