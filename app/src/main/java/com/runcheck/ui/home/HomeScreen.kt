package com.runcheck.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.domain.model.HealthScore
import com.runcheck.ui.common.healthStatusLabel
import com.runcheck.ui.common.resolve
import com.runcheck.ui.components.CenteredLoadingState
import com.runcheck.ui.components.CenteredRetryState
import com.runcheck.ui.components.ContentContainer
import com.runcheck.ui.components.ObservedScreenScaffold
import com.runcheck.ui.components.PrimaryTopBar
import com.runcheck.ui.components.observedScreenState
import com.runcheck.ui.home.insights.InsightNavigationHandlers
import com.runcheck.ui.home.insights.InsightsCard
import com.runcheck.ui.home.insights.InsightsCardState
import com.runcheck.ui.learn.LearnArticleIds
import com.runcheck.ui.theme.HomePeach
import com.runcheck.ui.theme.HomeTheme
import com.runcheck.ui.theme.homeHealthContextTextStyle
import com.runcheck.ui.theme.homeHealthScoreTextStyle
import com.runcheck.ui.theme.homeHealthScoreUnitTextStyle
import com.runcheck.ui.theme.homeHealthStatusTextStyle
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors
import com.runcheck.ui.theme.uiTokens
import com.runcheck.util.ReleaseSafeLog
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.tan

private const val TAG = "HomeScreen"
private const val MINUTE_MILLIS = 60_000L
private const val HEALTH_GAUGE_SEGMENT_COUNT = 22
private const val HEALTH_GAUGE_ASPECT_RATIO = 1.9f
private const val HEALTH_GAUGE_SWEEP_DEGREES = 168f
private const val HEALTH_GAUGE_GAP_RATIO = 0.2f
private const val HEALTH_GAUGE_SEGMENT_PITCH_DEGREES =
    HEALTH_GAUGE_SWEEP_DEGREES / HEALTH_GAUGE_SEGMENT_COUNT
private const val HEALTH_GAUGE_SEGMENT_DEGREES =
    HEALTH_GAUGE_SEGMENT_PITCH_DEGREES * (1f - HEALTH_GAUGE_GAP_RATIO)
private const val HEALTH_GAUGE_CORNER_RATIO = 0.016f
private const val HEALTH_GAUGE_EXPANDED_FONT_SCALE = 1.5f
private const val HOME_COMPACT_MAX_FONT_SCALE = 1.5f
private val HEALTH_GAUGE_EDGE_INSET = 2.dp
private val HEALTH_GAUGE_BAND_THICKNESS = 36.dp
private val HEALTH_GAUGE_ENDPOINT_LABEL_SIZE = 8.sp
private val HOME_COMPACT_HEIGHT_THRESHOLD = 840.dp

@Composable
fun HomeScreen(
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToLearn: () -> Unit = {},
    onNavigateToLearnArticle: (String) -> Unit = {},
    viewModelProvider: @Composable () -> HomeViewModel = { hiltViewModel() },
) {
    val viewModel = viewModelProvider()
    // CPD-OFF: Keep StateFlow collection at the screen boundary for Compose stability.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val screenState = observedScreenState(uiState, isRefreshing)

    HomeTheme {
        ObservedScreenScaffold(
            onStart = viewModel::startObserving,
            onStop = viewModel::stopObserving,
            modifier = modifier.background(MaterialTheme.colorScheme.background),
            topBar = {
                PrimaryTopBar(
                    title = stringResource(R.string.app_name),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.settings_title),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(MaterialTheme.uiTokens.iconXXLarge),
                            )
                        }
                    },
                )
            },
        ) {
            // CPD-ON
            when (val state = screenState.uiState) {
                is HomeUiState.Loading -> {
                    CenteredLoadingState(description = screenState.loadingDescription)
                }

                is HomeUiState.Error -> {
                    CenteredRetryState(
                        message = state.message.resolve(),
                        onRetry = viewModel::refresh,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }

                is HomeUiState.Success -> {
                    HomeContent(
                        state = state,
                        navigation =
                            HomeNavigationActions(
                                onNavigateToBattery = onNavigateToBattery,
                                onNavigateToNetwork = onNavigateToNetwork,
                                onNavigateToThermal = onNavigateToThermal,
                                onNavigateToStorage = onNavigateToStorage,
                                onNavigateToCharger = onNavigateToCharger,
                                onNavigateToSpeedTest = onNavigateToSpeedTest,
                                onNavigateToAppUsage = onNavigateToAppUsage,
                                onNavigateToInsights = onNavigateToInsights,
                                onNavigateToProUpgrade = onNavigateToProUpgrade,
                                onNavigateToLearn = onNavigateToLearn,
                                onNavigateToLearnArticle = onNavigateToLearnArticle,
                            ),
                        onDismissInsight = { viewModel.dismissInsight(it) },
                        isRefreshing = screenState.isRefreshing,
                        onRefresh = viewModel::refresh,
                    )
                }
            }
        }
    }
}

private data class HomeNavigationActions(
    val onNavigateToBattery: () -> Unit,
    val onNavigateToNetwork: () -> Unit,
    val onNavigateToThermal: () -> Unit,
    val onNavigateToStorage: () -> Unit,
    val onNavigateToCharger: () -> Unit,
    val onNavigateToSpeedTest: () -> Unit,
    val onNavigateToAppUsage: () -> Unit,
    val onNavigateToInsights: () -> Unit,
    val onNavigateToProUpgrade: () -> Unit,
    val onNavigateToLearn: () -> Unit,
    val onNavigateToLearnArticle: (String) -> Unit,
)

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    navigation: HomeNavigationActions,
    onDismissInsight: (Long) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    val insightNavigationHandlers =
        remember(
            navigation,
        ) {
            InsightNavigationHandlers(
                onNavigateToBattery = navigation.onNavigateToBattery,
                onNavigateToNetwork = navigation.onNavigateToNetwork,
                onNavigateToThermal = navigation.onNavigateToThermal,
                onNavigateToStorage = navigation.onNavigateToStorage,
                onNavigateToCharger = navigation.onNavigateToCharger,
                onNavigateToAppUsage = navigation.onNavigateToAppUsage,
                onNavigateToProUpgrade = navigation.onNavigateToProUpgrade,
            )
        }

    val insightsCardState =
        remember(
            state.insights,
            state.totalInsightCount,
            state.unseenInsightCount,
            state.isPro,
        ) {
            InsightsCardState(
                insights = state.insights,
                totalInsightCount = state.totalInsightCount,
                unseenInsightCount = state.unseenInsightCount,
                isPro = state.isPro,
            )
        }

    ContentContainer(
        modifier =
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val spacing = MaterialTheme.spacing
            val tokens = MaterialTheme.uiTokens
            val layoutMode = homeLayoutMode(maxHeight, LocalDensity.current.fontScale)
            val compact = layoutMode == HomeLayoutMode.COMPACT
            val sectionSpacing = if (compact) spacing.sm else spacing.md

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = spacing.base),
            ) {
                Spacer(modifier = Modifier.height(spacing.xs))

                HealthScoreHero(
                    healthScore = state.healthScore,
                    lastUpdatedAtEpochMillis = state.lastUpdatedAtEpochMillis,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal =
                                    if (compact) {
                                        tokens.homeCompactHeroHorizontalPadding
                                    } else {
                                        spacing.lg
                                    },
                            ),
                )

                Spacer(modifier = Modifier.height(sectionSpacing))

                HomeFullCheckButton(
                    isRefreshing = isRefreshing,
                    onClick = onRefresh,
                )

                Spacer(modifier = Modifier.height(sectionSpacing))

                InsightsCard(
                    state = insightsCardState,
                    navigationHandlers = insightNavigationHandlers,
                    onNavigateToInsights = navigation.onNavigateToInsights,
                    onDismissInsight = onDismissInsight,
                )

                if (state.insights.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(sectionSpacing))
                }

                if (state.monitoringStale) {
                    MonitoringStaleWarning(
                        onLearnWhy = {
                            navigation.onNavigateToLearnArticle(LearnArticleIds.BACKGROUND_MONITORING)
                        },
                    )
                    Spacer(modifier = Modifier.height(sectionSpacing))
                }

                HomeStatusTiles(
                    state = state,
                    onNavigateToBattery = navigation.onNavigateToBattery,
                    onNavigateToNetwork = navigation.onNavigateToNetwork,
                    onNavigateToThermal = navigation.onNavigateToThermal,
                    onNavigateToStorage = navigation.onNavigateToStorage,
                    compact = compact,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(sectionSpacing))

                HomeQuickToolsSection(
                    isPro = state.isPro,
                    onNavigateToSpeedTest = navigation.onNavigateToSpeedTest,
                    onNavigateToAppUsage = navigation.onNavigateToAppUsage,
                    onNavigateToProUpgrade = navigation.onNavigateToProUpgrade,
                    onNavigateToLearn = navigation.onNavigateToLearn,
                    compact = compact,
                )

                Spacer(modifier = Modifier.height(if (compact) spacing.sm else spacing.xl))
            }
        }
    }
}

@Composable
private fun HomeFullCheckButton(
    isRefreshing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = MaterialTheme.colorScheme.background
    val contentDescription = stringResource(R.string.home_full_check_content_description)
    val runningStateDescription = stringResource(R.string.home_full_check_state_running)
    val labelStyle: TextStyle =
        MaterialTheme.typography.labelLarge.copy(
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.01).em,
        )

    Button(
        onClick = onClick,
        enabled = !isRefreshing,
        modifier =
            modifier
                .fillMaxWidth()
                .height(MaterialTheme.uiTokens.homePrimaryActionHeight)
                .semantics {
                    this.contentDescription = contentDescription
                    if (isRefreshing) {
                        stateDescription = runningStateDescription
                    }
                },
        shape = RoundedCornerShape(MaterialTheme.uiTokens.homeStatusTileCornerRadius),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = contentColor,
                disabledContainerColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = contentColor,
            ),
        elevation =
            ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
                disabledElevation = 0.dp,
            ),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.base),
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .size(MaterialTheme.uiTokens.iconMedium)
                        .clearAndSetSemantics {},
                color = contentColor,
                strokeWidth = 3.dp,
            )
            Spacer(modifier = Modifier.size(width = 10.dp, height = 0.dp))
        }
        Text(
            text =
                stringResource(
                    if (isRefreshing) {
                        R.string.home_full_check_running
                    } else {
                        R.string.home_full_check
                    },
                ),
            style = labelStyle,
        )
    }
}

@Composable
private fun MonitoringStaleWarning(onLearnWhy: () -> Unit) {
    val context = LocalContext.current
    Card(
        onClick = {
            try {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: android.content.ActivityNotFoundException) {
                try {
                    context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
                } catch (e: android.content.ActivityNotFoundException) {
                    ReleaseSafeLog.warn(TAG, "Failed to open battery optimization settings", e)
                }
            }
        },
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.statusColors.poor.copy(alpha = 0.12f),
            ),
        elevation = runcheckCardElevation(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = stringResource(R.string.a11y_stale_data_warning),
                tint = MaterialTheme.statusColors.poor,
                modifier =
                    Modifier
                        .size(20.dp)
                        .padding(top = 2.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_monitoring_stale_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.statusColors.poor,
                )
                Text(
                    text = stringResource(R.string.home_monitoring_stale_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onLearnWhy,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
                ) {
                    Text(text = stringResource(R.string.home_monitoring_stale_learn_why))
                }
            }
        }
    }
}

@Composable
private fun HealthScoreHero(
    healthScore: HealthScore,
    lastUpdatedAtEpochMillis: Long,
    modifier: Modifier = Modifier,
) {
    val score = healthScore.overallScore
    val healthScoreDescription = stringResource(R.string.a11y_health_score, score)
    val spacing = MaterialTheme.spacing
    val scoreColor = HomePeach
    val statusLabel = healthStatusLabel(healthScore.status)
    val useExpandedTextLayout = LocalDensity.current.fontScale >= HEALTH_GAUGE_EXPANDED_FONT_SCALE
    val minutesSinceUpdate by
        produceState(
            initialValue =
                elapsedWholeMinutes(
                    lastUpdatedAtEpochMillis = lastUpdatedAtEpochMillis,
                    currentEpochMillis = System.currentTimeMillis(),
                ),
            key1 = lastUpdatedAtEpochMillis,
        ) {
            while (true) {
                val elapsedMillis =
                    (System.currentTimeMillis() - lastUpdatedAtEpochMillis).coerceAtLeast(0L)
                value = (elapsedMillis / MINUTE_MILLIS).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                delay(MINUTE_MILLIS - (elapsedMillis % MINUTE_MILLIS))
            }
        }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(HEALTH_GAUGE_ASPECT_RATIO)
                    .semantics(mergeDescendants = true) {
                        contentDescription = healthScoreDescription
                        stateDescription = statusLabel
                        liveRegion = LiveRegionMode.Polite
                    },
        ) {
            val outerRadius = maxWidth / 2 - HEALTH_GAUGE_EDGE_INSET
            val innerRadius = outerRadius - HEALTH_GAUGE_BAND_THICKNESS
            val endpointLabelStyle =
                MaterialTheme.homeHealthScoreUnitTextStyle.copy(
                    fontSize = HEALTH_GAUGE_ENDPOINT_LABEL_SIZE,
                    lineHeight = HEALTH_GAUGE_ENDPOINT_LABEL_SIZE,
                )

            HealthScoreGauge(
                score = score,
                activeColor = scoreColor,
                inactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxSize(),
            )

            if (!useExpandedTextLayout) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(innerRadius)
                            .align(Alignment.TopCenter)
                            .offset(y = HEALTH_GAUGE_EDGE_INSET + HEALTH_GAUGE_BAND_THICKNESS),
                ) {
                    HealthScoreValueAndStatus(
                        score = score,
                        scoreColor = scoreColor,
                        statusLabel = statusLabel,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            Text(
                text = stringResource(R.string.home_health_scale_min),
                style = endpointLabelStyle,
                color = MaterialTheme.colorScheme.outline,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = HEALTH_GAUGE_EDGE_INSET)
                        .clearAndSetSemantics {},
            )
            Text(
                text = stringResource(R.string.home_health_scale_max),
                style = endpointLabelStyle,
                color = MaterialTheme.colorScheme.outline,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = HEALTH_GAUGE_EDGE_INSET)
                        .clearAndSetSemantics {},
            )
        }

        if (useExpandedTextLayout) {
            Spacer(modifier = Modifier.height(spacing.xs))
            HealthScoreValueAndStatus(
                score = score,
                scoreColor = scoreColor,
                statusLabel = statusLabel,
            )
        }

        Spacer(modifier = Modifier.height(spacing.md))

        Text(
            text =
                if (minutesSinceUpdate == 0) {
                    stringResource(R.string.home_updated_just_now)
                } else {
                    pluralStringResource(R.plurals.home_health_context, minutesSinceUpdate, minutesSinceUpdate)
                },
            style = MaterialTheme.homeHealthContextTextStyle,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HealthScoreValueAndStatus(
    score: Int,
    scoreColor: Color,
    statusLabel: String,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = modifier.clearAndSetSemantics {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = score.toString(),
                style = MaterialTheme.homeHealthScoreTextStyle,
                color = scoreColor,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = stringResource(R.string.unit_per_hundred),
                style = MaterialTheme.homeHealthScoreUnitTextStyle,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.alignByBaseline(),
            )
        }

        Spacer(modifier = Modifier.height(spacing.xs))

        Text(
            text = statusLabel,
            style = MaterialTheme.homeHealthStatusTextStyle,
            color = scoreColor,
        )
    }
}

@Composable
private fun HealthScoreGauge(
    score: Int,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
) {
    val filledSegments = filledHealthGaugeSegments(score)

    Canvas(modifier = modifier.clearAndSetSemantics {}) {
        val edgeInset = HEALTH_GAUGE_EDGE_INSET.toPx()
        val outerRadius = size.width / 2f - edgeInset
        val center = Offset(size.width / 2f, outerRadius + edgeInset)
        val innerRadius = outerRadius - HEALTH_GAUGE_BAND_THICKNESS.toPx()
        val segmentHalfAngleRadians =
            Math.toRadians((HEALTH_GAUGE_SEGMENT_DEGREES / 2f).toDouble())
        val outerHalfWidth = outerRadius * tan(segmentHalfAngleRadians).toFloat()
        val innerHalfWidth = innerRadius * tan(segmentHalfAngleRadians).toFloat()
        val cornerRadius = outerRadius * HEALTH_GAUGE_CORNER_RATIO
        val segmentPath =
            healthGaugeSegmentPath(
                center = center,
                outerRadius = outerRadius,
                innerRadius = innerRadius,
                outerHalfWidth = outerHalfWidth,
                innerHalfWidth = innerHalfWidth,
                cornerRadius = cornerRadius,
            )
        val firstRotation =
            -HEALTH_GAUGE_SWEEP_DEGREES / 2f + HEALTH_GAUGE_SEGMENT_PITCH_DEGREES / 2f

        repeat(HEALTH_GAUGE_SEGMENT_COUNT) { index ->
            rotate(
                degrees = firstRotation + HEALTH_GAUGE_SEGMENT_PITCH_DEGREES * index,
                pivot = center,
            ) {
                drawPath(
                    path = segmentPath,
                    color = if (index < filledSegments) activeColor else inactiveColor,
                )
            }
        }
    }
}

private fun healthGaugeSegmentPath(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    outerHalfWidth: Float,
    innerHalfWidth: Float,
    cornerRadius: Float,
): Path {
    val outerY = center.y - outerRadius
    val innerY = center.y - innerRadius

    return Path().apply {
        moveTo(center.x - outerHalfWidth + cornerRadius, outerY)
        lineTo(center.x + outerHalfWidth - cornerRadius, outerY)
        quadraticTo(
            center.x + outerHalfWidth,
            outerY,
            center.x + outerHalfWidth,
            outerY + cornerRadius,
        )
        lineTo(center.x + innerHalfWidth, innerY - cornerRadius)
        quadraticTo(
            center.x + innerHalfWidth,
            innerY,
            center.x + innerHalfWidth - cornerRadius,
            innerY,
        )
        lineTo(center.x - innerHalfWidth + cornerRadius, innerY)
        quadraticTo(
            center.x - innerHalfWidth,
            innerY,
            center.x - innerHalfWidth,
            innerY - cornerRadius,
        )
        lineTo(center.x - outerHalfWidth, outerY + cornerRadius)
        quadraticTo(
            center.x - outerHalfWidth,
            outerY,
            center.x - outerHalfWidth + cornerRadius,
            outerY,
        )
        close()
    }
}

internal fun filledHealthGaugeSegments(score: Int): Int =
    (score.coerceIn(0, 100) * HEALTH_GAUGE_SEGMENT_COUNT / 100f).roundToInt()

internal fun elapsedWholeMinutes(
    lastUpdatedAtEpochMillis: Long,
    currentEpochMillis: Long,
): Int =
    ((currentEpochMillis - lastUpdatedAtEpochMillis).coerceAtLeast(0L) / MINUTE_MILLIS)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()

internal enum class HomeLayoutMode {
    REGULAR,
    COMPACT,
}

internal fun homeLayoutMode(
    availableHeight: Dp,
    fontScale: Float,
): HomeLayoutMode =
    if (availableHeight < HOME_COMPACT_HEIGHT_THRESHOLD && fontScale < HOME_COMPACT_MAX_FONT_SCALE) {
        HomeLayoutMode.COMPACT
    } else {
        HomeLayoutMode.REGULAR
    }
