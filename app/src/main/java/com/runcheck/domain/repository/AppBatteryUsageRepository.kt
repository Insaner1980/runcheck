package com.runcheck.domain.repository

import androidx.paging.PagingData
import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.domain.model.AppUsageListSummary
import kotlinx.coroutines.flow.Flow

interface AppBatteryUsageRepository {
    fun getAggregatedUsageSince(since: Long): Flow<PagingData<AppBatteryUsage>>
    fun getUsageSummarySince(since: Long): Flow<AppUsageListSummary>
    suspend fun collectUsageSnapshot()
    suspend fun deleteOlderThan(cutoff: Long)
    suspend fun deleteAll()
}
