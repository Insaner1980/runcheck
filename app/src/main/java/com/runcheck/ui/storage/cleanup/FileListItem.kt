package com.runcheck.ui.storage.cleanup

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runcheck.data.storage.ThumbnailLoader
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.components.IconCircle
import com.runcheck.ui.components.MiniBar
import com.runcheck.ui.theme.AccentBlue
import com.runcheck.ui.theme.AccentLime
import com.runcheck.ui.theme.AccentOrange
import com.runcheck.ui.theme.AccentTeal
import com.runcheck.ui.theme.AccentYellow
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.spacing

@Composable
fun FileListItem(
    file: ScannedFile,
    isSelected: Boolean,
    maxFileSize: Long,
    thumbnailLoader: ThumbnailLoader,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categoryColor = categoryColor(file.category)
    val categoryIcon = categoryIcon(file.category)

    var thumbnail by remember(file.uri) { mutableStateOf<ImageBitmap?>(null) }
    val showThumbnail = file.category == MediaCategory.IMAGE || file.category == MediaCategory.VIDEO

    if (showThumbnail) {
        LaunchedEffect(file.uri) {
            thumbnail = thumbnailLoader.loadThumbnail(file.uri)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = MaterialTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(4.dp))

        // Thumbnail or icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!,
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
                text = categoryLabel(file.category) +
                    " · " + formatRelativeDate(file.dateModified),
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
                progressColor = categoryColor
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

fun categoryColor(category: MediaCategory): Color = when (category) {
    MediaCategory.VIDEO -> AccentBlue
    MediaCategory.IMAGE -> AccentTeal
    MediaCategory.AUDIO -> AccentOrange
    MediaCategory.DOCUMENT -> AccentLime
    MediaCategory.DOWNLOAD -> AccentYellow
    MediaCategory.APK -> AccentLime
    MediaCategory.OTHER -> AccentYellow
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

fun categoryLabel(category: MediaCategory): String = when (category) {
    MediaCategory.VIDEO -> "Video"
    MediaCategory.IMAGE -> "Image"
    MediaCategory.AUDIO -> "Audio"
    MediaCategory.DOCUMENT -> "Document"
    MediaCategory.DOWNLOAD -> "Download"
    MediaCategory.APK -> "APK"
    MediaCategory.OTHER -> "Other"
}

private fun formatRelativeDate(timestampMs: Long): String {
    val now = System.currentTimeMillis()
    val diffDays = ((now - timestampMs) / 86_400_000).toInt()
    return when {
        diffDays < 1 -> "Today"
        diffDays == 1 -> "Yesterday"
        diffDays < 30 -> "${diffDays}d ago"
        diffDays < 365 -> "${diffDays / 30}mo ago"
        else -> "${diffDays / 365}y ago"
    }
}
