package com.runcheck.data.appusage

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.runcheck.data.db.dao.AppBatteryUsageDao
import com.runcheck.data.db.dao.AppBatteryUsageSummaryRow
import com.runcheck.data.db.entity.AppBatteryUsageEntity
import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.domain.model.AppUsageListSummary
import com.runcheck.domain.repository.AppBatteryUsageRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppBatteryUsageRepositoryImpl @Inject constructor(
    private val appBatteryUsageDao: AppBatteryUsageDao,
    private val appUsageDataSource: AppUsageDataSource,
    private val userPreferencesRepository: UserPreferencesRepository
) : AppBatteryUsageRepository {

    override fun getAggregatedUsageSince(since: Long): Flow<PagingData<AppBatteryUsage>> =
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PAGE_SIZE / 2,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { appBatteryUsageDao.getAggregatedUsageSince(since) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }

    override fun getUsageSummarySince(since: Long): Flow<AppUsageListSummary> =
        appBatteryUsageDao.getUsageSummarySince(since).map { it.toDomain() }

    override suspend fun collectUsageSnapshot() {
        if (!appUsageDataSource.hasUsageStatsPermission()) {
            return
        }

        val endTime = System.currentTimeMillis()
        val startTime = userPreferencesRepository.getAppUsageLastCollectedAt()
            ?.coerceAtMost(endTime)
            ?: (endTime - DEFAULT_COLLECTION_WINDOW_MS)

        val usage = appUsageDataSource.getUsageSince(startTime, endTime)
        if (usage.isNotEmpty()) {
            appBatteryUsageDao.insertAll(
                usage.map { snapshot ->
                    AppBatteryUsageEntity(
                        timestamp = endTime,
                        packageName = snapshot.packageName,
                        appLabel = snapshot.appLabel,
                        foregroundTimeMs = snapshot.foregroundTimeMs,
                        estimatedDrainMah = null
                    )
                }
            )
        }
        userPreferencesRepository.setAppUsageLastCollectedAt(endTime)
    }

    override suspend fun deleteOlderThan(cutoff: Long) {
        appBatteryUsageDao.deleteOlderThan(cutoff)
    }

    override suspend fun deleteAll() {
        appBatteryUsageDao.deleteAll()
    }

    companion object {
        private const val DEFAULT_COLLECTION_WINDOW_MS = 24L * 60L * 60L * 1000L
        private const val PAGE_SIZE = 20
    }
}

private fun AppBatteryUsageEntity.toDomain() = AppBatteryUsage(
    id = id,
    timestamp = timestamp,
    packageName = packageName,
    appLabel = appLabel,
    foregroundTimeMs = foregroundTimeMs,
    estimatedDrainMah = estimatedDrainMah
)

private fun AppBatteryUsageSummaryRow.toDomain() = AppUsageListSummary(
    totalForegroundTimeMs = total_foreground_time_ms,
    maxForegroundTimeMs = max_foreground_time_ms
)
