package com.runcheck.service.monitor

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.repository.MonitoringScheduler
import com.runcheck.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitorScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) : MonitoringScheduler {

    override fun schedule(interval: MonitoringInterval) {
        val alertWorkRequest = PeriodicWorkRequestBuilder<HealthMonitorWorker>(
            interval.minutes.toLong(),
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HealthMonitorWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            alertWorkRequest
        )

        val maintenanceWorkRequest = PeriodicWorkRequestBuilder<HealthMaintenanceWorker>(
            interval.minutes.toLong(),
            TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HealthMaintenanceWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            maintenanceWorkRequest
        )
    }

    override fun cancel() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(HealthMonitorWorker.WORK_NAME)
        WorkManager.getInstance(context)
            .cancelUniqueWork(HealthMaintenanceWorker.WORK_NAME)
    }

    override suspend fun ensureScheduled() {
        val interval = preferencesRepository.getPreferences().first().monitoringInterval
        schedule(interval)
    }
}
