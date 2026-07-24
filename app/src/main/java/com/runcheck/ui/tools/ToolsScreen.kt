package com.runcheck.ui.tools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.ui.components.ContentContainer
import com.runcheck.ui.components.ListRow
import com.runcheck.ui.components.PrimaryTopBar
import com.runcheck.ui.theme.spacing

@Composable
fun ToolsScreen(
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToStorageCleanup: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToLearn: () -> Unit,
    onNavigateToWeeklyReport: () -> Unit,
    onNavigateToExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTopBar(title = stringResource(R.string.navigation_tools))
        ContentContainer {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = MaterialTheme.spacing.base),
            ) {
                ListRow(
                    label = stringResource(R.string.speed_test_title),
                    icon = Icons.Outlined.Speed,
                    onClick = onNavigateToSpeedTest,
                    modifier = Modifier.fillMaxWidth(),
                )
                ListRow(
                    label = stringResource(R.string.storage_cleanup_tools),
                    icon = Icons.Outlined.CleaningServices,
                    onClick = onNavigateToStorageCleanup,
                    modifier = Modifier.fillMaxWidth(),
                )
                ListRow(
                    label = stringResource(R.string.charger_title),
                    icon = Icons.Outlined.BatteryChargingFull,
                    onClick = onNavigateToCharger,
                    modifier = Modifier.fillMaxWidth(),
                )
                ListRow(
                    label = stringResource(R.string.app_usage_title),
                    icon = Icons.Outlined.Apps,
                    onClick = onNavigateToAppUsage,
                    modifier = Modifier.fillMaxWidth(),
                )
                ListRow(
                    label = stringResource(R.string.learn_screen_title),
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    onClick = onNavigateToLearn,
                    modifier = Modifier.fillMaxWidth(),
                )
                ListRow(
                    label = stringResource(R.string.weekly_report_title),
                    icon = Icons.Outlined.Assessment,
                    onClick = onNavigateToWeeklyReport,
                    modifier = Modifier.fillMaxWidth(),
                )
                ListRow(
                    label = stringResource(R.string.export_title),
                    icon = Icons.Outlined.FileDownload,
                    onClick = onNavigateToExport,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
            }
        }
    }
}
