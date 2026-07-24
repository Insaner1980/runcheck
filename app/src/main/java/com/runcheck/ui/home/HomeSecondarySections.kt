package com.runcheck.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.ui.common.batteryHealthLabel
import com.runcheck.ui.common.chargingStatusLabel
import com.runcheck.ui.common.connectionDisplayLabel
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.common.formatTemperature
import com.runcheck.ui.common.signalQualityLabel
import com.runcheck.ui.common.temperatureBandLabel
import com.runcheck.ui.components.GridCard
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColorForPercent
import com.runcheck.ui.theme.statusColorForSignalQuality
import com.runcheck.ui.theme.statusColorForStoragePercent
import com.runcheck.ui.theme.statusColorForTemperature
import com.runcheck.ui.theme.statusColors

@Composable
internal fun HomeGridSection(
    state: HomeUiState.Success,
    isWideScreen: Boolean,
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
) {
    val context = LocalContext.current
    if (isWideScreen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            HomeGridCards(
                state = state,
                context = context,
                onNavigateToBattery = onNavigateToBattery,
                onNavigateToNetwork = onNavigateToNetwork,
                onNavigateToThermal = onNavigateToThermal,
                onNavigateToStorage = onNavigateToStorage,
            )
        }
    } else {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                BatteryGridCard(state = state, onClick = onNavigateToBattery)
                NetworkGridCard(state = state, onClick = onNavigateToNetwork)
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                ThermalGridCard(state = state, onClick = onNavigateToThermal)
                StorageGridCard(
                    state = state,
                    context = context,
                    onClick = onNavigateToStorage,
                )
            }
        }
    }
}

@Composable
private fun RowScope.BatteryGridCard(
    state: HomeUiState.Success,
    onClick: () -> Unit,
) {
    val battery = state.batteryState
    GridCard(
        icon = Icons.Outlined.BatteryStd,
        title = stringResource(R.string.home_battery_card),
        subtitle = stringResource(R.string.value_percent, battery.level),
        subtitleColor = MaterialTheme.colorScheme.onSurface,
        statusLabel =
            if (battery.health == BatteryHealth.GOOD) {
                chargingStatusLabel(battery.chargingStatus)
            } else {
                batteryHealthLabel(battery.health)
            },
        iconTint = statusColorForPercent(battery.level),
        iconBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        onClick = onClick,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun RowScope.NetworkGridCard(
    state: HomeUiState.Success,
    onClick: () -> Unit,
) {
    val isConnected = state.networkState.isConnected
    GridCard(
        icon = Icons.Outlined.SignalCellularAlt,
        title = stringResource(R.string.home_network_card),
        subtitle =
            connectionDisplayLabel(
                connectionType = state.networkState.connectionType,
                wifiSsid = state.networkState.wifiSsid,
                networkSubtype = state.networkState.networkSubtype,
            ),
        subtitleColor = MaterialTheme.colorScheme.onSurface,
        statusLabel =
            if (isConnected) {
                signalQualityLabel(state.networkState.signalQuality)
            } else {
                stringResource(R.string.score_unrated)
            },
        iconTint =
            if (isConnected) {
                statusColorForSignalQuality(state.networkState.signalQuality)
            } else {
                MaterialTheme.statusColors.unavailable
            },
        iconBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        onClick = onClick,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun RowScope.ThermalGridCard(
    state: HomeUiState.Success,
    onClick: () -> Unit,
) {
    GridCard(
        icon = Icons.Outlined.Thermostat,
        title = stringResource(R.string.home_thermal_card),
        subtitle =
            formatTemperature(
                state.thermalState.batteryTempC,
                state.temperatureUnit,
            ),
        subtitleColor = MaterialTheme.colorScheme.onSurface,
        statusLabel = temperatureBandLabel(state.thermalState.batteryTempC),
        iconTint = statusColorForTemperature(state.thermalState.batteryTempC),
        iconBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        onClick = onClick,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun RowScope.StorageGridCard(
    state: HomeUiState.Success,
    context: Context,
    onClick: () -> Unit,
) {
    GridCard(
        icon = Icons.Outlined.DataUsage,
        title = stringResource(R.string.home_storage_card),
        subtitle =
            stringResource(
                R.string.home_storage_free,
                formatStorageSize(context, state.storageState.availableBytes),
                stringResource(R.string.home_free_suffix),
            ),
        subtitleColor = MaterialTheme.colorScheme.onSurface,
        statusLabel =
            stringResource(
                R.string.home_storage_used,
                state.storageState.usagePercent.toInt().coerceIn(0, 100),
            ),
        iconTint =
            statusColorForStoragePercent(
                state.storageState.usagePercent.toInt().coerceIn(0, 100),
            ),
        iconBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        onClick = onClick,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun RowScope.HomeGridCards(
    state: HomeUiState.Success,
    context: Context,
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
) {
    BatteryGridCard(state = state, onClick = onNavigateToBattery)
    NetworkGridCard(state = state, onClick = onNavigateToNetwork)
    ThermalGridCard(state = state, onClick = onNavigateToThermal)
    StorageGridCard(
        state = state,
        context = context,
        onClick = onNavigateToStorage,
    )
}
