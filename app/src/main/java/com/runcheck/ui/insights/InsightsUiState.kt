package com.runcheck.ui.insights

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.ui.common.UiText

enum class InsightFilter {
    ALL,
    IMPORTANT,
    ;

    fun applyTo(insights: List<Insight>): List<Insight> =
        when (this) {
            ALL -> insights
            IMPORTANT ->
                insights.filter { insight ->
                    insight.priority == InsightPriority.HIGH ||
                        insight.priority == InsightPriority.MEDIUM
                }
        }
}

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
