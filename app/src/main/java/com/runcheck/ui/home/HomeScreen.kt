package com.runcheck.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.HealthStatus
import com.runcheck.pro.ProStatus
import com.runcheck.ui.common.LifecycleStartStopEffect
import com.runcheck.ui.common.rememberFormattedDateTime
import com.runcheck.ui.common.resolve
import com.runcheck.ui.common.scoreLabel
import com.runcheck.ui.components.ConfidenceBadge
import com.runcheck.ui.components.ContentContainer
import com.runcheck.ui.components.HeroGauge
import com.runcheck.ui.components.InfoBanner
import com.runcheck.ui.components.PrimaryTopBar
import com.runcheck.ui.components.RuncheckProgressSpinner
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.components.StatusPill
import com.runcheck.ui.components.StatusTone
import com.runcheck.ui.home.insights.InsightNavigationHandlers
import com.runcheck.ui.home.insights.InsightsCard
import com.runcheck.ui.home.insights.InsightsCardState
import com.runcheck.ui.learn.LearnArticleIds
import com.runcheck.ui.pro.TrialExpirationModal
import com.runcheck.ui.pro.TrialHomeCard
import com.runcheck.ui.pro.TrialWelcomeSheet
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.runcheckHeroCardColors
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.uiTokens

@Composable
fun HomeScreen(
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToLearnArticle: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loadingDescription = stringResource(R.string.a11y_loading)

    LifecycleStartStopEffect(
        onStart = viewModel::startObserving,
        onStop = viewModel::stopObserving,
    )

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTopBar(
            title = stringResource(R.string.app_name),
            actions = {
                IconButton(onClick = viewModel::refresh) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.home_refresh),
                    )
                }
            },
        )

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    contentAlignment = Alignment.Center,
                ) {
                    RuncheckProgressSpinner(contentDescription = loadingDescription)
                }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    ) {
                        Text(
                            text = state.message.resolve(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = viewModel::refresh) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }

            is HomeUiState.Success -> {
                if (state.showWelcomeSheet) {
                    TrialWelcomeSheet(onDismiss = viewModel::dismissWelcomeSheet)
                }

                val snackbarHostState = remember { SnackbarHostState() }
                val day5Message = stringResource(R.string.trial_day5_banner)
                LaunchedEffect(state.showDay5Banner) {
                    if (state.showDay5Banner) {
                        snackbarHostState.showSnackbar(day5Message)
                        viewModel.dismissDay5Banner()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    HomeContent(
                        state = state,
                        onNavigateToBattery = onNavigateToBattery,
                        onNavigateToNetwork = onNavigateToNetwork,
                        onNavigateToThermal = onNavigateToThermal,
                        onNavigateToStorage = onNavigateToStorage,
                        onNavigateToCharger = onNavigateToCharger,
                        onNavigateToAppUsage = onNavigateToAppUsage,
                        onNavigateToInsights = onNavigateToInsights,
                        onDismissInsight = viewModel::dismissInsight,
                        onNavigateToProUpgrade = onNavigateToProUpgrade,
                        onNavigateToLearnArticle = onNavigateToLearnArticle,
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }

                if (state.showExpirationModal) {
                    TrialExpirationModal(
                        formattedPrice = null,
                        onPurchase = { viewModel.dismissExpirationModal(onNavigateToProUpgrade) },
                        onDismiss = { viewModel.dismissExpirationModal() },
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomeContent(
    state: HomeUiState.Success,
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onDismissInsight: (Long) -> Unit,
    onNavigateToProUpgrade: () -> Unit,
    onNavigateToLearnArticle: (String) -> Unit,
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val isWideScreen = with(density) { windowInfo.containerSize.width.toDp() } >= 600.dp
    val insightNavigationHandlers =
        remember(
            onNavigateToBattery,
            onNavigateToNetwork,
            onNavigateToThermal,
            onNavigateToStorage,
            onNavigateToCharger,
            onNavigateToAppUsage,
            onNavigateToProUpgrade,
        ) {
            InsightNavigationHandlers(
                onNavigateToBattery = onNavigateToBattery,
                onNavigateToNetwork = onNavigateToNetwork,
                onNavigateToThermal = onNavigateToThermal,
                onNavigateToStorage = onNavigateToStorage,
                onNavigateToCharger = onNavigateToCharger,
                onNavigateToAppUsage = onNavigateToAppUsage,
                onNavigateToProUpgrade = onNavigateToProUpgrade,
            )
        }

    val insightsCardState =
        remember(
            state.insights,
            state.unseenInsightCount,
            state.isPro,
        ) {
            InsightsCardState(
                insights = state.insights,
                unseenInsightCount = state.unseenInsightCount,
                isPro = state.isPro,
            )
        }

    ContentContainer {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.base),
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            if (state.monitoringStale) {
                InfoBanner(
                    id = "home_monitoring_stale",
                    title = stringResource(R.string.home_monitoring_stale_title),
                    message = stringResource(R.string.home_monitoring_stale_body),
                    onDismiss = {},
                    onLearnMore = {
                        onNavigateToLearnArticle(LearnArticleIds.BACKGROUND_MONITORING)
                    },
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
            }

            HomeHealthHero(
                healthScore = state.healthScore,
                measurementTimestampMillis = state.measurementTimestampMillis,
                measurementConfidence = state.batteryState.currentMa.confidence,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            HomeGridSection(
                state = state,
                isWideScreen = isWideScreen,
                onNavigateToBattery = onNavigateToBattery,
                onNavigateToNetwork = onNavigateToNetwork,
                onNavigateToThermal = onNavigateToThermal,
                onNavigateToStorage = onNavigateToStorage,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

            InsightsCard(
                state = insightsCardState,
                navigationHandlers = insightNavigationHandlers,
                onNavigateToInsights = onNavigateToInsights,
                onDismissInsight = onDismissInsight,
            )

            if (state.proState.status == ProStatus.TRIAL_ACTIVE) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
                TrialHomeCard(
                    proState = state.proState,
                    onNavigateToProUpgrade = onNavigateToProUpgrade,
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

@Composable
internal fun HomeHealthHero(
    healthScore: HealthScore,
    measurementTimestampMillis: Long,
    measurementConfidence: Confidence,
    modifier: Modifier = Modifier,
) {
    val score = healthScore.overallScore.coerceIn(0, 100)
    val status = HealthScore.statusFromScore(score)
    val statusLabel = scoreLabel(score)
    val scoreDescription = stringResource(R.string.a11y_health_score, score)
    val useExpandedLayout = LocalDensity.current.fontScale >= 1.3f
    val tokens = MaterialTheme.uiTokens
    val gaugeSize =
        if (useExpandedLayout) {
            tokens.homeHeroGaugeLargeFontSize
        } else {
            tokens.homeHeroGaugeSize
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = runcheckHeroCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            SectionHeader(text = stringResource(R.string.home_health_score))
            if (useExpandedLayout) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                ) {
                    HomeHeroGauge(
                        score = score,
                        statusLabel = statusLabel,
                        measurementConfidence = measurementConfidence,
                        scoreDescription = scoreDescription,
                        modifier = Modifier.size(gaugeSize),
                    )
                    HealthScoreBreakdown(
                        healthScore = healthScore,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HomeHeroGauge(
                        score = score,
                        statusLabel = statusLabel,
                        measurementConfidence = measurementConfidence,
                        scoreDescription = scoreDescription,
                        modifier = Modifier.size(gaugeSize),
                    )
                    HealthScoreBreakdown(
                        healthScore = healthScore,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            HealthMeasurementRow(
                statusLabel = statusLabel,
                status = status,
                measurementTimestampMillis = measurementTimestampMillis,
                measurementConfidence = measurementConfidence,
                stackContent = useExpandedLayout,
            )
        }
    }
}

@Composable
private fun HomeHeroGauge(
    score: Int,
    statusLabel: String,
    measurementConfidence: Confidence,
    scoreDescription: String,
    modifier: Modifier = Modifier,
) {
    HeroGauge(
        value = score.toFloat(),
        label = stringResource(R.string.home_health_score),
        status = statusLabel,
        accent = MaterialTheme.colorScheme.onSurface,
        contentDescription = scoreDescription,
        animationKey = "home-health-score",
        confidence = measurementConfidence,
        showDetails = false,
        contentPadding = MaterialTheme.uiTokens.heroGaugeCompactPadding,
        modifier = modifier,
    )
}

@Composable
private fun HealthScoreBreakdown(
    healthScore: HealthScore,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            HealthSubscore(
                label = stringResource(R.string.home_battery_card),
                score = healthScore.batteryScore,
            )
            HealthSubscore(
                label = stringResource(R.string.home_network_card),
                score = healthScore.networkScore,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            HealthSubscore(
                label = stringResource(R.string.home_thermal_card),
                score = healthScore.thermalScore,
            )
            HealthSubscore(
                label = stringResource(R.string.home_storage_card),
                score = healthScore.storageScore,
            )
        }
    }
}

@Composable
private fun RowScope.HealthSubscore(
    label: String,
    score: Int,
) {
    Column(
        modifier =
            Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.home_subscore_value, score.coerceIn(0, 100)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun HealthMeasurementRow(
    statusLabel: String,
    status: HealthStatus,
    measurementTimestampMillis: Long,
    measurementConfidence: Confidence,
    stackContent: Boolean,
) {
    val formattedMeasurementTime =
        rememberFormattedDateTime(measurementTimestampMillis, "MMMdhm")
    val measuredAt =
        stringResource(
            R.string.home_health_measured_at,
            formattedMeasurementTime,
        )
    if (stackContent) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            StatusPill(
                label = statusLabel,
                tone = status.toStatusTone(),
            )
            Text(
                text = measuredAt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.home_health_confidence_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ConfidenceBadge(confidence = measurementConfidence)
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(
                label = statusLabel,
                tone = status.toStatusTone(),
            )
            Text(
                text = measuredAt,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ConfidenceBadge(confidence = measurementConfidence)
        }
    }
}

private fun HealthStatus.toStatusTone(): StatusTone =
    when (this) {
        HealthStatus.HEALTHY -> StatusTone.HEALTHY
        HealthStatus.FAIR -> StatusTone.FAIR
        HealthStatus.POOR -> StatusTone.POOR
        HealthStatus.CRITICAL -> StatusTone.CRITICAL
    }
