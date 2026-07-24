package com.runcheck.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.ui.components.info.CrossLinkButton
import com.runcheck.ui.components.info.InfoCard
import com.runcheck.ui.theme.MotionTokens
import com.runcheck.ui.theme.reducedMotion
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.runcheckOutlinedCardBorder
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors
import com.runcheck.ui.theme.uiTokens

@Composable
fun ExpressiveDetailScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(title = title, onBack = onBack)
        ContentContainer(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = MaterialTheme.spacing.base),
                content = content,
            )
        }
    }
}

@Composable
fun <T> ExpressiveSingleChoiceSelector(
    options: List<T>,
    selected: T,
    labelFor: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = labelFor(option),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun RuncheckActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
) {
    val tokens = MaterialTheme.uiTokens
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = runcheckCardColors(),
        border = runcheckOutlinedCardBorder(),
        elevation = runcheckCardElevation(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCircle(
                    icon = icon,
                    tint = iconTint,
                    size = tokens.iconCircle,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = onAction,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = tokens.primaryButtonHeight),
            ) {
                Text(actionLabel)
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
fun InfoBanner(
    id: String,
    title: String,
    message: String,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLearnMore: (() -> Unit)? = null,
) {
    InfoCard(
        id = id,
        headline = title,
        body = message,
        onDismiss = onDismiss,
        modifier = modifier,
        onLearnMore = onLearnMore,
    )
}

enum class StatusTone {
    HEALTHY,
    FAIR,
    POOR,
    CRITICAL,
    NEUTRAL,
    UNAVAILABLE,
}

@Composable
fun StatusPill(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = MaterialTheme.statusColors
    val (container, foreground) =
        when (tone) {
            StatusTone.HEALTHY -> colors.healthyContainer to colors.onHealthyContainer
            StatusTone.FAIR -> colors.fairContainer to colors.onFairContainer
            StatusTone.POOR -> colors.poorContainer to colors.onPoorContainer
            StatusTone.CRITICAL -> colors.criticalContainer to colors.onCriticalContainer
            StatusTone.NEUTRAL -> colors.neutralContainer to colors.onNeutralContainer
            StatusTone.UNAVAILABLE -> colors.unavailableContainer to colors.onUnavailableContainer
        }
    val tokens = MaterialTheme.uiTokens

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = foreground,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier =
                Modifier.padding(
                    horizontal = tokens.badgeHorizontalPadding,
                    vertical = tokens.badgeVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                androidx.compose.foundation.layout.Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(tokens.iconSmall),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun LearnTopicLink(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CrossLinkButton(
        label = label,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun ExpressiveEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier.sizeIn(
                    minWidth = MaterialTheme.uiTokens.touchTarget,
                    minHeight = MaterialTheme.uiTokens.touchTarget,
                ),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun RuncheckLoadingIndicator(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val reducedMotion = MaterialTheme.reducedMotion
    val indicatorModifier =
        modifier.then(
            if (contentDescription == null) {
                Modifier
            } else {
                Modifier.semantics { this.contentDescription = contentDescription }
            },
        )
    if (reducedMotion) {
        LoadingIndicator(
            progress = { 0.6f },
            modifier = indicatorModifier,
            color = MaterialTheme.colorScheme.primary,
        )
    } else {
        LoadingIndicator(
            modifier = indicatorModifier,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun RuncheckWavyProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    contentDescription: String,
    content: @Composable () -> Unit = {},
) {
    val reducedMotion = MaterialTheme.reducedMotion
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec =
            tween(
                durationMillis = if (reducedMotion) 0 else MotionTokens.RING,
                easing = MotionTokens.EaseOut,
            ),
        label = "runcheckWavyProgress",
    )

    Box(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(animatedProgress, 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        CircularWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.matchParentSize(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            amplitude =
                if (reducedMotion) {
                    { 0f }
                } else {
                    WavyProgressIndicatorDefaults.indicatorAmplitude
                },
            waveSpeed =
                if (reducedMotion) {
                    0.dp
                } else {
                    WavyProgressIndicatorDefaults.CircularWavelength
                },
        )
        content()
    }
}

@Composable
fun AppDisplayName(
    appLabel: String?,
    packageName: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Text(
        text =
            resolveAppDisplayName(
                appLabel = appLabel,
                packageName = packageName,
                unknownAppLabel = stringResource(R.string.app_unknown_name),
            ),
        modifier = modifier,
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

internal fun resolveAppDisplayName(
    appLabel: String?,
    packageName: String,
    unknownAppLabel: String,
): String {
    val resolvedLabel = appLabel?.trim().orEmpty()
    if (resolvedLabel.isNotEmpty() && resolvedLabel != packageName) return resolvedLabel

    val packageTail =
        packageName
            .substringAfterLast('.')
            .trim()
            .replace('_', ' ')
            .replace('-', ' ')
            .split(' ')
            .filter(String::isNotBlank)
            .joinToString(" ") { part ->
                part.replaceFirstChar { first -> first.titlecase() }
            }
    return packageTail.ifEmpty { unknownAppLabel }
}
