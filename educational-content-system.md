# runcheck — Educational Content System

Spec for adding in-app educational content across three tiers: metric info sheets, contextual cards, and a dedicated Learn section.

**Style direction:** Material Design 3 (Android), matching existing runcheck dark theme and component library.

**Audience:** Everyone — each piece of content starts with a plain-language explanation, then offers deeper technical context for users who want it.

---

## Architecture: Three Tiers

### Tier 1 — Info Bottom Sheets (per metric)

Small `(?)` icon next to individual metrics. Tap opens a modal bottom sheet with a focused explanation of that one metric.

**Purpose:** "What does this number mean?"

**Where:** Next to any metric that uses technical terminology or units — voltage, dBm, cycle count, thermal headroom, jitter, etc.

**Structure per sheet:**
- **Title** — metric name (e.g. "Voltage")
- **Plain explanation** — 1–2 sentences, no jargon (e.g. "Voltage is how much electrical pressure your battery is producing right now. Think of it like water pressure in a pipe.")
- **What's normal** — concrete range with color context (e.g. "3.7–4.2 V is healthy. Below 3.5 V under normal use may signal a worn battery.")
- **Why it matters** — 1–2 sentences connecting to user experience (e.g. "Erratic voltage causes unexpected shutdowns and inaccurate battery percentage readings.")
- **Deeper detail** (optional, collapsed by default) — technical explanation for curious users

**Visual design:**
- Trigger: `InfoIcon` composable — small `(?)` icon (16dp, `onSurfaceVariant` color, 48dp touch target), placed inline after the metric label
- Bottom sheet: Material 3 `ModalBottomSheet`, max height 60% of screen
- Title: `titleMedium`, Manrope
- Body: `bodyMedium`, Manrope, `onSurface`
- "Normal range" highlight: subtle card-within-sheet using `surfaceContainerHigh` background, 8dp corner radius
- "Learn more" expander: `TextButton` style, toggles the deeper detail section with `AnimatedVisibility`
- Close: drag down or tap scrim

### Tier 2 — Contextual Info Cards (per screen section)

Inline cards within detail screens that explain broader concepts relevant to what the user is looking at. Not tied to a single metric — tied to a section or situation.

**Purpose:** "Why does this matter? What should I do?"

**Where:** Placed within detail screen content flow, visually distinct from data cards but not intrusive.

**Structure per card:**
- **Headline** — short, benefit-oriented (e.g. "Why your phone slows down when it's hot")
- **Body** — 3–5 sentences explaining the concept in plain language, connecting observed data to real-world impact
- **Dismissible** — user can close it, preference stored locally (Room or DataStore). Once dismissed, stays dismissed. No "don't show again" dialog — just an X.
- **Not Pro-gated** — educational content is always free

**Visual design:**
- Card background: `surfaceContainerHigh` (slightly elevated from surrounding `surfaceContainer` data cards)
- Left accent border: 3dp, `primary` color (`#4A9EDE`)
- Icon: `InfoOutlined` (20dp) in `primary` color, top-left of card
- Headline: `titleSmall`, Manrope
- Body: `bodySmall`, `onSurfaceVariant`
- Dismiss: `IconButton` with `Close` icon (16dp), top-right corner
- Corner radius: 16dp (matching existing cards)
- Padding: 16dp
- Placement: between existing panels/cards in the LazyColumn, with standard 12dp vertical spacing

### Tier 3 — Learn Section (standalone screen)

A dedicated screen accessible from the Home screen, containing curated articles organized by topic. Longer-form educational content that isn't tied to a specific metric or screen.

**Purpose:** "I want to understand how my phone works."

**Where:** Home screen — new card in the grid or below Quick Tools. Navigate via `learn` route. Also back-linked from Tier 1 and Tier 2 where relevant.

**Structure:**
- Topic-based grouping (Battery, Thermal, Network, Storage, General)
- Each article: title, 2-line preview, estimated read time
- Article content: scrollable, formatted with headers and paragraphs (no complex markup needed — styled text in Compose)
- Cross-links to relevant detail screens where applicable ("Check your battery health →")

**Visual design:**
- Home entry point: `ActionCard` with `School` or `MenuBook` icon (Material Icons), `primary` color IconCircle. Label: "Learn" or "Device Guide". Subtitle: "Understand your phone's health"
- Learn screen: `DetailTopBar` with title "Learn"
- Topic groups: `CardSectionTitle` per group
- Article cards: standard card style, tap navigates to article detail
- Article detail screen: `DetailTopBar` with article title, body text in `bodyMedium`, section headers in `titleSmall`
- Cross-link buttons: `TextButton` with forward arrow, navigates to relevant app screen

---

## Navigation Update

```
Home
├── Battery Detail
│   └── Charger Comparison [PRO]
├── Network Detail
│   └── Speed Test
├── Thermal Detail
├── Storage Detail
│   └── Cleanup (Large Files / Old Downloads / APK Files)
├── App Usage [PRO]
├── Learn                          ← NEW
│   └── Article Detail             ← NEW
├── Settings
└── Pro Upgrade
```

---

## Tier 1: Info Bottom Sheets — Full Inventory

### Battery Detail Screen

| Metric | Plain explanation | Normal range | Why it matters |
|--------|-------------------|-------------|----------------|
| Voltage | How much electrical pressure the battery produces. Like water pressure in a pipe. | 3.7–4.2 V is healthy. Below 3.5 V during normal use suggests wear. | Unstable voltage causes percentage jumps and unexpected shutdowns. |
| Temperature | How warm the battery is right now. | 20–35°C is ideal. Above 40°C is concerning. Above 45°C is actively harmful. | Heat accelerates permanent battery wear. A hot battery during light use signals a problem. |
| Health Status | Android's own assessment of your battery's condition. | "Good" means normal operation. | Statuses like "Overheat" or "Dead" indicate serious issues requiring attention. |
| Cycle Count | How many full charge-discharge cycles the battery has completed. Using 50% twice equals one cycle. | Most batteries last 300–500 full cycles before noticeable wear. | Higher cycle counts correlate with reduced capacity. Useful for estimating remaining battery lifespan. |
| Health % | What percentage of original battery capacity remains. A 5,000 mAh battery at 80% health effectively holds 4,000 mAh. | Above 80% is good. 70–80% is noticeable. Below 70% significantly impacts daily use. | The single most important indicator of whether the battery needs replacement. |
| Capacity (Estimated / Design) | Design capacity is what the battery had when new. Estimated is what it can hold now. | Estimated should be close to design when new, decreasing over time. | The gap between these two numbers shows how much capacity has been permanently lost. |
| Current (mA) | How much electrical current is flowing in or out of the battery right now. Positive means charging, negative means discharging. | Varies by activity. Light use: 200–400 mA drain. Fast charging: 2000–4000+ mA. | Unusually high drain during light tasks points to a rogue app or hardware issue. |
| Power (W) | How much total power is being drawn or delivered, combining voltage and current. | Light use: 1–3 W. Gaming/heavy use: 5–10 W. Fast charging: 15–45+ W. | Sustained high wattage generates heat and accelerates battery wear. |
| Drain Rate (%/h) | How quickly the battery percentage is dropping per hour at current usage level. | 3–8%/h during active use is typical. Under 1%/h during idle is healthy. | Helps distinguish normal usage drain from abnormal background drain. |
| Confidence Badge | How reliable the current reading is on this specific device. Some phones report accurate mA values, others estimate. | "Accurate" means hardware-level measurement. "Estimated" means software approximation. | runcheck is transparent about measurement reliability — competing apps often show estimated values without disclosing this. |
| Screen On / Screen Off drain | Battery drain rate separated by whether the screen was on or off. | Screen-off drain should be significantly lower than screen-on. | High screen-off drain means something is keeping the phone awake — a rogue app, frequent notifications, or poor signal forcing the radio to work harder. |
| Deep Sleep / Held Awake | How much time the phone spent in deep power-saving mode vs. being kept active while the screen was off. | Deep sleep should dominate screen-off time. | If "Held Awake" exceeds "Deep Sleep", background processes are preventing the phone from resting, draining battery unnecessarily. |

### Thermal Detail Screen

| Metric | Plain explanation | Normal range | Why it matters |
|--------|-------------------|-------------|----------------|
| CPU Temperature | How hot the main processor is. The CPU generates the most heat in the phone. | 25–40°C during normal use. Above 45°C triggers throttling. | Sustained high CPU temps cause the phone to deliberately slow down to protect itself. |
| Thermal Headroom | How much thermal capacity remains before the phone starts throttling. Shown as a percentage — higher is better. | Above 50% means plenty of room. Below 20% means throttling is imminent. | Gives advance warning before performance drops. Useful during gaming or navigation. |
| Thermal Status | The system's overall thermal assessment, from Normal through escalating severity levels. | Normal or Light is fine. Moderate and above means the phone is actively managing heat. | Higher severity levels progressively disable features — camera, flashlight, fast charging — to cool down. |
| Throttling | Whether the phone is currently reducing CPU speed to manage temperature. | "None" is normal. "Active" means performance is being limited right now. | Explains why the phone suddenly feels slower. Not a hardware failure — a protective mechanism. |

### Network Detail Screen

| Metric | Plain explanation | Normal range | Why it matters |
|--------|-------------------|-------------|----------------|
| Signal Strength (dBm) | How strong the connection to the cell tower or Wi-Fi access point is, measured in decibels. More negative = weaker. | Wi-Fi: -30 to -60 dBm is strong, -70 to -80 is usable, below -80 is weak. Cellular: -50 to -90 is good, below -110 is poor. | Weak signal forces the phone's radio to work harder, draining the battery significantly faster and causing slower data speeds. |
| Latency (ms) | How long it takes for data to make a round trip between the phone and a server. Also called ping. | Under 30 ms is excellent. 30–100 ms is good for most uses. Above 200 ms feels laggy. | Affects video calls, gaming, and anything interactive. High latency makes real-time communication feel delayed. |
| Jitter (ms) | How much the latency varies from one moment to the next. | Under 10 ms is stable. Above 30 ms causes noticeable quality issues. | Even with good average latency, high jitter causes video call stuttering, audio dropouts, and inconsistent gaming performance. |
| Frequency (GHz) | Which Wi-Fi band the phone is connected to — 2.4 GHz or 5 GHz. | 5 GHz is faster but shorter range. 2.4 GHz is slower but reaches further. | Explains why Wi-Fi might be slow even with a strong signal — 2.4 GHz is more congested and inherently slower. |
| Wi-Fi Standard | Which generation of Wi-Fi technology is being used (Wi-Fi 4, 5, 6, 6E, 7). | Wi-Fi 5 or higher is typical for modern devices and routers. | Older standards have lower maximum speeds regardless of signal quality. Knowing this helps determine if the router or the phone is the bottleneck. |
| Link Speed (Mbps) | The negotiated maximum speed between the phone and the Wi-Fi router. Not the actual internet speed. | 200+ Mbps on 5 GHz is typical. Under 72 Mbps suggests poor conditions. | A low link speed despite good signal strength points to interference, a congested channel, or an older Wi-Fi standard. |
| Bandwidth (Mbps) | Android's estimate of available downstream and upstream capacity on the current connection. | Varies by connection type. Mobile 4G: 10–50 Mbps. 5G: 50–300+ Mbps. Wi-Fi: depends on plan and router. | Gives a quick sense of real-world speed without running a full speed test. |
| MTU | Maximum Transmission Unit — the largest data packet the network will carry without splitting it. | 1500 bytes is standard. 1400 on VPNs. | Non-standard MTU rarely causes problems, but mismatches can cause slow loading or failed connections on specific sites. |

### Storage Detail Screen

| Metric | Plain explanation | Normal range | Why it matters |
|--------|-------------------|-------------|----------------|
| Usage % | How full the internal storage is. | Below 80% is comfortable. 80–90% may cause slowdowns. Above 90% is problematic. | Full storage doesn't just mean no room for photos — it physically slows down the storage chip and makes Android struggle with basic operations. |
| Fill Rate | How quickly storage is filling up based on recent trends. | Varies by usage. | Predicts when storage might become a problem, giving time to act before it affects performance. |
| Cache | Temporary data apps store locally to load faster. Each app manages its own. | A few GB total is normal. | Cache can grow silently to consume significant space. Apps manage their own caches — you can clear them individually in Android Settings > Apps. |
| Apps total | How much storage all installed apps consume, including their data. | Varies widely. Social media and streaming apps are typically the largest. | Helps identify which apps are worth keeping vs. which ones consume disproportionate space for how often they're used. |

### Speed Test Results

| Metric | Plain explanation | Normal range | Why it matters |
|--------|-------------------|-------------|----------------|
| Download (Mbps) | How fast data arrives at the phone. What matters for streaming, browsing, and app downloads. | 25+ Mbps is good for most uses. 100+ is excellent. | The number most directly connected to "does the internet feel fast." |
| Upload (Mbps) | How fast data leaves the phone. What matters for sending photos, video calls, and cloud backups. | 5+ Mbps is adequate. 20+ is good. | Usually slower than download by design. Matters most for video calls and uploading large files. |
| Ping (ms) | Same as latency — round-trip time to the test server. | Under 30 ms is excellent. Under 100 ms is fine for most uses. | Low ping makes interactive things feel responsive — gaming, video calls, live collaboration. |
| Jitter (ms) | Variation in ping times during the test. | Under 10 ms is stable. | High jitter with low average ping still causes problems — the inconsistency is what breaks real-time applications. |

---

## Tier 2: Contextual Info Cards — Full Inventory

Cards appear once per section, can be dismissed permanently. Educational, not promotional.

### Battery Detail Screen

**Card: "What 80% battery health really means"**
- Placement: Below Details Panel (near Health % and Capacity rows)
- Content: Your battery was designed to hold [design] mAh when new. At 80% health, it effectively holds 20% less — meaning a 5,000 mAh battery stores only 4,000 mAh. That's not just shorter screen time; it means the battery also can't deliver peak power as reliably, which can cause slowdowns and shutdowns under heavy load. Most manufacturers consider 80% the threshold where replacement makes a meaningful difference.
- Show condition: Health % is available AND below 90%

**Card: "Why your phone dies before reaching 0%"**
- Placement: Below Details Panel
- Content: As batteries age, their voltage curve changes — the relationship between remaining charge and reported percentage becomes less accurate. The phone thinks charge remains, but the battery can't sustain enough voltage for the processor under load. This is normal for older batteries and one of the clearest signs that capacity has degraded beyond what the percentage indicator can track.
- Show condition: Health % is available AND below 80%

**Card: "Charging habits that extend battery life"**
- Placement: Below Current / Charging Panel (shown during charging)
- Content: Lithium-ion batteries last longest when kept between 20% and 80% charge. Consistently charging to 100% or draining to near-zero stresses the cell more than partial charges. Heat during charging also matters — removing the case and avoiding heavy use while plugged in reduces thermal stress. Modern phones often include charge-limiting features for this reason.
- Show condition: Currently charging

**Card: "What screen-off drain tells you"**
- Placement: Below Screen On/Off Panel
- Content: When the screen is off, drain should drop dramatically — a healthy phone in good signal loses under 1% per hour idle. If screen-off drain is high, something is keeping the phone awake: a rogue background app, frequent notification syncing, or weak signal forcing the radio to work overtime. The Sleep Analysis panel below shows whether the phone is actually reaching deep sleep.
- Show condition: Screen-off drain rate > 2%/h

### Thermal Detail Screen

**Card: "Why your phone slows down when it's hot"**
- Placement: Below Metrics Grid
- Content: When internal temperatures rise too high, Android deliberately reduces CPU speed — a process called thermal throttling. This protects the hardware from damage but makes everything feel sluggish. The throttling is progressive: first background tasks are limited, then the processor slows, then features like the camera and fast charging are temporarily disabled. It's a safety mechanism, not a malfunction.
- Show condition: Always shown (first visit)

**Card: "Heat and battery wear compound each other"**
- Placement: Below Metrics Grid (second card, below throttling card)
- Content: A warm phone degrades the battery faster, and a degraded battery produces more heat — creating a feedback loop. Temperatures above 35°C during light tasks (browsing, messaging) are worth investigating. Common causes: a thick case trapping heat, a background app keeping the CPU active, wireless charging at high wattage, or a battery that's developed increased internal resistance with age.
- Show condition: Battery temp > 35°C during first app visit, or thermal score < 70

### Network Detail Screen

**Card: "Weak signal drains your battery"**
- Placement: Below Hero Section
- Content: When the phone has poor cellular or Wi-Fi signal, the radio amplifies its transmission power to maintain the connection. In weak coverage areas, the phone may cycle between cell towers or between 4G and 5G repeatedly. This signal hunting is one of the biggest hidden battery drains — you might not notice the weak signal, but the battery does.
- Show condition: Signal quality is Poor or No Signal

**Card: "What speed tests actually measure"**
- Placement: Above Speed Test Summary
- Content: A speed test measures throughput between your phone and a test server, under ideal conditions for a few seconds. Real-world speeds are usually lower — other devices on the network, server load, distance, and network congestion all play a role. Speed test results are most useful for comparing different connections, different times of day, or tracking changes over time — not as absolute promises of what you'll experience.
- Show condition: Always shown (first visit), or when user navigates to speed test for the first time

### Storage Detail Screen

**Card: "Why a full phone is a slow phone"**
- Placement: Below Hero Card
- Content: Phone storage uses flash memory that physically slows down as it fills up. When space is limited, writing new data requires reading, erasing, and rewriting existing blocks — a process that gets slower the fuller the drive. Android also needs free space for temporary files, app updates, and system maintenance. Keeping at least 15–20% free helps both the storage hardware and the operating system perform normally.
- Show condition: Usage > 75%

**Card: "Where your storage actually goes"**
- Placement: Below Media Breakdown Card
- Content: Videos are almost always the biggest culprit — a few minutes of 4K video can consume gigabytes. Downloaded files accumulate silently, especially from messaging apps like WhatsApp that save every received photo and video automatically. App data (offline content, caches, saved media) often exceeds the app's own install size by a wide margin. The cleanup tools below help identify the largest offenders.
- Show condition: Always shown (first visit)

---

## Tier 3: Learn Section — Article Inventory

### Screen Layout

**Learn Home:**
```
┌──────────────────────────────────┐
│ ← Learn                         │
├──────────────────────────────────┤
│                                  │
│ ┌──── Battery ─────────────────┐ │
│ │                              │ │
│ │ ┌─ article card ───────────┐ │ │
│ │ │ Understanding battery    │ │ │
│ │ │ health and cycle count   │ │ │
│ │ │ 3 min read               │ │ │
│ │ └──────────────────────────┘ │ │
│ │ ┌─ article card ───────────┐ │ │
│ │ │ ...                      │ │ │
│ │ └──────────────────────────┘ │ │
│ └──────────────────────────────┘ │
│                                  │
│ ┌──── Temperature ─────────────┐ │
│ │ ...                          │ │
│ └──────────────────────────────┘ │
│                                  │
│ ┌──── Network ─────────────────┐ │
│ │ ...                          │ │
│ └──────────────────────────────┘ │
│ ...                              │
└──────────────────────────────────┘
```

**Article Detail:**
```
┌──────────────────────────────────┐
│ ← Article Title                  │
├──────────────────────────────────┤
│                                  │
│  Article body text with          │
│  section headers.                │
│                                  │
│  ┌─ cross-link ───────────────┐  │
│  │ Check your battery health → │  │
│  └────────────────────────────┘  │
│                                  │
│  More body text...               │
│                                  │
└──────────────────────────────────┘
```

### Articles by Topic

#### Battery (4 articles)

**"Understanding battery health and cycle count"** (~3 min)
- What a charge cycle actually is (50% twice = one cycle)
- Why 300–500 cycles is the typical lifespan
- What health percentage means in practice
- How charging habits affect cycle impact
- Cross-link: Battery Detail → Health %

**"Why your battery drains faster over time"** (~3 min)
- Chemical aging: capacity loss is permanent and irreversible
- Calendar aging: batteries degrade even sitting unused
- Heat as the primary accelerator
- The 80% threshold and when replacement makes sense
- Cross-link: Battery Detail

**"Charging myths and facts"** (~3 min)
- Overnight charging: safe on modern phones (trickle charging)
- 20–80% range: genuinely beneficial or marginal?
- Fast charging: the heat trade-off
- Wireless vs. wired charging and thermal impact
- Third-party chargers: certified vs. uncertified
- Cross-link: Charger Comparison

**"What battery current and power readings tell you"** (~2 min)
- Current (mA) = flow rate, Power (W) = total energy use
- Why these numbers fluctuate constantly
- What "normal" drain looks like during different activities
- How to spot abnormal drain from readings
- Confidence levels: why runcheck shows measurement reliability
- Cross-link: Battery Detail → Current Panel

#### Temperature (3 articles)

**"Normal phone temperatures and when to worry"** (~2 min)
- 20–35°C: ideal operating range
- 35–45°C: acceptable during heavy tasks
- Above 45°C: throttling begins, battery wear accelerates
- Above 50°C: risk of permanent damage
- Where heat comes from: CPU, battery, wireless charging, environment
- Cross-link: Thermal Detail

**"Thermal throttling explained"** (~2 min)
- What thermal throttling does and why it exists
- The progressive stages: background limits → CPU slowdown → feature disabling → shutdown
- Why it's protective, not a defect
- How to reduce triggering: case removal, avoid charging during heavy use, check for rogue apps
- Cross-link: Thermal Detail → Throttling Log

**"Heat and battery life: the feedback loop"** (~2 min)
- How heat accelerates chemical degradation
- How degradation increases internal resistance → more heat
- Wireless charging thermal considerations
- Practical steps to break the cycle
- Cross-link: Battery Detail, Thermal Detail

#### Network (3 articles)

**"Understanding signal strength and dBm"** (~2 min)
- What decibels (dBm) measure and why the numbers are negative
- Wi-Fi ranges: excellent, good, usable, poor
- Cellular ranges: strong to no signal
- Why signal quality affects battery life
- Cross-link: Network Detail

**"Wi-Fi bands, standards, and real-world speed"** (~3 min)
- 2.4 GHz vs. 5 GHz: range vs. speed trade-off
- Wi-Fi 4/5/6/6E/7: what each generation improves
- Link speed vs. internet speed vs. speed test results
- Why Wi-Fi feels slow: congestion, distance, walls, interference
- Cross-link: Network Detail → Connection Details

**"What speed test results actually mean"** (~2 min)
- Download vs. upload: why they differ
- Latency and jitter: why they matter for calls and gaming
- Why speed test ≠ real-world experience
- Using speed tests for comparison, not absolute measurement
- Cross-link: Speed Test

#### Storage (2 articles)

**"Why full storage slows down your phone"** (~2 min)
- How NAND flash memory works (simplified)
- Write amplification: why writes get slower as storage fills
- Android's need for working space: temp files, app updates, system maintenance
- The 15–20% free space guideline
- Cross-link: Storage Detail

**"Where phone storage actually goes"** (~2 min)
- App data vs. app size: why Facebook at 300 MB install uses 1.5 GB
- Messaging apps as silent storage eaters (WhatsApp, Telegram media)
- Downloads folder: the forgotten graveyard
- Videos: the single biggest space consumer
- Cross-link: Storage Detail → Cleanup Tools

#### General (2 articles)

**"How runcheck calculates your health score"** (~2 min)
- Four categories: Battery (40%), Thermal (25%), Network (25%), Storage (10%)
- Why battery is weighted highest
- What moves the score up and down
- Why the score can change throughout the day (network, thermal shifts)
- Cross-link: Home → Health Score Card

**"Software problems vs. hardware problems"** (~3 min)
- Safe Mode as the first diagnostic filter
- Patterns that point to software: post-update, app-specific, intermittent
- Patterns that point to hardware: consistent, location-specific, survives factory reset
- When a factory reset helps and when it doesn't
- Cross-link: Home

---

## New Components

### InfoIcon

Small inline help trigger placed after metric labels.

```
Properties:
- sheetTitle: String
- sheetContent: @Composable () -> Unit  (or structured data class)
- size: 16dp icon inside 48dp touch target
- color: onSurfaceVariant
- icon: Icons.Outlined.Info (or HelpOutline)
```

Behavior: Tap opens `ModalBottomSheet` with the content.

### InfoCard

Dismissible educational card for Tier 2 contextual content.

```
Properties:
- id: String (unique, for tracking dismissal state)
- headline: String
- body: String
- onDismiss: () -> Unit

Visual:
- Background: surfaceContainerHigh
- Left border: 3dp, primary (#4A9EDE)
- Icon: InfoOutlined, 20dp, primary color
- Dismiss: IconButton(Close), 16dp, top-right
- Corner radius: 16dp
- Padding: 16dp
```

Dismissal state stored in DataStore (simple Set<String> of dismissed card IDs). Dismissal is per-device, not synced.

### LearnArticleCard

Card for the Learn section article list.

```
Properties:
- title: String
- preview: String (max 2 lines)
- readTime: String (e.g. "3 min read")
- onClick: () -> Unit

Visual:
- Standard card background (surfaceContainer)
- Title: titleSmall, Manrope
- Preview: bodySmall, onSurfaceVariant, maxLines = 2, ellipsis
- Read time: labelSmall, onSurfaceVariant
- Corner radius: 16dp
- Padding: 16dp
```

### CrossLinkButton

In-article link to a relevant app screen.

```
Properties:
- label: String (e.g. "Check your battery health")
- onClick: () -> Unit (navigation action)

Visual:
- TextButton style
- Label: labelLarge, primary color
- Trailing icon: ArrowForward, 16dp
```

---

## Implementation Priority

### Phase 1 — Tier 1 (Info Bottom Sheets)
Highest impact, lowest complexity. Every metric that uses a unit or technical term gets a `(?)` icon. Content is static strings (localized). No new screens — just a bottom sheet composable and the InfoIcon component.

Start with Battery Detail (most metrics, most user confusion) → Thermal → Network → Storage.

### Phase 2 — Tier 2 (Contextual Info Cards)
Medium complexity. Requires dismissal state persistence (DataStore), show-condition logic per card, and content placement in existing screen LazyColumns. Start with the condition-based cards (battery health warning, high drain, full storage) since they're most valuable when relevant.

### Phase 3 — Tier 3 (Learn Section)
New screen + new route + article content. Can be built incrementally — start with 3–4 articles on the most common topics (battery health, thermal throttling, storage), expand over time. Article content lives in string resources for localization.

---

## Content Principles

1. **Lead with what the user cares about.** "This affects your battery life" before "The technical explanation is..."
2. **Use concrete numbers.** "20–35°C is ideal" is better than "keep temperatures low."
3. **Connect data to experience.** Don't just explain what voltage is — explain what unstable voltage feels like (shutdowns, percentage jumps).
4. **Be honest about uncertainty.** If a measurement is estimated, say so. This is core to runcheck's brand.
5. **No fear-mongering.** Present facts and thresholds without making users anxious about normal behavior.
6. **Keep it scannable.** Short paragraphs, clear structure, bold key terms only when it aids scanning.
7. **Free, always.** Educational content is never Pro-gated. It builds trust and helps users understand what Pro features reveal.
