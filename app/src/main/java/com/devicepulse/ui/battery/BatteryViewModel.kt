package com.devicepulse.ui.battery

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.data.billing.ProStatusRepository
import com.devicepulse.domain.model.HistoryPeriod
import com.devicepulse.domain.usecase.GetBatteryHistoryUseCase
import com.devicepulse.domain.usecase.GetBatteryStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BatteryViewModel @Inject constructor(
    private val getBatteryState: GetBatteryStateUseCase,
    private val getBatteryHistory: GetBatteryHistoryUseCase,
    private val proStatusRepository: ProStatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BatteryUiState>(BatteryUiState.Loading)
    val uiState: StateFlow<BatteryUiState> = _uiState.asStateFlow()

    private var selectedPeriod = HistoryPeriod.DAY

    init {
        loadBatteryData()
    }

    fun refresh() {
        loadBatteryData()
    }

    fun setHistoryPeriod(period: HistoryPeriod) {
        selectedPeriod = period
        loadBatteryData()
    }

    private fun loadBatteryData() {
        viewModelScope.launch {
            combine(
                getBatteryState(),
                getBatteryHistory(selectedPeriod).catch { emit(emptyList()) },
                proStatusRepository.isProUser
            ) { state, history, isPro ->
                BatteryUiState.Success(
                    batteryState = state,
                    history = history,
                    selectedPeriod = selectedPeriod,
                    isPro = isPro
                )
            }.catch { e ->
                Log.e("BatteryVM", "Battery data failed", e)
                _uiState.value = BatteryUiState.Error(e.message ?: "Unknown error")
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
