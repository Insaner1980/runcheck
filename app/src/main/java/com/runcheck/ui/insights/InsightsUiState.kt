package com.runcheck.ui.insights

import com.runcheck.domain.insights.model.Insight
import com.runcheck.ui.common.UiText

sealed interface InsightsUiState {
    data object Loading : InsightsUiState

    data class Success(
        val insights: List<Insight> = emptyList(),
        val unseenInsightCount: Int = 0,
        val isPro: Boolean = false,
    ) : InsightsUiState

    data class Error(
        val message: UiText,
    ) : InsightsUiState
}
