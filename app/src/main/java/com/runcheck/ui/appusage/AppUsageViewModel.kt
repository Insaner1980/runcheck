package com.runcheck.ui.appusage

import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.usecase.GetAppBatteryUsageUseCase
import com.runcheck.domain.usecase.GetAppBatteryUsageSummaryUseCase
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.domain.usecase.RefreshAppUsageSnapshotUseCase
import com.runcheck.ui.common.messageOr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val APP_USAGE_LOOKBACK_MS = 24 * 60 * 60 * 1000L

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AppUsageViewModel @Inject constructor(
    private val getAppBatteryUsage: GetAppBatteryUsageUseCase,
    private val getAppBatteryUsageSummary: GetAppBatteryUsageSummaryUseCase,
    private val refreshAppUsageSnapshot: RefreshAppUsageSnapshotUseCase,
    private val observeProAccess: ObserveProAccessUseCase,
    private val isProUser: IsProUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppUsageUiState>(AppUsageUiState.Loading)
    val uiState: StateFlow<AppUsageUiState> = _uiState.asStateFlow()
    private val pagingEnabled = MutableStateFlow(false)
    val pagedApps: Flow<PagingData<com.runcheck.domain.model.AppBatteryUsage>> = pagingEnabled
        .flatMapLatest { enabled ->
            if (enabled) {
                val since = System.currentTimeMillis() - APP_USAGE_LOOKBACK_MS
                getAppBatteryUsage(since)
            } else {
                flowOf(PagingData.empty())
            }
        }
        .cachedIn(viewModelScope)
    private var proObserverJob: Job? = null
    private var loadJob: Job? = null

    fun refresh() {
        if (isProUser()) {
            loadUsageData()
        } else {
            _uiState.value = AppUsageUiState.Locked
        }
    }

    fun startObserving() {
        if (proObserverJob?.isActive == true) return
        observeProState()
    }

    fun stopObserving() {
        proObserverJob?.cancel()
        proObserverJob = null
        loadJob?.cancel()
        loadJob = null
    }

    private fun observeProState() {
        proObserverJob?.cancel()
        proObserverJob = viewModelScope.launch {
            try {
                observeProAccess().collectLatest { isPro ->
                    if (!isPro) {
                        loadJob?.cancel()
                        pagingEnabled.value = false
                        _uiState.value = AppUsageUiState.Locked
                        return@collectLatest
                    }
                    loadUsageData()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = AppUsageUiState.Error(e.messageOr("Unknown error"))
            }
        }
    }

    private fun loadUsageData() {
        val since = System.currentTimeMillis() - APP_USAGE_LOOKBACK_MS
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                refreshAppUsageSnapshot()
                pagingEnabled.value = true
                getAppBatteryUsageSummary(since)
                    .catch { e ->
                        _uiState.value = AppUsageUiState.Error(e.messageOr("Unknown error"))
                    }
                    .collect { summary ->
                        _uiState.value = AppUsageUiState.Success(
                            totalForegroundTimeMs = summary.totalForegroundTimeMs,
                            maxForegroundTimeMs = summary.maxForegroundTimeMs
                        )
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = AppUsageUiState.Error(e.messageOr("Unknown error"))
            }
        }
    }

    override fun onCleared() {
        stopObserving()
        super.onCleared()
    }
}
