package com.runcheck

import com.runcheck.domain.model.ThemeMode

internal data class SystemBarAppearance(
    val isDarkTheme: Boolean,
) {
    val useDarkIcons: Boolean
        get() = !isDarkTheme

    val useDarkScrim: Boolean
        get() = isDarkTheme
}

internal fun shouldKeepSplashOnScreen(
    themeMode: ThemeMode?,
    systemBarsReady: Boolean,
): Boolean = themeMode == null || !systemBarsReady

internal fun resolveSystemBarAppearance(
    themeMode: ThemeMode,
    systemInDarkTheme: Boolean,
): SystemBarAppearance {
    val darkTheme =
        when (themeMode) {
            ThemeMode.SYSTEM -> systemInDarkTheme
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    return SystemBarAppearance(isDarkTheme = darkTheme)
}

internal fun resolveSystemBarAppearanceOrNull(
    themeMode: ThemeMode?,
    systemInDarkTheme: Boolean,
): SystemBarAppearance? =
    themeMode?.let { readyThemeMode ->
        resolveSystemBarAppearance(readyThemeMode, systemInDarkTheme)
    }
