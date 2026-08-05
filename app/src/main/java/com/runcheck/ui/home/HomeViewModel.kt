package com.runcheck.ui.home

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.R
import com.runcheck.domain.insights.engine.InsightHomeRankingPolicy
import com.runcheck.domain.insights.policy.visibleForProAccess
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.MonitoringFreshnessPolicy
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.repository.InsightRepository
import com.runcheck.domain.repository.MonitoringStatusRepository
import com.runcheck.domain.scoring.HealthScoreCalculator
import com.runcheck.domain.usecase.ChargerSessionTracker
import com.runcheck.domain.usecase.GetBatteryStateUseCase
import com.runcheck.domain.usecase.GetNetworkStateUseCase
import com.runcheck.domain.usecase.GetSpeedTestHistoryUseCase
import com.runcheck.domain.usecase.GetStorageStateUseCase
import com.runcheck.domain.usecase.GetThermalStateUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.pro.ProState
import com.runcheck.pro.ProStateProvider
import com.runcheck.pro.ProStatus
import com.runcheck.pro.TrialManager
import com.runcheck.pro.TrialPresentationState
import com.runcheck.ui.common.messageOrRes
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val getBatteryState: GetBatteryStateUseCase,
        private val getNetworkState: GetNetworkStateUseCase,
        private val getThermalState: GetThermalStateUseCase,
        private val getStorageState: GetStorageStateUseCase,
        private val getSpeedTestHistory: GetSpeedTestHistoryUseCase,
        private val insightRepository: InsightRepository,
        private val insightHomeRankingPolicy: InsightHomeRankingPolicy,
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

        fun dismissExpirationModal(onDismissed: () -> Unit = {}) {
            viewModelScope.launch {
                trialManager.setExpirationModalShown()
                updateSuccessState { copy(showExpirationModal = false) }
                onDismissed()
            }
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
                    val freshnessTicker = monitoringFreshnessTicker()
                    val monitoringStaleFlow =
                        combine(
                            monitoringStatusRepository.observeLastWorkerHeartbeat(),
                            preferencesFlow,
                            freshnessTicker,
                        ) { heartbeat, preferences, tick ->
                            MonitoringFreshnessPolicy.isStale(
                                heartbeat = heartbeat,
                                currentIntervalMinutes = preferences.monitoringInterval.minutes,
                                currentUptimeMillis = tick.uptimeMillis,
                                currentEpochMillis = tick.epochMillis,
                            )
                        }.distinctUntilChanged()

                    val speedTestScoreContextFlow =
                        combine(
                            getSpeedTestHistory.getLatest(),
                            freshnessTicker,
                        ) { speedTest, tick -> SpeedTestScoreContext(speedTest, tick.epochMillis) }

                    val deviceSnapshotFlow =
                        combine(
                            getBatteryState(),
                            getNetworkState(),
                            getThermalState(),
                            getStorageState(),
                        ) { battery, network, thermal, storage ->
                            DeviceSnapshot(
                                battery = battery,
                                network = network,
                                thermal = thermal,
                                storage = storage,
                                measurementTimestampMillis = System.currentTimeMillis(),
                            )
                        }

                    val dataFlow =
                        combine(
                            deviceSnapshotFlow,
                            speedTestScoreContextFlow,
                        ) { device, speedTestContext ->
                            DataSnapshot(
                                battery = device.battery,
                                network = device.network,
                                thermal = device.thermal,
                                storage = device.storage,
                                health =
                                    healthScoreCalculator.calculate(
                                        battery = device.battery,
                                        network = device.network,
                                        thermal = device.thermal,
                                        storage = device.storage,
                                        recentSpeedTest = speedTestContext.speedTest,
                                        nowMillis = speedTestContext.nowMillis,
                                    ),
                                measurementTimestampMillis = device.measurementTimestampMillis,
                            )
                        }

                    val insightFlow =
                        combine(
                            insightRepository.getActiveInsights(),
                            insightRepository.getUnseenCount(),
                        ) { activeInsights, _ -> activeInsights }

                    val readyProStateFlow =
                        combine(
                            proStateProvider.proState,
                            proStateProvider.proAccessReady,
                        ) { proState, ready -> proState.takeIf { ready } }
                            .filterNotNull()

                    val readyProPresentationFlow =
                        combine(
                            readyProStateFlow,
                            trialManager.observePresentationState(),
                        ) { proState, presentationState ->
                            ProPresentationContext(
                                proState = proState,
                                presentationState = presentationState,
                            )
                        }

                    combine(
                        dataFlow,
                        insightFlow,
                        readyProPresentationFlow,
                        preferencesFlow,
                        monitoringStaleFlow,
                    ) { data, activeInsights, proPresentation, preferences, monitoringStale ->
                        val proState = proPresentation.proState
                        val presentationState = proPresentation.presentationState
                        val showWelcomeSheet =
                            proState.status == ProStatus.TRIAL_ACTIVE &&
                                !presentationState.welcomeShown

                        val showDay5Banner =
                            proState.status == ProStatus.TRIAL_ACTIVE &&
                                hasReachedTrialDay(proState.trialStartTimestamp, DAY_5) &&
                                !presentationState.day5PromptShown

                        val showExpirationModal =
                            proState.status == ProStatus.TRIAL_EXPIRED &&
                                proState.trialStartTimestamp > 0L &&
                                !presentationState.expirationModalShown

                        val showUpgradeCard =
                            if (proState.status == ProStatus.TRIAL_EXPIRED &&
                                proState.trialStartTimestamp > 0L
                            ) {
                                val dismissCount = presentationState.upgradeCardDismissCount
                                val lastDismiss = presentationState.upgradeCardLastDismissTimestamp
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

                        val isPro = proState.isPro
                        val visibleActiveInsights = activeInsights.visibleForProAccess(isPro)
                        val visibleInsights =
                            insightHomeRankingPolicy.selectHomeInsights(
                                insights = visibleActiveInsights,
                                limit = MAX_HOME_INSIGHTS,
                            )

                        HomeUiState.Success(
                            healthScore = data.health,
                            batteryState = data.battery,
                            networkState = data.network,
                            thermalState = data.thermal,
                            storageState = data.storage,
                            measurementTimestampMillis = data.measurementTimestampMillis,
                            insights = visibleInsights,
                            totalInsightCount = visibleActiveInsights.size,
                            unseenInsightCount = visibleActiveInsights.count { !it.seen },
                            temperatureUnit = preferences.temperatureUnit,
                            monitoringStale = monitoringStale,
                            proState = proState,
                            showWelcomeSheet = showWelcomeSheet,
                            showDay5Banner = showDay5Banner,
                            showExpirationModal = showExpirationModal,
                            showUpgradeCard = showUpgradeCard,
                        )
                    }.onEach { state ->
                        maybeTrackChargerSession(state.batteryState)
                    }.sample(DISPLAY_UPDATE_INTERVAL_MS)
                        .catch { e ->
                            _uiState.value = HomeUiState.Error(e.messageOrRes(R.string.common_error_generic))
                        }.collect { state ->
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
                insightRepository.markSeen(unseenIds)
            }
        }

        private fun monitoringFreshnessTicker() =
            flow {
                emit(freshnessTick())
                while (true) {
                    delay(MONITORING_STALE_CHECK_INTERVAL_MS)
                    emit(freshnessTick())
                }
            }

        private fun freshnessTick() =
            FreshnessTick(
                epochMillis = System.currentTimeMillis(),
                uptimeMillis = SystemClock.uptimeMillis(),
            )

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
            val measurementTimestampMillis: Long,
        )

        private data class DeviceSnapshot(
            val battery: BatteryState,
            val network: NetworkState,
            val thermal: ThermalState,
            val storage: StorageState,
            val measurementTimestampMillis: Long,
        )

        private data class SpeedTestScoreContext(
            val speedTest: SpeedTestResult?,
            val nowMillis: Long,
        )

        private data class ProPresentationContext(
            val proState: ProState,
            val presentationState: TrialPresentationState,
        )

        private data class FreshnessTick(
            val epochMillis: Long,
            val uptimeMillis: Long,
        )

        companion object {
            private const val MAX_HOME_INSIGHTS = 1
            private const val DAY_5 = 5
            private const val DISPLAY_UPDATE_INTERVAL_MS = 333L
            private const val CHARGER_SESSION_TRACK_INTERVAL_MS = 15_000L
            private const val MONITORING_STALE_CHECK_INTERVAL_MS = 15_000L
        }
    }

internal fun hasReachedTrialDay(
    trialStartTimestamp: Long,
    day: Int,
    now: Long = System.currentTimeMillis(),
): Boolean =
    trialStartTimestamp > 0L &&
        now >= trialStartTimestamp + TimeUnit.DAYS.toMillis(day.toLong())
