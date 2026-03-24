package com.runcheck.ui.storage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.usecase.GetStorageHistoryUseCase
import com.runcheck.domain.usecase.GetStorageStateUseCase
import com.runcheck.domain.usecase.ManageInfoCardDismissalsUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.domain.usecase.StorageCleanupUseCase
import com.runcheck.ui.common.UiText
import com.runcheck.ui.common.messageOr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getStorageState: GetStorageStateUseCase,
    private val observeProAccess: ObserveProAccessUseCase,
    private val storageCleanup: StorageCleanupUseCase,
    private val manageInfoCardDismissals: ManageInfoCardDismissalsUseCase,
    private val manageUserPreferences: ManageUserPreferencesUseCase,
    private val getStorageHistory: GetStorageHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<StorageUiState>(StorageUiState.Loading)
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var historyJob: Job? = null

    private var selectedHistoryPeriod: HistoryPeriod
        get() = savedStateHandle.get<String>(SELECTED_HISTORY_PERIOD_KEY)
            ?.let { value -> runCatching { HistoryPeriod.valueOf(value) }.getOrNull() }
            ?: HistoryPeriod.WEEK
        set(value) { savedStateHandle[SELECTED_HISTORY_PERIOD_KEY] = value.name }

    private val _trashDeleteRequestUris = MutableSharedFlow<List<String>>(replay = 1)
    val trashDeleteRequestUris: SharedFlow<List<String>> = _trashDeleteRequestUris.asSharedFlow()

    fun startObserving() {
        if (loadJob?.isActive == true) return
        loadStorageData()
        loadHistory()
    }

    fun stopObserving() {
        loadJob?.cancel()
        loadJob = null
        historyJob?.cancel()
        historyJob = null
    }

    fun refresh() {
        loadStorageData()
    }

    fun setHistoryPeriod(period: HistoryPeriod) {
        selectedHistoryPeriod = period
        loadHistory()
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val uris = storageCleanup.getTrashedUris()
            if (uris.isEmpty()) return@launch
            _trashDeleteRequestUris.emit(uris)
        }
    }

    fun onTrashEmptied() {
        refresh()
    }

    fun onTrashDeleteRequestFailed(message: String) {
        _uiState.value = StorageUiState.Error(message)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun onTrashDeleteRequestConsumed() {
        _trashDeleteRequestUris.resetReplayCache()
    }

    fun dismissInfoCard(id: String) {
        viewModelScope.launch {
            manageInfoCardDismissals.dismissCard(id)
        }
    }

    private fun loadHistory() {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            getStorageHistory(selectedHistoryPeriod)
                .catch { e ->
                    _uiState.update { current ->
                        (current as? StorageUiState.Success)?.copy(
                            historyLoadError = UiText.Dynamic(e.message ?: "Error")
                        ) ?: current
                    }
                }
                .collect { readings ->
                    _uiState.update { current ->
                        (current as? StorageUiState.Success)?.copy(
                            storageHistory = readings,
                            selectedHistoryPeriod = selectedHistoryPeriod,
                            historyLoadError = null
                        ) ?: current
                    }
                }
        }
    }

    private fun loadStorageData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                getStorageState(),
                observeProAccess(),
                manageInfoCardDismissals.observeDismissedCardIds(),
                manageUserPreferences.observePreferences()
            ) { state, isPro, dismissedCards, preferences ->
                val currentSuccess = _uiState.value as? StorageUiState.Success
                StorageUiState.Success(
                    storageState = state,
                    isPro = isPro,
                    dismissedInfoCards = dismissedCards,
                    showInfoCards = preferences.showInfoCards,
                    storageHistory = currentSuccess?.storageHistory ?: emptyList(),
                    selectedHistoryPeriod = currentSuccess?.selectedHistoryPeriod ?: selectedHistoryPeriod,
                    historyLoadError = currentSuccess?.historyLoadError
                )
            }
                .catch { e ->
                    _uiState.value = StorageUiState.Error(e.messageOr("Unknown error"))
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    private companion object {
        const val SELECTED_HISTORY_PERIOD_KEY = "storage_selected_history_period"
    }
}
