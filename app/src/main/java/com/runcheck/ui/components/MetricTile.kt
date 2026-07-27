package com.runcheck.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import com.runcheck.R
import com.runcheck.domain.model.Confidence
import com.runcheck.ui.theme.cardMetricTextStyle
import com.runcheck.ui.theme.domainColors
import com.runcheck.ui.theme.runcheckCardBorder
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.uiTokens

enum class MetricDomain {
    BATTERY,
    NETWORK,
    THERMAL,
    STORAGE,
}

enum class MetricTileState {
    READY,
    LOADING,
    UNAVAILABLE,
}

internal data class MetricTilePresentation(
    val displayValue: String,
    val statusLabel: String?,
    val showLoadingIndicator: Boolean,
)

internal enum class MetricTileSlotVisibility {
    VISIBLE,
    PLACEHOLDER,
}

internal data class MetricTileSlotPolicy(
    val status: MetricTileSlotVisibility,
    val confidence: MetricTileSlotVisibility,
)

internal fun metricTileSlotPolicy(
    state: MetricTileState,
    hasStatus: Boolean,
    hasConfidence: Boolean,
): MetricTileSlotPolicy =
    MetricTileSlotPolicy(
        status =
            if (state != MetricTileState.READY || hasStatus) {
                MetricTileSlotVisibility.VISIBLE
            } else {
                MetricTileSlotVisibility.PLACEHOLDER
            },
        confidence =
            if (state == MetricTileState.READY && hasConfidence) {
                MetricTileSlotVisibility.VISIBLE
            } else {
                MetricTileSlotVisibility.PLACEHOLDER
            },
    )

internal fun metricTilePresentation(
    state: MetricTileState,
    value: String,
    status: String?,
    loadingLabel: String,
    unavailableLabel: String,
): MetricTilePresentation =
    when (state) {
        MetricTileState.READY -> {
            MetricTilePresentation(
                displayValue = value,
                statusLabel = status,
                showLoadingIndicator = false,
            )
        }

        MetricTileState.LOADING -> {
            MetricTilePresentation(
                displayValue = "—",
                statusLabel = loadingLabel,
                showLoadingIndicator = true,
            )
        }

        MetricTileState.UNAVAILABLE -> {
            MetricTilePresentation(
                displayValue = unavailableLabel,
                statusLabel = unavailableLabel,
                showLoadingIndicator = false,
            )
        }
    }

@Composable
fun MetricTile(
    domain: MetricDomain,
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
    state: MetricTileState = MetricTileState.READY,
    status: String? = null,
    statusTone: StatusTone = StatusTone.NEUTRAL,
    confidence: Confidence? = null,
    onClick: (() -> Unit)? = null,
) {
    val loadingLabel = stringResource(R.string.a11y_loading)
    val unavailableLabel = stringResource(R.string.not_available)
    val presentation =
        metricTilePresentation(
            state = state,
            value = value,
            status = status,
            loadingLabel = loadingLabel,
            unavailableLabel = unavailableLabel,
        )
    val tokens = MaterialTheme.uiTokens
    val slotPolicy =
        metricTileSlotPolicy(
            state = state,
            hasStatus = status != null,
            hasConfidence = confidence != null,
        )
    val accent = domain.accentColor()
    val icon = domain.outlinedIcon()
    val clickModifier =
        if (onClick == null) {
            Modifier
        } else {
            Modifier.clickable(
                role = Role.Button,
                onClick = onClick,
            )
        }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = tokens.touchTarget)
                .then(clickModifier)
                .semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        border = runcheckCardBorder(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.cardInternal),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.cardGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconCircle(
                    icon = icon,
                    tint = accent,
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = tokens.touchTarget),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = presentation.displayValue,
                        style = MaterialTheme.cardMetricTextStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier.size(tokens.touchTarget),
                    contentAlignment = Alignment.Center,
                ) {
                    if (presentation.showLoadingIndicator) {
                        RuncheckProgressSpinner()
                    }
                }
            }
            MetricTileReservedSlot(visibility = slotPolicy.status) {
                StatusPill(
                    label = presentation.statusLabel ?: unavailableLabel,
                    tone = if (state == MetricTileState.UNAVAILABLE) StatusTone.UNAVAILABLE else statusTone,
                )
            }
            MetricTileReservedSlot(visibility = slotPolicy.confidence) {
                ConfidenceBadge(confidence = confidence ?: Confidence.HIGH)
            }
        }
    }
}

@Composable
private fun MetricTileReservedSlot(
    visibility: MetricTileSlotVisibility,
    content: @Composable () -> Unit,
) {
    val modifier =
        if (visibility == MetricTileSlotVisibility.VISIBLE) {
            Modifier
        } else {
            Modifier
                .alpha(0f)
                .clearAndSetSemantics {}
        }
    Box(modifier = modifier) {
        content()
    }
}

@Composable
private fun MetricDomain.accentColor(): Color {
    val colors = MaterialTheme.domainColors
    return when (this) {
        MetricDomain.BATTERY -> colors.battery
        MetricDomain.NETWORK -> colors.network
        MetricDomain.THERMAL -> colors.thermal
        MetricDomain.STORAGE -> colors.storage
    }
}

private fun MetricDomain.outlinedIcon(): ImageVector =
    when (this) {
        MetricDomain.BATTERY -> Icons.Outlined.BatteryChargingFull
        MetricDomain.NETWORK -> Icons.Outlined.Wifi
        MetricDomain.THERMAL -> Icons.Outlined.DeviceThermostat
        MetricDomain.STORAGE -> Icons.Outlined.Storage
    }
