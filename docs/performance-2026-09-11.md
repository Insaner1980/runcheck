# Chart performance measurements — 2026-09-11

## Scope and changes

Two measured sources of avoidable work in `ChartHelpers.kt` were improved:

- `hasGraphData()` previously built a full current graph, and potentially a power graph, merely to check for two points. Both metrics require the same non-null `currentMa`. It now stops at the second valid reading without building either graph. `BatteryDetailScreen` uses this check when deciding whether to display the session graph.
- `downsamplePairs()` previously created a sublist for each averaging bucket and traversed it twice. It now accumulates the two sums in one indexed pass. Selection arithmetic, accumulation order within each sum, point budgets and boundary behavior are preserved. History chart builders share this function.

The starting checkout was dirty at HEAD `fe98980e1874025af88ec884e3a3ebe62e437ba5`; existing changes were retained. `ChartHelpers.kt` itself matched HEAD before this work. No dependencies or runtime settings changed.

## Method

Opt-in `ChartPerformanceBenchmark` executes the production Kotlin functions through `:app:testDebugUnitTest`. Windows 11, AMD Ryzen AI 7 350, Java test runtime `17.0.20+8`; the existing debug test instrumentation and JaCoCo configuration remain enabled. These are host JVM microbenchmarks, not Android ART or release-app measurements.

Each case warms up for at least one second, then runs 15 batches of 200 calls. The table reports the median batch time per call and median thread-allocated bytes per call (`ThreadMXBean`). A volatile result sink retains outputs. Input construction and session summary construction are outside timing. One fresh test JVM was used before and one after; background workload and CPU frequency were not controlled. Small timing changes, especially nanosecond-scale results, must not be interpreted as precise speedup factors. Allocation reductions and the bounded-work regression provide stronger evidence.

Inputs are deterministic chronological readings, one minute apart, with current `500 + index % 1500`, voltage 4000 mV and temperature 30 °C. The unavailable cases replace every current with null. Sizes 240 and 2880 exercise short and longer inputs; 28800 is a stress case, not a claim about typical charging-session length. The ALL history use case separately caps history at 5000 points; other time windows have no such count cap. Downsampling uses a 300-point budget; the 240-point case exercises its unchanged no-op path.

## Before and after

Times are microseconds per call; memory is allocated bytes per call, not retained heap.

| Operation | Input points | Before µs | After µs | Before bytes | After bytes |
|---|---:|---:|---:|---:|---:|
| Graph available | 240 | 3.032 | 0.038 | 18456 | 32 |
| Graph unavailable | 240 | 1.367 | 0.407 | 80 | 32 |
| Downsample to 300 (no-op) | 240 | 0.063 | 0.023 | 0 | 0 |
| Graph available | 2880 | 55.426 | 0.016 | 234616 | 56 |
| Graph unavailable | 2880 | 13.225 | 4.638 | 80 | 32 |
| Downsample to 300 | 2880 | 14.755 | 12.534 | 10784 | 1248 |
| Graph available | 28800 | 292.624 | 0.012 | 2223048 | 32 |
| Graph unavailable | 28800 | 97.112 | 54.334 | 80 | 32 |
| Downsample to 300 | 28800 | 119.637 | 111.288 | 10784 | 1248 |

At 2880 points the available-graph check allocates 99.98% less and downsampling allocates 88.43% less. Downsampling time decreased by 15.1% in that run, but its stress-case improvement was only 7.0% and the unchanged no-op timing also varied. These timings establish observed local results, not a statistically established device speedup. The availability check now reads only through the second valid sample; an all-null input still requires one complete scan.

Raw evidence is retained locally under ignored `reports/`:

- `performance-baseline.xml`, `performance-after.xml`: all batch timings, median allocations, fixture checksums and runtime version.
- `performance-baseline-build.txt`, `performance-after-build.txt`: commands' Gradle output.
- `performance-regression-before.xml`: expected pre-fix failure (3 reads expected, 2880 observed).
- `performance-after-TEST-*.xml`: successful focused test results.
- `ChartHelpers.before.kt`: original production source for comparison.

## Correctness and limits

28 focused functional tests plus the benchmark passed: chart rendering (19), accessibility helpers (5), graph availability (2), and remaining-time panel visibility (2). Availability tests compare the predicate with both graph metrics across 256 combinations of list length and missing, zero, negative and positive current values. The work-bound test failed before the production edit and passed after it. Downsampling tests cover empty/small budgets, bounds, extrema and unchanged baseline output fingerprints for all three benchmark fixtures. Fingerprints are a regression check, not exhaustive mathematical proof.

The focused Gradle run compiled changed debug production/test Kotlin successfully. `git diff --check` passed. No connected device was available (`adb devices` was empty), so startup, frame timing, UI interaction, battery consumption, Android heap behavior and release performance remain unmeasured. Full test suites, full lint and release builds were not run.

## Independent confirmation on the current checkout

At 2026-09-11 14:35 UTC the optimizations, benchmark and original results above were already present in the working tree. They were inspected and retained. The saved baseline source has Git blob `9005c188d824b825a90eda275456a33611431ff0`, identical to `HEAD:app/src/main/java/com/runcheck/ui/chart/ChartHelpers.kt`. The source diff contains only the two optimizations described above.

The documented command was rerun in a fresh test JVM with the same fixtures and Java runtime. All 28 functional tests and the benchmark passed with no skipped tests. Production and test compilation tasks were up-to-date; the unit-test task actually executed. All three output checksums matched the baseline. No additional production changes were needed.

| Operation | Input points | Original baseline µs | Confirmation µs | Baseline bytes | Confirmation bytes |
|---|---:|---:|---:|---:|---:|
| Graph available | 240 | 3.032 | 0.039 | 18456 | 32 |
| Graph unavailable | 240 | 1.367 | 0.653 | 80 | 32 |
| Downsample to 300 (no-op) | 240 | 0.063 | 0.023 | 0 | 0 |
| Graph available | 2880 | 55.426 | 0.014 | 234616 | 56 |
| Graph unavailable | 2880 | 13.225 | 4.573 | 80 | 32 |
| Downsample to 300 | 2880 | 14.755 | 11.952 | 10784 | 1248 |
| Graph available | 28800 | 292.624 | 0.012 | 2223048 | 32 |
| Graph unavailable | 28800 | 97.112 | 56.960 | 80 | 32 |
| Downsample to 300 | 28800 | 119.637 | 112.735 | 10784 | 1248 |

The confirmation reproduced the allocation reductions. Observed downsampling time was 19.0% lower for 2880 points and 5.8% lower for 28800 points relative to the saved baseline; timing variability and the device-measurement limits above still apply. The baseline was inspected, not rerun during this confirmation.

Evidence: `reports/performance-confirmation-build.txt` and `reports/performance-confirmation-TEST-*.xml`. `adb devices` was still empty. `git diff --check` passed. The one-shot Gradle run exited successfully; its temporary Java processes had exited when checked afterwards.

## Reproduce

Run from the repository root. The clean task clears only the existing debug unit-test task outputs so the benchmark actually reruns. Save the XML after each revision before the next run replaces it. The opt-in variable is restored afterwards; ordinary test runs skip the benchmark.

```powershell
$previousBenchmarkSetting = $env:RUNCHECK_PERFORMANCE_BENCHMARK
try {
    $env:RUNCHECK_PERFORMANCE_BENCHMARK = '1'
    .\gradlew.bat :app:cleanTestDebugUnitTest :app:testDebugUnitTest `
        --tests 'com.runcheck.ui.chart.*' `
        --tests 'com.runcheck.ui.battery.BatteryRemainingTimePanelVisibilityTest' `
        --no-daemon --no-configuration-cache --max-workers=1 --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Focused performance checks failed' }
} finally {
    $env:RUNCHECK_PERFORMANCE_BENCHMARK = $previousBenchmarkSetting
}
```

Results: `app/build/test-results/testDebugUnitTest/TEST-com.runcheck.ui.chart.ChartPerformanceBenchmark.xml`. Compare identical fixtures and runtime configurations. Timing thresholds intentionally do not fail CI.
