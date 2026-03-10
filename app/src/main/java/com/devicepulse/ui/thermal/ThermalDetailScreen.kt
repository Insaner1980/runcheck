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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.devicepulse.data.db.entity.ThrottlingEventEntity
import com.devicepulse.domain.model.ThermalStatus
import com.devicepulse.ui.components.HeatStrip
import com.devicepulse.ui.components.MetricTile
import com.devicepulse.ui.components.AdBanner
import com.devicepulse.ui.components.PullToRefreshWrapper
import com.devicepulse.ui.theme.spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ThermalDetailScreen(
    viewModel: ThermalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is ThermalUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ThermalUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.error_generic))
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
        is ThermalUiState.Success -> {
            ThermalContent(state = state, onRefresh = { viewModel.refresh() })
        }
    }
}

@Composable
private fun ThermalContent(
    state: ThermalUiState.Success,
    onRefresh: () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val thermal = state.thermalState

    PullToRefreshWrapper(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefresh()
            isRefreshing = false
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            Text(
                text = stringResource(R.string.thermal_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            HeatStrip(temperatureC = thermal.batteryTempC)

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            MetricTile(
                label = stringResource(R.string.thermal_battery_temp),
                value = "%.1f".format(thermal.batteryTempC),
                unit = stringResource(R.string.unit_celsius)
            )

            thermal.cpuTempC?.let { cpuTemp ->
                MetricTile(
                    label = stringResource(R.string.thermal_cpu_temp),
                    value = "%.1f".format(cpuTemp),
                    unit = stringResource(R.string.unit_celsius)
                )
            }

            thermal.thermalHeadroom?.let { headroom ->
                MetricTile(
                    label = stringResource(R.string.thermal_headroom),
                    value = "%.0f".format((1f - headroom.coerceIn(0f, 1f)) * 100),
                    unit = "%"
                )
            }

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

            MetricTile(
                label = stringResource(R.string.thermal_throttling),
                value = if (thermal.isThrottling) {
                    stringResource(R.string.thermal_throttling_active)
                } else {
                    stringResource(R.string.thermal_throttling_none)
                }
            )

            if (state.isPro) {
                ThrottlingLogSection(events = state.throttlingEvents)
            }

            AdBanner()

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

@Composable
private fun ThrottlingLogSection(events: List<ThrottlingEventEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        Text(
            text = stringResource(R.string.thermal_throttling_log),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (events.isEmpty()) {
            Text(
                text = stringResource(R.string.thermal_no_events),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            events.forEach { event ->
                ThrottlingEventItem(event = event)
            }
        }
    }
}

@Composable
private fun ThrottlingEventItem(event: ThrottlingEventEntity) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val formattedTime = remember(event.timestamp) {
        dateFormat.format(Date(event.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    text = "%.1f${stringResource(R.string.unit_celsius)}".format(event.batteryTempC),
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
