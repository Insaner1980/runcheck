# UI Reference — runcheck

Kattava dokumentaatio koko sovelluksen käyttöliittymästä. Päivitetty 2026-03-16.

---

## 1. Navigaatio

### Rakenne
Push-pohjainen navigaatio yhdeltä Home-näytöltä. Ei bottom navigation baria.

### Näytöt (Screen sealed class)
```
Home (hub)
├── Battery → Charger (Pro)
├── Network → SpeedTest
├── Thermal
├── Storage
├── AppUsage (Pro)
├── Settings
└── ProUpgrade
```

### Siirtymät
- 300ms slide + fade (horisontaali)
- Reduced motion: instant (ei animaatiota)

---

## 2. Teema

### Väripaletti (vain tumma teema)

| Token | Hex | Käyttö |
|-------|-----|--------|
| BgPage | `#0B1E24` | Sivun tausta |
| BgCard | `#133040` | Korttien tausta (surfaceContainer) |
| BgCardAlt | `#0F2A35` | Vaihtoehtoinen korttitausta |
| BgIconCircle | `#1A3A48` | Ikonien ympyrätausta, track-väri |
| AccentTeal | `#5DE4C7` | Healthy status, korostus |
| AccentBlue | `#4A9EDE` | Primary, aktiiviset elementit |
| AccentAmber | `#E8C44A` | Fair status |
| AccentOrange | `#F5963A` | Poor status |
| AccentRed | `#F06040` | Critical status |
| AccentLime | `#C8E636` | Korostus |
| AccentYellow | `#F5D03A` | Korostus |
| TextPrimary | `#E8E8ED` | Pääasiallinen teksti (onSurface) |
| TextSecondary | `#90A8B0` | Toissijainen teksti (onSurfaceVariant) |
| TextMuted | `#506068` | Himmennetty teksti (outline) |

### Status-värijärjestelmä

| Tila | Väri | Kynnysarvot |
|------|------|------------|
| Healthy | AccentTeal | Akku 75–100%, lämpö <35°C, tallennus <75% |
| Fair | AccentAmber | Akku 50–74%, lämpö 35–40°C, tallennus 75–85% |
| Poor | AccentOrange | Akku 25–49%, lämpö 40–45°C, tallennus 85–95% |
| Critical | AccentRed | Akku 0–24%, lämpö ≥45°C, tallennus ≥95% |

Signaalivoimakkuus: EXCELLENT/GOOD → healthy, FAIR → fair, POOR → poor, NO_SIGNAL → critical.

### Typografia

| Tyyli | Koko | Paino | Käyttö |
|-------|------|-------|--------|
| displayLarge | 48sp | Bold | Hero-luvut (akku%, health score) |
| displaySmall | 28sp | SemiBold | Virta (mA) |
| headlineLarge | 20sp | SemiBold | Yksikkö %-merkin |
| titleLarge | 20sp | Medium | Korttien otsikot, GridCard title |
| titleMedium | 16sp | Medium | MetricPill/MetricRow arvot, GridCard subtitle |
| bodyLarge | 15sp | Normal | ListRow label |
| bodyMedium | 14sp | Normal | Yleisin leipäteksti, labelit |
| bodySmall | 13sp | Normal | Toissijaiset tiedot, MetricPill label |
| labelLarge | 12sp | SemiBold | SectionHeader, CardSectionTitle (UPPERCASE) |
| labelMedium | 10sp | SemiBold | Badget, pienet labelit |
| labelSmall | 10sp | Medium | Pienet labelit |

**Fontit:** Manrope (body/headers), JetBrains Mono (numeerinen data via `MaterialTheme.numericFontFamily`).

### Spacing-tokenit

| Token | Arvo | Käyttö |
|-------|------|--------|
| xs | 4dp | Minimivälit |
| sm | 8dp | Pienet välit |
| md | 12dp | Keskikoko |
| base | 16dp | Oletuspadding, yleisin |
| lg | 24dp | Isot osiot |
| xl | 32dp | Lopun padding |

### Visuaaliset säännöt
- **Kortit:** `surfaceContainer`, `RoundedCornerShape(16.dp)`, ei elevaatiota
- **Dividerit:** `outlineVariant.copy(alpha = 0.35f)`
- **Kulmaradius:** Kortit 16dp, pienet elementit (badge, chip) 8dp
- **Ikonikoot:** 20dp (ListRow), 22dp (GridCard/IconCircle), 44dp (ympyrä)
- **Minimi touch target:** 48dp

---

## 3. Yhteiset komponentit

### ProgressRing
Pyöreä edistymispalkki. Käytetään health scoressa.
- `progress: Float` (0–1)
- `strokeWidth: Dp` (oletus 10dp)
- `progressColor: Color`
- `content: @Composable` (keskisisältö, esim. pisteluku)
- Animaatio: 1200ms ease-out, huomioi reduced motion
- Piirto: Canvas arc 270°:sta myötäpäivään

### GridCard
Neliökortti 2-sarake-layoutissa.
- `icon: ImageVector` — ikoni ympyrätaustalla (44dp)
- `title: String` — titleLarge, onSurface
- `subtitle: String` — titleMedium, parametrisoitu väri
- `statusLabel: String?` — erotettu " · " -merkillä, oma väri
- `statusColor: Color` — statusLabelin väri
- `subtitleColor: Color` — subtitlen väri (oletus: onSurface)
- `locked: Boolean` — Pro-lukittu: overlay + ProBadgePill

### MetricPill
Label + arvo pystysuunnassa.
- `label: String` — bodySmall, muted
- `value: String` — titleMedium
- `valueColor: Color` — oletus onSurface

### MetricRow
Label + arvo vaakasuunnassa dividerilla.
- `label: String` — bodyMedium, onSurfaceVariant (vasen)
- `value: String` — titleLarge, numericFont, SemiBold (oikea)
- `valueColor: Color` — oletus onSurface
- `copyable: Boolean` — napautus kopioi leikepöydälle + Toast
- `maxLines: Int` — truncate ellipsillä (käytössä IPv6:ssa)
- `showDivider: Boolean` — horisontaalinen viiva alla

### MetricTile
Isompi metriikka-kortti (storage).
- `label: String` — bodyMedium
- `value: String` — titleLarge
- `unit: String?` — bodyMedium, muted
- Minikorkeus 72dp, 16dp kulmat

### ListRow
Valikkomainen rivielementti.
- `label: String` — bodyLarge
- `icon: ImageVector?` — 20dp
- `value: String?` — bodyMedium, oikealle
- `onClick: (() -> Unit)?`
- `trailing: @Composable?` — custom oikea elementti
- Minikorkeus 48dp, oikea nuoli jos klikattava

### SectionHeader
Sivutason osion otsikko.
- `text: String` — labelLarge, SemiBold, UPPERCASE
- Väri: `outline` (TextMuted `#506068`)

### CardSectionTitle
Kortin sisäinen osion otsikko.
- `text: String` — labelLarge, UPPERCASE
- Väri: `onSurfaceVariant` (TextSecondary `#90A8B0`)

### IconCircle
Ikoni pyöreällä taustalla.
- `icon: ImageVector`, koko 44dp, ikoni 22dp
- Tausta: BgIconCircle

### StatusDot
Värillinen pisteen muotoinen indikaattori.
- `color: Color`, koko 8dp

### ProBadgePill
"PRO" -merkki.
- Lock-ikoni + teksti
- Primary-väri 12% alpha taustalla, 8dp kulmat

### SignalBars
5-palkkinäyttö signaalivoimakkuudelle.
- Palkkikorkeudet: 10/18/26/36/48dp, 4dp väli, 3dp kulmat
- Aktiiviset: statusColorForSignalQuality, inaktiiviset: surfaceVariant 30% alpha

### HeatStrip
Horisontaalinen lämpötilagradientti.
- Korkeus 24dp
- Gradientti: healthy → fair → critical
- Ympyräindikaattori normalisoidussa positiossa
- Pulssi-animaatio (alpha 0.7→1.0, 2000ms) jos kriittinen

### TrendChart
Viiva + alue -kaavio historialle. "Instrument Sweep" -animaatiokieli.
- Korkeus 200dp (Embedded) / fullscreen (Fullscreen)
- Canvas-piirto: viivapolku + strip-pohjainen gradienttitäyttö
- **Sisääntuloanimaatio (3 vaihetta):**
  1. Grid + akselit fade-in (200ms, FastOutSlowInEasing)
  2. Oskilloskooppi-pyyhkäisy: clipRect-pohjainen reveal + ohut skannausviiva (1000ms, CubicBezier(0.25, 0.1, 0.25, 1)), skannausviiva häipyy viimeisen 30% aikana
  3. Viimeisen arvon korostus: glow-piste (6dp, 30% alpha) + katkoviiva Y-akselille (200ms)
- **Datanvaihtosiirtymä** (aikavälin/metriikin vaihto): vanha data fade-out (300ms) → uusi data pyyhkäisy (800ms), grid pysyy näkyvänä
- **Status gradient -viiva:** viivan väri muuttuu datapisteiden mukaan quality zone -väreillä (healthy→fair→critical). Käyttää `Brush.horizontalGradient` + `qualityZoneColorForValue()`. Vain metrikoille joilla on quality zones.
- **Parannettu gradient-täyttö:** strip-pohjainen renderöinti, kunkin stripin alpha riippuu datapisteen korkeudesta (huiput 0.30, laaksot 0.08). Käyttää yhtä `Path`-objektia `reset()`-kutsulla per strip.
- **Viimeisen arvon korostus:** hehkuva piste viimeisessä datapisteessä + katkoviiva (DashPathEffect 4dp/4dp) Y-akselille, kontrolloituna `emphasisAlpha`-animaatiolla
- Auto-downsampling 300 pisteeseen (600 fullscreen)
- Tooltip: tap/drag, pystyviiva + piste + pyöristetty laatikko, estetty pyyhkäisyn aikana

### AreaChart
Yksinkertainen viiva + alue -kaavio (Pro-esikatselunäkymä, blur-efekti).
- Canvas-piirto: viivapolku + strip-pohjainen gradienttitäyttö
- Oskilloskooppi-pyyhkäisy (800ms, CubicBezier) + skannausviiva, sama kuin TrendChart
- Parannettu gradient-täyttö: strip-pohjainen, alpha 0.08–0.25 (hieman matalampi kuin TrendChart)
- Ei akseleita, labeleita, tooltippia tai gestuureita

### LiveChart
Reaaliaikainen sparkline-kaavio (akun virta/teho).
- Korkeus 80dp, oikealle tasattu (uusin data oikeassa reunassa)
- **Pehmeä liuku:** uuden datapisteen saapuessa koko käyrä liukuu vasemmalle 150ms animaatiolla (`Animatable` scroll offset)
- **Glow-pulssi:** uusin piste saa saapumispulssin (radius 8→5dp, alpha 0.5→0.3, 300ms), palautuu normaaliin glow-tilaan
- Grid: 3 horisontaalista viivaa (25/50/75%)
- Nykyarvon piste: ulompi glow (5dp, 30% alpha) + sisempi piste (3dp)

### ConfidenceBadge
Mittauksen luotettavuusmerki.
- HIGH → sininen "Accurate"
- LOW → keltainen "Estimated"
- UNAVAILABLE → harmaa "Unavailable"
- Spring-animaatio skaalauksella

### PrimaryTopBar
Etusivun yläpalkki.
- Sovelluksen nimi + oikean puolen toiminnot (Settings-ikoni)
- Ei elevaatiota, statusBarsPadding

### DetailTopBar
Alanäkymien yläpalkki.
- Takaisin-nappi + keskitetty otsikko + symmetrinen spacer

### ProFeatureCalloutCard
"Päivitä Pro:hon" -kortti näkymien sisällä.
- Viesti + toimintonappi
- Primary-reunus (35% alpha), OutlinedButton

### ProFeatureLockedState
Koko näytön lukitustila Pro-ominaisuuksille.
- Otsikko, viesti, päivitysnappi

### PullToRefreshWrapper
Pull-to-refresh -kääre.
- Material 3 PullToRefreshBox

---

## 4. Näkymät

### 4.1 Home Screen

**Rakenne ylhäältä alas:**

1. **PrimaryTopBar** — "runcheck" + Settings-ikoni
2. **HomeStatusSummary** — yksirivinen teksti: "{batteryStatus}, {tempBand}, {networkStatus}"
3. **TrialHomeCard** tai **PostExpirationUpgradeCard** (ehdollinen)
4. **HealthScoreCard**
   - SectionHeader: "HEALTH SCORE"
   - ProgressRing (152dp, 10dp stroke) + pisteluku (48sp)
   - Tilateksti: "Your device is in [status] shape" — status-sana AccentTeal boldilla
   - Varoitus jos lämpö ≥38°C (AccentOrange)
   - 4 HealthBreakdownRow:a (Battery, Thermal, Network, Storage):
     - StatusDot + label + prosentti, klikattava → detail screen
     - Dividerit välissä
5. **BatteryHeroCard** (klikattava → Battery detail)
   - Akun taso (54sp) + %-merkki + latausstatus
   - HomeBatteryChargeIcon (130dp, Canvas: akkurunko + gradienttitäyttö + aaltoanimaatio ladatessa)
   - 2 MetricPill: Battery Health (statusväri) + Plug Type
6. **GridCard-rivi 1:** Network + Thermal
   - Network: subtitle = verkon nimi (onSurface), statusLabel = signaalin laatu (statusväri)
   - Thermal: subtitle = lämpötila (onSurface), statusLabel = lämpötilaluokka (statusväri)
7. **GridCard-rivi 2:** Chargers (Pro-lukittu) + Storage
   - Chargers: subtitle = "Test & compare" (onSurfaceVariant)
   - Storage: subtitle = vapaa tila (onSurface), ikoni statusvärillä
8. **SectionHeader:** "QUICK TOOLS"
9. **ListRow-kortti:** Speed Test + App Usage (Pro-lukittu overlay)
10. **Pro/Insights-kortti** (ehdollinen tilan mukaan)
11. **TrialExpirationModal** (ehdollinen)

### 4.2 Battery Detail Screen

**Rakenne ylhäältä alas:**

1. **DetailTopBar** — "Battery"
2. **Pull-to-refresh**
3. **BatteryHeroPanel**
   - Iso akkuikoni (Canvas, 80×124dp) + taso (iso numero) + latausstatus
4. **BatteryInfoPanel**
   - CardSectionTitle: "BATTERY INFO"
   - MetricRow-rivit: Voltage, Temperature, Health, Cycle Count
   - Kukin label + arvo + valinnainen ConfidenceBadge
5. **Charging/Battery Current -panel** (dynaaminen otsikko tilan mukaan)
   - CardSectionTitle: "CHARGING" tai "BATTERY CURRENT"
   - "Current"-label + iso mA-arvo (displaySmall, numericFont) + ConfidenceBadge
   - **Charging session** (ehdollinen, vain ladatessa):
     - CardSectionTitle: "CURRENT SESSION"
     - BatterySessionSummary: Started %, Gain %, Duration, Peak Temp, Recent Speed, Avg Speed, Delivered mAh, Avg Power
     - Kukin pari MetricPill vierekkäin
   - Divider
   - MetricPill-rivi: Status (Charging/Discharging) + Type (AC/USB/Wireless/None)
6. **RemainingChargeTimePanel** (ehdollinen, vain ladatessa + mielekäs arvio)
   - SectionHeader: "REMAINING CHARGE TIME"
   - MetricPill: To 80% + To 100%
7. **SessionGraphPanel** (ehdollinen, vain ladatessa)
   - SectionHeader: "SESSION GRAPH"
   - FilterChip-rivi: Current / Power
   - TrendChart (session data)
8. **ChargerComparisonCard** — nappi Charger Comparison -näkymään
9. **BatteryHistoryPanel**
   - SectionHeader: "HISTORY"
   - Pro-lukittu: ProFeatureCalloutCard + blur/overlay
   - Pro: FilterChip-rivi (Day/Week/Month/All) + TrendChart
10. **DrainRatePanel** (ehdollinen, vain purkautuessa)
    - MetricPill: purkausnopeus %/h + arvioitu jäljellä oleva aika
11. **DetailScreenAdBanner**

### 4.3 Network Detail Screen

**Rakenne ylhäältä alas:**

1. **DetailTopBar** — "Network"
2. **Pull-to-refresh**
3. **NetworkHeroSection**
   - SignalBars (5 palkkia, statusvärillä)
   - Laadun nimi (titleMedium, bold, statusväri)
   - dBm-arvo (valinnainen)
   - 3 MetricPill: Latency, Bandwidth/WiFi Speed, Band/Subtype
4. **WifiNameHelpCard** (ehdollinen: WiFi mutta ei SSID:tä)
   - Warning-ikoni + otsikko + viesti
   - Toimintonappi: location permission tai settings
5. **ConnectionDetailsCard**
   - CardSectionTitle: "CONNECTION DETAILS"
   - MetricRow: Connection Type
   - WiFi: SSID, BSSID (copyable), WiFi Standard, Frequency, Link Speed
   - Cellular: Carrier, Subtype, Roaming
   - Est. Bandwidth Down/Up, Metered, VPN
   - Divider
   - CardSectionTitle: "IP ADDRESS & DNS"
   - MetricRow: IPv4 (copyable), IPv6 (copyable, maxLines=1), DNS 1–2 (copyable), MTU
6. **SignalHistoryCard**
   - CardSectionTitle: "SIGNAL HISTORY"
   - FilterChip: Signal / Latency
   - FilterChip: Day / Week / Month / All
   - TrendChart tai "Not enough data" -viesti
7. **SpeedTestSummaryCard**
   - CardSectionTitle: "SPEED TEST"
   - Viimeisin tulos: 3 MetricPill (Download, Upload, Ping) + Jitter
   - Serveri + aikaleima
   - Tai "No results" -viesti
   - "Run Speed Test" -nappi
8. **DetailScreenAdBanner**

### 4.4 Speed Test Screen

**Tilat:**
- **Idle:** "Run Speed Test" -nappi, Cellular warning dialog (ehdollinen)
- **Testing:** Vaiheittainen edistyminen (Ping → Download → Upload)
  - Kunkin vaiheen nimi + CircularProgressIndicator tai tulos
- **Completed:** Tulokset + historia
- **Failed:** Virheviesti + retry

**Tulosnäkymä:**
- LastResultCard: Download/Upload/Ping (3 saraketta) + aikaleima
- HistoryResultRow-lista: aikaleima | download | upload | ping (4-sarake taulukko)

### 4.5 Thermal Detail Screen

**Rakenne ylhäältä alas:**

1. **DetailTopBar** — "Thermal"
2. **Pull-to-refresh**
3. **ThermalHeroCard**
   - ThermometerIcon (Canvas, 40×120dp):
     - Varsi + polttimo, täyttö alhaalta ylös (animoitu 1200ms)
     - Tick-merkit, outline, gradientti
   - Lämpötila (48sp, displayLarge, numericFont)
   - °C-yksikkö (headlineLarge, TextSecondary)
   - Lämpötilaluokka (titleMedium, statusväri)
4. **HeatStrip** — gradienttipalkki indikaattorilla
5. **ThermalMetricsCard**
   - Rivi 1: CPU Temp (MetricPill) + Thermal Headroom (MetricPill)
   - Divider
   - Rivi 2: Thermal Status (MetricPill) + Throttling (MetricPill)
   - Kaikki arvot statusvärillä
6. **SectionHeader:** "THROTTLING LOG"
7. **Throttling-lista** (Pro-lukittu)
   - Tyhjä: StatusDot (healthy) + "No throttling events"
   - Tapahtumat: ThrottlingEventItem — aikaleima, status+StatusDot, lämpötila, CPU temp, sovellus, kesto
8. **DetailScreenAdBanner**

### 4.6 Storage Detail Screen

**Rakenne ylhäältä alas:**

1. **DetailTopBar** — "Storage"
2. **LinearProgressIndicator** (käyttöprosentti, 8dp korkeus)
3. **MetricTile-pino:**
   - Total Storage
   - Used Storage (+ prosentti)
   - Available Storage
   - Apps (ehdollinen)
   - Media (ehdollinen)
   - Fill Rate Estimate (ehdollinen)
4. **SD Card -osio** (ehdollinen):
   - SectionHeader: "SD CARD"
   - MetricTile: Total, Available

### 4.7 Settings Screen

**Osiot ylhäältä alas:**

1. **SectionHeader:** "MONITORING INTERVAL"
   - SettingsCard: 3 RadioButton-riviä (15/30/60 min)

2. **SectionHeader:** "NOTIFICATIONS"
   - SettingsCard: Toggle (päällä/pois)
   - PermissionHelpCard (ehdollinen, Android 13+)

3. **SectionHeader:** "PRO" tai "PRO ACTIVE"
   - Pro-käyttäjä: kiitosviesti + Refresh Purchase
   - Ilmaiskäyttäjä: kuvaus + Osta-nappi (hinta) + Palauta ostos

4. **SectionHeader:** "DATA"
   - Pro: Retention-valinnat (3mo/6mo/1yr/Forever) + Export-nappi
   - Ilmainen: "24h retention" -teksti + ProFeatureCalloutCard

5. **SectionHeader:** "MEASUREMENT INFO" (ehdollinen, DeviceProfile)
   - Laitemalli + valmistaja
   - 2×2 MetricPill: API Level, Current Reading, Cycle Count, Thermal Zones

6. **SectionHeader:** "PRIVACY"
   - Crash reporting toggle + selitys

7. **SectionHeader:** "ABOUT"
   - Versionumero

### 4.8 Pro Upgrade Screen

**Tilat:**
- **Ei ostettu:** Otsikko + alaotsikko + 5 feature-riviä (ikoni + teksti) + osta-nappi + palauta
- **Ostettu:** Vahvistusviesti
- **Juuri ostettu:** Kiitos-viesti + dismiss-nappi

**Feature-lista:**
1. Extended history (BarChart-ikoni)
2. Widgets (Widgets-ikoni)
3. Charger comparison (BatteryChargingFull-ikoni)
4. Advanced metrics (DataUsage-ikoni)
5. Data export (FileDownload-ikoni)

### 4.9 Charger Comparison Screen (Pro)

- DetailTopBar + FAB (lisää laturi)
- ProFeatureLockedState jos ei Pro
- Laturi-dialogi (nimi + wattimäärä)
- Laturilista + poisto
- Vertailulaskenta (arvioitu latausaika)

### 4.10 App Usage Screen (Pro)

- DetailTopBar
- ProFeatureLockedState jos ei Pro
- Usage stats -lupa tarkistus + help card
- Sovelluslista: ikoni + nimi + LinearProgressIndicator + ajat

---

## 5. Animaatiot

| Komponentti | Kesto | Easing | Reduced motion |
|-------------|-------|--------|----------------|
| ProgressRing | 1200ms | FastOutSlowInEasing | tween(0) |
| MiniBar | 800ms | FastOutSlowInEasing | tween(0) |
| TrendChart grid fade | 200ms | FastOutSlowInEasing | Instant |
| TrendChart sweep (sisääntulo) | 1000ms | CubicBezier(0.25, 0.1, 0.25, 1) | Instant |
| TrendChart sweep (siirtymä) | 800ms | CubicBezier(0.25, 0.1, 0.25, 1) | Instant |
| TrendChart scan line fade | 300ms (viimeinen 30%) | Linear | Ei näy |
| TrendChart emphasis | 200ms | FastOutSlowInEasing | Instant |
| TrendChart fade-out (vanha data) | 300ms | FastOutSlowInEasing | Instant |
| AreaChart sweep | 800ms | CubicBezier(0.25, 0.1, 0.25, 1) | Instant |
| AreaChart scan line | 240ms (viimeinen 30%) | Linear | Ei näy |
| LiveChart scroll | 150ms | LinearEasing | Instant |
| LiveChart glow pulse | 300ms | Linear | Ei pulssia |
| ThermometerIcon | 1200ms | FastOutSlowInEasing | tween(0) |
| HeatStrip pulse | 2000ms loop | alpha 0.7→1.0 | Ei pulssia |
| Battery wave | 2000ms loop | LinearEasing sine | Ei aaltoa |
| ConfidenceBadge | Spring | Scale bounce | Instant |
| Navigaatio | 300ms | Slide + fade | Instant |

---

## 6. Saavutettavuus

- Kaikki interaktiiviset elementit: `contentDescription`
- ProgressRing: `progressBarRangeInfo` semantiikka
- `reducedMotion` tarkistus: `AccessibilityManager.isReducedMotionEnabled()`
- Kontrasti: 4.5:1 leipäteksti, 3:1 iso teksti (WCAG AA)
- Touch target: minimum 48dp
- `semantics(mergeDescendants = true)` korteissa
- Numeerinen fontti (JetBrains Mono) selkeyden vuoksi

---

## 7. Pro-lukitus

### Lukitut ominaisuudet
- Charger Comparison -näkymä (koko näyttö)
- App Usage -näkymä (koko näyttö)
- Battery History (viikko/kuukausi/kaikki)
- Thermal Throttling Log
- Data Export
- Data Retention (>24h)

### Lukituksen visuaalinen esitys
- **GridCard:** `locked=true` → semi-transparent overlay + ProBadgePill oikeassa yläkulmassa
- **ListRow:** ProBadgePill trailing + scrim overlay
- **Osio näkymässä:** ProFeatureCalloutCard (reunus + nappi)
- **Koko näkymä:** ProFeatureLockedState (otsikko + viesti + nappi)

---

## 8. Lokalisointi

- Kaikki merkkijonot `res/values/strings.xml` (EN) + `res/values-fi/strings.xml` (FI)
- Yksiköt: °C, %, ms, Mbps, dBm, mA, W, mV, MHz, GHz
- Päivämääräformatointi: `DateFormat.getBestDateTimePattern()` (locale-aware)
- Tiedostokoot: `Formatter.formatShortFileSize()` (locale-aware)
- Desimaalit: `String.format(Locale.getDefault(), ...)` (locale-aware)

---

## 9. UI-tila ja datavirta

### Tilamallit (sealed interface)
Kaikki näkymät: Loading → Success (data) | Error (message)

| Näkymä | Tila-luokka | Success-data |
|--------|-------------|-------------|
| Home | HomeUiState | HealthScore, BatteryState, NetworkState, ThermalState, StorageState, ProState |
| Battery | BatteryUiState | BatteryState, history, chargingSession, drainRate, isPro |
| Network | NetworkUiState | NetworkState, signalHistory, selectedPeriod |
| SpeedTest | SpeedTestUiState | phase, metrics, progress, history |
| Thermal | ThermalUiState | ThermalState, throttlingEvents, isPro |
| Storage | StorageUiState | StorageState |
| Settings | SettingsUiState | preferences, deviceProfile, isPro, billingState |

### Elinkaaren hallinta
Kaikki näkymät käyttävät `DisposableEffect` + `LifecycleEventObserver`:
- `ON_START` → `viewModel.startObserving()`
- `ON_STOP` → `viewModel.stopObserving()`

### Formatointi (UiFormatters.kt)

| Funktio | Tulos | Esimerkki |
|---------|-------|-----------|
| `formatPercent(Int)` | `X%` | `85%` |
| `formatPercent(Float, Int)` | `X.X%` | `14.8%` |
| `formatTemperature(Float)` | `X.X°C` | `29.8°C` |
| `formatDecimal(Number, Int)` | `X.X` | `3.7` |
| `formatStorageSize(Context, Long)` | Locale-aware | `185 GB` |
| `rememberFormattedDateTime(Long, String)` | Locale-aware | `Mar 16, 8:36 PM` |
| `scoreLabel(Int)` | Teksti | `Excellent/Good/Fair/Poor` |
| `batteryHealthLabel(Health)` | Teksti | `Good/Overheat/Dead/...` |
| `chargingStatusLabel(Status)` | Teksti | `Charging/Discharging/Full` |
| `plugTypeLabel(PlugType)` | Teksti | `AC/USB/Wireless/None` |
| `temperatureBandLabel(Float)` | Teksti | `Cool/Warm/Hot` |
| `signalQualityLabel(Quality)` | Teksti | `Excellent/Good/Fair/Poor` |
| `connectionDisplayLabel(...)` | Teksti | `The Net_5G / LTE / None` |
