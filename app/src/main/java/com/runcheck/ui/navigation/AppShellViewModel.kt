package com.runcheck.ui.navigation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.insights.policy.visibleForProAccess
import com.runcheck.domain.repository.InsightRepository
import com.runcheck.pro.ProStateProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class AppShellUiState(
    val unseenInsightCount: Int = 0,
    val proStatusReady: Boolean = false,
)

@HiltViewModel
class AppShellViewModel
    @Inject
    constructor(
        insightRepository: InsightRepository,
        proStateProvider: ProStateProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AppShellUiState())
        val uiState: StateFlow<AppShellUiState> = _uiState.asStateFlow()

        init {
            observeShellState(
                insightRepository = insightRepository,
                proStateProvider = proStateProvider,
            )
        }

        @OptIn(FlowPreview::class)
        private fun observeShellState(
            insightRepository: InsightRepository,
            proStateProvider: ProStateProvider,
        ) {
            viewModelScope.launch {
                combine(
                    insightRepository
                        .getActiveInsights()
                        .catch { emit(emptyList()) },
                    proStateProvider.proState,
                    proStateProvider.proStatusReady,
                ) { insights, proState, proStatusReady ->
                    AppShellUiState(
                        unseenInsightCount =
                            insights
                                .visibleForProAccess(proState.isPro)
                                .count { !it.seen },
                        proStatusReady = proStatusReady,
                    )
                }.sample(DISPLAY_UPDATE_INTERVAL_MS)
                    .collect(_uiState)
            }
        }

        private companion object {
            private const val DISPLAY_UPDATE_INTERVAL_MS = 333L
        }
    }
