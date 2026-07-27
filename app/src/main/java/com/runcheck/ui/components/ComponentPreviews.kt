package com.runcheck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.ThemeMode
import com.runcheck.ui.theme.PreviewsRuncheckFontScale
import com.runcheck.ui.theme.PreviewsRuncheckThemes
import com.runcheck.ui.theme.RuncheckTheme
import com.runcheck.ui.theme.domainColors
import com.runcheck.ui.theme.spacing

@PreviewsRuncheckThemes
@Composable
private fun SignatureComponentsPreview() {
    PreviewSurface {
        HeroGauge(
            value = 82f,
            label = "Device health",
            status = "Healthy",
            accent = MaterialTheme.domainColors.battery,
            contentDescription = "Device health",
            animationKey = "preview",
            confidence = Confidence.HIGH,
            modifier = Modifier.fillMaxWidth(),
        )
        MetricTile(
            domain = MetricDomain.NETWORK,
            value = "-62",
            unit = "dBm",
            label = "Signal",
            status = "Good",
            statusTone = StatusTone.HEALTHY,
            confidence = Confidence.HIGH,
            onClick = {},
        )
        StatBlock(
            label = "Available storage",
            value = "118",
            unit = "GB",
            status = "Plenty of space",
            statusTone = StatusTone.HEALTHY,
        )
        StatusPill(
            label = "Healthy",
            tone = StatusTone.HEALTHY,
            icon = Icons.Outlined.CheckCircle,
        )
    }
}

@PreviewsRuncheckThemes
@Composable
private fun ComponentStatesPreview() {
    PreviewSurface {
        MetricTile(
            domain = MetricDomain.THERMAL,
            value = "36.5",
            unit = "°C",
            label = "Temperature",
            status = "Healthy",
            statusTone = StatusTone.HEALTHY,
            confidence = Confidence.HIGH,
        )
        MetricTile(
            domain = MetricDomain.THERMAL,
            value = "36.5",
            unit = "°C",
            label = "Temperature",
            state = MetricTileState.LOADING,
        )
        MetricTile(
            domain = MetricDomain.STORAGE,
            value = "0",
            unit = "GB",
            label = "Available storage",
            state = MetricTileState.UNAVAILABLE,
            confidence = Confidence.UNAVAILABLE,
        )
        ProFeatureLockedState(
            title = "Extended history",
            message = "Unlock longer history and export-ready comparisons.",
            actionLabel = "Upgrade",
            onAction = {},
        )
        EmptyStateIllustration(
            title = "No measurements yet",
            message = "Keep runcheck open for a moment while the first reading arrives.",
            actionLabel = "Try again",
            onAction = {},
        )
    }
}

@PreviewsRuncheckFontScale
@Composable
private fun LongTextAndFontScalePreview() {
    PreviewSurface {
        SectionHeader(
            text = "Measurements requiring additional device support",
            count = 12,
            actionLabel = "View all",
            onAction = {},
        )
        MetricTile(
            domain = MetricDomain.BATTERY,
            value = "100",
            unit = "percent of the estimated design capacity",
            label = "A deliberately long battery health measurement label",
            status = "Estimated from the measurements available on this device",
            statusTone = StatusTone.FAIR,
        )
        StatusPill(
            label = "A long status that remains a readable single line",
            tone = StatusTone.NEUTRAL,
        )
    }
}

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    RuncheckTheme(themeMode = ThemeMode.SYSTEM) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.cardGap),
                content = { content() },
            )
        }
    }
}
