package com.runcheck.ui.storage.cleanup

import android.app.Activity
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.runcheck.R
import com.runcheck.ui.common.formatStorageSize
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
    LaunchedEffect(Unit) {
        viewModel.deleteIntent.collect { pendingIntent ->
            deleteLauncher.launch(
                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            )
        }
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
                    visible = results != null && results.selectedUris.isNotEmpty(),
                    selectedSize = results?.selectedSize ?: 0L,
                    selectedCount = results?.selectedUris?.size ?: 0,
                    currentUsagePercent = results?.currentUsagePercent ?: 0f,
                    projectedUsagePercent = results?.projectedUsagePercent ?: 0f,
                    onDelete = { viewModel.requestDelete() }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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

                    is CleanupUiState.Results -> {
                        CleanupResultsList(
                            state = state,
                            viewModel = viewModel
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
                }
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
private fun CleanupResultsList(
    state: CleanupUiState.Results,
    viewModel: CleanupViewModel
) {
    val context = LocalContext.current
    val maxFileSize = remember(state.groups) {
        state.groups.flatMap { it.files }.maxOfOrNull { it.sizeBytes } ?: 1L
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
                CategoryGroup(
                    group = group,
                    selectedUris = state.selectedUris,
                    onToggleExpanded = { viewModel.toggleGroupExpanded(group.category) },
                    onToggleGroupSelection = { viewModel.toggleGroupSelection(group.category) }
                )
            }

            if (group.expanded) {
                itemsIndexed(
                    items = group.files,
                    key = { _, file -> file.uri },
                    contentType = { _, _ -> "cleanup_file" }
                ) { index, file ->
                    Column {
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                modifier = Modifier.padding(start = 56.dp)
                            )
                        }
                        FileListItem(
                            file = file,
                            isSelected = file.uri in state.selectedUris,
                            maxFileSize = maxFileSize,
                            onLoadThumbnail = { uri -> viewModel.loadThumbnail(uri) },
                            onToggle = { viewModel.toggleSelection(file.uri) }
                        )
                    }
                }
            }
        }

        // Bottom spacing for the action bar
        item(key = "spacer") {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
