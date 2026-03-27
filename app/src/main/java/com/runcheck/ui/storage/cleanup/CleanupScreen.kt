package com.runcheck.ui.storage.cleanup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import kotlinx.coroutines.flow.Flow
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.runcheck.R
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.common.resolve
import com.runcheck.ui.storage.buildMediaDeleteRequest
import com.runcheck.ui.storage.MediaDeleteRequestResult
import com.runcheck.ui.components.ContentContainer
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.theme.spacing

@Composable
fun CleanupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CleanupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cleanupType = viewModel.cleanupType
    val context = LocalContext.current

    // ActivityResult launcher for system delete dialog
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
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
                    viewModel.onDeleteFailed(com.runcheck.ui.common.UiText.Resource(requestResult.messageRes))
                }
            }
        }
    }

    // Re-scan when returning from settings after granting permission
    LifecycleResumeEffect(viewModel) {
        if (uiState is CleanupUiState.NeedsStoragePermission &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Environment.isExternalStorageManager()
        ) {
            viewModel.scan()
        }
        onPauseOrDispose { }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                DetailTopBar(
                    title = stringResource(cleanupType.titleRes),
                    onBack = onBack
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
                    onDelete = { viewModel.requestDelete() }
                )
            }
        ) { paddingValues ->
            ContentContainer(modifier = Modifier.padding(paddingValues)) {
                CleanupScreenBody(
                    uiState = uiState,
                    cleanupType = cleanupType,
                    viewModel = viewModel
                )
            }
        }

        // Success overlay on top of everything
        val successState = uiState as? CleanupUiState.Success
        CleanupSuccessOverlay(
            visible = successState != null,
            freedBytes = successState?.freedBytes ?: 0L
        )
    }
}

@Composable
private fun CleanupScreenBody(
    uiState: CleanupUiState,
    cleanupType: CleanupType,
    viewModel: CleanupViewModel
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.base)
    ) {
        // Filter chips
        if (cleanupType.filterOptions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
            ) {
                cleanupType.filterOptions.forEachIndexed { index, option ->
                    FilterChip(
                        selected = viewModel.getSelectedFilterIndex() == index,
                        onClick = { viewModel.setFilter(index) },
                        label = { Text(stringResource(option.labelRes)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        }

        when (val state = uiState) {
            is CleanupUiState.Idle,
            is CleanupUiState.Scanning -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = context.getString(R.string.a11y_scanning_files); liveRegion = LiveRegionMode.Polite },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is CleanupUiState.NeedsStoragePermission -> {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.base),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                    ) {
                        Text(
                            text = stringResource(R.string.cleanup_storage_permission_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.cleanup_storage_permission_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.cleanup_storage_permission_reason),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                } catch (_: Exception) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    )
                                }
                            }
                        ) {
                            Text(stringResource(R.string.cleanup_storage_permission_action))
                        }
                    }
                }
            }

            is CleanupUiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.cleanup_no_files),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.cleanup_no_files_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is CleanupUiState.UnsupportedVersion -> {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.base),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                    ) {
                        Text(
                            text = stringResource(R.string.cleanup_not_supported_version_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.cleanup_not_supported_version_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is CleanupUiState.Results -> {
                CleanupResultsList(
                    state = state,
                    pagerFlowFor = viewModel::pagerFlowFor,
                    isSelected = viewModel::isSelected,
                    onToggleGroupExpanded = viewModel::toggleGroupExpanded,
                    onToggleGroupSelection = viewModel::toggleGroupSelection,
                    onToggleSelection = viewModel::toggleSelection
                )
            }

            is CleanupUiState.Deleting -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = context.getString(R.string.a11y_deleting_files); liveRegion = LiveRegionMode.Polite },
                    contentAlignment = Alignment.Center
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
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message.resolve(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                        TextButton(onClick = { viewModel.scan() }) {
                            Text(text = stringResource(R.string.common_retry))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanupResultsList(
    state: CleanupUiState.Results,
    pagerFlowFor: (MediaCategory) -> Flow<PagingData<ScannedFile>>,
    isSelected: (ScannedFile) -> Boolean,
    onToggleGroupExpanded: (MediaCategory) -> Unit,
    onToggleGroupSelection: (MediaCategory) -> Unit,
    onToggleSelection: (ScannedFile) -> Unit
) {
    val context = LocalContext.current
    val maxFileSize = state.maxFileSizeBytes.coerceAtLeast(1L)
    val pagedItemsByCategory = state.groups
        .filter { it.expanded }
        .associate { group ->
            val pagingFlow = remember(group.category, state.pagerGeneration) {
                pagerFlowFor(group.category)
            }
            group.category to pagingFlow.collectAsLazyPagingItems()
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Summary header
        item(key = "header") {
            Text(
                text = pluralStringResource(R.plurals.cleanup_found_files, state.totalCount, state.totalCount, formatStorageSize(context, state.totalSize)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.sm)
            )
        }

        // Flattened file groups with category headers
        state.groups.forEachIndexed { groupIndex, group ->
            item(key = "group_${group.category}") {
                if (groupIndex > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                }
                val onToggleExpanded = remember(group.category) {
                    { onToggleGroupExpanded(group.category) }
                }
                val onToggleGroupSelectionCallback = remember(group.category) {
                    { onToggleGroupSelection(group.category) }
                }
                CategoryGroup(
                    group = group,
                    onToggleExpanded = onToggleExpanded,
                    onToggleGroupSelection = onToggleGroupSelectionCallback
                )
            }

            if (group.expanded) {
                expandedGroupItems(
                    group = group,
                    lazyItems = pagedItemsByCategory[group.category],
                    pagerGeneration = state.pagerGeneration,
                    maxFileSize = maxFileSize,
                    isSelected = isSelected,
                    onToggleSelection = onToggleSelection
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
    onToggleSelection: (ScannedFile) -> Unit
) {
    if (lazyItems != null && lazyItems.loadState.refresh is LoadState.Loading && lazyItems.itemCount == 0) {
        item(key = "loading_${group.category}_$pagerGeneration") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.base),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    } else if (lazyItems != null) {
        items(
            count = lazyItems.itemCount,
            key = { index -> lazyItems.peek(index)?.uri ?: "${group.category}_$index" },
            contentType = { _ -> "cleanup_file" }
        ) { index ->
            val file = lazyItems[index] ?: return@items
            val onToggle = remember(file.uri) {
                { onToggleSelection(file) }
            }
            Column {
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(start = 56.dp)
                    )
                }
                FileListItem(
                    file = file,
                    isSelected = isSelected(file),
                    maxFileSize = maxFileSize,
                    onToggle = onToggle
                )
            }
        }
    }
}
