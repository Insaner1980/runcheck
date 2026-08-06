# runcheck — Visual Overhaul Plan (v2)

**Target implementer:** Codex
**Scope:** presentation layer only — theme, components, layout, motion. No architecture changes, no new data sources, no new permissions.
**Status of prior work:** the `codex/ui-redesign-m3-expressive` branch delivered correct *structure* (4-tab navigation, theme mode plumbing, Tools hub, new routes) and a failed *visual layer*. This plan keeps the structure and replaces the visual layer.

---

<context>
<what_happened>
A full Material 3 Expressive redesign was executed. The navigation and theming architecture came out fine. The visual result regressed against the app it replaced:

- Information density collapsed. The Home first viewport now spends roughly 40% of vertical space on a single number, where the previous version showed a score, a four-category breakdown, a battery card and a quick-tools list in the same space.
- The colour system collapsed from a multi-hue identity (navy base, amber battery, teal storage/charts, blue primary, thermal gradient) to a single desaturated blue on grey-slate. The four category tiles on Home are visually identical.
- Data visualisations are small and thin. The health ring is a ~4dp stroke inside a mostly empty card. Charts on detail screens are sparkline-sized.
- There is no measurement choreography. Operations that genuinely take time (speed test, battery current sampling, storage scan) complete without communicating that work happened.
- Light theme contrast is insufficient. Surface steps sit ~2% apart, so cards do not separate from the background.

The reference app (Avast Cleanup) that inspired the redesign is dense, high-contrast, heavily colour-coded and animation-rich. The delivered redesign is sparse, low-contrast, monochrome and static. The redesign moved away from its own reference.
</what_happened>

<target_quality_bar>
The previous runcheck was competent but visually modest — its charts and numbers were small relative to the screen. Restoring it is not the goal. The goal is a diagnostics app that reads as premium and modern on first launch: large confident data displays, deliberate colour, and motion that makes measurement feel substantial.

Concretely, when a user opens any screen, one number should dominate, that number should be legible from arm's length, and any operation that takes longer than 400ms should be visibly narrated.
</target_quality_bar>
</context>

---

<framework_decision>
<decision>Drop Material 3 Expressive. Move to stable `androidx.compose.material3:material3:1.4.0`.</decision>

<rationale>
- As of 15 July 2026 the Expressive APIs remain in `1.5.0-alpha24`. There is no beta and no release candidate. Every alpha bump can break API in a shipping Play Store app.
- The Expressive components that justified the alpha dependency (wavy progress, shape morphing, motion scheme, button groups, FAB menu) are not visible in the delivered result. The alpha cost is being paid for nothing.
- Expressive's design thesis — large rounded shapes, springy motion, generous padding — is in direct tension with a data-dense diagnostics app. It is a contributing cause of the density collapse.
- Everything this plan requires exists in stable 1.4.0, plus custom `Canvas` work that this document specifies explicitly.
</rationale>

<migration_map>
| Expressive API in current branch | Stable replacement |
|---|---|
| `MaterialExpressiveTheme` | `MaterialTheme` |
| `MotionScheme.expressive()` | project-local `MotionTokens` (already exists — verify) |
| `RuncheckWavyProgress` / wavy indicators | custom `Canvas` ring, specified in `<component id="HeroGauge">` |
| Expressive `LoadingIndicator` | custom `MeasurementIndicator`, specified in `<motion_system>` |
| `ButtonGroup` / connected button group | `SingleChoiceSegmentedButtonRow` + `SegmentedButton` |
| `LargeFlexibleTopAppBar` | `LargeTopAppBar` with `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()` |
| `HorizontalFloatingToolbar` | custom `Surface` pinned to bottom with `navigationBarsPadding()` |
| FAB menu | `ExtendedFloatingActionButton` |
| Split button | `Button` + separate `IconButton`, or single `Button` |

Remove the experimental opt-in from the build configuration once the last call site is gone. If any opt-in remains at the end of the migration, the migration is incomplete.
</migration_map>

<dependencies>
Keep the existing Compose BOM (`2026.06.01` per the prior plan — verify against `gradle/libs.versions.toml` before changing anything). Remove the `material3` version override entirely so the BOM resolves it, or pin `1.4.0` if the BOM resolves lower. Add no other dependencies. In particular: do not add Lottie, do not add an animation library, do not add a charting library. Everything in this plan is achievable with Compose `Canvas` and `androidx.compose.animation`.
</dependencies>
</framework_decision>

---

<defect_list>
These are observed in screenshots of the current branch. Fix each; each has a verification step in `<acceptance>`.

<defect id="D1" severity="high">
Home health hero: the confidence label renders one character per line vertically ("A/c/c/u/r/a/t/e" stacked down the right edge next to the confidence bar). A width constraint is collapsing to near-zero — most likely a `Row` where the bar takes `weight(1f)` and the label gets what remains, or a `Column` wrapping an unconstrained `Text`. Rebuild the confidence display per `<screen id="home">`.
</defect>

<defect id="D2" severity="high">
Home first viewport shows one number and four inert tiles. The `INSIGHTS / View all` section header renders directly above the navigation bar with no content beneath it. Restructure per `<screen id="home">`.
</defect>

<defect id="D3" severity="high">
Light theme surfaces are indistinguishable. `background #F4F7F8` against `surfaceContainerLow #F0F4F5` measures **1.03:1** contrast — effectively the same colour. Even `surface #FFFFFF` against that background is only 1.08:1. Cards have no visible boundary. Replace the light palette per `<palette>`, which uses a distinctly darker background, and add a visible border to every card in light theme.

Note: grey-on-grey surface separation in a light theme can never reach 3:1 — that is a property of the luminance range, not a fixable palette choice. The new palette raises separation to 1.27:1, and the card **border** carries the rest of the work. This is why the border spec is mandatory rather than decorative.
</defect>

<defect id="D4" severity="medium">
Speed Test ring has a radial gradient fill inside the ring that reads as a smudge rather than a control. Remove the interior fill. The ring is a stroke, the interior is the surface colour, the label sits on the surface.
</defect>

<defect id="D5" severity="medium">
The four Home category tiles are visually identical — same surface, same grey icon chip, same typography. No colour differentiates Battery / Network / Thermal / Storage. Apply domain accents per `<palette>` and the tile spec in `<component id="MetricTile">`.
</defect>

<defect id="D6" severity="medium">
Health ring stroke is ~4dp on a ~300px diameter inside a card that is mostly empty. Replace with `HeroGauge` per spec: thicker stroke, band colouring, tick marks, and the score set in the display type scale.
</defect>

<defect id="D7" severity="low">
The Insights tab is empty in normal operation ("You are all caught up", full-screen, with a large grey circle icon). Resolved by the restructure in `<screen id="insights">`.
</defect>

<defect id="D8" severity="high">
**Chart Y-axis labels wrap one character per line.** On the Battery screen, the `Level` chart renders its axis labels (80, 79, 72, 68, 64) as vertically stacked, overlapping glyphs in a narrow gutter — completely illegible. The `Temp` chart is worse: its labels (30.7–33.4) collapse into an unreadable pile. The `Current` chart on the same screen renders correctly (0, -330, -660, -990, -1320, horizontal and legible).

The difference is instructive: `Current` has the *longest* label strings and works, while `Level` has the shortest and fails. So the axis gutter width is not being derived from the measured text. The likely cause is a gutter width computed from the value range's magnitude rather than from the formatted label's measured width — a narrow value range (61–80) yields a narrow gutter, the `Text` receives a width constraint smaller than one character advance, and Compose wraps per character.

**This is the same root cause as D1.** Both are `Text` composables placed in a container whose width constraint collapses below the text's intrinsic width. Treat them as one fix: audit every `Text` that sits in a computed-width container, and size axis gutters from `TextMeasurer.measure()` on the widest formatted label plus padding — never from the data range. Add `maxLines = 1` and `softWrap = false` as a defensive backstop on all axis labels, but do not rely on that alone; it hides the symptom rather than fixing the measurement.
</defect>

<defect id="D9" severity="high">
**The period selector is clipped and its options are unreachable.** The Battery history period row renders `Since unplug | 1h | 6h |` and is then cut off at the card's right edge. `12h`, `24h` and `Week` exist but are off-screen with no scroll affordance.

The consequence is a state/control mismatch: in three of the captured screenshots the chart header reads `24h · Level` while no segment in the visible row shows a selected state — because the selected segment is off-screen. The user cannot see what is selected, cannot reach the other options, and has no indication that more exist.

Contributing problems in the same row: `Since unplug` wraps to two lines inside its segment, inflating the row height; and the `Voltage` label in the metric row below touches its right border with no padding.

**Fix:** a fixed segmented row cannot hold six options on a compact phone. Replace it per the revised spec in `<screen id="detail-common">`.
</defect>

<defect id="D10" severity="medium">
Chart plot area measures roughly 140dp tall, below the 180dp floor in `<layout>`. On `Level` and `Temp` the gradient fill is so low in alpha over the dark surface that it reads as a grey smear rather than a fill, and on `Temp` the near-flat line plus the smear makes the chart look broken rather than stable. Apply `FullBleedChart` and `ChartTokens`.
</defect>

<defect id="D11" severity="medium">
The Pro-locked `Voltage` chart shows a blurred placeholder chart, a `PRO` badge, *and* the message "Not enough data for this metric yet" simultaneously. These contradict each other — the user cannot tell whether the metric is locked, unavailable, or still collecting. The blur also reads as a rendering fault rather than an intentional treatment.

**Fix:** these are three distinct states and must be mutually exclusive. Locked → the standard Pro-locked state with a clear unlock action and no fake chart. Insufficient data → an explicit "Collecting data — needs N more samples" message with no chart. Available → the chart. Never render a blurred decoy.
</defect>

<defect id="D12" severity="low">
Chart line colour varies per metric with no system behind it — `Level` renders green, `Temp` teal, `Current` blue. Apply the domain accent rule in `<palette>`: all charts on the Battery screen use the Battery accent, regardless of which metric is selected. The metric selector tells the user what they are looking at; the colour should tell them which domain they are in.
</defect>
</defect_list>

---

## 1. Design system

<palette>
Define these as a project-local token object, not scattered `Color()` literals. Map to Material roles where a role exists; keep domain and status colours in a separate object so they are not overwritten by scheme changes.

### Dark theme (primary identity)

| Role | Hex |
|---|---|
| `background` | `#08171C` |
| `surfaceContainerLowest` | `#0C1F26` |
| `surfaceContainerLow` | `#102831` |
| `surfaceContainer` | `#16333E` |
| `surfaceContainerHigh` | `#1D404E` |
| `surfaceContainerHighest` | `#25505F` |
| `onSurface` | `#F2F7F8` |
| `onSurfaceVariant` | `#A9BEC6` |
| `outline` | `#5C7883` |
| `outlineVariant` | `#24404C` |
| `primary` | `#4EA8F5` |
| `onPrimary` | `#04222F` |
| `secondary` | `#35DDBE` |
| `tertiary` | `#FFB627` |
| `error` | `#FF6B70` |

Note the deeper `background` versus the current `#0B1E24`. Cards must sit visibly above the background; `background` → `surfaceContainer` measures 1.37:1 here versus 1.15:1 on the current build.

### Light theme

| Role | Hex |
|---|---|
| `background` | `#DDE6EA` |
| `surface` | `#FFFFFF` |
| `surfaceContainerLowest` | `#FFFFFF` |
| `surfaceContainerLow` | `#F8FBFC` |
| `surfaceContainer` | `#F1F6F7` |
| `surfaceContainerHigh` | `#E8EFF2` |
| `surfaceContainerHighest` | `#DDE6EA` |
| `onSurface` | `#0C1F26` |
| `onSurfaceVariant` | `#44606C` |
| `outline` | `#7A939D` |
| `outlineVariant` | `#B9CBD2` |
| `primary` | `#0B63B0` |
| `onPrimary` | `#FFFFFF` |
| `secondary` | `#007A66` |
| `tertiary` | `#8A6100` |
| `error` | `#B3261E` |

The light background is deliberately darker than the current `#F4F7F8`. Cards are white or near-white and sit clearly on top of it.

**Card border rule (light theme only):** every card carries a 1dp border in `outline` `#7A939D` — measured 3.24:1 against a white card, which meets the WCAG non-text threshold. Do not use `outlineVariant` `#B9CBD2` for card borders; it measures 1.68:1 and is invisible at low brightness. `outlineVariant` is for dividers *inside* a card, where less weight is wanted. This border is deliberately heavier than Material's default outline and that is intentional.

Dark theme uses no card borders — the surface steps carry it.

### Domain accents

Each of the four measurement domains owns a colour. It appears on the domain's icon, its gauge arc, its chart line and fill, and the leading edge of its tile. This is the single strongest lever for making the app feel designed rather than generated.

| Domain | Dark | Light |
|---|---|---|
| Battery | `#FFB627` | `#9B5C00` |
| Network | `#4EA8F5` | `#0B63B0` |
| Thermal | `#FF7A45` | `#C24A12` |
| Storage | `#35DDBE` | `#007A66` |

Thermal additionally uses a gradient for its temperature scale: `#35DDBE → #FFC53D → #FF7A45 → #FF6B70` (cool → normal → warm → critical). Light theme uses the light-column values as gradient stops.

### Status colours

Applied by *value*, not decoratively. A large number is coloured; a small supporting number is not.

| Status | Dark | Light |
|---|---|---|
| Good | `#2FD98C` | `#007A4D` |
| Fair | `#FFC53D` | `#8A6100` |
| Poor | `#FF8A3D` | `#A84B00` |
| Critical | `#FF6B70` | `#B3261E` |
| Neutral | `#8FA6AF` | `#44606C` |
| Unavailable | `#5C7883` | `#7A939D` |

Every status colour has a paired container and foreground. Do not derive containers by applying alpha to the status colour — alpha over a dark background produces muddy, low-contrast fills. Define container hexes explicitly and verify each foreground/container pair at 4.5:1.

**Contrast has already been verified.** Every value in this section was measured against its intended surface before this document was written. Dark accents and statuses sit at 4.8–8.4:1 on `surfaceContainer` `#16333E`; light accents and statuses sit at 4.5–6.0:1 on `surfaceContainer` `#F1F6F7`. The tightest pairs are light Thermal `#C24A12` at 4.50:1 and dark Critical `#FF6B70` at 4.81:1 — do not darken or lighten either without re-measuring. Re-verify in code and report any pair that measures below 4.5:1 rather than adjusting it silently (see `<rule id="G6">`).

Status is never conveyed by colour alone. Every coloured status carries a word, an icon, or a position on a labelled scale.

### Chart colours

Centralise in a `ChartTokens` object: `grid`, `axis`, `line`, `fillTop`, `fillBottom`, `selectedPoint`, `threshold`.

- `line` = the domain accent at full opacity, 2.5dp stroke.
- `fillTop` = domain accent at 32% alpha; `fillBottom` = domain accent at 0% alpha. Vertical gradient between them.
- `grid` = `outlineVariant` at 40% alpha, horizontal lines only, maximum 4.
- Light theme reduces `fillTop` to 18% and removes any glow.
</palette>

<typography>
Keep Manrope for text and JetBrains Mono for numbers, measurements and chart values. The change is scale, not family.

| Token | Size | Weight | Family | Use |
|---|---|---|---|---|
| `heroNumber` | 64sp | 700 | Mono | the one dominant metric on a screen |
| `heroUnit` | 24sp | 500 | Mono | the unit or `/100` beside a hero number |
| `displayLarge` | 44sp | 600 | Mono | secondary large metric |
| `headlineLarge` | 30sp | 600 | Manrope | screen titles in large app bar |
| `titleLarge` | 22sp | 600 | Manrope | card titles |
| `titleMedium` | 17sp | 600 | Manrope | list row primary |
| `bodyLarge` | 15sp | 400 | Manrope | body |
| `bodyMedium` | 13sp | 400 | Manrope | supporting |
| `metricValue` | 28sp | 600 | Mono | tile values |
| `labelLarge` | 13sp | 600 | Manrope | buttons |
| `sectionHeader` | 12sp | 700 | Manrope | uppercase, `+8%` letter spacing |
| `labelSmall` | 11sp | 500 | Manrope | axis labels, timestamps |

`heroNumber` uses `-2%` letter spacing and `lineHeight = fontSize` (no extra leading) so it does not float in its box.

**Hierarchy rule:** on any screen, the ratio between the dominant number and its own label must be at least 4:1. The current tiles are roughly 2:1 and that is why they read as flat.
</typography>

<layout>
| Token | Value |
|---|---|
| screen horizontal padding | 20dp |
| card corner radius | 24dp |
| hero card corner radius | 32dp |
| small element corner radius | 12dp |
| card internal padding | 20dp |
| gap between cards | 12dp |
| gap between sections | 28dp |
| minimum touch target | 48dp |

No elevation, no shadows — the palette carries separation. Light theme adds the 1dp border described above.

**Chart sizing:** minimum 180dp height. The plot area bleeds to the card's horizontal edges (the card's 20dp padding does not apply to the plot; axis labels sit inside). A chart shorter than 180dp or inset on both sides is a spec violation.

**Gauge sizing:** `HeroGauge` diameter = `min(screenWidth * 0.62f, 280.dp)`. Stroke 18dp. This is roughly triple the current visual weight and is the main fix for D6.

**Density floor per viewport.** Each top-level screen must present this much within the first screenful on a 411dp × 850dp viewport at font scale 1.0:
- Home: score + four sub-scores + four category tiles each showing value, unit and status.
- Tools: the primary action card plus at least four tool entries.
- A detail screen: hero metric + status + at least three supporting metrics, with the chart beginning before the fold.

If a layout cannot meet this, reduce padding before reducing content.
</layout>

---

## 2. Motion system

This section is the highest-priority part of the plan. The absence of it is the main reason the app reads as unfinished.

<motion_tokens>
| Token | Value |
|---|---|
| `durationInstant` | 100ms |
| `durationFast` | 180ms |
| `durationMedium` | 320ms |
| `durationSlow` | 520ms |
| `durationDeliberate` | 900ms |
| `easingStandard` | `CubicBezier(0.2f, 0f, 0f, 1f)` |
| `easingEmphasized` | `CubicBezier(0.05f, 0.7f, 0.1f, 1f)` |
| `easingDecelerate` | `CubicBezier(0f, 0f, 0f, 1f)` |
| `springGauge` | `spring(dampingRatio = 0.72f, stiffness = 180f)` |
| `springChip` | `spring(dampingRatio = 0.55f, stiffness = 420f)` |
| `counterTween` | `tween(700, easing = easingDecelerate)` |

Extend the existing project `MotionTokens` rather than creating a parallel object. Verify what is already there before adding.
</motion_tokens>

<measurement_choreography>
### The principle

Avast feels professional partly because operations are narrated. A scan does not silently return a number — it shows phases, counters that climb, and a completion beat. The user forms a belief that the app did work.

For runcheck this is also *honest*, which matters. Battery current sampling genuinely averages over several seconds. A speed test genuinely runs for its duration. Thermal readings genuinely poll sensors. These operations already take time; they have simply been invisible. Narrating them exposes real work.

**Honesty constraint — apply this and do not exceed it.** A minimum duration floor is permitted only where the operation performs real I/O, sampling or computation. It is not permitted as pure theatre. For each operation below, the plan states whether a floor is allowed and why. If an operation completes genuinely instantly from cache, show the result immediately with the settle animation only — skip the phase sequence.

### The state machine

Implement once, reuse everywhere:

```
Idle → Preparing → Sampling(phase 1..n) → Computing → Settling → Result
                                                   ↘ Failed → Error
```

Expose as a `MeasurementState` sealed interface with a `progress: Float` (0f..1f), a `phaseLabel: String` and a `phaseIndex: Int`. The host composable (`MeasurementScope`) drives the gauge, the label and the counter from this single source. Screens do not each implement their own version.

### Health score recalculation

Minimum total duration **2400ms**. Permitted: the score reads live sensor values, samples battery current, and queries storage — this is real I/O.

| Window | Progress | Label | Visual |
|---|---|---|---|
| 0–600ms | indeterminate | "Reading sensors" | gauge arc is a 90° segment rotating at 1 rev/1200ms; centre shows `—` |
| 600–1400ms | 0.00 → 0.40 | "Sampling battery current" | arc becomes determinate and sweeps; centre counter begins at 0 |
| 1400–2000ms | 0.40 → 0.75 | "Checking thermal and storage" | sweep continues |
| 2000–2400ms | 0.75 → final | "Computing score" | sweep to final value using `springGauge` (slight overshoot, then settle) |
| 2400–3100ms | — | status word | counter rolls 0 → final over `counterTween`; gauge colour animates from `Neutral` to the band colour over `durationSlow`; status pill scales in from 0.8 with `springChip` |
| 3100ms+ | — | — | four sub-scores stagger in, 80ms apart, each fading up 12dp |

The label crossfades between phases over `durationFast`. Do not slide it — sliding text at this size is noisy.

### Speed test

No floor needed; the test genuinely runs. Phases follow the real NDT7 lifecycle.

- "Connecting to server" — indeterminate arc, as above.
- "Measuring latency" — arc holds; latency value counts in when received.
- "Testing download" — arc sweeps 0→0.5 across the download phase. The centre shows **live Mbps** in `heroNumber`. Follow real samples with `spring(dampingRatio = 0.8f, stiffness = 300f)` so the number moves continuously rather than stepping. Do not over-smooth; visible responsiveness to real throughput variation is the point.
- "Testing upload" — arc sweeps 0.5→1.0, same treatment.
- Completion — arc performs one full 360° sweep in the result band colour over `durationSlow`, then the result cards stagger in at 80ms intervals.

Ring colour shifts by throughput band as the number climbs (Poor / Fair / Good / Excellent thresholds — take existing thresholds from the codebase, do not invent them).

### Storage scan

Floor permitted (real filesystem traversal). Category counters tick up independently as each category resolves, using `counterTween` restarted per update. The donut segments grow as categories report. Total reclaimable figure climbs continuously. This mirrors the Avast Quick Clean pattern directly.

### Battery current sampling

Floor permitted and genuinely required — the reading is an average over a sampling window. Show the window explicitly: a small horizontal progress track labelled "Averaging over 5s" beneath the value, with the value updating live and the confidence indicator resolving at the end.

### Charts

On first composition and on data change, animate the path draw-in left-to-right over `durationDeliberate` with `easingDecelerate`, and fade the gradient fill in over the same window with a 200ms delay. Animate once per data set — do not re-animate on recomposition, scroll, or configuration change. Gate on a key derived from the data, not on `Unit`.

### Counters

Every number larger than `titleMedium` animates to its value rather than snapping. Implement a shared `AnimatedCounter` that takes a target, a formatter and a `counterTween`. Applies to: health score, speed values, temperature, storage figures, signal strength, cycle count. Does not apply to timestamps, list row values, or table cells.
</measurement_choreography>

<navigation_motion>
- Top-level tab switch: crossfade over `durationFast`. No horizontal slide — slide implies ordering the tabs do not have.
- Push to detail: content enters with `slideInVertically(initialOffsetY = { it / 12 })` + `fadeIn`, over `durationMedium` with `easingEmphasized`. Exit mirrors.
- Tile → detail: if a shared-element transition can be implemented without restructuring navigation, animate the tile's accent chip into the detail hero. If it requires navigation changes, skip it — this is polish, not a requirement.
- List content on any screen entry: stagger the first six items at 40ms intervals, fading up 8dp. Items beyond the first six appear without stagger.
</navigation_motion>

<motion_constraints>
- **Reduced motion** (`Settings.Global.ANIMATOR_DURATION_SCALE == 0f` or the accessibility preference): all phase sequences collapse to a single "Measuring…" label with a static indeterminate indicator; counters snap; charts draw instantly; stagger becomes simultaneous. The *result* is identical — only the presentation of the interim changes.
- **Battery cost.** This is a battery diagnostics app; burning battery on animation is self-defeating. No infinite animations on idle screens. Every continuous animation must stop when its composable leaves composition or the screen is not resumed. The only permitted continuous animation on a resting screen is the indeterminate arc during an active measurement.
- **Never block on animation.** The user must be able to leave a screen mid-measurement. Cancelling navigation cancels the choreography without leaving state inconsistent.
- **60fps floor.** Gauges, charts and counters must not allocate inside `draw`. Hoist `Path`, `Brush` and `TextLayoutResult` objects with `remember`.
</motion_constraints>

---

## 3. Signature components

Build these first; the screens are assembled from them. Each lives in the shared component layer, and screens must not build parallel versions.

<component id="HeroGauge">
The app's primary visual asset. Replaces the current thin ring.

- Diameter per `<layout>`. Stroke 18dp, `StrokeCap.Round`.
- Track: `onSurfaceVariant` at 14% alpha, full 360° minus the gap.
- Sweep: 270° of usable arc, starting at 135° (bottom-left), leaving a 90° gap at the bottom. This reads as a gauge rather than a pie and gives the status word somewhere to live.
- Arc colour: solid status band colour, or a sweep gradient across the thermal scale when used for temperature.
- Tick marks: 24 marks around the track, 1.5dp wide, `outlineVariant`, with the 5 major marks at 3dp. Ticks are what separate this from a generic progress ring — do not omit them.
- Centre: `heroNumber` value, `heroUnit` suffix baseline-aligned to the number's baseline (not centre-aligned — that is what produces the awkward `/100` placement currently).
- Below the number inside the arc gap: the status word in `titleMedium`, in the band colour.
- Animated per `<measurement_choreography>`.

Optional variant `HeroGauge.Compact` at 96dp diameter, 8dp stroke, no ticks — for detail screen headers.
</component>

<component id="MetricTile">
Replaces the four inert Home tiles (D5).

Layout, top to bottom, in a card with a 3dp leading vertical bar in the domain accent:
1. Row: domain icon (20dp, domain accent, no grey chip behind it) + domain name in `bodyMedium` `onSurfaceVariant`.
2. Value in `metricValue`, coloured by status when the status is not Good; `onSurface` when Good.
3. Row: unit/qualifier in `bodyMedium` + trend delta chip if a previous value exists (`▲ 2.1°C` style, in status colour).
4. A 28dp-high sparkline in the domain accent, full tile width, gradient fill beneath. Omit only when no history exists — and then show a single-line reason instead of blank space.

Tile aspect: fill available width in a 2-column grid, height driven by content, minimum 132dp.
</component>

<component id="SegmentedDonut">
For composition breakdowns — storage by category, media by type. This is the Avast Media Overview pattern and it is a strong "wow" element.

- Multi-arc donut, one arc per category, 1.5° gap between arcs, 20dp stroke.
- Centre: total in `displayLarge` + label in `bodyMedium`.
- Value chips positioned *on* the ring at each arc's midpoint angle — a small pill with the category icon, value and unit, in the category colour with an explicit foreground. Chips clamp inside the component bounds; if more than four categories, show chips for the top three and a legend beneath for the rest.
- Arcs grow from 0 on first display over `durationDeliberate`, staggered 60ms apart.
</component>

<component id="StatBlock">
Solid filled colour panel with a very large number. Used sparingly — one or two per screen maximum — for headline figures where a card would be too quiet. This is the Avast "32 Installed / 8% used by apps" pattern.

- Solid domain or status colour background, explicit foreground colour, 24dp corners.
- Number in `heroNumber` (or `displayLarge` when two blocks sit side by side), label in `bodyMedium` beneath.
- Optional small icon top-right at 20% foreground alpha.
- No border, no gradient. The point is a flat confident colour block against the dark surface.
</component>

<component id="FullBleedChart">
Replaces every sparkline-sized chart.

- Minimum 180dp plot height, full card width per `<layout>`.
- Line 2.5dp in domain accent, gradient fill beneath per `ChartTokens`.
- Up to 4 horizontal grid lines, no vertical grid.
- Y-axis labels right-aligned inside the plot at 11sp; X-axis labels beneath, maximum 4, first and last always shown.
- Touch scrub: drag shows a vertical rule and a value bubble following the finger; value in `metricValue` Mono. Releasing returns to the resting state after 1200ms.
- Threshold line where the domain has one (e.g. thermal throttling point) as a 1dp dashed line in `Critical` with an inline label.
- Draw-in animation per `<measurement_choreography>`.
</component>

<component id="MeasurementIndicator">
Replaces the expressive `LoadingIndicator`. A 90° arc segment on a track, rotating at 1 rev/1200ms with `LinearEasing`, stroke matching the context (18dp in `HeroGauge`, 4dp inline). Used for the indeterminate phases only.
</component>

<component id="StatusPill">
Small pill: status colour container, explicit foreground, `labelLarge`, 10dp corners, 8dp/4dp padding, optional 14dp leading icon. Always carries a word.
</component>

<component id="EmptyStateIllustration">
Empty states currently show a large grey circle icon, which reads as unfinished. Avast uses custom illustrations here and it is a meaningful part of its polish.

Build a small set of **geometric vector illustrations drawn in Compose `Canvas`** — not raster assets, not stock art, no character figures. Style: flat, 2–3 tones from the domain accent plus `surfaceContainerHigh`, isometric or front-on abstract shapes (a device outline with data bars, a stack of discs for storage, concentric signal arcs for network). Roughly 160dp tall. Five are needed: no insights, no chargers saved, no speed test history, no cleanup results, no data yet.

These must be original geometric compositions. Do not reproduce another app's illustrations.
</component>

<component id="SectionHeader">
`sectionHeader` type, uppercase, `onSurfaceVariant`, with an optional trailing text action in `primary`. 28dp top padding, 12dp bottom. A section header must never be the last element before the navigation bar (D2).
</component>

---

## 4. Screens

<screen id="home">
**Problem:** one number occupying 40% of the viewport, four inert tiles, an orphaned section header.

**Structure:**
1. **Top bar** — `runcheck` wordmark left, monitoring status dot right (green active / amber stale / grey off). Compact, 56dp.
2. **Health hero card** — `surfaceContainerLow`, 32dp corners, containing:
   - `HeroGauge` centred, per spec, with the score, `/100` and status word.
   - Beneath the gauge, a single row of four sub-score bars: Battery / Thermal / Network / Storage. Each is a label in `labelSmall`, a value in Mono, and a 4dp track filled in the domain accent. This is the breakdown the previous version had and the current one dropped. It restores density and it justifies the card's height.
   - A single line beneath: `Measured 5:13 PM · Accurate` — one row, `bodyMedium`, `onSurfaceVariant`, with the confidence word in the confidence status colour. This replaces the broken vertical-text confidence bar entirely (**D1**). Do not attempt a vertical bar visual.
   - Tapping the card re-runs the measurement with the full choreography.
3. **2×2 `MetricTile` grid** per component spec — coloured, with values, trends and sparklines.
4. **Top insight** — one `RuncheckActionCard` for the single most severe active insight, with its action. When there are none, this slot is omitted entirely — not replaced by an empty state.
5. **Insights link row** — a single row `View all insights (3)` with a chevron. Only rendered when insights exist. This replaces the orphaned section header (**D2**).
6. Trial banner only when a trial is genuinely active.

**Removed from Home:** charger card, quick tools list, Pro status card, multiple insight cards. These belong on Tools and Settings — this part of the previous plan was correct and should be kept.
</screen>

<screen id="insights">
**The tab stays, and it stops being empty.** The current screen shows only *active problems*, so on a healthy device it is blank — which is a poor use of one of four navigation slots and trains the user to ignore it. The fix is to widen what the tab contains rather than to remove the tab.

Insights becomes "what your device has been doing", in three stacked sections:

1. **Needs attention** — the current active insights. Rows: leading severity bar in status colour (3dp), title in `titleMedium`, one-line body in `bodyMedium`, relative timestamp in `labelSmall`, one trailing action. Existing visibility and Pro rules unchanged. When empty, this section collapses to a single quiet row (`Nothing needs attention · Last checked 5:13 PM`) — not a full-screen empty state.

2. **This week** — the Weekly Report content surfaced inline rather than buried as a Tools entry: battery health delta, average discharge rate, storage delta, thermal event count, speed test median. Each as a compact stat row with a trend delta and, where history exists, a `FullBleedChart`. This section is what makes the tab worth opening on a healthy device. Weekly Report keeps its own detail screen; this is its entry point. Preserve the existing Pro gate — for a free user this section shows the locked preview state, which is still more useful than a blank screen.

3. **Recently resolved** — insights closed in the last 30 days, collapsed by default, with resolution timestamps. Gives the user evidence the app is working.

Filter (`All` / `Important`) as `SingleChoiceSegmentedButtonRow`, applying to section 1 only.

`EmptyStateIllustration` is still needed for the genuine cold-start case (no monitoring history at all), but with this structure it will rarely be seen.

**Consequence:** move Weekly Report out of the Tools grid. Tools then holds Speed Test, Storage cleanup, Charger comparison, App usage, Learn and Export.
</screen>

<screen id="tools">
- Speed Test as the dominant card: domain accent, `HeroGauge.Compact` showing the last result if one exists, `Run speed test` as a full-width filled button. Currently this card is quiet and grey; it should be the loudest thing on the screen.
- Tool grid, 2 columns: each entry gets its own accent-tinted icon (not a uniform grey chip), a title, and a one-line description. Currently these four tiles are indistinguishable from each other.
- Locked Pro entries stay visible with a consistent lock indicator.
- Secondary list rows for Learn and Export.
- Weekly Report moves out of this grid into the Insights tab — see `<screen id="insights">`.
</screen>

<screen id="settings">
Settings is structurally fine. Visual changes only:
- Section headers per `SectionHeader`.
- Grouped rows share one `surfaceContainer` card per section with 1dp dividers between rows, rather than each row being its own card. This is denser and calmer.
- Theme selector as `SingleChoiceSegmentedButtonRow`, full width.
- Threshold sliders: replace the dotted-track appearance with a solid filled track in the relevant domain accent, and show the value in Mono at the row's trailing edge.
- Warning/restricted banners use the `Poor` container with an explicit foreground — currently they render as low-contrast orange text on dark, which is hard to read.
</screen>

<screen id="detail-common">
Applies to Battery, Network, Thermal, Storage.

1. `LargeTopAppBar` with `exitUntilCollapsedScrollBehavior`.
2. **Hero block** on `surfaceContainerLow`: the domain's single dominant metric in `heroNumber` with its unit, a `StatusPill`, and either `HeroGauge.Compact` or a domain-appropriate visual (thermal uses the gradient scale bar, network uses signal arcs).
3. **Primary chart** immediately after the hero — `FullBleedChart`, before the fold. On the current build the chart sits below several info cards and is rarely seen.
4. **Selectors** — see the revised spec below. Do not use a fixed `SingleChoiceSegmentedButtonRow` for the period row.
5. Supporting metrics in a 2-column grid of compact stat rows, Mono values.
6. At most one `InfoBanner`, dismissible, chosen by existing deterministic rules.
7. One Learn link.

**Selector spec (fixes D9).** Detail screens have two selector rows: *metric* (what is plotted) and *period* (over what span).

- **Metric row** — 2 to 4 options (`Level` / `Temp` / `Current` / `Voltage` on Battery). A fixed `SingleChoiceSegmentedButtonRow` is correct here; four short labels fit. Ensure each label has at least 8dp horizontal padding inside its segment — currently `Voltage` touches its border.
- **Period row** — 5 to 6 options including a variable-width one (`Since unplug`). A fixed segmented row cannot hold these on a compact phone and is the direct cause of D9. Use a horizontally scrollable `FilterChip` row instead:
  - `LazyRow` with 8dp spacing and 20dp content padding, chips in `labelLarge`, single line, `softWrap = false`.
  - The selected chip uses the domain accent as its container with an explicit foreground, plus a leading check icon.
  - On composition and on any selection change, scroll the selected chip into view (`animateScrollToItem` with a centring offset). The user must always be able to see what is selected — this is the specific failure in D9.
  - A 24dp horizontal fade at both edges of the row signals that more options exist. Without this affordance a scrollable row looks identical to a clipped one.
- **Never** let either row clip. Verify at 411dp width, font scale 1.0 and font scale 2.0.
- The chart header (`24h · Level`) stays — it is a useful redundant confirmation of both selections and should keep working when the selectors scroll.

**Per-domain hero visual:**
- Battery — `HeroGauge.Compact` with charge level, amber arc; current draw in `displayLarge` beneath with the sampling window indicator.
- Network — signal strength as five concentric arcs filling by strength, blue; dBm in `heroNumber`.
- Thermal — temperature in `heroNumber` over a full-width gradient scale bar (the four-stop thermal gradient) with a marker at the current position and the throttling threshold marked. Keep the existing gradient bar concept from the previous version; scale it up.
- Storage — `SegmentedDonut` of used categories with free space in the centre. This is the screen where the donut earns its place.

**Storage screen, current state (verified from screenshot).** The visible portion is a plain six-row key/value list (Total Storage, Used, Available, File System, Encryption, Storage Volumes), then a Quick Actions card, then a guides link. No chart, no gauge, no donut, no period selector anywhere in that viewport. This is the clearest example in the app of the "technically correct, visually insignificant" problem: six rows of text where the reference app would show a segmented donut and a fill-rate trend.

Required structure for Storage specifically:
1. Hero: `SegmentedDonut` with free space in the centre in `heroNumber`, arcs for the used categories.
2. `StatBlock` pair beneath: total capacity and fill-rate estimate.
3. `FullBleedChart` of used space over time with the period selector.
4. Only then the key/value list — as a collapsed "Details" section, since File System and Encryption are reference facts, not things a user checks daily.
5. Quick Actions and the guides link, unchanged.

The existing key/value rows are fine content and should be kept — the problem is that they are currently the *only* content.
</screen>

<screen id="speedtest">
- Remove the interior radial gradient (**D4**).
- `HeroGauge` at full size as the control. Idle: `Start Test` in `titleLarge` in the centre, arc at rest showing the last result faintly.
- Running: full choreography per `<measurement_choreography>`. Live Mbps in `heroNumber` in the centre, phase label beneath the gauge, arc colour tracking the throughput band.
- Result: four values in a 2×2 grid — Download, Upload, Ping, Jitter — each in `metricValue` Mono with its unit, staggered in.
- History list beneath with a `FullBleedChart` of past results once two or more exist.
- Cellular warning and NDT7 logic unchanged.
</screen>

<screen id="cleanup">
**Scope check first.** The Storage screen's `Free Up Space` quick action launches Files by Google, not a runcheck screen. That handoff is correct and must be preserved — runcheck cannot delete other apps' data, and pretending otherwise would violate the app's honesty constraints. Do not replace it with an in-app screen.

Separately, the previous build had a runcheck-owned `CLEANUP TOOLS` section (Large Files / Old Downloads / APK Files, each with a Scan action) operating on the user's own accessible files via MediaStore. This section is what the spec below applies to. Confirm it still exists on the redesign branch before implementing; if it was dropped, restoring it is in scope and should be reported.

- Scan uses the storage choreography: per-category counters climbing, `SegmentedDonut` segments growing, total reclaimable in `heroNumber` counting up.
- Results list with size badges — the size chip is coloured by magnitude (large = `Poor`/`Critical`, small = `Neutral`), which is the Avast pattern and reads far better than uniform grey.
- Multi-select action bar pinned to the bottom as a custom `Surface`.
- Existing MediaStore delete flow and honesty constraints unchanged.
</screen>

---

## 5. Phasing

Each phase must compile and be visually coherent on its own.

<phase n="0" name="Bug fixes">
Fix D8 (axis labels wrapping per character) and D9 (clipped period selector) **first**, before any framework or styling work. Both are functional defects that make existing features unusable — the user currently cannot read the Battery level chart's axis or reach the 12h/24h/Week periods at all. They live in shared components, so one fix should resolve all four detail screens.

These are worth shipping on their own if the rest of the overhaul takes time.
Verify: axis labels legible on every metric of every detail screen at font scale 1.0 and 2.0; every period option reachable and the selected one always visible.
</phase>

<phase n="1" name="Framework migration">
Remove Expressive. Apply the `<migration_map>` in full, replace `MaterialExpressiveTheme` with `MaterialTheme`, remove the experimental opt-in, resolve `material3` from the stable BOM. No visual redesign in this phase — the goal is a green build on stable APIs with the current appearance intact.
Verify: `:app:compileDebugKotlin` passes; grep for `ExperimentalMaterial3ExpressiveApi` returns nothing.
</phase>

<phase n="2" name="Tokens">
New palette, typography scale, shape and layout tokens, `ChartTokens`, extended `MotionTokens`. Delete the superseded colour and dimension constants in the same phase — do not leave two sources of truth.
Verify: every preview renders in both themes; contrast audit passes.
</phase>

<phase n="3" name="Signature components">
`HeroGauge`, `MetricTile`, `SegmentedDonut`, `StatBlock`, `FullBleedChart`, `MeasurementIndicator`, `StatusPill`, `SectionHeader`, `AnimatedCounter`. Build with previews covering every state (loading, value, error, unavailable, both themes) before wiring into screens.
Verify: preview matrix complete; no allocation inside `draw` blocks.
</phase>

<phase n="4" name="Measurement choreography">
`MeasurementState`, `MeasurementScope`, the phase sequencer, reduced-motion handling, cancellation. Wire to the health score first as the reference implementation, then speed test.
Verify: measurement can be cancelled mid-phase by navigating away with no state leak; reduced motion produces the same result with no interim animation.
</phase>

<phase n="5" name="Home and Tools">
Rebuild both per `<screen>`. Fixes D1, D2, D5, D6.
Verify: density floor met at 411×850 / font scale 1.0; screenshot comparison against the current build shows a clear difference.
</phase>

<phase n="6" name="Detail screens">
Battery, Network, Thermal, Storage on the common structure with per-domain heroes. Then Speed Test and Cleanup.
Verify: every chart ≥180dp and full-bleed; every hero metric in `heroNumber`.
</phase>

<phase n="7" name="Remaining screens and empty states">
Insights, Settings, Charger Comparison, App Usage, Weekly Report, Learn, Export, Pro Upgrade, Fullscreen Chart. Build the five `EmptyStateIllustration` compositions.
Verify: no screen retains a grey-circle empty state; no parallel colour or padding implementations remain.
</phase>

<phase n="8" name="Widgets and audit">
Map widgets to the new palette via Glance `ColorProvider`. Full accessibility and contrast pass. Update `PROJECT.md` and `UI-SPEC.md` from the implemented code, not from this document.
</phase>

---

## 6. Acceptance criteria

<acceptance>
**Visual**
- Home first viewport at 411dp × 850dp, font scale 1.0, shows the score, four sub-scores and four category tiles with values — all above the fold.
- The four category tiles are distinguishable by colour without reading their labels.
- Every chart in the app is ≥180dp tall and spans the full card width.
- `HeroGauge` stroke is 18dp and the gauge diameter is ≥0.55 × screen width on a compact phone.
- In light theme, every card carries the 1dp `outline` border and every card boundary is visible at 30% display brightness.
- No screen ends with a section header immediately above the navigation bar.
- Each defect D1–D12 is verified fixed by screenshot.
- No `Text` anywhere in the app wraps one character per line at any font scale. Verify specifically on chart axes with narrow value ranges (a battery level chart spanning 61–80% is the known failure case) and on the Home confidence label.
- Every period and metric option is reachable, and the selected option is visible without scrolling or scrolls itself into view.
- Locked, insufficient-data and available chart states are mutually exclusive; no blurred placeholder charts remain.

**Motion**
- Health score recalculation runs the full phase sequence with a visible counter roll-up.
- Speed test shows live Mbps in the gauge centre during download and upload phases.
- Storage scan shows climbing per-category counters.
- Every number in `metricValue` or larger animates to its value.
- Charts draw in once per data set and do not re-animate on scroll or recomposition.
- Reduced motion produces identical results with no interim animation.
- No infinite animation runs on a resting screen.

**Technical**
- No `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` remains anywhere.
- `material3` resolves to a stable version.
- No new dependencies.
- No changes to domain, data or DI layers beyond what a presentation change strictly requires.
- All existing Pro gates, permission flows, NDT7 behaviour and honesty constraints unchanged.
- Contrast ≥4.5:1 for body text and ≥3:1 for large text and non-text indicators, in both themes.

**Regression**
- Every user path reachable before is reachable after.
- Navigation state restoration across tab switches still works.
- Widgets render correctly in both themes.
</acceptance>

---

## 7. Guardrails

<guardrails>
These exist because the previous attempt over-reached and produced work that had to be discarded. Follow them literally.

<rule id="G1">
**Do not invent file paths, class names or existing APIs.** Read the actual source before referencing anything. Where this document names something that may not exist (`MotionTokens`, `ChartColors`, `RuncheckActionCard`, `libs.versions.toml` entries), verify it first and report the discrepancy rather than creating a duplicate.
</rule>

<rule id="G2">
**This is a presentation-layer change.** Do not modify domain models, repositories, use cases, DI wiring, Room entities or WorkManager scheduling. If a visual requirement appears to need a data change, stop and report it instead of implementing it.
</rule>

<rule id="G3">
**Do not fix what is not in this plan.** Do not refactor unrelated code, do not rename things for consistency, do not add tests for untouched areas, do not "improve" architecture noticed in passing. Report observations separately; do not act on them.
</rule>

<rule id="G4">
**Prefer the simplest implementation that meets the spec.** Where a custom `Canvas` and a stable component both satisfy a requirement, use the stable component. Do not build abstraction layers, generic parameterised systems, or configuration objects for things used in one or two places. A component with six parameters that is used twice is over-engineered.
</rule>

<rule id="G5">
**Do not invent thresholds, bands or values.** Health score bands, speed test quality bands, thermal throttling points, storage warning levels — all of these exist in the codebase. Read them. If a band mapping is genuinely absent, ask rather than choosing numbers.
</rule>

<rule id="G6">
**Hex values in this document are authoritative.** Do not adjust them for taste, do not generate a tonal palette from a seed colour, do not enable dynamic colour. If a specific pair fails contrast verification, report the pair and the measured ratio; do not silently substitute.
</rule>

<rule id="G7">
**No animation theatre without real work.** Minimum duration floors are permitted only where `<measurement_choreography>` explicitly allows them. Do not add artificial delays elsewhere to make operations feel substantial.
</rule>

<rule id="G8">
**Report, don't assume.** If any part of this plan conflicts with the actual code, is ambiguous, or turns out to be a bad idea once the code is visible — stop and say so with specifics. A wrong implementation delivered confidently costs more than a question.
</rule>

<rule id="G9">
Work in a branch off the current redesign branch. Do not rewrite history on the existing branch. Commit per phase.
</rule>
</guardrails>

---

## 8. Open questions

<resolved id="Q1">
**Insights stays a top-level destination.** Resolved — see `<screen id="insights">`. The tab is widened to include the weekly summary and recently resolved insights so that it is never blank, rather than being removed. Navigation structure is unchanged: Home, Insights, Tools, Settings.
</resolved>

<resolved id="Q3">
**Weekly Report is implemented** on the redesign branch. This plan does not change its logic — it only moves its entry point from the Tools grid into the Insights tab and restyles it. Do not modify the scheduler, worker, notification channel, period calculation or report content.
</resolved>

<resolved id="Q2a">
**Battery detail screen — verified intact.** Screenshots of the redesign branch confirm it retains the metric selector (`Level` / `Temp` / `Current` / `Voltage`), the period selector, the history chart with min/avg/max summary, a `STATISTICS · LAST 10 DAYS` block, a fullscreen chart action, a `Charger Comparison` link and an `Explore battery guides` link.

**Nothing was functionally removed.** Phase 6 is therefore a restyling task, not a restoration task — with the exception of the defects D8–D12, which are real bugs in the existing implementation and must be fixed as part of it.
</resolved>

<open_question id="Q2b">
**Network and Thermal detail screens — still unverified**, as is the top portion of Battery and Storage (all captures so far are scrolled past the hero).

Given that Battery came through intact, the working assumption is that Network and Thermal did too. Confirm during Phase 6 rather than treating it as a blocker. Specifically check whether D8 (axis label wrapping) and D9 (clipped selector) reproduce there — both live in shared chart and selector components, so they almost certainly do, and fixing them once should fix all four screens.
</open_question>

<open_question id="Q4">
**App Usage / unused apps.** The previous plan specified a runcheck-owned "Not used" list inside App Usage. Whether it was implemented is unverified. Note that Files by Google offers a `Delete unused apps` flow, so there is a question of whether runcheck's version adds enough value to justify the screen. Confirm implementation status before styling it.
</open_question>
