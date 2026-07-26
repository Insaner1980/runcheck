package com.runcheck.pro

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow

@Stable
interface ProStateProvider {
    val proState: StateFlow<ProState>
    val proAccessReady: StateFlow<Boolean>
}
