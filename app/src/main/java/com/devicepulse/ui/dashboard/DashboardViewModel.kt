package com.devicepulse.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.domain.usecase.CalculateHealthScoreUseCase
import com.devicepulse.domain.usecase.GetBatteryHistoryUseCase
import com.devicepulse.domain.usecase.GetBatteryStateUseCase
import com.devicepulse.domain.usecase.GetNetworkStateUseCase
import com.devicepulse.domain.usecase.GetStorageStateUseCase
import com.devicepulse.domain.usecase.GetThermalStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val calculateHealthScore: CalculateHealthScoreUseCase,
    private val getBatteryState: GetBatteryStateUseCase,
    private val getNetworkState: GetNetworkStateUseCase,
    private val getThermalState: GetThermalStateUseCase,
    private val getStorageState: GetStorageStateUseCase,
    private val getBatteryHistory: GetBatteryHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun refresh() {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            combine(
                calculateHealthScore(),
                getBatteryState(),
                getNetworkState(),
                getThermalState(),
                getStorageState(),
                getBatteryHistory()
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val healthScore = values[0] as com.devicepulse.domain.model.HealthScore
                val battery = values[1] as com.devicepulse.domain.model.BatteryState
                val network = values[2] as com.devicepulse.domain.model.NetworkState
                val thermal = values[3] as com.devicepulse.domain.model.ThermalState
                val storage = values[4] as com.devicepulse.domain.model.StorageState
                val history = values[5] as List<*>

                val batterySparkline = history
                    .filterIsInstance<com.devicepulse.data.db.entity.BatteryReadingEntity>()
                    .takeLast(SPARKLINE_POINTS)
                    .map { it.level.toFloat() }

                val thermalSparkline = history
                    .filterIsInstance<com.devicepulse.data.db.entity.BatteryReadingEntity>()
                    .takeLast(SPARKLINE_POINTS)
                    .map { it.temperatureC }

                DashboardUiState.Success(
                    healthScore = healthScore,
                    batteryState = battery,
                    networkState = network,
                    thermalState = thermal,
                    storageState = storage,
                    batterySparkline = batterySparkline,
                    thermalSparkline = thermalSparkline
                )
            }.catch { e ->
                _uiState.value = DashboardUiState.Error(
                    e.message ?: "Unknown error"
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    companion object {
        private const val SPARKLINE_POINTS = 20
    }
}
