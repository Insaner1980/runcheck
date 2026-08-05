# runcheck UI Redesign & Feature Expansion Plan

**Date:** 2026-07-24
**Scope:** Visual clarity overhaul, bottom navigation, Material 3 Expressive adoption (brand-preserving), light theme, weekly report, unused apps analysis, widget expansion, cleanup honesty audit.

---

## 1. Screenshot Analysis Findings

### runcheck today (22 screenshots reviewed)

**What works:**
- Strong, distinctive dark identity (deep teal palette, JetBrains Mono numerics)
- Data density is genuinely useful for the target user
- Confidence badges and status colors are consistent

**Problems identified:**
1. **Everything looks the same.** Nearly every screen is a vertical stack of identical `BgCard` rectangles with a caps-label header. There is no visual hierarchy between a hero metric, a data table, an info card, and a CTA — the eye has nowhere to land.
2. **Info/educational cards dominate.** On Battery and Thermal, dismissible info cards and "Related Articles" lists consume 40–60% of scroll length, pushing actual data far down. Thermal shows two full-height info cards before any metric grid.
3. **Home is doing too many jobs.** Health score + battery hero + 2×2 grid + quick tools + Pro status card = long scroll where the most-used actions (Speed Test, cleanup) are buried at the bottom.
4. **Chip row overload.** History sections stack two full chip rows (metric + period) that overflow horizontally ("24…", "M…" get cut). Filter UI is heavier than the charts themselves.
5. **Weak affordances.** Primary actions (Charger Comparison button, Speed Test "Open") are visually identical pill buttons floating mid-scroll; "Scan" links in Cleanup Tools are low-contrast text links.
6. **Flat monotony.** No shape variation, no tonal contrast between sections — the "recessed instrument panel" concept reads as uniform darkness on a real device.
7. **Charger Comparison empty state** is two grey cards + a lone FAB — feels unfinished.
8. **App Battery Usage** shows raw package names (`com.google.android.apps.nexuslauncher`) — needs label fallback handling.

### Avast Cleanup takeaways (30 screenshots reviewed)

Worth borrowing:
- **Bottom nav (4 tabs)** makes the app feel bigger and shallower at the same time — every major area is one tap away.
- **One dominant CTA per screen** (giant "Quick Clean" pill) — instant clarity about the primary action.
- **Hero card with a stacked segmented bar + legend with values** communicates "what you'll get" before any tap.
- **Bento-style stat tiles** (Apps Overview: big colored blocks with one number each) — scannable, playful, hierarchical.
- **"See tips" contextual links** instead of full-width educational cards — education is available, never in the way.
- **Weekly notifications/reports center** with per-category toggles — recurring value that keeps the app relevant.

Explicitly NOT borrowing: aggressive upsells, fake-urgency red badges, "optimizable photos" dark patterns, third-party data sharing toggles.

---

## 2. Design Direction: Brand-Preserving Material 3 Expressive

**Decision:** Keep the runcheck brand (teal/blue palette, Manrope + JetBrains Mono). Adopt M3 Expressive's shapes, motion physics, component set, and emphasized typography *on top of* the brand. No dynamic color.

### 2.1 Library baseline

- Compose BOM is already `2026.03.00`; verify the mapped `material3` version and bump BOM if needed so the Expressive APIs used below are available (target material3 **1.5.x**; `MaterialExpressiveTheme`, `expressiveLightColorScheme`, and FloatingToolbar are already stable-track, some components still need `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`).
- Add a single project-level opt-in via `freeCompilerArgs` for the experimental annotation rather than per-file opt-ins.

### 2.2 Theme architecture rework (`ui/theme/`)

1. Replace `MaterialTheme { }` entry with `MaterialExpressiveTheme(colorScheme, motionScheme = MotionScheme.expressive(), shapes, typography)`.
2. **Introduce full light + dark ColorSchemes.** Current `Color.kt` maps brand hexes directly to a single dark scheme. Refactor to:
   - `DarkColors` — existing palette unchanged.
   - `LightColors` — new palette derived from brand hues:
     - Background `#F4F7F8` (cool off-white), surface `#FFFFFF`, surfaceContainer `#E9EFF1`, surfaceContainerHigh `#DEE7EA`
     - Primary stays blue but darkened for contrast: `#2B7BB9` (AccentBlue fails 4.5:1 on white)
     - Status colors darkened variants: Healthy `#0E9B7E`, Fair `#A8860B`, Poor `#C46A10`, Critical `#D0442A`
     - Text: `#16262C` primary, `#4E6570` secondary, `#6E838C` muted
   - All exact light values to be contrast-verified (WCAG AA 4.5:1 body / 3:1 large) before implementation — treat the above as starting points.
3. **StatusColors extension** becomes theme-aware: two `StatusColorScheme` instances provided via the same CompositionLocal, selected by theme.
4. **Chart colors:** audit `ChartHelpers`, `TrendChart`, `AreaChart`, `LiveChart`, `HeatStrip`, `SegmentedBar` for hardcoded dark-assuming colors (grid alpha, fill alphas, glow effects). Glow/scan-line effects need reduced alpha on light backgrounds.
5. **Theme setting:** `Display → Theme: System / Light / Dark` stored in DataStore (`UserPreferencesRepository`). Default **System**. Read in `MainActivity` before `setContent` via cached synchronous read (same pattern as `pro_status_cache`) to avoid a wrong-theme flash.
6. **Shapes:** adopt M3E expanded shape tokens. Cards stay 16dp; introduce larger radii (24–28dp) for hero cards and the new bottom nav active indicator; use M3E shape morphing on the Speed Test start button (circle → rounded square during test).
7. **Typography:** keep Manrope/JetBrains Mono but adopt M3E *emphasized* styles for hero numerics — slightly tighter tracking and heavier weights on `numericHeroDisplayTextStyle` states (e.g., weight animates up when a value goes critical).
8. **Motion:** replace bespoke `MotionTokens` durations with `MaterialTheme.motionScheme` (spatial/effects springs) where components animate; keep chart "Instrument Sweep" as a brand signature but retune easings to M3E defaults. `reducedMotion` support carries over.
9. **Widgets & notifications:** Glance widgets and the live notification get light/dark variants matching system theme (Glance supports day/night color providers).

### 2.3 Component adoption map

| M3E component | Where it goes |
|---|---|
| `NavigationBar` (short/flexible) | New bottom nav, 4 destinations |
| Button groups (connected) | Replaces metric+period chip-row pairs in all History sections |
| Split button | Cleanup tool rows ("Scan" primary + filter dropdown) |
| FAB menu | Charger Comparison (add charger / start session) |
| Loading indicator (expressive) | Speed test phases, cleanup scanning |
| Floating toolbar | Cleanup selection mode (select all / delete) |
| Large flexible top app bar | Detail screens — large title collapses on scroll, replaces bare back-arrow rows |
| Expressive progress indicators | ProgressRing gains M3E wavy determinate style option for the health hero |

---

## 3. Information Architecture: Bottom Navigation

**Decision:** 4-tab bottom nav — **Home / Insights / Tools / Settings**. Detail screens (Battery, Network, Thermal, Storage, etc.) remain push destinations *above* the nav bar level.

### 3.1 Structure

```
BottomNav
├── Home          — health hero, 4 category cards, top insight, monitoring banner
├── Insights      — existing Insights screen (badge = unseen count)
├── Tools         — Speed Test, Cleanup hub, Charger Comparison, App Usage, Learn, Export
└── Settings      — existing settings + Pro section

Push (full-screen over nav, nav bar hidden):
Battery / Network / Thermal / Storage details, Cleanup/{type}, Speed Test run,
Learn article, Fullscreen chart, Pro upgrade
```

### 3.2 Implementation notes

- Rework `RuncheckNavHost`: top-level destinations use `popUpTo(startDestination) { saveState = true }` + `restoreState = true` (standard multi-back-stack pattern) so tab state survives switching.
- `Screen` sealed class gains a `topLevel: Boolean`; nav bar visibility derived from current destination.
- Deep links / notification routes (`Screen.directRoutes`) re-validated: notification taps land on the correct tab back stack.
- Predictive back: verify tab → Home → exit behavior on Android 17.
- Widget/notification intents currently targeting `home` keep working; ones targeting detail screens get parent-tab back stack via `TaskStackBuilder`-equivalent nav graph hierarchy.

### 3.3 Screen decluttering (the "selkeytys" work)

**Home (biggest win):**
- Keep: health hero (M3E wavy ring), 2×2 category grid (battery gets a grid card like the others — remove the separate battery hero card), single top insight card, monitoring-stale banner.
- Remove from Home: Quick Tools card (→ Tools tab), Pro/trial status cards (→ Settings; trial countdown becomes a slim banner only during active trial), battery hero card.
- Result: Home fits ~1.5 screen heights instead of ~3.

**Detail screens (Battery, Network, Thermal, Storage):**
- **Related Articles:** collapse the 3–4 stacked link cards into one row: "Learn: Battery →" leading to the filtered Learn topic. Frees 200–400dp per screen.
- **Info cards (Tier 2):** max one visible at a time per screen; render as compact single-line banner with expand affordance instead of full paragraph cards.
- **History filter rows:** metric selector + period selector become one connected button group each, horizontally scrollable with edge fade instead of clipping.
- **Section rhythm:** introduce tonal variation — hero on `surfaceContainerLow` (deep), data cards on `surfaceContainer`, one accent-tinted CTA card max per screen. Section headers get M3E emphasized label style.
- **Charger Comparison empty state:** proper empty-state illustration + inline "Add your first charger" primary button (FAB menu appears only once ≥1 charger exists).
- **App Usage:** fall back to `PackageManager.getApplicationLabel`, else prettified package tail; never show raw package names.

---

## 4. New Features

### 4.1 Weekly Report notification (builds on existing Room data)

- New `WeeklyReportWorker` (unique periodic work, 7-day interval, runs alongside existing workers; respects existing notification master toggle + new per-report toggle in Settings → Notifications, default **off** — opt-in like Live Notification).
- Content assembled by new `GenerateWeeklyReportUseCase` from existing DAOs: battery health trend + avg drain, charged/discharged totals, storage delta ("+3.2 GB this week"), top 3 battery apps (Pro), thermal events count, speed test best/avg.
- Notification (BigTextStyle) taps into a new **Weekly Report screen** (push destination, route `weekly_report`) rendering the same data as bento-style stat tiles (Avast Apps Overview pattern, runcheck palette).
- Report history: no new table needed initially — report is regenerated from Room on open; a `weekly_report_last_shown` DataStore key prevents duplicates.
- Free tier: battery + storage summary. Pro: full report. (Consistent with existing extended-history gating.)

### 4.2 Unused Apps analysis (extends App Usage)

- Uses existing `PACKAGE_USAGE_STATS` permission and app-usage snapshots — **no new permissions**.
- New `DetectUnusedAppsUseCase`: launchable apps (existing launcher-visibility query) with zero foreground time in N days (30 default, chip: 30/60/90), joined with app size from `StorageStatsManager` when available.
- UI: new section/tab inside App Usage screen — "Not used in 30 days" list with app icon, last-used date, size, and an **Uninstall** button firing `Intent(ACTION_DELETE, package:…)` (system confirmation dialog; no special permission).
- Limitations to surface honestly: usage stats availability varies per OEM; apps installed <N days ago excluded; system apps excluded (cannot be uninstalled — offer "Disable" deep link to app info instead: `ACTION_APPLICATION_DETAILS_SETTINGS`).
- Pro-gated (consistent with App Usage being Pro).

### 4.3 Widget expansion

- **New "Quick Glance" widget (4×2):** health score + battery % + free storage + temp in a bento layout; taps deep-link to respective tabs/details.
- **Restyle existing Battery + Health widgets** to M3E: larger corner radii, day/night Glance color providers (pairs with the new light theme), bigger numerics.
- Keep widgets Pro-gated as today; `WidgetDataProvider` extended for the combined snapshot.
- Glance 1.1.1 is current in the project; check for a newer Glance with M3E theming support before implementation.

### 4.4 Cleanup honesty audit (verified against code, 2026-07-24)

**Finding:** runcheck's cleanup is already honest. There is **no app-cache-clearing code** anywhere (`clearCache`/`CLEAR_APP_CACHE`/`freeStorageAndNotify` absent). Cleanup only deletes user-visible files via MediaStore (`createDeleteRequest` on API 30+): Large Files, Old Downloads, APK files, and Trash emptying. Cache appears only as a read-only statistic from `StorageStatsManager`. This is exactly right — on modern Android (11+), third-party apps **cannot** clear other apps' caches (the `CLEAR_APP_CACHE` permission path Avast-type apps used is dead; "free up space" claims in those apps largely deep-link to system UI or delete the same MediaStore files we already handle).

**Plan (small, wording + UX only):**
1. **Fix one overpromising string:** `info_storage_cache_matters` says "It's safe to clear and can recover significant space" — implies runcheck can clear it. Reword to: cache is managed by Android and each app; runcheck shows it for awareness; per-app cache can be cleared from system App info.
2. **Add honest deep links** in the cache info sheet and Storage Quick Actions: `ACTION_INTERNAL_STORAGE_SETTINGS` (already present as "Storage Settings") + per-flow note "Android does not allow apps to clear other apps' caches — this opens system tools that can."
3. **New Learn article:** "Why cleaner apps can't really clean caches anymore" — differentiator content that positions runcheck's honesty against Avast/CCleaner-type marketing. Cross-link from Storage.
4. **Marketing guardrail:** Play Store listing and in-app copy never claim cache cleaning, RAM boosting, or speed-up. Cleanup copy stays scoped to "your files": large files, old downloads, leftover APKs, trash.

---

## 5. Phasing & Verification

| Phase | Content | Depends on |
|---|---|---|
| **1. Theme foundation** | Light/dark ColorSchemes, theme-aware StatusColors, `MaterialExpressiveTheme`, motion scheme, theme setting UI, chart color audit, widget/notification theming | — |
| **2. Navigation** | Bottom nav, nav graph rework, tab state saving, deep-link revalidation | 1 |
| **3. Declutter** | Home slim-down, detail screen cleanups, related-articles collapse, button groups, top app bars, component adoption map | 1–2 |
| **4. Features** | Weekly report, unused apps, cleanup wording fixes + Learn article | 1 (theme), independent of 2–3 |
| **5. Widgets** | Quick Glance widget, existing widget restyle | 1 |

Each phase ends with: `.\gradlew.bat assembleDebug`, targeted unit tests (theme mapping, nav route tests, new use cases), `lc` lint pass, manual light/dark screenshot review on Pixel 9, and WCAG contrast spot-checks on the light palette. Phase 1 and 2 are the risky ones — do them as separate PRs.

### Key risks

- **Light theme touches everything.** Hardcoded dark-assuming colors likely hide in charts, widget layouts, and notification icons. Budget a dedicated audit pass (grep for `Color(0x`, direct `BgCard*`/`Accent*` references outside `theme/`).
- **Expressive APIs still partly experimental** — pin exact material3 version, wrap experimental usage behind the single opt-in, re-verify on BOM bumps.
- **Nav rework regressions:** SavedStateHandle-backed selections (history periods, fullscreen chart results) must survive the back-stack restructure — existing ViewModel tests cover much of this; add nav-graph tests.
- **detekt/ktlint compose-rules** may flag new experimental APIs; update rule config as needed.

### Out of scope (explicitly)

- Dynamic color / Material You (rejected — brand palette kept)
- Smart cleanup expansion (screenshots/duplicates) — not selected
- Cache/RAM cleaning of any kind — impossible on modern Android, and against product values
- Finnish localization (separate effort; strings stay in `strings.xml` as required)
