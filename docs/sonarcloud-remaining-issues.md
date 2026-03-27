# SonarCloud — Remaining Issues Analysis

**Date:** 2026-03-27
**Project:** Insaner1980_runcheck
**Total issues:** 119 → 11 fixed, 32 false positive, 76 remaining

## Fixed (11 issues)

| Rule | File | Fix |
|------|------|-----|
| S1481 | SegmentedStatusBar.kt:48 | Removed unused `reducedMotion` + import |
| S1481 | NavGraph.kt:165 | Removed unused `resultSource` StateFlow collection |
| S6615 | HealthMaintenanceWorker.kt:24 | Combined declaration with first assignment |
| S6615 | HealthMonitorWorker.kt:44 | Moved `var coreFailure` to first assignment |
| S1192 | MediaStoreScanner.kt:284 | Extracted `APK_MIME_TYPE` constant |
| S1192 | AppUsageViewModel.kt:92 | Extracted `UNKNOWN_ERROR` constant (3 uses) |
| S1192 | ChargerViewModel.kt:67 | Extracted `UNKNOWN_ERROR` constant (6 uses) |
| S1871 | FullscreenChartScreen.kt:456 | Merged BATTERY_HISTORY + NETWORK_HISTORY branches |
| S1871 | NetworkDetailScreen.kt:389 | Merged CELLULAR + VPN branches |
| S1871 | WidgetDataProvider.kt:219 | Merged CELLULAR + VPN branches |
| S1172 | GridCard.kt:38 | Wired `subtitleColor` parameter to subtitle Text |

## False Positives (32 issues)

SonarCloud does not understand Jetpack Compose patterns:

### S6615 — Compose state assignments (24 issues)

`var showDialog by remember { mutableStateOf(false) }` followed by `showDialog = false` inside onClick/onDismissRequest lambdas. SonarCloud thinks the assigned value is "never used" but in Compose, setting state triggers recomposition — the `if (showDialog)` conditional re-evaluates and hides the dialog.

**Files affected:** SettingsScreen.kt (13), StorageDetailScreen.kt (5), BatteryDetailScreen.kt (1), ChargerComparisonScreen.kt (2), NetworkDetailScreen.kt (1), SpeedTestScreen.kt (1), ThermalDetailScreen.kt (1)

**Recommended action:** Mark as "Won't Fix" in SonarCloud UI with reason: "Compose state assignment triggers recomposition"

### S1481 — rememberUpdatedState delegates (8 issues)

`val currentOnCallback by rememberUpdatedState(onCallback)` creates a Kotlin delegate property. SonarCloud doesn't follow the `by` delegate and thinks the variable is unused, but `currentOnCallback` is used later (e.g., inside `LaunchedEffect`).

**Files affected:** AppUsageScreen.kt (2), BatteryDetailScreen.kt (2), ChargerComparisonScreen.kt (1), NavGraph.kt (1), NetworkDetailScreen.kt (2)

**Recommended action:** Mark as "Won't Fix" in SonarCloud UI with reason: "Kotlin delegate property used via by keyword"

## Remaining — Not Safe to Auto-Fix (76 issues)

### S3776 Cognitive Complexity (33 issues) — Requires refactoring

Functions exceeding the complexity threshold of 15. These need manual decomposition into smaller composables/functions. Largest offenders:

| File | Complexity | Priority |
|------|-----------|----------|
| SettingsScreen.kt:93 | 222 | High — split into section composables |
| TrendChart.kt:133 | 181 | High — extract rendering phases |
| BatteryDetailScreen.kt:227 | 103 | High — extract card sections |
| SpeedTestService.kt:61 | 63 | Medium — extract protocol handling |
| NavGraph.kt:44 | 34 | Medium — extract screen declarations |
| NetworkDetailScreen.kt:826 | 32 | Medium |
| BatteryViewModel.kt:106 | 29 | Low |
| CleanupScreen.kt:69 | 29 | Low |
| StorageDetailScreen.kt:127 | 27 | Low |
| StorageDetailScreen.kt:311 | 24 | Low |
| NetworkDataSource.kt:562 | 24 | Low |
| LiveChart.kt:121 | 23 | Low |
| HealthMonitorWorker.kt:43 | 22 | Low |
| NetworkDataSource.kt:189 | 21 | Low |
| AreaChart.kt:31 | 19 | Low |
| BatteryDetailScreen.kt:784 | 18 | Low |
| CleanupViewModel.kt:85 | 17 | Low |
| NetworkViewModel.kt:111 | 16 | Low |

### S107 Too Many Parameters (13 issues) — Compose pattern, low priority

Compose composables commonly have many parameters (modifier, callbacks, state). Standard in the ecosystem. Options:
- Accept as Compose convention
- Group related params into data classes (bigger refactor)
- Suppress in SonarCloud quality profile

### S108 Empty Catch Blocks (2 issues) — Intentional

- HomeScreen.kt:659 — nested fallback for `startActivity` (battery optimization settings)
- SettingsScreen.kt:239 — same pattern

Both are last-resort catches for external intent launches where no handler may exist. Crashing would be wrong.

### S1871 Duplicate Branches — Billing (2 issues) — Intentional

- BillingManager.kt:146 — `else` same as specific billing response codes
- BillingManager.kt:215 — `else` same as non-ready codes

Intentional defensive coding: explicit handling of known codes + catch-all for unknown future response codes. Merging would lose the documentation value and risk missing new codes.

### S117 Parameter Naming (2 issues) — Room convention

- AppBatteryUsageDao.kt:69-70 — Room DAO query parameters follow SQL naming convention, not Kotlin convention.

### S6526 Abstract Class → Interface (1 issue) — Hilt requirement

- SystemBindingsModule.kt:25 — Hilt `@Module` with `@Binds` requires `abstract class`, not `interface`.

### S6517 Functional Interface (2 issues) — DI interfaces

- DeviceProfileProvider.kt:9 — injected via Hilt, changing to `fun interface` or function type would break DI bindings
- TrackThrottlingEventsUseCase.kt:90 — same

### S6511 Merge if→when (2 issues) — Low priority

- AppUsageScreen.kt:191 — chained `if` could become `when`. Minimal benefit.
- SegmentedBar.kt:123 — same.

## Recommendations

1. **False positives (32):** Configure SonarCloud quality profile to exclude Compose-specific rules or mark bulk "Won't Fix"
2. **Complexity (top 3):** Refactor SettingsScreen, TrendChart, BatteryDetailScreen in dedicated sessions
3. **Parameters (13):** Accept as Compose convention, suppress in quality profile
4. **Rest (8):** Intentional patterns, no action needed
