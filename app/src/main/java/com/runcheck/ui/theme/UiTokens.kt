package com.runcheck.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class UiTokens(
    val touchTarget: Dp = 48.dp,
    val iconTiny: Dp = 12.dp,
    val iconSmall: Dp = 16.dp,
    val iconMedium: Dp = 18.dp,
    val iconLarge: Dp = 20.dp,
    val iconXLarge: Dp = 24.dp,
    val iconCircle: Dp = 44.dp,
    val iconCircleInner: Dp = 22.dp,
    val compactIconCircle: Dp = 36.dp,
    val dialogIcon: Dp = 64.dp,
    val celebrationIcon: Dp = 80.dp,
    val primaryButtonHeight: Dp = 56.dp,
    val compactButtonHeight: Dp = 52.dp,
    val badgeHorizontalPadding: Dp = 12.dp,
    val badgeVerticalPadding: Dp = 4.dp,
    val proBadgeHorizontalPadding: Dp = 8.dp,
    val proBadgeVerticalPadding: Dp = 3.dp,
    val outlineWidth: Dp = 1.dp,
    val outlineAlpha: Float = 0.35f,
    val lockScrimAlpha: Float = 0.18f,
    val proBadgeBackgroundAlpha: Float = 0.12f,
    val chartPlotMinimum: Dp = 180.dp,
    val heroGaugeStroke: Dp = 18.dp,
    val homeHeroGaugeSize: Dp = 112.dp,
    val homeHeroGaugeLargeFontSize: Dp = 144.dp,
    val heroGaugeCompactPadding: Dp = 8.dp,
    val heroGaugeStartAngle: Float = 135f,
    val heroGaugeSweepAngle: Float = 270f,
    val statusPillMinWidth: Dp = 48.dp,
    val statusPillMinHeight: Dp = 32.dp,
    val progressIndicatorStrokeWidth: Dp = 4.dp,
)

val LocalUiTokens = staticCompositionLocalOf { UiTokens() }

val MaterialTheme.uiTokens: UiTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalUiTokens.current
