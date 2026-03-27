package com.runcheck.ui.battery

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.runcheck.R
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.ui.common.batteryHealthLabel
import com.runcheck.ui.common.chargingStatusLabel
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.common.formatTemperature
import com.runcheck.ui.common.plugTypeLabel
import com.runcheck.ui.common.temperatureBandLabel
import com.runcheck.ui.components.AreaChart
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.ConfidenceBadge
import com.runcheck.ui.components.LiveChart
import com.runcheck.ui.components.MetricPill
import com.runcheck.ui.components.MetricRow
import com.runcheck.ui.theme.statusColorForPercent
import com.runcheck.ui.theme.statusColorForTemperature
import com.runcheck.ui.components.ProBadgePill
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.components.ContentContainer
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.ProgressRing
import com.runcheck.ui.components.PullToRefreshWrapper
import com.runcheck.ui.components.ExpandableChartContainer
import com.runcheck.ui.components.TrendChart
import com.runcheck.ui.chart.BatteryHistoryMetric
import com.runcheck.ui.chart.SessionGraphMetric
import com.runcheck.ui.chart.SessionGraphWindow
import com.runcheck.ui.chart.ChargingSessionSummary
import com.runcheck.ui.chart.MAX_HISTORY_CHART_POINTS
import com.runcheck.ui.chart.MAX_SESSION_CHART_POINTS
import com.runcheck.ui.chart.FullscreenChartSource
import com.runcheck.ui.chart.batteryQualityZones
import com.runcheck.ui.chart.buildBatteryHistoryChartModel
import com.runcheck.ui.chart.buildBatterySessionChartModel
import com.runcheck.ui.chart.rememberChartAccessibilitySummary
import com.runcheck.ui.components.info.InfoBottomSheet
import com.runcheck.ui.components.info.InfoCard
import com.runcheck.ui.components.info.InfoCardCatalog
import com.runcheck.ui.learn.LearnArticleIds
import com.runcheck.ui.learn.RelatedArticlesSection
import com.runcheck.ui.chart.calculateChargingSessionSummary
import com.runcheck.ui.chart.formatChartTooltip
import com.runcheck.ui.chart.hasGraphData
import com.runcheck.ui.chart.historyMetricLabel
import com.runcheck.ui.chart.historyPeriodLabel
import com.runcheck.ui.chart.sessionGraphMetricLabel
import com.runcheck.ui.chart.sessionGraphWindowLabel
import com.runcheck.domain.model.ScreenUsageStats
import com.runcheck.domain.model.SleepAnalysis
import com.runcheck.domain.usecase.BatteryStatistics
import com.runcheck.ui.fullscreen.FullscreenChartSeedStore
import com.runcheck.ui.fullscreen.FullscreenChartUiState
import com.runcheck.ui.fullscreen.parseFullscreenChartSource
import com.runcheck.ui.fullscreen.sanitizeFullscreenMetric
import com.runcheck.ui.fullscreen.sanitizeFullscreenPeriod
import com.runcheck.ui.theme.heroCardColor
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.numericHeroDisplayTextStyle
import com.runcheck.ui.theme.numericHeroDisplayUnitTextStyle
import com.runcheck.ui.theme.numericHeroLevelTextStyle
import com.runcheck.ui.theme.numericHeroUnitTextStyle
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors

@Composable
fun BatteryDetailScreen(
    onBack: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToFullscreen: (source: String, metric: String, period: String) -> Unit = { _, _, _ -> },
    onNavigateToLearnArticle: (articleId: String) -> Unit = {},
    fullscreenResultSource: String? = null,
    fullscreenResultMetric: String? = null,
    fullscreenResultPeriod: String? = null,
    onFullscreenResultConsumed: () -> Unit = {},
    viewModel: BatteryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
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
        DetailTopBar(
            title = "",
            onBack = onBack
        )
        ContentContainer {
        when (val state = uiState) {
            is BatteryUiState.Loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = context.getString(R.string.a11y_loading); liveRegion = LiveRegionMode.Polite },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is BatteryUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    ) {
                        Text(stringResource(R.string.common_error_generic))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }
            is BatteryUiState.Success -> {
                BatteryContent(
                    state = state,
                    onRefresh = { viewModel.refresh() },
                    onPeriodChange = { viewModel.setHistoryPeriod(it) },
                    onNavigateToCharger = onNavigateToCharger,
                    onUpgradeToPro = onUpgradeToPro,
                    onNavigateToFullscreen = onNavigateToFullscreen,
                    onNavigateToLearnArticle = onNavigateToLearnArticle,
                    onDismissInfoCard = { viewModel.dismissInfoCard(it) },
                    fullscreenResultSource = fullscreenResultSource,
                    fullscreenResultMetric = fullscreenResultMetric,
                    fullscreenResultPeriod = fullscreenResultPeriod,
                    onFullscreenResultConsumed = onFullscreenResultConsumed
                )
            }
        }
        }
    }
}

@Composable
private fun BatteryContent(
    state: BatteryUiState.Success,
    onRefresh: () -> Unit,
    onPeriodChange: (HistoryPeriod) -> Unit,
    onNavigateToCharger: () -> Unit,
    onUpgradeToPro: () -> Unit,
    onNavigateToFullscreen: (source: String, metric: String, period: String) -> Unit,
    onNavigateToLearnArticle: (articleId: String) -> Unit,
    onDismissInfoCard: (String) -> Unit,
    fullscreenResultSource: String? = null,
    fullscreenResultMetric: String? = null,
    fullscreenResultPeriod: String? = null,
    onFullscreenResultConsumed: () -> Unit = {}
) {
    var activeInfoSheet by rememberSaveable { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val battery = state.batteryState
    var selectedHistoryMetric by rememberSaveable { mutableStateOf(BatteryHistoryMetric.LEVEL.name) }
    var selectedSessionMetric by rememberSaveable { mutableStateOf(SessionGraphMetric.CURRENT.name) }
    var selectedSessionWindow by rememberSaveable { mutableStateOf(SessionGraphWindow.ALL.name) }
    val historyMetric = BatteryHistoryMetric.entries.firstOrNull { it.name == selectedHistoryMetric }
        ?: BatteryHistoryMetric.LEVEL
    val sessionMetric = SessionGraphMetric.entries.firstOrNull { it.name == selectedSessionMetric }
        ?: SessionGraphMetric.CURRENT
    val sessionWindow = SessionGraphWindow.entries.firstOrNull { it.name == selectedSessionWindow }
        ?: SessionGraphWindow.ALL

    // Apply fullscreen chart selection results when navigating back
    val currentOnPeriodChange by rememberUpdatedState(onPeriodChange)
    val currentOnFullscreenResultConsumed by rememberUpdatedState(onFullscreenResultConsumed)
    LaunchedEffect(fullscreenResultSource, fullscreenResultMetric, fullscreenResultPeriod) {
        if (fullscreenResultSource != null && fullscreenResultMetric != null && fullscreenResultPeriod != null) {
            when (parseFullscreenChartSource(fullscreenResultSource)) {
                FullscreenChartSource.BATTERY_HISTORY -> {
                    selectedHistoryMetric = sanitizeFullscreenMetric(
                        source = FullscreenChartSource.BATTERY_HISTORY,
                        rawMetric = fullscreenResultMetric
                    )
                    val period = runCatching {
                        HistoryPeriod.valueOf(
                            sanitizeFullscreenPeriod(
                                source = FullscreenChartSource.BATTERY_HISTORY,
                                rawPeriod = fullscreenResultPeriod
                            )
                        )
                    }.getOrNull()
                    if (period != null) currentOnPeriodChange(period)
                }
                FullscreenChartSource.BATTERY_SESSION -> {
                    selectedSessionMetric = sanitizeFullscreenMetric(
                        source = FullscreenChartSource.BATTERY_SESSION,
                        rawMetric = fullscreenResultMetric
                    )
                    selectedSessionWindow = sanitizeFullscreenPeriod(
                        source = FullscreenChartSource.BATTERY_SESSION,
                        rawPeriod = fullscreenResultPeriod
                    )
                }
                else -> Unit
            }
            currentOnFullscreenResultConsumed()
        }
    }
    val chargingSessionSummary = remember(state.history, battery.level, battery.chargingStatus) {
        calculateChargingSessionSummary(
            history = state.history,
            currentLevel = battery.level,
            chargingStatus = battery.chargingStatus
        )
    }

    LaunchedEffect(state) {
        isRefreshing = false
    }

    PullToRefreshWrapper(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefresh()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            BatteryHeroSection(
                battery = battery,
                history = state.history,
                onInfoClick = { activeInfoSheet = it }
            )

            // Live notification info card - show once to inform about the feature
            InfoCard(
                id = InfoCardCatalog.BatteryLiveNotification.id,
                headline = stringResource(InfoCardCatalog.BatteryLiveNotification.headlineRes),
                body = stringResource(InfoCardCatalog.BatteryLiveNotification.bodyRes),
                onDismiss = onDismissInfoCard,
                visible = InfoCardCatalog.BatteryLiveNotification.id !in state.dismissedInfoCards && state.showInfoCards
            )

            // Health degradation card - show when healthPercent < 90% and not dismissed
            if (battery.healthPercent != null && battery.healthPercent < 90) {
                InfoCard(
                    id = InfoCardCatalog.BatteryHealthDegraded.id,
                    headline = stringResource(InfoCardCatalog.BatteryHealthDegraded.headlineRes),
                    body = stringResource(InfoCardCatalog.BatteryHealthDegraded.bodyRes),
                    onDismiss = onDismissInfoCard,
                    visible = InfoCardCatalog.BatteryHealthDegraded.id !in state.dismissedInfoCards && state.showInfoCards,
                    onLearnMore = {
                        InfoCardCatalog.resolveLearnArticleId(
                            InfoCardCatalog.BatteryHealthDegraded
                        )?.let(onNavigateToLearnArticle)
                    }
                )
            }

            // Dies before zero card - show when healthPercent < 80%
            if (battery.healthPercent != null && battery.healthPercent < 80) {
                InfoCard(
                    id = InfoCardCatalog.BatteryDiesBeforeZero.id,
                    headline = stringResource(InfoCardCatalog.BatteryDiesBeforeZero.headlineRes),
                    body = stringResource(InfoCardCatalog.BatteryDiesBeforeZero.bodyRes),
                    onDismiss = onDismissInfoCard,
                    visible = InfoCardCatalog.BatteryDiesBeforeZero.id !in state.dismissedInfoCards && state.showInfoCards,
                    onLearnMore = {
                        InfoCardCatalog.resolveLearnArticleId(
                            InfoCardCatalog.BatteryDiesBeforeZero
                        )?.let(onNavigateToLearnArticle)
                    }
                )
            }

            BatteryPanel {
                CardSectionTitle(text = stringResource(R.string.battery_section_details))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                MetricRow(
                    label = stringResource(R.string.battery_voltage),
                    value = stringResource(
                        R.string.value_voltage_volts,
                        (battery.voltageMv / 1000f).toDouble()
                    ),
                    onInfoClick = { activeInfoSheet = "voltage" }
                )
                MetricRow(
                    label = stringResource(R.string.battery_temperature),
                    value = buildTemperatureValue(
                        temperatureC = battery.temperatureC,
                        temperatureUnit = state.temperatureUnit
                    ),
                    valueColor = temperatureColor(battery.temperatureC),
                    onInfoClick = { activeInfoSheet = "temperature" }
                )
                MetricRow(
                    label = stringResource(R.string.battery_health),
                    value = batteryHealthLabel(battery.health),
                    valueColor = healthColor(battery.health),
                    onInfoClick = { activeInfoSheet = "healthStatus" }
                )
                MetricRow(
                    label = stringResource(R.string.battery_technology),
                    value = battery.technology.takeUnless { it.equals("Unknown", ignoreCase = true) }
                        ?.takeUnless(String::isBlank)
                        ?: stringResource(R.string.not_available),
                    onInfoClick = { activeInfoSheet = "technology" }
                )
                battery.cycleCount?.let { count ->
                    MetricRow(
                        label = stringResource(R.string.battery_cycle_count),
                        value = stringResource(
                            R.string.value_with_estimated_badge,
                            count.toString()
                        ),
                        showDivider = battery.healthPercent != null,
                        onInfoClick = { activeInfoSheet = "cycleCount" }
                    )
                }
                battery.healthPercent?.let { pct ->
                    MetricRow(
                        label = stringResource(R.string.battery_health_percent),
                        value = stringResource(
                            R.string.value_with_estimated_badge,
                            stringResource(R.string.value_percent, pct)
                        ),
                        showDivider = battery.estimatedCapacityMah != null,
                        onInfoClick = { activeInfoSheet = "healthPercent" }
                    )
                }
                if (battery.estimatedCapacityMah != null && battery.designCapacityMah != null) {
                    MetricRow(
                        label = stringResource(R.string.unit_milliamp_hours),
                        value = stringResource(
                            R.string.battery_capacity_mah,
                            battery.estimatedCapacityMah,
                            battery.designCapacityMah
                        ),
                        showDivider = false,
                        onInfoClick = { activeInfoSheet = "capacity" }
                    )
                }

                val hasBatteryLiveCharts = state.liveLevel.size >= 2 ||
                    state.liveTempC.size >= 2 ||
                    state.liveVoltage.size >= 2
                if (hasBatteryLiveCharts) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                }
                if (state.liveLevel.size >= 2) {
                    LiveChart(
                        data = state.liveLevel,
                        currentValueLabel = stringResource(R.string.value_percent, battery.level),
                        label = stringResource(R.string.battery_level),
                        lineColor = MaterialTheme.colorScheme.primary,
                        yMin = 0f,
                        yMax = 100f,
                        accessibilityDescription = stringResource(
                            R.string.a11y_chart_trend,
                            stringResource(R.string.battery_level)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (state.liveTempC.size >= 2) {
                    if (state.liveLevel.size >= 2) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                    }
                    LiveChart(
                        data = state.liveTempC,
                        currentValueLabel = buildTemperatureValue(
                            temperatureC = battery.temperatureC,
                            temperatureUnit = state.temperatureUnit
                        ),
                        label = stringResource(R.string.battery_temperature),
                        lineColor = temperatureColor(battery.temperatureC),
                        accessibilityDescription = stringResource(
                            R.string.a11y_chart_trend,
                            stringResource(R.string.battery_temperature)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (state.liveVoltage.size >= 2) {
                    if (state.liveLevel.size >= 2 || state.liveTempC.size >= 2) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                    }
                    LiveChart(
                        data = state.liveVoltage,
                        currentValueLabel = stringResource(
                            R.string.value_voltage_volts,
                            (battery.voltageMv / 1000f).toDouble()
                        ),
                        label = stringResource(R.string.battery_voltage),
                        lineColor = MaterialTheme.statusColors.fair,
                        accessibilityDescription = stringResource(
                            R.string.a11y_chart_trend,
                            stringResource(R.string.battery_voltage)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            BatteryPanel {
                CardSectionTitle(
                    text = if (battery.chargingStatus == ChargingStatus.CHARGING) {
                        stringResource(R.string.battery_charging_section)
                    } else {
                        stringResource(R.string.battery_current_section)
                    }
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                    ) {
                        Text(
                            text = stringResource(R.string.battery_current),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = if (battery.currentMa.confidence != Confidence.UNAVAILABLE) {
                                    battery.currentMa.value.toString()
                                } else {
                                    stringResource(R.string.battery_not_available)
                                },
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = MaterialTheme.numericFontFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (battery.currentMa.confidence != Confidence.UNAVAILABLE) {
                                Text(
                                    text = stringResource(R.string.unit_milliamps),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = MaterialTheme.numericFontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (battery.currentMa.confidence != Confidence.UNAVAILABLE) {
                            val powerW = remember(battery.currentMa.value, battery.voltageMv) {
                                val currentA = battery.currentMa.value / 1000f
                                val voltageV = battery.voltageMv / 1000f
                                kotlin.math.abs(currentA * voltageV)
                            }
                            Text(
                                text = stringResource(
                                    R.string.battery_power_voltage,
                                    formatDecimal(powerW, 1),
                                    battery.voltageMv
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ConfidenceBadge(confidence = battery.currentMa.confidence)
                }

                chargingSessionSummary?.let { summary ->
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))
                    CardSectionTitle(text = stringResource(R.string.battery_session_title))
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    BatterySessionSummary(
                        summary = summary,
                        temperatureUnit = state.temperatureUnit
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
                ) {
                    MetricPill(
                        label = stringResource(R.string.battery_status),
                        value = chargingStatusLabel(battery.chargingStatus),
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = stringResource(R.string.battery_plug_type),
                        value = plugTypeLabel(battery.plugType),
                        modifier = Modifier.weight(1f),
                        onInfoClick = { activeInfoSheet = "plugType" }
                    )
                }

                state.currentStats?.let { stats ->
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
                    ) {
                        MetricPill(
                            label = stringResource(R.string.battery_current_average),
                            value = stringResource(R.string.value_milliamps_int, stats.avg),
                            modifier = Modifier.weight(1f),
                            onInfoClick = { activeInfoSheet = "currentStats" }
                        )
                        MetricPill(
                            label = stringResource(R.string.battery_current_minimum),
                            value = stringResource(R.string.value_milliamps_int, stats.min),
                            modifier = Modifier.weight(1f),
                            onInfoClick = { activeInfoSheet = "currentStats" }
                        )
                        MetricPill(
                            label = stringResource(R.string.battery_current_maximum),
                            value = stringResource(R.string.value_milliamps_int, stats.max),
                            modifier = Modifier.weight(1f),
                            onInfoClick = { activeInfoSheet = "currentStats" }
                        )
                    }
                }
            }

            // Charging habits card - show when battery is charging
            if (battery.chargingStatus == ChargingStatus.CHARGING) {
                InfoCard(
                    id = InfoCardCatalog.BatteryChargingHabits.id,
                    headline = stringResource(InfoCardCatalog.BatteryChargingHabits.headlineRes),
                    body = stringResource(InfoCardCatalog.BatteryChargingHabits.bodyRes),
                    onDismiss = onDismissInfoCard,
                    visible = InfoCardCatalog.BatteryChargingHabits.id !in state.dismissedInfoCards && state.showInfoCards,
                    onLearnMore = {
                        InfoCardCatalog.resolveLearnArticleId(
                            InfoCardCatalog.BatteryChargingHabits
                        )?.let(onNavigateToLearnArticle)
                    }
                )
            }

            chargingSessionSummary?.let { summary ->
                if (summary.hasMeaningfulRemainingEstimate(currentLevel = battery.level)) {
                    BatteryRemainingTimePanel(
                        summary = summary,
                        currentLevel = battery.level
                    )
                }
                if (summary.hasGraphData()) {
                    BatterySessionGraphPanel(
                        summary = summary,
                        selectedMetric = sessionMetric,
                        onMetricChange = { selectedSessionMetric = it.name },
                        selectedWindow = sessionWindow,
                        onWindowChange = { selectedSessionWindow = it.name },
                        onNavigateToFullscreen = onNavigateToFullscreen
                    )
                }
            }

            // Screen On/Off usage (#5) — only during discharge
            state.screenUsage?.let { usage ->
                BatteryScreenUsagePanel(
                    usage = usage,
                    onInfoClick = { activeInfoSheet = it }
                )
            }

            // Screen-off drain card - show when screen-off drain rate > 2%/h
            if (state.screenUsage?.screenOffDrainRate != null &&
                state.screenUsage.screenOffDrainRate > 2f
            ) {
                InfoCard(
                    id = InfoCardCatalog.BatteryScreenOffDrain.id,
                    headline = stringResource(InfoCardCatalog.BatteryScreenOffDrain.headlineRes),
                    body = stringResource(InfoCardCatalog.BatteryScreenOffDrain.bodyRes),
                    onDismiss = onDismissInfoCard,
                    visible = InfoCardCatalog.BatteryScreenOffDrain.id !in state.dismissedInfoCards && state.showInfoCards,
                    onLearnMore = {
                        InfoCardCatalog.resolveLearnArticleId(
                            InfoCardCatalog.BatteryScreenOffDrain
                        )?.let(onNavigateToLearnArticle)
                    }
                )
            }

            // Sleep analysis (#8) — only during discharge
            state.sleepAnalysis?.let { sleep ->
                BatterySleepAnalysisPanel(
                    sleep = sleep,
                    onInfoClick = { activeInfoSheet = it }
                )
            }

            BatteryPanel {
                CardSectionTitle(text = stringResource(R.string.home_test_compare))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.charger_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.home_chargers_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                Button(
                    onClick = onNavigateToCharger,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.charger_title))
                }
            }

            BatteryHistoryPanel(
                state = state,
                selectedMetric = historyMetric,
                onMetricChange = { selectedHistoryMetric = it.name },
                onPeriodChange = onPeriodChange,
                onUpgradeToPro = onUpgradeToPro,
                onNavigateToFullscreen = onNavigateToFullscreen
            )

            // Long-term statistics (#9) — Pro feature
            if (state.isPro && state.statistics != null) {
                BatteryStatisticsPanel(
                    statistics = state.statistics,
                    onInfoClick = { activeInfoSheet = it }
                )
            }

            RelatedArticlesSection(
                articleIds = listOf(
                    LearnArticleIds.BATTERY_HEALTH,
                    LearnArticleIds.BATTERY_DRAIN,
                    LearnArticleIds.BATTERY_CHARGING,
                    LearnArticleIds.BATTERY_CURRENT_POWER
                ),
                onNavigateToArticle = onNavigateToLearnArticle
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }

    activeInfoSheet?.let { key ->
        val content = when (key) {
            "voltage" -> BatteryInfoContent.voltage
            "temperature" -> BatteryInfoContent.temperature
            "healthStatus" -> BatteryInfoContent.healthStatus
            "cycleCount" -> BatteryInfoContent.cycleCount
            "healthPercent" -> BatteryInfoContent.healthPercent
            "capacity" -> BatteryInfoContent.capacity
            "currentMa" -> BatteryInfoContent.currentMa
            "powerW" -> BatteryInfoContent.powerW
            "drainRate" -> BatteryInfoContent.drainRate
            "confidence" -> BatteryInfoContent.confidence
            "screenOnOff" -> BatteryInfoContent.screenOnOff
            "deepSleep" -> BatteryInfoContent.deepSleep
            "remaining" -> BatteryInfoContent.remaining
            "technology" -> BatteryInfoContent.technology
            "plugType" -> BatteryInfoContent.plugType
            "currentStats" -> BatteryInfoContent.currentStats
            "statsCharged" -> BatteryInfoContent.statsCharged
            "statsDischarged" -> BatteryInfoContent.statsDischarged
            "statsSessions" -> BatteryInfoContent.statsSessions
            "statsAvgUsage" -> BatteryInfoContent.statsAvgUsage
            "statsFullChargeEst" -> BatteryInfoContent.statsFullChargeEst
            else -> null
        }
        content?.let {
            InfoBottomSheet(
                content = it,
                onDismiss = { activeInfoSheet = null }
            )
        }
    }
}

@Composable
private fun BatteryHeroSection(
    battery: BatteryState,
    history: List<BatteryReading>,
    onInfoClick: (String) -> Unit = {}
) {
    val healthLabel = batteryHealthLabel(battery.health)
    val statusLabel = chargingStatusLabel(battery.chargingStatus)
    val statusText = stringResource(R.string.battery_hero_status, healthLabel, statusLabel)

    val drainRatePctPerHour = remember(history) {
        calculateDrainRate(history)
    }

    val powerW = remember(battery.currentMa.value, battery.voltageMv) {
        if (battery.currentMa.confidence != Confidence.UNAVAILABLE) {
            val currentA = battery.currentMa.value / 1000f
            val voltageV = battery.voltageMv / 1000f
            kotlin.math.abs(currentA * voltageV)
        } else null
    }

    val remainingHours = remember(battery.level, drainRatePctPerHour) {
        if (drainRatePctPerHour != null && drainRatePctPerHour > 0.1f &&
            battery.chargingStatus != ChargingStatus.CHARGING
        ) {
            battery.level / drainRatePctPerHour
        } else null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.heroCardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
      Column(
          modifier = Modifier
              .fillMaxWidth()
              .padding(MaterialTheme.spacing.base),
          verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
      ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            SectionHeader(text = stringResource(R.string.battery_title))

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg)
            ) {
                // Smaller decorative ring
                ProgressRing(
                    progress = battery.level / 100f,
                    modifier = Modifier.size(100.dp),
                    strokeWidth = 6.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    progressColor = statusColorForPercent(battery.level),
                    contentDescription = stringResource(
                        R.string.a11y_progress_percent,
                        stringResource(R.string.battery_level),
                        battery.level
                    )
                ) {}

                // Large typographic value
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = battery.level.toString(),
                            style = MaterialTheme.numericHeroDisplayTextStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.unit_percent),
                            style = MaterialTheme.numericHeroDisplayUnitTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp, bottom = 10.dp)
                        )
                    }
                    Text(
                        text = if (battery.remainingMah != null) {
                            stringResource(R.string.battery_remaining_mah, statusText, battery.remainingMah)
                        } else {
                            statusText
                        },
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
                verticalAlignment = Alignment.Bottom
            ) {
                MetricPill(
                    label = stringResource(R.string.battery_drain_rate),
                    value = drainRatePctPerHour?.let {
                        stringResource(R.string.value_percent_per_hour, it)
                    } ?: stringResource(R.string.battery_estimating),
                    modifier = Modifier.weight(1f),
                    onInfoClick = { onInfoClick("drainRate") }
                )
                MetricPill(
                    label = stringResource(R.string.battery_power),
                    value = powerW?.let {
                        stringResource(R.string.value_watts, it)
                    } ?: stringResource(R.string.battery_estimating),
                    modifier = Modifier.weight(1f),
                    onInfoClick = { onInfoClick("powerW") }
                )
                MetricPill(
                    label = stringResource(R.string.battery_remaining),
                    value = remainingHours?.let { hours ->
                        val h = hours.toInt()
                        val m = ((hours - h) * 60).toInt()
                        if (h > 0) stringResource(R.string.value_duration_hours_minutes, h, m)
                        else stringResource(R.string.value_duration_minutes, m)
                    } ?: stringResource(R.string.battery_estimating),
                    modifier = Modifier.weight(1f),
                    onInfoClick = { onInfoClick("remaining") }
                )
            }
        }
      }
    }
}

private fun calculateDrainRate(history: List<BatteryReading>): Float? {
    if (history.size < 2) return null
    val recent = history.sortedByDescending { it.timestamp }
    val newest = recent.first()
    val oldest = recent.last()
    val timeDiffMs = newest.timestamp - oldest.timestamp
    if (timeDiffMs < 10 * 60 * 1000) return null // need at least 10 min
    val levelDiff = oldest.level - newest.level
    if (levelDiff <= 0) return null // only for discharge
    val hours = timeDiffMs / (1000f * 60f * 60f)
    return levelDiff / hours
}

@Composable
private fun BatteryHistoryPanel(
    state: BatteryUiState.Success,
    selectedMetric: BatteryHistoryMetric,
    onMetricChange: (BatteryHistoryMetric) -> Unit,
    onPeriodChange: (HistoryPeriod) -> Unit,
    onUpgradeToPro: () -> Unit,
    onNavigateToFullscreen: (source: String, metric: String, period: String) -> Unit
) {
    BatteryPanel {
        CardSectionTitle(text = stringResource(R.string.battery_history_title))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        if (state.isPro) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
            ) {
                HistoryPeriod.entries.forEach { period ->
                    val label = when (period) {
                        HistoryPeriod.SINCE_UNPLUG -> stringResource(R.string.history_period_since_unplug)
                        HistoryPeriod.HOUR -> stringResource(R.string.history_period_hour)
                        HistoryPeriod.SIX_HOURS -> stringResource(R.string.history_period_6h)
                        HistoryPeriod.TWELVE_HOURS -> stringResource(R.string.history_period_12h)
                        HistoryPeriod.DAY -> stringResource(R.string.history_period_day)
                        HistoryPeriod.WEEK -> stringResource(R.string.history_period_week)
                        HistoryPeriod.MONTH -> stringResource(R.string.history_period_month)
                        HistoryPeriod.ALL -> stringResource(R.string.history_period_all)
                    }
                    FilterChip(
                        selected = state.selectedPeriod == period,
                        onClick = { onPeriodChange(period) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
            ) {
                BatteryHistoryMetric.entries.forEach { metric ->
                    FilterChip(
                        selected = selectedMetric == metric,
                        onClick = { onMetricChange(metric) },
                        label = { Text(historyMetricLabel(metric)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            val chartModel = remember(
                state.history,
                selectedMetric,
                state.selectedPeriod,
                state.temperatureUnit
            ) {
                buildBatteryHistoryChartModel(
                    history = state.history,
                    metric = selectedMetric,
                    period = state.selectedPeriod,
                    temperatureUnit = state.temperatureUnit,
                    maxPoints = MAX_HISTORY_CHART_POINTS
                )
            }
            val qualityZones = batteryQualityZones(selectedMetric, state.temperatureUnit)

            if (chartModel.chartData.size >= 2) {
                val chartAccessibilitySummary = rememberChartAccessibilitySummary(
                    title = stringResource(
                        R.string.fullscreen_chart_title_battery,
                        historyMetricLabel(selectedMetric)
                    ),
                    chartData = chartModel.chartData,
                    unit = chartModel.unit,
                    decimals = chartModel.tooltipDecimals,
                    timeContext = stringResource(
                        R.string.a11y_chart_context_history,
                        historyPeriodLabel(state.selectedPeriod)
                    )
                )
                val fullscreenSeed = remember(chartModel, selectedMetric, state.selectedPeriod, state.temperatureUnit) {
                    FullscreenChartUiState.Success(
                        chartData = chartModel.chartData,
                        chartTimestamps = chartModel.chartTimestamps,
                        unit = chartModel.unit,
                        selectedMetric = selectedMetric.name,
                        selectedPeriod = state.selectedPeriod.name,
                        metricOptions = BatteryHistoryMetric.entries.map { it.name },
                        periodOptions = HistoryPeriod.entries.map { it.name },
                        yLabels = chartModel.yLabels,
                        xLabels = chartModel.xLabels,
                        tooltipDecimals = chartModel.tooltipDecimals,
                        tooltipTimeSkeleton = chartModel.tooltipTimeSkeleton,
                        temperatureUnit = state.temperatureUnit
                    )
                }
                Text(
                    text = "${historyPeriodLabel(state.selectedPeriod)} · ${historyMetricLabel(selectedMetric)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                ExpandableChartContainer(
                    onExpand = {
                        FullscreenChartSeedStore.prime(
                            source = FullscreenChartSource.BATTERY_HISTORY,
                            state = fullscreenSeed
                        )
                        onNavigateToFullscreen(
                            FullscreenChartSource.BATTERY_HISTORY.name,
                            selectedMetric.name,
                            state.selectedPeriod.name
                        )
                    }
                ) {
                    TrendChart(
                        data = chartModel.chartData,
                        modifier = Modifier.fillMaxWidth(),
                        contentDescription = chartAccessibilitySummary,
                        yLabels = chartModel.yLabels.ifEmpty { null },
                        xLabels = chartModel.xLabels.ifEmpty { null },
                        showGrid = true,
                        qualityZones = qualityZones,
                        tooltipFormatter = { index ->
                            formatChartTooltip(chartModel, index)
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
                ) {
                    val decimals = chartModel.tooltipDecimals
                    chartModel.minValue?.let {
                        MetricPill(
                            label = stringResource(R.string.chart_stat_min),
                            value = "${formatDecimal(it, decimals)}${chartModel.unit}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    chartModel.averageValue?.let {
                        MetricPill(
                            label = stringResource(R.string.chart_stat_avg),
                            value = "${formatDecimal(it, decimals)}${chartModel.unit}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    chartModel.maxValue?.let {
                        MetricPill(
                            label = stringResource(R.string.chart_stat_max),
                            value = "${formatDecimal(it, decimals)}${chartModel.unit}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                BatteryHistoryEmptyState()
            }
        } else {
            BatteryHistoryLockedState(onUpgradeToPro = onUpgradeToPro)
        }
    }
}

@Composable
private fun BatteryHistoryLockedState(
    onUpgradeToPro: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        BatteryHistoryPreviewPlaceholder()
        Text(
            text = stringResource(R.string.pro_feature_battery_history_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = onUpgradeToPro,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(stringResource(R.string.pro_feature_upgrade_action))
        }
    }
}

@Composable
private fun BatteryHistoryEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        BatteryHistoryPreviewPlaceholder()
        Text(
            text = stringResource(R.string.battery_history_metric_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BatteryHistoryPreviewPlaceholder() {
    val fakeData = remember {
        listOf(72f, 70f, 65f, 68f, 60f, 55f, 58f, 52f, 48f, 53f, 50f, 45f, 42f, 47f, 44f, 40f, 38f, 43f, 46f, 50f)
    }
    val chartColor = MaterialTheme.colorScheme.primary
    val chartShape = MaterialTheme.shapes.large

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .graphicsLayer {
                clip = true
                shape = chartShape
            }
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                shape = chartShape
            )
    ) {
        // Blurred fake area chart (blur requires API 31+)
        val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.graphicsLayer {
                renderEffect = android.graphics.RenderEffect
                    .createBlurEffect(18f, 18f, android.graphics.Shader.TileMode.DECAL)
                    .asComposeRenderEffect()
            }
        } else {
            Modifier.graphicsLayer { alpha = 0.3f }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
                .then(blurModifier)
        ) {
            AreaChart(
                data = fakeData,
                modifier = Modifier.fillMaxSize(),
                lineColor = chartColor,
                animate = false
            )
        }

        ProBadgePill(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        )
    }
}



@Composable
private fun BatterySessionSummary(
    summary: ChargingSessionSummary,
    temperatureUnit: TemperatureUnit
) {
    val supplementalStats = listOfNotNull(
        summary.recentSpeedPctPerHour?.let {
            BatteryStatItem(
                label = stringResource(R.string.battery_session_recent_speed),
                value = formatPercentPerHour(it)
            )
        },
        summary.averageSpeedPctPerHour?.let {
            BatteryStatItem(
                label = stringResource(R.string.battery_session_avg_speed),
                value = formatPercentPerHour(it)
            )
        },
        summary.deliveredMah?.let {
            BatteryStatItem(
                label = stringResource(R.string.battery_session_delivered),
                value = stringResource(R.string.value_milliamp_hours, it)
            )
        },
        summary.averagePowerW?.let {
            BatteryStatItem(
                label = stringResource(R.string.battery_session_avg_power),
                value = formatWatts(it)
            )
        }
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            BatterySummaryStat(
                label = stringResource(R.string.battery_session_started),
                value = stringResource(R.string.value_percent, summary.startLevel),
                modifier = Modifier.weight(1f)
            )
            BatterySummaryStat(
                label = stringResource(R.string.battery_session_gain),
                value = stringResource(
                    R.string.value_signed_percent,
                    if (summary.gainPercent >= 0) "+" else "",
                    summary.gainPercent
                ),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            BatterySummaryStat(
                label = stringResource(R.string.battery_session_duration),
                value = formatDuration(summary.durationMs),
                modifier = Modifier.weight(1f)
            )
            BatterySummaryStat(
                label = stringResource(R.string.battery_session_peak_temp),
                value = formatTemperature(
                    summary.peakTemperatureC,
                    temperatureUnit
                ),
                modifier = Modifier.weight(1f)
            )
        }
        supplementalStats.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
            ) {
                row.forEach { item ->
                    BatterySummaryStat(
                        label = item.label,
                        value = item.value,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BatteryRemainingTimePanel(
    summary: ChargingSessionSummary,
    currentLevel: Int
) {
    BatteryPanel {
        CardSectionTitle(text = stringResource(R.string.battery_remaining_time_title))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            BatterySummaryStat(
                label = stringResource(R.string.battery_remaining_time_to_80),
                value = remainingChargeText(
                    currentLevel = currentLevel,
                    targetLevel = TARGET_CHARGE_EIGHTY,
                    remainingMs = summary.remainingTo80Ms
                ),
                modifier = Modifier.weight(1f)
            )
            BatterySummaryStat(
                label = stringResource(R.string.battery_remaining_time_to_full),
                value = remainingChargeText(
                    currentLevel = currentLevel,
                    targetLevel = TARGET_CHARGE_FULL,
                    remainingMs = summary.remainingTo100Ms
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BatterySessionGraphPanel(
    summary: ChargingSessionSummary,
    selectedMetric: SessionGraphMetric,
    onMetricChange: (SessionGraphMetric) -> Unit,
    selectedWindow: SessionGraphWindow,
    onWindowChange: (SessionGraphWindow) -> Unit,
    onNavigateToFullscreen: (source: String, metric: String, period: String) -> Unit
) {
    val hasAnyGraphData = remember(summary.readings) { summary.hasGraphData() }
    val chartModel = remember(summary.readings, selectedMetric, selectedWindow) {
        buildBatterySessionChartModel(
            summary = summary,
            metric = selectedMetric,
            window = selectedWindow,
            maxPoints = MAX_SESSION_CHART_POINTS
        )
    }

    if (!hasAnyGraphData) return

    BatteryPanel {
        CardSectionTitle(text = stringResource(R.string.battery_session_graph_title))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            SessionGraphMetric.entries.forEach { metric ->
                FilterChip(
                    selected = selectedMetric == metric,
                    onClick = { onMetricChange(metric) },
                    label = { Text(sessionGraphMetricLabel(metric)) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            SessionGraphWindow.entries.forEach { window ->
                FilterChip(
                    selected = selectedWindow == window,
                    onClick = { onWindowChange(window) },
                    label = { Text(sessionGraphWindowLabel(window)) }
                )
            }
        }

        if (chartModel.chartData.size >= 2) {
            val chartAccessibilitySummary = rememberChartAccessibilitySummary(
                title = stringResource(
                    R.string.fullscreen_chart_title_session,
                    sessionGraphMetricLabel(selectedMetric)
                ),
                chartData = chartModel.chartData,
                unit = chartModel.unit,
                decimals = chartModel.tooltipDecimals,
                timeContext = if (selectedWindow == SessionGraphWindow.ALL) {
                    stringResource(R.string.a11y_chart_context_session)
                } else {
                    stringResource(
                        R.string.a11y_chart_context_session_window,
                        sessionGraphWindowLabel(selectedWindow)
                    )
                }
            )
            val fullscreenSeed = remember(chartModel, selectedMetric, selectedWindow) {
                FullscreenChartUiState.Success(
                    chartData = chartModel.chartData,
                    chartTimestamps = chartModel.chartTimestamps,
                    unit = chartModel.unit,
                    selectedMetric = selectedMetric.name,
                    selectedPeriod = selectedWindow.name,
                    metricOptions = SessionGraphMetric.entries.map { it.name },
                    periodOptions = SessionGraphWindow.entries.map { it.name },
                    yLabels = chartModel.yLabels,
                    xLabels = chartModel.xLabels,
                    tooltipDecimals = chartModel.tooltipDecimals,
                    tooltipTimeSkeleton = chartModel.tooltipTimeSkeleton
                )
            }
            Text(
                text = sessionGraphMetricLabel(selectedMetric),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            ExpandableChartContainer(
                onExpand = {
                    FullscreenChartSeedStore.prime(
                        source = FullscreenChartSource.BATTERY_SESSION,
                        state = fullscreenSeed
                    )
                    onNavigateToFullscreen(
                        FullscreenChartSource.BATTERY_SESSION.name,
                        selectedMetric.name,
                        selectedWindow.name
                    )
                }
            ) {
                TrendChart(
                    data = chartModel.chartData,
                    modifier = Modifier.fillMaxWidth(),
                    contentDescription = chartAccessibilitySummary,
                    yLabels = chartModel.yLabels.ifEmpty { null },
                    xLabels = chartModel.xLabels.ifEmpty { null },
                    showGrid = true,
                    tooltipFormatter = { index -> formatChartTooltip(chartModel, index) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
            ) {
                val decimals = chartModel.tooltipDecimals
                chartModel.minValue?.let {
                    MetricPill(
                        label = stringResource(R.string.chart_stat_min),
                        value = "${formatDecimal(it, decimals)}${chartModel.unit}",
                        modifier = Modifier.weight(1f)
                    )
                }
                chartModel.averageValue?.let {
                    MetricPill(
                        label = stringResource(R.string.chart_stat_avg),
                        value = "${formatDecimal(it, decimals)}${chartModel.unit}",
                        modifier = Modifier.weight(1f)
                    )
                }
                chartModel.maxValue?.let {
                    MetricPill(
                        label = stringResource(R.string.chart_stat_max),
                        value = "${formatDecimal(it, decimals)}${chartModel.unit}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BatterySummaryStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = MaterialTheme.numericFontFamily
            ),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}


private const val TARGET_CHARGE_EIGHTY = 80
private const val TARGET_CHARGE_FULL = 100

@Composable
private fun BatteryPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            content = content
        )
    }
}


@Composable
private fun temperatureColor(temperatureC: Float): Color = when {
    temperatureC >= 40f -> MaterialTheme.statusColors.critical
    temperatureC >= 35f -> MaterialTheme.statusColors.fair
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun healthColor(health: BatteryHealth): Color = when (health) {
    BatteryHealth.GOOD -> MaterialTheme.statusColors.healthy
    BatteryHealth.UNKNOWN -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.statusColors.fair
}

@Composable
private fun buildTemperatureValue(
    temperatureC: Float,
    temperatureUnit: TemperatureUnit
): String {
    return stringResource(
        R.string.home_thermal_summary,
        formatTemperature(temperatureC, temperatureUnit),
        temperatureBandLabel(temperatureC)
    )
}

private data class BatteryStatItem(
    val label: String,
    val value: String
)

private fun ChargingSessionSummary.hasMeaningfulRemainingEstimate(currentLevel: Int): Boolean {
    val eightyAvailable = currentLevel >= TARGET_CHARGE_EIGHTY || remainingTo80Ms != null
    val fullAvailable = currentLevel >= TARGET_CHARGE_FULL || remainingTo100Ms != null
    return eightyAvailable || fullAvailable
}

@Composable
private fun remainingChargeText(
    currentLevel: Int,
    targetLevel: Int,
    remainingMs: Long?
): String = when {
    currentLevel >= targetLevel -> stringResource(R.string.battery_remaining_time_reached)
    remainingMs != null -> formatDuration(remainingMs)
    else -> stringResource(R.string.battery_remaining_time_estimating)
}

@Composable
private fun formatPercentPerHour(value: Float): String =
    stringResource(R.string.value_percent_per_hour, value.toDouble())

@Composable
private fun formatWatts(value: Float): String =
    stringResource(R.string.value_watts, value.toDouble())


@Composable
private fun formatDuration(durationMs: Long): String {
    val totalMinutes = durationMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(R.string.value_duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.value_duration_minutes, minutes)
    }
}

@Composable
private fun formatDurationCompact(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> stringResource(R.string.value_duration_hours_minutes, hours, minutes)
        minutes > 0 -> stringResource(R.string.value_duration_minutes_seconds, minutes, seconds)
        else -> stringResource(R.string.value_duration_seconds, seconds)
    }
}

// ── Screen On/Off panel (#5) ────────────────────────────────────────────────

@Composable
private fun BatteryScreenUsagePanel(
    usage: ScreenUsageStats,
    onInfoClick: (String) -> Unit = {}
) {
    BatteryPanel {
        CardSectionTitle(text = stringResource(R.string.battery_usage_section))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MetricPill(
                    label = stringResource(R.string.battery_screen_on),
                    value = usage.screenOnDrainRate?.let {
                        stringResource(R.string.value_percent_per_hour, it.toDouble())
                    } ?: stringResource(R.string.battery_estimating),
                    onInfoClick = { onInfoClick("screenOnOff") }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDurationCompact(usage.screenOnDurationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                MetricPill(
                    label = stringResource(R.string.battery_screen_off),
                    value = usage.screenOffDrainRate?.let {
                        stringResource(R.string.value_percent_per_hour, it.toDouble())
                    } ?: stringResource(R.string.battery_estimating),
                    onInfoClick = { onInfoClick("screenOnOff") }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDurationCompact(usage.screenOffDurationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Sleep Analysis panel (#8) ───────────────────────────────────────────────

@Composable
private fun BatterySleepAnalysisPanel(
    sleep: SleepAnalysis,
    onInfoClick: (String) -> Unit = {}
) {
    BatteryPanel {
        CardSectionTitle(text = stringResource(R.string.battery_sleep_analysis))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            MetricPill(
                label = stringResource(R.string.battery_deep_sleep),
                value = formatDurationCompact(sleep.deepSleepDurationMs),
                modifier = Modifier.weight(1f),
                valueColor = MaterialTheme.statusColors.healthy,
                onInfoClick = { onInfoClick("deepSleep") }
            )
            MetricPill(
                label = stringResource(R.string.battery_held_awake),
                value = formatDurationCompact(sleep.heldAwakeDurationMs),
                modifier = Modifier.weight(1f),
                valueColor = if (sleep.heldAwakeDurationMs > sleep.deepSleepDurationMs) {
                    MaterialTheme.statusColors.fair
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                onInfoClick = { onInfoClick("deepSleep") }
            )
        }
    }
}

// ── Long-term Statistics panel (#9) ─────────────────────────────────────────

@Composable
private fun BatteryStatisticsPanel(statistics: BatteryStatistics, onInfoClick: (String) -> Unit = {}) {
    BatteryPanel {
        CardSectionTitle(
            text = stringResource(
                R.string.battery_stats_section_with_period,
                stringResource(R.string.battery_stats_section),
                pluralStringResource(R.plurals.battery_stats_last_n_days, statistics.periodDays, statistics.periodDays)
            )
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            MetricPill(
                label = stringResource(R.string.battery_stats_charged),
                value = stringResource(R.string.battery_stats_pct_total, statistics.totalChargedPct),
                modifier = Modifier.weight(1f),
                onInfoClick = { onInfoClick("statsCharged") }
            )
            MetricPill(
                label = stringResource(R.string.battery_stats_discharged),
                value = stringResource(R.string.battery_stats_pct_total, statistics.totalDischargedPct),
                modifier = Modifier.weight(1f),
                onInfoClick = { onInfoClick("statsDischarged") }
            )
            MetricPill(
                label = stringResource(R.string.battery_stats_sessions),
                value = statistics.chargeSessions.toString(),
                modifier = Modifier.weight(1f),
                onInfoClick = { onInfoClick("statsSessions") }
            )
        }

        statistics.avgDrainRatePctPerHour?.let { rate ->
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
            ) {
                MetricPill(
                    label = stringResource(R.string.battery_stats_avg_usage),
                    value = stringResource(R.string.value_percent_per_hour, rate.toDouble()),
                    modifier = Modifier.weight(1f),
                    onInfoClick = { onInfoClick("statsAvgUsage") }
                )
                statistics.fullChargeEstimateHours?.let { hours ->
                    val h = hours.toInt()
                    val m = ((hours - h) * 60).toInt()
                    MetricPill(
                        label = stringResource(R.string.battery_stats_full_charge_est),
                        value = if (h > 0) {
                            stringResource(R.string.value_duration_hours_minutes, h.toLong(), m.toLong())
                        } else {
                            stringResource(R.string.value_duration_minutes, m.toLong())
                        },
                        modifier = Modifier.weight(1f),
                        onInfoClick = { onInfoClick("statsFullChargeEst") }
                    )
                }
            }
        }
    }
}
