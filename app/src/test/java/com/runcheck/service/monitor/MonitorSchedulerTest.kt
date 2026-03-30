package com.runcheck.service.monitor

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.UserPreferencesRepository
import com.runcheck.ui.MainDispatcherRule
import com.runcheck.worker.InsightGenerationWorker
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorSchedulerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = mockk(relaxed = true)
    private val preferencesRepository: UserPreferencesRepository = mockk()
    private val workManager: WorkManager = mockk()
    private val operation: Operation = mockk(relaxed = true)

    private lateinit var scheduler: MonitorScheduler

    @Before
    fun setUp() {
        every {
            workManager.enqueueUniquePeriodicWork(any(), any(), any())
        } returns operation
        every {
            workManager.cancelUniqueWork(any())
        } returns operation

        scheduler =
            MonitorScheduler(
                workManager = workManager,
                preferencesRepository = preferencesRepository,
            )
    }

    @Test
    fun `schedule enqueues health monitor maintenance and insight workers`() {
        val healthMonitorRequest = slot<PeriodicWorkRequest>()
        val maintenanceRequest = slot<PeriodicWorkRequest>()
        val insightRequest = slot<PeriodicWorkRequest>()

        every {
            workManager.enqueueUniquePeriodicWork(
                HealthMonitorWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                capture(healthMonitorRequest),
            )
        } returns operation
        every {
            workManager.enqueueUniquePeriodicWork(
                HealthMaintenanceWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                capture(maintenanceRequest),
            )
        } returns operation
        every {
            workManager.enqueueUniquePeriodicWork(
                InsightGenerationWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                capture(insightRequest),
            )
        } returns operation

        scheduler.schedule(MonitoringInterval.THIRTY)

        assertEquals(
            TimeUnit.MINUTES.toMillis(MonitoringInterval.THIRTY.minutes.toLong()),
            healthMonitorRequest.captured.workSpec.intervalDuration,
        )
        assertEquals(
            TimeUnit.MINUTES.toMillis(MonitoringInterval.THIRTY.minutes.toLong()),
            maintenanceRequest.captured.workSpec.intervalDuration,
        )
        assertEquals(
            TimeUnit.HOURS.toMillis(6),
            insightRequest.captured.workSpec.intervalDuration,
        )
        assertTrue(
            maintenanceRequest.captured.workSpec.constraints
                .requiresBatteryNotLow(),
        )
        assertTrue(
            insightRequest.captured.workSpec.constraints
                .requiresBatteryNotLow(),
        )
    }

    @Test
    fun `cancel removes all scheduled work`() {
        scheduler.cancel()

        verify(exactly = 1) { workManager.cancelUniqueWork(HealthMonitorWorker.WORK_NAME) }
        verify(exactly = 1) { workManager.cancelUniqueWork(HealthMaintenanceWorker.WORK_NAME) }
        verify(exactly = 1) { workManager.cancelUniqueWork(InsightGenerationWorker.WORK_NAME) }
    }

    @Test
    fun `ensureScheduled uses stored monitoring interval`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val healthMonitorRequest = slot<PeriodicWorkRequest>()

            every { preferencesRepository.getPreferences() } returns
                flowOf(UserPreferences(monitoringInterval = MonitoringInterval.SIXTY))
            every {
                workManager.enqueueUniquePeriodicWork(
                    HealthMonitorWorker.WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    capture(healthMonitorRequest),
                )
            } returns operation

            scheduler.ensureScheduled()

            assertEquals(
                TimeUnit.MINUTES.toMillis(MonitoringInterval.SIXTY.minutes.toLong()),
                healthMonitorRequest.captured.workSpec.intervalDuration,
            )
        }
}
