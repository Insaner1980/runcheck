package com.runcheck.ui.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.HealthStatus
import com.runcheck.pro.ProStatus
import com.runcheck.ui.common.batteryHealthLabel
import com.runcheck.ui.common.chargingStatusLabel
import com.runcheck.ui.common.connectionDisplayLabel
import com.runcheck.ui.common.formatPercent
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.common.formatTemperature
import com.runcheck.ui.common.plugTypeLabel
import com.runcheck.ui.common.scoreLabel
import com.runcheck.ui.common.signalQualityLabel
import com.runcheck.ui.common.temperatureBandLabel
import com.runcheck.ui.components.ContentContainer
import com.runcheck.ui.components.GridCard
import com.runcheck.ui.components.IconCircle
import com.runcheck.ui.components.ListRow
import com.runcheck.ui.components.MetricPill
import com.runcheck.ui.components.PrimaryTopBar
import com.runcheck.ui.components.ProBadgePill
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.components.StatusDot
import com.runcheck.ui.home.insights.InsightNavigationHandlers
import com.runcheck.ui.home.insights.InsightsCard
import com.runcheck.ui.learn.LearnArticleIds
import com.runcheck.ui.pro.PostExpirationUpgradeCard
import com.runcheck.ui.pro.TrialHomeCard
import com.runcheck.ui.pro.TrialWelcomeSheet
import com.runcheck.ui.theme.MotionTokens
import com.runcheck.ui.theme.heroCardColor
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.numericHeroDisplayTextStyle
import com.runcheck.ui.theme.numericHeroDisplayUnitTextStyle
import com.runcheck.ui.theme.numericHeroLargeValueTextStyle
import com.runcheck.ui.theme.reducedMotion
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.runcheckHeroCardColors
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColorForPercent
import com.runcheck.ui.theme.statusColorForSignalQuality
import com.runcheck.ui.theme.statusColorForStoragePercent
import com.runcheck.ui.theme.statusColorForTemperature
import com.runcheck.ui.theme.statusColors
import com.runcheck.util.ReleaseSafeLog
import kotlin.math.sin

private const val TAG = "HomeScreen"

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
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loadingDescription = stringResource(R.string.a11y_loading)

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> viewModel.startObserving()
                    Lifecycle.Event.ON_STOP -> viewModel.stopObserving()
                    else -> Unit
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.startObserving()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopObserving()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTopBar(
            title = stringResource(R.string.app_name),
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = MaterialTheme.colorScheme.onSurface,
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
                            .semantics {
                                contentDescription = loadingDescription
                                liveRegion = LiveRegionMode.Polite
                            },
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
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
                            text = stringResource(R.string.common_error_generic),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }

            is HomeUiState.Success -> {
                if (state.showWelcomeSheet) {
                    TrialWelcomeSheet(
                        onDismiss = { viewModel.dismissWelcomeSheet() },
                    )
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
                        onNavigateToSpeedTest = onNavigateToSpeedTest,
                        onNavigateToAppUsage = onNavigateToAppUsage,
                        onNavigateToInsights = onNavigateToInsights,
                        onDismissInsight = { viewModel.dismissInsight(it) },
                        onNavigateToProUpgrade = onNavigateToProUpgrade,
                        onNavigateToLearn = onNavigateToLearn,
                        onNavigateToLearnArticle = onNavigateToLearnArticle,
                        onDismissUpgradeCard = { viewModel.dismissUpgradeCard() },
                    )

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }

                if (state.showExpirationModal) {
                    com.runcheck.ui.pro.TrialExpirationModal(
                        formattedPrice = null,
                        onPurchase = onNavigateToProUpgrade,
                        onDismiss = { viewModel.dismissExpirationModal() },
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
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onDismissInsight: (Long) -> Unit,
    onNavigateToProUpgrade: () -> Unit,
    onNavigateToLearn: () -> Unit = {},
    onNavigateToLearnArticle: (String) -> Unit = {},
    onDismissUpgradeCard: () -> Unit = {},
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
                    .padding(horizontal = MaterialTheme.spacing.base)
                    .navigationBarsPadding(),
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            if (state.monitoringStale) {
                MonitoringStaleWarning(
                    onLearnWhy = { onNavigateToLearnArticle(LearnArticleIds.BACKGROUND_MONITORING) },
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
            }

            HomeStatusSummary(state = state)

            HomeTrialSection(
                state = state,
                onNavigateToProUpgrade = onNavigateToProUpgrade,
                onDismissUpgradeCard = onDismissUpgradeCard,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            HealthScoreCard(
                healthScore = state.healthScore,
                batteryTempC = state.thermalState.batteryTempC,
                onNavigateToBattery = onNavigateToBattery,
                onNavigateToThermal = onNavigateToThermal,
                onNavigateToNetwork = onNavigateToNetwork,
                onNavigateToStorage = onNavigateToStorage,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            BatteryHeroCard(
                battery = state.batteryState,
                onClick = onNavigateToBattery,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            HomeGridSection(
                state = state,
                isWideScreen = isWideScreen,
                onNavigateToNetwork = onNavigateToNetwork,
                onNavigateToThermal = onNavigateToThermal,
                onNavigateToCharger = onNavigateToCharger,
                onNavigateToStorage = onNavigateToStorage,
                onNavigateToProUpgrade = onNavigateToProUpgrade,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            InsightsCard(
                insights = state.insights,
                totalInsightCount = state.totalInsightCount,
                unseenInsightCount = state.unseenInsightCount,
                isPro = state.isPro,
                navigationHandlers = insightNavigationHandlers,
                onNavigateToInsights = onNavigateToInsights,
                onDismissInsight = onDismissInsight,
            )

            if (state.insights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))
            }

            HomeQuickToolsSection(
                isPro = state.isPro,
                onNavigateToSpeedTest = onNavigateToSpeedTest,
                onNavigateToAppUsage = onNavigateToAppUsage,
                onNavigateToProUpgrade = onNavigateToProUpgrade,
                onNavigateToLearn = onNavigateToLearn,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            HomeProStatusSection(
                proStatus = state.proState.status,
                onNavigateToProUpgrade = onNavigateToProUpgrade,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

@Composable
private fun HomeTrialSection(
    state: HomeUiState.Success,
    onNavigateToProUpgrade: () -> Unit,
    onDismissUpgradeCard: () -> Unit,
) {
    if (state.proState.status == ProStatus.TRIAL_ACTIVE) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
        TrialHomeCard(
            proState = state.proState,
            onNavigateToProUpgrade = onNavigateToProUpgrade,
        )
    } else if (state.showUpgradeCard) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
        PostExpirationUpgradeCard(
            formattedPrice = null,
            onNavigateToProUpgrade = onNavigateToProUpgrade,
            onDismiss = onDismissUpgradeCard,
        )
    }
}

@Composable
private fun HomeStatusSummary(state: HomeUiState.Success) {
    val batteryStatus = chargingStatusLabel(state.batteryState.chargingStatus)
    val networkStatus =
        connectionDisplayLabel(
            connectionType = state.networkState.connectionType,
            wifiSsid = state.networkState.wifiSsid,
            networkSubtype = state.networkState.networkSubtype,
        )
    val temperatureBand = temperatureBandLabel(state.thermalState.batteryTempC)

    Text(
        text =
            stringResource(
                R.string.home_status_summary,
                batteryStatus,
                temperatureBand,
                networkStatus,
            ),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MonitoringStaleWarning(onLearnWhy: () -> Unit) {
    val context = LocalContext.current
    Card(
        onClick = {
            val intent =
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            try {
                context.startActivity(intent)
            } catch (_: android.content.ActivityNotFoundException) {
                // Fallback to general battery optimization settings
                try {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
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
private fun HealthScoreCard(
    healthScore: HealthScore,
    batteryTempC: Float,
    onNavigateToBattery: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToStorage: () -> Unit,
) {
    val score = healthScore.overallScore
    val statusLabel = scoreLabel(score)
    val formattedSummary = stringResource(R.string.home_device_good_shape, statusLabel.lowercase())
    val statusWord = statusLabel.lowercase()
    val scoreColor = statusColorForPercent(score)
    val annotatedSummary =
        remember(formattedSummary, statusWord, scoreColor) {
            buildAnnotatedString {
                val startIndex = formattedSummary.indexOf(statusWord)
                if (startIndex >= 0) {
                    append(formattedSummary.substring(0, startIndex))
                    withStyle(
                        SpanStyle(
                            color = scoreColor,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) {
                        append(statusWord)
                    }
                    append(formattedSummary.substring(startIndex + statusWord.length))
                } else {
                    append(formattedSummary)
                }
            }
        }

    val batteryLabel = stringResource(R.string.home_battery_card)
    val thermalLabel = stringResource(R.string.home_thermal_card)
    val networkLabel = stringResource(R.string.home_network_card)
    val storageLabel = stringResource(R.string.home_storage_card)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = runcheckHeroCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                SectionHeader(stringResource(R.string.home_health_score))
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))
            val healthScoreDescription = stringResource(R.string.a11y_health_score, score)

            // Large typographic score
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier =
                    Modifier.semantics(mergeDescendants = true) {
                        contentDescription = healthScoreDescription
                        liveRegion = LiveRegionMode.Polite
                    },
            ) {
                Text(
                    text = score.toString(),
                    style = MaterialTheme.numericHeroDisplayTextStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.unit_per_hundred),
                    style = MaterialTheme.numericHeroDisplayUnitTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Text(
                text = annotatedSummary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (batteryTempC >= 38f) {
                Text(
                    text = stringResource(R.string.home_temp_elevated),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.statusColors.poor,
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            // Category health overview bar
            HealthCategoryBar(
                batteryScore = healthScore.batteryScore,
                thermalScore = healthScore.thermalScore,
                networkScore = healthScore.networkScore,
                storageScore = healthScore.storageScore,
                batteryLabel = batteryLabel,
                thermalLabel = thermalLabel,
                networkLabel = networkLabel,
                storageLabel = storageLabel,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            HealthBreakdownRow(
                label = batteryLabel,
                value = formatPercent(healthScore.batteryScore),
                status = HealthScore.statusFromScore(healthScore.batteryScore),
                onClick = onNavigateToBattery,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            HealthBreakdownRow(
                label = thermalLabel,
                value = formatPercent(healthScore.thermalScore),
                status = HealthScore.statusFromScore(healthScore.thermalScore),
                onClick = onNavigateToThermal,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            HealthBreakdownRow(
                label = networkLabel,
                value = formatPercent(healthScore.networkScore),
                status = HealthScore.statusFromScore(healthScore.networkScore),
                onClick = onNavigateToNetwork,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            HealthBreakdownRow(
                label = storageLabel,
                value = formatPercent(healthScore.storageScore),
                status = HealthScore.statusFromScore(healthScore.storageScore),
                onClick = onNavigateToStorage,
            )
        }
    }
}

@Composable
private fun BatteryHeroCard(
    battery: com.runcheck.domain.model.BatteryState,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            SectionHeader(stringResource(R.string.home_battery_card))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = battery.level.toString(),
                            style = MaterialTheme.numericHeroLargeValueTextStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.unit_percent),
                            style =
                                MaterialTheme.typography.headlineLarge.copy(
                                    fontFamily = MaterialTheme.numericFontFamily,
                                ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp, bottom = 12.dp),
                        )
                    }
                    Text(
                        text = chargingStatusLabel(battery.chargingStatus),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HomeBatteryChargeIcon(
                    level = battery.level,
                    isCharging = battery.chargingStatus == com.runcheck.domain.model.ChargingStatus.CHARGING,
                    progress = battery.level / 100f,
                    modifier = Modifier.size(130.dp),
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                MetricPill(
                    label = stringResource(R.string.battery_health),
                    value = batteryHealthLabel(battery.health),
                    valueColor =
                        if (battery.health == com.runcheck.domain.model.BatteryHealth.GOOD) {
                            MaterialTheme.statusColors.healthy
                        } else {
                            MaterialTheme.statusColors.fair
                        },
                    modifier = Modifier.weight(1f),
                )
                MetricPill(
                    label = stringResource(R.string.battery_plug_type),
                    value = plugTypeLabel(battery.plugType),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HealthBreakdownRow(
    label: String,
    value: String,
    status: HealthStatus,
    onClick: () -> Unit,
) {
    val statusText = healthStatusLabel(status)
    val clickLabel = label
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable(onClick = onClick, onClickLabel = clickLabel)
                .semantics(mergeDescendants = true) {
                    stateDescription = statusText
                }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(
            color = statusColor(status),
            modifier = Modifier.padding(end = MaterialTheme.spacing.sm),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontFamily = MaterialTheme.numericFontFamily,
                ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun healthStatusLabel(status: HealthStatus): String =
    when (status) {
        HealthStatus.HEALTHY -> stringResource(R.string.status_healthy)
        HealthStatus.FAIR -> stringResource(R.string.status_fair)
        HealthStatus.POOR -> stringResource(R.string.status_poor)
        HealthStatus.CRITICAL -> stringResource(R.string.status_critical)
    }

@Composable
private fun HomeBatteryChargeIcon(
    level: Int,
    isCharging: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val fillLevel = progress.coerceIn(0f, 1f)
    val reducedMotion = MaterialTheme.reducedMotion

    val wavePhase =
        if (isCharging && !reducedMotion && fillLevel in 0.01f..0.99f) {
            val infiniteTransition = rememberInfiniteTransition(label = "batteryWave")
            val phase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 2f * Math.PI.toFloat(),
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = MotionTokens.CONTINUOUS, easing = LinearEasing),
                    ),
                label = "wavePhase",
            )
            phase
        } else {
            0f
        }

    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fillColor = statusColorForPercent(level)
    val textColor =
        if (fillLevel > 0.55f) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    val clipPath = remember { Path() }
    val wavePath = remember { Path() }

    Box(
        modifier = modifier.clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier.size(width = 80.dp, height = 124.dp),
        ) {
            val capWidth = 18.dp.toPx()
            val capHeight = 5.dp.toPx()
            val capRadius = 2.dp.toPx()
            val bodyTop = capHeight + 1.dp.toPx()
            val bodyHeight = size.height - bodyTop
            val strokeW = 2.dp.toPx()
            val cornerRadius = 12.dp.toPx()
            val inset = 4.dp.toPx()
            val waveAmplitude = 3.dp.toPx()

            // Terminal cap
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset((size.width - capWidth) / 2f, 0f),
                size = Size(capWidth, capHeight),
                cornerRadius = CornerRadius(capRadius, capRadius),
            )

            // Battery body outline
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(0f, bodyTop),
                size = Size(size.width, bodyHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = strokeW),
            )

            // Fill area
            if (fillLevel > 0f) {
                val fillAreaTop = bodyTop + strokeW + inset
                val fillAreaBottom = bodyTop + bodyHeight - strokeW - inset
                val fillAreaLeft = strokeW + inset
                val fillAreaRight = size.width - strokeW - inset
                val fillAreaHeight = fillAreaBottom - fillAreaTop
                val fillTop = fillAreaBottom - fillAreaHeight * fillLevel
                val fillCorner = 8.dp.toPx()

                clipPath.reset()
                clipPath.addRoundRect(
                    RoundRect(
                        left = fillAreaLeft,
                        top = fillAreaTop,
                        right = fillAreaRight,
                        bottom = fillAreaBottom,
                        cornerRadius = CornerRadius(fillCorner, fillCorner),
                    ),
                )
                clipPath(clipPath) {
                    val showWave = isCharging && !reducedMotion && fillLevel in 0.01f..0.99f

                    if (showWave) {
                        wavePath.reset()
                        wavePath.moveTo(fillAreaLeft, fillTop)
                        val waveWidth = fillAreaRight - fillAreaLeft
                        val steps = 40
                        for (i in 0..steps) {
                            val x = fillAreaLeft + waveWidth * i / steps
                            val y =
                                fillTop + waveAmplitude *
                                    sin(
                                        wavePhase + 2f * Math.PI.toFloat() * i / steps,
                                    )
                            wavePath.lineTo(x, y)
                        }
                        wavePath.lineTo(fillAreaRight, fillAreaBottom)
                        wavePath.lineTo(fillAreaLeft, fillAreaBottom)
                        wavePath.close()
                        drawPath(
                            path = wavePath,
                            brush =
                                Brush.verticalGradient(
                                    colors = listOf(fillColor, fillColor.copy(alpha = 0.6f)),
                                    startY = fillTop,
                                    endY = fillAreaBottom,
                                ),
                        )
                    } else {
                        drawRect(
                            brush =
                                Brush.verticalGradient(
                                    colors = listOf(fillColor, fillColor.copy(alpha = 0.6f)),
                                    startY = fillTop,
                                    endY = fillAreaBottom,
                                ),
                            topLeft = Offset(fillAreaLeft, fillTop),
                            size = Size(fillAreaRight - fillAreaLeft, fillAreaBottom - fillTop),
                        )
                    }
                }
            }
        }

        // Overlaid level text (participates in semantics tree)
        Text(
            text = level.toString(),
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontFamily = MaterialTheme.numericFontFamily,
                    fontWeight = FontWeight.Bold,
                ),
            color = textColor,
        )
    }
}

@Composable
private fun statusColor(status: HealthStatus): Color {
    val colors = MaterialTheme.statusColors
    return when (status) {
        HealthStatus.HEALTHY -> colors.healthy
        HealthStatus.FAIR -> colors.fair
        HealthStatus.POOR -> colors.poor
        HealthStatus.CRITICAL -> colors.critical
    }
}

@Composable
private fun HealthCategoryBar(
    batteryScore: Int,
    thermalScore: Int,
    networkScore: Int,
    storageScore: Int,
    batteryLabel: String,
    thermalLabel: String,
    networkLabel: String,
    storageLabel: String,
    modifier: Modifier = Modifier,
) {
    val statusColors = MaterialTheme.statusColors
    val scores =
        listOf(
            batteryLabel to statusColorFromScore(batteryScore, statusColors),
            thermalLabel to statusColorFromScore(thermalScore, statusColors),
            networkLabel to statusColorFromScore(networkScore, statusColors),
            storageLabel to statusColorFromScore(storageScore, statusColors),
        )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            scores.forEach { (_, color) ->
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(
                                color = color,
                                shape = RoundedCornerShape(4.dp),
                            ),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            scores.forEach { (label, color) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
            }
        }
    }
}

private fun statusColorFromScore(
    score: Int,
    colors: com.runcheck.ui.theme.StatusColors,
): Color =
    when {
        score >= 75 -> colors.healthy
        score >= 50 -> colors.fair
        score >= 25 -> colors.poor
        else -> colors.critical
    }
