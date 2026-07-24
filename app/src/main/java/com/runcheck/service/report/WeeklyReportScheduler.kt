package com.runcheck.service.report

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeeklyReportScheduler
    @Inject
    constructor(
        private val workManager: WorkManager,
        private val preferencesRepository: UserPreferencesRepository,
        private val proStatusProvider: ProStatusProvider,
        private val timeProvider: WeeklyReportTimeProvider,
    ) {
        internal constructor(
            workManager: WorkManager,
            preferencesRepository: UserPreferencesRepository,
            proStatusProvider: ProStatusProvider,
            clock: Clock,
            zoneProvider: () -> ZoneId,
        ) : this(
            workManager,
            preferencesRepository,
            proStatusProvider,
            WeeklyReportTimeProvider().apply {
                overrideClock = clock
                overrideZoneProvider = zoneProvider
            },
        )

        private val reconcileMutex = Mutex()
        private var timezoneReconcilePending = false

        suspend fun ensureScheduled() =
            reconcileMutex.withLock {
                schedule(
                    existingWorkPolicy = ExistingWorkPolicy.KEEP,
                    cancelWhenDisabled = true,
                )
            }

        suspend fun rescheduleForTimezoneChange() =
            reconcileMutex.withLock {
                timezoneReconcilePending = true
                schedule(
                    existingWorkPolicy = ExistingWorkPolicy.KEEP,
                    cancelWhenDisabled = true,
                )
            }

        suspend fun scheduleNextAfterCurrent() =
            reconcileMutex.withLock {
                schedule(
                    existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE,
                    cancelWhenDisabled = false,
                )
            }

        private suspend fun schedule(
            existingWorkPolicy: ExistingWorkPolicy,
            cancelWhenDisabled: Boolean,
        ) {
            if (!proStatusProvider.isProStatusReady) return
            val preferences = preferencesRepository.getPreferences().first()
            if (!preferences.weeklyReportEnabled || !proStatusProvider.isPro()) {
                timezoneReconcilePending = false
                if (cancelWhenDisabled) {
                    workManager.cancelUniqueWork(WeeklyReportWorker.WORK_NAME)
                }
                return
            }
            val now = timeProvider.clock.instant()
            val scheduledAt = nextMondayMorning(now, timeProvider.zoneId())
            val delay = Duration.between(now, scheduledAt).toMillis().coerceAtLeast(0)
            val request =
                OneTimeWorkRequestBuilder<WeeklyReportWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .build()
            workManager.enqueueUniqueWork(
                WeeklyReportWorker.WORK_NAME,
                if (timezoneReconcilePending) ExistingWorkPolicy.REPLACE else existingWorkPolicy,
                request,
            )
            timezoneReconcilePending = false
        }

    }

internal fun nextMondayMorning(
    now: java.time.Instant,
    zoneId: ZoneId,
): java.time.Instant {
    val zonedNow = now.atZone(zoneId)
    var candidate =
        zonedNow
            .toLocalDate()
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
            .atTime(WEEKLY_REPORT_HOUR, 0)
            .atZone(zoneId)
    if (!candidate.toInstant().isAfter(now)) {
        candidate = candidate.plusWeeks(1)
    }
    return candidate.toInstant()
}

private const val WEEKLY_REPORT_HOUR = 9
