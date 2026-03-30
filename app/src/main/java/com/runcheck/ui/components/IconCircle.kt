package com.runcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.runcheck.ui.theme.iconCircleColor
import com.runcheck.ui.theme.uiTokens

@Composable
fun IconCircle(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    backgroundColor: Color = Color.Unspecified,
    size: Dp = Dp.Unspecified,
    iconSize: Dp = Dp.Unspecified,
) {
    val tokens = MaterialTheme.uiTokens
    val resolvedBackgroundColor =
        backgroundColor.takeIf { it != Color.Unspecified } ?: MaterialTheme.iconCircleColor
    val resolvedSize = if (size == Dp.Unspecified) tokens.iconCircle else size
    val resolvedIconSize = if (iconSize == Dp.Unspecified) tokens.iconCircleInner else iconSize
    Box(
        modifier =
            modifier
                .size(resolvedSize)
                .clearAndSetSemantics {}
                .background(color = resolvedBackgroundColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(resolvedIconSize),
            tint = tint,
        )
    }
}
