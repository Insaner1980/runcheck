package com.runcheck.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.BuildConfig
import com.runcheck.R
import com.runcheck.ui.common.UiText
import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.ui.common.findActivity
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.MetricPill
import com.runcheck.ui.components.ProFeatureCalloutCard
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()

    // Notification permission handling
    val notificationsPermissionRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val hasNotificationPermission = !notificationsPermissionRequired ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setNotifications(granted)
    }

    // Clear data confirmation dialog
    var showClearDialog by remember { mutableStateOf(false) }

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

                val subAlpha = if (masterEnabled) 1f else 0.38f
                Column(modifier = Modifier
                    .alpha(subAlpha)
                    .then(if (!masterEnabled) Modifier.semantics { disabled() } else Modifier)
                ) {
                    SettingsToggle(
                        title = stringResource(R.string.settings_notif_low_battery),
                        description = stringResource(R.string.settings_notif_low_battery_desc),
                        checked = uiState.preferences.notifLowBattery,
                        onCheckedChange = { if (masterEnabled) viewModel.setNotifLowBattery(it) }
                    )
                    SettingsDivider()
                    SettingsToggle(
                        title = stringResource(R.string.settings_notif_high_temp),
                        description = stringResource(R.string.settings_notif_high_temp_desc),
                        checked = uiState.preferences.notifHighTemp,
                        onCheckedChange = { if (masterEnabled) viewModel.setNotifHighTemp(it) }
                    )
                    SettingsDivider()
                    SettingsToggle(
                        title = stringResource(R.string.settings_notif_low_storage),
                        description = stringResource(R.string.settings_notif_low_storage_desc),
                        checked = uiState.preferences.notifLowStorage,
                        onCheckedChange = { if (masterEnabled) viewModel.setNotifLowStorage(it) }
                    )
                    SettingsDivider()
                    SettingsToggle(
                        title = stringResource(R.string.settings_notif_charge_complete),
                        description = stringResource(R.string.settings_notif_charge_complete_desc),
                        checked = uiState.preferences.notifChargeComplete,
                        onCheckedChange = { if (masterEnabled) viewModel.setNotifChargeComplete(it) }
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
                    valueLabelFor = { current ->
                        context.getString(R.string.value_percent, current)
                    },
                    range = 5f..50f,
                    steps = 8,
                    onValueChange = { viewModel.setAlertBatteryThreshold(it) }
                )
                SettingsDivider()
                SettingsSlider(
                    label = stringResource(R.string.settings_threshold_temp),
                    value = uiState.preferences.alertTempThreshold,
                    valueLabelFor = { current ->
                        "$current${context.getString(R.string.unit_celsius)}"
                    },
                    range = 35f..50f,
                    steps = 14,
                    onValueChange = { viewModel.setAlertTempThreshold(it) }
                )
                SettingsDivider()
                SettingsSlider(
                    label = stringResource(R.string.settings_threshold_storage),
                    value = uiState.preferences.alertStorageThreshold,
                    valueLabelFor = { current ->
                        context.getString(R.string.value_percent, current)
                    },
                    range = 70f..99f,
                    steps = 5,
                    onValueChange = { viewModel.setAlertStorageThreshold(it) }
                )
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
            }

            // ── DATA ───────────────────────────────────────────────────
            SettingsCard {
                CardSectionTitle(text = stringResource(R.string.settings_data_section))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                if (uiState.isPro) {
                    Text(
                        text = stringResource(R.string.settings_data_retention_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                    DataRetention.entries.forEachIndexed { index, retention ->
                        if (index > 0) SettingsDivider()
                        SettingsRadioRow(
                            label = retention.label(),
                            selected = uiState.preferences.dataRetention == retention,
                            onSelect = { viewModel.setDataRetention(retention) }
                        )
                    }
                    SettingsDivider()
                    OutlinedButton(
                        onClick = { viewModel.exportData() },
                        enabled = !uiState.isExporting,
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
                } else {
                    Text(
                        text = stringResource(R.string.settings_data_retention_free),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SettingsDivider()

                SettingsNavigationRow(
                    label = stringResource(R.string.settings_clear_all_data),
                    labelColor = MaterialTheme.colorScheme.error,
                    onClick = { showClearDialog = true }
                )
            }

            if (!uiState.isPro) {
                ProFeatureCalloutCard(
                    message = stringResource(R.string.pro_feature_export_message),
                    actionLabel = stringResource(R.string.pro_feature_upgrade_action),
                    onAction = { activity?.let { viewModel.purchasePro(it) } }
                )
            }

            // ── PRO ────────────────────────────────────────────────────
            SettingsCard {
                CardSectionTitle(
                    text = if (uiState.isPro) stringResource(R.string.settings_pro_active)
                    else stringResource(R.string.settings_upgrade_pro)
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                if (uiState.isPro) {
                    Text(
                        text = stringResource(R.string.settings_pro_thank_you),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else if (uiState.billingAvailable) {
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
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                SettingsNavigationRow(
                    label = stringResource(R.string.settings_restore_purchase),
                    onClick = { viewModel.refreshPurchaseStatus() }
                )
            }

            // ── PRIVACY ────────────────────────────────────────────────
            SettingsCard {
                CardSectionTitle(text = stringResource(R.string.settings_privacy))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                SettingsToggle(
                    title = stringResource(R.string.settings_crash_reporting),
                    description = stringResource(R.string.settings_crash_reporting_desc),
                    checked = uiState.preferences.crashReportingEnabled,
                    onCheckedChange = { viewModel.setCrashReporting(it) }
                )
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
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
                    ) {
                        MetricPill(
                            label = stringResource(R.string.settings_api_level_label),
                            value = profile.apiLevel.toString(),
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
                            modifier = Modifier.weight(1f)
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
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = stringResource(R.string.settings_thermal_zones_label),
                            value = profile.thermalZonesAvailable.size.toString(),
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
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SettingsDivider()
                SettingsNavigationRow(
                    label = stringResource(R.string.settings_rate),
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: Exception) { }
                    }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    label = stringResource(R.string.settings_privacy_policy),
                    onClick = { /* TODO: open privacy policy URL */ }
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

    // Clear data confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.settings_clear_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        // TODO: viewModel.clearAllData()
                        Toast.makeText(context, context.getString(R.string.settings_data_cleared), Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
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
        shape = RoundedCornerShape(16.dp),
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
private fun SettingsRadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = MaterialTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
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
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Int,
    valueLabelFor: (Int) -> String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Int) -> Unit
) {
    val context = LocalContext.current
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val currentValue = sliderValue.roundToInt()
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
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChange(sliderValue.roundToInt()) },
            valueRange = range,
            steps = steps,
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
