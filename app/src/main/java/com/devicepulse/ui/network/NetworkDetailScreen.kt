package com.devicepulse.ui.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.SignalQuality
import com.devicepulse.ui.components.MetricTile
import com.devicepulse.ui.components.AdBanner
import com.devicepulse.ui.components.PullToRefreshWrapper
import com.devicepulse.ui.theme.spacing

@Composable
fun NetworkDetailScreen(
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is NetworkUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is NetworkUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.error_generic))
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
        is NetworkUiState.Success -> {
            NetworkContent(state = state, onRefresh = { viewModel.refresh() })
        }
    }
}

@Composable
private fun NetworkContent(
    state: NetworkUiState.Success,
    onRefresh: () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val network = state.networkState

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
                text = stringResource(R.string.network_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            MetricTile(
                label = stringResource(R.string.network_connection_type),
                value = when (network.connectionType) {
                    ConnectionType.WIFI -> stringResource(R.string.connection_wifi)
                    ConnectionType.CELLULAR -> stringResource(R.string.connection_cellular)
                    ConnectionType.NONE -> stringResource(R.string.connection_none)
                }
            )

            MetricTile(
                label = stringResource(R.string.network_signal_strength),
                value = network.signalDbm?.toString() ?: stringResource(R.string.not_available),
                unit = if (network.signalDbm != null) stringResource(R.string.unit_dbm) else ""
            )

            MetricTile(
                label = stringResource(R.string.network_signal_quality),
                value = when (network.signalQuality) {
                    SignalQuality.EXCELLENT -> stringResource(R.string.signal_excellent)
                    SignalQuality.GOOD -> stringResource(R.string.signal_good)
                    SignalQuality.FAIR -> stringResource(R.string.signal_fair)
                    SignalQuality.POOR -> stringResource(R.string.signal_poor)
                    SignalQuality.NO_SIGNAL -> stringResource(R.string.signal_none)
                }
            )

            if (network.connectionType == ConnectionType.WIFI) {
                network.wifiSsid?.let { ssid ->
                    MetricTile(
                        label = stringResource(R.string.network_wifi_ssid),
                        value = ssid
                    )
                }
                network.wifiSpeedMbps?.let { speed ->
                    MetricTile(
                        label = stringResource(R.string.network_wifi_speed),
                        value = speed.toString(),
                        unit = stringResource(R.string.unit_mbps)
                    )
                }
                network.wifiFrequencyMhz?.let { freq ->
                    MetricTile(
                        label = stringResource(R.string.network_wifi_frequency),
                        value = "%.1f".format(freq / 1000f),
                        unit = stringResource(R.string.unit_ghz)
                    )
                }
            }

            if (network.connectionType == ConnectionType.CELLULAR) {
                network.carrier?.let { carrier ->
                    MetricTile(
                        label = stringResource(R.string.network_carrier),
                        value = carrier
                    )
                }
                network.networkSubtype?.let { subtype ->
                    MetricTile(
                        label = stringResource(R.string.network_subtype),
                        value = subtype
                    )
                }
            }

            network.latencyMs?.let { latency ->
                MetricTile(
                    label = stringResource(R.string.network_latency),
                    value = latency.toString(),
                    unit = stringResource(R.string.unit_ms)
                )
            }

            AdBanner()

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}
