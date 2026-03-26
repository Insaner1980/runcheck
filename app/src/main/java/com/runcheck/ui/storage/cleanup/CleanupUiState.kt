package com.runcheck.ui.storage.cleanup

import com.runcheck.domain.model.MediaCategory
import com.runcheck.ui.common.UiText

sealed interface CleanupUiState {
    data object Idle : CleanupUiState
    data class Scanning(val progress: Float = -1f) : CleanupUiState

    data class Results(
        val groups: List<FileGroup>,
        val selectedCount: Int,
        val selectedSize: Long,
        val totalSize: Long,
        val totalCount: Int,
        val currentUsagePercent: Float,
        val projectedUsagePercent: Float,
        val maxFileSizeBytes: Long,
        val pagerGeneration: Int
    ) : CleanupUiState

    data class Deleting(val count: Int) : CleanupUiState
    data class Success(val freedBytes: Long) : CleanupUiState
    data class Error(val message: UiText) : CleanupUiState
    data object Empty : CleanupUiState
    data object NeedsStoragePermission : CleanupUiState
    data object UnsupportedVersion : CleanupUiState
}

data class FileGroup(
    val category: MediaCategory,
    val itemCount: Int,
    val totalBytes: Long,
    val expanded: Boolean = false,
    val selectedCount: Int = 0
)
