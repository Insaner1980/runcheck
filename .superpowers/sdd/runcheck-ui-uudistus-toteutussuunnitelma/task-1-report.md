# Task 1 report — D8, D9 and D11

## Status

DONE_WITH_CONCERNS

The implementation is complete and the requested unit-test, compile, and ktlint gates pass. Device screenshots and an on-device TalkBack check remain unavailable because ADB reports no connected devices.

## Implementation

### D8 — readable chart viewport

- Added pure `ChartViewport` calculation in `ui/chart/ChartPresentationPolicy.kt`.
- Non-finite data and tick values are excluded.
- Empty data returns no viewport.
- Flat and single-value ranges receive symmetric padding.
- Data and explicit Y ticks determine the viewport; quality-zone bounds do not.
- Visible quality zones are clipped to the viewport before drawing.
- Tick selection is capped at four labels and respects the requested minimum pixel spacing.
- `TrendChart` now uses this viewport for embedded and fullscreen rendering.
- Embedded charts dynamically reserve at least 180 dp of actual plot height after axis labels and chart padding.
- Existing `TextMeasurer` label measurement, tooltips, fullscreen gestures, animations, and accessibility descriptions remain in place.

### D9 — dedicated history-period selector

- Replaced the history-specific use of the generic segmented selector with a dedicated `LazyRow` built from stable Material 3 `FilterChip` components.
- The selected item is brought into view with `animateScrollToItem`; reduced motion uses immediate `scrollToItem`.
- Start/end gradient affordances appear only while more content is available in that direction.
- Labels use one untruncated line.
- The row exposes collection metadata and each chip exposes its position; `FilterChip` retains selected-state semantics for TalkBack.
- The tested pure selector policy is used by the real component with the actual width, font scale, selected index, and reduced-motion state.
- Battery history now uses the dedicated selector, including the existing “Since unplug” option.
- Network, thermal, storage, and fullscreen history controls use the same selector.
- Existing ViewModel and `SavedStateHandle` ownership of selected periods is unchanged.

### D11 — exactly one chart primary state

- Added the pure precedence order `Loading -> Error -> Locked -> InsufficientData -> Data`.
- Battery, network, thermal, and storage history panels resolve one primary state before rendering chart-area content.
- History errors now replace data/empty content instead of appearing beside it.
- Battery Pro lock still uses the existing `state.isPro` gate and upgrade callback.
- Removed the blurred fake Battery chart from locked and insufficient states, so neither state renders chart data behind its message.
- Fullscreen already had mutually exclusive sealed UI states; its rendering remains exclusive and now shares the corrected viewport and selector.

## Files

Production:

- `app/src/main/java/com/runcheck/ui/chart/ChartPresentationPolicy.kt`
- `app/src/main/java/com/runcheck/ui/chart/HistoryPeriodFilterChipRow.kt`
- `app/src/main/java/com/runcheck/ui/components/TrendChart.kt`
- `app/src/main/java/com/runcheck/ui/battery/BatteryDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/thermal/ThermalDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/storage/StorageDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/fullscreen/FullscreenChartScreen.kt`

Report:

- `.superpowers/sdd/runcheck-ui-uudistus-toteutussuunnitelma/task-1-report.md`

No domain, data, DI, Room, WorkManager, route, permission, network-operation, or test-source files were changed.

## TDD RED

Command:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.runcheck.ui.chart.ChartViewportTest" --tests "com.runcheck.ui.chart.ChartStatePrecedenceTest" --tests "com.runcheck.ui.chart.HistoryPeriodSelectorPolicyTest" --no-parallel --max-workers=1 --console=plain
```

The first tool invocation exceeded its 120-second capture window while Gradle continued running, so no result was inferred from that timeout. After the process finished, the same command was rerun with a longer capture window.

Expected RED result:

- `:app:compileDebugUnitTestKotlin FAILED`
- unresolved `calculateChartViewport`
- unresolved `ChartPrimaryState` / `resolveChartPrimaryState`
- unresolved `historyPeriodSelectorPolicy`
- `BUILD FAILED`

This confirmed that the committed phase-0 tests failed because the required presentation APIs did not exist.

## TDD GREEN

The exact same three-class command was rerun after implementation.

Result:

- `BUILD SUCCESSFUL`
- 8 tests, 0 failures, 0 errors, 0 skipped

## Other focused checks

Directly related chart, fullscreen, period-state, and detail ViewModel tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.runcheck.ui.chart.ChartViewportTest" --tests "com.runcheck.ui.chart.ChartStatePrecedenceTest" --tests "com.runcheck.ui.chart.HistoryPeriodSelectorPolicyTest" --tests "com.runcheck.ui.chart.ChartRenderModelTest" --tests "com.runcheck.ui.chart.ChartAccessibilityTest" --tests "com.runcheck.ui.fullscreen.FullscreenChartViewModelTest" --tests "com.runcheck.ui.network.NetworkFullscreenSelectionTest" --tests "com.runcheck.ui.battery.BatteryViewModelTest" --tests "com.runcheck.ui.network.NetworkViewModelTest" --tests "com.runcheck.ui.thermal.ThermalViewModelTest" --tests "com.runcheck.ui.storage.StorageViewModelTest" --no-parallel --max-workers=1 --console=plain
```

Result: 11 classes, 63 tests, 0 failures, 0 errors, 0 skipped.

Compile:

```powershell
.\gradlew.bat :app:compileDebugKotlin --no-parallel --max-workers=1 --console=plain
```

Result: `BUILD SUCCESSFUL`. The output retained two pre-existing Material 3 deprecation warnings in `InfoBottomSheet.kt` and `TrialWelcomeSheet.kt`; neither file is in this task’s scope.

Style:

```powershell
.\gradlew.bat :app:ktlintMainSourceSetCheck :app:ktlintTestSourceSetCheck --no-parallel --max-workers=1 --console=plain
```

Result: `BUILD SUCCESSFUL`.

Additional checks:

- `git diff --check`: clean.
- Scope search found no new wildcard imports, `!!`, hardcoded color hexes, raw dispatchers, Room, DI, WorkManager, or network-operation changes.
- The generated worktree-local `.kotlin` cache was removed after verification.
- Heavy wrappers and full suites were not run, as required.

## Self-review

- Viewport inputs are data plus explicitly supplied ticks; quality zones only participate after the viewport is fixed.
- All zone drawing in `TrendChart` uses clipped zones.
- The label set used for measurement, grid lines, and Y-axis text is the same retained set.
- Embedded plot height calculation accounts for X-label height and chart padding before enforcing the 180 dp plot minimum.
- Battery’s previous direct history-period `EnumFilterChipRow` call was replaced; the remaining generic rows select metrics or session windows and are intentionally unchanged.
- Fullscreen seed priming, route callbacks, metric/period return handling, and tooltip behavior are unchanged.
- Pro gating still derives from the existing state and does not change free/trial/pro rules.
- Network, thermal, and storage errors are now mutually exclusive with empty/data content.
- The selector is controlled by its caller and does not take ownership away from ViewModels or `SavedStateHandle`.
- No confidence values, status values, API guards, navigation handlers, or user-visible measurement data were removed.
- Official Android Compose guidance was checked for `LazyListState.scrollToItem` / `animateScrollToItem`, lazy-list item structure, and collection/selected semantics before implementation.

## Device limitation and concerns

`adb devices` returned an empty device list. Therefore:

- no before/after screenshots were captured;
- 411 × 850 dp and 2.0 font-scale behavior was not visually confirmed on hardware/emulator;
- TalkBack selection/position announcements were implemented through Compose semantics but were not heard on a device;
- edge fades and reduced-motion scroll behavior were not interactively observed.

These are verification limitations, not known source or test failures.
