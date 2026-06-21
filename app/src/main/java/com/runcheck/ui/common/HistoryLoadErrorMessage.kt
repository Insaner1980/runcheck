package com.runcheck.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HistoryLoadErrorMessage(
    error: UiText?,
    modifier: Modifier = Modifier,
) {
    error?.let {
        Text(
            text = it.resolve(),
            modifier = modifier,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
