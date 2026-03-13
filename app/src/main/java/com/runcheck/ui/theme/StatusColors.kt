package com.devicepulse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class StatusColors(
    val healthy: Color,
    val fair: Color,
    val poor: Color,
    val critical: Color,
    val neutral: Color,
    val unavailable: Color,
    val confidenceAccurateBg: Color,
    val confidenceAccurateText: Color,
    val confidenceEstimatedBg: Color,
    val confidenceEstimatedText: Color,
    val confidenceUnavailableBg: Color,
    val confidenceUnavailableText: Color
)

val DevicePulseStatusColors = StatusColors(
    healthy = AccentTeal,
    fair = AccentOrange,
    poor = AccentOrange,
    critical = AccentRed,
    neutral = AccentBlue,
    unavailable = TextMuted,
    confidenceAccurateBg = AccentTeal,
    confidenceAccurateText = BgPage,
    confidenceEstimatedBg = AccentOrange,
    confidenceEstimatedText = BgPage,
    confidenceUnavailableBg = TextMuted,
    confidenceUnavailableText = TextPrimary
)

val LocalStatusColors = staticCompositionLocalOf { DevicePulseStatusColors }

val MaterialTheme.statusColors: StatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStatusColors.current
