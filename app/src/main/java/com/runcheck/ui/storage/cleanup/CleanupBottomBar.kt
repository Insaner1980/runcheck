package com.runcheck.ui.storage.cleanup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.components.MiniBar
import com.runcheck.ui.theme.AccentTeal
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.reducedMotion
import com.runcheck.ui.theme.spacing

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
                        .padding(MaterialTheme.spacing.base)
                ) {
                    // Before → after projection
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${currentUsagePercent.toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = MaterialTheme.numericFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " → ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${projectedUsagePercent.toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = MaterialTheme.numericFontFamily
                            ),
                            color = AccentTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    MiniBar(
                        progress = (projectedUsagePercent / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        progressColor = AccentTeal
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                    Button(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(
                            stringResource(
                                R.string.cleanup_free_action,
                                formatStorageSize(context, selectedSize),
                                selectedCount
                            )
                        )
                    }
                }
            }
        }
    }
}
