package com.runcheck.ui.thermal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.usecase.GetThermalStateUseCase
import com.runcheck.domain.usecase.GetThrottlingHistoryUseCase
import com.runcheck.ui.common.messageOr
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
    private var sessionMinTemp: Float? = null
    private var sessionMaxTemp: Float? = null

    fun refresh() {
        loadThermalData()
    }

    fun startObserving() {
        if (loadJob?.isActive == true) return
        loadThermalData()
    }

    fun stopObserving() {
        loadJob?.cancel()
        loadJob = null
    }

    private fun loadThermalData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                getThermalState(),
                getThrottlingHistory(),
                proStatusProvider.isProUser
            ) { thermalState: ThermalState, events: List<ThrottlingEvent>, isPro: Boolean ->
                val currentTemp = thermalState.batteryTempC
                sessionMinTemp = sessionMinTemp?.coerceAtMost(currentTemp) ?: currentTemp
                sessionMaxTemp = sessionMaxTemp?.coerceAtLeast(currentTemp) ?: currentTemp

                ThermalUiState.Success(
                    thermalState = thermalState,
                    throttlingEvents = events,
                    isPro = isPro,
                    sessionMinTemp = sessionMinTemp,
                    sessionMaxTemp = sessionMaxTemp
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
