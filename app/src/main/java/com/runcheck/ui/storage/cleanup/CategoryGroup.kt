package com.runcheck.ui.storage.cleanup

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.components.StatusDot
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.spacing

@Composable
fun CategoryGroup(
    group: FileGroup,
    selectedUris: Set<String>,
    onToggleExpanded: () -> Unit,
    onToggleGroupSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val color = categoryColor(group.category)
    val label = categoryLabel(context, group.category)
    val allSelected = group.files.all { it.uri in selectedUris }
    val expandedLabel = if (group.expanded) {
        stringResource(R.string.a11y_collapse)
    } else {
        stringResource(R.string.a11y_expand)
    }
    val checkboxLabel = stringResource(R.string.a11y_select_all, label)

    Column(modifier = modifier.fillMaxWidth()) {
        // Group header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded, role = Role.Button)
                .semantics(mergeDescendants = true) {
                    heading()
                    stateDescription = expandedLabel
                    if (group.expanded) {
                        collapse { onToggleExpanded(); true }
                    } else {
                        expand { onToggleExpanded(); true }
                    }
                }
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
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = " " + stringResource(R.string.value_count_parenthetical, group.files.size),
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
                modifier = Modifier.semantics {
                    contentDescription = checkboxLabel
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
