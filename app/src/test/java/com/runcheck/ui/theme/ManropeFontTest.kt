package com.runcheck.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.ResourceFont
import com.runcheck.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalTextApi::class)
class ManropeFontTest {
    @Test
    fun `home and material typography share the same Manrope family`() {
        assertSame(RuncheckTypography.bodyMedium.fontFamily, HomeManropeFontFamily)
    }

    @Test
    fun `material text weights select matching Manrope variation axes`() {
        with(RuncheckTypography) {
            listOf(bodyMedium, titleMedium, headlineMedium, displayLarge).forEach { style ->
                val family = style.fontFamily as FontListFontFamily
                val font = family.fonts.singleOrNull { it.weight == style.fontWeight } as? ResourceFont
                assertNotNull("Missing Manrope weight ${style.fontWeight}", font)
                requireNotNull(font)
                assertEquals(R.font.manrope, font.resId)
                assertEquals(
                    FontVariation.Settings(FontVariation.weight(font.weight.weight)),
                    font.variationSettings,
                )
            }
        }
    }
}
