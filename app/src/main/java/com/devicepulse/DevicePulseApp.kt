package com.devicepulse

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.devicepulse.data.billing.ProStatusRepository
import com.devicepulse.domain.repository.CrashReportingController
import com.devicepulse.domain.repository.MonitoringScheduler
import com.devicepulse.service.monitor.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    lateinit var monitorScheduler: MonitoringScheduler

    @Inject
    lateinit var crashReportingController: CrashReportingController

    override fun onCreate() {
        super.onCreate()

        proStatusRepository.initialize()
        notificationHelper.createChannels()
        applicationScope.launch {
            crashReportingController.initialize()
            monitorScheduler.ensureScheduled()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
