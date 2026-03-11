package com.devicepulse.ui.battery

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
import androidx.compose.material3.FilterChip
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.R
import com.devicepulse.domain.model.Confidence
import com.devicepulse.domain.model.HistoryPeriod
import com.devicepulse.ui.components.ConfidenceBadge
import com.devicepulse.ui.components.MetricTile
import com.devicepulse.ui.components.PullToRefreshWrapper
import com.devicepulse.ui.components.AdBanner
import com.devicepulse.ui.components.TrendChart
import com.devicepulse.ui.theme.spacing

@Composable
fun BatteryDetailScreen(
    viewModel: BatteryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is BatteryUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is BatteryUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.error_generic))
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
        is BatteryUiState.Success -> {
            BatteryContent(
                state = state,
                onRefresh = { viewModel.refresh() },
                onPeriodChange = { viewModel.setHistoryPeriod(it) }
            )
        }
    }
}

@Composable
private fun BatteryContent(
    state: BatteryUiState.Success,
    onRefresh: () -> Unit,
    onPeriodChange: (HistoryPeriod) -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val battery = state.batteryState

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
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Text(
                text = stringResource(R.string.battery_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            MetricTile(
                label = stringResource(R.string.battery_level),
                value = battery.level.toString(),
                unit = stringResource(R.string.unit_percent)
            )

            MetricTile(
                label = stringResource(R.string.battery_voltage),
                value = battery.voltageMv.toString(),
                unit = stringResource(R.string.unit_millivolts)
            )

            MetricTile(
                label = stringResource(R.string.battery_temperature),
                value = "%.1f".format(battery.temperatureC),
                unit = stringResource(R.string.unit_celsius)
            )

            MetricTile(
                label = stringResource(R.string.battery_current),
                value = if (battery.currentMa.confidence != Confidence.UNAVAILABLE) {
                    battery.currentMa.value.toString()
                } else {
                    stringResource(R.string.battery_not_available)
                },
                unit = if (battery.currentMa.confidence != Confidence.UNAVAILABLE) {
                    stringResource(R.string.unit_milliamps)
                } else "",
                trailing = {
                    ConfidenceBadge(confidence = battery.currentMa.confidence)
                }
            )

            MetricTile(
                label = stringResource(R.string.battery_status),
                value = when (battery.chargingStatus) {
                    com.devicepulse.domain.model.ChargingStatus.CHARGING ->
                        stringResource(R.string.charging_status_charging)
                    com.devicepulse.domain.model.ChargingStatus.DISCHARGING ->
                        stringResource(R.string.charging_status_discharging)
                    com.devicepulse.domain.model.ChargingStatus.FULL ->
                        stringResource(R.string.charging_status_full)
                    com.devicepulse.domain.model.ChargingStatus.NOT_CHARGING ->
                        stringResource(R.string.charging_status_not_charging)
                }
            )

            MetricTile(
                label = stringResource(R.string.battery_plug_type),
                value = when (battery.plugType) {
                    com.devicepulse.domain.model.PlugType.AC -> stringResource(R.string.plug_type_ac)
                    com.devicepulse.domain.model.PlugType.USB -> stringResource(R.string.plug_type_usb)
                    com.devicepulse.domain.model.PlugType.WIRELESS -> stringResource(R.string.plug_type_wireless)
                    com.devicepulse.domain.model.PlugType.NONE -> stringResource(R.string.plug_type_none)
                }
            )

            MetricTile(
                label = stringResource(R.string.battery_health),
                value = when (battery.health) {
                    com.devicepulse.domain.model.BatteryHealth.GOOD -> stringResource(R.string.battery_health_good)
                    com.devicepulse.domain.model.BatteryHealth.OVERHEAT -> stringResource(R.string.battery_health_overheat)
                    com.devicepulse.domain.model.BatteryHealth.DEAD -> stringResource(R.string.battery_health_dead)
                    com.devicepulse.domain.model.BatteryHealth.OVER_VOLTAGE -> stringResource(R.string.battery_health_over_voltage)
                    com.devicepulse.domain.model.BatteryHealth.COLD -> stringResource(R.string.battery_health_cold)
                    com.devicepulse.domain.model.BatteryHealth.UNKNOWN -> stringResource(R.string.battery_health_unknown)
                }
            )

            MetricTile(
                label = stringResource(R.string.battery_technology),
                value = battery.technology
            )

            battery.cycleCount?.let { count ->
                MetricTile(
                    label = stringResource(R.string.battery_cycle_count),
                    value = count.toString()
                )
            }

            battery.healthPercent?.let { pct ->
                MetricTile(
                    label = stringResource(R.string.battery_health_percent),
                    value = pct.toString(),
                    unit = stringResource(R.string.unit_percent)
                )
            }

            // History chart with period selector
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            Text(
                text = stringResource(R.string.battery_history_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (state.isPro) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                ) {
                    HistoryPeriod.entries.forEach { period ->
                        val label = when (period) {
                            HistoryPeriod.DAY -> stringResource(R.string.history_period_day)
                            HistoryPeriod.WEEK -> stringResource(R.string.history_period_week)
                            HistoryPeriod.MONTH -> stringResource(R.string.history_period_month)
                            HistoryPeriod.ALL -> stringResource(R.string.history_period_all)
                        }
                        FilterChip(
                            selected = state.selectedPeriod == period,
                            onClick = { onPeriodChange(period) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (state.history.isNotEmpty()) {
                TrendChart(
                    data = state.history.map { it.level.toFloat() }
                )
            }

            AdBanner()

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}
