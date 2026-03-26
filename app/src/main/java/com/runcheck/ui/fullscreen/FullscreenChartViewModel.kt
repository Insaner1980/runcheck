package com.runcheck.ui.fullscreen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.usecase.GetBatteryHistoryUseCase
import com.runcheck.domain.usecase.GetBatteryStateUseCase
import com.runcheck.domain.usecase.GetNetworkHistoryUseCase
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.ui.chart.BatteryHistoryMetric
import com.runcheck.ui.chart.FullscreenChartSource
import com.runcheck.ui.chart.MAX_FULLSCREEN_CHART_POINTS
import com.runcheck.ui.chart.MAX_FULLSCREEN_SESSION_POINTS
import com.runcheck.ui.chart.NetworkHistoryMetric
import com.runcheck.ui.chart.SessionGraphMetric
import com.runcheck.ui.chart.SessionGraphWindow
import com.runcheck.ui.chart.buildBatteryHistoryChartModel
import com.runcheck.ui.chart.buildBatterySessionChartModel
import com.runcheck.ui.chart.buildNetworkHistoryChartModel
import com.runcheck.ui.chart.calculateChargingSessionSummary
import com.runcheck.ui.components.ChartXLabel
import com.runcheck.ui.components.ChartYLabel
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FullscreenChartViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getBatteryHistory: GetBatteryHistoryUseCase,
    private val getBatteryState: GetBatteryStateUseCase,
    private val getNetworkHistory: GetNetworkHistoryUseCase,
    private val isProUser: IsProUserUseCase,
    private val observeProAccess: ObserveProAccessUseCase,
    private val manageUserPreferences: ManageUserPreferencesUseCase
) : ViewModel() {

    private val parsedSource = parseFullscreenChartSource(savedStateHandle["source"])

    val source: FullscreenChartSource = parsedSource ?: FullscreenChartSource.BATTERY_SESSION
    private var isProUserCached: Boolean = isProUser()
    val isProLocked: Boolean
        get() = fullscreenChartRequiresPro(source) && !isProUserCached

    /** Current metric selection, always up-to-date (backed by savedStateHandle). */
    val selectedMetric: String get() = currentMetric

    /** Current period selection, always up-to-date (backed by savedStateHandle). */
    val selectedPeriod: String get() = currentPeriod

    private val _uiState = MutableStateFlow<FullscreenChartUiState>(FullscreenChartUiState.Loading)
    val uiState: StateFlow<FullscreenChartUiState> = _uiState.asStateFlow()

    private var currentMetric: String
        get() = sanitizeFullscreenMetric(source, savedStateHandle["metric"])
        set(value) {
            savedStateHandle["metric"] = sanitizeFullscreenMetric(source, value)
        }
    private var currentPeriod: String
        get() = sanitizeFullscreenPeriod(source, savedStateHandle["period"])
        set(value) {
            savedStateHandle["period"] = sanitizeFullscreenPeriod(source, value)
        }
    private var loadJob: Job? = null

    init {
        savedStateHandle["source"] = source.name
        savedStateHandle["metric"] = sanitizeFullscreenMetric(source, savedStateHandle["metric"])
        savedStateHandle["period"] = sanitizeFullscreenPeriod(source, savedStateHandle["period"])
        FullscreenChartSeedStore.take(source, currentMetric, currentPeriod)?.let { seed ->
            _uiState.value = seed
        }
        observeProState()
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

    private fun metricOptionsForSource(): List<String> = when (source) {
        FullscreenChartSource.BATTERY_HISTORY -> BatteryHistoryMetric.entries.map { it.name }
        FullscreenChartSource.BATTERY_SESSION -> SessionGraphMetric.entries.map { it.name }
        FullscreenChartSource.NETWORK_HISTORY -> NetworkHistoryMetric.entries.map { it.name }
    }

    private fun periodOptionsForSource(): List<String> = when (source) {
        FullscreenChartSource.BATTERY_HISTORY -> HistoryPeriod.entries.map { it.name }
        FullscreenChartSource.BATTERY_SESSION -> SessionGraphWindow.entries.map { it.name }
        FullscreenChartSource.NETWORK_HISTORY -> HistoryPeriod.entries
            .filter { it != HistoryPeriod.SINCE_UNPLUG }
            .map { it.name }
    }

    private fun observeProState() {
        if (!fullscreenChartRequiresPro(source)) return

        viewModelScope.launch {
            observeProAccess()
                .distinctUntilChanged()
                .collect { isPro ->
                    val previous = isProUserCached
                    isProUserCached = isPro
                    if (previous != isPro) {
                        loadData()
                    }
                }
        }
    }

    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (isProLocked) {
                _uiState.value = FullscreenChartUiState.Locked
                return@launch
            }
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
                ReleaseSafeLog.error("FullscreenChartVM", "Failed to load chart data", e)
                _uiState.value = FullscreenChartUiState.Error(
                    selectedMetric = currentMetric,
                    selectedPeriod = currentPeriod,
                    metricOptions = metricOptionsForSource(),
                    periodOptions = periodOptionsForSource()
                )
            }
        }
    }

    private suspend fun loadBatteryHistory() {
        val metric = runCatching { BatteryHistoryMetric.valueOf(currentMetric) }
            .getOrDefault(BatteryHistoryMetric.LEVEL)
        val period = runCatching { HistoryPeriod.valueOf(currentPeriod) }
            .getOrDefault(HistoryPeriod.DAY)
        val metricOptions = BatteryHistoryMetric.entries.map { it.name }
        val periodOptions = HistoryPeriod.entries.map { it.name }

        combine(
            getBatteryHistory(period),
            manageUserPreferences.observePreferences()
                .map { it.temperatureUnit }
                .distinctUntilChanged()
        ) { history, temperatureUnit ->
            buildBatteryHistoryChartModel(
                history = history,
                metric = metric,
                period = period,
                temperatureUnit = temperatureUnit,
                maxPoints = MAX_FULLSCREEN_CHART_POINTS
            )
        }.collect { chartModel ->
            if (chartModel.chartData.size < 2) {
                _uiState.value = FullscreenChartUiState.Empty(
                    selectedMetric = metric.name,
                    selectedPeriod = period.name,
                    metricOptions = metricOptions,
                    periodOptions = periodOptions
                )
            } else {
                _uiState.value = FullscreenChartUiState.Success(
                    chartData = chartModel.chartData,
                    chartTimestamps = chartModel.chartTimestamps,
                    unit = chartModel.unit,
                    selectedMetric = metric.name,
                    selectedPeriod = period.name,
                    metricOptions = metricOptions,
                    periodOptions = periodOptions,
                    yLabels = chartModel.yLabels,
                    xLabels = chartModel.xLabels,
                    tooltipDecimals = chartModel.tooltipDecimals,
                    tooltipTimeSkeleton = chartModel.tooltipTimeSkeleton,
                    temperatureUnit = chartModel.temperatureUnit ?: TemperatureUnit.CELSIUS
                )
            }
        }
    }

    private suspend fun loadBatterySession() {
        val metric = runCatching { SessionGraphMetric.valueOf(currentMetric) }
            .getOrDefault(SessionGraphMetric.CURRENT)
        val window = runCatching { SessionGraphWindow.valueOf(currentPeriod) }
            .getOrDefault(SessionGraphWindow.ALL)

        val period = HistoryPeriod.DAY
        val metricOptions = SessionGraphMetric.entries.map { it.name }
        val periodOptions = SessionGraphWindow.entries.map { it.name }

        combine(getBatteryHistory(period), getBatteryState()) { history, batteryState ->
            calculateChargingSessionSummary(
                history = history,
                currentLevel = batteryState.level,
                chargingStatus = batteryState.chargingStatus
            )
        }.collect { summary ->
            if (summary == null) {
                _uiState.value = FullscreenChartUiState.Empty(
                    selectedMetric = metric.name,
                    selectedPeriod = window.name,
                    metricOptions = metricOptions,
                    periodOptions = periodOptions
                )
                return@collect
            }

            val chartModel = buildBatterySessionChartModel(
                summary = summary,
                metric = metric,
                window = window,
                maxPoints = MAX_FULLSCREEN_SESSION_POINTS
            )

            if (chartModel.chartData.size < 2) {
                _uiState.value = FullscreenChartUiState.Empty(
                    selectedMetric = metric.name,
                    selectedPeriod = window.name,
                    metricOptions = metricOptions,
                    periodOptions = periodOptions
                )
            } else {
                _uiState.value = FullscreenChartUiState.Success(
                    chartData = chartModel.chartData,
                    chartTimestamps = chartModel.chartTimestamps,
                    unit = chartModel.unit,
                    selectedMetric = metric.name,
                    selectedPeriod = window.name,
                    metricOptions = metricOptions,
                    periodOptions = periodOptions,
                    yLabels = chartModel.yLabels,
                    xLabels = chartModel.xLabels,
                    tooltipDecimals = chartModel.tooltipDecimals,
                    tooltipTimeSkeleton = chartModel.tooltipTimeSkeleton
                )
            }
        }
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

        getNetworkHistory(period).collect { history ->
            val chartModel = buildNetworkHistoryChartModel(
                history = history,
                metric = metric,
                period = period,
                maxPoints = MAX_FULLSCREEN_CHART_POINTS
            )

            if (chartModel.chartData.size < 2) {
                _uiState.value = FullscreenChartUiState.Empty(
                    selectedMetric = metric.name,
                    selectedPeriod = period.name,
                    metricOptions = metricOptions,
                    periodOptions = periodOptions
                )
            } else {
                _uiState.value = FullscreenChartUiState.Success(
                    chartData = chartModel.chartData,
                    chartTimestamps = chartModel.chartTimestamps,
                    unit = chartModel.unit,
                    selectedMetric = metric.name,
                    selectedPeriod = period.name,
                    metricOptions = metricOptions,
                    periodOptions = periodOptions,
                    yLabels = chartModel.yLabels,
                    xLabels = chartModel.xLabels,
                    tooltipDecimals = chartModel.tooltipDecimals,
                    tooltipTimeSkeleton = chartModel.tooltipTimeSkeleton
                )
            }
        }
    }
}

sealed interface FullscreenChartUiState {
    data object Loading : FullscreenChartUiState
    data object Locked : FullscreenChartUiState

    /** States that carry the user's current metric/period selection and available options. */
    interface HasSelections {
        val selectedMetric: String
        val selectedPeriod: String
        val metricOptions: List<String>
        val periodOptions: List<String>
    }

    data class Empty(
        override val selectedMetric: String,
        override val selectedPeriod: String,
        override val metricOptions: List<String>,
        override val periodOptions: List<String>
    ) : FullscreenChartUiState, HasSelections

    data class Error(
        override val selectedMetric: String,
        override val selectedPeriod: String,
        override val metricOptions: List<String>,
        override val periodOptions: List<String>
    ) : FullscreenChartUiState, HasSelections

    data class Success(
        val chartData: List<Float>,
        val chartTimestamps: List<Long>,
        val unit: String,
        override val selectedMetric: String,
        override val selectedPeriod: String,
        override val metricOptions: List<String>,
        override val periodOptions: List<String>,
        val yLabels: List<ChartYLabel>,
        val xLabels: List<ChartXLabel>,
        val tooltipDecimals: Int = 0,
        val tooltipTimeSkeleton: String = "HmMMMd",
        val temperatureUnit: TemperatureUnit? = null
    ) : FullscreenChartUiState, HasSelections
}
