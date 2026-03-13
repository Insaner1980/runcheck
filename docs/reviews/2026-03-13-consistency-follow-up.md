# DevicePulse Consistency Follow-Up

Date: 2026-03-13

This file tracks consistency and code-style work that is still worth doing after the latest cleanup pass.

## Already completed

- Added repo-wide `.editorconfig`
- Added `ktlint` Gradle integration and verified `ktlintCheck`
- Updated project documentation in `CODEX.md` to reflect actual package structure and style rules
- Unified the `ui/network` feature to a single screen `UiState`
- Normalized several common and monitoring string resource keys
- Restored `values-fi` string parity with the base `values` file
- Cleaned a small set of obvious `Modifier` ordering inconsistencies

## Recommended next work

### 1. Standardize remaining ViewModel state models

Current situation:
- `SettingsViewModel` uses a single immutable `SettingsUiState`
- `NetworkViewModel` now also uses a single immutable `NetworkUiState`
- Several other screens still use sealed `Loading / Success / Error` state types

Still to do:
- Pick one long-term standard for screen state
- Prefer applying it to touched features gradually, not in one large repo-wide refactor

Suggested order:
1. `ui/home`
2. `ui/battery`
3. `ui/thermal`
4. `ui/storage`
5. `ui/appusage`
6. `ui/charger`

Reason:
- These are safe to migrate one feature at a time when that feature is otherwise being edited
- Doing them all at once would create unnecessary regression risk

### 2. Continue `Modifier` ordering cleanup incrementally

Target default ordering:
- semantics/testing
- layout
- drawing
- interaction
- graphics/animation

Still to do:
- Review large Compose files and normalize only while already touching them
- Avoid pure style-only sweeping changes across the whole repo unless there is a dedicated cleanup pass

Likely follow-up candidates:
- `app/src/main/java/com/devicepulse/ui/battery/BatteryDetailScreen.kt`
- `app/src/main/java/com/devicepulse/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/devicepulse/ui/network/SpeedTestScreen.kt`
- `app/src/main/java/com/devicepulse/ui/network/NetworkDetailScreen.kt`

### 3. Tighten string resource naming over time

Current rule of thumb:
- `common_*` for shared generic strings
- feature prefixes like `home_*`, `battery_*`, `network_*`, `settings_*`
- `widget_*` for widget-only strings
- `notification_*` and `monitor_*` for notification/service strings

Still to do:
- Rename remaining generic or mixed-prefix keys only when touching those areas
- Keep Finnish translations in sync whenever new strings are added

Safe approach:
- Do not do a giant rename unless there is strong benefit
- Rename locally when editing the related feature

### 4. Consider adding `detekt` later

What exists now:
- Formatting and basic style drift are covered by `.editorconfig` + `ktlint`

Optional next step:
- Add `detekt` if you want stronger static-analysis rules for complexity, unused code, and maintainability

Recommendation:
- Not urgent
- Only add it when you are ready to tune rules and fix initial findings

### 5. Decide on source file header policy

Current state:
- Kotlin source files do not use copyright/license headers

Decision still needed:
- Either keep “no headers” as the project rule
- Or introduce a required header format and enforce it consistently

Recommendation:
- Leave as-is unless there is a legal or organizational requirement

## Practical rule for future work

When editing any feature:
- keep imports explicit
- keep string keys scoped by feature
- keep screen state in one top-level `uiState` where practical
- keep `values-fi` in sync with `values`
- run `./gradlew :app:compileDebugKotlin`
- run `./gradlew :app:ktlintCheck`

## Suggested prompt for a future chat

Use this file as context and ask for one bounded cleanup at a time.

Example:

`Read docs/reviews/2026-03-13-consistency-follow-up.md and do item 1 for ui/home only.`

or

`Read docs/reviews/2026-03-13-consistency-follow-up.md and clean up Modifier ordering in SettingsScreen only.`
