package com.runcheck.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.repository.InsightRepository
import com.runcheck.domain.repository.MonitoringStatusRepository
import com.runcheck.domain.scoring.HealthScoreCalculator
import com.runcheck.domain.usecase.ChargerSessionTracker
import com.runcheck.domain.usecase.GetBatteryStateUseCase
import com.runcheck.domain.usecase.GetNetworkStateUseCase
import com.runcheck.domain.usecase.GetStorageStateUseCase
import com.runcheck.domain.usecase.GetThermalStateUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.pro.ProStateProvider
import com.runcheck.pro.ProStatus
import com.runcheck.pro.TrialManager
import com.runcheck.ui.common.messageOr
import com.runcheck.util.getBooleanOrDefault
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val getBatteryState: GetBatteryStateUseCase,
        private val getNetworkState: GetNetworkStateUseCase,
        private val getThermalState: GetThermalStateUseCase,
        private val getStorageState: GetStorageStateUseCase,
        private val insightRepository: InsightRepository,
        private val monitoringStatusRepository: MonitoringStatusRepository,
        private val proStateProvider: ProStateProvider,
        private val trialManager: TrialManager,
        private val chargerSessionTracker: ChargerSessionTracker,
        private val healthScoreCalculator: HealthScoreCalculator,
        private val manageUserPreferences: ManageUserPreferencesUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
        private var loadJob: Job? = null
        private var lastSeenInsightIds: Set<Long> = emptySet()

        // Persisted across process death via SavedStateHandle
        private var expirationModalShownThisSession: Boolean
            get() = savedStateHandle.getBooleanOrDefault(KEY_EXPIRATION_MODAL_SHOWN, false)
            set(value) {
                savedStateHandle[KEY_EXPIRATION_MODAL_SHOWN] = value
            }
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

        private inline fun updateSuccessState(transform: HomeUiState.Success.() -> HomeUiState.Success) {
            val current = _uiState.value
            if (current is HomeUiState.Success) {
                _uiState.value = current.transform()
            }
        }

        fun dismissWelcomeSheet() {
            viewModelScope.launch {
                trialManager.setWelcomeShown()
                updateSuccessState { copy(showWelcomeSheet = false) }
            }
        }

        fun dismissDay5Banner() {
            viewModelScope.launch {
                trialManager.setDay5PromptShown()
                updateSuccessState { copy(showDay5Banner = false) }
            }
        }

        fun dismissExpirationModal() {
            expirationModalShownThisSession = true
            updateSuccessState { copy(showExpirationModal = false) }
        }

        fun dismissUpgradeCard() {
            viewModelScope.launch {
                trialManager.incrementUpgradeCardDismiss()
                updateSuccessState { copy(showUpgradeCard = false) }
            }
        }

        fun dismissInsight(id: Long) {
            viewModelScope.launch {
                insightRepository.dismiss(id)
            }
        }

        @OptIn(FlowPreview::class)
        private fun loadHome() {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    val preferencesFlow = manageUserPreferences.observePreferences()
                    val monitoringStaleFlow =
                        combine(
                            monitoringStatusRepository.observeLastWorkerHeartbeatAt(),
                            preferencesFlow,
                            monitoringFreshnessTicker(),
                        ) { lastHeartbeatAt, preferences, now ->
                            isMonitoringStale(
                                lastHeartbeatAt = lastHeartbeatAt,
                                intervalMinutes = preferences.monitoringInterval.minutes,
                                now = now,
                            )
                        }.distinctUntilChanged()

                    val dataFlow =
                        combine(
                            getBatteryState(),
                            getNetworkState(),
                            getThermalState(),
                            getStorageState(),
                        ) { battery, network, thermal, storage ->
                            DataSnapshot(
                                battery = battery,
                                network = network,
                                thermal = thermal,
                                storage = storage,
                                health =
                                    healthScoreCalculator.calculate(
                                        battery = battery,
                                        network = network,
                                        thermal = thermal,
                                        storage = storage,
                                    ),
                            )
                        }

                    val insightFlow =
                        combine(
                            insightRepository.getHomeInsights(limit = MAX_HOME_INSIGHTS),
                            insightRepository.getActiveInsights(),
                            insightRepository.getUnseenCount(),
                        ) { insights, activeInsights, unseenInsightCount ->
                            InsightSnapshot(
                                insights = insights,
                                totalInsightCount = activeInsights.size,
                                unseenInsightCount = unseenInsightCount,
                            )
                        }

                    combine(
                        dataFlow,
                        insightFlow,
                        proStateProvider.proState,
                        preferencesFlow,
                        monitoringStaleFlow,
                    ) { data, insightSnapshot, proState, preferences, monitoringStale ->
                        val showWelcomeSheet =
                            proState.status == ProStatus.TRIAL_ACTIVE &&
                                !trialManager.isWelcomeShown()

                        val daysRemaining = proState.trialDaysRemaining
                        val trialDaysElapsed = TrialManager.TRIAL_DURATION_DAYS - daysRemaining

                        val showDay5Banner =
                            proState.status == ProStatus.TRIAL_ACTIVE &&
                                trialDaysElapsed >= 5 &&
                                !trialManager.isDay5PromptShown()

                        val showExpirationModal =
                            proState.status == ProStatus.TRIAL_EXPIRED &&
                                proState.trialStartTimestamp > 0L &&
                                !expirationModalShownThisSession

                        val showUpgradeCard =
                            if (proState.status == ProStatus.TRIAL_EXPIRED &&
                                proState.trialStartTimestamp > 0L
                            ) {
                                val dismissCount = trialManager.getUpgradeCardDismissCount()
                                val lastDismiss = trialManager.getUpgradeCardLastDismissTimestamp()
                                val daysSinceDismiss =
                                    if (lastDismiss > 0L) {
                                        TimeUnit.MILLISECONDS
                                            .toDays(
                                                System.currentTimeMillis() - lastDismiss,
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
                            insights = insightSnapshot.insights,
                            totalInsightCount = insightSnapshot.totalInsightCount,
                            unseenInsightCount = insightSnapshot.unseenInsightCount,
                            temperatureUnit = preferences.temperatureUnit,
                            monitoringStale = monitoringStale,
                            proState = proState,
                            showWelcomeSheet = showWelcomeSheet,
                            showDay5Banner = showDay5Banner,
                            showExpirationModal = showExpirationModal,
                            showUpgradeCard = showUpgradeCard,
                        )
                    }.sample(DISPLAY_UPDATE_INTERVAL_MS)
                        .catch { e ->
                            _uiState.value = HomeUiState.Error(e.messageOr("Unknown error"))
                        }.collect { state ->
                            maybeTrackChargerSession(state.batteryState)
                            _uiState.value = state
                            maybeMarkInsightsSeen(state)
                        }
                }
        }

        private fun maybeMarkInsightsSeen(state: HomeUiState.Success) {
            val unseenIds =
                state.insights
                    .filterNot { it.seen }
                    .map { it.id }
                    .toSet()
            if (unseenIds.isEmpty()) {
                lastSeenInsightIds = emptySet()
                return
            }
            if (unseenIds == lastSeenInsightIds) return

            lastSeenInsightIds = unseenIds
            viewModelScope.launch {
                insightRepository.markAllSeen()
            }
        }

        private fun monitoringFreshnessTicker() =
            flow {
                emit(System.currentTimeMillis())
                while (true) {
                    delay(MONITORING_STALE_CHECK_INTERVAL_MS)
                    emit(System.currentTimeMillis())
                }
            }

        private fun isMonitoringStale(
            lastHeartbeatAt: Long?,
            intervalMinutes: Int,
            now: Long,
        ): Boolean {
            if (lastHeartbeatAt == null) return false
            val intervalMs = intervalMinutes * 60_000L
            return now - lastHeartbeatAt > intervalMs * STALE_THRESHOLD_MULTIPLIER
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
            val health: HealthScore,
        )

        private data class InsightSnapshot(
            val insights: List<com.runcheck.domain.insights.model.Insight>,
            val totalInsightCount: Int,
            val unseenInsightCount: Int,
        )

        companion object {
            private const val MAX_HOME_INSIGHTS = 3
            private const val DISPLAY_UPDATE_INTERVAL_MS = 333L
            private const val CHARGER_SESSION_TRACK_INTERVAL_MS = 15_000L
            private const val MONITORING_STALE_CHECK_INTERVAL_MS = 15_000L
            private const val STALE_THRESHOLD_MULTIPLIER = 3
            private const val KEY_EXPIRATION_MODAL_SHOWN = "expiration_modal_shown"
        }
    }
