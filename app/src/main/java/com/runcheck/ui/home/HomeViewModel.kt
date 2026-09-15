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
import com.runcheck.domain.model.UserPreferences
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
import com.runcheck.pro.ProStateProvider
import com.runcheck.ui.common.UI_STATE_SAMPLE_INTERVAL_MS
import com.runcheck.ui.common.UnseenInsightTracker
import com.runcheck.ui.common.launchUiMutation
import com.runcheck.ui.common.messageOrRes
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
        private val chargerSessionTracker: ChargerSessionTracker,
        private val healthScoreCalculator: HealthScoreCalculator,
        private val manageUserPreferences: ManageUserPreferencesUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
        private var loadJob: Job? = null
        private var refreshIndicatorJob: Job? = null
        private var refreshStartedAtUptimeMillis = 0L
        private val unseenInsightTracker = UnseenInsightTracker()

        fun startObserving() {
            if (loadJob?.isActive == true) return
            loadHome()
        }

        fun stopObserving() {
            loadJob?.cancel()
            loadJob = null
            resetRefreshIndicator()
        }

        private fun resetRefreshIndicator() {
            refreshIndicatorJob?.cancel()
            refreshIndicatorJob = null
            _isRefreshing.value = false
        }

        fun refresh() {
            if (_isRefreshing.value) return

            refreshStartedAtUptimeMillis = SystemClock.uptimeMillis()
            _isRefreshing.value = true
            refreshIndicatorJob?.cancel()
            refreshIndicatorJob =
                viewModelScope.launch {
                    delay(FULL_CHECK_TIMEOUT_MILLIS)
                    _isRefreshing.value = false
                    refreshIndicatorJob = null
                }
            loadHome()
        }

        private fun completeRefresh() {
            if (!_isRefreshing.value) return

            val elapsedMillis = SystemClock.uptimeMillis() - refreshStartedAtUptimeMillis
            val remainingMillis = (MIN_FULL_CHECK_INDICATOR_MILLIS - elapsedMillis).coerceAtLeast(0L)
            refreshIndicatorJob?.cancel()
            if (remainingMillis == 0L) {
                _isRefreshing.value = false
                refreshIndicatorJob = null
                return
            }

            refreshIndicatorJob =
                viewModelScope.launch {
                    delay(remainingMillis)
                    _isRefreshing.value = false
                    refreshIndicatorJob = null
                }
        }

        fun dismissInsight(id: Long) {
            viewModelScope.launchUiMutation(TAG, "dismiss insight") {
                insightRepository.dismiss(id)
            }
        }

        @OptIn(FlowPreview::class)
        @Suppress("TooGenericExceptionCaught")
        private fun loadHome() {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    val preferencesFlow = manageUserPreferences.observePreferences()
                    val freshnessTicker = monitoringFreshnessTicker()
                    val monitoringStaleFlow = observeMonitoringStale(preferencesFlow, freshnessTicker)

                    val dataFlow = observeData(freshnessTicker)

                    val insightFlow = insightRepository.getActiveInsights()
                    val readyProStateFlow = observeReadyProState()

                    combine(
                        dataFlow,
                        insightFlow,
                        readyProStateFlow,
                        preferencesFlow,
                        monitoringStaleFlow,
                    ) { data, activeInsights, proState, preferences, monitoringStale ->
                        val isPro = proState.isPro
                        val visibleActiveInsights = activeInsights.visibleForProAccess(isPro)
                        val visibleInsights =
                            insightHomeRankingPolicy.selectHomeInsights(
                                insights = visibleActiveInsights,
                            )

                        HomeUiState.Success(
                            healthScore = data.health,
                            batteryState = data.battery,
                            networkState = data.network,
                            thermalState = data.thermal,
                            storageState = data.storage,
                            lastUpdatedAtEpochMillis = data.updatedAtEpochMillis,
                            insights = visibleInsights,
                            totalInsightCount = visibleActiveInsights.size,
                            unseenInsightCount = visibleActiveInsights.count { !it.seen },
                            temperatureUnit = preferences.temperatureUnit,
                            monitoringStale = monitoringStale,
                            proState = proState,
                        )
                    }.onEach { state ->
                        try {
                            chargerSessionTracker.onObservedBatteryState(state.batteryState)
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            ReleaseSafeLog.error(TAG, "Charger session tracking failed", error)
                        }
                    }.sample(UI_STATE_SAMPLE_INTERVAL_MS)
                        .catch { e ->
                            if (e is CancellationException) throw e
                            _uiState.value = HomeUiState.Error(e.messageOrRes(R.string.common_error_generic))
                            resetRefreshIndicator()
                        }.collect { state ->
                            _uiState.value = state
                            maybeMarkInsightsSeen(state)
                            completeRefresh()
                        }
                }
        }

        private fun observeData(freshnessTicker: Flow<FreshnessTick>): Flow<DataSnapshot> {
            val speedTestScoreContextFlow =
                combine(
                    getSpeedTestHistory.getLatest(),
                    freshnessTicker,
                ) { speedTest, tick -> SpeedTestScoreContext(speedTest, tick.epochMillis) }

            val liveDataFlow =
                combine(
                    getBatteryState(),
                    getNetworkState(),
                    getThermalState(),
                    getStorageState(),
                ) { battery, network, thermal, storage ->
                    LiveDataSnapshot(
                        battery = battery,
                        network = network,
                        thermal = thermal,
                        storage = storage,
                        updatedAtEpochMillis = System.currentTimeMillis(),
                    )
                }

            return combine(
                liveDataFlow,
                speedTestScoreContextFlow,
            ) { liveData, speedTestContext ->
                DataSnapshot(
                    battery = liveData.battery,
                    network = liveData.network,
                    thermal = liveData.thermal,
                    storage = liveData.storage,
                    health =
                        healthScoreCalculator.calculate(
                            battery = liveData.battery,
                            network = liveData.network,
                            thermal = liveData.thermal,
                            storage = liveData.storage,
                            recentSpeedTest = speedTestContext.speedTest,
                            nowMillis = speedTestContext.nowMillis,
                        ),
                    updatedAtEpochMillis = liveData.updatedAtEpochMillis,
                )
            }
        }

        private fun observeMonitoringStale(
            preferencesFlow: Flow<UserPreferences>,
            freshnessTicker: Flow<FreshnessTick>,
        ): Flow<Boolean> =
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

        private fun observeReadyProState() =
            combine(
                proStateProvider.proState,
                proStateProvider.proAccessReady,
            ) { proState, ready -> proState.takeIf { ready } }
                .filterNotNull()

        private fun maybeMarkInsightsSeen(state: HomeUiState.Success) {
            val unseenIds = unseenInsightTracker.idsToMarkSeen(state.insights) ?: return
            viewModelScope.launchUiMutation(TAG, "mark insights seen") {
                unseenInsightTracker.markSeen(unseenIds, insightRepository::markSeen)
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

        private data class DataSnapshot(
            val battery: BatteryState,
            val network: NetworkState,
            val thermal: ThermalState,
            val storage: StorageState,
            val health: HealthScore,
            val updatedAtEpochMillis: Long,
        )

        private data class LiveDataSnapshot(
            val battery: BatteryState,
            val network: NetworkState,
            val thermal: ThermalState,
            val storage: StorageState,
            val updatedAtEpochMillis: Long,
        )

        private data class SpeedTestScoreContext(
            val speedTest: SpeedTestResult?,
            val nowMillis: Long,
        )

        private data class FreshnessTick(
            val epochMillis: Long,
            val uptimeMillis: Long,
        )

        companion object {
            private const val TAG = "HomeViewModel"
            private const val MIN_FULL_CHECK_INDICATOR_MILLIS = 900L
            private const val FULL_CHECK_TIMEOUT_MILLIS = 12_000L
            private const val MONITORING_STALE_CHECK_INTERVAL_MS = 15_000L
        }
    }
