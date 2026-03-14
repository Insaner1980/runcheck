package com.runcheck.ui.network

import com.runcheck.ui.ads.DetailScreenAdBanner
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.runcheck.R
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.SignalQuality
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.repository.NetworkReadingData
import com.runcheck.ui.common.findActivity
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.common.isUnknownValue
import com.runcheck.ui.common.rememberFormattedDateTime
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.MetricPill
import com.runcheck.ui.components.MetricRow
import com.runcheck.ui.components.PullToRefreshWrapper
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.components.SignalBars
import com.runcheck.ui.components.TrendChart
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColorForSignalQuality

@Composable
fun NetworkDetailScreen(
    onNavigateToSpeedTest: () -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val networkUiState by viewModel.networkUiState.collectAsStateWithLifecycle()
    val speedTestState by viewModel.speedTestState.collectAsStateWithLifecycle()

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

        when (val state = networkUiState) {
            is NetworkUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is NetworkUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message)
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }

            is NetworkUiState.Success -> {
                NetworkContent(
                    state = state,
                    speedTestState = speedTestState,
                    onRefresh = { viewModel.refresh() },
                    onNavigateToSpeedTest = onNavigateToSpeedTest,
                    onPeriodChange = { viewModel.setHistoryPeriod(it) }
                )
            }
        }
    }
}

// ── Card wrapper ────────────────────────────────────────────────────────────────

@Composable
private fun NetworkPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            content = content
        )
    }
}

// ── Hero section ────────────────────────────────────────────────────────────────

@Composable
private fun NetworkHeroSection(networkState: NetworkState) {
    val qualityLabel = signalQualityLabel(networkState.signalQuality)

    NetworkPanel {
        SectionHeader(text = stringResource(R.string.network_title))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SignalBars(
                signalQuality = networkState.signalQuality,
                qualityLabel = qualityLabel,
                modifier = Modifier.height(48.dp)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Text(
                text = qualityLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = statusColorForSignalQuality(networkState.signalQuality)
            )

            networkState.signalDbm?.let { dbm ->
                Text(
                    text = "$dbm ${stringResource(R.string.unit_dbm)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            MetricPill(
                label = stringResource(R.string.network_latency),
                value = networkState.latencyMs?.let { "$it ${stringResource(R.string.unit_ms)}" } ?: "\u2014",
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                label = bandwidthPillLabel(networkState),
                value = bandwidthPillValue(networkState),
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                label = bandPillLabel(networkState),
                value = bandPillValue(networkState),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Hero helper functions ───────────────────────────────────────────────────────

@Composable
private fun signalQualityLabel(quality: SignalQuality): String = when (quality) {
    SignalQuality.EXCELLENT -> stringResource(R.string.signal_excellent)
    SignalQuality.GOOD -> stringResource(R.string.signal_good)
    SignalQuality.FAIR -> stringResource(R.string.signal_fair)
    SignalQuality.POOR -> stringResource(R.string.signal_poor)
    SignalQuality.NO_SIGNAL -> stringResource(R.string.network_no_connection)
}

@Composable
private fun bandwidthPillLabel(state: NetworkState): String = when (state.connectionType) {
    ConnectionType.WIFI -> stringResource(R.string.network_wifi_speed)
    else -> stringResource(R.string.network_est_bandwidth_down)
}

@Composable
private fun bandwidthPillValue(state: NetworkState): String = when (state.connectionType) {
    ConnectionType.WIFI -> state.wifiSpeedMbps?.let { "$it ${stringResource(R.string.unit_mbps)}" } ?: "\u2014"
    ConnectionType.CELLULAR -> state.estimatedDownstreamKbps?.let { "${it / 1000} ${stringResource(R.string.unit_mbps)}" } ?: "\u2014"
    ConnectionType.NONE -> "\u2014"
}

@Composable
private fun bandPillLabel(state: NetworkState): String = when (state.connectionType) {
    ConnectionType.WIFI -> stringResource(R.string.network_wifi_frequency)
    else -> stringResource(R.string.network_subtype)
}

@Composable
private fun bandPillValue(state: NetworkState): String = when (state.connectionType) {
    ConnectionType.WIFI -> state.wifiFrequencyMhz?.let { freq ->
        "${formatDecimal(freq / 1000f, 1)} ${stringResource(R.string.unit_ghz)}"
    } ?: "\u2014"
    ConnectionType.CELLULAR -> state.networkSubtype ?: "\u2014"
    ConnectionType.NONE -> "\u2014"
}

// ── Connection Details card ─────────────────────────────────────────────────────

@Composable
private fun ConnectionDetailsCard(networkState: NetworkState) {
    NetworkPanel {
        CardSectionTitle(text = stringResource(R.string.network_section_connection_details))

        MetricRow(
            label = stringResource(R.string.network_connection_type),
            value = when (networkState.connectionType) {
                ConnectionType.WIFI -> stringResource(R.string.connection_wifi)
                ConnectionType.CELLULAR -> stringResource(R.string.connection_cellular)
                ConnectionType.NONE -> stringResource(R.string.connection_none)
            }
        )

        if (networkState.connectionType == ConnectionType.WIFI) {
            networkState.wifiSsid?.let {
                MetricRow(label = stringResource(R.string.network_wifi_ssid), value = it)
            }
            networkState.wifiBssid?.let {
                MetricRow(label = stringResource(R.string.network_bssid), value = it)
            }
            networkState.wifiStandard?.let {
                MetricRow(label = stringResource(R.string.network_wifi_standard), value = it)
            }
            networkState.wifiFrequencyMhz?.let { freq ->
                MetricRow(
                    label = stringResource(R.string.network_wifi_frequency),
                    value = "$freq ${stringResource(R.string.unit_mhz)}"
                )
            }
            networkState.wifiSpeedMbps?.let {
                MetricRow(
                    label = stringResource(R.string.network_wifi_speed),
                    value = "$it ${stringResource(R.string.unit_mbps)}"
                )
            }
        }

        if (networkState.connectionType == ConnectionType.CELLULAR) {
            networkState.carrier?.takeUnless { isUnknownValue(it) }?.let {
                MetricRow(label = stringResource(R.string.network_carrier), value = it)
            }
            networkState.networkSubtype?.let {
                MetricRow(label = stringResource(R.string.network_subtype), value = it)
            }
            networkState.isRoaming?.let {
                MetricRow(
                    label = stringResource(R.string.network_roaming),
                    value = if (it) stringResource(R.string.common_yes) else stringResource(R.string.common_no)
                )
            }
        }

        networkState.estimatedDownstreamKbps?.let {
            MetricRow(
                label = stringResource(R.string.network_est_bandwidth_down),
                value = "${it / 1000} ${stringResource(R.string.unit_mbps)}"
            )
        }
        networkState.estimatedUpstreamKbps?.let {
            MetricRow(
                label = stringResource(R.string.network_est_bandwidth_up),
                value = "${it / 1000} ${stringResource(R.string.unit_mbps)}"
            )
        }
        networkState.isMetered?.let {
            MetricRow(
                label = stringResource(R.string.network_metered),
                value = if (it) stringResource(R.string.common_yes) else stringResource(R.string.common_no)
            )
        }
        networkState.isVpn?.takeIf { it }?.let {
            MetricRow(
                label = stringResource(R.string.network_vpn),
                value = stringResource(R.string.common_yes)
            )
        }

        if (networkState.ipAddresses.isNotEmpty() || networkState.dnsServers.isNotEmpty() || networkState.mtuBytes != null) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
            CardSectionTitle(text = stringResource(R.string.network_section_ip_dns))

            networkState.ipAddresses.firstOrNull { it.contains('.') }?.let {
                MetricRow(label = stringResource(R.string.network_ipv4), value = it)
            }
            networkState.ipAddresses.firstOrNull { it.contains(':') }?.let {
                MetricRow(label = stringResource(R.string.network_ipv6), value = it)
            }
            networkState.dnsServers.getOrNull(0)?.let {
                MetricRow(label = stringResource(R.string.network_dns_1), value = it)
            }
            networkState.dnsServers.getOrNull(1)?.let {
                MetricRow(label = stringResource(R.string.network_dns_2), value = it)
            }
            networkState.mtuBytes?.let {
                MetricRow(label = stringResource(R.string.network_mtu), value = it.toString())
            }
        }
    }
}

// ── Signal History card ─────────────────────────────────────────────────────────

private enum class NetworkHistoryMetric {
    SIGNAL,
    LATENCY
}

@Composable
private fun SignalHistoryCard(
    history: List<NetworkReadingData>,
    selectedPeriod: HistoryPeriod,
    onPeriodChange: (HistoryPeriod) -> Unit
) {
    var selectedMetric by rememberSaveable { mutableStateOf(NetworkHistoryMetric.SIGNAL.name) }
    val metric = NetworkHistoryMetric.valueOf(selectedMetric)

    val chartData = remember(history, metric) {
        when (metric) {
            NetworkHistoryMetric.SIGNAL -> history.mapNotNull { it.signalDbm?.toFloat() }
            NetworkHistoryMetric.LATENCY -> history.mapNotNull { it.latencyMs?.toFloat() }
        }.downsampleForChart(MAX_NETWORK_HISTORY_POINTS)
    }

    NetworkPanel {
        CardSectionTitle(text = stringResource(R.string.network_section_signal_history))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            NetworkHistoryMetric.entries.forEach { m ->
                FilterChip(
                    selected = metric == m,
                    onClick = { selectedMetric = m.name },
                    label = { Text(networkHistoryMetricLabel(m)) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            HistoryPeriod.entries.forEach { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { onPeriodChange(period) },
                    label = { Text(historyPeriodLabel(period)) }
                )
            }
        }

        if (chartData.size >= 2) {
            Text(
                text = "${historyPeriodLabel(selectedPeriod)} \u00B7 ${networkHistoryMetricLabel(metric)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            TrendChart(
                data = chartData,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = stringResource(
                    R.string.a11y_chart_trend,
                    networkHistoryMetricLabel(metric)
                )
            )
        } else {
            Text(
                text = stringResource(R.string.network_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun networkHistoryMetricLabel(metric: NetworkHistoryMetric): String = when (metric) {
    NetworkHistoryMetric.SIGNAL -> stringResource(R.string.network_history_metric_signal)
    NetworkHistoryMetric.LATENCY -> stringResource(R.string.network_history_metric_latency)
}

@Composable
private fun historyPeriodLabel(period: HistoryPeriod): String = when (period) {
    HistoryPeriod.DAY -> stringResource(R.string.history_period_day)
    HistoryPeriod.WEEK -> stringResource(R.string.history_period_week)
    HistoryPeriod.MONTH -> stringResource(R.string.history_period_month)
    HistoryPeriod.ALL -> stringResource(R.string.history_period_all)
}

private const val MAX_NETWORK_HISTORY_POINTS = 300

private fun List<Float>.downsampleForChart(maxPoints: Int): List<Float> {
    if (size <= maxPoints || maxPoints <= 1) return this
    val lastIndex = lastIndex
    return buildList(maxPoints) {
        for (index in 0 until maxPoints) {
            val sourceIndex = ((index.toLong() * lastIndex) / (maxPoints - 1)).toInt()
            add(this@downsampleForChart[sourceIndex])
        }
    }
}

// ── Speed Test Summary card ─────────────────────────────────────────────────────

@Composable
private fun SpeedTestSummaryCard(
    lastResult: SpeedTestResult?,
    onNavigateToSpeedTest: () -> Unit
) {
    NetworkPanel {
        CardSectionTitle(text = stringResource(R.string.network_section_speed_test))

        if (lastResult != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
            ) {
                MetricPill(
                    label = stringResource(R.string.speed_test_download),
                    value = "${formatDecimal(lastResult.downloadMbps, 1)} ${stringResource(R.string.unit_mbps)}",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = stringResource(R.string.speed_test_upload),
                    value = "${formatDecimal(lastResult.uploadMbps, 1)} ${stringResource(R.string.unit_mbps)}",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = stringResource(R.string.speed_test_ping),
                    value = "${lastResult.pingMs} ${stringResource(R.string.unit_ms)}",
                    modifier = Modifier.weight(1f)
                )
            }

            lastResult.jitterMs?.let { jitter ->
                MetricPill(
                    label = stringResource(R.string.network_speed_test_jitter),
                    value = "$jitter ${stringResource(R.string.unit_ms)}"
                )
            }

            val serverText = listOfNotNull(lastResult.serverName, lastResult.serverLocation)
                .joinToString(" \u00B7 ")
            if (serverText.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.network_speed_test_server, serverText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val formattedTime = rememberFormattedDateTime(lastResult.timestamp, "MMMdhm")
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = stringResource(R.string.network_speed_test_no_results),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onNavigateToSpeedTest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.speed_test_open))
        }
    }
}

// ── Main content ────────────────────────────────────────────────────────────────

@Composable
private fun NetworkContent(
    state: NetworkUiState.Success,
    speedTestState: SpeedTestUiState,
    onRefresh: () -> Unit,
    onNavigateToSpeedTest: () -> Unit,
    onPeriodChange: (HistoryPeriod) -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context.findActivity()
    val networkState = state.networkState
    val hasLocationPermission = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    val locationEnabled = context.isLocationEnabled()
    var locationRequestAttempted by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationRequestAttempted = true
        if (granted) onRefresh()
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
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            NetworkHeroSection(networkState = networkState)

            if (networkState.connectionType == ConnectionType.WIFI && networkState.wifiSsid == null) {
                WifiNameHelpCard(
                    hasLocationPermission = hasLocationPermission,
                    locationEnabled = locationEnabled,
                    showOpenSettings = !hasLocationPermission &&
                        locationRequestAttempted &&
                        activity?.let {
                            !ActivityCompat.shouldShowRequestPermissionRationale(
                                it, Manifest.permission.ACCESS_FINE_LOCATION
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

            ConnectionDetailsCard(networkState = networkState)

            SignalHistoryCard(
                history = state.signalHistory,
                selectedPeriod = state.selectedHistoryPeriod,
                onPeriodChange = onPeriodChange
            )

            SpeedTestSummaryCard(
                lastResult = speedTestState.lastResult,
                onNavigateToSpeedTest = onNavigateToSpeedTest
            )

            DetailScreenAdBanner()

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

// ── WiFi name help card ─────────────────────────────────────────────────────────

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

// ── Utility extensions ──────────────────────────────────────────────────────────

private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private fun Context.isLocationEnabled(): Boolean {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return false
    return LocationManagerCompat.isLocationEnabled(locationManager)
}

// ── Composables used by SpeedTestScreen ─────────────────────────────────────────

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
