package com.runcheck.domain.repository

import kotlinx.coroutines.flow.Flow

interface ProStatusProvider {
    val isProUser: Flow<Boolean>
    val isProStatusReady: Boolean
        get() = true

    fun isPro(): Boolean
}
