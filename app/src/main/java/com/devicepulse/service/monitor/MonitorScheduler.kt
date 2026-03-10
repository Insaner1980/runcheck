package com.devicepulse.service.monitor

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.devicepulse.domain.model.MonitoringInterval
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitorScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun schedule(interval: MonitoringInterval) {
        val workRequest = PeriodicWorkRequestBuilder<HealthMonitorWorker>(
            interval.minutes.toLong(),
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HealthMonitorWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancel() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(HealthMonitorWorker.WORK_NAME)
    }
}
