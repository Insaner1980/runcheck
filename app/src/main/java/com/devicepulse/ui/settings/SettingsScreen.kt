package com.devicepulse.ui.settings

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.BuildConfig
import com.devicepulse.R
import com.devicepulse.domain.model.CurrentUnit
import com.devicepulse.domain.model.DeviceProfileInfo
import com.devicepulse.domain.model.MonitoringInterval
import com.devicepulse.domain.model.SignConvention
import com.devicepulse.ui.components.DetailTopBar
import com.devicepulse.ui.components.ProFeatureCalloutCard
import com.devicepulse.ui.components.SectionHeader
import com.devicepulse.ui.theme.BgIconCircle
import com.devicepulse.ui.theme.DevicePulseTheme
import com.devicepulse.ui.theme.spacing

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreenContent(
        uiState = uiState,
        onBack = onBack,
        onMonitoringIntervalChange = viewModel::setMonitoringInterval,
        onNotificationsChange = viewModel::setNotifications,
        onPurchasePro = { activity -> viewModel.purchasePro(activity) },
        onRefreshPurchaseStatus = viewModel::refreshPurchaseStatus,
        onExportData = viewModel::exportData,
        onClearBillingStatus = viewModel::clearBillingStatus,
        onClearExportStatus = viewModel::clearExportStatus,
        onClearErrorMessage = viewModel::clearErrorMessage
    )
}

@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onMonitoringIntervalChange: (MonitoringInterval) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onPurchasePro: (Activity) -> Unit,
    onRefreshPurchaseStatus: () -> Unit,
    onExportData: () -> Unit,
    onClearBillingStatus: () -> Unit,
    onClearExportStatus: () -> Unit,
    onClearErrorMessage: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()
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

            // --- MONITORING ---
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

            // --- NOTIFICATIONS ---
            SectionHeader(text = stringResource(R.string.settings_notifications))
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            SettingsToggle(
                title = stringResource(R.string.settings_notifications),
                description = stringResource(R.string.settings_notifications_desc),
                checked = uiState.preferences.notificationsEnabled,
                onCheckedChange = onNotificationsChange
            )

            HorizontalDivider(color = BgIconCircle, thickness = 1.dp)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            // --- PRO ---
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
                    text = stringResource(R.string.settings_pro_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                Button(
                    onClick = {
                        (context as? Activity)?.let { activity ->
                            onPurchasePro(activity)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
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

            HorizontalDivider(color = BgIconCircle, thickness = 1.dp)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            // --- DATA ---
            SectionHeader(text = stringResource(R.string.settings_data_section))
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            if (uiState.isPro) {
                OutlinedButton(
                    onClick = onExportData,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.settings_export_data))
                }
            } else {
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

            // --- ABOUT ---
            SectionHeader(text = stringResource(R.string.settings_about))
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Toast side-effects
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

@Preview(showBackground = true)
@Composable
private fun SettingsScreenContentPreview() {
    DevicePulseTheme {
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
            onNotificationsChange = {},
            onPurchasePro = {},
            onRefreshPurchaseStatus = {},
            onExportData = {},
            onClearBillingStatus = {},
            onClearExportStatus = {},
            onClearErrorMessage = {}
        )
    }
}
