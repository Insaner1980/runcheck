package com.runcheck.data.crash

import com.runcheck.BuildConfig
import com.runcheck.domain.repository.CrashReportingController
import com.runcheck.domain.repository.UserPreferencesRepository
import com.runcheck.util.ReleaseSafeLog
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
        }.onFailure { error ->
            ReleaseSafeLog.error(TAG, "Failed to update Crashlytics collection state", error)
        }
    }

    companion object {
        private const val TAG = "CrashReportingManager"
    }
}
