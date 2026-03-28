package com.runcheck.ui.storage.cleanup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.reducedMotion
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors

@Composable
fun CleanupSuccessOverlay(
    visible: Boolean,
    freedBytes: Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val noMotion = MaterialTheme.reducedMotion
    val successMessage =
        stringResource(
            R.string.a11y_cleanup_success,
            formatStorageSize(context, freedBytes),
        )

    AnimatedVisibility(
        visible = visible,
        enter = if (noMotion) EnterTransition.None else fadeIn(initialAlpha = 0f),
        exit = if (noMotion) ExitTransition.None else fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f))
                    .semantics { liveRegion = LiveRegionMode.Assertive },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = successMessage,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.statusColors.healthy,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))
                Text(
                    text =
                        stringResource(
                            R.string.cleanup_freed,
                            formatStorageSize(context, freedBytes),
                        ),
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontFamily = MaterialTheme.numericFontFamily,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
