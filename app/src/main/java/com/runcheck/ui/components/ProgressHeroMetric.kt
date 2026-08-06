package com.runcheck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runcheck.ui.theme.numericHeroDisplayTextStyle
import com.runcheck.ui.theme.numericHeroDisplayUnitTextStyle
import com.runcheck.ui.theme.spacing

@Composable
fun ProgressHeroMetric(
    progress: Float,
    value: String,
    unit: String,
    progressColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    supportingContent: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
    ) {
        ProgressRing(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier.size(100.dp),
            strokeWidth = 6.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            progressColor = progressColor,
            contentDescription = contentDescription,
        ) {}

        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                androidx.compose.material3.Text(
                    text = value,
                    style = MaterialTheme.numericHeroDisplayTextStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                androidx.compose.material3.Text(
                    text = unit,
                    style = MaterialTheme.numericHeroDisplayUnitTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 12.dp),
                )
            }
            supportingContent()
        }
    }
}
