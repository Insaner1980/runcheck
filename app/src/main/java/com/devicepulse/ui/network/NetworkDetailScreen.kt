package com.devicepulse.ui.network

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.R
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.SignalQuality
import com.devicepulse.domain.model.SpeedTestResult
import com.devicepulse.ui.common.formatDecimal
import com.devicepulse.ui.components.MetricTile
import com.devicepulse.ui.components.PrimaryTopBar
import com.devicepulse.ui.components.PullToRefreshWrapper
import com.devicepulse.ui.components.SpeedGauge
import com.devicepulse.ui.theme.spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NetworkDetailScreen(
    onBack: () -> Unit = {},
    onNavigateToSpeedTest: () -> Unit,
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
            Column(modifier = Modifier.fillMaxSize()) {
                PrimaryTopBar(title = stringResource(R.string.network_title))
                NetworkContent(
                    state = state,
                    onRefresh = { viewModel.refresh() },
                    onNavigateToSpeedTest = onNavigateToSpeedTest
                )
            }
        }
    }
}

@Composable
private fun NetworkContent(
    state: NetworkUiState.Success,
    onRefresh: () -> Unit,
    onNavigateToSpeedTest: () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val network = state.networkState
    val context = LocalContext.current
    val hasLocationPermission = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    val locationEnabled = context.isLocationEnabled()

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
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            MetricTile(
                label = stringResource(R.string.network_connection_type),
                value = when (network.connectionType) {
                    ConnectionType.WIFI -> stringResource(R.string.connection_wifi)
                    ConnectionType.CELLULAR -> network.networkSubtype ?: stringResource(R.string.connection_cellular)
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
                if (network.wifiSsid == null) {
                    WifiNameHelpCard(
                        hasLocationPermission = hasLocationPermission,
                        locationEnabled = locationEnabled,
                        onOpenSettings = {
                            if (!hasLocationPermission) {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                )
                            } else if (!locationEnabled) {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            }
                        }
                    )
                }
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
                        value = formatDecimal(freq / 1000f, 1),
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

            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.sm))

            SpeedTestEntryCard(
                connectionType = network.connectionType,
                onOpen = onNavigateToSpeedTest
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

@Composable
private fun WifiNameHelpCard(
    hasLocationPermission: Boolean,
    locationEnabled: Boolean,
    onOpenSettings: () -> Unit
) {
    val message = when {
        !hasLocationPermission -> stringResource(R.string.network_wifi_name_permission_needed)
        !locationEnabled -> stringResource(R.string.network_wifi_name_location_needed)
        else -> stringResource(R.string.network_wifi_name_unavailable)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.network_wifi_name_help_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onOpenSettings) {
                Text(
                    text = if (!hasLocationPermission) {
                        stringResource(R.string.network_wifi_name_open_app_settings)
                    } else {
                        stringResource(R.string.location_services_open_settings)
                    }
                )
            }
        }
    }
}

private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private fun Context.isLocationEnabled(): Boolean {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return false
    return LocationManagerCompat.isLocationEnabled(locationManager)
}

@Composable
private fun SpeedTestEntryCard(
    connectionType: ConnectionType,
    onOpen: () -> Unit
) {
    val isAvailable = connectionType != ConnectionType.NONE

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.speed_test_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isAvailable) {
                        stringResource(R.string.speed_test_entry_subtitle)
                    } else {
                        stringResource(R.string.speed_test_no_connection_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onOpen,
                enabled = isAvailable
            ) {
                Text(text = stringResource(R.string.speed_test_open))
            }
        }
    }
}

@Composable
private fun SpeedTestSection(
    speedTestState: SpeedTestUiState,
    isCellular: Boolean,
    hasConnection: Boolean,
    onStartSpeedTest: () -> Unit
) {
    var showCellularWarning by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.speed_test_title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

    // Start button or progress
    if (!speedTestState.isRunning && speedTestState.phase !is SpeedTestPhase.Completed) {
        StartTestButton(
            hasConnection = hasConnection,
            onClick = {
                if (isCellular) {
                    showCellularWarning = true
                } else {
                    onStartSpeedTest()
                }
            }
        )
    }

    // Active test display
    if (speedTestState.isRunning) {
        SpeedTestActiveDisplay(speedTestState)
    }

    // Completed results
    val phase = speedTestState.phase
    if (phase is SpeedTestPhase.Completed) {
        SpeedTestResultsDisplay(speedTestState)
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        Button(
            onClick = {
                if (isCellular) {
                    showCellularWarning = true
                } else {
                    onStartSpeedTest()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Speed,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(MaterialTheme.spacing.sm))
            Text(stringResource(R.string.speed_test_start))
        }
    }

    // Failed state
    if (phase is SpeedTestPhase.Failed) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(MaterialTheme.spacing.base),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = phase.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        Button(
            onClick = {
                if (isCellular) {
                    showCellularWarning = true
                } else {
                    onStartSpeedTest()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.retry))
        }
    }

    // Last saved result (when idle and not just completed)
    if (speedTestState.phase is SpeedTestPhase.Idle && speedTestState.lastResult != null) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        LastResultCard(speedTestState.lastResult)
    }

    // History
    if (speedTestState.recentResults.size > 1) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        Text(
            text = stringResource(R.string.speed_test_history),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        speedTestState.recentResults.drop(1).forEach { result ->
            HistoryResultRow(result)
        }
    }

    // Privacy notice
    Text(
        text = stringResource(R.string.speed_test_mlab_notice),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = MaterialTheme.spacing.sm)
    )

    // Cellular data warning dialog
    if (showCellularWarning) {
        CellularDataWarningDialog(
            onConfirm = {
                showCellularWarning = false
                onStartSpeedTest()
            },
            onDismiss = { showCellularWarning = false }
        )
    }
}

@Composable
private fun StartTestButton(
    hasConnection: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = hasConnection,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            Icons.Default.Speed,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(MaterialTheme.spacing.sm))
        Text(
            text = stringResource(R.string.speed_test_start),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun SpeedTestActiveDisplay(state: SpeedTestUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            // Phase indicator
            val phaseText = when (state.phase) {
                is SpeedTestPhase.Ping -> stringResource(R.string.speed_test_phase_ping)
                is SpeedTestPhase.Download -> stringResource(R.string.speed_test_phase_download)
                is SpeedTestPhase.Upload -> stringResource(R.string.speed_test_phase_upload)
                else -> stringResource(R.string.speed_test_running)
            }
            Text(
                text = phaseText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Progress bar
            val progress = when (state.phase) {
                is SpeedTestPhase.Ping -> -1f // indeterminate
                is SpeedTestPhase.Download -> state.downloadProgress
                is SpeedTestPhase.Upload -> state.uploadProgress
                else -> 0f
            }
            if (progress < 0) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Gauges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpeedGauge(
                    value = state.downloadMbps,
                    maxValue = 1000.0,
                    label = stringResource(R.string.speed_test_download),
                    unit = stringResource(R.string.unit_mbps),
                    size = 100.dp,
                    strokeWidth = 8.dp
                )
                SpeedGauge(
                    value = state.uploadMbps,
                    maxValue = 500.0,
                    label = stringResource(R.string.speed_test_upload),
                    unit = stringResource(R.string.unit_mbps),
                    size = 100.dp,
                    strokeWidth = 8.dp
                )
            }

            if (state.pingMs > 0) {
                Text(
                    text = "${stringResource(R.string.speed_test_ping)}: ${state.pingMs} ${stringResource(R.string.unit_ms)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SpeedTestResultsDisplay(state: SpeedTestUiState) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
            ) {
                Text(
                    text = stringResource(R.string.speed_test_completed),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpeedGauge(
                        value = state.downloadMbps,
                        maxValue = 1000.0,
                        label = stringResource(R.string.speed_test_download),
                        unit = stringResource(R.string.unit_mbps),
                        size = 120.dp,
                        strokeWidth = 10.dp
                    )
                    SpeedGauge(
                        value = state.uploadMbps,
                        maxValue = 500.0,
                        label = stringResource(R.string.speed_test_upload),
                        unit = stringResource(R.string.unit_mbps),
                        size = 120.dp,
                        strokeWidth = 10.dp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${state.pingMs}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${stringResource(R.string.speed_test_ping)} (${stringResource(R.string.unit_ms)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.jitterMs > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.jitterMs}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${stringResource(R.string.speed_test_jitter)} (${stringResource(R.string.unit_ms)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LastResultCard(result: SpeedTestResult) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.speed_test_last_result),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateFormat.format(Date(result.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResultMetric(
                    label = stringResource(R.string.speed_test_download),
                    value = formatDecimal(result.downloadMbps, 1),
                    unit = stringResource(R.string.unit_mbps)
                )
                ResultMetric(
                    label = stringResource(R.string.speed_test_upload),
                    value = formatDecimal(result.uploadMbps, 1),
                    unit = stringResource(R.string.unit_mbps)
                )
                ResultMetric(
                    label = stringResource(R.string.speed_test_ping),
                    value = result.pingMs.toString(),
                    unit = stringResource(R.string.unit_ms)
                )
            }
        }
    }
}

@Composable
private fun ResultMetric(
    label: String,
    value: String,
    unit: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryResultRow(result: SpeedTestResult) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.xs),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateFormat.format(Date(result.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatDecimal(result.downloadMbps, 1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatDecimal(result.uploadMbps, 1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${result.pingMs} ms",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CellularDataWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CellTower,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(stringResource(R.string.speed_test_title))
        },
        text = {
            Text(stringResource(R.string.speed_test_cellular_warning))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.speed_test_cellular_proceed))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.speed_test_cellular_cancel))
            }
        }
    )
}
