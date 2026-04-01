package com.runcheck.pro

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.runcheck.worker.TrialNotificationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class TrialState(
    val isActive: Boolean = false,
    val daysRemaining: Int = 0,
    val startTimestamp: Long = 0L,
    val isFirstLaunch: Boolean = false,
    val clockTampered: Boolean = false,
)

@Singleton
class TrialManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val workManager: WorkManager,
    ) {
        private val _trialState = MutableStateFlow(TrialState())
        val trialState: StateFlow<TrialState> = _trialState.asStateFlow()

        suspend fun initialize(): Boolean {
            val prefs = context.trialDataStore.data.first()
            val startTimestamp = prefs[KEY_TRIAL_START] ?: 0L
            val lastKnown = prefs[KEY_LAST_KNOWN_TIMESTAMP] ?: 0L
            val now = System.currentTimeMillis()

            // Check for clock tampering: current time is >1 hour before last known
            val clockTampered = lastKnown > 0L && now < lastKnown - TimeUnit.HOURS.toMillis(1)

            // Update last known timestamp
            context.trialDataStore.edit { it[KEY_LAST_KNOWN_TIMESTAMP] = now }

            val isFirstLaunch = startTimestamp == 0L
            val actualStart =
                if (isFirstLaunch) {
                    context.trialDataStore.edit { it[KEY_TRIAL_START] = now }
                    now
                } else {
                    startTimestamp
                }

            val daysElapsed = TimeUnit.MILLISECONDS.toDays(now - actualStart).toInt()
            val daysRemaining = (TRIAL_DURATION_DAYS - daysElapsed).coerceAtLeast(0)
            val isActive = daysRemaining > 0 && !clockTampered

            _trialState.value =
                TrialState(
                    isActive = isActive,
                    daysRemaining = daysRemaining,
                    startTimestamp = actualStart,
                    isFirstLaunch = isFirstLaunch,
                    clockTampered = clockTampered,
                )

            if (isFirstLaunch) {
                scheduleTrialNotifications(actualStart)
            }

            return isFirstLaunch
        }

        suspend fun updateTimestamp() {
            context.trialDataStore.edit {
                it[KEY_LAST_KNOWN_TIMESTAMP] = System.currentTimeMillis()
            }
        }

        suspend fun isWelcomeShown(): Boolean =
            context.trialDataStore.data
                .map { it[KEY_WELCOME_SHOWN] ?: false }
                .first()

        suspend fun setWelcomeShown() {
            context.trialDataStore.edit { it[KEY_WELCOME_SHOWN] = true }
        }

        suspend fun isDay5PromptShown(): Boolean =
            context.trialDataStore.data
                .map { it[KEY_DAY5_PROMPT_SHOWN] ?: false }
                .first()

        suspend fun setDay5PromptShown() {
            context.trialDataStore.edit { it[KEY_DAY5_PROMPT_SHOWN] = true }
        }

        suspend fun getUpgradeCardDismissCount(): Int =
            context.trialDataStore.data
                .map { it[KEY_UPGRADE_DISMISS_COUNT] ?: 0 }
                .first()

        suspend fun getUpgradeCardLastDismissTimestamp(): Long =
            context.trialDataStore.data
                .map { it[KEY_UPGRADE_DISMISS_TIMESTAMP] ?: 0L }
                .first()

        suspend fun incrementUpgradeCardDismiss() {
            context.trialDataStore.edit {
                it[KEY_UPGRADE_DISMISS_COUNT] = (it[KEY_UPGRADE_DISMISS_COUNT] ?: 0) + 1
                it[KEY_UPGRADE_DISMISS_TIMESTAMP] = System.currentTimeMillis()
            }
        }

        private fun scheduleTrialNotifications(trialStart: Long) {
            val now = System.currentTimeMillis()
            val day5Target = trialStart + TimeUnit.DAYS.toMillis(5)
            val day7Target = trialStart + TimeUnit.DAYS.toMillis(7)

            val day5Delay = (day5Target - now).coerceAtLeast(0)
            val day7Delay = (day7Target - now).coerceAtLeast(0)

            val day5Request =
                OneTimeWorkRequestBuilder<TrialNotificationWorker>()
                    .setInitialDelay(day5Delay, TimeUnit.MILLISECONDS)
                    .setInputData(
                        Data
                            .Builder()
                            .putString(
                                TrialNotificationWorker.KEY_NOTIFICATION_TYPE,
                                TrialNotificationWorker.TYPE_DAY5,
                            ).build(),
                    ).addTag(TrialNotificationWorker.WORK_TAG_DAY5)
                    .build()

            val day7Request =
                OneTimeWorkRequestBuilder<TrialNotificationWorker>()
                    .setInitialDelay(day7Delay, TimeUnit.MILLISECONDS)
                    .setInputData(
                        Data
                            .Builder()
                            .putString(
                                TrialNotificationWorker.KEY_NOTIFICATION_TYPE,
                                TrialNotificationWorker.TYPE_DAY7,
                            ).build(),
                    ).addTag(TrialNotificationWorker.WORK_TAG_DAY7)
                    .build()

            workManager.enqueueUniqueWork(
                TrialNotificationWorker.WORK_TAG_DAY5,
                ExistingWorkPolicy.KEEP,
                day5Request,
            )
            workManager.enqueueUniqueWork(
                TrialNotificationWorker.WORK_TAG_DAY7,
                ExistingWorkPolicy.KEEP,
                day7Request,
            )
        }

        fun cancelTrialNotifications() {
            workManager.cancelUniqueWork(TrialNotificationWorker.WORK_TAG_DAY5)
            workManager.cancelUniqueWork(TrialNotificationWorker.WORK_TAG_DAY7)
        }

        companion object {
            const val TRIAL_DURATION_DAYS = 7
            private val Context.trialDataStore: DataStore<Preferences>
                by preferencesDataStore(name = "trial_state")
            private val KEY_TRIAL_START = longPreferencesKey("trial_start_timestamp")
            private val KEY_LAST_KNOWN_TIMESTAMP = longPreferencesKey("last_known_timestamp")
            private val KEY_WELCOME_SHOWN = booleanPreferencesKey("trial_welcome_shown")
            private val KEY_DAY5_PROMPT_SHOWN = booleanPreferencesKey("day5_prompt_shown")
            private val KEY_UPGRADE_DISMISS_COUNT = intPreferencesKey("upgrade_card_dismiss_count")
            private val KEY_UPGRADE_DISMISS_TIMESTAMP = longPreferencesKey("upgrade_card_last_dismiss_timestamp")
        }
    }
