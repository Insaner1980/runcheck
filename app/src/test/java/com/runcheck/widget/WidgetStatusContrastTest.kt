package com.runcheck.widget

import com.runcheck.ui.theme.BgCard
import com.runcheck.ui.theme.LightSurfaceContainer
import com.runcheck.ui.theme.contrastRatio
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStatusContrastTest {
    @Test
    fun `every widget status color has normal text contrast in day and night themes`() {
        RuncheckWidgetStatusPalette.all.forEach { tone ->
            assertTrue(
                "${tone.name} day contrast",
                contrastRatio(tone.day, LightSurfaceContainer) >= 4.5,
            )
            assertTrue(
                "${tone.name} night contrast",
                contrastRatio(tone.night, BgCard) >= 4.5,
            )
        }
    }
}
