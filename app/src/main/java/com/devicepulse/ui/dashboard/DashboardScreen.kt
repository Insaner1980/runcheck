package com.devicepulse.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.R
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.BatteryHealth
import com.devicepulse.domain.model.BatteryState
import com.devicepulse.domain.model.ChargingStatus
import com.devicepulse.domain.model.Confidence
import com.devicepulse.domain.model.HealthScore
import com.devicepulse.domain.model.HealthStatus
import com.devicepulse.domain.model.MeasuredValue
import com.devicepulse.domain.model.NetworkState
import com.devicepulse.domain.model.PlugType
import com.devicepulse.domain.model.SignalQuality
import com.devicepulse.domain.model.StorageState
import com.devicepulse.domain.model.ThermalState
import com.devicepulse.domain.model.ThermalStatus
import com.devicepulse.ui.common.connectionDisplayLabel
import com.devicepulse.ui.common.formatPercent
import com.devicepulse.ui.common.formatStorageSize
import com.devicepulse.ui.common.formatTemperature
import com.devicepulse.ui.common.scoreLabel
import com.devicepulse.ui.components.AnimatedIntText
import com.devicepulse.ui.components.CategoryCard
import com.devicepulse.ui.components.HealthGauge
import com.devicepulse.ui.components.PrimaryTopBar
import com.devicepulse.ui.components.PullToRefreshWrapper
import com.devicepulse.ui.theme.DevicePulseTheme
import com.devicepulse.ui.theme.reducedMotion
import com.devicepulse.ui.theme.spacing
import com.devicepulse.ui.theme.statusColors

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
            Column(modifier = Modifier.fillMaxSize()) {
                PrimaryTopBar(title = stringResource(R.string.dashboard_title))
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
    val context = LocalContext.current

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.base),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            HealthGauge(
                score = state.healthScore.overallScore
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Text(
                text = scoreLabel(state.healthScore.overallScore),
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
                modifier = Modifier.fillMaxWidth().height(172.dp),
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
                        icon = Icons.Outlined.BatteryChargingFull,
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
                        value = connectionDisplayLabel(
                            connectionType = state.networkState.connectionType,
                            wifiSsid = state.networkState.wifiSsid,
                            networkSubtype = state.networkState.networkSubtype
                        ),
                        status = HealthScore.statusFromScore(state.healthScore.networkScore),
                        icon = Icons.Outlined.SignalCellularAlt,
                        subtitle = state.networkState.signalDbm?.let { "$it ${stringResource(R.string.unit_dbm)}" },
                        statusLabel = scoreLabel(state.healthScore.networkScore),
                        onClick = onNavigateToNetwork
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth().height(172.dp),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
            ) {
                AnimatedGridCard(visible = cardsVisible, index = 2, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CategoryCard(
                        title = stringResource(R.string.dashboard_thermal_card),
                        value = formatTemperature(state.thermalState.batteryTempC),
                        status = HealthScore.statusFromScore(state.healthScore.thermalScore),
                        icon = Icons.Outlined.Thermostat,
                        subtitle = null,
                        statusLabel = scoreLabel(state.healthScore.thermalScore),
                        onClick = onNavigateToThermal
                    )
                }
                AnimatedGridCard(visible = cardsVisible, index = 3, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CategoryCard(
                        title = stringResource(R.string.dashboard_storage_card),
                        value = formatPercent(state.storageState.usagePercent),
                        status = HealthScore.statusFromScore(state.healthScore.storageScore),
                        icon = Icons.Outlined.Storage,
                        subtitle = formatStorageSize(context, state.storageState.availableBytes),
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
    val items = remember(batteryScore, networkScore, thermalScore, storageScore) {
        listOf(
            BreakdownItem(
                type = BreakdownType.BATTERY,
                icon = Icons.Outlined.BatteryChargingFull,
                score = batteryScore
            ),
            BreakdownItem(
                type = BreakdownType.NETWORK,
                icon = Icons.Outlined.SignalCellularAlt,
                score = networkScore
            ),
            BreakdownItem(
                type = BreakdownType.THERMAL,
                icon = Icons.Outlined.Thermostat,
                score = thermalScore
            ),
            BreakdownItem(
                type = BreakdownType.STORAGE,
                icon = Icons.Outlined.Storage,
                score = storageScore
            )
        )
    }

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
                    MiniHealthCard(
                        type = item.type,
                        icon = item.icon,
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
private fun MiniHealthCard(
    type: BreakdownType,
    icon: ImageVector,
    score: Int,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.TopEnd)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                when (type) {
                    BreakdownType.BATTERY -> BatteryCellVisual(score = score)
                    BreakdownType.NETWORK -> NetworkBarsVisual(score = score)
                    BreakdownType.THERMAL -> ThermalScaleVisual(score = score)
                    BreakdownType.STORAGE -> StorageSegmentsVisual(score = score)
                }
            }
        }
    }
}

@Composable
private fun BatteryCellVisual(score: Int) {
    val reducedMotion = MaterialTheme.reducedMotion
    val accentColor = MaterialTheme.colorScheme.primary
    val fillTarget = score.coerceIn(0, 100) / 100f
    var animTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(score) { animTarget = fillTarget }
    val animatedFill by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = if (reducedMotion) tween(0) else tween(700, easing = FastOutSlowInEasing),
        label = "battery_cell_fill"
    )

    Box(
        modifier = Modifier
            .width(70.dp)
            .height(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.dp.toPx()
            val tipWidth = 6.dp.toPx()
            val bodyWidth = size.width - tipWidth - strokeWidth
            val bodyHeight = size.height - strokeWidth
            val bodyTop = strokeWidth / 2
            val bodyLeft = strokeWidth / 2
            val corner = 10.dp.toPx()

            drawRoundRect(
                color = accentColor.copy(alpha = 0.24f),
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
                style = Stroke(width = strokeWidth)
            )
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(bodyLeft + 4.dp.toPx(), bodyTop + 4.dp.toPx()),
                size = Size((bodyWidth - 8.dp.toPx()) * animatedFill, bodyHeight - 8.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner * 0.7f, corner * 0.7f)
            )
            drawRoundRect(
                color = accentColor.copy(alpha = 0.8f),
                topLeft = Offset(bodyLeft + bodyWidth, size.height * 0.3f),
                size = Size(tipWidth, size.height * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NetworkBarsVisual(score: Int) {
    val reducedMotion = MaterialTheme.reducedMotion
    val accentColor = MaterialTheme.colorScheme.primary
    val activeBarsTarget = when {
        score >= 85 -> 4f
        score >= 65 -> 3f
        score >= 45 -> 2f
        score >= 20 -> 1f
        else -> 0f
    }
    var animTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(score) { animTarget = activeBarsTarget }
    val animatedBars by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = if (reducedMotion) tween(0) else tween(650, easing = FastOutSlowInEasing),
        label = "network_bar_count"
    )
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            repeat(4) { index ->
                val barHeight = (20 + index * 12).dp
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (animatedBars > index + 0.15f) accentColor else dimColor)
                )
            }
        }
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ThermalScaleVisual(score: Int) {
    val reducedMotion = MaterialTheme.reducedMotion
    val progressTarget = score.coerceIn(0, 100) / 100f
    var animTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(score) { animTarget = progressTarget }
    val animatedProgress by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = if (reducedMotion) tween(0) else tween(700, easing = FastOutSlowInEasing),
        label = "thermal_scale_progress"
    )
    val statusColors = MaterialTheme.statusColors
    val gradient = Brush.horizontalGradient(
        listOf(
            statusColors.healthy,
            statusColors.fair,
            statusColors.poor,
            statusColors.critical
        )
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .widthIn(min = 76.dp)
                .height(18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(gradient)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = (68.dp * animatedProgress.coerceIn(0f, 1f)))
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceBright)
            )
        }
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StorageSegmentsVisual(score: Int) {
    val reducedMotion = MaterialTheme.reducedMotion
    val filledSegmentsTarget = when {
        score >= 88 -> 5f
        score >= 70 -> 4f
        score >= 50 -> 3f
        score >= 30 -> 2f
        score >= 10 -> 1f
        else -> 0f
    }
    var animTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(score) { animTarget = filledSegmentsTarget }
    val animatedSegments by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = if (reducedMotion) tween(0) else tween(650, easing = FastOutSlowInEasing),
        label = "storage_segment_count"
    )
    val accentColor = MaterialTheme.colorScheme.primary
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .width(11.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (animatedSegments > index + 0.15f) accentColor else dimColor)
                )
            }
        }
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private enum class BreakdownType {
    BATTERY,
    NETWORK,
    THERMAL,
    STORAGE
}

private data class BreakdownItem(
    val type: BreakdownType,
    val icon: ImageVector,
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
            animationSpec = tween(durationMillis = 220, delayMillis = index * 32)
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 220, delayMillis = index * 32),
            initialOffsetY = { it / 8 }
        )
    ) {
        content()
    }
}

@Composable
private fun rememberGaugeSweepBrush(baseColor: Color): Brush {
    val seamStart = lerp(baseColor, Color.White, 0.02f)
    val highlight = lerp(baseColor, Color.White, 0.05f)
    val middle = baseColor
    val shadow = lerp(baseColor, Color.Black, 0.03f)
    val seamEnd = lerp(baseColor, Color.White, 0.018f)
    return Brush.sweepGradient(
        0.0f to seamStart,
        0.18f to highlight,
        0.52f to middle,
        0.82f to shadow,
        1.0f to seamEnd
    )
}

private fun formatEnumName(name: String): String =
    name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

@Composable
private fun batteryDashboardValue(
    healthPercent: Int?,
    chargingStatusName: String
): String = healthPercent?.let(::formatPercent) ?: formatEnumName(chargingStatusName)

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

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun DashboardContentPreview() {
    DevicePulseTheme {
        DashboardContent(
            state = DashboardUiState.Success(
                healthScore = HealthScore(
                    overallScore = 86,
                    batteryScore = 82,
                    networkScore = 78,
                    thermalScore = 91,
                    storageScore = 73,
                    status = HealthStatus.HEALTHY
                ),
                batteryState = BatteryState(
                    level = 84,
                    voltageMv = 4021,
                    temperatureC = 33.4f,
                    currentMa = MeasuredValue(-420, Confidence.HIGH),
                    chargingStatus = ChargingStatus.DISCHARGING,
                    plugType = PlugType.NONE,
                    health = BatteryHealth.GOOD,
                    technology = "Li-ion",
                    healthPercent = 93
                ),
                networkState = NetworkState(
                    connectionType = ConnectionType.WIFI,
                    signalDbm = -61,
                    signalQuality = SignalQuality.EXCELLENT,
                    wifiSsid = "Office Wi-Fi",
                    latencyMs = 18
                ),
                thermalState = ThermalState(
                    batteryTempC = 34.1f,
                    cpuTempC = 40.3f,
                    thermalHeadroom = 0.72f,
                    thermalStatus = ThermalStatus.NONE,
                    isThrottling = false
                ),
                storageState = StorageState(
                    totalBytes = 256L * 1024 * 1024 * 1024,
                    availableBytes = 104L * 1024 * 1024 * 1024,
                    usedBytes = 152L * 1024 * 1024 * 1024,
                    usagePercent = 59.4f
                )
            ),
            onRefresh = {},
            onNavigateToBattery = {},
            onNavigateToNetwork = {},
            onNavigateToThermal = {},
            onNavigateToStorage = {}
        )
    }
}
