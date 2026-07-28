package com.runcheck.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.runcheck.ui.theme.uiTokens

@Composable
internal fun StaticProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val tokens = MaterialTheme.uiTokens

    Canvas(modifier = modifier.size(tokens.touchTarget)) {
        drawCircle(
            color = color,
            style = Stroke(width = tokens.progressIndicatorStrokeWidth.toPx()),
        )
    }
}
