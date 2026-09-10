package com.runcheck.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import com.runcheck.ui.components.info.InfoIcon
import com.runcheck.ui.theme.spacing

internal data class MetricPillItem(
    val label: String,
    val value: String,
    val valueColor: Color? = null,
    val infoKey: String? = null,
)

@Composable
fun MetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    onInfoClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onInfoClick != null) {
                InfoIcon(onClick = onInfoClick)
            }
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxs))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
        )
    }
}

@Composable
internal fun MetricPillItems(
    items: List<MetricPillItem>,
    modifier: Modifier = Modifier,
    onInfoClick: (String) -> Unit = {},
) {
    items.forEach { item ->
        MetricPill(
            label = item.label,
            value = item.value,
            valueColor = item.valueColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = modifier,
            onInfoClick = item.infoKey?.let { key -> { onInfoClick(key) } },
        )
    }
}
