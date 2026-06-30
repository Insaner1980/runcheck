package com.runcheck.ui.storage.cleanup

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.runcheck.R
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.common.resolve
import com.runcheck.ui.components.ContentContainer
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.storage.MediaDeleteRequestResult
import com.runcheck.ui.storage.buildMediaDeleteRequest
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.spacing
import kotlinx.coroutines.flow.Flow

@Composable
@Suppress("ViewModelForwarding")
fun CleanupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CleanupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cleanupType = viewModel.cleanupType
    val context = LocalContext.current

    // ActivityResult launcher for system delete dialog
    val deleteLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.onDeleteConfirmed()
            } else {
                viewModel.onDeleteCancelled()
            }
        }

    // Observe delete intents from ViewModel
    LaunchedEffect(viewModel, context) {
        viewModel.deleteRequestUris.collect { uris ->
            when (val requestResult = buildMediaDeleteRequest(context, uris)) {
                is MediaDeleteRequestResult.Ready -> {
                    deleteLauncher.launch(requestResult.request)
                }

                is MediaDeleteRequestResult.Failed -> {
                    viewModel.onDeleteFailed(
                        com.runcheck.ui.common.UiText
                            .Resource(requestResult.messageRes),
                    )
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                DetailTopBar(
                    title = stringResource(cleanupType.titleRes),
                    onBack = onBack,
                )
            },
            bottomBar = {
                val results = uiState as? CleanupUiState.Results
                CleanupBottomBar(
                    visible = results != null && results.selectedCount > 0,
                    selectedSize = results?.selectedSize ?: 0L,
                    selectedCount = results?.selectedCount ?: 0,
                    currentUsagePercent = results?.currentUsagePercent ?: 0f,
                    projectedUsagePercent = results?.projectedUsagePercent ?: 0f,
                    onDelete = { viewModel.requestDelete() },
                )
            },
        ) { paddingValues ->
            ContentContainer(modifier = Modifier.padding(paddingValues)) {
                CleanupScreenBody(
                    state =
                        CleanupScreenBodyState(
                            uiState = uiState,
                            cleanupType = cleanupType,
                            selectedFilterIndex = viewModel.getSelectedFilterIndex(),
                        ),
                    actions =
                        CleanupScreenBodyActions(
                            onFilterSelect = viewModel::setFilter,
                            onScan = viewModel::scan,
                            pagerFlowFor = viewModel::pagerFlowFor,
                            isSelected = viewModel::isSelected,
                            onToggleGroupExpansion = viewModel::toggleGroupExpanded,
                            onToggleGroupSelection = viewModel::toggleGroupSelection,
                            onToggleSelection = viewModel::toggleSelection,
                        ),
                )
            }
        }

        // Success overlay on top of everything
        val successState = uiState as? CleanupUiState.Success
        CleanupSuccessOverlay(
            visible = successState != null,
            freedBytes = successState?.freedBytes ?: 0L,
        )
    }
}

@Composable
private fun CleanupScreenBody(
    state: CleanupScreenBodyState,
    actions: CleanupScreenBodyActions,
) {
    val uiState = state.uiState
    val cleanupType = state.cleanupType
    val selectedFilterIndex = state.selectedFilterIndex
    val onFilterSelect = actions.onFilterSelect
    val onScan = actions.onScan
    val pagerFlowFor = actions.pagerFlowFor
    val isSelected = actions.isSelected
    val onToggleGroupExpansion = actions.onToggleGroupExpansion
    val onToggleGroupSelection = actions.onToggleGroupSelection
    val onToggleSelection = actions.onToggleSelection
    val scanningDescription = stringResource(R.string.a11y_scanning_files)
    val deletingDescription = stringResource(R.string.a11y_deleting_files)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.base),
    ) {
        // Filter chips
        if (cleanupType.filterOptions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                cleanupType.filterOptions.forEachIndexed { index, option ->
                    FilterChip(
                        selected = selectedFilterIndex == index,
                        onClick = { onFilterSelect(index) },
                        label = { Text(stringResource(option.labelRes)) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        }

        when (val state = uiState) {
            is CleanupUiState.Idle,
            is CleanupUiState.Scanning,
            -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .semantics {
                                contentDescription = scanningDescription
                                liveRegion =
                                    LiveRegionMode.Polite
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is CleanupUiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.cleanup_no_files),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.cleanup_no_files_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            is CleanupUiState.UnsupportedVersion -> {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = runcheckCardColors(),
                    elevation = runcheckCardElevation(),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(MaterialTheme.spacing.base),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        Text(
                            text = stringResource(R.string.cleanup_not_supported_version_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.cleanup_not_supported_version_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            is CleanupUiState.Results -> {
                CleanupResultsList(
                    state = state,
                    pagerFlowFor = pagerFlowFor,
                    isSelected = isSelected,
                    onToggleGroupExpansion = onToggleGroupExpansion,
                    onToggleGroupSelection = onToggleGroupSelection,
                    onToggleSelection = onToggleSelection,
                )
            }

            is CleanupUiState.Deleting -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .semantics {
                                contentDescription = deletingDescription
                                liveRegion =
                                    LiveRegionMode.Polite
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is CleanupUiState.Success -> {
                // Handled by overlay
            }

            is CleanupUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message.resolve(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                        TextButton(onClick = onScan) {
                            Text(text = stringResource(R.string.common_retry))
                        }
                    }
                }
            }
        }
    }
}

private data class CleanupScreenBodyState(
    val uiState: CleanupUiState,
    val cleanupType: CleanupType,
    val selectedFilterIndex: Int,
)

private class CleanupScreenBodyActions(
    val onFilterSelect: (Int) -> Unit,
    val onScan: () -> Unit,
    val pagerFlowFor: (MediaCategory) -> Flow<PagingData<ScannedFile>>,
    val isSelected: (ScannedFile) -> Boolean,
    val onToggleGroupExpansion: (MediaCategory) -> Unit,
    val onToggleGroupSelection: (MediaCategory) -> Unit,
    val onToggleSelection: (ScannedFile) -> Unit,
)

@Composable
private fun CleanupResultsList(
    state: CleanupUiState.Results,
    pagerFlowFor: (MediaCategory) -> Flow<PagingData<ScannedFile>>,
    isSelected: (ScannedFile) -> Boolean,
    onToggleGroupExpansion: (MediaCategory) -> Unit,
    onToggleGroupSelection: (MediaCategory) -> Unit,
    onToggleSelection: (ScannedFile) -> Unit,
) {
    val context = LocalContext.current
    val maxFileSize = state.maxFileSizeBytes.coerceAtLeast(1L)
    val pagedItemsByCategory =
        state.groups
            .filter { it.expanded }
            .associate { group ->
                val pagingFlow =
                    remember(group.category, state.pagerGeneration) {
                        pagerFlowFor(group.category)
                    }
                group.category to pagingFlow.collectAsLazyPagingItems()
            }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Summary header
        item(key = "header") {
            Text(
                text =
                    pluralStringResource(
                        R.plurals.cleanup_found_files,
                        state.totalCount,
                        state.totalCount,
                        formatStorageSize(context, state.totalSize),
                    ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.sm),
            )
        }

        // Flattened file groups with category headers
        state.groups.forEachIndexed { groupIndex, group ->
            item(key = "group_${group.category}") {
                if (groupIndex > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    )
                }
                val onToggleExpansion =
                    remember(group.category) {
                        { onToggleGroupExpansion(group.category) }
                    }
                val onToggleGroupSelectionCallback =
                    remember(group.category) {
                        { onToggleGroupSelection(group.category) }
                    }
                CategoryGroup(
                    group = group,
                    onToggleExpansion = onToggleExpansion,
                    onToggleGroupSelection = onToggleGroupSelectionCallback,
                )
            }

            if (group.expanded) {
                expandedGroupItems(
                    group = group,
                    lazyItems = pagedItemsByCategory[group.category],
                    pagerGeneration = state.pagerGeneration,
                    maxFileSize = maxFileSize,
                    isSelected = isSelected,
                    onToggleSelection = onToggleSelection,
                )
            }
        }

        // Bottom spacing for the action bar
        item(key = "spacer") {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private fun LazyListScope.expandedGroupItems(
    group: FileGroup,
    lazyItems: LazyPagingItems<ScannedFile>?,
    pagerGeneration: Int,
    maxFileSize: Long,
    isSelected: (ScannedFile) -> Boolean,
    onToggleSelection: (ScannedFile) -> Unit,
) {
    if (lazyItems != null && lazyItems.loadState.refresh is LoadState.Loading && lazyItems.itemCount == 0) {
        item(key = "loading_${group.category}_$pagerGeneration") {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = MaterialTheme.spacing.base),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    } else if (lazyItems != null) {
        items(
            count = lazyItems.itemCount,
            key = { index -> lazyItems.peek(index)?.uri ?: "${group.category}_$index" },
            contentType = { _ -> "cleanup_file" },
        ) { index ->
            val file = lazyItems[index] ?: return@items
            val onToggle =
                remember(file.uri) {
                    { onToggleSelection(file) }
                }
            Column {
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(start = 56.dp),
                    )
                }
                FileListItem(
                    file = file,
                    isSelected = isSelected(file),
                    maxFileSize = maxFileSize,
                    onToggle = onToggle,
                )
            }
        }
    }
}
