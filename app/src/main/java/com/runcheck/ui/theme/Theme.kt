package com.runcheck.ui.theme

import android.view.accessibility.AccessibilityManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalNumericFontFamily = staticCompositionLocalOf { JetBrainsMonoFontFamily }

val MaterialTheme.reducedMotion: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalReducedMotion.current

private val RuncheckColorScheme = darkColorScheme(
    background = BgPage,
    surface = BgPage,
    surfaceContainer = BgCard,
    surfaceContainerHigh = BgCardAlt,
    primary = AccentBlue,
    secondary = AccentTeal,
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
fun RuncheckTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val accessibilityManager = context.getSystemService(
        AccessibilityManager::class.java
    )
    val reducedMotion = if (accessibilityManager == null) {
        false
    } else {
        try {
            val field = AccessibilityManager::class.java
                .getDeclaredMethod("isReducedMotionEnabled")
            field.invoke(accessibilityManager) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalStatusColors provides RuncheckStatusColors,
        LocalReducedMotion provides reducedMotion,
        LocalNumericFontFamily provides JetBrainsMonoFontFamily
    ) {
        MaterialTheme(
            colorScheme = RuncheckColorScheme,
            typography = RuncheckTypography,
            content = content
        )
    }
}
