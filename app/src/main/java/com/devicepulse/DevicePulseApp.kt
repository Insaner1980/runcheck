package com.devicepulse

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.devicepulse.data.billing.ProStatusRepository
import com.devicepulse.domain.repository.UserPreferencesRepository
import com.devicepulse.service.monitor.NotificationHelper
import com.devicepulse.service.monitor.MonitorScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DevicePulseApp : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var proStatusRepository: ProStatusRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var monitorScheduler: MonitorScheduler

    override fun onCreate() {
        super.onCreate()

        proStatusRepository.initialize()
        notificationHelper.createChannels()
        applicationScope.launch {
            val prefs = preferencesRepository.getPreferences().first()
            monitorScheduler.schedule(prefs.monitoringInterval)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
