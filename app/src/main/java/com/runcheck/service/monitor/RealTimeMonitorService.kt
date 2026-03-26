package com.runcheck.service.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.runcheck.R
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import com.runcheck.ui.common.currentLocale
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.common.formatTemperature
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@AndroidEntryPoint
class RealTimeMonitorService : Service() {
    private val binder = Binder()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeBindings = AtomicInteger(0)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var updateJob: Job? = null
    private var prefCheckJob: Job? = null
    private val idleStopRunnable = Runnable {
        if (activeBindings.get() == 0 && !isLiveNotificationMode) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
    @Volatile
    private var isLiveNotificationMode = false

    @Inject lateinit var batteryRepository: BatteryRepository
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                createInitialNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, createInitialNotification())
        }
        checkLiveModeAndStart()
    }

    override fun onBind(intent: Intent?): IBinder {
        activeBindings.incrementAndGet()
        cancelIdleStop()
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        activeBindings.updateAndGet { (it - 1).coerceAtLeast(0) }
        if (!isLiveNotificationMode) scheduleIdleStop()
        return true
    }

    override fun onRebind(intent: Intent?) {
        activeBindings.incrementAndGet()
        cancelIdleStop()
        super.onRebind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                isLiveNotificationMode = false
                updateJob?.cancel()
                prefCheckJob?.cancel()
                serviceScope.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                // Don't restart after explicit stop — prevents wasteful restart cycle
                return START_NOT_STICKY
            }
            else -> checkLiveModeAndStart()
        }
        // START_STICKY so the system restarts the service after process death
        // when live notification is active. If not in live mode, the service
        // self-terminates via scheduleIdleStop() within 30 seconds.
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!isLiveNotificationMode) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        isRunning = false
        cancelIdleStop()
        updateJob?.cancel()
        prefCheckJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun checkLiveModeAndStart() {
        prefCheckJob?.cancel()
        prefCheckJob = serviceScope.launch {
            try {
                val prefs = userPreferencesRepository.getPreferences().first()
                isLiveNotificationMode = prefs.liveNotificationEnabled
                if (isLiveNotificationMode) {
                    startLiveUpdates()
                } else if (activeBindings.get() == 0) {
                    scheduleIdleStop()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ReleaseSafeLog.error(TAG, "Failed to load preferences in live mode check", e)
            }
        }
    }

    private fun startLiveUpdates() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                try {
                    val prefs = userPreferencesRepository.getPreferences().first()
                    if (!prefs.liveNotificationEnabled) {
                        isLiveNotificationMode = false
                        mainHandler.post {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                        return@launch
                    }
                    val battery = batteryRepository.getBatteryState().first()
                    val notification = buildLiveNotification(battery, prefs)
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIFICATION_ID, notification)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ReleaseSafeLog.warn(TAG, "Live notification update failed", e)
                }
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun buildLiveNotification(
        battery: BatteryState,
        prefs: UserPreferences
    ): Notification {
        val titleParts = mutableListOf<String>()
        val bodyLines = mutableListOf<String>()

        // Title: level + status
        val statusLabel = when (battery.chargingStatus) {
            ChargingStatus.CHARGING -> getString(R.string.charging_status_charging)
            ChargingStatus.FULL -> getString(R.string.charging_status_full)
            ChargingStatus.DISCHARGING -> getString(R.string.charging_status_discharging)
            ChargingStatus.NOT_CHARGING -> getString(R.string.charging_status_not_charging)
        }
        titleParts.add("${battery.level}%")
        titleParts.add(statusLabel)

        if (prefs.liveNotifTemperature) {
            titleParts.add(formatTemperature(this, battery.temperatureC, prefs.temperatureUnit))
        }

        // Body lines
        if (prefs.liveNotifCurrent) {
            val currentMa = battery.currentMa.value
            val voltageMv = battery.voltageMv
            val powerW = currentMa.let { ma ->
                val watts = kotlin.math.abs(ma) * voltageMv / 1_000_000f
                if (watts > 0.01f) watts else null
            }
            val currentLine = if (powerW != null) {
                getString(
                    R.string.live_notif_current_with_power,
                    currentMa,
                    formatDecimal(powerW, 1, currentLocale(this))
                )
            } else {
                getString(R.string.live_notif_current, currentMa)
            }
            bodyLines.add(currentLine)
        }

        if (prefs.liveNotifDrainRate) {
            // Drain rate from voltage trend — simplified display
            val drainLabel = when (battery.chargingStatus) {
                ChargingStatus.CHARGING -> getString(R.string.live_notif_charging)
                else -> getString(R.string.live_notif_discharging)
            }
            bodyLines.add(drainLabel)
        }

        if (prefs.liveNotifScreenStats) {
            bodyLines.add(getString(R.string.live_notif_screen_on))
        }

        if (prefs.liveNotifRemainingTime && battery.chargingStatus == ChargingStatus.DISCHARGING) {
            bodyLines.add(getString(R.string.live_notif_remaining))
        }

        val contentIntent = NotificationHelper.createContentIntent(
            context = this,
            route = null,
            requestCode = NOTIFICATION_ID
        )

        val title = titleParts.joinToString(" · ")
        val body = bodyLines.joinToString("\n").ifEmpty { getString(R.string.monitor_realtime_notification_text) }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(bodyLines.firstOrNull() ?: getString(R.string.monitor_realtime_notification_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentIntent)
            .setShowWhen(false)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.monitor_realtime_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.monitor_realtime_channel_description)
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createInitialNotification(): Notification {
        val contentIntent = NotificationHelper.createContentIntent(
            context = this,
            route = null,
            requestCode = NOTIFICATION_ID
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.monitor_realtime_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun scheduleIdleStop() {
        cancelIdleStop()
        if (activeBindings.get() == 0) {
            mainHandler.postDelayed(idleStopRunnable, IDLE_STOP_DELAY_MS)
        }
    }

    private fun cancelIdleStop() {
        mainHandler.removeCallbacks(idleStopRunnable)
    }

    companion object {
        const val CHANNEL_ID = "real_time_monitor"
        const val NOTIFICATION_ID = 2001
        const val ACTION_STOP = "com.runcheck.STOP_MONITORING"
        private const val TAG = "RealTimeMonitorService"
        private const val IDLE_STOP_DELAY_MS = 30_000L
        private const val UPDATE_INTERVAL_MS = 5_000L

        /** Tracks whether the service is alive — used by HealthMonitorWorker
         *  instead of the deprecated ActivityManager.getRunningServices(). */
        @Volatile
        var isRunning = false
            private set
    }
}
