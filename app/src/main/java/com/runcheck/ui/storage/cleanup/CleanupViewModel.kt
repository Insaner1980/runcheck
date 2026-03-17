package com.runcheck.ui.storage.cleanup

import android.app.PendingIntent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.data.storage.MediaStoreScanner
import com.runcheck.data.storage.StorageCleanupHelper
import com.runcheck.data.storage.StorageDataSource
import com.runcheck.data.storage.ThumbnailLoader
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CleanupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaStoreScanner: MediaStoreScanner,
    private val cleanupHelper: StorageCleanupHelper,
    private val storageDataSource: StorageDataSource,
    val thumbnailLoader: ThumbnailLoader
) : ViewModel() {

    val cleanupType: CleanupType = try {
        CleanupType.valueOf(savedStateHandle.get<String>("type") ?: "LARGE_FILES")
    } catch (_: Exception) { CleanupType.LARGE_FILES }

    private val _uiState = MutableStateFlow<CleanupUiState>(CleanupUiState.Idle)
    val uiState: StateFlow<CleanupUiState> = _uiState.asStateFlow()

    private val _deleteIntent = MutableSharedFlow<PendingIntent>()
    val deleteIntent: SharedFlow<PendingIntent> = _deleteIntent.asSharedFlow()

    private var selectedFilter: Int = cleanupType.defaultFilterIndex
    private var allFiles: List<ScannedFile> = emptyList()
    private var currentStorageTotal: Long = 1L
    private var currentStorageUsed: Long = 0L

    init {
        scan()
    }

    fun setFilter(index: Int) {
        selectedFilter = index
        scan()
    }

    fun getSelectedFilterIndex(): Int = selectedFilter

    fun scan() {
        viewModelScope.launch {
            _uiState.value = CleanupUiState.Scanning()
            try {
                // Get current storage info for projections
                val storageInfo = storageDataSource.getStorageInfo()
                currentStorageTotal = storageInfo.totalBytes.coerceAtLeast(1L)
                currentStorageUsed = storageInfo.usedBytes

                val filterValue = cleanupType.filterOptions
                    .getOrNull(selectedFilter)?.value

                allFiles = when (cleanupType) {
                    CleanupType.LARGE_FILES ->
                        mediaStoreScanner.scanLargeFiles(filterValue ?: (50L * 1024 * 1024))
                    CleanupType.OLD_DOWNLOADS ->
                        mediaStoreScanner.scanOldDownloads(filterValue ?: (30L * 86_400_000))
                    CleanupType.APK_FILES ->
                        mediaStoreScanner.scanApkFiles()
                }

                if (allFiles.isEmpty()) {
                    _uiState.value = CleanupUiState.Empty
                    return@launch
                }

                val initialSelected = if (cleanupType.preselectAll) {
                    allFiles.map { it.uri }.toSet()
                } else {
                    emptySet()
                }

                emitResults(initialSelected)
            } catch (e: Exception) {
                ReleaseSafeLog.error("CleanupVM", "Scan failed", e)
                _uiState.value = CleanupUiState.Empty
            }
        }
    }

    fun toggleSelection(uri: Uri) {
        val state = _uiState.value as? CleanupUiState.Results ?: return
        val newSelected = if (uri in state.selectedUris) {
            state.selectedUris - uri
        } else {
            state.selectedUris + uri
        }
        emitResults(newSelected)
    }

    fun toggleGroupSelection(category: MediaCategory) {
        val state = _uiState.value as? CleanupUiState.Results ?: return
        val groupUris = state.groups
            .find { it.category == category }
            ?.files?.map { it.uri }?.toSet() ?: return

        val allSelected = groupUris.all { it in state.selectedUris }
        val newSelected = if (allSelected) {
            state.selectedUris - groupUris
        } else {
            state.selectedUris + groupUris
        }
        emitResults(newSelected)
    }

    fun toggleGroupExpanded(category: MediaCategory) {
        val state = _uiState.value as? CleanupUiState.Results ?: return
        val newGroups = state.groups.map { group ->
            if (group.category == category) group.copy(expanded = !group.expanded)
            else group
        }
        _uiState.value = state.copy(groups = newGroups)
    }

    fun requestDelete() {
        val state = _uiState.value as? CleanupUiState.Results ?: return
        if (state.selectedUris.isEmpty()) return

        val uris = state.selectedUris.toList()
        val selectedSize = state.selectedSize

        viewModelScope.launch {
            _uiState.value = CleanupUiState.Deleting(uris.size)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = cleanupHelper.createDeleteRequest(uris)
                if (intent != null) {
                    _deleteIntent.emit(intent)
                    return@launch
                }
            }

            // Legacy fallback
            val deleted = cleanupHelper.deleteLegacy(uris)
            if (deleted > 0) {
                onDeleteSuccess(selectedSize)
            } else {
                scan() // re-scan on failure
            }
        }
    }

    fun onDeleteConfirmed() {
        val state = _uiState.value
        val size = when (state) {
            is CleanupUiState.Results -> state.selectedSize
            is CleanupUiState.Deleting -> {
                allFiles.filter { it.uri in ((_uiState.value as? CleanupUiState.Results)?.selectedUris ?: emptySet()) }
                    .sumOf { it.sizeBytes }
            }
            else -> 0L
        }
        onDeleteSuccess(size)
    }

    fun onDeleteCancelled() {
        // Return to results
        emitResults(emptySet())
    }

    private fun onDeleteSuccess(freedBytes: Long) {
        viewModelScope.launch {
            _uiState.value = CleanupUiState.Success(freedBytes)
            delay(1800)
            scan() // re-scan to show updated list
        }
    }

    private fun emitResults(selectedUris: Set<Uri>) {
        val groups = allFiles
            .groupBy { it.category }
            .map { (category, files) ->
                FileGroup(
                    category = category,
                    files = files.sortedByDescending { it.sizeBytes },
                    totalBytes = files.sumOf { it.sizeBytes },
                    expanded = (_uiState.value as? CleanupUiState.Results)
                        ?.groups?.find { it.category == category }?.expanded ?: false
                )
            }
            .sortedByDescending { it.totalBytes }
            .mapIndexed { index, group ->
                // Expand largest group by default
                if (index == 0 && groups().none { it.expanded }) group.copy(expanded = true)
                else group
            }

        val selectedSize = allFiles
            .filter { it.uri in selectedUris }
            .sumOf { it.sizeBytes }

        val currentPct = (currentStorageUsed.toFloat() / currentStorageTotal) * 100f
        val projectedUsed = (currentStorageUsed - selectedSize).coerceAtLeast(0L)
        val projectedPct = (projectedUsed.toFloat() / currentStorageTotal) * 100f

        _uiState.value = CleanupUiState.Results(
            groups = groups,
            selectedUris = selectedUris,
            selectedSize = selectedSize,
            totalSize = allFiles.sumOf { it.sizeBytes },
            totalCount = allFiles.size,
            currentUsagePercent = currentPct,
            projectedUsagePercent = projectedPct
        )
    }

    // Helper to avoid smart-cast issues
    private fun groups(): List<FileGroup> {
        return (_uiState.value as? CleanupUiState.Results)?.groups ?: emptyList()
    }
}
