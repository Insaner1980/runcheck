package com.runcheck.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
import com.runcheck.ui.components.InfoBanner
import com.runcheck.ui.components.PrimaryTopBar
import com.runcheck.ui.components.RuncheckLoadingIndicator
import com.runcheck.ui.components.RuncheckWavyProgress
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
import com.runcheck.ui.theme.numericHeroDisplayTextStyle
import com.runcheck.ui.theme.numericHeroDisplayUnitTextStyle
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.runcheckHeroCardColors
import com.runcheck.ui.theme.spacing

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
        PrimaryTopBar(title = stringResource(R.string.app_name))

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    contentAlignment = Alignment.Center,
                ) {
                    RuncheckLoadingIndicator(contentDescription = loadingDescription)
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
                        onPurchase = onNavigateToProUpgrade,
                        onDismiss = viewModel::dismissExpirationModal,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
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
    val isWideScreen = LocalConfiguration.current.screenWidthDp >= 600
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

    ContentContainer {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

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
            }

            HomeHealthHero(
                healthScore = state.healthScore,
                measurementTimestampMillis = state.measurementTimestampMillis,
                measurementConfidence = state.batteryState.currentMa.confidence,
            )

            HomeGridSection(
                state = state,
                isWideScreen = isWideScreen,
                onNavigateToBattery = onNavigateToBattery,
                onNavigateToNetwork = onNavigateToNetwork,
                onNavigateToThermal = onNavigateToThermal,
                onNavigateToStorage = onNavigateToStorage,
            )

            InsightsCard(
                state =
                    InsightsCardState(
                        insights = state.insights.take(1),
                        unseenInsightCount = state.unseenInsightCount,
                        isPro = state.isPro,
                    ),
                navigationHandlers = insightNavigationHandlers,
                onNavigateToInsights = onNavigateToInsights,
                onDismissInsight = onDismissInsight,
            )

            if (state.proState.status == ProStatus.TRIAL_ACTIVE) {
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
private fun HomeHealthHero(
    healthScore: HealthScore,
    measurementTimestampMillis: Long,
    measurementConfidence: Confidence,
    modifier: Modifier = Modifier,
) {
    val score = healthScore.overallScore.coerceIn(0, 100)
    val status = HealthScore.statusFromScore(score)
    val statusLabel = scoreLabel(score)
    val scoreDescription = stringResource(R.string.a11y_health_score, score)

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
                    .padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
        ) {
            SectionHeader(text = stringResource(R.string.home_health_score))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 280.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
                    ) {
                        HealthScoreRing(
                            score = score,
                            scoreDescription = scoreDescription,
                        )
                        HealthHeroSummary(
                            statusLabel = statusLabel,
                            status = status,
                            measurementTimestampMillis = measurementTimestampMillis,
                            measurementConfidence = measurementConfidence,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HealthScoreRing(
                            score = score,
                            scoreDescription = scoreDescription,
                        )
                        HealthHeroSummary(
                            statusLabel = statusLabel,
                            status = status,
                            measurementTimestampMillis = measurementTimestampMillis,
                            measurementConfidence = measurementConfidence,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthScoreRing(
    score: Int,
    scoreDescription: String,
) {
    RuncheckWavyProgress(
        progress = score / 100f,
        contentDescription = scoreDescription,
        modifier = Modifier.size(148.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = score.toString(),
                style = MaterialTheme.numericHeroDisplayTextStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.unit_per_hundred),
                style = MaterialTheme.numericHeroDisplayUnitTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.sm),
            )
        }
    }
}

@Composable
private fun HealthHeroSummary(
    statusLabel: String,
    status: HealthStatus,
    measurementTimestampMillis: Long,
    measurementConfidence: Confidence,
    modifier: Modifier = Modifier,
) {
    val formattedMeasurementTime =
        rememberFormattedDateTime(measurementTimestampMillis, "MMMdhm")
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        StatusPill(
            label = statusLabel,
            tone = status.toStatusTone(),
        )
        Text(
            text =
                stringResource(
                    R.string.home_health_measured_at,
                    formattedMeasurementTime,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_health_confidence_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
