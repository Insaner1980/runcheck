package com.runcheck.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.ui.components.ActionCard
import com.runcheck.ui.components.ContentContainer
import com.runcheck.ui.components.GridCard
import com.runcheck.ui.components.GridCardSubtitleStyle
import com.runcheck.ui.components.LearnTopicLink
import com.runcheck.ui.components.ListRow
import com.runcheck.ui.components.PrimaryTopBar
import com.runcheck.ui.components.ProBadgePill
import com.runcheck.ui.components.RuncheckActionCard
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.theme.domainColors
import com.runcheck.ui.theme.spacing

@Composable
fun ToolsScreen(
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToStorageCleanup: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToLearn: () -> Unit,
    onNavigateToExport: () -> Unit,
    modifier: Modifier = Modifier,
    hasProAccess: Boolean = false,
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
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                RuncheckActionCard(
                    icon = Icons.Outlined.Speed,
                    title = stringResource(R.string.speed_test_title),
                    subtitle = stringResource(R.string.tools_speed_test_subtitle),
                    actionLabel = stringResource(R.string.tools_speed_test_action),
                    onAction = onNavigateToSpeedTest,
                )

                ActionCard(
                    icon = Icons.Outlined.CleaningServices,
                    iconTint = MaterialTheme.domainColors.storage,
                    title = stringResource(R.string.storage_cleanup_tools),
                    subtitle = stringResource(R.string.tools_cleanup_subtitle),
                    actionLabel = stringResource(R.string.tools_cleanup_action),
                    onAction = onNavigateToStorageCleanup,
                    locked = !hasProAccess,
                )

                SectionHeader(text = stringResource(R.string.tools_device_actions))
                ToolsBentoGrid(
                    hasProAccess = hasProAccess,
                    onNavigateToCharger = onNavigateToCharger,
                    onNavigateToAppUsage = onNavigateToAppUsage,
                )

                SectionHeader(text = stringResource(R.string.tools_utilities))
                LearnTopicLink(
                    label = stringResource(R.string.learn_screen_title),
                    onClick = onNavigateToLearn,
                )
                ListRow(
                    label = stringResource(R.string.export_title),
                    icon = Icons.Outlined.FileDownload,
                    onClick = onNavigateToExport,
                    trailing = if (hasProAccess) null else ({ ProBadgePill() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
            }
        }
    }
}

@Composable
private fun ToolsBentoGrid(
    hasProAccess: Boolean,
    onNavigateToCharger: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        ToolGridCard(
            icon = Icons.Outlined.BatteryChargingFull,
            title = stringResource(R.string.charger_title),
            subtitle = stringResource(R.string.tools_charger_subtitle),
            locked = !hasProAccess,
            onClick = onNavigateToCharger,
        )
        ToolGridCard(
            icon = Icons.Outlined.Apps,
            title = stringResource(R.string.app_usage_title),
            subtitle = stringResource(R.string.tools_app_usage_subtitle),
            locked = !hasProAccess,
            onClick = onNavigateToAppUsage,
        )
    }
}

@Composable
private fun RowScope.ToolGridCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    locked: Boolean,
    onClick: () -> Unit,
) {
    GridCard(
        icon = icon,
        title = title,
        subtitle = subtitle,
        statusLabel = if (locked) stringResource(R.string.pro_feature_badge) else null,
        locked = locked,
        subtitleStyle = GridCardSubtitleStyle.BODY,
        onClick = onClick,
        modifier = Modifier.weight(1f),
    )
}
