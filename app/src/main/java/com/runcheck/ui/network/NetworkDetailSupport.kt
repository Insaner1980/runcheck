package com.runcheck.ui.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.NetworkState
import com.runcheck.ui.common.isUnknownValue
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.MetricRow
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.spacing

@Composable
internal fun ConnectionDetailsCard(
    networkState: NetworkState,
    onInfoClick: (String) -> Unit = {},
) {
    NetworkPanel {
        CardSectionTitle(text = stringResource(R.string.network_section_connection_details))

        MetricRow(
            label = stringResource(R.string.network_connection_type),
            value =
                when (networkState.connectionType) {
                    ConnectionType.WIFI -> stringResource(R.string.connection_wifi)
                    ConnectionType.CELLULAR -> stringResource(R.string.connection_cellular)
                    ConnectionType.VPN -> stringResource(R.string.connection_vpn)
                    ConnectionType.NONE -> stringResource(R.string.connection_none)
                },
            onInfoClick = { onInfoClick("connectionType") },
        )

        if (networkState.connectionType == ConnectionType.WIFI) {
            WifiDetailsRows(networkState = networkState, onInfoClick = onInfoClick)
        }

        if (networkState.connectionType == ConnectionType.CELLULAR ||
            networkState.connectionType == ConnectionType.VPN
        ) {
            CellularDetailsRows(networkState = networkState, onInfoClick = onInfoClick)
        }

        BandwidthAndGeneralRows(networkState = networkState, onInfoClick = onInfoClick)
    }
}

@Composable
private fun WifiDetailsRows(
    networkState: NetworkState,
    onInfoClick: (String) -> Unit,
) {
    networkState.wifiSsid?.let {
        MetricRow(label = stringResource(R.string.network_wifi_ssid), value = it)
    }
    networkState.wifiBssid?.let {
        MetricRow(label = stringResource(R.string.network_bssid), value = it, copyable = true)
    }
    networkState.wifiStandard?.let {
        MetricRow(label = stringResource(R.string.network_wifi_standard), value = it, onInfoClick = {
            onInfoClick("wifiStandard")
        })
    }
    networkState.wifiFrequencyMhz?.let { freq ->
        MetricRow(
            label = stringResource(R.string.network_wifi_frequency),
            value =
                stringResource(
                    R.string.value_with_unit_int,
                    freq,
                    stringResource(R.string.unit_mhz),
                ),
            onInfoClick = { onInfoClick("frequency") },
        )
    }
    networkState.wifiSpeedMbps?.let {
        MetricRow(
            label = stringResource(R.string.network_wifi_speed),
            value =
                stringResource(
                    R.string.value_with_unit_int,
                    it,
                    stringResource(R.string.unit_mbps),
                ),
            onInfoClick = { onInfoClick("linkSpeed") },
        )
    }
}

@Composable
private fun CellularDetailsRows(
    networkState: NetworkState,
    onInfoClick: (String) -> Unit,
) {
    networkState.carrier?.takeUnless { isUnknownValue(it) }?.let {
        MetricRow(label = stringResource(R.string.network_carrier), value = it)
    }
    networkState.networkSubtype?.let {
        MetricRow(
            label = stringResource(R.string.network_subtype),
            value = it,
            onInfoClick = { onInfoClick("subtype") },
        )
    }
    networkState.isRoaming?.let {
        MetricRow(
            label = stringResource(R.string.network_roaming),
            value = if (it) stringResource(R.string.common_yes) else stringResource(R.string.common_no),
            onInfoClick = { onInfoClick("roaming") },
        )
    }
}

@Composable
private fun BandwidthAndGeneralRows(
    networkState: NetworkState,
    onInfoClick: (String) -> Unit,
) {
    networkState.estimatedDownstreamKbps?.let {
        MetricRow(
            label = stringResource(R.string.network_est_bandwidth_down),
            value =
                stringResource(
                    R.string.value_with_unit_int,
                    it / 1000,
                    stringResource(R.string.unit_mbps),
                ),
            onInfoClick = { onInfoClick("bandwidth") },
        )
    }
    networkState.estimatedUpstreamKbps?.let {
        MetricRow(
            label = stringResource(R.string.network_est_bandwidth_up),
            value =
                stringResource(
                    R.string.value_with_unit_int,
                    it / 1000,
                    stringResource(R.string.unit_mbps),
                ),
            onInfoClick = { onInfoClick("bandwidth") },
        )
    }
    networkState.isMetered?.let {
        MetricRow(
            label = stringResource(R.string.network_metered),
            value = if (it) stringResource(R.string.common_yes) else stringResource(R.string.common_no),
            onInfoClick = { onInfoClick("metered") },
        )
    }
    MetricRow(
        label = stringResource(R.string.network_vpn),
        value =
            if (networkState.isVpn == true) {
                stringResource(R.string.common_on)
            } else {
                stringResource(R.string.common_off)
            },
        onInfoClick = { onInfoClick("vpn") },
    )
}

@Composable
internal fun IpDnsCard(
    networkState: NetworkState,
    onInfoClick: (String) -> Unit = {},
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
                onInfoClick = { onInfoClick("mtu") },
            )
        }
    }
}

@Composable
internal fun NetworkPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            content = content,
        )
    }
}
