# Battery & Thermal Enhancements Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add new metrics and UI enhancements to battery and thermal detail screens based on battery-enhancements-spec.md

**Architecture:** Each feature is a self-contained change touching data layer (BatteryState model, GenericBatterySource), ViewModel (session stats tracking), and UI (BatteryDetailScreen / ThermalDetailScreen composables). String resources added to both EN and FI.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Flow, BatteryManager API, Room

---

## Chunk 1: Quick UI Wins (Features #2, #4, #3)

### Task 1: W + mV line under current display (#2)

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-fi/strings.xml`
- Modify: `app/src/main/java/com/runcheck/ui/battery/BatteryDetailScreen.kt:244-323` (current section)

- [ ] **Step 1: Add string resources**

In `values/strings.xml`, after `battery_section_details` (line ~112):
```xml
<string name="battery_power_voltage">%1$s W · %2$d mV</string>
```

In `values-fi/strings.xml`, same location:
```xml
<string name="battery_power_voltage">%1$s W · %2$d mV</string>
```
(Same format — units don't need translation)

- [ ] **Step 2: Add W + mV text under the mA display**

In `BatteryDetailScreen.kt`, inside the current section (after the Row containing the mA number and before `ConfidenceBadge`), add a power + voltage line. The `powerW` value is already computed in `BatteryHeroSection` — we need to compute it here too.

After the closing `}` of the mA Row (line ~290) and before the `ConfidenceBadge` (line ~292), restructure the Column to include:

```kotlin
// After the mA number Row, add:
if (battery.currentMa.confidence != Confidence.UNAVAILABLE) {
    val powerW = remember(battery.currentMa.value, battery.voltageMv) {
        val currentA = battery.currentMa.value / 1000f
        val voltageV = battery.voltageMv / 1000f
        kotlin.math.abs(currentA * voltageV)
    }
    Text(
        text = stringResource(
            R.string.battery_power_voltage,
            formatDecimal(powerW, 1),
            battery.voltageMv
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
```

Place this inside the `Column` that holds the "Current" label and the mA number, right after the mA Row closes.

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values/strings.xml app/src/main/res/values-fi/strings.xml app/src/main/java/com/runcheck/ui/battery/BatteryDetailScreen.kt
git commit -m "Lisää W + mV -rivi virta-arvon alle akkutietonäkymässä"
```

---

### Task 2: Temperature min/max in thermal hero card (#4)

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/thermal/ThermalViewModel.kt`
- Modify: `app/src/main/java/com/runcheck/ui/thermal/ThermalUiState.kt`
- Modify: `app/src/main/java/com/runcheck/ui/thermal/ThermalDetailScreen.kt:214-286` (ThermalHeroCard)
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-fi/strings.xml`

- [ ] **Step 1: Add string resources**

In `values/strings.xml` after thermal strings (~line 249):
```xml
<string name="thermal_session_range">↓ %1$s · ↑ %2$s</string>
```

In `values-fi/strings.xml` same location:
```xml
<string name="thermal_session_range">↓ %1$s · ↑ %2$s</string>
```

- [ ] **Step 2: Add session min/max fields to ThermalUiState**

In `ThermalUiState.kt`, add to the `Success` data class:
```kotlin
val sessionMinTemp: Float? = null,
val sessionMaxTemp: Float? = null
```

- [ ] **Step 3: Track min/max in ThermalViewModel**

In `ThermalViewModel.kt`, add private tracking variables and update the combine block:

```kotlin
private var sessionMinTemp: Float? = null
private var sessionMaxTemp: Float? = null
```

In `loadThermalData()`, inside the combine lambda, update tracking before creating the state:

```kotlin
) { thermalState: ThermalState, events: List<ThrottlingEvent>, isPro: Boolean ->
    val currentTemp = thermalState.batteryTempC
    sessionMinTemp = sessionMinTemp?.coerceAtMost(currentTemp) ?: currentTemp
    sessionMaxTemp = sessionMaxTemp?.coerceAtLeast(currentTemp) ?: currentTemp

    ThermalUiState.Success(
        thermalState = thermalState,
        throttlingEvents = events,
        isPro = isPro,
        sessionMinTemp = sessionMinTemp,
        sessionMaxTemp = sessionMaxTemp
    )
}
```

- [ ] **Step 4: Display min/max in ThermalHeroCard**

In `ThermalDetailScreen.kt`, update `ThermalHeroCard` to accept and display the range. Change the function signature to take the full state (or add min/max params).

After the `bandLabel` Text (line ~279) and before the closing of the inner Column, add:

```kotlin
// In ThermalContent, pass state to ThermalHeroCard
// ThermalHeroCard signature: add sessionMinTemp/sessionMaxTemp params

// Inside ThermalHeroCard, after the bandLabel Text:
if (sessionMinTemp != null && sessionMaxTemp != null &&
    sessionMinTemp != sessionMaxTemp) {
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = statusColorForTemperature(sessionMinTemp))) {
                append("↓ ${formatTemperature(sessionMinTemp)}")
            }
            append(" · ")
            withStyle(SpanStyle(color = statusColorForTemperature(sessionMaxTemp))) {
                append("↑ ${formatTemperature(sessionMaxTemp)}")
            }
        },
        style = MaterialTheme.typography.bodySmall
    )
}
```

Note: Requires importing `buildAnnotatedString`, `withStyle`, `SpanStyle` from `androidx.compose.ui.text`.

- [ ] **Step 5: Wire up the data flow**

In `ThermalContent`, pass the session temps from state to `ThermalHeroCard`:
```kotlin
ThermalHeroCard(
    thermal = thermal,
    sessionMinTemp = state.sessionMinTemp,
    sessionMaxTemp = state.sessionMaxTemp
)
```

- [ ] **Step 6: Commit**

```
git add app/src/main/java/com/runcheck/ui/thermal/ app/src/main/res/values/strings.xml app/src/main/res/values-fi/strings.xml
git commit -m "Lisää session-aikainen lämpötilan min/max thermal-hero-korttiin"
```

---

### Task 3: Current avg/min/max stats (#3)

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/battery/BatteryViewModel.kt`
- Modify: `app/src/main/java/com/runcheck/ui/battery/BatteryUiState.kt`
- Modify: `app/src/main/java/com/runcheck/ui/battery/BatteryDetailScreen.kt:308-323` (after Status/Type pills)
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-fi/strings.xml`

- [ ] **Step 1: Add string resources**

In `values/strings.xml`:
```xml
<string name="battery_current_average">Average</string>
<string name="battery_current_minimum">Minimum</string>
<string name="battery_current_maximum">Maximum</string>
```

In `values-fi/strings.xml`:
```xml
<string name="battery_current_average">Keskiarvo</string>
<string name="battery_current_minimum">Minimi</string>
<string name="battery_current_maximum">Maksimi</string>
```

- [ ] **Step 2: Add CurrentStats data class and UiState field**

In `BatteryUiState.kt`, add the data class and the new field:

```kotlin
data class CurrentStats(
    val avg: Int,
    val min: Int,
    val max: Int,
    val sampleCount: Int
)
```

Add to `Success`:
```kotlin
val currentStats: CurrentStats? = null
```

- [ ] **Step 3: Track current stats in BatteryViewModel**

Add tracking variables to `BatteryViewModel`:

```kotlin
private var currentSum: Long = 0L
private var currentCount: Int = 0
private var currentMin: Int = Int.MAX_VALUE
private var currentMax: Int = Int.MIN_VALUE
private var lastChargingStatus: ChargingStatus? = null
```

In the `combine` lambda inside `loadBatteryData()`, before emitting state:

```kotlin
) { state, history, isPro ->
    val currentMa = state.currentMa.value
    val confidence = state.currentMa.confidence

    // Reset stats if charging status changed
    if (lastChargingStatus != null && lastChargingStatus != state.chargingStatus) {
        currentSum = 0L
        currentCount = 0
        currentMin = Int.MAX_VALUE
        currentMax = Int.MIN_VALUE
    }
    lastChargingStatus = state.chargingStatus

    // Accumulate stats if current is available
    val stats = if (confidence != Confidence.UNAVAILABLE) {
        currentSum += currentMa
        currentCount++
        currentMin = minOf(currentMin, currentMa)
        currentMax = maxOf(currentMax, currentMa)
        if (currentCount >= 2) {
            CurrentStats(
                avg = (currentSum / currentCount).toInt(),
                min = currentMin,
                max = currentMax,
                sampleCount = currentCount
            )
        } else null
    } else null

    BatteryUiState.Success(
        batteryState = state,
        history = history,
        selectedPeriod = selectedPeriod,
        isPro = isPro,
        currentStats = stats
    )
}
```

Note: Import `CurrentStats` and `Confidence` in ViewModel.

- [ ] **Step 4: Display avg/min/max pills in BatteryDetailScreen**

In `BatteryDetailScreen.kt`, after the Status/Type MetricPill Row (line ~322), add:

```kotlin
// After the Status/Type pills Row and before the closing of BatteryPanel
state.currentStats?.let { stats ->
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        MetricPill(
            label = stringResource(R.string.battery_current_average),
            value = stringResource(R.string.value_milliamps_int, stats.avg),
            modifier = Modifier.weight(1f)
        )
        MetricPill(
            label = stringResource(R.string.battery_current_minimum),
            value = stringResource(R.string.value_milliamps_int, stats.min),
            modifier = Modifier.weight(1f)
        )
        MetricPill(
            label = stringResource(R.string.battery_current_maximum),
            value = stringResource(R.string.value_milliamps_int, stats.max),
            modifier = Modifier.weight(1f)
        )
    }
}
```

Also add the formatting string to both strings.xml files:
```xml
<string name="value_milliamps_int">%1$d mA</string>
```

Note: The BatteryContent composable needs to pass `state` to the current section, or extract `currentStats` from state. Since BatteryContent already has access to `state`, pass it or extract `state.currentStats` into the composable scope.

- [ ] **Step 5: Commit**

```
git add app/src/main/java/com/runcheck/ui/battery/ app/src/main/res/values/strings.xml app/src/main/res/values-fi/strings.xml
git commit -m "Lisää virran avg/min/max -tilastot akkutietonäkymään"
```

---

## Chunk 2: Data Layer Enhancements (Features #1, #7)

### Task 4: mAh remaining in hero card (#1)

**Files:**
- Modify: `app/src/main/java/com/runcheck/domain/model/BatteryState.kt`
- Modify: `app/src/main/java/com/runcheck/data/battery/BatteryDataSource.kt`
- Modify: `app/src/main/java/com/runcheck/data/battery/GenericBatterySource.kt`
- Modify: `app/src/main/java/com/runcheck/ui/battery/BatteryDetailScreen.kt:382-504` (BatteryHeroSection)
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-fi/strings.xml`
- Need to check: how BatteryState is assembled (find the repository/use case that combines flows)

- [ ] **Step 1: Find where BatteryState is assembled**

Search for the code that creates `BatteryState(...)` to understand where the new field needs to be populated. This is likely in a repository or use case that combines all the `BatteryDataSource` flows.

- [ ] **Step 2: Add `getChargeCounter()` to BatteryDataSource interface**

```kotlin
fun getChargeCounter(): Flow<Int?> // µAh from BATTERY_PROPERTY_CHARGE_COUNTER
```

- [ ] **Step 3: Implement in GenericBatterySource**

```kotlin
override fun getChargeCounter(): Flow<Int?> = flow {
    while (true) {
        val raw = try {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                .takeUnless { it == Int.MIN_VALUE || it == 0 }
        } catch (_: Exception) {
            null
        }
        emit(raw?.let { it / 1000 }) // Convert µAh to mAh
        delay(POLLING_INTERVAL_MS)
    }
}.flowOn(Dispatchers.IO)
```

- [ ] **Step 4: Add `remainingMah` to BatteryState**

```kotlin
data class BatteryState(
    // ... existing fields ...
    val remainingMah: Int? = null
)
```

- [ ] **Step 5: Wire chargeCounter into BatteryState assembly**

In the repository/use case that assembles BatteryState, add the chargeCounter flow to the combine and pass it as `remainingMah`.

- [ ] **Step 6: Add string resources**

In `values/strings.xml`:
```xml
<string name="battery_remaining_mah">%1$s · ~%2$d mAh remaining</string>
```

In `values-fi/strings.xml`:
```xml
<string name="battery_remaining_mah">%1$s · ~%2$d mAh jäljellä</string>
```

- [ ] **Step 7: Display mAh in BatteryHeroSection**

In `BatteryHeroSection`, after the `statusText` Text (line ~468), conditionally show the mAh-enhanced version:

```kotlin
val displayText = if (battery.remainingMah != null) {
    stringResource(R.string.battery_remaining_mah, statusText, battery.remainingMah)
} else {
    statusText
}

Text(
    text = displayText,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
```

- [ ] **Step 8: Commit**

```
git commit -m "Lisää mAh jäljellä -arvio akkutason rinnalle hero-kortissa"
```

---

### Task 5: Battery capacity mAh display (#7)

**Files:**
- Modify: `app/src/main/java/com/runcheck/domain/model/BatteryState.kt`
- Create: `app/src/main/java/com/runcheck/data/battery/BatteryCapacityReader.kt`
- Modify: `app/src/main/java/com/runcheck/ui/battery/BatteryDetailScreen.kt` (Health MetricRow area)
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-fi/strings.xml`

- [ ] **Step 1: Create BatteryCapacityReader**

```kotlin
package com.runcheck.data.battery

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryCapacityReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Returns the device's design battery capacity in mAh using Android's
     * internal PowerProfile API via reflection. Returns null if unavailable.
     */
    fun getDesignCapacityMah(): Int? = try {
        val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
        val constructor = powerProfileClass.getConstructor(Context::class.java)
        val instance = constructor.newInstance(context)
        val capacity = powerProfileClass
            .getMethod("getBatteryCapacity")
            .invoke(instance) as Double
        if (capacity > 0) capacity.toInt() else null
    } catch (_: Exception) {
        null
    }
}
```

- [ ] **Step 2: Add fields to BatteryState**

```kotlin
val designCapacityMah: Int? = null,
val estimatedCapacityMah: Int? = null
```

- [ ] **Step 3: Wire capacity into BatteryState assembly**

Inject `BatteryCapacityReader` into the repository/use case. Read design capacity once at startup. Calculate estimated capacity as `designCapacity * healthPercent / 100` when healthPercent is available.

- [ ] **Step 4: Add string resources**

In `values/strings.xml`:
```xml
<string name="battery_capacity_mah">%1$d / %2$d mAh</string>
```

In `values-fi/strings.xml`:
```xml
<string name="battery_capacity_mah">%1$d / %2$d mAh</string>
```

- [ ] **Step 5: Display capacity in Details section**

In `BatteryDetailScreen.kt`, after the Health MetricRow (line ~220), when both values are available:

```kotlin
if (battery.estimatedCapacityMah != null && battery.designCapacityMah != null) {
    Text(
        text = stringResource(
            R.string.battery_capacity_mah,
            battery.estimatedCapacityMah,
            battery.designCapacityMah
        ),
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = MaterialTheme.numericFontFamily
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 0.dp) // aligned under the health value
    )
}
```

- [ ] **Step 6: Commit**

```
git commit -m "Lisää akun kapasiteetti mAh:na kuntotiedon yhteyteen"
```

---

## Chunk 3: History Enhancement (Feature #6)

### Task 6: "Since unplug" history period (#6)

**Files:**
- Modify: `app/src/main/java/com/runcheck/domain/model/HistoryPeriod.kt`
- Modify: `app/src/main/java/com/runcheck/ui/battery/BatteryDetailScreen.kt:519-594` (BatteryHistoryPanel)
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-fi/strings.xml`
- Need to check: `GetBatteryHistoryUseCase` to understand how history is fetched and filtered

- [ ] **Step 1: Find GetBatteryHistoryUseCase**

Read the use case to understand how it takes a HistoryPeriod and queries the database.

- [ ] **Step 2: Add SINCE_UNPLUG to HistoryPeriod enum**

```kotlin
enum class HistoryPeriod(val durationMs: Long) {
    SINCE_UNPLUG(-1L), // Special sentinel — resolved by use case
    DAY(24 * 60 * 60 * 1000L),
    WEEK(7 * 24 * 60 * 60 * 1000L),
    MONTH(30L * 24 * 60 * 60 * 1000L),
    ALL(0L)
}
```

- [ ] **Step 3: Add string resources**

In `values/strings.xml`:
```xml
<string name="history_period_since_unplug">Since unplug</string>
```

In `values-fi/strings.xml`:
```xml
<string name="history_period_since_unplug">Irrotuksesta</string>
```

- [ ] **Step 4: Handle SINCE_UNPLUG in GetBatteryHistoryUseCase**

When period is `SINCE_UNPLUG`:
1. Query the database for the most recent reading where status = "CHARGING"
2. Use that timestamp as the start point
3. If no charging reading found, fall back to returning all data

- [ ] **Step 5: Add FilterChip to BatteryHistoryPanel**

In the `HistoryPeriod.entries.forEach` loop, the new entry will automatically appear. Update the `historyPeriodLabel` function:

```kotlin
HistoryPeriod.SINCE_UNPLUG -> stringResource(R.string.history_period_since_unplug)
```

- [ ] **Step 6: Commit**

```
git commit -m "Lisää 'Irrotuksesta' -aikavälisuodatin akkuhistoriaan"
```

---

## Implementation Notes

- **No Gradle builds**: User builds manually from their terminal. After code changes, provide the build command.
- **No CLAUDE.md in res/**: Never create any non-XML files in `app/src/main/res/`.
- **String resources**: Always add both EN (`values/strings.xml`) and FI (`values-fi/strings.xml`).
- **Formatting helpers**: `formatDecimal()` from `ui/common/UiFormatters.kt`, `formatTemperature()` from same file.
- **Status colors**: Use `statusColorForTemperature()` from `ui/theme/` for temperature-based coloring.
- **MetricPill**: Simple label+value Column component at `ui/components/MetricPill.kt`.
- **BatteryPanel**: Local private composable in BatteryDetailScreen — Card with surfaceContainer + 16dp corners.
- **Design tokens**: Use `MaterialTheme.spacing.*` for all spacing, `MaterialTheme.numericFontFamily` for numbers.
