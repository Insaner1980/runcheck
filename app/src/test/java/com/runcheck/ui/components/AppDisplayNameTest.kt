package com.runcheck.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDisplayNameTest {
    @Test
    fun `uses a non-blank app label`() {
        assertEquals(
            "Maps",
            resolveAppDisplayName(
                appLabel = "  Maps  ",
                packageName = "com.example.maps",
                unknownAppLabel = "Unknown app",
            ),
        )
    }

    @Test
    fun `builds a readable fallback from the package name`() {
        assertEquals(
            "Battery Saver",
            resolveAppDisplayName(
                appLabel = " ",
                packageName = "com.example.battery_saver",
                unknownAppLabel = "Unknown app",
            ),
        )
    }

    @Test
    fun `uses a neutral fallback when label and package are blank`() {
        assertEquals(
            "Unknown app",
            resolveAppDisplayName(
                appLabel = null,
                packageName = "",
                unknownAppLabel = "Unknown app",
            ),
        )
    }
}
