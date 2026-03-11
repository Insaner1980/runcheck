package com.devicepulse.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.domain.repository.ProStatusProvider
import com.devicepulse.domain.model.BatteryHealth
import com.devicepulse.domain.model.BatteryState
import com.devicepulse.domain.model.ChargingStatus
import com.devicepulse.domain.model.Confidence
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.HealthScore
import com.devicepulse.domain.model.HealthStatus
import com.devicepulse.domain.model.MeasuredValue
import com.devicepulse.domain.model.NetworkState
import com.devicepulse.domain.model.PlugType
import com.devicepulse.domain.model.SignalQuality
import com.devicepulse.domain.model.StorageState
import com.devicepulse.domain.model.ThermalState
import com.devicepulse.domain.model.ThermalStatus
import com.devicepulse.domain.usecase.CalculateHealthScoreUseCase
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
class HomeViewModel @Inject constructor(
    private val calculateHealthScore: CalculateHealthScoreUseCase,
    private val getBatteryState: GetBatteryStateUseCase,
    private val getNetworkState: GetNetworkStateUseCase,
    private val getThermalState: GetThermalStateUseCase,
    private val getStorageState: GetStorageStateUseCase,
    private val proStatusProvider: ProStatusProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun refresh() {
        loadHome()
    }

    private fun loadHome() {
        viewModelScope.launch {
            val batteryFlow = getBatteryState()
                .catch { e -> Log.e(TAG, "Battery flow failed", e); emit(DEFAULT_BATTERY) }
            val networkFlow = getNetworkState()
                .catch { e -> Log.e(TAG, "Network flow failed", e); emit(DEFAULT_NETWORK) }
            val thermalFlow = getThermalState()
                .catch { e -> Log.e(TAG, "Thermal flow failed", e); emit(DEFAULT_THERMAL) }
            val storageFlow = getStorageState()
                .catch { e -> Log.e(TAG, "Storage flow failed", e); emit(DEFAULT_STORAGE) }
            val healthFlow = calculateHealthScore()
                .catch { emit(DEFAULT_HEALTH) }
            val proFlow = proStatusProvider.isProUser

            val dataFlow = combine(
                batteryFlow,
                networkFlow,
                thermalFlow,
                storageFlow,
                healthFlow
            ) { battery, network, thermal, storage, health ->
                DataSnapshot(battery, network, thermal, storage, health)
            }

            combine(dataFlow, proFlow) { data, isPro ->
                HomeUiState.Success(
                    healthScore = data.health,
                    batteryState = data.battery,
                    networkState = data.network,
                    thermalState = data.thermal,
                    storageState = data.storage,
                    isPro = isPro
                )
            }.catch { e ->
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private data class DataSnapshot(
        val battery: BatteryState,
        val network: NetworkState,
        val thermal: ThermalState,
        val storage: StorageState,
        val health: HealthScore
    )

    companion object {
        private const val TAG = "HomeVM"

        private val DEFAULT_BATTERY = BatteryState(
            level = 0,
            voltageMv = 0,
            temperatureC = 0f,
            currentMa = MeasuredValue(0, Confidence.UNAVAILABLE),
            chargingStatus = ChargingStatus.NOT_CHARGING,
            plugType = PlugType.NONE,
            health = BatteryHealth.UNKNOWN,
            technology = "Unknown"
        )

        private val DEFAULT_NETWORK = NetworkState(
            connectionType = ConnectionType.NONE,
            signalDbm = null,
            signalQuality = SignalQuality.NO_SIGNAL
        )

        private val DEFAULT_THERMAL = ThermalState(
            batteryTempC = 0f,
            cpuTempC = null,
            thermalStatus = ThermalStatus.NONE,
            isThrottling = false
        )

        private val DEFAULT_STORAGE = StorageState(
            totalBytes = 0,
            availableBytes = 0,
            usedBytes = 0,
            usagePercent = 0f
        )

        private val DEFAULT_HEALTH = HealthScore(
            overallScore = 50,
            batteryScore = 50,
            networkScore = 50,
            thermalScore = 50,
            storageScore = 50,
            status = HealthStatus.FAIR
        )
    }
}
