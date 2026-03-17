package com.runcheck.ui.storage.cleanup

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.runcheck.data.storage.MediaCategory
import com.runcheck.data.storage.ScannedFile

sealed interface CleanupUiState {
    data object Idle : CleanupUiState
    data class Scanning(val progress: Float = -1f) : CleanupUiState

    @Immutable
    data class Results(
        val groups: List<FileGroup>,
        val selectedUris: Set<Uri>,
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
