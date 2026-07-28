package com.runcheck.ui.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.ui.common.resolve
import com.runcheck.ui.components.EmptyStateIllustration
import com.runcheck.ui.components.RuncheckDetailScaffold
import com.runcheck.ui.components.RuncheckProgressSpinner
import com.runcheck.ui.settings.shareExportUris
import com.runcheck.ui.theme.spacing

@Composable
fun ExportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.shareUris) {
        state.shareUris?.let { uris ->
            shareExportUris(context, uris)
            viewModel.onShareHandled()
        }
    }

    RuncheckDetailScaffold(
        title = stringResource(R.string.export_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        ExportContent(
            state = state,
            onExport = viewModel::prepareExportShare,
        )
    }
}

@Composable
internal fun ExportContent(
    state: ExportUiState,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
        ) {
            if (state.isExporting) {
                RuncheckProgressSpinner(
                    contentDescription = stringResource(R.string.a11y_loading),
                )
            } else {
                EmptyStateIllustration(
                    title = stringResource(R.string.export_content_title),
                    message = stringResource(R.string.export_screen_message),
                    icon = Icons.Outlined.FileDownload,
                    actionLabel = stringResource(R.string.export_action),
                    onAction = onExport,
                )
            }
            state.status?.let { status ->
                Text(
                    text = status.resolve(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
