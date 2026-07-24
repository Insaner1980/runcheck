package com.runcheck.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.R
import com.runcheck.domain.usecase.ExportDataUseCase
import com.runcheck.ui.common.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportUiState(
    val isExporting: Boolean = false,
    val shareUris: List<String>? = null,
    val status: UiText? = null,
)

@HiltViewModel
class ExportViewModel
    @Inject
    constructor(
        private val exportDataUseCase: ExportDataUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExportUiState())
        val uiState = _uiState.asStateFlow()
        private var exportJob: Job? = null

        fun prepareExportShare() {
            if (exportJob?.isActive == true) return
            exportJob =
                viewModelScope.launch(start = CoroutineStart.LAZY) {
                    _uiState.update { it.copy(isExporting = true, status = null) }
                    try {
                        val uris = exportDataUseCase.prepareExportShare()
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                shareUris = uris.takeIf { it.isNotEmpty() },
                                status =
                                    UiText.Resource(
                                        if (uris.isNotEmpty()) {
                                            R.string.settings_export_ready
                                        } else {
                                            R.string.settings_export_error
                                        },
                                    ),
                            )
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                status = UiText.Resource(R.string.settings_export_error),
                            )
                        }
                    }
                }
            exportJob?.start()
        }

        fun onShareHandled() {
            _uiState.update { it.copy(shareUris = null) }
        }
    }
