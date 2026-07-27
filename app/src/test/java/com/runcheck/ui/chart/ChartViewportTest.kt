package com.runcheck.ui.chart

import androidx.compose.ui.graphics.Color
import com.runcheck.ui.components.ChartQualityZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartViewportTest {
    @Test
    fun `empty and entirely non-finite data produce no viewport`() {
        assertNull(
            calculateChartViewport(
                data = emptyList(),
                explicitTicks = listOf(0f),
                qualityZones = emptyList(),
                availableHeightPx = 200f,
                minimumLabelSpacingPx = 32f,
            ),
        )
        assertNull(
            calculateChartViewport(
                data = listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY),
                explicitTicks = emptyList(),
                qualityZones = emptyList(),
                availableHeightPx = 200f,
                minimumLabelSpacingPx = 32f,
            ),
        )
    }

    @Test
    fun `chart series filters non-finite values and their matching timestamps together`() {
        val series =
            sanitizeChartSeries(
                data = listOf(10f, Float.NaN, 20f, Float.POSITIVE_INFINITY),
                timestamps = listOf(100L, 200L, 300L, 400L),
            )

        assertEquals(listOf(10f, 20f), series.data)
        assertEquals(listOf(100L, 300L), series.timestamps)
        assertTrue(series.data.all(Float::isFinite))
    }

    @Test
    fun `single and flat data receive finite symmetric padding`() {
        val single =
            calculateChartViewport(
                data = listOf(-4f),
                explicitTicks = emptyList(),
                qualityZones = emptyList(),
                availableHeightPx = 200f,
                minimumLabelSpacingPx = 32f,
            )
        val flat =
            calculateChartViewport(
                data = listOf(42f, 42f, 42f),
                explicitTicks = emptyList(),
                qualityZones = emptyList(),
                availableHeightPx = 200f,
                minimumLabelSpacingPx = 32f,
            )

        requireNotNull(single)
        requireNotNull(flat)
        assertTrue(single.minValue < -4f)
        assertTrue(single.maxValue > -4f)
        assertTrue(flat.minValue < 42f)
        assertTrue(flat.maxValue > 42f)
        assertTrue(single.minValue.isFinite() && single.maxValue.isFinite())
        assertTrue(flat.minValue.isFinite() && flat.maxValue.isFinite())
    }

    @Test
    fun `extreme negative and positive values keep viewport and drawing fractions finite`() {
        val viewport =
            calculateChartViewport(
                data = listOf(-Float.MAX_VALUE, Float.MAX_VALUE),
                explicitTicks = emptyList(),
                qualityZones = emptyList(),
                availableHeightPx = 200f,
                minimumLabelSpacingPx = 32f,
            )

        requireNotNull(viewport)
        assertTrue(viewport.minValue.isFinite())
        assertTrue(viewport.maxValue.isFinite())
        val lowFraction = chartValueFraction(-Float.MAX_VALUE, viewport.minValue, viewport.maxValue)
        val highFraction = chartValueFraction(Float.MAX_VALUE, viewport.minValue, viewport.maxValue)
        assertTrue(lowFraction.isFinite())
        assertTrue(highFraction.isFinite())
        assertTrue(lowFraction in 0f..1f)
        assertTrue(highFraction in 0f..1f)
        assertTrue(lowFraction < highFraction)
    }

    @Test
    fun `fahrenheit zones are clipped without expanding measured temperatures`() {
        val viewport =
            calculateChartViewport(
                data = listOf(87.3f, 88.2f, 90.1f, 92.1f),
                explicitTicks = listOf(88f, 90f, 92f),
                qualityZones =
                    listOf(
                        ChartQualityZone(32f, 95f, Color.Green),
                        ChartQualityZone(95f, 140f, Color.Red),
                    ),
                availableHeightPx = 200f,
                minimumLabelSpacingPx = 32f,
            )

        requireNotNull(viewport)
        assertTrue(viewport.minValue > 80f)
        assertTrue(viewport.maxValue < 100f)
        assertEquals(1, viewport.visibleZones.size)
        assertFalse(viewport.visibleZones.any { it.maxValue > viewport.maxValue })
    }

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
        assertTrue("The viewport must expose at most four labels", viewport.ticks.size <= 4)
        assertEquals("Only the intersecting quality zone is visible", 1, viewport.visibleZones.size)
        val visibleZone = viewport.visibleZones.single()
        assertEquals(viewport.minValue, visibleZone.minValue, 0f)
        assertEquals(viewport.maxValue, visibleZone.maxValue, 0f)
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
        assertTrue("The viewport must expose at most four labels", viewport.ticks.size <= 4)
    }

    @Test
    fun `available height removes ticks that cannot meet minimum vertical spacing`() {
        val availableHeightPx = 72f
        val minimumLabelSpacingPx = 32f
        val requestedTicks = listOf(60f, 65f, 70f, 75f, 80f)
        val viewport =
            calculateChartViewport(
                data = listOf(61f, 68f, 72f, 79f, 80f),
                explicitTicks = requestedTicks,
                qualityZones = emptyList(),
                availableHeightPx = availableHeightPx,
                minimumLabelSpacingPx = minimumLabelSpacingPx,
            )

        assertNotNull(viewport)
        requireNotNull(viewport)
        assertTrue(
            "The height constraint must remove at least one requested tick",
            viewport.ticks.size < requestedTicks.size,
        )
        val tickPositionsPx =
            viewport.ticks
                .map { tick ->
                    (tick - viewport.minValue) /
                        (viewport.maxValue - viewport.minValue) *
                        availableHeightPx
                }.sorted()
        assertTrue(
            "Retained ticks must meet the requested vertical spacing",
            tickPositionsPx.zipWithNext().all { (first, second) ->
                second - first >= minimumLabelSpacingPx
            },
        )
    }
}
