package com.runcheck.domain.usecase

import com.runcheck.domain.repository.CrashReportingController
import javax.inject.Inject

class SetCrashReportingEnabledUseCase @Inject constructor(
    private val crashReportingController: CrashReportingController
) {
    suspend operator fun invoke(enabled: Boolean) {
        crashReportingController.setCollectionEnabled(enabled)
    }
}
