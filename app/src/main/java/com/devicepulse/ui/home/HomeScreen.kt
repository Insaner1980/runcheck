package com.devicepulse.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.R
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.HealthScore
import com.devicepulse.domain.model.HealthStatus
import com.devicepulse.ui.theme.InterFontFamily
import com.devicepulse.ui.theme.JetBrainsMonoFontFamily
import com.devicepulse.ui.theme.statusColors

// Fixed text styles — uses JetBrains Mono for hero numbers, Inter for labels.
// Every hero value on the home screen uses the exact same TextStyle object.
private val HeroValueStyle = TextStyle(
    fontSize = 32.sp,
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp
)

private val CardLabelStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.1.sp
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.more_settings),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                    onNavigateToDashboard = onNavigateToDashboard,
                    onNavigateToBattery = onNavigateToBattery,
                    onNavigateToNetwork = onNavigateToNetwork,
                    onNavigateToThermal = onNavigateToThermal,
                    onNavigateToCharger = onNavigateToCharger,
                    onNavigateToAppUsage = onNavigateToAppUsage,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    innerPadding: PaddingValues,
    onNavigateToDashboard: () -> Unit,
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var cardsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cardsVisible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Health + Battery
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedGridCard(visible = cardsVisible, index = 0, modifier = Modifier.weight(1f).fillMaxHeight()) {
                HealthCard(score = state.healthScore.overallScore, status = state.healthScore.status, onClick = onNavigateToDashboard)
            }
            AnimatedGridCard(visible = cardsVisible, index = 1, modifier = Modifier.weight(1f).fillMaxHeight()) {
                BatteryCard(level = state.batteryState.level, chargingStatus = formatEnumName(state.batteryState.chargingStatus.name),
                    onClick = onNavigateToBattery)
            }
        }
        // Row 2: Network + Thermal
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedGridCard(visible = cardsVisible, index = 2, modifier = Modifier.weight(1f).fillMaxHeight()) {
                NetworkCard(
                    connectionType = state.networkState.connectionType, networkSubtype = state.networkState.networkSubtype,
                    wifiSsid = state.networkState.wifiSsid, signalDbm = state.networkState.signalDbm,
                    subtitle = formatEnumName(state.networkState.signalQuality.name),
                    status = HealthScore.statusFromScore(state.healthScore.networkScore), onClick = onNavigateToNetwork)
            }
            AnimatedGridCard(visible = cardsVisible, index = 3, modifier = Modifier.weight(1f).fillMaxHeight()) {
                ThermalCard(tempC = state.thermalState.batteryTempC,
                    status = HealthScore.statusFromScore(state.healthScore.thermalScore), onClick = onNavigateToThermal)
            }
        }
        // Row 3: Chargers + App Usage
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedGridCard(visible = cardsVisible, index = 4, modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (state.isPro) MetricCard(label = stringResource(R.string.home_chargers_card), heroValue = "---", onClick = onNavigateToCharger)
                else LockedProCard(
                    icon = Icons.Default.BatteryChargingFull,
                    title = stringResource(R.string.home_chargers_card),
                    description = stringResource(R.string.home_chargers_desc),
                    onClick = onNavigateToCharger
                )
            }
            AnimatedGridCard(visible = cardsVisible, index = 5, modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (state.isPro) MetricCard(label = stringResource(R.string.home_app_usage_card), heroValue = "---", onClick = onNavigateToAppUsage)
                else LockedProCard(
                    icon = Icons.Default.DataUsage,
                    title = stringResource(R.string.home_app_usage_card),
                    description = stringResource(R.string.home_app_usage_desc),
                    onClick = onNavigateToAppUsage
                )
            }
        }
        // Pro banner
        AnimatedGridCard(visible = cardsVisible, index = 6, extraDelay = 80, modifier = Modifier.fillMaxWidth()) {
            if (state.isPro) InsightsCard() else ProPromotionCard(onClick = onNavigateToSettings)
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
    val scoreLabel = when {
        score >= 90 -> stringResource(R.string.score_excellent)
        score >= 70 -> stringResource(R.string.score_good)
        score >= 50 -> stringResource(R.string.score_fair)
        else -> stringResource(R.string.score_poor)
    }
    PressableCard(onClick = onClick, modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_dashboard_card),
                style = CardLabelStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                FullRingGauge(
                    fraction = score / 100f,
                    label = "${score}%"
                )
            }
            Text(
                text = scoreLabel,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor
            )
        }
    }
}

@Composable
private fun BatteryCard(level: Int, chargingStatus: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PressableCard(onClick = onClick, modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_battery_card),
                style = CardLabelStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                FullRingGauge(
                    fraction = level / 100f,
                    label = "${level}%"
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
    val heroValue = when (connectionType) {
        ConnectionType.WIFI -> wifiSsid ?: "WiFi"
        ConnectionType.CELLULAR -> networkSubtype ?: "4G"
        ConnectionType.NONE -> "---"
    }

    PressableCard(onClick = onClick, modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_network_card),
                style = CardLabelStyle,
                color = MaterialTheme.colorScheme.onSurface
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
                    style = HeroValueStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor
            )
        }
    }
}

@Composable
private fun ThermalCard(tempC: Float, status: HealthStatus, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accentColor = statusColor(status)
    PressableCard(onClick = onClick, modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_thermal_card),
                style = CardLabelStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${"%.1f".format(tempC)}°C",
                    style = HeroValueStyle,
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
private fun MetricCard(label: String, heroValue: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PressableCard(onClick = onClick, modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(text = label, style = CardLabelStyle, color = MaterialTheme.colorScheme.onSurface)
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = heroValue, style = HeroValueStyle, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun LockedProCard(
    icon: ImageVector, title: String, description: String,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color.White.copy(alpha = 0.02f)
            else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
        ),
        border = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)) else null
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = CardLabelStyle,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Icon(
                    imageVector = Icons.Default.Lock,
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
    gaugeSize: Dp = 120.dp,
    outerWidth: Dp = 3.dp,
    progressWidth: Dp = 8.dp
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val trackColor = accentColor.copy(alpha = 0.28f)
    val innerColor = accentColor.copy(alpha = 0.16f)
    val sweepAngle = fraction * 360f

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
                color = accentColor, startAngle = -90f, sweepAngle = sweepAngle,
                useCenter = false, topLeft = progressTopLeft, size = progressArcSize,
                style = Stroke(width = progressStroke, cap = StrokeCap.Round)
            )

            // Neutral inner border
            drawArc(
                color = innerColor, startAngle = -90f, sweepAngle = sweepAngle,
                useCenter = false, topLeft = innerTopLeft, size = innerArcSize,
                style = Stroke(width = outerStroke, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = HeroValueStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
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

// ── Shared Utilities ──────────────────────────────────────────────────────

@Composable
private fun PressableCard(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
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
            containerColor = if (isDark) Color.White.copy(alpha = 0.04f)
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null
    ) { content() }
}

@Composable
private fun AnimatedGridCard(visible: Boolean, index: Int, modifier: Modifier = Modifier, extraDelay: Int = 0, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible, modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = index * 40 + extraDelay)) +
                slideInVertically(animationSpec = tween(durationMillis = 300, delayMillis = index * 40 + extraDelay), initialOffsetY = { it / 4 })
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
