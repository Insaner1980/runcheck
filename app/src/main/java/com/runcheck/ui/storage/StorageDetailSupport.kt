package com.runcheck.ui.storage

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.runcheck.R
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.StorageState
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.components.ActionCard
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.ListRow
import com.runcheck.ui.components.MetricRow
import com.runcheck.ui.theme.categoryColor
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors

@Composable
internal fun StorageCleanupToolsSection(
    storage: StorageState,
    onNavigateToCleanup: (com.runcheck.ui.storage.cleanup.CleanupType) -> Unit = {},
    onEmptyTrash: () -> Unit = {},
) {
    val context = LocalContext.current

    com.runcheck.ui.components.SectionHeader(
        text =
            androidx.compose.ui.res
                .stringResource(R.string.storage_cleanup_tools),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        ActionCard(
            icon = Icons.Outlined.FolderOpen,
            iconTint = MaterialTheme.statusColors.poor,
            title =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_large_files),
            subtitle =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_large_files_desc),
            actionLabel =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_scan),
            onAction = {
                onNavigateToCleanup(com.runcheck.ui.storage.cleanup.CleanupType.LARGE_FILES)
            },
        )

        ActionCard(
            icon = Icons.Outlined.Download,
            iconTint = MaterialTheme.colorScheme.primary,
            title =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_old_downloads),
            subtitle =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_old_downloads_desc),
            actionLabel =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_scan),
            onAction = {
                onNavigateToCleanup(com.runcheck.ui.storage.cleanup.CleanupType.OLD_DOWNLOADS)
            },
        )

        ActionCard(
            icon = Icons.Outlined.PhoneAndroid,
            iconTint = categoryColor(MediaCategory.APK),
            title =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_apk_files),
            subtitle =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_apk_files_desc),
            actionLabel =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_scan),
            onAction = {
                onNavigateToCleanup(com.runcheck.ui.storage.cleanup.CleanupType.APK_FILES)
            },
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            storage.trashInfo?.let { trash ->
                ActionCard(
                    icon = Icons.Outlined.Delete,
                    iconTint = MaterialTheme.colorScheme.error,
                    title =
                        androidx.compose.ui.res
                            .stringResource(R.string.storage_trash),
                    subtitle =
                        androidx.compose.ui.res.pluralStringResource(
                            R.plurals.storage_trash_summary,
                            trash.itemCount,
                            formatStorageSize(context, trash.totalBytes),
                            trash.itemCount,
                        ),
                    actionLabel =
                        androidx.compose.ui.res
                            .stringResource(R.string.storage_empty_trash),
                    onAction = onEmptyTrash,
                )
            }
        }
    }
}

@Composable
internal fun StorageDetailsCard(
    storage: StorageState,
    onInfoClick: (String) -> Unit = {},
) {
    val context = LocalContext.current

    StoragePanel {
        CardSectionTitle(
            text =
                androidx.compose.ui.res
                    .stringResource(R.string.battery_section_details),
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        MetricRow(
            label =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_total),
            value = formatStorageSize(context, storage.totalBytes),
        )
        MetricRow(
            label =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_used),
            value =
                androidx.compose.ui.res.stringResource(
                    R.string.storage_used_with_percent,
                    formatStorageSize(context, storage.usedBytes),
                    storage.usagePercent.toInt(),
                ),
            onInfoClick = { onInfoClick("usagePercent") },
        )
        MetricRow(
            label =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_available),
            value = formatStorageSize(context, storage.availableBytes),
        )
        storage.appsBytes?.let { bytes ->
            MetricRow(
                label =
                    androidx.compose.ui.res
                        .stringResource(R.string.storage_apps),
                value = formatStorageSize(context, bytes),
                onInfoClick = { onInfoClick("appsTotal") },
            )
        }
        storage.totalCacheBytes?.let { cache ->
            val cacheText =
                if (storage.appCount != null) {
                    androidx.compose.ui.res.pluralStringResource(
                        R.plurals.storage_cache_summary,
                        storage.appCount,
                        formatStorageSize(context, cache),
                        storage.appCount,
                    )
                } else {
                    formatStorageSize(context, cache)
                }
            MetricRow(
                label =
                    androidx.compose.ui.res
                        .stringResource(R.string.storage_cache_total),
                value = cacheText,
                onInfoClick = { onInfoClick("cache") },
            )
        }

        val hasTechDetails =
            storage.fileSystemType != null ||
                storage.encryptionStatus != null ||
                storage.storageVolumes > 0
        if (hasTechDetails) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            )
            storage.fileSystemType?.let { fs ->
                MetricRow(
                    label =
                        androidx.compose.ui.res
                            .stringResource(R.string.storage_filesystem),
                    value = fs.uppercase(),
                    onInfoClick = { onInfoClick("filesystem") },
                )
            }
            storage.encryptionStatus?.let { enc ->
                MetricRow(
                    label =
                        androidx.compose.ui.res
                            .stringResource(R.string.storage_encryption),
                    value = enc,
                    onInfoClick = { onInfoClick("encryption") },
                )
            }
            if (storage.storageVolumes > 0) {
                MetricRow(
                    label =
                        androidx.compose.ui.res
                            .stringResource(R.string.storage_volumes),
                    value = storage.storageVolumes.toString(),
                    showDivider = false,
                )
            }
        }
    }
}

@Composable
internal fun StorageSdCardCard(storage: StorageState) {
    val context = LocalContext.current

    StoragePanel {
        CardSectionTitle(
            text =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_sd_card),
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        storage.sdCardTotalBytes?.let { total ->
            MetricRow(
                label =
                    androidx.compose.ui.res
                        .stringResource(R.string.storage_total),
                value = formatStorageSize(context, total),
            )
        }
        storage.sdCardAvailableBytes?.let { available ->
            MetricRow(
                label =
                    androidx.compose.ui.res
                        .stringResource(R.string.storage_available),
                value = formatStorageSize(context, available),
                showDivider = false,
            )
        }
    }
}

@Composable
internal fun StorageQuickActionsCard() {
    val context = LocalContext.current

    StoragePanel {
        CardSectionTitle(
            text =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_quick_actions),
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        ListRow(
            label =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_open_settings),
            icon = Icons.Outlined.Storage,
            onClick = {
                context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
            },
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        )
        ListRow(
            label =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_free_up_space),
            icon = Icons.Outlined.FolderOpen,
            onClick = {
                context.startActivity(Intent("android.os.storage.action.MANAGE_STORAGE"))
            },
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        )
        ListRow(
            label =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_usage_access),
            icon = Icons.Outlined.Settings,
            onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            },
        )
    }
}

@Composable
internal fun StoragePanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            content = content,
        )
    }
}
