package com.runcheck.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartStatePrecedenceTest {
    @Test
    fun `locked state wins over insufficient data`() {
        val state =
            resolveChartPrimaryState(
                isLoading = false,
                error = null,
                isLocked = true,
                dataPointCount = 0,
                minimumDataPointCount = 2,
            )

        assertEquals(ChartPrimaryState.Locked, state)
    }

    @Test
    fun `error wins over locked and insufficient data`() {
        val state =
            resolveChartPrimaryState(
                isLoading = false,
                error = "History failed",
                isLocked = true,
                dataPointCount = 0,
                minimumDataPointCount = 2,
            )

        assertEquals(ChartPrimaryState.Error("History failed"), state)
    }

    @Test
    fun `loading wins over every lower priority state`() {
        val state =
            resolveChartPrimaryState(
                isLoading = true,
                error = "History failed",
                isLocked = true,
                dataPointCount = 0,
                minimumDataPointCount = 2,
            )

        assertEquals(ChartPrimaryState.Loading, state)
    }
}
