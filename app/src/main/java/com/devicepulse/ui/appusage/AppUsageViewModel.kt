package com.devicepulse.ui.appusage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.domain.usecase.GetAppBatteryUsageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppUsageViewModel @Inject constructor(
    private val getAppBatteryUsage: GetAppBatteryUsageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppUsageUiState>(AppUsageUiState.Loading)
    val uiState: StateFlow<AppUsageUiState> = _uiState.asStateFlow()

    init {
        loadUsageData()
    }

    fun refresh() {
        loadUsageData()
    }

    private fun loadUsageData() {
        val since = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        viewModelScope.launch {
            getAppBatteryUsage(since)
                .catch { e ->
                    _uiState.value = AppUsageUiState.Error(e.message ?: "Unknown error")
                }
                .collect { apps ->
                    _uiState.value = AppUsageUiState.Success(apps = apps)
                }
        }
    }
}
