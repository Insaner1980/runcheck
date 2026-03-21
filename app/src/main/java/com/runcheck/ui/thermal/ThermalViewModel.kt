package com.runcheck.ui.thermal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.domain.usecase.GetThermalStateUseCase
import com.runcheck.domain.usecase.GetThrottlingHistoryUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
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
    private val savedStateHandle: SavedStateHandle,
    private val getThermalState: GetThermalStateUseCase,
    private val getThrottlingHistory: GetThrottlingHistoryUseCase,
    private val observeProAccess: ObserveProAccessUseCase,
    private val manageUserPreferences: ManageUserPreferencesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ThermalUiState>(ThermalUiState.Loading)
    val uiState: StateFlow<ThermalUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var sessionMinTemp: Float?
        get() = savedStateHandle.get<Float>(KEY_SESSION_MIN_TEMP)
        set(value) { savedStateHandle[KEY_SESSION_MIN_TEMP] = value }
    private var sessionMaxTemp: Float?
        get() = savedStateHandle.get<Float>(KEY_SESSION_MAX_TEMP)
        set(value) { savedStateHandle[KEY_SESSION_MAX_TEMP] = value }

    private companion object {
        const val KEY_SESSION_MIN_TEMP = "thermal_session_min_temp"
        const val KEY_SESSION_MAX_TEMP = "thermal_session_max_temp"
    }

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

    fun dismissInfoCard(id: String) {
        viewModelScope.launch {
            manageUserPreferences.dismissInfoCard(id)
        }
    }

    private fun loadThermalData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                getThermalState(),
                getThrottlingHistory(),
                observeProAccess(),
                manageUserPreferences.observePreferences(),
                manageUserPreferences.observeDismissedInfoCards()
            ) { thermalState: ThermalState, events: List<ThrottlingEvent>, isPro: Boolean, preferences, dismissedCards: Set<String> ->
                val currentTemp = thermalState.batteryTempC
                sessionMinTemp = sessionMinTemp?.coerceAtMost(currentTemp) ?: currentTemp
                sessionMaxTemp = sessionMaxTemp?.coerceAtLeast(currentTemp) ?: currentTemp

                ThermalUiState.Success(
                    thermalState = thermalState,
                    throttlingEvents = events,
                    isPro = isPro,
                    temperatureUnit = preferences.temperatureUnit,
                    sessionMinTemp = sessionMinTemp,
                    sessionMaxTemp = sessionMaxTemp,
                    dismissedInfoCards = dismissedCards
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
