package com.runcheck.ui.fullscreen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.usecase.GetBatteryHistoryUseCase
import com.runcheck.domain.usecase.GetBatteryStateUseCase
import com.runcheck.domain.usecase.GetNetworkHistoryUseCase
import com.runcheck.ui.chart.BatteryHistoryMetric
import com.runcheck.ui.chart.FullscreenChartSource
import com.runcheck.ui.chart.MAX_FULLSCREEN_CHART_POINTS
import com.runcheck.ui.chart.MAX_FULLSCREEN_SESSION_POINTS
import com.runcheck.ui.chart.NetworkHistoryMetric
import com.runcheck.ui.chart.SessionGraphMetric
import com.runcheck.ui.chart.SessionGraphWindow
import com.runcheck.ui.chart.batteryMetricUnit
import com.runcheck.ui.chart.buildBatteryXLabels
import com.runcheck.ui.chart.buildBatteryYLabels
import com.runcheck.ui.chart.buildNetworkXLabels
import com.runcheck.ui.chart.buildNetworkYLabels
import com.runcheck.ui.chart.buildSessionXLabels
import com.runcheck.ui.chart.calculateChargingSessionSummary
import com.runcheck.ui.chart.chartPointsFor
import com.runcheck.ui.chart.downsamplePairs
import com.runcheck.ui.chart.graphPointsFor
import com.runcheck.ui.chart.networkMetricUnit
import com.runcheck.ui.chart.sessionMetricUnit
import com.runcheck.ui.components.ChartXLabel
import com.runcheck.ui.components.ChartYLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FullscreenChartViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getBatteryHistory: GetBatteryHistoryUseCase,
    private val getBatteryState: GetBatteryStateUseCase,
    private val getNetworkHistory: GetNetworkHistoryUseCase
) : ViewModel() {

    private val sourceArg: String = savedStateHandle["source"] ?: ""

    val source: FullscreenChartSource = runCatching {
        FullscreenChartSource.valueOf(sourceArg)
    }.getOrDefault(FullscreenChartSource.BATTERY_HISTORY)

    private val _uiState = MutableStateFlow<FullscreenChartUiState>(FullscreenChartUiState.Loading)
    val uiState: StateFlow<FullscreenChartUiState> = _uiState.asStateFlow()

    private var currentMetric: String
        get() = savedStateHandle["metric"] ?: ""
        set(value) {
            savedStateHandle["metric"] = value
        }
    private var currentPeriod: String
        get() = savedStateHandle["period"] ?: ""
        set(value) {
            savedStateHandle["period"] = value
        }
    private var loadJob: Job? = null

    init {
        loadData()
    }

    fun setMetric(metric: String) {
        currentMetric = metric
        loadData()
    }

    fun setPeriod(period: String) {
        currentPeriod = period
        loadData()
    }

    fun retry() {
        loadData()
    }

    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Only show loading spinner on initial load — keep current content visible
            // during metric/period changes to avoid flicker
            val current = _uiState.value
            if (current !is FullscreenChartUiState.Success && current !is FullscreenChartUiState.Empty) {
                _uiState.value = FullscreenChartUiState.Loading
            }
            try {
                when (source) {
                    FullscreenChartSource.BATTERY_HISTORY -> loadBatteryHistory()
                    FullscreenChartSource.BATTERY_SESSION -> loadBatterySession()
                    FullscreenChartSource.NETWORK_HISTORY -> loadNetworkHistory()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = FullscreenChartUiState.Error
            }
        }
    }

    private suspend fun loadBatteryHistory() {
        val metric = runCatching { BatteryHistoryMetric.valueOf(currentMetric) }
            .getOrDefault(BatteryHistoryMetric.LEVEL)
        val period = runCatching { HistoryPeriod.valueOf(currentPeriod) }
            .getOrDefault(HistoryPeriod.DAY)

        val history = getBatteryHistory(period).firstOrNull() ?: emptyList()

        val chartPoints = history
            .chartPointsFor(metric)
            .downsamplePairs(MAX_FULLSCREEN_CHART_POINTS)

        val chartData = chartPoints.map { it.second }
        val chartTimestamps = chartPoints.map { it.first }

        val metricOptions = BatteryHistoryMetric.entries.map { it.name }
        val periodOptions = HistoryPeriod.entries.map { it.name }

        if (chartData.size < 2) {
            _uiState.value = FullscreenChartUiState.Empty(
                selectedMetric = metric.name,
                selectedPeriod = period.name,
                metricOptions = metricOptions,
                periodOptions = periodOptions
            )
            return
        }

        val minVal = chartData.min()
        val maxVal = chartData.max()
        val yLabels = buildBatteryYLabels(minVal, maxVal)
        val xLabels = buildBatteryXLabels(chartTimestamps, period)

        _uiState.value = FullscreenChartUiState.Success(
            chartData = chartData,
            chartTimestamps = chartTimestamps,
            unit = batteryMetricUnit(metric),
            selectedMetric = metric.name,
            selectedPeriod = period.name,
            metricOptions = metricOptions,
            periodOptions = periodOptions,
            yLabels = yLabels,
            xLabels = xLabels,
            tooltipDecimals = if (metric == BatteryHistoryMetric.VOLTAGE) 2 else 0
        )
    }

    private suspend fun loadBatterySession() {
        val metric = runCatching { SessionGraphMetric.valueOf(currentMetric) }
            .getOrDefault(SessionGraphMetric.CURRENT)
        val window = runCatching { SessionGraphWindow.valueOf(currentPeriod) }
            .getOrDefault(SessionGraphWindow.ALL)

        val period = HistoryPeriod.DAY
        val history = getBatteryHistory(period).firstOrNull() ?: emptyList()

        val batteryState = getBatteryState().firstOrNull()

        val summary = if (batteryState != null) {
            calculateChargingSessionSummary(
                history = history,
                currentLevel = batteryState.level,
                chargingStatus = batteryState.chargingStatus
            )
        } else null

        val metricOptions = SessionGraphMetric.entries.map { it.name }
        val periodOptions = SessionGraphWindow.entries.map { it.name }

        if (summary == null) {
            _uiState.value = FullscreenChartUiState.Empty(
                selectedMetric = metric.name,
                selectedPeriod = window.name,
                metricOptions = metricOptions,
                periodOptions = periodOptions
            )
            return
        }

        val chartPoints = summary.readings
            .graphPointsFor(metric, window)
            .downsamplePairs(MAX_FULLSCREEN_SESSION_POINTS)

        val chartData = chartPoints.map { it.second }
        val chartTimestamps = chartPoints.map { it.first }

        if (chartData.size < 2) {
            _uiState.value = FullscreenChartUiState.Empty(
                selectedMetric = metric.name,
                selectedPeriod = window.name,
                metricOptions = metricOptions,
                periodOptions = periodOptions
            )
            return
        }

        val minVal = chartData.min()
        val maxVal = chartData.max()
        val yLabels = buildBatteryYLabels(minVal, maxVal)
        val xLabels = buildSessionXLabels(chartTimestamps)

        _uiState.value = FullscreenChartUiState.Success(
            chartData = chartData,
            chartTimestamps = chartTimestamps,
            unit = sessionMetricUnit(metric),
            selectedMetric = metric.name,
            selectedPeriod = window.name,
            metricOptions = metricOptions,
            periodOptions = periodOptions,
            yLabels = yLabels,
            xLabels = xLabels,
            tooltipDecimals = if (metric == SessionGraphMetric.POWER) 1 else 0,
            tooltipTimeSkeleton = "Hm"
        )
    }

    private suspend fun loadNetworkHistory() {
        val metric = runCatching { NetworkHistoryMetric.valueOf(currentMetric) }
            .getOrDefault(NetworkHistoryMetric.SIGNAL)
        val period = runCatching { HistoryPeriod.valueOf(currentPeriod) }
            .getOrDefault(HistoryPeriod.DAY)

        val metricOptions = NetworkHistoryMetric.entries.map { it.name }
        val periodOptions = HistoryPeriod.entries
            .filter { it != HistoryPeriod.SINCE_UNPLUG }
            .map { it.name }

        val history = getNetworkHistory(period).firstOrNull() ?: emptyList()

        val chartPoints = history.mapNotNull { reading ->
            val value = when (metric) {
                NetworkHistoryMetric.SIGNAL -> reading.signalDbm?.toFloat()
                NetworkHistoryMetric.LATENCY -> reading.latencyMs?.toFloat()
            }
            value?.let { reading.timestamp to it }
        }.downsamplePairs(MAX_FULLSCREEN_CHART_POINTS)

        val chartData = chartPoints.map { it.second }
        val chartTimestamps = chartPoints.map { it.first }

        if (chartData.size < 2) {
            _uiState.value = FullscreenChartUiState.Empty(
                selectedMetric = metric.name,
                selectedPeriod = period.name,
                metricOptions = metricOptions,
                periodOptions = periodOptions
            )
            return
        }

        val minVal = chartData.min()
        val maxVal = chartData.max()
        val yLabels = buildNetworkYLabels(minVal, maxVal)
        val xLabels = buildNetworkXLabels(chartTimestamps, period)

        _uiState.value = FullscreenChartUiState.Success(
            chartData = chartData,
            chartTimestamps = chartTimestamps,
            unit = networkMetricUnit(metric),
            selectedMetric = metric.name,
            selectedPeriod = period.name,
            metricOptions = metricOptions,
            periodOptions = periodOptions,
            yLabels = yLabels,
            xLabels = xLabels,
            tooltipDecimals = 0
        )
    }
}

sealed interface FullscreenChartUiState {
    data object Loading : FullscreenChartUiState

    data class Empty(
        val selectedMetric: String,
        val selectedPeriod: String,
        val metricOptions: List<String>,
        val periodOptions: List<String>
    ) : FullscreenChartUiState

    data object Error : FullscreenChartUiState

    data class Success(
        val chartData: List<Float>,
        val chartTimestamps: List<Long>,
        val unit: String,
        val selectedMetric: String,
        val selectedPeriod: String,
        val metricOptions: List<String>,
        val periodOptions: List<String>,
        val yLabels: List<ChartYLabel>,
        val xLabels: List<ChartXLabel>,
        val tooltipDecimals: Int = 0,
        val tooltipTimeSkeleton: String = "HmMMMd"
    ) : FullscreenChartUiState
}
