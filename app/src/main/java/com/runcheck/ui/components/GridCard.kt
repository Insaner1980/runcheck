package com.runcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.runcheck.R
import androidx.compose.ui.unit.dp
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.spacing

@Composable
fun GridCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurface,
    statusLabel: String? = null,
    statusColor: Color = Color.Unspecified,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconBackgroundColor: Color = Color.Unspecified,
    locked: Boolean = false
) {
    val lockedStateDesc = if (locked) stringResource(R.string.a11y_locked_pro_feature) else null
    val hasStatus = statusColor != Color.Unspecified
    val resolvedIconBg = iconBackgroundColor.takeIf { it != Color.Unspecified }
        ?: MaterialTheme.colorScheme.surfaceContainerHighest
    val startPadding = if (hasStatus) MaterialTheme.spacing.base else MaterialTheme.spacing.md
    val statusStripModifier = if (hasStatus) Modifier.statusStrip(color = statusColor) else Modifier
    val resolvedStatusLabelColor = if (hasStatus) statusColor else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) {
            if (lockedStateDesc != null) {
                stateDescription = lockedStateDesc
            }
        },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(statusStripModifier)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = startPadding,
                        end = MaterialTheme.spacing.md,
                        top = MaterialTheme.spacing.base,
                        bottom = MaterialTheme.spacing.base
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color = resolvedIconBg, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = iconTint
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = MaterialTheme.numericFontFamily
                    ),
                    color = subtitleColor
                )
                if (statusLabel != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = resolvedStatusLabelColor
                    )
                }
            }

            if (locked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f))
                )
                ProBadgePill(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(MaterialTheme.spacing.md)
                )
            }
        }
    }
}
