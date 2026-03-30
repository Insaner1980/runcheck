package com.runcheck.ui.network

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.R
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.SpeedTestProgress
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.usecase.FinalizeSpeedTestUseCase
import com.runcheck.domain.usecase.GetMeasuredNetworkStateUseCase
import com.runcheck.domain.usecase.GetNetworkHistoryUseCase
import com.runcheck.domain.usecase.GetSpeedTestHistoryUseCase
import com.runcheck.domain.usecase.ManageInfoCardDismissalsUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.domain.usecase.RunSpeedTestUseCase
import com.runcheck.ui.common.UiText
import com.runcheck.ui.common.messageOrRes
import com.runcheck.util.appendLiveValue
import com.runcheck.util.getEnumOrDefault
import com.runcheck.util.putEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

private const val FREE_HISTORY_LIMIT = 5
private const val PRO_HISTORY_LIMIT = 100

@HiltViewModel
class NetworkViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val getMeasuredNetworkState: GetMeasuredNetworkStateUseCase,
        private val runSpeedTest: RunSpeedTestUseCase,
        private val getSpeedTestHistory: GetSpeedTestHistoryUseCase,
        private val finalizeSpeedTest: FinalizeSpeedTestUseCase,
        private val getNetworkHistory: GetNetworkHistoryUseCase,
        private val manageInfoCardDismissals: ManageInfoCardDismissalsUseCase,
        private val manageUserPreferences: ManageUserPreferencesUseCase,
        private val observeProAccess: ObserveProAccessUseCase,
    ) : ViewModel() {
        private val _networkUiState = MutableStateFlow<NetworkUiState>(NetworkUiState.Loading)
        val networkUiState: StateFlow<NetworkUiState> = _networkUiState.asStateFlow()

        private val liveSignalDbm = mutableListOf<Float>()

        private val _speedTestState = MutableStateFlow(SpeedTestUiState())
        val speedTestState: StateFlow<SpeedTestUiState> = _speedTestState.asStateFlow()
        private var networkJob: Job? = null
        private var historyJob: Job? = null
        private var speedTestJob: Job? = null
        private var selectedHistoryPeriod: HistoryPeriod
            get() = savedStateHandle.getEnumOrDefault(SELECTED_HISTORY_PERIOD_KEY, HistoryPeriod.DAY)
            set(value) {
                savedStateHandle.putEnum(SELECTED_HISTORY_PERIOD_KEY, value)
            }
        private var historyNetworkJob: Job? = null

        fun startObserving() {
            if (networkJob?.isActive != true) {
                loadNetworkData()
            }
            if (historyJob?.isActive != true) {
                loadSpeedTestHistory()
            }
        }

        fun stopObserving() {
            networkJob?.cancel()
            networkJob = null
            historyJob?.cancel()
            historyJob = null
            historyNetworkJob?.cancel()
            historyNetworkJob = null
            // Do NOT cancel speedTestJob here — it must survive config changes (rotation).
            // It runs in viewModelScope and will be cancelled in onCleared().
        }

        fun refresh() {
            loadNetworkData()
        }

        fun setHistoryPeriod(period: HistoryPeriod) {
            selectedHistoryPeriod = period
            loadNetworkData()
        }

        fun startSpeedTest() {
            if (_speedTestState.value.isRunning) return
            proceedWithSpeedTest(allowCellular = false)
        }

        fun confirmCellularSpeedTest() {
            updateSpeedTestState { copy(showCellularWarning = false) }
            proceedWithSpeedTest(allowCellular = true)
        }

        fun dismissCellularWarning() {
            updateSpeedTestState { copy(showCellularWarning = false) }
        }

        private fun proceedWithSpeedTest(allowCellular: Boolean) {
            speedTestJob?.cancel()
            updateSpeedTestState {
                copy(
                    phase = SpeedTestPhase.Ping,
                    isRunning = true,
                    pingMs = 0,
                    jitterMs = null,
                    downloadMbps = 0.0,
                    uploadMbps = 0.0,
                    downloadProgress = 0f,
                    uploadProgress = 0f,
                    historyLoadError = null,
                )
            }

            speedTestJob =
                viewModelScope.launch {
                    try {
                        withTimeout(SPEED_TEST_TIMEOUT_MS) {
                            runSpeedTest(allowCellular = allowCellular)
                                .catch { e ->
                                    updateSpeedTestState {
                                        copy(
                                            phase =
                                                SpeedTestPhase.Failed(
                                                    e.messageOrRes(R.string.speed_test_failed),
                                                ),
                                            isRunning = false,
                                        )
                                    }
                                }.collect { progress ->
                                    when (progress) {
                                        is SpeedTestProgress.CellularConfirmationRequired -> {
                                            updateSpeedTestState {
                                                copy(
                                                    phase = SpeedTestPhase.Idle,
                                                    isRunning = false,
                                                    showCellularWarning = true,
                                                )
                                            }
                                        }

                                        is SpeedTestProgress.PingPhase -> {
                                            updateSpeedTestState {
                                                copy(
                                                    phase = SpeedTestPhase.Ping,
                                                    pingMs = progress.pingMs,
                                                    jitterMs = progress.jitterMs,
                                                )
                                            }
                                        }

                                        is SpeedTestProgress.DownloadPhase -> {
                                            updateSpeedTestState {
                                                copy(
                                                    phase = SpeedTestPhase.Download,
                                                    downloadMbps = progress.currentMbps,
                                                    downloadProgress = progress.progress,
                                                )
                                            }
                                        }

                                        is SpeedTestProgress.UploadPhase -> {
                                            updateSpeedTestState {
                                                copy(
                                                    phase = SpeedTestPhase.Upload,
                                                    uploadMbps = progress.currentMbps,
                                                    uploadProgress = progress.progress,
                                                )
                                            }
                                        }

                                        is SpeedTestProgress.Completed -> {
                                            val result =
                                                SpeedTestResult(
                                                    timestamp = System.currentTimeMillis(),
                                                    downloadMbps = progress.downloadMbps,
                                                    uploadMbps = progress.uploadMbps,
                                                    pingMs = progress.pingMs,
                                                    jitterMs = progress.jitterMs,
                                                    serverName = progress.serverName,
                                                    serverLocation = progress.serverLocation,
                                                    connectionType = progress.connectionInfo.connectionType,
                                                    networkSubtype = progress.connectionInfo.networkSubtype,
                                                    signalDbm = progress.connectionInfo.signalDbm,
                                                )
                                            try {
                                                finalizeSpeedTest(result, FREE_HISTORY_LIMIT)
                                            } catch (e: CancellationException) {
                                                throw e
                                            } catch (error: Exception) {
                                                updateSpeedTestState {
                                                    copy(
                                                        phase =
                                                            SpeedTestPhase.Failed(
                                                                error.messageOrRes(R.string.speed_test_error_generic),
                                                            ),
                                                        isRunning = false,
                                                    )
                                                }
                                                return@collect
                                            }

                                            updateSpeedTestState {
                                                copy(
                                                    phase = SpeedTestPhase.Completed,
                                                    isRunning = false,
                                                    downloadMbps = progress.downloadMbps,
                                                    uploadMbps = progress.uploadMbps,
                                                    pingMs = progress.pingMs,
                                                    jitterMs = progress.jitterMs,
                                                    lastResult = result,
                                                )
                                            }
                                            loadSpeedTestHistory()
                                        }

                                        is SpeedTestProgress.Failed -> {
                                            updateSpeedTestState {
                                                copy(
                                                    phase =
                                                        SpeedTestPhase.Failed(
                                                            UiText.Dynamic(progress.error),
                                                        ),
                                                    isRunning = false,
                                                )
                                            }
                                        }
                                    }
                                }
                        }
                    } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                        updateSpeedTestState {
                            copy(
                                phase =
                                    SpeedTestPhase.Failed(
                                        UiText.Resource(R.string.speed_test_error_timeout),
                                    ),
                                isRunning = false,
                            )
                        }
                    }
                }
        }

        fun dismissInfoCard(id: String) {
            viewModelScope.launch {
                manageInfoCardDismissals.dismissCard(id)
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun loadNetworkData() {
            networkJob?.cancel()
            historyNetworkJob?.cancel()
            if (_networkUiState.value !is NetworkUiState.Success) {
                _networkUiState.value = NetworkUiState.Loading
            }
            networkJob = observeNetworkSnapshot()
            historyNetworkJob = observeNetworkHistory()
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun observeNetworkSnapshot() =
            viewModelScope.launch {
                combine(
                    getMeasuredNetworkState(),
                    manageInfoCardDismissals.observeDismissedCardIds(),
                    manageUserPreferences.observePreferences(),
                    observeProAccess().distinctUntilChanged(),
                ) { state, dismissedCards, preferences, isPro ->
                    NetworkScreenSnapshot(
                        state = state,
                        dismissedCards = dismissedCards,
                        showInfoCards = preferences.showInfoCards,
                        isPro = isPro,
                    )
                }.catch { error -> handleNetworkSnapshotError(error) }
                    .collect(::applyNetworkSnapshot)
            }

        private fun observeNetworkHistory() =
            viewModelScope.launch {
                getNetworkHistory(selectedHistoryPeriod)
                    .catch { error -> handleNetworkHistoryError(error) }
                    .collect(::applyNetworkHistory)
            }

        private fun handleNetworkSnapshotError(error: Throwable) {
            if (_networkUiState.value !is NetworkUiState.Success) {
                _networkUiState.value =
                    NetworkUiState.Error(
                        error.messageOrRes(R.string.common_error_generic),
                    )
            }
        }

        private fun applyNetworkSnapshot(snapshot: NetworkScreenSnapshot) {
            snapshot.state.signalDbm?.let { liveSignalDbm.appendLiveValue(it.toFloat()) }
            _networkUiState.update { current ->
                val existing = current as? NetworkUiState.Success
                NetworkUiState.Success(
                    networkState = snapshot.state,
                    signalHistory = existing?.signalHistory ?: emptyList(),
                    selectedHistoryPeriod = selectedHistoryPeriod,
                    historyLoadError = existing?.historyLoadError,
                    isPro = snapshot.isPro,
                    dismissedInfoCards = snapshot.dismissedCards,
                    showInfoCards = snapshot.showInfoCards,
                    liveSignalDbm = liveSignalDbm.toList(),
                )
            }
        }

        private fun handleNetworkHistoryError(error: Throwable) {
            _networkUiState.update { current ->
                (current as? NetworkUiState.Success)?.copy(
                    historyLoadError = error.messageOrRes(R.string.common_error_generic),
                ) ?: current
            }
        }

        private fun applyNetworkHistory(readings: List<com.runcheck.domain.model.NetworkReading>) {
            _networkUiState.update { current ->
                (current as? NetworkUiState.Success)?.copy(
                    signalHistory = readings,
                    selectedHistoryPeriod = selectedHistoryPeriod,
                    historyLoadError = null,
                ) ?: current
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun loadSpeedTestHistory() {
            historyJob?.cancel()
            historyJob =
                viewModelScope.launch {
                    observeProAccess()
                        .distinctUntilChanged()
                        .flatMapLatest { isPro ->
                            val limit = if (isPro) PRO_HISTORY_LIMIT else FREE_HISTORY_LIMIT
                            getSpeedTestHistory(limit)
                        }.catch { e ->
                            updateSpeedTestState {
                                copy(
                                    historyLoadError = e.messageOrRes(R.string.common_error_generic),
                                )
                            }
                        }.collect { results ->
                            updateSpeedTestState {
                                copy(
                                    historyLoadError = null,
                                    lastResult = results.firstOrNull(),
                                    recentResults = results,
                                )
                            }
                        }
                }
        }

        private fun updateSpeedTestState(transform: SpeedTestUiState.() -> SpeedTestUiState) {
            _speedTestState.update { it.transform() }
        }

        private data class NetworkScreenSnapshot(
            val state: NetworkState,
            val dismissedCards: Set<String>,
            val showInfoCards: Boolean,
            val isPro: Boolean,
        )

        override fun onCleared() {
            stopObserving()
            super.onCleared()
        }

        private companion object {
            private const val SELECTED_HISTORY_PERIOD_KEY = "network_selected_history_period"
            private const val SPEED_TEST_TIMEOUT_MS = 90_000L // 90 seconds total (ping + download + upload)
        }
    }
