package com.devicepulse.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.data.billing.ProStatusRepository
import com.devicepulse.data.network.SpeedTestRepository
import com.devicepulse.data.network.SpeedTestService
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.SpeedTestResult
import com.devicepulse.domain.usecase.GetNetworkStateUseCase
import com.devicepulse.domain.usecase.GetSpeedTestHistoryUseCase
import com.devicepulse.domain.usecase.RunSpeedTestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val getNetworkState: GetNetworkStateUseCase,
    private val runSpeedTest: RunSpeedTestUseCase,
    private val getSpeedTestHistory: GetSpeedTestHistoryUseCase,
    private val speedTestRepository: SpeedTestRepository,
    private val proStatusRepository: ProStatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NetworkUiState>(NetworkUiState.Loading)
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    private val _speedTestState = MutableStateFlow(SpeedTestUiState())
    val speedTestState: StateFlow<SpeedTestUiState> = _speedTestState.asStateFlow()

    init {
        loadNetworkData()
        loadSpeedTestHistory()
    }

    fun refresh() {
        loadNetworkData()
    }

    fun startSpeedTest() {
        if (_speedTestState.value.isRunning) return

        _speedTestState.update {
            it.copy(
                phase = SpeedTestPhase.Ping,
                isRunning = true,
                pingMs = 0,
                jitterMs = 0,
                downloadMbps = 0.0,
                uploadMbps = 0.0,
                downloadProgress = 0f,
                uploadProgress = 0f
            )
        }

        viewModelScope.launch {
            runSpeedTest()
                .catch { e ->
                    _speedTestState.update {
                        it.copy(
                            phase = SpeedTestPhase.Failed(e.message ?: "Speed test failed"),
                            isRunning = false
                        )
                    }
                }
                .collect { progress ->
                    when (progress) {
                        is SpeedTestService.SpeedTestProgress.PingPhase -> {
                            _speedTestState.update {
                                it.copy(
                                    phase = SpeedTestPhase.Download,
                                    pingMs = progress.pingMs,
                                    jitterMs = progress.jitterMs
                                )
                            }
                        }
                        is SpeedTestService.SpeedTestProgress.DownloadPhase -> {
                            _speedTestState.update {
                                it.copy(
                                    phase = SpeedTestPhase.Download,
                                    downloadMbps = progress.currentMbps,
                                    downloadProgress = progress.progress
                                )
                            }
                        }
                        is SpeedTestService.SpeedTestProgress.UploadPhase -> {
                            _speedTestState.update {
                                it.copy(
                                    phase = SpeedTestPhase.Upload,
                                    uploadMbps = progress.currentMbps,
                                    uploadProgress = progress.progress
                                )
                            }
                        }
                        is SpeedTestService.SpeedTestProgress.Completed -> {
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
                            speedTestRepository.saveResult(result)

                            val maxHistory = if (proStatusRepository.isPro()) Int.MAX_VALUE else FREE_HISTORY_LIMIT
                            speedTestRepository.trimResults(maxHistory)

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
                        is SpeedTestService.SpeedTestProgress.Failed -> {
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
        viewModelScope.launch {
            getNetworkState()
                .catch { e ->
                    _uiState.value = NetworkUiState.Error(e.message ?: "Unknown error")
                }
                .collect { state ->
                    _uiState.value = NetworkUiState.Success(networkState = state)
                }
        }
    }

    private fun loadSpeedTestHistory() {
        viewModelScope.launch {
            val limit = if (proStatusRepository.isPro()) Int.MAX_VALUE else FREE_HISTORY_LIMIT
            getSpeedTestHistory(limit)
                .catch { /* ignore */ }
                .collect { results ->
                    _speedTestState.update {
                        it.copy(
                            lastResult = results.firstOrNull(),
                            recentResults = results
                        )
                    }
                }
        }
    }

    fun isCellular(): Boolean {
        val state = (_uiState.value as? NetworkUiState.Success)?.networkState
        return state?.connectionType == ConnectionType.CELLULAR
    }

    companion object {
        private const val FREE_HISTORY_LIMIT = 5
    }
}
