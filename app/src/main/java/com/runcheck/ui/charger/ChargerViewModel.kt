package com.runcheck.ui.charger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.usecase.AddChargerUseCase
import com.runcheck.domain.usecase.DeleteChargerUseCase
import com.runcheck.domain.usecase.GetChargerComparisonUseCase
import com.runcheck.domain.usecase.GetChargerSessionsUseCase
import com.runcheck.ui.common.messageOr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    private val getChargerSessions: GetChargerSessionsUseCase,
    private val addChargerUseCase: AddChargerUseCase,
    private val deleteChargerUseCase: DeleteChargerUseCase,
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
            try {
                addChargerUseCase(name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ChargerUiState.Error(e.messageOr("Unknown error"))
            }
        }
    }

    fun deleteCharger(id: Long) {
        if (!proStatusProvider.isPro()) return
        viewModelScope.launch {
            try {
                deleteChargerUseCase(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ChargerUiState.Error(e.messageOr("Unknown error"))
            }
        }
    }

    private fun observeProState() {
        proObserverJob?.cancel()
        proObserverJob = viewModelScope.launch {
            try {
                proStatusProvider.isProUser.collectLatest { isPro ->
                    if (!isPro) {
                        loadJob?.cancel()
                        _uiState.value = ChargerUiState.Locked
                        return@collectLatest
                    }
                    loadChargerData()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ChargerUiState.Error(e.messageOr("Unknown error"))
            }
        }
    }

    private fun loadChargerData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                getChargerComparison(),
                getChargerSessions()
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
