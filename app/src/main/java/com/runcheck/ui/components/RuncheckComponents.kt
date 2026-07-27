package com.runcheck.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.MeasuredValue
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

private const val FIXED_SELECTOR_OPTION_LIMIT = 4
private const val SCROLLING_SELECTOR_OPTION_WIDTH_DP = 104
private const val MINIMUM_TOUCH_TARGET_DP = 48

internal data class SelectorLayoutPolicy(
    val isScrollable: Boolean,
    val minimumOptionWidthDp: Int?,
    val minimumTouchTargetDp: Int = MINIMUM_TOUCH_TARGET_DP,
)

internal fun selectorLayoutPolicy(optionCount: Int): SelectorLayoutPolicy =
    if (optionCount > FIXED_SELECTOR_OPTION_LIMIT) {
        SelectorLayoutPolicy(
            isScrollable = true,
            minimumOptionWidthDp = SCROLLING_SELECTOR_OPTION_WIDTH_DP,
        )
    } else {
        SelectorLayoutPolicy(
            isScrollable = false,
            minimumOptionWidthDp = null,
        )
    }

internal fun <T> platformTelemetryMeasurement(
    value: T?,
    unavailableValue: T,
): MeasuredValue<T> =
    MeasuredValue(
        value = value ?: unavailableValue,
        confidence = if (value == null) Confidence.UNAVAILABLE else Confidence.LOW,
    )

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RuncheckDetailScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        ContentContainer(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
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
fun <T> RuncheckSingleChoiceSelector(
    options: List<T>,
    selected: T,
    labelFor: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val policy = selectorLayoutPolicy(options.size)
    val scrollState = rememberScrollState()
    Box(
        modifier =
            if (policy.isScrollable) {
                modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            } else {
                modifier.fillMaxWidth()
            },
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = if (policy.isScrollable) Modifier else Modifier.fillMaxWidth(),
        ) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    modifier =
                        if (policy.isScrollable) {
                            Modifier
                                .widthIn(min = requireNotNull(policy.minimumOptionWidthDp).dp)
                                .defaultMinSize(minHeight = policy.minimumTouchTargetDp.dp)
                        } else {
                            Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = policy.minimumTouchTargetDp.dp)
                        },
                ) {
                    Text(
                        text = labelFor(option),
                        maxLines = if (policy.isScrollable) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun MeasuredHeroValue(
    value: String,
    unit: String,
    confidence: Confidence,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = MaterialTheme.typography.displaySmall,
    unitStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = valueStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = unit,
                style = unitStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = MaterialTheme.spacing.xs),
            )
        }
        ConfidenceBadge(confidence = confidence)
    }
}

@Composable
fun SecondaryActionLink(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier =
            modifier.defaultMinSize(
                minHeight = MaterialTheme.uiTokens.touchTarget,
            ),
    ) {
        Text(label)
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.uiTokens.iconMedium),
        )
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
                androidx.compose.foundation.layout.Arrangement
                    .spacedBy(MaterialTheme.spacing.xs),
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
fun RuncheckEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
    illustration: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (illustration == null) {
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
        } else {
            illustration()
        }
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
fun RuncheckProgressSpinner(
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
        CircularProgressIndicator(
            progress = { 0.6f },
            modifier = indicatorModifier,
            color = MaterialTheme.colorScheme.primary,
        )
    } else {
        CircularProgressIndicator(
            modifier = indicatorModifier,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun RuncheckProgressGauge(
    progress: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
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
        label = "runcheckProgressGauge",
    )

    Box(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(animatedProgress, 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.matchParentSize(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
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
