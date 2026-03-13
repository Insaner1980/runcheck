package com.devicepulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.domain.repository.ProStatusProvider
import com.devicepulse.domain.model.BatteryHealth
import com.devicepulse.domain.model.BatteryState
import com.devicepulse.domain.model.ChargingStatus
import com.devicepulse.domain.model.Confidence
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.HealthScore
import com.devicepulse.domain.model.MeasuredValue
import com.devicepulse.domain.model.NetworkState
import com.devicepulse.domain.model.PlugType
import com.devicepulse.domain.model.SignalQuality
import com.devicepulse.domain.model.StorageState
import com.devicepulse.domain.model.ThermalState
import com.devicepulse.domain.model.ThermalStatus
import com.devicepulse.domain.scoring.HealthScoreCalculator
import com.devicepulse.domain.usecase.GetBatteryStateUseCase
import com.devicepulse.domain.usecase.GetNetworkStateUseCase
import com.devicepulse.domain.usecase.GetStorageStateUseCase
import com.devicepulse.domain.usecase.GetThermalStateUseCase
import com.devicepulse.ui.common.messageOr
import com.devicepulse.util.ReleaseSafeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getBatteryState: GetBatteryStateUseCase,
    private val getNetworkState: GetNetworkStateUseCase,
    private val getThermalState: GetThermalStateUseCase,
    private val getStorageState: GetStorageStateUseCase,
    private val proStatusProvider: ProStatusProvider,
    private val healthScoreCalculator: HealthScoreCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadHome()
    }

    fun refresh() {
        loadHome()
    }

    @OptIn(FlowPreview::class)
    private fun loadHome() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val batteryFlow = getBatteryState()
                .catch { e -> ReleaseSafeLog.error(TAG, "Battery flow failed", e); emit(DEFAULT_BATTERY) }
            val networkFlow = getNetworkState()
                .catch { e -> ReleaseSafeLog.error(TAG, "Network flow failed", e); emit(DEFAULT_NETWORK) }
            val thermalFlow = getThermalState()
                .catch { e -> ReleaseSafeLog.error(TAG, "Thermal flow failed", e); emit(DEFAULT_THERMAL) }
            val storageFlow = getStorageState()
                .catch { e -> ReleaseSafeLog.error(TAG, "Storage flow failed", e); emit(DEFAULT_STORAGE) }
            val proFlow = proStatusProvider.isProUser

            val dataFlow = combine(
                batteryFlow,
                networkFlow,
                thermalFlow,
                storageFlow
            ) { battery, network, thermal, storage ->
                DataSnapshot(
                    battery = battery,
                    network = network,
                    thermal = thermal,
                    storage = storage,
                    health = healthScoreCalculator.calculate(
                        battery = battery,
                        network = network,
                        thermal = thermal,
                        storage = storage
                    )
                )
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
            }.sample(DISPLAY_UPDATE_INTERVAL_MS)
                .conflate()
                .catch { e ->
                _uiState.value = HomeUiState.Error(e.messageOr("Unknown error"))
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
        private const val DISPLAY_UPDATE_INTERVAL_MS = 333L

        private val DEFAULT_BATTERY = BatteryState(
            level = 0,
            voltageMv = 0,
            temperatureC = 0f,
            currentMa = MeasuredValue(0, Confidence.UNAVAILABLE),
            chargingStatus = ChargingStatus.NOT_CHARGING,
            plugType = PlugType.NONE,
            health = BatteryHealth.UNKNOWN,
            technology = ""
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
    }
}
