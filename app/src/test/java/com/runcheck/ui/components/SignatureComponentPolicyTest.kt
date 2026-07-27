package com.runcheck.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignatureComponentPolicyTest {
    @Test
    fun `hero gauge clamps invalid and out of range values before display and semantics`() {
        assertEquals(0f, heroGaugePresentation(Float.NaN, "Health", "Unavailable").value, 0f)
        assertEquals(0f, heroGaugePresentation(-12f, "Health", "Poor").value, 0f)
        assertEquals(100f, heroGaugePresentation(127f, "Health", "Healthy").value, 0f)

        val presentation = heroGaugePresentation(72.4f, "Battery health", "Healthy")
        assertEquals(72f, presentation.value, 0f)
        assertEquals("72", presentation.displayValue)
        assertEquals("Battery health, Healthy, 72%", presentation.stateDescription)
    }

    @Test
    fun `metric tile states retain a value and textual status slot`() {
        val ready =
            metricTilePresentation(
                state = MetricTileState.READY,
                value = "87",
                status = "Healthy",
                loadingLabel = "Loading",
                unavailableLabel = "N/A",
            )
        assertEquals("87", ready.displayValue)
        assertEquals("Healthy", ready.statusLabel)
        assertFalse(ready.showLoadingIndicator)

        val loading =
            metricTilePresentation(
                state = MetricTileState.LOADING,
                value = "87",
                status = "Healthy",
                loadingLabel = "Loading",
                unavailableLabel = "N/A",
            )
        assertEquals("—", loading.displayValue)
        assertEquals("Loading", loading.statusLabel)
        assertTrue(loading.showLoadingIndicator)

        val unavailable =
            metricTilePresentation(
                state = MetricTileState.UNAVAILABLE,
                value = "87",
                status = "Healthy",
                loadingLabel = "Loading",
                unavailableLabel = "N/A",
            )
        assertEquals("N/A", unavailable.displayValue)
        assertEquals("N/A", unavailable.statusLabel)
        assertFalse(unavailable.showLoadingIndicator)
    }

    @Test
    fun `counter semantics always use the final formatted value`() {
        assertEquals(
            "≈ 42%",
            formatCounterText(
                value = 42,
                formatter = Int::toString,
                prefix = "≈ ",
                suffix = "%",
            ),
        )
        assertEquals(
            "18.5 °C",
            formatCounterText(
                value = 18.45f,
                formatter = { value -> String.format(java.util.Locale.US, "%.1f", value) },
                prefix = "",
                suffix = " °C",
            ),
        )
    }
}
