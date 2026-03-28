package com.runcheck.domain.usecase

import androidx.paging.PagingData
import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.domain.repository.AppBatteryUsageRepository
import com.runcheck.domain.repository.ProStatusProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetAppBatteryUsageUseCase
    @Inject
    constructor(
        private val appBatteryUsageRepository: AppBatteryUsageRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        operator fun invoke(since: Long): Flow<PagingData<AppBatteryUsage>> =
            proStatusProvider.isProUser
                .distinctUntilChanged()
                .flatMapLatest { isPro ->
                    if (isPro) {
                        appBatteryUsageRepository.getAggregatedUsageSince(since)
                    } else {
                        flowOf(PagingData.empty())
                    }
                }
    }
