package com.runcheck

import android.app.Application
import android.content.Context
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.runcheck.data.billing.BillingManager
import com.runcheck.domain.repository.CrashReportingController
import com.runcheck.domain.repository.MonitoringScheduler
import com.runcheck.domain.repository.ScreenStateRepository
import com.runcheck.pro.ProManager
import com.runcheck.service.monitor.NotificationHelper
import com.runcheck.util.ReleaseSafeLog
import com.runcheck.widget.RuncheckWidgets
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class RuncheckApp : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Apply the user's chosen locale to the Application context so that
     * background workers (WorkManager) and services that call getString()
     * produce notification strings in the correct language.
     */
    override fun attachBaseContext(base: Context) {
        val prefs = base.getSharedPreferences(
            MainActivity.LANGUAGE_PREFS, Context.MODE_PRIVATE
        )
        val tag = prefs.getString(MainActivity.KEY_LANGUAGE_TAG, null)
        if (tag != null) {
            val locale = Locale.forLanguageTag(tag)
            val config = base.resources.configuration.apply { setLocale(locale) }
            super.attachBaseContext(base.createConfigurationContext(config))
        } else {
            super.attachBaseContext(base)
        }
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var billingManager: dagger.Lazy<BillingManager>

    @Inject
    lateinit var proManager: dagger.Lazy<ProManager>

    @Inject
    lateinit var notificationHelper: dagger.Lazy<NotificationHelper>

    @Inject
    lateinit var monitorScheduler: dagger.Lazy<MonitoringScheduler>

    @Inject
    lateinit var screenStateRepository: dagger.Lazy<ScreenStateRepository>

    @Inject
    lateinit var crashReportingController: dagger.Lazy<CrashReportingController>

    override fun onCreate() {
        super.onCreate()
        configureDebugStrictMode()

        // Defer heavy initialization to after the first frame
        launchSafely(Dispatchers.Default, "billing/pro initialization") {
            billingManager.get().initialize()
            proManager.get().initialize()
        }
        launchSafely(Dispatchers.Default, "notification channel creation") {
            notificationHelper.get().createChannels()
        }
        launchSafely(Dispatchers.Default, "crash reporting + scheduling") {
            crashReportingController.get().initialize()
            screenStateRepository.get().initialize()
            monitorScheduler.get().ensureScheduled()
        }
        // Update widgets when pro status changes
        launchSafely(Dispatchers.Default, "widget updates") {
            billingManager.get().isProUser.distinctUntilChanged().collect {
                RuncheckWidgets.updateAll(this@RuncheckApp)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        billingManager.get().destroy()
        applicationScope.cancel()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun launchSafely(
        dispatcher: kotlinx.coroutines.CoroutineDispatcher,
        taskName: String,
        block: suspend () -> Unit
    ) {
        applicationScope.launch(dispatcher) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                ReleaseSafeLog.error(TAG, "Application coroutine failed: $taskName", t)
            }
        }
    }

    private fun configureDebugStrictMode() {
        if (!BuildConfig.DEBUG) return

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }

    private companion object {
        private const val TAG = "RuncheckApp"
    }
}
