# Network Detail Screen Redesign

## Overview

Redesign the NetworkDetailScreen from a flat list of MetricTiles into a visually rich, card-based layout matching the style of HomeScreen and BatteryDetailScreen. Add all available network data including previously unused Android APIs and database history.

## Current State

NetworkDetailScreen displays a flat list of `MetricTile` components with no visual hierarchy, no cards, no charts, and no hero section. Data collected in the database (signal history, latency history) and speed test jitter are not surfaced. Many Android network APIs (LinkProperties, NetworkCapabilities) are not utilized.

## Design

### 1. Hero Card (NetworkPanel)

Top-level card with signal visualization and key metrics at a glance.

**Signal Bars component:**
- New `SignalBars` composable: 5 vertical bars of increasing height
- Total size: 80×48dp. Individual bars: 12dp wide, 4dp gap, corner radius 3dp
- Heights: 10dp, 18dp, 26dp, 36dp, 48dp (increasing)
- Active bars filled with color from `statusColorForSignalQuality()`
- Inactive bars use `surfaceVariant` at 0.3 alpha
- Color mapping via new `statusColorForSignalQuality()` in StatusColors.kt:
  - EXCELLENT → `colors.healthy`
  - GOOD → `colors.healthy`
  - FAIR → `colors.fair`
  - POOR → `colors.poor`
  - NO_SIGNAL → `colors.critical`
- Bar count mapping: EXCELLENT=5, GOOD=4, FAIR=3, POOR=2, NO_SIGNAL=0
- Below bars: SignalQuality text label + raw dBm value
- Accessibility: `contentDescription = "Signal quality: {quality}, {activeBars} of 5 bars"`

**MetricPill row (3 pills):**
- Latency (ms)
- Bandwidth: WiFi → link speed (Mbps), Cellular → estimated downstream (Mbps)
- Band: WiFi → frequency band ("5 GHz"), Cellular → network technology ("5G" / "LTE")

Layout: `Row` with `Arrangement.spacedBy(spacing.base)`, each pill `Modifier.weight(1f)` — same as BatteryHeroSection MetricPill row.

**Disconnected state (ConnectionType.NONE):**
- Signal bars: all inactive (0 active bars), color `colors.critical`
- Quality label: "No connection"
- MetricPills show "—" for all values

### 2. Connection Details Card

Single card with two sections separated by a HorizontalDivider.

**Section A: Connection Details** (CardSectionTitle)

MetricRow items, shown conditionally based on connection type:

| Metric | WiFi | Cellular | Source |
|--------|------|----------|--------|
| Connection Type | Yes | Yes | NetworkState.connectionType |
| Network (SSID / Carrier) | SSID | Carrier | wifiSsid / carrier |
| BSSID | Yes | No | NEW: wifiBssid |
| Technology | WiFi standard | 5G/LTE/HSPA | networkSubtype |
| Frequency | Yes (MHz) | No | wifiFrequencyMhz |
| Link Speed | Yes (Mbps) | No | wifiSpeedMbps |
| Est. Bandwidth ↓ | Yes | Yes | NEW: estimatedDownstreamKbps |
| Est. Bandwidth ↑ | Yes | Yes | NEW: estimatedUpstreamKbps |
| Roaming | No | Yes | NEW: isRoaming |
| Metered | Yes | Yes | NEW: isMetered |
| VPN | Yes | Yes | NEW: isVpn |

Rows with null/unavailable data are hidden entirely (no "N/A" text).

Note: Estimated bandwidth values from `NetworkCapabilities` are OS-level estimates and may not reflect actual throughput on all devices.

**Section B: IP Address & DNS** (CardSectionTitle)

| Metric | Source |
|--------|--------|
| IPv4 | NEW: ipAddresses (filtered) |
| IPv6 | NEW: ipAddresses (filtered) |
| DNS 1 | NEW: dnsServers[0] |
| DNS 2 | NEW: dnsServers[1] |
| MTU | NEW: mtuBytes (API 29+, null on older) |

All rows hidden if data unavailable.

### 3. Signal History Card

Historical signal and latency data from the Room database. All free — no Pro gating. This is intentionally different from BatteryHistoryPanel which Pro-gates extended history; network history is simpler data and serves as a value demonstration for the app.

**Metric selector:** `androidx.compose.material3.FilterChip` row with two options:
- Signal (dBm)
- Latency (ms)

Metric selection is local UI state via `rememberSaveable` (same pattern as `BatteryDetailScreen.selectedHistoryMetric`).

**Period selector:** FilterChip row:
- 24h / Week / Month / All

Period selection lives in the ViewModel and triggers a new DAO query (same pattern as `BatteryDetailScreen.selectedPeriod`).

**Chart:** Reuses existing `TrendChart` composable. Data sourced from new `GetNetworkHistoryUseCase` → `NetworkRepository.getReadingsSince()` → `NetworkReadingDao.getReadingsSince()`.

**Empty state:** Text "Not enough data yet" when < 2 data points.

Active label above chart: "{period} · {metric}" in `labelLarge` + `primary` color (matching BatteryHistoryPanel's "{period} · {metric}" order).

**NetworkHistoryMetric enum:** Defined as a private enum in `NetworkDetailScreen.kt` (matching the `BatteryHistoryMetric` pattern in `BatteryDetailScreen.kt`):

```kotlin
private enum class NetworkHistoryMetric {
    SIGNAL,
    LATENCY
}
```

### 4. Speed Test Card

Surfaces the most recent speed test result directly on the network screen.

Speed test data is accessed via the existing `NetworkViewModel.speedTestState: StateFlow<SpeedTestUiState>`, which is already collected in the ViewModel. The `NetworkDetailScreen` composable collects it as a separate state flow alongside `networkUiState`.

**When result exists:**
- MetricPill row: Download (Mbps) / Upload (Mbps) / Ping (ms) — `Row` with `weight(1f)` per pill
- Additional MetricPill: Jitter (ms) — currently collected and stored but not shown
- Server info: "Server: {name} · {location}" in bodySmall
- Timestamp: formatted date/time in bodySmall
- Button: navigates to full SpeedTestScreen

**When no result exists:**
- Text: "No speed test results yet"
- Button: navigates to SpeedTestScreen

### 5. Ad Banner + Bottom Spacing

Existing `DetailScreenAdBanner()` + `Spacer(xl)` at the bottom, same as current.

## NetworkState Model Extensions

```kotlin
data class NetworkState(
    // Existing fields
    val connectionType: ConnectionType,
    val signalDbm: Int?,
    val signalQuality: SignalQuality,
    val wifiSsid: String?,
    val wifiSpeedMbps: Int?,
    val wifiFrequencyMhz: Int?,
    val carrier: String?,
    val networkSubtype: String?,
    val latencyMs: Int?,
    // New fields
    val estimatedDownstreamKbps: Int? = null,
    val estimatedUpstreamKbps: Int? = null,
    val isMetered: Boolean? = null,
    val isRoaming: Boolean? = null,
    val isVpn: Boolean? = null,
    val ipAddresses: List<String> = emptyList(),
    val dnsServers: List<String> = emptyList(),
    val mtuBytes: Int? = null,
    val wifiBssid: String? = null
)
```

## Data Layer Changes

### NetworkDataSource

Extend `buildNetworkInfo()` to also receive `LinkProperties` (fetched via `connectivityManager.getLinkProperties(network)` alongside the existing `NetworkCapabilities`). The intermediate `NetworkInfo` data class in `NetworkDataSource` must be extended with matching new fields.

New data sources:

- `LinkProperties.getAddresses()` → ipAddresses (IPv4/IPv6 strings via `InetAddress.hostAddress`)
- `LinkProperties.getDnsServers()` → dnsServers (via `InetAddress.hostAddress`)
- `LinkProperties.getMtu()` → mtuBytes (**requires API 29 guard**: `Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q`, null on API 26–28)
- `NetworkCapabilities.getLinkDownstreamBandwidthKbps()` → estimatedDownstreamKbps
- `NetworkCapabilities.getLinkUpstreamBandwidthKbps()` → estimatedUpstreamKbps
- `NetworkCapabilities.hasTransport(TRANSPORT_VPN)` → isVpn
- `ConnectivityManager.isActiveNetworkMetered` → isMetered
- `TelephonyManager.isNetworkRoaming` → isRoaming
- `WifiInfo.getBSSID()` → wifiBssid

All new fields are nullable/defaulted — graceful degradation if API unavailable.

### NetworkRepositoryImpl

Extend `getNetworkState()` mapping from `NetworkInfo` → `NetworkState` to pass through the new fields.

### New: GetNetworkHistoryUseCase

Create `GetNetworkHistoryUseCase` following the `GetBatteryHistoryUseCase` pattern:
- Calls `NetworkRepository.getReadingsSince(since: Long, limit: Int?)`
- Returns `Flow<List<NetworkReadingData>>` (domain model, not Room entity)
- Repository/DAO maps `NetworkReadingEntity` → `NetworkReadingData` at the data layer boundary
- Applies `MAX_HISTORY_POINTS = 5000` cap for the ALL period to prevent loading unbounded data

### New: NetworkRepository.getReadingsSince()

Add `getReadingsSince(since: Long, limit: Int? = null): Flow<List<NetworkReadingData>>` to the `NetworkRepository` interface. The DAO method already exists but the repository does not expose it. The repository implementation maps entities to domain models.

### NetworkViewModel

- Add signal history loading from new `GetNetworkHistoryUseCase`
- Add `selectedHistoryPeriod` as a private var (matching BatteryViewModel pattern), calls reload on change via `setHistoryPeriod(period)` function
- Expose latest speed test result via existing `speedTestState` StateFlow (already present)

### NetworkUiState

Extend `Success` state with:
- `signalHistory: List<NetworkReadingData>` (domain model, for chart)
- `selectedHistoryPeriod: HistoryPeriod`

Note: `selectedHistoryMetric` is local UI state in the screen composable, not in UiState (matching the battery pattern).

## New Components

### SignalBars

```
File: app/src/main/java/com/runcheck/ui/components/SignalBars.kt
```

- 5 vertical bars with increasing height, bottom-aligned
- Total composable size: 80×48dp
- Individual bar: 12dp wide, 4dp gap between bars, 3dp corner radius
- Bar heights: 10dp, 18dp, 26dp, 36dp, 48dp
- Parameters: `signalQuality: SignalQuality`, `modifier: Modifier`
- Implementation: Row of Box elements with background color
- Active bars: color from `statusColorForSignalQuality()`
- Inactive bars: `surfaceVariant` at 0.3 alpha
- Bar count mapping: EXCELLENT=5, GOOD=4, FAIR=3, POOR=2, NO_SIGNAL=0
- Accessibility: `semantics { contentDescription = "Signal quality: $qualityLabel, $activeBars of 5 bars" }`

### statusColorForSignalQuality()

```
File: app/src/main/java/com/runcheck/ui/theme/StatusColors.kt (add to existing)
```

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

## Reused Components (no changes needed)

- `CardSectionTitle` — section headers inside cards
- `MetricPill` — compact metric display
- `MetricRow` — label + value rows
- `SectionHeader` — card-level headers (e.g., "NETWORK")
- `TrendChart` — line chart for history
- `androidx.compose.material3.FilterChip` — M3 standard component for metric/period selectors
- `DetailTopBar` — back navigation + title
- `PullToRefreshWrapper` — pull to refresh

## Card Styling

All cards follow the same pattern as BatteryDetailScreen (`NetworkPanel` helper composable):

```kotlin
Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    shape = RoundedCornerShape(16.dp)
)
```

Internal padding: `MaterialTheme.spacing.base`, spacing between sections: `MaterialTheme.spacing.sm`.

## String Resources

New strings needed in both `strings.xml` (EN) and `values-fi/strings.xml` (FI):

- Signal history section title
- History metric labels (Signal, Latency)
- Connection details section title
- IP & DNS section title
- Labels for all new MetricRows (BSSID, Est. Bandwidth ↓/↑, Metered, Roaming, VPN, IPv4, IPv6, DNS 1/2, MTU)
- Speed test card section title
- Jitter label
- Server/timestamp labels
- Empty history state text
- Yes/No for boolean fields
- No connection state text
- Signal quality labels (if not already present)

## Permissions

No new permissions required. All new data comes from APIs already accessible:
- `ConnectivityManager` — no permission needed
- `LinkProperties` — no permission needed
- `TelephonyManager.isNetworkRoaming` — no permission needed (basic info)
- `WifiInfo.getBSSID()` — requires existing location permission (already prompted for SSID)

## WiFi Name Help Card

The existing `WifiNameHelpCard` (location permission prompt) is preserved. It appears in the Connection Details card when WiFi SSID is null, same logic as current implementation.
