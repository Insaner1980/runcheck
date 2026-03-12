package com.devicepulse.ui.charger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.domain.repository.ChargerRepository
import com.devicepulse.domain.repository.ProStatusProvider
import com.devicepulse.domain.usecase.GetChargerComparisonUseCase
import com.devicepulse.ui.common.messageOr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChargerViewModel @Inject constructor(
    private val getChargerComparison: GetChargerComparisonUseCase,
    private val chargerRepository: ChargerRepository,
    private val proStatusProvider: ProStatusProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChargerUiState>(ChargerUiState.Loading)
    val uiState: StateFlow<ChargerUiState> = _uiState.asStateFlow()
    private var proObserverJob: Job? = null
    private var loadJob: Job? = null

    init {
        observeProState()
    }

    fun refresh() {
        if (proStatusProvider.isPro()) {
            loadChargerData()
        } else {
            _uiState.value = ChargerUiState.Locked
        }
    }

    fun addCharger(name: String) {
        if (!proStatusProvider.isPro()) return
        viewModelScope.launch {
            chargerRepository.insertCharger(name)
        }
    }

    fun deleteCharger(id: Long) {
        if (!proStatusProvider.isPro()) return
        viewModelScope.launch {
            chargerRepository.deleteChargerById(id)
        }
    }

    private fun observeProState() {
        proObserverJob?.cancel()
        proObserverJob = viewModelScope.launch {
            proStatusProvider.isProUser.collectLatest { isPro ->
                if (!isPro) {
                    loadJob?.cancel()
                    _uiState.value = ChargerUiState.Locked
                    return@collectLatest
                }
                loadChargerData()
            }
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
                _uiState.value = ChargerUiState.Error(e.messageOr("Unknown error"))
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
