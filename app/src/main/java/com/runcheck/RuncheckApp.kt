package com.runcheck

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.gms.ads.MobileAds
import com.runcheck.data.billing.ProStatusRepository
import com.runcheck.domain.repository.CrashReportingController
import com.runcheck.domain.repository.MonitoringScheduler
import com.runcheck.pro.ProManager
import com.runcheck.service.monitor.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class RuncheckApp : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var proStatusRepository: ProStatusRepository

    @Inject
    lateinit var proManager: ProManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var monitorScheduler: MonitoringScheduler

    @Inject
    lateinit var crashReportingController: CrashReportingController

    override fun onCreate() {
        super.onCreate()

        proStatusRepository.initialize()
        proManager.initialize()
        notificationHelper.createChannels()
        MobileAds.initialize(this) {}
        applicationScope.launch {
            crashReportingController.initialize()
            monitorScheduler.ensureScheduled()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
        proStatusRepository.destroy()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
