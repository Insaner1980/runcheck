package com.runcheck.data.appusage

import com.runcheck.data.db.dao.AppBatteryUsageDao
import com.runcheck.data.db.entity.AppBatteryUsageEntity
import com.runcheck.domain.model.AppBatteryUsage
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

    override fun getAggregatedUsageSince(since: Long): Flow<List<AppBatteryUsage>> =
        appBatteryUsageDao.getAggregatedUsageSince(since).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun collectUsageSnapshot() {
        if (!appUsageDataSource.hasUsageStatsPermission()) {
            userPreferencesRepository.setAppUsageLastCollectedAt(System.currentTimeMillis())
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

    companion object {
        private const val DEFAULT_COLLECTION_WINDOW_MS = 30L * 60L * 1000L
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
