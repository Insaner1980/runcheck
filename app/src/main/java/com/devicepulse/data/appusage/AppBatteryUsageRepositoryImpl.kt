package com.devicepulse.data.appusage

import com.devicepulse.data.db.dao.AppBatteryUsageDao
import com.devicepulse.data.db.entity.AppBatteryUsageEntity
import com.devicepulse.domain.model.AppBatteryUsage
import com.devicepulse.domain.repository.AppBatteryUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppBatteryUsageRepositoryImpl @Inject constructor(
    private val appBatteryUsageDao: AppBatteryUsageDao
) : AppBatteryUsageRepository {

    override fun getAggregatedUsageSince(since: Long): Flow<List<AppBatteryUsage>> =
        appBatteryUsageDao.getAggregatedUsageSince(since).map { entities ->
            entities.map { it.toDomain() }
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
