package com.devicepulse.service.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.devicepulse.R

class RealTimeMonitorService : Service() {
    private val binder = Binder()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeBindings = 0
    private val idleStopRunnable = Runnable {
        if (activeBindings == 0) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        scheduleIdleStop()
    }

    override fun onBind(intent: Intent?): IBinder {
        activeBindings += 1
        cancelIdleStop()
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        activeBindings = (activeBindings - 1).coerceAtLeast(0)
        scheduleIdleStop()
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
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> scheduleIdleStop()
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        cancelIdleStop()
        super.onDestroy()
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

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.monitor_realtime_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setSilent(true)
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
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.devicepulse.STOP_MONITORING"
        private const val IDLE_STOP_DELAY_MS = 30_000L
    }
}
