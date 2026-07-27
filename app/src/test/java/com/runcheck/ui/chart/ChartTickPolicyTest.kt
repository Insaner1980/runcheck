package com.runcheck.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartTickPolicyTest {
    @Test
    fun `tick policy caps labels and preserves minimum pixel spacing`() {
        val ticks =
            selectChartTicks(
                ticks = listOf(0f, 10f, 20f, 30f, 40f, 50f),
                minValue = 0f,
                maxValue = 50f,
                availableHeightPx = 96f,
                minimumLabelSpacingPx = 32f,
            )

        assertTrue(ticks.size <= 4)
        val positions = ticks.map { chartValueFraction(it, 0f, 50f) * 96f }
        assertTrue(positions.zipWithNext().all { (first, second) -> second - first >= 32f })
    }

    @Test
    fun `tick policy ignores non-finite and out-of-viewport candidates`() {
        val ticks =
            selectChartTicks(
                ticks = listOf(Float.NaN, Float.NEGATIVE_INFINITY, -10f, 0f, 25f, 50f, 60f),
                minValue = 0f,
                maxValue = 50f,
                availableHeightPx = 200f,
                minimumLabelSpacingPx = 32f,
            )

        assertEquals(listOf(0f, 25f, 50f), ticks)
    }
}
