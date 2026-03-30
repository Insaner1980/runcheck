# Insights Engine — Phase 2 Rules

Specification for the second batch of Insights Engine rules. Phase 1 delivered the architecture and `BatteryDegradationTrendRule`. This document describes the remaining rules that complete the Insights Engine.

All rules follow the same architecture established in Phase 1. Each rule queries Room data, checks significance thresholds, and produces an `Insight` if the observation is meaningful. No rule should surface noise — if the pattern is weak or the data is insufficient, the rule produces nothing.

---

## Context: Available Data

Rules can query from these Room tables and in-memory sources:

- **Battery readings** — level, voltage, temperature, current (with confidence), charge status, plug type, health, cycle count, health percentage
- **Network readings** — connection type, signal dBm, Wi-Fi speed/frequency, carrier, latency
- **Thermal readings** — thermal status from PowerManager API, battery temperature
- **Storage readings** — used/available bytes, media breakdown by category
- **Throttling events** — timestamps and thermal status when throttling was detected
- **Charger profiles and sessions** — named chargers, per-session current/voltage/power/time-to-full
- **App usage snapshots** — per-app foreground time, collected periodically by HealthMaintenanceWorker
- **Speed test results** — download/upload Mbps, ping, jitter, connection type, signal strength
- **Screen state** — screen-on/off timestamps from ScreenStateTracker

The health score weights are: Battery 40%, Thermal 25%, Network 25%, Storage 10%.

---

## General Design Principles

**Minimum data before any insight fires.** Each rule defines how much history it needs. Rules must never produce insights from a single data point or a few hours of data. False insights are worse than no insights.

**Actionable over informational.** Prioritize insights where the user can actually do something. "Your battery dropped 3% overnight" is informational. "Your battery is draining 4x faster than usual during sleep — a background app may be preventing deep sleep" is actionable.

**One insight per rule evaluation, maximum.** If a rule detects multiple patterns, pick the most significant one. The engine already enforces max one new insight per day across all rules.

**Plain language.** Insight titles should be short (under ~8 words). Descriptions should be 1-2 sentences, no jargon. Use the same terminology the app already uses in its UI (e.g., "Cool / Normal / Warm / Hot / Critical" for thermal bands, "Healthy / Fair / Poor / Critical" for health score).

**Confidence-aware.** Rules that depend on battery current must check the confidence level of readings. If most readings in the evaluation window have LOW or UNAVAILABLE confidence, the rule should either skip the evaluation or note reduced reliability in the insight description.

---

## Rule 1: Thermal Pattern Detection

### What it detects

Recurring temperature spikes that follow a time-of-day pattern or correlate with sustained high thermal states.

### Trigger conditions

- The device has entered the Warm, Hot, or Critical thermal band at least 3 times in the past 7 days
- These events cluster around a similar time window (within a 2-hour bracket) on at least 3 of those days

OR:

- Average battery temperature over any 4-hour window in the past 7 days exceeds 38°C on at least 3 separate days

### Minimum data requirement

7 days of thermal readings with at least 4 readings per day (based on the monitoring interval).

### Significance threshold

Skip if the device only entered the Warm band briefly (under 10 minutes per event). The pattern must represent sustained or repeated thermal load, not transient spikes from brief camera use or similar.

### Insight output

- **Title example:** "Afternoon heat pattern"
- **Description example:** "Your device consistently runs warm between 2–4 PM. This may be caused by apps running heavy tasks during that window. Sustained heat accelerates battery wear."
- **Actionability:** The user can check which apps are active during that time window (via App Usage if Pro), or simply be aware that this period is thermally stressful.

### Cross-category value

Thermal patterns directly affect battery degradation speed. If this rule fires alongside a battery degradation trend, the insight description should reference the connection: heat accelerates chemical aging of the battery.

---

## Rule 2: Storage Growth Projection

### What it detects

The rate at which storage is filling up, projected forward to estimate when the device will reach critical levels (90%+ usage).

### Trigger conditions

- Storage usage has grown by at least 2 percentage points over the evaluation window
- The projected fill date (based on linear growth rate) is within the next 90 days

### Minimum data requirement

14 days of storage readings. Growth rate is calculated as a simple linear regression over the available data points.

### Significance threshold

Skip if storage usage is currently below 50% and the projected fill date is more than 180 days away. Also skip if the growth rate is negative (user is freeing space).

### Insight output

- **Title example:** "Storage filling up"
- **Description example:** "At the current rate, your storage will be over 90% full in about 6 weeks. Photos and videos are growing fastest — the Cleanup tools can help free space."
- **Actionability:** Direct reference to the Cleanup tools available in Storage Detail. If the media breakdown data shows which category is growing fastest, mention it.

### Edge cases

- If the user recently did a large cleanup (sharp drop in storage), the projection should use only data after the cleanup event, not the full window. Detect this as a drop of more than 5 percentage points between consecutive readings.
- If storage is already above 90%, skip the projection and instead produce a more urgent insight: "Storage is critically low at X%. Performance and app updates may be affected."

---

## Rule 3: Network Signal Quality Patterns

### What it detects

Consistent differences in network signal quality across time periods, or a general downward trend in signal quality over weeks.

### Trigger conditions

Pattern A — Time-of-day variation:
- Signal quality during one 4-hour window is consistently 10+ dBm worse than another 4-hour window, measured over at least 5 days

Pattern B — Degradation trend:
- Average signal strength has dropped by 5+ dBm when comparing the most recent 7 days to the prior 7 days, and the connection type has not changed (e.g., the user did not switch carriers or move to a new location)

### Minimum data requirement

14 days of network readings for Pattern A. 14 days (split into two 7-day windows) for Pattern B.

### Significance threshold

Only fire for cellular signal patterns. Wi-Fi signal is too dependent on physical position relative to the router to produce meaningful temporal patterns. However, if the user consistently has poor Wi-Fi signal (below -70 dBm average over 7 days), a separate "weak Wi-Fi" insight can fire.

### Insight output

- **Pattern A title example:** "Weaker signal in the evenings"
- **Pattern A description example:** "Your cellular signal is noticeably weaker between 6–10 PM compared to the rest of the day. This is common in areas with heavy network congestion during peak hours. Wi-Fi may give better results during this window."
- **Pattern B title example:** "Signal strength declining"
- **Pattern B description example:** "Your average cellular signal has dropped over the past two weeks. If you haven't changed location, this could indicate network changes in your area."
- **Weak Wi-Fi title example:** "Weak Wi-Fi signal"
- **Weak Wi-Fi description example:** "Your Wi-Fi signal has averaged below -70 dBm this week. Moving closer to the router or using the 5 GHz band (if available) may improve speed and stability."

### Cross-category value

Poor network signal increases battery drain because the radio amplifies power to maintain connection. If this rule fires and battery drain during the same time window is elevated, note the connection.

---

## Rule 4: Charger Performance Comparison

### What it detects

Differences in charging performance across the user's saved charger profiles, identifying underperforming chargers/cables.

### Trigger conditions

- The user has at least 2 named charger profiles
- Each compared profile has at least 3 completed charging sessions
- One charger delivers at least 25% less average power (watts) than the best-performing charger

### Minimum data requirement

3 completed charging sessions per charger profile being compared. A "completed" session means charging started below 30% and reached at least 80%, or the session lasted at least 30 minutes with measurable current.

### Significance threshold

Skip if the power difference is under 25% — small variations are normal due to battery temperature, charge level, and adaptive charging behavior. Also skip if current confidence is LOW for the majority of readings in any session being compared.

### Insight output

- **Title example:** "Slow charger detected"
- **Description example:** "Your 'IKEA USB-C' cable charges at roughly 10W compared to 22W from your 'Samsung 25W'. Charging takes about twice as long. The cable may not support fast charging."
- **Actionability:** Clear comparison with real names and approximate performance numbers. User can decide whether to replace the cable.

### Edge cases

- Wireless chargers are inherently slower than wired. Do not flag a wireless charger as "underperforming" compared to a wired one. Compare wireless chargers only against other wireless chargers.
- If only one charger profile exists, this rule cannot fire. It should not produce a "you only have one charger" insight — that is not useful.

---

## Rule 5: App Battery Impact Analysis

### What it detects

Apps with disproportionate battery consumption relative to their foreground usage time.

### Trigger conditions

- An app is in the top 3 by estimated battery drain but is NOT in the top 3 by foreground time
- The disproportion factor is at least 2x (e.g., the app accounts for 20% of estimated drain but only 10% of foreground time)

### Minimum data requirement

7 days of app usage snapshots with corresponding battery readings. Battery drain attribution is estimated by correlating battery level drops with foreground app periods.

### Significance threshold

Skip if total foreground time for the flagged app is under 10 minutes in the evaluation window — too little data for reliable correlation. Also skip system apps and pre-installed apps that the user cannot uninstall (they cannot act on the insight).

### Insight output

- **Title example:** "[App name] using extra battery"
- **Description example:** "Instagram used about 18% of your battery this week but was only active for 12% of your screen time. It may be doing significant background work."
- **Actionability:** The user can check the app's battery settings, restrict background activity, or be aware of the cost.

### Confidence note

Battery drain per-app is an estimate based on temporal correlation, not a direct measurement. The insight description should use language like "about" and "estimated" rather than precise percentages. If current confidence is mostly LOW, downgrade the precision further or skip the insight.

---

## Cross-Category Correlation Rules

These rules are the key differentiator. They look across multiple data categories to find connections that single-category tools cannot see.

### Rule 6: Heat-Accelerated Battery Wear

**Condition:** `BatteryDegradationTrendRule` (Phase 1) has detected declining battery health AND `ThermalPatternRule` has detected recurring high temperatures.

**Minimum data:** Both rules must have independently fired at least once in the past 30 days, OR the current evaluation finds both patterns simultaneously.

**Insight:** "Heat may be wearing your battery faster. Your battery health has dropped [X]% over [period], and your device regularly runs warm during [time window]. Reducing thermal load during that period could slow the decline."

**Why this matters:** No competitor connects these two observations. AccuBattery shows battery health. A thermal monitor shows temperature. Neither tells you the first is caused by the second.

### Rule 7: Network-Driven Battery Drain

**Condition:** Battery drain rate during periods of poor cellular signal (below -100 dBm) is at least 30% higher than drain rate during periods of good signal (above -85 dBm), measured over the same usage patterns (screen-on only, similar time-of-day windows).

**Minimum data:** 7 days with at least 2 hours of poor-signal time and 2 hours of good-signal time.

**Insight:** "Weak signal is costing you battery. When your cellular signal is poor, your battery drains about [X]% faster. Switching to Wi-Fi in low-signal areas can help."

**Why this matters:** Users blame apps for battery drain. Sometimes it is the radio. This correlation is invisible in any single-category view.

### Rule 8: Storage Pressure Impact

**Condition:** Storage usage is above 85% AND at least one of the following is true in the past 7 days:
- App crashes or force-stops have increased (detectable if app usage snapshots show abnormal session terminations)
- The health score's storage component has dropped below Fair

**Minimum data:** 7 days of storage readings showing sustained high usage.

**Insight:** "Low storage may be affecting performance. With only [X] GB free, your device has less room for app caches and system operations. Freeing up space through the Cleanup tools can help."

**Why this matters:** Users often do not connect "my phone is slow" with "my storage is 95% full." This insight bridges that gap.

---

## Insight Priorities

When multiple rules fire in the same evaluation cycle, use this priority order to decide which insight gets surfaced (only one new insight per day):

1. **Cross-category rules** (6, 7, 8) — highest value, unique to runcheck
2. **Actionable single-category** (4: Charger, 5: App Impact, 2: Storage Projection) — user can do something immediately
3. **Awareness single-category** (1: Thermal Pattern, 3: Network Signal) — informational, less immediately actionable

Within the same priority tier, prefer the rule with the highest significance score (biggest deviation from normal).

---

## Insights UI Behavior

No changes to the existing Insights UI structure from Phase 1. New rules produce insights that appear in the same card on the Home screen and the same Insights history screen.

Each insight should include:

- **Icon** — category-appropriate icon (battery, thermometer, signal, storage, charger, app). Cross-category insights use the health score icon.
- **Title** — short, under ~8 words
- **Description** — 1-2 sentences, plain language
- **Timestamp** — when the insight was generated
- **Category tag** — which category or categories the insight relates to (for filtering in the history view)
- **Deep link** — tapping an insight navigates to the most relevant detail screen (e.g., a thermal pattern insight opens Thermal Detail, a charger insight opens Charger Comparison)

---

## Test Data Seeder Updates

The existing `InsightTestDataSeeder` in the debug Settings panel needs expansion to support Phase 2 rules. It should be able to generate synthetic data that triggers each rule:

- Thermal readings with a recurring afternoon spike pattern
- Storage readings with a steady upward growth trend
- Network readings with time-of-day signal variation
- Multiple charger profiles with differing power levels
- App usage snapshots with one high-drain, low-usage app
- Combined patterns for cross-category rules (e.g., declining battery health + high thermal readings)

The seeder should offer a "Seed all Phase 2 patterns" option that populates enough data to trigger every rule, so the full Insights experience can be previewed without waiting weeks for real data to accumulate.

---

## Evaluation Schedule

All rules run inside the existing Insights Engine evaluation cycle. No changes to scheduling needed. The engine evaluates all registered rules in sequence and picks the highest-priority insight that passes its significance threshold.

New rules should be registered in the same way as `BatteryDegradationTrendRule` — the architecture already supports adding rules without modifying the engine itself.

---

## What This Does NOT Cover

- UI changes beyond what is described above (the Insights card and history screen already exist)
- Notification-based insight delivery (out of scope — insights are passive, discovered when the user opens the app)
- Machine learning or cloud processing (all analysis runs locally with simple statistical methods)
- Export of insights (covered separately by CSV export feature)
