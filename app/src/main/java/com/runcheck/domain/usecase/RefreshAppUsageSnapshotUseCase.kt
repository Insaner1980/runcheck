package com.runcheck.domain.usecase

import com.runcheck.domain.repository.AppBatteryUsageRepository
import javax.inject.Inject

class RefreshAppUsageSnapshotUseCase
    @Inject
    constructor(
        private val appBatteryUsageRepository: AppBatteryUsageRepository,
    ) {
        suspend operator fun invoke() {
            appBatteryUsageRepository.collectUsageSnapshot()
        }
    }
