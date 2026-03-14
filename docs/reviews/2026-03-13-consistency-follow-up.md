# runcheck Consistency Follow-Up

Date: 2026-03-13

This file tracks consistency and code-style work that is still worth doing after the latest cleanup pass.

## All items completed

- Added repo-wide `.editorconfig`
- Added `ktlint` Gradle integration and verified `ktlintCheck`
- Updated project documentation in `CODEX.md` to reflect actual package structure and style rules
- Unified the `ui/network` feature to a single screen `UiState`
- Normalized several common and monitoring string resource keys
- Restored `values-fi` string parity with the base `values` file
- Cleaned a small set of obvious `Modifier` ordering inconsistencies
- Standardized ViewModel state models (item 1) — sealed interface is the standard; Network converted, Settings stays flat (no loading state)
- Reviewed Modifier ordering (item 2) — SettingsScreen ordering is intentionally correct (`.selectable()`/`.toggleable()` before `.padding()` = full-row touch targets, matching 48dp minimum)
- String resource naming (item 3) — audit confirmed all keys already follow feature-prefix convention
- Added detekt 2.0.0-alpha.2 (item 4) — required for Kotlin 2.3.0 compatibility; `ignoreFailures = true` during adoption, 66 pre-existing findings to address incrementally
- No source file headers (item 5) — decided: not required for Google Play Store, avoids maintenance overhead

## Practical rule for future work

When editing any feature:
- keep imports explicit
- keep string keys scoped by feature
- keep screen state as sealed `Loading / Success / Error` (except Settings which uses flat data class)
- keep `values-fi` in sync with `values`
- run `./gradlew :app:compileDebugKotlin`
- run `./gradlew :app:ktlintCheck`
- run `./gradlew :app:detekt` (findings are warnings, not blockers)
