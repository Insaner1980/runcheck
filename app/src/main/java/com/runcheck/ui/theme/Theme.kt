package com.runcheck.ui.theme

import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalNumericFontFamily = staticCompositionLocalOf { JetBrainsMonoFontFamily }

val MaterialTheme.reducedMotion: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalReducedMotion.current

val MaterialTheme.iconCircleColor: Color
    @Composable
    @ReadOnlyComposable
    get() = colorScheme.surfaceContainerHighest

private val RuncheckColorScheme = darkColorScheme(
    background = BgPage,
    surface = BgPage,
    surfaceContainer = BgCard,
    surfaceContainerHigh = BgCardAlt,
    surfaceContainerHighest = BgIconCircle,
    primary = AccentBlue,
    secondary = AccentTeal,
    tertiary = AccentAmber,
    error = AccentRed,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = TextMuted,
    outlineVariant = TextMuted,
    surfaceVariant = BgIconCircle,
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

    val reducedMotion = try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    } catch (_: Exception) {
        false
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
            shapes = RuncheckShapes,
            content = content
        )
    }
}
