package com.devicepulse.ui.theme

import android.view.accessibility.AccessibilityManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalReducedMotion = staticCompositionLocalOf { false }

val MaterialTheme.reducedMotion: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalReducedMotion.current

private val DevicePulseColorScheme = darkColorScheme(
    background = BgPage,
    surface = BgPage,
    surfaceContainer = BgCard,
    surfaceContainerHigh = BgCardAlt,
    primary = AccentTeal,
    secondary = AccentBlue,
    tertiary = AccentOrange,
    error = AccentRed,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = TextMuted,
    onBackground = TextPrimary,
    onPrimary = BgPage,
    onSecondary = BgPage,
    onTertiary = BgPage,
    onError = TextPrimary
)

@Composable
fun DevicePulseTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val accessibilityManager = context.getSystemService(
        AccessibilityManager::class.java
    )
    val reducedMotion = accessibilityManager?.let {
        try {
            val field = AccessibilityManager::class.java
                .getDeclaredMethod("isReducedMotionEnabled")
            field.invoke(it) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    } ?: false

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalStatusColors provides DevicePulseStatusColors,
        LocalReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = DevicePulseColorScheme,
            typography = DevicePulseTypography,
            content = content
        )
    }
}
