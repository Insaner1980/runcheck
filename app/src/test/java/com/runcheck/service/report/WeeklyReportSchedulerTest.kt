package com.runcheck.service.report

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.UserPreferencesRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class WeeklyReportSchedulerTest {
    private val workManager = mockk<WorkManager>()
    private val operation = mockk<Operation>(relaxed = true)
    private val preferencesRepository = mockk<UserPreferencesRepository>()

    @Test
    fun `enabled Pro schedules one unique Monday morning work`() =
        runTest {
            val request = slot<OneTimeWorkRequest>()
            every { preferencesRepository.getPreferences() } returns
                flowOf(UserPreferences(weeklyReportEnabled = true))
            every {
                workManager.enqueueUniqueWork(
                    WeeklyReportWorker.WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    capture(request),
                )
            } returns operation
            val now = Instant.parse("2026-02-16T06:00:00Z")
            val scheduler =
                WeeklyReportScheduler(
                    workManager,
                    preferencesRepository,
                    SchedulerProStatus(true),
                    Clock.fixed(now, ZoneId.of("UTC")),
                    { ZoneId.of("Europe/Helsinki") },
                )

            scheduler.ensureScheduled()

            assertEquals(Duration.ofHours(1).toMillis(), request.captured.workSpec.initialDelay)
        }

    @Test
    fun `worker appends next Monday without replacing itself`() =
        runTest {
            val request = slot<OneTimeWorkRequest>()
            every { preferencesRepository.getPreferences() } returns
                flowOf(UserPreferences(weeklyReportEnabled = true))
            every {
                workManager.enqueueUniqueWork(
                    WeeklyReportWorker.WORK_NAME,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    capture(request),
                )
            } returns operation
            val now = Instant.parse("2026-02-16T07:00:00Z")
            val scheduler =
                WeeklyReportScheduler(
                    workManager,
                    preferencesRepository,
                    SchedulerProStatus(true),
                    Clock.fixed(now, ZoneId.of("UTC")),
                    { ZoneId.of("Europe/Helsinki") },
                )

            scheduler.scheduleNextAfterCurrent()

            assertEquals(Duration.ofDays(7).toMillis(), request.captured.workSpec.initialDelay)
            verify(exactly = 0) {
                workManager.enqueueUniqueWork(
                    WeeklyReportWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    any<OneTimeWorkRequest>(),
                )
            }
        }

    @Test
    fun `toggle off cancels unique work`() =
        runTest {
            every { preferencesRepository.getPreferences() } returns
                flowOf(UserPreferences(weeklyReportEnabled = false))
            every { workManager.cancelUniqueWork(WeeklyReportWorker.WORK_NAME) } returns operation
            val scheduler =
                WeeklyReportScheduler(
                    workManager,
                    preferencesRepository,
                    SchedulerProStatus(true),
                    Clock.systemUTC(),
                    { ZoneId.of("UTC") },
                )

            scheduler.ensureScheduled()

            verify(exactly = 1) { workManager.cancelUniqueWork(WeeklyReportWorker.WORK_NAME) }
            verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
        }

    @Test
    fun `expired Pro preserves selection but cancels delivery`() =
        runTest {
            every { preferencesRepository.getPreferences() } returns
                flowOf(UserPreferences(weeklyReportEnabled = true))
            every { workManager.cancelUniqueWork(WeeklyReportWorker.WORK_NAME) } returns operation
            val scheduler =
                WeeklyReportScheduler(
                    workManager,
                    preferencesRepository,
                    SchedulerProStatus(false),
                    Clock.systemUTC(),
                    { ZoneId.of("UTC") },
                )

            scheduler.ensureScheduled()

            verify(exactly = 1) { workManager.cancelUniqueWork(WeeklyReportWorker.WORK_NAME) }
            coVerify(exactly = 0) { preferencesRepository.setWeeklyReportEnabled(false) }
        }

    @Test
    fun `unready cold start does not cancel Pro report before state restores`() =
        runTest {
            every { preferencesRepository.getPreferences() } returns
                flowOf(UserPreferences(weeklyReportEnabled = true))
            val proStatus = SchedulerProStatus(value = false, ready = false)
            val scheduler =
                WeeklyReportScheduler(
                    workManager,
                    preferencesRepository,
                    proStatus,
                    Clock.systemUTC(),
                    { ZoneId.of("UTC") },
                )

            scheduler.ensureScheduled()

            verify(exactly = 0) { workManager.cancelUniqueWork(WeeklyReportWorker.WORK_NAME) }
            verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }

            proStatus.value = true
            proStatus.ready = true
            every {
                workManager.enqueueUniqueWork(
                    WeeklyReportWorker.WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    any<OneTimeWorkRequest>(),
                )
            } returns operation

            scheduler.ensureScheduled()

            verify(exactly = 1) {
                workManager.enqueueUniqueWork(
                    WeeklyReportWorker.WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    any<OneTimeWorkRequest>(),
                )
            }
        }

    @Test
    fun `timezone change explicitly replaces the existing target`() =
        runTest {
            every { preferencesRepository.getPreferences() } returns
                flowOf(UserPreferences(weeklyReportEnabled = true))
            every {
                workManager.enqueueUniqueWork(
                    WeeklyReportWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    any<OneTimeWorkRequest>(),
                )
            } returns operation
            val scheduler =
                WeeklyReportScheduler(
                    workManager,
                    preferencesRepository,
                    SchedulerProStatus(true),
                    Clock.systemUTC(),
                    { ZoneId.of("UTC") },
                )

            scheduler.rescheduleForTimezoneChange()

            verify(exactly = 1) {
                workManager.enqueueUniqueWork(
                    WeeklyReportWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    any<OneTimeWorkRequest>(),
                )
            }
        }

    @Test
    fun `unready timezone change is consumed once with replacement after Pro restores`() =
        runTest {
            every { preferencesRepository.getPreferences() } returns
                flowOf(UserPreferences(weeklyReportEnabled = true))
            val proStatus = SchedulerProStatus(value = false, ready = false)
            val scheduler =
                WeeklyReportScheduler(
                    workManager,
                    preferencesRepository,
                    proStatus,
                    Clock.systemUTC(),
                    { ZoneId.of("UTC") },
                )
            every {
                workManager.enqueueUniqueWork(
                    WeeklyReportWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    any<OneTimeWorkRequest>(),
                )
            } returns operation
            every {
                workManager.enqueueUniqueWork(
                    WeeklyReportWorker.WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    any<OneTimeWorkRequest>(),
                )
            } returns operation

            scheduler.rescheduleForTimezoneChange()
            proStatus.value = true
            proStatus.ready = true
            scheduler.ensureScheduled()
            scheduler.ensureScheduled()

            verify(exactly = 1) {
                workManager.enqueueUniqueWork(
                    WeeklyReportWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    any<OneTimeWorkRequest>(),
                )
            }
            verify(exactly = 1) {
                workManager.enqueueUniqueWork(
                    WeeklyReportWorker.WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    any<OneTimeWorkRequest>(),
                )
            }
        }

    @Test
    fun `pending timezone reconcile clears without replacement for confirmed free access`() =
        runTest {
            every { preferencesRepository.getPreferences() } returns
                flowOf(UserPreferences(weeklyReportEnabled = true))
            every { workManager.cancelUniqueWork(WeeklyReportWorker.WORK_NAME) } returns operation
            val proStatus = SchedulerProStatus(value = false, ready = false)
            val scheduler =
                WeeklyReportScheduler(
                    workManager,
                    preferencesRepository,
                    proStatus,
                    Clock.systemUTC(),
                    { ZoneId.of("UTC") },
                )

            scheduler.rescheduleForTimezoneChange()
            proStatus.ready = true
            scheduler.ensureScheduled()

            verify(exactly = 1) { workManager.cancelUniqueWork(WeeklyReportWorker.WORK_NAME) }
            verify(exactly = 0) {
                workManager.enqueueUniqueWork(
                    WeeklyReportWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    any<OneTimeWorkRequest>(),
                )
            }
        }
}

private class SchedulerProStatus(
    var value: Boolean,
    var ready: Boolean = true,
) : ProStatusProvider {
    override val isProUser: Flow<Boolean> = flowOf(value)
    override val isProStatusReady: Boolean
        get() = ready

    override fun isPro(): Boolean = value
}
