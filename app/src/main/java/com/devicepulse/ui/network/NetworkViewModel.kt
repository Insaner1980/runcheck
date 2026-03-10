package com.devicepulse.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.domain.usecase.GetNetworkStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val getNetworkState: GetNetworkStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NetworkUiState>(NetworkUiState.Loading)
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    init {
        loadNetworkData()
    }

    fun refresh() {
        loadNetworkData()
    }

    private fun loadNetworkData() {
        viewModelScope.launch {
            getNetworkState()
                .catch { e ->
                    _uiState.value = NetworkUiState.Error(e.message ?: "Unknown error")
                }
                .collect { state ->
                    _uiState.value = NetworkUiState.Success(networkState = state)
                }
        }
    }
}
