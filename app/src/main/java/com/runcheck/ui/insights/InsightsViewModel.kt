package com.runcheck.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.repository.InsightRepository
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.ui.common.messageOr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel
    @Inject
    constructor(
        private val insightRepository: InsightRepository,
        private val observeProAccess: ObserveProAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<InsightsUiState>(InsightsUiState.Loading)
        val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

        private var lastSeenInsightIds: Set<Long> = emptySet()

        init {
            observeInsights()
        }

        fun dismissInsight(id: Long) {
            viewModelScope.launch {
                insightRepository.dismiss(id)
            }
        }

        private fun observeInsights() {
            viewModelScope.launch {
                combine(
                    insightRepository.getActiveInsights(),
                    insightRepository.getUnseenCount(),
                    observeProAccess(),
                ) { insights, unseenCount, isPro ->
                    InsightsUiState.Success(
                        insights = insights,
                        unseenInsightCount = unseenCount,
                        isPro = isPro,
                    )
                }.catch { error ->
                    _uiState.value = InsightsUiState.Error(error.messageOr("Unknown error"))
                }.collect { state ->
                    _uiState.value = state
                    maybeMarkSeen(state)
                }
            }
        }

        private fun maybeMarkSeen(state: InsightsUiState.Success) {
            val unseenIds =
                state.insights
                    .filterNot { it.seen }
                    .map { it.id }
                    .toSet()
            if (unseenIds.isEmpty()) {
                lastSeenInsightIds = emptySet()
                return
            }
            if (unseenIds == lastSeenInsightIds) return

            lastSeenInsightIds = unseenIds
            viewModelScope.launch {
                insightRepository.markAllSeen()
            }
        }
    }
