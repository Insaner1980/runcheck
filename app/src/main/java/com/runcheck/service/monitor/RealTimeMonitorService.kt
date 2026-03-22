package com.runcheck.service.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.runcheck.MainActivity
import com.runcheck.R
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import com.runcheck.ui.common.formatTemperature
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RealTimeMonitorService : Service() {
    private val binder = Binder()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeBindings = 0
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var updateJob: Job? = null
    private val idleStopRunnable = Runnable {
        if (activeBindings == 0 && !isLiveNotificationMode) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
    private var isLiveNotificationMode = false

    @Inject lateinit var batteryRepository: BatteryRepository
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate() {
        super.onCreate()
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
        activeBindings += 1
        cancelIdleStop()
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        activeBindings = (activeBindings - 1).coerceAtLeast(0)
        if (!isLiveNotificationMode) scheduleIdleStop()
        return true
    }

    override fun onRebind(intent: Intent?) {
        activeBindings += 1
        cancelIdleStop()
        super.onRebind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                isLiveNotificationMode = false
                updateJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> checkLiveModeAndStart()
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!isLiveNotificationMode) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        cancelIdleStop()
        updateJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun checkLiveModeAndStart() {
        serviceScope.launch {
            val prefs = try {
                userPreferencesRepository.getPreferences().first()
            } catch (_: Exception) {
                return@launch
            }
            isLiveNotificationMode = prefs.liveNotificationEnabled
            if (isLiveNotificationMode) {
                startLiveUpdates()
            } else if (activeBindings == 0) {
                scheduleIdleStop()
            }
        }
    }

    private fun startLiveUpdates() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (true) {
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
                } catch (_: Exception) {
                    // Continue on failure
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
                getString(R.string.live_notif_current_with_power, currentMa, String.format("%.1f", powerW))
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

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
        if (activeBindings == 0) {
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
        private const val IDLE_STOP_DELAY_MS = 30_000L
        private const val UPDATE_INTERVAL_MS = 5_000L
    }
}
