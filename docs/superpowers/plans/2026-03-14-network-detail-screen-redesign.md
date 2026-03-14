# Network Detail Screen Redesign — Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign NetworkDetailScreen from a flat MetricTile list into a card-based layout with signal bars, connection details, signal history chart, and speed test summary — matching BatteryDetailScreen's visual style.

**Architecture:** Extend NetworkState domain model with 9 new fields from Android APIs (LinkProperties, NetworkCapabilities, TelephonyManager). Add GetNetworkHistoryUseCase for historical chart data. Build new SignalBars component. Rewrite NetworkDetailScreen using existing card components (CardSectionTitle, MetricPill, MetricRow, TrendChart).

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room, Hilt, existing TrendChart/MetricPill/MetricRow components.

**Spec:** `docs/superpowers/specs/2026-03-14-network-detail-screen-redesign.md`

---

## Chunk 1: Data Layer & Domain

### Task 1: Extend NetworkState domain model

**Files:**
- Modify: `app/src/main/java/com/runcheck/domain/model/NetworkState.kt`

- [ ] **Step 1: Add new fields to NetworkState**

```kotlin
data class NetworkState(
    val connectionType: ConnectionType,
    val signalDbm: Int?,
    val signalQuality: SignalQuality,
    val wifiSsid: String? = null,
    val wifiSpeedMbps: Int? = null,
    val wifiFrequencyMhz: Int? = null,
    val carrier: String? = null,
    val networkSubtype: String? = null,
    val latencyMs: Int? = null,
    // New fields
    val estimatedDownstreamKbps: Int? = null,
    val estimatedUpstreamKbps: Int? = null,
    val isMetered: Boolean? = null,
    val isRoaming: Boolean? = null,
    val isVpn: Boolean? = null,
    val ipAddresses: List<String> = emptyList(),
    val dnsServers: List<String> = emptyList(),
    val mtuBytes: Int? = null,
    val wifiBssid: String? = null,
    val wifiStandard: String? = null
)
```

- [ ] **Step 2: Commit**

```
git add app/src/main/java/com/runcheck/domain/model/NetworkState.kt
git commit -m "Laajenna NetworkState-mallia uusilla verkkokentillä"
```

---

### Task 2: Extend NetworkDataSource to collect new data

**Files:**
- Modify: `app/src/main/java/com/runcheck/data/network/NetworkDataSource.kt`

- [ ] **Step 1: Add new fields to NetworkInfo data class**

In `NetworkDataSource.NetworkInfo`, add:

```kotlin
data class NetworkInfo(
    val connectionType: ConnectionType,
    val signalDbm: Int?,
    val signalQuality: SignalQuality,
    val wifiSsid: String? = null,
    val wifiSpeedMbps: Int? = null,
    val wifiFrequencyMhz: Int? = null,
    val carrier: String? = null,
    val networkSubtype: String? = null,
    // New fields
    val estimatedDownstreamKbps: Int? = null,
    val estimatedUpstreamKbps: Int? = null,
    val isMetered: Boolean? = null,
    val isRoaming: Boolean? = null,
    val isVpn: Boolean? = null,
    val ipAddresses: List<String> = emptyList(),
    val dnsServers: List<String> = emptyList(),
    val mtuBytes: Int? = null,
    val wifiBssid: String? = null,
    val wifiStandard: String? = null
) {
    companion object {
        fun disconnected() = NetworkInfo(
            connectionType = ConnectionType.NONE,
            signalDbm = null,
            signalQuality = SignalQuality.NO_SIGNAL
        )
    }
}
```

- [ ] **Step 2: Extend buildNetworkInfo() to fetch LinkProperties and new capabilities**

The existing `buildNetworkInfo(capabilities: NetworkCapabilities)` needs LinkProperties. Fetch it using the active network in `emitCurrentNetworkInfo()` and pass it alongside capabilities. Update `buildNetworkInfo` signature:

```kotlin
private fun buildNetworkInfo(
    capabilities: NetworkCapabilities,
    linkProperties: android.net.LinkProperties?
): NetworkInfo {
    // ... existing code stays ...

    // New: bandwidth estimates
    val estimatedDownstreamKbps = capabilities.linkDownstreamBandwidthKbps
        .takeIf { it > 0 }
    val estimatedUpstreamKbps = capabilities.linkUpstreamBandwidthKbps
        .takeIf { it > 0 }

    // New: VPN detection
    val isVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

    // New: metered status
    val isMetered = !capabilities.hasCapability(
        NetworkCapabilities.NET_CAPABILITY_NOT_METERED
    )

    // New: roaming (cellular only)
    val isRoaming = if (isCellular) {
        try { telephonyManager?.isNetworkRoaming } catch (_: Exception) { null }
    } else null

    // New: IP addresses from LinkProperties
    val ipAddresses = linkProperties?.linkAddresses
        ?.mapNotNull { it.address?.hostAddress }
        ?: emptyList()

    // New: DNS servers
    val dnsServers = linkProperties?.dnsServers
        ?.mapNotNull { it.hostAddress }
        ?: emptyList()

    // New: MTU (API 29+)
    val mtuBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        linkProperties?.mtu?.takeIf { it > 0 }
    } else null

    // New: BSSID (WiFi only, needs location permission)
    val wifiBssid = if (isWifi && canReadWifiDetails) {
        getBssid(capabilities)
    } else null

    // New: WiFi standard (API 30+)
    val wifiStandard = if (isWifi) getWifiStandard(capabilities) else null

    return NetworkInfo(
        // existing fields ...
        connectionType = connectionType,
        signalDbm = signalDbm,
        signalQuality = signalQuality,
        wifiSsid = wifiInfo?.ssid,
        wifiSpeedMbps = wifiInfo?.speedMbps,
        wifiFrequencyMhz = wifiInfo?.frequencyMhz,
        carrier = cellInfo?.carrier,
        networkSubtype = cellInfo?.networkType,
        // new fields
        estimatedDownstreamKbps = estimatedDownstreamKbps,
        estimatedUpstreamKbps = estimatedUpstreamKbps,
        isMetered = isMetered,
        isRoaming = isRoaming,
        isVpn = isVpn,
        ipAddresses = ipAddresses,
        dnsServers = dnsServers,
        mtuBytes = mtuBytes,
        wifiBssid = wifiBssid,
        wifiStandard = wifiStandard
    )
}
```

- [ ] **Step 3: Update emitCurrentNetworkInfo() to fetch LinkProperties**

```kotlin
private fun kotlinx.coroutines.channels.ProducerScope<NetworkInfo>.emitCurrentNetworkInfo() {
    val activeNetwork = connectivityManager.activeNetwork
    val capabilities = activeNetwork?.let {
        connectivityManager.getNetworkCapabilities(it)
    }
    val linkProperties = activeNetwork?.let {
        connectivityManager.getLinkProperties(it)
    }
    trySend(
        if (capabilities != null) buildNetworkInfo(capabilities, linkProperties)
        else NetworkInfo.disconnected()
    )
}
```

- [ ] **Step 4: Add getBssid() helper**

```kotlin
@Suppress("DEPRECATION")
private fun getBssid(capabilities: NetworkCapabilities): String? {
    // API 31+: try transportInfo
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val wifiInfo = capabilities.transportInfo as? WifiInfo
        val bssid = wifiInfo?.bssid
        if (bssid != null && bssid != "02:00:00:00:00:00") return bssid
    }
    // Fallback
    val bssid = wifiManager?.connectionInfo?.bssid
    return if (bssid != null && bssid != "02:00:00:00:00:00") bssid else null
}
```

- [ ] **Step 5: Add getWifiStandard() helper**

```kotlin
@Suppress("DEPRECATION")
private fun getWifiStandard(capabilities: NetworkCapabilities): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    val wifiInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        capabilities.transportInfo as? WifiInfo
    } else {
        wifiManager?.connectionInfo
    } ?: return null
    return when (wifiInfo.wifiStandard) {
        4 -> "WiFi 4 (n)"
        5 -> "WiFi 5 (ac)"
        6 -> "WiFi 6 (ax)"
        7 -> "WiFi 6E (ax)"
        8 -> "WiFi 7 (be)"
        else -> null
    }
}
```

Note: `WifiInfo.WIFI_STANDARD_*` constants are `4`, `5`, `6`, `7`, `8` (API 30+). `getWifiStandard()` requires API 30 (R).

- [ ] **Step 6: Commit**

```
git add app/src/main/java/com/runcheck/data/network/NetworkDataSource.kt
git commit -m "Lisää LinkProperties- ja NetworkCapabilities-kenttien luku NetworkDataSourceen"
```

---

### Task 3: Update NetworkRepositoryImpl to pass new fields

**Files:**
- Modify: `app/src/main/java/com/runcheck/data/network/NetworkRepositoryImpl.kt`

- [ ] **Step 1: Add new fields to getNetworkState() mapping**

In the `.map { info -> NetworkState(...) }` block, add:

```kotlin
.map { info ->
    NetworkState(
        connectionType = info.connectionType,
        signalDbm = info.signalDbm,
        signalQuality = info.signalQuality,
        wifiSsid = info.wifiSsid,
        wifiSpeedMbps = info.wifiSpeedMbps,
        wifiFrequencyMhz = info.wifiFrequencyMhz,
        carrier = info.carrier,
        networkSubtype = info.networkSubtype,
        latencyMs = null,
        // New
        estimatedDownstreamKbps = info.estimatedDownstreamKbps,
        estimatedUpstreamKbps = info.estimatedUpstreamKbps,
        isMetered = info.isMetered,
        isRoaming = info.isRoaming,
        isVpn = info.isVpn,
        ipAddresses = info.ipAddresses,
        dnsServers = info.dnsServers,
        mtuBytes = info.mtuBytes,
        wifiBssid = info.wifiBssid,
        wifiStandard = info.wifiStandard
    )
}
```

- [ ] **Step 2: Commit**

```
git add app/src/main/java/com/runcheck/data/network/NetworkRepositoryImpl.kt
git commit -m "Välitä uudet verkkokentät NetworkRepositoryImpl-mappauksessa"
```

---

### Task 4: Add getReadingsSince() to NetworkRepository + GetNetworkHistoryUseCase

**Files:**
- Modify: `app/src/main/java/com/runcheck/domain/repository/NetworkRepository.kt`
- Modify: `app/src/main/java/com/runcheck/data/network/NetworkRepositoryImpl.kt`
- Create: `app/src/main/java/com/runcheck/domain/usecase/GetNetworkHistoryUseCase.kt`

- [ ] **Step 1: Add getReadingsSince to NetworkRepository interface**

```kotlin
interface NetworkRepository {
    fun getNetworkState(): Flow<NetworkState>
    suspend fun measureLatency(): Int?
    suspend fun saveReading(state: NetworkState)
    suspend fun getAllReadings(): List<NetworkReadingData>
    suspend fun deleteOlderThan(cutoff: Long)
    fun getReadingsSince(since: Long, limit: Int? = null): Flow<List<NetworkReadingData>>
}
```

- [ ] **Step 2: Implement in NetworkRepositoryImpl**

Add to `NetworkRepositoryImpl`:

```kotlin
override fun getReadingsSince(since: Long, limit: Int?): Flow<List<NetworkReadingData>> {
    return networkReadingDao.getReadingsSince(since).map { entities ->
        val mapped = entities.map { it.toDomain() }
        if (limit != null) mapped.takeLast(limit) else mapped
    }
}
```

Add `import kotlinx.coroutines.flow.map` if not present.

- [ ] **Step 3: Create GetNetworkHistoryUseCase**

File: `app/src/main/java/com/runcheck/domain/usecase/GetNetworkHistoryUseCase.kt`

```kotlin
package com.runcheck.domain.usecase

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.repository.NetworkReadingData
import com.runcheck.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNetworkHistoryUseCase @Inject constructor(
    private val networkRepository: NetworkRepository
) {
    operator fun invoke(period: HistoryPeriod = HistoryPeriod.DAY): Flow<List<NetworkReadingData>> {
        val since = if (period == HistoryPeriod.ALL) 0L
            else System.currentTimeMillis() - period.durationMs
        val limit = if (period == HistoryPeriod.ALL) MAX_HISTORY_POINTS else null
        return networkRepository.getReadingsSince(since, limit)
    }

    companion object {
        private const val MAX_HISTORY_POINTS = 5_000
    }
}
```

- [ ] **Step 4: Commit**

```
git add app/src/main/java/com/runcheck/domain/repository/NetworkRepository.kt \
      app/src/main/java/com/runcheck/data/network/NetworkRepositoryImpl.kt \
      app/src/main/java/com/runcheck/domain/usecase/GetNetworkHistoryUseCase.kt
git commit -m "Lisää GetNetworkHistoryUseCase ja getReadingsSince repository-rajapintaan"
```

---

## Chunk 2: Theme & Components

### Task 5: Add statusColorForSignalQuality to StatusColors

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/theme/StatusColors.kt`

- [ ] **Step 1: Add the function after existing statusColorFor* functions**

```kotlin
@Composable
@ReadOnlyComposable
fun statusColorForSignalQuality(quality: SignalQuality): Color {
    val colors = MaterialTheme.statusColors
    return when (quality) {
        SignalQuality.EXCELLENT -> colors.healthy
        SignalQuality.GOOD -> colors.healthy
        SignalQuality.FAIR -> colors.fair
        SignalQuality.POOR -> colors.poor
        SignalQuality.NO_SIGNAL -> colors.critical
    }
}
```

Add import: `import com.runcheck.domain.model.SignalQuality`

- [ ] **Step 2: Commit**

```
git add app/src/main/java/com/runcheck/ui/theme/StatusColors.kt
git commit -m "Lisää statusColorForSignalQuality teemafunktioksi"
```

---

### Task 6: Create SignalBars component

**Files:**
- Create: `app/src/main/java/com/runcheck/ui/components/SignalBars.kt`

- [ ] **Step 1: Create the composable**

```kotlin
package com.runcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.runcheck.domain.model.SignalQuality
import com.runcheck.ui.theme.statusColorForSignalQuality

private val BAR_HEIGHTS = listOf(10.dp, 18.dp, 26.dp, 36.dp, 48.dp)
private val BAR_WIDTH = 12.dp
private val BAR_GAP = 4.dp
private val BAR_CORNER = 3.dp

private fun activeBarsFor(quality: SignalQuality): Int = when (quality) {
    SignalQuality.EXCELLENT -> 5
    SignalQuality.GOOD -> 4
    SignalQuality.FAIR -> 3
    SignalQuality.POOR -> 2
    SignalQuality.NO_SIGNAL -> 0
}

@Composable
fun SignalBars(
    signalQuality: SignalQuality,
    qualityLabel: String,
    modifier: Modifier = Modifier
) {
    val activeBars = activeBarsFor(signalQuality)
    val activeColor = statusColorForSignalQuality(signalQuality)
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Row(
        modifier = modifier.semantics {
            contentDescription = "$qualityLabel, $activeBars of 5 bars"
        },
        horizontalArrangement = Arrangement.spacedBy(BAR_GAP),
        verticalAlignment = Alignment.Bottom
    ) {
        BAR_HEIGHTS.forEachIndexed { index, height ->
            val isActive = index < activeBars
            Box(
                modifier = Modifier
                    .width(BAR_WIDTH)
                    .height(height)
                    .background(
                        color = if (isActive) activeColor else inactiveColor,
                        shape = RoundedCornerShape(BAR_CORNER)
                    )
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```
git add app/src/main/java/com/runcheck/ui/components/SignalBars.kt
git commit -m "Luo SignalBars-komponentti signaalinlaadun visualisointiin"
```

---

### Task 7: Add string resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-fi/strings.xml`

- [ ] **Step 1: Add English strings**

Add after the existing network strings section:

```xml
<!-- Network Detail Screen - Redesign -->
<string name="network_section_connection_details">Connection Details</string>
<string name="network_section_ip_dns">IP Address &amp; DNS</string>
<string name="network_section_signal_history">Signal History</string>
<string name="network_section_speed_test">Speed Test</string>
<string name="network_bssid">BSSID</string>
<string name="network_est_bandwidth_down">Est. Bandwidth ↓</string>
<string name="network_est_bandwidth_up">Est. Bandwidth ↑</string>
<string name="network_metered">Metered</string>
<string name="network_roaming">Roaming</string>
<string name="network_vpn">VPN</string>
<string name="network_ipv4">IPv4</string>
<string name="network_ipv6">IPv6</string>
<string name="network_dns_1">DNS 1</string>
<string name="network_dns_2">DNS 2</string>
<string name="network_mtu">MTU</string>
<string name="network_history_metric_signal">Signal</string>
<string name="network_history_metric_latency">Latency</string>
<string name="network_history_empty">Not enough data yet</string>
<string name="network_speed_test_jitter">Jitter</string>
<string name="network_speed_test_server">Server: %1$s</string>
<string name="network_speed_test_no_results">No speed test results yet</string>
<string name="network_wifi_standard">WiFi Standard</string>
<string name="unit_mhz">MHz</string>
<string name="common_yes">Yes</string>
<string name="common_no">No</string>
```

- [ ] **Step 2: Add Finnish strings**

```xml
<!-- Network Detail Screen - Redesign -->
<string name="network_section_connection_details">Yhteystiedot</string>
<string name="network_section_ip_dns">IP-osoite ja DNS</string>
<string name="network_section_signal_history">Signaalihistoria</string>
<string name="network_section_speed_test">Nopeustesti</string>
<string name="network_bssid">BSSID</string>
<string name="network_est_bandwidth_down">Arvioitu kaista ↓</string>
<string name="network_est_bandwidth_up">Arvioitu kaista ↑</string>
<string name="network_metered">Mittaroitu</string>
<string name="network_roaming">Roaming</string>
<string name="network_vpn">VPN</string>
<string name="network_ipv4">IPv4</string>
<string name="network_ipv6">IPv6</string>
<string name="network_dns_1">DNS 1</string>
<string name="network_dns_2">DNS 2</string>
<string name="network_mtu">MTU</string>
<string name="network_history_metric_signal">Signaali</string>
<string name="network_history_metric_latency">Latenssi</string>
<string name="network_history_empty">Ei tarpeeksi dataa vielä</string>
<string name="network_speed_test_jitter">Jitter</string>
<string name="network_speed_test_server">Palvelin: %1$s</string>
<string name="network_speed_test_no_results">Ei nopeustestituloksia vielä</string>
<string name="network_wifi_standard">WiFi-standardi</string>
<string name="unit_mhz">MHz</string>
<string name="common_yes">Kyllä</string>
<string name="common_no">Ei</string>
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values/strings.xml app/src/main/res/values-fi/strings.xml
git commit -m "Lisää network-uudistuksen merkkijonoresurssit (EN + FI)"
```

---

## Chunk 3: ViewModel & UiState

### Task 8: Extend NetworkUiState and NetworkViewModel

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/network/NetworkUiState.kt`
- Modify: `app/src/main/java/com/runcheck/ui/network/NetworkViewModel.kt`

- [ ] **Step 1: Extend NetworkUiState.Success**

```kotlin
@Immutable
data class Success(
    val networkState: NetworkState,
    val signalHistory: List<NetworkReadingData> = emptyList(),
    val selectedHistoryPeriod: HistoryPeriod = HistoryPeriod.DAY
) : NetworkUiState
```

Add imports:
```kotlin
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.repository.NetworkReadingData
```

- [ ] **Step 2: Add history loading to NetworkViewModel**

Add `GetNetworkHistoryUseCase` injection:

```kotlin
@HiltViewModel
class NetworkViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getMeasuredNetworkState: GetMeasuredNetworkStateUseCase,
    private val runSpeedTest: RunSpeedTestUseCase,
    private val getSpeedTestHistory: GetSpeedTestHistoryUseCase,
    private val finalizeSpeedTest: FinalizeSpeedTestUseCase,
    private val proStatusProvider: ProStatusProvider,
    private val getNetworkHistory: GetNetworkHistoryUseCase  // NEW
) : ViewModel() {
```

Add history state:

```kotlin
private var selectedHistoryPeriod = HistoryPeriod.DAY
private var historyNetworkJob: Job? = null
```

Add `setHistoryPeriod` function:

```kotlin
fun setHistoryPeriod(period: HistoryPeriod) {
    selectedHistoryPeriod = period
    loadNetworkData()
}
```

- [ ] **Step 3: Update loadNetworkData() to combine network state with history**

Replace the current `loadNetworkData()` to combine both flows:

```kotlin
private fun loadNetworkData() {
    networkJob?.cancel()
    historyNetworkJob?.cancel()
    if (_networkUiState.value !is NetworkUiState.Success) {
        _networkUiState.value = NetworkUiState.Loading
    }
    networkJob = viewModelScope.launch {
        getMeasuredNetworkState()
            .catch { e ->
                if (_networkUiState.value !is NetworkUiState.Success) {
                    _networkUiState.value = NetworkUiState.Error(
                        e.messageOr(context.getString(R.string.common_error_generic))
                    )
                }
            }
            .collect { state ->
                _networkUiState.update { current ->
                    val existing = current as? NetworkUiState.Success
                    NetworkUiState.Success(
                        networkState = state,
                        signalHistory = existing?.signalHistory ?: emptyList(),
                        selectedHistoryPeriod = selectedHistoryPeriod
                    )
                }
            }
    }
    historyNetworkJob = viewModelScope.launch {
        getNetworkHistory(selectedHistoryPeriod)
            .catch { /* silently ignore history errors */ }
            .collect { readings ->
                _networkUiState.update { current ->
                    (current as? NetworkUiState.Success)?.copy(
                        signalHistory = readings,
                        selectedHistoryPeriod = selectedHistoryPeriod
                    ) ?: current
                }
            }
    }
}
```

Add import: `import kotlinx.coroutines.flow.update`

- [ ] **Step 4: Commit**

```
git add app/src/main/java/com/runcheck/ui/network/NetworkUiState.kt \
      app/src/main/java/com/runcheck/ui/network/NetworkViewModel.kt
git commit -m "Laajenna NetworkViewModel signaalihistorian tuella"
```

---

## Chunk 4: NetworkDetailScreen Rewrite

### Task 9: Rewrite NetworkDetailScreen — Hero card

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt`

- [ ] **Step 1: Add NetworkPanel helper composable**

Same pattern as BatteryPanel in BatteryDetailScreen:

```kotlin
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
```

- [ ] **Step 2: Create NetworkHeroSection composable**

```kotlin
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
                    text = stringResource(R.string.value_with_unit_int, dbm, stringResource(R.string.unit_dbm)),
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
                value = networkState.latencyMs?.let {
                    stringResource(R.string.value_with_unit_int, it, stringResource(R.string.unit_ms))
                } ?: "—",
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
```

- [ ] **Step 3: Add hero helper functions**

```kotlin
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
    ConnectionType.WIFI -> state.wifiSpeedMbps?.let {
        stringResource(R.string.value_with_unit_int, it, stringResource(R.string.unit_mbps))
    } ?: "—"
    ConnectionType.CELLULAR -> state.estimatedDownstreamKbps?.let {
        stringResource(R.string.value_with_unit_int, it / 1000, stringResource(R.string.unit_mbps))
    } ?: "—"
    ConnectionType.NONE -> "—"
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
    } ?: "—"
    ConnectionType.CELLULAR -> state.networkSubtype ?: "—"
    ConnectionType.NONE -> "—"
}
```

- [ ] **Step 4: Commit**

```
git add app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt
git commit -m "Lisää NetworkHeroSection signal bars -visualisoinnilla"
```

---

### Task 10: Add Connection Details card

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt`

- [ ] **Step 1: Create ConnectionDetailsCard composable**

```kotlin
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

        // IP & DNS section
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
```

- [ ] **Step 2: Commit**

```
git add app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt
git commit -m "Lisää ConnectionDetailsCard yhteystiedoilla ja IP/DNS-osiolla"
```

---

### Task 11: Add Signal History card

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt`

- [ ] **Step 1: Add NetworkHistoryMetric enum**

```kotlin
private enum class NetworkHistoryMetric {
    SIGNAL,
    LATENCY
}
```

- [ ] **Step 2: Create SignalHistoryCard composable**

```kotlin
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
                text = "${historyPeriodLabel(selectedPeriod)} · ${networkHistoryMetricLabel(metric)}",
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
```

- [ ] **Step 3: Commit**

```
git add app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt
git commit -m "Lisää SignalHistoryCard TrendChart-signaalikaaviolla"
```

---

### Task 12: Add Speed Test card

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt`

- [ ] **Step 1: Create SpeedTestSummaryCard composable**

```kotlin
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
                .joinToString(" · ")
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
```

- [ ] **Step 2: Commit**

```
git add app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt
git commit -m "Lisää SpeedTestSummaryCard viimeisimmän tuloksen yhteenvedolla"
```

---

### Task 13: Rewrite NetworkContent to assemble all cards

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt`

- [ ] **Step 1: Update NetworkDetailScreen to collect speedTestState**

In `NetworkDetailScreen`, add:

```kotlin
val speedTestState by viewModel.speedTestState.collectAsStateWithLifecycle()
```

Pass it to `NetworkContent`:

```kotlin
NetworkContent(
    state = state,
    speedTestState = speedTestState,
    onRefresh = { viewModel.refresh() },
    onNavigateToSpeedTest = onNavigateToSpeedTest,
    onPeriodChange = { viewModel.setHistoryPeriod(it) }
)
```

- [ ] **Step 2: Rewrite NetworkContent composable**

Replace the entire `NetworkContent` body with the new card layout:

```kotlin
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
```

- [ ] **Step 3: Remove old MetricTile-based content, SpeedTestEntryCard, and unused composables**

Delete the old `MetricTile`-based body from `NetworkContent`. Keep `WifiNameHelpCard`, `hasPermission()`, `isLocationEnabled()`, and the speed test helper composables (`LastResultCard`, `ResultMetric`, `HistoryResultRow`, `CellularDataWarningDialog`). Remove `SpeedTestEntryCard` (replaced by `SpeedTestSummaryCard`).

- [ ] **Step 4: Update imports**

Add needed imports, remove unused ones. Key new imports:

```kotlin
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.saveable.rememberSaveable
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.repository.NetworkReadingData
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.MetricPill
import com.runcheck.ui.components.MetricRow
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.components.SignalBars
import com.runcheck.ui.components.TrendChart
import com.runcheck.ui.theme.statusColorForSignalQuality
import androidx.compose.ui.text.font.FontWeight
```

- [ ] **Step 5: Commit**

```
git add app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt
git commit -m "Kirjoita NetworkDetailScreen uudelleen korttityylillä"
```

---

## Chunk 5: Verification

### Task 14: Verify build compiles

- [ ] **Step 1: Run build** (user runs in their terminal)

```
./gradlew assembleDebug
```

- [ ] **Step 2: Fix any compilation errors**

- [ ] **Step 3: Commit any fixes**

---

### Task 15: Manual testing checklist

- [ ] WiFi: hero shows signal bars with correct color + quality label + dBm
- [ ] WiFi: MetricPills show latency, link speed, frequency band
- [ ] WiFi: Connection Details shows SSID, BSSID, frequency, link speed, bandwidth, metered, VPN
- [ ] WiFi: IP/DNS section shows IPv4, IPv6, DNS servers, MTU
- [ ] Cellular: hero adapts — MetricPills show latency, estimated bandwidth, network type
- [ ] Cellular: Connection Details shows carrier, technology, roaming, bandwidth, metered
- [ ] No connection: hero shows all bars inactive, "No connection", pills show "—"
- [ ] Signal History: chart renders with Signal metric selected
- [ ] Signal History: switching to Latency metric updates chart
- [ ] Signal History: period change (Day/Week/Month/All) reloads data
- [ ] Signal History: empty state shows when < 2 data points
- [ ] Speed Test card: shows last result with download/upload/ping/jitter
- [ ] Speed Test card: shows "No results yet" when no history
- [ ] Speed Test card: button navigates to SpeedTestScreen
- [ ] WiFi name help card still appears when SSID unavailable
- [ ] Pull to refresh works
- [ ] Ad banner shows at bottom for free tier
