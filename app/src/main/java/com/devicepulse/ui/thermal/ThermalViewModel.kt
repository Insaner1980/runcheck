package com.devicepulse.ui.thermal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.domain.model.ThermalState
import com.devicepulse.domain.model.ThrottlingEvent
import com.devicepulse.domain.repository.ProStatusProvider
import com.devicepulse.domain.usecase.GetThermalStateUseCase
import com.devicepulse.domain.usecase.GetThrottlingHistoryUseCase
import com.devicepulse.ui.common.messageOr
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
class ThermalViewModel @Inject constructor(
    private val getThermalState: GetThermalStateUseCase,
    private val getThrottlingHistory: GetThrottlingHistoryUseCase,
    private val proStatusProvider: ProStatusProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<ThermalUiState>(ThermalUiState.Loading)
    val uiState: StateFlow<ThermalUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadThermalData()
    }

    fun refresh() {
        loadThermalData()
    }

    private fun loadThermalData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                getThermalState(),
                getThrottlingHistory(),
                proStatusProvider.isProUser
            ) { thermalState: ThermalState, events: List<ThrottlingEvent>, isPro: Boolean ->
                ThermalUiState.Success(
                    thermalState = thermalState,
                    throttlingEvents = events,
                    isPro = isPro
                )
            }
                .catch { e ->
                    _uiState.value = ThermalUiState.Error(e.messageOr("Unknown error"))
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }
}
