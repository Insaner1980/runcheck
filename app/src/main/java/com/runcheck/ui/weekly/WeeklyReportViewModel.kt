package com.runcheck.ui.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.R
import com.runcheck.domain.model.WeeklyReport
import com.runcheck.domain.model.WeeklyReportPeriod
import com.runcheck.domain.usecase.GenerateWeeklyReportUseCase
import com.runcheck.domain.usecase.WeeklyReportGenerationResult
import com.runcheck.service.report.WeeklyReportTimeProvider
import com.runcheck.ui.common.UiText
import com.runcheck.ui.common.messageOrRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WeeklyReportUiState {
    data object Loading : WeeklyReportUiState

    data object Locked : WeeklyReportUiState

    data class Success(
        val report: WeeklyReport,
    ) : WeeklyReportUiState

    data class Error(
        val message: UiText,
    ) : WeeklyReportUiState
}

@HiltViewModel
class WeeklyReportViewModel
    @Inject
    constructor(
        private val generateWeeklyReport: GenerateWeeklyReportUseCase,
        private val timeProvider: WeeklyReportTimeProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<WeeklyReportUiState>(WeeklyReportUiState.Loading)
        val uiState: StateFlow<WeeklyReportUiState> = _uiState.asStateFlow()
        private var loadJob: Job? = null

        fun load() {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    _uiState.value = WeeklyReportUiState.Loading
                    try {
                        val period =
                            WeeklyReportPeriod.previousCompleted(
                                timeProvider.clock.instant(),
                                timeProvider.zoneId(),
                            )
                        _uiState.value =
                            when (val result = generateWeeklyReport(period)) {
                                WeeklyReportGenerationResult.Locked -> WeeklyReportUiState.Locked
                                is WeeklyReportGenerationResult.Available -> {
                                    WeeklyReportUiState.Success(result.report)
                                }
                            }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _uiState.value =
                            WeeklyReportUiState.Error(e.messageOrRes(R.string.common_error_generic))
                    }
                }
        }

        override fun onCleared() {
            loadJob?.cancel()
            super.onCleared()
        }
    }
