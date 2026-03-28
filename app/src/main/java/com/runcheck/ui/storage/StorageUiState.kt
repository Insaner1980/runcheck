package com.runcheck.ui.storage

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.StorageState
import com.runcheck.ui.common.UiText

sealed interface StorageUiState {
    data object Loading : StorageUiState

    data class Success(
        val storageState: StorageState,
        val isPro: Boolean = false,
        val dismissedInfoCards: Set<String> = emptySet(),
        val showInfoCards: Boolean = true,
        val liveUsagePercent: List<Float> = emptyList(),
        val storageHistory: List<StorageReading> = emptyList(),
        val selectedHistoryPeriod: HistoryPeriod = HistoryPeriod.WEEK,
        val historyLoadError: UiText? = null,
    ) : StorageUiState

    data class Error(
        val message: String,
    ) : StorageUiState
}
