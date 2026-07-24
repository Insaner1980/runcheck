package com.runcheck.service.report

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
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

        suspend fun ensureScheduled() {
            schedule(
                existingWorkPolicy = ExistingWorkPolicy.REPLACE,
                cancelWhenDisabled = true,
            )
        }

        suspend fun scheduleNextAfterCurrent() {
            schedule(
                existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE,
                cancelWhenDisabled = false,
            )
        }

        private suspend fun schedule(
            existingWorkPolicy: ExistingWorkPolicy,
            cancelWhenDisabled: Boolean,
        ) {
            val preferences = preferencesRepository.getPreferences().first()
            if (!preferences.weeklyReportEnabled || !proStatusProvider.isPro()) {
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
                existingWorkPolicy,
                request,
            )
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
