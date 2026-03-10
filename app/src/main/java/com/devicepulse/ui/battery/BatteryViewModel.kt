package com.devicepulse.ui.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val getBatteryHistory: GetBatteryHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<BatteryUiState>(BatteryUiState.Loading)
    val uiState: StateFlow<BatteryUiState> = _uiState.asStateFlow()

    init {
        loadBatteryData()
    }

    fun refresh() {
        loadBatteryData()
    }

    private fun loadBatteryData() {
        viewModelScope.launch {
            combine(
                getBatteryState(),
                getBatteryHistory()
            ) { state, history ->
                BatteryUiState.Success(
                    batteryState = state,
                    history = history
                )
            }.catch { e ->
                _uiState.value = BatteryUiState.Error(e.message ?: "Unknown error")
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
