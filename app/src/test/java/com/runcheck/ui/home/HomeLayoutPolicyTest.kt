package com.runcheck.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLayoutPolicyTest {
    @Test
    fun `compact normal font uses two columns`() {
        assertEquals(
            2,
            homeMetricGridColumns(
                isWideScreen = false,
                fontScale = 1f,
            ),
        )
    }

    @Test
    fun `compact large font uses one column to preserve content`() {
        listOf(1.3f, 2f).forEach { fontScale ->
            assertEquals(
                "fontScale=$fontScale",
                1,
                homeMetricGridColumns(
                    isWideScreen = false,
                    fontScale = fontScale,
                ),
            )
        }
    }

    @Test
    fun `wide layout keeps at most two columns at large font`() {
        assertEquals(4, homeMetricGridColumns(isWideScreen = true, fontScale = 1f))
        assertEquals(2, homeMetricGridColumns(isWideScreen = true, fontScale = 2f))
    }
}
