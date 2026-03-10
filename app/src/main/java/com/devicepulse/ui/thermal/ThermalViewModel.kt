package com.devicepulse.ui.thermal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.data.billing.ProStatusRepository
import com.devicepulse.data.db.entity.ThrottlingEventEntity
import com.devicepulse.domain.model.ThermalState
import com.devicepulse.domain.usecase.GetThermalStateUseCase
import com.devicepulse.domain.usecase.GetThrottlingHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThermalViewModel @Inject constructor(
    private val getThermalState: GetThermalStateUseCase,
    private val getThrottlingHistory: GetThrottlingHistoryUseCase,
    private val proStatusRepository: ProStatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ThermalUiState>(ThermalUiState.Loading)
    val uiState: StateFlow<ThermalUiState> = _uiState.asStateFlow()

    init {
        loadThermalData()
    }

    fun refresh() {
        loadThermalData()
    }

    private fun loadThermalData() {
        viewModelScope.launch {
            combine(
                getThermalState(),
                getThrottlingHistory(),
                proStatusRepository.isProUser
            ) { thermalState: ThermalState, events: List<ThrottlingEventEntity>, isPro: Boolean ->
                ThermalUiState.Success(
                    thermalState = thermalState,
                    throttlingEvents = events,
                    isPro = isPro
                )
            }
                .catch { e ->
                    _uiState.value = ThermalUiState.Error(e.message ?: "Unknown error")
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }
}
