package com.runcheck.ui.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.usecase.GetStorageStateUseCase
import com.runcheck.domain.usecase.ManageInfoCardDismissalsUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.domain.usecase.StorageCleanupUseCase
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val getStorageState: GetStorageStateUseCase,
    private val observeProAccess: ObserveProAccessUseCase,
    private val storageCleanup: StorageCleanupUseCase,
    private val manageInfoCardDismissals: ManageInfoCardDismissalsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<StorageUiState>(StorageUiState.Loading)
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    private val _trashDeleteRequestUris = MutableSharedFlow<List<String>>(replay = 1)
    val trashDeleteRequestUris: SharedFlow<List<String>> = _trashDeleteRequestUris.asSharedFlow()

    fun startObserving() {
        if (loadJob?.isActive == true) return
        loadStorageData()
    }

    fun stopObserving() {
        loadJob?.cancel()
        loadJob = null
    }

    fun refresh() {
        loadStorageData()
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

    private fun loadStorageData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                getStorageState(),
                observeProAccess(),
                manageInfoCardDismissals.observeDismissedCardIds()
            ) { state, isPro, dismissedCards ->
                StorageUiState.Success(
                    storageState = state,
                    isPro = isPro,
                    dismissedInfoCards = dismissedCards
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
}
