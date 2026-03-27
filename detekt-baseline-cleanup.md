# Task: Detekt Baseline Inventory and Prioritized Fixes

## Phase 1: Inventory (do this first, don't fix anything yet)

Read `app/detekt-baseline.xml` and count findings grouped by rule name. Output a simple table:

```
Rule Name                    | Count | Category
-----------------------------|-------|----------
RememberMissing              |     3 | Compose Rules
ComposableParamOrder         |    47 | Compose Rules
MagicNumber                  |   112 | Detekt built-in
...
```

Sort by category (Compose Rules first, then Detekt built-in), then by count descending within each category.

After the table, flag which rules fall into these buckets:

- **FIX (functional):** Rules that catch actual bugs or performance issues. These will be fixed.
- **FIX (mechanical):** Rules that improve code quality but don't change behavior. These will be fixed.
- **CONFIGURE OUT:** Rules where the finding is a false positive for this project (e.g. Room entity naming). These should be suppressed via detekt.yml config, not baseline.
- **LEAVE:** Rules where fixing isn't worth the risk or effort right now.

Stop here and show me the results before proceeding.

## Phase 2: Fix functional Compose Rules issues

Only proceed after Phase 1 is reviewed.

Fix these Compose Rules findings one rule at a time, in this order:

1. `RememberMissing` — add missing `remember` calls
2. `RememberContentMissing` — add missing `rememberSaveable` or similar
3. `ModifierReused` — ensure each child gets its own modifier
4. `MutableParams` — replace mutable params with immutable + callbacks
5. `ViewModelForwarding` — hoist ViewModel access to screen-level composables
6. `ModifierMissing` — add modifier parameter to public composables
7. `ModifierWithoutDefault` — add `Modifier` default value

For each rule:
1. Find all instances in the codebase (cross-reference with baseline)
2. Fix them
3. Run `./gradlew detekt` to verify no new issues introduced
4. Run `./gradlew assembleDebug` to verify the build still compiles
5. Remove the fixed findings from `app/detekt-baseline.xml`
6. Briefly list what you changed and in which files

Do NOT batch multiple rules together. One rule at a time, verify, then next.

## Phase 3: Fix mechanical Compose Rules issues

Same one-at-a-time approach for:

1. `ComposableParamOrder` — reorder parameters (modifier after required, before optional)
2. `PreviewPublic` — make preview functions private
3. `ComposableNaming` — fix any naming violations
4. `ContentTrailingLambda` — move content lambda to trailing position

## Phase 4: Fix safe Detekt built-in issues

Same approach, but only for these categories:

1. `UnusedPrivateMember` / `UnusedParameter` — remove dead code
2. `ReturnCount` — simplify functions with too many return points (only if the fix is clearer, skip if it would make the code worse)

## Phase 5: Configure out false positives

For findings that are project-specific false positives, update `config/detekt/detekt.yml` instead of fixing code. Common cases for this project:

- `ConstructorParameterNaming` on Room entities with snake_case column names → add `ignoreAnnotated: ['ColumnInfo', 'Entity']` or similar
- `MagicNumber` for dp/sp literal values that are more readable inline
- Any rule where the "fix" would make the code worse

After config changes, regenerate baseline: `./gradlew detektBaseline`

The new baseline should be significantly smaller than 553.

## Rules

- Read `PROJECT.md` and `CLAUDE.md` before starting
- Never fix more than one rule category before verifying build
- If a fix is ambiguous or risky, skip it and note why
- Don't rewrite functions just to satisfy a metric — if a function is complex but clear, suppress the finding with `@Suppress` and a comment explaining why
