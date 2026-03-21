package com.runcheck.ui.storage.cleanup

import android.app.RecoverableSecurityException
import android.os.Build
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.R
import com.runcheck.domain.model.CleanupScanQuery
import com.runcheck.domain.model.CleanupScanSource
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import com.runcheck.domain.model.StorageDeleteFailure
import com.runcheck.domain.usecase.StorageCleanupUseCase
import com.runcheck.ui.common.UiText
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CleanupViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val storageCleanup: StorageCleanupUseCase
) : ViewModel() {

    val cleanupType: CleanupType = try {
        CleanupType.valueOf(savedStateHandle.get<String>("type") ?: "LARGE_FILES")
    } catch (_: Exception) { CleanupType.LARGE_FILES }

    private val _uiState = MutableStateFlow<CleanupUiState>(CleanupUiState.Idle)
    val uiState: StateFlow<CleanupUiState> = _uiState.asStateFlow()

    private val _deleteRequestUris = MutableSharedFlow<List<String>>()
    val deleteRequestUris: SharedFlow<List<String>> = _deleteRequestUris.asSharedFlow()

    private var selectedFilter: Int
        get() = savedStateHandle.get<Int>(SELECTED_FILTER_KEY) ?: cleanupType.defaultFilterIndex
        set(value) {
            savedStateHandle[SELECTED_FILTER_KEY] = value
        }
    private var groupedFiles: List<FileGroup> = emptyList()
    private var fileSizeByUri = mutableMapOf<String, Long>()
    private var fileCategoryByUri = mutableMapOf<String, MediaCategory>()
    private var totalScannedSize: Long = 0L
    private var maxFileSizeBytes: Long = 0L
    private var currentStorageTotal: Long = 1L
    private var currentStorageUsed: Long = 0L
    private var pendingDeleteUris: Set<String> = emptySet()
    private var currentQuery: CleanupScanQuery? = null
    private var pagerFlows: Map<MediaCategory, Flow<PagingData<ScannedFile>>> = emptyMap()
    private var pagerGeneration: Int = 0
    private var selectedGroups: Set<MediaCategory> = emptySet()
    private var explicitSelectedUris: Set<String> = emptySet()
    private var explicitDeselectedUris: Set<String> = emptySet()
    private var pendingSelectionSnapshot: CleanupSelectionSnapshot? = null
    private var scanJob: Job? = null

    init {
        scan()
    }

    fun setFilter(index: Int) {
        selectedFilter = index
        scan()
    }

    fun getSelectedFilterIndex(): Int = selectedFilter

    fun scan() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.value = CleanupUiState.Scanning()
            try {
                val storageState = storageCleanup.getCurrentStorageState()
                currentStorageTotal = storageState.totalBytes.coerceAtLeast(1L)
                currentStorageUsed = storageState.usedBytes

                val filterValue = cleanupType.filterOptions
                    .getOrNull(selectedFilter)?.value
                    ?: defaultFilterValue()
                val query = CleanupScanQuery(
                    source = cleanupType.toScanSource(),
                    filterValue = filterValue
                )
                currentQuery = query
                val summary = storageCleanup.getCleanupSummary(query)

                if (summary.totalCount == 0) {
                    groupedFiles = emptyList()
                    pagerFlows = emptyMap()
                    fileSizeByUri.clear()
                    fileCategoryByUri.clear()
                    totalScannedSize = 0L
                    maxFileSizeBytes = 0L
                    selectedGroups = emptySet()
                    explicitSelectedUris = emptySet()
                    explicitDeselectedUris = emptySet()
                    _uiState.value = CleanupUiState.Empty
                    return@launch
                }

                totalScannedSize = summary.totalBytes
                maxFileSizeBytes = summary.maxFileSizeBytes
                groupedFiles = summary.groups
                    .map { group ->
                        FileGroup(
                            category = group.category,
                            itemCount = group.itemCount,
                            totalBytes = group.totalBytes
                        )
                    }
                    .mapIndexed { index, group ->
                        if (index == 0) group.copy(expanded = true) else group
                    }
                pagerGeneration += 1
                pagerFlows = groupedFiles.associate { group ->
                    group.category to storageCleanup
                        .getCleanupItems(query, group.category)
                        .cachedIn(viewModelScope)
                }

                selectedGroups = if (cleanupType.preselectAll) {
                    groupedFiles.map { it.category }.toSet()
                } else {
                    emptySet()
                }
                explicitSelectedUris = emptySet()
                explicitDeselectedUris = emptySet()

                emitResults()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ReleaseSafeLog.error("CleanupVM", "Scan failed", e)
                _uiState.value = CleanupUiState.Error(UiText.Resource(R.string.common_error_generic))
            }
        }
    }

    fun toggleSelection(file: ScannedFile) {
        registerKnownFile(file)
        val category = file.category
        val uri = file.uri
        if (category in selectedGroups) {
            explicitDeselectedUris = if (uri in explicitDeselectedUris) {
                explicitDeselectedUris - uri
            } else {
                explicitDeselectedUris + uri
            }
        } else {
            explicitSelectedUris = if (uri in explicitSelectedUris) {
                explicitSelectedUris - uri
            } else {
                explicitSelectedUris + uri
            }
        }
        emitResults()
    }

    fun toggleGroupSelection(category: MediaCategory) {
        val state = _uiState.value as? CleanupUiState.Results ?: return
        val group = state.groups.find { it.category == category } ?: return
        if (group.selectedCount == group.itemCount) {
            selectedGroups = selectedGroups - category
        } else {
            selectedGroups = selectedGroups + category
        }
        explicitSelectedUris = explicitSelectedUris.filterNot { fileCategoryByUri[it] == category }.toSet()
        explicitDeselectedUris = explicitDeselectedUris.filterNot { fileCategoryByUri[it] == category }.toSet()
        emitResults()
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
        if (state.selectedCount == 0) return

        viewModelScope.launch {
            val uris = resolveSelectedUris()
            if (uris.isEmpty()) return@launch
            pendingDeleteUris = uris.toSet()
            pendingSelectionSnapshot = snapshotSelection()
            _uiState.value = CleanupUiState.Deleting(uris.size)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                _deleteRequestUris.emit(uris)
                return@launch
            }

            try {
                val deletedUris = storageCleanup.deleteLegacy(uris)
                if (deletedUris.isNotEmpty()) {
                    onDeleteSuccess(freedBytesFor(deletedUris))
                } else {
                    restorePendingSelection(UiText.Resource(R.string.cleanup_delete_failed))
                }
            } catch (error: StorageDeleteFailure) {
                if (error.deletedUris.isNotEmpty()) {
                    onDeleteSuccess(freedBytesFor(error.deletedUris))
                } else {
                    restorePendingSelection(
                        UiText.Resource(
                            if (error.recoverable) {
                                R.string.cleanup_delete_permission_error
                            } else {
                                R.string.cleanup_delete_failed
                            }
                        )
                    )
                }
            } catch (error: RecoverableSecurityException) {
                restorePendingSelection(UiText.Resource(R.string.cleanup_delete_permission_error))
            } catch (error: SecurityException) {
                restorePendingSelection(UiText.Resource(R.string.cleanup_delete_permission_error))
            } catch (error: Exception) {
                ReleaseSafeLog.error("CleanupVM", "Delete failed", error)
                restorePendingSelection(UiText.Resource(R.string.cleanup_delete_failed))
            }
        }
    }

    fun onDeleteConfirmed() {
        viewModelScope.launch {
            delay(200)
            onDeleteSuccess(verifiedDeletedBytes())
        }
    }

    fun onDeleteCancelled() {
        pendingDeleteUris = emptySet()
        pendingSelectionSnapshot?.restore()
        pendingSelectionSnapshot = null
        emitResults()
    }

    fun onDeleteFailed(message: UiText) {
        restorePendingSelection(message)
    }

    private companion object {
        private const val SELECTED_FILTER_KEY = "cleanup_selected_filter"
    }

    private fun onDeleteSuccess(freedBytes: Long) {
        viewModelScope.launch {
            _uiState.value = CleanupUiState.Success(freedBytes)
            pendingDeleteUris = emptySet()
            pendingSelectionSnapshot = null
            delay(1800)
            scan() // re-scan to show updated list
        }
    }

    private fun emitResults() {
        val expandedByCategory = groups()
            .associate { it.category to it.expanded }
        val explicitSelectedCounts = countsByCategory(explicitSelectedUris)
        val explicitDeselectedCounts = countsByCategory(explicitDeselectedUris)
        val groups = groupedFiles.mapIndexed { index, group ->
            val expanded = expandedByCategory[group.category] ?: (index == 0 && expandedByCategory.isEmpty())
            val selectedCount = if (group.category in selectedGroups) {
                (group.itemCount - (explicitDeselectedCounts[group.category] ?: 0)).coerceAtLeast(0)
            } else {
                explicitSelectedCounts[group.category] ?: 0
            }
            group.copy(
                expanded = expanded,
                selectedCount = selectedCount
            )
        }

        val selectedCount = groups.sumOf { it.selectedCount }
        val groupSelectedBytes = groups
            .filter { it.category in selectedGroups }
            .sumOf { it.totalBytes }
        val explicitSelectedBytes = explicitSelectedUris
            .filter { uri -> fileCategoryByUri[uri] !in selectedGroups }
            .sumOf { uri -> fileSizeByUri[uri] ?: 0L }
        val explicitDeselectedBytes = explicitDeselectedUris
            .filter { uri -> fileCategoryByUri[uri] in selectedGroups }
            .sumOf { uri -> fileSizeByUri[uri] ?: 0L }
        val selectedSize = (groupSelectedBytes + explicitSelectedBytes - explicitDeselectedBytes)
            .coerceAtLeast(0L)

        val currentPct = (currentStorageUsed.toFloat() / currentStorageTotal) * 100f
        val projectedUsed = (currentStorageUsed - selectedSize).coerceAtLeast(0L)
        val projectedPct = (projectedUsed.toFloat() / currentStorageTotal) * 100f

        _uiState.value = CleanupUiState.Results(
            groups = groups,
            selectedCount = selectedCount,
            selectedSize = selectedSize,
            totalSize = totalScannedSize,
            totalCount = groups.sumOf { it.itemCount },
            currentUsagePercent = currentPct,
            projectedUsagePercent = projectedPct,
            maxFileSizeBytes = maxFileSizeBytes,
            pagerGeneration = pagerGeneration
        )
    }

    private fun freedBytesFor(uris: Set<String>): Long {
        if (uris.isEmpty()) return 0L
        return uris.sumOf { uri -> fileSizeByUri[uri] ?: 0L }
    }

    private suspend fun verifiedDeletedBytes(): Long {
        val uris = pendingDeleteUris
        if (uris.isEmpty()) return 0L
        val remainingUris = storageCleanup.findExistingUris(uris)
        return freedBytesFor(uris - remainingUris)
    }

    // Helper to avoid smart-cast issues
    private fun groups(): List<FileGroup> {
        return (_uiState.value as? CleanupUiState.Results)?.groups ?: emptyList()
    }

    fun pagerFlowFor(category: MediaCategory): Flow<PagingData<ScannedFile>> =
        pagerFlows[category] ?: flowOf(PagingData.empty())

    fun isSelected(file: ScannedFile): Boolean {
        registerKnownFile(file)
        return if (file.category in selectedGroups) {
            file.uri !in explicitDeselectedUris
        } else {
            file.uri in explicitSelectedUris
        }
    }

    private fun registerKnownFile(file: ScannedFile) {
        fileSizeByUri[file.uri] = file.sizeBytes
        fileCategoryByUri[file.uri] = file.category
    }

    private suspend fun resolveSelectedUris(): List<String> {
        val query = currentQuery ?: return emptyList()
        val explicitSelections = explicitSelectedUris
            .filter { uri -> fileCategoryByUri[uri] !in selectedGroups }
            .toMutableSet()
        selectedGroups.forEach { category ->
            val groupUris = storageCleanup.getCleanupGroupUris(query, category)
            explicitSelections += groupUris.filterNot { it in explicitDeselectedUris }
        }
        return explicitSelections.toList()
    }

    private fun countsByCategory(uris: Set<String>): Map<MediaCategory, Int> =
        uris.groupingBy { uri -> fileCategoryByUri[uri] }
            .eachCount()
            .filterKeys { it != null }
            .mapKeys { it.key!! }

    private fun restorePendingSelection(message: UiText) {
        pendingDeleteUris = emptySet()
        pendingSelectionSnapshot?.restore()
        pendingSelectionSnapshot = null
        _uiState.value = CleanupUiState.Error(message)
    }

    private fun snapshotSelection(): CleanupSelectionSnapshot = CleanupSelectionSnapshot(
        selectedGroups = selectedGroups,
        explicitSelectedUris = explicitSelectedUris,
        explicitDeselectedUris = explicitDeselectedUris
    )

    private fun defaultFilterValue(): Long = when (cleanupType) {
        CleanupType.LARGE_FILES -> 50L * 1024 * 1024
        CleanupType.OLD_DOWNLOADS -> 30L * 86_400_000
        CleanupType.APK_FILES -> 0L
    }

    private fun CleanupType.toScanSource(): CleanupScanSource = when (this) {
        CleanupType.LARGE_FILES -> CleanupScanSource.LARGE_FILES
        CleanupType.OLD_DOWNLOADS -> CleanupScanSource.OLD_DOWNLOADS
        CleanupType.APK_FILES -> CleanupScanSource.APK_FILES
    }

    private data class CleanupSelectionSnapshot(
        val selectedGroups: Set<MediaCategory>,
        val explicitSelectedUris: Set<String>,
        val explicitDeselectedUris: Set<String>
    )

    private fun CleanupSelectionSnapshot.restore() {
        this@CleanupViewModel.selectedGroups = this.selectedGroups
        this@CleanupViewModel.explicitSelectedUris = this.explicitSelectedUris
        this@CleanupViewModel.explicitDeselectedUris = this.explicitDeselectedUris
    }
}
