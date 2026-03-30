package com.runcheck.ui.insights

import com.runcheck.domain.insights.model.Insight

sealed interface InsightsUiState {
    data object Loading : InsightsUiState

    data class Success(
        val insights: List<Insight> = emptyList(),
        val unseenInsightCount: Int = 0,
        val isPro: Boolean = false,
    ) : InsightsUiState

    data class Error(
        val message: String,
    ) : InsightsUiState
}
