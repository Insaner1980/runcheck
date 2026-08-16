package com.runcheck.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
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
import com.runcheck.ui.components.IconCircle
import com.runcheck.ui.components.ListRow
import com.runcheck.ui.components.ObservedScreenScaffold
import com.runcheck.ui.components.PrimaryTopBar
import com.runcheck.ui.components.ProBadgePill
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.components.observedScreenState
import com.runcheck.ui.home.insights.InsightNavigationHandlers
import com.runcheck.ui.home.insights.InsightsCard
import com.runcheck.ui.home.insights.InsightsCardState
import com.runcheck.ui.learn.LearnArticleIds
import com.runcheck.ui.theme.homeHealthContextTextStyle
import com.runcheck.ui.theme.homeHealthScoreTextStyle
import com.runcheck.ui.theme.homeHealthScoreUnitTextStyle
import com.runcheck.ui.theme.homeHealthStatusTextStyle
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColor
import com.runcheck.ui.theme.statusColors
import com.runcheck.ui.theme.uiTokens
import com.runcheck.util.ReleaseSafeLog
import kotlinx.coroutines.delay

private const val TAG = "HomeScreen"
private const val MINUTE_MILLIS = 60_000L

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
    viewModel: HomeViewModel = hiltViewModel(),
) {
    // CPD-OFF: Keep StateFlow collection at the screen boundary for Compose stability.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val screenState = observedScreenState(uiState, isRefreshing)

    ObservedScreenScaffold(
        onStart = viewModel::startObserving,
        onStop = viewModel::stopObserving,
        modifier = modifier,
        topBar = {
            PrimaryTopBar(
                title = stringResource(R.string.app_name),
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

    ContentContainer {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.base)
                    .navigationBarsPadding(),
        ) {
            val spacing = MaterialTheme.spacing

            Spacer(modifier = Modifier.height(43.dp))

            HealthScoreHero(
                healthScore = state.healthScore,
                lastUpdatedAtEpochMillis = state.lastUpdatedAtEpochMillis,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.xs),
            )

            Spacer(modifier = Modifier.height(28.dp))

            HomeFullCheckButton(
                isRefreshing = isRefreshing,
                onClick = onRefresh,
                modifier = Modifier.padding(horizontal = spacing.xs),
            )

            Spacer(modifier = Modifier.height(44.dp))

            if (state.monitoringStale) {
                MonitoringStaleWarning(
                    onLearnWhy = {
                        navigation.onNavigateToLearnArticle(LearnArticleIds.BACKGROUND_MONITORING)
                    },
                )
                Spacer(modifier = Modifier.height(spacing.md))
            }

            HomeStatusTiles(
                state = state,
                onNavigateToBattery = navigation.onNavigateToBattery,
                onNavigateToNetwork = navigation.onNavigateToNetwork,
                onNavigateToThermal = navigation.onNavigateToThermal,
                onNavigateToStorage = navigation.onNavigateToStorage,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.xs),
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            InsightsCard(
                state = insightsCardState,
                navigationHandlers = insightNavigationHandlers,
                onNavigateToInsights = navigation.onNavigateToInsights,
                onDismissInsight = onDismissInsight,
            )

            if (state.insights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))
            }

            HomeQuickToolsSection(
                isPro = state.isPro,
                onNavigateToSpeedTest = navigation.onNavigateToSpeedTest,
                onNavigateToAppUsage = navigation.onNavigateToAppUsage,
                onNavigateToProUpgrade = navigation.onNavigateToProUpgrade,
                onNavigateToLearn = navigation.onNavigateToLearn,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            HomeProStatusSection(
                visible = state.isPro,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
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
        shape = CircleShape,
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
    val scoreColor = statusColor(healthScore.status)
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
    val scoreBrush =
        Brush.verticalGradient(
            colors =
                listOf(
                    lerp(scoreColor, MaterialTheme.colorScheme.onSurface, 0.28f),
                    scoreColor,
                ),
        )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier =
                Modifier
                    .semantics(mergeDescendants = true) {
                        contentDescription = healthScoreDescription
                        liveRegion = LiveRegionMode.Polite
                    },
            horizontalArrangement = Arrangement.spacedBy(spacing.sm + spacing.xxs),
        ) {
            Text(
                text = score.toString(),
                style = MaterialTheme.homeHealthScoreTextStyle.copy(brush = scoreBrush),
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = stringResource(R.string.unit_per_hundred),
                style = MaterialTheme.homeHealthScoreUnitTextStyle,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.alignByBaseline(),
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = healthStatusLabel(healthScore.status),
            style = MaterialTheme.homeHealthStatusTextStyle,
            color = statusColor(healthScore.status),
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        Text(
            text =
                pluralStringResource(
                    R.plurals.home_health_context,
                    minutesSinceUpdate,
                    minutesSinceUpdate,
                ),
            style = MaterialTheme.homeHealthContextTextStyle,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

internal fun elapsedWholeMinutes(
    lastUpdatedAtEpochMillis: Long,
    currentEpochMillis: Long,
): Int =
    ((currentEpochMillis - lastUpdatedAtEpochMillis).coerceAtLeast(0L) / MINUTE_MILLIS)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()
