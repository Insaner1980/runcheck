# Home Screen Visual Polish — Design Spec

## Overview

Four targeted improvements to the DevicePulse home screen to enhance visual quality and polish.

## Changes

### 1. Battery Card — Animated Battery Icon

**Current:** Static box outline with solid blue fill and centered level number (48×78dp inside 84dp container).

**New:** Canvas-drawn battery shape (~100dp container) with:
- Rounded battery outline drawn via `drawRoundRect` with a small terminal cap on top
- Fill level drawn as a rounded rect from the bottom, proportional to `battery.level`
- **Charging state:** Animated sine wave at the fill surface using `Path` + `infiniteTransition`. The wave scrolls horizontally at a steady pace (2s period). Fill color uses a subtle vertical gradient from `AccentBlue` to a lighter blue variant.
- **Not charging:** Static fill with the same gradient, no wave animation.
- Level number displayed centered via `drawText` (or overlaid `Text` composable).
- Respects `reducedMotion` — instant/static when enabled.

**File:** `HomeScreen.kt` — replace `HomeBatteryChargeIcon` composable.

**Size:** Container `Modifier.size(100.dp)`, battery body approximately 52×82dp.

### 2. Health Score Card — Remove "Healthy" Text Labels

**Current:** `HealthBreakdownRow` uses `StatusIndicator` which renders a colored dot + text label ("Healthy", "Fair", etc.).

**New:** Replace `StatusIndicator` with `StatusDot` (already exists) — renders only the colored 8dp circle, no text.

**Files:**
- `HomeScreen.kt` — in `HealthBreakdownRow`, replace `StatusIndicator(status = status, ...)` with `StatusDot(color = statusColor(status), ...)`. The existing private `statusColor()` function already maps `HealthStatus` to colors.
- Remove `StatusIndicator` import from HomeScreen.

### 3. GridCard — Colored Icon Backgrounds

**Current:** `GridCard` wraps icon in `Surface` with `surfaceVariant.copy(alpha = 0.4f)` — uniform gray for all cards.

**New:** Add `iconBackgroundColor: Color` parameter to `GridCard` (default: current gray). Pass feature-specific colors from HomeScreen:
- Network: `AccentTeal.copy(alpha = 0.15f)`
- Thermal: `AccentOrange.copy(alpha = 0.15f)`
- Chargers: `AccentBlue.copy(alpha = 0.15f)`
- Storage: `AccentTeal.copy(alpha = 0.15f)`

**Files:**
- `GridCard.kt` — add `iconBackgroundColor` parameter, use it in the `Surface` wrapping `IconCircle`.
- `HomeScreen.kt` — pass `iconBackgroundColor` in each `GridCard` call.

### 4. Card Spacing — 8dp to 12dp

**Current:** `Spacer(modifier = Modifier.height(8.dp))` between cards.

**New:** Change to `12.dp` for the spacers between:
- HealthScoreCard → BatteryHeroCard
- BatteryHeroCard → first GridCard row
- First GridCard row → second GridCard row

**File:** `HomeScreen.kt` — three spacer height changes.

## Out of Scope

- No changes to typography, UPPERCASE labels, color palette, or navigation.
- No changes to other screens.
- No new string resources needed.
- No structural changes to component APIs beyond the `iconBackgroundColor` addition to `GridCard`.
