package com.devicepulse.ui.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.domain.usecase.GetStorageStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val getStorageState: GetStorageStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<StorageUiState>(StorageUiState.Loading)
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    init {
        loadStorageData()
    }

    fun refresh() {
        loadStorageData()
    }

    private fun loadStorageData() {
        viewModelScope.launch {
            getStorageState()
                .catch { e ->
                    _uiState.value = StorageUiState.Error(e.message ?: "Unknown error")
                }
                .collect { state ->
                    _uiState.value = StorageUiState.Success(storageState = state)
                }
        }
    }
}
