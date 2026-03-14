package com.runcheck.service.monitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.runcheck.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized helper for creating notification channels and posting
 * device-alert notifications (low battery, high temp, low storage,
 * charge complete).
 */
@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ALERTS = "device_pulse_alerts"
        const val CHANNEL_STATUS = "device_pulse_status"
        const val NOTIFICATION_LOW_BATTERY = 1001
        const val NOTIFICATION_HIGH_TEMP = 1002
        const val NOTIFICATION_LOW_STORAGE = 1003
        const val NOTIFICATION_CHARGE_COMPLETE = 1004
    }

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Creates both alert and status notification channels.
     * Safe to call multiple times; the system ignores duplicates.
     */
    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.notification_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_alerts_description)
                enableVibration(true)
            }

            val statusChannel = NotificationChannel(
                CHANNEL_STATUS,
                context.getString(R.string.notification_channel_status),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_status_description)
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(listOf(alertChannel, statusChannel))
        }
    }

    /** Posts a notification when battery drops below the user-defined threshold. */
    fun showLowBatteryAlert(level: Int) {
        if (!canPostNotifications()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.notification_low_battery_title))
            .setContentText(context.getString(R.string.notification_low_battery_text, level))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_LOW_BATTERY, notification)
    }

    /** Posts a notification when device temperature exceeds 42 degrees C. */
    fun showHighTempAlert(tempC: Float) {
        if (!canPostNotifications()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.notification_high_temp_title))
            .setContentText(context.getString(R.string.notification_high_temp_text, tempC))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_HIGH_TEMP, notification)
    }

    /** Posts a notification when storage usage exceeds 90%. */
    fun showLowStorageAlert(percentUsed: Float) {
        if (!canPostNotifications()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.notification_low_storage_title))
            .setContentText(context.getString(R.string.notification_low_storage_text, percentUsed))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_LOW_STORAGE, notification)
    }

    /** Posts a notification when charging completes, with an optional summary. */
    fun showChargeCompleteNotification(summary: String) {
        if (!canPostNotifications()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notification_charge_complete_title))
            .setContentText(summary)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_CHARGE_COMPLETE, notification)
    }

    /** Cancels a notification by its ID. */
    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }
}
