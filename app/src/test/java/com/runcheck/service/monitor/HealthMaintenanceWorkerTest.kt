package com.runcheck.service.monitor

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.usecase.CleanupOldReadingsUseCase
import com.runcheck.domain.usecase.RefreshAppUsageSnapshotUseCase
import com.runcheck.widget.RuncheckWidgets
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HealthMaintenanceWorkerTest {
    private val context: Context = mockk(relaxed = true)
    private val workerParameters: WorkerParameters = mockk(relaxed = true)
    private val refreshAppUsageSnapshot: RefreshAppUsageSnapshotUseCase = mockk(relaxed = true)
    private val cleanupOldReadings: CleanupOldReadingsUseCase = mockk(relaxed = true)
    private val proStatusProvider: ProStatusProvider = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { context.applicationContext } returns context
        every { proStatusProvider.isProStatusReady } returns true
        mockkObject(RuncheckWidgets)
        coEvery { RuncheckWidgets.updateAll(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(RuncheckWidgets)
    }

    @Test
    fun `widget refresh failure remains best effort`() =
        runTest {
            coEvery { RuncheckWidgets.updateAll(any()) } throws IllegalStateException("widget failed")

            val result = createWorker().doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            coVerify(exactly = 1) { refreshAppUsageSnapshot() }
            coVerify(exactly = 1) { cleanupOldReadings() }
        }

    @Test
    fun `app usage snapshot failure requests retry without skipping later steps`() =
        runTest {
            coEvery { refreshAppUsageSnapshot() } throws IllegalStateException("app usage failed")

            val result = createWorker().doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
            coVerify(exactly = 1) { cleanupOldReadings() }
            coVerify(exactly = 1) { RuncheckWidgets.updateAll(context) }
        }

    @Test
    fun `old reading cleanup failure requests retry`() =
        runTest {
            coEvery { cleanupOldReadings() } throws IllegalStateException("cleanup failed")

            val result = createWorker().doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
            coVerify(exactly = 1) { RuncheckWidgets.updateAll(context) }
        }

    @Test
    fun `cleanup waits for ready pro status and requests retry`() =
        runTest {
            every { proStatusProvider.isProStatusReady } returns false

            val result = createWorker().doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
            coVerify(exactly = 0) { cleanupOldReadings() }
            coVerify(exactly = 1) { RuncheckWidgets.updateAll(context) }
        }

    private fun createWorker() =
        HealthMaintenanceWorker(
            context = context,
            workerParams = workerParameters,
            refreshAppUsageSnapshot = refreshAppUsageSnapshot,
            cleanupOldReadings = cleanupOldReadings,
            proStatusProvider = proStatusProvider,
        )
}
