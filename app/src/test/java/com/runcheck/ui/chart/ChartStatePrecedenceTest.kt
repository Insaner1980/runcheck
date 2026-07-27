package com.runcheck.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartStatePrecedenceTest {
    @Test
    fun `free access resolves locked when no higher priority state exists`() {
        val state =
            resolveChartPrimaryState(
                isLoading = false,
                error = null,
                isLocked = true,
                dataPointCount = 5,
                minimumDataPointCount = 2,
            )

        assertEquals(ChartPrimaryState.Locked, state)
    }

    @Test
    fun `trial and pro access resolve available data`() {
        listOf("trial", "pro").forEach { accessKind ->
            val state =
                resolveChartPrimaryState(
                    isLoading = false,
                    error = null,
                    isLocked = false,
                    dataPointCount = 2,
                    minimumDataPointCount = 2,
                )

            assertEquals(accessKind, ChartPrimaryState.Data, state)
        }
    }

    @Test
    fun `granted access without enough points resolves insufficient data`() {
        val state =
            resolveChartPrimaryState(
                isLoading = false,
                error = null,
                isLocked = false,
                dataPointCount = 1,
                minimumDataPointCount = 2,
            )

        assertEquals(ChartPrimaryState.InsufficientData, state)
    }

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
