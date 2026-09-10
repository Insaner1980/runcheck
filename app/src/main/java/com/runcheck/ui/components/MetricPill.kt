package com.runcheck.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import com.runcheck.domain.model.Confidence
import com.runcheck.ui.components.info.InfoIcon
import com.runcheck.ui.theme.spacing

internal data class MetricPillItem(
    val label: String,
    val value: String,
    val valueColor: Color? = null,
    val infoKey: String? = null,
    val confidence: Confidence? = null,
)

@Composable
fun MetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    confidence: Confidence? = null,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor,
            )
            confidence?.let {
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                ConfidenceBadge(confidence = it)
            }
        }
    }
}

@Composable
internal fun MetricPillItems(
    items: List<MetricPillItem>,
    onInfoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    items.forEach { item ->
        MetricPill(
            label = item.label,
            value = item.value,
            valueColor = item.valueColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = modifier,
            onInfoClick = item.infoKey?.let { key -> { onInfoClick(key) } },
            confidence = item.confidence,
        )
    }
}
