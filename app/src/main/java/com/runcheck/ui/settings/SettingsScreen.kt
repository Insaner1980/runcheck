package com.runcheck.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.BuildConfig
import com.runcheck.R
import com.runcheck.domain.model.CurrentUnit
import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.DeviceProfileInfo
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.model.SignConvention
import com.runcheck.ui.common.findActivity
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.ProFeatureCalloutCard
import com.runcheck.ui.components.SectionHeader
import androidx.compose.material3.ButtonDefaults
import com.runcheck.ui.theme.BgIconCircle
import com.runcheck.ui.theme.BgPage
import com.runcheck.ui.theme.RuncheckTheme
import com.runcheck.ui.theme.spacing

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreenContent(
        uiState = uiState,
        onBack = onBack,
        onMonitoringIntervalChange = viewModel::setMonitoringInterval,
        onDataRetentionChange = viewModel::setDataRetention,
        onNotificationsChange = viewModel::setNotifications,
        onCrashReportingChange = viewModel::setCrashReporting,
        onPurchasePro = { activity -> viewModel.purchasePro(activity) },
        onRefreshPurchaseStatus = viewModel::refreshPurchaseStatus,
        onExportData = viewModel::exportData,
        onClearBillingStatus = viewModel::clearBillingStatus,
        onClearExportUris = viewModel::clearExportUris,
        onClearExportStatus = viewModel::clearExportStatus,
        onClearErrorMessage = viewModel::clearErrorMessage,
        modifier = modifier
    )
}

@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onMonitoringIntervalChange: (MonitoringInterval) -> Unit,
    onDataRetentionChange: (DataRetention) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onCrashReportingChange: (Boolean) -> Unit,
    onPurchasePro: (Activity) -> Unit,
    onRefreshPurchaseStatus: () -> Unit,
    onExportData: () -> Unit,
    onClearBillingStatus: () -> Unit,
    onClearExportUris: () -> Unit,
    onClearExportStatus: () -> Unit,
    onClearErrorMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val notificationsPermissionRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val hasNotificationPermission = !notificationsPermissionRequired ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    var notificationRequestAttempted by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationRequestAttempted = true
        if (granted) {
            onNotificationsChange(true)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        DetailTopBar(
            title = stringResource(R.string.settings_title),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.base)
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            SectionHeader(text = stringResource(R.string.settings_monitoring_interval))
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            MonitoringInterval.entries.forEach { interval ->
                val label = when (interval) {
                    MonitoringInterval.FIFTEEN -> stringResource(R.string.settings_interval_15)
                    MonitoringInterval.THIRTY -> stringResource(R.string.settings_interval_30)
                    MonitoringInterval.SIXTY -> stringResource(R.string.settings_interval_60)
                }
                SettingsRadioRow(
                    label = label,
                    selected = uiState.preferences.monitoringInterval == interval,
                    onSelect = { onMonitoringIntervalChange(interval) }
                )
            }

            HorizontalDivider(color = BgIconCircle, thickness = 1.dp)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            SectionHeader(text = stringResource(R.string.settings_notifications))
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            SettingsToggle(
                title = stringResource(R.string.settings_notifications),
                description = stringResource(R.string.settings_notifications_desc),
                checked = uiState.preferences.notificationsEnabled,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        onNotificationsChange(false)
                    } else if (!hasNotificationPermission && notificationsPermissionRequired) {
                        onNotificationsChange(true)
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onNotificationsChange(true)
                    }
                }
            )

            if (
                uiState.preferences.notificationsEnabled &&
                notificationsPermissionRequired &&
                !hasNotificationPermission
            ) {
                PermissionHelpCard(
                    title = stringResource(R.string.notification_permission_title),
                    message = stringResource(R.string.notification_permission_message),
                    actionLabel = if (
                        notificationRequestAttempted &&
                        activity?.let {
                            !ActivityCompat.shouldShowRequestPermissionRationale(
                                it,
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        } == true
                    ) {
                        stringResource(R.string.notification_permission_open_settings)
                    } else {
                        stringResource(R.string.notification_permission_grant)
                    },
                    onAction = {
                        if (
                            notificationRequestAttempted &&
                            activity?.let {
                                !ActivityCompat.shouldShowRequestPermissionRationale(
                                    it,
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            } == true
                        ) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        } else {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            }

            HorizontalDivider(color = BgIconCircle, thickness = 1.dp)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            SectionHeader(
                text = if (uiState.isPro) stringResource(R.string.settings_pro_active)
                else stringResource(R.string.settings_upgrade_pro)
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            if (uiState.isPro) {
                Text(
                    text = stringResource(R.string.settings_pro_thank_you),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                OutlinedButton(
                    onClick = onRefreshPurchaseStatus,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.settings_refresh_pro_status))
                }
            } else {
                Text(
                    text = if (uiState.billingAvailable) {
                        stringResource(R.string.settings_pro_desc)
                    } else {
                        stringResource(R.string.settings_billing_unavailable)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                if (uiState.billingAvailable) {
                    Button(
                        onClick = {
                            (context as? Activity)?.let { activity ->
                                onPurchasePro(activity)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = BgPage
                        )
                    ) {
                        Text(
                            text = uiState.proPrice?.let { price ->
                                stringResource(R.string.settings_upgrade_pro_with_price, price)
                            } ?: stringResource(R.string.settings_upgrade_pro)
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                    OutlinedButton(
                        onClick = onRefreshPurchaseStatus,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.settings_restore_purchase))
                    }
                }
            }

            HorizontalDivider(color = BgIconCircle, thickness = 1.dp)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            SectionHeader(text = stringResource(R.string.settings_data_section))
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            if (uiState.isPro) {
                Text(
                    text = stringResource(R.string.settings_data_retention_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                DataRetention.entries.forEach { retention ->
                    SettingsRadioRow(
                        label = retention.label(),
                        selected = uiState.preferences.dataRetention == retention,
                        onSelect = { onDataRetentionChange(retention) }
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                OutlinedButton(
                    onClick = onExportData,
                    enabled = !uiState.isExporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isExporting) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Text(text = stringResource(R.string.settings_exporting))
                        }
                    } else {
                        Text(text = stringResource(R.string.settings_export_data))
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.settings_data_retention_free),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                ProFeatureCalloutCard(
                    message = stringResource(R.string.pro_feature_export_message),
                    actionLabel = stringResource(R.string.pro_feature_upgrade_action),
                    onAction = {
                        (context as? Activity)?.let { activity ->
                            onPurchasePro(activity)
                        }
                    }
                )
            }

            uiState.deviceProfile?.let { profile ->
                HorizontalDivider(color = BgIconCircle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                SectionHeader(text = stringResource(R.string.settings_measurement_info))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                Text(
                    text = stringResource(
                        R.string.settings_device_model,
                        profile.manufacturer,
                        profile.model
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingsInfoLine(
                    stringResource(R.string.settings_api_level, profile.apiLevel)
                )
                SettingsInfoLine(
                    stringResource(
                        R.string.settings_current_reading,
                        stringResource(
                            if (profile.currentNowReliable) {
                                R.string.settings_measurement_reliable
                            } else {
                                R.string.settings_measurement_unreliable
                            }
                        )
                    )
                )
                SettingsInfoLine(
                    stringResource(
                        R.string.settings_cycle_count,
                        stringResource(
                            if (profile.cycleCountAvailable) {
                                R.string.settings_measurement_available
                            } else {
                                R.string.settings_measurement_not_available
                            }
                        )
                    )
                )
                SettingsInfoLine(
                    stringResource(
                        R.string.settings_thermal_zones_found,
                        profile.thermalZonesAvailable.size
                    )
                )
            }

            HorizontalDivider(color = BgIconCircle, thickness = 1.dp)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            SectionHeader(text = stringResource(R.string.settings_privacy))
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            SettingsToggle(
                title = stringResource(R.string.settings_crash_reporting),
                description = stringResource(R.string.settings_crash_reporting_desc),
                checked = uiState.preferences.crashReportingEnabled,
                onCheckedChange = onCrashReportingChange
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            Text(
                text = stringResource(R.string.settings_crash_reporting_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = BgIconCircle, thickness = 1.dp)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            SectionHeader(text = stringResource(R.string.settings_about))
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            uiState.billingStatus?.let { status ->
                LaunchedEffect(status) {
                    Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                    onClearBillingStatus()
                }
            }

            uiState.exportStatus?.let { status ->
                LaunchedEffect(status) {
                    Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                    onClearExportStatus()
                }
            }

            uiState.exportUris?.let { exportUris ->
                LaunchedEffect(exportUris) {
                    val shareIntent = if (exportUris.size == 1) {
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, exportUris.first())
                            clipData = android.content.ClipData.newRawUri(null, exportUris.first())
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    } else {
                        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "text/csv"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(exportUris))
                            clipData = android.content.ClipData.newUri(
                                context.contentResolver,
                                null,
                                exportUris.first()
                            ).apply {
                                exportUris.drop(1).forEach { addItem(android.content.ClipData.Item(it)) }
                            }
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            context.getString(R.string.settings_export_share_title)
                        )
                    )
                    onClearExportUris()
                }
            }

            uiState.errorMessage?.let { message ->
                LaunchedEffect(message) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    onClearErrorMessage()
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

@Composable
private fun DataRetention.label(): String = when (this) {
    DataRetention.THREE_MONTHS -> stringResource(R.string.settings_retention_3_months)
    DataRetention.SIX_MONTHS -> stringResource(R.string.settings_retention_6_months)
    DataRetention.ONE_YEAR -> stringResource(R.string.settings_retention_1_year)
    DataRetention.FOREVER -> stringResource(R.string.settings_retention_forever)
}

@Composable
private fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MaterialTheme.spacing.xl * 2)
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton
            )
            .padding(vertical = MaterialTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = MaterialTheme.spacing.sm)
        )
    }
}

@Composable
private fun SettingsInfoLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
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
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch
            )
            .padding(vertical = MaterialTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}

@Composable
private fun PermissionHelpCard(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenContentPreview() {
    RuncheckTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                deviceProfile = DeviceProfileInfo(
                    manufacturer = "Google",
                    model = "Pixel 9",
                    apiLevel = 35,
                    currentNowReliable = true,
                    currentNowUnit = CurrentUnit.MILLIAMPS,
                    currentNowSignConvention = SignConvention.NEGATIVE_CHARGING,
                    cycleCountAvailable = true,
                    batteryHealthPercentAvailable = true,
                    thermalZonesAvailable = listOf("Battery", "CPU", "Skin"),
                    storageHealthAvailable = true
                ),
                isPro = true
            ),
            onBack = {},
            onMonitoringIntervalChange = {},
            onDataRetentionChange = {},
            onNotificationsChange = {},
            onCrashReportingChange = {},
            onPurchasePro = {},
            onRefreshPurchaseStatus = {},
            onExportData = {},
            onClearBillingStatus = {},
            onClearExportUris = {},
            onClearExportStatus = {},
            onClearErrorMessage = {}
        )
    }
}
