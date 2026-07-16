package com.runcheck.ui.storage.cleanup

import android.app.PendingIntent
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.runcheck.R
import com.runcheck.domain.model.CleanupScanQuery
import com.runcheck.domain.model.CleanupScanSource
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import com.runcheck.domain.model.StorageDeleteFailure
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.domain.usecase.StorageCleanupUseCase
import com.runcheck.ui.common.UiText
import com.runcheck.util.ReleaseSafeLog
import com.runcheck.util.api29RecoverableDeleteAction
import com.runcheck.util.getEnumOrDefault
import com.runcheck.util.getIntOrDefault
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("LargeClass") // Owns one cleanup screen state machine, including persisted delete consent.
class CleanupViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val storageCleanup: StorageCleanupUseCase,
        private val observeProAccess: ObserveProAccessUseCase,
        private val isProUser: IsProUserUseCase,
    ) : ViewModel() {
        val cleanupType: CleanupType =
            savedStateHandle.getEnumOrDefault("type", CleanupType.LARGE_FILES)

        private val _uiState = MutableStateFlow<CleanupUiState>(CleanupUiState.Idle)
        val uiState: StateFlow<CleanupUiState> = _uiState.asStateFlow()

        private val _deleteRequestUris = MutableSharedFlow<List<String>>()
        val deleteRequestUris: SharedFlow<List<String>> = _deleteRequestUris.asSharedFlow()

        private val _legacyDeleteConsentRequests = MutableSharedFlow<PendingIntent>()
        val legacyDeleteConsentRequests: SharedFlow<PendingIntent> =
            _legacyDeleteConsentRequests.asSharedFlow()

        private val _legacyDeleteConfirmationCount = MutableStateFlow<Int?>(null)
        val legacyDeleteConfirmationCount: StateFlow<Int?> =
            _legacyDeleteConfirmationCount.asStateFlow()

        private var selectedFilter: Int
            get() = savedStateHandle.getIntOrDefault(SELECTED_FILTER_KEY, cleanupType.defaultFilterIndex)
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
        private var pendingDeleteUris: Set<String>
            get() = savedStateHandle.get<ArrayList<String>>(PENDING_DELETE_URIS_KEY)?.toSet().orEmpty()
            set(value) {
                savedStateHandle[PENDING_DELETE_URIS_KEY] = ArrayList(value)
            }
        private var activeDeleteRequestUris: Set<String>
            get() = savedStateHandle.get<ArrayList<String>>(ACTIVE_DELETE_URIS_KEY)?.toSet().orEmpty()
            set(value) {
                savedStateHandle[ACTIVE_DELETE_URIS_KEY] = ArrayList(value)
            }
        private var confirmedDeleteRequestUris: Set<String>
            get() = savedStateHandle.get<ArrayList<String>>(CONFIRMED_DELETE_URIS_KEY)?.toSet().orEmpty()
            set(value) {
                savedStateHandle[CONFIRMED_DELETE_URIS_KEY] = ArrayList(value)
            }
        private var currentQuery: CleanupScanQuery? = null
        private var pagerFlows: Map<MediaCategory, Flow<PagingData<ScannedFile>>> = emptyMap()
        private var pagerGeneration: Int = 0
        private var selectedGroups: Set<MediaCategory> = emptySet()
        private var explicitSelectedUris: Set<String> = emptySet()
        private var explicitDeselectedUris: Set<String> = emptySet()
        private var selectionToRestoreAfterScan: CleanupSelectionSnapshot? = null
        private var pendingSelectionSnapshot: CleanupSelectionSnapshot?
            get() {
                val selectedGroupNames =
                    savedStateHandle.get<ArrayList<String>>(PENDING_SELECTED_GROUPS_KEY)
                        ?: return null
                val metadataUris =
                    savedStateHandle.get<ArrayList<String>>(PENDING_METADATA_URIS_KEY).orEmpty()
                val categoryNames =
                    savedStateHandle.get<ArrayList<String>>(PENDING_URI_CATEGORIES_KEY).orEmpty()
                val sizes = savedStateHandle.get<LongArray>(PENDING_URI_SIZES_KEY) ?: longArrayOf()
                val uriMetadata =
                    metadataUris
                        .mapIndexedNotNull { index, uri ->
                            val category =
                                categoryNames
                                    .getOrNull(index)
                                    ?.let { MediaCategory.valueOf(it) }
                                    ?: return@mapIndexedNotNull null
                            uri to (category to sizes.getOrElse(index) { 0L })
                        }.toMap()
                return CleanupSelectionSnapshot(
                    selectedGroups = selectedGroupNames.map { MediaCategory.valueOf(it) }.toSet(),
                    explicitSelectedUris =
                        savedStateHandle.get<ArrayList<String>>(PENDING_SELECTED_URIS_KEY)?.toSet().orEmpty(),
                    explicitDeselectedUris =
                        savedStateHandle.get<ArrayList<String>>(PENDING_DESELECTED_URIS_KEY)?.toSet().orEmpty(),
                    uriMetadata = uriMetadata,
                )
            }
            set(value) {
                if (value == null) {
                    savedStateHandle.remove<ArrayList<String>>(PENDING_SELECTED_GROUPS_KEY)
                    savedStateHandle.remove<ArrayList<String>>(PENDING_SELECTED_URIS_KEY)
                    savedStateHandle.remove<ArrayList<String>>(PENDING_DESELECTED_URIS_KEY)
                    savedStateHandle.remove<ArrayList<String>>(PENDING_METADATA_URIS_KEY)
                    savedStateHandle.remove<ArrayList<String>>(PENDING_URI_CATEGORIES_KEY)
                    savedStateHandle.remove<LongArray>(PENDING_URI_SIZES_KEY)
                    return
                }
                val metadataEntries = value.uriMetadata.entries.toList()
                savedStateHandle[PENDING_SELECTED_GROUPS_KEY] = ArrayList(value.selectedGroups.map(MediaCategory::name))
                savedStateHandle[PENDING_SELECTED_URIS_KEY] = ArrayList(value.explicitSelectedUris)
                savedStateHandle[PENDING_DESELECTED_URIS_KEY] = ArrayList(value.explicitDeselectedUris)
                savedStateHandle[PENDING_METADATA_URIS_KEY] = ArrayList(metadataEntries.map { it.key })
                savedStateHandle[PENDING_URI_CATEGORIES_KEY] =
                    ArrayList(metadataEntries.map { it.value.first.name })
                savedStateHandle[PENDING_URI_SIZES_KEY] = metadataEntries.map { it.value.second }.toLongArray()
            }
        private var scanJob: Job? = null

        init {
            viewModelScope.launch {
                observeProAccess()
                    .distinctUntilChanged()
                    .collect { isPro ->
                        if (isPro) {
                            scan()
                        } else {
                            revokeProAccess()
                        }
                    }
            }
        }

        fun setFilter(index: Int) {
            selectedFilter = index
            scan()
        }

        fun getSelectedFilterIndex(): Int = selectedFilter

        fun scan() {
            scanJob?.cancel()
            scanJob =
                viewModelScope.launch {
                    if (!isProUser()) {
                        _uiState.value =
                            CleanupUiState.Error(
                                UiText.Resource(R.string.pro_feature_locked_generic),
                            )
                        return@launch
                    }
                    if (isVersionRestrictedCleanup() && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        _uiState.value = CleanupUiState.UnsupportedVersion
                        return@launch
                    }
                    _uiState.value = CleanupUiState.Scanning()
                    try {
                        performScan()
                        pendingDeleteUris.takeIf { it.isNotEmpty() }?.let { uris ->
                            if (savedStateHandle.get<Boolean>(PENDING_LEGACY_CONFIRMATION_KEY) == true) {
                                _legacyDeleteConfirmationCount.value = uris.size
                            } else {
                                _uiState.value = CleanupUiState.Deleting(uris.size)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        ReleaseSafeLog.error("CleanupVM", "Scan failed", e)
                        _uiState.value = CleanupUiState.Error(UiText.Resource(R.string.common_error_generic))
                    }
                }
        }

        @Suppress("LongMethod")
        private suspend fun performScan() {
            val storageState = storageCleanup.getCurrentStorageState()
            currentStorageTotal = storageState.totalBytes.coerceAtLeast(1L)
            currentStorageUsed = storageState.usedBytes

            val filterValue =
                cleanupType.filterOptions
                    .getOrNull(selectedFilter)
                    ?.value
                    ?: defaultFilterValue()
            val query =
                CleanupScanQuery(
                    source = cleanupType.toScanSource(),
                    filterValue = filterValue,
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
                selectionToRestoreAfterScan = null
                _uiState.value = CleanupUiState.Empty
                return
            }

            totalScannedSize = summary.totalBytes
            maxFileSizeBytes = summary.maxFileSizeBytes
            groupedFiles =
                summary.groups
                    .map { group ->
                        FileGroup(
                            category = group.category,
                            itemCount = group.itemCount,
                            totalBytes = group.totalBytes,
                        )
                    }.mapIndexed { index, group ->
                        if (index == 0) group.copy(expanded = true) else group
                    }
            pagerGeneration += 1
            pagerFlows =
                groupedFiles.associate { group ->
                    group.category to
                        storageCleanup
                            .getCleanupItems(query, group.category)
                            .cachedIn(viewModelScope)
                }

            selectedGroups =
                if (cleanupType.preselectAll) {
                    groupedFiles.map { it.category }.toSet()
                } else {
                    emptySet()
                }
            explicitSelectedUris = emptySet()
            explicitDeselectedUris = emptySet()
            selectionToRestoreAfterScan?.restore()
            selectionToRestoreAfterScan = null

            emitResults()
        }

        fun toggleSelection(file: ScannedFile) {
            registerKnownFile(file)
            val category = file.category
            val uri = file.uri
            if (category in selectedGroups) {
                explicitDeselectedUris =
                    if (uri in explicitDeselectedUris) {
                        explicitDeselectedUris - uri
                    } else {
                        explicitDeselectedUris + uri
                    }
            } else {
                explicitSelectedUris =
                    if (uri in explicitSelectedUris) {
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
            val newGroups =
                state.groups.map { group ->
                    if (group.category == category) {
                        group.copy(expanded = !group.expanded)
                    } else {
                        group
                    }
                }
            _uiState.value = state.copy(groups = newGroups)
        }

        fun requestDelete(apiLevel: Int = Build.VERSION.SDK_INT) {
            if (!isProUser()) {
                revokeProAccess()
                return
            }
            val state = _uiState.value as? CleanupUiState.Results ?: return
            if (state.selectedCount == 0) return

            viewModelScope.launch {
                val uris = resolveSelectedUris()
                if (uris.isEmpty()) return@launch
                pendingDeleteUris = uris.toSet()
                activeDeleteRequestUris = emptySet()
                confirmedDeleteRequestUris = emptySet()
                pendingSelectionSnapshot = snapshotSelection()
                if (apiLevel >= Build.VERSION_CODES.R) {
                    _uiState.value = CleanupUiState.Deleting(uris.size)
                    emitNextDeleteRequest()
                } else {
                    savedStateHandle[PENDING_LEGACY_CONFIRMATION_KEY] = true
                    _legacyDeleteConfirmationCount.value = uris.size
                }
            }
        }

        fun confirmLegacyDelete() {
            if (!isProUser()) {
                revokeProAccess()
                return
            }
            val uris = pendingDeleteUris.toList()
            if (uris.isEmpty()) return
            savedStateHandle[PENDING_LEGACY_CONFIRMATION_KEY] = false
            _legacyDeleteConfirmationCount.value = null
            _uiState.value = CleanupUiState.Deleting(uris.size)
            viewModelScope.launch { performLegacyDelete(uris) }
        }

        fun onLegacyDeleteConsentGranted() {
            if (!isProUser()) {
                revokeProAccess()
                return
            }
            val uris = pendingDeleteUris.toList()
            if (uris.isEmpty()) return
            viewModelScope.launch { performLegacyDelete(uris) }
        }

        private suspend fun performLegacyDelete(uris: List<String>) {
            try {
                val deletedUris = storageCleanup.deleteLegacy(uris)
                completeLegacyDelete(deletedUris, UiText.Resource(R.string.cleanup_delete_failed))
            } catch (error: StorageDeleteFailure) {
                handleLegacyDeleteFailure(error)
            } catch (error: SecurityException) {
                handleLegacyDeleteSecurityException(error)
            } catch (error: Exception) {
                ReleaseSafeLog.error("CleanupVM", "Delete failed", error)
                restorePendingSelection(UiText.Resource(R.string.cleanup_delete_failed))
            }
        }

        private fun completeLegacyDelete(
            deletedUris: Set<String>,
            emptyResultMessage: UiText,
        ) {
            if (deletedUris.isEmpty()) {
                restorePendingSelection(emptyResultMessage)
                return
            }
            onDeleteSuccess(
                freedBytes = freedBytesFor(deletedUris),
                remainingSelectedUris = pendingDeleteUris - deletedUris,
            )
        }

        private fun handleLegacyDeleteFailure(error: StorageDeleteFailure) {
            val message =
                UiText.Resource(
                    if (error.recoverable) {
                        R.string.cleanup_delete_permission_error
                    } else {
                        R.string.cleanup_delete_failed
                    },
                )
            completeLegacyDelete(error.deletedUris, message)
        }

        private suspend fun handleLegacyDeleteSecurityException(error: SecurityException) {
            val consentRequest =
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    api29RecoverableDeleteAction(error)
                } else {
                    null
                }
            if (consentRequest != null) {
                _legacyDeleteConsentRequests.emit(consentRequest)
                return
            }
            val message =
                if (isRecoverableDeleteSecurityException(error)) {
                    "Delete permission denied (recoverable)"
                } else {
                    "Delete permission denied"
                }
            ReleaseSafeLog.error("CleanupVM", message, error)
            restorePendingSelection(UiText.Resource(R.string.cleanup_delete_permission_error))
        }

        fun onDeleteConfirmed() {
            if (!isProUser()) {
                revokeProAccess()
                return
            }
            viewModelScope.launch {
                scanJob?.join()
                if (activeDeleteRequestUris.isEmpty() && confirmedDeleteRequestUris.isEmpty()) {
                    finishDeleteAttempt()
                    return@launch
                }
                if (activeDeleteRequestUris.isNotEmpty()) {
                    confirmedDeleteRequestUris += activeDeleteRequestUris
                    activeDeleteRequestUris = emptySet()
                }
                if (pendingDeleteUris.any { it !in confirmedDeleteRequestUris }) {
                    emitNextDeleteRequest()
                } else {
                    finishDeleteAttempt()
                }
            }
        }

        fun onDeleteCancelled() {
            viewModelScope.launch {
                scanJob?.join()
                savedStateHandle[PENDING_LEGACY_CONFIRMATION_KEY] = false
                _legacyDeleteConfirmationCount.value = null
                activeDeleteRequestUris = emptySet()
                if (confirmedDeleteRequestUris.isEmpty()) {
                    pendingDeleteUris = emptySet()
                    pendingSelectionSnapshot?.restore()
                    pendingSelectionSnapshot = null
                    emitResults()
                } else {
                    finishDeleteAttempt()
                }
            }
        }

        fun onDeleteFailed(message: UiText) {
            if (confirmedDeleteRequestUris.isEmpty()) {
                restorePendingSelection(message)
            } else {
                viewModelScope.launch { finishDeleteAttempt() }
            }
        }

        private companion object {
            private const val SELECTED_FILTER_KEY = "cleanup_selected_filter"
            private const val PENDING_DELETE_URIS_KEY = "cleanup_pending_delete_uris"
            private const val ACTIVE_DELETE_URIS_KEY = "cleanup_active_delete_uris"
            private const val CONFIRMED_DELETE_URIS_KEY = "cleanup_confirmed_delete_uris"
            private const val PENDING_SELECTED_GROUPS_KEY = "cleanup_pending_selected_groups"
            private const val PENDING_SELECTED_URIS_KEY = "cleanup_pending_selected_uris"
            private const val PENDING_DESELECTED_URIS_KEY = "cleanup_pending_deselected_uris"
            private const val PENDING_METADATA_URIS_KEY = "cleanup_pending_metadata_uris"
            private const val PENDING_URI_CATEGORIES_KEY = "cleanup_pending_uri_categories"
            private const val PENDING_URI_SIZES_KEY = "cleanup_pending_uri_sizes"
            private const val PENDING_LEGACY_CONFIRMATION_KEY = "cleanup_pending_legacy_confirmation"
            private const val RECOVERABLE_SECURITY_EXCEPTION =
                "android.app.RecoverableSecurityException"
            private const val MAX_DELETE_REQUEST_SIZE = 2_000
        }

        private fun revokeProAccess() {
            scanJob?.cancel()
            scanJob = null
            groupedFiles = emptyList()
            pagerFlows = emptyMap()
            currentQuery = null
            selectedGroups = emptySet()
            explicitSelectedUris = emptySet()
            explicitDeselectedUris = emptySet()
            pendingDeleteUris = emptySet()
            activeDeleteRequestUris = emptySet()
            confirmedDeleteRequestUris = emptySet()
            pendingSelectionSnapshot = null
            selectionToRestoreAfterScan = null
            savedStateHandle[PENDING_LEGACY_CONFIRMATION_KEY] = false
            _legacyDeleteConfirmationCount.value = null
            _uiState.value = CleanupUiState.Error(UiText.Resource(R.string.pro_feature_locked_generic))
        }

        private fun onDeleteSuccess(
            freedBytes: Long,
            remainingSelectedUris: Set<String> = emptySet(),
        ) {
            viewModelScope.launch {
                _uiState.value = CleanupUiState.Success(freedBytes)
                selectionToRestoreAfterScan =
                    pendingSelectionSnapshot?.remainingSelection(remainingSelectedUris)
                pendingDeleteUris = emptySet()
                activeDeleteRequestUris = emptySet()
                confirmedDeleteRequestUris = emptySet()
                pendingSelectionSnapshot = null
                delay(1800)
                scan() // re-scan to show updated list
            }
        }

        private fun isRecoverableDeleteSecurityException(error: SecurityException): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                error.javaClass.name == RECOVERABLE_SECURITY_EXCEPTION

        private fun emitResults() {
            val expandedByCategory =
                groups()
                    .associate { it.category to it.expanded }
            val explicitSelectedCounts = countsByCategory(explicitSelectedUris)
            val explicitDeselectedCounts = countsByCategory(explicitDeselectedUris)
            val groups =
                groupedFiles.mapIndexed { index, group ->
                    val expanded = expandedByCategory[group.category] ?: (index == 0 && expandedByCategory.isEmpty())
                    val selectedCount =
                        if (group.category in selectedGroups) {
                            (group.itemCount - (explicitDeselectedCounts[group.category] ?: 0)).coerceAtLeast(0)
                        } else {
                            explicitSelectedCounts[group.category] ?: 0
                        }
                    group.copy(
                        expanded = expanded,
                        selectedCount = selectedCount,
                    )
                }

            val selectedCount = groups.sumOf { it.selectedCount }
            val groupSelectedBytes =
                groups
                    .filter { it.category in selectedGroups }
                    .sumOf { it.totalBytes }
            val explicitSelectedBytes =
                explicitSelectedUris
                    .filter { uri -> fileCategoryByUri[uri] !in selectedGroups }
                    .sumOf { uri -> fileSizeByUri[uri] ?: 0L }
            val explicitDeselectedBytes =
                explicitDeselectedUris
                    .filter { uri -> fileCategoryByUri[uri] in selectedGroups }
                    .sumOf { uri -> fileSizeByUri[uri] ?: 0L }
            val selectedSize =
                (groupSelectedBytes + explicitSelectedBytes - explicitDeselectedBytes)
                    .coerceAtLeast(0L)

            val currentPct = (currentStorageUsed.toFloat() / currentStorageTotal) * 100f
            val projectedUsed = (currentStorageUsed - selectedSize).coerceAtLeast(0L)
            val projectedPct = (projectedUsed.toFloat() / currentStorageTotal) * 100f

            _uiState.value =
                CleanupUiState.Results(
                    groups = groups,
                    selectedCount = selectedCount,
                    selectedSize = selectedSize,
                    totalSize = totalScannedSize,
                    totalCount = groups.sumOf { it.itemCount },
                    currentUsagePercent = currentPct,
                    projectedUsagePercent = projectedPct,
                    maxFileSizeBytes = maxFileSizeBytes,
                    pagerGeneration = pagerGeneration,
                )
        }

        private fun freedBytesFor(uris: Set<String>): Long {
            if (uris.isEmpty()) return 0L
            return uris.sumOf { uri -> fileSizeByUri[uri] ?: 0L }
        }

        private suspend fun verifyDeleteResult(): VerifiedDeleteResult {
            val uris = pendingDeleteUris
            if (uris.isEmpty()) return VerifiedDeleteResult(0L, emptySet())
            val remainingUris = storageCleanup.findExistingUris(uris)
            val persistedMetadata = pendingSelectionSnapshot?.uriMetadata.orEmpty()
            val freedBytes =
                (uris - remainingUris).sumOf { uri ->
                    persistedMetadata[uri]?.second ?: fileSizeByUri[uri] ?: 0L
                }
            return VerifiedDeleteResult(freedBytes, remainingUris)
        }

        private suspend fun emitNextDeleteRequest() {
            val nextBatch =
                pendingDeleteUris
                    .asSequence()
                    .filterNot { it in confirmedDeleteRequestUris }
                    .take(MAX_DELETE_REQUEST_SIZE)
                    .toSet()
            if (nextBatch.isEmpty()) {
                finishDeleteAttempt()
                return
            }
            activeDeleteRequestUris = nextBatch
            _deleteRequestUris.emit(nextBatch.toList())
        }

        private suspend fun finishDeleteAttempt() {
            try {
                delay(200)
                val result = verifyDeleteResult()
                onDeleteSuccess(result.freedBytes, result.remainingUris)
            } catch (error: SecurityException) {
                ReleaseSafeLog.error("CleanupVM", "Delete result access denied", error)
                restorePendingSelection(UiText.Resource(R.string.cleanup_delete_permission_error))
            } catch (error: Exception) {
                ReleaseSafeLog.error("CleanupVM", "Delete result verification failed", error)
                restorePendingSelection(UiText.Resource(R.string.cleanup_delete_failed))
            }
        }

        // Helper to avoid smart-cast issues
        private fun groups(): List<FileGroup> = (_uiState.value as? CleanupUiState.Results)?.groups ?: emptyList()

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
            val explicitSelections =
                explicitSelectedUris
                    .filter { uri -> fileCategoryByUri[uri] !in selectedGroups }
                    .toMutableSet()
            selectedGroups.forEach { category ->
                val groupFileSizes = storageCleanup.getCleanupGroupFileSizes(query, category)
                groupFileSizes.forEach { (uri, sizeBytes) ->
                    fileSizeByUri[uri] = sizeBytes
                    fileCategoryByUri[uri] = category
                }
                explicitSelections += groupFileSizes.keys.filterNot { it in explicitDeselectedUris }
            }
            return explicitSelections.toList()
        }

        private fun countsByCategory(uris: Set<String>): Map<MediaCategory, Int> =
            uris
                .groupingBy { uri -> fileCategoryByUri[uri] }
                .eachCount()
                .filterKeys { it != null }
                .mapKeys { it.key!! }

        private fun restorePendingSelection(message: UiText) {
            pendingDeleteUris = emptySet()
            activeDeleteRequestUris = emptySet()
            confirmedDeleteRequestUris = emptySet()
            pendingSelectionSnapshot?.restore()
            pendingSelectionSnapshot = null
            _uiState.value = CleanupUiState.Error(message)
        }

        private fun snapshotSelection(): CleanupSelectionSnapshot =
            CleanupSelectionSnapshot(
                selectedGroups = selectedGroups,
                explicitSelectedUris = explicitSelectedUris,
                explicitDeselectedUris = explicitDeselectedUris,
                uriMetadata =
                    (pendingDeleteUris + explicitDeselectedUris)
                        .mapNotNull { uri ->
                            val category = fileCategoryByUri[uri] ?: return@mapNotNull null
                            uri to (category to (fileSizeByUri[uri] ?: 0L))
                        }.toMap(),
            )

        private fun isVersionRestrictedCleanup(): Boolean =
            cleanupType in setOf(CleanupType.OLD_DOWNLOADS, CleanupType.APK_FILES)

        private fun defaultFilterValue(): Long =
            cleanupType.filterOptions
                .getOrNull(cleanupType.defaultFilterIndex)
                ?.value
                ?: 0L

        private fun CleanupType.toScanSource(): CleanupScanSource =
            when (this) {
                CleanupType.LARGE_FILES -> CleanupScanSource.LARGE_FILES
                CleanupType.OLD_DOWNLOADS -> CleanupScanSource.OLD_DOWNLOADS
                CleanupType.APK_FILES -> CleanupScanSource.APK_FILES
            }

        private data class CleanupSelectionSnapshot(
            val selectedGroups: Set<MediaCategory>,
            val explicitSelectedUris: Set<String>,
            val explicitDeselectedUris: Set<String>,
            val uriMetadata: Map<String, Pair<MediaCategory, Long>>,
        )

        private data class VerifiedDeleteResult(
            val freedBytes: Long,
            val remainingUris: Set<String>,
        )

        private fun CleanupSelectionSnapshot.remainingSelection(
            remainingUris: Set<String>,
        ): CleanupSelectionSnapshot? {
            if (remainingUris.isEmpty()) return null
            return CleanupSelectionSnapshot(
                selectedGroups = emptySet(),
                explicitSelectedUris = remainingUris,
                explicitDeselectedUris = emptySet(),
                uriMetadata = uriMetadata.filterKeys { it in remainingUris },
            )
        }

        private fun CleanupSelectionSnapshot.restore() {
            this@CleanupViewModel.selectedGroups = this.selectedGroups
            this@CleanupViewModel.explicitSelectedUris = this.explicitSelectedUris
            this@CleanupViewModel.explicitDeselectedUris = this.explicitDeselectedUris
            uriMetadata.forEach { (uri, metadata) ->
                this@CleanupViewModel.fileCategoryByUri[uri] = metadata.first
                this@CleanupViewModel.fileSizeByUri[uri] = metadata.second
            }
        }
    }
