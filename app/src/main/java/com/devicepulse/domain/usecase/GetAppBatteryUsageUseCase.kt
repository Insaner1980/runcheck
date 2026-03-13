package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.AppBatteryUsage
import com.devicepulse.domain.repository.AppBatteryUsageRepository
import com.devicepulse.domain.repository.ProStatusProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetAppBatteryUsageUseCase @Inject constructor(
    private val appBatteryUsageRepository: AppBatteryUsageRepository,
    private val proStatusProvider: ProStatusProvider
) {
    operator fun invoke(since: Long): Flow<List<AppBatteryUsage>> =
        if (proStatusProvider.isPro()) {
            appBatteryUsageRepository.getAggregatedUsageSince(since)
        } else {
            flowOf(emptyList())
        }
}
