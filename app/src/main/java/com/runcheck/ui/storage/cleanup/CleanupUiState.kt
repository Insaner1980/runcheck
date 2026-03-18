package com.runcheck.ui.storage.cleanup

import androidx.compose.runtime.Immutable
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile

sealed interface CleanupUiState {
    data object Idle : CleanupUiState
    data class Scanning(val progress: Float = -1f) : CleanupUiState

    @Immutable
    data class Results(
        val groups: List<FileGroup>,
        val selectedUris: Set<String>,
        val selectedSize: Long,
        val totalSize: Long,
        val totalCount: Int,
        val currentUsagePercent: Float,
        val projectedUsagePercent: Float
    ) : CleanupUiState

    data class Deleting(val count: Int) : CleanupUiState
    data class Success(val freedBytes: Long) : CleanupUiState
    data object Empty : CleanupUiState
}

data class FileGroup(
    val category: MediaCategory,
    val files: List<ScannedFile>,
    val totalBytes: Long,
    val expanded: Boolean = false
)
