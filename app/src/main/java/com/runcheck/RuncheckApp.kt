package com.runcheck

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.gms.ads.MobileAds
import com.runcheck.data.billing.BillingManager
import com.runcheck.domain.repository.CrashReportingController
import com.runcheck.domain.repository.MonitoringScheduler
import com.runcheck.pro.ProManager
import com.runcheck.service.monitor.NotificationHelper
import com.runcheck.widget.RuncheckWidgets
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class RuncheckApp : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var billingManager: BillingManager

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

        billingManager.initialize()
        proManager.initialize()
        notificationHelper.createChannels()
        MobileAds.initialize(this) {}
        applicationScope.launch {
            crashReportingController.initialize()
            monitorScheduler.ensureScheduled()
        }
        // Update widgets when pro status changes
        applicationScope.launch {
            billingManager.isProUser.distinctUntilChanged().collect {
                RuncheckWidgets.updateAll(this@RuncheckApp)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
        billingManager.destroy()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
