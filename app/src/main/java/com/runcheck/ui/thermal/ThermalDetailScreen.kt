package com.runcheck.ui.thermal

import android.os.Build
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.ui.chart.MAX_THERMAL_HISTORY_POINTS
import com.runcheck.ui.chart.ThermalHistoryMetric
import com.runcheck.ui.chart.buildThermalHistoryChartModel
import com.runcheck.ui.chart.formatChartTooltip
import com.runcheck.ui.chart.historyPeriodLabel
import com.runcheck.ui.chart.rememberChartAccessibilitySummary
import com.runcheck.ui.chart.thermalHistoryMetricLabel
import com.runcheck.ui.chart.thermalQualityZones
import com.runcheck.ui.common.EnumFilterChipRow
import com.runcheck.ui.common.UiText
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.common.formatTemperature
import com.runcheck.ui.common.formatTemperatureValue
import com.runcheck.ui.common.rememberFormattedDateTime
import com.runcheck.ui.common.rememberSaveableEnumState
import com.runcheck.ui.common.resolve
import com.runcheck.ui.common.temperatureBandLabel
import com.runcheck.ui.common.temperatureUnitRes
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.ContentContainer
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.HeatStrip
import com.runcheck.ui.components.LiveChart
import com.runcheck.ui.components.MetricPill
import com.runcheck.ui.components.ProFeatureCalloutCard
import com.runcheck.ui.components.PullToRefreshWrapper
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.components.SegmentedStatusBar
import com.runcheck.ui.components.StatusDot
import com.runcheck.ui.components.StatusSegment
import com.runcheck.ui.components.TrendChart
import com.runcheck.ui.components.info.InfoCard
import com.runcheck.ui.components.info.InfoCardCatalog
import com.runcheck.ui.components.info.InfoSheetContent
import com.runcheck.ui.components.info.InfoSheetHost
import com.runcheck.ui.components.info.rememberInfoSheetState
import com.runcheck.ui.learn.LearnArticleIds
import com.runcheck.ui.learn.RelatedArticlesSection
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.numericHeroDisplayTextStyle
import com.runcheck.ui.theme.numericHeroDisplayUnitTextStyle
import com.runcheck.ui.theme.numericHeroValueTextStyle
import com.runcheck.ui.theme.reducedMotion
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.runcheckHeroCardColors
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColorForTemperature
import com.runcheck.ui.theme.statusColors

@Composable
fun ThermalDetailScreen(
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToLearnArticle: (articleId: String) -> Unit = {},
    viewModel: ThermalViewModel = hiltViewModel(),
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
        DetailTopBar(
            title = stringResource(R.string.thermal_title),
            onBack = onBack,
        )
        ContentContainer {
            when (val state = uiState) {
                is ThermalUiState.Loading -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .semantics {
                                contentDescription = loadingDescription
                                liveRegion =
                                    LiveRegionMode.Polite
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ThermalUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        ) {
                            Text(stringResource(R.string.common_error_generic))
                            TextButton(onClick = { viewModel.refresh() }) {
                                Text(stringResource(R.string.common_retry))
                            }
                        }
                    }
                }

                is ThermalUiState.Success -> {
                    ThermalContent(
                        state = state,
                        onRefresh = { viewModel.refresh() },
                        onUpgradeToPro = onUpgradeToPro,
                        onNavigateToLearnArticle = onNavigateToLearnArticle,
                        onDismissInfoCard = { viewModel.dismissInfoCard(it) },
                        onPeriodChange = { viewModel.setHistoryPeriod(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ThermalContent(
    state: ThermalUiState.Success,
    onRefresh: () -> Unit,
    onUpgradeToPro: () -> Unit,
    onNavigateToLearnArticle: (articleId: String) -> Unit,
    onDismissInfoCard: (String) -> Unit,
    onPeriodChange: (HistoryPeriod) -> Unit,
) {
    var activeInfoSheet by rememberInfoSheetState()
    var isRefreshing by remember { mutableStateOf(false) }
    val thermal = state.thermalState

    LaunchedEffect(state) {
        isRefreshing = false
    }

    PullToRefreshWrapper(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefresh()
        },
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            item { Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm)) }

            thermalOverviewItems(
                state = state,
                thermal = thermal,
                onDismissInfoCard = onDismissInfoCard,
                onNavigateToLearnArticle = onNavigateToLearnArticle,
                onInfoClick = { activeInfoSheet = it },
            )

            thermalHistoryItems(
                state = state,
                onPeriodChange = onPeriodChange,
            )

            throttlingSection(state = state, onUpgradeToPro = onUpgradeToPro)

            item {
                RelatedArticlesSection(
                    articleIds =
                        listOf(
                            LearnArticleIds.THERMAL_NORMAL_TEMPS,
                            LearnArticleIds.THERMAL_THROTTLING,
                            LearnArticleIds.THERMAL_FEEDBACK,
                        ),
                    onNavigateToArticle = onNavigateToLearnArticle,
                )
            }

            item { Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl)) }
        }
    }

    InfoSheetHost(
        activeKey = activeInfoSheet,
        onDismiss = { activeInfoSheet = null },
        resolveContent = ::resolveThermalInfoContent,
    )
}

private fun LazyListScope.thermalOverviewItems(
    state: ThermalUiState.Success,
    thermal: ThermalState,
    onDismissInfoCard: (String) -> Unit,
    onNavigateToLearnArticle: (String) -> Unit,
    onInfoClick: (String) -> Unit,
) {
    item {
        ThermalHeroCard(
            thermal = thermal,
            temperatureUnit = state.temperatureUnit,
            sessionMinTemp = state.sessionMinTemp,
            sessionMaxTemp = state.sessionMaxTemp,
        )
    }

    item {
        HeatStrip(
            temperatureC = thermal.batteryTempC,
            temperatureUnit = state.temperatureUnit,
        )
    }

    thermalInfoCards(
        state = state,
        thermal = thermal,
        onDismissInfoCard = onDismissInfoCard,
        onNavigateToLearnArticle = onNavigateToLearnArticle,
    )

    item {
        ThermalMetricsCard(
            thermal = thermal,
            temperatureUnit = state.temperatureUnit,
            liveTempC = state.liveTempC,
            liveHeadroom = state.liveHeadroom,
            onInfoClick = onInfoClick,
        )
    }
}

private fun LazyListScope.thermalInfoCards(
    state: ThermalUiState.Success,
    thermal: ThermalState,
    onDismissInfoCard: (String) -> Unit,
    onNavigateToLearnArticle: (String) -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        item {
            InfoCard(
                id = InfoCardCatalog.ThermalThrottlingExplainer.id,
                headline = stringResource(InfoCardCatalog.ThermalThrottlingExplainer.headlineRes),
                body = stringResource(InfoCardCatalog.ThermalThrottlingExplainer.bodyRes),
                onDismiss = onDismissInfoCard,
                visible =
                    InfoCardCatalog.ThermalThrottlingExplainer.id !in state.dismissedInfoCards &&
                        state.showInfoCards,
                onLearnMore = {
                    InfoCardCatalog
                        .resolveLearnArticleId(
                            InfoCardCatalog.ThermalThrottlingExplainer,
                        )?.let(onNavigateToLearnArticle)
                },
            )
        }
    }

    if (thermal.batteryTempC > 35f) {
        item {
            InfoCard(
                id = InfoCardCatalog.ThermalHeatBatteryLoop.id,
                headline = stringResource(InfoCardCatalog.ThermalHeatBatteryLoop.headlineRes),
                body = stringResource(InfoCardCatalog.ThermalHeatBatteryLoop.bodyRes),
                onDismiss = onDismissInfoCard,
                visible =
                    InfoCardCatalog.ThermalHeatBatteryLoop.id !in state.dismissedInfoCards &&
                        state.showInfoCards,
                onLearnMore = {
                    InfoCardCatalog
                        .resolveLearnArticleId(
                            InfoCardCatalog.ThermalHeatBatteryLoop,
                        )?.let(onNavigateToLearnArticle)
                },
            )
        }
    }
}

private fun LazyListScope.thermalHistoryItems(
    state: ThermalUiState.Success,
    onPeriodChange: (HistoryPeriod) -> Unit,
) {
    if (state.isPro) {
        item {
            ThermalHistoryCard(
                history = state.thermalHistory,
                selectedPeriod = state.selectedHistoryPeriod,
                historyLoadError = state.historyLoadError,
                temperatureUnit = state.temperatureUnit,
                onPeriodChange = onPeriodChange,
            )
        }
    }
}

private fun LazyListScope.throttlingSection(
    state: ThermalUiState.Success,
    onUpgradeToPro: () -> Unit,
) {
    item {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        SectionHeader(stringResource(R.string.thermal_throttling_log))
    }

    if (state.isPro) {
        if (state.throttlingEvents.isEmpty()) {
            item {
                ThrottlingEmptyState()
            }
        } else {
            items(
                items = state.throttlingEvents,
                key = { event -> event.id.takeIf { it != 0L } ?: event.timestamp },
            ) { event ->
                ThrottlingEventItem(
                    event = event,
                    temperatureUnit = state.temperatureUnit,
                )
            }
        }
    } else {
        item {
            ProFeatureCalloutCard(
                message = stringResource(R.string.pro_feature_thermal_log_message),
                actionLabel = stringResource(R.string.pro_feature_upgrade_action),
                onAction = onUpgradeToPro,
            )
        }
    }
}

// ── Hero card ────────────────────────────────────────────────────────────────────

@Composable
private fun ThermalHeroCard(
    thermal: ThermalState,
    temperatureUnit: TemperatureUnit,
    sessionMinTemp: Float? = null,
    sessionMaxTemp: Float? = null,
) {
    val tempColor = statusColorForTemperature(thermal.batteryTempC)
    val bandLabel = temperatureBandLabel(thermal.batteryTempC)
    val statusColors = MaterialTheme.statusColors

    val optimalLabel = stringResource(R.string.thermal_cool)
    val normalLabel = stringResource(R.string.thermal_normal)
    val warmLabel = stringResource(R.string.thermal_warm)
    val criticalLabel = stringResource(R.string.thermal_critical)

    val thermalSegments =
        remember(statusColors) {
            listOf(
                StatusSegment(label = optimalLabel, color = statusColors.healthy, rangeStart = 0f, rangeEnd = 35f),
                StatusSegment(label = normalLabel, color = statusColors.fair, rangeStart = 35f, rangeEnd = 40f),
                StatusSegment(label = warmLabel, color = statusColors.poor, rangeStart = 40f, rangeEnd = 45f),
                StatusSegment(label = criticalLabel, color = statusColors.critical, rangeStart = 45f, rangeEnd = 60f),
            )
        }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = runcheckHeroCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.lg, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                SectionHeader(stringResource(R.string.thermal_battery_temp))
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            // Large typographic temperature
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatTemperatureValue(thermal.batteryTempC, temperatureUnit),
                    style = MaterialTheme.numericHeroDisplayTextStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(temperatureUnitRes(temperatureUnit)),
                    style = MaterialTheme.numericHeroDisplayUnitTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
                )
            }

            Text(
                text = bandLabel,
                style = MaterialTheme.typography.titleMedium,
                color = tempColor,
            )

            if (sessionMinTemp != null && sessionMaxTemp != null &&
                sessionMinTemp != sessionMaxTemp
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text =
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = statusColorForTemperature(sessionMinTemp))) {
                                append(
                                    stringResource(
                                        R.string.value_direction_down,
                                        formatTemperature(sessionMinTemp, temperatureUnit),
                                    ),
                                )
                            }
                            append(" · ")
                            withStyle(SpanStyle(color = statusColorForTemperature(sessionMaxTemp))) {
                                append(
                                    stringResource(
                                        R.string.value_direction_up,
                                        formatTemperature(sessionMaxTemp, temperatureUnit),
                                    ),
                                )
                            }
                        },
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            // Segmented thermal status bar
            SegmentedStatusBar(
                segments = thermalSegments,
                currentValue = thermal.batteryTempC,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        }
    }
}

// ── Metrics grid card ────────────────────────────────────────────────────────────

@Composable
private fun ThermalMetricsCard(
    thermal: ThermalState,
    temperatureUnit: TemperatureUnit,
    liveTempC: List<Float> = emptyList(),
    liveHeadroom: List<Float> = emptyList(),
    onInfoClick: (String) -> Unit = {},
) {
    val useNeutralThermalStatus = shouldUseNeutralThermalStatus(thermal)
    val defaultOnSurface = MaterialTheme.colorScheme.onSurface
    val unavailableText = stringResource(R.string.thermal_cpu_unavailable)

    // Pre-compute CPU temperature pill values
    val cpuTempValue =
        thermal.cpuTempC?.let {
            formatTemperature(it, temperatureUnit)
        } ?: unavailableText
    val cpuTempColor =
        thermal.cpuTempC?.let {
            statusColorForTemperature(it)
        } ?: defaultOnSurface

    // Pre-compute headroom pill values
    val headroomValue =
        thermal.thermalHeadroom?.let {
            stringResource(R.string.value_headroom_percent, formatDecimal((1f - it.coerceIn(0f, 1f)) * 100, 0))
        } ?: unavailableText
    val headroomValueColor =
        thermal.thermalHeadroom?.let { headroom ->
            headroomColor(headroom)
        } ?: defaultOnSurface

    // Pre-compute thermal status pill values
    val statusValue =
        if (useNeutralThermalStatus) {
            stringResource(R.string.thermal_status_not_reported)
        } else {
            thermalStatusLabel(thermal.thermalStatus)
        }
    val statusValueColor =
        if (useNeutralThermalStatus) {
            defaultOnSurface
        } else {
            thermalStatusColor(thermal.thermalStatus)
        }

    // Pre-compute throttling pill values
    val statusColors = MaterialTheme.statusColors
    val throttlingValue =
        when {
            thermal.isThrottling -> stringResource(R.string.thermal_throttling_active)
            useNeutralThermalStatus -> stringResource(R.string.thermal_throttling_not_reported)
            else -> stringResource(R.string.thermal_throttling_none)
        }
    val throttlingValueColor =
        when {
            thermal.isThrottling -> statusColors.critical
            useNeutralThermalStatus -> defaultOnSurface
            else -> statusColors.healthy
        }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
        ) {
            // Row 1: CPU Temperature + Thermal Headroom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                MetricPill(
                    label = stringResource(R.string.thermal_cpu_temp),
                    value = cpuTempValue,
                    valueColor = cpuTempColor,
                    onInfoClick = { onInfoClick("cpuTemp") },
                    modifier = Modifier.weight(1f),
                )
                MetricPill(
                    label = stringResource(R.string.thermal_headroom),
                    value = headroomValue,
                    valueColor = headroomValueColor,
                    onInfoClick = { onInfoClick("thermalHeadroom") },
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Row 2: Thermal Status + Throttling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                MetricPill(
                    label = stringResource(R.string.thermal_status),
                    value = statusValue,
                    valueColor = statusValueColor,
                    onInfoClick = { onInfoClick("thermalStatus") },
                    modifier = Modifier.weight(1f),
                )
                MetricPill(
                    label = stringResource(R.string.thermal_throttling),
                    value = throttlingValue,
                    valueColor = throttlingValueColor,
                    onInfoClick = { onInfoClick("throttling") },
                    modifier = Modifier.weight(1f),
                )
            }

            ThermalLiveCharts(
                thermal = thermal,
                temperatureUnit = temperatureUnit,
                liveTempC = liveTempC,
                liveHeadroom = liveHeadroom,
            )
        }
    }
}

@Composable
private fun ThermalLiveCharts(
    thermal: ThermalState,
    temperatureUnit: TemperatureUnit,
    liveTempC: List<Float>,
    liveHeadroom: List<Float>,
) {
    Column {
        if (liveTempC.size >= 2) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            LiveChart(
                data = liveTempC,
                currentValueLabel = formatTemperature(thermal.batteryTempC, temperatureUnit),
                label = stringResource(R.string.thermal_battery_temp),
                lineColor = statusColorForTemperature(thermal.batteryTempC),
                accessibilityDescription =
                    stringResource(
                        R.string.a11y_chart_trend,
                        stringResource(R.string.thermal_battery_temp),
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (liveHeadroom.size >= 2) {
            LiveChart(
                data = liveHeadroom,
                currentValueLabel =
                    thermal.thermalHeadroom?.let {
                        stringResource(R.string.value_headroom_percent, formatDecimal((1f - it.coerceIn(0f, 1f)) * 100, 0))
                    } ?: "\u2014",
                label = stringResource(R.string.thermal_headroom),
                lineColor =
                    thermal.thermalHeadroom?.let { headroomColor(it) }
                        ?: MaterialTheme.colorScheme.primary,
                accessibilityDescription =
                    stringResource(
                        R.string.a11y_chart_trend,
                        stringResource(R.string.thermal_headroom),
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Temperature history card ──────────────────────────────────────────────────────

@Composable
private fun ThermalHistoryCard(
    history: List<ThermalReading>,
    selectedPeriod: HistoryPeriod,
    historyLoadError: UiText?,
    temperatureUnit: TemperatureUnit,
    onPeriodChange: (HistoryPeriod) -> Unit,
) {
    var selectedMetric by rememberSaveableEnumState(ThermalHistoryMetric.BATTERY_TEMP)
    val metric = selectedMetric

    val chartModel =
        remember(history, metric, selectedPeriod, temperatureUnit) {
            buildThermalHistoryChartModel(
                history = history,
                metric = metric,
                period = selectedPeriod,
                maxPoints = MAX_THERMAL_HISTORY_POINTS,
                temperatureUnit = temperatureUnit,
            )
        }

    val qualityZones = thermalQualityZones(temperatureUnit)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            CardSectionTitle(text = stringResource(R.string.thermal_history))

            EnumFilterChipRow(
                values = ThermalHistoryMetric.entries,
                selected = metric,
                onSelect = { selectedMetric = it },
                labelFor = { thermalHistoryMetricLabel(it) },
            )

            EnumFilterChipRow(
                values = HistoryPeriod.entries.filter { it != HistoryPeriod.SINCE_UNPLUG },
                selected = selectedPeriod,
                onSelect = onPeriodChange,
                labelFor = { historyPeriodLabel(it) },
            )

            historyLoadError?.let { error ->
                Text(
                    text = error.resolve(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (chartModel.chartData.size >= 2) {
                val chartAccessibilitySummary =
                    rememberChartAccessibilitySummary(
                        title =
                            stringResource(
                                R.string.fullscreen_chart_title_thermal,
                                thermalHistoryMetricLabel(metric),
                            ),
                        chartData = chartModel.chartData,
                        unit = chartModel.unit,
                        decimals = chartModel.tooltipDecimals,
                        timeContext =
                            stringResource(
                                R.string.a11y_chart_context_history,
                                historyPeriodLabel(selectedPeriod),
                            ),
                    )

                Text(
                    text = "${historyPeriodLabel(selectedPeriod)} \u00B7 ${thermalHistoryMetricLabel(metric)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                TrendChart(
                    data = chartModel.chartData,
                    modifier = Modifier.fillMaxWidth(),
                    contentDescription = chartAccessibilitySummary,
                    yLabels = chartModel.yLabels.ifEmpty { null },
                    xLabels = chartModel.xLabels.ifEmpty { null },
                    showGrid = true,
                    qualityZones = qualityZones,
                    tooltipFormatter = { index -> formatChartTooltip(chartModel, index) },
                )

                // Min / Avg / Max summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
                    verticalAlignment = Alignment.Top,
                ) {
                    chartModel.minValue?.let {
                        MetricPill(
                            label = stringResource(R.string.chart_stat_min),
                            value = "${formatDecimal(it, chartModel.tooltipDecimals)}${chartModel.unit}",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    chartModel.averageValue?.let {
                        MetricPill(
                            label = stringResource(R.string.chart_stat_avg),
                            value = "${formatDecimal(it, chartModel.tooltipDecimals)}${chartModel.unit}",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    chartModel.maxValue?.let {
                        MetricPill(
                            label = stringResource(R.string.chart_stat_max),
                            value = "${formatDecimal(it, chartModel.tooltipDecimals)}${chartModel.unit}",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.network_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Throttling event list ────────────────────────────────────────────────────────

@Composable
private fun ThrottlingEmptyState() {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            StatusDot(color = MaterialTheme.statusColors.healthy)
            Text(
                text = stringResource(R.string.thermal_no_events),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThrottlingEventItem(
    event: ThrottlingEvent,
    temperatureUnit: TemperatureUnit,
) {
    val formattedTime = rememberFormattedDateTime(event.timestamp, "yMMMdHm")
    val statusColor =
        when (event.thermalStatus.lowercase()) {
            "severe" -> MaterialTheme.statusColors.poor
            "critical", "emergency", "shutdown" -> MaterialTheme.statusColors.critical
            else -> MaterialTheme.statusColors.fair
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusDot(color = statusColor)
                    Text(
                        text = event.thermalStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = formatTemperature(event.batteryTempC, temperatureUnit),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = MaterialTheme.numericFontFamily,
                        ),
                    color = statusColorForTemperature(event.batteryTempC),
                )
            }

            event.cpuTempC?.let { cpuTemp ->
                Text(
                    text =
                        stringResource(
                            R.string.value_label_colon,
                            stringResource(R.string.thermal_cpu_temp),
                            formatTemperature(cpuTemp, temperatureUnit),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            event.foregroundApp?.let { app ->
                Text(
                    text = app,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            event.durationMs?.let { duration ->
                val minutes = duration / 60_000
                val seconds = (duration % 60_000) / 1000
                val durationText =
                    if (minutes > 0) {
                        stringResource(R.string.value_duration_minutes_seconds, minutes, seconds)
                    } else {
                        stringResource(R.string.value_duration_seconds, seconds)
                    }
                Text(
                    text = stringResource(R.string.thermal_event_duration, durationText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Helper functions ─────────────────────────────────────────────────────────────

@Composable
private fun thermalStatusLabel(status: ThermalStatus): String =
    when (status) {
        ThermalStatus.NONE -> stringResource(R.string.thermal_status_no_throttling)
        ThermalStatus.LIGHT -> stringResource(R.string.thermal_status_light)
        ThermalStatus.MODERATE -> stringResource(R.string.thermal_status_moderate)
        ThermalStatus.SEVERE -> stringResource(R.string.thermal_status_severe)
        ThermalStatus.CRITICAL -> stringResource(R.string.thermal_status_critical)
        ThermalStatus.EMERGENCY -> stringResource(R.string.thermal_status_emergency)
        ThermalStatus.SHUTDOWN -> stringResource(R.string.thermal_status_shutdown)
    }

@Composable
private fun thermalStatusColor(status: ThermalStatus): Color {
    val colors = MaterialTheme.statusColors
    return when (status) {
        ThermalStatus.NONE -> MaterialTheme.colorScheme.onSurface
        ThermalStatus.LIGHT -> colors.healthy
        ThermalStatus.MODERATE -> colors.fair
        ThermalStatus.SEVERE -> colors.poor
        ThermalStatus.CRITICAL -> colors.critical
        ThermalStatus.EMERGENCY -> colors.critical
        ThermalStatus.SHUTDOWN -> colors.critical
    }
}

@Composable
private fun headroomColor(headroom: Float): Color {
    val usedPercent = ((1f - headroom.coerceIn(0f, 1f)) * 100).toInt()
    val colors = MaterialTheme.statusColors
    return when {
        usedPercent >= 90 -> colors.critical
        usedPercent >= 70 -> colors.poor
        usedPercent >= 50 -> colors.fair
        else -> colors.healthy
    }
}

private fun resolveThermalInfoContent(key: String): InfoSheetContent? =
    when (key) {
        "cpuTemp" -> ThermalInfoContent.cpuTemp
        "thermalHeadroom" -> ThermalInfoContent.thermalHeadroom
        "thermalStatus" -> ThermalInfoContent.thermalStatus
        "throttling" -> ThermalInfoContent.throttling
        else -> null
    }

internal fun shouldUseNeutralThermalStatus(thermal: ThermalState): Boolean =
    !thermal.isThrottling &&
        thermal.thermalStatus == ThermalStatus.NONE &&
        thermal.thermalHeadroom == null
