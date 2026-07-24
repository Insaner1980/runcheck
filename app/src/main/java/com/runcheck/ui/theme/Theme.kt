package com.runcheck.ui.theme

import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.runcheck.domain.model.ThemeMode

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalNumericFontFamily = staticCompositionLocalOf { JetBrainsMonoFontFamily }
private val LocalHeroCardColor = staticCompositionLocalOf { BgCardDeep }

val MaterialTheme.reducedMotion: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalReducedMotion.current

val MaterialTheme.heroCardColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalHeroCardColor.current

val MaterialTheme.iconCircleColor: Color
    @Composable
    @ReadOnlyComposable
    get() = colorScheme.surfaceContainerHighest

val MaterialTheme.cardStrokeColor: Color
    @Composable
    @ReadOnlyComposable
    get() = colorScheme.outlineVariant.copy(alpha = uiTokens.outlineAlpha)

@Composable
fun runcheckCardColors(containerColor: Color = MaterialTheme.colorScheme.surfaceContainer): CardColors =
    CardDefaults.cardColors(containerColor = containerColor)

@Composable
fun runcheckHeroCardColors(): CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.heroCardColor)

@Composable
fun runcheckCardElevation(): CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp)

@Composable
fun runcheckOutlinedCardBorder(): BorderStroke =
    BorderStroke(width = MaterialTheme.uiTokens.outlineWidth, color = MaterialTheme.cardStrokeColor)

internal val DarkRuncheckColorScheme =
    darkColorScheme(
        primary = AccentBlue,
        onPrimary = BgPage,
        primaryContainer = BgIconCircle,
        onPrimaryContainer = TextPrimary,
        inversePrimary = AccentBlue,
        secondary = AccentTeal,
        onSecondary = BgPage,
        secondaryContainer = BgIconCircle,
        onSecondaryContainer = TextPrimary,
        tertiary = AccentAmber,
        onTertiary = BgPage,
        tertiaryContainer = BgCard,
        onTertiaryContainer = TextPrimary,
        background = BgPage,
        onBackground = TextPrimary,
        surface = BgPage,
        onSurface = TextPrimary,
        surfaceVariant = BgIconCircle,
        onSurfaceVariant = TextSecondary,
        surfaceTint = AccentBlue,
        inverseSurface = TextPrimary,
        inverseOnSurface = BgPage,
        error = AccentRed,
        onError = BgPage,
        errorContainer = AccentRed,
        onErrorContainer = BgPage,
        outline = TextMuted,
        outlineVariant = TextMuted,
        scrim = BgPage,
        surfaceBright = BgIconCircle,
        surfaceDim = BgPage,
        surfaceContainer = BgCard,
        surfaceContainerHigh = BgIconCircle,
        surfaceContainerHighest = BgIconCircle,
        surfaceContainerLow = BgCardAlt,
        surfaceContainerLowest = BgPage,
        primaryFixed = AccentBlue,
        primaryFixedDim = LightPrimary,
        onPrimaryFixed = BgPage,
        onPrimaryFixedVariant = BgPage,
        secondaryFixed = AccentTeal,
        secondaryFixedDim = LightSecondary,
        onSecondaryFixed = BgPage,
        onSecondaryFixedVariant = BgPage,
        tertiaryFixed = AccentAmber,
        tertiaryFixedDim = LightTertiary,
        onTertiaryFixed = BgPage,
        onTertiaryFixedVariant = BgPage,
    )

internal val LightRuncheckColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = LightSurface,
        primaryContainer = LightSurfaceContainerHigh,
        onPrimaryContainer = LightOnSurface,
        inversePrimary = LightPrimary,
        secondary = LightSecondary,
        onSecondary = LightSurface,
        secondaryContainer = LightSurfaceContainerHighest,
        onSecondaryContainer = LightOnSurface,
        tertiary = LightTertiary,
        onTertiary = LightSurface,
        tertiaryContainer = LightSurfaceContainer,
        onTertiaryContainer = LightOnSurface,
        background = LightBackground,
        onBackground = LightOnSurface,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceContainerHighest,
        onSurfaceVariant = LightOnSurfaceVariant,
        surfaceTint = LightPrimary,
        inverseSurface = LightOnSurface,
        inverseOnSurface = LightSurface,
        error = LightError,
        onError = LightSurface,
        errorContainer = LightError,
        onErrorContainer = LightSurface,
        outline = LightOutline,
        outlineVariant = LightOutlineVariant,
        scrim = LightOnSurface,
        surfaceBright = LightSurface,
        surfaceDim = LightSurfaceContainerHigh,
        surfaceContainer = LightSurfaceContainer,
        surfaceContainerHigh = LightSurfaceContainerHigh,
        surfaceContainerHighest = LightSurfaceContainerHighest,
        surfaceContainerLow = LightSurfaceContainerLow,
        surfaceContainerLowest = LightSurface,
        primaryFixed = LightPrimary,
        primaryFixedDim = LightPrimary,
        onPrimaryFixed = LightSurface,
        onPrimaryFixedVariant = LightSurface,
        secondaryFixed = LightSecondary,
        secondaryFixedDim = LightSecondary,
        onSecondaryFixed = LightSurface,
        onSecondaryFixedVariant = LightSurface,
        tertiaryFixed = LightTertiary,
        tertiaryFixedDim = LightTertiary,
        onTertiaryFixed = LightSurface,
        onTertiaryFixedVariant = LightSurface,
    )

internal fun ThemeMode.resolveDarkTheme(systemInDarkTheme: Boolean): Boolean =
    when (this) {
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

internal fun reducedMotionEnabled(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f

@Composable
fun RuncheckTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = themeMode.resolveDarkTheme(isSystemInDarkTheme())
    val colorScheme = if (darkTheme) DarkRuncheckColorScheme else LightRuncheckColorScheme
    val chartColors = if (darkTheme) DarkChartColors else LightChartColors
    val heroCardColor = if (darkTheme) BgCardDeep else LightSurfaceContainerLow
    val context = LocalContext.current
    val reducedMotion =
        try {
            reducedMotionEnabled(
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f,
                ),
            )
        } catch (_: Exception) {
            false
        }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalStatusColors provides RuncheckStatusColors,
        LocalReducedMotion provides reducedMotion,
        LocalNumericFontFamily provides JetBrainsMonoFontFamily,
        LocalUiTokens provides UiTokens(),
        LocalHeroCardColor provides heroCardColor,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = RuncheckTypography,
            shapes = RuncheckShapes,
        ) {
            ChartTheme(colors = chartColors, content = content)
        }
    }
}
