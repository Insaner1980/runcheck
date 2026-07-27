package com.runcheck.ui.chart

import androidx.compose.ui.graphics.Color
import com.runcheck.ui.components.ChartQualityZone
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartViewportTest {
    @Test
    fun `quality zones are clipped without expanding a narrow data viewport`() {
        val viewport =
            calculateChartViewport(
                data = listOf(61f, 68f, 72f, 79f, 80f),
                explicitTicks = listOf(60f, 70f, 80f),
                qualityZones =
                    listOf(
                        ChartQualityZone(0f, 20f, Color.Red),
                        ChartQualityZone(20f, 50f, Color.Yellow),
                        ChartQualityZone(50f, 100f, Color.Green),
                    ),
                availableHeightPx = 200f,
                minimumLabelSpacingPx = 32f,
            )

        assertNotNull(viewport)
        requireNotNull(viewport)
        assertTrue("The 0..100 zones must not become the viewport", viewport.minValue > 50f)
        assertTrue("The 0..100 zones must not become the viewport", viewport.maxValue < 90f)
        assertTrue(
            "Every visible zone must be clipped to the viewport",
            viewport.visibleZones.all { zone ->
                zone.minValue >= viewport.minValue && zone.maxValue <= viewport.maxValue
            },
        )
    }

    @Test
    fun `temperature zones do not flatten a narrow measured range`() {
        val viewport =
            calculateChartViewport(
                data = listOf(30.7f, 31.2f, 32.1f, 33.4f),
                explicitTicks = listOf(31f, 32f, 33f),
                qualityZones =
                    listOf(
                        ChartQualityZone(0f, 35f, Color.Green),
                        ChartQualityZone(35f, 60f, Color.Red),
                    ),
                availableHeightPx = 200f,
                minimumLabelSpacingPx = 32f,
            )

        assertNotNull(viewport)
        requireNotNull(viewport)
        assertTrue(viewport.minValue > 25f)
        assertTrue(viewport.maxValue < 40f)
    }
}
