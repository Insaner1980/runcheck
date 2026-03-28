package com.runcheck.ui.charger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.usecase.AddChargerUseCase
import com.runcheck.domain.usecase.DeleteChargerUseCase
import com.runcheck.domain.usecase.GetChargerComparisonUseCase
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.ui.common.messageOr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val UNKNOWN_ERROR = "Unknown error"

@HiltViewModel
class ChargerViewModel
    @Inject
    constructor(
        private val getChargerComparison: GetChargerComparisonUseCase,
        private val addChargerUseCase: AddChargerUseCase,
        private val deleteChargerUseCase: DeleteChargerUseCase,
        private val observeProAccess: ObserveProAccessUseCase,
        private val isProUser: IsProUserUseCase,
        private val manageUserPreferences: ManageUserPreferencesUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ChargerUiState>(ChargerUiState.Loading)
        val uiState: StateFlow<ChargerUiState> = _uiState.asStateFlow()
        private var proObserverJob: Job? = null
        private var loadJob: Job? = null

        fun refresh() {
            if (isProUser()) {
                loadChargerData()
            } else {
                _uiState.value = ChargerUiState.Locked
            }
        }

        fun startObserving() {
            if (proObserverJob?.isActive == true) return
            observeProState()
        }

        fun stopObserving() {
            proObserverJob?.cancel()
            proObserverJob = null
            loadJob?.cancel()
            loadJob = null
        }

        fun addCharger(name: String) = launchProAction { addChargerUseCase(name) }

        fun deleteCharger(id: Long) = launchProAction { deleteChargerUseCase(id) }

        fun selectCharger(id: Long) = launchProAction { manageUserPreferences.setSelectedChargerId(id) }

        fun clearSelectedCharger() = launchProAction { manageUserPreferences.setSelectedChargerId(null) }

        private fun launchProAction(block: suspend () -> Unit) {
            if (!isProUser()) return
            viewModelScope.launch {
                try {
                    block()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value = ChargerUiState.Error(e.messageOr(UNKNOWN_ERROR))
                }
            }
        }

        private fun observeProState() {
            proObserverJob?.cancel()
            proObserverJob =
                viewModelScope.launch {
                    try {
                        observeProAccess().collectLatest { isPro ->
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
                        _uiState.value = ChargerUiState.Error(e.messageOr(UNKNOWN_ERROR))
                    }
                }
        }

        private fun loadChargerData() {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    combine(
                        getChargerComparison(),
                        manageUserPreferences.observeSelectedChargerId(),
                    ) { chargers, selectedChargerId ->
                        ChargerUiState.Success(
                            chargers = chargers,
                            selectedChargerId = selectedChargerId,
                        )
                    }.catch { e ->
                        _uiState.value = ChargerUiState.Error(e.messageOr(UNKNOWN_ERROR))
                    }.collect { state ->
                        _uiState.value = state
                    }
                }
        }

        override fun onCleared() {
            stopObserving()
            super.onCleared()
        }
    }
