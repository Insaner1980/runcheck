package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.AppBatteryUsage
import com.devicepulse.domain.repository.AppBatteryUsageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppBatteryUsageUseCase @Inject constructor(
    private val appBatteryUsageRepository: AppBatteryUsageRepository
) {
    operator fun invoke(since: Long): Flow<List<AppBatteryUsage>> =
        appBatteryUsageRepository.getAggregatedUsageSince(since)
}
