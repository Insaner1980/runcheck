package com.runcheck.pro

import android.content.Context
import androidx.annotation.VisibleForTesting
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

internal data class TrialStateResolution(
    val state: TrialState,
    val lastKnownTimestamp: Long,
    val clockTampered: Boolean,
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
            val storedClockTampered = prefs[KEY_CLOCK_TAMPERED] ?: false
            val now = System.currentTimeMillis()
            val resolution =
                resolveTrialState(
                    startTimestamp = startTimestamp,
                    lastKnownTimestamp = lastKnown,
                    storedClockTampered = storedClockTampered,
                    now = now,
                )

            context.trialDataStore.edit {
                it[KEY_LAST_KNOWN_TIMESTAMP] = resolution.lastKnownTimestamp
                if (resolution.state.isFirstLaunch) {
                    it[KEY_TRIAL_START] = resolution.state.startTimestamp
                }
                if (resolution.clockTampered) {
                    it[KEY_CLOCK_TAMPERED] = true
                }
            }

            _trialState.value = resolution.state

            if (resolution.state.isFirstLaunch && !resolution.state.clockTampered) {
                scheduleTrialNotifications(resolution.state.startTimestamp)
            }

            return resolution.state.isFirstLaunch
        }

        suspend fun updateTimestamp() {
            val prefs = context.trialDataStore.data.first()
            val lastKnown = prefs[KEY_LAST_KNOWN_TIMESTAMP] ?: 0L
            val now = System.currentTimeMillis()
            val clockTampered = isClockTampered(lastKnownTimestamp = lastKnown, now = now)
            context.trialDataStore.edit {
                it[KEY_LAST_KNOWN_TIMESTAMP] = maxOf(lastKnown, now)
                if (clockTampered) {
                    it[KEY_CLOCK_TAMPERED] = true
                }
            }
            if (clockTampered) {
                _trialState.value =
                    _trialState.value.copy(
                        isActive = false,
                        daysRemaining = 0,
                        clockTampered = true,
                    )
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
                    ).addTag(TrialNotificationWorker.TAG_DAY5)
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
                    ).addTag(TrialNotificationWorker.TAG_DAY7)
                    .build()

            workManager.enqueueUniqueWork(
                TrialNotificationWorker.UNIQUE_WORK_DAY5,
                ExistingWorkPolicy.KEEP,
                day5Request,
            )
            workManager.enqueueUniqueWork(
                TrialNotificationWorker.UNIQUE_WORK_DAY7,
                ExistingWorkPolicy.KEEP,
                day7Request,
            )
        }

        fun cancelTrialNotifications() {
            workManager.cancelUniqueWork(TrialNotificationWorker.UNIQUE_WORK_DAY5)
            workManager.cancelUniqueWork(TrialNotificationWorker.UNIQUE_WORK_DAY7)
        }

        companion object {
            const val TRIAL_DURATION_DAYS = 7
            private val Context.trialDataStore: DataStore<Preferences>
                by preferencesDataStore(name = "trial_state")
            private val KEY_TRIAL_START = longPreferencesKey("trial_start_timestamp")
            private val KEY_LAST_KNOWN_TIMESTAMP = longPreferencesKey("last_known_timestamp")
            private val KEY_CLOCK_TAMPERED = booleanPreferencesKey("clock_tampered")
            private val KEY_WELCOME_SHOWN = booleanPreferencesKey("trial_welcome_shown")
            private val KEY_DAY5_PROMPT_SHOWN = booleanPreferencesKey("day5_prompt_shown")
            private val KEY_UPGRADE_DISMISS_COUNT = intPreferencesKey("upgrade_card_dismiss_count")
            private val KEY_UPGRADE_DISMISS_TIMESTAMP = longPreferencesKey("upgrade_card_last_dismiss_timestamp")
            private val CLOCK_TAMPER_TOLERANCE_MS = TimeUnit.HOURS.toMillis(1)

            @VisibleForTesting
            internal fun resolveTrialState(
                startTimestamp: Long,
                lastKnownTimestamp: Long,
                storedClockTampered: Boolean,
                now: Long,
            ): TrialStateResolution {
                val clockTampered =
                    storedClockTampered || isClockTampered(lastKnownTimestamp = lastKnownTimestamp, now = now)
                val isFirstLaunch = startTimestamp == 0L
                val actualStart = if (isFirstLaunch) now else startTimestamp
                val elapsedMs = (now - actualStart).coerceAtLeast(0L)
                val daysElapsed = TimeUnit.MILLISECONDS.toDays(elapsedMs).toInt()
                val daysRemaining =
                    if (clockTampered) {
                        0
                    } else {
                        (TRIAL_DURATION_DAYS - daysElapsed).coerceIn(0, TRIAL_DURATION_DAYS)
                    }

                return TrialStateResolution(
                    state =
                        TrialState(
                            isActive = daysRemaining > 0 && !clockTampered,
                            daysRemaining = daysRemaining,
                            startTimestamp = actualStart,
                            isFirstLaunch = isFirstLaunch,
                            clockTampered = clockTampered,
                        ),
                    lastKnownTimestamp = maxOf(lastKnownTimestamp, now),
                    clockTampered = clockTampered,
                )
            }

            private fun isClockTampered(
                lastKnownTimestamp: Long,
                now: Long,
            ): Boolean = lastKnownTimestamp > 0L && now < lastKnownTimestamp - CLOCK_TAMPER_TOLERANCE_MS
        }
    }
