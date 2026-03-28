package com.runcheck.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Centers and constrains content to a maximum width for readable layouts
 * on tablets and foldables. On phones this has no visible effect since
 * screen width is already below the limit.
 */
private val ContentMaxWidth = 600.dp

@Composable
fun ContentContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(modifier = Modifier.widthIn(max = ContentMaxWidth)) {
            content()
        }
    }
}
