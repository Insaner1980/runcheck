package com.runcheck.ui.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.ui.components.ContentContainer
import com.runcheck.ui.components.DetailTopBar
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PendingToolEntryScreen(
        titleRes = R.string.export_title,
        messageRes = R.string.export_settings_hint,
        onBack = onBack,
        modifier = modifier,
    )
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
        ContentContainer(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(MaterialTheme.spacing.base),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
