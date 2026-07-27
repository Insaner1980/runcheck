package com.runcheck.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignatureComponentPolicyTest {
    @Test
    fun `hero gauge exposes one complete localized semantic description`() {
        assertEquals(
            0f,
            heroGaugePresentation(Float.NaN, "Health", "Unavailable", null).value,
            0f,
        )
        assertEquals(
            0f,
            heroGaugePresentation(-12f, "Health", "Poor", "Estimated").value,
            0f,
        )
        assertEquals(
            100f,
            heroGaugePresentation(127f, "Health", "Healthy", "Accurate").value,
            0f,
        )

        val presentation =
            heroGaugePresentation(
                value = 72.4f,
                semanticLabel = "Battery health",
                status = "Healthy",
                confidenceLabel = "Accurate",
            )
        assertEquals(72f, presentation.value, 0f)
        assertEquals("72", presentation.displayValue)
        assertEquals(
            HeroGaugeSemantics(
                label = "Battery health",
                valuePercent = "72%",
                status = "Healthy",
                confidence = "Accurate",
            ),
            presentation.semantics,
        )
        assertEquals(
            null,
            heroGaugePresentation(72.4f, "Battery health", "Healthy", null).semantics.confidence,
        )
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
    fun `metric tile reserves status and confidence slots for every async state`() {
        val ready =
            metricTileSlotPolicy(
                state = MetricTileState.READY,
                hasStatus = true,
                hasConfidence = true,
            )
        assertEquals(MetricTileSlotVisibility.VISIBLE, ready.status)
        assertEquals(MetricTileSlotVisibility.VISIBLE, ready.confidence)

        val readyWithoutMetadata =
            metricTileSlotPolicy(
                state = MetricTileState.READY,
                hasStatus = false,
                hasConfidence = false,
            )
        assertEquals(MetricTileSlotVisibility.PLACEHOLDER, readyWithoutMetadata.status)
        assertEquals(MetricTileSlotVisibility.PLACEHOLDER, readyWithoutMetadata.confidence)

        listOf(MetricTileState.LOADING, MetricTileState.UNAVAILABLE).forEach { state ->
            val policy =
                metricTileSlotPolicy(
                    state = state,
                    hasStatus = true,
                    hasConfidence = true,
                )
            assertEquals(MetricTileSlotVisibility.VISIBLE, policy.status)
            assertEquals(MetricTileSlotVisibility.PLACEHOLDER, policy.confidence)
        }
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

    @Test
    fun `legacy float and counter animations retain separate motion contracts`() {
        with(counterMotionSpec(CounterMotion.COUNTER)) {
            assertEquals(700, durationMillis)
            assertEquals(com.runcheck.ui.theme.MotionTokens.DecelerateEasing, easing)
        }
        with(counterMotionSpec(CounterMotion.LEGACY_FLOAT)) {
            assertEquals(200, durationMillis)
            assertEquals(FastOutSlowInEasing, easing)
        }
    }
}
