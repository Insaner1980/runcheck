# runcheck — Full-Project Code Review Questions for Codex


Total questions: 245

---

## Architecture & Layering

### Q1. Layer dependency direction

Inspect the dependency direction across the data/, domain/, and ui/ packages. Confirm that ui/ depends only on domain/ (plus the documented androidx.paging.PagingData exception), that domain/ contains no Android framework imports, and that no composable or ViewModel calls a data-layer implementation directly. Fix confirmed violations with minimal changes.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q2. Repository contract alignment

Compare each domain repository interface against its data-layer implementation. Look for methods that exist in the implementation but are unused, contract methods with mismatched semantics (nullable vs non-null, units, error behavior), and implementation types leaking into domain. Fix confirmed mismatches only.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q3. Use case single responsibility

Review the domain use cases for hidden side effects, logic duplicated across two or more use cases, and use cases that bypass repository interfaces to touch data sources directly. Fix confirmed issues with minimal diffs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q4. PagingData boundary exception containment

The project documents androidx.paging.PagingData as the one allowed Android type in domain (cleanup/app-usage flows). Verify this exception has not spread: confirm no other Android or AndroidX types have crossed into domain/. Fix confirmed leaks.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q5. UiText pattern compliance

Verify that ViewModels never hold a Context and that all user-visible strings flow through the UiText pattern (Resource/Dynamic) resolved in composables via the resolve() extension. Look for hardcoded UI strings in ViewModels or Context-based formatting inside ViewModels. Fix confirmed violations.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q6. Dead code and orphaned artifacts

Search for unused composables, unused use cases, unreferenced resources, and leftovers from removed features (for example anything still referencing MANAGE_EXTERNAL_STORAGE, removed light-theme remnants, or Finnish string leftovers outside git history). Remove only what is provably dead.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q7. Package placement sanity

Check that classes live in the packages the architecture expects (billing/, pro/, service/, worker/, widget/, util/, di/). Flag misplaced classes; move a file only if the move is trivially safe and improves clarity.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q8. Error propagation strategy

Review how errors travel from data sources to UI state. Look for swallowed exceptions (empty catch blocks), errors converted to silent nulls where the UI expects an Error state, and inconsistent Loading/Success/Error handling across ViewModels. Fix confirmed gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---


### Q9. Most complex data-layer file review

Choose the single most complex data-layer implementation file in the repository and review it end to end. Trace inputs, Android APIs, threading, error handling, cancellation, persistence, mapping to domain, tests, and UI consequences. Explain why that file was chosen and whether the same risk pattern appears elsewhere. Fix confirmed defects only.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Dependency Injection (Hilt)

### Q10. Module bindings review

Review RepositoryModule, DatabaseModule, SystemBindingsModule, InsightsModule, and DataModule. Look for duplicate bindings, bindings in the wrong component or scope, and singletons that capture short-lived references such as an Activity. Fix confirmed problems.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q11. Scope correctness

Verify @Singleton is used only for genuinely app-wide, safely shared objects, and that nothing singleton-scoped holds mutable per-screen state. Fix confirmed scope errors with minimal changes.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q12. Debug vs release InsightDebugModule symmetry

Compare the debug and release InsightDebugModule source sets. Verify release binds the no-op ReleaseSafeInsightDebugActions, that no debug-only class is reachable from release code, and that the release-safe stubs in main match the debug implementations' contract. Fix confirmed asymmetries.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q13. Worker injection wiring

Verify the HiltWorker setup end to end: the WorkManager Configuration.Provider in RuncheckApp, the worker factory wiring, and that removing androidx.work.WorkManagerInitializer cannot break any startup path, including BootReceiver-triggered scheduling. Fix confirmed wiring problems.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q14. Fragile or circular dependencies

Look for DI cycles worked around with Lazy or Provider, and for constructors pulling in heavyweight dependencies where a narrower one would do. Fix only clear, low-risk cases.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q15. InsightRule multibinding completeness

Verify every InsightRule implementation is present exactly once in the Hilt multibinding Set<InsightRule>, and that nothing anywhere depends on set iteration order. Fix confirmed omissions or duplicates.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---


### Q16. Shared Gson and profile JSON evolution

Review DataModule's shared Gson and every persisted JSON model that uses it, especially device profile JSON. Verify adapters, field naming, null handling, leniency, enum behavior, singleton use, and future model evolution are safe. Fix confirmed serialization defects with minimal changes.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Room Database & Migrations

### Q17. Migration chain integrity

Review Room migrations 1→2 through 9→10 against the exported schemas. Verify each migration's SQL matches the final entity definitions including indexes, and that a fresh install and the full migration chain produce identical schemas. Fix confirmed schema drift.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q18. Migration 4→5 table recreation

Migration 4→5 recreates network readings with a nullable signal_dbm column. Verify the data copy preserves all rows and columns, and that the old table is dropped or renamed cleanly. Fix confirmed data-loss risks.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q19. Index coverage vs real query patterns

Compare DAO WHERE/ORDER BY clauses against the declared indexes (battery status/timestamp, charging session end-time, app-usage composite). Flag confirmed full-table scans on hot paths only where the query pattern genuinely needs an index. Do not add speculative indexes.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q20. DAO query correctness

Review DAO SQL for off-by-one time-window boundaries (inclusive vs exclusive timestamps), epoch unit mismatches (seconds vs milliseconds), and LIMIT/ORDER combinations that could return the wrong rows. Fix confirmed query bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q21. Destructive migration callback path

Verify when the destructive migration fallback can trigger, that the callback correctly records destructive_migration_occurred in runcheck_db_events, and whether any path can wipe user data without recording it. Fix confirmed gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q22. Transaction atomicity

Look for multi-step writes that should be atomic (per-rule insight replacement, charger session updates, retention cleanup) but run as separate DAO calls without a transaction, risking partial state on failure. Fix confirmed atomicity gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q23. Entity nullability vs consumers

Check entity column nullability against how the code reads those columns: places where a nullable column is force-unwrapped, or where a default value silently masks missing data in charts or scoring. Fix confirmed mismatches.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q24. Retention cleanup math

Review the data-retention deletion logic in maintenance work. Verify the cutoff calculation for each retention setting, that every history table is covered consistently, and that insight expiry deletion cannot remove non-expired rows. Fix confirmed errors.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q25. Paging source stability

Review the paging queries for cleanup groups and app usage. Verify stable sort keys (paging over an unstable ORDER BY yields duplicates or gaps), page-size handling at 40, and invalidation when underlying data changes. Fix confirmed instability.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q26. Migration test fidelity

Exported schema assets cover versions 6–10 while migrations start at 1→2. Verify the migration tests exercise what they claim and identify any migration with no test coverage path. Add a test only where you also confirmed and fixed a bug.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## DataStore & Preferences

### Q27. Preference keys and defaults audit

Audit all DataStore keys across settings, trial_state, monitoring_status, and monitoring_alert_state. Look for key collisions, defaults defined in more than one place with different values, and reads assuming a value exists. Fix confirmed inconsistencies.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q28. Trial state robustness

Review trial_state handling: trial start, last-known timestamp, and prompt flags. Verify behavior when the device clock moves backward, on reinstall (known accepted vulnerability — do not silently worsen it), and when a DataStore read fails mid-flow. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q29. Absence-of-key vs explicit default

If any preference default has changed over time, verify existing users cannot get surprising behavior: check places where a missing key and an explicitly stored default value are treated differently. Fix confirmed cases.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q30. Concurrent DataStore writes

Look for read-modify-write sequences on DataStore that could race, for example alert state written from both worker and UI paths, and verify updates use the transactional updateData pattern. Fix confirmed races.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q31. Dismissed info card persistence

Verify dismissed-card keys are stable across releases, that reset-tips clears everything it claims to, and that the stored set cannot grow unboundedly. Fix confirmed issues.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q32. Monitoring heartbeat semantics

Verify the last-successful-worker heartbeat is written only on genuine success, that staleness math (more than 3x the configured interval) is correct, and that a never-run-yet state does not show a false stale warning on first launch. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## WorkManager & Background Monitoring

### Q33. Interval change rescheduling

Verify what happens when the user changes MonitoringInterval: existing periodic work must be replaced or updated with the right ExistingPeriodicWorkPolicy, not duplicated, and not left running at the old interval. Fix confirmed rescheduling bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q34. HealthMonitorWorker failure semantics

The worker should retry only when core collection/maintenance fails. Verify partial failures (thermal read fails, battery succeeds) produce sensible persisted results, and that Result.retry cannot loop forever on a permanently broken sensor. Fix confirmed issues.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q35. Maintenance worker best-effort boundaries

Verify widget refresh failure in HealthMaintenanceWorker genuinely does not force a retry, and that failures in old-reading cleanup and app-usage snapshotting are distinguished correctly from best-effort steps. Fix confirmed misclassifications.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q36. InsightGenerationWorker exception policy

The worker retries only on SQLException and rethrows cancellation. Verify other exceptions produce a proper failure (not silent success), and that cancellation cannot leave partially replaced insight state. Fix confirmed gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q37. BootReceiver correctness

Verify BootReceiver covers boot, package replacement, and unlock as documented, performs no heavy work on the main thread in onReceive, and tolerates being invoked before RuncheckApp initialization completes. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q38. Battery-not-low constraint effects

Verify requiresBatteryNotLow on the maintenance and insight workers cannot permanently starve them on a device that lives at low battery, and confirm HealthMonitorWorker intentionally lacks that constraint. Fix only confirmed problems.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q39. Worker execution time budget

Periodic workers have roughly a 10-minute execution window. Check the heaviest paths (app-usage snapshot, old-reading cleanup, insight generation over long history) for work that could exceed the budget and be killed mid-write. Fix confirmed risks minimally.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q40. Dual-path charger session tracking

Charger sessions are updated from both Home live observation and HealthMonitorWorker. Verify concurrent updates cannot create duplicate or overlapping sessions and that session-end detection is consistent between the two paths. Fix confirmed races.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q41. Alert evaluation and debounce

Review MonitoringAlertStateStore and alert evaluation. Verify each alert (low battery, high temperature, low storage, charge complete) fires once per condition episode, resets when the condition clears, and the charge-complete debounce holds across worker runs. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q42. TrialNotificationWorker scheduling

Verify day-5 and day-7 one-time work: correct delays from trial start, cancellation when Pro is purchased, no notification after conversion, and defined behavior when billing initialization inside the worker fails. Fix confirmed issues.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---


### Q43. Doze and OEM restriction behavior

Review behavior under Doze, App Standby, Battery Saver, and OEM background restrictions. Verify the UI does not promise exact cadence, stale state is honest, missed work recovers without burst duplicates, and user-facing troubleshooting does not overpromise. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Battery Data Source & Capability Detection

### Q44. validateCurrentNow classification

Review DeviceCapabilityManager.validateCurrentNow(): three reads with 300ms spacing, requiring non-zero, changing, plausible values. Verify a device with a legitimately steady current draw is not misclassified as unreliable, and that the 0..10000 mA plausibility window is applied to normalized values. Fix confirmed logic errors.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q45. Microamp/milliamp normalization

Verify the MICROAMP_THRESHOLD = 25_000 normalization: check boundary values, negative (discharging) currents, and devices reporting microamps with small absolute values that could be misread as milliamps. Fix confirmed normalization bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q46. Current sign convention consumers

Current sign is aligned so charging is positive and discharging negative. Trace every consumer (live notification, session stats, drain analysis, charts) and verify none re-applies or double-negates the sign. Fix confirmed sign bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q47. Charge counter and capacity estimation

Verify BATTERY_PROPERTY_CHARGE_COUNTER is used only when positive, and check estimateFullCapacityMah guards (level 1..100, result 500..20000 mAh) for division-by-zero and rounding problems at very low battery levels. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q48. Vendor-specific battery sources

Review the Samsung and OnePlus battery sources and their API 34+ variants: correct fallback when the vendor path fails, no reflection into private APIs, and no crash on devices with unexpected manufacturer strings. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q49. MeasuredValue confidence rendering

Verify the Confidence mapping (HIGH/LOW/UNAVAILABLE → Accurate/Estimated/Unavailable) is applied consistently, and that UNAVAILABLE values never render as zero or as a stale previous number anywhere. Fix confirmed rendering bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q50. ACTION_BATTERY_CHANGED handling

Verify the sticky broadcast usage: registration and unregistration balanced with lifecycle, extras parsed with correct scaling (temperature in tenths of °C, voltage in mV), and missing extras handled without crashing. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q51. Session current stats reset

In-memory current statistics reset on status change. Verify the reset triggers exactly on charging/discharging transitions, not on spurious status flaps, and that stats cannot mix data across a charger swap. Fix confirmed issues.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q52. Screen-on/off drain attribution

Review ScreenStateTracker and the drain analysis. Verify screen-state transitions are timestamped correctly across process death, and that computed drain percentages cannot go negative or exceed 100. Fix confirmed math errors.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q53. Sleep analysis correctness

Review the sleep-while-discharging analysis: how sleep periods are detected, correctness across timezone and DST changes, and behavior when monitoring snapshots are sparse. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Thermal

### Q54. Thermal listener lifecycle

Verify OnThermalStatusChangedListener registration and unregistration are balanced (no leak when leaving the Thermal screen) and the API 29 gate is correct. Fix confirmed leaks.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q55. Thermal headroom polling

getThermalHeadroom(10) is polled every 3 seconds. Verify polling stops when the screen is not active, the forecast-seconds argument is used consistently, and NaN or invalid returns are handled. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q56. Missing CPU sensor must not penalize

CPU temperature intentionally emits null. Verify the thermal score branch treats a missing CPU temperature as neutral, never as a penalty, matching the documented design rule. Fix confirmed scoring bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q57. Temperature unit handling

Verify °C/°F conversion happens exactly once at the formatting layer, that all internal threshold comparisons use °C, and that the HeatStrip gradient position and its indicator value use the same unit. Fix confirmed unit bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q58. Throttling event pairing

Verify throttling events record correct start/end pairs, no duplicate open events can accumulate, and the Pro-gated log renders event durations correctly across midnight boundaries. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q59. Thermal session min/max

Session min/max are tracked while the screen is active. Verify reset semantics, first-emission behavior, and that stale min/max values do not unintentionally persist across screen re-entry. Fix confirmed issues.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Network & Latency

### Q60. LatencyMeasurer sampling

Review the five-sample TCP-connect latency measurement: verify the 1.5s per-sample and 6s total timeouts are enforced, sockets are closed on every path including timeout, and the aggregation used matches what the UI labels the number as. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q61. Jitter computation

Verify the RFC 3550-style jitter formula: requires at least four samples, uses the correct smoothing factor, and never reports jitter from too few samples. Fix confirmed math errors.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q62. Latency lifecycle on network change

Latency resets to null on connection loss and re-measures on type change. Check for a race where a stale latency from the previous network is displayed against the new network's details. Fix confirmed races.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q63. ConnectivityManager callbacks

Review network callback registration: leak-free unregistration, sane behavior with simultaneous WiFi and cellular networks, and correct VPN detection when the VPN is the default network. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q64. Disconnected as unrated, not zero

The network score is 0 when disconnected, but the product rule is that disconnected should read as unrated in the UI. Verify signal display and health-score explanation treat disconnection as unrated context rather than a bare punitive zero. Fix confirmed presentation bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q65. SSID permission help card

Verify the WiFi name help card appears exactly when the SSID is unavailable due to missing location permission, does not appear for other causes such as location services being off (or distinguishes them), and requests permissions via RuncheckPermissionPolicy. Fix confirmed logic errors.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q66. Signal dBm parsing

Verify dBm extraction per transport (WiFi vs cellular), the nullable signal_dbm path introduced in migration 4→5, and the SignalBars quality boundary mapping. Fix confirmed parsing or boundary bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q67. LinkProperties display

Verify IP/DNS/MTU parsing: multiple IPv4/IPv6 addresses handled sensibly, DNS list formatting, MTU availability gating, and no crash on networks without link properties. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Speed Test (NDT7)

### Q68. Network identity lock

The active default network is locked at test start and identity changes fail the test. Verify the lock actually catches a WiFi-to-cellular handover mid-test and does not false-positive on benign capability changes. Fix confirmed detection bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q69. Cellular confirmation gate

Verify CellularConfirmationRequired fires only on cellular, cannot be bypassed by starting on WiFi and switching before the test begins, and that the confirmation state does not leak across test sessions. Fix confirmed gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q70. Phase state machine

Review ping/download/upload transitions: a failure in any phase must produce a failed state, never a partial result stored as complete, and the state machine must not hang if NDT7 stops emitting progress. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q71. ClientResponse parsing

Server name and location come from ClientResponse.origin/test when present. Verify null-safe parsing, no fabricated server metadata when absent, and correct Mbps computation from the NDT7 measurement payload. Fix confirmed parsing bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q72. FinalizeSpeedTestUseCase trimming

Verify free-tier history trimming: trims to the documented limit keeping the newest results, does not trim for Pro users, and cannot delete the result that was just inserted in the same operation. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q73. SpeedTestService lifecycle

Verify mid-test cancellation cleans up NDT7 resources, rapid repeated start attempts are rejected while a test runs, and the service does not outlive its purpose. Fix confirmed lifecycle bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q74. Metadata captured at start

Verify connection type/subtype and optional signal strength are captured at test start, not at completion, so a mid-test change cannot mislabel the stored result. Fix confirmed capture-timing bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q75. Validated connection precheck

Verify the required-validated-connection check uses NET_CAPABILITY_VALIDATED correctly and produces a distinct user-facing error from having no connection at all. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Storage & Cleanup

### Q76. StorageStatsManager unavailability

Verify aggregate app/data/cache byte queries emit unavailable (not zero) when usage access is missing or the platform denies the call, and that the UI distinguishes unavailable from genuinely empty. Fix confirmed conflation bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q77. App count semantics

App count means distinct launchable packages via queryIntentActivities with ACTION_MAIN + CATEGORY_LAUNCHER. Verify the query matches the manifest queries declaration, deduplication is correct, and refresh behavior while the screen is open is intentional. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q78. Encryption status mapping

Verify the DevicePolicyManager.storageEncryptionStatus mapping covers all platform constants, including unknown future values, without crashing or mislabeling. Fix confirmed mapping gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q79. /proc/mounts parsing

The /data file-system type is read from /proc/mounts. Verify parsing is robust (unusual mount options, missing entry), access restrictions do not crash, and the read happens off the main thread. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q80. Media breakdown accuracy

Verify MediaStore category queries: correct MIME and path filters per category, trashed items excluded from normal counts, and the segmented-bar Other remainder math never going negative. Fix confirmed calculation bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q81. MediaAccessState on Android 14+

Verify full vs selected visual-media access detection, the UI affordances for partial access, and re-querying after the user changes their selection in the system sheet. Fix confirmed detection bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q82. Delete request flow (API 30+)

Review the IntentSender delete path: correct result handling for partial deletions when the user deselects files in the system dialog, and state consistency if the app process is recreated while the system dialog is open. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q83. Legacy delete path and API 29

Verify StorageCleanupHelper.deleteLegacy: WRITE_EXTERNAL_STORAGE is declared maxSdk 28, yet legacy delete is described for Android 10 (API 29) and below. Determine which path API 29 actually takes and whether it works. Fix confirmed gaps, and never delete without user confirmation.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q84. Cleanup type version gating

Old Downloads and APK cleanup are restricted to API 30+ in CleanupViewModel. Verify the gate is enforced before scanning starts, the UI explains the restriction on older devices, and LARGE_FILES still works below API 30. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q85. Cleanup selection consistency

Verify per-file and whole-group selection stay consistent while pages load incrementally, when filters change, and after a partial delete. Fix confirmed state bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q86. Trash handling boundaries

Trash is intentionally not a cleanup route. Verify the API 30+ trash query and empty-trash request handle the zero-trash state, permission revocation mid-flow, and do not appear on older API levels. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q87. Cleanup filter restore

The selected filter lives in SavedStateHandle. Verify process-death restore lands on the same filter and re-triggers the correct scan rather than a default scan that overwrites results. Fix confirmed restore bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---


### Q88. Storage capacity and removable storage

Audit total, used, and free storage calculations plus SD-card, removable, and adoptable storage detection. Verify filesystem selection, StatFs or StorageStats semantics, reserved space, overflow, multiple volumes, unmounted states, duplicate primary reporting, unknown-capacity labels, score consistency, and chart consistency. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q89. Cleanup filters, APK scanning, and file churn

Audit cleanup filters and scanning for Large Files, Old Downloads, and APK Files. Verify 10, 50, 100, and 500 MB boundaries, 30, 60, and 90 day plus one-year boundaries, chosen date columns, MIME and extension detection, split APKs, renamed files, app-owned files, API restrictions, default preselection, and behavior when files are deleted, moved, revoked, ejected, or newly indexed during scanning or deletion. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## App Usage

### Q90. Usage access detection

Verify usage-access permission detection is accurate on current Android versions, refreshes when returning from system settings, and the education card does not flicker on the granted path. Fix confirmed detection bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q91. Snapshot aggregation windows

Review per-app usage snapshot computation against UsageStatsManager's approximate bucket semantics: verify window boundaries, no double-counting across snapshot runs, and that the collection timestamp in DataStore advances correctly. Fix confirmed aggregation bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q92. Foreground time plausibility

Verify total foreground time cannot exceed elapsed wall time in the window, apps uninstalled mid-window are handled, and system/launcher packages are included or excluded consistently and intentionally. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q93. Pro gating at execution time

Usage snapshots are collected only while trial/Pro is active. Verify the maintenance worker checks Pro state at execution time rather than schedule time, and that data from an expired trial is displayed or hidden intentionally. Fix confirmed gating bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q94. App usage paging behavior

Verify stable ordering in the paged list, correct empty-data behavior, and refresh after permission grant without requiring an app restart. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---


### Q95. Package metadata resilience

Audit App Usage package filtering and package metadata display. Verify runcheck itself, system components, removed apps, zero-usage apps, work-profile apps, instant apps, non-launcher packages, uninstalled packages, labels, icons, adaptive icons, placeholders, PackageManager failures, caching, and Main-thread work are handled intentionally. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Insights Engine

### Q96. InsightEngine filtering and replacement

Verify the 0.6 confidence filter is applied consistently, that per-rule result replacement cannot delete another rule's rows, and that dedupe-key matching preserves seen/dismissed state exactly as documented. Fix confirmed engine bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q97. Insight expiry semantics

Expired rows are deleted before and after generation. Verify the expiry comparison uses the right clock and cannot delete rows inserted by the same generation pass. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q98. Dedupe key stability

Review each rule's dedupe-key construction: keys must stay stable across runs for the same finding and change when the finding meaningfully changes. Look for keys built from raw floats or timestamps that churn every run. Fix confirmed instability.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q99. BatteryDegradationTrendRule

Review the linear-regression degradation logic: minimum data requirements, robustness to gaps and outliers in the history, slope-to-conclusion thresholds, and confidence computation. Fix confirmed statistical or logic errors.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q100. BaselineAnomalyRule

Review the Z-score anomaly detection: baseline window selection, zero-variance handling (division by zero when all readings are identical), and sensitivity that could spam insights on naturally noisy devices. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q101. AppBatteryImpactRule

Verify the attribution logic cannot blame an app for drain during periods it was not foregrounded, handles missing usage-access data gracefully, and respects the Pro gating of the underlying data. Fix confirmed attribution bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q102. ChargerPerformanceRule

Review the variance-based charger comparison: minimum sessions per charger before judging, handling of mixed fast/slow charging modes on the same charger, and division safety in performance ratios. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q103. StoragePressureProjectionRule

Verify the storage growth projection: extrapolation window, behavior after a large cleanup (negative growth), and that projections are capped at sensible horizons instead of absurd far-future predictions. Fix confirmed projection bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q104. RecurringThermalThrottlingRule

Verify recurrence detection windows, timezone handling for time-of-day patterns, and minimum event counts before claiming a pattern. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q105. Cross-category correlation rules

Review NetworkDrivenBatteryDrainRule, HeatAcceleratedBatteryWearRule, and StoragePressureImpactRule together. Verify time alignment between readings sampled at different moments, minimum overlap requirements, and that the insight text does not overstate correlation as causation. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q106. HeavyAppUsageRule and NetworkSignalPatternRule

Verify thresholds are reasonable and consistent with the rest of the system, results respect free vs Pro target visibility, and neither rule fails on sparse single-day history. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q107. ThermalPatternDetectionRule overlap

Verify this rule's window math and its overlap with RecurringThermalThrottlingRule: two rules describing the same episode would look broken to users. Check dedupe behavior between them. Fix confirmed duplication issues.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q108. InsightHomeRankingPolicy

Verify the ranking policy: at most three items, the same-target-bucket avoidance logic before filling remaining slots, deterministic ordering on ties, and Pro-only targets hidden for free users but visible for trial/Pro. Fix confirmed policy bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q109. Seen/dismissed lifecycle

Verify markAllSeen marks only the currently displayed rows, dismissed insights cannot resurrect through dedupe-key preservation, and dismissal survives regeneration. Fix confirmed lifecycle bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q110. InsightTestDataSeeder

Verify the debug seeder produces data that actually triggers each rule, is deterministic, and can neither ship in nor be invoked from release builds. Fix confirmed gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Health Score

### Q111. Weighting and rounding

Verify the 40/25/25/10 weighting sums correctly, subsystem scores are clamped to 0..100 before weighting, and rounding at tier boundaries (for example 74.5) is deterministic and consistent. Fix confirmed math errors.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q112. Battery subscore penalties

Review the battery penalties (health state, temperature, voltage, optional health percentage): verify each penalty's range, that stacking cannot push the score below 0, and that missing optional inputs do not penalize. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q113. Network subscore mode switch

Verify both scoring modes (with and without a recent speed test): the 1-hour freshness check, weight redistribution when jitter is unavailable, and no mode flip-flop producing a visible score jump exactly at 60 minutes. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q114. Thermal and storage subscores

Verify the thermal branches for missing CPU temperature (must be neutral) and that storage's sharp high-utilization penalties align with the documented status thresholds. Fix confirmed inconsistencies.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q115. Single source of truth for thresholds

Verify the same score produces the same status color and label everywhere (Home hero, widget, insight text): one calculator and one threshold table, with no duplicated threshold logic drifting apart. Fix confirmed duplication.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---


### Q116. Score freshness, explainability, and determinism

Audit the health score for input freshness, explainability, and deterministic behavior. Verify battery, network, thermal, and storage inputs come from a coherent time window, stale data is disclosed, component scores and major penalties are preserved for UI and support diagnostics, and results do not depend on collection order, locale, unordered maps or sets, floating-point platform quirks, or current time without an injectable clock. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Billing, Pro & Trial

### Q117. ProState decision propagation

Trace every consumer of ProState.isPro. Verify an active trial counts as Pro everywhere it should, an expired trial revokes access on every gated surface, and no surface caches a stale Pro decision past a state change. Fix confirmed gating bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q118. Purchase acknowledgement retries

Verify the acknowledge-with-3-retries logic: backoff between attempts, behavior when all retries fail (the purchase must not be lost), and re-acknowledgement on the next app start. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q119. Pending purchase handling

Verify pending purchases are tracked without unlocking Pro, convert correctly when completed, and clean up properly when cancelled. Fix confirmed state bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q120. Pro status cache integrity

The SharedPreferences pro_status_cache restores Pro synchronously at release cold start. Verify the cache updates on every Pro state change including revocations, and that a tampered cache cannot grant lasting Pro once Billing responds. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q121. Restore purchase flow

Verify the restore path queries purchases correctly, handles the no-purchase-found case with clear UI, and survives a flaky Billing connection with sane retry/disconnect handling. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q122. Debug Pro override containment

Debug builds force Pro active and allow a RUNCHECK_PRO_PRODUCT_ID override. Verify both are strictly debug-only and cannot influence release builds in any way. Fix confirmed leaks.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q123. Trial start integrity

Verify trial start recording: exactly-once on first launch, resilient to process death during first run, and that the known reinstall-reset vulnerability is not accidentally made worse by any recent code. Fix confirmed regressions.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q124. Trial UX gate timing

Verify the day-5 banner, day-7 expiration modal, and post-expiration upgrade card: correct day math from trial start, the modal does not re-show every launch, and upgrade-card dismissal pacing from DataStore is respected. Fix confirmed timing bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q125. Billing connection lifecycle

Review BillingManager connection handling: reconnection after service disconnect, no purchase processed twice on reconnect, and purchase queries at appropriate lifecycle points. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q126. Free-tier route redirects

Verify charger and app_usage redirect to pro_upgrade at the NavGraph level for free users, that deep links cannot bypass the redirect, and that trial/Pro users are never redirected. Fix confirmed bypasses.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Navigation & State Restoration

### Q127. Route argument parsing safety

Review fullscreen_chart/{source}/{metric}/{period}, cleanup/{type}, and learn/{articleId}: verify enum parsing rejects invalid values gracefully with no crash on a malformed deep link, and defaults are sensible. Fix confirmed crashes.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q128. FullscreenChartResult handoff

Verify the result keys are written to the correct previous back-stack entry, consumed exactly once, and that a stale result is never applied when re-entering by a different path. Fix confirmed handoff bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q129. singleTop coverage

Verify all navigation calls use navigateSingleTop as documented, and rapid double-taps on Home cards cannot push duplicate destinations. Fix confirmed gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q130. Landscape lock scope

The fullscreen chart is landscape-only while visible. Verify orientation restores when leaving via back, gesture, or process-death restore, and that no other screen inherits the lock. Note: a planned adaptive-layout change will remove the forced lock later; fix only confirmed bugs in current behavior.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q131. Direct routes and Learn cross-link validation

Verify Screen.directRoutes limits notification deep links to argument-free destinations, and that Learn cross-link validation at catalog init fails loudly or safely on an invalid route rather than deferring the crash to tap time. Fix confirmed gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q132. Saved state split correctness

Spot-check the documented split between rememberSaveable (sheet visibility, chip selections) and SavedStateHandle (period/metric, cleanup filter). Verify process-death restore works on Battery, Network, Cleanup, and Fullscreen chart. Fix confirmed restore bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q133. Back stack edge cases

Verify the nested parent-then-child navigation (Charger, SpeedTest): back from the child lands on the parent, and system back after a free-tier redirect to pro_upgrade cannot bounce-loop. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q134. Transitions under reduced motion

Verify navigation transitions become instant under reduced motion, and the fullscreen-chart scale transition cannot leave a mis-scaled screen if interrupted mid-animation. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---


### Q135. ViewModel scoping and saved stacks

Audit destination ViewModel scoping plus popUpTo, launchSingleTop, saveState, and restoreState usage. Verify shared versus per-destination ViewModels use the intended NavBackStackEntry, repeated destinations do not share stale state, popped screens release collectors, push-only navigation is preserved, parent result state is not erased, and stale gated screens cannot remain hidden in saved stacks. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Home Screen

### Q136. Flow combination and throttling

Home combines battery, network, thermal, storage, insight, Pro, preference, and freshness flows throttled to 333ms. Verify the combination cannot emit partially initialized state, throttling affects display only, and one flow's failure cannot silently freeze the whole screen. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q137. Monitoring stale banner logic

Verify stale detection (heartbeat older than 3x interval) accounts for interval changes (old-interval heartbeat judged against a new shorter interval) and for Doze/sleep gaps that are not the app's fault. Fix confirmed false-positive logic.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q138. Home insight summary behavior

Verify Home shows at most three ranked insights, marks only the displayed unseen rows as seen, and that tapping an insight with a Pro-only target behaves correctly per user tier. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q139. Trial/Pro card state machine

Verify only one of the trial, expired-trial, and Pro cards can show at a time, transitions are flicker-free at state change, and the welcome sheet shows exactly once. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q140. Quick grid status consistency

Verify each grid card's status label derives from the same threshold functions as the detail screens with no duplicated logic, and that lock overlays appear on exactly the Pro-gated cards for expired-trial users. Fix confirmed drift.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q141. Home recomposition hotspots

Look for recomposition problems on Home: unstable lambdas or collection parameters passed to cards, missing keys in any list, and heavy formatting done in composition instead of in the ViewModel. Fix confirmed hotspots minimally.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Battery Detail UI

### Q142. Confidence badge coverage

Verify every current/charging metric that can be Estimated or Unavailable shows its ConfidenceBadge, and that no badge shows Accurate for a value the repository marked LOW. Fix confirmed mismatches.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q143. Capacity estimate display

Verify remaining mAh and estimated full capacity render only when the repository provides them, are clearly labeled as estimates, and that no design-capacity value can appear anywhere (it is intentionally absent). Fix confirmed violations.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q144. Charging session graph

Verify session graph windowing, gap handling when monitoring missed samples, and correct behavior for an ongoing open-ended session versus a completed one. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q145. Pro sections on Battery detail

Verify the Pro-only remaining-charge estimate, history chart, and statistics panel are gated consistently, show the correct locked-state component, and do not fetch Pro data for free users only to hide it. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q146. Pull-to-refresh behavior

Verify PullToRefreshWrapper on detail screens: refresh genuinely re-reads live sources, the indicator clears on error, and refreshing during an active chart animation does not corrupt chart state. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Charts

### Q147. TrendChart downsampling

Verify the max-point limits (300 embedded, 600 fullscreen): the decimation approach, whether it preserves visual features like local min/max versus naive stride sampling, and that boundary points are always included. Fix confirmed data-fidelity bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q148. Instrument Sweep state machine

Review the three-phase entry animation: verify phase sequencing cannot deadlock if data changes mid-sweep, interaction disabled during the sweep reliably re-enables, and reduced motion skips all phases to the final state. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q149. Data transition animation

Verify the fade-out/sweep-in transition on period or metric change: the 200ms overlap, old-data reconstruction from stored points, and that rapid successive period changes (spam-tapping chips) cannot stack or corrupt animations. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q150. Status gradient line stops

Verify the per-point gradient built from qualityZoneColorForValue: stop positions must be monotonically increasing as Brush requires, colors correct at zone boundaries, and a single-color fallback when zones are absent. Fix confirmed rendering bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q151. Strip-based gradient fill

Verify the per-strip vertical gradient rendering: Path reuse with reset() leaves no cross-strip artifacts, alpha lerp stays within bounds, and performance is acceptable at 600 points fullscreen. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q152. Tooltip and point selection

Verify tap/drag selection hit-testing including nearest-point behavior at screen edges, tooltip positioning above/below without clipping, and deselection on a second tap. Fix confirmed interaction bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q153. LiveChart scroll and pulse

Verify the 150ms leftward shift on new data: no visual jump when multiple samples arrive between frames, previous-size tracking handles buffer trimming at 60 points, and the glow pulse does not retrigger on recomposition. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q154. Chart accessibility

Verify charts expose Role.Image with a meaningful summary description that updates with the data, and that selection changes are announced or intentionally silent. Fix confirmed accessibility gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Settings

### Q155. Alert threshold validation

Verify alert threshold inputs (low battery %, temperature, storage %) validate their ranges, persist correctly, and take effect on the next worker run without rescheduling problems. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q156. CSV export correctness

Review CSV export: escaping of commas, quotes, and newlines in values; locale-independent number formatting (a Finnish-locale device writing decimal commas would corrupt the CSV); FileProvider path confinement to cache/exports/; and temp-file cleanup. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q157. Clear-all-data completeness

Verify clear-all-data covers every Room table and the relevant DataStore keys, determine whether trial/Pro state is intentionally preserved or cleared, and verify the app is in a coherent state afterwards without a restart. Fix confirmed gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q158. Destructive action confirmations

Verify every destructive action (clear speed tests, clear all data, reset tips, reset thresholds) shows a confirmation dialog with the primary blue confirm button, and that dialogs restore correctly across rotation. Fix confirmed gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q159. Interval change side effects

Verify changing the monitoring interval reschedules all three periodic workers consistently and updates the staleness threshold math immediately. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q160. Device info accuracy

Verify the device capability rows (current-now reliability, cycle-count availability, thermal zone count) read from the persisted device profile and match what the capability manager actually detected. Fix confirmed mismatches.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---


### Q161. CSV export scope and file lifecycle

Audit CSV export scope and file lifecycle. Verify exactly which tables, date ranges, units, confidence values, settings, device identifiers, app package names, and Pro-only data are included or deliberately excluded. Also verify unique names, atomic writes, cancellation cleanup, cache/exports-only placement, bounded retention, clear-all behavior, and no stale partial files or unbounded cache growth. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Live Notification Service

### Q162. Foreground service lifecycle

Verify RealTimeMonitorService start/stop from the Settings toggle: startForeground is called within the required window, POST_NOTIFICATIONS is pre-checked on API 33+, the service stops immediately on toggle-off, and there is no restart loop after task removal. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q163. Update loop implementation

Verify the 5-second update loop uses a lifecycle-aware coroutine rather than a raw timer, behaves correctly in Doze, and that a battery-data read failure cannot kill the service silently. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q164. Notification content vs toggles

Verify the title format (level · status · temp) respects the per-metric toggles, the BigTextStyle expanded lines match enabled toggles exactly, and units follow the °C/°F preference. Fix confirmed mismatches.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q165. Channel configuration

Verify the real_time_monitor channel uses IMPORTANCE_LOW with no badge as documented, and that the channel is created before the first notify on every path. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q166. Special-use type declaration

Verify the FOREGROUND_SERVICE_TYPE_SPECIAL_USE declaration includes the required subtype property in the manifest and matches the runtime startForeground type on API 34+. Fix confirmed mismatches.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q167. Service/preference desync

Verify the case where the system kills the service while the preference still says enabled: does the toggle show on while nothing runs, and is there a reconciliation path such as on next app open? Fix confirmed desync handling gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Widgets

### Q168. Widget data freshness

Verify the Glance widgets read the latest Room snapshots, refresh on Pro state change as documented, and show a defined state when no snapshot exists yet. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q169. Widget Pro gating

Verify widget behavior for free and expired-trial users: what renders if Pro lapses while a widget is placed, and whether adding a widget as a free user communicates the Pro requirement. Fix confirmed gating gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q170. Widget update cost and containment

Verify widget update paths (maintenance worker best-effort, Pro change) cannot run expensive work on the main thread and that update failures are contained. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q171. Widget receiver security

Verify widget receivers are non-exported and protected with BIND_APPWIDGET as documented, with no exported action handlers. Fix confirmed exposure.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Notifications & Alerts

### Q172. Notification permission gating

Verify every notification post checks POST_NOTIFICATIONS on API 33+ through RuncheckPermissionPolicy, and denial degrades silently without repeated permission nags. Fix confirmed gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q173. Alert channel stability

Verify each alert type's channel configuration, that channel IDs are stable across releases, and that no code path posts to a nonexistent channel. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q174. Threshold comparison correctness

Verify low battery, high temperature, and low storage alerts compare against the user's configured thresholds with correct inclusive/exclusive boundaries and correct units. Fix confirmed boundary bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q175. Charge complete behavior

Verify the charge-complete alert fires at the right condition, respects its disabled-by-default setting, and that the debounce prevents re-firing while the phone stays plugged at 100%. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q176. Notification tap actions

Verify PendingIntent immutability flags, correct destination for each notification type, and compliance with the argument-free directRoutes restriction. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Learn

### Q177. Catalog integrity

Verify all 15 Learn articles have complete resources, every referenced article ID (including aliases) resolves, and cross-link route validation at startup covers every link. Fix confirmed gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q178. Article rendering

Verify the structured body renderer handles every block type present in the catalog and that long articles scroll without state glitches. Fix confirmed rendering bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q179. Legacy ID aliases

Verify the alias mapping: no alias points to a missing canonical ID, and no alias shadows a real ID. Fix confirmed mapping errors.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q180. Cross-links and tier behavior

Verify Learn cross-links into app routes behave correctly for each user tier — a link into a Pro route should behave like other free-tier entries into that route. Fix confirmed inconsistencies.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Permissions

### Q181. RuncheckPermissionPolicy coverage

Audit RuncheckPermissionPolicy: verify mediaPermissionsForApi returns the correct permission set per API level (granular media on 33+, partial visual access on 14+, READ_EXTERNAL_STORAGE on 32 and below), and that callers use the policy rather than hardcoding permissions. Fix confirmed drift.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q182. Location for SSID

Verify coarse and fine location are requested together as documented, only from the WiFi-detail context, and that denial leaves all network features functional except SSID display. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q183. Permanently-denied handling

Verify shouldShowRequestPermissionRationale handling: permanently-denied detection routes users to app settings instead of re-requesting in a loop. Fix confirmed loops.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q184. Manifest vs runtime permission alignment

Cross-check every declared manifest permission against actual code usage. Flag any declared-but-unused permission (a Play policy risk) and any usage path missing its declaration. Fix confirmed misalignments.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q185. maxSdk attribute alignment

Verify READ_EXTERNAL_STORAGE maxSdk 32 and WRITE_EXTERNAL_STORAGE maxSdk 28 align with the code paths referencing them, especially the legacy cleanup delete path around API 29. Fix confirmed misalignments.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Security & Privacy

### Q186. Backup exclusion completeness

Verify data_extraction_rules and backup_rules actually exclude databases, DataStore files, and SharedPreferences (including pro_status_cache) from both cloud backup and device transfer on all relevant API levels. Fix confirmed exclusion gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q187. Network security config

Verify network_security_config permits system trust anchors only, no debug-overrides block can leak into release, and cleartext is fully disabled. Fix confirmed weaknesses.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q188. FileProvider scope

Verify the csv_exports FileProvider exposes only cache/exports/, granted URIs are temporary and revoked appropriately, and exported filenames cannot traverse paths. Fix confirmed scope problems.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q189. Log hygiene

Verify ReleaseSafeLog is used instead of android.util.Log throughout, no sensitive values (SSID, IP addresses, purchase tokens) are logged even in debug, and the debug Sentry configuration matches the documented minimal surface. Fix confirmed leaks.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q190. Exported components audit

Verify the only exported component is the main launcher activity, every receiver/service/provider sets android:exported explicitly, and no intent filter widens exposure. Fix confirmed exposure.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q191. Outbound network surface

Search for network primitives (Socket, HttpURLConnection, OkHttp usage) and verify every outbound connection maps to one of the three approved surfaces: NDT7 speed tests, TCP latency to the configured host, and Play Billing. Fix confirmed unauthorized surfaces.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q192. Sentry release exclusion

Verify release builds have no Sentry classes on the classpath at all (not merely a no-op init), and that the debug DSN can never be sourced from a committed file. Fix confirmed leaks.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q193. Untrusted input handling

Review data arriving from broadcasts, MediaStore, and NDT7 responses as untrusted input: no raw-SQL injection paths, no format-string issues, and length limits where values render in notifications. Fix confirmed handling gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Performance & Memory

### Q194. Main-thread I/O audit

Audit for main-thread disk or IPC calls: the /proc/mounts read, StorageStatsManager queries, PackageManager queries, and any synchronous-style DataStore access. Cross-check against what debug StrictMode would flag. Fix confirmed main-thread violations.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q195. 333ms sampling semantics

Verify the 333ms live-flow sampling: whether sample, conflate, or another operator is used, that the semantics match the intent (latest value shown, no missed terminal states), and that no hot flow runs without a subscriber scope. Fix confirmed misuse.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q196. Leak candidates

Look for leaks: thermal/connectivity/screen-state listeners registered in objects that outlive their scope, uncancelled coroutine scopes, and composables capturing state in long-lived lambdas. Fix confirmed leaks.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q197. Draw-phase allocation

Verify charts and the thermometer hero do not allocate Paths or Brushes per frame where reuse is possible, and that Canvas draw lambdas read only the inputs they need. Fix confirmed per-frame allocation only where it measurably matters.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q198. Room query volume

Check for N+1 patterns (per-item DAO calls in loops), queries returning unbounded history where a limit exists conceptually, and flows re-querying due to observing overly broad table sets. Fix confirmed inefficiencies minimally.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q199. Startup cost

Review RuncheckApp initialization order: verify nothing blocks the main thread longer than necessary (billing init, channel creation, StrictMode setup) and that deferrable cold-start work is deferred. Fix confirmed blocking work.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q200. Background wakeup discipline

Verify background work coalesces sensibly and does not wake the device more often than the chosen monitoring interval implies. Fix confirmed excess wakeups.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q201. Layout and overdraw spot check

Spot-check the heaviest screens for unnecessary nested layers, redundant backgrounds, and modifier chains that force extra layout passes. Fix only clear, confirmed cases.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Coroutines & Flows

### Q202. Dispatcher discipline

Verify IO-bound work uses injected dispatchers for testability, no hardcoded Dispatchers.Main appears in domain or data layers, and ViewModels remain compatible with the shared MainDispatcherRule. Fix confirmed violations.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q203. Flow exception handling

Verify catch operators never swallow CancellationException, flows exposed to the UI have defined error emissions, and supervisorScope is used where one child's failure should not kill siblings. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q204. LifecycleStartStopEffect symmetry

Review the centralized start/stop effect: verify ON_START/ON_STOP symmetry, no double-start on configuration change, and that collectors genuinely stop at ON_STOP rather than continuing to buffer. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q205. stateIn/shareIn configuration

Verify SharingStarted policies and WhileSubscribed timeouts on shared flows, initial values that cannot flash incorrect UI, and no shareIn on flows collected exactly once. Fix confirmed misconfiguration.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q206. Check-then-act races

Look for races such as Pro state checked before an async gated action completes, monitoring reschedule racing a preference write, and speed-test start racing the network lock capture. Fix confirmed races.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q207. Blocking calls in coroutines

Search for runBlocking outside tests and legitimate entry points, Thread.sleep in production code (the 300ms capability-validation spacing should use delay), and blocking .get() calls on futures. Fix confirmed blocking calls.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---


### Q208. Android listener flow lifecycle

Audit callbackFlow and channelFlow wrappers plus lifecycle handling for Android listeners and callbacks. Verify awaitClose unregisters exactly the registered callback, duplicate registration is impossible, send failures are handled, callbacks cannot race after closure, thermal, network, battery, screen, Billing, and package observers have initial state, and Android 14+ dynamic receivers supply the required exported or not-exported flags. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q209. Locking and synchronization

Audit Mutex, synchronized blocks, atomic types, Room transactions, and shared mutable state used by flows, repositories, workers, and services. Verify lock scope is minimal, lock ordering cannot deadlock, suspending calls are not made under unsuitable JVM locks, unrelated keys are not serialized unnecessarily, and foreground/background paths cannot race into lost writes. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Error Handling & Edge Cases

### Q210. Fresh-install empty states

Verify every screen renders sensibly with zero history: charts, insights, statistics panels, and widgets all need a defined empty state, never a crash or an infinite spinner. Fix confirmed empty-state failures.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q211. Clock change resilience

Verify behavior under manual clock changes, NTP corrections, and timezone/DST shifts: history queries, trial day math, insight windows, and session durations should each use the appropriate clock source (elapsedRealtime vs wall clock). Fix confirmed clock-source errors.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q212. Process death mid-operation

Verify recovery when the process dies during an active speed test, a cleanup delete request, a purchase flow, and a monitoring write. Fix confirmed unrecoverable states.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q213. Extreme value rendering

Verify the UI handles extremes: 0% and 100% battery, negative temperatures, very large free-storage values, signal from -30 to -120 dBm, and multi-day session durations. Fix confirmed formatting or layout failures.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q214. Rapid toggle safety

Verify rapidly toggling settings switches (live notification, master notifications) cannot create inconsistent service or preference state. Fix confirmed inconsistency windows.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q215. Locale-dependent formatting

The app is English-only, but device locale affects number and date formatting. Verify UiFormatters output stays consistent and CSV export stays machine-parseable regardless of device locale. Fix confirmed locale bugs.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Formatting & Accessibility

### Q216. UiFormatters conventions

Review every formatter against the documented conventions (percentages, temperature, storage size, current, voltage grouping, power with one decimal, speed, latency, duration): verify rounding rules, negative-value handling, and unit spacing consistency. Fix confirmed deviations.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q217. Touch target compliance

Verify all interactive elements meet the 48dp minimum: the chart expand button, info icons, chip rows, and legend items if tappable. Fix confirmed undersized targets.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q218. Semantics coverage

Verify semantics: ProgressRing exposes progress info, decorative elements are cleared, headings are marked, ConfidenceBadge is described, and toggles announce state. Fix confirmed semantics gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q219. Contrast spot check

Verify the real text-on-accent combinations meet WCAG AA at their actual sizes: TextOnLime on lime, BgPage-colored text on amber badges, and status colors on card backgrounds. Fix confirmed contrast failures.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q220. Reduced motion completeness

Verify LocalReducedMotion reaches every documented animation (rings, bars, charts, count-ups, badges, heat-strip pulse, navigation transitions) with instant final states. Fix confirmed misses.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---


### Q221. Responsive layout and scaling

Audit large font, display scaling, RTL and pseudolocale assumptions, tablet, foldable, split-screen, landscape, font-scaled, and narrow-display layouts across Home, detail screens, Cleanup, App Usage, Settings, Pro Upgrade, widgets, dialogs, and fullscreen charts. Verify content does not clip, touch targets remain reachable, state restores across orientation changes, left/right assumptions do not break future localization, and large screens do not reveal locked or stale content accidentally. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Testing

### Q222. Coverage gap mapping

Map the existing unit tests against the riskiest logic: identify untested public behavior in scoring, insight rules, billing state transitions, and migrations. List the gaps; write a new test only where you also confirmed and fixed a bug.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q223. Test quality review

Review existing tests for assertions that cannot fail, mocks that re-implement the logic under test, and time-dependent tests likely to flake. Fix confirmed weak tests.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q224. MainDispatcherRule consistency

Verify all ViewModel tests use the shared MainDispatcherRule consistently and no test leaks a Main dispatcher override to other tests. Fix confirmed leaks.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q225. Test double boundaries

Verify test doubles sit at repository or data-source boundaries rather than mid-domain, so tests exercise real use-case logic. Fix only clear, confirmed boundary violations.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---


### Q226. Previews, test tags, and reusable component contracts

Audit Compose previews, test tags, semantic matchers, and reusable components. Verify previews do not require production DI or live Android services, test tags are stable and selective, and components accept realistic loading, error, unavailable, empty, long-text, large-font, and permission-denied states. Fix confirmed gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Build, Dependencies & Release

### Q227. Version catalog consistency

Verify all dependencies resolve through libs.versions.toml with no hardcoded versions in build files, and confirm the Kotlin 2.3.0 plugin vs 2.3.20 runtime-constraints split is intentional and internally consistent as documented. Report findings; change versions only if provably broken.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q228. R8/ProGuard keep rules

Verify release minification keeps everything that reflection needs: Room, Hilt, Glance, the NDT7 client, and especially Gson models used for serialization. Fix confirmed missing keep rules.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q229. BuildConfig field wiring

Verify LATENCY_HOST/LATENCY_PORT and RUNCHECK_PRO_PRODUCT_ID BuildConfig wiring: correct defaults, debug-only overrides, and no secrets embedded in BuildConfig. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q230. Release signing guards

Verify the release signing property requirements and the versionCode floor mechanism fail fast with clear messages when unset, and that the documented no-configuration-cache requirement is actually necessary and enforced. Fix confirmed guard gaps.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q231. Dependency hygiene

Do a source-level check for unused declared dependencies and used-but-undeclared ones. Flag only clear cases; do not churn the build files for marginal findings.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q232. Repository restriction enforcement

Verify the settings.gradle.kts repository restrictions hold (google, mavenCentral, JitPack only for m-lab) and that dependency verification metadata covers the current artifact set. Fix confirmed holes.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---


### Q233. Release shrinking and artifact contents

Audit release resource shrinking, minification, and APK/AAB contents together. Verify dynamically referenced routes, icons, strings, widget metadata, notification channels, Gson-reflected models, schema internals, credentials, local properties, source maps, debug reports, logs, unused native libraries, repository metadata, and certificates are handled intentionally and trace unexpected contents to their source. Fix confirmed defects without disabling shrinking globally.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q234. Release variant verification

Audit release-variant verification. Confirm release Kotlin compilation, manifest merge, R8/minification, resource shrink, Hilt/KSP generation, Sentry exclusion, signing preconditions, and a minimal release smoke path are checked somewhere. Fix confirmed gaps or add narrow verification only where necessary.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q235. CI workflow reliability

Review GitHub Actions, CodeQL, Sonar, and Qodana workflows end to end. Verify least-privilege permissions, action pinning, fork-PR secret safety, cache poisoning protections, concurrency cancellation, artifact retention, branch and path filters, Kotlin/AGP/Gradle compatibility, query and report inputs, coverage inputs, SARIF upload behavior, token handling, and failure behavior. Fix confirmed workflow defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q236. Local quality and security tooling

Review local PowerShell wrappers, quality/security baselines, suppressions, and report paths. Verify -PlanOnly performs no heavy or mutating work, prerequisites and exit codes are correct, quoting works, compatibility wrappers forward all arguments, and ktlint, Detekt, Android lint, compose-rules, Google security lints, Semgrep, mobsfscan, DeepSec, CPD, and Compose Stability exclusions are narrow, justified, current, and not hiding production code. Fix confirmed defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q237. Supply-chain and Play release readiness

Review dependency, secret, supply-chain, and v1.0 Play release readiness checks. Verify Gradle verification, OSV, Dependency-Check, Dependabot, gitleaks, TruffleHog, repository filters, lock/pin coverage, false-positive policy, actionable failure thresholds, versioning, signing, AAB contents, target SDK implications, data safety and privacy disclosures, permissions, special-use foreground service declaration, billing product, store assets, smoke tests, rollback, and reproducibility. Fix confirmed release blockers.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Manifest & Resources

### Q238. Full manifest audit

Review the complete AndroidManifest: allowBackup false, cleartext disabled, the queries block scope, explicit export flags on every component, locale config reference, and any attribute contradicting the documented security posture. Fix confirmed contradictions.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q239. Resource hygiene

Verify no hardcoded user-visible text exists in composables (everything through strings.xml), identify clearly unused resources, and confirm release builds ship the empty non-translatable strings for the debug insights section as documented. Fix confirmed violations.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q240. Locale configuration alignment

Verify localeFilters ["en"] and locales_config.xml agree, and that per-app language settings cannot produce a surprising state. Fix confirmed misalignment.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---


### Q241. App icon and visual asset audit

Audit app icons, adaptive icon layers, monochrome icon, notification icons, widget previews, SVG/PNG masters, density variants, and resource references. Identify missing densities, full-color notification icons, clipping, stale assets, or launcher and widget previews that do not match the current brand. Fix confirmed asset defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q242. Theme and splash resources

Audit theme and splash-screen resources across API levels. Verify dark-only behavior, status and navigation bar colors, edge-to-edge settings, splash icon and background, and absence of a light-theme flash during cold start. Fix confirmed visual or configuration defects.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

## Open-Ended Sweeps

### Q243. Most likely real user-visible bug

Beyond everything asked in previous review questions, read across the whole project and identify the single most likely real, user-visible bug you can find and confirm anywhere in the codebase. Fix it if confirmed.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q244. What did this review miss

Considering the whole codebase, what risky area, subsystem, or interaction has this review series most likely not covered but that deserves inspection? Pick the strongest candidate, investigate it, and fix confirmed issues.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---

### Q245. Documentation drift audit

Compare PROJECT.md and UI-SPEC.md against the actual code and list every place where the documentation and the implementation have drifted apart. Report the drift factually as a list; do not modify documentation or code for drift items without listing them first.

Important: LLM reviewers often hallucinate. They invent bugs that don't exist, flag correct code as broken, and over-engineer fixes where simpler code is fine. Verify every finding against the actual code before changing anything. "No confirmed issue found" is a fully valid and useful outcome. Fix only confirmed issues, using minimal diffs instead of rewrites. In your report, separate: confirmed fixes (with an explanation of each change), items needing verification you couldn't complete, optional refactors you deliberately did not apply, and explicit no-issue notes.

---
