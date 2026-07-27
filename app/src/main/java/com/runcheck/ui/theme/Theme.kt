package com.runcheck.ui.theme

import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.runcheck.domain.model.ThemeMode

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalNumericFontFamily = staticCompositionLocalOf { JetBrainsMonoFontFamily }
private val LocalHeroCardColor = staticCompositionLocalOf { Surface2Dark }
private val LocalMainCardBorderEnabled = staticCompositionLocalOf { false }

@Immutable
data class DomainColors(
    val battery: Color,
    val network: Color,
    val thermal: Color,
    val storage: Color,
)

internal val DarkDomainColors =
    DomainColors(
        battery = BatteryAccentDark,
        network = NetworkAccentDark,
        thermal = ThermalAccentDark,
        storage = StorageAccentDark,
    )

internal val LightDomainColors =
    DomainColors(
        battery = BatteryAccentLight,
        network = NetworkAccentLight,
        thermal = ThermalAccentLight,
        storage = StorageAccentLight,
    )

private val LocalDomainColors = staticCompositionLocalOf { DarkDomainColors }

val MaterialTheme.reducedMotion: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalReducedMotion.current

val MaterialTheme.heroCardColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalHeroCardColor.current

val MaterialTheme.domainColors: DomainColors
    @Composable
    @ReadOnlyComposable
    get() = LocalDomainColors.current

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

internal fun mainCardBorderEnabled(darkTheme: Boolean): Boolean = !darkTheme

@Composable
fun runcheckCardBorder(): BorderStroke? =
    if (LocalMainCardBorderEnabled.current) {
        BorderStroke(
            width = MaterialTheme.uiTokens.outlineWidth,
            color = MaterialTheme.colorScheme.outline,
        )
    } else {
        null
    }

@Composable
fun runcheckOutlinedCardBorder(): BorderStroke =
    BorderStroke(width = MaterialTheme.uiTokens.outlineWidth, color = MaterialTheme.cardStrokeColor)

internal val DarkRuncheckColorScheme =
    darkColorScheme(
        primary = NetworkAccentDark,
        onPrimary = BgPageDark,
        primaryContainer = Surface3Dark,
        onPrimaryContainer = TextPrimaryDark,
        inversePrimary = NetworkAccentDark,
        secondary = StorageAccentDark,
        onSecondary = BgPageDark,
        secondaryContainer = Surface3Dark,
        onSecondaryContainer = TextPrimaryDark,
        tertiary = BatteryAccentDark,
        onTertiary = BgPageDark,
        tertiaryContainer = Surface2Dark,
        onTertiaryContainer = TextPrimaryDark,
        background = BgPageDark,
        onBackground = TextPrimaryDark,
        surface = BgPageDark,
        onSurface = TextPrimaryDark,
        surfaceVariant = Surface3Dark,
        onSurfaceVariant = TextSecondaryDark,
        surfaceTint = NetworkAccentDark,
        inverseSurface = TextPrimaryDark,
        inverseOnSurface = BgPageDark,
        error = AccentRed,
        onError = BgPageDark,
        errorContainer = AccentRed,
        onErrorContainer = BgPageDark,
        outline = TextMutedDark,
        outlineVariant = TextMutedDark,
        scrim = BgPageDark,
        surfaceBright = Surface3Dark,
        surfaceDim = BgPageDark,
        surfaceContainer = Surface1Dark,
        surfaceContainerHigh = Surface2Dark,
        surfaceContainerHighest = Surface3Dark,
        surfaceContainerLow = Surface1Dark,
        surfaceContainerLowest = BgPageDark,
        primaryFixed = NetworkAccentDark,
        primaryFixedDim = LightPrimary,
        onPrimaryFixed = BgPageDark,
        onPrimaryFixedVariant = BgPageDark,
        secondaryFixed = StorageAccentDark,
        secondaryFixedDim = LightSecondary,
        onSecondaryFixed = BgPageDark,
        onSecondaryFixedVariant = BgPageDark,
        tertiaryFixed = BatteryAccentDark,
        tertiaryFixedDim = LightTertiary,
        onTertiaryFixed = BgPageDark,
        onTertiaryFixedVariant = BgPageDark,
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
        background = BgPageLight,
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
        outline = LightCardBorder,
        outlineVariant = LightCardBorder,
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
    val domainColors = if (darkTheme) DarkDomainColors else LightDomainColors
    val heroCardColor = if (darkTheme) Surface2Dark else Surface1Light
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
        LocalDomainColors provides domainColors,
        LocalMainCardBorderEnabled provides mainCardBorderEnabled(darkTheme),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RuncheckTypography,
            shapes = RuncheckShapes,
        ) {
            ChartTheme(colors = chartColors, content = content)
        }
    }
}
