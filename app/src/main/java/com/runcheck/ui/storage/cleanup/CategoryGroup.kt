package com.runcheck.ui.storage.cleanup

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.components.StatusDot
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.spacing

@Composable
fun CategoryGroup(
    group: FileGroup,
    selectedUris: Set<String>,
    maxFileSize: Long,
    onLoadThumbnail: suspend (String) -> Bitmap?,
    onToggleExpanded: () -> Unit,
    onToggleGroupSelection: () -> Unit,
    onToggleFileSelection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val color = categoryColor(group.category)
    val allSelected = group.files.all { it.uri in selectedUris }

    Column(modifier = modifier.fillMaxWidth()) {
        // Group header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(vertical = MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(color = color)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (group.expanded) Icons.Filled.ExpandMore
                    else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = categoryLabel(group.category),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = " (${group.files.size})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatStorageSize(context, group.totalBytes),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = MaterialTheme.numericFontFamily
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Checkbox(
                checked = allSelected,
                onCheckedChange = { onToggleGroupSelection() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        // Files list (collapsible)
        AnimatedVisibility(
            visible = group.expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                group.files.forEachIndexed { index, file ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            modifier = Modifier.padding(start = 56.dp)
                        )
                    }
                    FileListItem(
                        file = file,
                        isSelected = file.uri in selectedUris,
                        maxFileSize = maxFileSize,
                        onLoadThumbnail = onLoadThumbnail,
                        onToggle = { onToggleFileSelection(file.uri) }
                    )
                }
            }
        }
    }
}
