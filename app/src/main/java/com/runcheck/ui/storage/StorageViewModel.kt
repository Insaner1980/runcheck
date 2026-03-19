package com.runcheck.ui.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.usecase.GetStorageStateUseCase
import com.runcheck.ui.common.messageOr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val getStorageState: GetStorageStateUseCase,
    private val proStatusProvider: ProStatusProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<StorageUiState>(StorageUiState.Loading)
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun startObserving() {
        if (loadJob?.isActive == true) return
        loadStorageData()
    }

    fun stopObserving() {
        loadJob?.cancel()
        loadJob = null
    }

    fun refresh() {
        loadStorageData()
    }

    private fun loadStorageData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                getStorageState(),
                proStatusProvider.isProUser
            ) { state, isPro ->
                StorageUiState.Success(storageState = state, isPro = isPro)
            }
                .catch { e ->
                    _uiState.value = StorageUiState.Error(e.messageOr("Unknown error"))
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }
}
