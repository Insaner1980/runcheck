package com.runcheck.ui.common

import com.runcheck.R
import com.runcheck.domain.model.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

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

    @Test
    fun `throwable message helpers prefer non blank messages and fall back otherwise`() {
        assertEquals("Network failed", IllegalStateException("Network failed").messageOr("Fallback"))
        assertEquals("Fallback", IllegalStateException("").messageOr("Fallback"))

        assertEquals(
            UiText.Dynamic("Network failed"),
            IllegalStateException("Network failed").messageOrRes(R.string.common_error_generic),
        )
        assertEquals(
            UiText.Resource(R.string.common_error_generic),
            IllegalStateException("").messageOrRes(R.string.common_error_generic),
        )
    }

    @Test
    fun `temperature helpers convert and format with app display locale`() {
        assertEquals(21.0, convertTemperature(21, TemperatureUnit.CELSIUS), 0.0)
        assertEquals(69.8, convertTemperature(21, TemperatureUnit.FAHRENHEIT), 0.0)
        assertEquals(R.string.unit_celsius, temperatureUnitRes(TemperatureUnit.CELSIUS))
        assertEquals(R.string.unit_fahrenheit, temperatureUnitRes(TemperatureUnit.FAHRENHEIT))
        assertEquals("69.8", formatTemperatureValue(21, TemperatureUnit.FAHRENHEIT))
    }

    @Test
    fun `unknown value detection treats blank and unknown text as unavailable`() {
        assertTrue(isUnknownValue(null))
        assertTrue(isUnknownValue(""))
        assertTrue(isUnknownValue("Unknown"))
        assertFalse(isUnknownValue("WiFi"))
    }
}
