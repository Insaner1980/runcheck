package com.runcheck.ui.storage.cleanup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.state.ToggleableState
import com.runcheck.R
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.components.StatusDot
import com.runcheck.ui.theme.LARGE_CONTENT_FONT_SCALE
import com.runcheck.ui.theme.categoryColor
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.uiTokens

@Composable
fun CategoryGroup(
    group: FileGroup,
    onToggleExpansion: () -> Unit,
    onToggleGroupSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val color = categoryColor(group.category)
    val label = categoryLabel(context, group.category)
    val selectionState =
        when {
            group.selectedCount == 0 -> ToggleableState.Off
            group.selectedCount == group.itemCount -> ToggleableState.On
            else -> ToggleableState.Indeterminate
        }
    val useStackedHeader = LocalDensity.current.fontScale >= LARGE_CONTENT_FONT_SCALE
    val expandedLabel =
        if (group.expanded) {
            stringResource(R.string.a11y_collapse)
        } else {
            stringResource(R.string.a11y_expand)
        }
    val expansionStateLabel =
        if (group.expanded) {
            stringResource(R.string.a11y_expanded)
        } else {
            stringResource(R.string.a11y_collapsed)
        }
    val checkboxLabel = stringResource(R.string.a11y_select_all, label)

    Column(modifier = modifier.fillMaxWidth()) {
        // Group header
        val headerModifier =
            Modifier
                .defaultMinSize(minHeight = MaterialTheme.uiTokens.touchTarget)
                .clickable(onClick = onToggleExpansion, role = Role.Button)
                .semantics(mergeDescendants = true) {
                    heading()
                    stateDescription = expansionStateLabel
                    if (group.expanded) {
                        collapse {
                            onToggleExpansion()
                            true
                        }
                    } else {
                        expand {
                            onToggleExpansion()
                            true
                        }
                    }
                }.padding(vertical = MaterialTheme.spacing.sm)

        if (useStackedHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = headerModifier.weight(1f)) {
                    CategoryGroupTitle(
                        label = label,
                        itemCount = group.itemCount,
                        expanded = group.expanded,
                        color = color,
                        stackText = true,
                    )
                    CategoryGroupSize(group.totalBytes)
                }
                CategoryGroupSelection(
                    state = selectionState,
                    label = checkboxLabel,
                    onClick = onToggleGroupSelection,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = headerModifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryGroupTitle(
                        label = label,
                        itemCount = group.itemCount,
                        expanded = group.expanded,
                        color = color,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    CategoryGroupSize(group.totalBytes)
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                CategoryGroupSelection(
                    state = selectionState,
                    label = checkboxLabel,
                    onClick = onToggleGroupSelection,
                )
            }
        }
    }
}

@Composable
private fun CategoryGroupTitle(
    label: String,
    itemCount: Int,
    expanded: Boolean,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    stackText: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(color = color)
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
        Icon(
            imageVector =
                if (expanded) {
                    Icons.Outlined.ExpandMore
                } else {
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight
                },
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.uiTokens.iconLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
        if (stackText) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.value_count_parenthetical, itemCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.xxs))
            Text(
                text = stringResource(R.string.value_count_parenthetical, itemCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryGroupSize(
    totalBytes: Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Text(
        text = formatStorageSize(context, totalBytes),
        style =
            MaterialTheme.typography.bodySmall.copy(
                fontFamily = MaterialTheme.numericFontFamily,
            ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun CategoryGroupSelection(
    state: ToggleableState,
    label: String,
    onClick: () -> Unit,
) {
    TriStateCheckbox(
        state = state,
        onClick = onClick,
        modifier =
            Modifier.semantics {
                contentDescription = label
            },
        colors =
            CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
            ),
    )
}
