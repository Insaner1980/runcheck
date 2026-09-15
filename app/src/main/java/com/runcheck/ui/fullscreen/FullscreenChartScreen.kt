package com.runcheck.ui.fullscreen

import android.content.pm.ActivityInfo
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.ui.chart.FullscreenChartSource
import com.runcheck.ui.chart.SessionGraphWindow
import com.runcheck.ui.chart.batteryQualityZones
import com.runcheck.ui.chart.formatChartTooltip
import com.runcheck.ui.chart.historyMetricLabel
import com.runcheck.ui.chart.historyPeriodLabel
import com.runcheck.ui.chart.networkHistoryMetricLabel
import com.runcheck.ui.chart.networkSignalContextLabels
import com.runcheck.ui.chart.networkSignalHistoryContextLabel
import com.runcheck.ui.chart.rememberChartAccessibilitySummary
import com.runcheck.ui.chart.sessionGraphMetricLabel
import com.runcheck.ui.chart.sessionGraphWindowLabel
import com.runcheck.ui.common.findActivity
import com.runcheck.ui.components.CenteredLoadingState
import com.runcheck.ui.components.ProFeatureLockedState
import com.runcheck.ui.components.TrendChart
import com.runcheck.ui.components.TrendChartPresentation
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.uiTokens

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
    onSelectionChange: (source: String, metric: String, period: String) -> Unit = { _, _, _ -> },
    viewModelProvider: @Composable () -> FullscreenChartViewModel = { hiltViewModel() },
) {
    val viewModel = viewModelProvider()
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val title =
        when (val state = uiState) {
            is FullscreenChartUiState.Success -> resolveChartTitle(state.selection)

            is FullscreenChartUiState.Empty -> resolveChartTitle(state.selection)

            is FullscreenChartUiState.Error -> resolveChartTitle(state.selection)

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
                            selection = sel.selection,
                            viewModel = viewModel,
                            onSelectionChange = {
                                val selection = viewModel.selection
                                onSelectionChange(
                                    selection.source.name,
                                    selection.metricArgument(),
                                    selection.periodArgument(),
                                )
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
                CenteredLoadingState(
                    description = stringResource(R.string.a11y_loading),
                    modifier = contentModifier,
                )
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
    val tokens = MaterialTheme.uiTokens
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
                            .height(tokens.touchTarget),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.close_fullscreen_chart),
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier =
                            Modifier
                                .weight(1f)
                                .semantics { heading() },
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
    selection: FullscreenChartSelection,
    viewModel: FullscreenChartViewModel,
    onSelectionChange: () -> Unit,
) {
    when (selection) {
        is FullscreenChartSelection.BatteryHistory -> {
            FullscreenSelectionChips(
                selection.metric,
                selection.period,
                selection.metricOptions,
                selection.periodOptions,
                {
                    viewModel.setMetric(it)
                    onSelectionChange()
                },
                {
                    viewModel.setPeriod(it)
                    onSelectionChange()
                },
                { historyMetricLabel(it) },
                { historyPeriodLabel(it) },
            )
        }

        is FullscreenChartSelection.BatterySession -> {
            FullscreenSelectionChips(
                selection.metric,
                selection.period,
                selection.metricOptions,
                selection.periodOptions,
                {
                    viewModel.setMetric(it)
                    onSelectionChange()
                },
                {
                    viewModel.setPeriod(it)
                    onSelectionChange()
                },
                { sessionGraphMetricLabel(it) },
                { sessionGraphWindowLabel(it) },
            )
        }

        is FullscreenChartSelection.NetworkHistory -> {
            FullscreenSelectionChips(
                selection.metric,
                selection.period,
                selection.metricOptions,
                selection.periodOptions,
                {
                    viewModel.setMetric(it)
                    onSelectionChange()
                },
                {
                    viewModel.setPeriod(it)
                    onSelectionChange()
                },
                { networkHistoryMetricLabel(it) },
                { historyPeriodLabel(it) },
            )
        }
    }
}

@Composable
private fun <M, P> FullscreenSelectionChips(
    selectedMetric: M,
    selectedPeriod: P,
    metricOptions: List<M>,
    periodOptions: List<P>,
    onMetricChange: (M) -> Unit,
    onPeriodChange: (P) -> Unit,
    metricLabel: @Composable (M) -> String,
    periodLabel: @Composable (P) -> String,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        periodOptions.forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodChange(period) },
                label = { Text(periodLabel(period)) },
            )
        }
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
        metricOptions.forEach { metric ->
            FilterChip(
                selected = selectedMetric == metric,
                onClick = { onMetricChange(metric) },
                label = { Text(metricLabel(metric)) },
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
    modifier: Modifier = Modifier,
) {
    val qualityZones =
        when (val selection = state.selection) {
            is FullscreenChartSelection.BatteryHistory -> {
                batteryQualityZones(selection.metric, state.temperatureUnit ?: TemperatureUnit.CELSIUS)
            }

            is FullscreenChartSelection.BatterySession -> {
                null
            }

            is FullscreenChartSelection.NetworkHistory -> {
                null
            }
        }

    val networkContext = networkSignalHistoryContextLabel(state.networkSignalFamilies)
    val pointContexts = networkSignalContextLabels(state.networkSignalContexts)
    val title = resolveChartTitle(state.selection)
    val chartAccessibilitySummary =
        rememberChartAccessibilitySummary(
            title = listOfNotNull(title, networkContext).joinToString(". "),
            chartData = state.chartData,
            unit = state.unit,
            decimals = state.tooltipDecimals,
            timeContext = resolveChartTimeContext(state.selection),
        )

    val tooltipSeparator = stringResource(R.string.value_separator)
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
            lineBreakIndices = state.lineBreakIndices,
            showIsolatedPoints = state.networkSignalContexts.isNotEmpty(),
            qualityZones = qualityZones,
            tooltipFormatter = { index ->
                formatChartTooltip(
                    chartData = state.chartData,
                    chartTimestamps = state.chartTimestamps,
                    index = index,
                    unit = state.unit,
                    decimals = state.tooltipDecimals,
                    timeSkeleton = state.tooltipTimeSkeleton,
                    separator = tooltipSeparator,
                    pointContext = pointContexts.getOrNull(index),
                )
            },
            presentation = TrendChartPresentation.Fullscreen,
        )
    }
}

@Composable
private fun FullscreenChartEmptyContent(
    state: FullscreenChartUiState.Empty,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.fullscreen_chart_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            Text(
                text = resolveEmptyStateMessage(state.selection),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun resolveEmptyStateMessage(selection: FullscreenChartSelection): String =
    when (selection) {
        is FullscreenChartSelection.BatteryHistory,
        is FullscreenChartSelection.NetworkHistory,
        -> stringResource(R.string.fullscreen_chart_empty_history_message, resolvePeriodLabel(selection))

        is FullscreenChartSelection.BatterySession -> stringResource(R.string.fullscreen_chart_empty_session_message)
    }

@Composable
internal fun resolveChartTimeContext(selection: FullscreenChartSelection): String =
    when (selection) {
        is FullscreenChartSelection.BatteryHistory,
        is FullscreenChartSelection.NetworkHistory,
        -> {
            stringResource(R.string.a11y_chart_context_history, resolvePeriodLabel(selection))
        }

        is FullscreenChartSelection.BatterySession -> {
            if (selection.period == SessionGraphWindow.ALL) {
                stringResource(R.string.a11y_chart_context_session)
            } else {
                stringResource(R.string.a11y_chart_context_session_window, sessionGraphWindowLabel(selection.period))
            }
        }
    }

@Composable
internal fun resolveChartTitle(selection: FullscreenChartSelection): String =
    when (selection) {
        is FullscreenChartSelection.BatteryHistory -> {
            stringResource(R.string.fullscreen_chart_title_battery, historyMetricLabel(selection.metric))
        }

        is FullscreenChartSelection.BatterySession -> {
            stringResource(R.string.fullscreen_chart_title_session, sessionGraphMetricLabel(selection.metric))
        }

        is FullscreenChartSelection.NetworkHistory -> {
            stringResource(R.string.fullscreen_chart_title_network, networkHistoryMetricLabel(selection.metric))
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
internal fun resolvePeriodLabel(selection: FullscreenChartSelection): String =
    when (selection) {
        is FullscreenChartSelection.BatteryHistory -> historyPeriodLabel(selection.period)
        is FullscreenChartSelection.BatterySession -> sessionGraphWindowLabel(selection.period)
        is FullscreenChartSelection.NetworkHistory -> historyPeriodLabel(selection.period)
    }
