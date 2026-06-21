package com.runcheck.ui.common

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class UiFormattersTest {
    @Test
    fun `format decimal uses app English locale when device locale differs`() {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)

        try {
            assertEquals("12.5", formatDecimal(12.5, 1))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
