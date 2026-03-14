package com.runcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color = color, shape = CircleShape)
    )
}
