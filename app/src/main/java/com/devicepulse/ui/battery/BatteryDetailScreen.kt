package com.devicepulse.ui.battery

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.R
import com.devicepulse.domain.model.BatteryHealth
import com.devicepulse.domain.model.BatteryReading
import com.devicepulse.domain.model.BatteryState
import com.devicepulse.domain.model.ChargingStatus
import com.devicepulse.domain.model.Confidence
import com.devicepulse.domain.model.HistoryPeriod
import com.devicepulse.domain.model.PlugType
import com.devicepulse.ui.common.formatDecimal
import com.devicepulse.ui.components.ConfidenceBadge
import com.devicepulse.ui.components.DetailTopBar
import com.devicepulse.ui.components.ProgressRing
import com.devicepulse.ui.components.PullToRefreshWrapper
import com.devicepulse.ui.components.TrendChart
import com.devicepulse.ui.theme.numericFontFamily
import com.devicepulse.ui.theme.spacing
import com.devicepulse.ui.theme.statusColors
import kotlin.math.roundToInt

@Composable
fun BatteryDetailScreen(
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit,
    viewModel: BatteryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
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
                        Text(stringResource(R.string.error_generic))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            is BatteryUiState.Success -> {
                BatteryContent(
                    state = state,
                    onRefresh = { viewModel.refresh() },
                    onPeriodChange = { viewModel.setHistoryPeriod(it) },
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

            BatteryHeroSection(battery = battery)

            BatteryPanel {
                BatterySectionEyebrow(text = "DETAILS")
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                BatterySpecRow(
                    label = stringResource(R.string.battery_voltage),
                    value = "${formatDecimal(battery.voltageMv / 1000f, 1)}V"
                )
                BatterySpecRow(
                    label = stringResource(R.string.battery_temperature),
                    value = buildTemperatureValue(battery.temperatureC),
                    emphasis = BatterySpecEmphasis.High,
                    valueColor = temperatureColor(battery.temperatureC)
                )
                BatterySpecRow(
                    label = stringResource(R.string.battery_health),
                    value = healthText(battery.health),
                    emphasis = BatterySpecEmphasis.High,
                    valueColor = healthColor(battery.health)
                )
                BatterySpecRow(
                    label = stringResource(R.string.battery_technology),
                    value = battery.technology
                )
                battery.cycleCount?.let { count ->
                    BatterySpecRow(
                        label = stringResource(R.string.battery_cycle_count),
                        value = count.toString(),
                        showDivider = battery.healthPercent != null
                    )
                }
                battery.healthPercent?.let { pct ->
                    BatterySpecRow(
                        label = stringResource(R.string.battery_health_percent),
                        value = "$pct${stringResource(R.string.unit_percent)}",
                        emphasis = BatterySpecEmphasis.High,
                        showDivider = false
                    )
                }
            }

            BatteryPanel {
                BatterySectionEyebrow(text = stringResource(R.string.battery_current))
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
                            style = MaterialTheme.typography.labelLarge,
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
                    }
                    ConfidenceBadge(confidence = battery.currentMa.confidence)
                }

                chargingSessionSummary?.let { summary ->
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))
                    BatterySectionEyebrow(text = stringResource(R.string.battery_session_title))
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
                    BatteryMetaStat(
                        label = stringResource(R.string.battery_status),
                        value = chargingStatusText(battery.chargingStatus),
                        modifier = Modifier.weight(1f)
                    )
                    BatteryMetaStat(
                        label = stringResource(R.string.battery_plug_type),
                        value = plugTypeText(battery.plugType),
                        modifier = Modifier.weight(1f)
                    )
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

            BatteryHistoryPanel(
                state = state,
                selectedMetric = historyMetric,
                onMetricChange = { selectedHistoryMetric = it.name },
                onPeriodChange = onPeriodChange,
                onUpgradeToPro = onUpgradeToPro
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

@Composable
private fun BatteryHeroSection(battery: BatteryState) {
    BatteryPanel(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.spacing.xs,
                    top = MaterialTheme.spacing.xs,
                    end = MaterialTheme.spacing.xs,
                    bottom = MaterialTheme.spacing.sm
                )
        ) {
            Text(
                text = stringResource(R.string.battery_title).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            ProgressRing(
                progress = battery.level / 100f,
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.CenterHorizontally),
                strokeWidth = 16.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                progressColor = MaterialTheme.colorScheme.primary
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = battery.level.toString(),
                            fontSize = 84.sp,
                            lineHeight = 84.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-3).sp,
                            fontFamily = MaterialTheme.numericFontFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.unit_percent),
                            fontSize = 30.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = MaterialTheme.numericFontFamily,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp, bottom = 14.dp)
                        )
                    }
                }
            }
        }
    }
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
        BatterySectionEyebrow(text = stringResource(R.string.battery_history_title))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        if (state.isPro) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
            ) {
                HistoryPeriod.entries.forEach { period ->
                    val label = when (period) {
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
                state.history.chartDataFor(selectedMetric)
            }

            if (chartData.size >= 2) {
                Text(
                    text = "${historyPeriodLabel(state.selectedPeriod)} · ${historyMetricLabel(selectedMetric)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                TrendChart(
                    data = chartData,
                    modifier = Modifier.fillMaxWidth()
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
        Button(
            onClick = onUpgradeToPro,
            modifier = Modifier.fillMaxWidth()
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
    val barHeights = listOf(12.dp, 24.dp, 18.dp, 34.dp, 30.dp, 44.dp, 22.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = MaterialTheme.spacing.base, vertical = MaterialTheme.spacing.base)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom
            ) {
                barHeights.forEach { barHeight ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)
                            )
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            modifier = Modifier
                .align(Alignment.TopEnd)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = stringResource(R.string.pro_feature_badge).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BatterySectionEyebrow(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
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
                value = "$it ${stringResource(R.string.unit_milliamp_hours)}"
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
                value = "${summary.startLevel}${stringResource(R.string.unit_percent)}",
                modifier = Modifier.weight(1f)
            )
            BatterySummaryStat(
                label = stringResource(R.string.battery_session_gain),
                value = "${if (summary.gainPercent >= 0) "+" else ""}${summary.gainPercent}${stringResource(R.string.unit_percent)}",
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
                value = "${formatDecimal(summary.peakTemperatureC, 1)}${stringResource(R.string.unit_celsius)}",
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
        BatterySectionEyebrow(text = stringResource(R.string.battery_remaining_time_title))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            BatterySummaryStat(
                label = stringResource(R.string.battery_remaining_time_to_80),
                value = remainingChargeText(
                    currentLevel = currentLevel,
                    targetLevel = 80,
                    remainingMs = summary.remainingTo80Ms
                ),
                modifier = Modifier.weight(1f)
            )
            BatterySummaryStat(
                label = stringResource(R.string.battery_remaining_time_to_full),
                value = remainingChargeText(
                    currentLevel = currentLevel,
                    targetLevel = 100,
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
        )
    }

    if (!hasAnyGraphData) return

    BatteryPanel {
        BatterySectionEyebrow(text = stringResource(R.string.battery_session_graph_title))
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
                modifier = Modifier.fillMaxWidth()
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

@Composable
private fun BatteryMetaStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

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
        shape = MaterialTheme.shapes.extraLarge
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
private fun BatterySpecRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasis: BatterySpecEmphasis = BatterySpecEmphasis.Normal,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    showDivider: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = if (emphasis == BatterySpecEmphasis.High) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = if (emphasis == BatterySpecEmphasis.High) {
                    MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = MaterialTheme.numericFontFamily
                    )
                } else {
                    MaterialTheme.typography.titleLarge.copy(
                        fontFamily = MaterialTheme.numericFontFamily
                    )
                },
                color = valueColor,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (showDivider) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun chargingStatusText(status: ChargingStatus): String = when (status) {
    ChargingStatus.CHARGING -> stringResource(R.string.charging_status_charging)
    ChargingStatus.DISCHARGING -> stringResource(R.string.charging_status_discharging)
    ChargingStatus.FULL -> stringResource(R.string.charging_status_full)
    ChargingStatus.NOT_CHARGING -> stringResource(R.string.charging_status_not_charging)
}

@Composable
private fun plugTypeText(plugType: PlugType): String = when (plugType) {
    PlugType.AC -> stringResource(R.string.plug_type_ac)
    PlugType.USB -> stringResource(R.string.plug_type_usb)
    PlugType.WIRELESS -> stringResource(R.string.plug_type_wireless)
    PlugType.NONE -> stringResource(R.string.plug_type_none)
}

@Composable
private fun healthText(health: BatteryHealth): String = when (health) {
    BatteryHealth.GOOD -> stringResource(R.string.battery_health_good)
    BatteryHealth.OVERHEAT -> stringResource(R.string.battery_health_overheat)
    BatteryHealth.DEAD -> stringResource(R.string.battery_health_dead)
    BatteryHealth.OVER_VOLTAGE -> stringResource(R.string.battery_health_over_voltage)
    BatteryHealth.COLD -> stringResource(R.string.battery_health_cold)
    BatteryHealth.UNKNOWN -> stringResource(R.string.battery_health_unknown)
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
    return "${formatDecimal(temperatureC, 1)}${stringResource(R.string.unit_celsius)} · ${temperatureStatusLabel(temperatureC)}"
}

@Composable
private fun temperatureStatusLabel(temperatureC: Float): String = when {
    temperatureC >= 40f -> stringResource(R.string.thermal_hot)
    temperatureC >= 35f -> stringResource(R.string.thermal_warm)
    else -> stringResource(R.string.thermal_cool)
}

private enum class BatteryHistoryMetric {
    LEVEL,
    TEMPERATURE,
    CURRENT,
    VOLTAGE
}

private enum class BatterySpecEmphasis {
    Normal,
    High
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
            targetLevel = 80,
            pacePctPerHour = paceForEstimate
        ),
        remainingTo100Ms = estimateRemainingChargeMs(
            currentLevel = currentLevel,
            targetLevel = 100,
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
    if (durationMs < 10 * 60_000L || gainPercent <= 0) return null
    return gainPercent * 3_600_000f / durationMs
}

private fun sessionRecentSpeed(session: List<BatteryReading>): Float? {
    val recent = session.takeLast(4)
    if (recent.size < 2) return null
    val first = recent.first()
    val last = recent.last()
    val durationMs = (last.timestamp - first.timestamp).coerceAtLeast(0L)
    val levelGain = last.level - first.level
    if (durationMs < 5 * 60_000L || levelGain <= 0) return null
    return levelGain * 3_600_000f / durationMs
}

private fun sessionDeliveredMah(session: List<BatteryReading>): Int? {
    var deliveredMah = 0f
    var hasIntervals = false

    session.zipWithNext().forEach { (start, end) ->
        val startCurrent = start.currentMa
        val endCurrent = end.currentMa
        val durationMs = end.timestamp - start.timestamp
        if (startCurrent != null && endCurrent != null && durationMs in 1..(30 * 60_000L)) {
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
    if (pacePctPerHour == null || pacePctPerHour < 0.25f || currentLevel >= targetLevel) return null
    return (((targetLevel - currentLevel) / pacePctPerHour) * 3_600_000f).roundToInt().toLong()
}

private fun ChargingSessionSummary.hasMeaningfulRemainingEstimate(currentLevel: Int): Boolean {
    val eightyAvailable = currentLevel >= 80 || remainingTo80Ms != null
    val fullAvailable = currentLevel >= 100 || remainingTo100Ms != null
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

private fun formatPercentPerHour(value: Float): String =
    "${formatDecimal(value, 1)}${"%/h"}"

private fun formatWatts(value: Float): String =
    "${formatDecimal(value, 1)} ${"W"}"

private fun formatDuration(durationMs: Long): String {
    val totalMinutes = durationMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
