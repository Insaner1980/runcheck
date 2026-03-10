package com.devicepulse.ui.network

import com.devicepulse.domain.model.NetworkState

sealed interface NetworkUiState {
    data object Loading : NetworkUiState
    data class Success(val networkState: NetworkState) : NetworkUiState
    data class Error(val message: String) : NetworkUiState
}
