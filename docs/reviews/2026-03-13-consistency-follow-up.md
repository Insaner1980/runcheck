# runcheck Consistency Follow-Up

> Historiallinen follow-up 2026-03-13. Build-työkalujen versiot ja `ignoreFailures`-oletukset eivät ole nykyinen totuus; tarkista nykytila `gradle/libs.versions.toml`:sta ja Gradle-konfiguraatiosta.

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
- Historical record: detekt 2.0.0-alpha.2 was added during this pass; current Detekt, compose-rules, and failure behavior must be checked from the live Gradle configuration.
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
