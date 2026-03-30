# Safe Refactor Follow-ups

This file captures the next low-risk cleanup steps after consolidating enum parsing and enum-backed saved state across screens and ViewModels.

## Goal

Reduce UI fragmentation and repeated state-handling code without changing product behavior, layer boundaries, Pro gating, runtime permissions, or data collection behavior.

## Constraints

- No broad rewrites.
- No cross-layer moves that blur `data/`, `domain/`, and `ui/`.
- No changes to speed test behavior, Pro logic, worker scheduling, or measurement reliability.
- Prefer extraction and consolidation over redesign.
- Verify with the smallest useful check, not a full build/test sweep.

## Next Safe Targets

### 1. Extract shared history filter and fullscreen sync handling

Problem:
- Battery, network, thermal, and storage detail screens still each manage similar "selected metric / selected period / fullscreen return sync" logic locally.

Primary files:
- `app/src/main/java/com/runcheck/ui/battery/BatteryDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/thermal/ThermalDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/storage/StorageDetailScreen.kt`

Safe direction:
- Introduce a small reusable UI helper for chart selection state and fullscreen-return application.
- Keep screen-specific metric enums and chart builders local.
- Do not move chart business logic into composables that don't already own it.

### 2. Centralize info bottom sheet state handling

Problem:
- Multiple detail screens repeat the same `activeInfoSheet` state, lookup, and dismiss flow.

Primary files:
- `app/src/main/java/com/runcheck/ui/battery/BatteryDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/thermal/ThermalDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/storage/StorageDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/runcheck/ui/network/SpeedTestScreen.kt`

Safe direction:
- Add a narrow shared host/helper for info sheet presentation.
- Keep each screen's actual content resolver local unless the catalogs are truly identical.
- Do not merge unrelated info catalogs just to remove lines.

### 3. Split oversized detail screens into section-level composables

Problem:
- Several screens are very large and hard to reason about safely.

Highest-value candidates:
- `app/src/main/java/com/runcheck/ui/battery/BatteryDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/home/HomeScreen.kt`
- `app/src/main/java/com/runcheck/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/runcheck/ui/storage/StorageDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/thermal/ThermalDetailScreen.kt`

Safe direction:
- Extract section composables only.
- Preserve the current ViewModel API and navigation arguments.
- Avoid mixing visual decomposition with state model changes in the same step.

### 4. Unify repeated SavedStateHandle patterns beyond history period enums

Problem:
- Some route-backed state is still hand-parsed or manually sanitized in multiple places.

Primary files:
- `app/src/main/java/com/runcheck/ui/fullscreen/FullscreenChartViewModel.kt`
- `app/src/main/java/com/runcheck/ui/storage/cleanup/CleanupViewModel.kt`
- `app/src/main/java/com/runcheck/ui/home/HomeViewModel.kt`

Safe direction:
- Continue using small typed helpers.
- Prefer explicit defaults over nullable propagation.
- Keep argument sanitization close to the route model when behavior differs by screen.

## Order of Work

Recommended sequence:

1. Shared history/fullscreen selection helper
2. Shared info-sheet host
3. Section-level screen splits
4. Remaining SavedStateHandle cleanup

## Out of Scope for This Track

- Theme redesign
- Navigation graph redesign
- Domain/data layer reshaping
- Billing or Pro model changes
- Storage cleanup flow redesign
- Worker/service lifecycle changes
- Any release telemetry changes
