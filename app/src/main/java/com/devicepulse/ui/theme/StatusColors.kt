package com.devicepulse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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

val LightStatusColors = StatusColors(
    healthy = LightHealthy,
    fair = LightFair,
    poor = LightPoor,
    critical = LightCritical,
    neutral = LightNeutral,
    unavailable = LightUnavailable,
    confidenceAccurateBg = LightConfidenceAccurateBg,
    confidenceAccurateText = LightConfidenceAccurateText,
    confidenceEstimatedBg = LightConfidenceEstimatedBg,
    confidenceEstimatedText = LightConfidenceEstimatedText,
    confidenceUnavailableBg = LightConfidenceUnavailableBg,
    confidenceUnavailableText = LightConfidenceUnavailableText
)

val DarkStatusColors = StatusColors(
    healthy = DarkHealthy,
    fair = DarkFair,
    poor = DarkPoor,
    critical = DarkCritical,
    neutral = DarkNeutral,
    unavailable = DarkUnavailable,
    confidenceAccurateBg = DarkConfidenceAccurateBg,
    confidenceAccurateText = DarkConfidenceAccurateText,
    confidenceEstimatedBg = DarkConfidenceEstimatedBg,
    confidenceEstimatedText = DarkConfidenceEstimatedText,
    confidenceUnavailableBg = DarkConfidenceUnavailableBg,
    confidenceUnavailableText = DarkConfidenceUnavailableText
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

val MaterialTheme.statusColors: StatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStatusColors.current
