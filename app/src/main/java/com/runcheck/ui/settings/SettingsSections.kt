package com.runcheck.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.UserPreferences
import com.runcheck.ui.common.formatTemperature
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors
import com.runcheck.ui.theme.uiTokens
import com.runcheck.util.ReleaseSafeLog

@Composable
internal fun MonitoringSection(
    context: android.content.Context,
    monitoringInterval: MonitoringInterval,
    isBatteryOptimizationExempt: Boolean,
    onSetMonitoringInterval: (MonitoringInterval) -> Unit,
    onNavigateToMonitoringHelp: () -> Unit,
) {
    SettingsCard {
        CardSectionTitle(text = stringResource(R.string.settings_monitoring_interval))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Text(
            text = stringResource(R.string.settings_monitoring_interval_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        MonitoringInterval.entries.forEachIndexed { index, interval ->
            if (index > 0) SettingsDivider()
            SettingsRadioRow(
                label = monitoringIntervalLabel(interval),
                selected = monitoringInterval == interval,
                onSelect = { onSetMonitoringInterval(interval) },
            )
        }

        SettingsDivider()
        CardSectionTitle(text = stringResource(R.string.settings_battery_optimization))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        if (isBatteryOptimizationExempt) {
            Text(
                text = stringResource(R.string.settings_battery_optimization_exempt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.statusColors.healthy,
            )
            Text(
                text = stringResource(R.string.settings_battery_optimization_exempt_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            BatteryOptimizationRow(context = context)
        }

        SettingsDivider()
        SettingsNavigationRow(
            label = stringResource(R.string.settings_monitoring_help),
            onClick = onNavigateToMonitoringHelp,
        )
    }
}

@Composable
internal fun LiveNotificationSection(
    preferences: UserPreferences,
    onSetLiveNotificationEnabled: (Boolean) -> Unit,
    onSetLiveNotifCurrent: (Boolean) -> Unit,
    onSetLiveNotifDrainRate: (Boolean) -> Unit,
    onSetLiveNotifTemperature: (Boolean) -> Unit,
    onSetLiveNotifScreenStats: (Boolean) -> Unit,
    onSetLiveNotifRemainingTime: (Boolean) -> Unit,
) {
    val liveNotificationToggles =
        listOf(
            LiveNotificationToggle(
                titleRes = R.string.settings_live_notif_current,
                checked = preferences.liveNotifCurrent,
                onCheckedChange = onSetLiveNotifCurrent,
            ),
            LiveNotificationToggle(
                titleRes = R.string.settings_live_notif_drain_rate,
                checked = preferences.liveNotifDrainRate,
                onCheckedChange = onSetLiveNotifDrainRate,
            ),
            LiveNotificationToggle(
                titleRes = R.string.settings_live_notif_temperature,
                checked = preferences.liveNotifTemperature,
                onCheckedChange = onSetLiveNotifTemperature,
            ),
            LiveNotificationToggle(
                titleRes = R.string.settings_live_notif_screen_stats,
                checked = preferences.liveNotifScreenStats,
                onCheckedChange = onSetLiveNotifScreenStats,
            ),
            LiveNotificationToggle(
                titleRes = R.string.settings_live_notif_remaining_time,
                checked = preferences.liveNotifRemainingTime,
                onCheckedChange = onSetLiveNotifRemainingTime,
            ),
        )

    SettingsCard {
        CardSectionTitle(text = stringResource(R.string.settings_live_notification))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

        SettingsToggle(
            title = stringResource(R.string.settings_live_notification),
            description = stringResource(R.string.settings_live_notification_desc),
            checked = preferences.liveNotificationEnabled,
            onCheckedChange = onSetLiveNotificationEnabled,
        )

        if (preferences.liveNotificationEnabled) {
            SettingsDivider()
            Text(
                text = stringResource(R.string.settings_live_notification_show),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            liveNotificationToggles.forEach { toggle ->
                SettingsToggle(
                    title = stringResource(toggle.titleRes),
                    description = null,
                    checked = toggle.checked,
                    onCheckedChange = toggle.onCheckedChange,
                )
            }
        }
    }
}

@Suppress("kotlin:S107")
@Composable
internal fun NotificationsSection( // NOSONAR
    context: android.content.Context,
    preferences: UserPreferences,
    alertsEffectivelyEnabled: Boolean,
    isXiaomiFamilyDevice: Boolean,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onSetNotifLowBattery: (Boolean) -> Unit,
    onSetNotifHighTemp: (Boolean) -> Unit,
    onSetNotifLowStorage: (Boolean) -> Unit,
    onSetNotifChargeComplete: (Boolean) -> Unit,
) {
    val masterEnabled = preferences.notificationsEnabled
    val notificationToggles =
        listOf(
            NotificationToggle(
                titleRes = R.string.settings_notif_low_battery,
                descriptionRes = R.string.settings_notif_low_battery_desc,
                checked = preferences.notifLowBattery,
                onCheckedChange = onSetNotifLowBattery,
            ),
            NotificationToggle(
                titleRes = R.string.settings_notif_high_temp,
                descriptionRes = R.string.settings_notif_high_temp_desc,
                checked = preferences.notifHighTemp,
                onCheckedChange = onSetNotifHighTemp,
            ),
            NotificationToggle(
                titleRes = R.string.settings_notif_low_storage,
                descriptionRes = R.string.settings_notif_low_storage_desc,
                checked = preferences.notifLowStorage,
                onCheckedChange = onSetNotifLowStorage,
            ),
            NotificationToggle(
                titleRes = R.string.settings_notif_charge_complete,
                descriptionRes = R.string.settings_notif_charge_complete_desc,
                checked = preferences.notifChargeComplete,
                onCheckedChange = onSetNotifChargeComplete,
            ),
        )

    SettingsCard {
        CardSectionTitle(text = stringResource(R.string.settings_notifications))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

        SettingsToggle(
            title = stringResource(R.string.settings_notifications),
            description = stringResource(R.string.settings_notifications_desc),
            checked = masterEnabled,
            onCheckedChange = onSetNotificationsEnabled,
        )
        SettingsDivider()

        notificationToggles.forEachIndexed { index, toggle ->
            SettingsToggle(
                title = stringResource(toggle.titleRes),
                description = stringResource(toggle.descriptionRes),
                checked = toggle.checked,
                enabled = masterEnabled,
                onCheckedChange = { checked ->
                    if (masterEnabled) {
                        toggle.onCheckedChange(checked)
                    }
                },
            )
            if (index < notificationToggles.lastIndex) {
                SettingsDivider()
            }
        }

        if (masterEnabled && !alertsEffectivelyEnabled) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            Text(
                text = mutedNotificationMessage(isXiaomiFamilyDevice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .padding(horizontal = MaterialTheme.spacing.xs)
                        .clickable { openSystemNotificationSettings(context) },
            )
        }
    }
}

@Composable
internal fun AlertThresholdsSection(
    context: android.content.Context,
    preferences: UserPreferences,
    onSetAlertBatteryThreshold: (Int) -> Unit,
    onSetAlertTempThreshold: (Int) -> Unit,
    onSetAlertStorageThreshold: (Int) -> Unit,
    onResetThresholdsClick: () -> Unit,
) {
    val isDefault =
        preferences.alertBatteryThreshold == DEFAULT_ALERT_BATTERY_THRESHOLD &&
            preferences.alertTempThreshold == DEFAULT_ALERT_TEMPERATURE_THRESHOLD &&
            preferences.alertStorageThreshold == DEFAULT_ALERT_STORAGE_THRESHOLD

    SettingsCard {
        CardSectionTitle(text = stringResource(R.string.settings_alert_thresholds))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

        SettingsSlider(
            label = stringResource(R.string.settings_threshold_battery),
            value = preferences.alertBatteryThreshold,
            allowedValues = LOW_BATTERY_THRESHOLD_VALUES,
            valueLabelFor = { current -> context.getString(R.string.settings_threshold_percent, current) },
            onValueChange = onSetAlertBatteryThreshold,
        )
        SettingsDivider()
        SettingsSlider(
            label = stringResource(R.string.settings_threshold_temp),
            value = preferences.alertTempThreshold,
            allowedValues = TEMPERATURE_THRESHOLD_VALUES,
            valueLabelFor = { current ->
                formatTemperature(
                    context = context,
                    valueCelsius = current,
                    unit = preferences.temperatureUnit,
                    fractionDigits = 0,
                )
            },
            onValueChange = onSetAlertTempThreshold,
        )
        SettingsDivider()
        SettingsSlider(
            label = stringResource(R.string.settings_threshold_storage),
            value = preferences.alertStorageThreshold,
            allowedValues = LOW_STORAGE_THRESHOLD_VALUES,
            valueLabelFor = { current -> context.getString(R.string.settings_threshold_percent, current) },
            onValueChange = onSetAlertStorageThreshold,
        )

        if (!isDefault) {
            SettingsDivider()
            TextButton(
                onClick = onResetThresholdsClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_reset_thresholds))
            }
        }
    }
}

@Composable
internal fun DisplaySection(
    preferences: UserPreferences,
    onSetTemperatureUnit: (TemperatureUnit) -> Unit,
    onSetShowInfoCards: (Boolean) -> Unit,
) {
    SettingsCard {
        CardSectionTitle(text = stringResource(R.string.settings_display))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

        Text(
            text = stringResource(R.string.settings_temp_unit),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        SettingsRadioRow(
            label = stringResource(R.string.settings_temp_celsius),
            selected = preferences.temperatureUnit == TemperatureUnit.CELSIUS,
            onSelect = { onSetTemperatureUnit(TemperatureUnit.CELSIUS) },
        )
        SettingsRadioRow(
            label = stringResource(R.string.settings_temp_fahrenheit),
            selected = preferences.temperatureUnit == TemperatureUnit.FAHRENHEIT,
            onSelect = { onSetTemperatureUnit(TemperatureUnit.FAHRENHEIT) },
        )

        SettingsDivider()

        SettingsToggle(
            title = stringResource(R.string.settings_show_info_cards),
            description = stringResource(R.string.settings_show_info_cards_desc),
            checked = preferences.showInfoCards,
            onCheckedChange = onSetShowInfoCards,
        )
    }
}

@Composable
internal fun DataSection(
    uiState: SettingsUiState,
    onSetDataRetention: (DataRetention) -> Unit,
    onExportData: () -> Unit,
    onResetTipsClick: () -> Unit,
    onClearSpeedTestsClick: () -> Unit,
    onClearAllDataClick: () -> Unit,
) {
    val tokens = MaterialTheme.uiTokens
    val exportingDescription = stringResource(R.string.a11y_exporting_data)

    SettingsCard {
        CardSectionTitle(text = stringResource(R.string.settings_data_section))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

        Text(
            text =
                if (uiState.isPro) {
                    stringResource(R.string.settings_data_retention_description)
                } else {
                    stringResource(R.string.settings_data_retention_free)
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        DataRetention.entries.forEachIndexed { index, retention ->
            if (index > 0) SettingsDivider()
            SettingsRadioRow(
                label = retention.label(),
                selected = uiState.preferences.dataRetention == retention,
                enabled = uiState.isPro,
                onSelect = { onSetDataRetention(retention) },
            )
        }
        SettingsDivider()
        OutlinedButton(
            onClick = onExportData,
            enabled = uiState.isPro && !uiState.isExporting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isExporting) {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .size(tokens.iconMedium)
                            .semantics {
                                contentDescription = exportingDescription
                            },
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(R.string.settings_export_data))
            }
        }

        SettingsDivider()
        SettingsNavigationRow(
            label = stringResource(R.string.settings_reset_tips),
            onClick = onResetTipsClick,
        )
        SettingsDivider()
        SettingsNavigationRow(
            label = stringResource(R.string.settings_clear_speed_tests),
            onClick = onClearSpeedTestsClick,
        )
        SettingsDivider()
        SettingsNavigationRow(
            label = stringResource(R.string.settings_clear_all_data),
            labelColor = MaterialTheme.colorScheme.error,
            onClick = onClearAllDataClick,
        )
    }
}

@Composable
internal fun DebugInsightsSection(
    uiState: SettingsUiState,
    onSeedDemoInsights: () -> Unit,
    onGenerateInsightsNow: () -> Unit,
    onClearInsights: () -> Unit,
) {
    val tokens = MaterialTheme.uiTokens
    if (!uiState.debugInsightsAvailable) return

    SettingsCard {
        CardSectionTitle(text = stringResource(R.string.settings_debug_insights_section))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Text(
            text = stringResource(R.string.settings_debug_insights_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (uiState.isProcessingDebugInsights) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(tokens.iconMedium), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.settings_debug_insights_running),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        OutlinedButton(
            onClick = onSeedDemoInsights,
            enabled = !uiState.isProcessingDebugInsights,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_debug_insights_seed_demo))
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        OutlinedButton(
            onClick = onGenerateInsightsNow,
            enabled = !uiState.isProcessingDebugInsights,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_debug_insights_run_now))
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        TextButton(
            onClick = onClearInsights,
            enabled = !uiState.isProcessingDebugInsights,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_debug_insights_clear))
        }
    }
}

@Composable
internal fun ProSection(
    uiState: SettingsUiState,
    onPurchasePro: () -> Unit,
    onRefreshPurchaseStatus: () -> Unit,
) {
    SettingsCard {
        CardSectionTitle(text = stringResource(R.string.settings_pro_section))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

        SettingsValueRow(
            label = stringResource(R.string.settings_status_label),
            value =
                if (uiState.isPro) {
                    stringResource(R.string.settings_pro_status_active)
                } else {
                    stringResource(R.string.settings_pro_status_not_active)
                },
            valueColor =
                if (uiState.isPro) {
                    MaterialTheme.statusColors.healthy
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )

        if (uiState.isPro) {
            SettingsDivider()
            Text(
                text = stringResource(R.string.settings_pro_thank_you),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else if (uiState.billingAvailable) {
            SettingsDivider()
            Button(
                onClick = onPurchasePro,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    uiState.proPrice?.let {
                        stringResource(R.string.settings_upgrade_pro_with_price, it)
                    } ?: stringResource(R.string.settings_upgrade_pro),
                )
            }
        }
        if (!uiState.isPro) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            SettingsNavigationRow(
                label = stringResource(R.string.settings_restore_purchase),
                onClick = onRefreshPurchaseStatus,
            )
        }
    }
}

@Composable
private fun monitoringIntervalLabel(interval: MonitoringInterval): String =
    when (interval) {
        MonitoringInterval.FIFTEEN -> stringResource(R.string.settings_interval_15)
        MonitoringInterval.THIRTY -> stringResource(R.string.settings_interval_30)
        MonitoringInterval.SIXTY -> stringResource(R.string.settings_interval_60)
    }

@Composable
private fun mutedNotificationMessage(isXiaomiFamilyDevice: Boolean): String =
    stringResource(
        if (isXiaomiFamilyDevice) {
            R.string.settings_notifications_system_muted_xiaomi
        } else {
            R.string.settings_notifications_system_muted
        },
    )

@Composable
private fun BatteryOptimizationRow(context: android.content.Context) {
    val tokens = MaterialTheme.uiTokens
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = tokens.touchTarget)
                .clickable { openBatteryOptimizationSettings(context) }
                .padding(vertical = MaterialTheme.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_battery_optimization_restricted),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = stringResource(R.string.settings_battery_optimization_restricted_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(tokens.iconMedium),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun openBatteryOptimizationSettings(context: android.content.Context) {
    val requestIntent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    try {
        context.startActivity(requestIntent)
    } catch (_: android.content.ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (exception: android.content.ActivityNotFoundException) {
            ReleaseSafeLog.warn(
                TAG,
                "Failed to open battery optimization settings",
                exception,
            )
        }
    }
}

private fun openSystemNotificationSettings(context: android.content.Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        },
    )
}

private data class LiveNotificationToggle(
    val titleRes: Int,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
)

private data class NotificationToggle(
    val titleRes: Int,
    val descriptionRes: Int,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
)
