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
import com.runcheck.ui.chart.ChartRenderModel
import com.runcheck.ui.chart.FullscreenChartSource
import com.runcheck.ui.chart.MAX_FULLSCREEN_CHART_POINTS
import com.runcheck.ui.chart.MAX_FULLSCREEN_SESSION_POINTS
import com.runcheck.ui.chart.NetworkHistoryMetric
import com.runcheck.ui.chart.NetworkSignalContext
import com.runcheck.ui.chart.NetworkSignalFamily
import com.runcheck.ui.chart.SessionGraphMetric
import com.runcheck.ui.chart.SessionGraphWindow
import com.runcheck.ui.chart.buildBatteryHistoryChartModel
import com.runcheck.ui.chart.buildBatterySessionChartModel
import com.runcheck.ui.chart.buildNetworkHistoryChartModel
import com.runcheck.ui.chart.calculateChargingSessionSummary
import com.runcheck.ui.components.ChartXLabel
import com.runcheck.ui.components.ChartYLabel
import com.runcheck.ui.navigation.Screen
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
class FullscreenChartViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val getBatteryHistory: GetBatteryHistoryUseCase,
        private val getBatteryState: GetBatteryStateUseCase,
        private val getNetworkHistory: GetNetworkHistoryUseCase,
        private val isProUser: IsProUserUseCase,
        private val observeProAccess: ObserveProAccessUseCase,
        private val manageUserPreferences: ManageUserPreferencesUseCase,
    ) : ViewModel() {
        var selection: FullscreenChartSelection =
            parseFullscreenChartSelection(
                savedStateHandle[Screen.FullscreenChart.ARG_SOURCE],
                savedStateHandle[Screen.FullscreenChart.ARG_METRIC],
                savedStateHandle[Screen.FullscreenChart.ARG_PERIOD],
            )
            private set
        val source: FullscreenChartSource get() = selection.source
        private var isProUserCached: Boolean = isProUser()
        val isProLocked: Boolean
            get() = fullscreenChartRequiresPro(source) && !isProUserCached

        private val _uiState = MutableStateFlow<FullscreenChartUiState>(FullscreenChartUiState.Loading)
        val uiState: StateFlow<FullscreenChartUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        init {
            persistSelection()
            FullscreenChartSeedStore.take(selection)?.let { seed ->
                _uiState.value = seed
            }
            observeProState()
            loadData()
        }

        fun setMetric(metric: BatteryHistoryMetric) {
            setSelection(requireNotNull(selection as? FullscreenChartSelection.BatteryHistory).copy(metric = metric))
        }

        fun setMetric(metric: SessionGraphMetric) {
            setSelection(requireNotNull(selection as? FullscreenChartSelection.BatterySession).copy(metric = metric))
        }

        fun setMetric(metric: NetworkHistoryMetric) {
            setSelection(requireNotNull(selection as? FullscreenChartSelection.NetworkHistory).copy(metric = metric))
        }

        fun setPeriod(period: HistoryPeriod) {
            val updated =
                when (val current = selection) {
                    is FullscreenChartSelection.BatteryHistory -> current.copy(period = period)
                    is FullscreenChartSelection.NetworkHistory -> current.copy(period = period)
                    is FullscreenChartSelection.BatterySession -> error("History period is not a session window")
                }
            setSelection(updated)
        }

        fun setPeriod(period: SessionGraphWindow) {
            setSelection(requireNotNull(selection as? FullscreenChartSelection.BatterySession).copy(period = period))
        }

        private fun setSelection(value: FullscreenChartSelection) {
            selection = value
            persistSelection()
            loadData()
        }

        private fun persistSelection() {
            savedStateHandle[Screen.FullscreenChart.ARG_SOURCE] = source.name
            savedStateHandle[Screen.FullscreenChart.ARG_METRIC] = selection.metricArgument()
            savedStateHandle[Screen.FullscreenChart.ARG_PERIOD] = selection.periodArgument()
        }

        fun retry() {
            loadData()
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
            loadJob =
                viewModelScope.launch {
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
                        when (val currentSelection = selection) {
                            is FullscreenChartSelection.BatteryHistory -> loadBatteryHistory(currentSelection)
                            is FullscreenChartSelection.BatterySession -> loadBatterySession(currentSelection)
                            is FullscreenChartSelection.NetworkHistory -> loadNetworkHistory(currentSelection)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        ReleaseSafeLog.error("FullscreenChartVM", "Failed to load chart data", e)
                        _uiState.value =
                            FullscreenChartUiState.Error(
                                selection = selection,
                            )
                    }
                }
        }

        private suspend fun loadBatteryHistory(selection: FullscreenChartSelection.BatteryHistory) {
            val (metric, period) = selection

            combine(
                getBatteryHistory(period),
                manageUserPreferences
                    .observePreferences()
                    .map { it.temperatureUnit }
                    .distinctUntilChanged(),
            ) { history, temperatureUnit ->
                buildBatteryHistoryChartModel(
                    history = history,
                    metric = metric,
                    period = period,
                    temperatureUnit = temperatureUnit,
                    maxPoints = MAX_FULLSCREEN_CHART_POINTS,
                )
            }.collect { chartModel ->
                _uiState.value =
                    chartModel.toFullscreenUiState(
                        selection = selection,
                    )
            }
        }

        private suspend fun loadBatterySession(selection: FullscreenChartSelection.BatterySession) {
            val (metric, window) = selection
            val period = HistoryPeriod.DAY

            combine(getBatteryHistory(period), getBatteryState()) { history, batteryState ->
                calculateChargingSessionSummary(
                    history = history,
                    currentLevel = batteryState.level,
                    chargingStatus = batteryState.chargingStatus,
                )
            }.collect { summary ->
                if (summary == null) {
                    _uiState.value =
                        FullscreenChartUiState.Empty(
                            selection = selection,
                        )
                    return@collect
                }

                val chartModel =
                    buildBatterySessionChartModel(
                        summary = summary,
                        metric = metric,
                        window = window,
                        maxPoints = MAX_FULLSCREEN_SESSION_POINTS,
                    )

                _uiState.value =
                    chartModel.toFullscreenUiState(
                        selection = selection,
                    )
            }
        }

        private suspend fun loadNetworkHistory(selection: FullscreenChartSelection.NetworkHistory) {
            val (metric, period) = selection

            getNetworkHistory(period).collect { history ->
                val chartModel =
                    buildNetworkHistoryChartModel(
                        history = history,
                        metric = metric,
                        period = period,
                        maxPoints = MAX_FULLSCREEN_CHART_POINTS,
                    )

                _uiState.value =
                    chartModel.toFullscreenUiState(
                        selection = selection,
                    )
            }
        }
    }

private fun ChartRenderModel.toFullscreenUiState(selection: FullscreenChartSelection): FullscreenChartUiState =
    if (chartData.size < 2) {
        FullscreenChartUiState.Empty(
            selection = selection,
        )
    } else {
        toFullscreenSuccess(
            selection = selection,
        )
    }

internal fun ChartRenderModel.toFullscreenSuccess(
    selection: FullscreenChartSelection,
): FullscreenChartUiState.Success =
    FullscreenChartUiState.Success(
        chartData = chartData,
        chartTimestamps = chartTimestamps,
        lineBreakIndices = lineBreakIndices,
        networkSignalContexts = networkSignalContexts,
        networkSignalFamilies = networkSignalFamilies,
        unit = unit,
        selection = selection,
        yLabels = yLabels,
        xLabels = xLabels,
        tooltipDecimals = tooltipDecimals,
        tooltipTimeSkeleton = tooltipTimeSkeleton,
        temperatureUnit = temperatureUnit,
    )

sealed interface FullscreenChartUiState {
    data object Loading : FullscreenChartUiState

    data object Locked : FullscreenChartUiState

    /** States that carry the user's current metric/period selection and available options. */
    interface HasSelections {
        val selection: FullscreenChartSelection
    }

    data class Empty(
        override val selection: FullscreenChartSelection,
    ) : FullscreenChartUiState,
        HasSelections

    data class Error(
        override val selection: FullscreenChartSelection,
    ) : FullscreenChartUiState,
        HasSelections

    data class Success(
        val chartData: List<Float>,
        val chartTimestamps: List<Long>,
        val lineBreakIndices: Set<Int> = emptySet(),
        val networkSignalContexts: List<NetworkSignalContext> = emptyList(),
        val networkSignalFamilies: Set<NetworkSignalFamily> = emptySet(),
        val unit: String,
        override val selection: FullscreenChartSelection,
        val yLabels: List<ChartYLabel>,
        val xLabels: List<ChartXLabel>,
        val tooltipDecimals: Int = 0,
        val tooltipTimeSkeleton: String = "HmMMMd",
        val temperatureUnit: TemperatureUnit? = null,
    ) : FullscreenChartUiState,
        HasSelections
}
