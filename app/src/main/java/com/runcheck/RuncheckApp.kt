package com.runcheck

import android.app.Application
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.runcheck.data.billing.BillingManager
import com.runcheck.domain.repository.MonitoringScheduler
import com.runcheck.domain.repository.ScreenStateRepository
import com.runcheck.pro.ProManager
import com.runcheck.service.monitor.NotificationHelper
import com.runcheck.util.AppDispatchers
import com.runcheck.util.ReleaseSafeLog
import com.runcheck.widget.RuncheckWidgets
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class RuncheckApp :
    Application(),
    Configuration.Provider {
    private lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var dispatchers: AppDispatchers

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

    override fun onCreate() {
        super.onCreate()
        applicationScope = CoroutineScope(SupervisorJob() + dispatchers.default)
        SentryInit.init(this)
        configureDebugStrictMode()

        // Defer heavy initialization to after the first frame
        launchSafely(dispatchers.default, "billing/pro initialization") {
            billingManager.get().initialize()
            proManager.get().initialize()
        }
        launchSafely(dispatchers.default, "notification channel creation") {
            notificationHelper.get().createChannels()
        }
        launchSafely(dispatchers.default, "screen state + scheduling") {
            screenStateRepository.get().initialize()
            monitorScheduler.get().ensureScheduled()
        }
        // Update widgets when pro status changes
        launchSafely(dispatchers.default, "widget updates") {
            proManager.get().isProUser.distinctUntilChanged().collect {
                RuncheckWidgets.updateAll(this@RuncheckApp)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        billingManager.get().destroy()
        if (::applicationScope.isInitialized) {
            applicationScope.cancel()
        }
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    @Suppress("TooGenericExceptionCaught")
    private fun launchSafely(
        dispatcher: kotlinx.coroutines.CoroutineDispatcher,
        taskName: String,
        block: suspend () -> Unit,
    ) {
        applicationScope.launch(dispatcher) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ReleaseSafeLog.error(TAG, "Application coroutine failed: $taskName", e)
            }
        }
    }

    private fun configureDebugStrictMode() {
        if (!BuildConfig.DEBUG) return

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy
                .Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy
                .Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )
    }

    private companion object {
        private const val TAG = "RuncheckApp"
    }
}
