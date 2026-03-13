package com.devicepulse.ui.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.R
import com.devicepulse.ui.common.formatPercent
import com.devicepulse.ui.common.formatStorageSize
import com.devicepulse.ui.components.DetailTopBar
import com.devicepulse.ui.components.MetricTile
import com.devicepulse.ui.components.PullToRefreshWrapper
import com.devicepulse.ui.theme.spacing

@Composable
fun StorageDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StorageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(
            title = stringResource(R.string.storage_title),
            onBack = onBack
        )
        when (val state = uiState) {
            is StorageUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is StorageUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.common_error_generic))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }
            is StorageUiState.Success -> {
                StorageContent(state = state, onRefresh = { viewModel.refresh() })
            }
        }
    }
}

@Composable
private fun StorageContent(
    state: StorageUiState.Success,
    onRefresh: () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val storage = state.storageState
    val context = LocalContext.current

    LaunchedEffect(state) {
        isRefreshing = false
    }

    PullToRefreshWrapper(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefresh()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            // Usage progress bar
            LinearProgressIndicator(
                progress = { (storage.usagePercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MaterialTheme.spacing.sm),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )

            MetricTile(
                label = stringResource(R.string.storage_total),
                value = formatStorageSize(context, storage.totalBytes)
            )

            MetricTile(
                label = stringResource(R.string.storage_used),
                value = formatStorageSize(context, storage.usedBytes),
                unit = formatPercent(storage.usagePercent)
            )

            MetricTile(
                label = stringResource(R.string.storage_available),
                value = formatStorageSize(context, storage.availableBytes)
            )

            storage.appsBytes?.let { bytes ->
                MetricTile(
                    label = stringResource(R.string.storage_apps),
                    value = formatStorageSize(context, bytes)
                )
            }

            storage.mediaBytes?.let { bytes ->
                MetricTile(
                    label = stringResource(R.string.storage_media),
                    value = formatStorageSize(context, bytes)
                )
            }

            storage.fillRateEstimate?.let { estimate ->
                MetricTile(
                    label = stringResource(R.string.storage_fill_rate),
                    value = estimate
                )
            }

            if (storage.sdCardAvailable) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                Text(
                    text = stringResource(R.string.storage_sd_card),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                storage.sdCardTotalBytes?.let { total ->
                    MetricTile(
                        label = stringResource(R.string.storage_total),
                        value = formatStorageSize(context, total)
                    )
                }
                storage.sdCardAvailableBytes?.let { available ->
                    MetricTile(
                        label = stringResource(R.string.storage_available),
                        value = formatStorageSize(context, available)
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}
