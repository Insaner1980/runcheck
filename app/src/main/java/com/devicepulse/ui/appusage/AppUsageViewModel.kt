package com.devicepulse.ui.appusage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.domain.repository.ProStatusProvider
import com.devicepulse.domain.usecase.GetAppBatteryUsageUseCase
import com.devicepulse.ui.common.messageOr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppUsageViewModel @Inject constructor(
    private val getAppBatteryUsage: GetAppBatteryUsageUseCase,
    private val proStatusProvider: ProStatusProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppUsageUiState>(AppUsageUiState.Loading)
    val uiState: StateFlow<AppUsageUiState> = _uiState.asStateFlow()
    private var proObserverJob: Job? = null
    private var loadJob: Job? = null

    init {
        observeProState()
    }

    fun refresh() {
        if (proStatusProvider.isPro()) {
            loadUsageData()
        } else {
            _uiState.value = AppUsageUiState.Locked
        }
    }

    private fun observeProState() {
        proObserverJob?.cancel()
        proObserverJob = viewModelScope.launch {
            proStatusProvider.isProUser.collectLatest { isPro ->
                if (!isPro) {
                    loadJob?.cancel()
                    _uiState.value = AppUsageUiState.Locked
                    return@collectLatest
                }
                loadUsageData()
            }
        }
    }

    private fun loadUsageData() {
        val since = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getAppBatteryUsage(since)
                .catch { e ->
                    _uiState.value = AppUsageUiState.Error(e.messageOr("Unknown error"))
                }
                .collect { apps ->
                    _uiState.value = AppUsageUiState.Success(apps = apps)
                }
        }
    }
}
