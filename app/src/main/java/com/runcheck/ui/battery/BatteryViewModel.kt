package com.runcheck.ui.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.usecase.GetBatteryHistoryUseCase
import com.runcheck.domain.usecase.GetBatteryStateUseCase
import com.runcheck.domain.usecase.GetBatteryStatisticsUseCase
import com.runcheck.service.monitor.ScreenStateTracker
import com.runcheck.ui.common.messageOr
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BatteryViewModel @Inject constructor(
    private val getBatteryState: GetBatteryStateUseCase,
    private val getBatteryHistory: GetBatteryHistoryUseCase,
    private val getBatteryStatistics: GetBatteryStatisticsUseCase,
    private val proStatusProvider: ProStatusProvider,
    private val screenStateTracker: ScreenStateTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow<BatteryUiState>(BatteryUiState.Loading)
    val uiState: StateFlow<BatteryUiState> = _uiState.asStateFlow()

    private var selectedPeriod = HistoryPeriod.DAY
    private var loadJob: Job? = null

    // Current stats tracking (in-memory, resets on charging status change)
    private var currentSum: Long = 0L
    private var currentCount: Int = 0
    private var currentMin: Int = Int.MAX_VALUE
    private var currentMax: Int = Int.MIN_VALUE
    private var lastChargingStatus: ChargingStatus? = null

    // Statistics loaded once per session
    private var cachedStatistics: com.runcheck.domain.usecase.BatteryStatistics? = null
    private var statisticsLoaded = false

    fun startObserving() {
        if (loadJob?.isActive == true) return
        screenStateTracker.start()
        loadBatteryData()
    }

    fun stopObserving() {
        loadJob?.cancel()
        loadJob = null
        screenStateTracker.stop()
    }

    fun refresh() {
        statisticsLoaded = false
        loadBatteryData()
    }

    fun setHistoryPeriod(period: HistoryPeriod) {
        selectedPeriod = period
        loadBatteryData()
    }

    private fun resetCurrentStats() {
        currentSum = 0L
        currentCount = 0
        currentMin = Int.MAX_VALUE
        currentMax = Int.MIN_VALUE
    }

    private fun loadBatteryData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Load statistics once (suspend, not a flow)
            if (!statisticsLoaded) {
                try {
                    cachedStatistics = getBatteryStatistics()
                } catch (e: Exception) {
                    ReleaseSafeLog.error("BatteryVM", "Statistics load failed", e)
                }
                statisticsLoaded = true
            }

            combine(
                getBatteryState(),
                getBatteryHistory(selectedPeriod),
                proStatusProvider.isProUser
            ) { state, history, isPro ->
                // Reset stats if charging status changed
                if (lastChargingStatus != null && lastChargingStatus != state.chargingStatus) {
                    resetCurrentStats()
                }
                lastChargingStatus = state.chargingStatus

                // Accumulate current stats
                val stats = if (state.currentMa.confidence != Confidence.UNAVAILABLE) {
                    val currentMa = state.currentMa.value
                    currentSum += currentMa
                    currentCount++
                    currentMin = minOf(currentMin, currentMa)
                    currentMax = maxOf(currentMax, currentMa)
                    if (currentCount >= 2) {
                        CurrentStats(
                            avg = (currentSum / currentCount).toInt(),
                            min = currentMin,
                            max = currentMax,
                            sampleCount = currentCount
                        )
                    } else null
                } else null

                // Update screen state tracker tick for idle tracking
                screenStateTracker.tick()

                val isDischarging = state.chargingStatus != ChargingStatus.CHARGING &&
                    state.chargingStatus != ChargingStatus.FULL

                BatteryUiState.Success(
                    batteryState = state,
                    history = history,
                    selectedPeriod = selectedPeriod,
                    isPro = isPro,
                    currentStats = stats,
                    screenUsage = if (isDischarging) screenStateTracker.getScreenUsageStats() else null,
                    sleepAnalysis = if (isDischarging) screenStateTracker.getSleepAnalysis() else null,
                    statistics = cachedStatistics
                )
            }.catch { e ->
                ReleaseSafeLog.error("BatteryVM", "Battery data failed", e)
                _uiState.value = BatteryUiState.Error(e.messageOr("Unknown error"))
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        screenStateTracker.stop()
    }
}
