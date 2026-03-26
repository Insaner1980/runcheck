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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
import com.runcheck.ui.common.UiText
import com.runcheck.ui.common.resolve
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.SignalQuality
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.ui.common.findActivity
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.common.isUnknownValue
import com.runcheck.ui.common.rememberFormattedDateTime
import com.runcheck.ui.common.signalQualityLabel
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.LiveChart
import com.runcheck.ui.components.MetricPill
import com.runcheck.ui.components.MetricRow
import com.runcheck.ui.components.ProFeatureCalloutCard
import com.runcheck.ui.components.PullToRefreshWrapper
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.components.SignalBars
import com.runcheck.ui.components.ExpandableChartContainer
import com.runcheck.ui.components.TrendChart
import com.runcheck.ui.chart.FullscreenChartSource
import com.runcheck.ui.chart.NetworkHistoryMetric
import com.runcheck.ui.chart.MAX_NETWORK_HISTORY_POINTS
import com.runcheck.ui.chart.buildNetworkHistoryChartModel
import com.runcheck.ui.chart.formatChartTooltip
import com.runcheck.ui.chart.historyPeriodLabel
import com.runcheck.ui.chart.networkHistoryMetricLabel
import com.runcheck.ui.chart.rememberChartAccessibilitySummary
import com.runcheck.ui.chart.signalQualityZones
import com.runcheck.ui.components.info.InfoBottomSheet
import com.runcheck.ui.components.info.InfoCard
import com.runcheck.ui.components.info.InfoCardCatalog
import com.runcheck.ui.fullscreen.FullscreenChartSeedStore
import com.runcheck.ui.fullscreen.FullscreenChartUiState
import com.runcheck.ui.fullscreen.sanitizeFullscreenMetric
import com.runcheck.ui.fullscreen.sanitizeFullscreenPeriod
import com.runcheck.ui.learn.LearnArticleIds
import com.runcheck.ui.learn.RelatedArticlesSection
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors
import com.runcheck.ui.theme.statusColorForSignalQuality

@Composable
fun NetworkDetailScreen(
    onNavigateToSpeedTest: () -> Unit,
    onBack: () -> Unit = {},
    onUpgradeToPro: () -> Unit = {},
    onNavigateToFullscreen: (source: String, metric: String, period: String) -> Unit = { _, _, _ -> },
    onNavigateToLearnArticle: (articleId: String) -> Unit = {},
    fullscreenResultMetric: String? = null,
    fullscreenResultPeriod: String? = null,
    onFullscreenResultConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val context = LocalContext.current
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
                Box(
                    Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = context.getString(R.string.a11y_loading); liveRegion = LiveRegionMode.Polite },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is NetworkUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    ) {
                        Text(state.message.resolve())
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
                    onPeriodChange = { viewModel.setHistoryPeriod(it) },
                    onUpgradeToPro = onUpgradeToPro,
                    onNavigateToFullscreen = onNavigateToFullscreen,
                    onNavigateToLearnArticle = onNavigateToLearnArticle,
                    onDismissInfoCard = { viewModel.dismissInfoCard(it) },
                    fullscreenResultMetric = fullscreenResultMetric,
                    fullscreenResultPeriod = fullscreenResultPeriod,
                    onFullscreenResultConsumed = onFullscreenResultConsumed
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
        shape = MaterialTheme.shapes.large
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
private fun NetworkHeroSection(
    networkState: NetworkState,
    liveSignalDbm: List<Float> = emptyList(),
    onInfoClick: (String) -> Unit = {}
) {
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
                val dbmText = stringResource(
                    R.string.value_with_unit_int,
                    dbm,
                    stringResource(R.string.unit_dbm)
                )
                val displayText = networkState.signalAsu?.let { asu ->
                    stringResource(R.string.network_signal_with_asu, dbmText, asu, stringResource(R.string.unit_asu))
                } ?: dbmText
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (liveSignalDbm.size >= 2) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            LiveChart(
                data = liveSignalDbm,
                currentValueLabel = networkState.signalDbm?.let {
                    stringResource(R.string.value_with_unit_int, it, stringResource(R.string.unit_dbm))
                } ?: "—",
                label = stringResource(R.string.network_signal_strength),
                lineColor = statusColorForSignalQuality(networkState.signalQuality),
                accessibilityDescription = stringResource(
                    R.string.a11y_chart_trend,
                    stringResource(R.string.network_signal_strength)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            MetricPill(
                label = stringResource(R.string.network_latency),
                value = networkState.latencyMs?.let {
                    stringResource(R.string.value_with_unit_int, it, stringResource(R.string.unit_ms))
                } ?: stringResource(R.string.placeholder_dash),
                modifier = Modifier.weight(1f),
                onInfoClick = { onInfoClick("latency") }
            )
            MetricPill(
                label = bandwidthPillLabel(networkState),
                value = bandwidthPillValue(networkState),
                modifier = Modifier.weight(1f),
                onInfoClick = { onInfoClick("bandwidth") }
            )
            MetricPill(
                label = bandPillLabel(networkState),
                value = bandPillValue(networkState),
                modifier = Modifier.weight(1f),
                onInfoClick = { onInfoClick(if (networkState.connectionType == ConnectionType.WIFI) "frequency" else "bandwidth") }
            )
        }
    }
}

// ── Hero helper functions ───────────────────────────────────────────────────────


@Composable
private fun bandwidthPillLabel(state: NetworkState): String = when (state.connectionType) {
    ConnectionType.WIFI -> stringResource(R.string.network_wifi_speed)
    else -> stringResource(R.string.network_est_bandwidth_down)
}

@Composable
private fun bandwidthPillValue(state: NetworkState): String = when (state.connectionType) {
    ConnectionType.WIFI -> state.wifiSpeedMbps?.let {
        stringResource(R.string.value_with_unit_int, it, stringResource(R.string.unit_mbps))
    } ?: stringResource(R.string.placeholder_dash)
    ConnectionType.CELLULAR -> state.estimatedDownstreamKbps?.let {
        stringResource(R.string.value_with_unit_int, it / 1000, stringResource(R.string.unit_mbps))
    } ?: stringResource(R.string.placeholder_dash)
    ConnectionType.VPN -> state.estimatedDownstreamKbps?.let {
        stringResource(R.string.value_with_unit_int, it / 1000, stringResource(R.string.unit_mbps))
    } ?: stringResource(R.string.placeholder_dash)
    ConnectionType.NONE -> stringResource(R.string.placeholder_dash)
}

@Composable
private fun bandPillLabel(state: NetworkState): String = when (state.connectionType) {
    ConnectionType.WIFI -> stringResource(R.string.network_wifi_frequency)
    else -> stringResource(R.string.network_subtype)
}

@Composable
private fun bandPillValue(state: NetworkState): String = when (state.connectionType) {
    ConnectionType.WIFI -> state.wifiFrequencyMhz?.let { freq ->
        stringResource(
            R.string.value_with_unit_text,
            formatDecimal(freq / 1000f, 1),
            stringResource(R.string.unit_ghz)
        )
    } ?: stringResource(R.string.placeholder_dash)
    ConnectionType.CELLULAR -> state.networkSubtype ?: stringResource(R.string.placeholder_dash)
    ConnectionType.VPN -> state.networkSubtype ?: stringResource(R.string.placeholder_dash)
    ConnectionType.NONE -> stringResource(R.string.placeholder_dash)
}

// ── Connection Details card ─────────────────────────────────────────────────────

@Composable
private fun ConnectionDetailsCard(networkState: NetworkState, onInfoClick: (String) -> Unit = {}) {
    NetworkPanel {
        CardSectionTitle(text = stringResource(R.string.network_section_connection_details))

        MetricRow(
            label = stringResource(R.string.network_connection_type),
            value = when (networkState.connectionType) {
                ConnectionType.WIFI -> stringResource(R.string.connection_wifi)
                ConnectionType.CELLULAR -> stringResource(R.string.connection_cellular)
                ConnectionType.VPN -> stringResource(R.string.connection_vpn)
                ConnectionType.NONE -> stringResource(R.string.connection_none)
            },
            onInfoClick = { onInfoClick("connectionType") }
        )

        if (networkState.connectionType == ConnectionType.WIFI) {
            networkState.wifiSsid?.let {
                MetricRow(label = stringResource(R.string.network_wifi_ssid), value = it)
            }
            networkState.wifiBssid?.let {
                MetricRow(label = stringResource(R.string.network_bssid), value = it, copyable = true)
            }
            networkState.wifiStandard?.let {
                MetricRow(label = stringResource(R.string.network_wifi_standard), value = it, onInfoClick = { onInfoClick("wifiStandard") })
            }
            networkState.wifiFrequencyMhz?.let { freq ->
                MetricRow(
                    label = stringResource(R.string.network_wifi_frequency),
                    value = stringResource(
                        R.string.value_with_unit_int,
                        freq,
                        stringResource(R.string.unit_mhz)
                    ),
                    onInfoClick = { onInfoClick("frequency") }
                )
            }
            networkState.wifiSpeedMbps?.let {
                MetricRow(
                    label = stringResource(R.string.network_wifi_speed),
                    value = stringResource(
                        R.string.value_with_unit_int,
                        it,
                        stringResource(R.string.unit_mbps)
                    ),
                    onInfoClick = { onInfoClick("linkSpeed") }
                )
            }
        }

        if (networkState.connectionType == ConnectionType.CELLULAR || networkState.connectionType == ConnectionType.VPN) {
            networkState.carrier?.takeUnless { isUnknownValue(it) }?.let {
                MetricRow(label = stringResource(R.string.network_carrier), value = it)
            }
            networkState.networkSubtype?.let {
                MetricRow(label = stringResource(R.string.network_subtype), value = it, onInfoClick = { onInfoClick("subtype") })
            }
            networkState.isRoaming?.let {
                MetricRow(
                    label = stringResource(R.string.network_roaming),
                    value = if (it) stringResource(R.string.common_yes) else stringResource(R.string.common_no),
                    onInfoClick = { onInfoClick("roaming") }
                )
            }
        }

        networkState.estimatedDownstreamKbps?.let {
            MetricRow(
                label = stringResource(R.string.network_est_bandwidth_down),
                value = stringResource(
                    R.string.value_with_unit_int,
                    it / 1000,
                    stringResource(R.string.unit_mbps)
                ),
                onInfoClick = { onInfoClick("bandwidth") }
            )
        }
        networkState.estimatedUpstreamKbps?.let {
            MetricRow(
                label = stringResource(R.string.network_est_bandwidth_up),
                value = stringResource(
                    R.string.value_with_unit_int,
                    it / 1000,
                    stringResource(R.string.unit_mbps)
                ),
                onInfoClick = { onInfoClick("bandwidth") }
            )
        }
        networkState.isMetered?.let {
            MetricRow(
                label = stringResource(R.string.network_metered),
                value = if (it) stringResource(R.string.common_yes) else stringResource(R.string.common_no),
                onInfoClick = { onInfoClick("metered") }
            )
        }
        MetricRow(
            label = stringResource(R.string.network_vpn),
            value = if (networkState.isVpn == true) {
                stringResource(R.string.common_on)
            } else {
                stringResource(R.string.common_off)
            },
            onInfoClick = { onInfoClick("vpn") }
        )
    }
}

// ── IP & DNS card ────────────────────────────────────────────────────────────────

@Composable
private fun IpDnsCard(
    networkState: NetworkState,
    onInfoClick: (String) -> Unit = {}
) {
    if (networkState.ipAddresses.isEmpty() && networkState.dnsServers.isEmpty() && networkState.mtuBytes == null) return

    NetworkPanel {
        CardSectionTitle(text = stringResource(R.string.network_section_ip_dns))

        networkState.ipAddresses.firstOrNull { it.contains('.') }?.let {
            MetricRow(label = stringResource(R.string.network_ipv4), value = it, copyable = true)
        }
        networkState.ipAddresses.firstOrNull { it.contains(':') }?.let {
            MetricRow(label = stringResource(R.string.network_ipv6), value = it, maxLines = 1, copyable = true)
        }
        networkState.dnsServers.getOrNull(0)?.let {
            MetricRow(label = stringResource(R.string.network_dns_1), value = it, copyable = true)
        }
        networkState.dnsServers.getOrNull(1)?.let {
            MetricRow(label = stringResource(R.string.network_dns_2), value = it, copyable = true)
        }
        networkState.mtuBytes?.let {
            MetricRow(
                label = stringResource(R.string.network_mtu),
                value = it.toString(),
                onInfoClick = { onInfoClick("mtu") }
            )
        }
    }
}

// ── Signal History card ─────────────────────────────────────────────────────────

@Composable
private fun SignalHistoryCard(
    history: List<NetworkReading>,
    selectedPeriod: HistoryPeriod,
    historyLoadError: UiText?,
    onPeriodChange: (HistoryPeriod) -> Unit,
    onNavigateToFullscreen: (source: String, metric: String, period: String) -> Unit,
    overrideMetric: String? = null
) {
    var selectedMetric by rememberSaveable { mutableStateOf(NetworkHistoryMetric.SIGNAL.name) }

    // Apply metric override from fullscreen chart
    LaunchedEffect(overrideMetric) {
        if (overrideMetric != null) {
            selectedMetric = sanitizeFullscreenMetric(
                source = FullscreenChartSource.NETWORK_HISTORY,
                rawMetric = overrideMetric
            )
        }
    }

    val metric = NetworkHistoryMetric.entries.firstOrNull { it.name == selectedMetric }
        ?: NetworkHistoryMetric.SIGNAL

    val chartModel = remember(history, metric, selectedPeriod) {
        buildNetworkHistoryChartModel(
            history = history,
            metric = metric,
            period = selectedPeriod,
            maxPoints = MAX_NETWORK_HISTORY_POINTS
        )
    }

    // Quality zone bands (signal only — subtle background bands)
    val qualityZones = signalQualityZones(metric)

    NetworkPanel {
        CardSectionTitle(text = stringResource(R.string.network_section_signal_history))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
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
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            HistoryPeriod.entries
                .filter { it != HistoryPeriod.SINCE_UNPLUG }
                .forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { onPeriodChange(period) },
                        label = { Text(historyPeriodLabel(period)) }
                    )
                }
        }

        historyLoadError?.let { error ->
            Text(
                text = error.resolve(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (chartModel.chartData.size >= 2) {
            val chartAccessibilitySummary = rememberChartAccessibilitySummary(
                title = stringResource(
                    R.string.fullscreen_chart_title_network,
                    networkHistoryMetricLabel(metric)
                ),
                chartData = chartModel.chartData,
                unit = chartModel.unit,
                decimals = chartModel.tooltipDecimals,
                timeContext = stringResource(
                    R.string.a11y_chart_context_history,
                    historyPeriodLabel(selectedPeriod)
                )
            )
            val fullscreenSeed = remember(chartModel, metric, selectedPeriod) {
                FullscreenChartUiState.Success(
                    chartData = chartModel.chartData,
                    chartTimestamps = chartModel.chartTimestamps,
                    unit = chartModel.unit,
                    selectedMetric = metric.name,
                    selectedPeriod = selectedPeriod.name,
                    metricOptions = NetworkHistoryMetric.entries.map { it.name },
                    periodOptions = HistoryPeriod.entries
                        .filter { it != HistoryPeriod.SINCE_UNPLUG }
                        .map { it.name },
                    yLabels = chartModel.yLabels,
                    xLabels = chartModel.xLabels,
                    tooltipDecimals = chartModel.tooltipDecimals,
                    tooltipTimeSkeleton = chartModel.tooltipTimeSkeleton
                )
            }
            Text(
                text = "${historyPeriodLabel(selectedPeriod)} \u00B7 ${networkHistoryMetricLabel(metric)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            ExpandableChartContainer(
                onExpand = {
                    FullscreenChartSeedStore.prime(
                        source = FullscreenChartSource.NETWORK_HISTORY,
                        state = fullscreenSeed
                    )
                    onNavigateToFullscreen(
                        FullscreenChartSource.NETWORK_HISTORY.name,
                        metric.name,
                        selectedPeriod.name
                    )
                }
            ) {
                TrendChart(
                    data = chartModel.chartData,
                    modifier = Modifier.fillMaxWidth(),
                    contentDescription = chartAccessibilitySummary,
                    yLabels = chartModel.yLabels.ifEmpty { null },
                    xLabels = chartModel.xLabels.ifEmpty { null },
                    showGrid = true,
                    qualityZones = qualityZones,
                    tooltipFormatter = { index -> formatChartTooltip(chartModel, index) }
                )
            }

            // Min / Avg / Max summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
            ) {
                chartModel.minValue?.let {
                    MetricPill(
                        label = stringResource(R.string.chart_stat_min),
                        value = "${formatDecimal(it, chartModel.tooltipDecimals)}${chartModel.unit}",
                        modifier = Modifier.weight(1f)
                    )
                }
                chartModel.averageValue?.let {
                    MetricPill(
                        label = stringResource(R.string.chart_stat_avg),
                        value = "${formatDecimal(it, chartModel.tooltipDecimals)}${chartModel.unit}",
                        modifier = Modifier.weight(1f)
                    )
                }
                chartModel.maxValue?.let {
                    MetricPill(
                        label = stringResource(R.string.chart_stat_max),
                        value = "${formatDecimal(it, chartModel.tooltipDecimals)}${chartModel.unit}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.network_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Speed Test Summary card ─────────────────────────────────────────────────────

@Composable
private fun SpeedTestSummaryCard(
    lastResult: SpeedTestResult?,
    onNavigateToSpeedTest: () -> Unit,
    onInfoClick: (String) -> Unit = {}
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
                    value = stringResource(
                        R.string.value_with_unit_text,
                        formatDecimal(lastResult.downloadMbps, 1),
                        stringResource(R.string.unit_mbps)
                    ),
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = stringResource(R.string.speed_test_upload),
                    value = stringResource(
                        R.string.value_with_unit_text,
                        formatDecimal(lastResult.uploadMbps, 1),
                        stringResource(R.string.unit_mbps)
                    ),
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = stringResource(R.string.speed_test_ping),
                    value = formatPingMetric(lastResult.pingMs),
                    modifier = Modifier.weight(1f)
                )
            }

            lastResult.jitterMs?.let { jitter ->
                MetricPill(
                    label = stringResource(R.string.network_speed_test_jitter),
                    value = stringResource(
                        R.string.value_with_unit_int,
                        jitter,
                        stringResource(R.string.unit_ms)
                    ),
                    onInfoClick = { onInfoClick("jitter") }
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
    onPeriodChange: (HistoryPeriod) -> Unit,
    onUpgradeToPro: () -> Unit,
    onNavigateToFullscreen: (source: String, metric: String, period: String) -> Unit,
    onNavigateToLearnArticle: (articleId: String) -> Unit,
    onDismissInfoCard: (String) -> Unit,
    fullscreenResultMetric: String? = null,
    fullscreenResultPeriod: String? = null,
    onFullscreenResultConsumed: () -> Unit = {}
) {
    var isRefreshing by remember { mutableStateOf(false) }
    var activeInfoSheet by rememberSaveable { mutableStateOf<String?>(null) }

    // Apply fullscreen chart selection results when navigating back
    LaunchedEffect(fullscreenResultMetric, fullscreenResultPeriod) {
        if (fullscreenResultMetric != null && fullscreenResultPeriod != null) {
            val period = runCatching {
                HistoryPeriod.valueOf(
                    sanitizeFullscreenPeriod(
                        source = FullscreenChartSource.NETWORK_HISTORY,
                        rawPeriod = fullscreenResultPeriod
                    )
                )
            }.getOrNull()
            if (period != null) onPeriodChange(period)
            onFullscreenResultConsumed()
        }
    }
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

            NetworkHeroSection(
                networkState = networkState,
                liveSignalDbm = state.liveSignalDbm,
                onInfoClick = { activeInfoSheet = it }
            )

            // Info cards
            val shouldShowWeakSignalInfoCard = networkState.connectionType != ConnectionType.NONE &&
                networkState.signalDbm != null &&
                (networkState.signalQuality == SignalQuality.POOR ||
                    networkState.signalQuality == SignalQuality.NO_SIGNAL)

            if (shouldShowWeakSignalInfoCard) {
                InfoCard(
                    id = InfoCardCatalog.NetworkWeakSignalDrain.id,
                    headline = stringResource(InfoCardCatalog.NetworkWeakSignalDrain.headlineRes),
                    body = stringResource(InfoCardCatalog.NetworkWeakSignalDrain.bodyRes),
                    onDismiss = { onDismissInfoCard(it) },
                    visible = InfoCardCatalog.NetworkWeakSignalDrain.id !in state.dismissedInfoCards &&
                        state.showInfoCards,
                    onLearnMore = {
                        InfoCardCatalog.resolveLearnArticleId(
                            InfoCardCatalog.NetworkWeakSignalDrain
                        )?.let(onNavigateToLearnArticle)
                    }
                )
            }

            InfoCard(
                id = InfoCardCatalog.NetworkSpeedTestScope.id,
                headline = stringResource(InfoCardCatalog.NetworkSpeedTestScope.headlineRes),
                body = stringResource(InfoCardCatalog.NetworkSpeedTestScope.bodyRes),
                onDismiss = { onDismissInfoCard(it) },
                visible = InfoCardCatalog.NetworkSpeedTestScope.id !in state.dismissedInfoCards &&
                    state.showInfoCards,
                onLearnMore = {
                    InfoCardCatalog.resolveLearnArticleId(
                        InfoCardCatalog.NetworkSpeedTestScope
                    )?.let(onNavigateToLearnArticle)
                }
            )

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

            ConnectionDetailsCard(networkState = networkState, onInfoClick = { activeInfoSheet = it })
            IpDnsCard(networkState = networkState, onInfoClick = { activeInfoSheet = it })

            if (state.isPro) {
                SignalHistoryCard(
                    history = state.signalHistory,
                    selectedPeriod = state.selectedHistoryPeriod,
                    historyLoadError = state.historyLoadError,
                    onPeriodChange = onPeriodChange,
                    onNavigateToFullscreen = onNavigateToFullscreen,
                    overrideMetric = fullscreenResultMetric
                )
            } else {
                ProFeatureCalloutCard(
                    message = stringResource(R.string.pro_feature_network_history_message),
                    actionLabel = stringResource(R.string.pro_feature_upgrade_action),
                    onAction = onUpgradeToPro
                )
            }

            SpeedTestSummaryCard(
                lastResult = speedTestState.lastResult,
                onNavigateToSpeedTest = onNavigateToSpeedTest,
                onInfoClick = { activeInfoSheet = it }
            )

            RelatedArticlesSection(
                articleIds = listOf(
                    LearnArticleIds.NETWORK_SIGNAL,
                    LearnArticleIds.NETWORK_WIFI_BANDS,
                    LearnArticleIds.NETWORK_SPEED_TESTS
                ),
                onNavigateToArticle = onNavigateToLearnArticle
            )

            DetailScreenAdBanner()

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }

    activeInfoSheet?.let { key ->
        val content = when (key) {
            "signalStrength" -> NetworkInfoContent.signalStrength
            "latency" -> NetworkInfoContent.latency
            "jitter" -> NetworkInfoContent.jitter
            "frequency" -> NetworkInfoContent.frequency
            "wifiStandard" -> NetworkInfoContent.wifiStandard
            "linkSpeed" -> NetworkInfoContent.linkSpeed
            "bandwidth" -> NetworkInfoContent.bandwidth
            "mtu" -> NetworkInfoContent.mtu
            "connectionType" -> NetworkInfoContent.connectionType
            "metered" -> NetworkInfoContent.metered
            "roaming" -> NetworkInfoContent.roaming
            "vpn" -> NetworkInfoContent.vpn
            "subtype" -> NetworkInfoContent.subtype
            else -> null
        }
        content?.let {
            InfoBottomSheet(content = it, onDismiss = { activeInfoSheet = null })
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
        shape = MaterialTheme.shapes.large,
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
        shape = MaterialTheme.shapes.large,
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
                    value = if (result.pingMs > 0) result.pingMs.toString() else stringResource(R.string.placeholder_dash),
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
                text = formatPingMetric(result.pingMs),
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
        shape = MaterialTheme.shapes.large,
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

@Composable
private fun formatPingMetric(pingMs: Int): String =
    if (pingMs > 0) {
        stringResource(
            R.string.value_with_unit_int,
            pingMs,
            stringResource(R.string.unit_ms)
        )
    } else {
        stringResource(R.string.placeholder_dash)
    }
