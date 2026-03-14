# runcheck — Gauge & Color Hierarchy Update

KAIKKI SININEN KOROSTUSVÄRI SOVELLUKSESSA PITÄÄ VAIHTAA TRANSFORMATIVE TEAL (#2F6364)! Älä siis välitä kohdista jossa puhutaan sinisestä.

## Problem

The app's semantic status colors (green/yellow/red) dominate the entire home screen because gauge arcs use these colors. When device health is good, everything is green — the blue accent color (#2C84DB) is invisible. The accent color has no room to breathe.

## Solution

Make gauge arcs **neutral** (white/gray). Reserve semantic colors (green/yellow/orange/red) **only** for small status indicators. The accent color should be the most prominent color on screen for branding and interactive elements.

## Changes

### 1. Gauge Arcs — Neutral Color

The circular gauges on Health and Battery home cards, and the health gauge on the Dashboard detail screen:

**Current:** Arc color = semantic (green when healthy, yellow when fair, red when poor)
**New:** Arc color = always neutral white/light gray, regardless of status

```
Arc track (background): Color.White.copy(alpha = 0.08f)  — very subtle ring
Arc fill (progress):    Color.White.copy(alpha = 0.70f)  — visible but not colorful
```

The arc is now purely a **progress indicator** — it shows how full/high a value is, not whether it's good or bad. The status badge below handles the good/bad signal.

### 2. Status Badges — Keep Semantic Colors

The small badges/labels ("Healthy", "Fair", "Poor", "Good", etc.) keep their current semantic colors:

- Healthy/Good: green
- Fair/Attention: yellow/amber  
- Poor/Warning: orange
- Critical/Danger: red

These are small, contained elements. They provide the at-a-glance status signal without flooding the screen with color.

### 3. Accent Color — Where Blue (#2C84DB) Should Appear

These are the elements that should use the accent blue:

- **"runcheck" title** in the top bar
- **Pro card** background/border (the full-width card at the bottom)
- **Chart/graph lines** in all trend charts and sparklines
- **Chart gradient fill** (accent blue at ~15% opacity fading to transparent)
- **Active/selected states** in segmented controls, tabs, toggles
- **Interactive text** — links, tappable labels
- **Buttons** — primary action buttons
- **Pull-to-refresh indicator**

### 4. Network Signal Bars

**Current:** Bars colored by signal quality (green = good)
**New:** Filled bars = accent blue (#2C84DB), empty bars = white at low opacity

This reinforces the accent color on the home screen and removes another source of semantic color domination.

### 5. Thermal Temperature

The temperature value (e.g., "34.8°C") should remain neutral (standard text color). The status dot + "Healthy" label next to it provides the semantic signal.

### 6. Chargers & App Usage Locked Cards

Lock icons and descriptive text remain neutral/muted gray. No color changes needed here.

## Visual Result

After these changes, the home screen color hierarchy should be:

1. **Blue accent** — the most noticeable color: title, pro card, signal bars
2. **White/gray** — dominant neutral: gauge arcs, numbers, labels, card surfaces
3. **Semantic colors** — small and intentional: only in status badges and status text

When a user's device has problems (overheating, low battery), the semantic badges will turn yellow/red and draw attention precisely because the rest of the screen is neutral — the warning actually stands out more than it does now.

## What NOT to Change

- Status badge colors (keep semantic green/yellow/orange/red)
- Status text colors ("Healthy", "Fair", "Poor" etc.)
- Thermal heat strip on the detail screen (keep the gradient — it's a dedicated visualization)
- Card surface colors and glassmorphism styling
- Typography (already handled in separate update)
