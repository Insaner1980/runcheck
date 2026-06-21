package com.runcheck.domain.usecase

import com.runcheck.domain.repository.AppBatteryUsageRepository
import com.runcheck.domain.repository.ProStatusProvider
import javax.inject.Inject

class RefreshAppUsageSnapshotUseCase
    @Inject
    constructor(
        private val appBatteryUsageRepository: AppBatteryUsageRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        suspend operator fun invoke() {
            if (!proStatusProvider.isPro()) return
            appBatteryUsageRepository.collectUsageSnapshot()
        }
    }
