package com.devicepulse.ui.network

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.devicepulse.R
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.NetworkState
import com.devicepulse.domain.model.SignalQuality
import com.devicepulse.domain.model.SpeedTestResult
import com.devicepulse.ui.common.formatDecimal
import com.devicepulse.ui.common.findActivity
import com.devicepulse.ui.common.isUnknownValue
import com.devicepulse.ui.common.rememberFormattedDateTime
import com.devicepulse.ui.components.DetailTopBar
import com.devicepulse.ui.components.MetricTile
import com.devicepulse.ui.components.PullToRefreshWrapper
import com.devicepulse.ui.theme.spacing

@Composable
fun NetworkDetailScreen(
    onNavigateToSpeedTest: () -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val networkState = uiState.networkState
    val errorMessage = uiState.errorMessage

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
            title = stringResource(R.string.network_title),
            onBack = onBack
        )

        when {
            uiState.isLoading && networkState == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null && networkState == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage)
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }

            networkState != null -> {
                NetworkContent(
                    networkState = networkState,
                    onRefresh = { viewModel.refresh() },
                    onNavigateToSpeedTest = onNavigateToSpeedTest
                )
            }

            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.common_error_generic))
                }
            }
        }
    }
}

@Composable
private fun NetworkContent(
    networkState: NetworkState,
    onRefresh: () -> Unit,
    onNavigateToSpeedTest: () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context.findActivity()
    val hasLocationPermission = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    val locationEnabled = context.isLocationEnabled()
    var locationRequestAttempted by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationRequestAttempted = true
        if (granted) {
            onRefresh()
        }
    }

    LaunchedEffect(networkState) {
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
                value = when (networkState.connectionType) {
                    ConnectionType.WIFI -> stringResource(R.string.connection_wifi)
                    ConnectionType.CELLULAR -> networkState.networkSubtype ?: stringResource(R.string.connection_cellular)
                    ConnectionType.NONE -> stringResource(R.string.connection_none)
                }
            )

            MetricTile(
                label = stringResource(R.string.network_signal_strength),
                value = networkState.signalDbm?.toString() ?: stringResource(R.string.not_available),
                unit = if (networkState.signalDbm != null) stringResource(R.string.unit_dbm) else ""
            )

            MetricTile(
                label = stringResource(R.string.network_signal_quality),
                value = when (networkState.signalQuality) {
                    SignalQuality.EXCELLENT -> stringResource(R.string.signal_excellent)
                    SignalQuality.GOOD -> stringResource(R.string.signal_good)
                    SignalQuality.FAIR -> stringResource(R.string.signal_fair)
                    SignalQuality.POOR -> stringResource(R.string.signal_poor)
                    SignalQuality.NO_SIGNAL -> stringResource(R.string.signal_none)
                }
            )

            if (networkState.connectionType == ConnectionType.WIFI) {
                if (networkState.wifiSsid == null) {
                    WifiNameHelpCard(
                        hasLocationPermission = hasLocationPermission,
                        locationEnabled = locationEnabled,
                        showOpenSettings = !hasLocationPermission &&
                            locationRequestAttempted &&
                            activity?.let {
                                !ActivityCompat.shouldShowRequestPermissionRationale(
                                    it,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            } == true,
                        onRequestPermission = {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
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
                networkState.wifiSsid?.let { ssid ->
                    MetricTile(
                        label = stringResource(R.string.network_wifi_ssid),
                        value = ssid
                    )
                }
                networkState.wifiSpeedMbps?.let { speed ->
                    MetricTile(
                        label = stringResource(R.string.network_wifi_speed),
                        value = speed.toString(),
                        unit = stringResource(R.string.unit_mbps)
                    )
                }
                networkState.wifiFrequencyMhz?.let { freq ->
                    MetricTile(
                        label = stringResource(R.string.network_wifi_frequency),
                        value = formatDecimal(freq / 1000f, 1),
                        unit = stringResource(R.string.unit_ghz)
                    )
                }
            }

            if (networkState.connectionType == ConnectionType.CELLULAR) {
                networkState.carrier?.let { carrier ->
                    MetricTile(
                        label = stringResource(R.string.network_carrier),
                        value = if (isUnknownValue(carrier)) {
                            stringResource(R.string.not_available)
                        } else {
                            carrier
                        }
                    )
                }
                networkState.networkSubtype?.let { subtype ->
                    MetricTile(
                        label = stringResource(R.string.network_subtype),
                        value = if (isUnknownValue(subtype)) {
                            stringResource(R.string.connection_cellular)
                        } else {
                            subtype
                        }
                    )
                }
            }

            networkState.latencyMs?.let { latency ->
                MetricTile(
                    label = stringResource(R.string.network_latency),
                    value = latency.toString(),
                    unit = stringResource(R.string.unit_ms)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.sm))

            SpeedTestEntryCard(
                connectionType = networkState.connectionType,
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
    showOpenSettings: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val actionLabel = when {
        !hasLocationPermission && showOpenSettings ->
            stringResource(R.string.network_wifi_name_open_app_settings)
        !hasLocationPermission -> stringResource(R.string.network_wifi_name_grant_permission)
        !locationEnabled -> stringResource(R.string.location_services_open_settings)
        else -> null
    }
    val message = when {
        !hasLocationPermission -> stringResource(R.string.network_wifi_name_permission_needed)
        !locationEnabled -> stringResource(R.string.network_wifi_name_location_needed)
        else -> stringResource(R.string.network_wifi_name_unavailable)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
            if (actionLabel != null) {
                Button(
                    onClick = {
                        if (!hasLocationPermission && !showOpenSettings) {
                            onRequestPermission()
                        } else {
                            onOpenSettings()
                        }
                    }
                ) {
                    Text(text = actionLabel)
                }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
private fun LastResultCard(result: SpeedTestResult) {
    val formattedTime = rememberFormattedDateTime(result.timestamp, "MMMdhm")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    text = formattedTime,
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
    val formattedTime = rememberFormattedDateTime(result.timestamp, "MMMdhm")

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
                text = formattedTime,
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
                text = stringResource(R.string.value_with_unit_int, result.pingMs, stringResource(R.string.unit_ms)),
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
