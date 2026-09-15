package com.runcheck.ui.storage

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.runcheck.R
import com.runcheck.domain.model.MediaCategory
import com.runcheck.ui.storage.cleanup.CleanupType
import com.runcheck.ui.theme.categoryColor
import com.runcheck.ui.theme.statusColors

internal data class CleanupToolPresentation(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val tint: CleanupToolTint,
)

internal enum class CleanupToolTint {
    POOR,
    PRIMARY,
    APK,
}

internal fun CleanupType.toolPresentation(): CleanupToolPresentation =
    when (this) {
        CleanupType.LARGE_FILES -> {
            CleanupToolPresentation(
                titleRes = R.string.storage_large_files,
                descriptionRes = R.string.storage_large_files_desc,
                icon = Icons.Outlined.FolderOpen,
                tint = CleanupToolTint.POOR,
            )
        }

        CleanupType.OLD_DOWNLOADS -> {
            CleanupToolPresentation(
                titleRes = R.string.storage_old_downloads,
                descriptionRes = R.string.storage_old_downloads_desc,
                icon = Icons.Outlined.Download,
                tint = CleanupToolTint.PRIMARY,
            )
        }

        CleanupType.APK_FILES -> {
            CleanupToolPresentation(
                titleRes = R.string.storage_apk_files,
                descriptionRes = R.string.storage_apk_files_desc,
                icon = Icons.Outlined.PhoneAndroid,
                tint = CleanupToolTint.APK,
            )
        }
    }

@Composable
internal fun CleanupToolTint.color(): Color =
    when (this) {
        CleanupToolTint.POOR -> MaterialTheme.statusColors.poor
        CleanupToolTint.PRIMARY -> MaterialTheme.colorScheme.primary
        CleanupToolTint.APK -> categoryColor(MediaCategory.APK)
    }
