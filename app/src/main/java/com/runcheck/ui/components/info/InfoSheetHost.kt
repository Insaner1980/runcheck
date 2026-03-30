package com.runcheck.ui.components.info

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun rememberInfoSheetState(): MutableState<String?> = rememberSaveable { mutableStateOf(null) }

@Composable
fun InfoSheetHost(
    activeKey: String?,
    onDismiss: () -> Unit,
    resolveContent: (String) -> InfoSheetContent?,
) {
    val content = activeKey?.let(resolveContent)
    if (content != null) {
        InfoBottomSheet(content = content, onDismiss = onDismiss)
    }
}
