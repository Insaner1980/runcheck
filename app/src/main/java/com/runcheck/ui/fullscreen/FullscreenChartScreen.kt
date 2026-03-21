package com.runcheck.ui.fullscreen

import android.content.pm.ActivityInfo
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.ui.chart.BatteryHistoryMetric
import com.runcheck.ui.chart.FullscreenChartSource
import com.runcheck.ui.chart.NetworkHistoryMetric
import com.runcheck.ui.chart.SessionGraphMetric
import com.runcheck.ui.chart.SessionGraphWindow
import com.runcheck.ui.chart.batteryQualityZones
import com.runcheck.ui.chart.historyMetricLabel
import com.runcheck.ui.chart.historyPeriodLabel
import com.runcheck.ui.chart.networkHistoryMetricLabel
import com.runcheck.ui.chart.sessionGraphMetricLabel
import com.runcheck.ui.chart.sessionGraphWindowLabel
import com.runcheck.ui.chart.signalQualityZones
import com.runcheck.ui.common.findActivity
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.common.formatLocalizedDateTime
import com.runcheck.ui.components.TrendChart
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
    onSelectionChanged: (source: String, metric: String, period: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: FullscreenChartViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()

    DisposableEffect(Unit) {
        val previousOrientation = activity?.requestedOrientation
            ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = previousOrientation
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = MaterialTheme.spacing.sm)
    ) {
        when (val state = uiState) {
            is FullscreenChartUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is FullscreenChartUiState.Empty -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back)
                            )
                        }
                        Text(
                            text = stringResource(R.string.fullscreen_chart_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is FullscreenChartUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.fullscreen_chart_error),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
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
                    state = state,
                    source = viewModel.source,
                    onBack = onBack,
                    onMetricChange = {
                        viewModel.setMetric(it)
                        onSelectionChanged(viewModel.source.name, it, state.selectedPeriod)
                    },
                    onPeriodChange = {
                        viewModel.setPeriod(it)
                        onSelectionChanged(viewModel.source.name, state.selectedMetric, it)
                    }
                )
            }
        }
    }
}

@Composable
private fun FullscreenChartContent(
    state: FullscreenChartUiState.Success,
    source: FullscreenChartSource,
    onBack: () -> Unit,
    onMetricChange: (String) -> Unit,
    onPeriodChange: (String) -> Unit
) {
    val qualityZones = when (source) {
        FullscreenChartSource.BATTERY_HISTORY -> {
            val metric = runCatching { BatteryHistoryMetric.valueOf(state.selectedMetric) }
                .getOrDefault(BatteryHistoryMetric.LEVEL)
            batteryQualityZones(metric)
        }
        FullscreenChartSource.NETWORK_HISTORY -> {
            val metric = runCatching { NetworkHistoryMetric.valueOf(state.selectedMetric) }
                .getOrDefault(NetworkHistoryMetric.SIGNAL)
            signalQualityZones(metric)
        }
        FullscreenChartSource.BATTERY_SESSION -> null
    }

    val title = resolveChartTitle(source, state.selectedMetric)

    // Top control row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.navigate_back)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
        ) {
            state.periodOptions.forEach { period ->
                FilterChip(
                    selected = state.selectedPeriod == period,
                    onClick = { onPeriodChange(period) },
                    label = { Text(resolvePeriodLabel(source, period)) }
                )
            }
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
            state.metricOptions.forEach { metric ->
                FilterChip(
                    selected = state.selectedMetric == metric,
                    onClick = { onMetricChange(metric) },
                    label = { Text(resolveMetricLabel(source, metric)) }
                )
            }
        }
    }

    // Chart fills remaining space
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableHeight = with(LocalDensity.current) { constraints.maxHeight.toDp() }
        val chartHeight = (availableHeight - 16.dp).coerceAtLeast(100.dp)

        TrendChart(
            data = state.chartData,
            chartHeight = chartHeight,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = title,
            yLabels = state.yLabels.ifEmpty { null },
            xLabels = state.xLabels.ifEmpty { null },
            showGrid = true,
            qualityZones = qualityZones,
            tooltipFormatter = { index ->
                val v = formatDecimal(state.chartData[index], state.tooltipDecimals)
                val time = formatLocalizedDateTime(state.chartTimestamps[index], state.tooltipTimeSkeleton)
                "$v${state.unit} · $time"
            }
        )
    }
}

@Composable
private fun resolveChartTitle(source: FullscreenChartSource, metric: String): String = when (source) {
    FullscreenChartSource.BATTERY_HISTORY -> {
        val m = runCatching { BatteryHistoryMetric.valueOf(metric) }
            .getOrDefault(BatteryHistoryMetric.LEVEL)
        stringResource(R.string.fullscreen_chart_title_battery, historyMetricLabel(m))
    }
    FullscreenChartSource.BATTERY_SESSION -> {
        val m = runCatching { SessionGraphMetric.valueOf(metric) }
            .getOrDefault(SessionGraphMetric.CURRENT)
        stringResource(R.string.fullscreen_chart_title_session, sessionGraphMetricLabel(m))
    }
    FullscreenChartSource.NETWORK_HISTORY -> {
        val m = runCatching { NetworkHistoryMetric.valueOf(metric) }
            .getOrDefault(NetworkHistoryMetric.SIGNAL)
        stringResource(R.string.fullscreen_chart_title_network, networkHistoryMetricLabel(m))
    }
}

@Composable
private fun resolveMetricLabel(source: FullscreenChartSource, metric: String): String = when (source) {
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
private fun resolvePeriodLabel(source: FullscreenChartSource, period: String): String = when (source) {
    FullscreenChartSource.BATTERY_HISTORY -> {
        val p = runCatching { com.runcheck.domain.model.HistoryPeriod.valueOf(period) }.getOrNull()
        p?.let { historyPeriodLabel(it) } ?: period
    }
    FullscreenChartSource.BATTERY_SESSION -> {
        val w = runCatching { SessionGraphWindow.valueOf(period) }.getOrNull()
        w?.let { sessionGraphWindowLabel(it) } ?: period
    }
    FullscreenChartSource.NETWORK_HISTORY -> {
        val p = runCatching { com.runcheck.domain.model.HistoryPeriod.valueOf(period) }.getOrNull()
        p?.let { historyPeriodLabel(it) } ?: period
    }
}
