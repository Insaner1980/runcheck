package com.runcheck.ui.battery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.usecase.BatteryScreenInsightsUseCase
import com.runcheck.domain.usecase.ChargerSessionTracker
import com.runcheck.domain.usecase.GetBatteryHistoryUseCase
import com.runcheck.domain.usecase.GetBatteryStateUseCase
import com.runcheck.domain.usecase.GetBatteryStatisticsUseCase
import com.runcheck.domain.usecase.ManageInfoCardDismissalsUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.ui.common.messageOr
import com.runcheck.util.ReleaseSafeLog
import com.runcheck.util.appendLiveValue
import com.runcheck.util.getEnumOrDefault
import com.runcheck.util.putEnum
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
import javax.inject.Inject

@HiltViewModel
class BatteryViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val getBatteryState: GetBatteryStateUseCase,
        private val getBatteryHistory: GetBatteryHistoryUseCase,
        private val getBatteryStatistics: GetBatteryStatisticsUseCase,
        private val observeProAccess: ObserveProAccessUseCase,
        private val chargerSessionTracker: ChargerSessionTracker,
        private val batteryScreenInsights: BatteryScreenInsightsUseCase,
        private val manageUserPreferences: ManageUserPreferencesUseCase,
        private val manageInfoCardDismissals: ManageInfoCardDismissalsUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<BatteryUiState>(BatteryUiState.Loading)
        val uiState: StateFlow<BatteryUiState> = _uiState.asStateFlow()

        private var selectedPeriod: HistoryPeriod
            get() = savedStateHandle.getEnumOrDefault(SELECTED_PERIOD_KEY, HistoryPeriod.DAY)
            set(value) {
                savedStateHandle.putEnum(SELECTED_PERIOD_KEY, value)
            }
        private var loadJob: Job? = null

        // Current stats tracking (in-memory, resets on charging status change)
        private var currentSum: Long = 0L
        private var currentCount: Int = 0
        private var currentMin: Int = Int.MAX_VALUE
        private var currentMax: Int = Int.MIN_VALUE
        private var lastChargingStatus: ChargingStatus? = null
        private var lastTrackedSessionStatus: ChargingStatus? = null
        private var lastTrackedSessionAt: Long = 0L

        // Live chart ring buffers (keep last 60 values ≈ ~1-5 min depending on sample rate)
        private val liveCurrentMa = mutableListOf<Float>()
        private val livePowerW = mutableListOf<Float>()
        private val liveTempC = mutableListOf<Float>()
        private val liveLevel = mutableListOf<Float>()
        private val liveVoltage = mutableListOf<Float>()
        private var lastObservedBatteryState: BatteryState? = null

        // Statistics loaded once per session
        private var cachedStatistics: com.runcheck.domain.usecase.BatteryStatistics? = null
        private var statisticsLoaded = false

        fun startObserving() {
            if (loadJob?.isActive == true) return
            loadBatteryData()
        }

        fun stopObserving() {
            loadJob?.cancel()
            loadJob = null
        }

        fun refresh() {
            statisticsLoaded = false
            loadBatteryData()
        }

        fun setHistoryPeriod(period: HistoryPeriod) {
            selectedPeriod = period
            loadBatteryData()
        }

        private fun resetCurrentStats() {
            currentSum = 0L
            currentCount = 0
            currentMin = Int.MAX_VALUE
            currentMax = Int.MIN_VALUE
        }

        @OptIn(FlowPreview::class)
        @Suppress("TooGenericExceptionCaught")
        private fun loadBatteryData() {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    // Load statistics once (suspend, not a flow)
                    if (!statisticsLoaded) {
                        try {
                            cachedStatistics = getBatteryStatistics()
                        } catch (e: Exception) {
                            ReleaseSafeLog.error("BatteryVM", "Statistics load failed", e)
                        }
                        statisticsLoaded = true
                    }

                    combine(
                        getBatteryState(),
                        getBatteryHistory(selectedPeriod),
                        observeProAccess(),
                        manageUserPreferences.observePreferences(),
                        manageInfoCardDismissals.observeDismissedCardIds(),
                    ) { state, history, isPro, preferences, dismissedCards ->
                        processBatteryUpdate(state, history, isPro, preferences, dismissedCards)
                    }.sample(333L)
                        .catch { e ->
                            ReleaseSafeLog.error("BatteryVM", "Battery data failed", e)
                            _uiState.value = BatteryUiState.Error(e.messageOr("Unknown error"))
                        }.collect { state ->
                            _uiState.value = state
                        }
                }
        }

        private suspend fun processBatteryUpdate(
            state: BatteryState,
            history: List<BatteryReading>,
            isPro: Boolean,
            preferences: UserPreferences,
            dismissedCards: Set<String>,
        ): BatteryUiState.Success {
            if (state != lastObservedBatteryState) {
                if (lastChargingStatus != null && lastChargingStatus != state.chargingStatus) {
                    resetCurrentStats()
                }
                lastChargingStatus = state.chargingStatus

                batteryScreenInsights.updateChargingStatus(state.chargingStatus)
                maybeTrackChargerSession(state)

                if (state.currentMa.confidence != Confidence.UNAVAILABLE) {
                    liveCurrentMa.appendLiveValue(kotlin.math.abs(state.currentMa.value).toFloat())
                    val powerW = kotlin.math.abs(state.currentMa.value / 1000f * state.voltageMv / 1000f)
                    livePowerW.appendLiveValue(powerW)

                    val currentMa = state.currentMa.value
                    currentSum += currentMa
                    currentCount++
                    currentMin = minOf(currentMin, currentMa)
                    currentMax = maxOf(currentMax, currentMa)
                }

                liveTempC.appendLiveValue(state.temperatureC)
                liveLevel.appendLiveValue(state.level.toFloat())
                liveVoltage.appendLiveValue(state.voltageMv / 1000f)
                lastObservedBatteryState = state
            }

            val stats =
                if (state.currentMa.confidence != Confidence.UNAVAILABLE && currentCount >= 2) {
                    CurrentStats(
                        avg = (currentSum / currentCount).toInt(),
                        min = currentMin,
                        max = currentMax,
                        sampleCount = currentCount,
                    )
                } else {
                    null
                }

            val isDischarging =
                state.chargingStatus != ChargingStatus.CHARGING &&
                    state.chargingStatus != ChargingStatus.FULL

            return BatteryUiState.Success(
                batteryState = state,
                history = history,
                selectedPeriod = selectedPeriod,
                isPro = isPro,
                currentStats = stats,
                screenUsage = if (isDischarging) batteryScreenInsights.getScreenUsageStats() else null,
                sleepAnalysis = if (isDischarging) batteryScreenInsights.getSleepAnalysis() else null,
                temperatureUnit = preferences.temperatureUnit,
                statistics = cachedStatistics,
                dismissedInfoCards = dismissedCards,
                showInfoCards = preferences.showInfoCards,
                liveCurrentMa = liveCurrentMa.toList(),
                livePowerW = livePowerW.toList(),
                liveTempC = liveTempC.toList(),
                liveLevel = liveLevel.toList(),
                liveVoltage = liveVoltage.toList(),
            )
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

        fun dismissInfoCard(id: String) {
            viewModelScope.launch { manageInfoCardDismissals.dismissCard(id) }
        }

        companion object {
            private const val CHARGER_SESSION_TRACK_INTERVAL_MS = 15_000L
            private const val SELECTED_PERIOD_KEY = "battery_selected_period"
        }
    }
