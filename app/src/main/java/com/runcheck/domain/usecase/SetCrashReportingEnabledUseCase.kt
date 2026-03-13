package com.devicepulse.domain.usecase

import com.devicepulse.domain.repository.CrashReportingController
import javax.inject.Inject

class SetCrashReportingEnabledUseCase @Inject constructor(
    private val crashReportingController: CrashReportingController
) {
    suspend operator fun invoke(enabled: Boolean) {
        crashReportingController.setCollectionEnabled(enabled)
    }
}
