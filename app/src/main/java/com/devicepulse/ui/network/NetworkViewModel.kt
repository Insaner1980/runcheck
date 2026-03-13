package com.devicepulse.ui.network

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.R
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.SpeedTestProgress
import com.devicepulse.domain.model.SpeedTestResult
import com.devicepulse.domain.repository.ProStatusProvider
import com.devicepulse.domain.usecase.FinalizeSpeedTestUseCase
import com.devicepulse.domain.usecase.GetMeasuredNetworkStateUseCase
import com.devicepulse.domain.usecase.GetSpeedTestHistoryUseCase
import com.devicepulse.domain.usecase.RunSpeedTestUseCase
import com.devicepulse.ui.common.messageOr
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val FREE_HISTORY_LIMIT = 5
private const val PRO_HISTORY_LIMIT = 100

@HiltViewModel
class NetworkViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getMeasuredNetworkState: GetMeasuredNetworkStateUseCase,
    private val runSpeedTest: RunSpeedTestUseCase,
    private val getSpeedTestHistory: GetSpeedTestHistoryUseCase,
    private val finalizeSpeedTest: FinalizeSpeedTestUseCase,
    private val proStatusProvider: ProStatusProvider
) : ViewModel() {

    private val _networkUiState = MutableStateFlow<NetworkUiState>(NetworkUiState.Loading)
    val networkUiState: StateFlow<NetworkUiState> = _networkUiState.asStateFlow()

    private val _speedTestState = MutableStateFlow(SpeedTestUiState())
    val speedTestState: StateFlow<SpeedTestUiState> = _speedTestState.asStateFlow()
    private var networkJob: Job? = null
    private var historyJob: Job? = null
    private var speedTestJob: Job? = null

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
    }

    fun refresh() {
        loadNetworkData()
    }

    fun startSpeedTest() {
        if (_speedTestState.value.isRunning) return

        speedTestJob?.cancel()
        updateSpeedTestState {
            copy(
                phase = SpeedTestPhase.Ping,
                isRunning = true,
                pingMs = 0,
                jitterMs = 0,
                downloadMbps = 0.0,
                uploadMbps = 0.0,
                downloadProgress = 0f,
                uploadProgress = 0f,
                historyLoadError = null
            )
        }

        speedTestJob = viewModelScope.launch {
            runSpeedTest()
                .catch { e ->
                    updateSpeedTestState {
                        copy(
                            phase = SpeedTestPhase.Failed(
                                e.messageOr(context.getString(R.string.speed_test_failed))
                            ),
                            isRunning = false
                        )
                    }
                }
                .collect { progress ->
                    when (progress) {
                        is SpeedTestProgress.PingPhase -> {
                            updateSpeedTestState {
                                copy(
                                    phase = SpeedTestPhase.Ping,
                                    pingMs = progress.pingMs,
                                    jitterMs = progress.jitterMs
                                )
                            }
                        }
                        is SpeedTestProgress.DownloadPhase -> {
                            updateSpeedTestState {
                                copy(
                                    phase = SpeedTestPhase.Download,
                                    downloadMbps = progress.currentMbps,
                                    downloadProgress = progress.progress
                                )
                            }
                        }
                        is SpeedTestProgress.UploadPhase -> {
                            updateSpeedTestState {
                                copy(
                                    phase = SpeedTestPhase.Upload,
                                    uploadMbps = progress.currentMbps,
                                    uploadProgress = progress.progress
                                )
                            }
                        }
                        is SpeedTestProgress.Completed -> {
                            val networkState = (_networkUiState.value as? NetworkUiState.Success)?.networkState
                            val result = SpeedTestResult(
                                timestamp = System.currentTimeMillis(),
                                downloadMbps = progress.downloadMbps,
                                uploadMbps = progress.uploadMbps,
                                pingMs = progress.pingMs,
                                jitterMs = progress.jitterMs,
                                serverName = progress.serverName,
                                serverLocation = progress.serverLocation,
                                connectionType = networkState?.connectionType ?: ConnectionType.NONE,
                                networkSubtype = networkState?.networkSubtype,
                                signalDbm = networkState?.signalDbm
                            )
                            try {
                                finalizeSpeedTest(result, FREE_HISTORY_LIMIT)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (error: Exception) {
                                updateSpeedTestState {
                                    copy(
                                        phase = SpeedTestPhase.Failed(
                                            error.messageOr(
                                                context.getString(R.string.speed_test_error_generic)
                                            )
                                        ),
                                        isRunning = false
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
                                    lastResult = result
                                )
                            }
                            loadSpeedTestHistory()
                        }
                        is SpeedTestProgress.Failed -> {
                            updateSpeedTestState {
                                copy(
                                    phase = SpeedTestPhase.Failed(progress.error),
                                    isRunning = false
                                )
                            }
                        }
                    }
                }
        }
    }

    private fun loadNetworkData() {
        networkJob?.cancel()
        if (_networkUiState.value !is NetworkUiState.Success) {
            _networkUiState.value = NetworkUiState.Loading
        }
        networkJob = viewModelScope.launch {
            getMeasuredNetworkState()
                .catch { e ->
                    if (_networkUiState.value !is NetworkUiState.Success) {
                        _networkUiState.value = NetworkUiState.Error(
                            e.messageOr(context.getString(R.string.common_error_generic))
                        )
                    }
                }
                .collect { state ->
                    _networkUiState.value = NetworkUiState.Success(networkState = state)
                }
        }
    }

    private fun loadSpeedTestHistory() {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            val limit = if (proStatusProvider.isPro()) PRO_HISTORY_LIMIT else FREE_HISTORY_LIMIT
            getSpeedTestHistory(limit)
                .catch { e ->
                    updateSpeedTestState {
                        copy(
                            historyLoadError = e.messageOr(
                                context.getString(R.string.common_error_generic)
                            )
                        )
                    }
                }
                .collect { results ->
                    updateSpeedTestState {
                        copy(
                            historyLoadError = null,
                            lastResult = results.firstOrNull(),
                            recentResults = results
                        )
                    }
                }
        }
    }

    private fun updateSpeedTestState(transform: SpeedTestUiState.() -> SpeedTestUiState) {
        _speedTestState.update { it.transform() }
    }
}
