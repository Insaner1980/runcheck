package com.devicepulse.domain.usecase

import com.devicepulse.data.db.dao.AppBatteryUsageDao
import com.devicepulse.data.db.entity.AppBatteryUsageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppBatteryUsageUseCase @Inject constructor(
    private val appBatteryUsageDao: AppBatteryUsageDao
) {
    operator fun invoke(since: Long): Flow<List<AppBatteryUsageEntity>> =
        appBatteryUsageDao.getAggregatedUsageSince(since)
}
