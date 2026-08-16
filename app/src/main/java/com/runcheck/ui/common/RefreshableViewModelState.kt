package com.runcheck.ui.common

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class RefreshableViewModelState<S>(
    initialState: S,
) {
    private val mutableUiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = mutableUiState.asStateFlow()
    val refreshTracker = RefreshTracker()
    val isRefreshing: StateFlow<Boolean> = refreshTracker.isRefreshing
    var loadJob: Job? = null
    var historyJob: Job? = null

    fun updateUiState(transform: (S) -> S) {
        mutableUiState.update(transform)
    }

    fun stop() {
        loadJob?.cancel()
        loadJob = null
        historyJob?.cancel()
        historyJob = null
        refreshTracker.finish()
    }
}
