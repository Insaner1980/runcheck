package com.runcheck.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.SignalQuality

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
    val confidenceUnavailableText: Color,
)

val RuncheckStatusColors =
    StatusColors(
        healthy = AccentTeal,
        fair = AccentAmber,
        poor = AccentOrange,
        critical = AccentRed,
        neutral = TextSecondary,
        unavailable = TextMuted,
        confidenceAccurateBg = AccentBlue,
        confidenceAccurateText = BgPage,
        confidenceEstimatedBg = AccentAmber,
        confidenceEstimatedText = BgPage,
        confidenceUnavailableBg = TextMuted,
        confidenceUnavailableText = TextPrimary,
    )

val LocalStatusColors = staticCompositionLocalOf { RuncheckStatusColors }

@Composable
@ReadOnlyComposable
fun statusColorForPercent(percent: Int): Color {
    val colors = MaterialTheme.statusColors
    return when {
        percent >= 75 -> colors.healthy
        percent >= 50 -> colors.fair
        percent >= 25 -> colors.poor
        else -> colors.critical
    }
}

@Composable
@ReadOnlyComposable
fun statusColorForTemperature(tempC: Float): Color {
    val colors = MaterialTheme.statusColors
    return when {
        tempC >= 45f -> colors.critical
        tempC >= 40f -> colors.poor
        tempC >= 35f -> colors.fair
        else -> colors.healthy
    }
}

@Composable
@ReadOnlyComposable
fun statusColorForStoragePercent(usedPercent: Int): Color {
    val colors = MaterialTheme.statusColors
    return when {
        usedPercent >= 95 -> colors.critical
        usedPercent >= 85 -> colors.poor
        usedPercent >= 75 -> colors.fair
        else -> colors.healthy
    }
}

@Composable
@ReadOnlyComposable
fun statusColorForSignalQuality(quality: SignalQuality): Color {
    val colors = MaterialTheme.statusColors
    return when (quality) {
        SignalQuality.EXCELLENT -> colors.healthy
        SignalQuality.GOOD -> colors.healthy
        SignalQuality.FAIR -> colors.fair
        SignalQuality.POOR -> colors.poor
        SignalQuality.NO_SIGNAL -> colors.critical
    }
}

@Composable
@ReadOnlyComposable
fun categoryColor(category: MediaCategory): Color =
    when (category) {
        MediaCategory.IMAGE -> AccentBlue
        MediaCategory.VIDEO -> AccentLime
        MediaCategory.AUDIO -> AccentYellow
        MediaCategory.DOCUMENT -> MaterialTheme.colorScheme.onSurfaceVariant
        MediaCategory.DOWNLOAD -> AccentBlue.copy(alpha = 0.6f)
        MediaCategory.APK -> MaterialTheme.colorScheme.onSurfaceVariant
        MediaCategory.OTHER -> MaterialTheme.colorScheme.outline
    }

val MaterialTheme.statusColors: StatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStatusColors.current
