package com.devicepulse.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Thermostat
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.R
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.HealthScore
import com.devicepulse.domain.model.HealthStatus
import com.devicepulse.ui.components.StatusIndicator
import com.devicepulse.ui.theme.spacing
import com.devicepulse.ui.theme.statusColors

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
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.more_settings)
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
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
                    innerPadding = innerPadding,
                    onNavigateToDashboard = onNavigateToDashboard,
                    onNavigateToBattery = onNavigateToBattery,
                    onNavigateToNetwork = onNavigateToNetwork,
                    onNavigateToThermal = onNavigateToThermal,
                    onNavigateToCharger = onNavigateToCharger,
                    onNavigateToAppUsage = onNavigateToAppUsage
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
    onNavigateToAppUsage: () -> Unit
) {
    var cardsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cardsVisible = true
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(MaterialTheme.spacing.base),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
    ) {
        // Dashboard card
        item {
            AnimatedGridCard(visible = cardsVisible, index = 0) {
                HomeGridCard(
                    icon = Icons.Default.Dashboard,
                    title = stringResource(R.string.home_dashboard_card),
                    heroValue = state.healthScore.overallScore.toString(),
                    status = state.healthScore.status,
                    onClick = onNavigateToDashboard,
                    miniGauge = {
                        MiniArcGauge(
                            score = state.healthScore.overallScore,
                            status = state.healthScore.status
                        )
                    }
                )
            }
        }

        // Battery card
        item {
            AnimatedGridCard(visible = cardsVisible, index = 1) {
                HomeGridCard(
                    icon = Icons.Default.BatteryChargingFull,
                    title = stringResource(R.string.home_battery_card),
                    heroValue = "${state.batteryState.level}%",
                    subtitle = formatEnumName(state.batteryState.chargingStatus.name),
                    status = HealthScore.statusFromScore(state.healthScore.batteryScore),
                    onClick = onNavigateToBattery
                )
            }
        }

        // Network card
        item {
            AnimatedGridCard(visible = cardsVisible, index = 2) {
                HomeGridCard(
                    icon = Icons.Default.SignalCellularAlt,
                    title = stringResource(R.string.home_network_card),
                    heroValue = when (state.networkState.connectionType) {
                        ConnectionType.WIFI -> state.networkState.wifiSsid ?: "WiFi"
                        ConnectionType.CELLULAR -> state.networkState.networkSubtype ?: "4G"
                        ConnectionType.NONE -> "---"
                    },
                    subtitle = formatEnumName(state.networkState.signalQuality.name),
                    status = HealthScore.statusFromScore(state.healthScore.networkScore),
                    onClick = onNavigateToNetwork
                )
            }
        }

        // Thermal card
        item {
            AnimatedGridCard(visible = cardsVisible, index = 3) {
                HomeGridCard(
                    icon = Icons.Default.Thermostat,
                    title = stringResource(R.string.home_thermal_card),
                    heroValue = "${"%.1f".format(state.thermalState.batteryTempC)}°C",
                    subtitle = formatEnumName(state.thermalState.thermalStatus.name),
                    status = HealthScore.statusFromScore(state.healthScore.thermalScore),
                    onClick = onNavigateToThermal
                )
            }
        }

        // Chargers card (Pro-gated)
        item {
            AnimatedGridCard(visible = cardsVisible, index = 4) {
                if (state.isPro) {
                    HomeGridCard(
                        icon = Icons.Default.BatteryChargingFull,
                        title = stringResource(R.string.home_chargers_card),
                        heroValue = "---",
                        onClick = onNavigateToCharger
                    )
                } else {
                    LockedProCard(
                        icon = Icons.Default.BatteryChargingFull,
                        title = stringResource(R.string.home_chargers_card),
                        onClick = onNavigateToCharger
                    )
                }
            }
        }

        // App Usage card (Pro-gated)
        item {
            AnimatedGridCard(visible = cardsVisible, index = 5) {
                if (state.isPro) {
                    HomeGridCard(
                        icon = Icons.Default.DataUsage,
                        title = stringResource(R.string.home_app_usage_card),
                        heroValue = "---",
                        onClick = onNavigateToAppUsage
                    )
                } else {
                    LockedProCard(
                        icon = Icons.Default.DataUsage,
                        title = stringResource(R.string.home_app_usage_card),
                        onClick = onNavigateToAppUsage
                    )
                }
            }
        }

        // Full-width Pro / Insights card
        item(span = { GridItemSpan(2) }) {
            AnimatedGridCard(visible = cardsVisible, index = 6, extraDelay = 80) {
                if (state.isPro) {
                    InsightsCard()
                } else {
                    ProPromotionCard(onClick = onNavigateToSettings)
                }
            }
        }
    }
}

@Composable
private fun AnimatedGridCard(
    visible: Boolean,
    index: Int,
    extraDelay: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = index * 40 + extraDelay
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = index * 40 + extraDelay
            ),
            initialOffsetY = { it / 4 }
        )
    ) {
        content()
    }
}

@Composable
private fun HomeGridCard(
    icon: ImageVector,
    title: String,
    heroValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    status: HealthStatus? = null,
    miniGauge: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_press"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 120.dp)
            .scale(scale),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                status?.let {
                    StatusIndicator(status = it)
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (miniGauge != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.xs),
                    contentAlignment = Alignment.Center
                ) {
                    miniGauge()
                }
            } else {
                Text(
                    text = heroValue,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LockedProCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.home_locked_pro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProPromotionCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
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
private fun InsightsCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base)
        ) {
            Text(
                text = stringResource(R.string.home_insights_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MiniArcGauge(
    score: Int,
    status: HealthStatus,
    modifier: Modifier = Modifier
) {
    val statusColors = MaterialTheme.statusColors
    val arcColor = when (status) {
        HealthStatus.HEALTHY -> statusColors.healthy
        HealthStatus.FAIR -> statusColors.fair
        HealthStatus.POOR -> statusColors.poor
        HealthStatus.CRITICAL -> statusColors.critical
    }
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val sweepAngle = (score / 100f) * 270f

    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val stroke = 4.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)

            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            drawArc(
                color = arcColor,
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Text(
            text = score.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatEnumName(name: String): String =
    name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
