package com.runcheck.domain.repository

import com.runcheck.domain.model.AppBatteryUsage
import kotlinx.coroutines.flow.Flow

interface AppBatteryUsageRepository {
    fun getAggregatedUsageSince(since: Long): Flow<List<AppBatteryUsage>>
    suspend fun collectUsageSnapshot()
    suspend fun deleteOlderThan(cutoff: Long)
}
