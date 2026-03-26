package com.runcheck.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.BuildConfig
import com.runcheck.R
import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.service.monitor.NotificationHelper
import com.runcheck.service.monitor.RealTimeMonitorService
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.ui.common.findActivity
import com.runcheck.ui.learn.LearnArticleIds
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.common.formatTemperature
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.MetricPill
import com.runcheck.ui.components.info.InfoBottomSheet
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToLearnArticle: (String) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val isXiaomiFamilyDevice = remember {
        when (Build.MANUFACTURER.lowercase()) {
            "xiaomi", "redmi", "poco" -> true
            else -> false
        }
    }

    // Notification permission handling — re-checked every time the screen resumes
    // so that revoking permission in system settings is reflected immediately.
    val notificationsPermissionRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var hasNotificationPermission by remember {
        mutableStateOf(
            !notificationsPermissionRequired ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var alertsEffectivelyEnabled by remember { mutableStateOf(true) }
    var isBatteryOptimizationExempt by remember {
        val pm = context.getSystemService(PowerManager::class.java)
        mutableStateOf(pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true)
    }
    // Track whether the permission request was triggered by the live-notification toggle
    var permissionRequestedForLive by remember { mutableStateOf(false) }
    var showNotifPermissionDeniedDialog by rememberSaveable { mutableStateOf(false) }
    var activeInfoSheet by rememberSaveable { mutableStateOf<String?>(null) }

    LifecycleResumeEffect(Unit) {
        hasNotificationPermission = !notificationsPermissionRequired ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        val nm = context.getSystemService(android.app.NotificationManager::class.java)
        alertsEffectivelyEnabled = nm.areNotificationsEnabled() &&
            (nm.getNotificationChannel(NotificationHelper.CHANNEL_ALERTS)?.importance
                != android.app.NotificationManager.IMPORTANCE_NONE)
        val pm = context.getSystemService(PowerManager::class.java)
        isBatteryOptimizationExempt = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        onPauseOrDispose { }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            if (permissionRequestedForLive) {
                viewModel.setLiveNotificationEnabled(true)
                val serviceIntent = Intent(context, RealTimeMonitorService::class.java)
                context.startForegroundService(serviceIntent)
            } else {
                viewModel.setNotifications(true)
            }
        } else {
            // Detect permanent denial: shouldShowRequestPermissionRationale returns
            // false after the user selected "Don't ask again".
            val permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            if (permanentlyDenied) {
                showNotifPermissionDeniedDialog = true
            }
        }
        permissionRequestedForLive = false
    }

    // Confirmation dialog states
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    var showClearSpeedTestsDialog by rememberSaveable { mutableStateOf(false) }
    var showResetTipsDialog by rememberSaveable { mutableStateOf(false) }
    var showResetThresholdsDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(title = stringResource(R.string.settings_title), onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            // ── MONITORING ─────────────────────────────────────────────
            SettingsCard {
                CardSectionTitle(text = stringResource(R.string.settings_monitoring_interval))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.settings_monitoring_interval_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                MonitoringInterval.entries.forEachIndexed { index, interval ->
                    if (index > 0) SettingsDivider()
                    SettingsRadioRow(
                        label = when (interval) {
                            MonitoringInterval.FIFTEEN -> stringResource(R.string.settings_interval_15)
                            MonitoringInterval.THIRTY -> stringResource(R.string.settings_interval_30)
                            MonitoringInterval.SIXTY -> stringResource(R.string.settings_interval_60)
                        },
                        selected = uiState.preferences.monitoringInterval == interval,
                        onSelect = { viewModel.setMonitoringInterval(interval) }
                    )
                }

                SettingsDivider()
                CardSectionTitle(text = stringResource(R.string.settings_battery_optimization))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                if (isBatteryOptimizationExempt) {
                    Text(
                        text = stringResource(R.string.settings_battery_optimization_exempt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.statusColors.healthy
                    )
                    Text(
                        text = stringResource(R.string.settings_battery_optimization_exempt_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                    } catch (_: Exception) { }
                                }
                            }
                            .padding(vertical = MaterialTheme.spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_battery_optimization_restricted),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(R.string.settings_battery_optimization_restricted_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SettingsDivider()
                SettingsNavigationRow(
                    label = stringResource(R.string.settings_monitoring_help),
                    onClick = { onNavigateToLearnArticle(LearnArticleIds.BACKGROUND_MONITORING) }
                )
            }

            // ── LIVE NOTIFICATION ─────────────────────────────────────
            SettingsCard {
                CardSectionTitle(text = stringResource(R.string.settings_live_notification))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                val liveEnabled = uiState.preferences.liveNotificationEnabled

                SettingsToggle(
                    title = stringResource(R.string.settings_live_notification),
                    description = stringResource(R.string.settings_live_notification_desc),
                    checked = liveEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasNotificationPermission && notificationsPermissionRequired) {
                            permissionRequestedForLive = true
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setLiveNotificationEnabled(enabled)
                            val serviceIntent = Intent(context, RealTimeMonitorService::class.java)
                            if (enabled) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                serviceIntent.action = RealTimeMonitorService.ACTION_STOP
                                context.startService(serviceIntent)
                            }
                        }
                    }
                )

                if (liveEnabled) {
                    SettingsDivider()
                    Text(
                        text = stringResource(R.string.settings_live_notification_show),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = MaterialTheme.spacing.xs)
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    SettingsToggle(
                        title = stringResource(R.string.settings_live_notif_current),
                        description = null,
                        checked = uiState.preferences.liveNotifCurrent,
                        onCheckedChange = { viewModel.setLiveNotifCurrent(it) }
                    )
                    SettingsToggle(
                        title = stringResource(R.string.settings_live_notif_drain_rate),
                        description = null,
                        checked = uiState.preferences.liveNotifDrainRate,
                        onCheckedChange = { viewModel.setLiveNotifDrainRate(it) }
                    )
                    SettingsToggle(
                        title = stringResource(R.string.settings_live_notif_temperature),
                        description = null,
                        checked = uiState.preferences.liveNotifTemperature,
                        onCheckedChange = { viewModel.setLiveNotifTemperature(it) }
                    )
                    SettingsToggle(
                        title = stringResource(R.string.settings_live_notif_screen_stats),
                        description = null,
                        checked = uiState.preferences.liveNotifScreenStats,
                        onCheckedChange = { viewModel.setLiveNotifScreenStats(it) }
                    )
                    SettingsToggle(
                        title = stringResource(R.string.settings_live_notif_remaining_time),
                        description = null,
                        checked = uiState.preferences.liveNotifRemainingTime,
                        onCheckedChange = { viewModel.setLiveNotifRemainingTime(it) }
                    )
                }
            }

            // ── NOTIFICATIONS ──────────────────────────────────────────
            SettingsCard {
                CardSectionTitle(text = stringResource(R.string.settings_notifications))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                val masterEnabled = uiState.preferences.notificationsEnabled

                SettingsToggle(
                    title = stringResource(R.string.settings_notifications),
                    description = stringResource(R.string.settings_notifications_desc),
                    checked = masterEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasNotificationPermission && notificationsPermissionRequired) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setNotifications(enabled)
                        }
                    }
                )
                SettingsDivider()

                Column {
                    SettingsToggle(
                        title = stringResource(R.string.settings_notif_low_battery),
                        description = stringResource(R.string.settings_notif_low_battery_desc),
                        checked = uiState.preferences.notifLowBattery,
                        enabled = masterEnabled,
                        onCheckedChange = { if (masterEnabled) viewModel.setNotifLowBattery(it) }
                    )
                    SettingsDivider()
                    SettingsToggle(
                        title = stringResource(R.string.settings_notif_high_temp),
                        description = stringResource(R.string.settings_notif_high_temp_desc),
                        checked = uiState.preferences.notifHighTemp,
                        enabled = masterEnabled,
                        onCheckedChange = { if (masterEnabled) viewModel.setNotifHighTemp(it) }
                    )
                    SettingsDivider()
                    SettingsToggle(
                        title = stringResource(R.string.settings_notif_low_storage),
                        description = stringResource(R.string.settings_notif_low_storage_desc),
                        checked = uiState.preferences.notifLowStorage,
                        enabled = masterEnabled,
                        onCheckedChange = { if (masterEnabled) viewModel.setNotifLowStorage(it) }
                    )
                    SettingsDivider()
                    SettingsToggle(
                        title = stringResource(R.string.settings_notif_charge_complete),
                        description = stringResource(R.string.settings_notif_charge_complete_desc),
                        checked = uiState.preferences.notifChargeComplete,
                        enabled = masterEnabled,
                        onCheckedChange = { if (masterEnabled) viewModel.setNotifChargeComplete(it) }
                    )
                }

                // Warning when notifications are enabled in-app but muted at the system level
                if (uiState.preferences.notificationsEnabled && !alertsEffectivelyEnabled) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                    Text(
                        text = stringResource(
                            if (isXiaomiFamilyDevice) {
                                R.string.settings_notifications_system_muted_xiaomi
                            } else {
                                R.string.settings_notifications_system_muted
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(horizontal = MaterialTheme.spacing.xs)
                            .clickable {
                                val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            }
                    )
                }
            }

            // ── ALERT THRESHOLDS ───────────────────────────────────────
            SettingsCard {
                CardSectionTitle(text = stringResource(R.string.settings_alert_thresholds))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                SettingsSlider(
                    label = stringResource(R.string.settings_threshold_battery),
                    value = uiState.preferences.alertBatteryThreshold,
                    allowedValues = LOW_BATTERY_THRESHOLD_VALUES,
                    valueLabelFor = { current ->
                        context.getString(R.string.value_percent, current)
                    },
                    onValueChange = { viewModel.setAlertBatteryThreshold(it) }
                )
                SettingsDivider()
                SettingsSlider(
                    label = stringResource(R.string.settings_threshold_temp),
                    value = uiState.preferences.alertTempThreshold,
                    allowedValues = TEMPERATURE_THRESHOLD_VALUES,
                    valueLabelFor = { current ->
                        formatTemperature(
                            context = context,
                            valueCelsius = current,
                            unit = uiState.preferences.temperatureUnit,
                            fractionDigits = 0
                        )
                    },
                    onValueChange = { viewModel.setAlertTempThreshold(it) }
                )
                SettingsDivider()
                SettingsSlider(
                    label = stringResource(R.string.settings_threshold_storage),
                    value = uiState.preferences.alertStorageThreshold,
                    allowedValues = LOW_STORAGE_THRESHOLD_VALUES,
                    valueLabelFor = { current ->
                        context.getString(R.string.value_percent, current)
                    },
                    onValueChange = { viewModel.setAlertStorageThreshold(it) }
                )

                val isDefault = uiState.preferences.alertBatteryThreshold == 20 &&
                    uiState.preferences.alertTempThreshold == 42 &&
                    uiState.preferences.alertStorageThreshold == 90
                if (!isDefault) {
                    SettingsDivider()
                    TextButton(
                        onClick = { showResetThresholdsDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_reset_thresholds))
                    }
                }
            }

            // ── DISPLAY ────────────────────────────────────────────────
            SettingsCard {
                CardSectionTitle(text = stringResource(R.string.settings_display))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                Text(
                    text = stringResource(R.string.settings_temp_unit),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                SettingsRadioRow(
                    label = stringResource(R.string.settings_temp_celsius),
                    selected = uiState.preferences.temperatureUnit == TemperatureUnit.CELSIUS,
                    onSelect = { viewModel.setTemperatureUnit(TemperatureUnit.CELSIUS) }
                )
                SettingsRadioRow(
                    label = stringResource(R.string.settings_temp_fahrenheit),
                    selected = uiState.preferences.temperatureUnit == TemperatureUnit.FAHRENHEIT,
                    onSelect = { viewModel.setTemperatureUnit(TemperatureUnit.FAHRENHEIT) }
                )

                SettingsDivider()

                SettingsToggle(
                    title = stringResource(R.string.settings_show_info_cards),
                    description = stringResource(R.string.settings_show_info_cards_desc),
                    checked = uiState.preferences.showInfoCards,
                    onCheckedChange = { viewModel.setShowInfoCards(it) }
                )
            }

            // ── DATA ───────────────────────────────────────────────────
            SettingsCard {
                CardSectionTitle(text = stringResource(R.string.settings_data_section))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                Text(
                    text = if (uiState.isPro) {
                        stringResource(R.string.settings_data_retention_description)
                    } else {
                        stringResource(R.string.settings_data_retention_free)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                DataRetention.entries.forEachIndexed { index, retention ->
                    if (index > 0) SettingsDivider()
                    SettingsRadioRow(
                        label = retention.label(),
                        selected = uiState.preferences.dataRetention == retention,
                        enabled = uiState.isPro,
                        onSelect = { viewModel.setDataRetention(retention) }
                    )
                }
                SettingsDivider()
                OutlinedButton(
                    onClick = { viewModel.exportData() },
                    enabled = uiState.isPro && !uiState.isExporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(18.dp)
                                .semantics {
                                    contentDescription = context.getString(R.string.a11y_exporting_data)
                                },
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.settings_export_data))
                    }
                }

                SettingsDivider()

                SettingsNavigationRow(
                    label = stringResource(R.string.settings_reset_tips),
                    onClick = { showResetTipsDialog = true }
                )

                SettingsDivider()

                SettingsNavigationRow(
                    label = stringResource(R.string.settings_clear_speed_tests),
                    onClick = { showClearSpeedTestsDialog = true }
                )

                SettingsDivider()

                SettingsNavigationRow(
                    label = stringResource(R.string.settings_clear_all_data),
                    labelColor = MaterialTheme.colorScheme.error,
                    onClick = { showClearDialog = true }
                )
            }

            // ── PRO ────────────────────────────────────────────────────
            SettingsCard {
                CardSectionTitle(text = stringResource(R.string.settings_pro_section))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                SettingsValueRow(
                    label = stringResource(R.string.settings_status_label),
                    value = if (uiState.isPro) {
                        stringResource(R.string.settings_pro_status_active)
                    } else {
                        stringResource(R.string.settings_pro_status_not_active)
                    },
                    valueColor = if (uiState.isPro) {
                        MaterialTheme.statusColors.healthy
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (uiState.isPro) {
                    SettingsDivider()
                    Text(
                        text = stringResource(R.string.settings_pro_thank_you),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else if (uiState.billingAvailable) {
                    SettingsDivider()
                    Button(
                        onClick = { activity?.let { viewModel.purchasePro(it) } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            uiState.proPrice?.let { stringResource(R.string.settings_upgrade_pro_with_price, it) }
                                ?: stringResource(R.string.settings_upgrade_pro)
                        )
                    }
                }
                if (!uiState.isPro) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    SettingsNavigationRow(
                        label = stringResource(R.string.settings_restore_purchase),
                        onClick = { viewModel.refreshPurchaseStatus() }
                    )
                }
            }

            // ── DEVICE ─────────────────────────────────────────────────
            uiState.deviceProfile?.let { profile ->
                SettingsCard {
                    CardSectionTitle(text = stringResource(R.string.settings_measurement_info))
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    Text(
                        text = stringResource(R.string.settings_device_model, profile.manufacturer, profile.model),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    Text(
                        text = stringResource(
                            if (profile.currentNowReliable) {
                                R.string.settings_current_support_summary_reliable
                            } else {
                                R.string.settings_current_support_summary_unreliable
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
                    ) {
                        MetricPill(
                            label = stringResource(R.string.settings_api_level_label),
                            value = androidVersionName(profile.apiLevel),
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = stringResource(R.string.settings_current_reading_label),
                            value = stringResource(
                                if (profile.currentNowReliable) R.string.settings_measurement_reliable
                                else R.string.settings_measurement_unreliable
                            ),
                            valueColor = if (profile.currentNowReliable) MaterialTheme.statusColors.healthy
                                else MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                            onInfoClick = { activeInfoSheet = "currentReading" }
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
                    ) {
                        MetricPill(
                            label = stringResource(R.string.settings_cycle_count_label),
                            value = stringResource(
                                if (profile.cycleCountAvailable) R.string.settings_measurement_available
                                else R.string.settings_measurement_not_available
                            ),
                            modifier = Modifier.weight(1f),
                            onInfoClick = { activeInfoSheet = "cycleCount" }
                        )
                        MetricPill(
                            label = stringResource(R.string.settings_thermal_zones_label),
                            value = profile.thermalZonesAvailable.size.toString(),
                            modifier = Modifier.weight(1f),
                            onInfoClick = { activeInfoSheet = "thermalZones" }
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
                    ) {
                        val memoryInfo = remember {
                            val activityManager = context.getSystemService(android.app.ActivityManager::class.java)
                            android.app.ActivityManager.MemoryInfo().also { activityManager?.getMemoryInfo(it) }
                        }
                        MetricPill(
                            label = stringResource(R.string.settings_ram_label),
                            value = formatStorageSize(context, memoryInfo.totalMem),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── ABOUT ──────────────────────────────────────────────────
            SettingsCard {
                CardSectionTitle(text = stringResource(R.string.settings_about))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                Text(
                    text = stringResource(
                        R.string.settings_version,
                        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SettingsDivider()
                SettingsNavigationRow(
                    label = stringResource(R.string.settings_rate),
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            openExternalUri(
                                context = context,
                                uri = context.getString(R.string.settings_play_store_web_url)
                            )
                        }
                    }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    label = stringResource(R.string.settings_privacy_policy),
                    onClick = {
                        openExternalUri(
                            context = context,
                            uri = context.getString(R.string.settings_privacy_policy_url)
                        )
                    }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    label = stringResource(R.string.settings_feedback),
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_email_subject))
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (_: Exception) { }
                    }
                )
            }

            // ── Side effects ───────────────────────────────────────────
            uiState.billingStatus?.let { status ->
                LaunchedEffect(status) {
                    Toast.makeText(context, status.resolve(context), Toast.LENGTH_SHORT).show()
                    viewModel.clearBillingStatus()
                }
            }
            uiState.exportStatus?.let { status ->
                LaunchedEffect(status) {
                    Toast.makeText(context, status.resolve(context), Toast.LENGTH_SHORT).show()
                    viewModel.clearExportStatus()
                }
            }
            uiState.clearDataStatus?.let { status ->
                LaunchedEffect(status) {
                    Toast.makeText(context, status.resolve(context), Toast.LENGTH_SHORT).show()
                    viewModel.clearClearDataStatus()
                }
            }
            uiState.exportUris?.let { exportUriStrings ->
                LaunchedEffect(exportUriStrings) {
                    val parsedUris = exportUriStrings.map { Uri.parse(it) }
                    val shareIntent = if (parsedUris.size == 1) {
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, parsedUris.first())
                            clipData = android.content.ClipData.newRawUri(null, parsedUris.first())
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    } else {
                        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "text/csv"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(parsedUris))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.settings_export_share_title)))
                    viewModel.clearExportUris()
                }
            }
            uiState.errorMessage?.let { message ->
                LaunchedEffect(message) {
                    Toast.makeText(context, message.resolve(context), Toast.LENGTH_SHORT).show()
                    viewModel.clearErrorMessage()
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }

    // Reset thresholds confirmation dialog
    if (showResetThresholdsDialog) {
        AlertDialog(
            onDismissRequest = { showResetThresholdsDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.settings_reset_thresholds_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_thresholds_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetThresholdsDialog = false
                        viewModel.resetAlertThresholds()
                    }
                ) { Text(stringResource(R.string.settings_reset_thresholds)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetThresholdsDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Reset tips confirmation dialog
    if (showResetTipsDialog) {
        AlertDialog(
            onDismissRequest = { showResetTipsDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.settings_reset_tips_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_tips_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetTipsDialog = false
                        viewModel.resetTips()
                        Toast.makeText(context, context.getString(R.string.settings_reset_tips_done), Toast.LENGTH_SHORT).show()
                    }
                ) { Text(stringResource(R.string.settings_reset_tips)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetTipsDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Clear speed tests confirmation dialog
    if (showClearSpeedTestsDialog) {
        AlertDialog(
            onDismissRequest = { showClearSpeedTestsDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.settings_clear_speed_tests_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_speed_tests_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearSpeedTestsDialog = false
                        viewModel.clearSpeedTests()
                    }
                ) { Text(stringResource(R.string.settings_clear_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearSpeedTestsDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Notification permission permanently denied dialog
    if (showNotifPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showNotifPermissionDeniedDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.notification_permission_denied_title)) },
            text = { Text(stringResource(R.string.notification_permission_denied_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showNotifPermissionDeniedDialog = false
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        )
                    }
                ) { Text(stringResource(R.string.notification_permission_denied_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showNotifPermissionDeniedDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Measurement info bottom sheets
    activeInfoSheet?.let { key ->
        val content = when (key) {
            "currentReading" -> SettingsInfoContent.currentReading
            "cycleCount" -> SettingsInfoContent.cycleCount
            "thermalZones" -> SettingsInfoContent.thermalZones
            else -> null
        }
        content?.let {
            InfoBottomSheet(
                content = it,
                onDismiss = { activeInfoSheet = null }
            )
        }
    }

    // Clear all data confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.settings_clear_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearAllData()
                    }
                ) { Text(stringResource(R.string.settings_clear_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

// ── Reusable settings components ──────────────────────────────────────────────

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.base),
            content = content
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
}

@Composable
private fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onSelect,
                role = Role.RadioButton
            )
            .padding(vertical = MaterialTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = MaterialTheme.spacing.sm)
        )
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .toggleable(
                value = checked,
                enabled = enabled,
                onValueChange = onCheckedChange,
                role = Role.Switch
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (description != null) {
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Int,
    allowedValues: List<Int>,
    valueLabelFor: (Int) -> String,
    onValueChange: (Int) -> Unit
) {
    val context = LocalContext.current
    var sliderValue by remember(value, allowedValues) {
        mutableFloatStateOf(allowedValues.indexForValue(value).toFloat())
    }
    val currentIndex = sliderValue.roundToInt().coerceIn(0, allowedValues.lastIndex)
    val currentValue = allowedValues[currentIndex]
    val valueLabel = valueLabelFor(currentValue)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MaterialTheme.numericFontFamily),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it.coerceIn(0f, allowedValues.lastIndex.toFloat()) },
            onValueChangeFinished = {
                onValueChange(
                    allowedValues[sliderValue.roundToInt().coerceIn(0, allowedValues.lastIndex)]
                )
            },
            valueRange = 0f..allowedValues.lastIndex.toFloat(),
            steps = (allowedValues.size - 2).coerceAtLeast(0),
            modifier = Modifier.semantics {
                contentDescription = context.getString(R.string.value_label_colon, label, valueLabel)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun SettingsValueRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(vertical = MaterialTheme.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

@Composable
private fun SettingsNavigationRow(
    label: String,
    onClick: () -> Unit,
    labelColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = MaterialTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = labelColor)
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DataRetention.label(): String = when (this) {
    DataRetention.THREE_MONTHS -> stringResource(R.string.settings_retention_3_months)
    DataRetention.SIX_MONTHS -> stringResource(R.string.settings_retention_6_months)
    DataRetention.ONE_YEAR -> stringResource(R.string.settings_retention_1_year)
    DataRetention.FOREVER -> stringResource(R.string.settings_retention_forever)
}

private fun List<Int>.indexForValue(value: Int): Int =
    indexOf(value).takeIf { it >= 0 } ?: indices.minBy { index -> abs(this[index] - value) }

private fun openExternalUri(context: android.content.Context, uri: String) {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun androidVersionName(apiLevel: Int): String = when (apiLevel) {
    26 -> "Android 8.0"
    27 -> "Android 8.1"
    28 -> "Android 9"
    29 -> "Android 10"
    30 -> "Android 11"
    31 -> "Android 12"
    32 -> "Android 12L"
    33 -> "Android 13"
    34 -> "Android 14"
    35 -> "Android 15"
    36 -> "Android 16"
    37 -> "Android 17"
    else -> "API $apiLevel"
}

private val LOW_BATTERY_THRESHOLD_VALUES = (5..50 step 5).toList()
private val TEMPERATURE_THRESHOLD_VALUES = (35..50).toList()
private val LOW_STORAGE_THRESHOLD_VALUES = listOf(70, 75, 80, 85, 90, 95, 99)
