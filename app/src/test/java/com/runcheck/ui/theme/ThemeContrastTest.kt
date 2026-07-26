package com.runcheck.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ThemeContrastTest {
    @Test
    fun `documented text and status combinations meet WCAG AA`() {
        assertContrastAtLeast(TextOnLime, AccentLime, 4.5)
        assertContrastAtLeast(BgPage, AccentAmber, 4.5)
        listOf(
            AccentTeal,
            AccentAmber,
            AccentOrange,
            StatusCritical,
        ).forEach { statusColor ->
            assertContrastAtLeast(statusColor, BgCard, 4.5)
        }
    }

    private fun assertContrastAtLeast(
        foreground: Color,
        background: Color,
        minimum: Double,
    ) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("Expected contrast >= $minimum but was $ratio", ratio >= minimum)
    }

    private fun contrastRatio(
        first: Color,
        second: Color,
    ): Double {
        val firstLuminance = first.relativeLuminance()
        val secondLuminance = second.relativeLuminance()
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun Color.relativeLuminance(): Double =
        0.2126 * red.linearized() +
            0.7152 * green.linearized() +
            0.0722 * blue.linearized()

    private fun Float.linearized(): Double =
        if (this <= 0.04045f) {
            this / 12.92
        } else {
            ((this + 0.055) / 1.055).pow(2.4)
        }
}
