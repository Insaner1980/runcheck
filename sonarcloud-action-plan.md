# SonarCloud Issue Action Plan

**Date:** 2026-03-27
**Project:** Insaner1980_runcheck
**Open issues:** 108

Issues are categorized by required action. Each issue has the exact file and line from the current SonarCloud scan.

---

## Category A: Mark "Won't Fix" in SonarCloud (44 issues)

These are false positives caused by SonarCloud not understanding Jetpack Compose and Kotlin delegate patterns. No code changes needed — mark each as "Won't Fix" in the SonarCloud UI.

### A1. S6615 — Compose state assignments (33 issues)

SonarCloud flags `showDialog = false` inside `onDismissRequest` as "value never used", but in Compose this triggers recomposition and hides the dialog.

**Won't Fix reason:** *"Compose state assignment triggers recomposition — value is read on next recompose cycle"*

| # | File | Line |
|---|------|------|
| 1 | SettingsScreen.kt | 818 |
| 2 | SettingsScreen.kt | 825 |
| 3 | SettingsScreen.kt | 831 |
| 4 | SettingsScreen.kt | 841 |
| 5 | SettingsScreen.kt | 848 |
| 6 | SettingsScreen.kt | 855 |
| 7 | SettingsScreen.kt | 865 |
| 8 | SettingsScreen.kt | 872 |
| 9 | SettingsScreen.kt | 878 |
| 10 | SettingsScreen.kt | 888 |
| 11 | SettingsScreen.kt | 895 |
| 12 | SettingsScreen.kt | 905 |
| 13 | SettingsScreen.kt | 923 |
| 14 | SettingsScreen.kt | 931 |
| 15 | SettingsScreen.kt | 938 |
| 16 | SettingsScreen.kt | 944 |
| 17 | StorageDetailScreen.kt | 270 |
| 18 | StorageDetailScreen.kt | 289 |
| 19 | StorageDetailScreen.kt | 295 |
| 20 | StorageDetailScreen.kt | 302 |
| 21 | StorageDetailScreen.kt | 336 |
| 22 | StorageDetailScreen.kt | 459 |
| 23 | BatteryDetailScreen.kt | 305 |
| 24 | BatteryDetailScreen.kt | 777 |
| 25 | ChargerComparisonScreen.kt | 158 |
| 26 | ChargerComparisonScreen.kt | 161 |
| 27 | ChargerComparisonScreen.kt | 169 |
| 28 | ChargerComparisonScreen.kt | 172 |
| 29 | NetworkDetailScreen.kt | 883 |
| 30 | NetworkDetailScreen.kt | 1023 |
| 31 | SpeedTestScreen.kt | 262 |
| 32 | ThermalDetailScreen.kt | 214 |
| 33 | ThermalDetailScreen.kt | 361 |

### A2. S1481 — rememberUpdatedState delegates (9 issues)

SonarCloud doesn't follow Kotlin `by` delegates and thinks `val currentX by rememberUpdatedState(x)` is unused, but `currentX` is used inside `LaunchedEffect` or callbacks.

**Won't Fix reason:** *"Kotlin delegate property — value accessed via by keyword, used in LaunchedEffect/callback"*

| # | File | Line |
|---|------|------|
| 1 | AppUsageScreen.kt | 140 |
| 2 | AppUsageScreen.kt | 166 |
| 3 | BatteryDetailScreen.kt | 255 |
| 4 | BatteryDetailScreen.kt | 256 |
| 5 | ChargerComparisonScreen.kt | 134 |
| 6 | NavGraph.kt | 53 |
| 7 | NetworkDetailScreen.kt | 842 |
| 8 | NetworkDetailScreen.kt | 843 |
| 9 | StorageDetailScreen.kt | 327 |

### A3. S6526 — Abstract class should be interface (2 issues)

Hilt `@Module` with `@Binds` requires `abstract class`, not `interface`.

**Won't Fix reason:** *"Hilt @Module with @Binds requires abstract class"*

| # | File | Line |
|---|------|------|
| 1 | SystemBindingsModule.kt | 25 |
| 2 | RepositoryModule.kt | 38 |

---

## Category B: Suppress in SonarCloud Quality Profile (16 issues)

These rules conflict with standard Compose/Kotlin conventions. Suppress the rule in the quality profile or mark individually as "Won't Fix".

### B1. S107 — Too many parameters (14 issues)

Compose composables routinely take 8–13 parameters (modifier, state, callbacks). This is standard in the Compose ecosystem.

**Recommendation:** Suppress `kotlin:S107` in quality profile for `ui/` package, or raise threshold to 13.

| # | File | Line | Params |
|---|------|------|--------|
| 1 | HomeScreen.kt | 131 | 13 |
| 2 | HomeScreen.kt | 268 | 12 |
| 3 | HomeScreen.kt | 1140 | 9 |
| 4 | BatteryDetailScreen.kt | 138 | 11 |
| 5 | BatteryDetailScreen.kt | 227 | 12 |
| 6 | NetworkDetailScreen.kt | 123 | 10 |
| 7 | NetworkDetailScreen.kt | 824 | 12 |
| 8 | StorageDetailScreen.kt | 311 | 11 |
| 9 | TrendChart.kt | 133 | 12 |
| 10 | LiveChart.kt | 61 | 10 |
| 11 | LiveChart.kt | 121 | 8 |
| 12 | GridCard.kt | 32 | 11 |
| 13 | MetricRow.kt | 34 | 8 |
| 14 | ProgressRing.kt | 26 | 8 |

### B2. S6517 — Interface should be functional (3 issues)

Single-method interfaces injected via Hilt DI. Changing to `fun interface` or function type breaks Hilt bindings.

**Won't Fix reason:** *"DI-injected interface — fun interface/function type breaks Hilt @Binds"*

| # | File | Line |
|---|------|------|
| 1 | DeviceProfileProvider.kt | 9 |
| 2 | TrackThrottlingEventsUseCase.kt | 90 |
| 3 | FileExportRepository.kt | 3 |

---

## Category C: Intentional — No Action Needed (9 issues)

Code is correct as-is. Changing it would be worse.

### C1. S108 — Empty catch blocks (3 issues)

Last-resort fallbacks for external intent launches where the target activity may not exist. Adding logging here would spam logs on devices without the setting. Crashing would be wrong.

| # | File | Line | Context |
|---|------|------|---------|
| 1 | HomeScreen.kt | 659 | Nested fallback for battery optimization intent |
| 2 | SettingsScreen.kt | 239 | Same pattern |
| 3 | SettingsScreen.kt | 758 | Same pattern |

### C2. S1871 — Duplicate branches (2 issues)

Explicit handling of known billing response codes + catch-all for future unknown codes. Merging loses documentation value.

| # | File | Line | Context |
|---|------|------|---------|
| 1 | BillingManager.kt | 146 | else same as specific billing response |
| 2 | BillingManager.kt | 215 | else same as non-ready codes |

### C3. S117 — Parameter naming convention (2 issues)

Room DAO query parameters use SQL naming convention (e.g., `start_time`), not Kotlin camelCase.

| # | File | Line |
|---|------|------|
| 1 | AppBatteryUsageDao.kt | 69 |
| 2 | AppBatteryUsageDao.kt | 70 |

### C4. S6511 — Merge if to when (2 issues)

Minimal readability benefit, not worth the churn.

| # | File | Line |
|---|------|------|
| 1 | AppUsageScreen.kt | 191 |
| 2 | SegmentedBar.kt | 123 |

---

## Category D: Quick Fixes (2 issues) — DONE

Small, safe code changes.

### ~~D1. S6519 — Use == instead of .equals() (2 issues) — FALSE POSITIVE~~

Both use `ignoreCase = true` which `==` does not support. Mark Won't Fix.

| # | File | Line | Status |
|---|------|------|--------|
| 1 | SpeedTestService.kt | 325 | `.equals(other, ignoreCase = true)` — can't use `==` |
| 2 | UiFormatters.kt | 95 | `.equals("unknown", ignoreCase = true)` — can't use `==` |

---

## Category E: Refactor — Cognitive Complexity S3776 (36 issues)

Functions exceeding complexity threshold of 15. Sorted by complexity (highest first). Split into priority tiers.

### E1. High Priority — Complexity > 50 (3 issues)

These are genuinely hard to navigate and maintain. Refactor in dedicated sessions.

| # | File | Line | Complexity | Suggested approach |
|---|------|------|------------|-------------------|
| 1 | SettingsScreen.kt | 93 | **222** | Split into section composables (Monitoring, Notifications, Thresholds, Display, Data, Pro, Device, About) |
| 2 | TrendChart.kt | 133 | **181** | Extract rendering phases (grid, sweep animation, data line, tooltip, quality zones) into separate functions |
| 3 | BatteryDetailScreen.kt | 227 | **103** | Extract hero card, info cards, stats card, history chart, session chart into composables |

### E2. Medium Priority — Complexity 25–50 (10 issues)

Worth refactoring if touching the file. Each can be done independently.

| # | File | Line | Complexity |
|---|------|------|------------|
| 4 | SpeedTestService.kt | 61 | 63 |
| 5 | HomeScreen.kt | 268 | 38 |
| 6 | NavGraph.kt | 44 | 34 |
| 7 | NetworkDetailScreen.kt | 824 | 32 |
| 8 | BatteryViewModel.kt | 106 | 29 |
| 9 | CleanupScreen.kt | 69 | 29 |
| 10 | StorageDetailScreen.kt | 127 | 27 |
| 11 | MediaStoreScanner.kt | 259 | 27 |
| 12 | GridCard.kt | 32 | 26 |
| 13 | CleanupViewModel.kt | 208 | 25 |

### E3. Low Priority — Complexity 15–24 (23 issues)

Just over threshold. Fix opportunistically when modifying the file.

| # | File | Line | Complexity |
|---|------|------|------------|
| 14 | StorageDetailScreen.kt | 311 | 24 |
| 15 | NetworkDataSource.kt | 562 | 24 |
| 16 | ThermalDetailScreen.kt | 598 | 24 |
| 17 | SpeedTestScreen.kt | 318 | 24 |
| 18 | LiveChart.kt | 121 | 23 |
| 19 | HealthMonitorWorker.kt | 43 | 22 |
| 20 | ThermalDetailScreen.kt | 195 | 22 |
| 21 | NetworkDataSource.kt | 189 | 21 |
| 22 | ChargerComparisonScreen.kt | 391 | 21 |
| 23 | HomeScreen.kt | 985 | 21 |
| 24 | GetBatteryStatisticsUseCase.kt | 11 | 21 |
| 25 | AppUsageScreen.kt | 160 | 20 |
| 26 | CleanupScreen.kt | 335 | 20 |
| 27 | AreaChart.kt | 31 | 19 |
| 28 | LearnArticleBodyFormatter.kt | 39 | 19 |
| 29 | BatteryDetailScreen.kt | 784 | 18 |
| 30 | MetricRow.kt | 34 | 17 |
| 31 | CleanupViewModel.kt | 85 | 17 |
| 32 | HomeScreen.kt | 131 | 17 |
| 33 | NetworkDetailScreen.kt | 416 | 17 |
| 34 | NetworkViewModel.kt | 111 | 16 |
| 35 | MediaStoreScanner.kt | 45 | 16 |
| 36 | GetChargerComparisonUseCase.kt | 16 | 16 |

---

## Summary by Action

| Category | Action | Issues | Effort |
|----------|--------|--------|--------|
| **A** | Mark "Won't Fix" in SonarCloud UI | 44 | ~15 min clicking |
| **B** | Suppress rule / mark "Won't Fix" | 16 | ~5 min |
| **C** | No action (intentional) | 9 | Mark in UI if desired |
| **D** | Quick code fix | 2 | ~5 min |
| **E1** | Major refactoring | 3 | 1–2 hours each |
| **E2** | Medium refactoring | 10 | 15–30 min each |
| **E3** | Low priority, opportunistic | 23 | 10–15 min each |
| **Total** | | **108** | |

After marking A + B + C in SonarCloud: **108 → 39 visible issues** (2 quick fixes + 36 complexity + 1 overlap).
After quick fixes: **37 visible issues** (all complexity).
After E1 refactoring: **34 visible issues**.

---

## Claude Code Prompts

Copy-paste these into Claude Code one at a time. Each prompt covers all instances of one issue type.

---

### Prompt 1: Empty catch blocks (S108 — 3 issues)

```
Fix all SonarCloud "Either remove or fill this block of code" issues (S108) in the project. These are empty catch blocks. For each one, change the unused _ parameter to e and add an appropriate log statement like Log.w(TAG, "descriptive message", e). The log message should describe what operation failed based on the surrounding context. Add a TAG companion object if the file doesn't already have one. Known locations:
- HomeScreen.kt:659
- SettingsScreen.kt:239
- SettingsScreen.kt:758
Check all files — there might be additional instances.
```

---

### Prompt 2: .equals() → == (S6519 — 2 issues)

```
Fix all SonarCloud "Replace 'equals' with binary operator '=='" issues (S6519) in the project. Replace .equals() calls with the Kotlin == operator. Known locations:
- SpeedTestService.kt:325
- UiFormatters.kt:95
Check all Kotlin files for other .equals() calls that should use == instead.
```

---

### Prompt 3: if → when conversion (S6511 — 2 issues)

```
Fix all SonarCloud "Merge this if statement with the enclosing one" issues (S6511) in the project. Merge nested if statements into a single condition using && or convert to a when expression where it improves readability. Known locations:
- AppUsageScreen.kt:191
- SegmentedBar.kt:123
Do not change the behavior — only restructure the conditionals.
```

---

### Prompt 4: SettingsScreen complexity refactor (S3776 — complexity 222)

```
Refactor SettingsScreen.kt to reduce cognitive complexity (currently 222, threshold is 15). The main composable at line 93 is a single massive function. Split it into separate composable functions for each settings section:
- MonitoringSection
- LiveNotificationSection
- NotificationsSection
- AlertThresholdsSection
- DisplaySection
- DataSection
- ProSection
- DeviceSection
- AboutSection
Each section composable should take only the state and callbacks it needs from the ViewModel. Keep them in the same file. The visible UI must not change at all — same layout, same behavior, same theme. Do not rename or move any existing state variables or ViewModel functions.
```

---

### Prompt 5: TrendChart complexity refactor (S3776 — complexity 181)

```
Refactor TrendChart.kt to reduce cognitive complexity (currently 181, threshold is 15). The main composable at line 133 has all rendering logic in one function. Extract the Canvas drawing phases into separate private functions:
- drawGrid (grid lines and labels)
- drawSweepAnimation (oscilloscope sweep effect and scan line)
- drawDataLine (status gradient line with quality zone colors)
- drawFill (height-proportional fill below the line)
- drawLastValueEmphasis (glow dot + dashed line to Y-axis)
- drawTooltip (tap/drag tooltip overlay)
Each function should take DrawScope (or the relevant Canvas parameters) and the data it needs. Keep everything in the same file. The visual output and all animations must remain identical. Do not change the public API of the TrendChart composable.
```

---

### Prompt 6: BatteryDetailScreen complexity refactor (S3776 — complexity 103)

```
Refactor BatteryDetailScreen.kt to reduce cognitive complexity (currently 103 at line 227). The BatteryDetailContent composable contains all card sections inline. Extract each major section into its own composable:
- BatteryHeroCard (ProgressRing, level, status, mAh)
- BatteryCurrentStatsCard (current, W+mV, avg/min/max)
- BatteryCapacityCard (design capacity, estimated capacity, health)
- BatteryScreenStateCard (screen on/off drain rates, deep sleep)
- BatteryHistorySection (TrendChart with period selector, MetricPills)
- BatterySessionSection (session charts with window selector)
Keep all new composables in the same file. Pass only the needed state slices as parameters. The UI must remain visually identical. Do not touch BatteryViewModel or any other files.
```

---

### Prompt 7: Medium complexity functions (S3776 — 10 issues, complexity 25–63)

```
Reduce cognitive complexity in these functions by extracting helper functions, replacing nested if/else with early returns or when expressions, and simplifying control flow. Do not change any visible behavior. For each file, keep helpers private and in the same file.

Files and target functions:
1. SpeedTestService.kt:61 (complexity 63) — extract phases of the speed test into separate functions
2. HomeScreen.kt:268 (complexity 38) — extract card sections into composables
3. NavGraph.kt:44 (complexity 34) — extract route groups (home, detail screens, settings, etc.) into separate NavGraphBuilder extension functions
4. NetworkDetailScreen.kt:824 (complexity 32) — extract section composables
5. BatteryViewModel.kt:106 (complexity 29) — extract data collection setup into separate private functions
6. CleanupScreen.kt:69 (complexity 29) — extract UI sections into composables
7. StorageDetailScreen.kt:127 (complexity 27) — extract section composables
8. MediaStoreScanner.kt:259 (complexity 27) — extract per-media-type query logic
9. GridCard.kt:32 (complexity 26) — simplify conditional logic, extract status strip rendering
10. CleanupViewModel.kt:208 (complexity 25) — extract file processing steps into private functions

Goal: get each function below complexity 15 if possible, or at least below 20. Use early returns, when expressions, and extracted private functions. Do not change public APIs or visible behavior.
```

---

### Prompt 8: Low complexity functions (S3776 — 23 issues, complexity 15–24)

```
Reduce cognitive complexity in these functions. These are just over the threshold of 15, so small changes like early returns, when expressions, or extracting one helper function should be enough. Do not change visible behavior or public APIs.

Files (complexity in parentheses):
1. StorageDetailScreen.kt:311 (24)
2. NetworkDataSource.kt:562 (24)
3. ThermalDetailScreen.kt:598 (24)
4. SpeedTestScreen.kt:318 (24)
5. LiveChart.kt:121 (23)
6. HealthMonitorWorker.kt:43 (22)
7. ThermalDetailScreen.kt:195 (22)
8. NetworkDataSource.kt:189 (21)
9. ChargerComparisonScreen.kt:391 (21)
10. HomeScreen.kt:985 (21)
11. GetBatteryStatisticsUseCase.kt:11 (21)
12. AppUsageScreen.kt:160 (20)
13. CleanupScreen.kt:335 (20)
14. AreaChart.kt:31 (19)
15. LearnArticleBodyFormatter.kt:39 (19)
16. BatteryDetailScreen.kt:784 (18)
17. MetricRow.kt:34 (17)
18. CleanupViewModel.kt:85 (17)
19. HomeScreen.kt:131 (17)
20. NetworkDetailScreen.kt:416 (17)
21. NetworkViewModel.kt:111 (16)
22. MediaStoreScanner.kt:45 (16)
23. GetChargerComparisonUseCase.kt:16 (16)

Typical fixes: replace nested if/else with early return or when, extract a small private helper for repeated logic, flatten boolean conditions. Keep changes minimal — just enough to get under 15.
```

---

### SonarCloud UI: Bulk "Won't Fix" (categories A + B + C — 69 issues)

These cannot be fixed in code. Open SonarCloud web UI and bulk-resolve:

1. Go to **Issues** → filter by rule `S6615` → select all 33 → **Bulk Change** → Won't Fix → reason: *"Compose state assignment triggers recomposition"*
2. Filter by rule `S1481` → select all 9 → Won't Fix → reason: *"Kotlin delegate property used via by keyword"*
3. Filter by rule `S107` → select all 14 → Won't Fix → reason: *"Standard Compose composable parameter count"*
4. Filter by rule `S6526` → select all 2 → Won't Fix → reason: *"Hilt @Module requires abstract class"*
5. Filter by rule `S6517` → select all 3 → Won't Fix → reason: *"DI-injected interface, fun interface breaks Hilt"*
6. Filter by rule `S1871` → select all 2 → Won't Fix → reason: *"Explicit billing response handling for documentation"*
7. Filter by rule `S117` → select all 2 → Won't Fix → reason: *"Room DAO SQL parameter naming convention"*

Alternatively, suppress S6615, S1481, S107 at quality profile level to prevent future false positives on new Compose code.
