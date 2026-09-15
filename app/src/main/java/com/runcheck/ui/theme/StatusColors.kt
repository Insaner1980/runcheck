package com.runcheck.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.HealthStatus
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.SignalQuality
import com.runcheck.ui.common.BatteryTemperaturePresentation
import com.runcheck.ui.common.StorageUsagePresentation

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
        critical = StatusCritical,
        neutral = TextSecondary,
        unavailable = TextMuted,
        confidenceAccurateBg = AccentBlue,
        confidenceAccurateText = BgPage,
        confidenceEstimatedBg = AccentAmber,
        confidenceEstimatedText = BgPage,
        confidenceUnavailableBg = TextMuted,
        confidenceUnavailableText = BgPage,
    )

val LocalStatusColors = staticCompositionLocalOf { RuncheckStatusColors }

@Composable
@ReadOnlyComposable
fun statusColorForPercent(percent: Int): Color = statusColor(HealthScore.statusFromScore(percent))

@Composable
@ReadOnlyComposable
fun statusColor(status: HealthStatus): Color = MaterialTheme.statusColors.forHealthStatus(status)

fun StatusColors.forHealthStatus(status: HealthStatus): Color =
    when (status) {
        HealthStatus.HEALTHY -> healthy
        HealthStatus.FAIR -> fair
        HealthStatus.POOR -> poor
        HealthStatus.CRITICAL -> critical
    }

@Composable
@ReadOnlyComposable
fun statusColorForBatteryTemperature(tempC: Float): Color =
    statusColor(BatteryTemperaturePresentation.classify(tempC).severity)

// Preserve the legacy CPU pill colors independently of battery presentation.
@Composable
@ReadOnlyComposable
fun statusColorForCpuTemperature(tempC: Float): Color {
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
fun statusColorForStoragePercent(usedPercent: Int): Color =
    statusColor(StorageUsagePresentation.classify(usedPercent.toFloat()))

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
