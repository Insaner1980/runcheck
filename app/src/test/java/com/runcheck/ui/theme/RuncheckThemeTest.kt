package com.runcheck.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import com.runcheck.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuncheckThemeTest {
    @Test
    fun `theme mode resolves against system appearance only for system mode`() {
        assertFalse(ThemeMode.LIGHT.resolveDarkTheme(systemInDarkTheme = true))
        assertTrue(ThemeMode.DARK.resolveDarkTheme(systemInDarkTheme = false))
        assertFalse(ThemeMode.SYSTEM.resolveDarkTheme(systemInDarkTheme = false))
        assertTrue(ThemeMode.SYSTEM.resolveDarkTheme(systemInDarkTheme = true))
    }

    @Test
    fun `dark scheme exposes the planned Material roles`() {
        with(DarkRuncheckColorScheme) {
            assertColor(0xFF0B1E24, background)
            assertColor(0xFF0F2A35, surfaceContainerLow)
            assertColor(0xFF133040, surfaceContainer)
            assertColor(0xFF1A3A48, surfaceContainerHigh)
            assertColor(0xFF4A9EDE, primary)
            assertColor(0xFF5DE4C7, secondary)
            assertColor(0xFFF4F7F8, onSurface)
            assertColor(0xFFB5C7CE, onSurfaceVariant)
        }
    }

    @Test
    fun `light scheme exposes the planned Material roles`() {
        with(LightRuncheckColorScheme) {
            assertColor(0xFFF4F7F8, background)
            assertColor(0xFFFFFFFF, surface)
            assertColor(0xFFF0F4F5, surfaceContainerLow)
            assertColor(0xFFE9EFF1, surfaceContainer)
            assertColor(0xFFDEE7EA, surfaceContainerHigh)
            assertColor(0xFFD4E0E4, surfaceContainerHighest)
            assertColor(0xFF246A9F, primary)
            assertColor(0xFF006B5A, secondary)
            assertColor(0xFF795F00, tertiary)
            assertColor(0xFFB3261E, error)
            assertColor(0xFF16262C, onSurface)
            assertColor(0xFF4E6570, onSurfaceVariant)
            assertColor(0xFF647A83, outline)
            assertColor(0xFFC0CDD1, outlineVariant)
        }
    }

    @Test
    fun `status containers are opaque exact colors with accessible foregrounds`() {
        val colors = RuncheckStatusColors
        val pairs =
            listOf(
                StatusColorPair(colors.healthy, colors.healthyContainer, colors.onHealthyContainer),
                StatusColorPair(colors.fair, colors.fairContainer, colors.onFairContainer),
                StatusColorPair(colors.poor, colors.poorContainer, colors.onPoorContainer),
                StatusColorPair(colors.critical, colors.criticalContainer, colors.onCriticalContainer),
                StatusColorPair(colors.neutral, colors.neutralContainer, colors.onNeutralContainer),
                StatusColorPair(colors.unavailable, colors.unavailableContainer, colors.onUnavailableContainer),
            )
        val expected =
            listOf(
                0xFF006B57,
                0xFF795F00,
                0xFF9C4E00,
                0xFFB3261E,
                0xFF4E6570,
                0xFF647A83,
            )

        pairs.zip(expected).forEach { (pair, expectedArgb) ->
            assertColor(expectedArgb, pair.base)
            assertColor(expectedArgb, pair.container)
            assertEquals(1f, pair.container.alpha, 0f)
            assertEquals(preferredStatusForeground(pair.container), pair.foreground)
            assertTrue(
                "Expected WCAG AA contrast for ${expectedArgb.toString(16)}",
                contrastRatio(pair.foreground, pair.container) >= 4.5,
            )
        }
    }

    @Test
    fun `chart palettes are centralized opaque and distinct per appearance`() {
        assertEquals(DarkRuncheckColorScheme.outlineVariant, DarkChartColors.grid)
        assertEquals(DarkRuncheckColorScheme.onSurfaceVariant, DarkChartColors.axis)
        assertEquals(DarkRuncheckColorScheme.primary, DarkChartColors.line)
        assertEquals(DarkRuncheckColorScheme.surfaceContainerHigh, DarkChartColors.fill)
        assertEquals(DarkRuncheckColorScheme.secondary, DarkChartColors.selectedPoint)
        assertEquals(DarkRuncheckColorScheme.secondary, DarkChartColors.glow)

        assertEquals(LightRuncheckColorScheme.outlineVariant, LightChartColors.grid)
        assertEquals(LightRuncheckColorScheme.onSurfaceVariant, LightChartColors.axis)
        assertEquals(LightRuncheckColorScheme.primary, LightChartColors.line)
        assertEquals(LightRuncheckColorScheme.surfaceContainer, LightChartColors.fill)
        assertEquals(LightRuncheckColorScheme.secondary, LightChartColors.selectedPoint)
        assertEquals(LightRuncheckColorScheme.surfaceContainerHighest, LightChartColors.glow)
        assertEquals(1f, LightChartColors.fill.alpha, 0f)
        assertEquals(1f, LightChartColors.glow.alpha, 0f)
        assertNotEquals(DarkChartColors, LightChartColors)
    }

    @Test
    fun `shape hierarchy keeps cards sheets and pills distinct`() {
        assertEquals(8f, RuncheckShapes.small.cornerPx(), 0f)
        assertEquals(16f, RuncheckShapes.medium.cornerPx(), 0f)
        assertEquals(16f, RuncheckShapes.large.cornerPx(), 0f)
        assertEquals(28f, RuncheckShapes.extraLarge.cornerPx(), 0f)
        assertEquals(28f, BottomSheetShape.cornerPx(), 0f)
        assertEquals(50f, RuncheckPillShape.cornerPx(), 0f)
    }

    @Test
    fun `reduced motion follows the disabled animator scale`() {
        assertTrue(reducedMotionEnabled(animatorDurationScale = 0f))
        assertFalse(reducedMotionEnabled(animatorDurationScale = 0.5f))
        assertFalse(reducedMotionEnabled(animatorDurationScale = 1f))
    }

    private fun assertColor(
        expectedArgb: Long,
        actual: Color,
    ) {
        assertEquals(expectedArgb.toInt(), actual.toArgb())
    }

    private fun androidx.compose.ui.graphics.Shape.cornerPx(): Float =
        (this as CornerBasedShape).topStart.toPx(
            shapeSize = Size(width = 100f, height = 100f),
            density = Density(density = 1f),
        )

    private data class StatusColorPair(
        val base: Color,
        val container: Color,
        val foreground: Color,
    )
}
