package com.runcheck.pro

import kotlinx.coroutines.flow.StateFlow

interface ProStateProvider {
    val proState: StateFlow<ProState>
}
