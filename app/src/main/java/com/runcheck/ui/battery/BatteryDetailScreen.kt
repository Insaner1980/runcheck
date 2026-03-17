package com.runcheck.ui.battery

import com.runcheck.ui.ads.DetailScreenAdBanner
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
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
import com.runcheck.ui.common.batteryHealthLabel
import com.runcheck.ui.common.chargingStatusLabel
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.common.plugTypeLabel
import com.runcheck.ui.common.temperatureBandLabel
import com.runcheck.ui.components.AreaChart
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.ConfidenceBadge
import com.runcheck.ui.components.MetricPill
import com.runcheck.ui.components.MetricRow
import com.runcheck.ui.theme.statusColorForPercent
import com.runcheck.ui.components.ProBadgePill
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.ProgressRing
import com.runcheck.ui.components.PullToRefreshWrapper
import com.runcheck.ui.components.TrendChart
import com.runcheck.domain.usecase.BatteryStatistics
import com.runcheck.service.monitor.ScreenUsageStats
import com.runcheck.service.monitor.SleepAnalysis
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors
import kotlin.math.roundToInt

@Composable
fun BatteryDetailScreen(
    onBack: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BatteryViewModel = hiltViewModel()
) {
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
        when (val state = uiState) {
            is BatteryUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is BatteryUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    onUpgradeToPro = onUpgradeToPro
                )
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
    onUpgradeToPro: () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val battery = state.batteryState
    var selectedHistoryMetric by rememberSaveable { mutableStateOf(BatteryHistoryMetric.LEVEL.name) }
    var selectedSessionMetric by rememberSaveable { mutableStateOf(SessionGraphMetric.CURRENT.name) }
    var selectedSessionWindow by rememberSaveable { mutableStateOf(SessionGraphWindow.ALL.name) }
    val historyMetric = BatteryHistoryMetric.valueOf(selectedHistoryMetric)
    val sessionMetric = SessionGraphMetric.valueOf(selectedSessionMetric)
    val sessionWindow = SessionGraphWindow.valueOf(selectedSessionWindow)
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

            BatteryHeroSection(battery = battery, history = state.history)

            BatteryPanel {
                CardSectionTitle(text = stringResource(R.string.battery_section_details))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                MetricRow(
                    label = stringResource(R.string.battery_voltage),
                    value = stringResource(
                        R.string.value_voltage_volts,
                        (battery.voltageMv / 1000f).toDouble()
                    )
                )
                MetricRow(
                    label = stringResource(R.string.battery_temperature),
                    value = buildTemperatureValue(battery.temperatureC),

                    valueColor = temperatureColor(battery.temperatureC)
                )
                MetricRow(
                    label = stringResource(R.string.battery_health),
                    value = batteryHealthLabel(battery.health),

                    valueColor = healthColor(battery.health)
                )
                MetricRow(
                    label = stringResource(R.string.battery_technology),
                    value = battery.technology.takeUnless { it.equals("Unknown", ignoreCase = true) }
                        ?.takeUnless(String::isBlank)
                        ?: stringResource(R.string.not_available)
                )
                battery.cycleCount?.let { count ->
                    MetricRow(
                        label = stringResource(R.string.battery_cycle_count),
                        value = count.toString(),
                        showDivider = battery.healthPercent != null
                    )
                }
                battery.healthPercent?.let { pct ->
                    MetricRow(
                        label = stringResource(R.string.battery_health_percent),
                        value = stringResource(R.string.value_percent, pct),
                        showDivider = battery.estimatedCapacityMah != null
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
                        showDivider = false
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
                    BatterySessionSummary(summary = summary)
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
                        modifier = Modifier.weight(1f)
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
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = stringResource(R.string.battery_current_minimum),
                            value = stringResource(R.string.value_milliamps_int, stats.min),
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = stringResource(R.string.battery_current_maximum),
                            value = stringResource(R.string.value_milliamps_int, stats.max),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
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
                        onWindowChange = { selectedSessionWindow = it.name }
                    )
                }
            }

            // Screen On/Off usage (#5) — only during discharge
            state.screenUsage?.let { usage ->
                BatteryScreenUsagePanel(usage = usage)
            }

            // Sleep analysis (#8) — only during discharge
            state.sleepAnalysis?.let { sleep ->
                BatterySleepAnalysisPanel(sleep = sleep)
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
                onUpgradeToPro = onUpgradeToPro
            )

            // Long-term statistics (#9) — Pro feature
            if (state.isPro && state.statistics != null) {
                BatteryStatisticsPanel(statistics = state.statistics)
            }

            DetailScreenAdBanner()

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

@Composable
private fun BatteryHeroSection(
    battery: BatteryState,
    history: List<BatteryReading>
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

    BatteryPanel(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                SectionHeader(text = stringResource(R.string.battery_title))
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            ProgressRing(
                progress = battery.level / 100f,
                modifier = Modifier.size(152.dp),
                strokeWidth = 10.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                progressColor = statusColorForPercent(battery.level),
                contentDescription = stringResource(
                    R.string.a11y_progress_percent,
                    stringResource(R.string.battery_level),
                    battery.level
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = battery.level.toString(),
                            fontSize = 48.sp,
                            lineHeight = 48.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-2).sp,
                            fontFamily = MaterialTheme.numericFontFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.unit_percent),
                            fontSize = 20.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = MaterialTheme.numericFontFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 3.dp, bottom = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Text(
                text = if (battery.remainingMah != null) {
                    stringResource(R.string.battery_remaining_mah, statusText, battery.remainingMah)
                } else {
                    statusText
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
            ) {
                MetricPill(
                    label = stringResource(R.string.battery_drain_rate),
                    value = drainRatePctPerHour?.let {
                        stringResource(R.string.value_percent_per_hour, it)
                    } ?: stringResource(R.string.battery_estimating),
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = stringResource(R.string.battery_power),
                    value = powerW?.let {
                        stringResource(R.string.value_watts, it)
                    } ?: stringResource(R.string.battery_estimating),
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = stringResource(R.string.battery_remaining),
                    value = remainingHours?.let { hours ->
                        val h = hours.toInt()
                        val m = ((hours - h) * 60).toInt()
                        if (h > 0) stringResource(R.string.value_duration_hours_minutes, h, m)
                        else stringResource(R.string.value_duration_minutes, m)
                    } ?: stringResource(R.string.battery_estimating),
                    modifier = Modifier.weight(1f)
                )
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
    onUpgradeToPro: () -> Unit
) {
    BatteryPanel {
        CardSectionTitle(text = stringResource(R.string.battery_history_title))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        if (state.isPro) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
            ) {
                HistoryPeriod.entries.forEach { period ->
                    val label = when (period) {
                        HistoryPeriod.SINCE_UNPLUG -> stringResource(R.string.history_period_since_unplug)
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
                modifier = Modifier.fillMaxWidth(),
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

            val chartData = remember(state.history, selectedMetric) {
                state.history
                    .chartDataFor(selectedMetric)
                    .downsampleForChart(MAX_HISTORY_CHART_POINTS)
            }

            if (chartData.size >= 2) {
                Text(
                    text = "${historyPeriodLabel(state.selectedPeriod)} · ${historyMetricLabel(selectedMetric)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                TrendChart(
                    data = chartData,
                    modifier = Modifier.fillMaxWidth(),
                    contentDescription = stringResource(
                        R.string.a11y_chart_trend,
                        historyMetricLabel(selectedMetric)
                    )
                )
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(18.dp)
            }
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        // Blurred fake area chart
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
                .graphicsLayer {
                    renderEffect = android.graphics.RenderEffect
                        .createBlurEffect(18f, 18f, android.graphics.Shader.TileMode.DECAL)
                        .asComposeRenderEffect()
                }
        ) {
            AreaChart(
                data = fakeData,
                modifier = Modifier.fillMaxSize(),
                lineColor = chartColor
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
private fun BatterySessionSummary(summary: ChargingSessionSummary) {
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
                value = stringResource(
                    R.string.value_temperature,
                    summary.peakTemperatureC.toDouble()
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
    onWindowChange: (SessionGraphWindow) -> Unit
) {
    val hasAnyGraphData = remember(summary.readings) { summary.hasGraphData() }
    val chartData = remember(summary.readings, selectedMetric, selectedWindow) {
        summary.readings.graphDataFor(
            metric = selectedMetric,
            window = selectedWindow
        ).downsampleForChart(MAX_SESSION_CHART_POINTS)
    }

    if (!hasAnyGraphData) return

    BatteryPanel {
        CardSectionTitle(text = stringResource(R.string.battery_session_graph_title))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.fillMaxWidth(),
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

        if (chartData.size >= 2) {
            Text(
                text = sessionGraphMetricLabel(selectedMetric),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            TrendChart(
                data = chartData,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = stringResource(
                    R.string.a11y_chart_trend,
                    sessionGraphMetricLabel(selectedMetric)
                )
            )
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
        modifier = modifier,
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


private fun List<Float>.downsampleForChart(maxPoints: Int): List<Float> {
    if (size <= maxPoints || maxPoints <= 1) return this

    val lastIndex = lastIndex
    return buildList(maxPoints) {
        for (index in 0 until maxPoints) {
            val sourceIndex = ((index.toLong() * lastIndex) / (maxPoints - 1)).toInt()
            add(this@downsampleForChart[sourceIndex])
        }
    }
}

private const val MAX_HISTORY_CHART_POINTS = 300
private const val MAX_SESSION_CHART_POINTS = 240
private const val RECENT_SESSION_SAMPLE_COUNT = 4
private const val MIN_SESSION_SPEED_DURATION_MS = 10 * 60_000L
private const val MIN_RECENT_SPEED_DURATION_MS = 5 * 60_000L
private const val MAX_DELIVERY_INTERVAL_MS = 30 * 60_000L
private const val MIN_ESTIMATE_PACE_PER_HOUR = 0.25f
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
        shape = RoundedCornerShape(16.dp)
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
private fun historyPeriodLabel(period: HistoryPeriod): String = when (period) {
    HistoryPeriod.SINCE_UNPLUG -> stringResource(R.string.history_period_since_unplug)
    HistoryPeriod.DAY -> stringResource(R.string.history_period_day)
    HistoryPeriod.WEEK -> stringResource(R.string.history_period_week)
    HistoryPeriod.MONTH -> stringResource(R.string.history_period_month)
    HistoryPeriod.ALL -> stringResource(R.string.history_period_all)
}

@Composable
private fun historyMetricLabel(metric: BatteryHistoryMetric): String = when (metric) {
    BatteryHistoryMetric.LEVEL -> stringResource(R.string.battery_history_metric_level)
    BatteryHistoryMetric.TEMPERATURE -> stringResource(R.string.battery_history_metric_temperature)
    BatteryHistoryMetric.CURRENT -> stringResource(R.string.battery_history_metric_current)
    BatteryHistoryMetric.VOLTAGE -> stringResource(R.string.battery_history_metric_voltage)
}

@Composable
private fun sessionGraphMetricLabel(metric: SessionGraphMetric): String = when (metric) {
    SessionGraphMetric.CURRENT -> stringResource(R.string.battery_history_metric_current)
    SessionGraphMetric.POWER -> stringResource(R.string.battery_session_graph_metric_power)
}

@Composable
private fun sessionGraphWindowLabel(window: SessionGraphWindow): String = when (window) {
    SessionGraphWindow.FIFTEEN_MINUTES -> stringResource(R.string.battery_session_graph_window_15m)
    SessionGraphWindow.THIRTY_MINUTES -> stringResource(R.string.battery_session_graph_window_30m)
    SessionGraphWindow.ALL -> stringResource(R.string.history_period_all)
}

@Composable
private fun buildTemperatureValue(temperatureC: Float): String {
    return stringResource(
        R.string.home_thermal_summary,
        stringResource(R.string.value_temperature, temperatureC.toDouble()),
        temperatureBandLabel(temperatureC)
    )
}

private enum class BatteryHistoryMetric {
    LEVEL,
    TEMPERATURE,
    CURRENT,
    VOLTAGE
}

private enum class SessionGraphMetric {
    CURRENT,
    POWER
}

private enum class SessionGraphWindow(val durationMs: Long?) {
    FIFTEEN_MINUTES(15 * 60_000L),
    THIRTY_MINUTES(30 * 60_000L),
    ALL(null)
}

private data class ChargingSessionSummary(
    val startLevel: Int,
    val gainPercent: Int,
    val durationMs: Long,
    val peakTemperatureC: Float,
    val averageCurrentMa: Int?,
    val deliveredMah: Int?,
    val averagePowerW: Float?,
    val averageSpeedPctPerHour: Float?,
    val recentSpeedPctPerHour: Float?,
    val remainingTo80Ms: Long?,
    val remainingTo100Ms: Long?,
    val readings: List<BatteryReading>
)

private data class BatteryStatItem(
    val label: String,
    val value: String
)

private fun List<BatteryReading>.chartDataFor(metric: BatteryHistoryMetric): List<Float> = when (metric) {
    BatteryHistoryMetric.LEVEL -> map { it.level.toFloat() }
    BatteryHistoryMetric.TEMPERATURE -> map { it.temperatureC }
    BatteryHistoryMetric.CURRENT -> mapNotNull { it.currentMa?.toFloat() }
    BatteryHistoryMetric.VOLTAGE -> map { it.voltageMv / 1000f }
}

private fun calculateChargingSessionSummary(
    history: List<BatteryReading>,
    currentLevel: Int,
    chargingStatus: ChargingStatus
): ChargingSessionSummary? {
    if (chargingStatus != ChargingStatus.CHARGING || history.isEmpty()) return null

    val sorted = history.sortedBy { it.timestamp }
    val latestChargingIndex = sorted.indexOfLast { it.status == ChargingStatus.CHARGING.name }
    if (latestChargingIndex == -1) return null

    var startIndex = latestChargingIndex
    while (startIndex > 0 && sorted[startIndex - 1].status == ChargingStatus.CHARGING.name) {
        startIndex--
    }

    val session = sorted.subList(startIndex, latestChargingIndex + 1)
    val first = session.firstOrNull() ?: return null
    val last = session.lastOrNull() ?: return null
    val durationMs = (last.timestamp - first.timestamp).coerceAtLeast(0L)
    val gainPercent = currentLevel - first.level
    val averageSpeedPctPerHour = sessionAverageSpeed(gainPercent, durationMs)
    val recentSpeedPctPerHour = sessionRecentSpeed(session)
    val deliveredMah = sessionDeliveredMah(session)
    val averageCurrentMa = sessionAverageCurrent(session, deliveredMah)
    val averagePowerW = averageCurrentMa?.let { currentMa ->
        val avgVoltageV = session.map { it.voltageMv / 1000f }.average().toFloat()
        (currentMa * avgVoltageV) / 1000f
    }
    val paceForEstimate = averageSpeedPctPerHour ?: recentSpeedPctPerHour

    return ChargingSessionSummary(
        startLevel = first.level,
        gainPercent = gainPercent,
        durationMs = durationMs,
        peakTemperatureC = session.maxOf { it.temperatureC },
        averageCurrentMa = averageCurrentMa,
        deliveredMah = deliveredMah,
        averagePowerW = averagePowerW,
        averageSpeedPctPerHour = averageSpeedPctPerHour,
        recentSpeedPctPerHour = recentSpeedPctPerHour,
        remainingTo80Ms = estimateRemainingChargeMs(
            currentLevel = currentLevel,
            targetLevel = TARGET_CHARGE_EIGHTY,
            pacePctPerHour = paceForEstimate
        ),
        remainingTo100Ms = estimateRemainingChargeMs(
            currentLevel = currentLevel,
            targetLevel = TARGET_CHARGE_FULL,
            pacePctPerHour = paceForEstimate
        ),
        readings = session
    )
}

private fun List<BatteryReading>.graphDataFor(
    metric: SessionGraphMetric,
    window: SessionGraphWindow
): List<Float> {
    if (isEmpty()) return emptyList()

    val filtered = window.durationMs?.let { duration ->
        val latestTimestamp = last().timestamp
        filter { latestTimestamp - it.timestamp <= duration }
    } ?: this

    return when (metric) {
        SessionGraphMetric.CURRENT -> filtered.mapNotNull { it.currentMa?.toFloat() }
        SessionGraphMetric.POWER -> filtered.mapNotNull { reading ->
            reading.currentMa?.let { currentMa ->
                (currentMa * (reading.voltageMv / 1000f)) / 1000f
            }
        }
    }
}

private fun sessionAverageSpeed(gainPercent: Int, durationMs: Long): Float? {
    if (durationMs < MIN_SESSION_SPEED_DURATION_MS || gainPercent <= 0) return null
    return gainPercent * 3_600_000f / durationMs
}

private fun sessionRecentSpeed(session: List<BatteryReading>): Float? {
    val recent = session.takeLast(RECENT_SESSION_SAMPLE_COUNT)
    if (recent.size < 2) return null
    val first = recent.first()
    val last = recent.last()
    val durationMs = (last.timestamp - first.timestamp).coerceAtLeast(0L)
    val levelGain = last.level - first.level
    if (durationMs < MIN_RECENT_SPEED_DURATION_MS || levelGain <= 0) return null
    return levelGain * 3_600_000f / durationMs
}

private fun sessionDeliveredMah(session: List<BatteryReading>): Int? {
    var deliveredMah = 0f
    var hasIntervals = false

    session.zipWithNext().forEach { (start, end) ->
        val startCurrent = start.currentMa
        val endCurrent = end.currentMa
        val durationMs = end.timestamp - start.timestamp
        if (startCurrent != null && endCurrent != null && durationMs in 1..MAX_DELIVERY_INTERVAL_MS) {
            val averageCurrent = ((startCurrent + endCurrent) / 2f).coerceAtLeast(0f)
            deliveredMah += averageCurrent * (durationMs / 3_600_000f)
            hasIntervals = true
        }
    }

    return if (hasIntervals) deliveredMah.roundToInt() else null
}

private fun sessionAverageCurrent(
    session: List<BatteryReading>,
    deliveredMah: Int?
): Int? {
    if (deliveredMah == null || session.size < 2) return null
    val durationMs = (session.last().timestamp - session.first().timestamp).coerceAtLeast(0L)
    if (durationMs <= 0L) return null
    return (deliveredMah / (durationMs / 3_600_000f)).roundToInt()
}

private fun estimateRemainingChargeMs(
    currentLevel: Int,
    targetLevel: Int,
    pacePctPerHour: Float?
): Long? {
    if (pacePctPerHour == null || pacePctPerHour < MIN_ESTIMATE_PACE_PER_HOUR || currentLevel >= targetLevel) {
        return null
    }
    return (((targetLevel - currentLevel) / pacePctPerHour) * 3_600_000f).roundToInt().toLong()
}

private fun ChargingSessionSummary.hasMeaningfulRemainingEstimate(currentLevel: Int): Boolean {
    val eightyAvailable = currentLevel >= TARGET_CHARGE_EIGHTY || remainingTo80Ms != null
    val fullAvailable = currentLevel >= TARGET_CHARGE_FULL || remainingTo100Ms != null
    return eightyAvailable || fullAvailable
}

private fun ChargingSessionSummary.hasGraphData(): Boolean =
    readings.graphDataFor(SessionGraphMetric.CURRENT, SessionGraphWindow.ALL).size >= 2 ||
        readings.graphDataFor(SessionGraphMetric.POWER, SessionGraphWindow.ALL).size >= 2

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
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

// ── Screen On/Off panel (#5) ────────────────────────────────────────────────

@Composable
private fun BatteryScreenUsagePanel(usage: ScreenUsageStats) {
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
                    } ?: stringResource(R.string.battery_estimating)
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
                    } ?: stringResource(R.string.battery_estimating)
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
private fun BatterySleepAnalysisPanel(sleep: SleepAnalysis) {
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
                valueColor = MaterialTheme.statusColors.healthy
            )
            MetricPill(
                label = stringResource(R.string.battery_held_awake),
                value = formatDurationCompact(sleep.heldAwakeDurationMs),
                modifier = Modifier.weight(1f),
                valueColor = if (sleep.heldAwakeDurationMs > sleep.deepSleepDurationMs) {
                    MaterialTheme.statusColors.fair
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

// ── Long-term Statistics panel (#9) ─────────────────────────────────────────

@Composable
private fun BatteryStatisticsPanel(statistics: BatteryStatistics) {
    BatteryPanel {
        CardSectionTitle(
            text = stringResource(R.string.battery_stats_section) + " · " +
                stringResource(R.string.battery_stats_last_n_days, statistics.periodDays)
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            MetricPill(
                label = stringResource(R.string.battery_stats_charged),
                value = stringResource(R.string.battery_stats_pct_total, statistics.totalChargedPct),
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                label = stringResource(R.string.battery_stats_discharged),
                value = stringResource(R.string.battery_stats_pct_total, statistics.totalDischargedPct),
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                label = stringResource(R.string.battery_stats_sessions),
                value = statistics.chargeSessions.toString(),
                modifier = Modifier.weight(1f)
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
                    modifier = Modifier.weight(1f)
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
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
