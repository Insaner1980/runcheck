# runcheck — Project Overview

Android device health diagnostics app built with Kotlin and Jetpack Compose. Single dark theme, one-time Pro purchase, no subscriptions.

Reading map:

- [Technical snapshot and source map](#technical-snapshot): versions, configuration inputs, architecture-review entry points.
- [Architecture and data flow](#architecture-and-data-flow), [navigation](#navigation), [runtime systems](#runtime-systems): ownership, state, background work, failures.
- [Measurement reliability](#measurement-reliability-and-health-score): valid values, unavailable data, confidence, scoring.
- [Home](#home-screen), [Insights](#insights-screen), [Battery](#battery-detail), [Charger](#charger-comparison), [Network](#network-detail), [Speed Test](#speed-test), [Thermal](#thermal-detail), [Storage](#storage-detail), [Cleanup](#cleanup-screen), [App Usage](#app-usage), [Learn](#learn), [Fullscreen Chart](#fullscreen-chart), [Settings](#settings), [Pro Upgrade](#pro-upgrade-screen): screen-specific behavior.
- [Persistence](#persistence), [monetization](#monetization-model), [security/privacy](#security-and-privacy-surface): storage, reset/export, entitlements, platform boundaries.
- [Design system](#brand--design-system), [responsive layout](#responsive-layout-and-interaction-reference): colors, typography, dimensions, semantics, motion, font-scale branches.
- [Testing](#testing-and-verification), [CI/CD](#cicd-pipeline), [Insight rule inputs](#rule-inputs-windows-and-interpretation): verification limits, operational tooling, algorithm review.

---

## Document Purpose, Scope, and Evidence Rules

This document is the detailed current-state map for architecture reviews, code-review question generation, implementation planning, security review, test planning, and maintenance work.

Snapshot rules:

- Last source-backed refresh: **2026-09-08**.
- At refresh time the checkout was on `codex/julkaise-paikalliset-muutokset-20260726` at commit `0eec4dd74cd11646a39e4d847d5ad6b3fb177085`, with pre-existing uncommitted work. This document was updated in place without reverting or rewriting that work.
- The snapshot includes the existing uncommitted Home redesign, enlarged-text layouts, ViewModel provider seams, network identity/Ethernet handling, battery validity checks, persistence/error handling, cleanup verification, billing transitions, tests, and checker configuration. It describes current files, not only `HEAD`.
- This refresh is a documentation-only source/configuration review. No Gradle task, scanner, emulator, physical-device session, or external service status check was run. Test descriptions below identify source coverage, not passing results from this refresh.
- This is a checkout snapshot, not an API compatibility promise, release note, or proof that every runtime path has been exercised on a physical device.
- If this document conflicts with executable code or configuration, the executable source wins and this document must be corrected.
- Intended behavior that exists only in a plan, issue, design mock, or roadmap is not current product behavior.
- Historical reports prove only the exact run, source scope, commit/worktree, device, and timestamp recorded in that report. They do not prove the current dirty checkout.

Evidence precedence for review work:

1. Production source and source-set-specific implementations.
2. Tests, exported Room schemas, and generated/configuration contracts.
3. Gradle files, version catalog, manifest/resources, CI workflows, and local wrapper scripts.
4. `AGENTS.md`, `CODEX.md`, `UI-SPEC.md`, and this file.
5. Historical plans, reports, screenshots, and roadmap text.

Reviewers should distinguish:

- **Code-confirmed**: directly supported by current source/configuration.
- **Test source present**: a named test encodes the behavior, but was not necessarily executed.
- **Test-confirmed**: that named test passed in a recorded run against the relevant inputs; still not necessarily proof of device behavior.
- **Tool-confirmed**: supported by a fresh, scoped analyzer/build report.
- **Device-confirmed**: observed on a named device and APK provenance.
- **Unverified**: plausible or intended, but not proven in the current task.

The highest-risk review surfaces are layer boundaries, Android API guards, measurement confidence, Pro gating, lifecycle cancellation, WorkManager retry/idempotency, Room migrations, outbound networking, release telemetry exclusion, permission/version branching, accessibility semantics, reduced motion, and state restoration.

---

## Technical Snapshot

- Package root: `com.runcheck`
- Application ID: `com.runcheck`
- Current app version: `versionName = "1.0.0"`, `versionCode = 1`
- Main module: single `app` module
- Architecture: Clean Architecture with `data/`, `domain/`, and `ui/`
- Dependency injection: Hilt
- Database: Room (`RuncheckDatabase` schema version 10, exported schema enabled)
- Preferences: DataStore
- Background work: WorkManager
- Widgets: Glance app widgets
- Speed test backend: M-Lab NDT7
- Build: Gradle Kotlin DSL
- Build tooling: Gradle wrapper 9.7.0, AGP 9.2.1, Kotlin Gradle/Compose plugin 2.4.10, Kotlin runtime constraints 2.3.20, KSP 2.3.11, Compose BOM 2026.06.01
- Compile SDK: Android 17 (API 37)
- Target SDK: Android 17 (API 37)
- Min SDK: 26
- Java target: 17
- Localization: English-only (`localeFilters = ["en"]`)
- Build variants: `app/src/debug` and `app/src/release` source sets are active
- Release signing: optional until a release artifact is requested, then `RUNCHECK_KEYSTORE_PATH`, `RUNCHECK_KEYSTORE_PASSWORD`, `RUNCHECK_KEY_ALIAS`, and `RUNCHECK_KEY_PASSWORD` are required; signed release artifact tasks must run with `--no-configuration-cache` and must provide the latest published versionCode through `--project-prop=runcheck.releaseVersionCodeFloor=<code>` or `RUNCHECK_RELEASE_VERSION_CODE_FLOOR`

High-level package layout:

```text
app/src/main/java/com/runcheck/
├── billing/
├── data/
├── debug/
├── di/
├── domain/
├── pro/
├── service/
├── ui/
├── util/
├── worker/
└── widget/
```

Source inventory at this snapshot:

| Source surface | Kotlin files | Notes |
|----------------|-------------:|-------|
| `app/src/main/java` | 369 | Includes `MainActivity`, `RuncheckApp`, and all shared production packages |
| `app/src/debug/java` | 4 | Debug Sentry and insight tooling/bindings |
| `app/src/release/java` | 2 | Release-safe Sentry and insight bindings |
| `app/src/test/java` | 126 | JVM unit-test source files |
| `app/src/testDebug/java` | 2 | Debug-source-set tests |
| `app/src/androidTest/java` | 2 | Instrumented/migration test sources |

The largest shared source areas are `ui/` (145 files), `domain/` (129), and `data/` (59). This count is an orientation aid, not an architectural quality metric; review questions should follow dependencies and runtime ownership rather than file volume.

Debug/release-specific insight tooling also lives outside the shared main source tree:

- `app/src/debug/java/com/runcheck/debug/insights/` for debug implementations
- `app/src/debug/java/com/runcheck/di/InsightDebugModule.kt` for debug Hilt bindings
- `app/src/release/java/com/runcheck/di/InsightDebugModule.kt` for release Hilt bindings
- `app/src/main/java/com/runcheck/debug/insights/` for release-safe public stubs used by shared code
- `app/src/release/java/com/runcheck/SentryInit.kt` for release no-op Sentry initialization

### Build and dependency source of truth

- `gradle/libs.versions.toml` is the source of truth for dependency and plugin versions.
- `gradle.properties` owns the `runcheck.buildTools.*` security versions for vulnerable transitive build-tool dependencies. Root `build.gradle.kts` applies these pins only to the matching buildscript, ktlint, Android Lint, and Unified Test Platform configurations; application runtime configurations remain unaffected.
- `gradle/wrapper/gradle-wrapper.properties` pins Gradle to `9.7.0` and verifies the binary distribution with the checked-in SHA-256.
- `settings.gradle.kts` enforces centralized repositories with `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.
- Approved repositories are `google()`, `mavenCentral()`, and JitPack only for `com.github.m-lab`.
- `settings.gradle.kts` pins `org.gradle.toolchains.foojay-resolver-convention` to `1.0.0` and applies repository content filters to plugin resolution.
- The app intentionally uses `org.jetbrains.kotlin.plugin.compose`; do not reintroduce `kotlin-android` unless the AGP/Kotlin integration model changes and is verified.
- Detekt uses the Detekt 2 plugin id `dev.detekt`; do not apply old Detekt 1 plugin assumptions without checking `gradle/libs.versions.toml` and `app/build.gradle.kts`.
- `debug.credentials.properties` is ignored local-only input for debug Sentry DSN. It is read through Gradle Providers API and must not be committed.
- `localeFilters = ["en"]` and `app/src/main/res/xml/locales_config.xml` both encode the English-only product state.

Current version catalog highlights:

| Area | Current value |
|------|---------------|
| Gradle wrapper | `9.7.0` |
| Android Gradle Plugin | `9.2.1` |
| Kotlin Gradle / Compose plugin | `2.4.10` |
| Kotlin runtime constraints | `2.3.20` |
| KSP | `2.3.11` |
| Hilt | `2.60.1` |
| Hilt AndroidX / Hilt Work | `1.4.0` |
| Room | `2.8.4` |
| Compose BOM | `2026.06.01` |
| Navigation Compose | `2.9.8` |
| Lifecycle | `2.11.0` |
| Kotlin coroutines | `1.11.0` |
| Activity Compose | `1.13.0` |
| Core KTX | `1.19.0` |
| ProfileInstaller | `1.4.1` |
| WorkManager | `2.11.2` |
| DataStore | `1.2.1` |
| Paging | `3.5.0` |
| Play Billing | `9.1.0` |
| Glance | `1.1.1` |
| M-Lab NDT7 client | `e0cb663613eb252a7793216ad28cf54a35677b8f` |
| OkHttp | `5.4.0` |
| Gson | `2.14.0` |
| kotlinx.serialization JSON | `1.11.0` |
| JUnit | `4.13.2` |
| MockK | `1.14.11` |
| AndroidX Test Ext JUnit | `1.3.0` |
| AndroidX Test Runner | `1.7.0` |
| Sentry debug-only core | `8.51.0` |
| Dependency Analysis Gradle plugin | `3.17.0` |
| ktlint rule engine | `1.8.0` |
| ktlint Gradle plugin | `14.2.0` |
| Detekt | `2.0.0-alpha.5` |
| compose-rules for ktlint | `0.6.4` |
| compose-rules for Detekt | `0.6.4` |
| OWASP Dependency-Check Gradle plugin | `12.2.2` |
| SonarQube Gradle plugin | `7.4.0.8496` |
| Compose Stability Analyzer | `0.12.0` |
| Google Android Security Lints | `1.0.4` |
| JaCoCo | `0.8.14` |

Local checker-helper versions outside the Android application dependency graph:

| Area | Current value | Ownership and scope |
|------|---------------|---------------------|
| DeepSec | `2.3.8` | Exact dependency in `.deepsec/package.json` and `.deepsec/pnpm-lock.yaml`; used only by the local DeepSec scan/process/export scripts |
| TypeScript | `^7.0.2` | `.deepsec` development dependency, not an Android runtime dependency |
| Node type declarations | `^26.2.0` | `.deepsec` development dependency, not an Android runtime dependency |

The `.deepsec` scripts expose full/custom scan, process, revalidate, and Markdown export paths. The custom scan is restricted to the repository's named Android/export/FileProvider/URI-sharing/network/telemetry/logging matcher set. OSV source scanning excludes `.deepsec`, so these helper dependencies are reviewed through the DeepSec/helper-tool path rather than being reported as app dependencies.

Build-tool-only transitive security pins from `gradle.properties`:

| Property family | Current value | Applied scope |
|-----------------|---------------|---------------|
| Jackson | `2.22.1` (`jackson-annotations` remains on compatible `2.22`) | Affected Gradle buildscript modules |
| jose4j | `0.9.6` | Gradle buildscript classpath |
| Bouncy Castle | `1.84` | Gradle buildscript plus affected Android Lint / Unified Test Platform configurations |
| JDOM | `2.0.6.1` | Gradle buildscript classpath |
| Logback | `1.5.34` | ktlint configuration only |
| Netty | `4.1.137.Final` | Affected Android Lint / Unified Test Platform configurations |
| Commons Lang | `3.20.0` | Affected Android Lint / Unified Test Platform configurations |
| Apache HttpClient 4 | `4.5.14` | Affected Android Lint / Unified Test Platform configurations |
| Apache HttpClient 5 / HttpCore 5 | `5.6.3` / `5.4.3` | Gradle buildscript classpath |
| jsoup | `1.23.1` | Gradle buildscript classpath |

These pins are deliberately configuration-scoped in root `build.gradle.kts`; they must not be converted into application-runtime-wide forcing or broad package-level vulnerability suppression.

### Toolchain compatibility and build execution

- The active coordinated toolchain is AGP 9.2.1, Kotlin Gradle/Compose plugin 2.4.10, Kotlin runtime constraints 2.3.20, KSP 2.3.11, Hilt 2.60.1, Detekt 2.0.0-alpha.5, and Compose Stability Analyzer 0.12.0.
- The earlier Kotlin 2.4 / stability-analyzer incompatibility is resolved. Both debug and release stability variants have current baselines and `failOnStabilityChange = true`; missing baselines are not allowed.
- Compose library versions come from the Compose BOM, while the Compose compiler is managed through the Kotlin Compose plugin. Treat Kotlin, Compose, KSP, Detekt, analyzer, AGP, dependency verification, and CI extractor changes as a compatibility set.
- Gradle configuration cache is enabled. Build cache and parallel execution are disabled, `org.gradle.workers.max = 2`, and the Kotlin compiler execution strategy is in-process.
- Gradle and Kotlin task build caches are disabled through `org.gradle.caching=false` and `kotlin.caching.enabled=false` while the time-bounded CVE-2026-53914 advisory exception remains active.
- Release builds are minified and resource-shrunk. `copyReleaseArtifacts` names outputs `runcheck-1.0.0-code1-release.apk` and `.aab`.
- Release artifact tasks validate signing inputs, require `--no-configuration-cache`, and require the version-code floor described in the Technical Snapshot. Ordinary debug checks do not require release signing.
- Debug BuildConfig values may read validated local/environment overrides for Sentry DSN, latency host/port, and Pro product id. Release keeps the checked-in product id and no-op Sentry path.

### Build-time and checker configuration inputs

The repository contains variable names and defaults, but no secret values. Review configuration changes against these exact ownership boundaries:

| Input | Scope and current behavior |
|-------|----------------------------|
| `RUNCHECK_SENTRY_DSN`, then `SENTRY_DSN` | Optional debug-only Sentry DSN; ignored `debug.credentials.properties` key `sentry.dsn` is the final local fallback. Release ignores all three paths and uses the no-op source set. |
| `RUNCHECK_LATENCY_HOST` | Optional debug-only host/IP override. Default and release value is `locate.measurementlab.net`; validation rejects blanks, URLs, host-port pairs, whitespace, slashes, quotes, brackets, and values longer than 253 characters. |
| `RUNCHECK_LATENCY_PORT` | Optional debug-only TCP port override; integer `1..65535`, default and release value `443`. |
| `RUNCHECK_PRO_PRODUCT_ID` | Optional debug-only Google Play product-id override. Default and release value is `runcheck_pro`; accepted values are 1-40 characters, must start with a lowercase letter or number, may otherwise contain only lowercase letters, numbers, underscores, or periods, and may not start with `android.test`. |
| `RUNCHECK_KEYSTORE_PATH`, `RUNCHECK_KEYSTORE_PASSWORD`, `RUNCHECK_KEY_ALIAS`, `RUNCHECK_KEY_PASSWORD` | Required together only for signed release artifact tasks. They are read through Gradle Providers and release tasks reject configuration-cache use so signing secrets are not persisted there. |
| `runcheck.releaseVersionCodeFloor` / `RUNCHECK_RELEASE_VERSION_CODE_FLOOR` | Required non-negative latest-published version code for release artifact tasks. Current `versionCode = 1` must be greater than the supplied floor; use `0` before the first Play upload. |
| `DEPENDENCY_CHECK_DATA_DIRECTORY` | Optional OWASP data directory; defaults to `.gradle/dependency-check-data`. |
| `DEPENDENCY_CHECK_AUTO_UPDATE` | Optional OWASP update switch; defaults to enabled. |
| `DEPENDENCY_CHECK_FAIL_BUILD_ON_CVSS` | Optional numeric build-failure threshold; defaults to `7`. |
| `NVD_API_KEY`, `NVD_API_DELAY_MS`, `NVD_API_MAX_RETRY_COUNT`, `NVD_VALID_FOR_HOURS` | Optional NVD inputs. Local Gradle defaults are no key, `6000` ms, `20` retries, and `24` hours; scheduled/manual CI overrides retries to `5` and validity to `168` hours. |
| `SONAR_TOKEN`, `SONAR_HOST_URL` | Local `tools/sonar.ps1` requires the token and defaults the host to `https://sonarcloud.io`; the CI workflow receives `SONAR_TOKEN` from GitHub Secrets. |
| `ANDROID_CHECK_ROOT` | Optional first-priority override for the delegated Android-check checkout. `tools/Invoke-RuncheckProjectCheck.ps1` then checks `C:\Dev\Android-check` and finally the `Android-check` sibling of the resolved runcheck repository root, selecting the first checkout that contains `tools\InvokeProjectCheck.ps1`. |
| `PMD_CPD_MINIMUM_TOKENS` | Optional CPD threshold override; `tools/pc.ps1` supplies `100` when unset. |

### Current authoritative files

When auditing this project, treat these as stronger than older prose docs:

- Build and dependency truth: `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`
- Runtime entry points: `RuncheckApp`, `MainActivity`, `RuncheckNavHost`, `MonitorScheduler`
- Persistence truth: `RuncheckDatabase`, `DatabaseModule`, Room entities/DAOs, app schema exports
- Product gates: `ProState`, `ProManager`, `BillingManager`, `IsProUserUseCase`
- Visual system: `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, `Shapes.kt`, `Spacing.kt`, `MotionTokens.kt`, `UiTokens.kt`, `StatusColors.kt`
- Shared UI/runtime helpers: `LifecycleStartStopEffect`, `HistoryLoadErrorMessage`, `HistoryPeriodFilterChipRow`, `ChartStatsRow`, `RuncheckPermissionPolicy`
- Security surface: `AndroidManifest.xml`, `network_security_config.xml`, `data_extraction_rules.xml`, `backup_rules.xml`, `file_export_paths.xml`, `ReleaseSafeLog`, Semgrep config, Sentry source sets

Current documentation/configuration alignment notes:

- The version catalog pins both compose-rules artifacts to `0.6.4`.
- The active code has no destructive Room fallback. A registered Room callback can record a destructive open event, but `DatabaseModule` does not call `fallbackToDestructiveMigration`; an absent migration must fail instead of silently wiping data.
- Production code centralizes coroutine dispatchers through `AppDispatchers`; cleanup thumbnail loading remains a UI-side default-parameter exception that uses `Dispatchers.IO` and should not be copied.
- `UI-SPEC.md` sections 2.4, 3.3, 4.2, 7, 9.1, and 14 document Home colors, type scale, geometry, shared primitives, responsive-height behavior, and known legacy visual exceptions. Current provider seams and enlarged-text branches must also be checked in source; this refresh does not certify all companion prose as synchronized. Review Home changes against `HomeScreen.kt`, `HomeStatusTiles.kt`, `UiTokens.kt`, `Type.kt`, `HomeScreenTest.kt`, and both companion documents together.

### Code-review source map

Use this map to turn a broad review topic into questions that point at the real implementation and its contract surface.

| Review area | Primary implementation sources | Required review questions |
|-------------|--------------------------------|---------------------------|
| Build/release reproducibility | `settings.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/verification-metadata.xml` | Are repository restrictions, version constraints, dependency verification, optional signing, release version-code floor, and configuration-cache exclusions still internally consistent? |
| Application startup | `RuncheckApp.kt`, `MainActivity.kt`, `SentryInit.kt` in debug/release, `RuncheckTheme` | Are billing, Pro state, WorkManager, screen tracking, widgets, notification channels, debug StrictMode, and debug-only Sentry initialized exactly once and in the correct source set? |
| Dependency injection | `di/RepositoryModule.kt`, `DatabaseModule.kt`, `SystemBindingsModule.kt`, `InsightsModule.kt`, `DataModule.kt`, debug/release `InsightDebugModule.kt` | Does every interface resolve to the intended singleton/scoped implementation, and do debug bindings remain release-inaccessible? |
| Layering | `domain/`, `data/`, `ui/` imports and constructor dependencies | Does domain remain free of `android.*` and data implementations? Does UI reach repositories only through domain contracts/use cases and ViewModels? Is the documented Paging exception the only AndroidX boundary? |
| Navigation and restoration | `ui/navigation/Screen.kt`, `NavGraph.kt`, screen ViewModels using `SavedStateHandle`, composables using `rememberSaveable` | Are route arguments encoded/decoded safely, direct routes argument-free, Pro gates applied before protected content, fullscreen results returned to the correct parent, and process-death-sensitive selections restored? |
| Home aggregation and refresh | `ui/home/HomeViewModel.kt`, `HomeScreen.kt`, `HomeStatusTiles.kt`, `HomeUiState.kt` | Does the 333ms sampled aggregate remain lifecycle-cancelable? Does refresh reuse the same observation graph, reject duplicates, finish after the 900ms minimum, time out at 12s, and reset on failure/stop? Are tiles ordered deterministically and accessible without color-only meaning? |
| Measurement confidence | `domain/model/MeasuredValue.kt`, battery/network/thermal/storage data sources, `ConfidenceBadge.kt` | Can every unavailable or estimated sensor value remain distinguishable from an accurate value? Are normalization, plausibility ranges, sign conventions, API guards, null handling, and user-visible labels aligned? |
| Battery | `data/battery/`, `domain/usecase/ChargerSessionTracker.kt`, `ui/battery/` | Are `CURRENT_NOW` reads normalized from µA to mA, validated, charge-sign aligned, and confidence-tagged? Are public charge-counter estimates clearly estimates? Can foreground/background session updates duplicate or corrupt a charger session? |
| Network and outbound traffic | `data/network/`, `domain/usecase/GetMeasuredNetworkStateUseCase.kt`, `ui/network/` | Do connection details remain on-device? Is periodic latency limited to the configured TCP host/port? Does NDT7 auto-select its server, require validated connectivity, enforce cellular confirmation, lock network identity, and cancel cleanly? |
| Thermal | `data/thermal/`, `domain/usecase/TrackThrottlingEventsUseCase.kt`, `ui/thermal/` | Are PowerManager APIs guarded at API 29/30, listener registration symmetric, missing CPU temperature neutral, and sysfs reads absent? Are throttling event durations and screen lifecycle handled safely? |
| Storage and cleanup | `data/storage/`, `domain/usecase/StorageCleanupUseCase.kt`, cleanup-related use cases, `ui/storage/`, `util/RuncheckPermissionPolicy.kt` | Are media permission branches correct for APIs 26–37? Are launcher-visible app counts described honestly? Are MediaStore deletion batches bounded, consent results revalidated, API 29 recoverable-security flow correct, and free-user scans blocked before work begins? |
| Persistence and migrations | `data/db/RuncheckDatabase.kt`, entities, DAOs, `di/DatabaseModule.kt`, `app/schemas/com.runcheck.data.db.RuncheckDatabase/`, migration tests | Does schema 10 match exported JSON? Are migrations 1→10 complete, indexed for real queries, and free of unintended destructive fallback? Are multi-row insight replacements transactionally atomic? |
| WorkManager and monitoring | `service/monitor/MonitorScheduler.kt`, `service/monitor/HealthMonitorWorker.kt`, `service/monitor/HealthMaintenanceWorker.kt`, `worker/InsightGenerationWorker.kt` | Are unique-work names/policies stable, intervals and constraints correct, cancellation rethrown, retry limited to recoverable failure, notifications debounced, and best-effort widget failure prevented from retrying otherwise successful work? |
| Insights | `domain/insights/`, `data/insights/`, `di/InsightsModule.kt`, `ui/insights/`, `ui/home/insights/` | Are all production rules multibound, confidence-filtered, deduped, expiry-aware, and visibility-filtered for Pro targets? Can dismissed rows resurrect? Is `AppBatteryImpactRule` still excluded from production? |
| Pro and billing | `data/billing/BillingManager.kt`, `pro/ProManager.kt`, `pro/ProState.kt`, `ui/pro/` | Can pending/unacknowledged purchases incorrectly unlock Pro? Is cached release state reconciled with Billing? Does the verified permanent purchase gate protect routes, widgets, export, cleanup, and history? |
| Security/privacy | manifest and XML security resources, source-set Sentry, `ReleaseSafeLog.kt`, project Semgrep rules | Are exported components minimal, backup/cleartext/FileProvider rules restrictive, package visibility narrow, sensitive logging debug-only, release telemetry absent, and every new socket/HTTP/client call inside an approved network surface? |
| Compose UI/accessibility | `ui/theme/`, shared `ui/components/`, screen composables, `UI-SPEC.md` | Are colors/spacing/type/motion centralized, icons outlined, touch targets at least 48dp, visual charts described semantically, state not color-only, reduced motion honored, and high-frequency flows sampled before recomposition? |
| Tests and analyzers | `app/src/test/`, `app/src/androidTest/`, `app/schemas/`, `config/android-check.json`, `config/check-exceptions.json`, wrapper reports | Does the test actually exercise the claimed production branch? Is a report fresh and scoped to this worktree? Are scanner exceptions exact, owned, time-bounded, and still justified? |

Explicit non-capabilities that must not be “filled in” during review or implementation:

- No light theme, AMOLED theme, or dynamic color.
- No subscription, ads, or recurring billing.
- No release telemetry, analytics, crash reporting, replay, tracing, or NDK capture.
- No private `PowerProfile` battery design-capacity lookup.
- No production CPU-temperature source and no sysfs thermal reads.
- No defensible per-app mAh attribution; App Usage is foreground-time based.
- No `QUERY_ALL_PACKAGES` or complete installed-package inventory.
- No fixed NDT7 server and no alternate speed-test backend.
- No automatic network probe when merely displaying current Wi-Fi/cellular details; TCP latency is its own approved measurement flow.
- No claim that unit/static checks alone prove a signed release, physical-device behavior, background survival, Play Billing, external CI, SonarCloud, Qodana Cloud, or GitHub security services.

---

## Architecture and Data Flow

runcheck is a single-module Android app using a layered Clean Architecture style. The UI observes domain-facing use cases and repository interfaces; data implementations own Android framework APIs and persistence.

```text
Android framework / Play Billing / MediaStore / M-Lab NDT7
        ↓
data/ sources + repository implementations
        ↓
domain/ models, repository contracts, use cases, scoring, insights
        ↓
ui/ Hilt ViewModels
        ↓
Compose screens and reusable components
```

Layer expectations:

- `domain/` owns business models, repository interfaces, use cases, health scoring, and insight rules.
- `data/` owns Android APIs, Room, MediaStore, BatteryManager, ConnectivityManager, PowerManager, StorageStatsManager, DevicePolicyManager, PackageManager launcher visibility queries, Play Billing, and NDT7 integration.
- `ui/` owns Compose screens, components, ViewModels, navigation, saved UI state, and visual formatting.
- ViewModels bridge UI and domain only; UI should not call data implementations directly.
- `androidx.paging.PagingData` is an allowed documented boundary exception in domain cleanup/app-usage flows.

Dependency injection:

- `RepositoryModule` binds data implementations to domain repository contracts.
- `DatabaseModule` builds Room with migrations 1 through 10 and provides DAOs.
- `SystemBindingsModule` binds Pro, billing, device profile, monitoring scheduler, screen-state tracking, foreground-app provider, and transaction-runner abstractions.
- `InsightsModule` multibinds each `InsightRule` into `Set<InsightRule>`.
- `DataModule` currently provides shared `Gson`.
- Debug and release `InsightDebugModule` source sets keep debug insight tooling out of release builds.

Important runtime data flows:

- Home combines live battery, network, thermal, storage, latest speed-test, insight, Pro, preference, and monitoring-freshness flows, throttled to 333ms display updates.
- Home's explicit full-check action cancels and restarts the same `loadHome()` observation graph; it does not introduce a second sensor or repository path.
- Battery/network/thermal/storage detail ViewModels sample live updates at 333ms to avoid high-frequency UI churn.
- Periodic monitoring persists snapshots to Room, updates charger sessions, evaluates alerts, records heartbeat, and retries only when core collection/maintenance fails.
- Insight generation is rule-driven and persisted; Home shows ranked persisted rows, not ad hoc generated UI-only messages.
- Fullscreen chart route passes selected source/metric/period back to the parent back stack entry through `SavedStateHandle`.
- Screen observation start/stop is centralized through `LifecycleStartStopEffect` on Home and detail/tool screens so active collectors are started at `ON_START` and stopped at `ON_STOP`.
- Home, Battery, Thermal, Storage, App Usage, Charger Comparison, Insights, Settings, Pro Upgrade, Cleanup, Fullscreen Chart, Network Detail, and Speed Test accept `viewModelProvider: @Composable () -> ... = { hiltViewModel() }` and invoke it inside the composable. The parameter is a provider function, not a ViewModel instance.
- Network Detail and Speed Test additionally use this seam for parent scoping: `NavGraph` supplies the Network parent entry's `NetworkViewModel` to Speed Test when that entry exists; otherwise lookup is route-local. A provider seam alone does not imply that two routes share a ViewModel.

### Failure and cancellation ownership

These contracts matter when reviewing a successful-looking UI or worker result:

| Operation | Current failure behavior and owner |
|-----------|------------------------------------|
| Persist/delete battery, network, thermal, or storage history | Repository `saveReading` and `deleteOlderThan` propagate DAO failures. Worker/use-case callers decide retry or failure; repository logging must not convert an unsuccessful write into success. |
| Interactive thermal event tracking | `GetThermalStateUseCase` uses `getLiveThermalState`. Event-tracking exceptions are reported through `ReleaseSafeLog` (debug only) per measurement; sensor failures and cancellation still propagate. The worker retains `getThermalState`, where event failures propagate into its existing retry policy. Both paths use the same singleton tracker and reset lock. |
| Home/Battery charger-session tracking | Observation awaits the shared singleton tracker before display sampling. Session failures are debug-logged per update without replacing valid live state; cancellation propagates. No detached work is launched. Worker `onBatteryState` failures still reach its existing retry policy. |
| Home/Insights dismissal and seen markers | `ui/common/UiCoroutine.kt` implements `launchUiMutation`: cancellation is rethrown; other exceptions are debug-logged without replacing an already visible screen with an error. |
| Duplicate seen writes | Both ViewModels own an `UnseenInsightTracker`. It suppresses an identical in-flight write, records the unseen-ID set only after persistence succeeds, and allows an unchanged set to retry after failure. An empty set resets the tracker. |
| Speed-test finalization | `NetworkViewModel` reports `Failed` if persistence/finalization throws, even after NDT7 completes. Cancellation still propagates. Successful transfer and successfully stored result are separate milestones. |
| App usage permission loss | `AppUsageDataSource.getUsageSince` returns null for missing permission/service, distinguishing unavailable collection from a successful empty interval. The repository does not advance its collection cursor for null. |
| MediaStore reads | Null cursors and provider failures in summaries, pages, and file-size verification propagate; they are not silently interpreted as zero files or confirmed deletion. `StorageDataSource` may show null media/trash data while retaining primary capacity data; cancellation is rethrown. |
| Settings reset tips | Success text is set only after dismissal-state persistence succeeds. Failure sets a generic UI error instead of showing a premature success toast. |
| External Settings/share intents | `startActivitySafely` returns false for `ActivityNotFoundException` or `SecurityException`; callers can handle failure. CSV sharing attaches every shared URI to ClipData with read grants. |

Application startup jobs use `launchSafely` to retain sibling work after failures. The Pro-driven widget observer waits for `proAccessReady` and catches each refresh failure inside the observation loop, so one failed refresh does not permanently stop future Pro updates.

---

### Architecture coordination and live persistence boundaries

- `MonitoringDataCoordinator` serializes the `ClearMonitoringDataUseCase` Room deletion transaction and debug history seeding with the complete insight read/evaluate/publish operation. Reset waits for admitted generation before deleting its results; later generation reads the remaining or newly collected history. Lock order is coordinator, thermal tracker, then Room. The tracker invalidates its cached active event in `finally` before releasing its lock, including failed or cancelled resets, and restores any surviving Room event on the next observation. Lock waits remain cancellable; monitoring schedules are unchanged.
- Samsung constant-current evidence belongs to the source instance. A mutex serializes sensor reads and evidence updates across collectors; equal readings count at most once per two seconds. A changed signed value or a gap greater than six seconds since the last counted observation restarts the count. Six seconds covers three normal polling intervals and the live notification's five-second one-shot cadence. Missing readings do not refresh evidence. Measurements are read afresh on demand, with no current replay cache or polling after collectors detach.

| Path | Failure and cancellation ownership |
|------|------------------------------------|
| Interactive thermal event tracking | `GetThermalStateUseCase` uses `getLiveThermalState`. Event-tracking exceptions are reported through `ReleaseSafeLog` (debug only) per measurement; sensor failures and cancellation still propagate. The worker retains `getThermalState`, where event failures propagate into its existing retry policy. Both paths use the same singleton tracker and reset lock. |
| Home/Battery charger-session tracking | Observation awaits the shared singleton tracker before display sampling. Session failures are debug-logged per update without replacing valid live state; cancellation propagates. No detached work is launched. Worker `onBatteryState` failures still reach its existing retry policy. |

## Navigation

Push-based navigation from a single Home screen. No bottom nav, no tabs.

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

Defined routes in code:

- `home`
- `insights`
- `battery`
- `charger`
- `network`
- `speed_test`
- `thermal`
- `storage`
- `cleanup/{type}`
- `app_usage`
- `learn`
- `learn/{articleId}`
- `fullscreen_chart/{source}/{metric}/{period}`
- `settings`
- `pro_upgrade`

State restoration details:

- `rememberSaveable` is used for screen-local UI state such as sheet visibility and metric chip selections.
- `SavedStateHandle` is used for route-backed or deep state that must survive recreation, including battery/network history period, cleanup filter selection, and fullscreen chart metric/period.
- Free-tier entry into `charger` and `app_usage` routes redirects to `pro_upgrade`.
- Direct notification/deep-link routes are limited to argument-free destinations in `Screen.directRoutes`.
- Learn article cross-links are validated against direct routes at catalog initialization time.
- Fullscreen chart args are route-backed, but selection changes are returned to Battery/Network through `FullscreenChartResult` keys on the previous back stack entry.

Navigation/runtime details:

- `Screen.directRoutes` contains only argument-free destinations. Notification routes are validated before `MainActivity` consumes them.
- Direct/deep-link navigation waits until Pro access is ready, then resolves `charger` and `app_usage` through `resolveProRoute`; protected routes redirect to `pro_upgrade` for free users.
- `navigateNested` constructs the expected parent stack for Speed Test under Network and Charger Comparison under Battery when navigation starts from Home or Insights.
- Speed Test reuses the parent Network `NetworkViewModel` when that parent entry exists, keeping connection context consistent across the nested flow.
- Route-level screen composables invoke their composable ViewModel provider internally. Network Detail and Speed Test are the parent-sharing case; other provider-based screens still use their own route scope by default.
- Standard destination transitions combine horizontal slide and fade over 300ms. Fullscreen chart uses its own scale/fade motion tokens. Both paths remove transition motion when reduced motion is active.
- `MainActivity` enables edge-to-edge layout, consumes the one-time destructive-database-reset notice, maintains pending notification navigation, refreshes purchase state on every resume, and keeps deep-link handling inside the same navigation host.

---

## Runtime Systems

### App Startup

`RuncheckApp` initializes and coordinates:

- Billing and Pro state
- Notification channels
- Screen-state tracking repository
- Periodic monitoring scheduling
- Widget refreshes when Pro state changes
- Source-set-specific `SentryInit` initialization; debug builds may report to Sentry through `sentry-android-core` only when `RUNCHECK_SENTRY_DSN`, `SENTRY_DSN`, or ignored `debug.credentials.properties` provides `sentry.dsn`; release builds are a no-op and do not include Sentry on the release classpath
- Debug-only StrictMode policies for thread and VM issue logging
- Hilt WorkManager factory through `Configuration.Provider`

Initialization uses an application coroutine scope backed by `SupervisorJob` and `AppDispatchers.default`. Billing/Pro initialization, notification-channel creation, monitor scheduling, and Pro-driven widget refreshes are started as separate application-scope jobs so one failing child does not cancel the others.

### Background Monitoring

Three periodic WorkManager jobs are scheduled through `MonitorScheduler`:

All three requests use `ExistingPeriodicWorkPolicy.UPDATE` and currently set no explicit WorkManager constraints. Disabling monitoring cancels all three unique jobs.

- `HealthMonitorWorker`
  - unique work name: `health_monitor`
  - interval: current `MonitoringInterval` preference (`15`, `30`, or `60` minutes; default `30`)
  - collects battery, network, thermal, and storage snapshots
  - persists readings into Room
  - updates charger sessions through `ChargerSessionTracker`
  - evaluates alert conditions
  - persists last successful worker heartbeat
  - posts notifications for low battery, high temperature, low storage, and charge complete
  - persists alert/debounce state before posting notifications
  - retries core collection failures while `runAttemptCount < 3`; after that it returns success and waits for the next periodic execution
  - attempts the heartbeat only when all core steps succeed; a heartbeat persistence failure itself is logged and does not change the worker result. A WorkManager success therefore does not prove heartbeat persistence or atomic persistence across all four sensor tables.
- `HealthMaintenanceWorker`
  - unique work name: `health_maintenance`
  - interval: current `MonitoringInterval` preference
  - has no battery-level constraint so maintenance cannot be deferred indefinitely on a persistently low-battery device
  - invokes `RefreshAppUsageSnapshotUseCase` first; that use case is a no-op unless the current `ProStatusProvider.isPro()` value is true
  - cleans up old readings only after Pro status is ready; unresolved Pro state makes this run retry, free access retains 24 hours, and purchased Pro uses the selected retention preference
  - refreshes widgets best-effort; widget refresh failure does not force a retry
  - retries when app-usage collection fails, cleanup fails, or Pro readiness prevents cleanup
- `InsightGenerationWorker`
  - unique work name: `insight_generation`
  - evaluates persisted Room history through the Insights Engine
  - refreshes the Home insights surface every 6 hours
  - has no battery-level constraint so insight refresh cannot be deferred indefinitely on a persistently low-battery device
  - retries only on `SQLException`; cancellation is rethrown

Supporting monitor components include:

- `BootReceiver`
- `ScreenStateTracker`
- `MonitoringAlertStateStore`
- `NotificationHelper`

### Live Notification

`RealTimeMonitorService` provides an opt-in persistent notification with real-time battery stats:

- **Opt-in** — disabled by default, toggled in Settings → Live Notification
- Runs as a foreground service (`FOREGROUND_SERVICE_TYPE_SPECIAL_USE`)
- Updates every 5 seconds with live battery data from `BatteryRepository`
- Uses `BigTextStyle` — collapsed shows level/status/temp, expanded shows additional lines
- Configurable per-metric toggles: Current (mA/W), Charging status, Temperature, Screen stats, Remaining time
- Stops immediately when user disables the toggle
- Tapping the notification opens the app
- Uses `START_STICKY`, but self-stops after 30 seconds without enabled live mode or bound clients, and stops when notification permission is unavailable.
- Current can include calculated watts when voltage is available. The setting internally named `liveNotifDrainRate` currently controls the textual Charging/Discharging line.
- LOW-confidence current includes an Estimated text label; UNAVAILABLE current is omitted. The battery widget preserves the same LOW/UNAVAILABLE distinction through `BatteryWidgetSnapshot.currentConfidence`.
- “Screen stats” currently reports only that screen tracking is active; it does not render accumulated screen-on/off durations.
- “Remaining time” currently shows an estimating placeholder while discharging; it is not yet a calculated remaining-time estimate.

### Widgets

Two Glance widgets are present:

- Battery widget
- Health score widget

Widget data is read from the latest Room snapshots through a Hilt `WidgetDataEntryPoint`. Widget access is treated as a Pro feature, and both widgets expose explicit Locked, Empty, Stale, and Content render states.

- The Battery widget observes Pro access, the monitoring-interval preference, and the latest battery row. Content shows level, temperature formatted in Celsius through `widget_temperature_value`, and current only when the persisted confidence is not `UNAVAILABLE`. The Settings Celsius/Fahrenheit preference is not currently applied to this widget.
- The Health widget requires the latest battery, network, thermal, and storage rows. It rejects negative/future timestamps, inputs spanning more than 120 seconds, and data older than `MonitoringFreshnessPolicy.staleAfterMillis(interval)` before calculating the score with the shared `HealthScoreCalculator`.
- Both widgets use responsive small/medium/large Glance layouts. Battery breakpoints are `110x40`, `180x60`, and `250x100` dp; Health breakpoints are `110x110`, `180x110`, and `250x150` dp. Both set `updatePeriodMillis="0"` and depend on explicit app/worker refreshes rather than the platform's periodic widget timer.
- Tapping any widget state opens `MainActivity`; locked widgets do not deep-link directly to purchase.
- `RuncheckApp` refreshes widgets when Pro state changes, and maintenance work refreshes them best-effort. A widget refresh failure does not convert otherwise successful maintenance into a retry.

---

## Measurement Reliability and Health Score

### Measured values and confidence

- Sensor values that can be unreliable use `MeasuredValue<T>` with `Confidence`.
- Internal confidence enum values are `HIGH`, `LOW`, and `UNAVAILABLE`.
- The UI maps those internal values to user-facing badge labels: `HIGH` → Accurate, `LOW` → Estimated, `UNAVAILABLE` → Unavailable.
- `ConfidenceBadge` is the shared UI component and uses theme status tokens for backgrounds/text.

Battery current reliability:

- `DeviceCapabilityManager.validateCurrentNow()` reads `BATTERY_PROPERTY_CURRENT_NOW` three times with 300ms spacing.
- A current source is considered reliable when at least one of the three reads is non-zero and every normalized absolute reading is within `0..10000` mA. The validator does not require the readings to vary.
- Runtime current normalization converts `BATTERY_PROPERTY_CURRENT_NOW` from microamps to milliamps.
- Plausible normalized current must be in `0..10000` mA during capability validation.
- Current sign is aligned with charging state so charging values are positive and discharging values are negative.
- `GenericBatterySource` also rejects a normalized absolute current above 10,000 mA at runtime, before assigning LOW/HIGH confidence. Its Samsung constant-current heuristic can downgrade HIGH to LOW, but cannot promote an invalid reading.
- Samsung constant-current evidence belongs to the source instance. A mutex serializes sensor reads and evidence updates across collectors; equal readings count at most once per two seconds. A changed signed value or a gap greater than six seconds since the last counted observation restarts the count. Six seconds covers three normal polling intervals and the live notification's five-second one-shot cadence. Missing readings do not refresh evidence. Measurements are read afresh on demand, with no current replay cache or polling after collectors detach.
- Capability detection records the source unit as microamps and infers the sign convention from the average sample plus `BatteryManager.isCharging`.
- Remaining battery capacity uses `BATTERY_PROPERTY_CHARGE_COUNTER` only when Android returns a positive value.
- Estimated full battery capacity is calculated by `estimateFullCapacityMah(remainingMah, levelPercent)` and is emitted only when the battery level is 1..100 and the estimate is in the plausible 500..20,000 mAh range.
- Battery design capacity is not queried or displayed in production; `designCapacityMah` remains `null` because this codebase does not use private `PowerProfile` or other private design-capacity APIs.
- Device profile stores manufacturer, model, API level, current unit, sign convention, cycle-count availability, thermal-zone list, and storage-health availability.
- Cycle count is read from the battery-changed intent on API 34+ and normalized to `0..10000`; zero is a valid reported count, while negative/missing and implausibly large values are unavailable. `DeviceCapabilityManager` uses the same normalization. Current device-profile detection leaves `thermalZonesAvailable` empty and marks storage-health support available.
- `Android14BatterySource` does not read battery sysfs paths. If the API 34 battery-changed intent does not provide a valid cycle count, the value is unavailable. Battery health percentage and the UI's separate `designCapacityMah` field remain null because no supported public API exposes those values.
- Vendor-specific battery sources exist for Samsung and OnePlus, with API 34+ variants using Android 14+ capabilities when available.

Thermal reliability:

- Battery temperature comes from `ACTION_BATTERY_CHANGED`.
- Thermal status uses `PowerManager.currentThermalStatus` and `OnThermalStatusChangedListener` on API 29+.
- Thermal headroom uses `PowerManager.getThermalHeadroom(10)` on API 30+ and polls every 3 seconds.
- CPU temperature currently emits `null`; no sysfs thermal reads are used.

Network reliability:

- Current connection details are read through Android network APIs.
- Latency uses five TCP-connect samples against `BuildConfig.LATENCY_HOST` / `BuildConfig.LATENCY_PORT`, with a 1.5s per-sample timeout and 6s total timeout.
- Jitter is computed with an RFC 3550-style moving jitter formula when at least four samples are available.
- Network detail and speed test may display signal, latency, Wi-Fi standard, cellular subtype, DNS/IP/MTU, and VPN state when Android exposes them.

Storage reliability:

- Aggregate app/data/cache bytes use `StorageStatsManager.queryStatsForUser(...)` and are unavailable when the service, access, or platform call is unavailable.
- App count is a distinct count of launchable packages visible through `PackageManager.queryIntentActivities(Intent.ACTION_MAIN + Intent.CATEGORY_LAUNCHER)`; it is not a full installed-app inventory.
- Storage encryption status uses `DevicePolicyManager.storageEncryptionStatus` and maps public platform states to FBE, Encrypted, Inactive, Unsupported, or unavailable.
- No `SystemProperties` reflection or `PackageManager.getInstalledApplications(...)` package inventory is used for these storage values.

### Health score calculation

`HealthScoreCalculator` combines four subsystem scores:

| Subsystem | Weight |
|-----------|--------|
| Battery | 40% |
| Network | 25% |
| Thermal | 25% |
| Storage | 10% |

Status thresholds:

| Score | Status |
|-------|--------|
| 75-100 | Healthy |
| 50-74 | Fair |
| 25-49 | Poor |
| 0-24 | Critical |

Scoring details:

- Battery score penalizes health state, battery temperature, voltage, and optional health percentage.
- Network score is `0` when disconnected.
- The UI presents a disconnected network category as `Unrated`; the internal zero is not shown as a measured, critical result.
- Without a recent speed test, network score is based on signal quality and latency.
- A speed test contributes only when its connection type matches the current connection and its age is within `0..<1 hour`.
- A fresh matching speed test weighs signal 40%, latency/ping 30%, download speed 20%, and jitter/stability 10%; when jitter is absent, available weights are renormalized.
- During the final five minutes before the one-hour cutoff, the speed-test score fades toward the live signal/latency score instead of disappearing as a step change.
- Thermal score penalizes battery temperature, known CPU temperature, and Android thermal status; a missing CPU temperature is neutral.
- Storage score penalizes usage percent, with sharp penalties at high utilization.

---

## Home Screen

Home is the single entry point and aggregates the latest device state from battery, network, thermal, and storage.

Main sections:

- Cardless peach health gauge with 22 rounded segments across 168 degrees.
  Fill is rounded from the clamped score (100 -> 22, 74 -> 16, 48 -> 11, 22 -> 5).
  The actual score and verdict remain visible; centered update age says Updated
  just now for the first minute, then pluralized minutes.
- Peach Run full check action reuses `HomeViewModel.refresh()` -> `loadHome()`.
  Duplicate taps remain disabled, progress lasts at least 900ms after a completed
  snapshot, and timeout/failure/stop clear it through the existing lifecycle.
- Persisted Insights move above the status tiles. Up to three ranked rows retain
  dismissal, destination navigation, unseen count, and access to additional insights.
- Fixed status mosaic: Battery / Thermal with complementary curved edges, then
  a wider Storage tile beside Network. Narrow widths and enlarged text use one column.
  Values, temperature preference, verdict thresholds, and detail destinations are unchanged.
  Storage adds real used/total capacity and a neutral usage bar.
- Tool mosaic: App usage above Learn on the left, with Speed test spanning both
  rows on the right. Icons and descriptions replace the old Quick Tools list.
- Safe content heights below 840dp use a compact Home variant at normal font scale:
  the gauge narrows, section gaps tighten, status tiles use reduced vertical padding,
  and tool cards retain icons and titles while omitting descriptions. The compact layout targets a 360x800dp phone with three-button navigation;
  fit and touch behavior were not revalidated on a device during this refresh. Scrolling remains
  the fallback for enlarged text, additional insights/warnings, and shorter viewports.
- Home's near-black, cream, peach, stone, and graphite palette is scoped through
  `HomeTheme.kt`. Detail routes retain the existing palette. Home uses explicit
  variable Manrope weights rather than the font file's default weight of 200.
- `UI-SPEC.md` section 9.1 defines geometry, responsive behavior, and source files.
  `HomeScreenTest` covers gauge bounds and category order; `HomeViewModelTest`
  covers the existing refresh/observation behavior.

Pro UI handled on Home:

- App usage retains its Pro badge and protected navigation. The former Pro
  unlocked footer card is removed; purchase status remains available in Settings.
- Free users encounter Pro explanations and purchase actions at the protected feature surfaces
- Top-level Insights summary available to all users, with the full list available from the dedicated Insights screen
- Insight targets for Pro-only destinations such as Charger Comparison and App Usage are hidden for free users and visible for purchased Pro users
- Monitoring stale state is derived from the last worker heartbeat and becomes stale after more than 3x the longer of the heartbeat's interval and the current interval, measured in awake uptime so deep-sleep gaps do not trigger the banner.
- Home marks only its currently displayed unseen insight rows as seen through `InsightRepository.markSeen(ids)`.
- Home observation is lifecycle-owned through `LifecycleStartStopEffect`; leaving Home cancels the active load job and clears a running full-check indicator.

---

## Insights Screen

The dedicated Insights route shows the complete active insight list that is visible for the current Pro state; Home uses a separately ranked subset capped at three.

Current behavior:

- `InsightsViewModel` combines active persisted insights, repository unseen-count invalidations, and `ObserveProAccessUseCase`.
- `visibleForProAccess(isPro)` removes Charger and App Usage targets for free users before the screen computes its displayed count and unseen count.
- The DAO order is priority ascending, then confidence descending, then generation time descending. The screen preserves that repository order and does not apply Home's target-diversifying ranking policy.
- Each visible row reuses `InsightRow`, supports dismissal, and resolves its destination through the shared `InsightNavigationHandlers` mapping. A `NONE` target is non-clickable.
- Visible unseen IDs are marked seen after a successful state emission. The ViewModel remembers the last unseen-ID set so repeated identical emissions do not issue duplicate `markSeen` calls.
- Loading, error, count, empty, and populated states are explicit. The populated surface is a vertically scrolling `Column`, not a paged or lazy list.
- Dismissal persists in Room; matching dismissed dedupe keys remain tombstones during regeneration and do not reappear merely because a rule produces the same candidate again.

---

## Battery Detail

Battery detail is the richest diagnostics screen and mixes live state, recent history, current-session insights, and Pro-only long-range history.

Key sections:

- Hero ring with battery level
- Health + charging summary
- Optional mAh remaining estimate
- Current / charging details with `ConfidenceBadge`
- Session-level current statistics
- Screen-on / screen-off drain analysis
- Sleep analysis while discharging
- Charger comparison CTA
- Charging session graph
- Pro-only remaining charge estimates
- Pro-only history chart
- Pro-only battery statistics panel

Battery-specific supporting behavior:

- Current readings use `MeasuredValue<Int>`
- Current confidence is internally `HIGH`, `LOW`, or `UNAVAILABLE`; badge copy presents those as Accurate, Estimated, or Unavailable.
- Remaining mAh comes from the public BatteryManager charge-counter value when the platform provides one.
- Estimated full capacity is shown as an estimate only when it can be derived from remaining mAh and current battery level inside the repository's plausible range.
- Design capacity is intentionally absent from the UI because no stable public design-capacity source is used.
- Current stats are tracked in-memory and reset on status change
- Session and history charts can open a fullscreen landscape chart route
- History charts use "Instrument Sweep" animation (grid fade → illuminated sweep reveal → latest-value emphasis)
- Live charts use eased scroll interpolation and a single settling halo on new data; live current remains signed around a visible zero reference
- Battery screen also consumes dismissed educational/info cards
- Charger session tracking runs from Home and Battery live observation and from `HealthMonitorWorker` so charge sessions can be updated in foreground and background.
- Available history periods are Since Unplug, 1 hour, 6 hours, 12 hours, 1 day, 1 week, 1 month, and All. Free repository access is clamped to the last day regardless of the requested long period; Pro receives the requested period, and All is capped at 5,000 rows.
- The current charging-session chart remains available without Pro. Persisted long-range battery history and its fullscreen source are Pro-gated.

---

## Charger Comparison

Charger Comparison is a purchased-Pro route backed by charger profiles, charging sessions, selected-charger preference state, and `ChargerSessionTracker` measurements.

Current behavior:

- Navigation applies `ProRouteGate`, and `ChargerViewModel` independently fails closed to `Locked` when `IsProUserUseCase` is false. Add, delete, select, and clear-selection actions also return without work for non-Pro state.
- Observation is lifecycle-controlled. Losing Pro access cancels the active charger-data load and exposes the locked state.
- Users can add a non-blank charger name, select one profile for future sessions, clear that selection, and delete a profile through a confirmation dialog. Repository insertion trims surrounding whitespace.
- Deleting the selected charger clears the DataStore selection first. Room's charger-session foreign key uses `ON DELETE CASCADE`, so deleting a charger also deletes its persisted sessions.
- Charger summaries are sorted by most recent use. They include all-session count and active-session state, while average/latest speed and power plus estimated time-to-full use completed sessions only.
- Stored average power is preferred. When absent, comparison derives milliwatts from average current and average voltage; the UI falls back to current when power is unavailable.
- Historical comparison ranks chargers by average power, then average charging current, and displays per-charger latest/completed-test context. Profiles without history remain listed but do not enter the comparison chart.

---

## Network Detail

Network detail focuses on current connection state plus historical signal/latency data.

Connection identity and unavailable values:

- `ConnectionType` includes `WIFI`, `CELLULAR`, `ETHERNET`, `VPN`, and `NONE`. Transport resolution prefers Wi-Fi, cellular, Ethernet, then VPN; `isVpn` remains a separate flag so an underlying bearer and VPN can coexist.
- `NetworkState.defaultNetworkHandle` identifies the current default Android network. Callback state clears old capabilities/link properties when a different network becomes available.
- Wi-Fi RSSI is accepted only in `-126..-1` dBm. Link speed and frequency must be positive; unknown/sentinel values stay null.
- Ethernet and VPN without radio dBm receive the model's `GOOD` signal-quality fallback. This is a classification default, not measured radio strength; the UI must not invent dBm for them.

Main sections:

- Signal hero with `SignalBars`
- Wi-Fi name permission help card when SSID is unavailable
- Latency, link speed, frequency metrics
- Connection details card
- IP / DNS / MTU section
- Pro-only signal history chart
- Speed test summary card

Latency measurement:

- `GetMeasuredNetworkStateUseCase` combines the network state flow with periodic TCP latency measurements (30-second interval via `LatencyMeasurer`)
- Latency resets to null when disconnected or when `(connectionType, defaultNetworkHandle)` changes, including Wi-Fi-to-Wi-Fi handovers. `NetworkRepositoryImpl` binds measurement to the validated Android `Network` and discards the result if that network no longer matches afterward
- Approved outbound latency surface is TCP connect only to `BuildConfig.LATENCY_HOST` / `BuildConfig.LATENCY_PORT` (`locate.measurementlab.net:443` by default).

Historical chart behavior:

- Metrics: signal strength or latency
- Period selection is stored in ViewModel saved state
- Signal chart uses status gradient line (quality zone colors on the data line)
- Fullscreen chart route is available from the chart section
- The repository applies the same history periods as Battery. Free repository access is clamped to one day, Pro can request the selected period, and All is capped at 5,000 rows; the current UI exposes the persisted history chart only to Pro.

---

## Speed Test

Speed tests use M-Lab NDT7 only.

Flow:

1. Ready state with current connection context
2. Cellular warning dialog before test start when the active network is cellular
3. Ping phase
4. Download phase
5. Upload phase
6. Completed or failed state
7. Result history list

Stored result fields include:

- Download Mbps
- Upload Mbps
- Ping
- Jitter
- Server name/location
- Connection type and subtype
- Optional signal strength

Connection type (WiFi/Cellular) and network subtype are shown in both the latest result card and the history list with icons and labels (for example, WiFi with WiFi 6 or Cellular with 5G). Signal strength (dBm) is displayed in the latest result.

Free tier retains 5 results. Pro retains 100 results.

Implementation constraints:

- `SpeedTestService` uses `net.measurementlab.ndt7.android.NdtTest`.
- The service lets NDT7 auto-select the server; it does not hardcode a fixed test server.
- A validated internet connection is required before starting.
- Cellular starts are blocked with `CellularConfirmationRequired` until the user confirms.
- The active default network is locked at test start; network loss or connection identity changes fail the test instead of mixing measurements.
- Download and upload phases each use roughly 10 seconds of NDT7 progress.
- The ViewModel wraps the complete run in a 90-second timeout.
- Server metadata is extracted from NDT7 `ClientResponse.origin` / `ClientResponse.test` when present.
- `FinalizeSpeedTestUseCase` always calls `saveResultAndTrim`: the caller's free limit (currently 5) or a fixed 100 for Pro. Save and trim share the repository transaction path.
- Phase progress comes from NDT7 `ClientResponse.appInfo.elapsedTime` in microseconds divided by 10,000,000 and clamped to 0..1; it is not derived from time spent discovering a server or waiting for the first callback.

---

## Thermal Detail

Thermal detail surfaces both live thermal state and throttling history.

Main sections:

- Large numeric battery-temperature hero with textual thermal band; the screen does not call a thermometer component
- Heat strip
- Metrics grid
- Pro-only throttling log
- Pro-only thermal history chart
- Educational/info cards

Important implementation constraints:

- Thermal state comes from Android PowerManager APIs
- No sysfs-based thermal reads
- Session min/max values are tracked while the screen is active
- Free history access emits an empty list; Pro history supports the shared period set, with All capped at 5,000 rows.

---

## Storage Detail

Storage detail combines usage diagnostics, media breakdown, cleanup entry points, and storage health guidance.

Main sections:

- Usage hero ring
- Media permission card when needed
- Media breakdown segmented bar
- Cleanup tools section
- Pro-only storage history chart; used-space history is percentage-based with the UI-SPEC storage zones, while available-space history uses SI gigabytes
- Storage detail metrics
- Optional removable-storage section
- Educational/info cards

Permission behavior:

- Storage asks through `RuncheckPermissionPolicy.mediaPermissionsForApi()`.
- Android 14+ distinguishes full media access from selected visual media access through `MediaAccessState`.
- Media breakdown and cleanup affordances are shown only when the relevant media access exists.

Storage-specific data behavior:

- Aggregate app/data/cache bytes use `StorageStatsManager.queryStatsForUser(...)` and may be null without usage access or when Android denies the call.
- App count means distinct launchable packages visible to this app through `ACTION_MAIN` + `CATEGORY_LAUNCHER`, not all installed packages on the device.
- Encryption status comes from `DevicePolicyManager.storageEncryptionStatus`.
- File-system type is read from `/proc/mounts` for `/data`; the storage volume count includes mounted and read-only-mounted `StorageManager.storageVolumes` entries.
- Removable storage is reported only for mounted, non-primary, non-emulated volumes. Adopted storage is therefore not mislabeled as a portable SD card, and multiple portable volumes are aggregated only when every capacity read succeeds.
- Free history access emits an empty list; Pro history supports the shared period set, with All capped at 5,000 rows.

Cleanup tool entry points:

- Large Files
- Old Downloads
- APK Files
- Trash is not a `cleanup/{type}` route; Storage shows trash info and empties trash through a separate API 30+ MediaStore delete request path when trashed media exists.

Free tier behavior:

- Storage screen itself is available
- Cleanup tools are gated behind a Pro callout

---

## Cleanup Screen

Cleanup is a shared route driven by `cleanup/{type}` and `CleanupType`.

Supported cleanup types:

- `LARGE_FILES`
- `OLD_DOWNLOADS`
- `APK_FILES`

Filter behavior:

- Large files: 10 / 50 / 100 / 500 MB
- Old downloads: 30 / 60 / 90 days / 1 year
- APK files: no chip filters

UI and data behavior:

- Scanning, empty, results, deleting, and success states
- Grouped file results by media category
- Expand/collapse per category
- Selection by file or whole group
- Paging-backed item loading per group
- Paging page size: 40
- API 30+ delete flow uses system delete request / intent sender
- API 30+ delete flow splits selections into system requests of at most 2,000 URIs and verifies the final MediaStore state before reporting freed bytes
- Android 9 and below deletion uses `StorageCleanupHelper.deleteLegacy` after an app confirmation dialog.
- Android 10 uses the same legacy delete call after app confirmation, then launches the per-item
  `RecoverableSecurityException` consent action for non-owned MediaStore items before retrying.
- Old Downloads and APK cleanup are version-restricted to API 30+ in `CleanupViewModel`
- APK cleanup preselects all groups by default
- Old Downloads keeps one scan-start timestamp across its summary, pages, and group-selection resolution so age boundaries do not drift during a scan
- Selected filter is stored through `SavedStateHandle`
- Route is Pro-gated in the ViewModel; non-Pro users receive a locked error state before scanning
- Storage, thermal, network, and battery trend sections share `HistoryPeriodFilterChipRow`, `HistoryLoadErrorMessage`, and `ChartStatsRow` for period chips, history-load failures, and min/avg/max chart stats.

Deletion and paging boundaries:

- An absent or invalid route `type` yields an error before scanning. The fallback enum used to render a title is not authorization to scan Large Files.
- The Storage screen hides cleanup entry points unless `hasAllMediaPermissions` is true. Selected visual access is tracked separately and must not be treated as full access.
- APK summaries from multiple collections are merged into one APK category, summing item counts/bytes and taking the maximum file size.
- `CleanupPagingSource` rethrows cancellation and exposes actual load failures through Paging. `CategoryGroup` renders refresh/append failures and retry controls, keeping a provider error distinct from an empty group.
- Resolving a selected group into URIs can fail; the ViewModel reports an error and does not start deletion with a partial inferred selection.
- Legacy deletion and API 30+ consent both verify remaining MediaStore URIs against the original selection and captured sizes. If every selected URI remains, the selection is restored with a failure message. Partial deletion preserves remaining selection; freed bytes are based on verified disappearance.
- An accepted system dialog alone is not proof of deletion. Verification failure or lost access must not produce an invented successful freed-byte count.
- During the success overlay, the underlying Scaffold clears its semantics so accessibility focus does not remain on obscured controls.

---

## App Usage

App Usage is Pro-gated and backed by paging.

Behavior:

- Locked route redirects to Pro Upgrade for free tier
- Uses usage-access permission instead of normal runtime permission
- Shows permission education card when access is missing
- Refreshes usage snapshot when the user returns from system settings
- Displays total foreground time summary and per-app list items
- Uses a fixed 24-hour lookback and a paging page size of 40.
- Snapshot collection resumes from the last collection timestamp but clamps the collection window to at most 24 hours.
- Production snapshot collection stores foreground duration and leaves `estimatedDrainMah` null. The column/domain property remains for schema compatibility, and the UI renders mAh only if legacy or test data provides a value; foreground time is not converted into fabricated battery drain.

Background support:

- Usage snapshots are collected periodically in maintenance work only while purchased Pro access is active
- Data is persisted and then paged back into the UI

Collection is serialized with a repository `Mutex`. `AppBatteryUsageDao.replaceSnapshotsAfter(startTime, usages)` transactionally deletes rows strictly after the collection start and inserts the replacement interval before the DataStore cursor advances. Room and DataStore do not share a transaction: retry after a cursor-write failure re-collects and replaces that interval. UI aggregation sums foreground time per package, selects the latest label by timestamp/id, and sorts by descending duration then package name. This is interval-snapshot accounting, not exact clipping of every historical event to the displayed 24-hour boundary.

---

## Learn

Learn is a lightweight educational content flow built entirely in-app.

Screens:

- Learn topic list screen
- Learn article detail screen

Behavior:

- Articles are grouped by topic
- Current catalog size: 15 articles
- Current topics: Battery, Temperature, Network, Storage, General
- Article detail renders structured body text from catalog resources
- Some articles expose cross-link buttons into app routes
- Cross-link routes are validated at startup, and legacy article IDs can alias to canonical IDs.
- Read/unread state is not persisted.

---

## Fullscreen Chart

Fullscreen charts are launched from battery and network trend sections.

Behavior:

- Landscape-only while the route is visible
- Supports battery history, battery session, and network history sources through `FullscreenChartSource`
- Metric and period chip selections are stored in `SavedStateHandle`
- Uses the same chart data model, tooltip formatting, and "Instrument Sweep" animation as embedded charts
- The route itself is `fullscreen_chart/{source}/{metric}/{period}`.
- Pro lock is applied for `BATTERY_HISTORY` and `NETWORK_HISTORY`; `BATTERY_SESSION` remains available as the current-session fullscreen source.
- Selection changes are sent back to the previous route with `FullscreenChartResult.KEY_SOURCE`, `KEY_METRIC`, and `KEY_PERIOD`.

---

## Settings

Settings is broader than a simple preferences page and covers monitoring, privacy, export, purchase flow, and device capability info.

Sections:

- Monitoring interval
- Live Notification
  - master toggle (opt-in, disabled by default)
  - per-metric toggles: current, drain rate, temperature, screen stats, remaining time
  - starts/stops `RealTimeMonitorService` foreground service
- Notifications
  - master notifications toggle
  - low battery
  - high temperature
  - low storage
  - charge complete
- Alert thresholds
  - low battery threshold
  - temperature threshold
  - low storage threshold
- Display
  - Celsius / Fahrenheit
  - Show/hide info cards toggle
- Data
  - data retention
  - CSV export
  - reset info tips
  - clear speed test results
  - clear all data
  - all destructive actions require confirmation dialog
- Pro
  - current Pro status
  - upgrade CTA
  - restore purchase
- Device
  - device model
  - API level
  - current-now reliability
  - cycle-count availability
  - thermal zone count
- Debug Insights
  - visible only when the injected `InsightDebugActions` implementation reports availability
  - debug builds can seed deterministic demo insight data, regenerate insights from local data, and clear insight rows
  - release builds bind `ReleaseSafeInsightDebugActions`, return unavailable/no-op behavior, and ship empty non-translatable release strings for this section
- About
  - version
  - Play Store link
  - privacy policy
  - feedback email intent

Notes:

- Notification permission handling is built into Settings for Android 13+ through `RuncheckPermissionPolicy`
- Export shares one or more CSV files through `FileProvider`
- All destructive actions (clear speed tests, clear all data, reset tips, reset thresholds) show a confirmation AlertDialog with primary blue confirm button

---

## Pro Upgrade Screen

The Pro Upgrade route is the purchase presentation surface for the single permanent in-app product.

Current behavior:

- Free state lists every `ProFeature` entry with an outlined icon and localized label.
- The purchase button is disabled until Billing reports availability. It shows the formatted Play price when available and uses price-independent copy when price lookup fails. The ViewModel attempts price lookup when availability becomes true and no formatted price has been stored; cancellation is rethrown.
- Purchase launch requires an `Activity`; the ViewModel clears any prior purchase error before calling `ProPurchaseManager.launchPurchaseFlow(activity)`.
- Pending purchase state is shown separately and does not unlock Pro. Billing errors are mapped to app-owned strings by `BillingManager` before the ViewModel exposes them as `UiText`.
- `PurchaseEvent.Success` sets the purchase-completion flag, clears pending/error state, and drives `PurchaseThankYouContent`. A cached/restored Pro state emission alone does not trigger the purchase-completion sheet. Dismissal clears the flag and returns to the prior route.
- Already purchased state shows a static active/thank-you surface. Purchase restoration is owned by Settings rather than duplicated on this route.
- The UI enumerates `ProFeature.entries`, while actual entitlement remains the single `ProState.isPro` decision; adding an enum entry requires both localized label and icon mappings to stay exhaustive.

---

## CSV Export

`domain/usecase/ExportDataUseCase.kt` checks Pro independently of the Settings UI and creates four files:

| File | CSV columns |
|------|-------------|
| `runcheck_battery.csv` | timestamp, level, voltage_mv, temperature_c, current_ma, current_confidence, status, plug_type, health, cycle_count, health_pct |
| `runcheck_network.csv` | timestamp, type, signal_dbm, wifi_speed_mbps, wifi_frequency, carrier, network_subtype, latency_ms |
| `runcheck_thermal.csv` | timestamp, battery_temp_c, cpu_temp_c, thermal_status, throttling |
| `runcheck_storage.csv` | timestamp, total_bytes, available_bytes, apps_bytes, media_bytes |

Each category reads persisted history and filters by the selected data-retention duration; it is not limited to the chart's 5,000-row cap. Categories are read sequentially rather than in one cross-category snapshot transaction. Missing nullable values become empty CSV cells, timestamps use ISO offset date/time with the formatter's system time zone, and text fields containing commas, quotes, CR, or LF are CSV-escaped. Thermal values remain Celsius regardless of the display preference. Export does not include insights, app-usage rows, charger sessions, or speed-test history.

`data/export/FileExportRepositoryImpl.kt` serializes cache operations with a Mutex, validates single `.csv` filenames without path separators, writes UTF-8 files into a unique staging directory, and requests an atomic move to a unique export directory. It returns FileProvider URI strings only after preparation; failed preparation attempts to remove staging/export directories. Old exports older than 15 minutes are cleaned on a subsequent preparation, not by an independent expiry timer. Review atomic-move support, partial cleanup, CSV consumers, and actual URI sharing separately from file generation.

---

## Persistence

Primary persisted data:

- Battery readings
- Network readings
- Thermal readings
- Storage readings
- Throttling events
- Speed test results
- Charger profiles / sessions
- App usage snapshots
- Device profile info
- Insight rows with rule id, dedupe key, priority, confidence, seen/dismissed state, and expiry window
- User preferences and dismissed info cards

Persistence technologies:

- Room database name: `runcheck.db`
- Room schema version: 10
- Room for telemetry/history, charger, app-usage, speed-test, and insight entities
- Room schema export is enabled and androidTest assets include `app/schemas`; exported schema assets currently cover versions 6-10
- Room migrations are explicitly registered from 1→2 through 9→10
- A destructive migration callback can log debug-only and record `destructive_migration_occurred` in `runcheck_db_events`, but the builder does not enable `fallbackToDestructiveMigration`; an unregistered migration is expected to fail rather than erase data.
- DataStore `settings` for user preferences, dismissed info cards, selected charger, and app-usage collection timestamp
- DataStore `monitoring_status` for the last successful periodic worker heartbeat's wall time, awake uptime, and monitoring interval
- DataStore `monitoring_alert_state` for the previous alert snapshot and charge-complete debounce state
- SharedPreferences `pro_status_cache` for synchronous cached purchase status during release cold start

Corruption and reset boundaries:

- The three preference DataStores (`settings`, `monitoring_status`, `monitoring_alert_state`) use `ReplaceFileCorruptionHandler { emptyPreferences() }`. Corrupt serialized preferences revert to defaults/empty state; this does not claim that every I/O failure is recoverable.
- A wrong-type Boolean in `ProStatusCache` is removed and returns false. `ProManager.initialize()` applies the currently cached purchase value before awaiting purchase-status readiness, then follows subsequent purchase state.
- `ClearMonitoringDataUseCase` deletes readings, throttling events, app usage, speed tests, insights, and charger profiles/sessions in one Room transaction. Device profile rows and the purchase cache are not in this deletion list.
- `MonitoringDataCoordinator` serializes that transaction and debug history seeding with the complete insight read/evaluate/publish operation. Reset waits for admitted generation before deleting its results; later generation reads the remaining or newly collected history. Lock order is coordinator, thermal tracker, then Room. The tracker invalidates its cached active event in `finally` before releasing its lock, including failed or cancelled resets, and restores any surviving Room event on the next observation. Lock waits remain cancellable; monitoring schedules are unchanged.
- After the Room transaction, it separately clears monitoring-related preference state, alert/debounce state through `MonitoringAlertStateRepository`, worker heartbeat, and prepared exports. All non-cancellation cleanup steps are attempted; the first failure is rethrown with subsequent failures suppressed. The overall reset is therefore not one transaction spanning Room, preferences, and files.

Room tables/entities:

| Table | Entity | Purpose |
|-------|--------|---------|
| `battery_readings` | `BatteryReadingEntity` | Battery history |
| `network_readings` | `NetworkReadingEntity` | Network signal/latency history |
| `thermal_readings` | `ThermalReadingEntity` | Thermal history |
| `storage_readings` | `StorageReadingEntity` | Storage usage history |
| `throttling_events` | `ThrottlingEventEntity` | Thermal throttling log |
| `charger_profiles` | `ChargerProfileEntity` | User charger labels |
| `charging_sessions` | `ChargingSessionEntity` | Charging session measurements |
| `app_battery_usage` | `AppBatteryUsageEntity` | Per-app foreground-time snapshots; nullable estimated-drain field retained for schema compatibility but not populated by production collection |
| `speed_test_results` | `SpeedTestResultEntity` | NDT7 speed test history |
| `devices` | `DeviceEntity` | Current device profile JSON |
| `insights` | `InsightEntity` | Persisted rule-driven insight rows |

Migration history:

| Migration | Main change |
|-----------|-------------|
| 1→2 | Adds `throttling_events` |
| 2→3 | Adds charger profiles and charging sessions |
| 3→4 | Adds app battery usage |
| 4→5 | Recreates network readings with nullable `signal_dbm` |
| 5→6 | Adds speed test results |
| 6→7 | Adds battery status/timestamp and charging session end-time indexes |
| 7→8 | Adds app-usage package/timestamp composite index |
| 8→9 | Drops redundant package-name-only app-usage index |
| 9→10 | Adds `insights` table and indexes |

Default preference values:

| Setting | Default |
|---------|---------|
| Monitoring interval | 30 minutes |
| Notifications master toggle | Enabled |
| Data retention | 3 months |
| Low battery alert | Enabled, threshold 20% |
| High temperature alert | Enabled, threshold 42°C |
| Low storage alert | Enabled, threshold 90% |
| Charge-complete alert | Disabled |
| Temperature unit | Celsius |
| Live notification | Disabled |
| Live current/drain/temp lines | Enabled |
| Live screen stats/remaining time lines | Disabled |
| Info cards | Enabled |

---

## Monetization Model

Pro state is handled through `BillingManager`, `ProManager`, and `ProState`.

Current product behavior:

- Free is the default state and never receives time-limited Pro access
- Purchased Pro unlocks all Pro features permanently
- No subscriptions
- Debug builds force `BillingManager` Pro state to active for development
- Release builds use Google Play Billing one-time `INAPP` product id `runcheck_pro`
- App startup cancels legacy day-5/day-7 trial notification work and removes the obsolete trial notification channel from older installs
- Debug builds can override the product id with `RUNCHECK_PRO_PRODUCT_ID`; release builds keep the checked-in product id so release artifacts are reproducible.
- Pending purchases are tracked separately and do not unlock Pro until purchased
- Purchased but unacknowledged purchases are acknowledged with up to 3 retries
- A purchased item unlocks Pro only after it was already acknowledged or acknowledgement succeeds; exhausted acknowledgement retries keep the entitlement inactive and emit a safe generic purchase error
- Billing 9 purchase sub-response codes distinguish insufficient funds and user ineligibility; unknown codes fall back to a known response-code message or a generic localized error without exposing SDK debug text
- Cached Pro state is restored synchronously in release builds to avoid a free-tier flash while Billing queries run
- Billing availability remains false during service setup/product refresh and reconnectable purchase-query failures. Cached product details alone do not make checkout available while the billing service is reconnecting.

Pro-gated areas currently include:

- Charger Comparison
- App Usage
- Extended history
- Thermal logs
- CSV export
- Widgets
- Remaining charge time
- Storage cleanup route

`ProFeature` enum values:

- `EXTENDED_HISTORY`
- `CHARGER_COMPARISON`
- `PER_APP_BATTERY`
- `WIDGETS`
- `CSV_EXPORT`
- `THERMAL_LOGS`
- `REMAINING_CHARGE_TIME`
- `STORAGE_CLEANUP`

All current features share the single `ProState.isPro` decision. The per-feature enum remains in place so a future per-feature gate can be added without changing all UI call sites.

Current enforcement map:

| Surface | Primary enforcement |
|---------|---------------------|
| Charger Comparison, App Usage | Navigation redirect plus feature ViewModel/use-case checks |
| Battery/Network persisted history | UI lock/visibility plus repository period clamping; fullscreen history route also checks Pro |
| Thermal/Storage history | UI lock/visibility plus free history flows that emit no rows |
| Thermal throttling log | Pro UI state/use-case access |
| Storage cleanup | Storage callout plus `CleanupViewModel` fail-closed check before scanning |
| App-usage background collection | `RefreshAppUsageSnapshotUseCase` reads the current Pro state and returns without collecting unless it is already purchased Pro; unlike retention cleanup, this path does not wait for Pro-status readiness |
| CSV export | Settings/ViewModel Pro check before export |
| Widgets | Pro-driven refresh/access state |
| Remaining charge time | Pro presentation gate |
| Home Insights | Not Pro-gated; only insight destinations are filtered when their target feature is protected |

The top-level route set marks only `charger` and `app_usage` as direct Pro-only destinations. That is not evidence that every other feature is free: several screens contain a mix of free live diagnostics and Pro-only history/tools, so reviews must inspect the feature-level gate as well as navigation.

---

## Security and Privacy Surface

Manifest and network posture:

- `android:allowBackup="false"`
- `android:dataExtractionRules="@xml/data_extraction_rules"` and `android:fullBackupContent="@xml/backup_rules"` exclude root, files, databases, shared preferences, and external app data from cloud backup and device transfer rules.
- `android:usesCleartextTraffic="false"`
- `network_security_config.xml` permits system trust anchors only and disallows cleartext traffic.
- Manifest package visibility is limited to an `ACTION_MAIN` + `CATEGORY_LAUNCHER` `<queries>` intent for launcher-app visibility; the app does not request `QUERY_ALL_PACKAGES`.
- `androidx.startup.InitializationProvider` remains merged for non-WorkManager App Startup components; `androidx.work.WorkManagerInitializer` is removed so WorkManager uses `RuncheckApp`'s explicit `Configuration.Provider`.
- Main launcher activity is exported by design.
- App widget receivers are not exported and are protected with `android.permission.BIND_APPWIDGET`.
- `RealTimeMonitorService` is not exported and uses `foregroundServiceType="specialUse"` with a declared special-use subtype.
- FileProvider is not exported and exposes only cache path `exports/` as `csv_exports`.

Declared permission surface:

| Permission | Purpose |
|------------|---------|
| `ACCESS_NETWORK_STATE` | Network type/capability detection |
| `ACCESS_WIFI_STATE` | Wi-Fi connection details |
| `INTERNET` | User-initiated NDT7 speed tests and TCP latency measurement |
| `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` | Optional Wi-Fi SSID/BSSID visibility; requested together so Android can offer precise access |
| `POST_NOTIFICATIONS` | Android 13+ notifications |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Opt-in live monitoring notification |
| `RECEIVE_BOOT_COMPLETED` | Reschedule monitoring after boot/package replacement/unlock |
| `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VISUAL_USER_SELECTED` | Android 13+ media breakdown/cleanup, including Android 14+ selected visual media state |
| `READ_EXTERNAL_STORAGE` maxSdk 32 | Android 12 and below media fallback |
| `WRITE_EXTERNAL_STORAGE` maxSdk 28 | Android 9 and below legacy delete fallback |
| `PACKAGE_USAGE_STATS` | App Usage foreground time and aggregate app/cache storage stats through `StorageStatsManager` |
| `READ_BASIC_PHONE_STATE` | Android 13+ cellular network generation fallback |

Runtime permission decisions are centralized in `RuncheckPermissionPolicy` for Wi-Fi detail location permissions, Android-version-specific media permission lists, Android 14+ partial visual media access, and Android 13+ notification permission checks.

Approved outbound network surfaces:

- User-initiated M-Lab NDT7 speed tests
- TCP latency measurement to the configured latency host/port
- Google Play Billing

Telemetry/logging boundary:

- Release Sentry init is a no-op.
- Debug Sentry disables tracing, profiling, auto session tracking, breadcrumbs, activity lifecycle tracing, frames tracking, screenshots, view hierarchy, NDK, performance v2, and auto trace id generation.
- `ReleaseSafeLog` emits Android logs only in debug builds.
- Semgrep has project-specific rules for backup, cleartext, exported components, broad FileProvider paths, sensitive Android logging, outbound network primitives, and release telemetry expansion.

---

## Future Considerations

- **Learn article read/unread tracking:** Currently no persistence of which Learn articles the user has read. With only 15 articles this isn't needed yet, but if the catalog grows significantly (30+), consider adding DataStore-backed read state with visual indicators (e.g., unread dot on `LearnArticleCard`).

---

## Brand & Design System

Single dark theme — no light mode, no AMOLED toggle, no dynamic colors.

Visual decision constraints implemented by the current system:

- Use Material `Icons.Outlined` (including auto-mirrored outlined variants) throughout; the current Kotlin source contains no Default, Filled, or Rounded icon family usage.
- Gauge/ring tracks and arcs stay neutral; semantic or category color belongs on the indicator, small status mark, label, or deliberately defined Home tile fill.
- Status meaning is always paired with text, an icon, or chart semantics; color alone is insufficient.
- Shared screens should use `ContentContainer`, theme spacing, `UiTokens`, shared cards, and shared chart components instead of introducing per-screen layout constants.
- User-facing copy belongs in resources. The current product deliberately filters resources to English only.
- The 4dp spacing grid is the default. The centralized 2dp micro-spacing token and the exact Home status-tile dimensions centralized in `UiTokens` are explicit visual-system exceptions; `UI-SPEC.md` sections 4.2 and 9.1 mirror those values and their accessibility-driven minimum-height behavior.

### Color Palette

**Backgrounds:**

| Token | Hex | Material3 Role | Usage |
|-------|-----|----------------|-------|
| BgPage | `#0B1E24` | `background`, `surface` | Page background |
| BgCard | `#133040` | `surfaceContainer` | Card backgrounds |
| BgCardDeep | `#0D2530` | — | Deeper card surfaces where explicitly used |
| BgCardAlt | `#0F2A35` | `surfaceContainerHigh` | Info cards, elevated surfaces |
| BgIconCircle | `#1A3A48` | `surfaceContainerHighest`, `surfaceVariant` | Icon circle backgrounds |

**Accents:**

| Token | Hex | Material3 Role | Usage |
|-------|-----|----------------|-------|
| AccentBlue | `#4A9EDE` | `primary` | Primary accent, buttons, links, brand |
| AccentTeal | `#5DE4C7` | `secondary` | Healthy status, positive values |
| AccentAmber | `#E8C44A` | `tertiary` | Fair status, warnings |
| AccentOrange | `#F5963A` | — | Poor status |
| AccentRed | `#F06040` | `error` | Error text and destructive actions |
| StatusCritical | `#F66A4C` | — | Critical status text and indicators on cards |
| AccentLime | `#C8E636` | — | Storage video category |
| AccentYellow | `#F5D03A` | — | Storage audio category |

**Home palette, scoped by `HomeTheme.kt`:**

| Token | Hex | Usage |
|-------|-----|-------|
| HomeBackground | `#10110F` | Page |
| HomeSurface | `#20211D` | Insight cards |
| HomeCream | `#E9E6DC` | Battery and App usage |
| HomePeach | `#EDA079` | Thermal, Full Check, gauge |
| HomeStone | `#A3A198` | Network and Speed test |
| HomeGraphite | `#5C5D57` | Storage and Learn |

These fixed category fills do not encode severity. Dark text appears on light
surfaces; cream text appears on graphite. Text badges use actual verdicts.
The old `Category*` and `Tile*` declarations are not imported by Home.

**Text:**

| Token | Hex | Material3 Role | Usage |
|-------|-----|----------------|-------|
| TextPrimary | `#E8E8ED` | `onSurface`, `onBackground` | Main text |
| TextSecondary | `#90A8B0` | `onSurfaceVariant` | Labels, descriptions |
| TextMuted | `#7A949E` | `outline`, `outlineVariant` | Hints, dividers, disabled text |
| TextOnLime | `#1A2E0A` | — | Text on lime-colored backgrounds |

### Status Colors

Used via `MaterialTheme.statusColors` extension. Always paired with icons or text labels for accessibility.

| Status | Color | Score / direct metric mapping |
|--------|-------|-------------------------------|
| Healthy | AccentTeal `#5DE4C7` | Subsystem/overall score 75–100; temperature <35°C; storage used <75%; signal Excellent/Good |
| Fair | AccentAmber `#E8C44A` | Score 50–74; temperature 35–39.9°C; storage 75–84%; signal Fair |
| Poor | AccentOrange `#F5963A` | Score 25–49; temperature 40–44.9°C; storage 85–94%; signal Poor |
| Critical | StatusCritical `#F66A4C` | Score 0–24; temperature ≥45°C; storage ≥95%; No Signal |

Battery Home status is derived from the battery subsystem score, not directly from battery level. Temperature, storage, and signal also have direct presentation mappings in `StatusColors.kt`; do not substitute the overall-score thresholds for those metric-specific helpers.

**Confidence badges:**

| Badge | Background | Text |
|-------|-----------|------|
| Accurate | AccentBlue `#4A9EDE` | BgPage `#0B1E24` |
| Estimated | AccentAmber `#E8C44A` | BgPage `#0B1E24` |
| Unavailable | TextMuted `#7A949E` | TextPrimary `#E8E8ED` |

### Typography

**Font families:**
- **Manrope** — all body text, headers, labels (`MaterialTheme.typography`)
- **JetBrains Mono** — numeric displays, values, charts (`MaterialTheme.numericFontFamily`)

**Type scale (Manrope):**

| Style | Size | Weight |
|-------|------|--------|
| displayLarge | 48sp | Bold, -0.04em tracking |
| displayMedium | 36sp | Bold |
| displaySmall | 28sp | SemiBold |
| headlineLarge | 20sp | SemiBold |
| headlineMedium | 16sp | SemiBold |
| headlineSmall | 14sp | SemiBold |
| titleLarge | 20sp | Medium |
| titleMedium | 16sp | Medium |
| titleSmall | 14sp | Medium |
| bodyLarge | 15sp | Normal |
| bodyMedium | 14sp | Normal |
| bodySmall | 13sp | Normal |
| labelLarge | 12sp | SemiBold, 0.08em tracking |
| labelMedium | 10sp | SemiBold |
| labelSmall | 10sp | Medium |

**Numeric text styles (JetBrains Mono):**

| Style | Base | Size | Usage |
|-------|------|------|-------|
| numericHeroDisplayTextStyle | displayLarge | 64sp Bold, -3sp tracking | Primary large hero displays |
| numericHeroDisplayUnitTextStyle | headlineLarge | 28sp SemiBold | Units next to primary large hero displays |
| numericHeroValueTextStyle | displayLarge | 48sp Bold | Large hero values |
| numericHeroLargeValueTextStyle | displayLarge | 54sp | Battery level display |
| numericHeroLevelTextStyle | displayLarge | 48sp Bold, -2sp tracking | Compact hero values |
| numericHeroUnitTextStyle | headlineLarge | 20sp SemiBold | Units next to hero values |
| numericRingValueTextStyle | displayMedium | 32sp Bold | ProgressRing center value |
| numericSpeedHeroValueTextStyle | displaySmall | 40sp | Speed test hero display |
| numericMetricDisplayTextStyle | displayLarge | 48sp Bold, -3sp tracking | Secondary hero numbers (dBm, latency) |
| chartAxisTextStyle | labelSmall | 12sp | Chart axis labels |
| chartTooltipTextStyle | bodySmall | 13sp | Chart tooltip values |

**Home-specific centralized text styles:**

| Style | Font / size | Purpose |
|-------|-------------|---------|
| `homeHealthScoreTextStyle` | JetBrains Mono, 52sp Bold | Segmented-gauge health score |
| `homeHealthScoreUnitTextStyle` | JetBrains Mono, 20sp SemiBold | `/100` unit inside the gauge |
| `homeHealthStatusTextStyle` | Manrope, 18sp SemiBold | Overall textual status inside the gauge |
| `homeHealthContextTextStyle` | Manrope, 14sp | Update-age context |
| `homeStatusTileTypeScale` | Manrope: category 15sp SemiBold, value 40sp Bold (Thermal/Storage 30sp, Network 28sp), suffix 13sp SemiBold, status 12sp SemiBold | Shared scale for every Home status tile |

### Shapes & Spacing

**Corner radii:**

| Shape | Radius | Usage |
|-------|--------|-------|
| large | 16dp | Cards, panels, dialogs |
| medium | 8dp | Badges, chips, small elements |
| small | 8dp | Compact elements |
| extraLarge | 50% | Circles (icons, avatars) |

Home status tiles are an explicit visual-system exception to the normal 16dp card radius and default spacing grid: `homeStatusTileCornerRadius = 20dp`, with current geometry centralized in `UiTokens` and mirrored in `UI-SPEC.md`. Do not duplicate those values in screen code.

**Spacing grid (4dp base):**

| Token | Value | Usage |
|-------|-------|-------|
| xxs | 2dp | Micro gaps, baseline alignment |
| xs | 4dp | Tight gaps |
| sm | 8dp | Small gaps, inter-row spacing |
| md | 12dp | Between cards, standard gaps |
| base | 16dp | Card padding, section spacing |
| lg | 24dp | Between sections |
| xl | 32dp | Page margins, large separations |

**Dividers:** `outlineVariant.copy(alpha = 0.35f)` — no hardcoded colors.

Shared UI dimensions such as touch targets, icon sizes, button heights, outline width, and Pro lock/badge alpha values live in `UiTokens`.

Current shared dimensions:

| Token group | Values |
|-------------|--------|
| Touch target | 48dp minimum |
| Standard icons | 12, 16, 18, 20, 24, and 32dp |
| Icon circle | 44dp outer / 22dp inner |
| Compact and dialog icon containers | 36dp / 64dp |
| Celebration icon | 80dp |
| Primary / compact button height | 56dp / 52dp |
| Home primary action height | 48dp |
| Home status-tile gap / corner / minimum height | 8dp / 20dp / 128dp regular, 112dp compact |
| Home tile content flow | vertical padding 12dp regular or 8dp compact, content gap 4dp, value/suffix gap 4dp |
| Home tile horizontal padding | 16dp, plus 20dp on curved Battery/Thermal sides |
| Home compact hero / tool layout | 40dp hero inset; 76dp tool minimum with 12dp vertical padding |

`ContentContainer` centers screen content and caps it at 600dp on tablets/foldables. Standard cards use the flat `surfaceContainer` color with no elevation; hero cards use `BgCardDeep`. Borders are generally absent, with `ActionCard` as the intentional 1dp `outlineVariant` at 35% alpha exception.

### Motion and reduced motion

| Motion token | Duration | Primary use |
|--------------|---------:|-------------|
| `SHORT` | 200ms | Micro-interactions |
| `MEDIUM` | 300ms | Navigation slide/fade |
| `SWEEP` | 800ms | Chart sweeps and segmented bars |
| `RING` | 1200ms | Rings and gauges |
| `CONTINUOUS` | 2000ms | Continuous indicators and heat-strip loops |
| `SCROLL` | 150ms | Live-chart interpolation |
| Fullscreen enter scale / fade | 260ms / 220ms | Fullscreen chart entry |
| Fullscreen exit | 180ms | Fullscreen chart exit |
| Speed gauge / sweep / result | 1700ms / 1800ms / 700ms | Speed-test presentation |

`RuncheckTheme` derives reduced motion from the global animator-duration scale being exactly zero and exposes it through `LocalReducedMotion` / `MaterialTheme.reducedMotion`. Animated components and navigation must skip or collapse motion when that flag is true; bare duration literals should not replace centralized `MotionTokens`.

`Theme.kt` reads the setting initially and registers a `ContentObserver` on `ANIMATOR_DURATION_SCALE`, updating composition when it changes. Disposal unregisters the observer. A failed initial read defaults to false; failed registration retains the last read value. Reduced motion is therefore reactive when the device permits observation.

### Responsive layout and interaction reference

The following are source-defined branches, not screenshot or device-test results from this refresh:

| Surface / source | Current responsive or interaction contract |
|------------------|-----------------------------------------------|
| `ui/home/HomeScreen.kt` | Compact mode requires available height <840dp and font scale <1.5. At font scale >=1.5 the regular scrollable layout remains, and gauge text uses its expanded arrangement. Gauge geometry uses aspect ratio 1.9, 36dp band, 2dp edge inset, 22 segments, 168-degree sweep, and 20% angular gaps. |
| `ui/home/HomeStatusTiles.kt` | Single column if font scale >=1.5 or available width / font scale <320dp. Otherwise Battery/Thermal weights are 0.55/0.45 with -12dp layout spacing to accommodate complementary shapes; Storage/Network weights are 0.63/0.37 with an 8dp gap. Single-column order is Battery, Thermal, Storage, Network. |
| `ui/home/HomeTileShape.kt` | Independent clickable tiles use custom Path outlines for Battery, Thermal, Learn, and Speed. Nominal radius and bend are 20dp; radius is capped by tile size. Curved-side padding and shaped gaps must be reviewed together with touch behavior. |
| `ui/home/HomeSecondarySections.kt` | Tool mosaic uses the same >=1.5 font-scale / <320dp effective-width fallback. Compact tools omit descriptions and retain their icon, title, navigation, and App usage Pro badge. |
| `ui/components/ProgressHeroMetric.kt` | At font scale >=1.5, the 100dp ring moves above the metric content; normal layout places them side by side. The value/unit inside the metric content still share a row. |
| `ui/network/NetworkDetailScreen.kt` | Signal and latency readouts stack at font scale >=1.5. Each readout keeps its own value/unit row; null values remain absent. |
| `ui/network/SpeedTestScreen.kt` | Metric groups and result history use stacked arrangements at font scale >=1.5. |
| `ui/thermal/ThermalDetailScreen.kt` | At font scale >=1.5, temperature and unit stack, temperature switches to the smaller hero-value style, and thermal metric pairs become vertically arranged. |
| `ui/storage/StorageDetailScreen.kt`, `ui/settings/SettingsScreen.kt` | Hero/device metric groups stack at font scale >=1.5 through `MetricPillItem` / `MetricPillItems`. These shared renderers preserve labels, values, colors, and optional info actions. |
| `ui/components/MetricRow.kt` | Copyable rows set a 48dp minimum height and an accessibility click label, copy the full value, and show feedback. `maxLines` controls ellipsis; text truncation does not alter copied content. Descendant semantics are merged. |
| `ui/fullscreen/FullscreenChartScreen.kt` | Requests sensor landscape for the visible route and returns to unspecified orientation on disposal. Metric/period controls remain available in Success, Empty, and Error states; Locked/Loading hide them. |

Home additionally overrides status healthy/poor/critical to `#8AD5AE`, `#E98548`, and `#F16C54`, with muted/neutral/unavailable `#B5B5AE`; fair remains inherited. `HomeTheme` overrides the large shape to 20dp and selected Manrope typography styles while retaining the numeric font composition local. These scoped overrides do not redefine the other routes' base palette.

Text formatting lives in `ui/common/UiFormatters.kt`: `appDisplayLocale()` is English, decimal/date helpers use it, and Android file-size formatting receives an English configuration context. Celsius remains the measurement/storage basis; Fahrenheit is a display conversion. Product copy uses resources, and separators should use punctuation or layout rather than U+00B7.

**Contrast:** Minimum 4.5:1 body text, 3:1 large text (WCAG AA). Minimum touch target 48dp.

### Logo

Health-score arc (~210°) wrapping a phone silhouette, rendered in AccentBlue.
The repository root contains `runcheck-logo.svg` and `icon.png`; Android launcher assets live under the normal `app/src/main/res/drawable*` and `mipmap*` resource directories. There is no root `icons/` directory in this snapshot.

---

## Testing and Verification

Current test surface:

- Unit tests: 126 Kotlin files under `app/src/test/java/com/runcheck/` in this working-tree snapshot
- Debug unit tests: 2 Kotlin files under `app/src/testDebug/java/com/runcheck/`
- Instrumented tests: 2 Kotlin files under `app/src/androidTest/java/com/runcheck/`
- Android test assets include exported Room schemas for migration tests; current exported assets cover versions 6-10.
- Shared coroutine main dispatcher rule lives in `ui/MainDispatcherRule.kt`.
- Home-specific unit coverage includes minute-age clamping, reference gauge-segment fills and score clamping, fixed status-tile order, compact-height/font-scale boundaries, refresh source reuse, the 900ms minimum indicator, the 12-second timeout, failure reset, and lifecycle-stop reset.

Named regression sources useful for review-question generation (under `app/src/test/java/com/runcheck/`; presence does not imply execution during this refresh):

| Contract | Test source |
|----------|-------------|
| Valid zero cycles / invalid cycles | `data/battery/BatteryCycleCountTest.kt` |
| Current validity and database write failures | `data/battery/GenericBatterySourceTest.kt`, `data/battery/BatteryRepositoryImplTest.kt` |
| Wrong-type cached entitlement / initial Pro state | `data/billing/ProStatusCacheTest.kt`, `pro/ProManagerTest.kt` |
| Billing readiness / purchase-success UI | `data/billing/BillingManagerApiContractTest.kt`, `ui/pro/ProUpgradeViewModelTest.kt` |
| Default-network changes, Ethernet, signal sentinels | `data/network/NetworkDataSourceVpnDetectionTest.kt`, `data/network/NetworkSignalDbmTest.kt`, `data/network/NetworkRepositoryImplTest.kt`, `domain/usecase/GetMeasuredNetworkStateUseCaseTest.kt` |
| NDT7 progress and finalization | `data/network/SpeedTestResponseParsingTest.kt`, `domain/usecase/FinalizeSpeedTestUseCaseTest.kt`, `ui/network/NetworkViewModelTest.kt` |
| Provider failures / cancellation / deletion verification | `data/storage/CleanupPagingSourceTest.kt`, `data/storage/StorageDataSourcePublicInfoTest.kt`, `ui/storage/cleanup/CleanupViewModelTest.kt` |
| App-usage collection and cursor handling | `data/appusage/AppBatteryUsageRepositoryImplTest.kt`, `ui/appusage/AppUsageViewModelTest.kt` |
| Cross-store data reset / external intents | `domain/usecase/ClearMonitoringDataUseCaseTest.kt`, `ui/settings/SettingsViewModelTest.kt`, `ui/settings/SettingsExternalIntentTest.kt` |
| UI mutation failure preservation | `ui/home/HomeViewModelTest.kt`, `ui/insights/InsightsViewModelTest.kt` |
| Estimated current outside Compose | `service/monitor/LiveNotificationCurrentTest.kt`, `widget/BatteryWidgetSnapshotTest.kt` |

`HomeScreenTest` exercises pure helpers such as segment fill and layout-mode selection; its name does not establish a rendered Compose or touch-target test. Likewise, mocked Android unit tests cannot prove OEM APIs, system consent dialogs, live billing, or widget rendering on a device.

Coverage interpretation: `app/build.gradle.kts` defines `jacocoDebugUnitTestReportExclusions`, excluding generated code, DI/startup, many framework-backed sources, billing implementations, workers/services, most Compose surfaces, theme, and widgets. The XML/HTML report is a scoped JVM coverage denominator, not coverage of the complete Android app. It depends on `testDebugUnitTest` and reads both traditional and AGP built-in Kotlin class output directories. Never infer runtime safety for excluded code from an aggregate Sonar/JaCoCo percentage.

High-value unit coverage areas:

- Battery source normalization and device capability detection
- Billing helper behavior and Pro state transitions
- Network VPN detection and network ViewModel behavior
- Thermal helper mapping
- Health score calculation
- Domain use cases for cleanup, alerts, export, battery stats, charger comparison, throttling, and retention
- Insight home ranking policy and every current Insight rule
- Home, Settings, Storage cleanup, App Usage, Charger, Battery, Network, Insights, Fullscreen Chart, and Learn ViewModel/helper behavior
- Chart render/accessibility helpers
- Debug/release-safe insight debug action boundaries
- Worker behavior for monitor scheduler, health monitor, and insight generation

Current delegated Android-check contract:

- `config/android-check.json` declares one required Android application module, `:app`, with `debug` and `release` variants and the `main`, `debug`, `release`, `test`, `testDebug`, and `androidTest` source sets.
- The configured build surface is `:app:assembleDebug`; the default test surface is `:app:testDebugUnitTest`, and `tc -Full` also runs `:app:connectedDebugAndroidTest` after confirming that a device or emulator is connected.
- Configured quality tasks are `:app:ktlintCheck`, `:app:detekt`, `:app:lintDebug`, and `:app:stabilityCheck`; dependency analysis covers `debugRuntimeClasspath` and `releaseRuntimeClasspath`, and OWASP uses `:app:dependencyCheckAnalyze`.
- `config/check-exceptions.json` currently contains 34 owned, time-bounded exceptions, all expiring `2026-10-31`: 30 exact mobsfscan entries, 2 CodeQL entries, 1 Detekt baseline registration, and 1 OWASP false-CPE group.
- The registered Detekt baseline contains 28 finding IDs in `app/detekt-baseline.xml`; a passing wrapper with that baseline is not the same as zero underlying recorded findings.
- Every current MobSF exception includes one exact rule, one existing `findingPath`, and one or more exact `findingSelectors`. The delegated checker normalizes selector whitespace/casing and suppresses only a finding whose rule, normalized path, and normalized selector all match; another finding in the same file remains visible unless it has its own registered selector.
- `ms -PlanOnly` currently prints each exception's rule and path but not its selector list. That abbreviated plan text does not weaken runtime classification: execution still applies the exact selector matching described above.
- The target-SDK exception is limited to `android_task_hijacking2` at `app/src/main/AndroidManifest.xml` with its exact current finding selector. `.mobsf` contains path exclusions and severity filtering only; it must not suppress that rule globally.
- CodeQL exceptions remain exact rule/path registrations. The Detekt and OWASP entries use their own source/selector contracts rather than MobSF's `findingSelectors` matching.
- Scanner output must be interpreted after wrapper classification and exact exceptions. A raw-match count of zero is neither required for `CLEAN` nor sufficient if the wrapper reports a technical/configuration error.

Low-CPU verification policy:

- Do not run full Gradle builds, `lc`, `sc`, Sonar, Dependency-Check, MobSF, DeepSec, or broad verification by default.
- Prefer source-backed review, targeted tests, targeted Gradle tasks, wrapper `-PlanOnly`, and static grep first.
- User-facing aliases `lc` and `sc` are intentionally run by the user when they want full lint/security reports.
- When report artifacts exist, read them first instead of rerunning heavy wrappers.
- `reports/` is ignored and must not be committed.

Useful narrow commands:

```powershell
.\gradlew --no-daemon testDebugUnitTest --tests "com.runcheck.domain.scoring.HealthScoreCalculatorTest"
.\gradlew.bat :app:connectedDebugAndroidTest --project-prop=android.testInstrumentationRunnerArguments.class=com.runcheck.data.db.RuncheckDatabaseMigrationTest --dry-run --no-daemon --no-configuration-cache --console=plain
.\gradlew --no-daemon :app:compileDebugKotlin :app:compileDebugUnitTestKotlin --no-configuration-cache
.\tools\pc.ps1 -PlanOnly
.\tools\ms.ps1 -PlanOnly
.\tools\ds.ps1 -PlanOnly
.\tools\sentry.ps1 -PlanOnly
.\tools\dc.ps1 -PlanOnly
.\tools\sc.ps1 -PlanOnly -Full
.\tools\sonar.ps1 -PlanOnly
```

Report-reading convention:

- "lue lint-tulokset" means read `reports/ktlint.txt`, `reports/detekt.txt`, and `reports/lint.txt`.
- "lue security-tulokset" means read `reports/security-summary.txt` first when present, then the reports generated by the chosen security mode: `reports/semgrep-kotlin.txt`, `reports/semgrep-secrets.txt`, `reports/gitleaks.txt`, `reports/trufflehog.txt`, `reports/dependency-verification.txt`, `reports/osv.txt`, and `reports/security-deps.txt` for the full `sc -Full` path.

---

## CI/CD Pipeline

GitHub Actions workflows in `.github/workflows/`:

| Workflow | Purpose | Status |
|----------|---------|--------|
| `codeql.yml` | CodeQL security analysis (`java-kotlin`, manual `assembleDebug`) | Active on main pushes, main PRs, manual dispatch, and weekly schedule; CodeQL Action `v4.37.6`, checkout `v7.0.1`, setup-java `v5.7.0`, setup-android `v4.0.1` |
| `security.yml` | Semgrep SAST on PRs/main plus OWASP Dependency-Check on weekly/manual runs | Semgrep `1.175.0` runs for push/PR and uploads SARIF through CodeQL Action `v4.37.9` except on fork and Dependabot PRs, whose read-only tokens cannot publish code-scanning results; setup-python is `v7.0.0`. OWASP stays out of push/PR analysis, runs scheduled/manually with setup-java `v5.7.0`, setup-gradle `v6.3.0`, cache action `v6.1.0`, a 45-minute job timeout, a 35-minute step timeout, and artifact upload `v7.0.1` when a report exists. |
| `sonar.yml` | SonarCloud scan through Gradle (`assembleDebug`, `:app:jacocoDebugUnitTestReport`, `sonar`) | Active on main pushes; checkout `v7.0.1`, setup-java `v6.0.0`, setup-android `v4.0.1` |
| `qodana.yml` | JetBrains Qodana main-branch scan through `JetBrains/qodana-action` pinned at `v2026.2.0` | Uses `jetbrains/qodana-jvm-community:2026.1`; retained after the documented AGP 9.1.x Android-linter import failure, while current AGP 9.2.1 Android-linter compatibility remains unverified |
| `qodana_code_quality.yml` | JetBrains Qodana action pinned at `v2026.2.0` for `releases/*`, PRs, and manual dispatch | Uses the same JVM Community linter; `qodana.yml` owns `main` pushes, avoiding a duplicate scan. Current AGP 9.2.1 Android-linter compatibility remains unverified |

External services:
- **SonarCloud** — continuous code quality (`Insaner1980_runcheck`, org `insaner1980`). CI path is `.github/workflows/sonar.yml`; local path is `tools/sonar.ps1`.
- **Qodana Cloud** — org "Finnvek Dev", project "runcheck"; current workflow files do not pass `QODANA_TOKEN`, so they should be read as local/action-based Qodana analysis unless a token is added later.

Local PowerShell wrappers:

- `tools/lc.ps1` (`lc`) — ktlint, detekt, Android lint; writes `reports/ktlint.txt`, `reports/detekt.txt`, and `reports/lint.txt`; the shared wrapper appends the Android lint text report and fails high-risk lint policy findings
- `tools/bc.ps1` (`bc`) — shared `build-check` wrapper for the configured `:app:assembleDebug` build surface
- `tools/tc.ps1` (`tc`) — shared `test-check` wrapper for `:app:testDebugUnitTest`; `-Full` also runs `:app:connectedDebugAndroidTest` after device preflight
- `tools/ac.ps1` (`ac`) — Android security surface: project Semgrep and mobsfscan; DeepSec remains a separate `ds` run with explicit external-AI consent
- `tools/dc.ps1` (`dc`) — Gradle dependency verification, OSV Scanner, and OWASP Dependency-Check; the single `app` module uses `:app:dependencyCheckAnalyze` because the aggregate task can succeed with an empty dependency report and is rejected fail-closed
- `tools/ss.ps1` (`ss`) — gitleaks, TruffleHog, and Semgrep secrets
- `tools/ds.ps1` (`ds`) — DeepSec custom scan/report/revalidate paths
- `tools/ms.ps1` (`ms`) — mobsfscan
- `tools/os.ps1` (`os`) — OSV Scanner
- `tools/ql.ps1` (`ql`) — checks the latest GitHub default-branch CodeQL baseline and the current local Java/Kotlin/Gradle inputs. It reuses a clean covered HEAD when possible; otherwise it runs the locked local CodeQL CLI against the exact current inputs and caches SARIF. `-CurrentCommit` is a legacy remote-scope override, and the wrapper does not upload local results.
- `tools/db.ps1` (`db`) — Dependabot config and alert check
- `tools/pc.ps1` (`pc`) — PMD CPD duplicate scan; default minimum token threshold is 100 and can be overridden with `PMD_CPD_MINIMUM_TOKENS`
- `tools/cs.ps1` (`cs`) — Compose Stability Analyzer (`:app:stabilityCheck`)
- `tools/cr.ps1` (`cr`) — compose-rules through ktlint and detekt
- `tools/ga.ps1` (`ga`) — Google Android Security Lints through Android lint
- `tools/sc.ps1` (`sc`) — combined security check; `-Full` also runs Android security checks
- `tools/sentry.ps1` (`sentry`) — verifies debug-only Sentry wiring and release classpath exclusion; writes `reports/sentry.txt`
- `tools/sonar.ps1` — SonarCloud local path; requires `SONAR_TOKEN`, runs `assembleDebug`, `:app:jacocoDebugUnitTestReport`, prepares an empty Android Lint import placeholder because `lc` owns real lint findings, and runs `sonar`, then writes `reports/sonar.txt`
- `tools/sonar-timeout-test.ps1` — isolated PowerShell fixture that verifies a timed-out Sonar Gradle process is terminated and that stdout/stderr plus the timeout marker are persisted

The checked-in `.deepsec` helper currently pins DeepSec `2.3.8`; `tools/ds.ps1` delegates to the shared Android-check implementation, which drives the package scripts rather than making DeepSec part of the Android app dependency graph. Every delegated DeepSec mode currently requires explicit per-run external-AI consent: `-AllowExternalAI`, a single-line `-Provider`, exact `-ExternalAIDataScope 'entire-project-working-tree'`, a single-line `-ExternalAICostEstimate`, and a single-line `-ExternalAIRetentionPolicy`. Without that complete declaration the wrapper fails closed with exit category `ERROR/2` and an `EXTERNAL_AI_*` reason; `-PlanOnly` reports the missing consent without uploading data. The default path is `deepsec:report:custom`; `-Full` selects `deepsec:report`, `-Scan` selects `deepsec:scan`, and `-Revalidate` selects `deepsec:revalidate`.

When `osv-scanner`, gitleaks, TruffleHog, or PMD are missing from `PATH`, the shared Android-check wrappers may download and cache verified tool binaries under `.gradle/android-check-tools/`; offline first runs can therefore skip or fail before a cached tool exists. The OSV source scan excludes `.deepsec` so Android-check's own DeepSec tooling dependencies do not fail app dependency scans.

Compatibility wrappers and config:

- `scripts/security-check.ps1` forwards to `tools/sc.ps1`
- No Linux shell security wrapper is maintained in this Windows-first repo
- Check configuration lives in `config/semgrep/runcheck-security.yml`, `config/dependency-check/suppressions.xml`, `.mobsf`, `.deepsec/`, `.github/dependabot.yml`, `sonar-project.properties`, and `gradle/osv-scanner.toml`
- `reports/` is ignored and must not be committed

---

## Project Management

- **Linear:** Project "runcheck" in Finnvek team, priority High, status In Progress
- **Linear URL:** https://linear.app/loikka1/project/runcheck-5d6d01d874c1
- **Milestones:**
  - v1.0 — Play Store Release (all features, tested, security audited)
  - Insights Engine (cross-category correlation — the differentiator)
- **GitHub:** https://github.com/Insaner1980/runcheck

The product-management service state above is documentation metadata and was not live-queried during the 2026-09-08 source refresh. Treat code/configuration as current; verify external Linear/Qodana/Sonar/GitHub status live when a task depends on it.

---

## Roadmap

### Current Major Differentiator: Insights Engine

runcheck now includes a cross-category correlation engine that analyzes Room data across battery, thermal, network, storage, charger, and app-usage history and surfaces insights automatically on Home. It is one of the clearest product differentiators versus apps that only show siloed metrics. Examples:
- Correlation between temperature rise and battery drain spike
- Network quality degradation at specific times
- Anomaly detection from normal usage patterns

Current Insights rule set:

- `BatteryDegradationTrendRule`
- `BaselineAnomalyRule`
- `ChargerPerformanceRule`
- `StoragePressureProjectionRule`
- `RecurringThermalThrottlingRule`
- `HeavyAppUsageRule`
- `NetworkSignalPatternRule`
- `NetworkDrivenBatteryDrainRule`
- `HeatAcceleratedBatteryWearRule`
- `StoragePressureImpactRule`
- `ThermalPatternDetectionRule`

Rules are Hilt multibindings into `Set<InsightRule>`. `InsightEngine` filters generated candidates below 0.6 confidence and replaces results per rule. Matching dedupe keys preserve existing seen/dismissed state, and dismissed rows remain as dedupe tombstones when a candidate is temporarily absent or expired so regeneration cannot resurrect them. Expired undismissed rows are deleted during generation.

### Rule inputs, windows, and interpretation

The production registry is `app/src/main/java/com/runcheck/di/InsightsModule.kt`; rule sources are under `app/src/main/java/com/runcheck/domain/insights/rules/`. This table records important entry conditions for review, not every branch in the algorithms. Candidate eligibility and engine acceptance are separate: a rule can meet its sample minimum and still fall below the engine's 0.6 confidence threshold.

| Rule | Input window / candidate lifetime | Key review boundaries |
|------|-----------------------------------|-----------------------|
| `BatteryDegradationTrendRule` | Two consecutive 7-day windows / 24h | At least 20 discharging readings per window; average drain ratio must exceed 1.15. Confidence uses the smaller window count divided by 40, so the engine threshold further restricts accepted samples. This observes drain trends, not a laboratory capacity-loss measurement. |
| `BaselineAnomalyRule` | Current day versus preceding 14-day baseline / 24h | At least 40 total readings, 4 current discharge pairs and at least 7 baseline days each with >=4 discharge pairs; minimum current drain 3 percentage points/hour, z-score 2.5, and rate ratio 1.75 are explicit algorithm thresholds. Review daily aggregation and variance handling as well as raw count. |
| `ChargerPerformanceRule` | Completed sessions within 45 days, excluding future end times / 24h | At least 2 chargers and 4 completed sessions; at least 2 positive power samples per charger. Best average power is compared with the weakest qualifying average; even the slower charger's maximum must be >=20% below the best charger's minimum. Confidence is min sample count /4, so two samples per charger alone do not pass the engine. |
| `StoragePressureProjectionRule` | 14 days / 24h | Shared loader requires >=4 readings and a valid `StorageGrowthAnalyzer` projection; candidate horizon <=30 days. High-priority boundaries include 14 days and 90% used. |
| `RecurringThermalThrottlingRule` | 7 days / 24h | At least 3 SEVERE-or-higher events; confidence denominator 5 severe events; total recorded duration 15 minutes is the high-priority duration boundary. Review ongoing versus completed event time handling. |
| `HeavyAppUsageRule` | 24h / 12h | Top app >=2h foreground time and >=60% of collected foreground time; HIGH at >=4h or >=75% share. Confidence is distinct aggregated app count /5. This describes usage concentration, never per-app mAh. |
| `NetworkSignalPatternRule` | 3 days / 12h | Cellular dBm only; at least 6 samples, 4 weak readings, and 60% weak ratio, with weak threshold -110 dBm. Confidence denominator 10; high-priority boundaries include -115 dBm and 75%. |
| `NetworkDrivenBatteryDrainRule` | 48h / 12h | At least 9 battery readings, 8 network readings, 8 discharge intervals, and 3 samples per compared class. Weak cellular <=-110 dBm versus strong >=-100 dBm; minimum drain ratio 1.2. Alignment uses `TimeWindowAligner`, not arbitrary nearest future samples. |
| `HeatAcceleratedBatteryWearRule` | 48h / 12h | At least 9 battery readings, 8 thermal readings, 8 discharge intervals, and 3 samples per class. Hot means >=40°C or status >=SEVERE; cool means <=35°C and status <=LIGHT. Minimum drain ratio 1.2. The result is correlated observed drain, not proof of irreversible chemical wear. |
| `StoragePressureImpactRule` | 14 days / 24h | Shared valid-growth projection with horizon <=45 days; storage-score boundary 65, high-priority boundaries 45 and 14 days. |
| `ThermalPatternDetectionRule` | 48h / 12h | At least 6 readings, 4 hot readings, 60% hot ratio, hot means >=39.5°C or status >=MODERATE; confidence denominator 8, high-priority boundaries 43°C and 75%. |

The shared `SingleCandidateInsightRule`, `ContextualBatteryDrainRule`, and `StoragePressureInsightRule` encode orchestration; extend them rather than duplicating load/compare/candidate pipelines. Time-bounded network, heat, usage, and storage loaders explicitly discard future timestamps before analysis.

`domain/insights/engine/InsightEngine.kt` evaluates every rule before calling `replaceGenerationResults` once. If evaluation throws, that generation does not reach replacement. If accepted recurring-throttling and thermal-pattern candidates coexist, the engine clears the thermal-pattern candidate list to avoid overlapping alerts. `data/insights/InsightRepositoryImpl.kt` and the DAO own transactional replacement and dedupe persistence.

`InsightHomeRankingPolicy` sorts by priority, confidence descending, generation time descending, then id. It first selects distinct target buckets (type buckets for NONE), then fills any remaining slots from the ranked list, with a hard cap of three. The dedicated Insights screen retains DAO ordering instead. Neither Home's full-check refresh nor merely reading the Insights screen is equivalent to explicitly regenerating the rule engine.

`AppBatteryImpactRule` is intentionally not part of the production rule set. Android does not expose other apps' battery statistics to ordinary third-party apps, and runcheck does not manufacture per-app mAh attribution from foreground time alone.

### Known Tool Limitations

- **Qodana:** `qodana.yaml` still records the original AGP 9.1.x Android-linter import failure and selects `jetbrains/qodana-jvm-community:2026.1`. The app has since moved to AGP 9.2.1, so the recorded Android-linter incompatibility is historical evidence, not fresh proof for the current AGP line. Keep the JVM linter until the Android linter is explicitly re-tested, and update the comment/result together.
- **CodeQL:** `.github/workflows/codeql.yml` pins `github/codeql-action/init` and `analyze` to `v4.37.6` and builds with `assembleDebug --no-configuration-cache`. Check the actual CodeQL Action runner and Kotlin extractor support before Kotlin plugin upgrades.
- **Sonar:** AGP 9 support has had scanner-side compatibility churn. Keep `tools/sonar.ps1` and `.github/workflows/sonar.yml` verified when changing AGP, Gradle, or Kotlin.
- **OWASP Dependency-Check:** NVD updates can take a very long time or return transient 503 responses, so PRs and ordinary main pushes run Semgrep/CodeQL/Qodana while Dependency-Check is reserved for weekly scheduled or manual runs with cache, bounded retries, a job timeout, and a shorter OWASP step timeout (no `continue-on-error` in the current workflow). Dependency-Check reports are uploaded as Actions artifacts instead of GitHub Code scanning SARIF so stale dependency analyses do not keep fixed Dependabot issues open.

---

## Notes for Maintenance

- `PROJECT.md` should describe the code as it exists now, not the intended roadmap only.
- `CODEX.md` and `AGENTS.md` should stay aligned when repository rules or project snapshot notes are updated.
- This 2026-09-08 refresh coordinates changes across `PROJECT.md`, `UI-SPEC.md`, `AGENTS.md`, `CODEX.md`, and the related implementation files. Do not interpret this date as a fresh validation of every companion document.
