# runcheck Coverage Debt Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Raise runcheck's meaningful SonarCloud overall coverage without weakening the new-code quality gate or adding brittle coverage-only tests.

**Architecture:** Keep production behavior stable. Prefer focused JVM unit tests for pure logic, ViewModel state transitions, repository mapping, and insight rules; use Robolectric, instrumented tests, or coverage exclusions only for Android framework boundaries that cannot be exercised safely from local JVM tests. Preserve the current `new_coverage >= 80%` gate as the hard daily guardrail.

**Tech Stack:** Kotlin, JUnit4, MockK, kotlinx-coroutines-test, AndroidX Lifecycle ViewModel tests, JaCoCo XML, SonarCloud.

---

## Current Evidence

SonarCloud project: `Insaner1980_runcheck`

Current SonarCloud overall metrics checked on 2026-06-22:

| Metric | Value |
|---|---:|
| Overall coverage | 52.8% |
| Line coverage | 54.6% |
| Branch coverage | 47.6% |
| Lines to cover | 7,263 |
| Uncovered lines | 3,295 |
| Conditions to cover | 2,569 |
| Uncovered conditions | 1,347 |

Local JaCoCo after the existing pending `ProManagerTest` change:

| Metric | Value |
|---|---:|
| Covered lines | 3,987 |
| Missed lines | 3,151 |
| Local line coverage | 55.86% |
| Covered branches | 1,224 |
| Missed branches | 1,351 |
| Local branch coverage | 47.53% |

Do not optimize for the overall number alone. Sonar's `coverage` metric combines line and condition coverage, so low branch coverage must be improved with real branch/assertion coverage, not only extra line hits.

## Highest-Value Targets

| Rank | File or area | Current signal | Recommended action |
|---:|---|---|---|
| 1 | `app/src/main/java/com/runcheck/ui/chart/ChartHelpers.kt` | Many uncovered pure helper branches | Add focused tests to `ChartRenderModelTest.kt` |
| 2 | `app/src/main/java/com/runcheck/data/battery/GenericBatterySource.kt` | Android boundary plus pure mapping helpers | Extend `GenericBatterySourceTest.kt`; avoid broadcast-flow tests |
| 3 | `app/src/main/java/com/runcheck/domain/insights/rules/BatteryDegradationTrendRule.kt` | 0% but pure domain rule | Create a rule test using existing insight fakes |
| 4 | `app/src/main/java/com/runcheck/ui/thermal/ThermalViewModel.kt` | 0% but ViewModel pattern already exists | Create `ThermalViewModelTest.kt` |
| 5 | `app/src/main/java/com/runcheck/ui/charger/ChargerViewModel.kt` | 0% but dependencies are mockable | Create `ChargerViewModelTest.kt` |
| 6 | `app/src/main/java/com/runcheck/data/insights/InsightRepositoryImpl.kt` | 0% repository mapping/merge logic | Add DAO fake or MockK-backed repository tests |
| 7 | `app/src/main/java/com/runcheck/data/battery/BatteryRepositoryImpl.kt` | 0% repository mapping/error paths | Test DAO delegation and `Confidence.UNAVAILABLE` current filtering |
| 8 | `app/src/main/java/com/runcheck/ui/storage/cleanup/CleanupViewModel.kt` | Partial coverage with many branches | Add error/pro/selection edge-case tests |
| 9 | `app/src/main/java/com/runcheck/ui/settings/SettingsViewModel.kt` | Partial coverage, many branches | Add narrow purchase/export/debug error tests |
| 10 | `app/src/main/java/com/runcheck/data/network/SpeedTestService.kt` and `app/src/main/java/com/runcheck/data/preferences/UserPreferencesRepositoryImpl.kt` | 0% but framework-heavy | Do not rush unit tests; decide Robolectric/instrumented/exclusion path |

## Guardrails

- Do not run `lc`, `sc`, full Dependency-Check, full Sonar, or broad Gradle verification unless explicitly requested.
- Do not change app behavior to raise coverage.
- Do not test Compose `@Composable` label functions only for coverage; prefer pure helper extraction only when a real behavior needs protection.
- Do not add broad `sonar.coverage.exclusions` for business logic.
- Keep each task independently testable with one narrow `:app:testDebugUnitTest --tests ...` command.
- After each task, run `git diff --check -- <changed files>`.
- Commit messages, when committing later, should be Finnish.

---

### Task 1: Chart Helper Branch Coverage

**Files:**
- Modify: `app/src/test/java/com/runcheck/ui/chart/ChartRenderModelTest.kt`
- No production code changes expected.

- [x] **Step 1: Add imports for missing assertions and chart colors**

Add these imports to `ChartRenderModelTest.kt`:

```kotlin
import androidx.compose.ui.graphics.Color
import com.runcheck.ui.components.ChartQualityZone
import org.junit.Assert.assertNull
```

- [x] **Step 2: Add null-path tests for charging session summaries**

Add this test to `ChartRenderModelTest`:

```kotlin
@Test
fun `charging session summary is absent when not charging or no charging samples exist`() {
    val dischargingHistory =
        listOf(
            batteryReading(timestamp = 0L, level = 70, status = "DISCHARGING"),
            batteryReading(timestamp = 10 * 60_000L, level = 69, status = "DISCHARGING"),
        )

    assertNull(
        calculateChargingSessionSummary(
            history = dischargingHistory,
            currentLevel = 69,
            chargingStatus = ChargingStatus.DISCHARGING,
        ),
    )
    assertNull(
        calculateChargingSessionSummary(
            history = dischargingHistory,
            currentLevel = 69,
            chargingStatus = ChargingStatus.CHARGING,
        ),
    )
}
```

- [x] **Step 3: Add low-signal charging summary branch test**

Add this test to `ChartRenderModelTest`:

```kotlin
@Test
fun `charging summary omits pace estimates for short flat sessions`() {
    val history =
        listOf(
            batteryReading(timestamp = 0L, level = 50),
            batteryReading(timestamp = 4 * 60_000L, level = 50),
        )

    val summary =
        calculateChargingSessionSummary(
            history = history,
            currentLevel = 50,
            chargingStatus = ChargingStatus.CHARGING,
        )

    assertNotNull(summary)
    assertEquals(0, summary?.gainPercent)
    assertNull(summary?.averageSpeedPctPerHour)
    assertNull(summary?.recentSpeedPctPerHour)
    assertNull(summary?.remainingTo80Ms)
    assertNull(summary?.remainingTo100Ms)
}
```

- [x] **Step 4: Add axis-label branch tests**

Add this test to `ChartRenderModelTest`:

```kotlin
@Test
fun `axis label builders reject tiny ranges and round visible steps`() {
    assertTrue(buildBatteryYLabels(4f, 4.5f).isEmpty())
    assertTrue(buildNetworkYLabels(-55f, -54.5f).isEmpty())

    assertEquals(
        listOf("0", "20", "40", "60", "80"),
        buildBatteryYLabels(0f, 83f).map { it.label },
    )
    assertEquals(
        listOf("-90", "-80", "-70", "-60", "-50"),
        buildNetworkYLabels(-95f, -50f).map { it.label },
    )
}
```

- [x] **Step 5: Add quality-zone color branch test**

Add this test to `ChartRenderModelTest`:

```kotlin
@Test
fun `quality zone color returns matched color at full alpha and default outside zones`() {
    val defaultColor = Color.White
    val zoneColor = Color.Red.copy(alpha = 0.08f)
    val zones = listOf(ChartQualityZone(minValue = 40f, maxValue = 45f, color = zoneColor))

    val matched = qualityZoneColorForValue(42f, zones, defaultColor)
    val outside = qualityZoneColorForValue(30f, zones, defaultColor)

    assertEquals(Color.Red.red, matched.red, 0.0f)
    assertEquals(Color.Red.green, matched.green, 0.0f)
    assertEquals(Color.Red.blue, matched.blue, 0.0f)
    assertEquals(1f, matched.alpha, 0.0f)
    assertEquals(defaultColor, outside)
}
```

- [x] **Step 6: Run focused verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.runcheck.ui.chart.ChartRenderModelTest --console=plain
git diff --check -- app\src\test\java\com\runcheck\ui\chart\ChartRenderModelTest.kt
```

Expected:
- Gradle task succeeds.
- `git diff --check` reports no whitespace errors.

- [ ] **Step 7: Commit**

```powershell
git add app\src\test\java\com\runcheck\ui\chart\ChartRenderModelTest.kt
git commit -m "Lisää chart-apureiden haarakattavuutta"
```

---

### Task 2: Generic Battery Source Mapping Coverage

**Files:**
- Modify: `app/src/test/java/com/runcheck/data/battery/GenericBatterySourceTest.kt`
- No production code changes expected.

- [x] **Step 1: Extend the test helper to control charge state**

Change `createTestSource` so it accepts `isCharging` and wires the mocked `BatteryManager` explicitly:

```kotlin
private fun createTestSource(
    unit: CurrentUnit,
    convention: SignConvention,
    reliable: Boolean = true,
    isCharging: Boolean = false,
): TestableGenericBatterySource {
    val profile =
        DeviceProfile(
            manufacturer = "google",
            model = "Pixel 8",
            apiLevel = 34,
            currentNowReliable = reliable,
            currentNowUnit = unit,
            currentNowSignConvention = convention,
            cycleCountAvailable = true,
            thermalZonesAvailable = emptyList(),
            storageHealthAvailable = true,
        )
    val batteryManager =
        mockk<BatteryManager>(relaxed = true) {
            every { this@mockk.isCharging } returns isCharging
        }
    val mockContext: Context =
        mockk {
            every { getSystemService(Context.BATTERY_SERVICE) } returns batteryManager
        }
    return TestableGenericBatterySource(mockContext, profile, AppDispatchers())
}
```

- [x] **Step 2: Expose existing protected helpers through the test subclass**

Extend `TestableGenericBatterySource`:

```kotlin
private class TestableGenericBatterySource(
    context: Context,
    profile: DeviceProfile,
    dispatchers: AppDispatchers,
) : GenericBatterySource(context, profile, dispatchers) {
    fun testNormalizeCurrent(raw: Int): Int = normalizeCurrent(raw)

    fun testCalculateCurrentConfidence(raw: Int): Confidence = calculateCurrentConfidence(raw)

    fun testAlignCurrentSignWithChargeState(currentMa: Int): Int =
        alignCurrentSignWithChargeState(currentMa)

    fun testMapHealth(health: Int) = mapHealth(health)

    fun testMapChargingStatus(status: Int) = mapChargingStatus(status)

    fun testMapPlugType(plugged: Int) = mapPlugType(plugged)
}
```

- [x] **Step 3: Add current sign alignment tests**

Add imports:

```kotlin
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.PlugType
```

Add this test:

```kotlin
@Test
fun `alignCurrentSignWithChargeState corrects signs that disagree with charge state`() {
    val chargingSource =
        createTestSource(
            unit = CurrentUnit.MILLIAMPS,
            convention = SignConvention.POSITIVE_CHARGING,
            isCharging = true,
        )
    val dischargingSource =
        createTestSource(
            unit = CurrentUnit.MILLIAMPS,
            convention = SignConvention.POSITIVE_CHARGING,
            isCharging = false,
        )

    assertEquals(500, chargingSource.testAlignCurrentSignWithChargeState(-500))
    assertEquals(500, chargingSource.testAlignCurrentSignWithChargeState(500))
    assertEquals(-500, dischargingSource.testAlignCurrentSignWithChargeState(500))
    assertEquals(-500, dischargingSource.testAlignCurrentSignWithChargeState(-500))
}
```

- [x] **Step 4: Add enum mapping tests**

Add this test:

```kotlin
@Test
fun `battery intent integer mappings fall back safely for unknown values`() {
    val source =
        createTestSource(
            unit = CurrentUnit.MILLIAMPS,
            convention = SignConvention.POSITIVE_CHARGING,
        )

    assertEquals(BatteryHealth.GOOD, source.testMapHealth(BatteryManager.BATTERY_HEALTH_GOOD))
    assertEquals(BatteryHealth.OVERHEAT, source.testMapHealth(BatteryManager.BATTERY_HEALTH_OVERHEAT))
    assertEquals(BatteryHealth.UNKNOWN, source.testMapHealth(-1))

    assertEquals(ChargingStatus.CHARGING, source.testMapChargingStatus(BatteryManager.BATTERY_STATUS_CHARGING))
    assertEquals(ChargingStatus.DISCHARGING, source.testMapChargingStatus(BatteryManager.BATTERY_STATUS_DISCHARGING))
    assertEquals(ChargingStatus.NOT_CHARGING, source.testMapChargingStatus(-1))

    assertEquals(PlugType.AC, source.testMapPlugType(BatteryManager.BATTERY_PLUGGED_AC))
    assertEquals(PlugType.USB, source.testMapPlugType(BatteryManager.BATTERY_PLUGGED_USB))
    assertEquals(PlugType.WIRELESS, source.testMapPlugType(BatteryManager.BATTERY_PLUGGED_WIRELESS))
    assertEquals(PlugType.NONE, source.testMapPlugType(-1))
}
```

- [x] **Step 5: Run focused verification**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.runcheck.data.battery.GenericBatterySourceTest --console=plain
git diff --check -- app\src\test\java\com\runcheck\data\battery\GenericBatterySourceTest.kt
```

Expected:
- Gradle task succeeds.
- No whitespace errors.

- [ ] **Step 6: Commit**

```powershell
git add app\src\test\java\com\runcheck\data\battery\GenericBatterySourceTest.kt
git commit -m "Lisää akun mittausapureiden testikattavuutta"
```

---

### Task 3: Battery Degradation Insight Rule Coverage

**Files:**
- Create: `app/src/test/java/com/runcheck/domain/insights/rules/BatteryDegradationTrendRuleTest.kt`
- No production code changes expected.

- [x] **Step 1: Create the test file**

Create `BatteryDegradationTrendRuleTest.kt` with this content:

```kotlin
package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.model.BatteryReading
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryDegradationTrendRuleTest {
    @Test
    fun `emits high priority battery insight when current week drains faster than previous week`() =
        runTest {
            val now = WINDOW_MS * 3
            val previousWindowStart = now - (WINDOW_MS * 2)
            val currentWindowStart = now - WINDOW_MS
            val readings =
                windowReadings(previousWindowStart, startLevel = 100, dropPerSample = 1) +
                    windowReadings(currentWindowStart, startLevel = 100, dropPerSample = 3)
            val rule =
                BatteryDegradationTrendRule(
                    batteryRepository = TestBatteryRepository(readings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                )

            val result = rule.evaluate(now)

            assertEquals(1, result.size)
            val insight = result.single()
            assertEquals(BatteryDegradationTrendRule.RULE_ID, insight.ruleId)
            assertEquals(InsightType.BATTERY, insight.type)
            assertEquals(InsightPriority.HIGH, insight.priority)
            assertEquals(InsightTarget.BATTERY, insight.target)
            assertEquals(listOf("200"), insight.bodyArgs)
            assertTrue(insight.confidence >= 0.5f)
        }

    @Test
    fun `returns empty when either comparison window has too few discharging readings`() =
        runTest {
            val now = WINDOW_MS * 3
            val previousWindowStart = now - (WINDOW_MS * 2)
            val currentWindowStart = now - WINDOW_MS
            val readings =
                windowReadings(previousWindowStart, startLevel = 100, dropPerSample = 1, count = 20) +
                    windowReadings(currentWindowStart, startLevel = 100, dropPerSample = 3, count = 10)
            val rule =
                BatteryDegradationTrendRule(
                    batteryRepository = TestBatteryRepository(readings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                )

            assertTrue(rule.evaluate(now).isEmpty())
        }

    @Test
    fun `returns empty when drain increase is below threshold`() =
        runTest {
            val now = WINDOW_MS * 3
            val previousWindowStart = now - (WINDOW_MS * 2)
            val currentWindowStart = now - WINDOW_MS
            val readings =
                windowReadings(previousWindowStart, startLevel = 100, dropPerSample = 2) +
                    windowReadings(currentWindowStart, startLevel = 100, dropPerSample = 2)
            val rule =
                BatteryDegradationTrendRule(
                    batteryRepository = TestBatteryRepository(readings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                )

            assertTrue(rule.evaluate(now).isEmpty())
        }

    private fun windowReadings(
        start: Long,
        startLevel: Int,
        dropPerSample: Int,
        count: Int = 20,
    ): List<BatteryReading> {
        val interval = WINDOW_MS / (count + 1)
        return (0 until count).map { index ->
            batteryReading(
                timestamp = start + ((index + 1) * interval),
                level = startLevel - (index * dropPerSample),
            )
        }
    }

    private companion object {
        private const val WINDOW_MS = 7L * 24L * 60L * 60L * 1000L
    }
}
```

- [x] **Step 2: Run focused verification**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.runcheck.domain.insights.rules.BatteryDegradationTrendRuleTest --console=plain
git diff --check -- app\src\test\java\com\runcheck\domain\insights\rules\BatteryDegradationTrendRuleTest.kt
```

Expected:
- Gradle task succeeds.
- No whitespace errors.

- [ ] **Step 3: Commit**

```powershell
git add app\src\test\java\com\runcheck\domain\insights\rules\BatteryDegradationTrendRuleTest.kt
git commit -m "Lisää akun heikkenemistrendin regressiotestit"
```

---

### Task 4: Thermal ViewModel Coverage

**Files:**
- Create: `app/src/test/java/com/runcheck/ui/thermal/ThermalViewModelTest.kt`
- No production code changes expected.

- [x] **Step 1: Create the test file**

Create `ThermalViewModelTest.kt` with this content:

```kotlin
package com.runcheck.ui.thermal

import androidx.lifecycle.SavedStateHandle
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.usecase.GetThermalHistoryUseCase
import com.runcheck.domain.usecase.GetThermalStateUseCase
import com.runcheck.domain.usecase.GetThrottlingHistoryUseCase
import com.runcheck.domain.usecase.ManageInfoCardDismissalsUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.ui.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThermalViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getThermalState: GetThermalStateUseCase = mockk()
    private val getThrottlingHistory: GetThrottlingHistoryUseCase = mockk()
    private val getThermalHistory: GetThermalHistoryUseCase = mockk()
    private val observeProAccess: ObserveProAccessUseCase = mockk()
    private val manageUserPreferences: ManageUserPreferencesUseCase = mockk(relaxed = true)
    private val manageInfoCardDismissals: ManageInfoCardDismissalsUseCase = mockk(relaxed = true)

    @Before
    fun setup() {
        every { getThrottlingHistory() } returns flowOf(emptyList())
        every { getThermalHistory(any()) } returns flowOf(emptyList())
        every { observeProAccess() } returns flowOf(true)
        every { manageUserPreferences.observePreferences() } returns flowOf(UserPreferences())
        every { manageInfoCardDismissals.observeDismissedCardIds() } returns flowOf(emptySet())
    }

    @Test
    fun `startObserving emits success state with live thermal buffers and session bounds`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val thermalFlow = MutableSharedFlow<ThermalState>()
            every { getThermalState() } returns thermalFlow
            val viewModel = createViewModel()

            viewModel.startObserving()
            runCurrent()
            thermalFlow.emit(thermalState(tempC = 34f, headroom = 0.6f))
            advanceThermalSample()
            thermalFlow.emit(thermalState(tempC = 38f, headroom = 0.4f))
            advanceThermalSample()

            val state = viewModel.uiState.value
            assertTrue("Expected Success but got $state", state is ThermalUiState.Success)
            val success = state as ThermalUiState.Success
            assertEquals(34f, success.sessionMinTemp ?: 0f, 0.01f)
            assertEquals(38f, success.sessionMaxTemp ?: 0f, 0.01f)
            assertEquals(listOf(34f, 38f), success.liveTempC)
            assertEquals(listOf(0.6f, 0.4f), success.liveHeadroom)
            assertTrue(success.isPro)
        }

    @Test
    fun `history period selection persists and reloads history`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val thermalFlow = MutableSharedFlow<ThermalState>()
            val weekReadings = listOf(thermalReading(timestamp = 1L, tempC = 36f))
            every { getThermalState() } returns thermalFlow
            every { getThermalHistory(HistoryPeriod.WEEK) } returns flowOf(weekReadings)
            val viewModel = createViewModel()

            viewModel.startObserving()
            runCurrent()
            thermalFlow.emit(thermalState(tempC = 35f))
            advanceThermalSample()
            viewModel.setHistoryPeriod(HistoryPeriod.WEEK)
            runCurrent()

            val success = viewModel.uiState.value as ThermalUiState.Success
            assertEquals(HistoryPeriod.WEEK, success.selectedHistoryPeriod)
            assertEquals(weekReadings, success.thermalHistory)
        }

    @Test
    fun `thermal state collection failure emits error state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getThermalState() } returns flow { throw IllegalStateException("thermal failed") }
            val viewModel = createViewModel()

            viewModel.startObserving()
            runCurrent()

            assertEquals(ThermalUiState.Error("thermal failed"), viewModel.uiState.value)
        }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): ThermalViewModel =
        ThermalViewModel(
            savedStateHandle = savedStateHandle,
            getThermalState = getThermalState,
            getThrottlingHistory = getThrottlingHistory,
            getThermalHistory = getThermalHistory,
            observeProAccess = observeProAccess,
            manageUserPreferences = manageUserPreferences,
            manageInfoCardDismissals = manageInfoCardDismissals,
        )

    private fun advanceThermalSample() {
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
        mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(334L)
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
    }

    private fun thermalState(
        tempC: Float,
        headroom: Float? = null,
    ): ThermalState =
        ThermalState(
            batteryTempC = tempC,
            cpuTempC = null,
            thermalHeadroom = headroom,
            thermalStatus = ThermalStatus.NONE,
            isThrottling = false,
        )

    private fun thermalReading(
        timestamp: Long,
        tempC: Float,
    ): ThermalReading =
        ThermalReading(
            timestamp = timestamp,
            batteryTempC = tempC,
            cpuTempC = null,
            thermalStatus = 0,
            throttling = false,
        )
}
```

- [x] **Step 2: Run focused verification**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.runcheck.ui.thermal.ThermalViewModelTest --console=plain
git diff --check -- app\src\test\java\com\runcheck\ui\thermal\ThermalViewModelTest.kt
```

Expected:
- Gradle task succeeds.
- No whitespace errors.

- [ ] **Step 3: Commit**

```powershell
git add app\src\test\java\com\runcheck\ui\thermal\ThermalViewModelTest.kt
git commit -m "Lisää ThermalViewModelin tilatestit"
```

---

### Task 5: Charger ViewModel Coverage

**Files:**
- Create: `app/src/test/java/com/runcheck/ui/charger/ChargerViewModelTest.kt`
- No production code changes expected.

- [x] **Step 1: Create the test file**

Create `ChargerViewModelTest.kt` with this content:

```kotlin
package com.runcheck.ui.charger

import com.runcheck.domain.model.ChargerSummary
import com.runcheck.domain.usecase.AddChargerUseCase
import com.runcheck.domain.usecase.DeleteChargerUseCase
import com.runcheck.domain.usecase.GetChargerComparisonUseCase
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChargerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getChargerComparison: GetChargerComparisonUseCase = mockk()
    private val addChargerUseCase: AddChargerUseCase = mockk(relaxed = true)
    private val deleteChargerUseCase: DeleteChargerUseCase = mockk(relaxed = true)
    private val observeProAccess: ObserveProAccessUseCase = mockk()
    private val isProUser: IsProUserUseCase = mockk()
    private val manageUserPreferences: ManageUserPreferencesUseCase = mockk(relaxed = true)

    @Before
    fun setup() {
        every { observeProAccess() } returns flowOf(false)
        every { isProUser() } returns false
        every { manageUserPreferences.observeSelectedChargerId() } returns flowOf(null)
        every { getChargerComparison() } returns flowOf(emptyList())
    }

    @Test
    fun `refresh locks charger comparison for non pro users`() {
        val viewModel = createViewModel()

        viewModel.refresh()

        assertEquals(ChargerUiState.Locked, viewModel.uiState.value)
    }

    @Test
    fun `pro observer loads charger data and selected charger`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val chargers = listOf(chargerSummary(id = 7L, name = "Desk charger"))
            every { observeProAccess() } returns flowOf(true)
            every { getChargerComparison() } returns flowOf(chargers)
            every { manageUserPreferences.observeSelectedChargerId() } returns flowOf(7L)
            val viewModel = createViewModel()

            viewModel.startObserving()
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue("Expected Success but got $state", state is ChargerUiState.Success)
            val success = state as ChargerUiState.Success
            assertEquals(chargers, success.chargers)
            assertEquals(7L, success.selectedChargerId)
        }

    @Test
    fun `pro actions are ignored when user is not pro`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { isProUser() } returns false
            val viewModel = createViewModel()

            viewModel.addCharger("Travel charger")
            viewModel.deleteCharger(3L)
            viewModel.selectCharger(3L)
            viewModel.clearSelectedCharger()
            runCurrent()

            coVerify(exactly = 0) { addChargerUseCase(any()) }
            coVerify(exactly = 0) { deleteChargerUseCase(any()) }
            coVerify(exactly = 0) { manageUserPreferences.setSelectedChargerId(any()) }
        }

    @Test
    fun `load errors are exposed as error state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { observeProAccess() } returns flowOf(true)
            every { getChargerComparison() } returns flow { throw IllegalStateException("charger failed") }
            val viewModel = createViewModel()

            viewModel.startObserving()
            runCurrent()

            assertEquals(ChargerUiState.Error("charger failed"), viewModel.uiState.value)
        }

    private fun createViewModel(): ChargerViewModel =
        ChargerViewModel(
            getChargerComparison = getChargerComparison,
            addChargerUseCase = addChargerUseCase,
            deleteChargerUseCase = deleteChargerUseCase,
            observeProAccess = observeProAccess,
            isProUser = isProUser,
            manageUserPreferences = manageUserPreferences,
        )

    private fun chargerSummary(
        id: Long,
        name: String,
    ): ChargerSummary =
        ChargerSummary(
            chargerId = id,
            chargerName = name,
            sessionCount = 2,
            avgChargingSpeedMa = 1_200,
            avgPowerMw = 5_000,
            latestChargingSpeedMa = 1_300,
            latestPowerMw = 5_200,
            avgTimeToFullMinutes = 90,
            lastUsed = 123L,
            hasActiveSession = false,
        )
}
```

- [x] **Step 2: Run focused verification**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.runcheck.ui.charger.ChargerViewModelTest --console=plain
git diff --check -- app\src\test\java\com\runcheck\ui\charger\ChargerViewModelTest.kt
```

Expected:
- Gradle task succeeds.
- No whitespace errors.

- [ ] **Step 3: Commit**

```powershell
git add app\src\test\java\com\runcheck\ui\charger\ChargerViewModelTest.kt
git commit -m "Lisää ChargerViewModelin pro-tilatestit"
```

---

### Task 6: Repository Mapping Coverage

**Files:**
- Create: `app/src/test/java/com/runcheck/data/insights/InsightRepositoryImplTest.kt`
- No production code changes expected.

Current note: this task was attempted and then reverted in this pass because `InsightRepositoryImpl` uses Android's `org.json.JSONArray`; local JVM unit tests hit Android's not-mocked JSON stubs. Revisit this as either a Robolectric/instrumented test, a documented JSON codec seam, or a separate dependency-verification-aware test dependency decision.

- [ ] **Step 1: Create repository mapping tests without production seams**

Create `InsightRepositoryImplTest.kt` with this content:

```kotlin
package com.runcheck.data.insights

import com.runcheck.data.db.dao.InsightDao
import com.runcheck.data.db.entity.InsightEntity
import com.runcheck.domain.insights.engine.InsightHomeRankingPolicy
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.repository.DatabaseTransactionRunner
import com.runcheck.util.AppDispatchers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightRepositoryImplTest {
    private val transactionRunner =
        object : DatabaseTransactionRunner {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
        }

    @Test
    fun `replaceRuleResults preserves existing seen and dismissed state for matching dedupe keys`() =
        runTest {
            val insightDao: InsightDao = mockk(relaxed = true)
            val repository = createRepository(insightDao)
            coEvery { insightDao.getByRule("rule") } returns
                listOf(
                    insightEntity(
                        id = 5L,
                        ruleId = "rule",
                        dedupeKey = "same",
                        dismissed = true,
                        seen = true,
                    ),
                    insightEntity(
                        id = 6L,
                        ruleId = "rule",
                        dedupeKey = "stale",
                    ),
                )

            repository.replaceRuleResults(
                ruleId = "rule",
                candidates =
                    listOf(
                        InsightCandidate(
                            ruleId = "rule",
                            dedupeKey = "same",
                            type = InsightType.BATTERY,
                            priority = InsightPriority.HIGH,
                            confidence = 0.9f,
                            titleKey = "new_title",
                            bodyKey = "new_body",
                            bodyArgs = listOf("42"),
                            generatedAt = 10L,
                            expiresAt = 20L,
                            dataWindowStart = 7L,
                            dataWindowEnd = 8L,
                            target = InsightTarget.BATTERY,
                        ),
                    ),
            )

            val slot = slot<List<InsightEntity>>()
            coVerify { insightDao.deleteByIds(listOf(6L)) }
            coVerify { insightDao.insertAll(capture(slot)) }
            val merged = slot.captured.single()
            assertEquals(5L, merged.id)
            assertEquals(true, merged.dismissed)
            assertEquals(true, merged.seen)
            assertEquals("[\"42\"]", merged.bodyArgsJson)
        }

    @Test
    fun `getActiveInsights maps live entities and filters expired rows`() =
        runTest {
            val insightDao: InsightDao = mockk(relaxed = true)
            val now = System.currentTimeMillis()
            every { insightDao.observeUndismissedInsights() } returns
                flowOf(
                    listOf(
                        insightEntity(
                            id = 1L,
                            expiresAt = now + 60_000L,
                            bodyArgsJson = "[\"25\"]",
                        ),
                        insightEntity(
                            id = 2L,
                            expiresAt = now - 1L,
                        ),
                    ),
                )
            val repository = createRepository(insightDao)

            val result = repository.getActiveInsights().first()

            assertEquals(1, result.size)
            val insight = result.single()
            assertEquals(1L, insight.id)
            assertEquals(InsightType.BATTERY, insight.type)
            assertEquals(InsightPriority.HIGH, insight.priority)
            assertEquals(InsightTarget.BATTERY, insight.target)
            assertEquals(listOf("25"), insight.bodyArgs)
        }

    private fun createRepository(insightDao: InsightDao): InsightRepositoryImpl =
        InsightRepositoryImpl(
            insightDao = insightDao,
            homeRankingPolicy = mockk<InsightHomeRankingPolicy>(relaxed = true),
            transactionRunner = transactionRunner,
            dispatchers = TestDispatchers(),
        )

    private fun insightEntity(
        id: Long = 1L,
        ruleId: String = "rule",
        dedupeKey: String = "same",
        priority: Int = InsightPriority.HIGH.sortOrder,
        bodyArgsJson: String = "[]",
        generatedAt: Long = 1L,
        expiresAt: Long = generatedAt + 60_000L,
        dismissed: Boolean = false,
        seen: Boolean = false,
    ): InsightEntity =
        InsightEntity(
            id = id,
            ruleId = ruleId,
            dedupeKey = dedupeKey,
            type = InsightType.BATTERY.name,
            priority = priority,
            confidence = 0.8f,
            titleKey = "title",
            bodyKey = "body",
            bodyArgsJson = bodyArgsJson,
            generatedAt = generatedAt,
            expiresAt = expiresAt,
            dataWindowStart = 0L,
            dataWindowEnd = 1L,
            target = InsightTarget.BATTERY.name,
            dismissed = dismissed,
            seen = seen,
        )

    private class TestDispatchers : AppDispatchers() {
        override val io = Dispatchers.Unconfined
    }
}
```

- [ ] **Step 2: Run focused verification**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.runcheck.data.insights.InsightRepositoryImplTest --console=plain
git diff --check -- app\src\test\java\com\runcheck\data\insights\InsightRepositoryImplTest.kt
```

Expected:
- Gradle task succeeds.
- No whitespace errors.

- [ ] **Step 3: Commit**

```powershell
git add app\src\test\java\com\runcheck\data\insights\InsightRepositoryImplTest.kt
git commit -m "Lisää insight-repositoryn yhdistelytestit"
```

---

### Task 7: Battery Repository Coverage

**Files:**
- Create: `app/src/test/java/com/runcheck/data/battery/BatteryRepositoryImplTest.kt`
- No production code changes expected unless Room DAO mocking exposes a final-method limitation.

- [x] **Step 1: Create repository persistence and filtering tests**

Create `BatteryRepositoryImplTest.kt` with this content:

```kotlin
package com.runcheck.data.battery

import com.runcheck.data.db.dao.BatteryReadingDao
import com.runcheck.data.db.entity.BatteryReadingEntity
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.PlugType
import com.runcheck.util.AppDispatchers
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryRepositoryImplTest {
    @Test
    fun `saveReading stores null current when confidence is unavailable`() =
        runTest {
            val dao: BatteryReadingDao = mockk(relaxed = true)
            val repository = createRepository(dao)

            repository.saveReading(
                BatteryState(
                    level = 55,
                    voltageMv = 3900,
                    temperatureC = 31f,
                    currentMa = MeasuredValue(0, Confidence.UNAVAILABLE),
                    chargingStatus = ChargingStatus.DISCHARGING,
                    plugType = PlugType.NONE,
                    health = BatteryHealth.GOOD,
                    technology = "Li-ion",
                ),
            )

            val slot = slot<BatteryReadingEntity>()
            coVerify { dao.insert(capture(slot)) }
            assertEquals(null, slot.captured.currentMa)
            assertEquals(Confidence.UNAVAILABLE.name, slot.captured.currentConfidence)
        }

    @Test
    fun `getReadingsSince filters unusable timestamps after DAO returns rows`() =
        runTest {
            val dao: BatteryReadingDao = mockk(relaxed = true)
            every { dao.getReadingsSince(10L) } returns
                flowOf(
                    listOf(
                        batteryReadingEntity(timestamp = 0L),
                        batteryReadingEntity(timestamp = 123L),
                    ),
                )
            val repository = createRepository(dao)

            val result = repository.getReadingsSince(since = 10L, limit = null).first()

            assertEquals(listOf(123L), result.map { it.timestamp })
        }

    @Test
    fun `getReadingsSince delegates to limited query when limit is provided`() =
        runTest {
            val dao: BatteryReadingDao = mockk(relaxed = true)
            every { dao.getReadingsSinceLimited(10L, 1) } returns
                flowOf(listOf(batteryReadingEntity(timestamp = 123L)))
            val repository = createRepository(dao)

            val result = repository.getReadingsSince(since = 10L, limit = 1).first()

            assertEquals(1, result.size)
            assertEquals(123L, result.single().timestamp)
        }

    private fun createRepository(dao: BatteryReadingDao): BatteryRepositoryImpl =
        BatteryRepositoryImpl(
            batteryDataSourceFactory = mockk(relaxed = true),
            deviceProfileProvider = mockk(relaxed = true),
            batteryReadingDao = dao,
            batteryCapacityReader = mockk(relaxed = true),
            dispatchers = TestDispatchers(),
        )

    private fun batteryReadingEntity(timestamp: Long): BatteryReadingEntity =
        BatteryReadingEntity(
            id = timestamp,
            timestamp = timestamp,
            level = 55,
            voltageMv = 3900,
            temperatureC = 31f,
            currentMa = -250,
            currentConfidence = Confidence.ACCURATE.name,
            status = ChargingStatus.DISCHARGING.name,
            plugType = PlugType.NONE.name,
            health = BatteryHealth.GOOD.name,
            cycleCount = null,
            healthPct = null,
        )

    private class TestDispatchers : AppDispatchers() {
        override val io = Dispatchers.Unconfined
    }
}
```

- [x] **Step 2: Run focused verification**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.runcheck.data.battery.BatteryRepositoryImplTest --console=plain
git diff --check -- app\src\test\java\com\runcheck\data\battery\BatteryRepositoryImplTest.kt
```

Expected:
- Gradle task succeeds.
- No whitespace errors.

- [ ] **Step 3: Commit**

```powershell
git add app\src\test\java\com\runcheck\data\battery\BatteryRepositoryImplTest.kt
git commit -m "Lisää akku-repositoryn tallennustestit"
```

---

### Task 8: Existing ViewModel Edge Coverage

**Files:**
- Modify: `app/src/test/java/com/runcheck/ui/storage/cleanup/CleanupViewModelTest.kt`
- Modify: `app/src/test/java/com/runcheck/ui/settings/SettingsViewModelTest.kt`

- [x] **Step 1: Add cleanup non-pro branch test**

Add to `CleanupViewModelTest`:

```kotlin
@Test
fun `cleanup scan returns pro locked error for non pro users`() =
    runTest(mainDispatcherRule.testDispatcher) {
        every { isProUser() } returns false

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error but got $state", state is CleanupUiState.Error)
        assertEquals(
            UiText.Resource(R.string.pro_feature_locked_generic),
            (state as CleanupUiState.Error).message,
        )
        coVerify(exactly = 0) { storageCleanup.getCleanupSummary(any()) }
    }
```

Add this import to `CleanupViewModelTest.kt`:

```kotlin
import com.runcheck.R
```

- [x] **Step 2: Add settings debug failure branch test**

Add to `SettingsViewModelTest`:

```kotlin
@Test
fun `seed demo insights exposes failure and clears busy flag`() =
    runTest(mainDispatcherRule.testDispatcher) {
        coEvery { insightDebugActions.seedDemoInsights() } throws IllegalStateException("seed failed")
        val viewModel = createViewModel()
        runCurrent()

        viewModel.seedDemoInsights()
        runCurrent()

        assertFalse(viewModel.uiState.value.isProcessingDebugInsights)
        assertEquals(
            UiText.Resource(R.string.common_error_generic),
            viewModel.uiState.value.errorMessage,
        )
        assertEquals(null, viewModel.uiState.value.debugStatus)
    }
```

- [x] **Step 3: Run focused verification**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.runcheck.ui.storage.cleanup.CleanupViewModelTest --tests com.runcheck.ui.settings.SettingsViewModelTest --console=plain
git diff --check -- app\src\test\java\com\runcheck\ui\storage\cleanup\CleanupViewModelTest.kt app\src\test\java\com\runcheck\ui\settings\SettingsViewModelTest.kt
```

Expected:
- Gradle task succeeds.
- No whitespace errors.

- [ ] **Step 4: Commit**

```powershell
git add app\src\test\java\com\runcheck\ui\storage\cleanup\CleanupViewModelTest.kt app\src\test\java\com\runcheck\ui\settings\SettingsViewModelTest.kt
git commit -m "Lisää asetusten ja siivouksen virhepolkutestit"
```

---

### Task 9: Framework Boundary Decision

**Files:**
- Modify only if justified: `sonar-project.properties`
- Modify only if justified: `app/build.gradle.kts`
- Optional create: `docs/coverage-boundary-decisions.md`

- [ ] **Step 1: Classify framework-heavy files before editing**

Review these files and decide per file:

| File | Default decision |
|---|---|
| `app/src/main/java/com/runcheck/data/network/SpeedTestService.kt` | Prefer small pure helper extraction only if testing error/server metadata mapping matters; otherwise leave as integration boundary |
| `app/src/main/java/com/runcheck/data/preferences/UserPreferencesRepositoryImpl.kt` | Prefer Robolectric/DataStore integration test if this becomes risk-bearing; do not mock private `Context.dataStore` extension |
| `app/src/main/java/com/runcheck/data/network/NetworkDataSource.kt` | Keep coverage-excluded unless Android framework test is intentionally added |
| `app/src/main/java/com/runcheck/data/storage/StorageDataSource.kt` | Keep coverage-excluded unless Android framework test is intentionally added |
| `app/src/main/java/com/runcheck/data/appusage/AppUsageDataSource.kt` | Keep coverage-excluded unless Android framework test is intentionally added |

- [ ] **Step 2: If excluding, keep Sonar and JaCoCo exclusions aligned**

For every new exclusion added to `sonar.coverage.exclusions`, add the matching class pattern to `jacocoDebugUnitTestReportExclusions` in `app/build.gradle.kts`.

Example only if `SpeedTestService` is formally classified as an integration boundary:

```properties
sonar.coverage.exclusions=...,app/src/main/java/com/runcheck/data/network/SpeedTestService.kt
```

```kotlin
"**/data/network/SpeedTestService*.*",
```

- [ ] **Step 3: Run narrow configuration verification**

```powershell
.\gradlew.bat :app:jacocoDebugUnitTestReport --console=plain
git diff --check -- sonar-project.properties app\build.gradle.kts docs\coverage-boundary-decisions.md
```

Expected:
- JaCoCo report task succeeds.
- Exclusion patterns stay intentionally narrow.
- No whitespace errors.

- [ ] **Step 4: Commit only if a config/doc change was made**

```powershell
git add sonar-project.properties app\build.gradle.kts docs\coverage-boundary-decisions.md
git commit -m "Rajaa coverage-rajapinnat perustellusti"
```

---

### Task 10: Coverage Measurement Pass

**Files:**
- No source edits expected.
- Read generated report: `app/build/reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml`

- [x] **Step 1: Generate local coverage XML**

```powershell
.\gradlew.bat :app:jacocoDebugUnitTestReport --console=plain
```

Expected:
- Build succeeds.
- XML exists at `app/build/reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml`.

- [x] **Step 2: Print local line and branch coverage**

```powershell
[xml]$xml = Get-Content -Path .\app\build\reports\jacoco\jacocoDebugUnitTestReport\jacocoDebugUnitTestReport.xml
$line = $xml.report.counter | Where-Object type -eq 'LINE'
$branch = $xml.report.counter | Where-Object type -eq 'BRANCH'
[pscustomobject]@{
    lineMissed = [int]$line.missed
    lineCovered = [int]$line.covered
    lineCoverage = [math]::Round(([int]$line.covered / ([int]$line.covered + [int]$line.missed)) * 100, 2)
    branchMissed = [int]$branch.missed
    branchCovered = [int]$branch.covered
    branchCoverage = [math]::Round(([int]$branch.covered / ([int]$branch.covered + [int]$branch.missed)) * 100, 2)
} | Format-List
```

Expected:
- Line coverage and branch coverage improve from the baseline recorded in this plan.

Current measured result after Tasks 1-5, 7, 8, plus the additional repository/ViewModel/chart/insight tests:

| Metric | Value |
|---|---:|
| Covered lines | 4,980 |
| Missed lines | 2,161 |
| Local line coverage | 69.74% |
| Covered branches | 1,379 |
| Missed branches | 1,196 |
| Local branch coverage | 53.55% |

`InsightRepositoryImplTest` is now included. The prior Android `org.json.JSONArray` JVM-test blocker was removed by replacing the private body-args JSON helpers with a small Gson-based codec while keeping the stored JSON array format.

Additional low-risk tests added after the first pass:

- `ManageUserPreferencesUseCaseTest`
- `ChargerRepositoryImplTest`
- `ThrottlingRepositoryImplTest`
- `ThermalRepositoryImplTest`
- `NetworkRepositoryImplTest`
- `AppBatteryUsageRepositoryImplTest`
- `StorageRepositoryImplTest`
- `DeviceProfileRepositoryImplTest`
- `AppUsageViewModelTest`
- `InsightRepositoryImplTest`
- More `ChartRenderModelTest` model-builder coverage

- [ ] **Step 3: Optional Sonar upload only when requested**

Do not run `tools\sonar.ps1`, `sonar`, or GitHub CI manually unless the user asks. If requested, use the existing wrapper and token requirements:

```powershell
tools\sonar.ps1 -PlanOnly
```

Then run the real scan only with `SONAR_TOKEN` available.

---

## Expected Impact

This plan should improve overall coverage incrementally while adding tests that protect real behavior:

- Chart helpers: more branch coverage around chart axes, session summary null paths, quality-zone mapping.
- Battery source: more branch coverage around current sign correction and Android battery constant mapping.
- Battery degradation rule: covers one current 0% domain rule with user-visible insight behavior.
- Thermal and Charger ViewModels: covers two currently untested UI state machines without Compose UI rendering.
- Repository tests: covers data mapping and merge behavior without using a real database.

Do not expect one pass to take overall coverage from 52.8% to 80%. The realistic first milestone is to keep new-code coverage green and move overall coverage upward in small, defensible steps.

## Self-Review

- Spec coverage: The plan answers why overall coverage is low, identifies top actionable files, separates true test targets from Android framework boundaries, and gives focused commands.
- Placeholder scan: No step contains incomplete-marker text or an unspecified "write tests" instruction.
- Type consistency: Code snippets use live package names and current test dependencies observed in this checkout.
- Risk check: Production code changes are not required through Tasks 1-5 and are constrained in later tasks to small testability seams only if compilation proves they are needed.
