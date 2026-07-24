package com.runcheck.ui.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.ui.components.ContentContainer
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.ProFeatureLockedState
import com.runcheck.ui.navigation.ExportAccessState
import com.runcheck.ui.navigation.exportAccessState
import com.runcheck.ui.theme.spacing

@Composable
fun WeeklyReportEntryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PendingToolEntryScreen(
        titleRes = R.string.weekly_report_title,
        messageRes = R.string.weekly_report_empty,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun ExportEntryScreen(
    proStatusReady: Boolean,
    hasProAccess: Boolean,
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(
            title = stringResource(R.string.export_title),
            onBack = onBack,
        )
        when (exportAccessState(proStatusReady, hasProAccess)) {
            ExportAccessState.WAITING_FOR_PRO_STATUS -> {
                CenteredToolContent {
                    CircularProgressIndicator()
                }
            }

            ExportAccessState.LOCKED -> {
                ProFeatureLockedState(
                    title = stringResource(R.string.export_title),
                    message =
                        stringResource(
                            R.string.pro_feature_locked_message,
                            stringResource(R.string.pro_feature_csv_export),
                        ),
                    actionLabel = stringResource(R.string.pro_feature_upgrade_action),
                    onAction = onUpgradeToPro,
                )
            }

            ExportAccessState.AVAILABLE -> {
                CenteredToolContent {
                    Text(
                        text = stringResource(R.string.export_settings_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingToolEntryScreen(
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(
            title = stringResource(titleRes),
            onBack = onBack,
        )
        CenteredToolContent {
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CenteredToolContent(content: @Composable () -> Unit) {
    ContentContainer(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}
