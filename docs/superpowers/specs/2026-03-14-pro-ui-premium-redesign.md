# Pro UI Premium Redesign

## Goal

Make all Pro/upgrade UI elements visually consistent, attractive, and premium-feeling across the app while maintaining the existing dark Avast-style design language. The gold/amber accent distinguishes "upgrade" actions from regular teal interactions.

## Color Tokens

Add to `Color.kt`:

```kotlin
val ProGold = Color(0xFFF5B83A)  // warm gold — all pro accent usage
```

Derived at call site:
- `ProGold.copy(alpha = 0.12f)` — badge/icon backgrounds (consistent 12% everywhere)
- `ProGold.copy(alpha = 0.35f)` — card border strokes

Remove: no longer use `AccentYellow` for Pro elements (AccentYellow stays for non-Pro usage like status indicators).

## Component Changes

### 1. ProBadgePill — lock icon + text

**File:** `ui/components/ProBadgePill.kt`

Current: yellow "pro" text in a pill.
New: 12dp lock icon + "pro" text, both in ProGold, on ProGold 12% alpha background. Pill shape stays (RoundedCornerShape 8dp).

```
┌─────────────┐
│ 🔒  pro     │   ProGold text + icon on ProGold/12% bg
└─────────────┘
```

- Icon: `Icons.Outlined.Lock`, size 12dp, tint ProGold (new import needed: `androidx.compose.material.icons.outlined.Lock`)
- Text: `labelMedium`, ProGold
- Background: `ProGold.copy(alpha = 0.12f)`
- Padding: horizontal 8dp, vertical 3dp
- Arrangement: Row with 4dp spacing

### 2. GridCard locked state (Chargers card)

**File:** `ui/components/GridCard.kt`

Current: dim overlay + standalone neutral-tinted lock icon pill in top-end corner.
New: dim overlay (unchanged) + **ProBadgePill** replaces the standalone lock icon. This changes the lock indicator from neutral (onSurface) to gold, matching the premium accent.

Remove: the custom `Surface` > `Box` > `Icon` lock badge (lines 95-113).
Add: `ProBadgePill(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))`

### 3. Home callout card ("Unlock runcheck pro")

**File:** `ui/home/HomeScreen.kt` (around line 369-409)

Current: plain BgCard card with lock IconCircle wrapped in AccentBlue Surface, title, subtitle, chevron.
New additions:
- **Gold border:** `border(1.dp, ProGold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))`
- **Replace IconCircle with custom gold circle:** `IconCircle` has a hardcoded `BgIconCircle` background that would obscure the gold. Instead, replace the `Surface` + `IconCircle` block with a direct `Box` using `ProGold.copy(alpha = 0.12f)` background + lock icon tinted ProGold. Same 44dp size.
- Rest stays the same (title, subtitle, chevron)

```kotlin
Box(
    modifier = Modifier
        .size(44.dp)
        .background(color = ProGold.copy(alpha = 0.12f), shape = CircleShape),
    contentAlignment = Alignment.Center
) {
    Icon(
        imageVector = Icons.Outlined.Lock,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        tint = ProGold
    )
}
```

### 4. Home App Usage row — simplify lock treatment

**File:** `ui/home/HomeScreen.kt` (around line 331-361)

Current: dim overlay + separate lock circle Surface + ProBadgePill.
New: remove the separate lock circle entirely. ProBadgePill already contains the lock icon. Keep the dim overlay for the locked feel.

Remove: the `Surface(shape = CircleShape, ...)` block containing the standalone lock icon (lines ~347-360).

### 5. ProFeatureCalloutCard — gold border + gold button

**File:** `ui/components/ProFeatureCalloutCard.kt`

Current: plain card with teal Button.
New:
- **Gold border:** `border(1.dp, ProGold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))` on the Card
- **Gold outline button:** replace `Button` with `OutlinedButton` using ProGold border and ProGold text color

```kotlin
OutlinedButton(
    onClick = onAction,
    border = BorderStroke(1.dp, ProGold),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = ProGold)
) {
    Text(actionLabel)
}
```

Used in: Battery detail (history locked), Thermal detail (throttling log locked), Settings (export locked).

### 6. BatteryHistoryLockedState — gold upgrade button

**File:** `ui/battery/BatteryDetailScreen.kt`

The `BatteryHistoryPreviewPlaceholder` already uses `ProBadgePill` (updated automatically). But `BatteryHistoryLockedState` still has a plain teal `Button` for the upgrade action.

Change: replace `Button` with gold `OutlinedButton` matching section 5's treatment:

```kotlin
OutlinedButton(
    onClick = onUpgradeToPro,
    modifier = Modifier.fillMaxWidth(),
    border = BorderStroke(1.dp, ProGold),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = ProGold)
) {
    Text(stringResource(R.string.pro_feature_upgrade_action))
}
```

### 7. ProFeatureLockedState — gold upgrade button

**File:** `ui/components/ProFeatureLockedState.kt`

This full-screen locked state is shown when navigating to Charger Comparison or App Usage without Pro. Users see this at the exact moment they're being asked to upgrade, so it must match the premium styling.

Change: replace `Button` with gold `OutlinedButton`:

```kotlin
OutlinedButton(
    onClick = onAction,
    border = BorderStroke(1.dp, ProGold),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = ProGold)
) {
    Text(actionLabel)
}
```

### 8. Settings upgrade section

**File:** `ui/settings/SettingsScreen.kt`

Current: teal filled `Button` for purchase, teal `OutlinedButton` for restore.
New:
- Purchase button: gold **filled** `Button` with `ButtonDefaults.buttonColors(containerColor = ProGold, contentColor = BgPage)` — this is the primary purchase CTA and should be high-emphasis
- Restore button: stays as is (secondary action, doesn't need gold treatment)

## Files Changed

1. `ui/theme/Color.kt` — add ProGold
2. `ui/components/ProBadgePill.kt` — add lock icon, switch to ProGold
3. `ui/components/GridCard.kt` — replace lock icon with ProBadgePill
4. `ui/components/ProFeatureCalloutCard.kt` — add gold border + gold outline button
5. `ui/components/ProFeatureLockedState.kt` — gold outline button
6. `ui/home/HomeScreen.kt` — gold border + custom gold icon circle on callout, simplify App Usage lock
7. `ui/battery/BatteryDetailScreen.kt` — gold outline button in BatteryHistoryLockedState
8. `ui/settings/SettingsScreen.kt` — gold filled button for purchase

## What Stays the Same

- Card backgrounds (BgCard)
- Dim overlay on locked content
- Text content and strings
- Navigation behavior
- All non-Pro UI elements
