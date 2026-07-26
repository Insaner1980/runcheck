package com.runcheck.ui.appusage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.runcheck.R
import com.runcheck.domain.model.UnusedAppsPeriod
import com.runcheck.domain.model.UsageAccess
import com.runcheck.domain.usecase.GetAppBatteryUsageSummaryUseCase
import com.runcheck.domain.usecase.GetAppBatteryUsageUseCase
import com.runcheck.domain.usecase.GetUnusedAppsUseCase
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.domain.usecase.RefreshAppUsageSnapshotUseCase
import com.runcheck.domain.usecase.UnusedAppsQueryResult
import com.runcheck.ui.common.messageOrRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

private const val APP_USAGE_LOOKBACK_MS = 24 * 60 * 60 * 1000L

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AppUsageViewModel
    @Inject
    constructor(
        private val getAppBatteryUsage: GetAppBatteryUsageUseCase,
        private val getAppBatteryUsageSummary: GetAppBatteryUsageSummaryUseCase,
        private val refreshAppUsageSnapshot: RefreshAppUsageSnapshotUseCase,
        private val observeProAccess: ObserveProAccessUseCase,
        private val isProUser: IsProUserUseCase,
        private val getUnusedApps: GetUnusedAppsUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<AppUsageUiState>(AppUsageUiState.Loading)
        val uiState: StateFlow<AppUsageUiState> = _uiState.asStateFlow()
        private val _unusedAppsState = MutableStateFlow<UnusedAppsUiState>(UnusedAppsUiState.Idle)
        val unusedAppsState: StateFlow<UnusedAppsUiState> = _unusedAppsState.asStateFlow()
        private val pagingEnabled = MutableStateFlow(false)
        val pagedApps: Flow<PagingData<com.runcheck.domain.model.AppBatteryUsage>> =
            pagingEnabled
                .flatMapLatest { enabled ->
                    if (enabled) {
                        val since = System.currentTimeMillis() - APP_USAGE_LOOKBACK_MS
                        getAppBatteryUsage(since)
                    } else {
                        flowOf(PagingData.empty())
                    }
                }.cachedIn(viewModelScope)
        private var proObserverJob: Job? = null
        private var loadJob: Job? = null
        private var unusedAppsJob: Job? = null
        private var unusedAppsObservedAt: Instant? = null

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
            unusedAppsJob?.cancel()
            unusedAppsJob = null
        }

        private fun observeProState() {
            proObserverJob?.cancel()
            proObserverJob =
                viewModelScope.launch {
                    try {
                        observeProAccess().collectLatest { isPro ->
                            if (!isPro) {
                                loadJob?.cancel()
                                unusedAppsJob?.cancel()
                                pagingEnabled.value = false
                                _uiState.value = AppUsageUiState.Locked
                                _unusedAppsState.value = UnusedAppsUiState.Locked
                                return@collectLatest
                            }
                            loadUsageData()
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _uiState.value = AppUsageUiState.Error(e.messageOrRes(R.string.common_error_generic))
                    }
                }
        }

        fun loadUnusedApps(
            period: UnusedAppsPeriod,
            forceRefresh: Boolean,
        ) {
            val observedAt =
                if (forceRefresh) {
                    Instant.now().also { unusedAppsObservedAt = it }
                } else {
                    unusedAppsObservedAt ?: Instant.now().also { unusedAppsObservedAt = it }
                }
            unusedAppsJob?.cancel()
            unusedAppsJob =
                viewModelScope.launch {
                    _unusedAppsState.value = UnusedAppsUiState.Loading
                    try {
                        when (val result = getUnusedApps(period, observedAt, forceRefresh)) {
                            UnusedAppsQueryResult.Locked -> {
                                _unusedAppsState.value = UnusedAppsUiState.Locked
                            }

                            is UnusedAppsQueryResult.Available -> {
                                _unusedAppsState.value =
                                    if (result.result.usageAccess == UsageAccess.REQUIRED) {
                                        UnusedAppsUiState.PermissionRequired(period)
                                    } else {
                                        UnusedAppsUiState.Success(
                                            period = period,
                                            candidates = result.result.candidates,
                                            partialErrors = result.result.partialErrors,
                                        )
                                    }
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _unusedAppsState.value =
                            UnusedAppsUiState.Error(e.messageOrRes(R.string.common_error_generic))
                    }
                }
        }

        private fun loadUsageData() {
            val since = System.currentTimeMillis() - APP_USAGE_LOOKBACK_MS
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    try {
                        refreshAppUsageSnapshot()
                        pagingEnabled.value = true
                        getAppBatteryUsageSummary(since)
                            .catch { e ->
                                _uiState.value = AppUsageUiState.Error(e.messageOrRes(R.string.common_error_generic))
                            }.collect { summary ->
                                _uiState.value =
                                    AppUsageUiState.Success(
                                        totalForegroundTimeMs = summary.totalForegroundTimeMs,
                                        maxForegroundTimeMs = summary.maxForegroundTimeMs,
                                    )
                            }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _uiState.value = AppUsageUiState.Error(e.messageOrRes(R.string.common_error_generic))
                    }
                }
        }

        override fun onCleared() {
            stopObserving()
            super.onCleared()
        }
    }
