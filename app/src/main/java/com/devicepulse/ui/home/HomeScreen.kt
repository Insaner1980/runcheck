package com.devicepulse.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.R
import com.devicepulse.domain.model.BatteryHealth
import com.devicepulse.domain.model.BatteryState
import com.devicepulse.domain.model.ChargingStatus
import com.devicepulse.domain.model.Confidence
import com.devicepulse.domain.model.ConnectionType
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
import com.devicepulse.ui.common.formatTemperature
import com.devicepulse.ui.common.scoreLabel
import com.devicepulse.ui.components.AnimatedIntText
import com.devicepulse.ui.components.PrimaryTopBar
import com.devicepulse.ui.theme.DevicePulseTheme
import com.devicepulse.ui.theme.reducedMotion
import com.devicepulse.ui.theme.statusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToHealth: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMore: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            PrimaryTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.more_settings),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(R.string.error_generic), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { viewModel.refresh() }) { Text(stringResource(R.string.retry)) }
                    }
                }
            }

            is HomeUiState.Success -> {
                HomeContent(
                    state = state, innerPadding = innerPadding,
                    onNavigateToHealth = onNavigateToHealth,
                    onNavigateToNetwork = onNavigateToNetwork,
                    onNavigateToMore = onNavigateToMore
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    innerPadding: PaddingValues,
    onNavigateToHealth: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToMore: () -> Unit
) {
    var cardsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cardsVisible = true }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        val verticalGap = 10.dp
        val contentPaddingTop = 2.dp
        val contentPaddingBottom = 12.dp
        val totalGap = verticalGap * 3
        val availableHeight = maxHeight - contentPaddingTop - contentPaddingBottom - totalGap

        val bannerHeight = (availableHeight * 0.12f).coerceIn(64.dp, 82.dp)
        val utilityRowHeight = (availableHeight * 0.20f).coerceIn(132.dp, 148.dp)
        val primaryRowHeight = ((availableHeight - bannerHeight - utilityRowHeight) / 2f).coerceIn(168.dp, 196.dp)
        val shouldScroll = maxHeight < 620.dp
        val contentModifier = if (shouldScroll) {
            Modifier.verticalScroll(rememberScrollState())
        } else {
            Modifier
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = contentPaddingTop, bottom = contentPaddingBottom)
                .then(contentModifier),
            verticalArrangement = Arrangement.spacedBy(verticalGap)
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(primaryRowHeight), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedGridCard(visible = cardsVisible, index = 0, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    HealthCard(score = state.healthScore.overallScore, status = state.healthScore.status, onClick = onNavigateToHealth)
                }
                AnimatedGridCard(visible = cardsVisible, index = 1, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    BatteryCard(level = state.batteryState.level, chargingStatus = formatEnumName(state.batteryState.chargingStatus.name),
                        onClick = onNavigateToHealth)
                }
            }
            Row(modifier = Modifier.fillMaxWidth().height(primaryRowHeight), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedGridCard(visible = cardsVisible, index = 2, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NetworkCard(
                        connectionType = state.networkState.connectionType, networkSubtype = state.networkState.networkSubtype,
                        wifiSsid = state.networkState.wifiSsid, signalDbm = state.networkState.signalDbm,
                        subtitle = formatEnumName(state.networkState.signalQuality.name),
                        status = HealthScore.statusFromScore(state.healthScore.networkScore), onClick = onNavigateToNetwork)
                }
                AnimatedGridCard(visible = cardsVisible, index = 3, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    ThermalCard(tempC = state.thermalState.batteryTempC,
                        status = HealthScore.statusFromScore(state.healthScore.thermalScore), onClick = onNavigateToHealth)
                }
            }
            Row(modifier = Modifier.fillMaxWidth().height(utilityRowHeight), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedGridCard(visible = cardsVisible, index = 4, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (state.isPro) MetricCard(
                        label = stringResource(R.string.home_chargers_card),
                        heroValue = "---",
                        icon = Icons.Outlined.BatteryChargingFull,
                        onClick = onNavigateToMore
                    )
                    else LockedProCard(
                        icon = Icons.Outlined.BatteryChargingFull,
                        title = stringResource(R.string.home_chargers_card),
                        description = stringResource(R.string.home_chargers_desc),
                        onClick = onNavigateToMore
                    )
                }
                AnimatedGridCard(visible = cardsVisible, index = 5, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (state.isPro) MetricCard(
                        label = stringResource(R.string.home_app_usage_card),
                        heroValue = "---",
                        icon = Icons.Outlined.DataUsage,
                        onClick = onNavigateToMore
                    )
                    else LockedProCard(
                        icon = Icons.Outlined.DataUsage,
                        title = stringResource(R.string.home_app_usage_card),
                        description = stringResource(R.string.home_app_usage_desc),
                        onClick = onNavigateToMore
                    )
                }
            }
            AnimatedGridCard(
                visible = cardsVisible,
                index = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight)
            ) {
                if (state.isPro) InsightsCard(modifier = Modifier.fillMaxHeight()) else ProPromotionCard(
                    onClick = onNavigateToMore,
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}

// ── Card Composables ─────────────────────────────────────────────────────
// All cards follow the same vertical structure:
//   Top:    category label (16sp medium)
//   Center: hero content (gauge or text, weight 1f, centered)
//   Bottom: status indicator (12sp)

@Composable
private fun HealthCard(score: Int, status: HealthStatus, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accentColor = statusColor(status)
    val statusLabel = scoreLabel(score)
    PressableCard(onClick = onClick, modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CardHeader(
                title = stringResource(R.string.home_dashboard_card),
                icon = Icons.Outlined.Favorite
            )
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                FullRingGauge(
                    fraction = score / 100f,
                    label = formatPercent(score),
                    animatedValue = score,
                    animatedSuffix = "%",
                    gaugeSize = 108.dp
                )
            }
            StatusText(
                label = statusLabel,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun BatteryCard(level: Int, chargingStatus: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PressableCard(onClick = onClick, modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CardHeader(
                title = stringResource(R.string.home_battery_card),
                icon = Icons.Outlined.BatteryChargingFull
            )
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                FullRingGauge(
                    fraction = level / 100f,
                    label = formatPercent(level),
                    animatedValue = level,
                    animatedSuffix = "%",
                    gaugeSize = 108.dp
                )
            }
            Text(
                text = chargingStatus,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NetworkCard(
    connectionType: ConnectionType, networkSubtype: String?, wifiSsid: String?,
    signalDbm: Int?, subtitle: String, status: HealthStatus, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val accentColor = statusColor(status)
    val heroValue = connectionDisplayLabel(
        connectionType = connectionType,
        wifiSsid = wifiSsid,
        networkSubtype = networkSubtype
    )

    PressableCard(onClick = onClick, modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CardHeader(
                title = stringResource(R.string.home_network_card),
                icon = Icons.Outlined.SignalCellularAlt
            )
            // Center: signal bars + hero value stacked
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                signalDbm?.let { dbm ->
                    BigSignalBars(dbm = dbm)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = heroValue,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            StatusText(
                label = subtitle,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun ThermalCard(tempC: Float, status: HealthStatus, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accentColor = statusColor(status)
    PressableCard(onClick = onClick, modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CardHeader(
                title = stringResource(R.string.home_thermal_card),
                icon = Icons.Outlined.Thermostat
            )
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatTemperature(tempC),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accentColor))
                Text(
                    text = when {
                        tempC < 35f -> stringResource(R.string.thermal_cool)
                        tempC < 40f -> stringResource(R.string.thermal_warm)
                        else -> stringResource(R.string.thermal_hot)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Generic / Locked / Pro Cards ──────────────────────────────────────────

@Composable
private fun MetricCard(
    label: String,
    heroValue: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PressableCard(onClick = onClick, modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            CardHeader(title = label, icon = icon)
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = heroValue, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun CardHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun LockedProCard(
    icon: ImageVector, title: String, description: String,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                    modifier = Modifier.size(56.dp)
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun ProPromotionCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_pro_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.home_pro_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun InsightsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_insights_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Visual Components ─────────────────────────────────────────────────────

@Composable
private fun FullRingGauge(
    fraction: Float,
    label: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    animatedValue: Int? = null,
    animatedSuffix: String = "",
    gaugeSize: Dp = 120.dp,
    outerWidth: Dp = 3.dp,
    progressWidth: Dp = 8.dp
) {
    val reducedMotion = MaterialTheme.reducedMotion
    val accentColor = MaterialTheme.colorScheme.primary
    val trackColor = accentColor.copy(alpha = 0.28f)
    val innerColor = accentColor.copy(alpha = 0.16f)
    val progressBrush = rememberGaugeSweepBrush(accentColor)
    val innerBrush = rememberGaugeSweepBrush(innerColor)
    val targetSweep = fraction * 360f
    val sweepAngle by animateFloatAsState(
        targetValue = targetSweep,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = 0)
        } else {
            tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        },
        label = "home_full_ring_gauge"
    )

    Box(modifier = modifier.requiredSize(gaugeSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerStroke = outerWidth.toPx()
            val progressStroke = progressWidth.toPx()

            // Positions
            val progressInset = progressStroke / 2
            val progressArcSize = Size(size.width - progressInset * 2, size.height - progressInset * 2)
            val progressTopLeft = Offset(progressInset, progressInset)

            val innerInset = progressStroke + outerStroke / 2
            val innerArcSize = Size(size.width - innerInset * 2, size.height - innerInset * 2)
            val innerTopLeft = Offset(innerInset, innerInset)

            // Neutral tracks — full circle on both rings
            drawArc(
                color = trackColor, startAngle = -90f, sweepAngle = 360f,
                useCenter = false, topLeft = progressTopLeft, size = progressArcSize,
                style = Stroke(width = progressStroke, cap = StrokeCap.Butt)
            )
            drawArc(
                color = trackColor, startAngle = -90f, sweepAngle = 360f,
                useCenter = false, topLeft = innerTopLeft, size = innerArcSize,
                style = Stroke(width = outerStroke, cap = StrokeCap.Butt)
            )

            // Accent progress ring
            drawArc(
                brush = progressBrush, startAngle = -90f, sweepAngle = sweepAngle,
                useCenter = false, topLeft = progressTopLeft, size = progressArcSize,
                style = Stroke(width = progressStroke, cap = StrokeCap.Round)
            )

            // Subtle inner accent
            drawArc(
                brush = innerBrush, startAngle = -90f, sweepAngle = sweepAngle,
                useCenter = false, topLeft = innerTopLeft, size = innerArcSize,
                style = Stroke(width = outerStroke, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (animatedValue != null) {
                AnimatedIntText(
                    value = animatedValue,
                    suffix = animatedSuffix,
                    style = MaterialTheme.typography.displayLarge
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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

@Composable
private fun BigSignalBars(dbm: Int, modifier: Modifier = Modifier) {
    val bars = when {
        dbm > -70 -> 4
        dbm > -85 -> 3
        dbm > -100 -> 2
        dbm > -110 -> 1
        else -> 0
    }
    val activeColor = MaterialTheme.colorScheme.primary
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until 4) {
            val barHeight = (28 + i * 16).dp
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (i < bars) activeColor else dimColor)
            )
        }
    }
}

@Composable
private fun StatusChip(status: HealthStatus, accentColor: Color, modifier: Modifier = Modifier) {
    val label = when (status) {
        HealthStatus.HEALTHY -> stringResource(R.string.status_healthy)
        HealthStatus.FAIR -> stringResource(R.string.status_fair)
        HealthStatus.POOR -> stringResource(R.string.status_poor)
        HealthStatus.CRITICAL -> stringResource(R.string.status_critical)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accentColor
        )
    }
}

@Composable
private fun StatusText(label: String, accentColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Shared Utilities ──────────────────────────────────────────────────────

@Composable
private fun PressableCard(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "card_press"
    )
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().scale(scale),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) { content() }
}

@Composable
private fun AnimatedGridCard(visible: Boolean, index: Int, modifier: Modifier = Modifier, extraDelay: Int = 0, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible, modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = index * 28 + extraDelay)) +
                slideInVertically(animationSpec = tween(durationMillis = 220, delayMillis = index * 28 + extraDelay), initialOffsetY = { it / 8 })
    ) { content() }
}

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

private fun formatEnumName(name: String): String =
    name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun HomeContentPreview() {
    DevicePulseTheme {
        HomeContent(
            state = HomeUiState.Success(
                healthScore = HealthScore(
                    overallScore = 88,
                    batteryScore = 81,
                    networkScore = 76,
                    thermalScore = 90,
                    storageScore = 69,
                    status = HealthStatus.HEALTHY
                ),
                batteryState = BatteryState(
                    level = 87,
                    voltageMv = 4110,
                    temperatureC = 32.8f,
                    currentMa = MeasuredValue(-315, Confidence.HIGH),
                    chargingStatus = ChargingStatus.DISCHARGING,
                    plugType = PlugType.NONE,
                    health = BatteryHealth.GOOD,
                    technology = "Li-ion",
                    healthPercent = 94
                ),
                networkState = NetworkState(
                    connectionType = ConnectionType.WIFI,
                    signalDbm = -58,
                    signalQuality = SignalQuality.EXCELLENT,
                    wifiSsid = "DevicePulse Lab",
                    latencyMs = 22
                ),
                thermalState = ThermalState(
                    batteryTempC = 33.1f,
                    cpuTempC = 39.4f,
                    thermalHeadroom = 0.76f,
                    thermalStatus = ThermalStatus.NONE,
                    isThrottling = false
                ),
                storageState = StorageState(
                    totalBytes = 128L * 1024 * 1024 * 1024,
                    availableBytes = 42L * 1024 * 1024 * 1024,
                    usedBytes = 86L * 1024 * 1024 * 1024,
                    usagePercent = 67.2f
                ),
                isPro = false
            ),
            innerPadding = PaddingValues(),
            onNavigateToHealth = {},
            onNavigateToNetwork = {},
            onNavigateToMore = {}
        )
    }
}
