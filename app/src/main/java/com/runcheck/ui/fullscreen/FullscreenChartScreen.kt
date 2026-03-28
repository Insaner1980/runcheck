package com.runcheck.ui.fullscreen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.ui.chart.BatteryHistoryMetric
import com.runcheck.ui.chart.FullscreenChartSource
import com.runcheck.ui.chart.NetworkHistoryMetric
import com.runcheck.ui.chart.SessionGraphMetric
import com.runcheck.ui.chart.SessionGraphWindow
import com.runcheck.ui.chart.batteryQualityZones
import com.runcheck.ui.chart.formatChartTooltip
import com.runcheck.ui.chart.historyMetricLabel
import com.runcheck.ui.chart.historyPeriodLabel
import com.runcheck.ui.chart.networkHistoryMetricLabel
import com.runcheck.ui.chart.rememberChartAccessibilitySummary
import com.runcheck.ui.chart.sessionGraphMetricLabel
import com.runcheck.ui.chart.sessionGraphWindowLabel
import com.runcheck.ui.chart.signalQualityZones
import com.runcheck.ui.components.ProFeatureLockedState
import com.runcheck.ui.components.TrendChart
import com.runcheck.ui.components.TrendChartPresentation
import com.runcheck.ui.theme.spacing

/** Keys for passing selections back via savedStateHandle. */
object FullscreenChartResult {
    const val KEY_SOURCE = "fullscreen_result_source"
    const val KEY_METRIC = "fullscreen_result_metric"
    const val KEY_PERIOD = "fullscreen_result_period"
}

@Composable
fun FullscreenChartScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onUpgradeToPro: () -> Unit = {},
    onSelectionChanged: (source: String, metric: String, period: String) -> Unit = { _, _, _ -> },
    viewModel: FullscreenChartViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val title =
        when (val state = uiState) {
            is FullscreenChartUiState.Success -> resolveChartTitle(viewModel.source, state.selectedMetric)

            is FullscreenChartUiState.Empty -> resolveChartTitle(viewModel.source, state.selectedMetric)

            is FullscreenChartUiState.Error -> resolveChartTitle(viewModel.source, state.selectedMetric)

            FullscreenChartUiState.Locked,
            FullscreenChartUiState.Loading,
            -> resolveSourceTitle(viewModel.source)
        }

    FullscreenChartScaffold(
        modifier = modifier,
        title = title,
        onClose = onBack,
        controls =
            when (val state = uiState) {
                is FullscreenChartUiState.Success,
                is FullscreenChartUiState.Empty,
                is FullscreenChartUiState.Error,
                -> {
                    val sel = state as FullscreenChartUiState.HasSelections
                    {
                        FullscreenChartControls(
                            source = viewModel.source,
                            selectedMetric = sel.selectedMetric,
                            selectedPeriod = sel.selectedPeriod,
                            metricOptions = sel.metricOptions,
                            periodOptions = sel.periodOptions,
                            onMetricChange = {
                                viewModel.setMetric(it)
                                onSelectionChanged(viewModel.source.name, it, viewModel.selectedPeriod)
                            },
                            onPeriodChange = {
                                viewModel.setPeriod(it)
                                onSelectionChanged(viewModel.source.name, viewModel.selectedMetric, it)
                            },
                        )
                    }
                }

                FullscreenChartUiState.Locked,
                FullscreenChartUiState.Loading,
                -> {
                    null
                }
            },
    ) { contentModifier ->
        when (val state = uiState) {
            is FullscreenChartUiState.Loading -> {
                Box(contentModifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            FullscreenChartUiState.Locked -> {
                FullscreenChartLockedContent(
                    modifier = contentModifier,
                    source = viewModel.source,
                    onUpgradeToPro = onUpgradeToPro,
                )
            }

            is FullscreenChartUiState.Empty -> {
                FullscreenChartEmptyContent(
                    modifier = contentModifier,
                    state = state,
                    source = viewModel.source,
                )
            }

            is FullscreenChartUiState.Error -> {
                Box(contentModifier, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.fullscreen_chart_error),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                        Button(onClick = { viewModel.retry() }) {
                            Text(stringResource(R.string.fullscreen_chart_retry))
                        }
                    }
                }
            }

            is FullscreenChartUiState.Success -> {
                FullscreenChartContent(
                    modifier = contentModifier,
                    state = state,
                    source = viewModel.source,
                )
            }
        }
    }
}

@Composable
private fun FullscreenChartScaffold(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    controls: (@Composable () -> Unit)? = null,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets =
            WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
            ),
        topBar = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                            ),
                        ).padding(
                            start = MaterialTheme.spacing.sm,
                            end = MaterialTheme.spacing.sm,
                            top = MaterialTheme.spacing.xs,
                            bottom = MaterialTheme.spacing.xs,
                        ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.close_fullscreen_chart),
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (controls != null) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    controls()
                }
            }
        },
    ) { innerPadding ->
        content(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = MaterialTheme.spacing.sm)
                .padding(bottom = MaterialTheme.spacing.sm),
        )
    }
}

@Composable
private fun FullscreenChartControls(
    source: FullscreenChartSource,
    selectedMetric: String,
    selectedPeriod: String,
    metricOptions: List<String>,
    periodOptions: List<String>,
    onMetricChange: (String) -> Unit,
    onPeriodChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        periodOptions.forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodChange(period) },
                label = { Text(resolvePeriodLabel(source, period)) },
            )
        }
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
        metricOptions.forEach { metric ->
            FilterChip(
                selected = selectedMetric == metric,
                onClick = { onMetricChange(metric) },
                label = { Text(resolveMetricLabel(source, metric)) },
            )
        }
    }
}

@Composable
private fun FullscreenChartLockedContent(
    source: FullscreenChartSource,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = resolveSourceTitle(source)
    val message =
        when (source) {
            FullscreenChartSource.BATTERY_HISTORY -> stringResource(R.string.pro_feature_battery_history_message)
            FullscreenChartSource.NETWORK_HISTORY -> stringResource(R.string.pro_feature_network_history_message)
            FullscreenChartSource.BATTERY_SESSION -> stringResource(R.string.pro_feature_locked_generic)
        }

    ProFeatureLockedState(
        title = title,
        message = message,
        actionLabel = stringResource(R.string.pro_feature_upgrade_action),
        onAction = onUpgradeToPro,
        modifier = modifier,
    )
}

@Composable
private fun FullscreenChartContent(
    state: FullscreenChartUiState.Success,
    source: FullscreenChartSource,
    modifier: Modifier = Modifier,
) {
    val qualityZones =
        when (source) {
            FullscreenChartSource.BATTERY_HISTORY -> {
                val metric =
                    runCatching { BatteryHistoryMetric.valueOf(state.selectedMetric) }
                        .getOrDefault(BatteryHistoryMetric.LEVEL)
                batteryQualityZones(metric, state.temperatureUnit ?: TemperatureUnit.CELSIUS)
            }

            FullscreenChartSource.NETWORK_HISTORY -> {
                val metric =
                    runCatching { NetworkHistoryMetric.valueOf(state.selectedMetric) }
                        .getOrDefault(NetworkHistoryMetric.SIGNAL)
                signalQualityZones(metric)
            }

            FullscreenChartSource.BATTERY_SESSION -> {
                null
            }
        }

    val title = resolveChartTitle(source, state.selectedMetric)
    val chartAccessibilitySummary =
        rememberChartAccessibilitySummary(
            title = title,
            chartData = state.chartData,
            unit = state.unit,
            decimals = state.tooltipDecimals,
            timeContext = resolveChartTimeContext(source, state.selectedPeriod),
        )

    BoxWithConstraints(modifier = modifier) {
        val availableHeight =
            when (constraints.maxHeight) {
                Constraints.Infinity -> 180.dp
                else -> with(LocalDensity.current) { constraints.maxHeight.toDp() }
            }.coerceAtLeast(1.dp)

        TrendChart(
            data = state.chartData,
            chartHeight = availableHeight,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = chartAccessibilitySummary,
            yLabels = state.yLabels.ifEmpty { null },
            xLabels = state.xLabels.ifEmpty { null },
            showGrid = true,
            qualityZones = qualityZones,
            tooltipFormatter = { index ->
                formatChartTooltip(
                    chartData = state.chartData,
                    chartTimestamps = state.chartTimestamps,
                    index = index,
                    unit = state.unit,
                    decimals = state.tooltipDecimals,
                    timeSkeleton = state.tooltipTimeSkeleton,
                )
            },
            presentation = TrendChartPresentation.Fullscreen,
        )
    }
}

@Composable
private fun FullscreenChartEmptyContent(
    state: FullscreenChartUiState.Empty,
    source: FullscreenChartSource,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.fullscreen_chart_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            Text(
                text = resolveEmptyStateMessage(source, state.selectedPeriod),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun resolveEmptyStateMessage(
    source: FullscreenChartSource,
    period: String,
): String =
    when (source) {
        FullscreenChartSource.BATTERY_HISTORY,
        FullscreenChartSource.NETWORK_HISTORY,
        -> {
            stringResource(
                R.string.fullscreen_chart_empty_history_message,
                resolvePeriodLabel(source, period),
            )
        }

        FullscreenChartSource.BATTERY_SESSION -> {
            stringResource(
                R.string.fullscreen_chart_empty_session_message,
            )
        }
    }

@Composable
private fun resolveChartTimeContext(
    source: FullscreenChartSource,
    period: String,
): String? =
    when (source) {
        FullscreenChartSource.BATTERY_HISTORY,
        FullscreenChartSource.NETWORK_HISTORY,
        -> {
            stringResource(
                R.string.a11y_chart_context_history,
                resolvePeriodLabel(source, period),
            )
        }

        FullscreenChartSource.BATTERY_SESSION -> {
            val window =
                runCatching { SessionGraphWindow.valueOf(period) }
                    .getOrDefault(SessionGraphWindow.ALL)
            if (window == SessionGraphWindow.ALL) {
                stringResource(R.string.a11y_chart_context_session)
            } else {
                stringResource(
                    R.string.a11y_chart_context_session_window,
                    sessionGraphWindowLabel(window),
                )
            }
        }
    }

@Composable
private fun resolveChartTitle(
    source: FullscreenChartSource,
    metric: String,
): String =
    when (source) {
        FullscreenChartSource.BATTERY_HISTORY -> {
            val m =
                runCatching { BatteryHistoryMetric.valueOf(metric) }
                    .getOrDefault(BatteryHistoryMetric.LEVEL)
            stringResource(R.string.fullscreen_chart_title_battery, historyMetricLabel(m))
        }

        FullscreenChartSource.BATTERY_SESSION -> {
            val m =
                runCatching { SessionGraphMetric.valueOf(metric) }
                    .getOrDefault(SessionGraphMetric.CURRENT)
            stringResource(R.string.fullscreen_chart_title_session, sessionGraphMetricLabel(m))
        }

        FullscreenChartSource.NETWORK_HISTORY -> {
            val m =
                runCatching { NetworkHistoryMetric.valueOf(metric) }
                    .getOrDefault(NetworkHistoryMetric.SIGNAL)
            stringResource(R.string.fullscreen_chart_title_network, networkHistoryMetricLabel(m))
        }
    }

@Composable
private fun resolveSourceTitle(source: FullscreenChartSource): String =
    when (source) {
        FullscreenChartSource.BATTERY_HISTORY -> stringResource(R.string.battery_history_title)
        FullscreenChartSource.BATTERY_SESSION -> stringResource(R.string.battery_session_graph_title)
        FullscreenChartSource.NETWORK_HISTORY -> stringResource(R.string.network_section_signal_history)
    }

@Composable
private fun resolveMetricLabel(
    source: FullscreenChartSource,
    metric: String,
): String =
    when (source) {
        FullscreenChartSource.BATTERY_HISTORY -> {
            val m = runCatching { BatteryHistoryMetric.valueOf(metric) }.getOrNull()
            m?.let { historyMetricLabel(it) } ?: metric
        }

        FullscreenChartSource.BATTERY_SESSION -> {
            val m = runCatching { SessionGraphMetric.valueOf(metric) }.getOrNull()
            m?.let { sessionGraphMetricLabel(it) } ?: metric
        }

        FullscreenChartSource.NETWORK_HISTORY -> {
            val m = runCatching { NetworkHistoryMetric.valueOf(metric) }.getOrNull()
            m?.let { networkHistoryMetricLabel(it) } ?: metric
        }
    }

@Composable
private fun resolvePeriodLabel(
    source: FullscreenChartSource,
    period: String,
): String =
    when (source) {
        FullscreenChartSource.BATTERY_HISTORY,
        FullscreenChartSource.NETWORK_HISTORY,
        -> {
            val p =
                runCatching {
                    com.runcheck.domain.model.HistoryPeriod
                        .valueOf(period)
                }.getOrNull()
            p?.let { historyPeriodLabel(it) } ?: period
        }

        FullscreenChartSource.BATTERY_SESSION -> {
            val w = runCatching { SessionGraphWindow.valueOf(period) }.getOrNull()
            w?.let { sessionGraphWindowLabel(it) } ?: period
        }
    }
