package com.devicepulse.ui.thermal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.domain.usecase.GetThermalStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThermalViewModel @Inject constructor(
    private val getThermalState: GetThermalStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ThermalUiState>(ThermalUiState.Loading)
    val uiState: StateFlow<ThermalUiState> = _uiState.asStateFlow()

    init {
        loadThermalData()
    }

    fun refresh() {
        loadThermalData()
    }

    private fun loadThermalData() {
        viewModelScope.launch {
            getThermalState()
                .catch { e ->
                    _uiState.value = ThermalUiState.Error(e.message ?: "Unknown error")
                }
                .collect { state ->
                    _uiState.value = ThermalUiState.Success(thermalState = state)
                }
        }
    }
}
