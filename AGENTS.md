# AGENTS.md — runcheck

This file and `CODEX.md` should stay aligned. If one is updated, update the other in the same change.

Android device health diagnostics app. Kotlin + Jetpack Compose. System, light, and dark themes.

When product/runtime facts or visual system rules matter, treat `PROJECT.md` and `UI-SPEC.md` as the authoritative companion docs and keep them aligned with code.

## Instruction Hierarchy

- Direct user instructions in the current task override repository docs.
- `AGENTS.md` and `CODEX.md` are the primary repository instruction files for agents. Keep overlapping rules in sync; if they conflict, fix the mismatch in both files instead of following divergent rule sets.
- `PROJECT.md` is the current-state product, runtime, build, and report-reading source of truth; `UI-SPEC.md` is the visual-system companion.
- Executable workflow behavior comes from `.github/workflows/`, `tools/`, `scripts/`, and the delegated Android-check wrapper source resolved by `tools\Invoke-RuncheckProjectCheck.ps1`. Documentation should describe those files, not override them.

---

## Architecture

Clean Architecture with three layers:

- `data/` — Android framework APIs, Room database, BatteryManager, TelephonyManager, StorageStatsManager, PowerManager
- `domain/` — Business logic, use cases, domain models. No `android.*` imports; keep `androidx.*` out unless there is an explicitly documented boundary exception such as `androidx.paging.PagingData`
- `ui/` — Jetpack Compose screens and components. No direct data layer access.

Dependency injection: Hilt. Database: Room. UI: Jetpack Compose + Material 3.

---

## Current Project Snapshot

- Package root: `com.runcheck`
- Main module: single `app` module
- Architecture: Clean Architecture with `data/`, `domain/`, and `ui/`
- Dependency injection: Hilt
- Coroutine dispatchers: centralized through `AppDispatchers`; production code should not call coroutine builders with raw `Dispatchers.*`
- Database: Room
- Preferences: DataStore
- UI: Jetpack Compose + stable Material 3; Compose BOM `2026.06.01` is the single library-version source and currently resolves Material 3 `1.4.0`
- Theme: persisted `SYSTEM`, `LIGHT`, and `DARK` modes
- Background work: WorkManager
- Widgets: Glance; Battery, Health Score, and responsive Quick Glance (2×2 compact, 3×2 standard, 4×2 expanded)
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
Home [top level]
├── Battery Detail
│   └── Fullscreen Chart
├── Network Detail
│   └── Fullscreen Chart
├── Thermal Detail
└── Storage Detail
Insights [top level]
└── Weekly Report [PRO]
Tools [top level]
├── Speed Test
├── Storage Detail
│   └── Cleanup/{type}
├── Charger Comparison [PRO]
├── App Usage [PRO]
├── Learn
│   └── Learn Article
└── Export [PRO]
Settings [top level]
└── Pro Upgrade
```

Current runtime systems:

- `RuncheckApp` initializes billing, Pro state, notification channels, screen-state tracking, periodic monitoring, and widget refresh hooks
- `MainActivity` uses one theme-neutral AndroidX starting splash for day/night resources, keeps it visible until DataStore emits the first theme preference and the matching edge-to-edge system-bar appearance is applied, then reveals `RuncheckTheme`
- `RuncheckApp` also initializes source-set-specific `SentryInit`; debug builds may report to Sentry through `sentry-android-core` only when `RUNCHECK_SENTRY_DSN`, `SENTRY_DSN`, or ignored `debug.credentials.properties` provides `sentry.dsn`; release builds are a no-op and must remain telemetry-free
- WorkManager runs `HealthMonitorWorker` for snapshot collection + alert evaluation
- WorkManager runs `HealthMaintenanceWorker` for app-usage refresh, cleanup, and widget refresh
- WorkManager runs `InsightGenerationWorker` on the monitoring scheduler lifecycle to generate persisted Home insights from Room history; rule evaluation completes before all generated rule results are replaced in one Room transaction
- `WeeklyReportScheduler` waits for confirmed Pro readiness before reconciling its unique one-time `weekly_report` job for the next local Monday at 09:00; routine reconciliation keeps pending/running work, while an unready timezone change is retained and consumed as one explicit replacement after eligible readiness
- `WeeklyReportWorker` retries while Pro state is unready, then reads only the previous completed local Monday-to-Monday interval, posts through the low-importance reports channel, records notification-denied periods as handled without catch-up, and schedules the next occurrence after terminal handling
- App Usage's `Not used` mode is domain-Pro-gated and derives 30/60/90-day candidates from launcher-visible user apps plus `UsageStats`; bounded `StorageStatsManager` lookups run on `AppDispatchers.IO`, tolerate per-app failures, and cache only within one screen refresh session
- `RealTimeMonitorService` is an opt-in live notification foreground service and must stay user-controlled from Settings
- Battery, Health Score, and responsive Quick Glance widgets are backed by the existing Room health snapshot sources and treated as a Pro feature; free users are gated before snapshot or health-score work
- Widgets follow launcher/system day/night colors independently of the app's persisted `ThemeMode`; Quick Glance cells deep-link to Home, Battery, Storage, and Thermal
- Storage may measure aggregate app cache as read-only data but cannot clear other apps' caches; cleanup remains limited to the existing MediaStore-backed categories and trash flow
- Trial state currently counts as Pro access through `ProState.isPro`
- `AppShellViewModel` combines Room insight state with Pro readiness for the four-item top-level navigation bar; the Insights badge counts visible unseen items, protected external routes wait for the initial Pro state, external routes rebuild their documented parent root without restoring stale child stacks, and Export renders the shared locked state until access is confirmed
- Home now includes a rule-driven Insights surface backed by Room-persisted insight rows; Home shows at most one ranked item and the full list lives in the dedicated Insights screen
- Home's implemented hierarchy is a health `HeroGauge`, four domain-specific `MetricTile`s, an Insights preview, and an active-trial card only when applicable
- Insights owns the current-week summary and Weekly Report entry; Tools owns Speed Test, Storage Cleanup, Charger Comparison, App Usage, Learn, and Export
- The multibound production `InsightRule` set is the supported-rule source of truth; repository observation and generation purge persisted rows for removed rule IDs before they can render
- Debug-only insight seeding and manual regeneration live behind debug source-set wiring and must stay release-inaccessible

State restoration conventions:

- Use `rememberSaveable` for screen-local UI state such as sheet visibility, dialogs, and metric chip selections
- Use `SavedStateHandle` for route-backed or process-death-sensitive state such as selected history period, cleanup type, and fullscreen chart args

---

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

The project uses the Detekt 2 plugin id `dev.detekt` at `2.0.0-alpha.3`. Both ktlint and Detekt use compose-rules `0.5.9`, while the ktlint rule engine remains pinned to `1.8.0`; verify all three together before changing this toolchain.

---

## Code Review Priorities

When reviewing a PR or file, check for these in order:

### 1. Layer violations
- Does `domain/` import anything from `android.*` or `data/`?
- Does `ui/` call data sources directly, bypassing use cases?
- Are ViewModels the only bridge between `ui/` and `domain/`?

### 2. Measurement reliability
- Every sensor value must be wrapped in `MeasuredValue<T>` with a confidence level: `ACCURATE`, `ESTIMATED`, or `UNAVAILABLE`.
- Raw values must never be shown to the user without a confidence indicator (ConfidenceBadge component).
- `BATTERY_PROPERTY_CURRENT_NOW` must be validated: multiple reads, non-zero, range -10000..+10000 mA, sign matches charge state.
- Thermal data must use `PowerManager.getCurrentThermalStatus()` (API 29+) and `getThermalHeadroom()` (API 30+). No sysfs reads — SELinux blocks these on modern Android.

### 3. API level guards
- All API 29+ calls guarded with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q`.
- All API 30+ calls guarded with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.R`.
- All API 34+ calls guarded with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE`.
- Minimum SDK is 26. No calls to APIs below 26 without a fallback.

### 4. Pro feature gating
- Pro features: Charger Comparison, App Usage, Extended History, Thermal Logs, Weekly Report, CSV Export, Widgets, and Storage Cleanup.
- Each must check `ProManager.isPro()` or the injected `ProStatusProvider` / `IsProUserUseCase` path before showing content.
- Locked state must use `ProFeatureLockedState` component, not custom implementations.
- The top-level Home Insights card is not a Pro feature. It may link into Pro-gated destinations, but the destinations themselves must remain gated.

### 5. Speed test
- Uses M-Lab NDT7 (`ndt7-client-android` Kotlin library). No other speed test backend.
- Never hardcode a fixed server — NDT7 auto-selects nearest global server.
- Cellular warning dialog must appear before test starts if active network is not WiFi.
- Outbound network calls are allowed only for user-initiated speed tests, latency measurement, and Google Play Billing.
- Reading current connection details (WiFi, 5G, SSID, signal, IP, DNS) must stay on-device via Android APIs and must not trigger socket, HTTP, or ping-style probes.

### 6. Animations
- All animations must check `LocalReducedMotion.current` (or `MaterialTheme.reducedMotion`) and skip/shorten if true.
- Named visual-system durations live in `MotionTokens`: instant 100ms, fast 180ms, medium 320ms, slow 520ms, deliberate 900ms, counter 700ms, result stagger 80ms, list-item stagger 40ms, and chart-fill delay 200ms.
- `AnimatedFloatText` intentionally preserves its legacy 200ms `FastOutSlowInEasing` motion for the live speed-test callers; new `AnimatedCounter` uses the 700ms decelerate spec.
- Shared springs are centralized: gauge 0.72/180, chip 0.55/420, and speed value 0.8/300.

### 7. UI consistency
- Dark surfaces: page `#08171C`, surface 1 `#0D2229`, surface 2 `#123039`, surface 3 `#183D47`; dark text is `#F4FAFC`, `#A9BEC6`, and `#789099`.
- Light surfaces: page `#DDE6EA`, surface 1 `#FFFFFF`, surface 2 `#F4F7F8`, surface 3 `#E8EFF2`; light text is `#172A32`, `#405A64`, and `#5B727C`.
- Domain accents are theme-aware: Battery `#FFB627`/`#9B5C00`, Network `#4EA8F5`/`#0B63B0`, Thermal `#FF7A45`/`#C24A12`, Storage `#35DDBE`/`#007A66` (dark/light).
- Semantic status palette: Healthy `#006B57`, Fair `#795F00`, Poor `#9C4E00`, Critical `#B3261E`, Neutral `#4E6570`, Unavailable `#647A83`.
- Gauge arcs must be neutral (white/gray) — not colored. Accent color is for the indicator only.
- Status colors use explicit opaque container/foreground pairs for small badges and status dots only, never for large fills, and must always be paired with text or an icon.
- Typography: Manrope for body text and hero units; JetBrains Mono for hero numbers, gauge values, and card metrics. Signature sizes are 64sp hero number, 24sp hero unit, 44sp gauge value, and 28sp card metric.
- Main cards use 24dp corners, hero cards and bottom sheets use 32dp corners, and small elements use 12dp corners.
- Cards remain flat. Light-theme main cards use the centralized 1dp `LightCardBorder #7A939D`; dark-theme main cards have no general border. `ActionCard` keeps one separate 1dp `outlineVariant` border at 35% alpha and must not receive the general card border too.
- Signature components are `HeroGauge`, `MetricTile`, `StatBlock`, `StatusPill`, `EmptyStateIllustration`, and `AnimatedCounter`; use them as their screen migrations land instead of creating parallel variants.
- History charts share `ChartViewport`: finite data plus explicit ticks determine the padded viewport, quality zones are clipped instead of expanding it, and at most four Y labels survive the pixel-spacing policy.
- Chart primary-state precedence is `loading → error → locked → insufficient → data`; one chart region renders exactly one of these states.
- History periods use a dedicated `LazyRow` of stable `FilterChip`s with 12dp spacing, selected-item scrolling, reduced-motion snapping, and conditional edge fades.
- Screen horizontal and card internal padding are 20dp, card gap is 12dp, and section gap is 28dp.
- Shared touch targets, icon sizes, and common CTA heights should come from `UiTokens` instead of repeating raw values in shared components.
- No dynamic colors. If a task changes visual design, follow `UI-SPEC.md` instead of inventing alternate tokens or component variants.
- English-only strings are intentional right now. Do not reintroduce partial localization without updating docs and string coverage together.
- Icons: use `Icons.Outlined` exclusively — no `Icons.Default`, `Icons.Filled`, or `Icons.Rounded`
- All padding/spacing values must be on the 4dp grid (2/4/8/12/16/20/24/28/32dp)
- All animation durations must use `MotionTokens` constants, never bare `tween()` without explicit spec
- All ViewModels with live state flows must use `.sample(333L)` to throttle UI updates

### 8. Accessibility
- Minimum touch target: 48dp.
- All visual elements (charts, rings, bars) must have content descriptions.
- Status information must never rely on color alone — always paired with text or icon.

---

## What to Flag

Raise a review comment for any of the following:

- Layer violation (data/domain/ui boundary crossed)
- Missing API level guard on a version-gated API
- Sensor value shown without MeasuredValue wrapper or ConfidenceBadge
- Pro feature accessible without `isPro()` check
- Animation missing reduced motion check
- Hardcoded color hex that doesn't match the palette above
- Touch target smaller than 48dp
- Sysfs read for thermal data (use PowerManager API instead)
- NDT7 speed test using a fixed server URL
- Any outbound network call outside the speed test flow, latency measurement, or billing
- Any release-path telemetry, crash reporting, or analytics expansion beyond the current debug-only Sentry setup

---

## What Not to Change

- App name is `runcheck` (lowercase). Never change to RunCheck, Runcheck, or any other casing.
- System, light, and dark modes only. No AMOLED toggle.
- No dynamic colors.
- One-time Pro purchase only. No subscription, no ads.
- English-only localization is intentional for now. Do not reintroduce partial Finnish strings ad hoc.
- Debug-only Sentry wiring exists for local/dev diagnostics; keep it on `sentry-android-core`, do not hardcode the DSN, and do not ship crash reporting, analytics, replay, tracing, NDK symbol capture, or tracking in release.
- NDT7 backend for speed tests. No alternatives.
- Minimum SDK: 26. Do not lower.

---

## Working Conventions

- Prefer explicit imports.
- Avoid wildcard imports.
- Keep code comments in English.
- Avoid `!!`.
- Put user-facing strings in resources.
- Keep composables small and focused.
- Keep ViewModel state explicit and testable.
- Prefer minimal, targeted edits over broad rewrites.

---

## Preferred Local Skills

If local Codex skills are installed, prefer:

- `runcheck-deep-review` for deep reviews, large change audits, LLM-generated Android code audits, and subtle API/lifecycle regression checks
- `runcheck-security-scan` for manifest, permission, exported-component, logging, secrets, and release-safety audits

---

## Low-CPU Verification

- This repository is often worked on with limited CPU headroom. Avoid heavy local verification by default.
- Do not run full Gradle builds or full test suites unless explicitly requested or required to unblock the task.
- Prefer static analysis, targeted file review, and minimal commands first.
- If verification is needed, use the smallest scoped check possible: one compile task, one module task, or one narrowly filtered test class.
- Avoid running multiple coding agents or tools that may build the same repo in parallel.
- When verification is intentionally skipped or minimized, say so clearly in the final response.


<claude-mem-context>
# Memory Context

# [runcheck] recent context, 2026-06-26 12:56pm GMT+3

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 50 obs (18,445t read) | 409,222t work | 95% savings

### Mar 16, 2026
4489 8:49p 🔵 Network detail screen structure examined for tap-to-copy integration
4490 " 🔵 MetricRow component structure analyzed for interactive enhancement
4491 8:50p ✅ Added clipboard feedback string resource
4492 " 🟣 Added Finnish localization for clipboard copy feedback
4493 " 🟣 MetricRow component enhanced with tap-to-copy and text truncation
4494 " 🟣 Network detail fields enabled for tap-to-copy interaction
4495 8:51p 🟣 Made WiFi BSSID copyable with tap-to-copy functionality
4496 8:52p 🟣 MetricRow component enhanced with tap-to-copy and truncation support
4497 " 🔵 MetricRow tap-to-copy implementation verified for design consistency
4498 8:53p 🔄 Optimized MetricRow component for copyable and truncation logic
4499 " 🔴 Text alignment corrected for truncated values in MetricRow
S383 Update Get Shit Done (GSD) meta-prompting system from v1.22.4 to latest version (Mar 16, 8:53 PM)
### Mar 17, 2026
4500 5:37p 🔵 Battery & Thermal Enhancements Specification
4501 5:41p 🔵 Battery and Thermal Subsystem Architecture Analysis
### Mar 18, 2026
4502 10:52a ✅ GSD upgraded from v1.22.4 to v1.25.1 globally
S384 Fetch updated runcheck Android app code from GitHub repository (Mar 18, 10:53 AM)
4503 10:54a 🟣 Device-specific battery monitoring and storage model enhancements
S385 Comprehensive architecture audit of runcheck Android app after pulling GitHub updates (Mar 18, 10:55 AM)
4504 11:06a 🔴 DAO dependency injection scoping corrected with @Singleton annotations
4505 11:08a 🔄 Domain model ScannedFile decoupled from Android Uri dependency
S386 Comprehensive Jetpack Compose code review for performance, correctness, and best practices across all UI files (Mar 18, 11:08 AM)
4506 " 🔴 Fixed hardcoded navigation route in cleanup feature
4507 " 🔄 FileExportRepository implementation updated to return String URIs
4508 11:09a 🔄 Removed Compose UI dependency from ThumbnailLoader data layer class
4509 " 🔄 FileListItem UI component decoupled from ThumbnailLoader data layer dependency
4510 11:10a 🔄 SettingsUiState updated to use String URIs instead of android.net.Uri
S387 Comprehensive Jetpack Compose code review for runcheck Android app with fixes applied to all issues including minor ones (Mar 18, 12:43 PM)
S388 Comprehensive Jetpack Compose code review with systematic implementation of fixes; clarification requested on impact of removing unused imports (Mar 18, 12:44 PM)
S389 Comprehensive Room database review covering entities, DAOs, migrations, type converters, threading, and lifecycle (Mar 18, 12:45 PM)
S390 Verification that all minor/low severity Room database issues were addressed (Mar 18, 4:20 PM)
S391 Fix duplicate Android string resource preventing unit test execution in runcheck app (Mar 18, 4:22 PM)
4511 4:46p 🔵 Test Coverage Analysis Complete for runcheck Android App
4512 6:50p 🔵 Duplicate string resource blocks Android build
4513 " 🔵 String resource settings_data_section duplicated four times
4514 " 🔵 First duplicate settings_data_section found in Export section
4515 " 🔵 Finnish locale duplicates untranslated settings_data_section string
4516 6:51p 🔵 Second duplicate settings_data_section found in Data Management section
4517 " 🔵 Finnish locale duplicates untranslated settings_data_section in Data Management section
4518 " 🔴 Removed duplicate settings_data_section from Export section
4519 " 🔴 Removed duplicate settings_data_section from Finnish Export section
4520 6:52p 🔴 Remove CLAUDE.md files from Android resource directories
4521 6:53p 🔵 Identified claude-md-management plugin causing auto-CLAUDE.md creation
4522 6:54p ✅ Add CLAUDE.md to .gitignore for Android resource directories
4523 6:55p 🟣 Implement hookify rule to block CLAUDE.md creation in Android res directories
S392 Fix recurring Android Gradle build failures caused by CLAUDE.md files in res directories (Mar 18, 6:55 PM)
4524 6:57p 🔵 Android Build Failure: CLAUDE.md Files in Resource Directories
4525 6:58p 🔵 Claude-Mem Plugin Enabled Despite CLAUDE.md Conflicts
4526 " 🔵 Prevention Hook Exists But Ineffective Against CLAUDE.md Creation
4527 " 🔵 Multiple Hook Layers Failed to Prevent CLAUDE.md Creation
4528 6:59p 🔵 Claude-Mem Plugin Operates on PostToolUse Lifecycle Phase
4529 7:01p 🔵 Claude-Mem Creates CLAUDE.md Files in Every Processed Directory
4530 " 🔴 Removed Build-Breaking CLAUDE.md Files from Android Resource Directories
4531 7:02p 🔄 ProGuard rules optimized by removing unnecessary keep rules
4532 " 🔄 Removed unused kotlin-android plugin from version catalog
4533 " ✅ Enforced centralized repository management in Gradle settings
**4534** 7:03p 🔄 **Migrated environment variable reads to Gradle Providers API**
The build configuration was modernized by migrating from direct System.getenv() calls to Gradle's Providers API using providers.environmentVariable(). This is critical for enabling Gradle's configuration cache feature, which can dramatically improve build performance by caching the result of the configuration phase. Direct System.getenv() reads break configuration cache because they're not tracked as build inputs, while the Providers API creates lazy providers that Gradle can properly track and cache. The migration maintains all existing default values (runcheck_pro for product ID, locate.measurementlab.net for latency host, 443 for port, and AdMob test IDs) while enabling modern Gradle optimizations. This is particularly important for CI/CD pipelines and local development where configuration cache can reduce build times significantly.
~386t 🛠️ 4,624

**4535** " 🔄 **Migrated signing configuration to Providers API**
The signing configuration was updated to use Gradle's Providers API for reading release signing credentials from environment variables. This completes the migration away from System.getenv() calls in the build script. The signing config reads four sensitive environment variables (keystore path, keystore password, key alias, and key password) that are required for creating signed release APKs. Using providers.environmentVariable().getOrNull() maintains the existing behavior where signing is optional - if the keystore path is not set, the release build simply won't be signed (useful for CI builds that don't need signing). This migration is critical for enabling Gradle configuration cache, which significantly improves build performance by caching configuration phase results.
~375t 🛠️ 4,696

**4536** 7:04p ✅ **Removed unused dependency locking configuration**
The dependency locking configuration was removed from the build script as it was not being actively used. Gradle's dependency locking feature requires explicit lock file generation with --write-locks flag and committed lockfiles to provide reproducible builds. When declared but not maintained, it adds configuration overhead without benefits. The project already uses a version catalog (libs.versions.toml) for centralized dependency version management, which provides similar reproducibility guarantees. Removing unused features simplifies the build configuration and reduces potential confusion. If reproducible builds become a requirement later, dependency locking can be re-enabled with proper lock file maintenance workflow.
~333t 🛠️ 3,002

**4537** " ✅ **Updated documentation to reflect Kotlin plugin configuration**
The project documentation was updated to remove an outdated note about AGP built-in Kotlin configuration. The previous documentation mentioned that android.builtInKotlin was disabled for KSP compatibility, but this configuration detail was removed from the tech stack overview. This aligns with earlier changes where the kotlin-android plugin declaration was removed from the version catalog, with the project now relying on the kotlin-compose plugin for Kotlin compilation. The simplified documentation reflects the current build configuration without implementation details that may change or become outdated.
~285t 🛠️ 5,794

### Jun 24, 2026
**5639** 8:24p 🔵 **AGP lint 32.1.1 missing from Gradle dependency verification metadata**
Investigation into a Gradle build or lint failure in the runcheck Android project revealed that AGP/lint tooling had been upgraded from version 32.1.0 to 32.1.1, but the dependency verification metadata was stale. The most recent dependency-verification-report.html (timestamped at-1782315526490 on 2026-06-24) documented exactly which artifacts were missing verification checksums. The git diff shows verification-metadata.xml and verification-keyring.keys have been refreshed to add the missing 32.1.1 entries for all AGP/lint tooling components including intellij-core, kotlin-compiler, lint, lint-api, lint-checks, lint-gradle, play-sdk-proto, and uast. This matches the established pattern in MEMORY.md where lint/security wrapper failures often stem from missing dependency-verification metadata rather than actual code defects.
~391t 🔍 139,676


Access 409k tokens of past work via get_observations([IDs]) or mem-search skill.
</claude-mem-context>
