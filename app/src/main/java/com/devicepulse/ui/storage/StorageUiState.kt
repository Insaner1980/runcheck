package com.devicepulse.ui.storage

import com.devicepulse.domain.model.StorageState

sealed interface StorageUiState {
    data object Loading : StorageUiState
    data class Success(val storageState: StorageState) : StorageUiState
    data class Error(val message: String) : StorageUiState
}
