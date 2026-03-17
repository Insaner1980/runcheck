# Storage UI Design

Visuaalinen suunnitelma storage-näkymän toteutukselle. Perustuu sovelluksen olemassa olevaan design systemiin ja noudattaa samoja konventioita kuin battery-, thermal- ja network-näkymät.

Katso featurelista ja tekninen arkkitehtuuri: [storage-enhancements-spec.md](storage-enhancements-spec.md)

---

## Design system -konteksti

### Väripaletti (tumma teema, ainoa teema)

| Token | Hex | Käyttö |
|-------|-----|--------|
| BgPage | `#0B1E24` | Sivun tausta |
| BgCard | `#133040` | Korttien tausta (`surfaceContainer`) |
| BgIconCircle | `#1A3A4D` | Ikonien ympyrätausta |
| AccentTeal | `#4DD0B8` | Ensisijainen korostus |
| AccentBlue | `#5BA8F5` | Toissijainen korostus |
| AccentOrange | `#F5A05B` | Varoitus / lämpö |
| AccentRed | `#F55B5B` | Kriittinen / virhe |
| AccentLime | `#A8F55B` | Positiivinen / terve |
| AccentYellow | `#F5D45B` | Huomio / kohtalainen |
| TextPrimary | `#E0E0E0` | Pääteksti (`onSurface`) |
| TextSecondary | `#ABABAB` | Toissijainen teksti (`onSurfaceVariant`) |
| TextMuted | `#707070` | Himmennetty teksti |

### Typografia

| Rooli | Fontti | Käyttö |
|-------|--------|--------|
| Manrope | `MaterialTheme.typography` | Kaikki leipäteksti ja otsikot |
| JetBrains Mono | `MaterialTheme.numericFontFamily` | Numerot, koot, prosentit |

### Komponenttikirjasto (olemassa olevat)

| Komponentti | Kuvaus |
|-------------|--------|
| `ProgressRing` | Ympyräindikaattori, 1200ms ease-out animaatio |
| `MiniBar` | Vaakapalkkiindikaattori, 800ms ease-out |
| `MetricPill` | Label + arvo pystysuunnassa |
| `MetricRow` | Label + arvo vaakasuunnassa, divider |
| `GridCard` | Kortti gridissä arvolla ja statuslabelilla |
| `SectionHeader` | Osion otsikko, TextMuted / outline |
| `CardSectionTitle` | Osion alaotsikko kortin sisällä |
| `StatusDot` | Pieni väripiste statusindikaattorina |
| `IconCircle` | BgIconCircle-taustalla oleva ikoni |
| `TrendChart` | Viiva/aluediagrammi historialle |
| `FilterChip` | Material 3 valintasiru |
| `ProBadgePill` | Pro-merkki |
| `DetailTopBar` | Yläpalkki takaisin-nuolella |
| `ConfidenceBadge` | Tarkkuusmerkki (Accurate/Estimated/N/A) |

### Muotoilu

| Elementti | Arvo |
|-----------|------|
| Korttien pyöristys | 16dp |
| Pienten elementtien pyöristys | 8dp |
| Korttien tausta | `surfaceContainer`, ei reunusta, ei varjoa |
| Dividerit | `outlineVariant.copy(alpha = 0.35f)` |
| Kosketusalue min | 48dp |
| Välilyönnit | 4dp grid: 4/8/12/16/24/32dp (`MaterialTheme.spacing`) |
| Animaatiot | Kunnioittavat `MaterialTheme.reducedMotion` |

---

## Näkymien visuaaliset identiteetit

Jokaisella päänäkymällä on tunnistettava hero-visualisointi:

| Näkymä | Hero | Ominaispiirre |
|--------|------|---------------|
| Battery | ProgressRing (%) | Akkutaso ympyrässä |
| Thermal | Canvas-lämpömittari | Täyttyvä lämpömittari + HeatStrip |
| Network | SignalBars | Signaalipalkki-animaatio |
| **Storage** | **ProgressRing + SegmentedBar** | **Käyttöaste + kategoriaerittely** |

Storage-näkymän visuaalinen identiteetti muodostuu kahdesta elementistä: tuttu ProgressRing käyttöasteelle ja uusi SegmentedBar-komponentti median erittelylle.

---

## Hero-kortti

```
┌──────────────────────────────────────────┐
│  ⬡ STORAGE                              │  ← SectionHeader
│                                          │
│              ╭─────────────╮             │
│             ╱    67.4      ╲            │  ← JetBrains Mono, 48sp
│            │    ─────────    │            │
│            │    128.0 GB     │            │  ← JetBrains Mono, 16sp
│             ╲     52%       ╱            │  ← TextSecondary, 14sp
│              ╰─────────────╯             │
│                                          │
│  60.6 GB free · +1.2 GB/week            │  ← bodyMedium, onSurfaceVariant
│                                          │
│  [Cache    ] [Fill Rate ] [Free     ]    │  ← MetricPill-rivi
│   2.4 GB     +1.2 GB/w    60.6 GB       │
└──────────────────────────────────────────┘
```

### Yksityiskohdat

- **ProgressRing**: Sama komponentti kuin akkusivulla. Väri `statusColorForStoragePercent()`:
  - `< 70%` → `statusColors.healthy` (vihreä)
  - `70–85%` → `statusColors.fair` (keltainen)
  - `85–95%` → `AccentOrange` (oranssi)
  - `> 95%` → `statusColors.critical` (punainen)
- **Ringin sisältö**: Käytetty GB isolla fontilla (`48sp`, `JetBrains Mono`, `Bold`). Alla kokonaiskoko (`16sp`, `TextSecondary`). Alla prosentti (`14sp`, `TextSecondary`).
- **Statusteksti**: Vapaa tila + täyttövauhti samalla rivillä, `bodyMedium`, `onSurfaceVariant`. Täyttövauhti näytetään vain kun Room-dataa on riittävästi.
- **MetricPill-rivi**: Cache (jos lupa), Fill Rate, Free. Kolme pilliä `weight(1f)`.

---

## Media Breakdown -kortti

Tämä on storage-näkymän erottava visuaalinen elementti — muilla näkymillä ei ole vastaavaa.

```
┌──────────────────────────────────────────┐
│  Media Breakdown                         │  ← CardSectionTitle
│                                          │
│  ████████████▓▓▓▓▓▓░░░▒▒▒▒░░░░░░░░░░░  │  ← SegmentedBar
│                                          │
│  ● Images       3.2 GB                  │  ← Teal
│  ● Videos       8.1 GB                  │  ← Blue
│  ● Audio        1.4 GB                  │  ← Orange
│  ● Documents    420 MB                   │  ← Lime
│  ● Downloads    1.8 GB                  │  ← Yellow
│  ● Other        2.3 GB                  │  ← TextMuted
└──────────────────────────────────────────┘
```

### SegmentedBar — uusi komponentti

**Canvas-pohjainen** vaakapalkki joka näyttää kategorioiden suhteelliset osuudet.

```
Tekniset mitat:
- Korkeus: 12dp
- Pyöristys: 6dp (RoundedCornerShape, eli täysin pyöristetty)
- Segmenttien välissä: 2dp gap (BgCard-väri näkyy läpi)
- Pienin segmentti: minWidth 4dp (jotta näkyy aina)
- Animaatio: 800ms ease-out leveyksille (kuten MiniBar)
```

**Värikartta:**

| Kategoria | Väri | Perustelu |
|-----------|------|-----------|
| Images | AccentTeal `#4DD0B8` | Ensisijainen accent, kuvat ovat yleisiä |
| Videos | AccentBlue `#5BA8F5` | Erottuu tealista, video = "media" |
| Audio | AccentOrange `#F5A05B` | Lämmin kontrasti kylmille väreille |
| Documents | AccentLime `#A8F55B` | Kevyt, dokumentit ovat "kevyitä" |
| Downloads | AccentYellow `#F5D45B` | Huomioväri, lataukset = siivouskohde |
| Other | TextMuted `#707070` | Määrittelemätön, himmennetty |

Nämä 6 väriä erottuvat toisistaan tumma taustaa vasten ja käyttävät sovelluksen olemassa olevaa paletia.

### SegmentedBarLegend

Legendarivi per kategoria:

```
Row(verticalAlignment = CenterVertically) {
    StatusDot(color = categoryColor)        // 8dp piste
    Spacer(8.dp)
    Text(label, bodySmall, onSurfaceVariant)
    Spacer(weight)
    Text(size, bodySmall, numericFontFamily, onSurface)
}
```

Rivivälillä `4.dp`. `StatusDot` on jo olemassa oleva komponentti.

---

## Cleanup Tools -osio

### ActionCard — uusi komponentti

Erottaa toiminnalliset kortit informatiivisista visuaalisesti:

```
┌──────────────────────────────────────────┐
│  CLEANUP TOOLS                           │  ← SectionHeader
│                                          │
│  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐ │
│  │ ○  Large Files                     │ │  ← IconCircle + titleSmall
│  │    14 files · 4.2 GB              │ │  ← bodySmall, onSurfaceVariant
│  │                          [Scan ›] │ │  ← TextButton, primary
│  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘ │
│                                          │
│  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐ │
│  │ ○  Old Downloads                   │ │
│  │    34 files · 2.8 GB              │ │
│  │                          [Scan ›] │ │
│  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘ │
│  ...                                     │
└──────────────────────────────────────────┘
```

### ActionCard vs InfoCard erottelu

| Ominaisuus | InfoCard (BatteryPanel) | ActionCard |
|------------|----------------------|------------|
| Tausta | `surfaceContainer` | `surfaceContainer` |
| Reunus | Ei | `outlineVariant.copy(alpha = 0.35f)`, 1dp |
| Pyöristys | 16dp | 16dp |
| Varjo | Ei | Ei |
| Sisältö | Dataa | Ikoni + otsikko + kuvaus + toimintopainike |

Ainoa visuaalinen ero on hienovarainen reunaviiva. Tämä riittää erottamaan ne ilman teeman rikkomista.

### ActionCard-layout

```
Row {
    IconCircle(                         // Vasen: ikoni
        icon = Icons.Outlined.FolderOpen,
        tint = AccentTeal,
        background = BgIconCircle,
        size = 40.dp
    )
    Spacer(12.dp)
    Column(Modifier.weight(1f)) {       // Keski: teksti
        Text(title, titleSmall, onSurface)
        Text(subtitle, bodySmall, onSurfaceVariant)
    }
    TextButton(onClick) {               // Oikea: toiminto
        Text("Scan", primary)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight)
    }
}
```

### Ikonit per toiminto

| Toiminto | Material Icon | Väri |
|----------|--------------|------|
| Large Files | `FolderOpen` | AccentOrange |
| Old Downloads | `Download` | AccentBlue |
| APK Files | `Android` | AccentLime |
| Screenshots | `Screenshot` | AccentTeal |
| Trash | `Delete` | AccentRed |
| Duplicates | `FileCopy` | AccentYellow |

---

## Top Apps -listaus

```
┌──────────────────────────────────────────┐
│  TOP APPS BY SIZE                  [PRO] │  ← CardSectionTitle + ProBadgePill
│                                          │
│  Chrome                        1.8 GB   │  ← titleSmall + numericFontFamily
│  ██████████████████░░░░░░░░░░░░░░░░░░░  │  ← MiniBar (suhteellinen)
│  APK 245 MB · Cache 312 MB              │  ← bodySmall, onSurfaceVariant
│                              [Info ›]    │  ← TextButton
│  ─────────────────────────────────────── │  ← Divider
│  WhatsApp                      1.2 GB   │
│  ████████████░░░░░░░░░░░░░░░░░░░░░░░░░  │
│  APK 68 MB · Cache 187 MB               │
│                              [Info ›]    │
│  ─────────────────────────────────────── │
│  ...                                     │
│                                          │
│  Showing top 10                          │  ← bodySmall, TextMuted
└──────────────────────────────────────────┘
```

### MiniBar-käyttö

- `MiniBar` progress = `app.totalBytes / maxAppBytes` (suhteellinen suurimpaan)
- `progressColor` = `AccentTeal`
- `trackColor` = `surfaceVariant.copy(alpha = 0.5f)` (sama kuin ProgressRingin track)

### Lupapyyntö (jos PACKAGE_USAGE_STATS puuttuu)

```
┌──────────────────────────────────────────┐
│  TOP APPS BY SIZE                  [PRO] │
│                                          │
│  ○  Grant usage access to see app sizes  │  ← IconCircle(Lock) + bodyMedium
│                                          │
│  [Open Settings]                         │  ← OutlinedButton, primary
└──────────────────────────────────────────┘
```

Sama pattern kuin Pro-feature locked state — tuttu käyttäjälle.

---

## Tiedostolistanäkymät

Erilliset näkymät: LargeFilesScreen, OldDownloadsScreen, jne. Yhtenäinen rakenne.

### Yläosa

```
┌──────────────────────────────────────────┐
│  ←  Large Files                          │  ← DetailTopBar
│                                          │
│  [10 MB] [50 MB] [100 MB] [500 MB]      │  ← FilterChip-rivi (threshold)
│                                          │
│  Found 14 files · 4.2 GB                │  ← labelLarge, onSurfaceVariant
└──────────────────────────────────────────┘
```

### Tiedostolista

```
┌──────────────────────────────────────────┐
│  ☑  VID_20260102_143022.mp4     1.2 GB  │
│     Video · Jan 2, 2026                  │
│  ─────────────────────────────────────── │
│  ☐  backup_full.zip             890 MB  │
│     Download · Oct 15, 2025              │
│  ─────────────────────────────────────── │
│  ☑  podcast_episode_234.mp3     245 MB  │
│     Audio · Dec 3, 2025                  │
│  ─────────────────────────────────────── │
│  ...                                     │
└──────────────────────────────────────────┘
```

### FileListItem-layout

```
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable { toggleSelection(item) }
        .padding(vertical = 12.dp)
) {
    Checkbox(                               // Vasen
        checked = isSelected,
        onCheckedChange = { toggle },
        colors = CheckboxDefaults.colors(
            checkedColor = MaterialTheme.colorScheme.primary
        )
    )
    Spacer(12.dp)
    Column(Modifier.weight(1f)) {           // Keski: nimi + meta
        Text(
            displayName,
            titleSmall,
            onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "$category · $date",
            bodySmall,
            onSurfaceVariant
        )
    }
    Text(                                   // Oikea: koko
        formattedSize,
        titleSmall,
        numericFontFamily,
        onSurface
    )
}
```

Kosketusalue: koko rivi on klikkattava (48dp+ korkeus). Checkbox-väri `primary`.

### Sticky bottom bar

```
┌──────────────────────────────────────────┐
│                                          │
│  ┌────────────────────────────────────┐  │
│  │     Delete selected (2.1 GB)       │  │  ← Button, fillMaxWidth
│  └────────────────────────────────────┘  │
│                                          │
└──────────────────────────────────────────┘
```

- **Surface**: `surfaceContainer` tausta, `elevation = 0.dp`, yläreuna `outlineVariant` divider
- **Button**: `MaterialTheme.colorScheme.error` tausta kun poistaa (punainen), `onError` teksti
- **Teksti**: "Delete selected" + valittujen yhteiskoko
- **Animaatio**: slide-up kun vähintään 1 valittu, slide-down kun 0 valittu (`AnimatedVisibility` + `slideInVertically`)
- **Tyhjä tila**: `Select all` TextButton yläpuolella kun items > 5

---

## Duplikaattinäkymä

```
┌──────────────────────────────────────────┐
│  POSSIBLE DUPLICATES               [PRO] │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │  IMG_20260115.jpg                  │  │  ← titleSmall
│  │  4.2 MB × 2 copies                │  │  ← bodySmall, onSurfaceVariant
│  │                                    │  │
│  │  ✓  /DCIM/Camera/...        Keep  │  │  ← healthy-väri
│  │  ☐  /Pictures/WhatsApp/...        │  │  ← valittuna poistettavaksi
│  └────────────────────────────────────┘  │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │  video_2026.mp4                    │  │
│  │  890 MB × 2 copies                │  │
│  │                                    │  │
│  │  ✓  /DCIM/Camera/...        Keep  │  │
│  │  ☐  /Downloads/...                │  │
│  └────────────────────────────────────┘  │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │     Delete duplicates (894 MB)     │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

- Ryhmä = oma kortti (`surfaceContainer`, 16dp pyöristys)
- "Keep"-merkintä: `statusColors.healthy` väri + check-ikoni
- Polku: `bodySmall`, `TextMuted`, `maxLines = 1`, `Ellipsis` alusta (näyttää loppuosan)
- Käyttäjä voi vaihtaa "Keep"-kohdetta napauttamalla

---

## Historiakaavio

```
┌──────────────────────────────────────────┐
│  STORAGE HISTORY                   [PRO] │
│                                          │
│  [Day] [Week] [Month] [All]             │  ← FilterChip-rivi
│                                          │
│  Week · Used Space                       │  ← labelLarge, primary
│  ┌────────────────────────────────────┐  │
│  │         ╱‾‾‾‾‾‾‾╲                 │  │  ← TrendChart
│  │   ╱‾‾‾╱          ╲────╲           │  │
│  │ ╱                       ╲──        │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

Täsmälleen sama pattern kuin `BatteryHistoryPanel`. Käyttää olemassa olevaa `TrendChart`-komponenttia. Data: `StorageReadingDao.getReadingsSince()` → `usedBytes` mapattuna float-listaksi.

---

## Roskakori-osio

```
┌──────────────────────────────────────────┐
│  ○  Trash                      345 MB   │  ← IconCircle(Delete, Red) + koko
│     23 items                             │  ← bodySmall
│                       [Empty trash ›]    │  ← TextButton, error-väri
└──────────────────────────────────────────┘
```

- Integroitu ActionCard-osioon, mutta koko näytetään heti (ei "Scan"-vaihetta)
- "Empty trash" on `error`-värinen koska tuhoaa dataa
- API < 30: osio piilotetaan kokonaan

---

## Quick Actions

```
┌──────────────────────────────────────────┐
│  Quick Actions                           │  ← CardSectionTitle
│                                          │
│  ○  Storage Settings                 ›   │
│  ─────────────────────────────────────── │
│  ○  Free Up Space                    ›   │
│  ─────────────────────────────────────── │
│  ○  Usage Access Settings            ›   │
└──────────────────────────────────────────┘
```

- `MetricRow`-tyylinen lista ikonilla ja nuolella
- Jokainen rivi on `clickable`, avaa Intent-toiminnon
- `IconCircle` vasemmalla, `ChevronRight` oikealla
- Rivi näytetään vain jos `Intent.resolveActivity()` löytää kohteen

---

## Navigaatiorakenne

```
StorageDetailScreen
├── Hero Card (info)
├── Media Breakdown (info)
├── Cleanup Tools (ActionCard-lista)
│   ├── → LargeFilesScreen (push-navigaatio)
│   ├── → OldDownloadsScreen (push-navigaatio)
│   ├── APK cleanup (inline skannaus + delete)
│   ├── Screenshots cleanup (inline skannaus + delete)
│   └── Trash (inline empty)
├── Top Apps [PRO] (inline lista)
├── Duplicates [PRO] (→ DuplicatesScreen tai inline)
├── Storage History [PRO] (inline kaavio)
├── SD Card (info, jos saatavilla)
└── Quick Actions (intent-linkit)
```

Isot tiedostot ja vanhat lataukset saavat omat näkymänsä koska niissä on FilterChip-valinta + pitkä lista + batch-delete. Muut toiminnot mahtuvat inline StorageDetailScreeniin.

---

## Animaatiot

| Elementti | Animaatio | Kesto | Easing |
|-----------|-----------|-------|--------|
| ProgressRing | 0 → target | 1200ms | FastOutSlowInEasing |
| SegmentedBar segmentit | 0 → target width | 800ms | FastOutSlowInEasing |
| MiniBar (top apps) | 0 → target | 800ms | FastOutSlowInEasing |
| Sticky bottom bar | slideIn/Out vertical | 200ms | EaseOut |
| Scan progress | indeterminate | - | - |

Kaikki kunnioittavat `MaterialTheme.reducedMotion` — instant kun true.

---

## Saavutettavuus

- Kontrasti: min 4.5:1 leipäteksti, 3:1 iso teksti (WCAG AA)
- SegmentedBar: `contentDescription` listaa kategoriat ja koot screenreaderille
- Checkbox-rivit: `toggleable` semantiikka
- Toimintopainikkeet: selkeät labelit
- StatusDot-värit: aina parillistettu tekstilabelin kanssa (ei pelkkä väri)
- Kosketusalue: min 48dp kaikille interaktiivisille elementeille

---

## Yhteenveto: Uudet komponentit

| Komponentti | Tyyppi | Tiedosto | Kuvaus |
|-------------|--------|----------|--------|
| `SegmentedBar` | Canvas | `ui/components/SegmentedBar.kt` | Värisegmentoitu vaakapalkki |
| `SegmentedBarLegend` | Composable | Samassa tiedostossa | StatusDot + label + koko rivit |
| `ActionCard` | Composable | `ui/components/ActionCard.kt` | Reunuksellinen kortti toimintopainikkeella |
| `FileListItem` | Composable | `ui/storage/FileListItem.kt` | Checkbox + nimi + koko + meta |
| `StickyBottomAction` | Composable | `ui/storage/StickyBottomAction.kt` | Kiinteä alapalkki delete-painikkeella |

Kaikki muut käytetyt komponentit ovat jo olemassa.
