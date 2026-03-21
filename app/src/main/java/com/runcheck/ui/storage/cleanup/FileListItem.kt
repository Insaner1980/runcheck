package com.runcheck.ui.storage.cleanup

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.components.IconCircle
import com.runcheck.ui.components.MiniBar
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors
import kotlin.math.roundToInt

@Composable
fun FileListItem(
    file: ScannedFile,
    isSelected: Boolean,
    maxFileSize: Long,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categoryColor = categoryColor(file.category)
    val categoryIcon = categoryIcon(file.category)
    val toggleLabel = stringResource(R.string.a11y_toggle_selection, file.displayName)
    val relativeSizePercent = if (maxFileSize > 0) {
        ((file.sizeBytes.toFloat() / maxFileSize) * 100f).roundToInt().coerceIn(0, 100)
    } else {
        0
    }

    var thumbnail by remember(file.uri) { mutableStateOf<ImageBitmap?>(null) }
    val showThumbnail = file.category == MediaCategory.IMAGE || file.category == MediaCategory.VIDEO

    if (showThumbnail) {
        LaunchedEffect(file.uri, context) {
            thumbnail = loadCleanupThumbnail(context, file.uri)?.asImageBitmap()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle, role = Role.Checkbox, onClickLabel = toggleLabel)
            .semantics(mergeDescendants = true) {
                toggleableState = if (isSelected) ToggleableState.On else ToggleableState.Off
            }
            .padding(vertical = MaterialTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(4.dp))

        // Thumbnail or icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            val loadedThumbnail = thumbnail
            if (loadedThumbnail != null) {
                Image(
                    bitmap = loadedThumbnail,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                IconCircle(
                    icon = categoryIcon,
                    tint = categoryColor,
                    size = 48.dp,
                    iconSize = 24.dp
                )
            }
        }
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = categoryLabel(context, file.category) +
                    " \u00B7 " + formatRelativeDate(context, file.dateModified),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            MiniBar(
                progress = if (maxFileSize > 0) {
                    (file.sizeBytes.toFloat() / maxFileSize).coerceIn(0f, 1f)
                } else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                fillColor = categoryColor,
                contentDescription = stringResource(
                    R.string.a11y_progress_percent,
                    file.displayName,
                    relativeSizePercent
                )
            )
        }
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))

        Text(
            text = formatStorageSize(context, file.sizeBytes),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = MaterialTheme.numericFontFamily
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun categoryColor(category: MediaCategory): Color {
    val colors = MaterialTheme.statusColors
    return when (category) {
        MediaCategory.VIDEO -> MaterialTheme.colorScheme.primary
        MediaCategory.IMAGE -> colors.healthy
        MediaCategory.AUDIO -> colors.poor
        MediaCategory.DOCUMENT -> colors.fair
        MediaCategory.DOWNLOAD -> MaterialTheme.colorScheme.primary
        MediaCategory.APK -> MaterialTheme.colorScheme.onSurfaceVariant
        MediaCategory.OTHER -> MaterialTheme.colorScheme.outline
    }
}

fun categoryIcon(category: MediaCategory): ImageVector = when (category) {
    MediaCategory.VIDEO -> Icons.Outlined.Videocam
    MediaCategory.IMAGE -> Icons.Outlined.Image
    MediaCategory.AUDIO -> Icons.Outlined.AudioFile
    MediaCategory.DOCUMENT -> Icons.Outlined.Description
    MediaCategory.DOWNLOAD -> Icons.Outlined.Download
    MediaCategory.APK -> Icons.Outlined.Android
    MediaCategory.OTHER -> Icons.Outlined.Description
}

fun categoryLabel(context: Context, category: MediaCategory): String = when (category) {
    MediaCategory.VIDEO -> context.getString(R.string.a11y_category_video)
    MediaCategory.IMAGE -> context.getString(R.string.a11y_category_image)
    MediaCategory.AUDIO -> context.getString(R.string.a11y_category_audio)
    MediaCategory.DOCUMENT -> context.getString(R.string.a11y_category_document)
    MediaCategory.DOWNLOAD -> context.getString(R.string.a11y_category_download)
    MediaCategory.APK -> context.getString(R.string.a11y_category_apk)
    MediaCategory.OTHER -> context.getString(R.string.a11y_category_other)
}

fun formatRelativeDate(context: Context, timestampMs: Long): String {
    val now = System.currentTimeMillis()
    val diffDays = ((now - timestampMs) / 86_400_000).toInt()
    return when {
        diffDays < 1 -> context.getString(R.string.a11y_date_today)
        diffDays == 1 -> context.getString(R.string.a11y_date_yesterday)
        diffDays < 30 -> context.resources.getQuantityString(R.plurals.a11y_date_days_ago, diffDays, diffDays)
        diffDays < 365 -> context.resources.getQuantityString(R.plurals.a11y_date_months_ago, diffDays / 30, diffDays / 30)
        else -> context.resources.getQuantityString(R.plurals.a11y_date_years_ago, diffDays / 365, diffDays / 365)
    }
}
