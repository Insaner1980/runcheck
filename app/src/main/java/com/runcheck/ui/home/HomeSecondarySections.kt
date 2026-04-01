package com.runcheck.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.pro.ProStatus
import com.runcheck.ui.common.connectionDisplayLabel
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.common.formatTemperature
import com.runcheck.ui.common.signalQualityLabel
import com.runcheck.ui.common.temperatureBandLabel
import com.runcheck.ui.components.GridCard
import com.runcheck.ui.components.IconCircle
import com.runcheck.ui.components.ListRow
import com.runcheck.ui.components.ProBadgePill
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColorForSignalQuality
import com.runcheck.ui.theme.statusColorForStoragePercent
import com.runcheck.ui.theme.statusColorForTemperature
import com.runcheck.ui.theme.statusColors

@Composable
internal fun HomeGridSection(
    state: HomeUiState.Success,
    isWideScreen: Boolean,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
) {
    val context = LocalContext.current
    if (isWideScreen) {
        WideHomeGridSection(
            state = state,
            context = context,
            onNavigateToNetwork = onNavigateToNetwork,
            onNavigateToThermal = onNavigateToThermal,
            onNavigateToCharger = onNavigateToCharger,
            onNavigateToStorage = onNavigateToStorage,
            onNavigateToProUpgrade = onNavigateToProUpgrade,
        )
    } else {
        CompactHomeGridSection(
            state = state,
            context = context,
            onNavigateToNetwork = onNavigateToNetwork,
            onNavigateToThermal = onNavigateToThermal,
            onNavigateToCharger = onNavigateToCharger,
            onNavigateToStorage = onNavigateToStorage,
            onNavigateToProUpgrade = onNavigateToProUpgrade,
        )
    }
}

@Composable
private fun WideHomeGridSection(
    state: HomeUiState.Success,
    context: Context,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        HomeGridCards(
            state = state,
            context = context,
            onNavigateToNetwork = onNavigateToNetwork,
            onNavigateToThermal = onNavigateToThermal,
            onNavigateToCharger = onNavigateToCharger,
            onNavigateToStorage = onNavigateToStorage,
            onNavigateToProUpgrade = onNavigateToProUpgrade,
        )
    }
}

@Composable
private fun CompactHomeGridSection(
    state: HomeUiState.Success,
    context: Context,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
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
                statusLabel = signalQualityLabel(state.networkState.signalQuality),
                iconTint = statusColorForSignalQuality(state.networkState.signalQuality),
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                onClick = onNavigateToNetwork,
                modifier = Modifier.weight(1f),
            )
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
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                onClick = onNavigateToThermal,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            GridCard(
                icon = Icons.Outlined.BatteryChargingFull,
                title = stringResource(R.string.home_chargers_card),
                subtitle = stringResource(R.string.home_test_compare),
                subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                locked = !state.isPro,
                onClick = if (state.isPro) onNavigateToCharger else onNavigateToProUpgrade,
                modifier = Modifier.weight(1f),
            )
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
                iconTint =
                    statusColorForStoragePercent(
                        (
                            (state.storageState.totalBytes - state.storageState.availableBytes) * 100 /
                                state.storageState.totalBytes.coerceAtLeast(1)
                        ).toInt(),
                    ),
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                onClick = onNavigateToStorage,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun HomeQuickToolsSection(
    isPro: Boolean,
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
    onNavigateToLearn: () -> Unit,
) {
    Column {
        SectionHeader(stringResource(R.string.home_quick_tools))

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

        Card(
            shape = MaterialTheme.shapes.large,
            colors = runcheckCardColors(),
            elevation = runcheckCardElevation(),
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = MaterialTheme.spacing.base,
                        vertical = MaterialTheme.spacing.xs,
                    ),
            ) {
                ListRow(
                    label = stringResource(R.string.home_speed_test),
                    icon = Icons.Outlined.Speed,
                    onClick = onNavigateToSpeedTest,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                HomeAppUsageQuickToolRow(
                    isPro = isPro,
                    onNavigateToAppUsage = onNavigateToAppUsage,
                    onNavigateToProUpgrade = onNavigateToProUpgrade,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ListRow(
                    label = stringResource(R.string.home_learn),
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    onClick = onNavigateToLearn,
                )
            }
        }
    }
}

@Composable
private fun HomeAppUsageQuickToolRow(
    isPro: Boolean,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        ListRow(
            label = stringResource(R.string.home_app_usage_card),
            icon = Icons.Outlined.DataUsage,
            onClick = if (isPro) onNavigateToAppUsage else onNavigateToProUpgrade,
            trailing = if (!isPro) ({ ProBadgePill() }) else null,
        )

        if (!isPro) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.14f)),
            )
        }
    }
}

@Composable
internal fun HomeProStatusSection(
    proStatus: ProStatus,
    onNavigateToProUpgrade: () -> Unit,
) {
    if (proStatus == ProStatus.PRO_PURCHASED) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = runcheckCardColors(),
            elevation = runcheckCardElevation(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconCircle(
                    icon = Icons.Outlined.Star,
                    tint = MaterialTheme.statusColors.healthy,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_insights_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.home_insights_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    } else if (proStatus == ProStatus.TRIAL_EXPIRED) {
        Card(
            onClick = onNavigateToProUpgrade,
            shape = MaterialTheme.shapes.large,
            colors = runcheckCardColors(),
            elevation = runcheckCardElevation(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconCircle(
                    icon = Icons.Outlined.Lock,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_pro_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.home_pro_history_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RowScope.HomeGridCards(
    state: HomeUiState.Success,
    context: Context,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
) {
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
        statusLabel = signalQualityLabel(state.networkState.signalQuality),
        iconTint = statusColorForSignalQuality(state.networkState.signalQuality),
        iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        onClick = onNavigateToNetwork,
        modifier = Modifier.weight(1f),
    )
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
        iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        onClick = onNavigateToThermal,
        modifier = Modifier.weight(1f),
    )
    GridCard(
        icon = Icons.Outlined.BatteryChargingFull,
        title = stringResource(R.string.home_chargers_card),
        subtitle = stringResource(R.string.home_test_compare),
        subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
        iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        locked = !state.isPro,
        onClick = if (state.isPro) onNavigateToCharger else onNavigateToProUpgrade,
        modifier = Modifier.weight(1f),
    )
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
        iconTint =
            statusColorForStoragePercent(
                (
                    (state.storageState.totalBytes - state.storageState.availableBytes) * 100 /
                        state.storageState.totalBytes.coerceAtLeast(1)
                ).toInt(),
            ),
        iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        onClick = onNavigateToStorage,
        modifier = Modifier.weight(1f),
    )
}
