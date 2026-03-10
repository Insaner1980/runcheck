package com.devicepulse.ui.settings

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.R
import com.devicepulse.domain.model.MonitoringInterval
import com.devicepulse.domain.model.ThemeMode
import com.devicepulse.ui.theme.spacing

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.base)
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

        // Theme section
        SectionHeader(stringResource(R.string.settings_theme))

        ThemeMode.entries.forEach { mode ->
            val label = when (mode) {
                ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setThemeMode(mode) }
                    .padding(vertical = MaterialTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = uiState.preferences.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) }
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = MaterialTheme.spacing.sm)
                )
            }
        }

        // AMOLED Black toggle (only visible in dark mode)
        if (uiState.preferences.themeMode == ThemeMode.DARK) {
            SettingsToggle(
                title = stringResource(R.string.settings_amoled_black),
                description = stringResource(R.string.settings_amoled_black_desc),
                checked = uiState.preferences.amoledBlack,
                onCheckedChange = { viewModel.setAmoledBlack(it) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md))

        // Dynamic colors
        SettingsToggle(
            title = stringResource(R.string.settings_dynamic_colors),
            description = stringResource(R.string.settings_dynamic_colors_desc),
            checked = uiState.preferences.dynamicColors,
            onCheckedChange = { viewModel.setDynamicColors(it) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md))

        // Monitoring interval
        SectionHeader(stringResource(R.string.settings_monitoring_interval))

        MonitoringInterval.entries.forEach { interval ->
            val label = when (interval) {
                MonitoringInterval.FIFTEEN -> stringResource(R.string.settings_interval_15)
                MonitoringInterval.THIRTY -> stringResource(R.string.settings_interval_30)
                MonitoringInterval.SIXTY -> stringResource(R.string.settings_interval_60)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setMonitoringInterval(interval) }
                    .padding(vertical = MaterialTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = uiState.preferences.monitoringInterval == interval,
                    onClick = { viewModel.setMonitoringInterval(interval) }
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = MaterialTheme.spacing.sm)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md))

        // Notifications
        SettingsToggle(
            title = stringResource(R.string.settings_notifications),
            description = stringResource(R.string.settings_notifications_desc),
            checked = uiState.preferences.notificationsEnabled,
            onCheckedChange = { viewModel.setNotifications(it) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md))

        // Device measurement info
        uiState.deviceProfile?.let { profile ->
            SectionHeader(stringResource(R.string.settings_measurement_info))
            Text(
                text = "${profile.manufacturer} ${profile.model}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "API Level: ${profile.apiLevel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Current reading: ${if (profile.currentNowReliable) "Reliable" else "Unreliable"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Cycle count: ${if (profile.cycleCountAvailable) "Available" else "Not available"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Thermal zones: ${profile.thermalZonesAvailable.size} found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md))

        // Upgrade to Pro
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
            val context = LocalContext.current
            Button(
                onClick = {
                    (context as? Activity)?.let { activity ->
                        viewModel.purchasePro(activity)
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

        HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md))

        // About
        SectionHeader(stringResource(R.string.settings_about))
        Text(
            text = stringResource(R.string.settings_version, "1.0.0"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = MaterialTheme.spacing.sm)
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
            onCheckedChange = onCheckedChange
        )
    }
}
