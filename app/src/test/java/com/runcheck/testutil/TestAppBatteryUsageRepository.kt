package com.runcheck.testutil

import androidx.paging.PagingData
import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.domain.model.AppUsageListSummary
import com.runcheck.domain.repository.AppBatteryUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class TestAppBatteryUsageRepository(
    private val usages: List<AppBatteryUsage> = emptyList(),
) : AppBatteryUsageRepository {
    var collectCalls = 0

    override fun getAggregatedUsageSince(since: Long): Flow<PagingData<AppBatteryUsage>> = emptyFlow()

    override fun getUsageSummarySince(since: Long): Flow<AppUsageListSummary> = emptyFlow()

    override suspend fun getUsageSinceSync(since: Long): List<AppBatteryUsage> = usages.filter { it.timestamp >= since }

    override suspend fun collectUsageSnapshot() {
        collectCalls++
    }

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}
