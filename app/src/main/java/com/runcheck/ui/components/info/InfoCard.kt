package com.runcheck.ui.components.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.ui.theme.reducedMotion

@Composable
fun InfoCard(
    id: String,
    headline: String,
    body: String,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLearnMore: (() -> Unit)? = null
) {
    val accentColor = MaterialTheme.colorScheme.primary
    var visible by remember { mutableStateOf(true) }
    val skipAnimation = MaterialTheme.reducedMotion
    val animDuration = if (skipAnimation) 0 else 300

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(animationSpec = tween(animDuration)) +
            shrinkVertically(animationSpec = tween(animDuration)),
        modifier = modifier
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val strokeWidth = 3.dp.toPx()
                    drawLine(
                        color = accentColor,
                        start = Offset(strokeWidth / 2, 16.dp.toPx()),
                        end = Offset(strokeWidth / 2, size.height - 16.dp.toPx()),
                        strokeWidth = strokeWidth
                    )
                }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accentColor
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (onLearnMore != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.info_card_read_article),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = onLearnMore)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        visible = false
                        onDismiss(id)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.a11y_dismiss_card),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
