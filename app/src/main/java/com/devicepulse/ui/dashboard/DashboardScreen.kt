package com.devicepulse.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.R
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.BatteryHealth
import com.devicepulse.domain.model.HealthScore
import com.devicepulse.domain.model.HealthStatus
import com.devicepulse.ui.components.CategoryCard
import com.devicepulse.ui.components.HealthGauge
import com.devicepulse.ui.components.PullToRefreshWrapper
import com.devicepulse.ui.theme.spacing

@Composable
fun DashboardScreen(
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is DashboardUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is DashboardUiState.Error -> {
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

        is DashboardUiState.Success -> {
            DashboardContent(
                state = state,
                onRefresh = { viewModel.refresh() },
                onNavigateToBattery = onNavigateToBattery,
                onNavigateToNetwork = onNavigateToNetwork,
                onNavigateToThermal = onNavigateToThermal,
                onNavigateToStorage = onNavigateToStorage
            )
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState.Success,
    onRefresh: () -> Unit,
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    var cardsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cardsVisible = true
    }

    LaunchedEffect(state) {
        isRefreshing = false
    }

    PullToRefreshWrapper(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefresh()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.base),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Text(
                text = stringResource(R.string.dashboard_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            HealthGauge(
                score = state.healthScore.overallScore
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Text(
                text = when {
                    state.healthScore.overallScore >= 90 -> stringResource(R.string.score_excellent)
                    state.healthScore.overallScore >= 70 -> stringResource(R.string.score_good)
                    state.healthScore.overallScore >= 50 -> stringResource(R.string.score_fair)
                    else -> stringResource(R.string.score_poor)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            // Mini score breakdown
            ScoreBreakdown(
                batteryScore = state.healthScore.batteryScore,
                networkScore = state.healthScore.networkScore,
                thermalScore = state.healthScore.thermalScore,
                storageScore = state.healthScore.storageScore
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            // 2x2 grid
            Row(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
            ) {
                AnimatedGridCard(visible = cardsVisible, index = 0, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CategoryCard(
                        title = stringResource(R.string.dashboard_battery_card),
                        value = batteryDashboardValue(
                            healthPercent = state.batteryState.healthPercent,
                            chargingStatusName = state.batteryState.chargingStatus.name
                        ),
                        status = batteryDashboardStatus(
                            healthPercent = state.batteryState.healthPercent,
                            health = state.batteryState.health
                        ),
                        icon = Icons.Default.BatteryChargingFull,
                        subtitle = batteryDashboardSubtitle(
                            healthPercent = state.batteryState.healthPercent,
                            chargingStatusName = state.batteryState.chargingStatus.name
                        ),
                        statusLabel = batteryHealthLabel(state.batteryState.health.name),
                        onClick = onNavigateToBattery
                    )
                }
                AnimatedGridCard(visible = cardsVisible, index = 1, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CategoryCard(
                        title = stringResource(R.string.dashboard_network_card),
                        value = when (state.networkState.connectionType) {
                            ConnectionType.WIFI -> state.networkState.wifiSsid ?: "WiFi"
                            ConnectionType.CELLULAR -> state.networkState.networkSubtype ?: "Cellular"
                            ConnectionType.NONE -> stringResource(R.string.network_no_connection)
                        },
                        status = HealthScore.statusFromScore(state.healthScore.networkScore),
                        icon = Icons.Default.SignalCellularAlt,
                        subtitle = null,
                        statusLabel = scoreLabel(state.healthScore.networkScore),
                        onClick = onNavigateToNetwork
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
            ) {
                AnimatedGridCard(visible = cardsVisible, index = 2, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CategoryCard(
                        title = stringResource(R.string.dashboard_thermal_card),
                        value = "${"%.1f".format(state.thermalState.batteryTempC)}°C",
                        status = HealthScore.statusFromScore(state.healthScore.thermalScore),
                        icon = Icons.Default.Thermostat,
                        subtitle = null,
                        statusLabel = scoreLabel(state.healthScore.thermalScore),
                        onClick = onNavigateToThermal
                    )
                }
                AnimatedGridCard(visible = cardsVisible, index = 3, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CategoryCard(
                        title = stringResource(R.string.dashboard_storage_card),
                        value = "${"%.1f".format(state.storageState.usagePercent)}%",
                        status = HealthScore.statusFromScore(state.healthScore.storageScore),
                        icon = Icons.Default.Memory,
                        subtitle = formatBytes(state.storageState.availableBytes),
                        statusLabel = scoreLabel(state.healthScore.storageScore),
                        onClick = onNavigateToStorage
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

@Composable
private fun ScoreBreakdown(
    batteryScore: Int,
    networkScore: Int,
    thermalScore: Int,
    storageScore: Int,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BreakdownItem(
            icon = Icons.Default.BatteryChargingFull,
            label = stringResource(R.string.dashboard_battery_card),
            score = batteryScore
        ),
        BreakdownItem(
            icon = Icons.Default.SignalCellularAlt,
            label = stringResource(R.string.dashboard_network_card),
            score = networkScore
        ),
        BreakdownItem(
            icon = Icons.Default.Thermostat,
            label = stringResource(R.string.dashboard_thermal_card),
            score = thermalScore
        ),
        BreakdownItem(
            icon = Icons.Default.Memory,
            label = stringResource(R.string.dashboard_storage_card),
            score = storageScore
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
            ) {
                rowItems.forEach { item ->
                    MiniRingScore(
                        icon = item.icon,
                        label = item.label,
                        score = item.score,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniRingScore(
    icon: ImageVector,
    label: String,
    score: Int,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val trackColor = accentColor.copy(alpha = 0.28f)
    val innerColor = accentColor.copy(alpha = 0.16f)
    val targetSweep = (score / 100f) * 360f

    var animTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(score) { animTarget = targetSweep }
    val animatedSweep by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = tween(durationMillis = 550, delayMillis = 120),
        label = "mini_ring_score"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(72.dp)) {
                val progressStroke = 7.dp.toPx()
                val outerStroke = 3.dp.toPx()

                val progressInset = progressStroke / 2
                val progressArcSize = Size(size.width - progressInset * 2, size.height - progressInset * 2)
                val progressTopLeft = Offset(progressInset, progressInset)

                val innerInset = progressStroke + outerStroke / 2
                val innerArcSize = Size(size.width - innerInset * 2, size.height - innerInset * 2)
                val innerTopLeft = Offset(innerInset, innerInset)

                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = progressTopLeft,
                    size = progressArcSize,
                    style = Stroke(width = progressStroke, cap = StrokeCap.Butt)
                )

                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerArcSize,
                    style = Stroke(width = outerStroke, cap = StrokeCap.Butt)
                )

                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = progressTopLeft,
                    size = progressArcSize,
                    style = Stroke(width = progressStroke, cap = StrokeCap.Round)
                )

                drawArc(
                    color = innerColor,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerArcSize,
                    style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                )
            }

            Text(
                text = "$score",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private data class BreakdownItem(
    val icon: ImageVector,
    val label: String,
    val score: Int
)

@Composable
private fun AnimatedGridCard(
    visible: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 300, delayMillis = index * 60)
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 300, delayMillis = index * 60),
            initialOffsetY = { it / 4 }
        )
    ) {
        content()
    }
}

@Composable
private fun scoreLabel(score: Int): String = when {
    score >= 90 -> stringResource(R.string.score_excellent)
    score >= 70 -> stringResource(R.string.score_good)
    score >= 50 -> stringResource(R.string.score_fair)
    else -> stringResource(R.string.score_poor)
}

private fun formatEnumName(name: String): String =
    name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

@Composable
private fun batteryDashboardValue(
    healthPercent: Int?,
    chargingStatusName: String
): String = healthPercent?.let { "$it%" } ?: formatEnumName(chargingStatusName)

@Composable
private fun batteryDashboardSubtitle(
    healthPercent: Int?,
    chargingStatusName: String
): String = if (healthPercent != null) {
    stringResource(R.string.battery_health)
} else {
    stringResource(R.string.battery_health_unavailable_short)
}

@Composable
private fun batteryHealthLabel(healthName: String): String = when (healthName) {
    "GOOD" -> stringResource(R.string.battery_health_good)
    "OVERHEAT" -> stringResource(R.string.battery_health_overheat)
    "DEAD" -> stringResource(R.string.battery_health_dead)
    "OVER_VOLTAGE" -> stringResource(R.string.battery_health_over_voltage)
    "COLD" -> stringResource(R.string.battery_health_cold)
    else -> stringResource(R.string.battery_health_unknown)
}

private fun batteryDashboardStatus(
    healthPercent: Int?,
    health: BatteryHealth
): HealthStatus = when {
    health == BatteryHealth.OVERHEAT -> HealthStatus.CRITICAL
    health == BatteryHealth.DEAD -> HealthStatus.CRITICAL
    health == BatteryHealth.OVER_VOLTAGE -> HealthStatus.POOR
    health == BatteryHealth.COLD -> HealthStatus.FAIR
    healthPercent != null && healthPercent < 60 -> HealthStatus.POOR
    healthPercent != null && healthPercent < 75 -> HealthStatus.FAIR
    health == BatteryHealth.UNKNOWN -> HealthStatus.FAIR
    else -> HealthStatus.HEALTHY
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1.0) {
        "${"%.1f".format(gb)} GB"
    } else {
        val mb = bytes / (1024.0 * 1024.0)
        "${"%.0f".format(mb)} MB"
    }
}
