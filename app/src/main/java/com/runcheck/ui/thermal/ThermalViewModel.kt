package com.runcheck.ui.thermal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.domain.usecase.GetThermalHistoryUseCase
import com.runcheck.domain.usecase.GetThermalStateUseCase
import com.runcheck.domain.usecase.GetThrottlingHistoryUseCase
import com.runcheck.domain.usecase.ManageInfoCardDismissalsUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.ui.common.UiText
import com.runcheck.ui.common.messageOr
import com.runcheck.util.appendLiveValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThermalViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val getThermalState: GetThermalStateUseCase,
        private val getThrottlingHistory: GetThrottlingHistoryUseCase,
        private val getThermalHistory: GetThermalHistoryUseCase,
        private val observeProAccess: ObserveProAccessUseCase,
        private val manageUserPreferences: ManageUserPreferencesUseCase,
        private val manageInfoCardDismissals: ManageInfoCardDismissalsUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ThermalUiState>(ThermalUiState.Loading)
        val uiState: StateFlow<ThermalUiState> = _uiState.asStateFlow()
        private var loadJob: Job? = null
        private var historyJob: Job? = null
        private var selectedHistoryPeriod: HistoryPeriod
            get() =
                savedStateHandle
                    .get<String>(SELECTED_HISTORY_PERIOD_KEY)
                    ?.let { value -> runCatching { HistoryPeriod.valueOf(value) }.getOrNull() }
                    ?: HistoryPeriod.DAY
            set(value) {
                savedStateHandle[SELECTED_HISTORY_PERIOD_KEY] = value.name
            }
        private var sessionMinTemp: Float?
            get() = savedStateHandle.get<Float>(KEY_SESSION_MIN_TEMP)
            set(value) {
                savedStateHandle[KEY_SESSION_MIN_TEMP] = value
            }
        private var sessionMaxTemp: Float?
            get() = savedStateHandle.get<Float>(KEY_SESSION_MAX_TEMP)
            set(value) {
                savedStateHandle[KEY_SESSION_MAX_TEMP] = value
            }

        // Live chart ring buffers
        private val liveTempC = mutableListOf<Float>()
        private val liveHeadroom = mutableListOf<Float>()
        private var lastObservedThermalState: ThermalState? = null

        private companion object {
            const val KEY_SESSION_MIN_TEMP = "thermal_session_min_temp"
            const val KEY_SESSION_MAX_TEMP = "thermal_session_max_temp"
            const val SELECTED_HISTORY_PERIOD_KEY = "thermal_selected_history_period"
        }

        fun refresh() {
            loadThermalData()
        }

        fun startObserving() {
            if (loadJob?.isActive == true) return
            loadThermalData()
            loadHistory()
        }

        fun stopObserving() {
            loadJob?.cancel()
            loadJob = null
            historyJob?.cancel()
            historyJob = null
        }

        fun setHistoryPeriod(period: HistoryPeriod) {
            selectedHistoryPeriod = period
            loadHistory()
        }

        fun dismissInfoCard(id: String) {
            viewModelScope.launch {
                manageInfoCardDismissals.dismissCard(id)
            }
        }

        private fun loadHistory() {
            historyJob?.cancel()
            historyJob =
                viewModelScope.launch {
                    getThermalHistory(selectedHistoryPeriod)
                        .catch { e ->
                            _uiState.update { current ->
                                (current as? ThermalUiState.Success)?.copy(
                                    historyLoadError = UiText.Dynamic(e.message ?: "Error"),
                                ) ?: current
                            }
                        }.collect { readings ->
                            _uiState.update { current ->
                                (current as? ThermalUiState.Success)?.copy(
                                    thermalHistory = readings,
                                    selectedHistoryPeriod = selectedHistoryPeriod,
                                    historyLoadError = null,
                                ) ?: current
                            }
                        }
                }
        }

        private fun loadThermalData() {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    combine(
                        getThermalState(),
                        getThrottlingHistory(),
                        observeProAccess(),
                        manageUserPreferences.observePreferences(),
                        manageInfoCardDismissals.observeDismissedCardIds(),
                    ) {
                        thermalState: ThermalState,
                        events: List<ThrottlingEvent>,
                        isPro: Boolean,
                        preferences,
                        dismissedCards: Set<String>,
                        ->
                        if (thermalState != lastObservedThermalState) {
                            val currentTemp = thermalState.batteryTempC
                            sessionMinTemp = sessionMinTemp?.coerceAtMost(currentTemp) ?: currentTemp
                            sessionMaxTemp = sessionMaxTemp?.coerceAtLeast(currentTemp) ?: currentTemp
                            liveTempC.appendLiveValue(currentTemp)
                            thermalState.thermalHeadroom?.let { liveHeadroom.appendLiveValue(it) }
                            lastObservedThermalState = thermalState
                        }

                        val currentSuccess = _uiState.value as? ThermalUiState.Success
                        ThermalUiState.Success(
                            thermalState = thermalState,
                            throttlingEvents = events,
                            isPro = isPro,
                            temperatureUnit = preferences.temperatureUnit,
                            sessionMinTemp = sessionMinTemp,
                            sessionMaxTemp = sessionMaxTemp,
                            dismissedInfoCards = dismissedCards,
                            showInfoCards = preferences.showInfoCards,
                            liveTempC = liveTempC.toList(),
                            liveHeadroom = liveHeadroom.toList(),
                            thermalHistory = currentSuccess?.thermalHistory ?: emptyList(),
                            selectedHistoryPeriod = currentSuccess?.selectedHistoryPeriod ?: selectedHistoryPeriod,
                            historyLoadError = currentSuccess?.historyLoadError,
                        )
                    }.catch { e ->
                        _uiState.value = ThermalUiState.Error(e.messageOr("Unknown error"))
                    }.collect { state ->
                        _uiState.value = state
                    }
                }
        }
    }
