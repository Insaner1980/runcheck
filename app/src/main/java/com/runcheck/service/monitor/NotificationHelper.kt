package com.runcheck.service.monitor

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.runcheck.MainActivity
import com.runcheck.R
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.ui.common.formatTemperature
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized helper for creating notification channels and posting
 * device-alert notifications (low battery, high temp, low storage,
 * charge complete).
 */
@Singleton
class NotificationHelper
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        companion object {
            const val CHANNEL_ALERTS = "runcheck_alerts"

            // Legacy and obsolete channel IDs removed on upgrade to keep OEM settings clean.
            private val LEGACY_CHANNEL_IDS =
                listOf(
                    "device_pulse",
                    "device_pulse_alerts",
                    "device_pulse_status",
                    "device_pulse_trial",
                    "runcheck_status",
                )
            const val CHANNEL_TRIAL = "runcheck_trial"
            const val NOTIFICATION_LOW_BATTERY = 1001
            const val NOTIFICATION_HIGH_TEMP = 1002
            const val NOTIFICATION_LOW_STORAGE = 1003
            const val NOTIFICATION_CHARGE_COMPLETE = 1004
            const val NOTIFICATION_TRIAL_DAY5 = 1005
            const val NOTIFICATION_TRIAL_DAY7 = 1006

            /** Intent extra key for deep-linking to a specific screen from notifications. */
            const val EXTRA_NAVIGATE_TO = "navigate_to"
            const val NAVIGATE_BATTERY = "battery"
            const val NAVIGATE_THERMAL = "thermal"
            const val NAVIGATE_STORAGE = "storage"
            const val NAVIGATE_PRO_UPGRADE = "pro_upgrade"

            fun createContentIntent(
                context: Context,
                route: String?,
                requestCode: Int,
            ): PendingIntent {
                val intent =
                    Intent().apply {
                        component = ComponentName(context, MainActivity::class.java)
                        setPackage(context.packageName)
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        if (!route.isNullOrBlank()) {
                            putExtra(EXTRA_NAVIGATE_TO, route)
                        }
                    }
                return PendingIntent.getActivity(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
        }

        private val notificationManager: NotificationManager
            get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        /**
         * Creates the app's alert/reminder channels.
         * Safe to call multiple times; the system ignores duplicates.
         */
        fun createChannels() {
            val alertChannel =
                NotificationChannel(
                    CHANNEL_ALERTS,
                    context.getString(R.string.notification_channel_alerts),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.notification_channel_alerts_description)
                    enableVibration(true)
                }

            val trialChannel =
                NotificationChannel(
                    CHANNEL_TRIAL,
                    context.getString(R.string.notification_channel_trial),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.notification_channel_trial_description)
                }

            notificationManager.createNotificationChannels(
                listOf(alertChannel, trialChannel),
            )

            // Remove leftover channels from the old "DevicePulse" app name
            for (legacyId in LEGACY_CHANNEL_IDS) {
                notificationManager.deleteNotificationChannel(legacyId)
            }
        }

        /** Posts a notification when battery drops below the user-defined threshold. */
        fun showLowBatteryAlert(level: Int) {
            if (!canPostNotifications()) return
            createChannels()
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ALERTS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notification_low_battery_title))
                    .setContentText(context.getString(R.string.notification_low_battery_text, level))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(createContentIntent(context, NAVIGATE_BATTERY, NOTIFICATION_LOW_BATTERY))
                    .setAutoCancel(true)
                    .build()

            notificationManager.notify(NOTIFICATION_LOW_BATTERY, notification)
        }

        /** Posts a notification when device temperature exceeds the alert threshold. */
        fun showHighTempAlert(
            tempC: Float,
            temperatureUnit: TemperatureUnit,
        ) {
            if (!canPostNotifications()) return
            createChannels()
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ALERTS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notification_high_temp_title))
                    .setContentText(
                        context.getString(
                            R.string.notification_high_temp_text,
                            formatTemperature(context, tempC, temperatureUnit),
                        ),
                    ).setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(createContentIntent(context, NAVIGATE_THERMAL, NOTIFICATION_HIGH_TEMP))
                    .setAutoCancel(true)
                    .build()

            notificationManager.notify(NOTIFICATION_HIGH_TEMP, notification)
        }

        /** Posts a notification when storage usage exceeds 90%. */
        fun showLowStorageAlert(percentUsed: Float) {
            if (!canPostNotifications()) return
            createChannels()
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ALERTS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notification_low_storage_title))
                    .setContentText(context.getString(R.string.notification_low_storage_text, percentUsed))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(createContentIntent(context, NAVIGATE_STORAGE, NOTIFICATION_LOW_STORAGE))
                    .setAutoCancel(true)
                    .build()

            notificationManager.notify(NOTIFICATION_LOW_STORAGE, notification)
        }

        /** Posts a notification when charging completes. */
        fun showChargeCompleteNotification(level: Int) {
            if (!canPostNotifications()) return
            createChannels()
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ALERTS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notification_charge_complete_title))
                    .setContentText(context.getString(R.string.notification_charge_complete_text, level))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(createContentIntent(context, NAVIGATE_BATTERY, NOTIFICATION_CHARGE_COMPLETE))
                    .setAutoCancel(true)
                    .build()

            notificationManager.notify(NOTIFICATION_CHARGE_COMPLETE, notification)
        }

        fun showTrialDay5Notification() {
            if (!canPostNotifications()) return
            createChannels()
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_TRIAL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notification_trial_day5_title))
                    .setContentText(context.getString(R.string.notification_trial_day5_text))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(createContentIntent(context, NAVIGATE_PRO_UPGRADE, NOTIFICATION_TRIAL_DAY5))
                    .setAutoCancel(true)
                    .build()
            notificationManager.notify(NOTIFICATION_TRIAL_DAY5, notification)
        }

        fun showTrialDay7Notification() {
            if (!canPostNotifications()) return
            createChannels()
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_TRIAL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notification_trial_day7_title))
                    .setContentText(context.getString(R.string.notification_trial_day7_text))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(createContentIntent(context, NAVIGATE_PRO_UPGRADE, NOTIFICATION_TRIAL_DAY7))
                    .setAutoCancel(true)
                    .build()
            notificationManager.notify(NOTIFICATION_TRIAL_DAY7, notification)
        }

        /** Cancels a notification by its ID. */
        fun cancelNotification(id: Int) {
            notificationManager.cancel(id)
        }

        /**
         * Returns true when notifications can actually reach the user:
         * POST_NOTIFICATIONS permission granted (API 33+) AND app-level
         * notifications enabled AND the alerts channel is not disabled.
         */
        fun areAlertsEffectivelyEnabled(): Boolean {
            if (!canPostNotifications()) return false
            val nm = notificationManager
            if (!nm.areNotificationsEnabled()) return false
            val channel = nm.getNotificationChannel(CHANNEL_ALERTS)
            return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
        }

        private fun canPostNotifications(): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
    }
