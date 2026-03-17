package com.runcheck.domain.repository

import com.runcheck.pro.ProState
import kotlinx.coroutines.flow.Flow

interface ProStatusProvider {
    val isProUser: Flow<Boolean>
    val proState: Flow<ProState>
    fun isPro(): Boolean
}
