# Home Screen Visual Polish — Design Spec

## Overview

Four targeted improvements to the DevicePulse home screen to enhance visual quality and polish.

## Changes

### 1. Battery Card — Animated Battery Icon

**Current:** Static box outline with solid blue fill and centered level number (48×78dp inside 84dp container).

**New:** Canvas-drawn battery shape (~100dp container) with:
- Rounded battery outline drawn via `drawRoundRect` with a small terminal cap on top.
- Fill level drawn as a rounded rect from the bottom, proportional to `battery.level`.
- **Charging state:** Animated sine wave at the fill surface using `Path` + `rememberInfiniteTransition`. The wave scrolls horizontally (2s period, `LinearEasing`). Wave parameters: amplitude 3dp, one full sine cycle across the battery width. Fill uses a vertical gradient from `AccentBlue` to `AccentBlue.copy(alpha = 0.6f)` (no new color token needed).
- **Not charging:** Static fill with the same gradient, no wave animation.
- **Boundary behavior:** At 0%, no fill or wave is drawn. At 100%, wave is suppressed (full static fill, clipped to battery outline).
- Level number displayed as an overlaid `Text` composable centered over the Canvas (participates in semantics tree for accessibility).
- Respects `reducedMotion` — static fill in all states when enabled.

**File:** `HomeScreen.kt` — replace `HomeBatteryChargeIcon` composable.

**Size:** Container `Modifier.size(100.dp)`, battery body approximately 52×82dp. The text column in `BatteryHeroCard` has `Modifier.weight(1f)` so it adapts to the remaining space. Tested layout works at 320dp minimum width.

### 2. Health Score Card — Remove "Healthy" Text Labels

**Current:** `HealthBreakdownRow` uses `StatusIndicator` which renders a colored dot + text label ("Healthy", "Fair", etc.).

**New:** Replace `StatusIndicator` with `StatusDot` — renders only the colored 8dp circle, no visible text. Add `Modifier.semantics { contentDescription = statusLabel }` to the `StatusDot` so the status category ("Healthy", "Fair", "Poor", "Critical") remains accessible to screen readers. The visible row still shows the category name (Battery, Thermal, etc.) and percentage, so sighted users retain full context. The colored dot serves as a quick visual indicator.

**Accessibility note:** This is an intentional refinement of the "status colors paired with labels" rule. The semantic label is preserved programmatically — only the visible text is removed. Update `CLAUDE.md` design system section to note that status dots may use semantic labels instead of visible text when the surrounding context provides category information.

**Files:**
- `HomeScreen.kt` — in `HealthBreakdownRow`, replace `StatusIndicator(status = status, ...)` with `StatusDot(color = statusColor(status), modifier = Modifier.semantics { contentDescription = statusLabel })`.
- Remove `StatusIndicator` import from HomeScreen.
- `StatusIndicator.kt` — retain file (may be used in other screens in the future).

### 3. GridCard — Colored Icon Backgrounds

**Current:** `GridCard` wraps icon in `Surface` with `surfaceVariant.copy(alpha = 0.4f)` — uniform gray for all cards.

**New:** Add `iconBackgroundColor: Color = Color.Unspecified` parameter to `GridCard`. Inside the composable body, resolve: `val resolvedIconBg = if (iconBackgroundColor == Color.Unspecified) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else iconBackgroundColor`. Pass feature-specific colors from HomeScreen:
- Network: `AccentTeal.copy(alpha = 0.15f)`
- Thermal: `AccentOrange.copy(alpha = 0.15f)`
- Chargers: `AccentBlue.copy(alpha = 0.15f)`
- Storage: `AccentTeal.copy(alpha = 0.15f)`

**Files:**
- `GridCard.kt` — add `iconBackgroundColor` parameter with `Color.Unspecified` default, resolve in body.
- `HomeScreen.kt` — pass `iconBackgroundColor` in each `GridCard` call.

### 4. Card Spacing — 8dp to 12dp

**Current:** `Spacer(modifier = Modifier.height(8.dp))` between cards.

**New:** Change to `12.dp` for the spacers between:
- HealthScoreCard → BatteryHeroCard
- BatteryHeroCard → first GridCard row
- First GridCard row → second GridCard row

**File:** `HomeScreen.kt` — three spacer height changes.

## Acceptance Criteria

- Battery wave animation runs smoothly (60fps) on Pixel 6-class hardware.
- Wave is not visible when `reducedMotion` is true — static fill only.
- All health breakdown rows are accessible via TalkBack with status category announced.
- GridCard icon backgrounds match their feature accent colors.
- Layout does not overflow or truncate on 320dp-wide screens.
- Visual verification on device after each change.

## Out of Scope

- No changes to typography, UPPERCASE labels, or navigation.
- No changes to other screens.
- No new string resources needed.
- No structural changes to component APIs beyond the `iconBackgroundColor` addition to `GridCard`.
