package com.devicepulse.ui.charger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.domain.repository.ChargerRepository
import com.devicepulse.domain.usecase.GetChargerComparisonUseCase
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
class ChargerViewModel @Inject constructor(
    private val getChargerComparison: GetChargerComparisonUseCase,
    private val chargerRepository: ChargerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChargerUiState>(ChargerUiState.Loading)
    val uiState: StateFlow<ChargerUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadChargerData()
    }

    fun refresh() {
        loadChargerData()
    }

    fun addCharger(name: String) {
        viewModelScope.launch {
            chargerRepository.insertCharger(name)
        }
    }

    fun deleteCharger(id: Long) {
        viewModelScope.launch {
            chargerRepository.deleteChargerById(id)
        }
    }

    private fun loadChargerData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                getChargerComparison(),
                chargerRepository.getAllSessions()
            ) { chargers, sessions ->
                ChargerUiState.Success(
                    chargers = chargers,
                    sessions = sessions
                )
            }.catch { e ->
                _uiState.value = ChargerUiState.Error(e.message ?: "Unknown error")
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
