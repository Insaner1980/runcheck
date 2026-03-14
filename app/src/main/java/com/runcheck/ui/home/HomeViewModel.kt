package com.runcheck.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.scoring.HealthScoreCalculator
import com.runcheck.domain.usecase.GetBatteryStateUseCase
import com.runcheck.domain.usecase.GetNetworkStateUseCase
import com.runcheck.domain.usecase.GetStorageStateUseCase
import com.runcheck.domain.usecase.GetThermalStateUseCase
import com.runcheck.ui.common.messageOr
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

    fun startObserving() {
        if (loadJob?.isActive == true) return
        loadHome()
    }

    fun stopObserving() {
        loadJob?.cancel()
        loadJob = null
    }

    fun refresh() {
        loadHome()
    }

    @OptIn(FlowPreview::class)
    private fun loadHome() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val dataFlow = combine(
                getBatteryState(),
                getNetworkState(),
                getThermalState(),
                getStorageState()
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

            combine(dataFlow, proStatusProvider.isProUser) { data, isPro ->
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
        private const val DISPLAY_UPDATE_INTERVAL_MS = 333L
    }
}
