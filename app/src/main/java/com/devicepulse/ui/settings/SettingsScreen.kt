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
import com.devicepulse.R
import com.devicepulse.domain.model.CurrentUnit
import com.devicepulse.domain.model.DeviceProfileInfo
import com.devicepulse.domain.model.MonitoringInterval
import com.devicepulse.domain.model.SignConvention
import com.devicepulse.domain.model.ThemeMode
import com.devicepulse.ui.components.DetailTopBar
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
        onThemeModeChange = viewModel::setThemeMode,
        onAmoledBlackChange = viewModel::setAmoledBlack,
        onDynamicColorsChange = viewModel::setDynamicColors,
        onMonitoringIntervalChange = viewModel::setMonitoringInterval,
        onNotificationsChange = viewModel::setNotifications,
        onPurchasePro = { activity -> viewModel.purchasePro(activity) },
        onExportData = viewModel::exportData,
        onClearExportStatus = viewModel::clearExportStatus,
        onClearErrorMessage = viewModel::clearErrorMessage
    )
}

@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAmoledBlackChange: (Boolean) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
    onMonitoringIntervalChange: (MonitoringInterval) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onPurchasePro: (Activity) -> Unit,
    onExportData: () -> Unit,
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

            SectionHeader(stringResource(R.string.settings_theme))

            ThemeMode.entries.forEach { mode ->
                val label = when (mode) {
                    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                }
                SettingsRadioRow(
                    label = label,
                    selected = uiState.preferences.themeMode == mode,
                    onSelect = { onThemeModeChange(mode) }
                )
            }

            if (uiState.preferences.themeMode == ThemeMode.DARK) {
                SettingsToggle(
                    title = stringResource(R.string.settings_amoled_black),
                    description = stringResource(R.string.settings_amoled_black_desc),
                    checked = uiState.preferences.amoledBlack,
                    onCheckedChange = onAmoledBlackChange
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md))

            SettingsToggle(
                title = stringResource(R.string.settings_dynamic_colors),
                description = stringResource(R.string.settings_dynamic_colors_desc),
                checked = uiState.preferences.dynamicColors,
                onCheckedChange = onDynamicColorsChange
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md))

            SectionHeader(stringResource(R.string.settings_monitoring_interval))

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

            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md))

            SettingsToggle(
                title = stringResource(R.string.settings_notifications),
                description = stringResource(R.string.settings_notifications_desc),
                checked = uiState.preferences.notificationsEnabled,
                onCheckedChange = onNotificationsChange
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md))

            uiState.deviceProfile?.let { profile ->
                SectionHeader(stringResource(R.string.settings_measurement_info))
                Text(
                    text = "${profile.manufacturer} ${profile.model}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingsInfoLine("API Level: ${profile.apiLevel}")
                SettingsInfoLine(
                    "Current reading: ${if (profile.currentNowReliable) "Reliable" else "Unreliable"}"
                )
                SettingsInfoLine(
                    "Cycle count: ${if (profile.cycleCountAvailable) "Available" else "Not available"}"
                )
                SettingsInfoLine("Thermal zones: ${profile.thermalZonesAvailable.size} found")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md))

            SectionHeader(
                if (uiState.isPro) stringResource(R.string.settings_pro_active)
                else stringResource(R.string.settings_upgrade_pro)
            )
            if (uiState.isPro) {
                Text(
                    text = stringResource(R.string.settings_pro_thank_you),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                        text = uiState.proPrice?.let {
                            "${stringResource(R.string.settings_upgrade_pro)} — $it"
                        } ?: stringResource(R.string.settings_upgrade_pro)
                    )
                }
            }

            if (uiState.isPro) {
                HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md))

                OutlinedButton(
                    onClick = onExportData,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.settings_export_data))
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

            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md))

            SectionHeader(stringResource(R.string.settings_about))
            Text(
                text = stringResource(R.string.settings_version, "1.0.0"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = MaterialTheme.spacing.sm)
    )
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
            onThemeModeChange = {},
            onAmoledBlackChange = {},
            onDynamicColorsChange = {},
            onMonitoringIntervalChange = {},
            onNotificationsChange = {},
            onPurchasePro = {},
            onExportData = {},
            onClearExportStatus = {},
            onClearErrorMessage = {}
        )
    }
}
