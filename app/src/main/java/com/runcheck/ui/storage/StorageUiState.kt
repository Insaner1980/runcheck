package com.runcheck.ui.storage

import com.runcheck.domain.model.StorageState

sealed interface StorageUiState {
    data object Loading : StorageUiState
    data class Success(
        val storageState: StorageState,
        val isPro: Boolean = false,
        val dismissedInfoCards: Set<String> = emptySet()
    ) : StorageUiState
    data class Error(val message: String) : StorageUiState
}
