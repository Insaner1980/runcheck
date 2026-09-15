package com.runcheck.ui.thermal

import com.runcheck.ui.theme.RuncheckStatusColors
import org.junit.Assert.assertEquals
import org.junit.Test

class ThrottlingEventPresentationTest {
    private val colors = RuncheckStatusColors

    @Test
    fun `valid event identifiers preserve their semantic colors`() {
        listOf("NONE", "LIGHT", "MODERATE").forEach { status ->
            assertEquals(colors.fair, colors.forThrottlingEvent(status))
        }
        assertEquals(colors.poor, colors.forThrottlingEvent("SEVERE"))
        listOf("CRITICAL", "EMERGENCY", "SHUTDOWN").forEach { status ->
            assertEquals(colors.critical, colors.forThrottlingEvent(status))
        }
    }

    @Test
    fun `unknown event identifiers use unavailable color without normalization`() {
        listOf("", "severe", "Severe", " SEVERE ", "UNKNOWN").forEach { status ->
            assertEquals(colors.unavailable, colors.forThrottlingEvent(status))
        }
    }
}
