package com.runcheck.domain.repository

import kotlinx.coroutines.flow.Flow

interface ProStatusProvider {
    val isProUser: Flow<Boolean>

    fun isPro(): Boolean
}
