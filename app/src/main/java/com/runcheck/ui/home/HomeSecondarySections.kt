package com.runcheck.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.HealthScore
import com.runcheck.ui.common.batteryHealthLabel
import com.runcheck.ui.common.chargingStatusLabel
import com.runcheck.ui.common.connectionDisplayLabel
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.common.formatTemperatureValue
import com.runcheck.ui.common.signalQualityLabel
import com.runcheck.ui.common.temperatureBandLabel
import com.runcheck.ui.common.temperatureUnitRes
import com.runcheck.ui.components.MetricDomain
import com.runcheck.ui.components.MetricTile
import com.runcheck.ui.components.MetricTileLayout
import com.runcheck.ui.components.StatusTone
import com.runcheck.ui.theme.spacing

private const val LARGE_FONT_GRID_THRESHOLD = 1.3f

internal fun homeMetricGridColumns(
    isWideScreen: Boolean,
    fontScale: Float,
): Int =
    when {
        fontScale >= LARGE_FONT_GRID_THRESHOLD && isWideScreen -> 2
        fontScale >= LARGE_FONT_GRID_THRESHOLD -> 1
        isWideScreen -> 4
        else -> 2
    }

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
    val columns =
        homeMetricGridColumns(
            isWideScreen = isWideScreen,
            fontScale = LocalDensity.current.fontScale,
        )
    val cards =
        listOf<@Composable (Modifier) -> Unit>(
            { modifier ->
                BatteryMetricTile(
                    state = state,
                    onClick = onNavigateToBattery,
                    modifier = modifier,
                )
            },
            { modifier ->
                NetworkMetricTile(
                    state = state,
                    onClick = onNavigateToNetwork,
                    modifier = modifier,
                )
            },
            { modifier ->
                ThermalMetricTile(
                    state = state,
                    onClick = onNavigateToThermal,
                    modifier = modifier,
                )
            },
            { modifier ->
                StorageMetricTile(
                    state = state,
                    context = context,
                    onClick = onNavigateToStorage,
                    modifier = modifier,
                )
            },
        )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        cards.chunked(columns).forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                rowCards.forEach { card ->
                    card(Modifier.weight(1f))
                }
                repeat(columns - rowCards.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BatteryMetricTile(
    state: HomeUiState.Success,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val battery = state.batteryState
    MetricTile(
        domain = MetricDomain.BATTERY,
        value = battery.level.toString(),
        unit = stringResource(R.string.unit_percent),
        label = stringResource(R.string.home_battery_card),
        status =
            if (battery.health == BatteryHealth.GOOD) {
                chargingStatusLabel(battery.chargingStatus)
            } else {
                batteryHealthLabel(battery.health)
            },
        statusTone = state.healthScore.batteryScore.toStatusTone(),
        onClick = onClick,
        layout = MetricTileLayout.COMPACT,
        modifier = modifier,
    )
}

@Composable
private fun NetworkMetricTile(
    state: HomeUiState.Success,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val network = state.networkState
    MetricTile(
        domain = MetricDomain.NETWORK,
        value =
            connectionDisplayLabel(
                connectionType = network.connectionType,
                wifiSsid = null,
                networkSubtype = network.networkSubtype,
            ),
        unit = stringResource(R.string.home_network_connection_unit),
        label = stringResource(R.string.home_network_card),
        status =
            if (network.isConnected) {
                signalQualityLabel(network.signalQuality)
            } else {
                stringResource(R.string.score_unrated)
            },
        statusTone =
            if (network.isConnected) {
                state.healthScore.networkScore.toStatusTone()
            } else {
                StatusTone.UNAVAILABLE
            },
        onClick = onClick,
        layout = MetricTileLayout.COMPACT,
        modifier = modifier,
    )
}

@Composable
private fun ThermalMetricTile(
    state: HomeUiState.Success,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val temperature = state.thermalState.batteryTempC
    MetricTile(
        domain = MetricDomain.THERMAL,
        value = formatTemperatureValue(temperature, state.temperatureUnit),
        unit = stringResource(temperatureUnitRes(state.temperatureUnit)),
        label = stringResource(R.string.home_thermal_card),
        status = temperatureBandLabel(temperature),
        statusTone = state.healthScore.thermalScore.toStatusTone(),
        onClick = onClick,
        layout = MetricTileLayout.COMPACT,
        modifier = modifier,
    )
}

@Composable
private fun StorageMetricTile(
    state: HomeUiState.Success,
    context: Context,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val storage = state.storageState
    MetricTile(
        domain = MetricDomain.STORAGE,
        value =
            storage.usagePercent
                .toInt()
                .coerceIn(0, 100)
                .toString(),
        unit = stringResource(R.string.home_storage_used_unit),
        label = stringResource(R.string.home_storage_card),
        status =
            stringResource(
                R.string.home_storage_free,
                formatStorageSize(context, storage.availableBytes),
                stringResource(R.string.home_free_suffix),
            ),
        statusTone = state.healthScore.storageScore.toStatusTone(),
        onClick = onClick,
        layout = MetricTileLayout.COMPACT,
        modifier = modifier,
    )
}

private fun Int.toStatusTone(): StatusTone =
    when (HealthScore.statusFromScore(this)) {
        com.runcheck.domain.model.HealthStatus.HEALTHY -> StatusTone.HEALTHY
        com.runcheck.domain.model.HealthStatus.FAIR -> StatusTone.FAIR
        com.runcheck.domain.model.HealthStatus.POOR -> StatusTone.POOR
        com.runcheck.domain.model.HealthStatus.CRITICAL -> StatusTone.CRITICAL
    }
