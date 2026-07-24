package com.runcheck.service.report

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.UserPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.coVerify
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
                    ExistingWorkPolicy.REPLACE,
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
}

private class SchedulerProStatus(
    private val value: Boolean,
) : ProStatusProvider {
    override val isProUser: Flow<Boolean> = flowOf(value)
    override val isProStatusReady: Boolean = true

    override fun isPro(): Boolean = value
}
