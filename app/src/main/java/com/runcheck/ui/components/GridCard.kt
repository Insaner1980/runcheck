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
import androidx.compose.ui.text.SpanStyle
import com.runcheck.R
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
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
        val resolvedIconBg = if (iconBackgroundColor == Color.Unspecified) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            iconBackgroundColor
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.md,
                        vertical = MaterialTheme.spacing.base
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(color = resolvedIconBg, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = iconTint
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                if (statusLabel != null) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = subtitleColor)) {
                                append(subtitle)
                            }
                            append(" \u00B7 ")
                            withStyle(
                                SpanStyle(
                                    color = if (statusColor == Color.Unspecified) {
                                        subtitleColor
                                    } else {
                                        statusColor
                                    }
                                )
                            ) {
                                append(statusLabel)
                            }
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = subtitleColor
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
