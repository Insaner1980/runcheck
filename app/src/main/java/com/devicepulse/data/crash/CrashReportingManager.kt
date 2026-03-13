package com.devicepulse.data.crash

import com.devicepulse.BuildConfig
import com.devicepulse.domain.repository.CrashReportingController
import com.devicepulse.domain.repository.UserPreferencesRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashReportingManager @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : CrashReportingController {

    override suspend fun initialize() {
        val enabled = if (BuildConfig.DEBUG) {
            false
        } else {
            userPreferencesRepository.getPreferences().first().crashReportingEnabled
        }
        applyCollectionState(enabled)
    }

    override suspend fun setCollectionEnabled(enabled: Boolean) {
        userPreferencesRepository.setCrashReportingEnabled(enabled)
        applyCollectionState(enabled)
    }

    private fun applyCollectionState(enabled: Boolean) {
        val effectiveEnabled = enabled && !BuildConfig.DEBUG
        runCatching {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(effectiveEnabled)
            if (effectiveEnabled) {
                FirebaseCrashlytics.getInstance().sendUnsentReports()
            } else {
                FirebaseCrashlytics.getInstance().deleteUnsentReports()
            }
        }
    }
}
