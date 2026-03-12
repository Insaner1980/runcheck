# DevicePulse Visual Redesign — Implementation Guide for Claude Code

## Overview

This document describes a complete visual redesign of DevicePulse. The goal is to move from the current dashboard-with-bottom-nav layout to a single scrollable home screen with drill-down detail screens. The visual style is inspired by Avast Cleanup: flat, dark, confident, with strong typographic hierarchy and strategic use of color.

**This is a visual and structural change only.** All existing data collection, APIs, Pro gating logic, and feature implementations stay the same. We are changing how things look and where they live in the navigation, not what they do.

---

## 1. NAVIGATION STRUCTURE

### Remove bottom navigation entirely.

The app currently uses 4 bottom tabs: Home, Health, Network, More. Replace this with:

- **Home screen**: single scrollable page, all entry points visible
- **Detail screens**: Battery, Network, Thermal, Storage, System Info, Charger Test, Speed Test, App Usage, Settings
- **Navigation**: tap a card/row on Home → push detail screen with back arrow

There is no tab bar, no drawer navigation. Home is the hub. The hamburger icon in the header opens Settings directly (or a minimal side sheet with Settings + About).

### Screen hierarchy:

```
Home
├── Battery Detail
│   └── Charger Comparison (sub-screen or section within Battery)
├── Network Detail
│   └── Speed Test (sub-screen or section within Network)
├── Thermal Detail
├── Storage Detail
├── System Info
├── App Usage (PRO)
├── Settings
└── Pro Upgrade
```

---

## 2. COLOR SYSTEM

Replace the current color tokens with these. Apply globally via your theme/design system.

### Backgrounds
```kotlin
val bgPage = Color(0xFF0B1E24)        // Main app background — deep teal-navy
val bgCard = Color(0xFF133040)        // Card surfaces
val bgCardAlt = Color(0xFF0F2A35)     // Slightly darker card variant (optional)
val bgIconCircle = Color(0xFF1A3A48)  // Icon containers, subtle separators, track fills
```

### Accent colors
```kotlin
val accentTeal = Color(0xFF5DE4C7)    // Primary status: good, active, healthy
val accentBlue = Color(0xFF4A9EDE)    // Battery/charging, informational
val accentOrange = Color(0xFFF5963A)  // Warning states: warm temperature, cache
val accentRed = Color(0xFFF06040)     // Error states: hot, critical
val accentLime = Color(0xFFC8E636)    // CTA buttons ONLY — nothing else uses this
val accentYellow = Color(0xFFF5D03A)  // PRO badges, secondary highlights
```

### Text colors
```kotlin
val textPrimary = Color(0xFFE8E8ED)   // Main text, headings, hero numbers
val textSecondary = Color(0xFF90A8B0) // Subtitles, descriptions, secondary values
val textMuted = Color(0xFF506068)     // Disabled, placeholders, section headers
val textOnLime = Color(0xFF1A2E0A)    // Dark text on lime CTA buttons
```

### Key rules:
- `accentLime` is ONLY for primary CTA buttons. Never use it for status indicators, text, or decorative elements.
- Status dots use `accentTeal` (good), `accentBlue` (info/charging), `accentOrange` (warning), `accentRed` (error).
- Cards have NO border and NO shadow. They stand out from the background purely through the color difference between `bgPage` and `bgCard`.

---

## 3. TYPOGRAPHY

Use the system font (Roboto on Android). No custom fonts.

### Scale
```
Hero number:     48sp, Bold (700), letterSpacing = -0.04em
Hero unit:       20sp, Regular (400), color = textSecondary
Page title:      20sp, SemiBold (600)
Section header:  12sp, SemiBold (600), UPPERCASE, letterSpacing = 0.08em, color = textMuted
Card title:      16sp, SemiBold (600)
Card subtitle:   13sp, Regular (400), color = textSecondary or accent color
Body text:       14-15sp, Regular (400)
Body value:      14-15sp, Medium (500), right-aligned
Badge text:      10sp, SemiBold (600)
```

### Key rules:
- Hero numbers are 3-4x larger than body text. This is intentional. Do not reduce them.
- Unit labels (%, °C, GB) are always smaller and lighter weight than the number they accompany.
- Section headers are always uppercase with wide letter-spacing.

---

## 4. CARD STYLES

### Standard card
```kotlin
shape = RoundedCornerShape(16.dp)
color = bgCard
border = none
elevation = 0.dp
padding = 20.dp
```

### Hero card (Health Score on Home, Battery card)
```kotlin
shape = RoundedCornerShape(16.dp)
color = bgCard
border = none
elevation = 0.dp
padding = PaddingValues(horizontal = 24.dp, vertical = 28.dp)
```

### Grid card (2x2 feature grid)
```kotlin
shape = RoundedCornerShape(16.dp)
color = bgCard
border = none
elevation = 0.dp
contentAlignment = center
padding = PaddingValues(horizontal = 12.dp, vertical = 20.dp)
```
Content: icon in circle (top, centered) → title (centered) → optional subtitle in accent color (centered)

### Key rules:
- NO borders on any card. Ever.
- NO shadows or elevation on any card.
- NO glassmorphism, no blur, no transparency on cards.
- Cards are flat, solid `bgCard` color.

---

## 5. COMPONENTS

### Icon circles
```kotlin
size = 44.dp (standard) or 48.dp (grid cards)
shape = CircleShape
color = bgIconCircle
iconSize = 22.dp
iconColor = Color(0xFF708890) // gray, NOT accent color
```
Icons inside circles are always gray outline style. The accent color is reserved for status dots and text.

### Status dots
```kotlin
size = 8.dp
shape = CircleShape
color = status color (teal/blue/orange/red)
```
Used inline before text labels in status rows.

### Progress rings
```kotlin
strokeWidth = 10.dp (hero) or 6.dp (compact)
trackColor = bgIconCircle
strokeCap = StrokeCap.Round
```
Animate from 0 to value on screen entry, duration ~1200ms, ease-out curve.

### Progress bars (mini, horizontal)
```kotlin
height = 6.dp (standard) or 4.dp (compact)
shape = RoundedCornerShape(50%)
trackColor = bgIconCircle
fillColor = accent color based on context
```

### Buttons

**Primary CTA (lime):**
```kotlin
shape = RoundedCornerShape(28.dp)
color = accentLime
contentColor = textOnLime
height = 56.dp
fontSize = 16sp, SemiBold
width = fillMaxWidth (within parent card)
elevation = 0.dp
border = none
```

**Secondary button (outline):**
```kotlin
shape = RoundedCornerShape(28.dp)
color = transparent
border = BorderStroke(1.5.dp, textMuted)
contentColor = textSecondary
height = 48.dp
fontSize = 14sp, Medium
```

**PRO badge pill:**
```kotlin
shape = RoundedCornerShape(8.dp)
color = accentYellow with 12% opacity
contentColor = accentYellow
fontSize = 10sp, SemiBold
padding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
```

### List rows (inside cards)
```kotlin
padding = PaddingValues(vertical = 14.dp)
separator = 1px line using bgIconCircle color
```
Layout: [optional icon 20dp] — [label, left-aligned] — [value, right-aligned] — [chevron if tappable]

---

## 6. HOME SCREEN LAYOUT

The home screen scrolls vertically. Content order from top to bottom:

### 6.1 Header
```
[Hamburger ≡]  [DevicePulse title]  ............  [★ Pro icon]
```
- Hamburger opens Settings (or minimal side sheet)
- Pro icon: circle outline in `accentYellow`, star icon inside
- Title: 20sp, Bold

### 6.2 Device subtitle (optional)
```
Google Pixel 9 · 12:30
```
- 12sp, color = textMuted
- Below header, left-aligned
- Padding: 4dp top, 16dp horizontal

### 6.3 Health Score hero card
The biggest element on screen. Contains:

1. **Progress ring** (170dp diameter, 10dp stroke, teal)
   - Center: health score number (48sp Bold) + "HEALTH SCORE" label (13sp, uppercase, muted)
2. **Summary text** below ring
   - "Your device is in **good shape**." (15sp, secondary. "good shape" in teal, semibold)
   - "Temperature is slightly elevated." (second line)
3. **Status breakdown list** below summary
   - 4 rows: Battery, Thermal, Network, Storage
   - Each row: [color dot] [label] .............. [value]
   - Separated by 1px lines in `bgIconCircle` color
   - Each row is tappable → navigates to that category's detail screen

### 6.4 Battery card
Immediately below hero card, 8dp gap. This is the second most prominent element.

Layout:
```
Left side:                          Right side:
BATTERY (section label)             [Ring 80dp, blue]
50 % (hero number + unit)           [bolt icon center]
Charging · 18W · ~52 min

[━━━━━━━━━━━━━━━░░░░░░░░░░] progress bar full width

Health: Good          Temp: 37.2°C
```

- The battery card is tappable → navigates to Battery Detail
- "Health: Good" in teal, "Temp: 37.2°C" in orange

### 6.5 Feature grid (2x2)
Below battery card, 8dp gap.

```
[Network]       [Thermal]
5G · Excellent   37.2°C · Warm

[Chargers]      [Storage]
Test & compare   202 GB free
```

- Each cell: gray icon in circle → title (16sp) → subtitle (13sp, accent color)
- Grid gap: 8dp
- Each card tappable → navigates to respective detail screen

### 6.6 Quick Tools section
Below grid, 24dp gap.

Section header: "QUICK TOOLS" (uppercase, muted)

Card containing list rows:
```
[icon] Speed Test                              [>]
[icon] System Info                             [>]
[icon] App Usage                          PRO  [>]
```

### 6.7 Pro banner
Below quick tools, 24dp gap.

```
[★ icon in circle]  Unlock DevicePulse Pro     [>]
                    History, charger testing...
```
- Standard card, not visually louder than other cards
- Tappable → navigates to Pro purchase screen

### 6.8 Settings link
Below pro banner, centered text button:
```
⚙ Settings
```
- Color: textMuted
- Tappable → navigates to Settings screen

### 6.9 Bottom padding
32dp below settings link.

---

## 7. DETAIL SCREENS

Each detail screen follows this pattern:

### Header
```
[← Back]    Screen Title (centered, 20sp SemiBold)
```

### Content
The existing content for each detail screen stays the same functionally. Apply the new color tokens, card styles, and typography scale to the existing layouts.

### Specific notes per screen:

**Battery Detail:**
- Move Charger Comparison into Battery Detail as a section or sub-navigation
- Keep existing battery health gauge, charging info, temperature readings
- PRO features (history, trends, export) shown with locked state or PRO badge

**Network Detail:**
- Keep connection info, signal strength, latency
- Speed Test accessible as a section or button within Network Detail
- Keep existing speed test UI

**Thermal Detail:**
- Keep temperature readings, throttling info
- PRO: temperature history graph

**Storage Detail:**
- Keep used/available breakdown
- Keep app size list if implemented

**System Info:**
- Keep existing device info, network status, memory, storage readings
- Use categorized list pattern with uppercase section headers

**Settings:**
- Use categorized list pattern
- Keep existing settings options

---

## 8. SPACING SYSTEM

Apply consistently throughout the app:

```kotlin
val spaceXs = 4.dp     // Icon internal padding
val spaceSm = 8.dp     // Grid gap, tight spacing
val spaceMd = 12.dp    // Within components
val spaceBase = 16.dp  // Page margins, standard padding
val spaceLg = 20.dp    // Card inner padding
val spaceXl = 24.dp    // Between sections
val space2xl = 32.dp   // Major section breaks
```

Page horizontal margin is always 16dp. Card-to-card gap in grids is 8dp. Section-to-section gap is 24dp.

---

## 9. ANIMATION

Keep it minimal and functional:

- **Progress rings**: animate from 0 to value, 1200ms, ease-out. Trigger on screen entry.
- **Progress bars**: fill animation, 800ms, ease-out.
- **Screen transitions**: standard Material 3 shared axis or slide.
- **NO** entrance animations on cards (they render instantly).
- **NO** parallax, spring physics, or bounce effects.
- **NO** heavy animations that tax performance.

---

## 10. WHAT TO KEEP

- All existing data collection and measurement logic
- All existing Pro purchase and gating logic
- All existing detail screen functionality (just restyle them)
- Privacy-first, offline-first approach
- Kotlin + Jetpack Compose stack
- Material 3 foundation (but with custom colors overriding M3 defaults)

## WHAT TO REMOVE

- Bottom navigation bar and all tab-based navigation
- Health tab (merged into Home hero card)
- More tab (features distributed to Home grid and Quick Tools)
- Any card borders or shadows in the current design
- Any glassmorphism or blur effects

## WHAT TO ADD

- Hamburger menu or settings icon in header
- Battery as prominent standalone card on Home
- Quick Tools list section
- Pro banner on Home
- Uppercase section headers throughout
- Status dot + label pattern for status rows

---

## 11. IMPLEMENTATION ORDER

Suggested sequence to minimize breakage:

1. **Theme first**: Replace all color tokens, typography scale, and shape values app-wide
2. **Home screen**: Build new scrollable Home with hero card, battery card, grid, quick tools, pro banner
3. **Remove bottom nav**: Switch from tab navigation to push-based navigation from Home
4. **Restyle detail screens**: Apply new card styles, typography, and colors to each existing detail screen
5. **Adjust battery placement**: Move battery out of More, make it accessible from Home card
6. **Polish**: Animations on rings/bars, spacing consistency, edge cases
