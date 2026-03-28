package com.runcheck.domain.usecase

import androidx.paging.PagingData
import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.domain.repository.AppBatteryUsageRepository
import com.runcheck.domain.repository.ProStatusProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetAppBatteryUsageUseCase
    @Inject
    constructor(
        private val appBatteryUsageRepository: AppBatteryUsageRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        operator fun invoke(since: Long): Flow<PagingData<AppBatteryUsage>> =
            if (proStatusProvider.isPro()) {
                appBatteryUsageRepository.getAggregatedUsageSince(since)
            } else {
                flowOf(PagingData.empty())
            }
    }
