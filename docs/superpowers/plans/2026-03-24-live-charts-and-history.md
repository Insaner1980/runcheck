# Live Charts & History Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add real-time animated LiveCharts and TrendChart history across all four detail screens — battery (level, temp, voltage), network (signal), thermal (Room history), and storage (usage history).

**Architecture:** Extend existing ring buffer + LiveChart pattern (already in battery current/power and thermal temp/headroom) to new metrics. For thermal and storage history, expose existing Room DAO `getReadingsSince` queries through repository interfaces and use cases, then render with TrendChart + period chips following the network history pattern.

**Tech Stack:** Kotlin, Jetpack Compose, Room, StateFlow, LiveChart (custom), TrendChart (custom), HistoryPeriod enum

---

## File Map

### Task 1 — Battery live charts (level, temp, voltage)
- Modify: `ui/battery/BatteryViewModel.kt` — add `liveLevel`, `liveVoltage` ring buffers
- Modify: `ui/battery/BatteryUiState.kt` — add `liveLevel`, `liveVoltage` fields
- Modify: `ui/battery/BatteryDetailScreen.kt` — add 3 LiveCharts (level, temp, voltage) in session card

### Task 2 — Network live signal chart
- Modify: `ui/network/NetworkViewModel.kt` — add `liveSignalDbm` ring buffer
- Modify: `ui/network/NetworkUiState.kt` — add `liveSignalDbm` field
- Modify: `ui/network/NetworkDetailScreen.kt` — add LiveChart below hero card

### Task 3 — Thermal history (TrendChart)
- Modify: `domain/repository/ThermalRepository.kt` — add `getReadingsSince(since, limit)`
- Modify: `data/thermal/ThermalRepositoryImpl.kt` — implement `getReadingsSince`
- Create: `domain/usecase/GetThermalHistoryUseCase.kt` — period-based query (mirrors GetNetworkHistoryUseCase)
- Modify: `ui/chart/ChartRenderModel.kt` — add `buildThermalHistoryChartModel()`
- Modify: `ui/thermal/ThermalUiState.kt` — add history fields
- Modify: `ui/thermal/ThermalViewModel.kt` — inject use case, collect history, period selection
- Modify: `ui/thermal/ThermalDetailScreen.kt` — add TrendChart + period/metric chips

### Task 4 — Storage history (TrendChart)
- Modify: `domain/repository/StorageRepository.kt` — add `getReadingsSince(since, limit)`
- Modify: `data/storage/StorageRepositoryImpl.kt` — implement `getReadingsSince`
- Create: `domain/usecase/GetStorageHistoryUseCase.kt` — period-based query
- Modify: `ui/chart/ChartRenderModel.kt` — add `buildStorageHistoryChartModel()`
- Modify: `ui/storage/StorageUiState.kt` — add history fields
- Modify: `ui/storage/StorageViewModel.kt` — inject use case, collect history, period selection
- Modify: `ui/storage/StorageDetailScreen.kt` — add TrendChart + period chips

### Shared across Tasks 3 & 4
- Modify: `data/db/dao/ThermalReadingDao.kt` — add `getReadingsSinceLimited` query
- Modify: `data/db/dao/StorageReadingDao.kt` — add `getReadingsSinceLimited` query
- Modify: `ui/chart/ChartHelpers.kt` — add `thermalQualityZones()`, `thermalHistoryMetricLabel()`, `storageHistoryMetricLabel()`

---

## Reference Patterns

### Ring buffer (from BatteryViewModel)
```kotlin
private val liveXxx = mutableListOf<Float>()
private const val LIVE_CHART_MAX_POINTS = 60

private fun appendLive(buffer: MutableList<Float>, value: Float) {
    buffer.add(value)
    if (buffer.size > LIVE_CHART_MAX_POINTS) buffer.removeFirst()
}
```

### History use case (from GetNetworkHistoryUseCase)
```kotlin
operator fun invoke(period: HistoryPeriod): Flow<List<Reading>> {
    val since = when (period) {
        HistoryPeriod.ALL, HistoryPeriod.SINCE_UNPLUG -> 0L
        else -> System.currentTimeMillis() - period.durationMs
    }
    val limit = if (period == HistoryPeriod.ALL) 5_000 else null
    return repository.getReadingsSince(since, limit)
}
```

### Chart model builder (from buildNetworkHistoryChartModel)
```kotlin
fun buildXxxHistoryChartModel(
    history: List<Reading>,
    metric: MetricEnum,
    period: HistoryPeriod,
    maxPoints: Int
): ChartRenderModel { ... }
```

---

## Tasks

### Task 1: Battery — Add live level, temperature, and voltage charts

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/battery/BatteryUiState.kt`
- Modify: `app/src/main/java/com/runcheck/ui/battery/BatteryViewModel.kt`
- Modify: `app/src/main/java/com/runcheck/ui/battery/BatteryDetailScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add UiState fields**

In `BatteryUiState.kt`, add to `Success`:
```kotlin
val liveLevel: List<Float> = emptyList(),
val liveVoltage: List<Float> = emptyList(),
```
(`liveTempC` already exists)

- [ ] **Step 2: Add ring buffers and populate them in ViewModel**

In `BatteryViewModel.kt`, add alongside existing ring buffers (line ~65):
```kotlin
private val liveLevel = mutableListOf<Float>()
private val liveVoltage = mutableListOf<Float>()
```

In the combine flow (line ~167), add after the existing `appendLive` calls:
```kotlin
appendLive(liveLevel, state.level.toFloat())
appendLive(liveVoltage, state.voltageMv.toFloat())
```

In the UiState construction (line ~175), add:
```kotlin
liveLevel = liveLevel.toList(),
liveVoltage = liveVoltage.toList(),
```

- [ ] **Step 3: Add LiveChart UI in BatteryDetailScreen**

In `BatteryDetailScreen.kt`, after the existing livePowerW LiveChart (around line 523), add three more LiveCharts for level, temperature, and voltage. Follow the same pattern:

```kotlin
if (state.liveLevel.size >= 2) {
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
    LiveChart(
        data = state.liveLevel,
        currentValueLabel = stringResource(R.string.value_percent, battery.level),
        label = stringResource(R.string.battery_level),
        lineColor = MaterialTheme.statusColors.healthy,
        yMin = 0f,
        yMax = 100f,
        modifier = Modifier.fillMaxWidth()
    )
}
if (state.liveTempC.size >= 2) {
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
    LiveChart(
        data = state.liveTempC,
        currentValueLabel = formatTemperature(battery.temperatureC, state.temperatureUnit),
        label = stringResource(R.string.battery_temperature),
        lineColor = statusColorForTemperature(battery.temperatureC),
        modifier = Modifier.fillMaxWidth()
    )
}
if (state.liveVoltage.size >= 2) {
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
    LiveChart(
        data = state.liveVoltage,
        currentValueLabel = stringResource(R.string.value_with_unit_int, battery.voltageMv, stringResource(R.string.unit_mv)),
        label = stringResource(R.string.battery_voltage),
        lineColor = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.fillMaxWidth()
    )
}
```

Note: `formatTemperature` and `statusColorForTemperature` must be imported — check existing imports in the file. The battery temperature string `R.string.battery_temperature` should already exist; verify and add to strings.xml if missing.

- [ ] **Step 4: Add any missing strings**

In `strings.xml`, verify these exist and add if missing:
```xml
<string name="battery_voltage">Voltage</string>
<string name="unit_mv">mV</string>
```

- [ ] **Step 5: Commit**

```
git add app/src/main/java/com/runcheck/ui/battery/
git commit -m "feat: add live charts for battery level, temperature, and voltage"
```

---

### Task 2: Network — Add live signal strength chart

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/network/NetworkUiState.kt`
- Modify: `app/src/main/java/com/runcheck/ui/network/NetworkViewModel.kt`
- Modify: `app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add UiState field**

In `NetworkUiState.kt` `Success`:
```kotlin
val liveSignalDbm: List<Float> = emptyList()
```

- [ ] **Step 2: Add ring buffer in ViewModel**

In `NetworkViewModel.kt`, add the ring buffer field:
```kotlin
private val liveSignalDbm = mutableListOf<Float>()
```

Add the constant to the **existing** `companion object` (do NOT create a second one):
```kotlin
// Inside existing companion object at ~line 340:
private const val LIVE_CHART_MAX_POINTS = 60
```

Add `appendLive` helper (same as battery pattern):
```kotlin
private fun appendLive(buffer: MutableList<Float>, value: Float) {
    buffer.add(value)
    if (buffer.size > LIVE_CHART_MAX_POINTS) buffer.removeFirst()
}
```

In the `networkJob` collect block (line ~264), after constructing `NetworkUiState.Success`, append signal data:
```kotlin
state.signalDbm?.let { appendLive(liveSignalDbm, it.toFloat()) }
```

Pass to UiState:
```kotlin
liveSignalDbm = liveSignalDbm.toList()
```

Also update `collectDismissedCards` to preserve `liveSignalDbm` when copying state.

- [ ] **Step 3: Add LiveChart UI in NetworkDetailScreen**

In `NetworkDetailScreen.kt`, after the hero section dBm/ASU text (around line 270), before the MetricPill row, add:

```kotlin
if (state.liveSignalDbm.size >= 2) {
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
    LiveChart(
        data = state.liveSignalDbm,
        currentValueLabel = networkState.signalDbm?.let {
            stringResource(R.string.value_with_unit_int, it, stringResource(R.string.unit_dbm))
        } ?: "—",
        label = stringResource(R.string.network_signal_strength),
        lineColor = statusColorForSignalQuality(networkState.signalQuality),
        modifier = Modifier.fillMaxWidth()
    )
}
```

- [ ] **Step 4: Add missing strings**

In `strings.xml`, add:
```xml
<string name="network_signal_strength">Signal Strength</string>
```

- [ ] **Step 5: Commit**

```
git add app/src/main/java/com/runcheck/ui/network/ app/src/main/res/
git commit -m "feat: add live signal strength chart on network detail screen"
```

---

### Task 3: Thermal — Add Room-based history TrendChart

**Files:**
- Modify: `app/src/main/java/com/runcheck/domain/repository/ThermalRepository.kt`
- Modify: `app/src/main/java/com/runcheck/data/thermal/ThermalRepositoryImpl.kt`
- Create: `app/src/main/java/com/runcheck/domain/usecase/GetThermalHistoryUseCase.kt`
- Modify: `app/src/main/java/com/runcheck/ui/chart/ChartRenderModel.kt`
- Modify: `app/src/main/java/com/runcheck/ui/chart/ChartModels.kt`
- Modify: `app/src/main/java/com/runcheck/ui/thermal/ThermalUiState.kt`
- Modify: `app/src/main/java/com/runcheck/ui/thermal/ThermalViewModel.kt`
- Modify: `app/src/main/java/com/runcheck/ui/thermal/ThermalDetailScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Expose getReadingsSince in ThermalRepository interface**

In `ThermalRepository.kt`, add:
```kotlin
fun getReadingsSince(since: Long, limit: Int? = null): Flow<List<ThermalReading>>
```

- [ ] **Step 2a: Add `getReadingsSinceLimited` to ThermalReadingDao**

In `ThermalReadingDao.kt`, add (following NetworkReadingDao pattern):
```kotlin
@Query("""
    SELECT * FROM thermal_readings
    WHERE id IN (
        SELECT id FROM thermal_readings
        WHERE timestamp >= :since
        ORDER BY timestamp DESC
        LIMIT :limit
    )
    ORDER BY timestamp ASC
""")
fun getReadingsSinceLimited(since: Long, limit: Int): Flow<List<ThermalReadingEntity>>
```

- [ ] **Step 2b: Implement in ThermalRepositoryImpl**

In `ThermalRepositoryImpl.kt`, add:
```kotlin
override fun getReadingsSince(since: Long, limit: Int?): Flow<List<ThermalReading>> {
    val source = if (limit != null) {
        thermalReadingDao.getReadingsSinceLimited(since, limit)
    } else {
        thermalReadingDao.getReadingsSince(since)
    }
    return source.map { entities -> entities.map { it.toDomain() } }
}
```

- [ ] **Step 3: Create GetThermalHistoryUseCase**

Create `domain/usecase/GetThermalHistoryUseCase.kt`:
```kotlin
package com.runcheck.domain.usecase

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.repository.ThermalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThermalHistoryUseCase @Inject constructor(
    private val thermalRepository: ThermalRepository
) {
    operator fun invoke(period: HistoryPeriod = HistoryPeriod.DAY): Flow<List<ThermalReading>> {
        val since = when (period) {
            HistoryPeriod.ALL, HistoryPeriod.SINCE_UNPLUG -> 0L
            else -> System.currentTimeMillis() - period.durationMs
        }
        val limit = if (period == HistoryPeriod.ALL) 5_000 else null
        return thermalRepository.getReadingsSince(since, limit)
    }
}
```

- [ ] **Step 4: Add ThermalHistoryMetric enum**

In `ui/chart/ChartModels.kt`, add:
```kotlin
enum class ThermalHistoryMetric {
    BATTERY_TEMP,
    CPU_TEMP
}
```

- [ ] **Step 5: Add buildThermalHistoryChartModel**

In `ui/chart/ChartRenderModel.kt`, add:
```kotlin
fun buildThermalHistoryChartModel(
    history: List<ThermalReading>,
    metric: ThermalHistoryMetric,
    period: HistoryPeriod,
    maxPoints: Int,
    temperatureUnit: TemperatureUnit
): ChartRenderModel {
    val chartPoints = history.mapNotNull { reading ->
        val value = when (metric) {
            ThermalHistoryMetric.BATTERY_TEMP -> reading.batteryTempC
            ThermalHistoryMetric.CPU_TEMP -> reading.cpuTempC
        }
        value?.let { reading.timestamp to it }
    }.downsamplePairs(maxPoints)
    val chartData = chartPoints.map { it.second }
    val chartTimestamps = chartPoints.map { it.first }
    val unit = if (temperatureUnit == TemperatureUnit.CELSIUS) " °C" else " °F"
    // Convert if Fahrenheit
    val displayData = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) {
        chartData.map { it * 9f / 5f + 32f }
    } else chartData

    return ChartRenderModel(
        chartData = displayData,
        chartTimestamps = chartTimestamps,
        unit = unit,
        yLabels = if (displayData.size >= 2) buildSimpleYLabels(displayData.min(), displayData.max(), 3) else emptyList(),
        xLabels = if (chartTimestamps.size >= 2) buildNetworkXLabels(chartTimestamps, period) else emptyList(),
        tooltipDecimals = 1,
        temperatureUnit = temperatureUnit
    )
}
```

Also add a reusable `buildSimpleYLabels` helper if it doesn't exist:
```kotlin
private fun buildSimpleYLabels(min: Float, max: Float, count: Int): List<ChartYLabel> {
    if (max <= min) return emptyList()
    return (0..count).map { i ->
        val value = min + (max - min) * i / count
        ChartYLabel(value = value, label = formatDecimal(value, 0))
    }
}
```

- [ ] **Step 5b: Add quality zones and metric label helpers**

In `ui/chart/ChartHelpers.kt`, add:
```kotlin
@Composable
fun thermalQualityZones(temperatureUnit: TemperatureUnit): List<ChartQualityZone> {
    fun convert(c: Float) = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) c * 9f / 5f + 32f else c
    return listOf(
        ChartQualityZone(minValue = 0f, maxValue = convert(35f), color = MaterialTheme.statusColors.healthy.copy(alpha = 0.08f)),
        ChartQualityZone(minValue = convert(35f), maxValue = convert(42f), color = MaterialTheme.statusColors.fair.copy(alpha = 0.08f)),
        ChartQualityZone(minValue = convert(42f), maxValue = convert(60f), color = MaterialTheme.statusColors.critical.copy(alpha = 0.08f))
    )
}

@Composable
fun thermalHistoryMetricLabel(metric: ThermalHistoryMetric): String = when (metric) {
    ThermalHistoryMetric.BATTERY_TEMP -> stringResource(R.string.thermal_metric_battery_temp)
    ThermalHistoryMetric.CPU_TEMP -> stringResource(R.string.thermal_metric_cpu_temp)
}
```

- [ ] **Step 6: Add history fields to ThermalUiState**

In `ThermalUiState.kt` `Success`, add:
```kotlin
val thermalHistory: List<ThermalReading> = emptyList(),
val selectedHistoryPeriod: HistoryPeriod = HistoryPeriod.DAY,
```

- [ ] **Step 7: Update ThermalViewModel to collect history**

In `ThermalViewModel.kt`:
1. Inject `GetThermalHistoryUseCase`
2. Add `selectedHistoryPeriod` from `SavedStateHandle` (same pattern as NetworkViewModel)
3. Add a separate `historyJob` coroutine that collects `getThermalHistory(period)` and updates UiState
4. Add `setHistoryPeriod(period)` function that cancels and re-launches the history job

- [ ] **Step 8: Add TrendChart + chips UI in ThermalDetailScreen**

In `ThermalDetailScreen.kt`, add a new history card section after the existing metrics card. Follow the exact pattern from `NetworkDetailScreen.kt` `SignalHistoryCard`:
- Gate behind `if (state.isPro)` (same as network signal history)
- SectionHeader "HISTORY"
- Card with FilterChips for metric (Battery Temp / CPU Temp) using `thermalHistoryMetricLabel()` and period (24H / Week / Month / All) using `historyPeriodLabel()`
- TrendChart with the built model inside `ExpandableChartContainer`
- Quality zones via `thermalQualityZones(state.temperatureUnit)`
- Min / Avg / Max MetricPills below chart
- If not Pro, show `ProFeatureCalloutCard` for history instead

- [ ] **Step 9: Add strings**

```xml
<string name="thermal_history">History</string>
<string name="thermal_metric_battery_temp">Battery Temp</string>
<string name="thermal_metric_cpu_temp">CPU Temp</string>
```

- [ ] **Step 10: Commit**

```
git add app/src/main/java/com/runcheck/domain/ app/src/main/java/com/runcheck/data/thermal/ app/src/main/java/com/runcheck/ui/thermal/ app/src/main/java/com/runcheck/ui/chart/ app/src/main/res/
git commit -m "feat: add temperature history TrendChart to thermal detail screen"
```

---

### Task 4: Storage — Add usage history TrendChart

**Files:**
- Modify: `app/src/main/java/com/runcheck/domain/repository/StorageRepository.kt`
- Modify: `app/src/main/java/com/runcheck/data/storage/StorageRepositoryImpl.kt`
- Create: `app/src/main/java/com/runcheck/domain/usecase/GetStorageHistoryUseCase.kt`
- Modify: `app/src/main/java/com/runcheck/ui/chart/ChartRenderModel.kt`
- Modify: `app/src/main/java/com/runcheck/ui/chart/ChartModels.kt`
- Modify: `app/src/main/java/com/runcheck/ui/storage/StorageUiState.kt`
- Modify: `app/src/main/java/com/runcheck/ui/storage/StorageViewModel.kt`
- Modify: `app/src/main/java/com/runcheck/ui/storage/StorageDetailScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Expose getReadingsSince in StorageRepository interface**

In `StorageRepository.kt`, add:
```kotlin
fun getReadingsSince(since: Long, limit: Int? = null): Flow<List<StorageReading>>
```

- [ ] **Step 2a: Add `getReadingsSinceLimited` to StorageReadingDao**

In `StorageReadingDao.kt`, add (same pattern as thermal):
```kotlin
@Query("""
    SELECT * FROM storage_readings
    WHERE id IN (
        SELECT id FROM storage_readings
        WHERE timestamp >= :since
        ORDER BY timestamp DESC
        LIMIT :limit
    )
    ORDER BY timestamp ASC
""")
fun getReadingsSinceLimited(since: Long, limit: Int): Flow<List<StorageReadingEntity>>
```

- [ ] **Step 2b: Implement in StorageRepositoryImpl**

Same limit-switching pattern as thermal:
```kotlin
override fun getReadingsSince(since: Long, limit: Int?): Flow<List<StorageReading>> {
    val source = if (limit != null) {
        storageReadingDao.getReadingsSinceLimited(since, limit)
    } else {
        storageReadingDao.getReadingsSince(since)
    }
    return source.map { entities -> entities.map { it.toDomain() } }
}
```

- [ ] **Step 3: Create GetStorageHistoryUseCase**

Same pattern as thermal — `domain/usecase/GetStorageHistoryUseCase.kt`:
```kotlin
class GetStorageHistoryUseCase @Inject constructor(
    private val storageRepository: StorageRepository
) {
    operator fun invoke(period: HistoryPeriod = HistoryPeriod.DAY): Flow<List<StorageReading>> {
        val since = when (period) {
            HistoryPeriod.ALL, HistoryPeriod.SINCE_UNPLUG -> 0L
            else -> System.currentTimeMillis() - period.durationMs
        }
        val limit = if (period == HistoryPeriod.ALL) 5_000 else null
        return storageRepository.getReadingsSince(since, limit)
    }
}
```

- [ ] **Step 4: Add StorageHistoryMetric enum**

In `ui/chart/ChartModels.kt`:
```kotlin
enum class StorageHistoryMetric {
    USED_SPACE,
    AVAILABLE_SPACE
}
```

- [ ] **Step 5: Add buildStorageHistoryChartModel**

In `ui/chart/ChartRenderModel.kt`:
```kotlin
fun buildStorageHistoryChartModel(
    history: List<StorageReading>,
    metric: StorageHistoryMetric,
    period: HistoryPeriod,
    maxPoints: Int
): ChartRenderModel {
    val chartPoints = history.map { reading ->
        val value = when (metric) {
            StorageHistoryMetric.USED_SPACE ->
                (reading.totalBytes - reading.availableBytes).toFloat() / (1024f * 1024f * 1024f)
            StorageHistoryMetric.AVAILABLE_SPACE ->
                reading.availableBytes.toFloat() / (1024f * 1024f * 1024f)
        }
        reading.timestamp to value
    }.downsamplePairs(maxPoints)
    val chartData = chartPoints.map { it.second }
    val chartTimestamps = chartPoints.map { it.first }

    return ChartRenderModel(
        chartData = chartData,
        chartTimestamps = chartTimestamps,
        unit = " GB",
        yLabels = if (chartData.size >= 2) buildSimpleYLabels(chartData.min(), chartData.max(), 3) else emptyList(),
        xLabels = if (chartTimestamps.size >= 2) buildNetworkXLabels(chartTimestamps, period) else emptyList(),
        tooltipDecimals = 1
    )
}
```

- [ ] **Step 6: Add history fields to StorageUiState**

In `StorageUiState.kt` `Success`, add:
```kotlin
val storageHistory: List<StorageReading> = emptyList(),
val selectedHistoryPeriod: HistoryPeriod = HistoryPeriod.WEEK,
```

Default to WEEK since storage changes slowly — daily view is often flat.

- [ ] **Step 7: Update StorageViewModel to collect history**

In `StorageViewModel.kt`:
1. Add `savedStateHandle: SavedStateHandle` as a **new constructor parameter** (StorageViewModel does NOT currently have it)
2. Inject `GetStorageHistoryUseCase`
3. Add `selectedHistoryPeriod` from `SavedStateHandle`
4. Add a separate `historyJob` that collects history data
5. Add `setHistoryPeriod(period)` function
6. Wire into existing combine or parallel job

- [ ] **Step 8: Add TrendChart + chips UI in StorageDetailScreen**

In `StorageDetailScreen.kt`, add a history section after the hero card and before cleanup tools. Pattern:
- Gate behind `if (state.isPro)` (same as thermal/network)
- SectionHeader "HISTORY"
- Card with FilterChips for metric (Used Space / Available Space) using `storageHistoryMetricLabel()` and period (24H / Week / Month / All)
- TrendChart with the built model inside `ExpandableChartContainer`
- Min / Avg / Max MetricPills (in GB)
- If not Pro, show `ProFeatureCalloutCard` for history

Also add `storageHistoryMetricLabel()` composable to `ChartHelpers.kt`:
```kotlin
@Composable
fun storageHistoryMetricLabel(metric: StorageHistoryMetric): String = when (metric) {
    StorageHistoryMetric.USED_SPACE -> stringResource(R.string.storage_metric_used)
    StorageHistoryMetric.AVAILABLE_SPACE -> stringResource(R.string.storage_metric_available)
}
```

- [ ] **Step 9: Add strings**

```xml
<string name="storage_history">History</string>
<string name="storage_metric_used">Used Space</string>
<string name="storage_metric_available">Available Space</string>
```

- [ ] **Step 10: Commit**

```
git add app/src/main/java/com/runcheck/domain/ app/src/main/java/com/runcheck/data/storage/ app/src/main/java/com/runcheck/ui/storage/ app/src/main/java/com/runcheck/ui/chart/ app/src/main/res/
git commit -m "feat: add storage usage history TrendChart to storage detail screen"
```

---

## Implementation Notes

- **Data sampling rate:** WorkManager collects readings every 30min by default. TrendCharts will show this cadence — no need for denser sampling since LiveCharts handle real-time.
- **LiveChart vs TrendChart roles:** LiveChart = in-memory ring buffer, session-scoped, real-time animation. TrendChart = Room-persisted data, survives app restarts, supports period selection and tooltips.
- **Quality zones:** Battery temp and thermal history should use quality zones (green/amber/red). Storage and network don't need them.
- **Imports needed in detail screens:** `LiveChart` from `ui.components`, `TrendChart` from `ui.components`, `ChartRenderModel` builders from `ui.chart`, `HistoryPeriod` from `domain.model`, `formatTemperature`/`statusColorForTemperature` from `ui.common`/`ui.theme`.
- **ExpandableChartContainer:** Use this wrapper for TrendCharts (already used in network) — provides fullscreen expand support.
- **Pro gating:** History TrendCharts should respect existing Pro gating pattern if used elsewhere. Check NetworkDetailScreen `SignalHistoryCard` for Pro gating logic and replicate for thermal/storage.
