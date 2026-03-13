package com.devicepulse.service.monitor

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.devicepulse.domain.model.MonitoringInterval
import com.devicepulse.domain.repository.MonitoringScheduler
import com.devicepulse.domain.repository.UserPreferencesRepository
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

    override fun cancel() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(HealthMonitorWorker.WORK_NAME)
    }

    override suspend fun ensureScheduled() {
        val interval = preferencesRepository.getPreferences().first().monitoringInterval
        schedule(interval)
    }
}
