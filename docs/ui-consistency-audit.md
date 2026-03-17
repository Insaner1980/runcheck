# UI Consistency Audit — runcheck

Auditoitu 2026-03-16. Kattaa kaikki ui/-kansion tiedostot.

---

## 1. Värien käyttö data-arvoissa vs. statuksissa

**Ongelma:** Etusivun GridCard-korteissa faktatiedot ja statusteksti ovat samaa väriä.

| Kortti | Nykyinen | Ehdotus |
|--------|----------|---------|
| Network | `The Net_5G · Good` kaikki vihreänä | `The Net_5G` onSurface, `Good` statusväri |
| Thermal | `29.8°C · Cool` kaikki vihreänä | `29.8°C` onSurface, `Cool` statusväri |
| Storage | `185 GB free` kaikki vihreänä | `185 GB free` onSurface (ei statusarviota) |

**Tarvittava muutos:** GridCard tarvitsee tuen kahdelle erilliselle tekstille tai AnnotatedString-pohjaiselle subtitle-parametrille.

**Tiedostot:** `HomeScreen.kt`, `GridCard.kt`

---

## 2. Hardcoded spacing arvot

**Ongelma:** Osa näytöistä käyttää `MaterialTheme.spacing.*` tokeneita, osa hardcoodaa dp-arvoja.

**Spacing-tokenit:** xs=4, sm=8, md=12, base=16, lg=24, xl=32

| Tiedosto | Rivi | Nyt | Pitäisi |
|----------|------|-----|---------|
| HomeScreen.kt | 267 | `padding(horizontal = 16.dp)` | `spacing.base` |
| HomeScreen.kt | 270 | `Spacer(height = 6.dp)` | `spacing.xs` (tai uusi 6dp token) |
| HomeScreen.kt | 312, 350 | `Arrangement.spacedBy(8.dp)` | `spacing.sm` |
| HomeScreen.kt | 386, 427 | `Spacer(height = 24.dp)` | `spacing.lg` |
| HomeScreen.kt | 522 | `Spacer(height = 32.dp)` | `spacing.xl` |
| GridCard.kt | 58 | `padding(horizontal = 14.dp, vertical = 16.dp)` | `spacing.md` + `spacing.base` |
| GridCard.kt | 73 | `Spacer(height = 14.dp)` | `spacing.md` (12dp, lähin token) |
| NetworkDetailScreen.kt | 697 | `Spacer(height = 4.dp)` | `spacing.xs` |

**Prioriteetti:** Keskitaso. Visuaalisesti lähes huomaamaton, mutta ylläpidettävyys kärsii.

---

## 3. Divider-värien epäyhtenäisyys

**Ongelma:** Kaksi eri divider-tyyliä käytössä.

| Tyyli | Käyttöpaikat |
|-------|-------------|
| `color = BgIconCircle` (hardcoded import) | HomeScreen, SettingsScreen, ThermalDetailScreen |
| `outlineVariant.copy(alpha = 0.35f)` | NetworkDetailScreen (IP & DNS -osio) |
| `outlineVariant.copy(alpha = 0.45f)` | MetricRow-komponentti |

**Ehdotus:** Yhtenäistä kaikki käyttämään `outlineVariant.copy(alpha = 0.35f)` tai määrittele teemaan `MaterialTheme.dividerColor` -extensio.

**Tiedostot:** `HomeScreen.kt`, `SettingsScreen.kt`, `ThermalDetailScreen.kt`, `NetworkDetailScreen.kt`, `MetricRow.kt`

---

## 4. SectionHeader vs CardSectionTitle -värilogiikka

**Nykyiset erot:**

| Komponentti | Käyttötarkoitus | Väri | Arvo |
|-------------|----------------|------|------|
| SectionHeader | Sivutason osiot | `outline` | TextMuted `#506068` |
| CardSectionTitle | Kortin sisäiset osiot | `onSurfaceVariant` | TextSecondary `#90A8B0` |

**Arvio:** Hierarkia on oikea (kortin sisäinen näkyvämpi kuin sivutason otsikko), mutta saattaa hämmentää. Dokumentoitava selkeästi tai yhtenäistettävä.

---

## 5. MetricPill valueColor -oletusarvo

**Ongelma:** Fallback-väri vaihtelee eri käyttöpaikoissa.

| Paikka | Fallback |
|--------|----------|
| MetricPill oletusarvo | `onSurface` |
| ThermalDetailScreen (rivi ~439) | `onSurfaceVariant` |

**Ehdotus:** Kaikki fallbackit `onSurface`-arvoon. `onSurfaceVariant` vain labeleissa.

---

## 6. Fontti: Manrope vs. CLAUDE.md-spesifikaatio

**Ongelma:** CLAUDE.md sanoo "System Roboto (no custom fonts)", mutta `Type.kt` käyttää Manrope-fonttiperhettä.

**Vaihtoehdot:**
1. Päivitä CLAUDE.md kuvaamaan nykyinen tilanne (Manrope + JetBrains Mono)
2. Vaihda takaisin Roboto-systeemifonttiin

**Kommentti:** Manrope näyttää hyvältä sovelluksessa, joten CLAUDE.md:n päivitys on järkevämpää.

---

## 7. Hardcoded fontSize -ylikirjoitukset

**Ongelma:** `displayLarge` käytetään `.copy(fontSize = ...)`-ylikirjoituksella.

| Tiedosto | Rivi | Koko |
|----------|------|------|
| HomeScreen.kt (health score) | ~614 | `displayLarge.copy(fontSize = 48.sp)` |
| HomeScreen.kt (battery %) | ~704 | `displayLarge.copy(fontSize = 54.sp)` |
| ThermalDetailScreen (lämpötila) | ~259 | `displayLarge.copy(fontSize = 48.sp)` |

**Arvio:** Matala prioriteetti. Nämä ovat hero-lukemia joissa eri koko on tarkoituksellinen. Jos halutaan yhtenäisyys, voidaan määritellä teemaan `displayHero` custom style.

---

## 8. Settings-napin väri

**Ongelma:** `SettingsScreen.kt` (rivi ~275) käyttää `BgPage`-väriä nappitekstissä hardcoded-importtina.

**Ehdotus:** Käytä `MaterialTheme.colorScheme.onPrimary`.

---

## 9. GridCard sisäinen spacing

**Ongelma:** GridCard käyttää `14.dp` ja `6.dp`, jotka eivät vastaa mitään spacing-tokenia.

| Paikka | Nyt | Lähin token |
|--------|-----|-------------|
| Horizontal padding | 14.dp | `spacing.md` (12dp) tai `spacing.base` (16dp) |
| Title-subtitle gap | 6.dp | `spacing.xs` (4dp) tai `spacing.sm` (8dp) |

**Ehdotus:** Käytä `spacing.md` (12dp) horizontal ja `spacing.sm` (8dp) gap, tai lisää 6dp token.

---

## 10. ProBadgePill kulmaradius

**Ongelma:** ProBadgePill käyttää 8dp kulmia, kaikki kortit käyttävät 16dp.

**Arvio:** Ei varsinainen ongelma — pieni badge-elementti saa olla pienempi. Dokumentoitava periaatteeksi: kortit 16dp, pienet elementit (badge, chip) 8dp.

---

## Korjausten tila

| # | Muutos | Tila |
|---|--------|------|
| 1 | Data vs. status -värilogiikka GridCardissa | KORJATTU |
| 2 | Hardcoded spacing -> tokenit | KORJATTU (HomeScreen, GridCard, ThermalDetailScreen, SettingsScreen) |
| 3 | Divider-värien yhtenäistäminen | KORJATTU (kaikki outlineVariant 0.35f) |
| 4 | SectionHeader/CardSectionTitle dokumentointi | KORJATTU (CLAUDE.md päivitetty) |
| 5 | MetricPill fallback-väri | KORJATTU |
| 6 | CLAUDE.md fonttispesifikaation päivitys | KORJATTU |
| 7 | Hero fontSize ylikirjoitukset | Ei korjata — tarkoituksellinen, 3 käyttöpaikkaa ei tarvitse abstraktiota |
| 8 | Settings-napin väri | KORJATTU |
| 9 | GridCard spacing | KORJATTU (md + sm tokeneiksi) |
| 10 | Badge kulmaradius dokumentointi | KORJATTU (CLAUDE.md päivitetty: kortit 16dp, pienet elementit 8dp) |
