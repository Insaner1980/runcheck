package com.runcheck.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun `theme mode has the supported persisted values in stable order`() {
        assertEquals(
            listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK),
            ThemeMode.entries,
        )
    }

    @Test
    fun `user preferences default to the system theme`() {
        assertEquals(ThemeMode.SYSTEM, UserPreferences().themeMode)
    }

    @Test
    fun `unknown persisted theme falls back to system`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStoredValue("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStoredValue("DARK"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStoredValue("REMOVED_MODE"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStoredValue(null))
    }
}
