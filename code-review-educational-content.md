# Code Review — Educational Content System

Kattava koodintarkistus kolmitasoisesta opetussisältöjärjestelmästä (Info Bottom Sheets, Contextual Info Cards, Learn Section).

Tarkistettu: 2026-03-20
Tiedostoja: 19 uutta + 21 muokattua

---

## Yhteenveto

| Kategoria | Tila |
|-----------|------|
| Kompilointivirheet | OK — ei löytynyt |
| String-resurssit | OK — kaikki ~230 EN+FI paria paikoillaan |
| Navigaatio | OK — Screen, NavGraph, argumentit |
| State-hallinta | OK — activeInfoSheet, dismissedInfoCards |
| Arkkitehtuuri | OK — MVVM, Clean Architecture noudatettu |
| Saavutettavuus | 1 huomio |
| Lokalisointi | 1 huomio |
| UX | 2 huomiota |

---

## Löydetyt korjattavat

### 1. Kovakoodattu teksti LearnArticleDetailScreenissä
**Tiedosto:** `ui/learn/LearnArticleDetailScreen.kt`, rivi ~40
**Ongelma:** `"Article not found"` on englanniksi suoraan koodissa
**Korjaus:** Siirrä `strings.xml`:ään (`learn_article_not_found` / `Artikkelia ei löytynyt`)
**Prioriteetti:** Matala — reunatapaus jota ei pitäisi normaalisti osua

### 2. InfoCard saavutettavuus — dismiss-painikkeen contentDescription
**Tiedosto:** `ui/components/info/InfoCard.kt`, rivi ~83
**Ongelma:** `contentDescription = null` Close-ikonissa — TalkBack ei kerro mitä painike tekee
**Korjaus:** Lisää `contentDescription = stringResource(R.string.a11y_dismiss_card)` tai vastaava
**Prioriteetti:** Keskitaso — saavutettavuusongelma

---

## Tarkistetut kohteet — OK

### Komponentit (Phase 1A)

| Tiedosto | Status | Huomiot |
|----------|--------|---------|
| `info/InfoSheetContent.kt` | OK | Data class, @StringRes-annotaatiot oikein |
| `info/InfoIcon.kt` | OK | 16dp ikoni, 48dp kosketusalue, a11y-teksti |
| `info/InfoBottomSheet.kt` | OK | ModalBottomSheet pattern, scrollable, AnimatedVisibility |
| `info/InfoCard.kt` | OK* | *dismiss-painikkeen a11y puuttuu (ks. yllä) |
| `info/CrossLinkButton.kt` | OK | AutoMirrored-ikoni RTL-tukeen |

### MetricRow / MetricPill muutokset

| Tiedosto | Status | Huomiot |
|----------|--------|---------|
| `MetricRow.kt` | OK | `onInfoClick` lisätty nullable-parametrina, ei riko olemassa olevia kutsuja |
| `MetricPill.kt` | OK | Sama pattern, Row + InfoIcon oikein |

### Info-sisältötiedostot (Phase 1B–1E)

| Tiedosto | Metriikat | Status |
|----------|-----------|--------|
| `BatteryInfoContent.kt` | 12 | OK — kaikki R.string-viittaukset löytyvät |
| `ThermalInfoContent.kt` | 4 | OK |
| `NetworkInfoContent.kt` | 8 | OK |
| `StorageInfoContent.kt` | 4 | OK |
| `SpeedTestInfoContent.kt` | 4 | OK |

### Detail screen -kytkentä (Phase 1B–1E)

| Screen | activeInfoSheet | InfoBottomSheet sijoitus | onInfoClick-threading |
|--------|----------------|--------------------------|----------------------|
| BatteryDetailScreen | OK (BatteryContent) | OK — PullToRefreshWrapper ulkopuolella | OK — sub-composables saavat callbackin |
| ThermalDetailScreen | OK (ThermalContent) | OK — PullToRefreshWrapper ulkopuolella | OK — ThermalMetricsCard saa callbackin |
| NetworkDetailScreen | OK (NetworkContent) | OK — PullToRefreshWrapper ulkopuolella | OK — Hero, ConnectionDetails, SpeedTestSummary |
| StorageDetailScreen | OK (StorageContent) | OK — PullToRefreshWrapper ulkopuolella | OK — Hero, Details |
| SpeedTestScreen | OK (SpeedTestContent) | OK — ulkopuolella scrollable-sisällön | OK — SpeedMetricsCard |

### Contextual Info Cards (Phase 2)

| Kohde | Status | Huomiot |
|-------|--------|---------|
| `InfoCard.kt` | OK | drawBehind accent, dismiss-callback |
| `UserPreferencesRepository` | OK | getDismissedInfoCards + dismissInfoCard |
| `UserPreferencesRepositoryImpl` | OK | stringSetPreferencesKey, oikea add-logiikka |
| `BatteryUiState` / ViewModel | OK | dismissedInfoCards virtaa, dismissInfoCard metodi |
| `ThermalUiState` / ViewModel | OK | combine-flowssa mukana |
| `NetworkUiState` / ViewModel | OK | erillinen collectDismissedCards-job |
| `StorageUiState` / ViewModel | OK | combine-flowssa mukana |

**Info Card -ehdot tarkistettu:**
- `HEALTH_80_PERCENT`: healthPercent < 90 && !dismissed ✓
- `DIES_BEFORE_ZERO`: healthPercent < 80 && !dismissed ✓
- `CHARGING_HABITS`: lataa && !dismissed ✓
- `SCREEN_OFF_DRAIN`: screenOffDrainRate > 2 && !dismissed ✓
- `THROTTLING_EXPLAINER`: aina && !dismissed ✓
- `HEAT_BATTERY_LOOP`: batteryTemp > 35 && !dismissed ✓
- `WEAK_SIGNAL_DRAIN`: Poor/NoSignal && !dismissed ✓
- `SPEED_TEST_INFO`: aina && !dismissed ✓
- `FULL_STORAGE_SLOW`: usage > 75% && !dismissed ✓
- `STORAGE_OVERVIEW`: aina && !dismissed ✓

### Learn Section (Phase 3)

| Tiedosto | Status | Huomiot |
|----------|--------|---------|
| `LearnModels.kt` | OK | Enum + data class |
| `LearnArticleCatalog.kt` | OK | 14 artikkelia, crossLinkRoute oikein |
| `LearnScreen.kt` | OK | Topic-ryhmittely, CardSectionTitle |
| `LearnArticleCard.kt` | OK | Card pattern, read time |
| `LearnArticleDetailScreen.kt` | OK* | *"Article not found" kovakoodattu (ks. yllä) |
| `CrossLinkButton.kt` | OK | TextButton + ArrowForward |

**Body-tekstin parsinta:**
- `split("\\n\\n")` on oikein — Android-resurssit palauttavat `\n` oikeina rivinvaihtoina
- `## ` prefix → `titleSmall` otsikko, muu → `bodyMedium` ✓

### Navigaatio

| Kohde | Status |
|-------|--------|
| `Screen.Learn` | OK — `"learn"` route |
| `Screen.LearnArticle` | OK — `"learn/$articleId"`, ROUTE = `"learn/{articleId}"` |
| `NavGraph` — Learn composable | OK — navArgument StringType |
| `NavGraph` — LearnArticle composable | OK — articleId argumentti parsitaan oikein |
| `HomeScreen` — onNavigateToLearn | OK — threading HomeScreen → HomeContent → NavGraph |
| `HomeScreen` — Learn ListRow | OK — MenuBook-ikoni Quick Tools -kortissa |

### String-resurssit

| Kategoria | EN | FI | Status |
|-----------|----|----|--------|
| Info common (5) | ✓ | ✓ | OK |
| Battery info (12 × 4-5) | ✓ | ✓ | OK |
| Thermal info (4 × 4-5) | ✓ | ✓ | OK |
| Network info (8 × 4) | ✓ | ✓ | OK |
| Storage info (4 × 4) | ✓ | ✓ | OK |
| Speed Test info (4 × 4) | ✓ | ✓ | OK |
| Info cards (10 × 2) | ✓ | ✓ | OK |
| Learn UI (9) | ✓ | ✓ | OK |
| Learn articles (14 × 3) | ✓ | ✓ | OK |

---

## Yleishyödyllinen tarkistuslista runcheck-projektille

Tämä lista sopii mille tahansa muutokselle tässä projektissa.

### Kompilointikelposuus
- [ ] Kaikki uudet Kotlin-tiedostot ovat oikeassa paketissa
- [ ] Kaikki importit ovat eksplisiittisiä (ei wildcard `*`)
- [ ] Ei `!!`-operaattoria — käytä `?.let`, `requireNotNull`, sealed error
- [ ] Ei Android-importteja domain-kerroksessa (`domain/`)
- [ ] Ei data-kerroksen importteja UI-kerroksessa (ViewModel injektoi vain use caset / repository-interfacet)

### String-resurssit
- [ ] Kaikki käyttäjälle näkyvät tekstit `strings.xml`:ssä (ei kovakoodattuja)
- [ ] Kaikki EN-stringit löytyvät myös `values-fi/strings.xml`:stä
- [ ] Erikoismerkit escapoitu XML:ssä (`\'`, `&amp;`, `%1$s` parametrit)
- [ ] String-nimet noudattavat konventiota: `näkymä_konteksti_kuvaus` (esim. `battery_voltage`)

### Compose & UI
- [ ] Composable-funktiot nimetty substantiiveina (ProgressRing, InfoCard)
- [ ] Composablet pidetty pieninä (< ~50 riviä, muuten extract)
- [ ] Modifier-parametri on `modifier: Modifier = Modifier` ja sijoitettu parametrilistan alkuun (oletusarvojen jälkeen)
- [ ] Kaikki värit tulevat `MaterialTheme.colorScheme` tai `MaterialTheme.statusColors` -kautta, ei kovakoodattuja
- [ ] Dividerit: `outlineVariant.copy(alpha = 0.35f)` — ei muita värejä
- [ ] Kulmat: 16dp korteille, 8dp pienille elementeille
- [ ] Kosketusalue vähintään 48dp
- [ ] Spacing-tokenien käyttö: `MaterialTheme.spacing` (xs/sm/base/lg/xl)

### Saavutettavuus
- [ ] Interaktiivisilla elementeillä on `contentDescription`
- [ ] StatusDotin/värin rinnalla aina tekstilabel
- [ ] `semantics(mergeDescendants = true)` ryhmitellyillä elementeillä
- [ ] `liveRegion` dynaamisesti päivittyvillä arvoilla
- [ ] Animaatiot kunnioittavat `MaterialTheme.reducedMotion`

### State & arkkitehtuuri
- [ ] ViewModel ei pidä Context-viitettä
- [ ] UiState on `sealed interface` (Loading / Success / Error)
- [ ] StateFlow ViewModel → UI
- [ ] `rememberSaveable` tiloille jotka pitää säilyä konfiguraatiomuutoksissa
- [ ] DataStore-preferenssit Flow-pohjaisina, ei suoria lukuja
- [ ] Hilt @Inject konstruktorissa, @Binds interfaceille

### Navigaatio
- [ ] Screen-routes uniikkeja eikä kollidoi olemassa olevien kanssa
- [ ] NavGraph-argumentit parsitaan navArgument-tyypeillä
- [ ] navigateSingleTop estää duplikaattipinon
- [ ] onBack = popBackStack

### Bottom Sheets & Dialogit
- [ ] ModalBottomSheet EI OLE scrollable-sisällön (Column/LazyColumn) sisällä
- [ ] skipPartiallyExpanded = true jos halutaan aina täysi korkeus
- [ ] onDismissRequest käsitelty

### Testattavuus
- [ ] Uudet use caset: onko yksikkötestejä?
- [ ] Uudet UI-komponentit: onko preview-funktioita?
- [ ] Repository-muutokset: mock/fake päivitetty testeissä?

### Lokalisointi
- [ ] FI-käännösten oikeellisuus (oikeinkirjoitus, luontevuus)
- [ ] Parametrilliset stringit: `%1$s`, `%1$d` — järjestys voi vaihdella kielittäin
- [ ] Pitkät tekstit mahtuvat UI:hin (erityisesti Learn-artikkelit)

### Git & build
- [ ] Ei salaisuuksia (.env, avaimia) commitissa
- [ ] Ei uusia tiedostoja `app/src/main/res/` alle jotka eivät ole XML (CLAUDE.md ym.)
- [ ] Build testattu erikseen (user tekee)
