package com.devicepulse.domain.repository

import com.devicepulse.domain.model.AppBatteryUsage
import kotlinx.coroutines.flow.Flow

interface AppBatteryUsageRepository {
    fun getAggregatedUsageSince(since: Long): Flow<List<AppBatteryUsage>>
}
