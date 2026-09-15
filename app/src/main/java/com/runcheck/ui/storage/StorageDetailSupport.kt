package com.runcheck.ui.storage

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.domain.model.StorageState
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.components.ActionCard
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.ListRow
import com.runcheck.ui.components.MetricRow
import com.runcheck.ui.components.RuncheckCard
import com.runcheck.ui.storage.cleanup.CleanupType
import com.runcheck.ui.theme.dividerColor
import com.runcheck.ui.theme.spacing

@Composable
internal fun StorageCleanupToolsSection(
    storage: StorageState,
    onNavigateToCleanup: (CleanupType) -> Unit = {},
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
        CleanupType.entries.forEach { type ->
            val presentation = type.toolPresentation()
            ActionCard(
                icon = presentation.icon,
                iconTint = presentation.tint.color(),
                title = stringResource(presentation.titleRes),
                subtitle = stringResource(presentation.descriptionRes),
                actionLabel = stringResource(R.string.storage_scan),
                onAction = { onNavigateToCleanup(type) },
            )
        }

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
                color = MaterialTheme.dividerColor,
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
internal fun StorageRemovableStorageCard(storage: StorageState) {
    val context = LocalContext.current

    StoragePanel {
        CardSectionTitle(
            text =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_removable),
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        MetricRow(
            label =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_total),
            value =
                storage.removableStorageTotalBytes?.let { formatStorageSize(context, it) }
                    ?: androidx.compose.ui.res
                        .stringResource(R.string.fallback_unknown),
        )
        MetricRow(
            label =
                androidx.compose.ui.res
                    .stringResource(R.string.storage_available),
            value =
                storage.removableStorageAvailableBytes?.let { formatStorageSize(context, it) }
                    ?: androidx.compose.ui.res
                        .stringResource(R.string.fallback_unknown),
            showDivider = false,
        )
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
            color = MaterialTheme.dividerColor,
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
            color = MaterialTheme.dividerColor,
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
    RuncheckCard(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        content = content,
    )
}
