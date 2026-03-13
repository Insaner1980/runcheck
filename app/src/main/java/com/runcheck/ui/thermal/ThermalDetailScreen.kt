package com.devicepulse.ui.thermal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.devicepulse.R
import com.devicepulse.domain.model.ThrottlingEvent
import com.devicepulse.domain.model.ThermalStatus
import com.devicepulse.ui.common.formatDecimal
import com.devicepulse.ui.common.formatTemperature
import com.devicepulse.ui.common.rememberFormattedDateTime
import com.devicepulse.ui.components.ProFeatureCalloutCard
import com.devicepulse.ui.components.DetailTopBar
import com.devicepulse.ui.components.HeatStrip
import com.devicepulse.ui.components.MetricTile
import com.devicepulse.ui.components.PullToRefreshWrapper
import com.devicepulse.ui.theme.spacing

@Composable
fun ThermalDetailScreen(
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThermalViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.startObserving()
                Lifecycle.Event.ON_STOP -> viewModel.stopObserving()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.startObserving()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopObserving()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(
            title = stringResource(R.string.thermal_title),
            onBack = onBack
        )
        when (val state = uiState) {
            is ThermalUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ThermalUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.common_error_generic))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }
            is ThermalUiState.Success -> {
                ThermalContent(
                    state = state,
                    onRefresh = { viewModel.refresh() },
                    onUpgradeToPro = onUpgradeToPro
                )
            }
        }
    }
}

@Composable
private fun ThermalContent(
    state: ThermalUiState.Success,
    onRefresh: () -> Unit,
    onUpgradeToPro: () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val thermal = state.thermalState

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            }

            item {
                HeatStrip(temperatureC = thermal.batteryTempC)
            }

            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            }

            item {
                MetricTile(
                    label = stringResource(R.string.thermal_battery_temp),
                    value = formatDecimal(thermal.batteryTempC, 1),
                    unit = stringResource(R.string.unit_celsius)
                )
            }

            thermal.cpuTempC?.let { cpuTemp ->
                item {
                    MetricTile(
                        label = stringResource(R.string.thermal_cpu_temp),
                        value = formatDecimal(cpuTemp, 1),
                        unit = stringResource(R.string.unit_celsius)
                    )
                }
            }

            thermal.thermalHeadroom?.let { headroom ->
                item {
                    MetricTile(
                        label = stringResource(R.string.thermal_headroom),
                        value = formatDecimal((1f - headroom.coerceIn(0f, 1f)) * 100, 0),
                        unit = stringResource(R.string.unit_percent)
                    )
                }
            }

            item {
                MetricTile(
                    label = stringResource(R.string.thermal_status),
                    value = when (thermal.thermalStatus) {
                        ThermalStatus.NONE -> stringResource(R.string.thermal_status_none)
                        ThermalStatus.LIGHT -> stringResource(R.string.thermal_status_light)
                        ThermalStatus.MODERATE -> stringResource(R.string.thermal_status_moderate)
                        ThermalStatus.SEVERE -> stringResource(R.string.thermal_status_severe)
                        ThermalStatus.CRITICAL -> stringResource(R.string.thermal_status_critical)
                        ThermalStatus.EMERGENCY -> stringResource(R.string.thermal_status_emergency)
                        ThermalStatus.SHUTDOWN -> stringResource(R.string.thermal_status_shutdown)
                    }
                )
            }

            item {
                MetricTile(
                    label = stringResource(R.string.thermal_throttling),
                    value = if (thermal.isThrottling) {
                        stringResource(R.string.thermal_throttling_active)
                    } else {
                        stringResource(R.string.thermal_throttling_none)
                    }
                )
            }

            if (state.isPro) {
                item {
                    Text(
                        text = stringResource(R.string.thermal_throttling_log),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (state.throttlingEvents.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.thermal_no_events),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(
                        items = state.throttlingEvents,
                        key = { event -> event.id.takeIf { it != 0L } ?: event.timestamp }
                    ) { event ->
                        ThrottlingEventItem(event = event)
                    }
                }
            } else {
                item {
                    ProFeatureCalloutCard(
                        message = stringResource(R.string.pro_feature_thermal_log_message),
                        actionLabel = stringResource(R.string.pro_feature_upgrade_action),
                        onAction = onUpgradeToPro
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
            }
        }
    }
}

@Composable
private fun ThrottlingEventItem(event: ThrottlingEvent) {
    val formattedTime = rememberFormattedDateTime(event.timestamp, "yMMMdHm")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
        ) {
            Text(
                text = stringResource(R.string.thermal_event_at, formattedTime),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.thermalStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatTemperature(event.batteryTempC),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (event.foregroundApp != null) {
                Text(
                    text = event.foregroundApp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
