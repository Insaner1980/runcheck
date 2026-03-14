package com.runcheck.ui.storage

import androidx.compose.runtime.Immutable
import com.runcheck.domain.model.StorageState

sealed interface StorageUiState {
    data object Loading : StorageUiState
    @Immutable
    data class Success(val storageState: StorageState) : StorageUiState
    data class Error(val message: String) : StorageUiState
}
