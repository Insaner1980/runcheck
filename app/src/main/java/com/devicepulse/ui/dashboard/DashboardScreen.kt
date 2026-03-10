package com.devicepulse.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.R
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.HealthScore
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.base),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

            Text(
                text = stringResource(R.string.dashboard_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            HealthGauge(
                score = state.healthScore.overallScore,
                status = state.healthScore.status
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
            ) {
                val cards = listOf(
                    CardData(
                        title = stringResource(R.string.dashboard_battery_card),
                        value = "${state.batteryState.level}%",
                        status = HealthScore.statusFromScore(state.healthScore.batteryScore),
                        subtitle = state.batteryState.chargingStatus.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        onClick = onNavigateToBattery,
                        sparkline = state.batterySparkline
                    ),
                    CardData(
                        title = stringResource(R.string.dashboard_network_card),
                        value = when (state.networkState.connectionType) {
                            ConnectionType.WIFI -> state.networkState.wifiSsid ?: "WiFi"
                            ConnectionType.CELLULAR -> state.networkState.networkSubtype ?: "Cellular"
                            ConnectionType.NONE -> stringResource(R.string.network_no_connection)
                        },
                        status = HealthScore.statusFromScore(state.healthScore.networkScore),
                        subtitle = state.networkState.signalQuality.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        onClick = onNavigateToNetwork
                    ),
                    CardData(
                        title = stringResource(R.string.dashboard_thermal_card),
                        value = "${state.thermalState.batteryTempC}°C",
                        status = HealthScore.statusFromScore(state.healthScore.thermalScore),
                        subtitle = state.thermalState.thermalStatus.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        onClick = onNavigateToThermal,
                        sparkline = state.thermalSparkline
                    ),
                    CardData(
                        title = stringResource(R.string.dashboard_storage_card),
                        value = "${"%.1f".format(state.storageState.usagePercent)}%",
                        status = HealthScore.statusFromScore(state.healthScore.storageScore),
                        subtitle = formatBytes(state.storageState.availableBytes) + " available",
                        onClick = onNavigateToStorage
                    )
                )

                cards.forEachIndexed { index, card ->
                    AnimatedVisibility(
                        visible = cardsVisible,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = 300,
                                delayMillis = index * 60
                            )
                        ) + slideInVertically(
                            animationSpec = tween(
                                durationMillis = 300,
                                delayMillis = index * 60
                            ),
                            initialOffsetY = { it / 4 }
                        )
                    ) {
                        CategoryCard(
                            title = card.title,
                            value = card.value,
                            status = card.status,
                            subtitle = card.subtitle,
                            onClick = card.onClick,
                            sparklineData = card.sparkline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

private data class CardData(
    val title: String,
    val value: String,
    val status: com.devicepulse.domain.model.HealthStatus,
    val subtitle: String?,
    val onClick: () -> Unit,
    val sparkline: List<Float> = emptyList()
)

private fun formatBytes(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1.0) {
        "${"%.1f".format(gb)} GB"
    } else {
        val mb = bytes / (1024.0 * 1024.0)
        "${"%.0f".format(mb)} MB"
    }
}
