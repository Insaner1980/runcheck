# Consistency Cleanup Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete all remaining consistency items from `docs/reviews/2026-03-13-consistency-follow-up.md` — standardize NetworkViewModel to sealed interface, add detekt, fix Finnish string placement, and document resolved decisions.

**Architecture:** Split `NetworkUiState` flat data class into sealed `NetworkUiState` (Loading/Success/Error) + separate `SpeedTestUiState` flow. Add detekt 2.0.0-alpha.2 (required for Kotlin 2.3.0 compatibility). Modifier ordering in SettingsScreen is already correct (interaction before padding = full-row touch targets) — no changes needed.

**Tech Stack:** Kotlin, Jetpack Compose, Gradle version catalog, detekt

---

## Chunk 1: NetworkUiState Sealed Interface Conversion

### Task 1: Convert NetworkUiState to sealed interface

**Files:**
- Modify: `app/src/main/java/com/devicepulse/ui/network/NetworkUiState.kt`

- [ ] **Step 1: Replace the flat data class with sealed interface**

Replace the current `NetworkUiState` data class (lines 7-13) with:

```kotlin
sealed interface NetworkUiState {
    data object Loading : NetworkUiState

    @Immutable
    data class Success(
        val networkState: NetworkState
    ) : NetworkUiState

    data class Error(val message: String) : NetworkUiState
}
```

Keep `SpeedTestPhase` and `SpeedTestUiState` exactly as they are — they remain unchanged.

Remove the `import com.devicepulse.domain.model.SpeedTestResult` if it was only used by the old `NetworkUiState` (it's used by `SpeedTestUiState`, so it stays).

### Task 2: Update NetworkViewModel to use two state flows

**Files:**
- Modify: `app/src/main/java/com/devicepulse/ui/network/NetworkViewModel.kt`

- [ ] **Step 1: Replace single state flow with two flows**

Replace lines 41-42:
```kotlin
private val _uiState = MutableStateFlow(NetworkUiState())
val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()
```

With:
```kotlin
private val _networkUiState = MutableStateFlow<NetworkUiState>(NetworkUiState.Loading)
val networkUiState: StateFlow<NetworkUiState> = _networkUiState.asStateFlow()

private val _speedTestState = MutableStateFlow(SpeedTestUiState())
val speedTestState: StateFlow<SpeedTestUiState> = _speedTestState.asStateFlow()
```

- [ ] **Step 2: Update `startSpeedTest()` to use new flows**

Line 68 — replace `_uiState.value.speedTestState.isRunning` with `_speedTestState.value.isRunning`.

Line 127 — replace `_uiState.value.networkState` with:
```kotlin
val networkState = (_networkUiState.value as? NetworkUiState.Success)?.networkState
```

- [ ] **Step 3: Update `loadNetworkData()` to emit sealed states**

Replace `loadNetworkData()` body (lines 184-212) with:

```kotlin
private fun loadNetworkData() {
    networkJob?.cancel()
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
                _networkUiState.value = NetworkUiState.Success(networkState = state)
            }
    }
}
```

Note: On refresh when data already exists, we keep the existing Success state visible (no flash to Loading). Error only replaces Loading, not Success — this matches the UX pattern of other screens.

- [ ] **Step 4: Update `updateSpeedTestState` helper**

Replace lines 240-243:
```kotlin
private fun updateSpeedTestState(transform: SpeedTestUiState.() -> SpeedTestUiState) {
    _speedTestState.update { it.transform() }
}
```

- [ ] **Step 5: Remove unused imports**

Remove `import kotlinx.coroutines.flow.update` only if no longer used (it's still used by `_speedTestState.update`). Actually keep it — `_speedTestState.update` uses it.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/devicepulse/ui/network/NetworkUiState.kt \
      app/src/main/java/com/devicepulse/ui/network/NetworkViewModel.kt
git commit -m "Muunna NetworkUiState sealed-rajapinnaksi ja erota speed test -tila"
```

### Task 3: Update NetworkDetailScreen to use sealed state

**Files:**
- Modify: `app/src/main/java/com/devicepulse/ui/network/NetworkDetailScreen.kt`

- [ ] **Step 1: Update state collection**

Replace lines 81-83:
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
val networkState = uiState.networkState
val errorMessage = uiState.errorMessage
```

With:
```kotlin
val networkUiState by viewModel.networkUiState.collectAsStateWithLifecycle()
```

- [ ] **Step 2: Replace when-block with sealed when**

Replace lines 111-142 (the `when` block) with:

```kotlin
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
            networkState = state.networkState,
            onRefresh = { viewModel.refresh() },
            onNavigateToSpeedTest = onNavigateToSpeedTest
        )
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/devicepulse/ui/network/NetworkDetailScreen.kt
git commit -m "Päivitä NetworkDetailScreen käyttämään sealed-tilaa"
```

### Task 4: Update SpeedTestScreen to use new flows

**Files:**
- Modify: `app/src/main/java/com/devicepulse/ui/network/SpeedTestScreen.kt`

- [ ] **Step 1: Update state collection**

Replace the current state collection (around lines 91-93):
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
val networkState = uiState.networkState
val errorMessage = uiState.errorMessage
```

With:
```kotlin
val networkUiState by viewModel.networkUiState.collectAsStateWithLifecycle()
val speedTestState by viewModel.speedTestState.collectAsStateWithLifecycle()
```

- [ ] **Step 2: Update the when-block for loading/error/content**

Replace the `uiState.isLoading && networkState == null` pattern (around line 122) with sealed `when`:

```kotlin
when (val netState = networkUiState) {
    is NetworkUiState.Loading -> {
        // existing loading UI
    }
    is NetworkUiState.Error -> {
        // existing error UI using netState.message
    }
    is NetworkUiState.Success -> {
        SpeedTestContent(
            networkState = netState.networkState,
            speedTestState = speedTestState,
            // ... other params stay the same
        )
    }
}
```

- [ ] **Step 3: Update all `uiState.speedTestState` references**

Replace all occurrences of `uiState.speedTestState` with just `speedTestState` (it's now collected directly).

- [ ] **Step 4: Update all `uiState.networkState` references inside content**

If any content composables access `uiState.networkState`, they should now receive `networkState` as a parameter from the `Success` state.

- [ ] **Step 5: Compile and verify**

```bash
cd /home/emma/dev/DevicePulse && ./gradlew :app:compileDebugKotlin
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/devicepulse/ui/network/SpeedTestScreen.kt
git commit -m "Päivitä SpeedTestScreen käyttämään erillisiä tilafloita"
```

---

## Chunk 2: Finnish Strings Fix & Detekt Setup

### Task 5: Fix Finnish string placement

**Files:**
- Modify: `app/src/main/res/values-fi/strings.xml`

- [ ] **Step 1: Move misplaced settings strings to correct positions**

Lines 375-380 in values-fi contain 5 misplaced strings. They need to be inserted at TWO separate positions to match the English file ordering:

**Position A:** Insert these 4 strings between line 245 (`settings_measurement_not_available`) and line 246 (`settings_about`):
```xml
    <string name="settings_privacy">Tietosuoja</string>
    <string name="settings_crash_reporting">Jaa kaatumisraportit</string>
    <string name="settings_crash_reporting_desc">Lähetä anonyymejä kaatumisdiagnostiikkatietoja Firebase Crashlyticsiin virheiden korjaamista varten.</string>
    <string name="settings_crash_reporting_note">Pois oletuksena. Debug-versiot eivät koskaan lähetä raportteja. Kun asetus poistetaan käytöstä, odottavat lähettämättömät raportit poistetaan paikallisesti.</string>
```

**Position B:** Insert this 1 string between `settings_restore_unavailable` and `settings_pro_active` (originally lines 259-260, shifted by 4 after Position A insertion):
```xml
    <string name="settings_billing_unavailable">Play Billing ei ole käytettävissä tällä laitteella. Pro-oston vaihtoehdot piilotetaan, kunnes laskutus on saatavilla.</string>
```

**Then:** Remove the original block at the end of the file (the `<!-- Settings Privacy -->` comment and 5 strings before `</resources>`).

- [ ] **Step 2: Verify string count matches base**

```bash
cd /home/emma/dev/DevicePulse
grep -c '<string name=' app/src/main/res/values/strings.xml
grep -c '<string name=' app/src/main/res/values-fi/strings.xml
```

Counts should be equal.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values-fi/strings.xml
git commit -m "Siirrä suomenkieliset privacy-merkkijonot oikeaan kohtaan"
```

### Task 6: Add detekt to build system

**Context:** Detekt 1.23.x is incompatible with Kotlin 2.3.0 (metadata version mismatch). Must use detekt 2.0.0-alpha.2 which supports Kotlin 2.3.0 and Gradle 9.x. The plugin ID changed from `io.gitlab.arturbosch.detekt` to `dev.detekt`.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts` (root)
- Modify: `app/build.gradle.kts`
- Create: `config/detekt/detekt.yml`

- [ ] **Step 1: Add detekt to version catalog**

In `gradle/libs.versions.toml`:

Add to `[versions]`:
```toml
detekt = "2.0.0-alpha.2"
```

Add to `[plugins]`:
```toml
detekt = { id = "dev.detekt", version.ref = "detekt" }
```

- [ ] **Step 2: Add detekt plugin to root build.gradle.kts**

In `build.gradle.kts` (root), add to the plugins block:
```kotlin
alias(libs.plugins.detekt) apply false
```

- [ ] **Step 3: Add detekt plugin to app build.gradle.kts**

In `app/build.gradle.kts`, add to the plugins block:
```kotlin
alias(libs.plugins.detekt)
```

Add detekt configuration after the ktlint block:
```kotlin
detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    parallel = true
}
```

- [ ] **Step 4: Create detekt configuration**

Create `config/detekt/detekt.yml` with sensible defaults for an Android Compose project:

```yaml
complexity:
  LongMethod:
    threshold: 80
  LongParameterList:
    functionThreshold: 10
    constructorThreshold: 10
    ignoreAnnotated:
      - "Composable"
  TooManyFunctions:
    thresholdInFiles: 25
    thresholdInClasses: 20
    thresholdInInterfaces: 15
  CyclomaticComplexMethod:
    threshold: 20
  NestedBlockDepth:
    threshold: 5

style:
  MagicNumber:
    active: false
  ReturnCount:
    max: 4
  MaxLineLength:
    maxLineLength: 120
    excludeCommentStatements: true
  ForbiddenComment:
    active: false
  UnusedPrivateMember:
    active: true
    ignoreAnnotated:
      - "Preview"
      - "Composable"

naming:
  FunctionNaming:
    ignoreAnnotated:
      - "Composable"
  TopLevelPropertyNaming:
    constantPattern: "[A-Z][A-Za-z0-9]*"

exceptions:
  TooGenericExceptionCaught:
    active: true
    allowedExceptionNameRegex: "_|(ignore|expected).*"
```

- [ ] **Step 5: Update Gradle dependency locks if active**

```bash
cd /home/emma/dev/DevicePulse && ./gradlew dependencies --write-locks 2>/dev/null || true
```

- [ ] **Step 6: Run detekt and verify it works**

```bash
cd /home/emma/dev/DevicePulse && ./gradlew detekt
```

Fix any configuration issues. It's OK if detekt reports some findings — the goal is to get it running. Review findings and fix only clear issues (unused imports, etc.). Do NOT fix stylistic findings in this pass.

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts config/detekt/detekt.yml gradle.lockfile app/gradle.lockfile 2>/dev/null
git commit -m "Lisää detekt 2.0.0-alpha.2 staattiseen koodianalyysiin"
```

### Task 7: Run full verification

- [ ] **Step 1: Compile**

```bash
cd /home/emma/dev/DevicePulse && ./gradlew :app:compileDebugKotlin
```

- [ ] **Step 2: Run ktlint**

```bash
cd /home/emma/dev/DevicePulse && ./gradlew :app:ktlintCheck
```

- [ ] **Step 3: Run unit tests**

```bash
cd /home/emma/dev/DevicePulse && ./gradlew :app:testDebugUnitTest
```

### Task 8: Update follow-up document

**Files:**
- Modify: `docs/reviews/2026-03-13-consistency-follow-up.md`

- [ ] **Step 1: Mark completed items**

Update the document to reflect:
- Item 1 (ViewModel state models): Completed — Network converted to sealed, Settings stays flat (no loading state)
- Item 2 (Modifier ordering): Reviewed — current ordering in SettingsScreen is intentionally correct (`.selectable()/.toggleable()` before `.padding()` makes the full row touchable, matching 48dp min touch targets)
- Item 3 (String naming): Already complete (confirmed by audit)
- Item 4 (detekt): Completed — detekt 2.0.0-alpha.2 added (required for Kotlin 2.3.0)
- Item 5 (Headers): Decided — no headers, not required for Play Store

- [ ] **Step 2: Commit**

```bash
git add docs/reviews/2026-03-13-consistency-follow-up.md
git commit -m "Merkitse consistency-seurantadokumentti valmiiksi"
```

---

## Decisions & Notes

### Modifier Ordering — No Changes Needed

The SettingsScreen patterns flagged in the review are intentionally correct:

```kotlin
.selectable(selected, onClick, role)
.padding(vertical = spacing.sm)
```

In Compose, modifiers apply outside-in. Placing `.selectable()` before `.padding()` means the entire row area (including visual padding) is the touch target. This is the correct Material Design pattern for settings rows and meets the 48dp minimum touch target requirement. Reversing the order would shrink the touchable area.

### Why detekt 2.0.0-alpha.2

Detekt 1.23.8 cannot parse Kotlin 2.3.0 metadata (see [GitHub issue #8865](https://github.com/detekt/detekt/issues/8865)). The 2.0.0-alpha.2 release is built against Kotlin 2.2.20 and handles 2.3.0 metadata correctly. This is an alpha but is the only option for the project's Kotlin version.

### NetworkUiState Split Rationale

The flat `NetworkUiState` mixed two independent concerns:
1. Network connection data lifecycle (loading → success/error)
2. Speed test progress (always available, orthogonal to network data)

Splitting into `networkUiState: StateFlow<NetworkUiState>` (sealed) and `speedTestState: StateFlow<SpeedTestUiState>` (flat) aligns with the sealed interface standard used by all other screens while keeping speed test state independently accessible.
