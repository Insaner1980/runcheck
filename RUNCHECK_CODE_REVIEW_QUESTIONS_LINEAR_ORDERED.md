# Runcheck - Linear Ordered Codex Review Prompts

Validation:

- Total prompts in this file: 511
- Original ChatGPT prompts included: RC-001 through RC-500, all present exactly once
- Supplemental prompts included: RC-501 through RC-511, all present exactly once
- Claude comparison source parsed: 215 questions

## Q001 (source RC-002)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Compare settings.gradle.kts, root build.gradle.kts, app/build.gradle.kts, gradle/libs.versions.toml, and gradle/wrapper/gradle-wrapper.properties with all prose documentation. Which documented versions, plugins, repositories, SDK levels, feature flags, or build assumptions no longer match executable configuration? Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```
## Q002 (source RC-003)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Verify that package root, namespace, applicationId, Kotlin package declarations, manifest component names, test packages, generated-code packages, FileProvider authority, and widget authorities consistently use com.runcheck without legacy identifiers or variant collisions. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q003 (source RC-004)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Inventory app/src/main, app/src/debug, app/src/release, app/src/test, and app/src/androidTest. Identify undocumented production code, missing documented components, duplicate implementations, orphaned files, source-set shadowing, or files placed in a source set that changes release behavior unexpectedly. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q004 (source RC-005)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Reconstruct the actual navigation graph from code and compare it with the documented route list and hierarchy. Verify route names, arguments, Pro gating, direct-route eligibility, learn cross-links, fullscreen-chart arguments, result keys, and expected back-stack behavior. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q005 (source RC-006)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Reconstruct the actual Room schema from entities, DAOs, RuncheckDatabase, exported schema JSON, and migrations. Compare it with the documented table list, schema version, indexes, nullability, defaults, and migration history. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q006 (source RC-007)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Map every actual Pro-gated user path and backend action, then compare it with PROJECT.md and ProFeature. Check screens, routes, ViewModels, widgets, export, history queries, cleanup, remaining-time estimates, thermal logs, background collection, and fullscreen access for undocumented or inconsistent gates. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q007 (source RC-009)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Inventory all manifest and runtime permission use sites. Verify each declared permission has a real feature justification, every permission-dependent feature handles denial and API-level differences, no used permission is missing, and no obsolete permission remains. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q008 (source RC-010)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Audit the debug-only insight tooling and release-safe stubs across source sets. Verify class names, packages, DI bindings, resources, call sites, and public contracts resolve correctly in both variants without debug behavior or dependencies leaking into release. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q009 (source RC-011)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Verify every version number and compatibility statement recorded in PROJECT.md against the repository itself, including app version, Room schema version, Gradle wrapper, AGP, Kotlin plugins and constraints, KSP, Compose BOM, Java target, compile SDK, target SDK, and min SDK. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q010 (source RC-012)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit all documented environment variables, local credential files, ignored paths, BuildConfig overrides, and release-signing inputs. Check actual names, precedence, validation, error messages, .gitignore coverage, and whether secrets can reach logs or generated artifacts. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q011 (source RC-013)

```text
[Priority: P1 | Execution: TARGETED_BUILD, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Verify that the English-only product decision is enforced by localeFilters, locales_config.xml, resource directories, manifest metadata, app resources, tests, and user-facing strings. Flag hidden non-English resources, hardcoded text, or partial locale declarations. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q012 (source RC-014)

```text
[Priority: P2 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Compare the documented low-CPU verification policy and narrow commands with actual Gradle tasks and PowerShell wrappers. Identify commands that no longer exist, silently run broader work, require unavailable tools, ignore -PlanOnly, or write reports somewhere other than documented. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q013 (source RC-015)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit every GitHub Actions workflow named in PROJECT.md. Verify trigger branches, action versions, permissions, build commands, artifacts, SARIF upload behavior, secrets, cache keys, and whether the described status and purpose still match the YAML. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q014 (source RC-016)

```text
[Priority: P2 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Separate shipped behavior from future considerations and roadmap claims. Find code, flags, UI, tests, database fields, or documentation that partially implements a roadmap item but describes it as absent, or describes an unshipped behavior as current. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q015 (source RC-017)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review naming consistency across files, classes, routes, database tables, DataStore names, unique work names, notification channels, BuildConfig fields, product IDs, analytics/debug identifiers, and user-facing labels. Identify synonyms or collisions likely to cause maintenance errors. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q016 (source RC-018)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Find dead documentation references, obsolete comments, stale TODOs, commented-out implementations, unused diagrams, old migration notes, and generated files that imply behavior the app no longer has. Determine which should be removed, updated, or deliberately retained. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q017 (source RC-019)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Audit checked-in and generated artifacts - Room schemas, dependency verification metadata, lockfiles if present, baseline files, icon exports, reports exclusions, generated resources, and security suppressions - for completeness, freshness, reproducibility, and source-of-truth clarity. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q018 (source RC-020)

```text
[Priority: P1 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Compare PROJECT.md, CODEX.md, AGENTS.md, README files, workflow comments, issue templates, and tool-script help text for contradictory repository rules. Propose the smallest documentation changes needed to establish one unambiguous instruction hierarchy. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q019 (source RC-021)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit compatibility among Gradle wrapper 9.4.0, AGP 9.1.0, Android 17 CinnamonBun preview SDK, Java 17, Kotlin Gradle/Compose plugin 2.3.0, Kotlin runtime constraints 2.3.20, and KSP 2.3.1 using the actual resolved toolchain rather than documentation alone. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q020 (source RC-022)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Inspect plugin declarations and application across settings, root, and app Gradle files. Verify org.jetbrains.kotlin.plugin.compose is configured correctly and kotlin-android is not implicitly or accidentally reintroduced through aliases, convention plugins, applied scripts, or transitive plugin behavior. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q021 (source RC-023)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Verify compileSdk and targetSdk preview configuration for Android 17 CinnamonBun, including SDK syntax, installed-platform assumptions, build-tools selection, lint behavior, manifest merging, and runtime guards for preview-only APIs. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q022 (source RC-024)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR, EXTERNAL_CURRENT_DATA | Mode: AUDIT]

In the runcheck Android repository, Audit minSdk 26 compatibility across source code and dependencies. Find calls, resources, manifest attributes, Compose APIs, Glance APIs, notification APIs, MediaStore APIs, and Java library methods that require a higher API without guards or desugaring support. When a platform API name, overload, flag, or signature is uncertain, verify it against the installed Android SDK stubs or official Android documentation instead of relying on memory. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q023 (source RC-025)

```text
[Priority: P1 | Execution: TARGETED_BUILD, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review Java and Kotlin target configuration across production, unit tests, instrumented tests, KSP, lint, and custom tasks. Verify consistent Java 17 toolchains and bytecode without target-validation suppression or mixed target levels. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q024 (source RC-026)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Compose compiler/plugin configuration and Compose BOM use. Verify no explicit Compose artifact version unintentionally overrides the BOM, no obsolete compiler option remains, and build features are enabled only where required. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q025 (source RC-027)

```text
[Priority: P1 | Execution: TARGETED_BUILD, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Inspect KSP setup for Room and Hilt. Verify processor configurations, generated source visibility, incremental mode, schema arguments, test processors, variant coverage, and compatibility with the selected Kotlin plugin. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q026 (source RC-028)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review all BuildConfig fields such as latency host, latency port, Pro product ID, and telemetry inputs. Verify type-safe quoting, defaults, variant overrides, secret handling, and protection against malformed environment values that break generated source or runtime behavior. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q027 (source RC-029)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit release signing logic. Verify unsigned local release builds remain possible only when intended, signed artifact tasks fail early when any RUNCHECK_* input is missing, and secrets never enter logs, configuration-cache entries, build scans, or generated files. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q028 (source RC-030)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Check configuration-cache and build-cache compatibility of custom Gradle logic and PowerShell wrappers. Identify eager environment/file reads, deprecated APIs, task-time mutation, undeclared inputs/outputs, or Provider API violations. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q029 (source RC-031)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit dependency and repository resolution management in settings.gradle.kts. Verify FAIL_ON_PROJECT_REPOS is effective, plugin repositories are appropriately restricted, and no subproject or applied script can add an unapproved repository. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q030 (source RC-032)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR, EXTERNAL_CURRENT_DATA | Mode: AUDIT]

In the runcheck Android repository, Review packaging configuration for duplicate META-INF files, native libraries, license files, resources, and service descriptors. Verify exclusions or pickFirst rules do not remove runtime-critical metadata from Room, Hilt, OkHttp, NDT7, Billing, Glance, or security tooling. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q031 (source RC-033)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit debug and release optimization settings, including minification, resource shrinking, R8/ProGuard rules, debuggability, JNI debug flags, and BuildConfig generation. Determine whether optimized release behavior has actually been compiled and smoke-tested. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q032 (source RC-034)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Inspect Android lint configuration, baselines, disabled checks, warning-as-error policy, generated/test-source handling, and Google security lint integration. Identify suppressions that hide production defects, no longer match issue IDs, or apply too broadly. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q033 (source RC-035)

```text
[Priority: P1 | Execution: TARGETED_BUILD, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit local and instrumented test Gradle configuration, including Android resources, test runner, orchestration, managed devices if any, runner arguments, animations, packaging, and schema assets. Verify narrow commands select the expected variant and class. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q034 (source RC-036)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review source-set declarations for main, debug, release, test, and androidTest. Check duplicate roots, unconventional generated-source wiring, resource overlays, manifest overlays, and whether release-safe stubs are guaranteed to compile in release. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q035 (source RC-037)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit reproducible-build properties: deterministic archives, pinned wrapper distribution and checksum, dependency verification, dynamic timestamps, environment-sensitive BuildConfig values, generated schemas, and machine-specific paths. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q036 (source RC-038)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Inspect custom Gradle tasks and task dependencies created by quality/security plugins. Verify narrow checks do not unexpectedly trigger full builds, require network access without notice, write outside ignored reports paths, or mutate source files. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q037 (source RC-039)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Review app versioning and artifact naming. Verify versionCode and versionName have one authoritative definition, release artifacts cannot reuse a published versionCode accidentally, and debug/release application identifiers and labels behave as intended. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q038 (source RC-040)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Perform a source-backed audit of APIs deprecated or removed by AGP 9.x and Gradle 9.x in build scripts. Identify deprecated DSL properties, internal APIs, eager task access, or plugin assumptions likely to fail on a future minor upgrade. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q039 (source RC-508)

```text
[Priority: P1 | Execution: REPO_ONLY, TARGETED_BUILD, EXTERNAL_CURRENT_DATA | Mode: AUDIT]

In the runcheck Android repository, Validate every suspicious Android API name, overload, manifest attribute, permission, receiver flag, foreground-service declaration, Billing API, WorkManager API, Room annotation, Glance API, and Compose API against the installed SDK stubs or official documentation. Do not trust memory for preview SDK or recently changed APIs. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q040 (source RC-041)

```text
[Priority: P1 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Audit gradle/libs.versions.toml as the dependency and plugin source of truth. Find hardcoded versions elsewhere, duplicate aliases, unused aliases, inconsistent bundles, and coordinates that bypass the catalog without a documented reason. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q041 (source RC-042)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Inspect the resolved dependency graph for Kotlin artifacts. Verify the 2.3.20 runtime constraints align stdlib, reflect, and transitive Kotlin modules without forcing versions incompatible with the 2.3.0 Gradle/Compose plugin. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q042 (source RC-043)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Compose dependency alignment under BOM 2026.03.00. Find artifacts outside the BOM, alpha/beta mismatches, duplicate Material generations, incompatible Glance/Compose runtime combinations, or explicit versions that defeat platform alignment. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q043 (source RC-044)

```text
[Priority: P1 | Execution: TARGETED_BUILD, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review every direct dependency for actual use. Identify unused libraries, duplicate functionality, APIs referenced only by dead code, debug tools on production configurations, and dependencies that should use narrower scopes such as runtimeOnly, ksp, testImplementation, or debugImplementation. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q044 (source RC-045)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit transitive conflicts and forced versions, focusing on OkHttp/Okio, Gson, lifecycle, navigation, WorkManager, Room, Paging, Hilt, Billing, Kotlin, coroutines, Guava/listenablefuture, and annotations. Explain any resolution rule that masks incompatibility. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q045 (source RC-046)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Verify repository provenance for every resolved artifact. Confirm JitPack is content-filtered to com.github.m-lab only, no dependency redirects to an unapproved repository, and plugin resolution cannot fetch arbitrary artifacts from JitPack. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q046 (source RC-047)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Gradle dependency verification metadata and checksum/signature coverage. Determine whether production, test, plugin, and buildscript artifacts are verified, whether trusted keys are overly broad, and whether recent changes left verification gaps. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q047 (source RC-048)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Check for dynamic, changing, snapshot, preview, local-file, composite-build, or unpinned Git dependencies. Confirm versions and commit references are immutable enough for reproducible release builds. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q048 (source RC-049)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR, EXTERNAL_CURRENT_DATA | Mode: AUDIT]

In the runcheck Android repository, Audit license and notice obligations for production dependencies, fonts, icons, and M-Lab components. Verify required attribution is present and no license conflicts with Play Store distribution or planned commercial use. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q049 (source RC-050)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Verify debug-only Sentry dependency isolation by inspecting releaseRuntimeClasspath, release compile inputs, merged manifests, generated resources, R8 inputs, and dependency reports for Sentry or transitive telemetry classes. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q050 (source RC-051)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit Play Billing 8.3.0 integration against the exact APIs used. Find deprecated methods, missing connection handling, incorrect product-type assumptions, and transitive version overrides that can change purchase behavior. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q051 (source RC-052)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit Room 2.8.4 and KSP processor alignment. Verify runtime, KTX, Paging, testing, compiler, schema export, and migration-test artifacts are compatible with no kapt remnant or mixed code generation. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q052 (source RC-053)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit Hilt 2.59.2 dependencies and processors across app, tests, WorkManager integration, and Android entry points. Identify duplicate javax/jakarta injection artifacts, processor mismatch, or missing test components. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q053 (source RC-054)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit WorkManager 2.11.1 and Hilt-Work integration for API 26+, expedited/foreground behavior, startup components, and compatibility with the custom Configuration.Provider setup. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q054 (source RC-055)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Glance 1.1.1 against the app's Compose, Kotlin, target SDK, and widget APIs. Identify obsolete APIs, missing receivers/resources, or combinations that compile but fail during widget rendering or update. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q055 (source RC-056)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit M-Lab NDT7 client 1.0.0 with OkHttp 4.12.0. Verify it does not introduce an incompatible networking stack, duplicate TLS provider, unexpected cleartext endpoint, or unbounded transitive dependency. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q056 (source RC-057)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review versions of ktlint, ktlint Gradle plugin, Detekt, compose-rules, OWASP Dependency-Check, SonarQube, Compose Stability Analyzer, and Google Android Security Lints for mutually compatible APIs and Gradle 9 support. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q057 (source RC-058)

```text
[Priority: P0 | Execution: EXTERNAL_CURRENT_DATA | Mode: AUDIT]

In the runcheck Android repository, Audit vulnerability suppressions and dependency exclusions. For every suppression, verify component, version range, rationale, expiry, reachability, and whether a blanket rule or stale CVE assumption hides a real issue. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q058 (source RC-059)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Check dependency metadata for minimum SDK, native ABI, Java bytecode, Android resource, and namespace requirements. Identify artifacts whose published constraints exceed minSdk 26, Java 17, or the app's packaging choices. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q059 (source RC-061)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit class and resource resolution between app/src/debug and app/src/release. Verify each debug implementation has an API-compatible release-safe counterpart where required, with no duplicate classes, missing methods, or unintended selection. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q060 (source RC-062)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Inspect merged debug and release manifests separately. Verify exported flags, permissions, providers, receivers, services, intent filters, metadata, foreground-service types, widget declarations, and removed AndroidX Startup entries match intended behavior. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q061 (source RC-063)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Prove release Sentry initialization is a no-op at both source and packaging levels. Check manifest components, resources, service loaders, dependency classes, ProGuard rules, and reflective references in addition to the SentryInit implementation. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q062 (source RC-064)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit BuildConfig and resource values across variants. Verify debug credentials, latency endpoint, product ID, app label, version display, and feature flags resolve as expected without secret leakage or fallback ambiguity. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q063 (source RC-065)

```text
[Priority: P1 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Review manifest placeholder use and environment expansion. Verify missing placeholders fail predictably, XML escaping is safe, and untrusted environment text cannot create malformed manifests or alter component exposure. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q064 (source RC-066)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit app icons, adaptive icon layers, monochrome icon, notification icons, widget previews, SVG/PNG masters, density variants, and resource references. Identify missing densities, full-color notification icons, clipping, or stale assets. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q065 (source RC-067)

```text
[Priority: P1 | Execution: TARGETED_BUILD, TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Verify locale filtering and resource packaging. Check that only English resources ship intentionally, pseudolocales are test-only if present, and library translations do not create a misleading locale picker or partial localization state. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q066 (source RC-068)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit theme and splash-screen resources across API levels. Verify dark-only behavior, status/navigation bar colors, edge-to-edge settings, splash icon/background, and absence of a light-theme flash during cold start. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q067 (source RC-069)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Review network_security_config.xml and its manifest application in every variant. Confirm cleartext is disabled, only system trust anchors are allowed, no debug override weakens release, and domain configuration does not broaden trust. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q068 (source RC-070)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit backup and data-extraction rules for supported Android versions. Verify allowBackup=false is effective, no obsolete backup XML contradicts it, and cloud/device transfer cannot expose Room, DataStore, SharedPreferences, or exported CSV files. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q069 (source RC-071)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Review file_export_paths.xml and FileProvider authority configuration. Verify only cache/exports is shared, path traversal is impossible, authorities are unique per application ID, and debug/release installations do not collide. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q070 (source RC-072)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit R8 keep rules and consumer rules for Room, Hilt, Billing, NDT7, Gson serialization, WorkManager, Glance, Android components, and reflection. Flag both missing rules and overly broad keep rules. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q071 (source RC-073)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Inspect resource shrinking/minification for dynamically referenced routes, icons, strings, widget metadata, notification channels, and Gson-reflected models. Verify release retains required resources without disabling shrinking globally. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q072 (source RC-074)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit application labels, package visibility queries, launcher activity, and intents for Play Store, mail, system settings, and sharing. Verify manifest queries are minimal and every external intent is safely resolvable. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q073 (source RC-075)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review notification channel identifiers, names, descriptions, icons, colors, and sound/vibration resources for stability across upgrades. Verify renamed resources cannot create duplicate channels or orphan settings. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q074 (source RC-076)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit widget receiver/provider XML metadata, preview resources, min dimensions, resize modes, update periods, exported/permission attributes, and manifest merge behavior around BIND_APPWIDGET. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q075 (source RC-077)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Inspect test and debug manifests for permissions or components that mask production issues. Verify tests do not pass only because debug adds storage, network, exported, or cleartext allowances absent from release. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q076 (source RC-078)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit packaging of Room schema assets and test assets. Verify migration tests consume exact production-exported schemas and cannot silently select stale schemas from another path. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q077 (source RC-079)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Review APK/AAB contents for unexpected source maps, credentials, local properties, debug reports, schema internals, certificates, logs, unused native libraries, or repository metadata, and trace each to its source. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q078 (source RC-080)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Compare debug and release behavior for Pro state, telemetry, StrictMode, logging, debuggability, billing IDs, and network endpoints. Identify boundaries that rely only on a runtime flag instead of compile-time isolation. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q079 (source RC-081)

```text
[Priority: P2 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit all imports and dependency directions to verify ui depends on domain contracts/use cases rather than concrete data implementations. List every direct UI-to-data or UI-to-Android-service access and decide whether it is justified. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q080 (source RC-082)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit domain for Android framework dependencies. Excluding the documented PagingData exception, identify Context, Uri, Intent, Bitmap, Parcelable, lifecycle, Compose, Room, Billing, or platform API types leaking into domain. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q081 (source RC-083)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review the documented PagingData exception in cleanup and app-usage flows. Verify it is narrow, does not expose data entities, and does not force domain code to understand PagingSource, RemoteMediator, Android resources, or UI formatting. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q082 (source RC-084)

```text
[Priority: P2 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Trace each repository interface to its implementation and consumers. Verify contracts are cohesive, methods have consistent error/null semantics, streaming versus one-shot APIs are intentional, and implementation-specific behavior is not required implicitly. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q083 (source RC-085)

```text
[Priority: P2 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Audit use cases for single responsibility and meaningful business logic. Identify pass-through wrappers that add no value, oversized use cases coordinating unrelated concerns, and rules duplicated in ViewModels or repositories. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q084 (source RC-086)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review entity-to-domain and domain-to-UI mapping. Find Room entities, column names, JSON blobs, platform enums, or raw framework errors escaping the data layer, and redundant mappings that lose precision. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q085 (source RC-087)

```text
[Priority: P2 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit where formatting occurs for dates, durations, bytes, temperatures, percentages, dBm, Mbps, current, power, and statuses. Verify locale-sensitive presentation stays in UI while domain calculations use typed unformatted values. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q086 (source RC-088)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Search for circular conceptual dependencies among battery, thermal, network, storage, charger, scoring, insights, Pro, settings, and monitoring. Identify managers that have become hidden service locators or god objects. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q087 (source RC-089)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit package visibility and constructor visibility. Determine whether implementation classes, DAO details, mutable state holders, and test hooks are exposed more broadly than necessary. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q088 (source RC-090)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review transaction boundaries through the transaction-runner abstraction and direct Room usage. Verify multi-table operations and read-modify-write sequences are atomic at the correct layer without incompatible nested transactions. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q089 (source RC-091)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit error modeling across layers. Identify swallowed exceptions, raw SQLException/IOException/BillingResponseCode leakage into UI, ambiguous nulls, generic Exception catches, and user messages built from implementation errors. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q090 (source RC-092)

```text
[Priority: P0 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit time handling architecture. Verify wall clock, elapsed realtime, timezone, and date calculations are chosen intentionally; identify direct system-clock calls that make trial, freshness, retention, insights, and tests fragile. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q091 (source RC-093)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review testability of Android API wrappers. Verify BatteryManager, ConnectivityManager, PowerManager, MediaStore, UsageStatsManager, BillingClient, WorkManager, and clocks are isolated behind replaceable abstractions where deterministic tests matter. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q092 (source RC-094)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit shared util packages for misplaced business logic, Android dependencies, unsafe global state, and unrelated helpers. Recommend moves only where they improve dependency direction or testability. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q093 (source RC-095)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Find duplicate business rules across workers, foreground flows, ViewModels, use cases, and repositories - for example charger tracking, Pro gates, thresholds, freshness, history limits, or unit conversion - and identify the authoritative implementation. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q094 (source RC-096)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR, EXTERNAL_CURRENT_DATA | Mode: AUDIT]

In the runcheck Android repository, Audit ownership of navigation arguments, saved state, permissions, and external intents. Verify lower layers do not construct UI routes or Activity results and UI does not reimplement data safety checks. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q095 (source RC-097)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review Pro authorization placement. Verify UI gates improve UX while ViewModels/use cases/repositories also prevent unauthorized action when routes, widgets, notifications, restored state, or direct calls bypass UI controls. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q096 (source RC-098)

```text
[Priority: P0 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Search for static singletons, global mutable objects, companion caches, and object declarations holding Context, CoroutineScope, listeners, billing state, or mutable collections. Determine lifecycle and test-isolation risks. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q097 (source RC-099)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit interfaces with one trivial implementation and concrete framework classes with no interface. Determine where abstraction protects a real boundary versus adding ceremony, and where a missing boundary blocks testing. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q098 (source RC-100)

```text
[Priority: P2 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Assess whether the single app module remains internally enforceable as Clean Architecture. Identify package-level dependency rules or static checks that can prevent erosion without prematurely splitting every feature into modules. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q099 (source RC-510)

```text
[Priority: P1 | Execution: REPO_ONLY, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Choose the single most complex data-layer implementation file in the repository and review it end to end. Trace inputs, Android APIs, threading, error handling, cancellation, persistence, mapping to domain, tests, and UI consequences. Explain why that file was chosen and whether the same risk pattern appears elsewhere. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q100 (source RC-101)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit RuncheckApp as the Hilt application entry point. Verify initialization order for billing, Pro state, notification channels, screen-state tracking, monitoring scheduling, widget refresh observation, SentryInit, StrictMode, and WorkManager configuration. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q101 (source RC-102)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Inspect every Hilt module and @InstallIn target. Verify bindings live in the correct component, scopes match consumer lifetimes, and Activity, Service, Receiver, or Worker dependencies are not accidentally retained in SingletonComponent. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q102 (source RC-103)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit RepositoryModule bindings against all domain repository contracts. Find missing, duplicate, unused, or qualifier-dependent bindings and verify each implementation constructor is injectable without hidden service-locator calls. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q103 (source RC-104)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit DatabaseModule creation of RuncheckDatabase. Verify application context, singleton scope, all migrations 1→10, schema export, callbacks, journal/executor choices, and absence of accidental multiple database instances. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q104 (source RC-105)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review every DAO provider. Verify each DAO comes from the singleton database, same-typed providers cannot be confused, provider methods are scoped correctly, and test modules can replace the database cleanly. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q105 (source RC-106)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit SystemBindingsModule for Pro, billing, device profile, monitoring scheduler, screen-state tracking, foreground-app provider, and transaction-runner abstractions. Verify intended scope and absence of cycles hidden by Provider or Lazy. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q106 (source RC-107)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit InsightsModule multibindings. Prove every production InsightRule is included exactly once, ordering assumptions are absent or explicit, debug rules cannot enter release, and tests can provide deterministic sets. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q107 (source RC-108)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review DataModule's shared Gson. Verify adapters, naming, null handling, leniency, enum behavior, and singleton use are compatible with persisted device profile JSON and future model evolution. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q108 (source RC-109)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Audit debug and release InsightDebugModule implementations. Verify identical contracts, correct source-set selection, no duplicate @Module declarations, and no release dependency on debug-only classes, Sentry, or resources. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q109 (source RC-110)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Inspect Context injection throughout the app. Verify @ApplicationContext is used for long-lived objects, Activity context is not stored, and unqualified Context parameters cannot receive the wrong lifetime. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q110 (source RC-111)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit WorkManager initialization through Configuration.Provider and HiltWorkerFactory. Verify AndroidX Startup removal does not disable required initialization, getWorkManagerConfiguration is correct, and no code requests WorkManager too early. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q111 (source RC-112)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review every @HiltWorker and assisted constructor. Verify @Assisted Context and WorkerParameters placement, dependency scopes, generated factory compatibility, test construction, and recreation after process death. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q112 (source RC-113)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit injection into BootReceiver, ScreenStateReceiver, app-widget receivers/providers where applicable, and RealTimeMonitorService. Verify each Android component uses @AndroidEntryPoint or a safe explicit entry point and works before UI startup. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q113 (source RC-114)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review BillingManager, ProManager, and ProState injection/scoping. Verify there is one authoritative process-level instance, callbacks cannot target disposed objects, and UI consumers cannot create parallel BillingClient ownership. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q114 (source RC-115)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit singleton repositories and managers for thread safety. Identify mutable fields, listener registrations, caches, and internal coroutine scopes that need synchronization, immutable snapshots, or explicit teardown. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q115 (source RC-116)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review eager startup work. Identify database opens, package scans, network calls, Billing connection, device-capability validation, file I/O, or JSON parsing on Main and determine which can be lazy without correctness loss. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q116 (source RC-117)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Search for Hilt dependency cycles, including cycles broken by Lazy, Provider, entry points, interfaces, or callbacks. Explain initialization order and whether any cycle can expose partially initialized state. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q117 (source RC-118)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit optional capability dependencies. Verify absent Play Store/Billing, unsupported Android APIs, missing permissions, unavailable hardware data, and debug-only services are modeled as capabilities rather than failed injection. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q118 (source RC-119)

```text
[Priority: P1 | Execution: TARGETED_BUILD, TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review Hilt test setup. Verify instrumented tests can replace production modules, fakes do not leak between tests, generated components are available for all test variants, and tests do not require production credentials. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q119 (source RC-120)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Inspect generated Hilt component warnings and variant outputs for duplicate scopes, incompatible qualifiers, missing aggregating tasks, or release-only failures. Confirm both debug and release create complete component graphs. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q120 (source RC-121)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit all UI Flow collection for lifecycle awareness. Verify Compose screens use collectAsStateWithLifecycle or equivalent, collectors stop off-screen where appropriate, and process-wide flows are not redundantly collected on every recomposition. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q121 (source RC-122)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review every stateIn and shareIn call. Verify CoroutineScope ownership, SharingStarted policy, replay, initial value, and stop timeout match source semantics without keeping expensive hardware polling alive forever. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q122 (source RC-123)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit the 333 ms sampling or throttling used by Home and detail ViewModels. Verify it reduces UI churn without dropping terminal states, permission changes, disconnects, errors, or transitions that must appear immediately. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q123 (source RC-124)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Inspect large combine chains on Home and elsewhere. Check for emission storms, inconsistent snapshots, expensive mapping on Main, unbounded recomputation, and resets caused by one flow's placeholder initial value. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q124 (source RC-125)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit cancellation propagation through repositories, use cases, ViewModels, workers, NDT7, latency measurement, cleanup scans, export, and foreground-service loops. Find catches that consume CancellationException or continue after owner teardown. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q125 (source RC-126)

```text
[Priority: P0 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review dispatcher use for Room, DataStore, files, MediaStore, sockets, Billing callbacks, package queries, JSON parsing, scoring, and insight rules. Verify blocking work never runs on Main and context switches are not excessive. Search explicitly for runBlocking outside tests and explain any production occurrence as a potential ANR or deadlock risk unless repository evidence proves it is unreachable or safe. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q126 (source RC-127)

```text
[Priority: P1 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Audit callbackFlow and channelFlow wrappers around Android listeners. Verify awaitClose unregisters exactly the registered callback, duplicate registration is impossible, send failures are handled, and callbacks cannot race after closure. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q127 (source RC-128)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Inspect lifecycle of thermal listeners, network callbacks, battery broadcasts, screen-state receivers, BillingClient, package/usage observers, and service collectors. Check leaks, double unregister, registration exceptions, and missing initial state. For Android 14/API 34+ dynamic receiver registrations, verify RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED is supplied where required. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q128 (source RC-129)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review shared mutable state in managers and repositories. Identify non-atomic read-modify-write sequences, mutable collections exposed through StateFlow, unsynchronized caches, and races between foreground observers and workers. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q129 (source RC-130)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Mutex, synchronized blocks, atomic types, and Room transactions. Verify lock scope is minimal, lock ordering cannot deadlock, suspending calls are not made under unsuitable JVM locks, and unrelated keys are not serialized unnecessarily. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q130 (source RC-131)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review SavedStateHandle-backed flows and mutable state. Verify defaults are stable, writes are idempotent, route argument updates do not loop, and simultaneous UI events cannot overwrite a newer selection. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q131 (source RC-132)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit one-shot UI events such as navigation, permission prompts, dialogs, snackbars, IntentSender launches, billing launches, and share intents. Check replay after rotation, loss without a collector, and duplicate handling. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q132 (source RC-133)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Inspect timeout implementation for latency, NDT7 phases, Billing connection, file scans, export, and workers. Verify nested timeout behavior matches the documented total bound and resources close promptly on timeout. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q133 (source RC-134)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit retry and backoff loops. Verify retryable errors are narrowly classified, delays are cancellable, counters reset, exponential backoff cannot overflow, and permanent failures surface instead of looping. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q134 (source RC-135)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review CoroutineWorker implementations for structured concurrency. Verify child jobs complete or cancel before Result is returned, best-effort branches cannot outlive the worker, and supervisor semantics do not hide core failure. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q135 (source RC-136)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit RealTimeMonitorService's five-second loop. Verify one job exists, start commands are idempotent, disabling cancels immediately, process recreation cannot duplicate collectors, and notification updates serialize safely. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q136 (source RC-137)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Inspect app-start scopes and any custom process scope. Verify SupervisorJob, dispatcher, exception handling, and ownership are explicit; flag GlobalScope or scopes attached to objects that are never cancelled. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q137 (source RC-138)

```text
[Priority: P1 | Execution: EXTERNAL_CURRENT_DATA | Mode: AUDIT]

In the runcheck Android repository, Audit exception handling in flows. Check catch and retry placement, whether upstream failure terminates state permanently, whether errors become ambiguous empty data, and whether recoverable failures remain observable. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q138 (source RC-139)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review cold versus hot flow semantics for hardware and database sources. Verify expensive sources are shared intentionally, collectors receive correct initial snapshots, and one-shot requests do not silently start endless polling. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q139 (source RC-140)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Perform race-focused review of Pro state, trial expiry, preference changes, monitoring rescheduling, charger tracking, insight replacement, retention cleanup, and widget refresh occurring concurrently. Identify stale UI, duplicate work, or lost writes. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q140 (source RC-502)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Search for runBlocking outside unit or instrumented tests. For every production occurrence, inspect the call site, dispatcher, lock ownership, and lifecycle. Determine whether it can block the main thread, a Worker thread, a receiver, a Billing callback, or startup, and whether a suspending alternative is safer. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q141 (source RC-141)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit RuncheckDatabase version 10 against every @Entity and exported schema JSON. Verify table names, columns, affinities, defaults, primary keys, indexes, foreign keys, and auto-migration metadata are identical. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q142 (source RC-142)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Inspect every DAO query for correctness under nulls, empty lists, timestamp boundaries, ordering ties, LIMIT/OFFSET, aggregation, and deletion, especially charts, latest snapshots, retention, insights, charger comparison, and paging. Include every @Insert, @Upsert, and OnConflictStrategy choice; distinguish append-only history rows from replacement/update snapshots so a wrong REPLACE, IGNORE, or upsert cannot silently lose data. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q143 (source RC-143)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit indexes for actual query patterns. Verify battery, network, thermal, storage, charging session, app usage, speed-test, device, event, and insight queries have useful indexes without redundant write-heavy indexes. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q144 (source RC-144)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review migration registration from 1→2 through 9→10. Verify a continuous path exists from every historical version, each step is registered once, and no build variant or test database omits a migration. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q145 (source RC-145)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit migration 1→2 adding throttling_events. Verify created columns, types, defaults, keys, indexes, and compatibility with the current ThrottlingEventEntity and historical rows. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q146 (source RC-146)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit migration 2→3 adding charger_profiles and charging_sessions. Verify primary keys, indexes, nullable end times, defaults, profile references, and later entity evolution preserve existing data. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q147 (source RC-147)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit migration 3→4 adding app_battery_usage. Verify initial schema, timestamp representation, package constraints, numeric units, and compatibility with later composite-index migrations. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q148 (source RC-148)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit migration 4→5 recreating network_readings with nullable signal_dbm. Verify every column copies exactly, keys/indexes survive, nullability does not coerce values, and large-table migration is safe. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q149 (source RC-149)

```text
[Priority: P0 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit migration 5→6 adding speed_test_results. Verify throughput, ping, jitter, server, signal, connection fields, units, defaults, nullability, indexes, and current history queries. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q150 (source RC-150)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit migration 6→7 adding battery status/timestamp and charging-session end-time indexes. Verify old-row defaults are semantically valid and indexes match current predicates. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q151 (source RC-151)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit migrations 7→8 and 8→9 for app-usage indexes. Verify composite index column order supports paging and cleanup and the removed package-only index is truly redundant. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q152 (source RC-152)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit migration 9→10 adding insights. Verify rule ID, dedupe key, priority, confidence, seen/dismissed state, expiry, timestamps, unique/index constraints, and replacement queries match InsightEntity semantics. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q153 (source RC-153)

```text
[Priority: P0 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review Room migration tests and schema assets. Verify tests start from every supported historical schema, insert edge-case data, run complete paths, use Room validation, and assert data meaning rather than table existence only. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q154 (source RC-154)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit destructive migration behavior. Determine exactly when fallback can occur, whether production enables it, how destructive_migration_occurred is recorded, and whether logging/event insertion is reliable after destruction. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q155 (source RC-155)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review transaction boundaries for periodic monitoring. Verify battery, network, thermal, storage, charger, alert state, heartbeat, and maintenance operations cannot leave contradictory partial database state after failure. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q156 (source RC-156)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit paging DAO methods for stable order and invalidation. Verify deterministic tie-breakers, no duplicates or skips during concurrent updates/deletes, efficient count/group queries, and correct page-size behavior. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q157 (source RC-157)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review timestamp units and timezone assumptions in every entity and DAO. Verify all producers and consumers agree on milliseconds versus seconds and wall time versus elapsed time for retention, charts, insights, and recency. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q158 (source RC-158)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit nullable columns and sentinel values. Identify fields where 0, empty string, -1, null, or UNKNOWN overlap and can corrupt scoring, confidence, chart gaps, aggregation, or migrations. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q159 (source RC-159)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review clear-all-data, clear-speed-tests, retention cleanup, insight replacement, history trimming, and event cleanup queries. Verify each deletes exactly intended rows and preserves active session/configuration invariants. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q160 (source RC-160)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit database concurrency and performance settings: executors, WAL or journal mode, long scans, main-thread safeguards, widgets/workers access, transaction duration, cursor size, and need for batched cleanup. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q161 (source RC-501)

```text
[Priority: P0 | Execution: REPO_ONLY, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit every Room @Insert, @Upsert, and OnConflictStrategy choice across all DAOs. Distinguish append-only telemetry/history rows from single-row snapshots, device/profile records, cache rows, and replace/update operations. Identify any conflict strategy that could silently drop history, erase related rows, fail to update a current snapshot, or create duplicate logical records. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q162 (source RC-161)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit every stored preference default against PROJECT.md: monitoring interval, notification toggles, retention, thresholds, temperature unit, live lines, info cards, trial state, and Pro cache. Identify storage/UI default mismatches. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q163 (source RC-162)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Inventory keys in settings, trial_state, monitoring_status, and monitoring_alert_state. Check naming collisions, type changes, obsolete keys, missing migrations, and stale behavior after a key is renamed or removed. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q164 (source RC-163)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review DataStore creation and corruption handling. Verify singleton instance per file, application context, IOException handling, corruption policy, serializer safety if used, and protection from duplicate or multi-process access. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q165 (source RC-164)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit preference read flows for exceptions and initial values. Verify I/O failure does not permanently terminate UI or silently reset notification, monitoring, trial, or Pro-related behavior without diagnostics. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q166 (source RC-165)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review every preference update for atomicity. Find read-then-write races, related fields that require one transaction, and settings screens that can overwrite worker/service changes with stale state. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q167 (source RC-166)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit monitoring interval changes. Verify only 15, 30, or 60 minutes persist, unique work is rescheduled correctly, unchanged choices do not churn WorkManager, and interruption between write and reschedule self-heals. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q168 (source RC-167)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit live-notification master and metric toggles. Verify persistence occurs before service actions, failed starts surface correctly, disabling stops immediately, and metric changes update a running notification without duplicate services. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q169 (source RC-168)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review notification master and child-toggle semantics. Verify master-off suppresses all alerts while preserving child choices, reenabling restores them, and stale worker state cannot post immediately against user intent. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q170 (source RC-169)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit validation of low-battery, temperature, and low-storage thresholds. Verify ranges, units, steps, Fahrenheit display conversion, corrupted values, reset defaults, and exact equality behavior. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q171 (source RC-170)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review temperature-unit preference use. Verify storage/calculation remains Celsius, every UI/export/notification path converts once, and changing unit updates live values, thresholds, charts, tooltips, and dialogs consistently. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q172 (source RC-171)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit info-card dismissal and reset. Verify card IDs are stable, reset clears exactly educational dismissals, removed cards do not accumulate indefinitely, and concurrent dismiss/reset is deterministic. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q173 (source RC-172)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit data-retention preference. Verify allowed values map to correct cutoffs, changes affect cleanup as intended, boundary timestamps are handled consistently, and unknown/corrupt values cannot delete too much history. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q174 (source RC-173)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review selected-charger persistence. Verify deleted or renamed profiles cannot leave invalid selection, defaults are deterministic, and selection stays synchronized with comparison and active-session views. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q175 (source RC-174)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit app-usage collection timestamp. Verify its meaning, update point, retry behavior, clock rollback handling, and whether a failed or partial collection can suppress the next legitimate run. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q176 (source RC-175)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit monitoring heartbeat persistence and freshness. Verify it is written only after the documented success boundary, uses consistent time units, survives restart, and handles interval changes, future timestamps, and corruption. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q177 (source RC-176)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review monitoring_alert_state for prior snapshots and charge-complete debounce. Verify atomic serialization, reboot behavior, threshold changes, stale entries, and no state can suppress alerts forever or repeat them. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q178 (source RC-177)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit SharedPreferences pro_status_cache. Verify cold-start reads, atomic writes, stale purchased/free states, schema/version evolution, process consistency, and ordering against authoritative Billing updates. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q179 (source RC-178)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review clear-all-data behavior across Room, DataStores, SharedPreferences, cache exports, widget state, scheduled work, notifications, and active service. Verify the UI promise precisely matches what is reset and preserved. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q180 (source RC-179)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit trial_state against wall-clock rollback and forward jumps. Verify start, last-known timestamp, welcome/day-5 flags, upgrade-card pacing, and ordinary timezone/manual clock changes cannot extend or regress trial unexpectedly. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q181 (source RC-180)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Inspect process-restart consistency. Verify ViewModels, services, workers, widgets, and startup derive state from persisted flows rather than stale in-memory mirrors and cannot briefly apply unsafe defaults. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q182 (source RC-181)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit BatteryRepository and battery-broadcast parsing. Verify level/scale math, status, plugged type, health, voltage, temperature, technology, present flag, and missing/unknown extras across API 26-37 without divide-by-zero or stale data. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q183 (source RC-182)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review BATTERY_PROPERTY_CURRENT_NOW reads and normalization. Verify raw units, MICROAMP_THRESHOLD=25,000 behavior, absolute-value choices, unavailable sentinels, overflow, and devices already reporting milliamps near the threshold. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q184 (source RC-183)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit current sign alignment with charging state. Verify charging becomes positive and discharging negative without corrupting devices with inverted drivers, FULL/NOT_CHARGING/UNKNOWN states, or readings near zero. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q185 (source RC-184)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review DeviceCapabilityManager.validateCurrentNow(): three samples spaced 300 ms apart. Verify cancellation, timing tolerance, zero/changing/plausible criteria, noisy readings, identical valid readings, and mapping to stored confidence. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q186 (source RC-185)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit the 0..10000 mA plausibility range. Check normalization order, negative raw values, inclusive boundaries, impossible spikes, and whether later runtime paths apply the same validation or clearly explain differences. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q187 (source RC-186)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review current unit and sign convention persistence in DeviceEntity or profile JSON. Verify invalidation after OS update, device change, restored app data, capability re-detection, and backward-compatible Gson evolution. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q188 (source RC-187)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Audit vendor-specific Samsung and OnePlus battery sources, including API 34+ variants. Verify manufacturer matching, property/reflection access, permissions, fallback order, units, exceptions, and no vendor path runs on unrelated devices. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q189 (source RC-188)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit cycle-count detection and display. Verify API guards, unavailable sentinels, vendor fallback consistency, capability flags, and no inferred value is presented as a measured cycle count. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q190 (source RC-189)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review battery health percentage and mAh remaining estimation. Verify source reliability, design versus full-charge capacity, precision, aging, unavailable data, charging state, confidence, and user-facing wording. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q191 (source RC-190)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit voltage and temperature conversion. Verify millivolts-to-volts and tenths-Celsius conversion happens exactly once, preserves precision, rejects impossible values, and is consistent in scoring, alerts, export, and insights. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q192 (source RC-191)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review remaining charge/discharge time calculations. Verify rate window, minimum samples, sign, zero current, charging taper, full status, screen state, stale samples, extreme estimates, units, and Pro gate. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q193 (source RC-192)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit in-memory current statistics that reset on battery status change. Verify transition definition, process/configuration recreation, thread safety, unavailable samples, and min/max/average behavior. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q194 (source RC-193)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review charger session graph data construction. Verify timestamps, gaps, duplicate samples, session boundaries, status transitions, unit conversion, downsampling, and foreground/background samples do not reorder or double count. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q195 (source RC-194)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit ChargerSessionTracker calls from Home live observation and HealthMonitorWorker. Verify idempotency, locking, dedupe, transition detection, concurrent updates, process death, and no duplicate or permanently open sessions. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q196 (source RC-195)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review screen-on versus screen-off drain analysis. Verify screen-state source, attribution across transitions, charging exclusion, sample gaps, elapsed versus wall time, minimum duration, and rate/percentage math. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q197 (source RC-196)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit sleep analysis while discharging. Verify sleep-window inference, screen-off assumptions, charging interruptions, day/timezone boundaries, missing samples, reboot, and wording that does not overstate certainty. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q198 (source RC-197)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review battery-history persistence and queries. Verify sample cadence, retention, status/timestamp columns, chart period boundaries, latest-reading selection, and no duplicate collection from live and worker paths. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q199 (source RC-198)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit mappings for CHARGING, DISCHARGING, FULL, NOT_CHARGING, UNKNOWN, and plugged types. Verify UI labels, icons, sign rules, remaining time, power calculation, and session tracking handle each state. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q200 (source RC-199)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review MeasuredValue<Int> use for current. Verify unavailable or low-confidence data never enters calculations as a real zero, confidence survives transformations, and badges reflect the actual selected source after fallback. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q201 (source RC-200)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit tests for OEM unit/sign quirks, unavailable properties, rapid plug/unplug, full-but-plugged, scale not equal to 100, impossible temperature/voltage, restart, and concurrent session updates. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q202 (source RC-201)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit ConnectivityManager integration for the current default network. Verify initial snapshot, callback registration, capability/link-property updates, lost/unavailable events, multiple transports, suspended networks, and cleanup across process and collector lifecycles. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q203 (source RC-202)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review validated-internet detection. Verify NET_CAPABILITY_VALIDATED and INTERNET semantics, captive portals, partial connectivity, local-only Wi-Fi, VPN underlying networks, and transient validation changes for UI and speed-test preflight. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q204 (source RC-203)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit active connection identity used by Network Detail and Speed Test. Define which fields identify a network, verify stability across callback object changes, and distinguish benign updates from true handoffs. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q205 (source RC-204)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review Wi-Fi SSID and BSSID retrieval across API 26-37. Verify location permission, location-services state, unknown SSID redaction, quote cleanup, restricted APIs, privacy, and permission-card guidance. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q206 (source RC-205)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit Wi-Fi standard, link speed, frequency, channel, RSSI, and signal-level mapping. Verify API guards, 2.4/5/6 GHz interpretation, Wi-Fi 6/6E/7 labels, unavailable values, and multi-link data if exposed. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q207 (source RC-206)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review cellular subtype detection and labels. Verify legacy types, LTE/LTE-CA, NR limitations, unknown values, carrier restrictions, and icon/label consistency in live state and stored speed results. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q208 (source RC-207)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit VPN detection. Verify VPN transport, underlying networks, always-on VPN, work profile, and simultaneous Wi-Fi/cellular transports do not produce misleading connection labels or bypass identity checks. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q209 (source RC-208)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review IP, DNS, route, proxy, and MTU extraction from LinkProperties. Verify IPv4/IPv6 formatting, scope IDs, multiple DNS servers, null/zero MTU, privacy, and updates when properties change. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q210 (source RC-209)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit signal normalization and quality buckets for Wi-Fi and cellular. Verify dBm thresholds, unavailable sentinels, API differences, cellular signal classes, and consistency with Home status, score, charts, and results. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q211 (source RC-210)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review BuildConfig.LATENCY_HOST and LATENCY_PORT. Verify defaults, environment overrides, validation, DNS and IPv6 handling, raw TCP expectations, and protection from malformed or unintended endpoint values. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q212 (source RC-211)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit LatencyMeasurer's five TCP-connect samples, 1.5-second per-sample timeout, and six-second total timeout. Verify timeout nesting, DNS inclusion, socket closure, cancellation, failed-sample policy, and aggregation. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q213 (source RC-212)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review RFC 3550-style jitter computation with at least four samples. Verify ordering, units, initialization, failed samples, precision, and whether UI copy accurately describes TCP-connect variation. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q214 (source RC-213)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit latency execution and resource use. Verify DNS/socket work never runs on Main, concurrency is bounded, navigation cancels unneeded work, and slow resolution cannot escape the total timeout. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q215 (source RC-214)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review GetMeasuredNetworkStateUseCase combining network state with 30-second measurements. Verify immediate first measure, scheduling drift, no measurement while disconnected, cancellation on change, and no duplicate timers per collector. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q216 (source RC-215)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit disconnect behavior. Verify latency becomes null immediately, stale signal/details clear appropriately, fake zeroes are not persisted, and reconnection triggers a fresh measure without waiting 30 seconds. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q217 (source RC-216)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit connection type or identity changes. Verify one immediate remeasure, old results cannot overwrite new state, and rapid Wi-Fi/cellular/VPN transitions cannot create a mixed snapshot. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q218 (source RC-217)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review network-history persistence. Verify signal and latency timestamps align, nullable signal_dbm remains null, only meaningful samples are stored, retention works, and charts distinguish gaps from zero. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q219 (source RC-218)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit permission-denied and capability-limited states. Verify non-sensitive connectivity still displays, permission prompts do not loop, and unavailable SSID is clearly different from disconnected network. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q220 (source RC-219)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review network-related privacy. Identify logging, export, database storage, or sharing of SSID, BSSID, IP, DNS, server, carrier, and signal data and verify necessity and minimization. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q221 (source RC-220)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit tests for captive portal, VPN, IPv6-only, dual transport, unvalidated internet, handoff, denied location, unknown SSID, missing signal, timeout, partial sample failure, and cancellation. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q222 (source RC-221)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit SpeedTestService's direct use of net.measurementlab.ndt7.android.NdtTest. Verify lifecycle, callbacks or flows, threading, resource closure, supported client API, and absence of obsolete NDT implementations. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q223 (source RC-222)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Prove the speed test lets NDT7 auto-select its server. Search for hardcoded server URLs, locate overrides, test fixtures leaking into production, or manual endpoint selection contradicting requirements. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q224 (source RC-223)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review preflight validation. Verify a validated active default network is required, unsupported states produce actionable errors, and preflight cannot become stale before the NDT session locks the network. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q225 (source RC-224)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit cellular confirmation. Verify CellularConfirmationRequired appears only for active cellular, one confirmation starts one test, cancellation returns to Ready, and Wi-Fi, VPN, and mixed transports are classified intentionally. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q226 (source RC-225)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review default-network locking at test start. Verify captured identity is sufficient, callbacks monitor loss/change through all phases, and results are rejected rather than mixed after a material handoff. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q227 (source RC-226)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit benign versus material network changes during a test. Determine whether IP renewal, capability update, VPN metadata change, or callback-object replacement falsely fails stable connectivity and whether true changes always fail. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q228 (source RC-227)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review ping-phase implementation. Verify its measurement source, sample count, timeouts, DNS inclusion, relationship to NDT7 server selection, and whether the UI accurately describes it. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q229 (source RC-228)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit roughly ten-second download and upload phases. Verify timers, progress callbacks, early completion, stalls, cancellation, app backgrounding, and device sleep do not extend or misreport duration. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q230 (source RC-229)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review state and progress mapping across Ready, CellularConfirmationRequired, Ping, Download, Upload, Completed, and Failed. Verify monotonicity, rotation, process recreation, and stale callbacks from previous runs. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q231 (source RC-230)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit throughput conversion to Mbps. Verify bits versus bytes, decimal versus binary units, averaging/window logic, NaN/infinity/negative values, rounding, and consistency between stored and displayed values. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q232 (source RC-231)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review jitter calculation in speed-test results. Verify sample source, minimum count, formula, units, missing-value handling, and distinction from periodic TCP latency jitter. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q233 (source RC-232)

```text
[Priority: P0 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit server metadata extraction from ClientResponse.origin and ClientResponse.test. Verify null or malformed values, privacy, location parsing, fallback labels, and no reliance on undocumented response structure. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q234 (source RC-233)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review captured connection type, subtype, and optional signal at completion. Verify values correspond to the locked start network, not later state, and unavailable fields remain null instead of defaults. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q235 (source RC-234)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit result persistence. Verify a completed result stores once, partial runs never persist as success, database failure surfaces, and duplicate or reordered callbacks cannot create duplicate rows. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q236 (source RC-235)

```text
[Priority: P0 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review FinalizeSpeedTestUseCase history trimming. Verify free and Pro limits, deterministic oldest-row deletion, pending Pro-state resolution, transaction boundaries, and retention of the just-completed result. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q237 (source RC-236)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit free and Pro history access. Verify free users cannot retrieve hidden excess rows through paging, export, direct repository calls, or restored state, and downgrade after trial does not corrupt history. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q238 (source RC-237)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review cancellation for user cancel/back, ViewModel clearing, app background, activity destruction, network loss, timeout, and exceptions. Verify NDT jobs and sockets stop promptly and terminal state is coherent. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q239 (source RC-238)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit concurrent-start protection. Verify rapid taps, duplicate collectors, replayed UI events, and multiple ViewModel instances cannot run overlapping tests or interleave callbacks and results. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q240 (source RC-239)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review error classification and copy. Map NDT errors, discovery failure, timeout, handoff, cancellation, invalid connectivity, and persistence failure to stable domain errors without exposing internals or promising false remedies. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q241 (source RC-240)

```text
[Priority: P0 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit tests with fake NDT callbacks for cellular confirmation, handoff, cancellation between phases, zero/NaN throughput, malformed metadata, duplicate completion, database failure, and Pro trimming. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q242 (source RC-241)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit thermal acquisition from PowerManager and battery broadcasts. Verify API guards, initial values, listener registration, unsupported devices, security exceptions, and absence of hidden sysfs thermal reads. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q243 (source RC-242)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review currentThermalStatus mapping on API 29+. Verify NONE, LIGHT, MODERATE, SEVERE, CRITICAL, EMERGENCY, SHUTDOWN, and unknown future values map safely to domain and UI. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q244 (source RC-243)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit OnThermalStatusChangedListener lifecycle. Verify one listener per active source, correct executor, immediate initial status, unregister on cancellation, and no callback after teardown. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q245 (source RC-244)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review getThermalHeadroom(10) on API 30+. Verify forecast argument, NaN/error handling, range assumptions, polling cadence, and UI wording that explains forecast headroom rather than temperature. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q246 (source RC-245)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit the three-second thermal-headroom loop. Verify it stops when unobserved, does not overlap calls, handles exceptions, and cannot keep the process awake or consume unnecessary battery. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q247 (source RC-246)

```text
[Priority: P0 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Prove CPU temperature intentionally remains null. Search for dead sysfs/proc/vendor readers, fallback heuristics, UI placeholders, scoring, tests, and export paths that still treat CPU temperature as measured. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q248 (source RC-247)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review battery temperature as the thermal temperature source. Verify broadcast conversion, stale data, impossible values, cadence, and clear wording that it is battery - not CPU or ambient - temperature. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q249 (source RC-248)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Celsius/Fahrenheit conversion across hero, metrics, history, alerts, settings thresholds, notifications, export, and insight text. Verify persisted/calculated values remain Celsius and convert exactly once. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q250 (source RC-249)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review thermal status and temperature mappings to Healthy, Fair, Poor, and Critical. Verify documented thresholds, theme colors, score penalties, alerts, and educational copy do not contradict each other. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q251 (source RC-250)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit session min/max tracking while Thermal Detail is active. Verify initialization, reset, unavailable values, configuration recreation, sample gaps, thread safety, and no carryover from a previous visit. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q252 (source RC-251)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review throttling-event detection and persistence. Verify transition criteria, duplicate suppression, start/end semantics, duration, severity ordering, timestamps, reboot, and oscillation handling. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q253 (source RC-252)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit the Pro-only throttling log gate. Verify free users cannot query, export, or navigate to hidden events while background collection behavior remains intentional and privacy-consistent. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q254 (source RC-253)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review thermal-history queries and charts. Verify gaps, status/temperature alignment, periods, downsampling, retention, boundary timestamps, and no null CPU values are plotted as zero. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q255 (source RC-254)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit thermal collection in HealthMonitorWorker. Verify unsupported APIs do not fail core monitoring, snapshot writes are coherent, and listener APIs are not misused from a short-lived worker. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q256 (source RC-255)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review unavailable and low-confidence UI states. Verify missing headroom, status, CPU temperature, or battery temperature produce distinct non-alarming labels rather than zero or healthy defaults. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q257 (source RC-256)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit thermal impact in HealthScoreCalculator. Verify missing CPU handling, monotonic status penalties, battery-temperature units, and unavailable data cannot improve the score accidentally. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q258 (source RC-257)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review thermal insight inputs. Verify events/readings share units and time bases, minimum duration/sample requirements, sparse worker cadence, and sleep gaps do not create false recurring patterns. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q259 (source RC-258)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit API and vendor anomalies: headroom NaN, listener never firing, status jumping backward, missing battery temperature, or future status values. Verify graceful degradation and useful diagnostics. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q260 (source RC-259)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review thermal animations and accessibility. Verify thermometer and heat-strip visuals do not encode status only by color, stop off-screen, and expose values and state to screen readers. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q261 (source RC-260)

```text
[Priority: P0 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit tests for every status, API 28 fallback, API 29 listener, API 30 headroom, NaN/unavailable data, cancellation, oscillation, threshold boundaries, Fahrenheit, and Pro log gating. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q262 (source RC-261)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit total, used, and free storage calculations. Verify filesystem selection, StatFs or StorageStats semantics, reserved space, overflow, multiple volumes, rounding, zero capacity, and consistency with score and charts. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q263 (source RC-262)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review media breakdown for images, video, audio, documents, downloads, APKs, and other. Verify MediaStore queries, MIME handling, duplicate classification, pending or trashed items, inaccessible rows, and API-level columns. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q264 (source RC-263)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit media permission handling on API 26-37. Verify READ_EXTERNAL_STORAGE maxSdk 32, READ_MEDIA_* on 33+, partial media access, denial rationale, and features that remain usable without broad access. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q265 (source RC-264)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review SD-card detection and metrics. Verify removable versus adoptable storage, multiple volumes, unmounted/ejected states, permissions, duplicate primary reporting, and unknown-capacity labels. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q266 (source RC-265)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit storage quality zones 0-70 healthy, 70-90 fair, and 90-100 critical. Verify inclusive boundaries and reconcile them with status-color thresholds, health score, and alert defaults. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q267 (source RC-266)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review cleanup/{type} route parsing and CleanupType mapping for LARGE_FILES, OLD_DOWNLOADS, and APK_FILES. Verify malformed or case-altered args fail safely and Trash cannot be reached through this route. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q268 (source RC-267)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit large-file filters at 10, 50, 100, and 500 MB. Verify byte conversion, strict versus inclusive comparison, filter persistence, query predicates, and exact-boundary files. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q269 (source RC-268)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit old-download filters at 30, 60, 90 days, and one year. Verify calendar versus fixed duration, timezone, chosen date column, future/zero timestamps, persistence, and boundary inclusion. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q270 (source RC-269)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit APK cleanup scanning. Verify MIME/extension detection, split APKs, renamed files, app-owned files, API 30+ restriction, default preselection, and no installed package is mistaken for a file. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q271 (source RC-270)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review CleanupViewModel's Pro gate before scanning. Verify non-Pro users get locked state without starting MediaStore, paging, filesystem, count, thumbnail, or delete work through any direct/restored route. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q272 (source RC-271)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit grouped paging with page size 40. Verify stable group/item keys, deterministic ordering, no duplicates/skips as files change, efficient counts, group-specific loading/error states, and memory use. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q273 (source RC-272)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review expand/collapse and selection state. Verify selection survives paging refresh and rotation as intended, select-all handles unloaded pages, removed files are dropped, and counts/bytes remain accurate. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q274 (source RC-273)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit API 30+ deletion through MediaStore delete requests and IntentSender. Verify selected URI set, request size/batching, ownership, user cancel, partial deletion, result handling, and post-return reconciliation. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q275 (source RC-274)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Android 10-and-below StorageCleanupHelper.deleteLegacy. Verify WRITE_EXTERNAL_STORAGE maxSdk, scoped-storage differences, path/URI validation, SecurityException, symlink/path traversal, and partial failure reporting. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q276 (source RC-275)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review version restrictions for Old Downloads and APK cleanup on API 29 and below. Verify UI, navigation, ViewModel, use case, and repository enforce the same rule with an explanation rather than empty results. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q277 (source RC-276)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit scans against concurrent file changes. Verify deleted, moved, renamed, revoked, ejected, or newly indexed files cannot corrupt selection, crash paging, or report false success. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q278 (source RC-277)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review privacy of file metadata. Verify names, paths, URIs, MIME types, sizes, and thumbnails are not logged or exported unintentionally and full paths are not exposed unnecessarily. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q279 (source RC-278)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Trash as a separate API 30+ MediaStore delete-request path. Verify trashed-item counting, no cleanup route, confirmation, partial failure, empty state, and behavior when no eligible media exists. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q280 (source RC-279)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review cleanup state machine: scanning, empty, results, deleting, system confirmation, success, partial success, cancellation, and fatal error. Verify recoverability without duplicate deletion intents. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q281 (source RC-280)

```text
[Priority: P0 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit tests for permission matrices, API 29/30/33 behavior, exact filter boundaries, paging invalidation, unloaded-page selection, delete cancel/partial success, stale URIs, SD removal, and Pro gating. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q282 (source RC-281)

```text
[Priority: P1 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Audit usage-access detection for PACKAGE_USAGE_STATS. Verify AppOpsManager and UsageStatsManager behavior across APIs, MODE_DEFAULT, OEM differences, and that manifest declaration is never treated as granted access. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q283 (source RC-282)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review permission education and the system settings intent. Verify the correct screen opens, missing handlers are safe, returning triggers one refresh, and denial does not create repeated automatic navigation. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q284 (source RC-283)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit usage snapshot collection intervals and APIs. Verify start/end timestamps, timezone/day boundaries, device reboot, UsageEvents versus aggregate stats, and foreground-time derivation on API 26-37. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q285 (source RC-284)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review foreground-time accuracy and naming. Verify split screen, picture-in-picture, services, system apps, launcher time, overlapping activities, and UsageStats limitations are not presented as precise battery consumption. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q286 (source RC-285)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit package filtering. Verify runcheck itself, system components, removed apps, zero-usage apps, work-profile apps, instant apps, and non-launcher packages are included or excluded intentionally. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q287 (source RC-286)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review app label and icon loading. Verify PackageManager failures, removed/disabled packages, adaptive icons, memory caching, placeholders, Main-thread work, and bitmap retention in paging lists. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q288 (source RC-287)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit AppBatteryUsageEntity semantics. Verify package, timestamp, foreground duration, estimated usage, units, nullability, key design, and relationship to collection snapshots are unambiguous and migration-safe. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q289 (source RC-288)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review periodic app-usage persistence. Verify a snapshot is transactional, duplicate collection for a period is handled, failed collection does not advance the marker, and retention removes complete windows correctly. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q290 (source RC-289)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit paging queries for per-app usage. Verify deterministic order, stable keys, aggregation window, package/timestamp index use, invalidation after refresh, and no duplicate rows as snapshots arrive. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q291 (source RC-290)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review total foreground-time summary. Verify it sums the same filter and window as the list, avoids double counting, handles durations beyond 24 hours, and clearly labels the period. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q292 (source RC-291)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit HealthMaintenanceWorker's usage collection. Verify missing permission is expected and non-retryable, package work is bounded, cancellation leaves no partial snapshot, and battery constraint is honored. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q293 (source RC-292)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review the DataStore collection timestamp. Verify it prevents redundant work without skipping required intervals, handles reboot/clock rollback, and updates only after database commit. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q294 (source RC-293)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Pro gating for app_usage. Verify free direct navigation redirects before permission or data queries and widgets, insights, notification routes, or learn links cannot expose gated detail indirectly. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q295 (source RC-294)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review behavior when trial expires or purchase state changes while App Usage is visible. Verify lock timing, back stack, paging cancellation, preserved permission state, and absence of crashes or data leaks. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q296 (source RC-295)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit refresh-on-return from system settings. Verify lifecycle event, debounce, cancellation, distinction from configuration change, and no repeated expensive refresh on every recomposition. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q297 (source RC-296)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review unavailable package metadata. Verify rows remain stable when an app is uninstalled between collection and display, with package-name fallback and no PackageManager or paging loop. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q298 (source RC-297)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Audit data minimization and privacy. Verify per-app history remains on-device except explicit export if intended, package names are not release-logged, and retention/clear-all fully removes snapshots. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q299 (source RC-298)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review any estimated battery-impact calculation. Verify it is labeled estimated, uses defensible inputs, handles missing battery data, and does not imply Android-provided per-app energy when unavailable. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q300 (source RC-299)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit AppBatteryImpactRule and HeavyAppUsageRule inputs. Verify period alignment, minimum samples, package stability, exclusions, confidence, and wording do not mistake correlation or ordinary use for harm. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q301 (source RC-300)

```text
[Priority: P0 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit tests for access modes, settings return, empty stats, duplicate snapshots, reboot/time change, uninstalled apps, paging order, totals, Pro expiry, worker cancellation, and sensitive logging. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q302 (source RC-301)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit HealthScoreCalculator's 40/25/25/10 battery, network, thermal, and storage weights. Verify arithmetic, rounding order, normalization, missing-subsystem behavior, and final clamping to 0..100. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q303 (source RC-302)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review status thresholds 75-100 Healthy, 50-74 Fair, 25-49 Poor, and 0-24 Critical. Verify every boundary, UI mapping, color, insight/notification wording, and test uses identical inclusive ranges. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q304 (source RC-303)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit battery-score penalties for health state, temperature, voltage, and optional health percentage. Verify units, monotonicity, overlap, unavailable data, and no malformed reading drives a score below zero or above 100. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q305 (source RC-304)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit network score when disconnected. Prove it is exactly zero regardless of cached signal, latency, or recent speed results and that reconnect does not retain zero longer than intended. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q306 (source RC-305)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review network scoring without a recent speed test. Verify signal and latency weights, missing input semantics, units, VPN/cellular differences, caps, and whether no latency is neutral, penalized, or unavailable by design. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q307 (source RC-306)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit the recent-speed-test cutoff of less than one hour. Verify strict behavior at exactly one hour, clock source, future timestamps, timezone independence, and consistency between queries and calculator logic. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q308 (source RC-307)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review recent-test network weights: signal 40%, ping 30%, download 20%, jitter 10%. Verify normalization, missing fields, upload exclusion, units, caps, and that fast download cannot hide unusable latency or signal. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q309 (source RC-308)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit thermal-score penalties for battery temperature, missing or known CPU temperature, and Android thermal status. Verify missing CPU behavior is intentional and not a permanent hidden bonus or penalty. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q310 (source RC-309)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit storage-score penalties and sharp high-utilization thresholds. Verify percentage calculation, boundaries, removable storage treatment, impossible values, and consistency with Storage Detail zones and alert defaults. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q311 (source RC-310)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review numeric safety across all score inputs. Check NaN, infinity, negative values, integer overflow, percentage above 100, extreme voltage/current, nulls, empty histories, and division by zero. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q312 (source RC-311)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit freshness of Home score inputs. Verify battery, network, thermal, and storage values come from a coherent time window, stale data is disclosed, and live values do not silently combine with days-old snapshots. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q313 (source RC-312)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review score explainability. Determine whether domain output preserves component scores and major penalties so UI, tests, support diagnostics, and insights can explain a result truthfully. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q314 (source RC-313)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit deterministic behavior. Verify score does not depend on collection order, locale, unordered sets/maps, floating-point platform differences, or current time without an injectable clock. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q315 (source RC-314)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review MeasuredValue<T> and Confidence semantics. Verify HIGH, LOW, and UNAVAILABLE are exhaustive, value/null combinations are coherent, and confidence survives mapping and fallback selection. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q316 (source RC-315)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit UI mapping HIGH→Accurate, LOW→Estimated, UNAVAILABLE→Unavailable. Verify labels are applied only to relevant values and Accurate does not overclaim reliability beyond implemented validation. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q317 (source RC-316)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review ConfidenceBadge colors, text, and contrast. Verify status tokens, accessibility without color, disabled/unknown handling, and consistent use across Battery, Device, Thermal, charts, and settings. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q318 (source RC-317)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit how unavailable values enter charts, statistics, export, notifications, score, and insights. Verify they become gaps or omissions rather than zeros and do not affect averages or trends. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q319 (source RC-318)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Compare score thresholds with Home quick-status thresholds and Material status colors for battery, temperature, storage, and signal. Identify contradictions where one measurement receives incompatible labels without explanation. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q320 (source RC-319)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review time-dependent score testability. Verify clocks are injectable for recency/staleness, exact boundaries are tested, and system clock rollback or future timestamps cannot improve score misleadingly. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q321 (source RC-320)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit score and confidence tests using table-driven boundary cases, unavailable inputs, extreme values, disconnected network, recent-test cutoff, missing CPU temperature, and displayed rounding. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q322 (source RC-321)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit InsightsModule and runtime discovery to prove all eleven documented InsightRule implementations are bound exactly once and no obsolete, test, or debug rule participates in release generation. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q323 (source RC-322)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review InsightEngine's 0.6 confidence filter. Verify confidence scale, equality at 0.6, NaN/out-of-range values, calibration, and whether filtered candidates correctly remove or preserve previous rows. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q324 (source RC-323)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit replace-results-per-rule semantics. Verify one rule deletes or replaces only its own rows, handles zero candidates, runs transactionally, and cannot erase another rule after partial failure. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q325 (source RC-324)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review dedupe-key handling and preservation of seen/dismissed state. Verify key stability, collisions, changed message/priority, expired rows, multiple candidates, and upgrade compatibility. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q326 (source RC-325)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit expiry deletion before and after generation. Verify timestamp units, equality at expiry, transaction order, future timestamps, cancellation, and whether Home can briefly observe expired or duplicate rows. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q327 (source RC-326)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review priority and confidence ordering in persisted queries and Home ranking. Verify deterministic tie-breakers and intentional treatment of high-priority lower-confidence versus lower-priority higher-confidence items. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q328 (source RC-327)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit InsightHomeRankingPolicy. Prove it avoids duplicate target buckets before filling remaining slots, returns at most three, preserves useful priority, handles few buckets, and is deterministic for ties. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q329 (source RC-328)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review Home's markAllSeen behavior. Verify only currently displayed unseen rows are marked, newly arriving rows are not accidentally included, recomposition does not repeat writes, and dismissed rows remain excluded. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q330 (source RC-329)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit dismissal, expiry, regeneration, and reappearance. Verify a dismissed dedupe key stays suppressed as intended, materially new events can surface, and clear/reset behavior is explicit. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q331 (source RC-330)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review insight target navigation, including Pro-gated Charger Comparison and App Usage. Verify target validation, upgrade redirection, context preservation, unknown routes, and gate bypass resistance. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q332 (source RC-331)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit BatteryDegradationTrendRule. Verify history duration, health/capacity proxy, sample filtering, charging effects, trend method, minimum effect, confidence, dedupe, and non-overclaiming wording. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q333 (source RC-332)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit AppBatteryImpactRule. Verify usage and drain windows align, charging/screen/thermal confounders, minimum samples, package labels, confidence, and correlation wording rather than causal claims. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q334 (source RC-333)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit ChargerPerformanceRule. Verify complete sessions, profile identity, comparable battery ranges, temperature/screen-state controls, current/power units, outliers, minimum sessions, Pro target, and dedupe. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q335 (source RC-334)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit StoragePressureProjectionRule. Verify trend window, units, regression/projection horizon, cleanup outliers, full-storage cap, minimum slope, confidence, expiry, and uncertainty wording. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q336 (source RC-335)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit RecurringThermalThrottlingRule and ThermalPatternDetectionRule. Verify event windows, recurrence, duration/count minima, time patterns, sleep gaps, sparse monitoring, severity, and nonduplicate output. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q337 (source RC-336)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit HeavyAppUsageRule. Verify usage period, system exclusions, normalization, minimum duration/share, package changes, confidence, Pro target, and whether expected use is mislabeled harmful. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q338 (source RC-337)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit NetworkSignalPatternRule. Verify network identity/type separation, privacy implications, time grouping, dBm thresholds, missing signal, observation minimums, timezone changes, and cautious wording. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q339 (source RC-338)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit NetworkDrivenBatteryDrainRule. Verify sample alignment, charging/screen/app/thermal confounders, network transitions, minimum effect, method, confidence, and non-causal wording. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q340 (source RC-339)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit HeatAcceleratedBatteryWearRule and StoragePressureImpactRule. Verify timestamp joins, lag assumptions, thresholds, minimum data, confounders, directionality, confidence, dedupe, and scientific defensibility. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q341 (source RC-340)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit InsightGenerationWorker and rule tests for cancellation, per-rule failure isolation, SQL retry only, deterministic clocks, seeded histories, threshold boundaries, expiry, state preservation, and every current rule. For every InsightRule, require both at least one positive-result test and at least one insufficient-data or no-result test unless the repository already has a stronger equivalent. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q342 (source RC-509)

```text
[Priority: P1 | Execution: REPO_ONLY, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, For every InsightRule, verify there is at least one positive-result test and at least one insufficient-data or no-result test. Check threshold boundaries, confidence calculation, dedupe key stability, expiry, target route, Pro-gated target behavior, and cautious user-facing wording. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q343 (source RC-341)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit MonitorScheduler's health_monitor, health_maintenance, and insight_generation unique periodic works. Verify names, tags, policies, intervals, flex/backoff, constraints, and idempotent scheduling at startup. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q344 (source RC-342)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review mapping of 15, 30, or 60 minute preference to HealthMonitorWorker and HealthMaintenanceWorker. Verify WorkManager minimums, existing-work updates, unchanged settings, and restart persistence. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q345 (source RC-343)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit the six-hour InsightGenerationWorker schedule. Verify independence from monitoring interval, requiresBatteryNotLow, survival across boot/upgrades, and no cadence shift from repeated app startup. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q346 (source RC-344)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review HealthMonitorWorker's core collection transaction. Verify failure classification, partial persistence policy, charger update order, alert evaluation, heartbeat write, widget effects, and Result decisions. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q347 (source RC-345)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit the rule that HealthMonitorWorker retries only on core collection or maintenance failure. Verify exact retryable exceptions, cancellation rethrow, and best-effort notification/widget failures cannot force retry. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q348 (source RC-346)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review charger-session updates in HealthMonitorWorker alongside foreground tracking. Verify idempotency, concurrency, sparse-sample transition detection, and behavior after delayed execution. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q349 (source RC-347)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit alert evaluation in periodic work. Verify consistent preference snapshots, atomic debounce-state update, notification permission/channel handling, and no state corruption when posting fails. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q350 (source RC-348)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review heartbeat persistence. Verify it occurs only after the documented success boundary, never on retry/failure/cancellation, and stale detection handles changed intervals and future or corrupt timestamps. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q351 (source RC-349)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit HealthMaintenanceWorker's battery-not-low constraint and operations. Verify app-usage collection, history cleanup, and widget refresh order, cancellation, and per-operation failure policy. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q352 (source RC-350)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review best-effort widget refresh. Verify widget exceptions are contained and release-safely logged while successful database maintenance still returns success and failed core work still retries. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q353 (source RC-351)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit retention cleanup in maintenance work. Verify cutoffs, table coverage, batching, transactions, active sessions, insight expiry overlap, preference changes, and large-database performance. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q354 (source RC-352)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review InsightGenerationWorker error handling. Prove only SQLException returns retry, CancellationException is rethrown, rule computation failures are intentional, and malformed data cannot cause an infinite retry loop. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q355 (source RC-353)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit BootReceiver actions for BOOT_COMPLETED, LOCKED_BOOT_COMPLETED if declared, MY_PACKAGE_REPLACED, and USER_UNLOCKED. Verify filters, direct-boot assumptions, duplicate broadcasts, DI, and scheduling timing. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q356 (source RC-354)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review ScreenStateReceiver and ScreenStateTracker around boot and process death. Verify initial state, registration ownership, duplicate receiver avoidance, and accurate transitions for drain analysis. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q357 (source RC-503)

```text
[Priority: P0 | Execution: REPO_ONLY, TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit every dynamically registered BroadcastReceiver. On Android 14/API 34+ and later, verify each registration supplies RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED where required, unregisters exactly once, and cannot expose privileged behavior to another app. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q358 (source RC-355)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit worker overlap and database contention. Determine whether monitor, maintenance, insights, trial work, and foreground tracking can run simultaneously and whether transactions/locks prevent harmful races. Check whether any path risks exceeding WorkManager practical execution limits, especially MediaStore scans, full-history insight generation, maintenance cleanup, and widget refresh cascades. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q359 (source RC-356)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review behavior under Doze, standby, battery saver, and OEM restrictions. Verify UI does not promise exact cadence, stale state is honest, and missed work recovers without burst duplicates. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q360 (source RC-357)

```text
[Priority: P1 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Audit custom WorkManager Configuration.Provider. Verify worker factory, logging, executor choices, initialization timing, and absence of a second initializer introduced by manifest merge. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q361 (source RC-358)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review TrialNotificationWorker day-5 and day-7 unique scheduling. Verify names, delays, idempotency, clock changes, Billing initialization, Pro skip logic, and cancellation after purchase. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q362 (source RC-359)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit ProManager cancellation of trial work. Verify both jobs cancel after confirmed permanent purchase, pending purchases do not cancel prematurely, and state remains correct after restart. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q363 (source RC-360)

```text
[Priority: P0 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit WorkManager tests for unique scheduling, interval changes, constraints, retries, cancellation, partial database failure, widget failure, boot rescheduling, heartbeat freshness, and trial timing. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q364 (source RC-504)

```text
[Priority: P1 | Execution: REPO_ONLY, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit WorkManager execution-time risk. Identify workers or worker branches that can approach or exceed practical execution limits, especially MediaStore scans, cleanup, full-history insight generation, app-usage collection, widget refresh cascades, export, and network work. Verify long operations are bounded, cancellable, chunked, or deliberately excluded from periodic work. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q365 (source RC-361)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit notification-channel creation and stability. Verify IDs, names, descriptions, importance, sound/vibration, grouping, creation timing, and upgrades do not silently reset user choices. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q366 (source RC-362)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review Android 13+ POST_NOTIFICATIONS handling. Verify permission, rationale, denial, don't-ask-again, revocation, pre-33 behavior, and distinction from notification-channel enabled state. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q367 (source RC-363)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit master notifications and per-alert toggles for low battery, high temperature, low storage, and charge complete. Verify every posting path applies both levels and respects newly disabled settings. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q368 (source RC-364)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review low-battery crossing and debounce. Verify threshold equality, repeated samples, charging transition, threshold changes, reboot, persisted state, and notification dismissal cannot cause spam or permanent suppression. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q369 (source RC-365)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review high-temperature alert logic. Verify Celsius storage versus Fahrenheit display, exact boundary, recovery/cooldown, missing temperature, rapid oscillation, and interaction with thermal status. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q370 (source RC-366)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review low-storage alert logic. Verify used-percent calculation, threshold equality, primary versus removable volume, cleanup recovery, threshold changes, and rounding cannot trigger early alerts. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q371 (source RC-367)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit charge-complete notification logic. Verify FULL versus 100%, plug state, debounce across oscillation, reboot/process death, unplug reset, default disabled state, and duplicate foreground/worker observations. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q372 (source RC-368)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit notification IDs and PendingIntents. Verify stable uniqueness, immutable/update flags, request-code collisions, extras, task-stack behavior, and one notification cannot overwrite another's destination. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q373 (source RC-369)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review notification navigation. Verify only argument-free Screen.directRoutes are accepted, unknown and Pro-gated destinations are handled, cold start builds the right back stack, and repeated taps do not duplicate MainActivity. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q374 (source RC-370)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit RealTimeMonitorService startup on API 26-37. Verify startForegroundService timing, prompt startForeground, notification permission limitations, background-start restrictions, and recovery when start is disallowed. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q375 (source RC-371)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review foregroundServiceType=specialUse and subtype declaration. Verify FOREGROUND_SERVICE permissions, non-exported status, Play-policy description, API guards, and no undeclared service type is used. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q376 (source RC-372)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit the five-second battery update loop. Verify one loop, immediate initial notification, cancellation on stop/destroy, exception containment, dispatcher choice, and no unnecessary wake lock or exact alarm. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q377 (source RC-373)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review live-notification master disable. Verify immediate service stop, ongoing-notification removal, collector cancellation, persistence, and no restart after process recreation, boot, or worker execution. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q378 (source RC-374)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit per-metric live lines: current/power or drain, charging status, temperature, screen stats, and remaining time. Verify names match implementation, unavailable values omit cleanly, and toggles update live. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q379 (source RC-375)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review BigTextStyle collapsed and expanded content. Verify required metrics, units, resource strings, line length, lock-screen privacy, and graceful layout with unavailable values. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q380 (source RC-376)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit behavior when notification permission is denied or channel is disabled. Verify foreground-service legality, actionable settings UI, and no false claim that the notification is visible. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q381 (source RC-377)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review process death and START_* return semantics. Verify recreation behavior, settings reread, duplicate-job prevention, and removal or replacement of stale notification content after crash. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q382 (source RC-378)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit tapping the live notification. Verify a safe immutable PendingIntent opens the existing task, works cold/locked, preserves expected navigation, and exposes no mutable extras. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q383 (source RC-379)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review notification privacy and release logging. Verify SSID, app packages, IP/DNS, filenames, purchase state, or other sensitive details are not exposed beyond intended content. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q384 (source RC-380)

```text
[Priority: P0 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit tests for API 26/31/33+, denied permission, disabled channel, rapid toggles, restart, unavailable values, metric changes, duplicate starts, and every alert boundary/debounce case. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q385 (source RC-381)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit BillingClient lifecycle in BillingManager. Verify setup, disconnection/reconnection, listener ownership, concurrent callers, process death, app background, and cleanup without multiple clients. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q386 (source RC-382)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review product configuration for one-time INAPP runcheck_pro and RUNCHECK_PRO_PRODUCT_ID override. Verify type, default/override validation, BuildConfig generation, no subscription path, and accurate one-time purchase copy. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q387 (source RC-383)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit ProductDetails querying and offer selection. Verify unavailable product, region/account limitations, stale cache, multiple one-time offers, price formatting, and behavior before Billing setup completes. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q388 (source RC-384)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review billing-flow launch. Verify Activity lifetime, already-owned handling, rapid-tap protection, BillingResult interpretation, and no access is granted merely because launch returned OK. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q389 (source RC-385)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit PurchasesUpdatedListener. Verify OK, USER_CANCELED, ITEM_ALREADY_OWNED, SERVICE_DISCONNECTED, ERROR, null/empty lists, duplicate callbacks, and callbacks from an obsolete client. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q390 (source RC-386)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review pending purchases. Prove PENDING is separate, never unlocks Pro, survives restart/queryPurchases, displays correctly, and transitions exactly once when PURCHASED. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q391 (source RC-387)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit acknowledgement with up to three retries. Verify eligibility, already-acknowledged purchases, retryable codes, delays, cancellation, duplicate callbacks, and behavior after exhaustion. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q392 (source RC-388)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review purchase-token dedupe and multiple records. Verify duplicate callbacks, restored ownership, canceled/refunded state visible through Billing, account changes, and conflicting records cannot corrupt Pro state. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q393 (source RC-389)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit synchronous pro_status_cache cold-start restoration. Verify trust window, authoritative replacement, stale true/false risks, free-tier flash prevention, and update ordering after purchase or loss of ownership. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q394 (source RC-390)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review offline and Play Store unavailable behavior. Verify defensible cached access for known purchasers, no access for new free users, actionable restore errors, and no startup deadlock. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q395 (source RC-391)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit debug builds forcing Pro active. Prove compile-time or debug-source-set isolation, no release activation through environment mistakes, and ability to test free/expired states through fakes. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q396 (source RC-392)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review TrialManager's seven-day duration. Verify start moment, exact expiry boundary, last-known timestamp defense, timezone independence, process death, reinstall/restore assumptions, and no accidental renewal. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q397 (source RC-393)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit day-5 and day-7 trial notifications and Home prompts. Verify scheduling, no duplicate snackbar/banner/modal, dismissal pacing, expiry transition, and purchase suppression. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q398 (source RC-394)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review transition from trial Pro to permanent purchase. Verify no lock gap, trial-work cancellation, cache update, widgets, Home UI, acknowledgement state, and persistence across restart. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q399 (source RC-395)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit restore purchase in Settings and Pro Upgrade. Verify Billing initialization, queryPurchases, pending versus purchased, acknowledgement, errors, UI progress, and idempotent repeated restores. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q400 (source RC-396)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review ProState as the sole authorization decision. Verify trial and purchase combination, stable initial StateFlow, atomic concurrent expiry/purchase transitions, and no legacy boolean diverges. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q401 (source RC-397)

```text
[Priority: P0 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit ProFeature coverage. Verify EXTENDED_HISTORY, CHARGER_COMPARISON, PER_APP_BATTERY, WIDGETS, CSV_EXPORT, and THERMAL_LOGS map to current features and non-enum gates are deliberate. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q402 (source RC-398)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit gate bypass paths: direct routes, restored state, fullscreen sources, widgets, export intents, cleanup ViewModel, repositories, learn links, insight targets, notifications, and process recreation. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q403 (source RC-399)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review access loss while a gated screen or operation is active. Verify charts, cleanup deletion, export, app usage, charger comparison, widgets, and pending UI transition without corruption or loops. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q404 (source RC-400)

```text
[Priority: P0 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit billing and trial tests with deterministic clocks and fake responses for purchase, pending, already owned, acknowledgement retry, offline cache, restore, trial boundary, clock rollback, races, and every gate. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q405 (source RC-401)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit RuncheckNavHost and all route constants. Verify every documented destination exists exactly once, route strings are stable, home is the start destination, and no raw route literal diverges from the central definition. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q406 (source RC-402)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review route argument definitions and encoding for cleanup/{type}, learn/{articleId}, and fullscreen_chart/{source}/{metric}/{period}. Verify allowed values, URL encoding, case, nullability, defaults, malformed input, and oversized arguments. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q407 (source RC-403)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Screen.directRoutes. Verify it contains only argument-free destinations intended for notification or deep-link use, every entry maps to a real route, and validation tests catch unsafe additions. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q408 (source RC-404)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review learn cross-link validation at catalog initialization. Verify canonical and legacy article IDs, direct-route targets, Pro-gated targets, startup failure behavior, and safe handling of malformed resource data. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q409 (source RC-405)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit free-tier redirection for charger and app_usage. Verify redirect happens before gated work, avoids flashing protected content, produces a sensible back stack, and can resume context after purchase without loops. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q410 (source RC-406)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review cleanup route parsing and ViewModel Pro gating together. Verify direct entry, restored back stack, invalid CleanupType, expired trial, and repeated navigation produce one deterministic locked or error state. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q411 (source RC-407)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit fullscreen_chart source, metric, and period validation. Verify only supported combinations are accepted, Pro-required sources are gated, invalid values cannot crash enum parsing, and renamed metrics remain migration-safe. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q412 (source RC-408)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review FullscreenChartResult through the previous back-stack entry SavedStateHandle. Verify KEY_SOURCE, KEY_METRIC, and KEY_PERIOD are written, observed, consumed, and cleared exactly once without updating the wrong parent. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q413 (source RC-409)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit battery and network period or metric saved state. Verify route results, local chip changes, defaults, process death, multiple chart instances, and restored obsolete values cannot overwrite each other. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q414 (source RC-410)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review rememberSaveable use for sheets, dialogs, chip selections, expanded groups, and onboarding UI. Verify stable savers, size limits, restoration semantics, and no restored transient state relaunches an external action. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q415 (source RC-411)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit navigation events emitted by ViewModels or UI. Verify rotation does not replay them, rapid taps do not push duplicate destinations, and calls occur only while the NavController entry is valid. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q416 (source RC-412)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review back-stack construction for notification taps, cross-links, upgrade redirects, fullscreen charts, and external deep links if present. Verify Back and Up return to a logical parent without duplicate Home entries. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q417 (source RC-413)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit landscape-only fullscreen-chart orientation. Verify orientation is applied only while visible, restored on every exit/error/process path, behaves in multi-window, and does not cause recreation loops. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q418 (source RC-414)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review process-death restoration for every argument-bearing and Pro-gated route. Verify data reloads from stable identifiers rather than retained objects and obsolete route values after app upgrade fail gracefully. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q419 (source RC-415)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit external intents from Settings and feature screens: Play Store, privacy policy, feedback email, usage access, notification settings, media settings, CSV share, and delete IntentSender. Verify safe URI construction and resolution. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q420 (source RC-416)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review unknown learn article IDs and legacy aliases. Verify canonicalization, not-found handling, no recursive alias cycle, safe Back behavior, and no cross-link can recurse indefinitely. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q421 (source RC-417)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit destination ViewModel scoping. Verify shared versus per-destination ViewModels use the intended NavBackStackEntry, repeated destinations do not share stale state, and popped screens release collectors. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q422 (source RC-418)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review popUpTo, launchSingleTop, saveState, and restoreState options. Verify they match push-only navigation and cannot preserve stale gated screens, erase parent result state, or create hidden duplicate stacks. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q423 (source RC-419)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit navigation accessibility. Verify cards and rows announce actions, locked destinations explain the lock, Back and close controls have descriptions, and dialogs or sheets restore focus after dismissal. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q424 (source RC-420)

```text
[Priority: P0 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit navigation tests covering every route, malformed argument, direct route, free/trial/Pro state, process recreation, fullscreen result, learn alias, notification cold start, and repeated rapid navigation. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q425 (source RC-421)

```text
[Priority: P2 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit RuncheckTheme and MaterialTheme. Verify dark-only behavior, no dynamic color or light fallback, complete color roles, statusColors availability, system-bar handling, and consistent use in screens, dialogs, previews, and widgets where applicable. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q426 (source RC-422)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Search production Compose for hardcoded colors duplicating BgPage, BgCard variants, accents, text, status, confidence, or divider tokens. Separate legitimate data-driven chart colors from accidental theme bypasses. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q427 (source RC-423)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Search for repeated hardcoded dp or sp values that should use Spacing, Shapes, UiTokens, MotionTokens, or typography tokens. Focus on touch targets, icons, cards, buttons, corners, outlines, locks, and badges. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q428 (source RC-424)

```text
[Priority: P2 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit typography. Verify Manrope backs normal Material styles, JetBrains Mono is limited to numeric displays and charts, font resources package correctly, fallback is acceptable, and manual styles match documented tokens. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q429 (source RC-425)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review actual color contrast for text, icons, dividers, disabled states, Pro locks, confidence badges, chips, charts, tooltips, dialogs, snackbars, and destructive actions, including alpha compositing against real backgrounds. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q430 (source RC-426)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit minimum 48 dp touch targets for icons, chips, list rows, chart controls, close buttons, expand toggles, selectors, and inline links. Verify semantic hit areas remain adequate when visual elements are smaller. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q431 (source RC-427)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review content descriptions and semantics for ProgressRing, SignalBars, thermometer, heat strip, storage segments, charts, lock icons, confidence badges, selection controls, and decorative assets. Remove duplicate or meaningless announcements. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q432 (source RC-428)

```text
[Priority: P2 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit screen-reader grouping and reading order for hero values, units, status, metric cards, chart summaries, cleanup groups, settings rows, dialogs, and list items. Verify coherent nodes without duplicate text. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q433 (source RC-429)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Verify status and selection are never conveyed by color alone. Check Healthy/Fair/Poor/Critical, signal, confidence, chart zones, selected chips, errors, Pro locks, and cleanup selections for text, icon, pattern, or semantics. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q434 (source RC-430)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit large font and display scaling. Verify cards, chips, buttons, hero values, units, dialogs, lists, and landscape chart controls do not clip and remain scrollable or reflow correctly. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q435 (source RC-431)

```text
[Priority: P2 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review layout-direction assumptions despite English-only shipping. Find left/right padding, manually mirrored icons, Canvas labels, row ordering, or gesture logic that breaks pseudolocale, RTL testing, or future localization. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q436 (source RC-432)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Compose stability and recomposition. Identify unstable parameters, mutable collections, non-immutable UI models, hot-list lambda allocation, broad state reads, and components needlessly recomposed by live 333 ms updates. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q437 (source RC-433)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review LazyColumn, LazyRow, and Paging item keys and content types. Verify stable uniqueness, no index keys for mutable content, correct placeholder handling, state preservation, and bounded nested lazy layouts. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q438 (source RC-434)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit remember, derivedStateOf, produceState, LaunchedEffect, DisposableEffect, SideEffect, and snapshotFlow keys. Find stale captures, effects that restart every recomposition, missing cleanup, or state that should be hoisted. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q439 (source RC-435)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review ProgressRing, Instrument Sweep, live-chart scroll/glow, thermometer, heat-strip, and status animations. Verify cancellation, off-screen stopping, battery cost, deterministic tests, and a reasonable reduced-motion behavior. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q440 (source RC-436)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit Canvas and chart rendering performance. Verify path/allocation reuse, data downsampling, clipping, text-measurement caching, density handling, touch hit testing, and no full-history redraw on every small update. Include chart edge cases with zero data points, one data point, min == max, all-null values, NaN, infinity, extreme outliers, and mixed unavailable values. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q441 (source RC-437)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review chart accessibility. Verify each chart exposes source, metric, unit, period, summary/trend, selected point value/time, and a non-gesture-only way to understand essential data. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q442 (source RC-505)

```text
[Priority: P1 | Execution: REPO_ONLY, TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit every chart, graph, sparkline, hero ring, segmented bar, progress ring, and fullscreen chart for edge cases: zero points, one point, min == max, all values null, NaN, infinity, extreme outliers, missing units, unavailable values, and duplicate timestamps. Verify the UI shows an honest empty/unavailable state instead of crashing, drawing nonsense, or treating missing data as zero. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q443 (source RC-438)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit dialogs, modal sheets, snackbars, and menus for focus trapping, initial focus, Back/Escape behavior, outside-tap policy, destructive-action labeling, screen-reader announcements, and focus restoration. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q444 (source RC-439)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review edge-to-edge, status/navigation bar insets, IME insets, gesture navigation, cutouts, landscape, split-screen, and small-height devices. Verify content and bottom actions are never obscured. Include tablet, foldable, split-screen, large display, landscape, and gesture-navigation layouts, not only normal phone portrait. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q445 (source RC-440)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit previews, test tags, semantic matchers, and reusable components. Verify previews do not require production DI or live Android services, test tags are stable and selective, and components accept realistic loading/error/long-text states. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q446 (source RC-507)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit tablet, foldable, split-screen, landscape, font-scaled, and narrow-display layouts across Home, detail screens, Cleanup, App Usage, Settings, Pro Upgrade, widgets, dialogs, and fullscreen charts. Verify content is not clipped, touch targets remain reachable, orientation changes restore state, and large screens do not reveal locked or stale content accidentally. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q447 (source RC-441)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit HomeViewModel's aggregation of battery, network, thermal, storage, insights, Pro, preferences, and monitoring freshness. Verify initial/loading/error states, coherent snapshots, 333 ms throttling, cancellation, and no subsystem failure blanks the whole Home screen. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q448 (source RC-442)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review Home health-score hero, battery hero, quick-status grid, quick tools, insights, and trial/Pro cards as one screen. Verify priorities, duplicate messages, contradictory states, scroll behavior, and stable ordering across rapid updates. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q449 (source RC-443)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Home monitoring-stale calculation of more than three times the configured interval. Verify exact equality, interval changes, future/missing heartbeat, first install, worker delay, and user-facing recovery guidance. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q450 (source RC-444)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review Home insight display and markAllSeen timing. Verify only visible ranked items become seen, navigation preserves state, empty/error/loading states are distinct, and the full Insights entry remains available to all users. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q451 (source RC-445)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Battery Detail's live hero, health summary, current/power, confidence, mAh estimate, session stats, drain/sleep analysis, charger CTA, and Pro sections for a single internally consistent state model. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q452 (source RC-446)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review Battery Detail charts and statistics during charge-state changes, missing current, sparse history, rotation, fullscreen return, trial expiry, and rapid live samples. Verify no stale labels, jumps, or misleading zeroes. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q453 (source RC-447)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit Charger Comparison end to end: profile creation/rename/delete/select, session eligibility, comparison math, empty/error states, Pro gating, active session updates, and stale selected profile recovery. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q454 (source RC-448)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review Network Detail states for disconnected, validating, connected without SSID, connected with limited metadata, VPN, cellular, and permission denial. Verify help cards and metrics never contradict connection status. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q455 (source RC-449)

```text
[Priority: P0 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Network Detail history controls and speed-test summary. Verify signal/latency metric switching, periods, Pro lock, chart gaps, latest result freshness, network subtype labels, and fullscreen synchronization. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q456 (source RC-450)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review Speed Test UI across Ready, confirmation, ping, download, upload, completed, failed, canceled, and history states. Verify button enabled state, progress, back handling, retry, rotation, and no overlapping launch. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q457 (source RC-451)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Thermal Detail hero, heat strip, metrics, session min/max, educational cards, and Pro log for unavailable headroom/status/temperature, API differences, unit changes, live updates, and accessibility. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q458 (source RC-452)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review Storage Detail hero, media permission card, segmented breakdown, cleanup callout/tools, history zones, metrics, SD card, trash action, and educational cards for consistent totals and permissions. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q459 (source RC-453)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Cleanup UI state and user actions: scanning, filters, grouped paging, expand/collapse, item/group selection, total bytes, delete confirmation, system IntentSender, cancellation, partial success, refresh, and locked state. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q460 (source RC-454)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review App Usage UI for missing access, returning from settings, loading, empty data, totals, paging rows, uninstalled apps, refresh, Pro expiry, and long app names or durations. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q461 (source RC-455)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit the Learn catalog's documented 15 articles and five topics against resources and code. Verify stable IDs, topic grouping/order, titles, summaries, structured bodies, missing resources, and duplicate entries. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q462 (source RC-456)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review Learn Article rendering and cross-link buttons. Verify paragraphs/lists/headings preserve intended structure, legacy aliases resolve, links are valid and accessible, and Pro-target links route safely. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q463 (source RC-457)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit Fullscreen Chart for source/metric/period combinations, landscape lifecycle, embedded/fullscreen data parity, tooltip formatting, animation reuse, Pro locks, empty data, and result return to parent. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q464 (source RC-458)

```text
[Priority: P1 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review Settings state synchronization for monitoring, live service, notifications, thresholds, units, info cards, retention, Pro, device capability, version, and external links. Verify changes persist exactly once and failure is visible. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q465 (source RC-459)

```text
[Priority: P0 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit every destructive Settings action - clear speed tests, clear all data, reset tips, reset thresholds - for correct confirmation copy, blue primary action, cancellation, in-progress protection, atomic result, and UI refresh. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q466 (source RC-460)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review Pro Upgrade and common loading, empty, locked, recoverable-error, and fatal-error surfaces across all screens. Verify truthful copy, retry behavior, focus, no raw exceptions, and consistent navigation after success. Include first-launch states and completely empty database states for every data-backed screen, not only ordinary loading/error states. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q467 (source RC-506)

```text
[Priority: P1 | Execution: REPO_ONLY, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit first-launch and completely empty-data states for every screen that depends on Room, DataStore, permissions, Billing, monitoring heartbeat, widgets, speed-test history, app usage, cleanup results, insights, and charts. Verify the app remains useful and honest before the first worker run or first user action. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q468 (source RC-461)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit the Battery Glance widget's data query, units, confidence or unavailable handling, timestamp freshness, charging status, formatting, and behavior before any Room reading exists. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q469 (source RC-462)

```text
[Priority: P0 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit the Health Score Glance widget's latest inputs and calculation path. Verify it matches in-app scoring, handles stale or partial data, uses accessible text, and never displays a fabricated healthy default. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q470 (source RC-463)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review Pro gating for both widgets. Verify free or expired users cannot receive protected data after pinning, process restart, cache restoration, trial expiry, backup/restore, or purchase-state race. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q471 (source RC-464)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit widget refresh triggers: periodic maintenance, Pro-state changes, new readings, clear data, settings/unit changes, app update, boot, and manual refresh if present. Verify updates are neither stale nor excessive. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q472 (source RC-465)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review widget click actions and PendingIntents. Verify immutable, unique, safe destinations, correct task stack, Pro redirection, no argument injection, and no collision between widget instances or types. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q473 (source RC-466)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit multiple widget instances, resize, process death, unavailable database, and Glance state. Verify one failing instance does not block others and stale content has a clear freshness or unavailable state. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q474 (source RC-467)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit CSV export scope. Verify exactly which tables, date ranges, units, confidence, settings, device identifiers, app package names, and Pro-only data are included or deliberately excluded. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q475 (source RC-468)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review CSV correctness and injection safety. Verify RFC-compatible quoting, commas/newlines/quotes, UTF-8, stable headers, locale-independent numbers/timestamps, nulls, large files, and protection from spreadsheet formula injection. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q476 (source RC-469)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit export file lifecycle. Verify unique names, atomic writes, cancellation cleanup, no stale partial files, cache/exports-only placement, bounded retention, clear-all behavior, and no unbounded cache growth. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q477 (source RC-470)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review FileProvider and sharing. Verify authority, path XML, content URIs, MIME type, temporary read grants, ClipData for multiple files, chooser behavior, recipient access duration, and no raw file:// URI. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q478 (source RC-471)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Audit network security posture in code and merged manifest. Verify cleartext remains disabled, only system trust anchors are used, no permissive hostname verifier or trust manager exists, and production endpoints use expected TLS behavior. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q479 (source RC-472)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review backup and data-transfer exposure. Verify allowBackup=false and data-extraction rules protect Room, DataStore, SharedPreferences, exported cache files, purchase cache, trial state, and per-app usage across API levels. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q480 (source RC-473)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit every exported Activity, Receiver, Service, Provider, and widget component. Verify export is required, intent inputs are validated, permissions are correct, no privileged action is externally triggerable, and task hijacking is prevented. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q481 (source RC-474)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review permission minimization. Verify each declared permission is required at runtime, maxSdk restrictions are correct, optional permissions degrade gracefully, no broad storage/location access is requested early, and special app access is explained. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q482 (source RC-475)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Audit ReleaseSafeLog and all direct Android, Timber, println, exception, or third-party logging. Verify release emits no sensitive device, network, file, app-usage, billing, token, path, or user-state data. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q483 (source RC-476)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Prove Sentry remains debug-only. Verify release source, runtime classpath, manifest, resources, initialization, reflection, Gradle plugins, R8 mapping upload, breadcrumbs, screenshots, view hierarchy, and DSN handling. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q484 (source RC-477)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Audit all outbound network primitives against the allowlist. Find direct sockets, URLConnection, OkHttp clients, WebViews, image loaders, analytics, crash reporters, DNS calls, or library auto-upload that expand the documented surface. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q485 (source RC-478)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review secret handling in debug.credentials.properties, environment variables, signing configuration, CI secrets, local.properties, logs, reports, BuildConfig, source maps, and git history-facing config. Verify missing secrets fail safely. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q486 (source RC-479)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit privacy and deletion semantics for device telemetry, network details, app usage, file metadata, trial/billing cache, insights, widgets, exports, and debug telemetry. Verify retention, clear-all, and explicit sharing match user expectations. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q487 (source RC-480)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Perform an abuse-case review for malicious intents, malformed route args, hostile CSV values, path traversal, oversized MediaStore selections, PendingIntent mutation, exported receiver spam, database corruption, and denial-of-service through repeated actions. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q488 (source RC-482)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit all 51 unit-test files for determinism and signal quality. Find tests that pass without asserting outcomes, overuse relaxed mocks, depend on execution order, hide exceptions, test private implementation details, or miss negative cases. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q489 (source RC-483)

```text
[Priority: P1 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review MainDispatcherRule and coroutine-test use. Verify Dispatchers.Main reset, scheduler sharing, runTest time control, uncaught coroutine detection, no real delays, and no background jobs leak between tests. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q490 (source RC-484)

```text
[Priority: P0 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit clocks, random values, locale, timezone, filesystem, network, Billing, and Android static state in tests. Replace accidental environment dependence with deterministic fakes only where production boundaries support it. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q491 (source RC-485)

```text
[Priority: P0 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review the instrumented Room migration test surface. Verify every migration path, representative historical data, schema assets, destructive fallback marker, indexes, and current DAO reads after migration are tested on a real database. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q492 (source RC-486)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit fakes and test doubles for BatteryManager, ConnectivityManager, PowerManager, MediaStore, UsageStatsManager, WorkManager, PackageManager, and file deletion. Verify they model cancellation, errors, API levels, and callbacks realistically enough to catch bugs. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q493 (source RC-487)

```text
[Priority: P0 | Execution: TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review Billing, Pro, and trial tests for purchase states, acknowledgement retries, cache races, offline behavior, trial boundaries, clock rollback, refund uncertainty, restore, debug override isolation, and every feature gate. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q494 (source RC-488)

```text
[Priority: P0 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit worker and receiver tests for unique work, interval change, constraints, retry classification, cancellation, boot actions, heartbeat freshness, alert debounce, charger concurrency, widget failure, and trial scheduling. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q495 (source RC-489)

```text
[Priority: P0 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review Compose UI tests for Home and every destination. Verify loading/error/empty/locked states, navigation, rotation/restoration, permission flows, destructive confirmations, long text, rapid taps, and no reliance on fragile coordinates. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q496 (source RC-490)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit automated accessibility coverage. Verify semantic labels, traversal order, touch targets, color-independent status, contrast calculations, large-font layouts, focus in dialogs/sheets, chart summaries, and keyboard/switch access. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q497 (source RC-491)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Audit release-variant verification. Confirm release Kotlin compilation, manifest merge, R8/minification, resource shrink, Hilt/KSP generation, Sentry exclusion, signing preconditions, and a minimal release smoke path are checked somewhere. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q498 (source RC-492)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Review every GitHub Actions workflow for least-privilege permissions, action pinning, fork-PR secret safety, cache poisoning, concurrency cancellation, artifact retention, matrix consistency, and branch/path filters. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q499 (source RC-493)

```text
[Priority: P0 | Execution: TARGETED_BUILD | Mode: AUDIT]

In the runcheck Android repository, Audit CodeQL workflow against current Kotlin, AGP, and Gradle behavior. Verify java-kotlin language selection, manual assembleDebug, generated-source visibility, runner version, query packs, SARIF upload, and failure handling. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q500 (source RC-494)

```text
[Priority: P0 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Review security.yml end to end. Verify Semgrep and Dependency-Check execute the intended configs, failures are not swallowed, SARIF categories do not overwrite each other, suppressions are loaded, and reports are retained safely. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q501 (source RC-495)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Audit Sonar and both Qodana paths for AGP 9 compatibility and meaningful failure behavior. Verify tokens, project keys, generated reports, coverage inputs, unsupported API risks, and no green workflow hides skipped analysis. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q502 (source RC-496)

```text
[Priority: P1 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Review every PowerShell wrapper and legacy shell script. Verify -PlanOnly performs no heavy or mutating work, prerequisites and exit codes are correct, reports paths match documentation, quoting works, and compatibility wrappers forward all arguments. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q503 (source RC-497)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit ktlint, Detekt, Android lint, compose-rules, Google security lints, Semgrep, mobsfscan, DeepSec, CPD, and Compose Stability baselines or suppressions. Verify each exclusion is narrow, justified, current, and not hiding production code. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q504 (source RC-498)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST | Mode: AUDIT]

In the runcheck Android repository, Review dependency, secret, and supply-chain checks: Gradle verification, OSV, Dependency-Check, Dependabot, gitleaks, TruffleHog, and repository filters. Verify lock/pin coverage, false-positive policy, and actionable failure thresholds. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q505 (source RC-001)

```text
[Priority: P0 | Execution: DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit PROJECT.md against the current repository. Verify every claimed module, package, source set, entry point, route, worker, service, widget, persistence store, Pro gate, permission, and external network surface, and identify stale, missing, contradictory, or roadmap-only statements. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q506 (source RC-008)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Inventory every outbound network primitive and endpoint in production, debug, tests, Gradle scripts, and CI. Verify that only M-Lab NDT7, the configured TCP latency endpoint, Google Play Billing, and explicitly debug-only telemetry are reachable at runtime as documented. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q507 (source RC-060)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, Produce a dependency-risk review that separates verified vulnerable and reachable code, tooling-only findings, debug-only exposure, false positives, and upgrade risks. Do not recommend a version bump without checking source/API compatibility in this repository. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q508 (source RC-481)

```text
[Priority: P1 | Execution: TARGETED_TEST, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Map every major behavior in PROJECT.md to existing unit or instrumented tests. Identify untested critical paths, duplicated low-value tests, assertions that only mirror implementation, and requirements with no regression coverage. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q509 (source RC-499)

```text
[Priority: P0 | Execution: TARGETED_BUILD, DEVICE_OR_EMULATOR | Mode: AUDIT]

In the runcheck Android repository, Audit runtime performance and battery risk before release. Check cold start, main-thread I/O, ANR paths, Compose jank, chart allocation, polling loops, database growth/query latency, worker overlap, widget churn, and foreground-service cost. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q510 (source RC-500)

```text
[Priority: P0 | Execution: TARGETED_BUILD, TARGETED_TEST, DEVICE_OR_EMULATOR, EXTERNAL_CURRENT_DATA | Mode: AUDIT]

In the runcheck Android repository, Perform a v1.0 Play release readiness review covering versioning, signing, AAB contents, target-preview implications, data safety/privacy disclosures, permissions, special-use foreground service declaration, billing product, store assets, crash-free smoke tests, rollback, and reproducibility. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```

## Q511 (source RC-511)

```text
[Priority: P1 | Execution: REPO_ONLY | Mode: AUDIT]

In the runcheck Android repository, After completing this full ordered review sequence, identify any major user-visible or release-blocking risk area that this question bank still failed to cover. Do not invent a gap: justify any proposed missing area from actual repository structure, PROJECT.md, CODEX.md, AGENTS.md, build files, or observed feature surfaces. Return: (1) verdict, (2) verified findings ordered by severity, (3) file paths and symbols with line ranges when stable and available, (4) concrete runtime/build/security/user impact, (5) smallest safe remediation, and (6) focused tests or verification commands. Do not modify files unless I explicitly switch this prompt to FIX mode. In FIX mode, change only confirmed issues with the smallest safe diff and report the verification you ran. Prefer source-backed inspection and the narrowest relevant checks; read existing reports before rerunning heavy tooling. Do not assume a defect exists: inspect the actual repository first, separate verified facts from hypotheses, never invent files, symbols, APIs, command output, or runtime behavior, and explicitly state when evidence is insufficient. AI code-review tools can hallucinate.
```
