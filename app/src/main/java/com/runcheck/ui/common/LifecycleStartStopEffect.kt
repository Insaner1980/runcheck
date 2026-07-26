package com.runcheck.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleStartEffect

@Composable
fun LifecycleStartStopEffect(
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnStop by rememberUpdatedState(onStop)

    LifecycleStartEffect(Unit) {
        currentOnStart()
        onStopOrDispose {
            currentOnStop()
        }
    }
}
