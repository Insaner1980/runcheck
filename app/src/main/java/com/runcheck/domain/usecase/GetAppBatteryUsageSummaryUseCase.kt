package com.runcheck.domain.usecase

import com.runcheck.domain.model.AppUsageListSummary
import com.runcheck.domain.repository.AppBatteryUsageRepository
import com.runcheck.domain.repository.ProStatusProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetAppBatteryUsageSummaryUseCase
    @Inject
    constructor(
        private val appBatteryUsageRepository: AppBatteryUsageRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        operator fun invoke(since: Long): Flow<AppUsageListSummary> =
            proStatusProvider.isProUser
                .distinctUntilChanged()
                .flatMapLatest { isPro ->
                    if (isPro) {
                        appBatteryUsageRepository.getUsageSummarySince(since)
                    } else {
                        flowOf(AppUsageListSummary(totalForegroundTimeMs = 0L, maxForegroundTimeMs = 0L))
                    }
                }
    }
