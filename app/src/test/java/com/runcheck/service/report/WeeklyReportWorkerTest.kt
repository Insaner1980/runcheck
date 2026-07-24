package com.runcheck.service.report

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.model.WeeklyReport
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.UserPreferencesRepository
import com.runcheck.domain.usecase.GenerateWeeklyReportUseCase
import com.runcheck.domain.usecase.WeeklyReportGenerationResult
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class WeeklyReportWorkerTest {
    private val context = mockk<Context>(relaxed = true)
    private val params = mockk<WorkerParameters>(relaxed = true)
    private val preferences = mockk<UserPreferencesRepository>()
    private val generateReport = mockk<GenerateWeeklyReportUseCase>()
    private val notifier = mockk<WeeklyReportNotifier>(relaxed = true)
    private val scheduler = mockk<WeeklyReportScheduler>(relaxed = true)
    private val now = Instant.parse("2026-02-16T07:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("UTC"))

    @Test
    fun `denied notifications mark period handled without loading report`() =
        runTest {
            every { preferences.getPreferences() } returns
                flowOf(UserPreferences(notificationsEnabled = true, weeklyReportEnabled = true))
            coEvery { preferences.getWeeklyReportLastProcessedPeriod() } returns null
            coJustRun { preferences.setWeeklyReportLastProcessedPeriod(any(), any()) }
            val worker = worker(canNotify = false)

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            coVerify(exactly = 1) { preferences.setWeeklyReportLastProcessedPeriod(any(), any()) }
            coVerify(exactly = 0) { generateReport(any()) }
            coVerify(exactly = 1) { scheduler.scheduleNextAfterCurrent() }
        }

    @Test
    fun `successful delivery is processed once and schedules next Monday`() =
        runTest {
            every { preferences.getPreferences() } returns
                flowOf(UserPreferences(notificationsEnabled = true, weeklyReportEnabled = true))
            coEvery { preferences.getWeeklyReportLastProcessedPeriod() } returns null
            coJustRun { preferences.setWeeklyReportLastProcessedPeriod(any(), any()) }
            coEvery { generateReport(any()) } returns
                WeeklyReportGenerationResult.Available(mockk<WeeklyReport>())
            coEvery { notifier.show(any()) } returns true
            val worker = worker(canNotify = true)

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            coVerify(exactly = 1) { notifier.show(any()) }
            coVerify(exactly = 1) { preferences.setWeeklyReportLastProcessedPeriod(any(), any()) }
            coVerify(exactly = 1) { scheduler.scheduleNextAfterCurrent() }
        }

    @Test
    fun `transient generation failure retries without creating another schedule`() =
        runTest {
            every { preferences.getPreferences() } returns
                flowOf(UserPreferences(notificationsEnabled = true, weeklyReportEnabled = true))
            coEvery { preferences.getWeeklyReportLastProcessedPeriod() } returns null
            coEvery { generateReport(any()) } throws IllegalStateException("database unavailable")
            val worker = worker(canNotify = true)

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
            coVerify(exactly = 0) { preferences.setWeeklyReportLastProcessedPeriod(any(), any()) }
            coVerify(exactly = 0) { scheduler.ensureScheduled() }
        }

    private fun worker(canNotify: Boolean) =
        WeeklyReportWorker(
            context,
            params,
            preferences,
            generateReport,
            WorkerProStatus(true),
            notifier,
            scheduler,
            WeeklyReportNotificationGate { canNotify },
            clock,
            { ZoneId.of("Europe/Helsinki") },
        )
}

private class WorkerProStatus(
    private val value: Boolean,
) : ProStatusProvider {
    override val isProUser: Flow<Boolean> = flowOf(value)
    override val isProStatusReady: Boolean = true

    override fun isPro(): Boolean = value
}
