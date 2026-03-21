package com.runcheck.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.scoring.HealthScoreCalculator
import com.runcheck.domain.usecase.ChargerSessionTracker
import com.runcheck.domain.usecase.GetBatteryStateUseCase
import com.runcheck.domain.usecase.GetNetworkStateUseCase
import com.runcheck.domain.usecase.GetStorageStateUseCase
import com.runcheck.domain.usecase.GetThermalStateUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.pro.ProManager
import com.runcheck.pro.ProStatus
import com.runcheck.pro.TrialManager
import com.runcheck.ui.common.messageOr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getBatteryState: GetBatteryStateUseCase,
    private val getNetworkState: GetNetworkStateUseCase,
    private val getThermalState: GetThermalStateUseCase,
    private val getStorageState: GetStorageStateUseCase,
    private val proManager: ProManager,
    private val trialManager: TrialManager,
    private val chargerSessionTracker: ChargerSessionTracker,
    private val healthScoreCalculator: HealthScoreCalculator,
    private val manageUserPreferences: ManageUserPreferencesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    // In-memory flag: show expiration modal only once per session
    private var expirationModalShownThisSession = false
    private var lastTrackedSessionStatus: com.runcheck.domain.model.ChargingStatus? = null
    private var lastTrackedSessionAt: Long = 0L

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

    fun dismissWelcomeSheet() {
        viewModelScope.launch {
            trialManager.setWelcomeShown()
            _uiState.value = (_uiState.value as? HomeUiState.Success)
                ?.copy(showWelcomeSheet = false) ?: _uiState.value
        }
    }

    fun dismissDay5Banner() {
        viewModelScope.launch {
            trialManager.setDay5PromptShown()
            _uiState.value = (_uiState.value as? HomeUiState.Success)
                ?.copy(showDay5Banner = false) ?: _uiState.value
        }
    }

    fun dismissExpirationModal() {
        expirationModalShownThisSession = true
        _uiState.value = (_uiState.value as? HomeUiState.Success)
            ?.copy(showExpirationModal = false) ?: _uiState.value
    }

    fun dismissUpgradeCard() {
        viewModelScope.launch {
            trialManager.incrementUpgradeCardDismiss()
            _uiState.value = (_uiState.value as? HomeUiState.Success)
                ?.copy(showUpgradeCard = false) ?: _uiState.value
        }
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

            combine(
                dataFlow,
                proManager.proState,
                manageUserPreferences.observePreferences()
            ) { data, proState, preferences ->
                val showWelcomeSheet = proState.status == ProStatus.TRIAL_ACTIVE &&
                    !trialManager.isWelcomeShown()

                val daysRemaining = proState.trialDaysRemaining
                val trialDaysElapsed = TrialManager.TRIAL_DURATION_DAYS - daysRemaining

                val showDay5Banner = proState.status == ProStatus.TRIAL_ACTIVE &&
                    trialDaysElapsed >= 5 &&
                    !trialManager.isDay5PromptShown()

                val showExpirationModal = proState.status == ProStatus.TRIAL_EXPIRED &&
                    proState.trialStartTimestamp > 0L &&
                    !expirationModalShownThisSession

                val showUpgradeCard = if (proState.status == ProStatus.TRIAL_EXPIRED &&
                    proState.trialStartTimestamp > 0L
                ) {
                    val dismissCount = trialManager.getUpgradeCardDismissCount()
                    val lastDismiss = trialManager.getUpgradeCardLastDismissTimestamp()
                    val daysSinceDismiss = if (lastDismiss > 0L) {
                        TimeUnit.MILLISECONDS.toDays(
                            System.currentTimeMillis() - lastDismiss
                        ).toInt()
                    } else {
                        Int.MAX_VALUE
                    }
                    dismissCount < 3 && (dismissCount == 0 || daysSinceDismiss >= 7)
                } else {
                    false
                }

                HomeUiState.Success(
                    healthScore = data.health,
                    batteryState = data.battery,
                    networkState = data.network,
                    thermalState = data.thermal,
                    storageState = data.storage,
                    temperatureUnit = preferences.temperatureUnit,
                    proState = proState,
                    showWelcomeSheet = showWelcomeSheet,
                    showDay5Banner = showDay5Banner,
                    showExpirationModal = showExpirationModal,
                    showUpgradeCard = showUpgradeCard
                )
            }.sample(DISPLAY_UPDATE_INTERVAL_MS)
                .catch { e ->
                    _uiState.value = HomeUiState.Error(e.messageOr("Unknown error"))
                }.collect { state ->
                maybeTrackChargerSession(state.batteryState)
                _uiState.value = state
            }
        }
    }

    private suspend fun maybeTrackChargerSession(state: BatteryState) {
        val now = System.currentTimeMillis()
        if (lastTrackedSessionStatus != state.chargingStatus ||
            now - lastTrackedSessionAt >= CHARGER_SESSION_TRACK_INTERVAL_MS
        ) {
            chargerSessionTracker.onBatteryState(state, now)
            lastTrackedSessionStatus = state.chargingStatus
            lastTrackedSessionAt = now
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
        private const val CHARGER_SESSION_TRACK_INTERVAL_MS = 15_000L
    }
}
