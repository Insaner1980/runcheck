package com.runcheck.ui.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.usecase.GetBatteryHistoryUseCase
import com.runcheck.domain.usecase.GetBatteryStateUseCase
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
    private val proStatusProvider: ProStatusProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<BatteryUiState>(BatteryUiState.Loading)
    val uiState: StateFlow<BatteryUiState> = _uiState.asStateFlow()

    private var selectedPeriod = HistoryPeriod.DAY
    private var loadJob: Job? = null

    fun startObserving() {
        if (loadJob?.isActive == true) return
        loadBatteryData()
    }

    fun stopObserving() {
        loadJob?.cancel()
        loadJob = null
    }

    fun refresh() {
        loadBatteryData()
    }

    fun setHistoryPeriod(period: HistoryPeriod) {
        selectedPeriod = period
        loadBatteryData()
    }

    private fun loadBatteryData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                getBatteryState(),
                getBatteryHistory(selectedPeriod),
                proStatusProvider.isProUser
            ) { state, history, isPro ->
                BatteryUiState.Success(
                    batteryState = state,
                    history = history,
                    selectedPeriod = selectedPeriod,
                    isPro = isPro
                )
            }.catch { e ->
                ReleaseSafeLog.error("BatteryVM", "Battery data failed", e)
                _uiState.value = BatteryUiState.Error(e.messageOr("Unknown error"))
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
