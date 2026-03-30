package com.runcheck.ui.theme

import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalNumericFontFamily = staticCompositionLocalOf { JetBrainsMonoFontFamily }

val MaterialTheme.reducedMotion: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalReducedMotion.current

val MaterialTheme.heroCardColor: Color
    @Composable
    @ReadOnlyComposable
    get() = BgCardDeep

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

private val RuncheckColorScheme =
    darkColorScheme(
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
        onError = TextPrimary,
    )

@Composable
fun RuncheckTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current

    val reducedMotion =
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        } catch (_: Exception) {
            false
        }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalStatusColors provides RuncheckStatusColors,
        LocalReducedMotion provides reducedMotion,
        LocalNumericFontFamily provides JetBrainsMonoFontFamily,
        LocalUiTokens provides UiTokens(),
    ) {
        MaterialTheme(
            colorScheme = RuncheckColorScheme,
            typography = RuncheckTypography,
            shapes = RuncheckShapes,
            content = content,
        )
    }
}
