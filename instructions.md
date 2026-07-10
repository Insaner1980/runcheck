# Fable 5 Instructions - runcheck Visual Modernization

Use this file together with `UI-SPEC.md`.

`UI-SPEC.md` is the current code-derived visual reference. It describes what
the app looks like now. This file describes how to use that reference when
modernizing the UI.

## 1. Working Method

1. Read `UI-SPEC.md` first.
2. Inspect the current Compose code before changing UI behavior.
3. Treat the code as the source of truth when `UI-SPEC.md` and code disagree.
4. Make visual changes in the Compose source and theme tokens, not only in docs.
5. Keep reusable visual decisions centralized in:
   - `ui/theme/Color.kt`
   - `ui/theme/Theme.kt`
   - `ui/theme/Type.kt`
   - `ui/theme/Shapes.kt`
   - `ui/theme/Spacing.kt`
   - `ui/theme/UiTokens.kt`
   - `ui/theme/MotionTokens.kt`
   - `ui/theme/StatusColors.kt`
6. After changing visuals, update `UI-SPEC.md` so it describes the new code.

## 2. Non-Negotiable App Constraints

- Keep the app name `runcheck`.
- Keep single dark theme only.
- Do not add light mode.
- Do not add AMOLED mode.
- Do not add Material dynamic colors.
- Do not add ads.
- Do not change the one-time Pro purchase model.
- Do not introduce partial localization changes.
- Keep user-facing app strings in resources.
- Keep Pro-gated destinations gated.
- Keep confidence/measurement indicators visible where the UI currently shows
  measured health data.
- Keep accessibility semantics for charts, progress, cleanup selection, dialogs,
  and live regions.
- Keep 48dp minimum interactive targets.
- Keep reduced-motion behavior for animations.
- Use outlined Material icons unless the codebase is intentionally migrated as a
  whole.

## 3. Modernization Boundaries

Modernize by improving the existing system, not by replacing the app identity.

Allowed modernization directions:

- Cleaner hierarchy and scanning.
- Better density on repeated operational screens.
- Stronger consistency between Home and detail cards.
- More deliberate hero sections.
- Better chart legibility.
- More consistent filter chip and action placement.
- Better empty, loading, error, and Pro-locked states.
- More resilient spacing for long values and small screens.
- Better accessibility labeling where current semantics are thin.

Avoid:

- Marketing-style landing pages inside the app.
- Decorative gradients or unrelated background effects.
- Large color fills using status colors.
- New one-off hardcoded colors when a token should exist.
- New one-off spacing values for normal layout.
- Shadows/elevation as the main modernization method.
- Changing product behavior while doing visual work.
- Moving responsibilities across layers just to change UI appearance.

## 4. Implementation Rules

- Prefer editing shared components before duplicating visual logic.
- If the same visual pattern appears in multiple places, extract or reuse a
  shared component/token.
- Keep card radius, spacing, typography, and motion consistent with the theme
  unless the design system is intentionally updated.
- If a new token is needed, add it once and use it everywhere.
- Remove dead UI code when replacing a component or variant.
- Search all callers before renaming or moving a component.
- Keep animations in `MotionTokens` or explicit token-backed specs.
- Every animation must respect reduced motion.
- Keep charts and gauges readable without relying on color alone.
- Keep small status accents paired with text or semantics.

## 5. Verification Checklist

Before considering visual modernization complete:

- Compare the changed screens against `UI-SPEC.md`.
- Verify small and large screen widths.
- Verify long text/value cases do not overlap.
- Verify loading, empty, error, locked, and success states.
- Verify reduced-motion mode.
- Verify chart/progress content descriptions still exist.
- Verify touch targets remain at least 48dp.
- Verify no Fable-only assumptions were added to `UI-SPEC.md`.
- Update `UI-SPEC.md` to match the final code.

## 6. Current External References Checked

These are useful references for implementation decisions, but code remains the
local source of truth for the current app:

- Android Developers: Material 3 in Compose.
- Android Developers: Compose accessibility API defaults and minimum touch
  target behavior.
- Android Developers: Compose animation guidance.
