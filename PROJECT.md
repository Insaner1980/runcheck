# runcheck — Project Overview

Android device health diagnostics app built with Kotlin and Jetpack Compose. Single dark theme, one-time Pro purchase, no subscriptions.

Reading map:

- [Technical snapshot and source map](#technical-snapshot): versions, configuration inputs, architecture-review entry points.
- [Architecture and data flow](#architecture-and-data-flow), [navigation](#navigation), [runtime systems](#runtime-systems): ownership, state, background work, failures.
- [Measurement reliability](#measurement-reliability-and-health-score): valid values, unavailable data, confidence, scoring.
- [Home](#home-screen), [Insights](#insights-screen), [Battery](#battery-detail), [Charger](#charger-comparison), [Network](#network-detail), [Speed Test](#speed-test), [Thermal](#thermal-detail), [Storage](#storage-detail), [Cleanup](#cleanup-screen), [App Usage](#app-usage), [Learn](#learn), [Fullscreen Chart](#fullscreen-chart), [Settings](#settings), [Pro Upgrade](#pro-upgrade-screen): screen-specific behavior.
- [Persistence](#persistence), [monetization](#monetization-model), [security/privacy](#security-and-privacy-surface): storage, reset/export, entitlements, platform boundaries.
- [Design system](#brand--design-system), [responsive layout](#responsive-layout-and-interaction-reference): colors, typography, dimensions, semantics, motion, font-scale branches.
- [Testing](#testing-and-verification), [CI/CD](#cicd-pipeline), [Insights Engine](#insights-engine), [Insight rule inputs](#rule-inputs-windows-and-interpretation), [companion discrepancies](#companion-documentation-and-unverified-boundaries): verification limits, operational tooling, algorithm review.

---

## Document Purpose, Scope, and Evidence Rules

This document is the detailed current-state map for architecture reviews, code-review question generation, implementation planning, security review, test planning, and maintenance work.

Snapshot rules:

- Last source-backed refresh: **2026-09-13**.
- Audit baseline: branch `main`, HEAD `beb56c5a66d795095904d991a030d17bf072fadb`. The initial worktree was clean: no staged changes, unstaged tracked changes, or untracked files. This audit changes only root `PROJECT.md`, leaving it as the sole unstaged modification.
- The snapshot describes the current working tree, whose implementation/configuration matched that HEAD at audit start. Chart performance sources, stability baselines, scanner exceptions, and the September 11 performance report are tracked inputs. Local branch/HEAD inspection does not establish synchronization with GitHub or any remote branch; no remote operation was performed.
- This refresh is a documentation-only source/configuration review. No Gradle task, scanner, emulator, physical-device session, or external service status check was run. Test descriptions below identify source coverage, not passing results from this refresh.
- This is a checkout snapshot, not an API compatibility promise, release note, or proof that every runtime path has been exercised on a physical device.
- If this document conflicts with executable code or configuration, the executable source wins and this document must be corrected.
- Intended behavior that exists only in a plan, issue, design mock, or roadmap is not current product behavior.
- Historical reports support only the exact run, source scope, commit/worktree, device, and timestamp recorded in that report. In particular, the September 11 chart measurements remain dated historical evidence, not September 13 test results or device validation.

Evidence precedence for review work:

1. Production source and source-set-specific implementations.
2. Tests, exported Room schemas, and generated/configuration contracts.
3. Gradle files, version catalog, manifest/resources, CI workflows, and local wrapper scripts.
4. `AGENTS.md`, `CODEX.md`, `UI-SPEC.md`, and this file.
5. Historical plans, reports, screenshots, and roadmap text.

Reviewers should distinguish:

- **Code-confirmed**: directly supported by current implementation.
- **Configuration-confirmed**: declared in current build, manifest, resources, scripts, or workflows; not proof of successful execution or resolved artifact contents.
- **Test source present**: a named test encodes the behavior, but was not necessarily executed.
- **Test-confirmed**: that named test passed in a recorded run against the relevant inputs; still not necessarily proof of device behavior.
- **Tool-confirmed**: supported by a fresh, scoped analyzer/build report.
- **Device-confirmed**: observed on a named device and APK provenance.
- **Historically measured/test-confirmed**: supported by a dated report for its recorded inputs; later source changes require new evidence.
- **External-service status / planned**: service metadata and intended work are separate from implemented product behavior; neither was live-verified here.
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
- Build tooling: Gradle wrapper 9.7.1, AGP 9.4.0, Kotlin Gradle/Compose plugin 2.4.10, Kotlin runtime constraints 2.4.20, KSP 2.3.11, Compose BOM 2026.08.00
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
| `app/src/main/java` | 370 | Includes `MainActivity`, `RuncheckApp`, and all shared production packages |
| `app/src/debug/java` | 4 | Debug Sentry and insight tooling/bindings |
| `app/src/release/java` | 2 | Release-safe Sentry and insight bindings |
| `app/src/test/java` | 136 | JVM tests and support files, including the opt-in benchmark |
| `app/src/testDebug/java` | 3 | Debug-source-set tests |
| `app/src/androidTest/java` | 3 | Instrumented/migration test sources |

The largest shared source areas are `ui/` (145 files), `domain/` (130), and `data/` (59). This count is an orientation aid, not an architectural quality metric; review questions should follow dependencies and runtime ownership rather than file volume.

Debug/release-specific insight tooling also lives outside the shared main source tree:

- `app/src/debug/java/com/runcheck/debug/insights/` for debug implementations
- `app/src/debug/java/com/runcheck/di/InsightDebugModule.kt` for debug Hilt bindings
- `app/src/release/java/com/runcheck/di/InsightDebugModule.kt` for release Hilt bindings
- `app/src/main/java/com/runcheck/debug/insights/` for release-safe public stubs used by shared code
- `app/src/release/java/com/runcheck/SentryInit.kt` for release no-op Sentry initialization

### Build and dependency source of truth

- `gradle/libs.versions.toml` is the source of truth for dependency and plugin versions.
- `gradle.properties` owns the `runcheck.buildTools.*` security versions for vulnerable transitive build-tool dependencies. Root `build.gradle.kts` applies these pins only to the matching buildscript, ktlint, Android Lint, and Unified Test Platform configurations; application runtime configurations remain unaffected.
- `gradle/wrapper/gradle-wrapper.properties` pins Gradle to `9.7.1` and verifies the binary distribution with the checked-in SHA-256.
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
| Gradle wrapper | `9.7.1` |
| Android Gradle Plugin | `9.4.0` |
| Kotlin Gradle / Compose plugin | `2.4.10` |
| Kotlin runtime constraints | `2.4.20` |
| KSP | `2.3.11` |
| Hilt | `2.60.1` |
| Hilt AndroidX / Hilt Work | `1.4.0` |
| Room | `2.8.4` |
| Compose BOM | `2026.08.00` |
| Navigation Compose | `2.9.8` |
| Lifecycle | `2.11.0` |
| Kotlin coroutines | `1.11.0` |
| Activity Compose | `1.13.0` |
| Core KTX | `1.19.0` |
| ProfileInstaller | `1.4.1` |
| WorkManager | `2.11.2` |
| DataStore | `1.2.1` |
| Paging | `3.5.1` |
| Play Billing | `9.1.0` |
| Glance | `1.1.1` |
| M-Lab NDT7 client | `e0cb663613eb252a7793216ad28cf54a35677b8f` |
| OkHttp | `5.5.0` |
| Gson | `2.14.0` |
| kotlinx.serialization JSON | `1.11.0` |
| JUnit | `4.13.2` |
| MockK | `1.14.11` |
| AndroidX Test Ext JUnit | `1.3.0` |
| AndroidX Test Runner | `1.7.0` |
| Sentry debug-only core | `8.54.0` |
| Dependency Analysis Gradle plugin | `3.19.1` |
| ktlint rule engine | `1.8.0` |
| ktlint Gradle plugin | `14.2.0` |
| Detekt | `2.0.0-alpha.6` |
| compose-rules for ktlint | `0.6.4` |
| compose-rules for Detekt | `0.6.4` |
| OWASP Dependency-Check Gradle plugin | `13.0.0` |
| SonarQube Gradle plugin | `7.4.0.8496` |
| Compose Stability Analyzer | `0.12.0` |
| Google Android Security Lints | `1.0.4` |
| JaCoCo | `0.8.14` |

Local checker-helper versions outside the Android application dependency graph:

| Area | Current value | Ownership and scope |
|------|---------------|---------------------|
| DeepSec | `2.3.9` | Exact dependency in `.deepsec/package.json` and `.deepsec/pnpm-lock.yaml`; used only by the local DeepSec scan/process/export scripts |
| TypeScript | `^7.0.2` (locked `7.0.2`) | `.deepsec` development dependency, not an Android runtime dependency |
| Node type declarations | `^26.5.0` (locked `26.5.1`) | `.deepsec` development dependency, not an Android runtime dependency |

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

- The active coordinated toolchain is AGP 9.4.0, Kotlin Gradle/Compose plugin 2.4.10, Kotlin runtime constraints 2.4.20, KSP 2.3.11, Hilt 2.60.1, Detekt 2.0.0-alpha.6, and Compose Stability Analyzer 0.12.0.
- Both debug and release stability baselines are checked in. Analyzer 0.12.0 is configured with `includeTests = false`, `failOnStabilityChange = true`, `ignoreNonRegressiveChanges = false`, and missing baselines disallowed. Earlier documentation records resolution of the Kotlin 2.4 compatibility issue; this source-only refresh did not regenerate either variant.
- Compose library versions come from the Compose BOM, while the Compose compiler is managed through the Kotlin Compose plugin. Treat Kotlin, Compose, KSP, Detekt, analyzer, AGP, dependency verification, and CI extractor changes as a compatibility set.
- Gradle configuration cache is enabled. Build cache and parallel execution are disabled, `org.gradle.workers.max = 2`, and the Kotlin compiler execution strategy is in-process.
- Gradle and Kotlin task build caches are disabled through `org.gradle.caching=false` and `kotlin.caching.enabled=false` while the time-bounded CVE-2026-53914 advisory exception remains active.
- Release builds are minified and resource-shrunk. `copyReleaseArtifacts` names outputs `runcheck-1.0.0-code1-release.apk` and `.aab`.
- Release artifact tasks validate signing inputs, require `--no-configuration-cache`, and require the version-code floor described in the Technical Snapshot. Ordinary debug checks do not require release signing.
- Debug BuildConfig values may read validated local/environment overrides for Sentry DSN, latency host/port, and Pro product id. Release keeps the checked-in product id and no-op Sentry path.

Build/release details that configuration alone cannot prove:

- Only `:app` is included; there is no benchmark or baseline-profile generation module. `app/src/main/baseline-prof.txt` has 18 checked-in HSP entries and ProfileInstaller is a runtime dependency. Their presence does not prove installation, compilation benefit, startup time, or release performance.
- `settings-gradle.lockfile` records an empty incoming catalog configuration (`empty=incomingCatalogForLibs0`). There is no maintained application dependency-lock graph. Catalog pins, the restricted repositories, and `verification-metadata.xml` provide different controls from complete dependency locking.
- The Gradle daemon heap is 2GB, workers are capped at two, and Gradle parallel execution is disabled; Detekt separately sets its own parallel flag. Do not infer every tool is single-threaded.
- Artifact task matching includes `copyReleaseArtifacts`, assemble/bundle/package/publish names ending in Release, and `packageReleaseBundle`, `packageReleaseUniversalApk`, `signReleaseBundle`. Explicit requests validate during configuration; matched tasks depend on `validateReleaseArtifactInputs`, including resolved abbreviated/up-to-date requests. The floor must be non-negative and strictly below current code 1; this task did not query Play's published version.
- `copyReleaseArtifacts` writes to `app/build/outputs/release-upload/`. R8 full mode, minification, and shrinking are configured; signed APK/AAB contents, merged release manifest, and effective dependency graph remain unverified here.

### Build-time and checker configuration inputs

The following table records variable names and defaults, never credential values. This audit did not run a secrets scanner and does not certify the entire worktree secret-free. Review configuration changes against these ownership boundaries:

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

Additional questions for the current cross-cutting contracts:

- Do worker freshness and screen accounting use their distinct uptime/elapsed/wall-clock rules, and can stored boot identity prevent counting a reboot gap?
- Can a sampled live update overwrite newer history/error state, or a consumed fullscreen result detach the StateFlow needed by the next return?
- Does cancellation release pending seen writes and propagate through deletion verification? Can generation preserve seen/dismissed metadata at expiry without resurrecting dismissed keys?
- Do export's post-suspension Pro checks and Settings' export/reset mutex retain their scope, and does a cross-store failure remain distinguishable from a successful full reset?
- Do charging-current averages use exactly the intervals integrated into mAh, and do duplicate timestamps or large epoch/capacity offsets distort drain/growth calculations?

### Feature ownership and state map

All paths below are relative to `app/src/main/java/com/runcheck/`. Repository contracts live in `domain/repository/`; implementations bind through `di/`.

| Surface | State owner and domain entry points | Data/platform owner |
|---------|------------------------------------|---------------------|
| Home | `ui/home/HomeViewModel`; four live-state use cases, latest speed test, `InsightRepository`, `MonitoringStatusRepository` | Four sensor repositories, Room insights/speed results, heartbeat DataStore |
| Battery | `ui/battery/BatteryViewModel`; history/statistics, `BatteryScreenInsightsUseCase`, `ChargerSessionTracker` | `data/battery/`, Room battery rows, `ScreenStateTracker`, charger repository |
| Charger | `ui/charger/ChargerViewModel`; comparison/add/delete use cases, selected-charger preference | `data/charger/ChargerRepositoryImpl`, Room profiles/sessions and settings DataStore |
| Network and Speed Test | `ui/network/NetworkViewModel`; measured-state, run/finalize/history use cases | `data/network/NetworkDataSource`, `LatencyMeasurer`, `SpeedTestService`, network/speed repositories |
| Thermal | `ui/thermal/ThermalViewModel`; live thermal and history/log use cases | `data/thermal/`, singleton `TrackThrottlingEventsUseCase`, Room events |
| Storage and cleanup | `ui/storage/StorageViewModel`, `ui/storage/cleanup/CleanupViewModel`; storage state/history and `StorageCleanupUseCase` | `data/storage/`, StorageStatsManager, MediaStore, Paging, system consent |
| App Usage | `ui/appusage/AppUsageViewModel`; refresh, page, summary use cases | `data/appusage/`, UsageStatsManager/AppOps, Room snapshots, DataStore cursor |
| Insights | `ui/insights/InsightsViewModel`; domain `InsightRepository` plus observed Pro access | `data/insights/`; generation separately owned by `InsightEngine` and worker |
| Learn | Catalog/resources and Compose screens; no Learn ViewModel or repository | Local article resource text and validated route aliases |
| Fullscreen | `ui/fullscreen/FullscreenChartViewModel`; history/live use cases and `FullscreenChartSeedStore` | Shared chart models; existing battery/network repositories |
| Settings | `ui/settings/SettingsViewModel`; observe/set/reset/export use cases and speed-result repository | Preferences, device profile, billing abstractions, FileProvider cache |
| Pro Upgrade | `ui/pro/ProUpgradeViewModel`; Pro access and purchase abstraction | `data/billing/BillingManager`, `pro/ProManager`, Play client/cache |
| Widgets | `widget/` data providers and render-state models | Hilt entry point accesses DAOs directly, outside the `ui/` package |

ViewModels may depend directly on **domain repository interfaces**, notably Home/Insights and Settings; "only use cases" is not an accurate description of all current constructors. Widgets and service orchestration have their own data-entry boundaries. Route state, persistent domain data, and short-lived display state are different lifetimes: stopped collectors do not erase Room history, while an in-memory chart buffer or refresh flag is not process-death persistence.

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

`util/AppDispatchers.kt` is an injectable singleton exposing overridable IO, Default, Main, and Main.immediate dispatchers. Domain code uses this utility seam rather than Android APIs; tests can supply controlled dispatchers. Scope/lifetime still belongs to the caller (application, billing/service, ViewModel, or worker), not to the dispatcher object. The purchase interface outside domain deliberately accepts an Android Activity for the Play checkout UI.

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
| MediaStore reads | Cleanup summaries/pages/file verification propagate provider failures rather than inventing confirmed deletion. Storage's optional breakdown can degrade to unavailable, and its separate trash query has empty-list fallbacks. These paths do not share one universal error contract. |
| Settings reset tips | Success text is set only after dismissal-state persistence succeeds. Failure sets a generic UI error instead of showing a premature success toast. |
| External Settings/share intents | `startActivitySafely` returns false for `ActivityNotFoundException` or `SecurityException`; callers can handle failure. CSV sharing attaches every shared URI to ClipData with read grants. |

Application startup jobs use `launchSafely` to retain sibling work after failures. The Pro-driven widget observer waits for `proAccessReady` and catches each refresh failure inside the observation loop, so one failed refresh does not permanently stop future Pro updates.

There is no explicit first-frame barrier: jobs are launched during application initialization even though a nearby comment describes deferral. `onTerminate()` cleanup is not a guaranteed normal-device shutdown callback. Live sensor ViewModels commonly sample at 333ms; this is not a blanket sampling guarantee for Settings, Paging, purchase events, or every other flow.

---

### Architecture coordination and live persistence boundaries

- `MonitoringDataCoordinator` serializes the `ClearMonitoringDataUseCase` Room deletion transaction and debug history seeding with the complete insight read/evaluate/publish operation. Reset waits for admitted generation before deleting its results; later generation reads the remaining or newly collected history. Lock order is coordinator, thermal tracker, then Room. The tracker invalidates its cached active event in `finally` before releasing its lock, including failed or cancelled resets, and restores any surviving Room event on the next observation. Lock waits remain cancellable; monitoring schedules are unchanged.
- Samsung constant-current evidence belongs to the source instance. A mutex serializes sensor reads and evidence updates across collectors; equal readings count at most once per two seconds. A changed signed value or a gap greater than six seconds since the last counted observation restarts the count. Six seconds covers three normal polling intervals and the live notification's five-second one-shot cadence. Missing readings do not refresh evidence. Measurements are read afresh on demand, with no current replay cache or polling after collectors detach.

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

Defined routes in code (15 destinations; 12 are argument-free direct routes):

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
- `MainActivity` saves a pending validated route under Bundle key `pending_notification_route`, restores it before reading a new Intent, removes consumed route extras, and clears pending state after navigation acknowledges it. `onNewIntent` updates the Activity's Intent. This is explicit notification routing; the manifest declares no HTTP/app-link intent filter.
- Direct/deep-link navigation waits until Pro access is ready, then resolves `charger` and `app_usage` through `resolveProRoute`; protected routes redirect to `pro_upgrade` for free users.
- `navigateNested` constructs the expected parent stack for Speed Test under Network and Charger Comparison under Battery when navigation starts from Home or Insights.
- Speed Test reuses the parent Network `NetworkViewModel` when that parent entry exists, keeping connection context consistent across the nested flow.
- Route-level screen composables invoke their composable ViewModel provider internally. Network Detail and Speed Test are the parent-sharing case; other provider-based screens still use their own route scope by default.
- Standard destination transitions combine horizontal slide and fade over 300ms. Fullscreen chart uses its own scale/fade motion tokens. Both paths remove transition motion when reduced motion is active.
- `RuncheckNavHost` applies and consumes horizontal and bottom safe-drawing insets for every route; top bars handle the top inset.
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
- Source-set-specific `SentryInit` initialization; debug may report through `sentry-android-core` when a supported DSN input is configured. Release initialization is a no-op and the dependency is declared only for debug; a fresh resolved release classpath/artifact was not inspected here.
- Debug-only StrictMode policies for thread and VM issue logging
- Hilt WorkManager factory through `Configuration.Provider`

Initialization uses an application coroutine scope backed by `SupervisorJob` and `AppDispatchers.default`. Billing/Pro initialization, notification-channel creation, monitor scheduling, and Pro-driven widget refreshes are started as separate application-scope jobs so one failing child does not cancel the others.

### Background Monitoring

Three periodic WorkManager jobs are scheduled through `MonitorScheduler`:

All three requests use `ExistingPeriodicWorkPolicy.UPDATE` and currently set no explicit WorkManager constraints. `MonitorScheduler.cancel()` cancels all three, but there is no persisted monitoring-enabled preference or user-facing master stop switch. Startup and boot/unlock call `ensureScheduled()` using the interval preference. Disabling alert notifications does not disable collection, retention, or insight generation.

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
  - has no explicit battery-level constraint; Android/OEM scheduling restrictions still apply
  - invokes `RefreshAppUsageSnapshotUseCase` first; that use case is a no-op unless the current `ProStatusProvider.isPro()` value is true
  - cleans up old readings only after Pro status is ready; unresolved Pro state makes this run retry, free access retains 24 hours, and purchased Pro uses the selected retention preference
  - refreshes widgets best-effort; widget refresh failure does not force a retry
  - retries when app-usage collection fails, cleanup fails, or Pro readiness prevents cleanup
- `InsightGenerationWorker`
  - unique work name: `insight_generation`
  - evaluates persisted Room history through the Insights Engine
  - requests periodic insight generation with a 6-hour interval
  - has no explicit battery-level constraint; actual delivery timing remains platform-controlled
  - retries only on `SQLException`; cancellation is rethrown

Supporting monitor components include:

- `BootReceiver`
- `ScreenStateTracker`
- `MonitoringAlertStateStore`
- `NotificationHelper`

Boot recovery and notification rules:

- `BootReceiver` accepts boot completed, package replaced, and user unlocked. It skips locked-user startup until unlock, uses `goAsync()`, initializes screen tracking, schedules periodic work, and conditionally restores the enabled live notification when permission allows. Its pending result is finished in `finally`.
- Interval updates use the singleton `SetMonitoringIntervalUseCase` mutex, persist the preference, then reschedule. DataStore and WorkManager are not one transaction; a scheduling failure can follow a successful preference write.
- Sensor steps are independent: a later failure does not roll back an earlier saved reading. The monitor still attempts remaining steps. A recoverable latency failure may yield a network row with null latency.
- Low battery fires at `level <= threshold` on first/descending crossing; temperature and storage fire at `value > threshold` on first/upward crossing. Charge complete requires the prior state CHARGING and the new state FULL plus an unfired debounce flag. Master/type preferences control alerts, not collection.
- Alert snapshot/debounce persistence precedes notification posting. This avoids duplicate posting on a repeated observation but does not guarantee delivery after a posting failure.
- `HealthMonitorWorker` has four possible attempt indices (0 through 3) before abandoning a failed periodic cycle. Maintenance retries and insight SQL retries have no equivalent explicit attempt cap in their code. Insight non-SQL failures return failure.
- WorkManager constraints and enqueue calls do not prove exact wall-clock delivery, OEM background survival, boot timing, notification visibility, or foreground-service policy acceptance.

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
- Current can include calculated watts when voltage is available. The setting internally named `liveNotifDrainRate` currently controls the textual charging-status line.
- LOW-confidence current includes an Estimated text label; UNAVAILABLE current is omitted. The battery widget preserves the same LOW/UNAVAILABLE distinction through `BatteryWidgetSnapshot.currentConfidence`.
- “Screen stats” currently reports only that screen tracking is active; it does not render accumulated screen-on/off durations.
- “Remaining time” currently shows an estimating placeholder while discharging; it is not yet a calculated remaining-time estimate.

The 5-second delay follows each read/update, so it is a cadence rather than a hard real-time timer. Special-use foreground type is API-34 guarded. Live mode is free and separate from periodic monitoring; default current/status/temperature lines are on, screen/remaining lines off. HIGH current also has an Accurate label. `ACTION_STOP` returns START_NOT_STICKY, while normal starts return START_STICKY. Binding count, enabled preference, idle timeout, permission, and task-removal handling govern stopping; source code does not guarantee Android will restart the service.

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

Widget freshness is recalculated when its input flow emits, not on a continuous age timer. Its wall-clock threshold is three monitoring intervals; Home's heartbeat banner uses a different uptime-based policy. The health widget calls the four-input calculator without a recent speed test, so its score can differ from Home even with similar sensor data. Providers have no dedicated Error render state; a DAO/provider exception is not guaranteed to become Empty. The shared refresh helper updates Battery then Health, so a Battery update exception can prevent that invocation's Health update.

---

## Measurement Reliability and Health Score

### Measured values and confidence

- Battery current uses `MeasuredValue<T>` with `Confidence`; other sensor fields use the scalar/nullable/default conventions detailed below.
- Internal confidence enum values are `HIGH`, `LOW`, and `UNAVAILABLE`.
- The UI maps those internal values to resource-backed badge labels: `HIGH` → Accurate, `LOW` → Estimated, `UNAVAILABLE` → N/A.
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

- `domain/model/ThermalStatusPersistence` owns the persistence contract: thermal-reading INTEGER codes are explicitly NONE=0, LIGHT=1, MODERATE=2, SEVERE=3, CRITICAL=4, EMERGENCY=5, SHUTDOWN=6; throttling-event TEXT identifiers are explicitly `NONE`, `LIGHT`, `MODERATE`, `SEVERE`, `CRITICAL`, `EMERGENCY`, `SHUTDOWN`. Writers no longer depend on enum ordinal/name. Existing valid rows remain compatible without a Room schema, version, or migration change.
- Unknown persisted codes/identifiers decode to null, never NONE. Heat/drain analysis excludes the aligned sample without substituting older context; thermal patterns exclude unknown rows before sample counts and ratios; recurring throttling excludes unknown events. Active-event restoration fails explicitly before any event write. A fresh Health widget input with unknown thermal status renders `WidgetRenderState.Empty` without calculating a score; freshness checks retain precedence. CSV exports valid identifiers and preserves unknown integer values as numeric text. Event rows retain raw status text and use the unavailable color for unknown identifiers.
- Android platform mapping remains separate: unsupported or unknown platform thermal status still falls back to NONE.

- Battery temperature comes from `ACTION_BATTERY_CHANGED`.
- Thermal status uses `PowerManager.currentThermalStatus` and `OnThermalStatusChangedListener` on API 29+.
- Thermal headroom uses `PowerManager.getThermalHeadroom(10)` on API 30+ and polls every 3 seconds.
- CPU temperature currently emits `null`; no sysfs thermal reads are used.

Network reliability:

- Current connection details are read through Android network APIs. Wi-Fi quality thresholds are strictly >-50/>-60/>-70/>-80dBm; cellular 5G uses >=-65/>=-80/>=-90/>=-110 and other cellular uses >=-98/>=-108/>=-118/>=-128 for Excellent/Good/Fair/Poor. Below these thresholds is No Signal. These fixed model thresholds do not dynamically apply carrier tuning.
- Latency uses five TCP-connect samples against `BuildConfig.LATENCY_HOST` / `BuildConfig.LATENCY_PORT`, with a 1.5s per-sample timeout and 6s total timeout.
- Jitter is computed with an RFC 3550-style moving jitter formula when at least four samples are available.
- Network detail and speed test may display signal, latency, Wi-Fi standard, cellular subtype, DNS/IP/MTU, and VPN state when Android exposes them.
- Network history and Speed Test rows persist raw `ConnectionType.name` strings. Network repository reads and CSV export preserve Network history strings exactly. Typed persistence decoding accepts only exact, case-sensitive current enum names without normalization; an unknown value is not `NONE`. Signal history retains a numeric unknown-type point with unknown context and existing line breaks, while a fresh Health widget input with an unknown required Network type renders `WidgetRenderState.Empty` after freshness validation. Speed Test reads return null for an unknown latest row and skip unknown rows in recent history without deleting or rewriting them; retained known rows keep DAO order, and the original query limit is neither expanded nor refilled after filtering.

Storage reliability:

- Aggregate app/data/cache bytes use `StorageStatsManager.queryStatsForUser(...)` and are unavailable when the service, access, or platform call is unavailable.
- App count is a distinct count of launchable packages visible through `PackageManager.queryIntentActivities(Intent.ACTION_MAIN + Intent.CATEGORY_LAUNCHER)`; it is not a full installed-app inventory.
- Storage encryption status uses `DevicePolicyManager.storageEncryptionStatus` and maps public platform states to FBE, Encrypted, Inactive, Unsupported, or unavailable.
- No `SystemProperties` reflection or `PackageManager.getInstalledApplications(...)` package inventory is used for these storage values.

### Availability, fallback, and interpretation boundaries

`MeasuredValue` is concretely used for battery current; the full sensor model is not uniformly confidence-wrapped. Other values use scalars, nullable fields, enum defaults, or estimated labels. Reviewers must distinguish a documented confidence policy from this actual representation.

| Input | Current normalization/fallback | Interpretation limit |
|-------|--------------------------------|----------------------|
| Battery current | Missing/sentinel/error, raw zero, or normalized magnitude above 10,000mA is UNAVAILABLE; raw µA are integer-divided by 1,000 and sign-aligned. Nonzero sub-mA raw values can truncate to displayed 0mA without becoming unavailable. Samsung repeats of absolute current >=3,000mA reach LOW after three qualifying equal observations. | Unavailable objects may retain a numeric value; consumers must inspect confidence, not value alone. |
| Battery level/voltage/temperature | Sticky-intent defaults include level 0, scale 100, voltage 0mV, temperature 0°C; non-positive scale produces level 0. Level normalization uses Long multiplication and clamps the percentage to 0..100. | Voltage/temperature have no general plausibility filter; these defaults are not validated hardware observations. Non-positive voltage incurs no battery-score penalty. |
| Charge counter and capacity | Positive µAh counter is integer-converted to mAh; full capacity uses level and a 500–20,000mAh filter. | Remaining/derived capacity is an estimate; health percentage and design capacity remain null. |
| Thermal status/headroom | Before API 29 status defaults to NONE; before API 30 headroom is null. Unknown status codes map to NONE. Headroom rejects NaN/negative/error results. | NONE can mean platform fallback; headroom is not a CPU temperature and has no source-level upper plausibility cap. |
| Network signal | Wi-Fi rejects sentinel/out-of-range RSSI. Cellular selects available signal data and rejects integer sentinels; non-radio transports use a GOOD fallback. | Signal category is transport-dependent; missing radio readings cannot be replaced by invented dBm. |
| Primary storage | Validated StorageStats total/free values fall back to StatFs for the data directory. Optional media/app/cache values can remain null. | App data already includes cache; adding cache again double-counts. Separate portable-volume capacity can be unavailable even when a removable volume exists. |
| Device capabilities | Cached profile includes public-API capabilities; current thermal-zone list is empty and storage-health availability flag is true. | The storage flag is not flash-wear measurement, and a capability flag is not proof every subsequent sensor read succeeds. |

`BatteryRepositoryImpl` owns the process-lifetime source cache and guards initialization with its coroutine Mutex and double-checked cache lookup. The cache is assigned only after successful profile retrieval and source construction, so failed initialization can be retried. `BatteryDataSourceFactory` is a stateless manufacturer/runtime-API selector and constructor; it performs no profile-key caching or source replacement. Shared battery broadcasts replay one sticky snapshot, while current/charge-counter sampling is demand-driven at two-second intervals. `GenericBatterySource.close()` remains available to cancel its own scope, but production does not close or replace the repository's source. Sampling intervals do not imply a continuously recorded two-second Room history; the worker writes on its separate periodic schedule.

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

Exact score thresholds below come from `domain/scoring/HealthScoreCalculator.kt`. Each subsystem clamps to 0–100; overall score is the integer rounded weighted sum, `(battery*40 + network*25 + thermal*25 + storage*10 + 50) / 100`.

| Component | Ordered ranges and penalties (points subtracted from 100) |
|-----------|----------------------------------------------------------|
| Battery health | GOOD 0; OVERHEAT 40; DEAD 80; OVER_VOLTAGE 50; COLD 20; UNKNOWN 10 |
| Battery temperature °C | <0:30; [0,10):15; [10,20):6; [20,32):0; [32,35):3; [35,40):10; [40,45]:25; >45:40 |
| Battery voltage mV | <=0:0 (missing); (0,3200):20; [3200,3500):10; [3500,4250]:0; >4250:15 |
| Optional battery health % | null or >=95:0; [90,95):3; [85,90):7; [80,85):12; [75,80):18; [70,75):24; [60,70):35; [50,60):45; <50:60 |
| Live network signal | EXCELLENT 0; GOOD 5; FAIR 15; POOR 35; NO_SIGNAL 70 |
| Live network latency ms | null or <50:0; [50,100):5; [100,200):10; [200,500):20; [500,1000):35; >=1000:50 |
| Thermal battery temperature °C | <10:20; [10,20):8; [20,30):0; [30,33):2; [33,35):5; [35,40):15; [40,45]:35; >45:60 |
| Optional CPU temperature °C | null or <50:0; [50,60):5; [60,70):15; [70,80):30; >=80:50 |
| Thermal status | NONE 0; LIGHT 10; MODERATE 25; SEVERE 45; CRITICAL 65; EMERGENCY 80; SHUTDOWN 95 |
| Storage used % | <10:4; [10,25):2; [25,50):0; [50,70):5; [70,75):15; [75,85):35; [85,95):55; >=95:80 |

The fresh-speed-test path uses **component scores**, not the live penalties: signal Excellent/Good/Fair/Poor/None is 100/90/70/40/0; ping <=0 is 80, then positive <30/<50/<100/<200/<500/otherwise is 100/95/85/70/50/30. Download score truncates `min(downloadMbps / expectedMbps, 1) * 100`, with expected 50Mbps for Wi-Fi/Ethernet and 20Mbps for cellular/VPN. Jitter <5/<15/<30/<50/otherwise scores 100/85/65/45/20. The 40/30/20/10 weighted component mean truncates; missing jitter removes its weight. Upload speed is not scored.

Speed-result reuse matches only connection **type** and age; the stored result has no default-network handle or SSID identity check. A different Wi-Fi network within the hour can therefore reuse an earlier Wi-Fi test. The 55–60 minute blend uses rounded interpolation. `HealthScoreDiagnostics` records penalties, component scores, age/weight and DISCONNECTED/LIVE/SPEED_TEST/FADING_SPEED_TEST mode.

Battery level/current/confidence do not directly enter battery scoring. Missing optional CPU/capacity/latency data generally adds no penalty, rather than reducing the score's confidence. Disconnected network still contributes its internal zero to the overall weighted score even though the UI labels that tile Unrated. These are product heuristics, not a calibrated probability of hardware failure.

---

## Home Screen

Home is the single entry point and aggregates the latest device state from battery, network, thermal, and storage.

Main sections:

- Cardless peach health gauge with 22 rounded segments across 168 degrees.
  Fill is rounded from the clamped score (100 -> 22, 74 -> 16, 48 -> 11, 22 -> 5).
  The actual score and verdict remain visible; centered update age says Updated
  just now for the first minute, then pluralized minutes.
- Peach Run full check action reuses `HomeViewModel.refresh()` -> `loadHome()`.
  Duplicate taps remain disabled; the indicator finishes after a new snapshot and
  at least 900ms from the tap. A 12-second timeout, failure, or stop clears it.
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
- Monitoring stale state is derived from the last worker heartbeat and becomes stale after more than 3x the longer of the heartbeat's interval and the current interval. Same-boot awake uptime avoids deep-sleep warnings; a stored/current `Settings.Global.BOOT_COUNT` mismatch or decreased uptime selects wall-clock age instead. Missing/unknown boot metadata cannot establish a mismatch. A missing heartbeat/required recorded field does not itself trigger this banner.
- Home marks only its currently displayed unseen insight rows as seen through `InsightRepository.markSeen(ids)`.
- Home observation is lifecycle-owned through `LifecycleStartStopEffect`; leaving Home cancels the active load job and clears a running full-check indicator.

Full Check reads the live observation graph; it neither persists a new four-table snapshot nor initiates NDT7 or explicit insight regeneration. Home uses the unmeasured network-state use case, so it does not itself add the Network Detail latency loop. A 15-second freshness tick updates age-dependent scoring; this does not reset the sensor update timestamp. The 12-second refresh timeout clears the progress indicator without necessarily stopping the ongoing live collector.

---

## Insights Screen

The dedicated Insights route shows the complete active insight list that is visible for the current Pro state; Home uses a separately ranked subset capped at three.

Current behavior:

- `InsightsViewModel` combines active persisted insights and `ObserveProAccessUseCase`.
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
- Current confidence is internally `HIGH`, `LOW`, or `UNAVAILABLE`; badge copy presents those as Accurate, Estimated, or N/A.
- Remaining mAh comes from the public BatteryManager charge-counter value when the platform provides one.
- Estimated full capacity is shown as an estimate only when it can be derived from remaining mAh and current battery level inside the repository's plausible range.
- Design capacity is intentionally absent from the UI because no stable public design-capacity source is used.
- Current stats are tracked in-memory and reset when charging status or the selected charger session changes
- Session and history charts can open a fullscreen landscape chart route
- History charts use "Instrument Sweep" animation (grid fade → illuminated sweep reveal → latest-value emphasis)
- Live charts use eased scroll interpolation and a single settling halo on new data; live current remains signed around a visible zero reference
- Battery screen also consumes dismissed educational/info cards
- Charger session tracking runs from Home and Battery live observation and from `HealthMonitorWorker` so charge sessions can be updated in foreground and background.
- Available history periods are Since Unplug, 1 hour, 6 hours, 12 hours, 1 day, 1 week, 1 month, and All. `GetBatteryHistoryUseCase` clamps free access to the last day; Pro receives the requested period, and All is capped at 5,000 rows. Raw repository reads are not entitlement-clamped.
- The current charging-session chart remains available without Pro. Persisted long-range battery history and its fullscreen source are Pro-gated.

Battery analysis contracts:

- `ScreenStateTracker` owns persisted screen-on/off duration and percentage-point drain since charging/power transitions. It listens for screen, power, and device-idle broadcasts and reconciles state on reads. Its SharedPreferences file is `screen_state_tracker`, separate from Room and the three DataStores.
- Screen drain uses level differences clamped non-negative and cumulative drain bounded to 100 points; rate appears only after more than one minute in that screen state. Sleep analysis appears after at least one minute of tracked screen-off time. "Deep sleep" means Android `isDeviceIdleMode`; "held awake" means screen-off without that flag. Durations use `SystemClock.elapsedRealtime()`, including sleep, and are state approximations rather than measured CPU residency or attribution to a wakelock/app. Missed process-off transitions still limit precision.
- `ScreenStateTracker` persists a boot count with elapsed-time anchors, converts legacy wall-clock anchors once, and on a detected reboot resets anchors to now while preserving completed durations. Restored anchors are bounded to 0..now, durations to non-negative values, battery levels to 0..100, and finite drain percentages to 0..100; wrong preference types start a fresh interval. This clock differs from Home heartbeat's awake uptime clock.
- In-memory live buffers and current min/max/mean are ViewModel-owned, with at least two available current samples required for stats. LOW samples remain estimates. Session statistics loading is cached until refresh; it is not recalculated on every history emission.
- `GetBatteryStatisticsUseCase` reads a default ten-day raw history window, sums level gains/losses, counts observed charging sessions (including one when the first row is already CHARGING), and estimates 100% runtime from average discharge rate only above 0.1 percentage points/hour. The caller applies the Pro presentation gate.
- The displayed history drain calculation needs two readings spanning at least ten minutes and a positive first-to-last level drop. It is separate from the engine's adjacent-discharge-pair algorithm.
- `calculateChargingSessionSummary` takes the trailing contiguous CHARGING rows while the current status is CHARGING. Average pace needs ten minutes and positive gain; fallback recent pace uses the last four readings over at least five minutes. Remaining-to-80/100 estimates need pace >=0.25 percentage points/hour and a target above the current level. These linear estimates do not model charge taper.
- Delivered mAh uses trapezoidal current integration only across positive intervals <=30 minutes with both currents present, clamping the interval mean current to non-negative. Average current divides rounded delivered mAh by the sum of those same valid interval durations, excluding missing-current and long-gap intervals. Graphs break across gaps over 30 minutes; unavailable currents are absent. Chart availability needs only two non-null current readings and is independent of the stricter remaining-time panel conditions.

---

## Charger Comparison

Charger Comparison is a purchased-Pro route backed by charger profiles, charging sessions, selected-charger preference state, and `ChargerSessionTracker` measurements.

Current behavior:

- Navigation applies `ProRouteGate`, and `ChargerViewModel` independently fails closed to `Locked` when `IsProUserUseCase` is false. Add, delete, select, and clear-selection actions also return without work for non-Pro state.
- Observation is lifecycle-controlled. Losing Pro access cancels the active charger-data load and exposes the locked state.
- Charger mutations have their own single active `mutationJob`; repeated mutations are ignored while it runs. Refresh restarts the comparison/preference observer without cancelling that mutation, and lifecycle observation stop cancels observation jobs. Mutation cancellation propagates; other failures become the screen error state.
- Users can add a non-blank charger name, select one profile for future sessions, clear that selection, and delete a profile through a confirmation dialog. Repository insertion trims surrounding whitespace.
- Deleting the selected charger clears the DataStore selection first. Room's charger-session foreign key uses `ON DELETE CASCADE`, so deleting a charger also deletes its persisted sessions.
- Charger summaries are sorted by most recent use. They include all-session count and active-session state, while average/latest speed and power plus estimated time-to-full use completed sessions only.
- Stored average power remains canonical when each consumer accepts it. Comparison preserves every non-null stored value, including zero and negative values; only a missing value is reconstructed from session-average current and voltage with Long-safe, truncating integer arithmetic. Charger Performance Insight accepts only positive stored or reconstructed power, so reconstructed zero/negative eligibility remains consumer-specific. The UI falls back to current when power is unavailable.
- Historical comparison ranks chargers by average power, then average charging current, and displays per-charger latest/completed-test context. Profiles without history remain listed but do not enter the comparison chart.

The singleton tracker serializes updates, rejects timestamps older than its last successful update, and throttles unchanged observed charging state to 15 seconds. It opens a session only while CHARGING with a selected profile, completes it on stop/full/deselection, and transactionally completes/starts when the selected profile changes. Completed measurements derive from persisted charging rows; fallback live current is allowed only while still CHARGING and not UNAVAILABLE. Tracker-derived stored power is integer mW (`current mA * voltage mV / 1000`); each historical or live measurement pair uses a Long intermediate with truncating division, and a divided result outside the Int range is unavailable. Chart power is W. These measurements reflect battery-side samples and user-assigned charger labels, not detected USB charger identity or wall-outlet power. Clearing a selected profile in DataStore and deleting it in Room are separate operations.

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

The detail loop measures immediately, then delays 30 seconds after each attempt. The worker independently probes the active validated network during its collection step. DNS resolves through the selected Android Network and sockets are network-bound; the latency timer measures the TCP connect after DNS, without a TLS handshake or ICMP echo. Successful samples are averaged and rounded. A total six-second timeout returns unavailable even if earlier samples succeeded. With four or more samples, jitter uses successive connect-time differences and `J += (abs(delta) - J) / 16`; positive results are rounded upward. This is connect-time variability, not measured packet loss or an end-to-end application request.

Wi-Fi name/BSSID visibility depends on precise location permission and location services; placeholders are normalized away. API-31 callback flags/telephony display-info registration have version guards and registration cleanup. Cellular generation can use basic phone-state fallback, while signal dBm/ASU remain nullable. Android callbacks and link properties provide IP/DNS/MTU without additional probe traffic.

Cellular ASU is accepted only in 0..97; sentinels such as 99, 255, and Int.MAX_VALUE remain unavailable. NDT7's `DefaultNetworkIdentityLock` captures both the Android Network and resolved connection type; capability changes that alter transport fail an active test even when the Network object is unchanged.

Historical chart behavior:

- Metrics: signal strength or latency
- Period selection is stored in ViewModel saved state
- Signal history is a neutral numeric dBm chart in embedded and fullscreen views. It uses the normal chart line color without quality zones or live signal-quality thresholds, including pure Wi-Fi, 5G, and other cellular histories. The numeric axis follows actual data; constant signal history uses a neutral ±1 dBm margin.
- Persisted connection type/subtype stays attached to each retained signal point. Tooltips retain dBm and timestamp and add the known connection label; absent/generic cellular subtype does not imply a generation. Accessibility identifies Wi-Fi, 5G, cellular, or mixed network history using the complete input before downsampling; trend wording describes numeric movement, not quality.
- Wi-Fi, 5G cellular (the shared case-sensitive `contains("5G")` policy), and other cellular are separate continuous families. Null signal and disconnected rows break the line. VPN/Ethernet without dBm produce no point; numeric rows with unknown radio provenance remain isolated rather than assuming a bearer.
- Segment identities are assigned before null removal. Existing triangle-area downsampling retains its numeric selection and endpoint behavior at 300/600 points; a line break is retained whenever sampled neighbors belong to different original segments, even if an intervening family or null interval was entirely omitted. Short segments can be reduced away but never reconnect across the gap. Equal timestamps/values retain their own context. Isolated signal points are visible as neutral dots.
- Live `NetworkSignalQuality` classification, latency history, health scoring, insight policies, measurement collection, and Room schema/migrations are unchanged.
- Fullscreen chart route is available from the chart section
- `GetNetworkHistoryUseCase` applies the shared period set, clamps free history to one day, and caps All at 5,000 rows. Raw repository reads are not Pro-clamped. The UI exposes persisted history only to Pro. Since Unplug uses the last charging timestamp for battery, but a zero start timestamp for network; it is not a shared network unplug event.

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
- Accessibility live announcements follow the test phase; frequently changing metric values remain readable without a live region.
- The ViewModel wraps the complete run in a 90-second timeout.
- Server metadata is extracted from NDT7 `ClientResponse.origin` / `ClientResponse.test` when present.
- `SpeedTestHistoryPolicy.resultLimit(isPro)` owns the shared Free 5 / Pro 100 count policy. `NetworkViewModel` uses observed access for its history query; `FinalizeSpeedTestUseCase` checks current access through `ProStatusProvider` and passes the same policy's limit to `saveResultAndTrim`. Save and trim share the unchanged repository transaction path.
- Phase progress comes from NDT7 `ClientResponse.appInfo.elapsedTime` in microseconds divided by 10,000,000 and clamped to 0..1; it is not derived from time spent discovering a server or waiting for the first callback.

Only cellular connections request the confirmation dialog; Ethernet/VPN are not treated as cellular solely because they are non-Wi-Fi. The active-session guard rejects concurrent tests. The NDT7 HTTP client binds both sockets and DNS to the selected Network. Cancellation/failure closes the session, stops NDT7, cancels HTTP calls, evicts connections, and unregisters the network callback.

Initial TCP ping/jitter can be replaced by NDT7 `tcpInfo.rtt` / `rttVar` converted from microseconds to milliseconds. A stored jitter result therefore need not have the same algorithm as the initial latency measurer's jitter. The library selects its endpoint; source configuration alone does not prove the geographically nearest server, live server metadata, transfer accuracy, or successful persistence.

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

The singleton throttling tracker opens at SEVERE, updates a record only for a higher peak status, and closes below SEVERE. It restores an unfinished Room event after process recreation. Duration uses a monotonic clock for an event opened in the current process and a non-negative wall-clock difference for a restored event. Foreground-app labels require Usage Access and use recent usage evidence; they are contextual labels, not proof that the named app caused throttling.

`AppUsageDataSource.getCurrentForegroundApp()` uses a separate two-minute recent-use lookup, selecting the latest nonblank package only when `lastTimeUsed` lies inside `[start, end)`. Older/future daily aggregates are excluded. This contextual label lookup is distinct from the event-based foreground-duration collector.

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

- `StorageRepositoryImpl` reads current capacity and a seven-day Room history window, then uses `StorageGrowthAnalyzer` directly before delaying 30 seconds between updates. At least three readings and a non-zero regression denominator are needed. Positive bytes/day and positive available bytes produce a truncated d/w/mo/y estimate. The 14-day Insight projections are a separate caller/window; neither estimate is a guaranteed fill date.
- A non-positive total capacity yields a 0% live usage fallback. Missing app bytes are stored as `-1L`. The Storage data-layer persistence decoder maps every negative persisted app or media byte value to null at entity-to-domain boundaries. Future unavailable media totals (missing breakdown, negative category, or total overflow) persist as `-1L`, and CSV exports decoded null values as empty cells. Measured zero remains zero. Pre-change zero rows are intentionally unchanged and inherently ambiguous. Null/unavailable does not identify the exact cause, and a non-null media breakdown does not guarantee complete device-wide MediaStore visibility under partial permissions. Live/storage-DAO failures can prevent the combined state from emitting even when capacity itself was readable.
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

Permission and deletion scope is narrower than the labels may suggest. On API 34+, `MediaAccessState.FULL` means READ_MEDIA_IMAGES **or** READ_MEDIA_VIDEO is granted, not that image/video/audio permissions are all granted. The Storage UI separately calculates `hasAllMediaPermissions` from an empty missing-permission list and gates its breakdown/cleanup affordances with that stricter check. Underlying MediaStore access still depends on actual per-category grants. Below API 34, the helper requires all permissions in that API's requested list.

Trash uses a separate API-30+ system request for its URI list. On RESULT_OK Storage refreshes; it does not use Cleanup's 2,000-URI batching and per-selection freed-byte verification. Trash collection queries can degrade to an empty list on provider failure. Do not describe all MediaStore paths as uniformly strict or all successful consent results as verified deletion.

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
- Old Downloads and APK cleanup require API 30+ through `CleanupType.minimumSupportedApi`, checked by `CleanupViewModel`
- APK cleanup preselects all groups by default
- Old Downloads keeps one scan-start timestamp across its summary, pages, and group-selection resolution so age boundaries do not drift during a scan
- Selected filter is stored through `SavedStateHandle`; restored out-of-range indices fall back to the cleanup type's default
- Route is Pro-gated in the ViewModel; non-Pro users receive a locked error state before scanning
- Storage, thermal, network, and battery trend sections share `HistoryPeriodFilterChipRow`, `HistoryLoadErrorMessage`, and `ChartStatsRow` for period chips, history-load failures, and min/avg/max chart stats.

Deletion and paging boundaries:

- A missing route/SavedStateHandle `type` argument defaults to `CleanupType.LARGE_FILES`; scanning proceeds when the other normal scan conditions are satisfied. An unknown/non-matching `type` value is invalid and yields an error before scanning. Its UI/title fallback to `LARGE_FILES` does not make it a valid Large Files scan request.
- The Storage screen hides cleanup entry points unless `hasAllMediaPermissions` is true. Selected visual access is tracked separately and must not be treated as full access.
- APK summaries from multiple collections are merged into one APK category, summing item counts/bytes and taking the maximum file size.
- APK pages merge deduplicated collection prefixes in descending byte size, then numeric MediaStore ID, then URI order before applying offset/limit. The numeric ID tie-break preserves collection ordering across equal-size page boundaries.
- `CleanupPagingSource` rethrows cancellation and exposes actual load failures through Paging. `CategoryGroup` renders refresh/append failures and retry controls, keeping a provider error distinct from an empty group.
- Resolving a selected group into URIs can fail; the ViewModel reports an error and does not start deletion with a partial inferred selection.
- Legacy deletion and API 30+ consent both verify remaining MediaStore URIs against the original selection and captured sizes. If every selected URI remains, the selection is restored with a failure message. Partial deletion preserves remaining selection; freed bytes are based on verified disappearance.
- An accepted system dialog alone is not proof of deletion. Verification failure or lost access must not produce an invented successful freed-byte count.
- During the success overlay, the underlying Scaffold clears its semantics. The overlay exits immediately when scanning resumes, removing its announcement before the underlying semantics return.

Legacy and modern deletion share final verification and result handling, which propagate cancellation and calculate disappearance from captured pre-delete sizes. The modern path retains its 200ms pre-verification delay. Other verification failures restore an error/selection state. Deleting state is acquired before asynchronous group-URI resolution, with Pro checked again after resolution and duplicate consent handling guarded. A verified URI disappearance is a MediaStore result, not a measurement that identical physical bytes were reclaimed from storage.

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

The source queries usage events with a 24-hour pre-window lookback to reconstruct initial activity state, then clips accumulation to the collection interval. It tracks resumed activities per package and assigns each elapsed interval to the most recently resumed active package rather than summing overlapping multi-window activity durations. Pause/stop/startup/shutdown events change that state. A successful empty event interval advances the cursor; missing permission/service does not. System/launcher packages are not categorically excluded, and label lookup can fall back to the package name.

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

`FullscreenChartSeedStore` is one process-local slot for matching source/metric/period Success or Empty state. It is consumed once to reduce a loading transition; it is not persisted across process death and never replaces the Pro check or subsequent observation. Invalid route arguments are sanitized to supported fallback selections. Disposal sets orientation to unspecified rather than restoring a saved previous orientation.

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
- The normal Settings actions clear speed tests, clear all data, reset tips, and reset thresholds show a confirmation AlertDialog with primary blue confirm button

The current `monitoringDataOperationMutex` serializes export preparation and clear-all **within one SettingsViewModel**. An UNDISPATCHED launch attempts `tryLock` immediately, rejects overlapping operations instead of queuing them, checks cancellation, and always releases the lock/reset export state in `finally`. It is not a process-wide lock over all possible repository callers. Clear-all success appears only after Room plus post-transaction preference/file cleanup succeeds.

Changing Pro retention persists the new preference before invoking cleanup; cleanup failure does not roll back the preference. Retention choices are 90 days, 180 days, 365 days, or forever. Free effective retention remains 24 hours. "Reset tips" resets dismissed educational cards, not Learn article read history; clearing speed results is separate from the full monitoring reset.

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

`clearPreparedExports()` now throws IOException when recursive export deletion reports failure. That failure reaches reset instead of falsely confirming completion. Atomic move has no non-atomic fallback, and stale cleanup is best-effort after a successful move. Export builds CSV content in memory and performs category checks/reads sequentially, so it is not a streaming export or one synchronized cross-category snapshot. FileProvider sharing uses read-granted ClipData for all URIs; external recipients control their own copies after sharing.

`prepareExportShare()` rechecks Pro after CSV construction and again after URI preparation. If access is lost during preparation, it clears prepared exports and fails instead of returning share URIs; cleanup failure itself may propagate. Filename extension matching is case-insensitive, but path separators and the bare `.csv` name are rejected. These checks do not revoke copies already received by another application.

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
- DataStore `monitoring_status` for the last successful periodic worker heartbeat's wall time, awake uptime, monitoring interval, and optional boot count
- DataStore `monitoring_alert_state` for the previous alert snapshot and charge-complete debounce state
- SharedPreferences `pro_status_cache` for synchronous cached purchase status during release cold start
- SharedPreferences `screen_state_tracker` for screen/power/idle session accounting; `runcheck_db_events` for the one-time destructive-migration notice

`DeviceProfileRepositoryImpl` reuses decoded device-profile JSON when its API level matches the current OS. Refresh and stale/missing-profile detection persist through `DeviceDao.replaceCurrent`: insert/replace first, then remove other profiles in one Room transaction. The table may be empty before initialization; after a successful write, exactly one current row remains. A failed replacement cannot leave a partially committed replacement. `firstSeen` still inherits from the post-detection `getDeviceSync()` row selected by `ORDER BY first_seen DESC, id DESC LIMIT 1`, even when its ID differs; only a missing row uses the current wall time. No update timestamp exists. This is an API-level cache policy, not per-read revalidation of hardware behavior; JSON/DAO errors can still propagate.

### Preference keys and deletion scope

| Store | Current keys / ownership |
|-------|---------------------------|
| DataStore `settings` (22 keys) | `monitoring_interval`, `notifications`, `data_retention`, `permission_education_seen`, `app_usage_last_collected_at`, `selected_charger_id`; `notif_low_battery`, `notif_high_temp`, `notif_low_storage`, `notif_charge_complete`; `alert_battery_threshold`, `alert_temp_threshold`, `alert_storage_threshold`; `temp_unit`; `live_notif_enabled`, `live_notif_current`, `live_notif_drain_rate`, `live_notif_temperature`, `live_notif_screen_stats`, `live_notif_remaining_time`; `show_info_cards`, `dismissed_info_cards` |
| DataStore `monitoring_status` (4 keys) | `last_worker_heartbeat_at`, `last_worker_heartbeat_uptime`, `last_worker_heartbeat_interval`, `last_worker_heartbeat_boot_count`; the first three are required to map a heartbeat; boot count is written only when known, otherwise removed |
| DataStore `monitoring_alert_state` (5 keys) | `last_battery_level`, `last_battery_temp_c`, `last_storage_usage_percent`, `last_charging_status`, `charge_complete_fired` |
| SharedPreferences `pro_status_cache` | `is_pro`; synchronous cached entitlement, reconciled with Play |
| SharedPreferences `screen_state_tracker` (13 keys) | `screen_on`, `last_transition_time`, `last_transition_level`, `screen_on_duration_ms`, `screen_off_duration_ms`, `screen_on_drain_pct`, `screen_off_drain_pct`, `deep_sleep_duration_ms`, `held_awake_duration_ms`, `last_idle_check_time`, `last_idle_state`, `last_charging_status`, `elapsed_realtime_boot_count` |
| SharedPreferences `runcheck_db_events` | `destructive_migration_occurred`; consumed/removed by MainActivity |

Dismissed info-card IDs are normalized by base key, preserving the highest `_v<number>` variant. They are separate from Room insight seen/dismissed flags and from the unimplemented Learn read state.

"Clear all data" is a monitoring-data reset, not an application factory reset. It preserves device profile, purchase cache, screen-state SharedPreferences, ordinary preferences, dismissed tips, and monitoring schedules. Only the app-usage cursor and selected charger are removed from settings. The coordinator protects insight generation and thermal-reset consistency, not every sensor writer or charger collector; later/in-flight collection can repopulate history.

Retention cleanup deletes old battery/network/thermal/storage readings, throttling events, app usage, speed results, and completed charger sessions in one Room transaction. It does not delete device profiles or charger names, and insight expiry is managed separately by generation. Free rows become eligible after 24 hours; deletion happens when maintenance/cleanup actually runs, not exactly on the 24-hour clock. Pro FOREVER yields no time cutoff. Speed-result count limits (5/100) apply independently when saving a test.

### Corruption and reset boundaries

- The three preference DataStores (`settings`, `monitoring_status`, `monitoring_alert_state`) use `ReplaceFileCorruptionHandler { emptyPreferences() }`. Corrupt serialized preferences revert to defaults/empty state; this does not claim that every I/O failure is recoverable.
- Ordinary settings and monitoring-status DataStore read failures propagate to the collector rather than silently emitting default preferences or an absent heartbeat. Empty/corrupt data and an unreadable store are distinct conditions; `PreferenceReadFailureTest` records this contract without establishing Android filesystem behavior.
- A wrong-type Boolean in `ProStatusCache` is removed and returns false. `ProManager.initialize()` applies the currently cached purchase value before awaiting purchase-status readiness, then follows subsequent purchase state.
- `ClearMonitoringDataUseCase` deletes readings, throttling events, app usage, speed tests, insights, and charger profiles/sessions in one Room transaction. Device profile rows and the purchase cache are not in this deletion list.
- `MonitoringDataCoordinator` serializes that transaction and debug history seeding with the complete insight read/evaluate/publish operation. Reset waits for admitted generation before deleting its results; later generation reads the remaining or newly collected history. Lock order is coordinator, thermal tracker, then Room. The tracker invalidates its cached active event in `finally` before releasing its lock, including failed or cancelled resets, and restores any surviving Room event on the next observation. Lock waits remain cancellable; monitoring schedules are unchanged.
- After the Room transaction, it separately clears monitoring-related preference state, alert/debounce state through `MonitoringAlertStateRepository`, worker heartbeat, and prepared exports. All non-cancellation cleanup steps are attempted; the first failure is rethrown with subsequent failures suppressed. The overall reset is therefore not one transaction spanning Room, preferences, and files.

Room has 11 entities/tables and ten DAO interfaces (charger profiles and sessions share a DAO). Schema exports 6–10 and the nine registered migrations are versioned separately from source-test counts. Nullable fields include current mA, health percentage, CPU temperature and estimated per-app mAh; battery current confidence is a non-null string. String/JSON decoding is not equivalent to universal corruption recovery.

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
- Purchased but unacknowledged purchases receive at most 3 acknowledgement attempts total, with 2s and 4s waits between attempts
- A purchased item unlocks Pro only after it was already acknowledged or acknowledgement succeeds; exhausted acknowledgement retries keep the entitlement inactive and emit a safe generic purchase error
- Billing 9 purchase sub-response codes distinguish insufficient funds and user ineligibility; unknown codes fall back to a known response-code message or a generic localized error without exposing SDK debug text
- Cached Pro state is restored synchronously in release builds to avoid a free-tier flash while Billing queries run
- Billing availability remains false during service setup/product refresh and reconnectable purchase-query failures. Cached product details alone do not make checkout available while the billing service is reconnecting.

Entitlement and readiness are distinct:

- Release initialization starts from the cached Boolean. A successful INAPP query reconciles matching PURCHASED products: already acknowledged or successfully acknowledged purchases can activate Pro; pending-only/empty results deactivate it. If all acknowledgement attempts fail, access remains inactive.
- A failed purchase query reports UNAVAILABLE without necessarily clearing the previously cached access. "Pro ready" means initialization completed, including failure paths; it is not proof of a fresh successful Play verification. Offline cached Pro can remain active until a successful reconciliation.
- Restore reports ACTIVE, NOT_ACTIVE, or UNAVAILABLE distinctly. `refreshPurchaseStatus` queries purchases, and MainActivity uses it directly on resume. Connection recovery has up to three scheduled delays of 2/4/8 seconds.
- The code uses Google Play Billing client purchase/acknowledgement results and a local cache; it does not implement a server-side receipt-verification backend.
- Debug initialization forces Pro and billing availability but does not establish real ProductDetails or a working checkout. A debug purchase screen is not evidence of a successful paid flow. A formatted price is obtained dynamically from Play; no fixed price is authoritative in this document.

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

All current features share the single `ProState.isPro` decision. The enum classifies presentation; it does not implement separate purchases or per-feature entitlements.

Current enforcement map:

| Surface | Primary enforcement |
|---------|---------------------|
| Charger Comparison, App Usage | Navigation redirect plus feature ViewModel/use-case checks |
| Battery/Network persisted history | UI lock/visibility plus history-use-case period clamping; fullscreen history route also checks Pro |
| Thermal/Storage history | UI lock/visibility plus free history flows that emit no rows |
| Thermal throttling log | Pro UI state/use-case access |
| Storage cleanup | Storage callout plus `CleanupViewModel` fail-closed check before scanning |
| App-usage background collection | `RefreshAppUsageSnapshotUseCase` reads the current Pro state and returns without collecting unless it is already purchased Pro; unlike retention cleanup, this path does not wait for Pro-status readiness |
| CSV export | Settings/ViewModel check and independent `ExportDataUseCase` Pro checks |
| Widgets | Pro checks in widget data providers before exposing stored readings |
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

The source manifest declares 17 permissions and seven application component entries: MainActivity, RealTimeMonitorService, BootReceiver, two widget receivers, FileProvider, and the merge entry for InitializationProvider. Only MainActivity explicitly sets exported=true; BootReceiver is non-exported. There is no browsable HTTP/app-link filter. Notification "deep links" here mean validated route strings in an internal intent extra, consumed once, not an exported website routing contract. Dependency-added components and permissions require a merged-manifest/artifact check; this count is only the checked-in app manifest.

Declared permission surface:

| Permission | Purpose |
|------------|---------|
| `ACCESS_NETWORK_STATE` | Network type/capability detection |
| `ACCESS_WIFI_STATE` | Wi-Fi connection details |
| `INTERNET` | NDT7, TCP latency, Billing, and the conditional debug-only Sentry path |
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
- Sentry is a debug-only dependency and initializes only with a configured DSN. This is an additional debug outbound path beyond the release allowlist; do not describe all build variants as sending no crash data.
- Source-set/dependency configuration supports release exclusion, but this audit did not inspect a fresh resolved release classpath or signed artifact.
- Debug Sentry disables tracing, profiling, auto session tracking, breadcrumbs, activity lifecycle tracing, frames tracking, screenshots, view hierarchy, NDK, performance v2, and auto trace id generation.
- `ReleaseSafeLog` emits Android logs only in debug builds.
- Semgrep has project-specific rules for backup, cleartext, exported components, broad FileProvider paths, sensitive Android logging, outbound network primitives, and release telemetry expansion.

---

### Stored and shared sensitive information

Room can contain battery/thermal timestamps, network radio/carrier/subtype measurements, app package names/labels and foreground durations, thermal-event foreground labels, user-named chargers, and derived insights. DataStore/preferences retain user settings and monitoring state. Live network state can expose SSID/BSSID/IP/DNS to the UI without those fields being part of every persisted network row. Device profile stores manufacturer/model/capability JSON.

The app has no application-layer Room encryption or backend history synchronization in its implementation. Android sandboxing, restrictive backup rules, FileProvider cache scope, and debug-only logging are separate controls, not an encryption claim. CSV is explicitly shared by the user through another application. Privacy-policy/store/feedback actions launch external intents rather than fetching those pages into a runcheck networking client.

---

## Future Considerations

- **Learn article read/unread tracking:** Not implemented. Older prose proposed persisted read state for a larger catalog; this is an optional idea, not a committed requirement or current capability.

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
| AccentRed | `#F06040` | No Material error-role mapping | Accent token; Material `error` uses StatusCritical `#F66A4C` |
| StatusCritical | `#F66A4C` | `error` | Critical/error text and indicators on cards |
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
| Healthy | AccentTeal `#5DE4C7` | Subsystem/overall score 75–100; battery temperature <35°C; storage used <75%; signal Excellent/Good |
| Fair | AccentAmber `#E8C44A` | Score 50–74; battery temperature 35..<40°C; storage 75–84%; signal Fair |
| Poor | AccentOrange `#F5963A` | Score 25–49; battery temperature 40..<45°C; storage 85–94%; signal Poor |
| Critical | StatusCritical `#F66A4C` | Score 0–24; battery temperature ≥45°C; storage ≥95%; No Signal |

Battery Home status is derived from the battery subsystem score, not directly from battery level. Storage and signal have direct presentation mappings in `StatusColors.kt`; do not substitute overall-score thresholds for metric-specific helpers.

Battery-temperature presentation is owned by the Compose-independent `ui/common/BatteryTemperaturePresentation.kt`. It classifies the original Celsius measurement before rounding or Fahrenheit conversion:

| Descriptive band | Celsius range | Severity |
|------------------|---------------|----------|
| Cool | <25 | Healthy |
| Normal | 25..<35 | Healthy |
| Warm | 35..<40 | Fair |
| Hot | 40..<45 | Poor |
| Critical | >=45 | Critical |

`statusColorForBatteryTemperature` maps that severity to theme colors; `temperatureBandLabel` maps the band to existing string resources. Battery Detail values and the live-temperature line use Healthy below 35°C and Poor at 40..<45°C. Home consumes severity directly, retaining its own palette and Healthy / Warm / Running hot / Very hot wording. Thermal hero segments are Healthy / Fair / Poor / Critical, while its detailed word retains all five bands. Thermal session min/max, live line, and throttling-event battery values use the same colors.

Battery history, its fullscreen view, and Thermal history's battery metric share the 35 / 40 / 45°C quality-zone boundaries at alpha 0.06. CPU history has no quality zones because no CPU zone policy is established; its optional temperature pill retains the previous independent color mapping in `statusColorForCpuTemperature`.

HeatStrip uses the shared severity boundaries with its existing ±1°C gradient transitions and descriptive accessibility labels. Its fixed >42°C pulse rule remains separate and disabled with reduced motion. The configurable alert (default 42°C), score penalties, insight thresholds, CPU rules, Android thermal status/headroom/throttling, and >35°C informational-card condition are independent policies. Numeric formatting, collection, storage, exports, widgets, notifications, monitoring, and Pro gating are unchanged.

**Confidence badges:**

| Badge | Background | Text |
|-------|-----------|------|
| Accurate | AccentBlue `#4A9EDE` | BgPage `#0B1E24` |
| Estimated | AccentAmber `#E8C44A` | BgPage `#0B1E24` |
| N/A (UNAVAILABLE) | TextMuted `#7A949E` | BgPage `#0B1E24` |

### Typography

`Type.kt` defines the shared variable `ManropeFontFamily` with explicit 400/500/600/700 weight axes. `HomeManropeFontFamily` aliases that family; correct axis selection is shared by detail screens and Home, not confined to Home. Numeric-font selection remains separate.

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

### Chart data and rendering contracts

Thermal and Storage ViewModels own latest history and its load error independently from sampled sensor state, merging both at collection time after sampling. An early Room emission or a newer history error must survive the first/delayed live update. Fullscreen return-result keys are consumed by setting their SavedStateHandle values to null, preserving the existing StateFlow observers for subsequent returns.

Shared `ui/chart/ChartModels.kt`, `ChartHelpers.kt`, `ChartRenderModel.kt`, and `ChartAccessibility.kt` own metric conversion, labels, tooltips, semantics summaries, and point budgets. Embedded history charts cap display data at 300 points and charging-session charts at 240; fullscreen raises these budgets to 600 and 480 respectively; this is independent of the 5,000-row history query cap. `downsamplePairs` uses triangle-area bucket selection, preserves first/last endpoints for budgets >=2, returns no points for <=0, and returns the first point for budget 1. It assumes ordered input rather than sorting arbitrary callers.

Battery history supports level, temperature, current, and voltage; session graphs support current/power. Network supports dBm/latency; thermal and storage models preserve unavailable data and unit conversions. Storage used-space history ignores non-positive total capacity and bounds available bytes to the total; available-space history rejects negatives and displays SI GB. Session gaps greater than 30 minutes break the line. Missing current is skipped, not plotted as zero.

The shared drawing layer provides trend/area/live charts, quality zones, an instrument-sweep reveal, live interpolation, tooltips and accessible summaries. A tooltip's formatted value and chart semantics do not prove physical-device touch exploration, TalkBack order, font-scale fit, frame time, or GPU cost. The dated host-JVM benchmark below measures data preparation only.

`chartXLabelLeft` bounds oversized X-axis labels without constructing an inverted clamp range; it does not guarantee that every label fits. Inactive segmented-bar labels use full `onSurfaceVariant`. `ProBadgePill` accepts a surface-appropriate content color (default `onSurface`); Home passes its tile foreground so a badge on a light category fill does not inherit inappropriate detail-screen text.

### Localization and resources

Only default English application strings are shipped by the configured locale filter and `locales_config.xml`; there are no translated `values-<language>/strings.xml` inventories in this snapshot. Debug-only insight strings have release-safe empty non-translatable counterparts. Formatters explicitly use English for numbers/dates and file-size formatting. Celsius is the stored/scoring unit; Fahrenheit is a presentation conversion, with the battery-widget exception documented above. Bundled font and launcher resources are local, not network-downloaded UI assets.

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

**Accessibility targets:** 4.5:1 body-text contrast, 3:1 large-text contrast, and 48dp minimum touch targets. These are design requirements; token/helper tests and source inspection do not establish every rendered text pair, focus order, hit region, or system-font configuration meets them.

### Logo

Health-score arc (~210°) wrapping a phone silhouette, rendered in AccentBlue.
The repository root contains `runcheck-logo.svg` and `icon.png`; Android launcher assets live under the normal `app/src/main/res/drawable*` and `mipmap*` resource directories. There is no root `icons/` directory in this snapshot.

---

## Testing and Verification

Current test surface:

- Unit-test tree: 136 Kotlin files under `app/src/test/java/com/runcheck/` in this working-tree snapshot
- Debug unit tests: 3 Kotlin files under `app/src/testDebug/java/com/runcheck/`
- Instrumented tests: 3 Kotlin files under `app/src/androidTest/java/com/runcheck/`
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

### What the test inventory establishes

Static counting of literal `@Test` annotations finds 785 across 129 test-bearing files in `src/test`, plus seven supporting Kotlin files; the 785 includes the opt-in benchmark declaration. `testDebug` has three declarations in three files, and `androidTest` has 11 in three files. These are 799 source declarations, not 799 passing tests; assumptions, runner behavior, source-set selection, and generated cases can change executed counts.

- JVM tests cover rules, score boundaries, ViewModels, repository error contracts, parsing, and policy helpers. Some `*ContractTest` classes read source/config text or use mocks; they establish those assertions, not instrumented Android behavior.
- `HomeScreenTest` exercises extracted geometry/order helpers, not an actual rendered Compose tree. No current instrumented class is a Compose screenshot, TalkBack, font-scale, or navigation end-to-end suite.
- `RuncheckDatabaseMigrationTest` has six cases: 6→8, 7→8, 8→9, 6→9, 6→10 with preserved fixtures, and 9→10 indexes. Registered 1→6 migrations exist but no exported schemas 1–5 support equivalent historical-start migration tests here.
- `MonitoringDataResetTransactionTest` has three real in-memory Room cases for committed reset, rollback, and cancelled-transaction recovery of the thermal tracker. `SpeedTestResultDaoTest` has two cases for flow invalidation and count trimming with protected insertion.
- Additional current regression sources include `SamsungCurrentConfidenceOwnershipTest`, `MonitoringDataResetTest`, `UnseenInsightTrackerTest`, `FileExportRepositoryImplTest`, `SettingsViewModelTest`, `NetworkCallbackRegistrationTest`, `SessionGraphAvailabilityTest`, and `ChartRenderModelTest`. These sources were inspected, not executed.
- Current regression sources also cover preference I/O propagation (`PreferenceReadFailureTest`), heartbeat boot identity (`MonitoringStatusRepositoryImplTest`), screen-accounting recreation/reboot/sanitization (`ScreenStateTrackerTest`), same-network transport changes (`DefaultNetworkIdentityLockTest`), restored cleanup filters (`CleanupFilterSelectionTest`), repeat fullscreen results (`FullscreenChartResultTest`), Manrope axis declarations and theme contrast (`ManropeFontTest`, `ThemeContrastTest`). Export, charger mutation, history-before-live, valid session intervals, duplicate-time drain and large-epoch storage-growth cases live in their existing use-case/ViewModel/rule suites. These are static coverage descriptions, not freshly passing regressions or rendered accessibility results.
- Debug containment and seed tests live in `testDebug`; release-safe debug-action tests are in `test`. They do not replace inspecting a signed release for telemetry/debug tooling.

PowerShell fixture sources under `tools/` are outside the Kotlin counts: `project-root-test.ps1` checks wrapper root forwarding with a fake checker; `sonar-timeout-test.ps1` and `sonar-upload-retry-test.ps1` exercise bounded child-process/report handling and upload-retry classification using fixtures; `export-launcher-icons-test.ps1` distinguishes existing-asset verification, absent SVG generation inputs, and wrong dimensions. `release-gate-test.ps1` extracts the release-task matcher/wiring into a Gradle fixture to check an abbreviated up-to-date release task cannot bypass the gate. None was executed here; fixture/source checks are not a real scan, signed-release build, or launcher rendering test.

### Coverage configuration and exclusions

`:app:jacocoDebugUnitTestReport` depends on `:app:testDebugUnitTest`, emits XML and HTML under `app/build/reports/jacoco/jacocoDebugUnitTestReport/`, and reads source roots `src/main/java` and `src/main/kotlin`. It accepts the Javac, legacy Kotlin, and AGP built-in Kotlin debug class directories and either `jacoco/testDebugUnitTest.exec` or `outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec`.

The exclusion list removes generated/Hilt/Room classes, R/BuildConfig, previews/tests, many rendering classes, theme/components/DI/widgets, startup and selected Android framework boundaries (including sources, preferences, billing, services/workers, logging, and URI handling). Broad `**/*Test*.*` matching also excludes production classes whose name contains Test, including SpeedTest classes. Sonar has its own extensive `sonar.coverage.exclusions`, which is not identical to JaCoCo's list. Coverage percentages consequently describe selected measurable code, not the entire app, all Pro/security boundaries, or all rendering paths. This audit produced no coverage percentage.

Root Gradle lets the Android/Sonar plugin own source and binary discovery instead of applying stale manual paths from properties. The Sonar dependency chain assembles debug, generates JaCoCo, and creates an **empty Android Lint import placeholder** at `app/build/reports/lint-results-debug.xml`. That placeholder prevents unrelated/stale lint import and is not a clean Android Lint result; `lc` owns actual lint. Its task is ordered after lintDebug if both are scheduled.

Current delegated Android-check contract:

- Shared wrappers distinguish CLEAN (exit 0), FINDINGS (exit 1), and technical/configuration ERROR (exit 2) and publish run reports atomically. Read the classified status, exact scope and timestamp together; a partial report, empty report, baseline, or configured task is not a passing run. Wrapper implementation is delegated outside this repository, so its behavior also depends on the resolved Android-check checkout.
- `config/android-check.json` declares one required Android application module, `:app`, with `debug` and `release` variants and the `main`, `debug`, `release`, `test`, `testDebug`, and `androidTest` source sets.
- The configured build surface is `:app:assembleDebug`; the default test surface is `:app:testDebugUnitTest`, and `tc -Full` also runs `:app:connectedDebugAndroidTest` after confirming that a device or emulator is connected.
- Configured quality tasks are `:app:ktlintCheck`, `:app:detekt`, `:app:lintDebug`, and `:app:stabilityCheck`; dependency analysis covers `debugRuntimeClasspath` and `releaseRuntimeClasspath`, and OWASP uses `:app:dependencyCheckAnalyze`.
- `config/check-exceptions.json` contains 33 owned, time-bounded exceptions, all expiring `2026-10-31`: 31 exact mobsfscan entries, 1 Detekt baseline registration, and 1 OWASP false-CPE group. No CodeQL exceptions are currently registered.
- The registered Detekt baseline contains 28 finding IDs in `app/detekt-baseline.xml`; a passing wrapper with that baseline is not the same as zero underlying recorded findings.
- Every current MobSF exception includes one exact rule, one existing `findingPath`, and one or more exact `findingSelectors`. The delegated checker normalizes selector whitespace/casing and suppresses only a finding whose rule, normalized path, and normalized selector all match; another finding in the same file remains visible unless it has its own registered selector.
- `ms -PlanOnly` currently prints each exception's rule and path but not its selector list. That abbreviated plan text does not weaken runtime classification: execution still applies the exact selector matching described above.
- The target-SDK exception is limited to `android_task_hijacking2` at `app/src/main/AndroidManifest.xml` with its exact current finding selector. `.mobsf` contains path exclusions and severity filtering only; it must not suppress that rule globally.
- The Detekt and OWASP entries use their own source/selector contracts rather than MobSF's `findingSelectors` matching. A historical CodeQL exception is not current approval when it is absent from the registry.
- Scanner output must be interpreted after wrapper classification and exact exceptions. A raw-match count of zero is neither required for `CLEAN` nor sufficient if the wrapper reports a technical/configuration error.

Other expiry/baseline scopes must be read separately:

| Artifact | Current inventory and expiry |
|----------|-------------------------------|
| `config/check-exceptions.json` | 33 entries, owner project-maintainers, expiry 2026-10-31; includes the exact reset-transaction-test MobSF selector |
| `config/dependency-check/suppressions.xml` | Three suppression blocks: two exact false-CPE groups expire 2026-10-31Z; the kotlin-stdlib 2.4.10 / CVE-2026-53914 block expires **2026-09-30Z** |
| `gradle/osv-scanner.toml` | One exact advisory GHSA-r937-wjx7-w2jp, ignoreUntil **2026-09-30** |
| `app/detekt-baseline.xml` | 28 CurrentIssues IDs; registration is time-bounded in the exception file |
| `app/stability/app-debug.stability`, `app-release.stability` | Checked-in comparison baselines; no passing regeneration implied by presence |
| `.mobsf` | Path/severity filters; no global target-SDK rule bypass |

No listed expiry has passed on the snapshot date; September 30 is the earliest and is not extended by the registry's October date. Advisory rationale text claiming Kotlin 2.4.20 is still pre-release is historical prose, whereas current configuration already constrains runtime libraries to 2.4.20 and keeps the compiler/plugin at 2.4.10. This audit did not consult upstream advisories or revalidate exception necessity; do not infer a vulnerability is fixed or an exception remains justified solely from these files.

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

## Measured Chart Performance (2026-09-11)

This section summarizes the checked-in September 11 report. No benchmark or test was rerun for the September 13 audit; later changes in the same source file, including charging-summary interval accounting, are not covered by those historical timings. Ignored raw report paths are references from the report, not fresh evidence inspected here.

Two measured sources of avoidable work in `app/src/main/java/com/runcheck/ui/chart/ChartHelpers.kt` were improved:

- `ChargingSessionSummary.hasGraphData()` now stops at the second reading with non-null `currentMa`, instead of constructing complete current and potentially power graph point lists. Both graph metrics use the same current-availability condition. An all-null input still requires one full scan.
- `downsamplePairs()` now calculates timestamp and value sums in one pass per averaging bucket, removing temporary sublists and a second traversal. Point selection arithmetic and accumulation order within each sum are preserved.

The opt-in `app/src/test/java/com/runcheck/ui/chart/ChartPerformanceBenchmark.kt` measures production functions on the host JVM. The recorded environment was Windows 11, AMD Ryzen AI 7 350, Java test runtime `17.0.20+8`, with the existing debug test/JaCoCo configuration. Each case uses at least one second of warmup followed by 15 batches of 200 calls; results below are median time and thread-allocated bytes per call. Input construction is excluded from timing.

Results for the same deterministic 2,880-reading input. Confirmation used a second fresh JVM after the change; allocated bytes were identical in both post-change runs:

| Operation | Before time | After time | Confirmation time | Before allocated bytes | After allocated bytes |
|-----------|-------------|------------|-------------------|------------------------|-----------------------|
| Graph availability, current present | 55.426 µs | 0.016 µs | 0.014 µs | 234,616 | 56 |
| Graph availability, all currents missing | 13.225 µs | 4.638 µs | 4.573 µs | 80 | 32 |
| Downsample to 300 points | 14.755 µs | 12.534 µs | 11.952 µs | 10,784 | 1,248 |

The available-graph check allocated 99.98% less memory, and downsampling allocated 88.43% less. These are allocated bytes, not retained heap. Downsampling time was 15.1% lower in the first post-change run and 19.0% lower in confirmation. Timing results are descriptive: the baseline was measured in one fresh JVM, background workload and CPU frequency were uncontrolled, and even the unchanged no-op case varied. They do not establish equivalent Android device speedups.

The pre-existing performance report records verification for that change: 28 focused functional tests plus the benchmark passed. Those results were not rerun during this documentation audit. The bounded-work regression failed before the fix (2,880 reads instead of the expected 3) and passed afterwards. Availability equivalence was checked across 256 combinations of input length and missing, zero, negative and positive current values. Downsampling tests covered small budgets, boundaries, extrema and unchanged baseline output fingerprints for 240, 2,880 and 28,800 points. Changed debug production/test Kotlin compiled, and `git diff --check` passed.

The September 11 report records no connected device/emulator and no full suite, full lint, or release build for that performance change. Startup, frame timing, UI interaction, battery consumption, Android heap behavior and release performance therefore remain unmeasured by that report; this audit did not inspect device connection state.

Set `RUNCHECK_PERFORMANCE_BENCHMARK=1` to enable the benchmark; ordinary test runs skip it. Run the focused `:app:testDebugUnitTest` selection with `--tests 'com.runcheck.ui.chart.*'` and `--tests 'com.runcheck.ui.battery.BatteryRemainingTimePanelVisibilityTest'`. The [full measurement report](docs/performance-2026-09-11.md) includes all input sizes, the exact repeat command and methodology. Raw baseline/after XML, build logs and the pre-fix regression failure are retained locally under ignored `reports/performance-*` paths.

The confirmation column is dated 2026-09-11 14:35 UTC in the report, which references `reports/performance-confirmation-*` and records the same 28 functional tests, benchmark, and output checksums. Raw-file availability was not revalidated here.

---

## CI/CD Pipeline

GitHub Actions workflows in `.github/workflows/`:

| Workflow | Purpose | Configured triggers and tooling (not a passing-run claim) |
|----------|---------|--------|
| `codeql.yml` | CodeQL security analysis (`java-kotlin`, manual `assembleDebug`) | Active on main pushes, main PRs, manual dispatch, and weekly schedule; CodeQL Action `v4.37.9`, checkout `v7.0.1`, setup-java `v6.0.0`, setup-android `v4.0.1` |
| `security.yml` | Semgrep plus scheduled/manual OWASP | Main push/PR, Monday 08:00 UTC, manual. Semgrep 1.175.0 runs on all these triggers with Python 3.13/setup-python 7.0.0; SARIF upload 4.37.9 requires an existing file and excludes fork/Dependabot PR publication. OWASP runs only schedule/manual: Java 17/setup-java 6.0.0, setup-gradle 6.3.0, cache 6.1.0, 195-minute job / 180-minute scan timeout, upload-artifact 7.0.1. |
| `sonar.yml` | SonarCloud scan through Gradle (`assembleDebug`, `:app:jacocoDebugUnitTestReport`, `sonar`) | Active on main pushes; checkout `v7.0.1`, setup-java `v6.0.0`, setup-android `v4.0.1` |
| `qodana.yml` | JetBrains Qodana main-branch scan through `JetBrains/qodana-action` pinned at `v2026.2.1` | Uses `jetbrains/qodana-jvm-community:2026.1`; retained after the documented AGP 9.1.x Android-linter import failure, while current AGP 9.4.0 Android-linter compatibility remains unverified |
| `qodana_code_quality.yml` | JetBrains Qodana action pinned at `v2026.2.1` for `releases/*`, PRs, and manual dispatch | Uses the same JVM Community linter; `qodana.yml` owns `main` pushes, avoiding a duplicate scan. Current AGP 9.4.0 Android-linter compatibility remains unverified |
| `deepsec-dependencies.yml` | Frozen helper dependency/type checks, not an AI source audit | Main push/PR filtered to `.deepsec/**` or this workflow, plus manual; 15-minute job, Node 26/setup-node 7.0.0, pnpm 11.25.0. Frozen install with scripts disabled, TypeScript no-emit, DeepSec help, moderate-level dependency audit, and a manifest/lock/workspace diff check. |

There are exactly six checked-in workflows. Actions are pinned by full commit SHA; version labels above are the adjacent repository comments. CodeQL uses manual Java/Kotlin extraction via assembleDebug, runs weekly Monday 14:25 UTC, and has a 360-minute job limit. Sonar runs only on main pushes; Qodana's PR/release workflow uses PR head/full history with pr-mode disabled. No workflow here constitutes a signed Play release/deployment or physical-device acceptance pipeline.

CodeQL publishes through its analyze action; security.yml explicitly uploads Semgrep SARIF and OWASP/verification artifacts. Sonar's result publication is the scanner's external-service operation. Qodana delegates result/check behavior to its pinned action, with no separate upload-artifact step or token configured here; the helper-dependency workflow has no explicit report upload. There is no separate full Android test-suite or instrumented-test workflow in this inventory.

OWASP enables NVD updates with five retries, a 168-hour validity window, and an OS/week-keyed cache. It retains normal failure behavior (no continue-on-error), uploads available dependency reports, and uploads dependency-verification diagnostics on failure. These bounds do not guarantee initial NVD data can be obtained in time. Dependabot separately configures weekly Gradle, `.deepsec` npm, and GitHub Actions updates.

External services:
- **SonarCloud** — continuous code quality (`Insaner1980_runcheck`, org `insaner1980`). CI path is `.github/workflows/sonar.yml`; local path is `tools/sonar.ps1`.
- **Qodana** — current workflows do not pass `QODANA_TOKEN`. Older prose names a cloud organization/project, but their current existence, access and results were not verified; the checked-in evidence establishes action-based analysis configuration.

Local PowerShell wrappers:

`tools/Invoke-RuncheckProjectCheck.ps1` resolves the shared entry point in order: `ANDROID_CHECK_ROOT`, fixed `C:\Dev\Android-check`, then sibling `Android-check`. Runcheck owns the forwarding scripts, project JSON, task lists and exception inputs; the resolved checkout owns delegated implementation/tool acquisition. Its independent revision can change wrapper behavior without changing runcheck HEAD. Commands below describe configured operations, none executed for this audit.

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
- `tools/sonar.ps1` — SonarCloud local path; requires explicit `-AllowExternalUpload` and `SONAR_TOKEN`, runs `assembleDebug`, `:app:jacocoDebugUnitTestReport`, prepares an empty Android Lint import placeholder because `lc` owns real lint findings, and runs `sonar`, then writes `reports/sonar.txt`. Forwarded `sonar.exe` arguments also require the upload flag; `-PlanOnly` is inspection, not upload or analysis.
- `tools/sonar-timeout-test.ps1` — isolated PowerShell fixture that verifies a timed-out Sonar Gradle process is terminated and that stdout/stderr plus the timeout marker are persisted
- `tools/sonar-upload-retry-test.ps1` — isolated fixture covering upload-write-timeout recovery, retry exhaustion, retained attempt logs, and no retries for build, authentication, or read-timeout failures

The checked-in `.deepsec` helper currently pins DeepSec `2.3.9`; `tools/ds.ps1` delegates to the shared Android-check implementation, which drives the package scripts rather than making DeepSec part of the Android app dependency graph. Every delegated DeepSec mode currently requires explicit per-run external-AI consent: `-AllowExternalAI`, a single-line `-Provider`, exact `-ExternalAIDataScope 'entire-project-working-tree'`, a single-line `-ExternalAICostEstimate`, and a single-line `-ExternalAIRetentionPolicy`. Without that complete declaration the wrapper fails closed with exit category `ERROR/2` and an `EXTERNAL_AI_*` reason; `-PlanOnly` reports the missing consent without uploading data. The default path is `deepsec:report:custom`; `-Full` selects `deepsec:report`, `-Scan` selects `deepsec:scan`, and `-Revalidate` selects `deepsec:revalidate`.

When `osv-scanner`, gitleaks, TruffleHog, or PMD are missing from `PATH`, the shared Android-check wrappers may download and cache verified tool binaries under `.gradle/android-check-tools/`; offline first runs can therefore skip or fail before a cached tool exists. The OSV source scan excludes `.deepsec` so Android-check's own DeepSec tooling dependencies do not fail app dependency scans.

Compatibility wrappers and config:

- `scripts/security-check.ps1` forwards to `tools/sc.ps1`
- No Linux shell security wrapper is maintained in this Windows-first repo
- Check configuration lives in `config/semgrep/runcheck-security.yml`, `config/dependency-check/suppressions.xml`, `.mobsf`, `.deepsec/`, `.github/dependabot.yml`, `sonar-project.properties`, and `gradle/osv-scanner.toml`
- `reports/` is ignored and must not be committed

---

## Project Management

- **Linear:** Older documentation names project "runcheck" in the Finnvek team. Current priority, status, milestones and completion are unverified external metadata.
- **Linear URL:** https://linear.app/loikka1/project/runcheck-5d6d01d874c1
- **Historical milestone labels:** v1.0 / Play Store Release and Insights Engine. These labels do not establish publication, complete testing, or security-audit completion. The engine is already implemented and documented as a current subsystem below.
- **GitHub:** https://github.com/Insaner1980/runcheck

External product-management/service state was not live-queried during this 2026-09-13 source refresh. Treat code/configuration as current; verify external Linear/Qodana/Sonar/GitHub status live when a task depends on it.

---

## Insights Engine

The implemented engine analyzes Room battery, thermal, network, storage, charger, and app-usage history and persists candidates for Home and Insights. It detects windowed drain anomalies, usage concentration, pressure projections, and co-occurring conditions. No registered rule analyzes recurring network degradation by time of day; rule names must not be interpreted as causal diagnoses or capabilities beyond their actual inputs.

Current Insights rule set (11 production Hilt bindings):

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

Persisted Insight metadata is decoded conservatively per row after expiry filtering. An unknown exact type/target name, unsupported priority sort order, or body-arguments JSON parse failure omits only that row from the current read result, preserving the order of all retained rows. If all rows are undecodable, the repository emits an empty list and continues observing. Reading never rewrites or deletes the raw entity; existing generation replacement, expiry cleanup, and dedupe rules still govern its later replacement or removal. Genuine DAO/repository failures and cancellation still propagate. Raw title/body key compatibility remains unchanged. Successful Gson decoding retains its existing null/empty and coercion behavior; successfully decoded arguments that are insufficient for a known message's formatting remain a separate presentation concern.

`InsightMessageId` owns explicit stable title/body String key pairs for all 11 active messages and `LEGACY_APP_BATTERY_IMPACT`. Only the write-side `InsightCandidate` is typed; its derived keys are persisted unchanged. Read-side `Insight` and `InsightEntity` retain raw String keys so legacy, unknown, partially known, and mismatched rows remain representable. The UI first decodes an exact, case-sensitive pair without normalization and uses one exhaustive ID-to-resource-pair mapping. Noncanonical pairs retain independent per-half compatibility fallback derived from the same catalog: known keys resolve normally, unknown keys display verbatim without appended body arguments. The legacy pair still resolves its existing resources and three historical body arguments, but no rule generates it. Room columns, schema, database version, migrations, resources, and visible behavior are unchanged.

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

Additional algorithm boundaries:

- Adjacent discharge pairs require both endpoints in DISCHARGING/NOT_CHARGING and a strictly increasing timestamp, excluding duplicate-time pairs. Generic drain rates sum non-negative level drops over elapsed durations; contextual comparison samples further require a positive drop. There is no universal maximum-gap filter shared by all rules.
- `TimeWindowAligner` selects the latest context **inside** each interval, including endpoints; it does not carry an older context across an empty interval or borrow a future reading. Aligned co-occurrence does not prove causation.
- Baseline anomaly uses sample standard deviation with a 0.5 floor. HIGH begins at z>=4 or ratio>=3; confidence averages baseline-day support /14 and current-pair support /6, capped at one.
- Drain correlation confidence uses the smaller class count /5. Network HIGH starts at ratio>=1.5 or weak-average signal<=-115dBm; heat HIGH starts at ratio>=1.4 or peak>=43°C. Thus three samples per class merely reaches the common 0.6 acceptance threshold.
- Storage projection uses positive ordinary least-squares growth and available capacity, not a scheduled certainty that storage will fill. Regression coordinates subtract the first timestamp and first used-byte value using Double arithmetic to avoid cancellation at large epoch/capacity offsets. Projection confidence is count/10; impact confidence count/8. Recurring throttling treats unfinished duration as zero and is HIGH also at CRITICAL or worse. Thermal pattern is HIGH also at CRITICAL or worse.
- Confidence values are bounded evidence-support heuristics, not calibrated probabilities. Candidate wording/keys and sample filters must be reviewed together, particularly "degradation", "wear" and "impact".

`domain/insights/engine/InsightEngine.kt` evaluates every rule before calling `replaceGenerationResults` once. If evaluation throws, that generation does not reach replacement. If accepted recurring-throttling and thermal-pattern candidates coexist, the engine clears the thermal-pattern candidate list to avoid overlapping alerts. `data/insights/InsightRepositoryImpl.kt` and the DAO own transactional replacement and dedupe persistence.

`InsightHomeRankingPolicy` sorts by priority, confidence descending, generation time descending, then id. It first selects distinct target buckets (type buckets for NONE), then fills any remaining slots from the ranked list, with a hard cap of three. The dedicated Insights screen retains DAO ordering instead. Neither Home's full-check refresh nor merely reading the Insights screen is equivalent to explicitly regenerating the rule engine.

The Room unique key is `(rule_id, dedupe_key)`; severity/sample buckets can change a rule's dedupe key, allowing a new candidate despite a previously dismissed different key. Dismissed matching keys persist as tombstones until explicit insight/full-history clearing. Seen-state tracking commits its dedupe state only after the Room write succeeds; a failed write can retry on a later identical emission, not on an independent retry timer.

Replacement loads matching existing rows before removing expired undismissed rows, preserving seen/dismissed metadata when the same candidate is regenerated at expiry. `UnseenInsightTracker.markSeen` releases its pending set in `finally`, including cancellation, and accepts completion only for the still-pending set; Home and Insights each own a tracker instance. An empty unseen emission resets its local dedupe state.

Active/unseen flows filter expiry using current time **when Room emits**. They contain no expiry timer; an item may remain on screen past expiry until invalidation/reload/generation. Enum and JSON mapping failures are not uniformly swallowed into an empty list. Generation is separate from Pro display filtering and can consume retained historical inputs even when a target is hidden for free users.

`AppBatteryImpactRule` is intentionally not part of the production rule set. Android does not expose other apps' battery statistics to ordinary third-party apps, and runcheck does not manufacture per-app mAh attribution from foreground time alone.

### Known Tool Limitations

- **Qodana:** `qodana.yaml` still records the original AGP 9.1.x Android-linter import failure and selects `jetbrains/qodana-jvm-community:2026.1`. The app has since moved to AGP 9.4.0, so the recorded Android-linter incompatibility is historical evidence, not fresh proof for the current AGP line. Keep the JVM linter until the Android linter is explicitly re-tested, and update the comment/result together.
- **CodeQL:** `.github/workflows/codeql.yml` pins `github/codeql-action/init` and `analyze` to `v4.37.9` and builds with `assembleDebug --no-configuration-cache`. Check the actual CodeQL Action runner and Kotlin extractor support before Kotlin plugin upgrades.
- **Sonar:** AGP 9 support has had scanner-side compatibility churn. Keep `tools/sonar.ps1` and `.github/workflows/sonar.yml` verified when changing AGP, Gradle, or Kotlin. The local wrapper retries HTTP/2 report-upload write timeouts up to three scan attempts within the existing overall Gradle timeout, preserving each attempt in `reports/sonar.txt`. Other failures are not retried. Older troubleshooting identified scanner engine `13.12.0.5977` and a 60-second write-timeout boundary; that is historical external-engine evidence, not the currently resolved engine or behavior established by this source audit.
- **OWASP Dependency-Check:** NVD updates can take a very long time or return transient 503 responses, so PRs and ordinary main pushes run Semgrep/CodeQL/Qodana while Dependency-Check is reserved for weekly scheduled or manual runs with cache, bounded retries, a job timeout, and a shorter OWASP step timeout (no `continue-on-error` in the current workflow). Dependency-Check reports are uploaded as Actions artifacts instead of GitHub Code scanning SARIF so stale dependency analyses do not keep fixed Dependabot issues open.

---

## Companion Documentation and Unverified Boundaries

This audit deliberately leaves companion files unchanged. The following differences matter when using them as review inputs:

| File / claim | Source-backed interpretation at this snapshot |
|--------------|-----------------------------------------------|
| `AGENTS.md` / `CODEX.md` | Earlier confidence-enum/wrapper, non-Wi-Fi confirmation, per-app battery-attribution and universal sampling discrepancies are resolved in the current files. Their abbreviated Pro list omits remaining-charge estimates and Storage Cleanup present in the eight-value `ProFeature` inventory. The wrapper-resolution description also omits the fixed `C:\Dev\Android-check` candidate between the environment override and sibling fallback. |
| `UI-SPEC.md` snapshot / typography | Its September 8 snapshot metadata predates this audit. Section 9.1 still describes the variable Manrope binding as Home-only; current `Type.kt` defines the shared family and `HomeTheme.kt` aliases it. Its tokens and branches are design references, not evidence of current rendered/device behavior. |
| `docs/privacy-policy.md` | September 12 text now distinguishes release from conditional debug Sentry, delayed retention cleanup, and app-layer database storage. The old March 27 contradictions are resolved. Contact remains a placeholder, and the externally published policy is unverified. |
| `docs/play-store-listing.md` | Health-percentage availability is advertised although production design capacity/health percentage remain unavailable. "Widgets and advanced insights" under Pro is less precise than the current free Home Insights surface plus individually gated destinations. Listing copy is not a verified Play publication or entitlement contract. |
| `docs/battery-enhancements-spec.md` | Private PowerProfile/design-capacity proposals do not describe production, where design capacity/health percentage remain unavailable. |
| `docs/storage-cleanup-spec.md` | Old API-29 no-dialog deletion description conflicts with current RecoverableSecurityException consent/retry flow. |
| Older settings/storage enhancement specs | Proposed keys and features are not evidence of implemented settings; use the actual 22-key inventory and route contracts above. |
| `qodana.yaml`, advisory notes | Old AGP incompatibility and Kotlin availability statements are historical; configuration and expiry dates are current, external compatibility/advisory conclusions are not freshly verified. |
| `tools/export-launcher-icons.ps1` | Generation still needs three SVG inputs under absent root `icons/`; `-VerifyOnly` deliberately skips those source checks and verifies existing WebP sizes and manifest wiring with ImageMagick. Neither mode was run; existing assets do not establish that regeneration is possible. |
| Old UI/Sonar/Detekt cleanup reports and plans | Findings, execution instructions and completion statements belong to their recorded scope/date. They do not authorize changes or establish today's clean result. |

This refresh corrects the old dirty-worktree snapshot, test/exception counts, unavailable-current/badge and missing-voltage descriptions, screen-accounting clock, charging-current denominator, cleanup cancellation, preference-key inventory, stability option name, and shared font/error-color contracts. It removes already-resolved companion discrepancies and obsolete CodeQL registrations while preserving useful architecture, screen/design detail, migrations, rule thresholds, and explicitly dated chart-performance evidence.

This was a repository-wide **static documentation audit**: build/configuration, source sets, feature/data flows, Room exports, test sources, manifests/resources, workflows, local wrappers and relevant historical docs were inspected. No build, test, scanner, wrapper plan execution, emulator/device session, network probe, service-status lookup, dependency resolution, or Git publication was run. In particular, these remain unverified:

- Compilation, full/unit/instrumented test results, current analyzer cleanliness and coverage.
- Signed/minified release contents, merged manifests, resource shrinking, signing/version acceptance and telemetry exclusion in a built artifact.
- Live Billing acknowledgement/restoration/refund/offline behavior and actual product price/availability.
- Real device sensor reliability across API 26–37/OEMs, radio/permission transitions, NDT7 transfer accuracy, media-provider consent/deletion and portable-storage behavior.
- WorkManager scheduling, process death/reboot recovery, live notification survival, widget age refresh and permission policies under Android/OEM restrictions.
- Rendered UI, TalkBack, keyboard/switch access, font-scale/inset/orientation behavior, frame/startup performance and energy use.
- Current remote CI, CodeQL, Dependabot, Sonar, Qodana, Linear, advisory or Play Store state.

---

## Notes for Maintenance

- `PROJECT.md` should describe the code as it exists now, not the intended roadmap only.
- `CODEX.md` and `AGENTS.md` should stay aligned when repository rules or project snapshot notes are updated.
- This 2026-09-13 refresh changes only `PROJECT.md`. Companion-document discrepancies are recorded here without modifying those files or the implementation.
