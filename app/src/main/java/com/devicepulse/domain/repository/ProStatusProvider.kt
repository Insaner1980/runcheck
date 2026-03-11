package com.devicepulse.domain.repository

import kotlinx.coroutines.flow.Flow

interface ProStatusProvider {
    val isProUser: Flow<Boolean>
    fun isPro(): Boolean
}
