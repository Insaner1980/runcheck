# runcheck Typography Update

## Summary

Replace the current Roboto/Roboto Mono typography system with Inter + JetBrains Mono. This applies across the entire codebase — fonts, theme definitions, typography scale, and any Compose `TextStyle` or `FontFamily` references.

## Font Changes

| Current | New | Reason |
|---------|-----|--------|
| Roboto | **Inter** (variable font) | Designed for screens, superior small-size legibility, modern aesthetic that complements glassmorphism theme |
| Roboto Mono | **Inter with tabular figures** for inline metric values; **JetBrains Mono** for hero numbers and gauges | Inter's built-in `fontFeatureSettings = "tnum"` handles most numeric display without a separate monospace font. JetBrains Mono used only for large display numbers. |

## Font Files

Download and add to `res/font/`:

- `inter_variable.ttf` — Inter variable font (single file, all weights 100–900)
- `jetbrains_mono_variable.ttf` — JetBrains Mono variable font

Both are available from Google Fonts and are licensed under OFL (Open Font License).

## FontFamily Definitions

```kotlin
// Typography.kt or FontFamilies.kt

val InterFontFamily = FontFamily(
    Font(R.font.inter_variable, weight = FontWeight.Normal),
    Font(R.font.inter_variable, weight = FontWeight.Medium),
    Font(R.font.inter_variable, weight = FontWeight.SemiBold),
    Font(R.font.inter_variable, weight = FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono_variable, weight = FontWeight.Normal),
    Font(R.font.jetbrains_mono_variable, weight = FontWeight.Medium)
)
```

## Updated Typography Scale

All `TextStyle` definitions should use these font families. The sizes and weights remain the same as the current spec — only the font families change.

| Style | Font Family | Size | Weight | Feature Settings | Usage |
|-------|-------------|------|--------|-----------------|-------|
| Display Large | JetBrains Mono | 57sp | 400 | — | Health score number in dashboard gauge |
| Display Small | JetBrains Mono | 32sp | 400 | — | Hero metric values on home grid cards ("57%", "36.6°C") |
| Headline Medium | Inter | 28sp | 400 | — | Screen titles (detail screens) |
| Title Medium | Inter | 16sp | 500 | — | Card titles, section headers |
| Body Large | Inter | 18sp | 400 | `"tnum"` | Real-time metric values in detail screens (mA, mV, °C) |
| Body Medium | Inter | 14sp | 400 | — | Metric labels, descriptions |
| Body Small | Inter | 12sp | 400 | — | Timestamps, secondary info |
| Label Medium | Inter | 12sp | 500 | — | Confidence badges, chip labels, status text |
| Label Small | Inter | 11sp | 500 | — | Chart axis labels |

### Key difference from before

- **Body Large** now uses Inter with tabular figures (`fontFeatureSettings = "tnum"`) instead of a separate monospace font. Tabular figures make all digits equal width, preventing layout jitter in real-time displays — same effect as monospace but with the visual consistency of the main UI font.
- **Display Large and Display Small** use JetBrains Mono. These are the big hero numbers (gauge scores, card values) where a dedicated monospace font adds visual distinction and a "technical instrument" feel.

## Compose TextStyle Examples

```kotlin
// Body Large — inline metric values with tabular figures
val BodyLarge = TextStyle(
    fontFamily = InterFontFamily,
    fontSize = 18.sp,
    fontWeight = FontWeight.Normal,
    fontFeatureSettings = "tnum" // tabular (fixed-width) numbers
)

// Display Small — hero numbers on home cards
val DisplaySmall = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontSize = 32.sp,
    fontWeight = FontWeight.Normal
)
```

## What to Update

1. **Font files** — Add Inter and JetBrains Mono variable font files to `res/font/`
2. **FontFamily definitions** — Replace Roboto/Roboto Mono FontFamily objects with Inter/JetBrains Mono
3. **Typography object** — Update all TextStyle entries in the app's Material 3 Typography definition
4. **Any hardcoded font references** — Search the codebase for `Roboto`, `RobotoMono`, `FontFamily.Monospace`, or `FontFamily.Default` and replace accordingly
5. **Remove old font files** — Delete any Roboto/Roboto Mono font files from `res/font/` if they were bundled manually (if using system Roboto, there's nothing to remove)

## Notes

- Inter variable font is ~300KB, JetBrains Mono variable is ~200KB. Total ~500KB addition to APK size — negligible.
- Both fonts support Latin Extended character sets, covering Finnish (ä, ö, å) and other European languages needed for localization.
- The `"tnum"` feature setting is the critical detail — without it, Inter's default proportional figures would cause the same jitter issue that monospace was solving before.
