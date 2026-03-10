package com.devicepulse.data.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProStatusRepository @Inject constructor() {

    val isProUser: Flow<Boolean> = flowOf(false)

    fun isPro(): Boolean = false
}
