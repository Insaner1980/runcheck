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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getMeasuredNetworkState: GetMeasuredNetworkStateUseCase,
    private val runSpeedTest: RunSpeedTestUseCase,
    private val getSpeedTestHistory: GetSpeedTestHistoryUseCase,
    private val finalizeSpeedTest: FinalizeSpeedTestUseCase,
    private val proStatusProvider: ProStatusProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<NetworkUiState>(NetworkUiState.Loading)
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    private val _speedTestState = MutableStateFlow(SpeedTestUiState())
    val speedTestState: StateFlow<SpeedTestUiState> = _speedTestState.asStateFlow()
    private var networkJob: Job? = null
    private var historyJob: Job? = null
    private var speedTestJob: Job? = null

    init {
        loadNetworkData()
        loadSpeedTestHistory()
    }

    fun refresh() {
        loadNetworkData()
    }

    fun startSpeedTest() {
        if (_speedTestState.value.isRunning) return

        speedTestJob?.cancel()
        _speedTestState.update {
            it.copy(
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
                    _speedTestState.update {
                        it.copy(
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
                            _speedTestState.update {
                                it.copy(
                                    phase = SpeedTestPhase.Ping,
                                    pingMs = progress.pingMs,
                                    jitterMs = progress.jitterMs
                                )
                            }
                        }
                        is SpeedTestProgress.DownloadPhase -> {
                            _speedTestState.update {
                                it.copy(
                                    phase = SpeedTestPhase.Download,
                                    downloadMbps = progress.currentMbps,
                                    downloadProgress = progress.progress
                                )
                            }
                        }
                        is SpeedTestProgress.UploadPhase -> {
                            _speedTestState.update {
                                it.copy(
                                    phase = SpeedTestPhase.Upload,
                                    uploadMbps = progress.currentMbps,
                                    uploadProgress = progress.progress
                                )
                            }
                        }
                        is SpeedTestProgress.Completed -> {
                            val networkState = (_uiState.value as? NetworkUiState.Success)?.networkState
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
                            runCatching {
                                finalizeSpeedTest(result, FREE_HISTORY_LIMIT)
                            }.onFailure { error ->
                                _speedTestState.update {
                                    it.copy(
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

                            _speedTestState.update {
                                it.copy(
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
                            _speedTestState.update {
                                it.copy(
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
        networkJob = viewModelScope.launch {
            getMeasuredNetworkState()
                .catch { e ->
                    _uiState.value = NetworkUiState.Error(e.messageOr("Unknown error"))
                }
                .collect { state ->
                    _uiState.value = NetworkUiState.Success(networkState = state)
                }
        }
    }

    private fun loadSpeedTestHistory() {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            val limit = if (proStatusProvider.isPro()) PRO_HISTORY_LIMIT else FREE_HISTORY_LIMIT
            getSpeedTestHistory(limit)
                .catch { e ->
                    _speedTestState.update {
                        it.copy(
                            historyLoadError = e.messageOr(
                                context.getString(R.string.error_generic)
                            )
                        )
                    }
                }
                .collect { results ->
                    _speedTestState.update {
                        it.copy(
                            historyLoadError = null,
                            lastResult = results.firstOrNull(),
                            recentResults = results
                        )
                    }
                }
        }
    }

    companion object {
        private const val FREE_HISTORY_LIMIT = 5
        private const val PRO_HISTORY_LIMIT = 100
    }
}
