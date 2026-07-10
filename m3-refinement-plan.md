# runcheck — Material 3 Refinement Plan

**Style direction:** Existing custom dark Material 3 theme (`RuncheckColorScheme`), unchanged colors and typography, no dynamic color, no light mode. Target feel: current default Android / Pixel-era Material 3 — generous spacing, large soft surfaces, tonal grouping instead of lines and borders, pill-shaped interactive elements, one calm hero per screen.

Based on: `UI-SPEC.md` (audit date 2026-07-06). All values below respect the 4dp grid.

---

## 1. Overall Diagnosis

Colors, typography, top bars, and system bar handling are out of scope and are fine as-is. Everything below is about rhythm, shape, grouping, and hierarchy.

What currently makes runcheck feel less like a modern default Android app:

1. **One flat visual weight everywhere.** Nearly every surface is the same recipe: `surfaceContainer`, 16dp radius, 16dp padding, 8dp internal gap, 12dp to the next card. Hero, chart, metric table, list, and callout all read at the same size and importance. Modern M3 apps vary radius, padding, and surface role by *function* — heroes are visibly bigger and rounder than list cards.
2. **Dividers do the work spacing should do.** Quick Tools, Settings, storage details, thermal metrics, and speed metrics all separate rows with hairline dividers inside a card. That is Android-9-settings-table language. Current Pixel apps group with whitespace and tonal blocks, and reserve dividers for genuinely long uniform lists.
3. **Borders where M3 uses tone.** `ActionCard` and Pro callouts use 1dp outlines. M3 is essentially borderless in dark themes; hierarchy comes from `surfaceContainer` vs `surfaceContainerHigh`, not strokes.
4. **16dp corner radius is one notch too tight.** Everything shares one 16dp radius. Current Google apps (see the Google Health / Pixel screenshots) use noticeably larger, softer radii on primary cards and near-stadium shapes on buttons and metric chips. The uniform 16dp reads "dashboard widget", not "Android app".
5. **Density spikes.** 8dp gaps between text blocks inside cards, `MetricRow` label-left/value-right tables, four-metric hero rows, status strips, colored values, and mono numerals all stacked in one viewport make diagnostics screens feel like an engineering console. The data is right; the packing is too tight and too colorful.
6. **Status color is over-distributed.** GridCard has a 4dp status strip *and* a status label *and* status-tinted ring/values elsewhere on the same screen. Modern M3 uses one status carrier per component.
7. **Small icon containers.** 36–44dp circles with 20–22dp icons look timid next to the large icon containers in current Google apps.
8. **Inconsistent hero grammar.** Home/Thermal heroes use 24dp padding; Battery/Network/Storage heroes use 16dp. Insights uses 24dp screen padding while everything else uses 16dp. These small inconsistencies erode the "designed system" feel.
9. **States are bare.** Loading = centered spinner on the page background; errors and empty states are plain body text. Modern apps render states inside the same card container the content will occupy, so the layout never "collapses".
10. **Buttons and chips use card radius.** 56dp buttons with 16dp corners read as tiles. M3 buttons are stadium/pill shaped.

None of this requires new colors, fonts, screens, or content. It is a shape, spacing, and grouping problem.

---

## 2. Material 3 Direction for runcheck

Keep the identity: dark instrument panel, JetBrains Mono numerals, oscilloscope charts, teal/amber/orange/red status language. Change the *frame around the instruments* from "engineering console" to "calm Pixel-style app".

Concretely, after the refinement runcheck should feel like this:

- **One obvious hero per screen.** The hero is the only `BgCardDeep` surface, the roundest and most padded element on the screen (28dp radius, 24dp padding), typography-dominant, with at most one status accent. Everything under it is visibly secondary.
- **Cards breathe.** Default content cards move from 16dp to 20dp internal padding and from 12dp to 16dp separation. Sections are separated by 24dp + a `SectionHeader`. The screen scrolls in clear "paragraphs", not a continuous wall.
- **Grouping by tone and gap, not by line.** Related rows live inside one card separated by spacing; nested emphasis blocks use `surfaceContainerHigh`. Dividers survive only in Settings-style long lists and multi-row value tables.
- **Pill language for interaction.** Buttons, chips, badges, and small metric containers become fully rounded. Anything tappable that isn't a card looks like a pill.
- **Quieter status.** Status color appears exactly once per component (dot, label, or ring — not all three). Values stay `onSurface`. The screen reads calm until something is actually wrong, and then the one red element is unmissable.
- **Softer, bigger icon containers.** 48dp containers with 24dp icons as the default anchor for rows and cards.
- **States belong to the layout.** Loading, empty, error, and locked all render inside standard cards with an icon, title, body, and action — same width and roughly same height as the content they replace.

The result: less dense, more Android-like, more user-friendly — without losing a single metric.

---

## 3. Token-Level Recommendations

All proposals stay on the 4dp grid. New tokens go into `Spacing.kt`, `Shapes.kt`, or `UiTokens.kt`; no color or typography tokens change.

| Area | Current token / pattern | Proposed adjustment | Reason | Expected visual effect |
|---|---|---|---|---|
| Screen horizontal padding | 16dp on most screens; 24dp on Insights and Pro upgrade | Standardize **16dp** everywhere; keep 24dp only on Pro upgrade (focused funnel layout) | One inconsistency (Insights) with no design intent | All screens align to one grid; Insights stops feeling like a different app |
| Card-to-card gap | 12dp everywhere | **16dp between unrelated cards**, **8dp between cards in the same group** (e.g. insight rows, cleanup tools) | 12dp is neither grouped nor separated; M3 apps use two distinct gaps | Clear "these belong together / these don't" rhythm |
| Section spacing | Mixed 12/24dp spacers | **24dp above every `SectionHeader`**, **8dp below it** (keep) | Sections need more air above than cards do | Screen scrolls in readable chapters |
| Card internal padding | 16dp | **20dp default** (new `Spacing.cardInner = 20.dp`); 16dp allowed only for compact rows (history items, chips rows) | 16dp with 8dp gaps makes text touch edges visually | Cards feel roomier without growing much |
| Hero card padding | 24dp (Home, Thermal) vs 16dp (Battery, Network, Storage) | **24dp all sides on every hero** | Inconsistent grammar; heroes deserve the most air | All five heroes read as the same component |
| Internal gap inside cards | 8dp default | **12dp default** between blocks inside a card; 8dp only between a label and its value | 8dp between paragraphs/blocks is cramped | Content inside cards stops feeling stacked |
| Card corner radius | `large` = 16dp for everything | `large` → **20dp** (standard cards); add **`heroShape` = 28dp** (hero cards, dialogs, bottom-sheet top corners); `medium` → **12dp** for nested inner blocks; `small` stays 8dp (thumbnails, tiny chips) | Single 16dp radius is the strongest "not modern M3" signal | Immediately softer, closer to current Pixel apps |
| Icon circle | 44dp/22dp default, 36dp/20dp compact | **48dp/24dp default** (`iconCircle`), **40dp/20dp compact** | Current containers look undersized next to modern apps | Rows and cards get a stronger, friendlier anchor |
| Button height and shape | 56dp/52dp, `shapes.large` (16dp) | Keep heights; shape → **fully rounded pill** (`CircleShape` / 50%) for all filled/outlined/text buttons | M3 buttons are stadium-shaped; 16dp corners read as tiles | The single cheapest "instantly M3" win |
| Chip spacing and shape | 8dp gap, M3 `FilterChip` default (8dp corners) | Keep M3 default chip shape; keep 8dp gap; add **4dp vertical padding around chip rows**; fullscreen chip gap 4dp → **8dp** | Chips are fine; they just sit too tight against neighbors | Chip rows stop colliding with titles and charts |
| Divider usage | `outlineVariant @ 0.35` inside most multi-row cards | Keep color/alpha; **remove dividers wherever rows are ≤4 and replace with 12dp gaps**; keep in Settings sections, storage details table, cleanup file list | Dividers everywhere = settings-table look | Cards look calmer; remaining dividers regain meaning |
| Outline usage | 1dp border on `ActionCard`, outlined Pro buttons | **Drop the `ActionCard` border**; use `surfaceContainerHigh` fill instead. Keep 1dp outline only on `OutlinedButton` | M3 dark themes are borderless; tone conveys the "action" role | Cleanup tools and callouts blend into the card system |
| Surface layering | `BgPage` → `surfaceContainer` cards; `surfaceContainerHigh` used ad hoc; hero `BgCardDeep` | Codify 4 layers: **page = `BgPage`**, **card = `surfaceContainer`**, **nested block / secondary emphasis = `surfaceContainerHigh`**, **icon container & tracks = `surfaceContainerHighest`**. `heroCardColor` reserved for exactly one hero per screen. Never place `surfaceContainer` inside `surfaceContainer` | Layering exists but isn't systematic | Predictable depth; hero stays special |
| Empty / error / loading / locked states | Bare centered spinner; plain text; assorted card styles | One shared **StateCard** pattern: standard card, `IconCircle` 48dp, `titleMedium` title, `bodyMedium` body, optional pill button; loading uses the same card silhouette with a centered spinner (or chart placeholder box, as Battery already does) | States currently break the layout rhythm | Screens keep their shape in every state; locked/error/empty feel designed |

---

## 4. Card and Surface System

Ten card roles, all built from existing colors. "No border" applies everywhere except where noted.

| Card type | Surface token | Radius | Padding | Border | Icon treatment | Internal spacing | Use when |
|---|---|---|---|---|---|---|---|
| **Page background** | `background` (`BgPage`) | — | 16dp horizontal screen padding | — | — | 16dp between cards, 24dp above sections | Always |
| **Default content card** | `surfaceContainer` (`BgCard`) | 20dp | 20dp | No | Optional leading `IconCircle` 40dp | 12dp between blocks, 8dp label→value | Any grouped content: overview panels, history cards, details |
| **Hero card** | `heroCardColor` (`BgCardDeep`) | 28dp | 24dp | No | Ring/gauge or none; no icon circles | 16dp header→value, 12dp value→status, 24dp→metrics row | Exactly one per screen, always the first card |
| **Secondary / info card** | `surfaceContainerHigh` (`BgCardAlt`) | 20dp | 16dp | No (keep 3dp left accent on dismissible `InfoCard` only) | 20dp icon, no circle | 12dp icon→text, 4dp title→body | Tips, WiFi-name help, dismissible InfoCards, CrossLink surfaces |
| **Action card** | `surfaceContainerHigh` | 20dp | 16dp | **No** (remove current 1dp outline) | Leading `IconCircle` 48dp/24dp, category/status tint on icon only | 12dp icon→text, trailing chevron in pill container like `ListRow` | Cleanup tools, "do something" entry points |
| **Metric card / block** | Inside a default card; individual metrics may sit in `surfaceContainerHigh` blocks, 12dp radius, 12dp padding | 12dp | 12dp | No | Optional 16dp icon | Label `bodySmall` over value, 4dp gap | Hero metric rows, min/avg/max stats, speed test results |
| **List group card** | `surfaceContainer` | 20dp | 16dp horizontal, 8dp vertical | No | Per-row 20dp icons or `IconCircle` 40dp | Rows min 56dp tall, separated by spacing (≤4 rows) or dividers (5+) | Quick Tools, Settings sections, storage quick actions |
| **Locked / Pro callout card** | `surfaceContainer`; preview area `surfaceVariant @ 0.12` (as Battery already does) | 20dp | 20dp | No | `ProBadgePill` top-end; lock icon in `IconCircle` 48dp for full locked states | 12dp between message and action; action = pill `OutlinedButton` | Pro-gated charts/features |
| **Error / warning card** | `surfaceContainerHigh` with 3dp left accent in `error` (or status color); full error states use `errorContainer` only in dialogs | 20dp | 16dp | No | 20dp status icon, tinted | Same as info card | Load errors, muted-notification warnings, cellular warnings |
| **Chart card** | `surfaceContainer` | 20dp | 20dp, but chart canvas may bleed to 8dp side padding inside the card | No | Expand button stays `surfaceContainerHigh` pill | Title → chips 12dp, chips → chart 12dp, chart → stats 16dp | All TrendChart/AreaChart/LiveChart hosts |

Rules of thumb:

- A screen shows **at most three surface tones** at once: page, card, nested block (plus the single hero).
- `surfaceContainerHighest` is never a card background — only icon containers, tracks, and gauge backgrounds.
- Nested blocks (`surfaceContainerHigh`) always use a smaller radius (12dp) than their parent card (20dp), which is what makes nesting legible.

---

## 5. Component-Level Recommendations

- **GridCard** — Radius 20dp. **Remove the 4dp status strip**; carry status with the existing status label + tinting the `IconCircle` icon (not the circle background). Icon circle 40dp/20dp. Padding: 16dp all sides (equalize; the current 12/16 mix disappears with the strip). Gap icon→title 12dp. Result: the Home grid reads like Pixel "at a glance" tiles instead of a monitoring dashboard.
- **ListRow** — Min height 48dp → **56dp** for primary navigation rows (Quick Tools); keep 48dp for value rows. Leading icon inside a 40dp `IconCircle` instead of a bare 20dp icon for navigation rows. Trailing chevron: keep the pill container, bump inner padding 2dp → 4dp so it reads as a soft affordance. Drop dividers between ≤4 rows.
- **ActionCard** — Biggest single change: **borderless**, `surfaceContainerHigh` fill, 20dp radius, `IconCircle` 48dp/24dp with category tint on the icon, title `titleSmall` → keep, but give the whole card 16dp padding and 12dp icon→text gap. Replace the trailing `TextButton` with the same chevron-pill as ListRow (the whole card is already the tap target).
- **MetricPill** — Currently label+value text only. Give it an optional **contained variant**: `surfaceContainerHigh`, 12dp radius, 12dp padding — used in hero metric rows and chart stats. Uncontained variant remains for tight tables. Label→value gap 4dp (from 2dp).
- **MetricRow** — Keep for long value tables (storage details, connection details). Value `titleLarge` mono is fine, but **stop coloring values**; status belongs to the label side as a small dot if needed. Divider stays for 5+ row tables; for shorter groups use 12dp spacing. Vertical padding per row: 12dp (from 8dp effective).
- **ProgressRing** — Keep exactly as is (size, stroke, animation). Only rule change: the ring is the *only* status-colored element in its card.
- **MiniBar** — Keep. Default height 6dp is fine; ensure track is always `surfaceContainerHighest` (already `iconCircleColor`). No changes needed beyond consistent use.
- **SegmentedBar** — Height 12dp → **16dp**, segment corner 6dp → **8dp** (full pill per segment at that height), gap 2dp → **3dp**. Legend dot 8dp → 10dp, row spacing 4dp → 8dp. Makes the media breakdown feel like the Google Health bars instead of a debug strip.
- **SegmentedStatusBar** — Height 6dp → **8dp**, gap 3dp → 4dp, label gap 4dp → 8dp. Same intent: slightly bolder, less hairline.
- **SignalBars** — Keep geometry (it's characterful). Corner radius 3dp → 4dp for softness. Inactive alpha 0.3 is fine.
- **HeatStrip** — Height 24dp → **32dp**, radius stays 16dp (fully rounded at that height). More presence, easier to read the indicator position.
- **ConfidenceBadge** — Already a pill; keep. Only adjustment: vertical padding 4dp → 6dp so it doesn't look squeezed next to 48dp targets.
- **IconCircle** — Default 44/22 → **48/24**; compact 36/20 → **40/20**. Background stays `iconCircleColor`. When status matters, tint the *icon*, never the circle background (keeps chroma down).
- **SectionHeader / CardSectionTitle** — Keep styles (out of typography scope). Enforce placement rules instead: `SectionHeader` only on the page background with 24dp above / 8dp below; `CardSectionTitle` only inside cards with 4dp above / 8dp below. Never both for the same block.
- **InfoCard** — Keep the 3dp accent (it's a good M3 pattern). Radius → 20dp, padding stays 16dp, icon→text 12dp. Consider `AnimatedVisibility` enter fade (currently none) for symmetry — optional.
- **CrossLinkButton** — Make it visibly a pill action: radius 20dp → **fully rounded** (it's a single-line button-like row), vertical padding to give 48dp min height, keep `surfaceContainerHigh`. Reads as a soft tonal button, matching M3 "tonal button" language without new colors.
- **ProBadgePill** — Rename in spirit to what it already is: shape small 8dp → **extraLarge (pill)**. Padding 8/3 → 10dp/4dp. Everything else stays.
- **ProFeatureCalloutCard** — Borderless `surfaceContainerHigh` (from `surfaceContainer`), 20dp radius, 20dp padding, message→action gap 12dp, action = pill `OutlinedButton`. Add a leading lock `IconCircle` 40dp to match the StateCard pattern.
- **FilterChip rows** — Keep Material 3 `FilterChip` as-is (it is already the modern component). Add 4dp vertical padding around rows; ensure 12dp gap to the chart below. In fullscreen, gap 4dp → 8dp.
- **TrendChart and chart containers** — The chart itself (sweep, zones, tooltip) is a signature; don't touch drawing. Container changes only: host card 20dp radius / 20dp padding; title→chips 12dp; chips→chart 12dp; chart→stats 16dp; stats as contained MetricPills. Expand button: shape small 8dp → pill, keep `surfaceContainerHigh @ 0.9`.

---

## 6. Screen-by-Screen Recommendations

### Home
- **Keep:** overall order (health hero → battery hero → grid → quick tools → insights), 600dp wide-layout switch, insights preview limit of three.
- **Too dense/technical:** GridCard status strips; battery hero crammed at 16dp vertical padding; 12dp gaps everywhere flatten the hierarchy.
- **Hero:** health score card gets `heroShape` 28dp; it already has 24dp padding — keep. Category bar segments 6dp → 8dp tall.
- **Grouping:** three groups with 24dp between them: (1) heroes, (2) status grid, (3) quick tools + insights. Within a group, 12dp (heroes) / 8–12dp (grid rows).
- **Spacing:** grid gaps 8dp → keep column 8dp, but row gap 12dp → 8dp so the 2×2 grid reads as one unit; 24dp before Quick Tools header.
- **Reuse:** GridCard (destripped), ListRow with icon circles in Quick Tools, InsightRow shared with Insights screen.
- **High-impact:** (1) remove GridCard status strips + 20dp radii; (2) hero at 28dp radius — Home instantly reads "Pixel", everything else follows.

### Battery Detail
- **Keep:** panel order, session graphs, statistics, since-unplug logic, blurred Pro preview (already a great locked pattern).
- **Too dense:** hero at 16dp padding with a 4-pill metrics row; many panels of identical weight; dividers inside overview panels.
- **Hero:** 24dp padding, 28dp radius, ring stays 100dp; metrics row becomes 3 contained MetricPills (12dp radius blocks), W+mV as secondary text, not extra pills.
- **Grouping:** Now (hero + overview + charging) → History → Session → Screen/Sleep → Statistics, each behind a `SectionHeader` with 24dp above.
- **Spacing:** card gap 12dp → 16dp; panel padding 16dp → 20dp; drop dividers in ≤4-row panels.
- **Reuse:** chart card grammar identical to Network/Thermal/Storage; StateCard for load errors.
- **High-impact:** (1) hero padding/radius upgrade; (2) replace overview-panel dividers with spacing — the screen loses its "console" feel while keeping every metric.

### Network Detail
- **Keep:** hero quality row + SignalBars + dBm/latency numerals; copyable rows; connection details table.
- **Too dense:** connection details is a long divider table mixing WiFi/cellular/IP/DNS; hero at 16dp.
- **Hero:** 24dp padding, 28dp radius; SignalBars stays; only the quality label carries status color; dBm value in `onSurface`.
- **Grouping:** split Connection Details into two cards — "Connection" (type, band, bandwidth) and "Addresses" (IP, DNS, MTU; keeps dividers, it's a 5+ row table).
- **Spacing:** 16dp between cards; 20dp padding; 12dp row rhythm in short cards.
- **Reuse:** Speed test summary uses contained MetricPills; Pro callout uses updated ProFeatureCalloutCard.
- **High-impact:** (1) split the details table; (2) contained metric blocks in hero — the screen goes from "network debug tool" to "status page".

### Speed Test
- **Keep:** the gauge hero entirely (size, arcs, animations) — it's the app's best modern moment.
- **Too dense:** metrics card and latest-result card look identical; history rows are cramped at 12dp vertical.
- **Hero:** no change to the gauge; give the wrapper 16dp breathing room below before the metrics card.
- **Grouping:** Live metrics card directly under the gauge (contained MetricPills, no divider — 2×2 block grid with 8dp gaps); latest result as its own card behind a `SectionHeader`; history as a list group card.
- **Spacing:** history item vertical padding 12dp → 16dp; card padding 24/16 → 20dp uniform.
- **Reuse:** the 2×2 contained-metric grid here becomes the shared pattern for chart stats everywhere.
- **High-impact:** (1) replace the divider-split metrics card with a 2×2 metric-block grid (mirrors Google Health tiles); (2) 20dp-radius history group card.

### Thermal Detail
- **Keep:** hero (already 24dp padding, centered, segmented bar — closest to target today), throttling event list, live charts.
- **Too dense:** metrics card divides CPU/headroom/status/throttling with dividers; HeatStrip is thin.
- **Hero:** just add 28dp radius; segmented bar 8dp tall.
- **Grouping:** metrics card → 2×2 contained MetricPill grid, no dividers; history card standard chart grammar; throttling behind `SectionHeader`.
- **Spacing:** 16dp card gaps, 20dp padding.
- **Reuse:** empty throttling state → StateCard with healthy dot.
- **High-impact:** (1) 2×2 metric grid; (2) HeatStrip at 32dp — thermal becomes the most "Pixel" screen with minimal work.

### Storage Detail
- **Keep:** hero ring + fill rate, SegmentedBar breakdown, cleanup tool list, details table (keeps dividers — 5+ rows).
- **Too dense:** hero at 16dp; cleanup ActionCards with borders; media breakdown bar thin.
- **Hero:** 24dp/28dp treatment; ring stays; used/total as `bodyMedium` mono under the value; free-space + fill-rate as two contained MetricPills.
- **Grouping:** Overview (hero + breakdown) → History → Cleanup tools (SectionHeader, borderless ActionCards at 8dp gaps) → Details & quick actions.
- **Spacing:** cleanup tool gap 8dp keep; 24dp before each section header.
- **Reuse:** ActionCard restyle shared with any future tool entries; SegmentedBar upgrade shared with legend.
- **High-impact:** (1) borderless `surfaceContainerHigh` ActionCards; (2) 16dp-tall SegmentedBar with 10dp legend dots — the storage screen suddenly matches Google's storage UIs.

### Cleanup
- **Keep:** category groups, checkbox flow, bottom bar projection, success overlay, 56dp divider inset in the file list (dividers are correct here — long uniform list).
- **Too dense:** group headers at 8dp vertical padding feel tight; filter chips collide with the list.
- **Hero:** none — correct; don't add one.
- **Grouping:** wrap the whole result list in one list-group card (20dp radius) OR keep edge-to-edge list but give group headers 12dp vertical padding and `titleSmall` prominence. Prefer the second (cheaper, keeps LazyColumn perf).
- **Spacing:** chip row bottom spacer 8dp → 12dp; file row vertical padding 8dp → 12dp.
- **Reuse:** bottom bar delete button becomes pill-shaped (full width, 56dp, stadium).
- **High-impact:** (1) pill delete button + roomier rows; (2) 12dp-padded group headers — the list feels like Files by Google.

### App Usage
- **Keep:** per-app card structure, progress bars, drain text.
- **Too dense:** every app is its own card at 8dp gaps → striped wall.
- **Hero:** none needed.
- **Grouping:** merge all app rows into **one list-group card** with rows (icon 40dp, 12dp vertical padding, progress bar under label), no dividers — spacing only. Top 3 apps could keep slightly larger rows.
- **Spacing:** 20dp card padding, rows 12dp apart.
- **Reuse:** StateCard for permission/error/empty (already close).
- **High-impact:** one grouped card instead of N small cards — instantly calmer and more modern.

### Charger Comparison
- **Keep:** FAB (very M3 — keep it), comparison bars, charger cards.
- **Too dense:** three near-identical cards stacked; bar rows at 8dp gaps.
- **Hero:** selected-charger card acts as hero-lite: `surfaceContainerHigh`, 20dp radius, no `BgCardDeep` (reserve that for real heroes).
- **Grouping:** comparison card = chart-card grammar (title, subtitle, bars with 12dp row gaps); charger list = one card per charger at 8dp gaps (they're peers in a group).
- **Spacing:** bar height 8dp → 12dp pill bars; per-row gap 8dp → 12dp.
- **Reuse:** OutlinedButton → pill; add-charger dialog keeps large shape → `heroShape` 28dp like other dialogs.
- **High-impact:** thicker pill comparison bars — the card starts resembling Google Health's energy chart.

### Insights
- **Keep:** InsightRow anatomy (icon circle, title, body, dismiss), count text, deep links.
- **Too dense:** 24dp screen padding (inconsistent) with 8dp row gaps.
- **Hero:** none — correct.
- **Grouping:** rows at 8dp gaps as one visual group; unseen badge stays.
- **Spacing:** screen padding → 16dp; row internal padding 16dp → 20dp; icon circle 48dp with priority-tinted *icon*.
- **Reuse:** identical row on Home; StateCard for empty ("all clear" with healthy dot) and error.
- **High-impact:** 16dp screen padding + 20dp row padding — small change, screen falls in line with the rest.

### Learn
- **Keep:** topic grouping, article cards, read time, cross-links.
- **Too dense:** 4dp between sections is far too tight; cards at 8dp gaps with 16dp padding feel like search results.
- **Hero:** none.
- **Grouping:** 24dp between topic sections; article cards 8dp apart within a topic.
- **Spacing:** article card padding 16dp → 20dp; title/preview gap 4dp → 8dp.
- **Reuse:** CrossLinkButton pill treatment in article details; related-articles section uses standard SectionHeader rhythm.
- **High-impact:** section spacing 4dp → 24dp — one-line change, the screen stops feeling like a dump of links.

### Settings
- **Keep:** grouped card layout, toggles, sliders, radio rows, confirmation dialogs, dividers *between subsections* (Settings is the one screen where divider language is native M3).
- **Too dense:** every section identical 16dp cards at 12dp gaps; slider blocks touch dividers.
- **Hero:** none.
- **Grouping:** keep one card per section but 20dp padding, 16dp gaps between cards, `CardSectionTitle` with 8dp below; inside a card, prefer 12dp spacing between control groups and keep dividers only between mixed control types (e.g. radio group | help row).
- **Spacing:** toggle rows min height 48dp → 56dp for master toggles; slider value row gets 8dp below before the slider.
- **Reuse:** destructive rows keep `error` color (correct); Pro purchase button becomes pill.
- **High-impact:** 56dp master toggle rows + fewer intra-card dividers — reads like Pixel Settings.

### Pro Upgrade
- **Keep:** 24dp screen padding, centered funnel layout, feature list, one-time-purchase note, trial sheets/modals.
- **Too dense:** nothing much — it's the airiest screen already.
- **Hero:** the headline block is the hero; give feature rows a list-group card (`surfaceContainerHigh`, 20dp radius, 16dp padding) instead of floating on the page.
- **Grouping:** headline → feature card → price button → notes.
- **Spacing:** keep 32dp rhythms.
- **Reuse:** buy button → pill 56dp; trial sheet top corners → 28dp.
- **High-impact:** feature list inside a tonal card + pill buy button — the purchase screen looks like a Play-store-era upsell.

### Fullscreen Chart (content area only)
- **Keep:** edge-to-edge chart, landscape orientation, fullscreen chart style table (3dp stroke etc.), quality zones.
- **Too dense:** chip gap 4dp; controls hug the chart.
- **Hero:** the chart is the hero; no card around it — correct, keep it surface-on-background.
- **Grouping:** controls row → 8dp chip gaps, 8dp below before the chart.
- **Spacing:** content horizontal padding 8dp is fine in landscape.
- **Reuse:** empty state → StateCard (max width 420dp, keep).
- **High-impact:** 8dp chip gaps + StateCard empty state; otherwise leave this screen alone.

---

## 7. Density and Hierarchy Rules

Concrete rules a developer can apply without guessing:

1. **Spacing ladder:** 4dp = label→value inside a metric; 8dp = items within one tight group (chips, grid cards in the same row, cards in the same group); 12dp = blocks inside a card; 16dp = unrelated cards; 24dp = above every `SectionHeader` / between major sections; 32dp = bottom-of-screen spacer only.
2. **Padding ladder:** 12dp = nested inner blocks (`surfaceContainerHigh`, 12dp radius); 16dp = compact rows, secondary/info cards, list-group cards (vertical 8dp); 20dp = default content card; 24dp = hero cards and full-screen state layouts.
3. **Radius ladder:** 8dp = thumbnails and tiny surfaces; 12dp = nested blocks inside cards; 20dp = every standard card; 28dp = hero, dialogs, bottom-sheet tops; pill (50%) = buttons, chips-adjacent badges, CrossLink, trailing chevron containers.
4. **Full card vs row-in-card:** an item gets its own card only if it is (a) the screen hero, (b) independently dismissible (insight, InfoCard), or (c) a peer in a deliberate card group (charger cards, learn articles). Everything else is a row inside a grouped card.
5. **Dividers:** only inside a card with **5 or more uniform rows** (settings sections, details tables, cleanup file list). Never between exactly two blocks — use 12dp spacing. Never adjacent to a chart.
6. **Icon circles:** navigation rows and action cards get 40–48dp circles; value rows and metric tables get bare 20dp icons or none. Never put an icon circle inside a hero.
7. **Status color — use it when:** the element *is* the status (ring progress, status label, status dot, chart zone, HeatStrip, alert accent). **One status carrier per component.**
8. **Status color — don't use it for:** numeric values (stay `onSurface`), icon circle backgrounds, card backgrounds, more than one element in the same card, or anything currently in the healthy range (healthy can just look normal; consider showing teal only in the hero and dots).
9. **Numbers:** JetBrains Mono values default `onSurface`; labels always `onSurfaceVariant`; unit glyphs one step smaller than the value (already the pattern — keep).
10. **Avoiding noisy diagnostics:** never show more than one chart per viewport-height of scroll; cap hero metric rows at 3 pills; move overflow metrics into the details table below; prefer "value + one-word label" blocks over label:value rows when there are ≤4 metrics.
11. **States:** every async region renders loading/empty/error/locked inside the same card silhouette it occupies when successful. No bare spinners on the page background except full-screen first load.
12. **Tap affordance:** if it navigates, it has either a chevron pill or is a pill itself. If it has neither, it should not navigate.

---

## 8. Before / After Examples

### 8.1 Home health hero
- **Current pattern:** `BgCardDeep` card, 16dp radius, 24dp padding, SectionHeader, 64sp score, category bar (6dp segments), breakdown rows with status dots and mono values, all in one card.
- **Problem:** 16dp radius makes the most important card look like every other card; breakdown rows inside the hero add table density to what should be a single calm statement.
- **Proposed M3 pattern:** hero = score + status summary + category bar only, at 28dp radius. Breakdown rows move to a standard 20dp-radius card directly below (8dp gap — same group), rows 48dp min height with dot + label + mono value, no dividers.
- **Exact implementation notes:** add `heroShape = RoundedCornerShape(28.dp)` to `Shapes.kt`/theme extension; in HomeScreen split the hero composable at the category bar; breakdown card uses `runcheckCardColors()`, `Spacing.cardInner (20dp)`, `Arrangement.spacedBy(0.dp)` with 12dp row padding.

### 8.2 Home quick status grid (GridCard)
- **Current pattern:** 2×2 GridCards, 16dp radius, 4dp left status strip, 36dp icon circle, title + mono subtitle + status label, asymmetric 16/12dp padding.
- **Problem:** four colored strips + four status labels = eight status signals above the fold; strips read as "monitoring dashboard".
- **Proposed M3 pattern:** 20dp radius, no strip, uniform 16dp padding, 40dp icon circle with status-tinted *icon*, status label keeps its color — one carrier per card (icon tint and label share the same hue, reading as one signal).
- **Exact implementation notes:** delete the `StatusStrip` modifier usage in GridCard (keep the modifier for other uses if any); set `contentPadding = 16.dp` uniformly; pass `tint = statusColor` into `IconCircle`'s icon, background stays `iconCircleColor`.

### 8.3 Storage cleanup tools (ActionCard)
- **Current pattern:** outlined 1dp cards, 16dp radius, 40dp icon circle, title/subtitle, trailing `TextButton` with arrow, 8dp gaps.
- **Problem:** borders + text buttons are pre-M3 language; the trailing button duplicates the card's own tap target.
- **Proposed M3 pattern:** borderless `surfaceContainerHigh` cards, 20dp radius, 48dp icon circle with category-tinted icon, chevron in a small pill container at the trailing edge, whole card clickable.
- **Exact implementation notes:** in `ActionCard`, drop `border = runcheckOutlinedCardBorder()`, set `colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)`; replace the TextButton with the ListRow chevron pattern (`Surface(shape = extraLarge, color = surfaceVariant.copy(alpha = 0.35f))` + 20dp arrow); onClick moves to the Card.

### 8.4 Settings cards
- **Current pattern:** identical 16dp-padded `surfaceContainer` cards at 12dp gaps; rows at 48dp min height; dividers between nearly every row; sliders inline with dividers above and below.
- **Problem:** wall of same-weight cards + hairlines everywhere = legacy preference screen.
- **Proposed M3 pattern:** 20dp radius and padding, 16dp gaps between section cards; master toggles at 56dp; dividers only between control-type changes (radio group → help row), not between siblings; sliders get 8dp headroom.
- **Exact implementation notes:** in `SettingsCard` change padding to 20dp; in `SettingsToggle` add a `prominent: Boolean` param mapping to `defaultMinSize(minHeight = 56.dp)`; audit `SettingsDivider` call sites and delete those between same-type rows; keep DataStore logic untouched.

### 8.5 Speed Test result metrics
- **Current pattern:** metrics card 24/16dp padding, two metric rows split by a divider; download/upload values in primary color.
- **Problem:** divider-split rows read as a form; primary-colored values compete with the gauge.
- **Proposed M3 pattern:** 2×2 grid of contained metric blocks (`surfaceContainerHigh`, 12dp radius, 12dp padding, 8dp gaps): Download, Upload, Ping, Jitter. Values `onSurface` mono; labels `bodySmall`. The gauge remains the only accent-colored element.
- **Exact implementation notes:** build a `MetricBlock` (contained MetricPill variant) composable in `ui/components/`; two `Row`s of two blocks with `Modifier.weight(1f)` and `Arrangement.spacedBy(8.dp)`; remove the divider; reuse the same composable for chart `ChartStatsRow` and thermal metrics.

---

## 9. Compose Implementation Plan

### Phase 1 — Token cleanup and shared component updates
Files: `Shapes.kt`, `Spacing.kt`, `UiTokens.kt`, `Theme.kt` (extensions), `ui/components/` (IconCircle, MetricPill, ConfidenceBadge, ProBadgePill).
- Add `heroShape` (28dp), bump `large` 16→20dp, `medium` 8→12dp.
- Add `Spacing.cardInner = 20.dp`; document the spacing ladder in comments.
- Bump `iconCircle` 44→48, `iconCircleInner` 22→24, `compactIconCircle` 36→40.
- Button shape default → pill: introduce a shared `RuncheckButtonDefaults` (shape = CircleShape) and sweep filled/outlined/text buttons.
- Add contained `MetricBlock` variant of MetricPill.
- Verify: `assembleDebug`, then eyeball Home/Battery — radius and icon changes propagate via theme, so most screens update for free.

### Phase 2 — Card and spacing system updates
Files: `GridCard`, `ListRow`, `ActionCard`, `InfoCard`, `CrossLinkButton`, `ProFeatureCalloutCard`, `SegmentedBar(+Legend)`, `SegmentedStatusBar`, `HeatStrip`, chart host sections in battery/network/thermal/storage screens.
- Remove GridCard strip; ActionCard borderless restyle; ListRow 56dp nav variant + icon circles.
- SegmentedBar 16dp / HeatStrip 32dp / status bar 8dp geometry bumps.
- Establish card gap 16dp / group gap 8dp / section 24dp in each screen's scroll column (mostly changing `Arrangement.spacedBy` and spacer values).

### Phase 3 — Home and detail screen layout refinements
Files: `ui/home/`, `ui/battery/`, `ui/network/`, `ui/thermal/`, `ui/storage/`, `ui/network/` speed test.
- Hero normalization: 24dp padding + heroShape on all five heroes; split Home health hero/breakdown; hero metric rows → max 3 `MetricBlock`s.
- Divider removal passes per screen (≤4-row cards); Network details split into two cards; Thermal/Speed 2×2 metric grids.
- Section headers with 24dp rhythm on Battery/Storage.

### Phase 4 — State surfaces and Pro/locked/empty/error polish
Files: `ui/components/` (new `StateCard`), `ProFeatureLockedState`, cleanup screens, app usage, insights, fullscreen chart empty state.
- Build `StateCard(icon, title, body, action)`; sweep empty/error/permission states to it.
- Locked states: keep Battery's blurred-preview pattern, apply the pill OutlinedButton and 48dp lock icon circle consistently.
- Cleanup: pill delete button, 12dp rows/headers; App Usage: single grouped card.

### Phase 5 — Final consistency pass
Files: all screens; `docs/ui-reference.md` and `docs/ui-consistency-audit.md` updates.
- Audit against Section 7 rules: one status carrier per component, spacing ladder, divider rule, radius ladder.
- Insights screen padding 24→16dp; Learn section spacing; chip-row padding sweep.
- Update UI-SPEC.md / ui-reference docs from source; run ktlint/detekt/lint and `assembleDebug`; screenshot compare Home, Battery, Storage, Settings before/after.

Suggested order matters: Phases 1–2 are shared code and give ~70% of the visual change; 3–5 are screen sweeps that can ship independently per screen.

---

## 10. Do-Not-Change List

- **Do not change app colors** — all hex tokens, the Material role mapping, and status color assignments stay exactly as defined in `Color.kt` / `StatusColors.kt`.
- **Do not change typography** — no font family, type scale, numeric style, or text style edits; only *where* existing styles are applied may change.
- **Do not change app content** — no renamed sections, removed metrics, new screens, or new features.
- **Do not change top bars** — `PrimaryTopBar` and `DetailTopBar` stay as they are.
- **Do not change status bar, navigation bar, bottom bar, or system bar handling** — insets, edge-to-edge, and the cleanup bottom bar mechanics remain untouched (only the delete button's shape changes).
- **Do not introduce dynamic color.**
- **Do not introduce a light theme or AMOLED variant.**
- **Do not redesign the product** — runcheck remains a dark, data-forward device health app; charts, gauges, mono numerals, and the oscilloscope animation language are identity, not debt.
- **Do not remove diagnostic density where the information is necessary** — every current metric survives; the fix is grouping (grouped cards, metric blocks, details tables) and hierarchy (one hero, one status carrier, spacing ladder), never deletion.
- **Do not touch chart drawing internals** — TrendChart/AreaChart/LiveChart rendering, animations, zones, tooltips, and reduced-motion behavior are out of scope; only their host cards change.

