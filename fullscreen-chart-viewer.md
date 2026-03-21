# Fullscreen Chart Viewer

Spec for expanding inline history charts into a dedicated fullscreen landscape view.

**Style direction:** Material Design 3, consistent with existing runcheck dark theme and component system.

---

## Problem

All history/trend charts (TrendChart, AreaChart) are currently displayed inline within detail screen cards. They are small, making it difficult to analyze data, identify patterns, spot anomalies, or read precise values. Users who want to actually study their data have no way to get a better view.

## Solution

Every inline history chart becomes tappable. Tapping opens a dedicated fullscreen screen locked to landscape orientation, showing the same chart data at full display size with added interactivity that isn't practical in the small inline view.

The inline charts remain as-is — they serve as quick trend previews. The fullscreen view is the "details on demand" layer.

---

## Scope

This applies generally to all inline history/trend chart instances across the app. Claude Code should identify which charts qualify by looking for TrendChart and AreaChart usages that display time-series history data. Not every chart-like visual qualifies — static indicators like ProgressRing, MiniBar, HeatStrip, and SegmentedBar are excluded.

---

## Interaction Flow

1. User sees an inline chart within a detail screen card
2. A small expand icon (top-right corner of the chart area) signals that the chart is tappable
3. User taps the chart or the expand icon
4. A new screen opens via standard push navigation, locked to landscape orientation
5. The screen displays the chart at full display size with interactive features
6. User navigates back via the system back gesture or a back/close button, returning to the previous detail screen in portrait

---

## Fullscreen Chart Screen

### Layout (Landscape)

```
┌──────────────────────────────────────────────────────────┐
│ [<] Chart Title                          Period Selector │
│                                                          │
│                                                          │
│                    Full-width chart                       │
│                    Full-height chart                      │
│                                                          │
│                                                          │
│                                              Value label │
└──────────────────────────────────────────────────────────┘
```

### Top Bar

- Minimal: back button, chart title (e.g. "Battery Level", "Signal Strength"), and the period/metric selectors
- Use the same period and metric options that the inline chart already supports (e.g. 24h / Week / Month / All, or Level / Temperature / Current / Voltage)
- The selected period and metric should carry over from the inline chart state so the user sees the same view they tapped on, just bigger

### Chart Area

- Fills all remaining space after the top bar
- Same data, same styling, same color conventions as the inline version
- Higher point density allowed since there is more horizontal space — increase the downsampling threshold proportionally to the available width

### Touch-to-Inspect (Crosshair)

- When the user touches and holds on the chart, a vertical crosshair line appears at the touch position
- A floating label near the crosshair shows the exact value and timestamp at that point
- The label follows the finger horizontally as it drags across the chart
- Lifting the finger dismisses the crosshair
- This is the primary added value of the fullscreen view — it lets users read precise data points, which is impractical on the small inline chart

### Pinch-to-Zoom (Optional, Lower Priority)

- Horizontal pinch gesture zooms the time axis in and out
- Not required for initial implementation — touch-to-inspect alone justifies the fullscreen view
- If implemented, add a "reset zoom" action to return to the default time range

---

## Inline Chart Changes

- Add a small expand icon (use an appropriate Material icon like `Fullscreen` or `OpenInFull`) in the top-right corner of each qualifying inline chart
- The icon should be subtle (use `onSurface` at reduced alpha) so it doesn't clutter the card
- The entire chart area should also be tappable as a tap target, not just the icon
- No other changes to the inline charts — they keep their current size and behavior

---

## Orientation Handling

- The fullscreen chart screen forces landscape orientation on entry
- On navigation back, orientation returns to the app default (portrait or system-controlled)
- Handle the orientation transition smoothly — no visible flicker or layout jump

---

## Screen Transition

- Use the standard app screen transition (300ms slide + fade) for consistency
- The orientation change will naturally create a visual break, so the transition doesn't need to be fancy

---

## States

- **Loading:** If chart data needs to reload for a different period, show a centered loading indicator within the chart area while keeping the top bar visible
- **Empty:** If no data exists for the selected period, show a centered message (same pattern as existing empty states in the app)
- **Error:** If data fails to load, show an error message with a retry action

---

## Accessibility

- The expand icon needs a content description (e.g. "View chart fullscreen")
- Touch-to-inspect value label must be announced for screen readers
- Respect `reducedMotion` for the crosshair animation
- Back navigation must work via both the back button and the system back gesture
