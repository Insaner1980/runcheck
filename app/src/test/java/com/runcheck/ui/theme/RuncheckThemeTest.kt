package com.runcheck.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runcheck.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
    fun `dark scheme exposes the layered surface and content roles`() {
        with(DarkRuncheckColorScheme) {
            assertColor(0xFF08171C, background)
            assertColor(0xFF0D2229, surfaceContainer)
            assertColor(0xFF123039, surfaceContainerHigh)
            assertColor(0xFF183D47, surfaceContainerHighest)
            assertColor(0xFF4EA8F5, primary)
            assertColor(0xFF35DDBE, secondary)
            assertColor(0xFFF4FAFC, onSurface)
            assertColor(0xFFA9BEC6, onSurfaceVariant)
            assertColor(0xFF789099, outline)
        }
    }

    @Test
    fun `light scheme separates page cards and readable content`() {
        with(LightRuncheckColorScheme) {
            assertColor(0xFFDDE6EA, background)
            assertColor(0xFFFFFFFF, surface)
            assertColor(0xFFFFFFFF, surfaceContainer)
            assertColor(0xFFF4F7F8, surfaceContainerLow)
            assertColor(0xFFE8EFF2, surfaceContainerHigh)
            assertColor(0xFFE8EFF2, surfaceContainerHighest)
            assertColor(0xFF0B63B0, primary)
            assertColor(0xFF007A66, secondary)
            assertColor(0xFF9B5C00, tertiary)
            assertColor(0xFFB3261E, error)
            assertColor(0xFF172A32, onSurface)
            assertColor(0xFF405A64, onSurfaceVariant)
            assertColor(0xFF7A939D, outline)
            assertColor(0xFF7A939D, outlineVariant)
            assertTrue(contrastRatio(onSurface, background) >= 4.5)
            assertTrue(contrastRatio(onSurfaceVariant, background) >= 4.5)
            assertTrue(contrastRatio(onSurface, surfaceContainer) >= 4.5)
            assertTrue(contrastRatio(onSurfaceVariant, surfaceContainer) >= 4.5)
        }
    }

    @Test
    fun `domain accents remain distinct and theme appropriate`() {
        assertDomainColors(
            actual = DarkDomainColors,
            battery = 0xFFFFB627,
            network = 0xFF4EA8F5,
            thermal = 0xFFFF7A45,
            storage = 0xFF35DDBE,
        )
        assertDomainColors(
            actual = LightDomainColors,
            battery = 0xFF9B5C00,
            network = 0xFF0B63B0,
            thermal = 0xFFC24A12,
            storage = 0xFF007A66,
        )
        assertNotEquals(DarkDomainColors, LightDomainColors)
    }

    @Test
    fun `main card border policy uses the exact light theme outline`() {
        assertNull(mainCardBorderPolicy(darkTheme = true))

        val lightPolicy = requireNotNull(mainCardBorderPolicy(darkTheme = false))
        assertEquals(1.dp, lightPolicy.width)
        assertEquals(LightCardBorder, lightPolicy.color)
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
        assertEquals(LightRuncheckColorScheme.surfaceContainerHigh, LightChartColors.fill)
        assertEquals(LightRuncheckColorScheme.secondary, LightChartColors.selectedPoint)
        assertEquals(LightRuncheckColorScheme.surfaceContainerHighest, LightChartColors.glow)
        assertEquals(1f, LightChartColors.fill.alpha, 0f)
        assertEquals(1f, LightChartColors.glow.alpha, 0f)
        assertNotEquals(DarkChartColors, LightChartColors)
    }

    @Test
    fun `shape hierarchy keeps small main and hero surfaces distinct`() {
        assertEquals(12f, RuncheckShapes.small.cornerPx(), 0f)
        assertEquals(24f, RuncheckShapes.medium.cornerPx(), 0f)
        assertEquals(24f, RuncheckShapes.large.cornerPx(), 0f)
        assertEquals(32f, RuncheckShapes.extraLarge.cornerPx(), 0f)
        assertSame(RuncheckShapes.extraLarge, HeroCardShape)
        assertEquals(32f, HeroCardShape.cornerPx(), 0f)
        assertEquals(32f, BottomSheetShape.cornerPx(), 0f)
        assertEquals(50f, RuncheckPillShape.cornerPx(), 0f)
    }

    @Test
    fun `layout typography and component metrics share the visual system tokens`() {
        val spacing = Spacing()
        val uiTokens = UiTokens()

        assertEquals(20.dp, spacing.screenHorizontal)
        assertEquals(20.dp, spacing.cardInternal)
        assertEquals(12.dp, spacing.cardGap)
        assertEquals(28.dp, spacing.sectionGap)
        assertEquals(48.dp, uiTokens.touchTarget)
        assertEquals(180.dp, uiTokens.chartPlotMinimum)
        assertEquals(18.dp, uiTokens.heroGaugeStroke)
        assertEquals(64.sp, HeroNumberTextStyle.fontSize)
        assertEquals(24.sp, HeroUnitTextStyle.fontSize)
        assertTrue(GaugeValueTextStyle.fontSize.value in 40f..48f)
        assertTrue(CardMetricTextStyle.fontSize.value in 24f..32f)
    }

    @Test
    fun `motion policies expose the named duration and physics contracts`() {
        assertEquals(100, MotionTokens.INSTANT)
        assertEquals(180, MotionTokens.FAST)
        assertEquals(320, MotionTokens.MEDIUM)
        assertEquals(520, MotionTokens.SLOW)
        assertEquals(900, MotionTokens.DELIBERATE)
        assertEquals(700, MotionTokens.COUNTER)
        assertEquals(80, MotionTokens.RESULT_STAGGER)
        assertEquals(40, MotionTokens.LIST_ITEM_STAGGER)
        assertEquals(200, MotionTokens.CHART_FILL_DELAY)
        assertEquals(900, MotionTokens.CHART_PATH)
        assertEquals(700, MotionTokens.CHART_SCAN_FADE_DELAY)
        assertEquals(300, MotionTokens.CHART_SCAN_FADE)
        assertEquals(560, MotionTokens.CHART_TRANSITION_SCAN_FADE_DELAY)
        assertEquals(240, MotionTokens.CHART_TRANSITION_SCAN_FADE)
        assertEquals(300, MotionTokens.CHART_DATA_FADE_OUT)
        assertEquals(200, MotionTokens.CHART_TRANSITION_OVERLAP)

        with(MotionTokens.gaugeSpring<Float>()) {
            assertEquals(0.72f, dampingRatio, 0f)
            assertEquals(180f, stiffness, 0f)
        }
        with(MotionTokens.chipSpring<Float>()) {
            assertEquals(0.55f, dampingRatio, 0f)
            assertEquals(420f, stiffness, 0f)
        }
        with(MotionTokens.speedValueSpring<Float>()) {
            assertEquals(0.8f, dampingRatio, 0f)
            assertEquals(300f, stiffness, 0f)
        }
        with(MotionTokens.counterTween<Float>()) {
            assertEquals(700, durationMillis)
            assertEquals(MotionTokens.DecelerateEasing, easing)
        }
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

    private fun assertDomainColors(
        actual: DomainColors,
        battery: Long,
        network: Long,
        thermal: Long,
        storage: Long,
    ) {
        assertColor(battery, actual.battery)
        assertColor(network, actual.network)
        assertColor(thermal, actual.thermal)
        assertColor(storage, actual.storage)
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
