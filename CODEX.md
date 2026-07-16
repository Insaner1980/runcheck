runcheck Codex Instructions

## Scope

These instructions are for agents working in this repository. Keep this file aligned with `AGENTS.md`. If they ever conflict, resolve the mismatch instead of following two different rule sets.

runcheck is an Android device health diagnostics app built with Kotlin and Jetpack Compose. Product direction is fixed:

- App name is `runcheck` in lowercase
- Single dark theme only
- One-time Pro purchase only
- No subscriptions
- No ads as a product direction
- NDT7 is the speed test backend
- Minimum SDK stays 26

When product/runtime facts or the visual system matter, use `PROJECT.md` and `UI-SPEC.md` as the authoritative companion docs and keep them aligned with code.

Legacy billing or ad-related code may still exist in the repo. Do not expand that surface unless the task is explicitly about cleanup or migration.

## Instruction Hierarchy

- Direct user instructions in the current task override repository docs.
- `AGENTS.md` and `CODEX.md` are the primary repository instruction files for agents. Keep overlapping rules in sync; if they conflict, fix the mismatch in both files instead of following divergent rule sets.
- `PROJECT.md` is the current-state product, runtime, build, and report-reading source of truth; `UI-SPEC.md` is the visual-system companion.
- Executable workflow behavior comes from `.github/workflows/`, `tools/`, `scripts/`, and the delegated Android-check wrapper source resolved by `tools\Invoke-RuncheckProjectCheck.ps1`. Documentation should describe those files, not override them.

---

## Current Project Snapshot

- Package root: `com.runcheck`
- Main module: single `app` module
- Architecture: Clean Architecture with `data/`, `domain/`, and `ui/`
- Dependency injection: Hilt
- Coroutine dispatchers: centralized through `AppDispatchers`; production code should not call coroutine builders with raw `Dispatchers.*`
- Database: Room
- Preferences: DataStore
- UI: Jetpack Compose + Material 3
- Background work: WorkManager
- Widgets: Glance
- Speed test: M-Lab NDT7 (`ndt7-client-android`)
- Build: Gradle Kotlin DSL
- Compile SDK: Android 17 (API 37)
- Target SDK: Android 17 (API 37)
- Min SDK: 26
- Java target: 17
- Localization: English-only (`localeFilters = ["en"]`)
- Build variants: `app/src/debug` and `app/src/release` source sets are active

High-level package layout:

```text
app/src/main/java/com/runcheck/
├── data/
├── domain/
├── ui/
├── billing/
├── pro/
├── di/
├── service/
├── worker/
├── widget/
└── util/
```

Debug-only insight tooling also lives outside the main source tree:

- `app/src/debug/java/com/runcheck/debug/insights/` for debug implementations
- `app/src/main/java/com/runcheck/debug/insights/` for release-safe stubs

Current navigation snapshot:

```text
Home
├── Insights
├── Battery Detail
│   ├── Charger Comparison [PRO]
│   └── Fullscreen Chart
├── Network Detail
│   ├── Speed Test
│   └── Fullscreen Chart
├── Thermal Detail
├── Storage Detail
│   └── Cleanup/{type}
├── App Usage [PRO]
├── Learn
│   └── Learn Article
├── Settings
└── Pro Upgrade
```

Current runtime systems:

- `RuncheckApp` initializes billing, Pro state, notification channels, screen-state tracking, periodic monitoring, and widget refresh hooks
- `RuncheckApp` also initializes source-set-specific `SentryInit`; debug builds may report to Sentry through `sentry-android-core` only when `RUNCHECK_SENTRY_DSN`, `SENTRY_DSN`, or ignored `debug.credentials.properties` provides `sentry.dsn`; release builds are a no-op and must remain telemetry-free
- WorkManager runs `HealthMonitorWorker` for snapshot collection + alert evaluation
- WorkManager runs `HealthMaintenanceWorker` for app-usage refresh, cleanup, and widget refresh
- WorkManager runs `InsightGenerationWorker` on the monitoring scheduler lifecycle to generate persisted Home insights from Room history; rule evaluation completes before all generated rule results are replaced in one Room transaction
- `RealTimeMonitorService` is an opt-in live notification foreground service and must stay user-controlled from Settings
- Widgets are backed by Room snapshots and treated as a Pro feature
- Trial state currently counts as Pro access through `ProState.isPro`
- Home now includes a rule-driven Insights surface backed by Room-persisted insight rows; Home shows a curated subset of up to three items and the full list lives in the dedicated Insights screen
- `AppBatteryImpactRule` is intentionally excluded from production because foreground duration alone cannot support defensible per-app mAh attribution
- Debug-only insight seeding and manual regeneration live behind debug source-set wiring and must stay release-inaccessible

State restoration conventions:

- Use `rememberSaveable` for screen-local UI state such as sheet visibility, dialogs, and metric chip selections
- Use `SavedStateHandle` for route-backed or process-death-sensitive state such as selected history period, cleanup type, and fullscreen chart args

## Local Check Tooling

PowerShell wrappers live in `tools/` and forward through `tools\Invoke-RuncheckProjectCheck.ps1`. The helper resolves the shared Android-check repository from `ANDROID_CHECK_ROOT` first, then from a sibling `Android-check` checkout next to `runcheck`.

- `lc` / `tools\lc.ps1` — ktlint, detekt, Android lint; writes `reports\ktlint.txt`, `reports\detekt.txt`, and `reports\lint.txt`
- `ac` / `tools\ac.ps1` — Android security surface; project Semgrep, mobsfscan, and DeepSec custom report
- `dc` / `tools\dc.ps1` — dependency verification, OSV, OWASP Dependency-Check; use `dc -InitVerification` only when intentionally creating or updating `gradle\verification-metadata.xml`
- `ss` / `tools\ss.ps1` — gitleaks, TruffleHog, Semgrep secrets
- `ds` / `tools\ds.ps1` — DeepSec custom scan/report/revalidate paths
- `ms` / `tools\ms.ps1` — mobsfscan
- `os` / `tools\os.ps1` — OSV Scanner
- `ql` / `tools\ql.ps1` — CodeQL workflow/status check through GitHub tooling
- `db` / `tools\db.ps1` — Dependabot config and alert check
- `pc` / `tools\pc.ps1` — PMD CPD duplicate scan; runcheckin oletuskynnys on 100 tokenia, ja sen voi ohittaa `PMD_CPD_MINIMUM_TOKENS`-ympäristömuuttujalla
- `cs` / `tools\cs.ps1` — Compose Stability Analyzer (`:app:stabilityCheck`)
- `cr` / `tools\cr.ps1` — compose-rules through ktlint and detekt
- `ga` / `tools\ga.ps1` — Google Android Security Lints through Android lint
- `sc` / `tools\sc.ps1` — combined security check; `-Full` also runs Android security checks
- `sentry` / `tools\sentry.ps1` — verifies debug-only Sentry wiring; debug must contain `io.sentry`, release must not contain `io.sentry`, and results are written to `reports\sentry.txt`
- `tools\sonar.ps1` — SonarCloud path; requires `SONAR_TOKEN`, runs `assembleDebug`, `:app:jacocoDebugUnitTestReport`, prepares an empty Android Lint import placeholder because `lc` owns real lint findings, and runs `sonar`, then writes `reports\sonar.txt`

`scripts\security-check.ps1` is only a compatibility wrapper to `tools\sc.ps1`. No Linux shell security wrapper is maintained in this Windows-first repo. `reports/` is ignored and must not be committed.

Report-reading phrase conventions live in `PROJECT.md` under "Report-reading convention"; use that list when the user says "lue lint-tulokset" or "lue security-tulokset" instead of inferring a shorter report list from wrapper summaries.

When `osv-scanner`, gitleaks, TruffleHog, or PMD are missing from `PATH`, the shared Android-check wrappers may download and cache verified tool binaries under `.gradle\android-check-tools\`; offline first runs can therefore skip or fail before a cached tool exists. The OSV source scan excludes `.deepsec` so Android-check's own DeepSec tooling dependencies do not fail app dependency scans.

Do not run the heavy `lc`, `sc`, Sonar, Dependency-Check, MobSF, DeepSec, or full Gradle verification paths unless the user explicitly asks or they are required to unblock the task. Prefer `-PlanOnly`, task listing, targeted config checks, and narrow tests first.

Project-specific check configuration lives in:

- `config\semgrep\runcheck-security.yml`
- `config\dependency-check\suppressions.xml`
- `.mobsf`
- `.deepsec\`
- `.github\dependabot.yml`

Compose-rules versions are intentionally split while the project stays on Detekt 1.23.8: ktlint uses the current 0.5.x line with ktlint explicitly pinned to the compatible 1.8.x rule engine, and Detekt uses the latest 0.4.x line compatible with Detekt 1.x. Do not move Detekt compose-rules to 0.5.x without a Detekt 2.x migration.

## Architecture Rules

- `data/` owns Android framework access, persistence, device APIs, and external SDK integration
- `domain/` contains business logic, use cases, repository contracts, and domain models
- `domain/` must not import `android.*` or concrete `data/` implementations
- Keep `androidx.*` out of `domain/` unless there is a documented boundary exception such as `androidx.paging.PagingData`
- `ui/` contains Compose screens, components, navigation, and ViewModels
- `ui/` must not bypass use cases or repository contracts to talk directly to data sources
- ViewModels are the bridge between `ui/` and `domain/`

Prefer targeted changes that preserve the existing structure instead of cross-layer rewrites.

## Review Priorities

When reviewing or modifying code, check these first and in this order.

### 1. Layer boundaries

- Flag any `domain/` dependency on Android or `data/`
- Flag any `ui/` dependency that bypasses the ViewModel/use-case path
- Keep business logic out of composables

### 2. Measurement reliability

- Every sensor-facing value must use `MeasuredValue<T>`
- Confidence must be `ACCURATE`, `ESTIMATED`, or `UNAVAILABLE`
- Raw values must not be shown without a confidence indicator such as `ConfidenceBadge`
- Validate `BATTERY_PROPERTY_CURRENT_NOW` with repeated reads, non-zero checks, plausible range `-10000..+10000 mA`, and charge-state sign sanity
- Thermal data must use `PowerManager.getCurrentThermalStatus()` on API 29+ and `getThermalHeadroom()` on API 30+
- Do not add sysfs-based thermal reads

### 3. API level guards

- Guard API 29+ calls with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q`
- Guard API 30+ calls with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.R`
- Guard API 34+ calls with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE`
- Min SDK is 26, so anything above that needs an explicit guard or safe fallback

### 4. Pro gating

Pro features are:

- Charger Comparison
- Per-App Battery
- Extended History
- Thermal Logs
- CSV Export
- Widgets

Rules:

- Check `ProManager.isPro()` or the injected `ProStatusProvider` / `IsProUserUseCase` path before exposing the feature
- Use `ProFeatureLockedState` for locked UI, not a custom replacement
- Preserve the one-time purchase model
- The top-level Home Insights card is not a Pro feature. It may link into Pro-gated destinations, but the destinations themselves must remain gated.

### 5. Speed test

- Use M-Lab NDT7 only
- Do not hardcode a server; NDT7 chooses the nearest server
- Show the cellular warning before the test starts when the active network is not Wi-Fi
- Outbound network calls are allowed only for user-initiated speed tests, latency measurement, and Google Play Billing
- Reading current connection details such as Wi-Fi, 5G, SSID, signal, IP, or DNS must stay on-device via Android APIs and must not trigger socket, HTTP, or ping-style probes

### 6. Motion and animation

- All animations must respect `LocalReducedMotion.current` or `MaterialTheme.reducedMotion`
- Skip or shorten motion when reduced motion is enabled
- Standard durations:
  - `ProgressRing`: 1200ms
  - `MiniBar`: 800ms
  - `SegmentedBar`: 800ms
  - `Thermometer`: 1200ms
  - Battery wave: 2000ms loop

### 7. UI consistency

- Background colors:
  - `BgPage` `#0B1E24`
  - `BgCard` `#133040`
  - `BgCardAlt` `#0F2A35`
  - `BgIconCircle` `#1A3A48`
- Primary accent: blue `#4A9EDE`
- Secondary/status accent: teal `#5DE4C7`
- Gauge arcs stay neutral white/gray; accent color is only for the indicator
- Status colors are for small badges and dots, not large fills
- Typography:
  - Manrope for body text
  - JetBrains Mono for hero numbers and gauge values
- Card radius: 16dp
- Small-element radius: 8dp
- No shadows, no elevation, no borders, except `ActionCards` with `1dp outlineVariant` at `35%` alpha
- No dynamic colors. If a task changes visual design, follow `UI-SPEC.md` instead of inventing alternate tokens or component variants
- English-only strings are intentional right now. Do not reintroduce partial localization without updating docs and string coverage together.
- Icons: use `Icons.Outlined` exclusively — no `Icons.Default`, `Icons.Filled`, or `Icons.Rounded`
- All padding/spacing values must be on the 4dp grid (2/4/8/12/16/24/32dp)
- Shared touch targets, icon sizes, icon circles, and common CTA heights should come from `UiTokens` instead of being repeated in shared components
- All animation durations must use `MotionTokens` constants, never bare `tween()` without explicit spec
- All ViewModels with live state flows must use `.sample(333L)` to throttle UI updates

### 8. Accessibility

- Minimum touch target: 48dp
- Charts, rings, bars, and similar visuals need content descriptions
- Status must never rely on color alone; pair it with text or icon

## What To Flag

Raise a review comment or fix request for any of these:

- Layer boundary violations
- Missing API guards
- Sensor data shown without `MeasuredValue` or `ConfidenceBadge`
- Pro content exposed without `isPro()` gating
- Animation ignoring reduced motion
- Hardcoded colors that do not match the palette
- Touch targets smaller than 48dp
- Sysfs-based thermal reads
- NDT7 speed tests pinned to a fixed server
- Any outbound network call outside the speed test flow, latency measurement, or billing
- Any release-path telemetry, crash reporting, or analytics expansion beyond the current debug-only Sentry setup

## Working Conventions

- Prefer explicit imports.
- Avoid wildcard imports.
- Keep code comments in English.
- Avoid `!!`.
- Put user-facing strings in resources.
- Keep composables small and focused.
- Keep ViewModel state explicit and testable.
- Prefer minimal, targeted edits over broad rewrites.

## Practical Build Notes

- Kotlin version comes from `gradle/libs.versions.toml`
- Compose uses the BOM defined in the version catalog
- Hilt, Room, KSP, ktlint, and detekt are already wired into the build
- No release-path crash reporting, analytics, replay, tracing, NDK symbol capture, or tracking — do not add telemetry beyond the current debug-only `sentry-android-core` setup

## Preferred Local Skills

If local Codex skills are installed, prefer:

- `runcheck-deep-review` for deep reviews, large change audits, LLM-generated Android code audits, and subtle API/lifecycle regression checks
- `runcheck-security-scan` for manifest, permission, exported-component, logging, secrets, and release-safety audits

## Low-CPU Verification

- This repository is often worked on with limited CPU headroom. Avoid heavy local verification by default.
- Do not run full Gradle builds or full test suites unless explicitly requested or required to unblock the task.
- Prefer static analysis, targeted file review, and minimal commands first.
- If verification is needed, use the smallest scoped check possible: one compile task, one module task, or one narrowly filtered test class.
- Avoid running multiple coding agents or tools that may build the same repo in parallel.
- When verification is intentionally skipped or minimized, say so clearly in the final response.

Useful command references:

- Prefer the narrow examples in `PROJECT.md` under "Useful narrow commands".
- Run broad Gradle tasks only when explicitly requested or required to unblock the task, and say why in the final response.
