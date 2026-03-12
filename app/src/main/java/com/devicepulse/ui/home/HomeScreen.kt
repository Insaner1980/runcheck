package com.devicepulse.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.R
import com.devicepulse.domain.model.HealthScore
import com.devicepulse.domain.model.HealthStatus
import com.devicepulse.ui.common.connectionDisplayLabel
import com.devicepulse.ui.common.formatPercent
import com.devicepulse.ui.common.formatStorageSize
import com.devicepulse.ui.common.formatTemperature
import com.devicepulse.ui.common.scoreLabel
import com.devicepulse.ui.components.GridCard
import com.devicepulse.ui.components.IconCircle
import com.devicepulse.ui.components.ListRow
import com.devicepulse.ui.components.PrimaryTopBar
import com.devicepulse.ui.components.ProBadgePill
import com.devicepulse.ui.components.ProgressRing
import com.devicepulse.ui.components.SectionHeader
import com.devicepulse.ui.components.StatusDot
import com.devicepulse.ui.theme.AccentBlue
import com.devicepulse.ui.theme.AccentOrange
import com.devicepulse.ui.theme.AccentTeal
import com.devicepulse.ui.theme.BgIconCircle
import com.devicepulse.ui.theme.BgCardAlt
import com.devicepulse.ui.theme.TextMuted
import com.devicepulse.ui.theme.TextSecondary
import com.devicepulse.ui.theme.numericFontFamily
import com.devicepulse.ui.theme.statusColors

@Composable
fun HomeScreen(
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Header
        PrimaryTopBar(
            title = stringResource(R.string.app_name),
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.error_generic),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }

            is HomeUiState.Success -> {
                HomeContent(
                    state = state,
                    onNavigateToBattery = onNavigateToBattery,
                    onNavigateToNetwork = onNavigateToNetwork,
                    onNavigateToThermal = onNavigateToThermal,
                    onNavigateToStorage = onNavigateToStorage,
                    onNavigateToCharger = onNavigateToCharger,
                    onNavigateToSpeedTest = onNavigateToSpeedTest,
                    onNavigateToAppUsage = onNavigateToAppUsage,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToProUpgrade = onNavigateToProUpgrade
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProUpgrade: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        HomeStatusSummary(state = state)

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Health Score hero card
        HealthScoreCard(
            state = state,
            onNavigateToBattery = onNavigateToBattery,
            onNavigateToThermal = onNavigateToThermal,
            onNavigateToNetwork = onNavigateToNetwork,
            onNavigateToStorage = onNavigateToStorage
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Battery hero card
        BatteryHeroCard(
            state = state,
            onClick = onNavigateToBattery
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Feature grid 2x2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GridCard(
                icon = Icons.Outlined.SignalCellularAlt,
                title = stringResource(R.string.home_network_card),
                subtitle = connectionDisplayLabel(
                    connectionType = state.networkState.connectionType,
                    wifiSsid = state.networkState.wifiSsid,
                    networkSubtype = state.networkState.networkSubtype
                ),
                subtitleColor = AccentTeal,
                onClick = onNavigateToNetwork,
                modifier = Modifier.weight(1f)
            )
            GridCard(
                icon = Icons.Outlined.Thermostat,
                title = stringResource(R.string.home_thermal_card),
                subtitle = formatTemperature(state.thermalState.batteryTempC) + " \u00B7 " + thermalLabel(state.thermalState.batteryTempC),
                subtitleColor = AccentOrange,
                onClick = onNavigateToThermal,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GridCard(
                icon = Icons.Outlined.BatteryChargingFull,
                title = stringResource(R.string.home_chargers_card),
                subtitle = stringResource(R.string.home_test_compare),
                subtitleColor = AccentBlue,
                onClick = onNavigateToCharger,
                modifier = Modifier.weight(1f)
            )
            GridCard(
                icon = Icons.Outlined.DataUsage,
                title = stringResource(R.string.home_storage_card),
                subtitle = formatStorageSize(context, state.storageState.availableBytes) + " " + stringResource(R.string.home_free_suffix),
                subtitleColor = AccentTeal,
                onClick = onNavigateToStorage,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. Quick Tools section
        SectionHeader(stringResource(R.string.home_quick_tools))

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                ListRow(
                    label = stringResource(R.string.home_speed_test),
                    icon = Icons.Outlined.Speed,
                    onClick = onNavigateToSpeedTest
                )
                HorizontalDivider(color = BgIconCircle, thickness = 1.dp)
                ListRow(
                    label = stringResource(R.string.home_app_usage_card),
                    icon = Icons.Outlined.DataUsage,
                    onClick = onNavigateToAppUsage,
                    trailing = if (!state.isPro) {
                        { ProBadgePill() }
                    } else null
                )
            }
        }

        // 7. Pro banner (hide if isPro)
        if (!state.isPro) {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                onClick = onNavigateToProUpgrade,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = AccentBlue.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AccentBlue.copy(alpha = 0.18f)
                    ) {
                        IconCircle(icon = Icons.Outlined.Lock)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_pro_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.home_pro_history_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun HomeStatusSummary(state: HomeUiState.Success) {
    val batteryStatus = when (state.batteryState.chargingStatus) {
        com.devicepulse.domain.model.ChargingStatus.CHARGING -> stringResource(R.string.charging_status_charging)
        com.devicepulse.domain.model.ChargingStatus.FULL -> stringResource(R.string.charging_status_full)
        else -> stringResource(R.string.charging_status_not_charging)
    }
    val networkStatus = when (state.networkState.connectionType) {
        com.devicepulse.domain.model.ConnectionType.WIFI -> stringResource(R.string.connection_wifi)
        com.devicepulse.domain.model.ConnectionType.CELLULAR -> connectionDisplayLabel(
            connectionType = state.networkState.connectionType,
            wifiSsid = state.networkState.wifiSsid,
            networkSubtype = state.networkState.networkSubtype
        )
        else -> stringResource(R.string.connection_none)
    }

    Text(
        text = stringResource(
            R.string.home_status_summary,
            batteryStatus,
            thermalLabel(state.thermalState.batteryTempC),
            networkStatus
        ),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ── Health Score Hero Card ──────────────────────────────────────────────────

@Composable
private fun HealthScoreCard(
    state: HomeUiState.Success,
    onNavigateToBattery: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToStorage: () -> Unit
) {
    val score = state.healthScore.overallScore
    val statusLabel = scoreLabel(score)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                SectionHeader(stringResource(R.string.home_health_score))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress ring with score
            ProgressRing(
                progress = score / 100f,
                modifier = Modifier.size(152.dp),
                strokeWidth = 10.dp,
                progressColor = AccentTeal
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = score.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = MaterialTheme.numericFontFamily,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary text with colored status word
            val formattedSummary = stringResource(R.string.home_device_good_shape, statusLabel.lowercase())
            val statusWord = statusLabel.lowercase()
            val startIndex = formattedSummary.indexOf(statusWord)
            Text(
                text = buildAnnotatedString {
                    if (startIndex >= 0) {
                        append(formattedSummary.substring(0, startIndex))
                        withStyle(SpanStyle(color = AccentTeal, fontWeight = FontWeight.Bold)) {
                            append(statusWord)
                        }
                        append(formattedSummary.substring(startIndex + statusWord.length))
                    } else {
                        append(formattedSummary)
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            // Temperature warning if elevated
            if (state.thermalState.batteryTempC >= 38f) {
                Text(
                    text = stringResource(R.string.home_temp_elevated),
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentOrange
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status breakdown rows
            val breakdownItems = listOf(
                BreakdownItem(
                    label = stringResource(R.string.home_battery_card),
                    value = formatPercent(state.healthScore.batteryScore),
                    status = HealthScore.statusFromScore(state.healthScore.batteryScore),
                    onClick = onNavigateToBattery
                ),
                BreakdownItem(
                    label = stringResource(R.string.home_thermal_card),
                    value = formatPercent(state.healthScore.thermalScore),
                    status = HealthScore.statusFromScore(state.healthScore.thermalScore),
                    onClick = onNavigateToThermal
                ),
                BreakdownItem(
                    label = stringResource(R.string.home_network_card),
                    value = formatPercent(state.healthScore.networkScore),
                    status = HealthScore.statusFromScore(state.healthScore.networkScore),
                    onClick = onNavigateToNetwork
                ),
                BreakdownItem(
                    label = stringResource(R.string.home_storage_card),
                    value = formatPercent(state.healthScore.storageScore),
                    status = HealthScore.statusFromScore(state.healthScore.storageScore),
                    onClick = onNavigateToStorage
                )
            )

            breakdownItems.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(color = BgIconCircle, thickness = 1.dp)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = item.onClick)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusDot(color = statusColor(item.status))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = MaterialTheme.numericFontFamily
                        ),
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

private data class BreakdownItem(
    val label: String,
    val value: String,
    val status: HealthStatus,
    val onClick: () -> Unit
)

// ── Battery Hero Card ───────────────────────────────────────────────────────

@Composable
private fun BatteryHeroCard(
    state: HomeUiState.Success,
    onClick: () -> Unit
) {
    val battery = state.batteryState

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    SectionHeader(stringResource(R.string.home_battery_card))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = battery.level.toString(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = MaterialTheme.numericFontFamily,
                                fontSize = 54.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "%",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = MaterialTheme.numericFontFamily
                            ),
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 2.dp, bottom = 11.dp)
                        )
                    }
                    Text(
                        text = formatEnumName(battery.chargingStatus.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                HomeBatteryChargeIcon(
                    level = battery.level,
                    isCharging = battery.chargingStatus == com.devicepulse.domain.model.ChargingStatus.CHARGING,
                    progress = battery.level / 100f,
                    modifier = Modifier.size(84.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom row: Health + Charger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeMetricPill(
                    label = stringResource(R.string.battery_health),
                    value = formatEnumName(battery.health.name),
                    valueColor = AccentTeal,
                    modifier = Modifier.weight(1f)
                )
                HomeMetricPill(
                    label = stringResource(R.string.battery_plug_type),
                    value = formatPlugTypeName(battery.plugType.name),
                    valueColor = AccentBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HomeBatteryChargeIcon(
    level: Int,
    isCharging: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val fillLevel = progress.coerceIn(0f, 1f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 78.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 16.dp, height = 5.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(1.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(top = 4.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(fillLevel)
                        .background(
                            color = AccentBlue,
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }
            Text(
                text = "$level",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = MaterialTheme.numericFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = if (fillLevel > 0.55f) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun HomeMetricPill(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor
        )
    }
}

// ── Shared Utilities ────────────────────────────────────────────────────────

@Composable
private fun statusColor(status: HealthStatus): Color {
    val colors = MaterialTheme.statusColors
    return when (status) {
        HealthStatus.HEALTHY -> colors.healthy
        HealthStatus.FAIR -> colors.fair
        HealthStatus.POOR -> colors.poor
        HealthStatus.CRITICAL -> colors.critical
    }
}

@Composable
private fun thermalLabel(tempC: Float): String = when {
    tempC < 35f -> stringResource(R.string.thermal_cool)
    tempC < 40f -> stringResource(R.string.thermal_warm)
    else -> stringResource(R.string.thermal_hot)
}

private fun formatEnumName(name: String): String =
    name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun formatPlugTypeName(name: String): String = when (name.uppercase()) {
    "USB" -> "USB"
    "AC" -> "AC"
    else -> formatEnumName(name)
}
