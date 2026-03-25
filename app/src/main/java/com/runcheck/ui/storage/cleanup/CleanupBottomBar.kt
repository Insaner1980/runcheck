package com.runcheck.ui.storage.cleanup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.components.MiniBar
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.reducedMotion
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors

@Composable
fun CleanupBottomBar(
    visible: Boolean,
    selectedSize: Long,
    selectedCount: Int,
    currentUsagePercent: Float,
    projectedUsagePercent: Float,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val noMotion = MaterialTheme.reducedMotion
    val projectionDesc = stringResource(
        R.string.a11y_storage_projection,
        currentUsagePercent.toInt(),
        projectedUsagePercent.toInt()
    )

    AnimatedVisibility(
        visible = visible,
        enter = if (noMotion) EnterTransition.None else slideInVertically { it },
        exit = if (noMotion) ExitTransition.None else slideOutVertically { it },
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp
        ) {
            Column {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(MaterialTheme.spacing.base)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                ) {
                    // Before → after projection
                    Text(
                        text = stringResource(
                            R.string.storage_projection_visual,
                            currentUsagePercent.toInt(),
                            projectedUsagePercent.toInt()
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = MaterialTheme.numericFontFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = projectionDesc
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    MiniBar(
                        progress = (projectedUsagePercent / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        fillColor = MaterialTheme.statusColors.healthy,
                        contentDescription = projectionDesc
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                    Button(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            pluralStringResource(R.plurals.cleanup_free_action, selectedCount, formatStorageSize(context, selectedSize), selectedCount)
                        )
                    }
                }
            }
        }
    }
}
