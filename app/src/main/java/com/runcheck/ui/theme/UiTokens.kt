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
    val iconCircle: Dp = 44.dp,
    val iconCircleInner: Dp = 22.dp,
    val badgeHorizontalPadding: Dp = 12.dp,
    val badgeVerticalPadding: Dp = 4.dp,
    val proBadgeHorizontalPadding: Dp = 8.dp,
    val proBadgeVerticalPadding: Dp = 3.dp,
    val outlineWidth: Dp = 1.dp,
    val outlineAlpha: Float = 0.35f,
    val lockScrimAlpha: Float = 0.18f,
    val proBadgeBackgroundAlpha: Float = 0.12f,
)

val LocalUiTokens = staticCompositionLocalOf { UiTokens() }

val MaterialTheme.uiTokens: UiTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalUiTokens.current
