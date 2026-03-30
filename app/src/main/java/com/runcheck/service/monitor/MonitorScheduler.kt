package com.runcheck.service.monitor

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.repository.MonitoringScheduler
import com.runcheck.domain.repository.UserPreferencesRepository
import com.runcheck.worker.InsightGenerationWorker
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitorScheduler
    @Inject
    constructor(
        private val workManager: WorkManager,
        private val preferencesRepository: UserPreferencesRepository,
    ) : MonitoringScheduler {
        override fun schedule(interval: MonitoringInterval) {
            val alertWorkRequest =
                PeriodicWorkRequestBuilder<HealthMonitorWorker>(
                    interval.minutes.toLong(),
                    TimeUnit.MINUTES,
                ).build()

            workManager.enqueueUniquePeriodicWork(
                HealthMonitorWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                alertWorkRequest,
            )

            val maintenanceWorkRequest =
                PeriodicWorkRequestBuilder<HealthMaintenanceWorker>(
                    interval.minutes.toLong(),
                    TimeUnit.MINUTES,
                ).setConstraints(
                    Constraints
                        .Builder()
                        .setRequiresBatteryNotLow(true)
                        .build(),
                ).build()

            workManager.enqueueUniquePeriodicWork(
                HealthMaintenanceWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                maintenanceWorkRequest,
            )

            val insightWorkRequest =
                PeriodicWorkRequestBuilder<InsightGenerationWorker>(
                    INSIGHT_INTERVAL_HOURS,
                    TimeUnit.HOURS,
                ).setConstraints(
                    Constraints
                        .Builder()
                        .setRequiresBatteryNotLow(true)
                        .build(),
                ).build()

            workManager.enqueueUniquePeriodicWork(
                InsightGenerationWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                insightWorkRequest,
            )
        }

        override fun cancel() {
            workManager.cancelUniqueWork(HealthMonitorWorker.WORK_NAME)
            workManager.cancelUniqueWork(HealthMaintenanceWorker.WORK_NAME)
            workManager.cancelUniqueWork(InsightGenerationWorker.WORK_NAME)
        }

        override suspend fun ensureScheduled() {
            val interval = preferencesRepository.getPreferences().first().monitoringInterval
            schedule(interval)
        }

        private companion object {
            const val INSIGHT_INTERVAL_HOURS = 6L
        }
    }
