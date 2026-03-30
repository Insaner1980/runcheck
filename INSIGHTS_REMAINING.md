# Insights Remaining Work

This file tracks the remaining work after the initial Insights Engine rollout.

## Completed In This Pass

- Added a Room migration test for schema version `9 -> 10`
- Added `MonitorScheduler` unit coverage for:
  - scheduling `HealthMonitorWorker`
  - scheduling `HealthMaintenanceWorker`
  - scheduling `InsightGenerationWorker`
  - cancelling all three unique works
  - `ensureScheduled()` using the stored monitoring interval
- Added release-safety coverage for Settings debug availability and the default release-safe debug-actions implementation
- Synced `AGENTS.md`, `CODEX.md`, and `PROJECT.md` with the implemented Insights behavior
- Moved the release-safe `InsightDebugActions` no-op into `main` so it is covered by standard unit tests

## Still Open

- No required follow-up items for the current Insights rollout.
- Future product refinements may still revisit ranking weights or the dedicated Insights screen UX if the rule set expands further.

## Verification Targets

- `./gradlew :app:testDebugUnitTest --tests com.runcheck.ui.home.HomeViewModelTest --console=plain`
- `./gradlew :app:testDebugUnitTest --tests com.runcheck.ui.settings.SettingsViewModelTest --console=plain`
- `./gradlew :app:testDebugUnitTest --tests com.runcheck.service.monitor.MonitorSchedulerTest --console=plain`
- `./gradlew :app:testDebugUnitTest --tests com.runcheck.debug.insights.ReleaseSafeInsightDebugActionsTest --console=plain`
- `env GRADLE_USER_HOME=/tmp/runcheck-gradle-home RUNCHECK_KEYSTORE_PATH=/home/emma/.android/debug.keystore RUNCHECK_KEYSTORE_PASSWORD=android RUNCHECK_KEY_ALIAS=androiddebugkey RUNCHECK_KEY_PASSWORD=android ./gradlew :app:compileReleaseKotlin --no-daemon --console=plain`
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.runcheck.data.db.RuncheckDatabaseMigrationTest --console=plain`

Notes:

- `connectedDebugAndroidTest` requires a connected emulator or device.
- This project currently does not expose a `:app:testReleaseUnitTest` task.
- Release-safe verification is therefore covered by:
  - Settings debug-availability unit tests
  - the default `ReleaseSafeInsightDebugActions` unit test
  - release-source compile attempts when the environment supports them
