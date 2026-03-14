# runcheck — Phase 1 Completion & Roadmap

**Päivämäärä:** 2026-03-10
**Tila:** Kaikki vaiheet (Phase 1-4) valmiit — julkaisuvalmis
**Repo:** https://github.com/Insaner1980/runcheck

### Toteutetut korjaukset (Phase 1 — Sessiot A-D):
- [x] Gradle wrapper 9.4.0 luotu
- [x] `.gitignore` lisätty
- [x] `settings.gradle.kts` korjattu (`dependencyResolutionManagement`)
- [x] App-ikonit luotu (adaptive icon + PNG fallback kaikille densiteeteille)
- [x] Room schema location lisätty `build.gradle.kts`:ään
- [x] Kaikki riippuvuudet päivitetty uusimpiin (AGP 9.1.0, Kotlin 2.3.0, Compose BOM 2026.02.01, Hilt 2.59.2, Room 2.8.4, Vico 3.0.3 jne.)
- [x] `compileSdk` nostettu 36:een (Vico 3.x vaatimus)
- [x] AGP 9 + KSP 2.3.1 yhteensopivuus korjattu (`builtInKotlin=false`, `disallowKotlinSourceSets=false`)
- [x] `PullToRefreshWrapper` päivitetty käyttämään `PullToRefreshBox` (uusi M3 API)
- [x] `Android14BatterySource` korjattu käyttämään raakoja property ID:itä
- [x] Roboto Mono -fonttitiedosto ladattu
- [x] `local.properties` luotu SDK-polulla
- [x] **BUILD SUCCESSFUL** — 21MB debug APK
- [x] **29 yksikkötestiä menevät läpi**

### Toteutetut Phase 2 -ominaisuudet (Sessiot E-H):
- [x] Sparkline-kaaviot dashboard-kortteihin (akku + lämpötila)
- [x] Pull-to-refresh korjattu (LaunchedEffect-pohjainen state tracking)
- [x] AdMob SDK 25.0.0 integraatio (bannerimainos detail-näyttöihin, ei dashboardiin)
- [x] AdBanner-composable Hilt-injektoidulla Pro-tarkistuksella
- [x] Google Play Billing Library 8.3.0 integraatio
- [x] ProStatusRepository toteutettu oikeasti (ei enää stub)
- [x] Settings: "Upgrade to Pro" -osio hintoineen
- [x] HistoryPeriod: 24h / Week / Month / All -aikajännevalinta
- [x] Pro-käyttäjien laajennettu historia (FilterChip-napit)

---

## 1. Nykytilan analyysi

### 1.1 Mitä on tehty

Koko Phase 1 MVP on toteutettu yhdessä commitissa Claude Code webissä (7238d7d). Kaikki
tiedostot sisältävät oikeaa tuotantokoodia. Arkkitehtuuri noudattaa MVVM + Clean Architecture
-mallia: data → domain → UI.

| Kerros | Tiedostoja | Tila |
|--------|-----------|------|
| Build & konfiguraatio | 5 | Valmis |
| Manifest & resurssit | 5 | Valmis |
| Entry pointit | 2 | Valmis |
| Data-kerros | ~32 | Valmis |
| Domain-kerros | ~14 | Valmis |
| DI (Hilt) | 2/3 | DomainModule puuttuu |
| UI-teema | 5/6 | Shape.kt puuttuu |
| UI-komponentit | 10 | Valmis |
| Navigointi | 4 | Valmis |
| Näytöt + ViewModelit | 15 | Valmis |
| Taustapalvelut | 4 | Valmis |
| Testit | 2/5 | 3 testiä puuttuu |

### 1.2 Kriittiset puutteet (estävät kääntämisen)

Nämä ovat asioita, jotka todennäköisesti estävät projektin kääntymisen:

| # | Puute | Selitys | Prioriteetti |
|---|-------|---------|-------------|
| 1 | **Gradle wrapper puuttuu** | Ei `gradlew`, `gradlew.bat` eikä `gradle/wrapper/` -hakemistoa. Projekti ei käänny ilman näitä. | KRIITTINEN |
| 2 | **App-ikonit puuttuvat** | Manifest viittaa `@mipmap/ic_launcher`, mutta `res/mipmap-*/` -hakemistoja ei ole. Build kaatuu. | KRIITTINEN |
| 3 | **`.gitignore` puuttuu** | Build-artifaktit, `.gradle/`, `local.properties` jne. pääsevät repoon. | KORKEA |
| 4 | **`settings.gradle.kts` käyttää `dependencyResolution`** | Pitäisi olla `dependencyResolutionManagement`. Tämä on kirjoitusvirhe joka kaataa buildin. | KRIITTINEN |

### 1.3 Todennäköiset käännösvirheet (vaatii Android Studion)

Nämä eivät ole varmoja, mutta todennäköisiä ongelmia jotka paljastuvat kääntäessä:

| # | Riski | Selitys |
|---|-------|---------|
| 1 | Vico 2.0.0-beta.3 API-muutokset | Beta-kirjasto, API voi olla erilainen kuin mitä koodi olettaa. TrendChart.kt ja SparklineChart.kt voivat vaatia korjauksia. |
| 2 | Hilt WorkManager -integraatio | `hilt-work-compiler` viittaa `androidx.hilt:hilt-compiler:1.2.0` — tämä on eri kuin Dagger Hilt compiler. Voi aiheuttaa ristiriitoja. |
| 3 | `MeasuredValue` sijainti | Suunnitelmassa `data/battery/MeasuredValue.kt`, mutta koodissa käytetään `domain/model/MeasuredValue.kt`. Tämä on jo korjattu koodissa, mutta importit voivat olla ristiriitaisia. |
| 4 | Room schema export | `exportSchema = true` mutta `room.schemaLocation` ei ole määritelty build.gradle.kts:ssä. |
| 5 | Reduced motion -reflektio | `AccessibilityManager.isReducedMotionEnabled` ei ole julkinen API. Reflektio voi epäonnistua ilman varoitusta (käsitelty try-catchillä, ei kaada). |

### 1.4 Riippuvuusversiot — tarkistettava

Koodissa olevat versiot ovat joulukuulta 2024. Maaliskuussa 2026 nämä ovat vanhoja.
Tarkistettava uusimmat versiot **ennen** ensimmäistä kääntämistä:

| Riippuvuus | Nykyinen | Huomio |
|-----------|----------|--------|
| AGP | 8.7.3 | Tarkista uusin |
| Kotlin | 2.1.0 | Tarkista uusin |
| KSP | 2.1.0-1.0.29 | Pitää vastata Kotlin-versiota |
| Compose BOM | 2024.12.01 | Tarkista uusin |
| Hilt | 2.53.1 | Tarkista uusin |
| Room | 2.6.1 | Tarkista uusin |
| Vico | 2.0.0-beta.3 | Tarkista onko stable-versio |
| WorkManager | 2.10.0 | Tarkista uusin |
| Core KTX | 1.15.0 | Tarkista uusin |
| Activity Compose | 1.9.3 | Tarkista uusin |

---

## 2. Phase 1 — Viimeistely

### Sessio A: Build-infrastruktuuri (estää kaiken muun)

Ilman näitä projekti ei käänny eikä mitään voi testata.

**A1. Gradle wrapper**
- Asenna Gradle wrapper projektin juureen
- Tarvitaan `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`,
  `gradle/wrapper/gradle-wrapper.properties`
- Vaatii toimivan Gradle-asennuksen tai Android Studion

**A2. `.gitignore`**
- Lisää standardi Android `.gitignore`:
  ```
  *.iml
  .gradle/
  /local.properties
  .idea/
  /build/
  /app/build/
  /captures/
  .externalNativeBuild/
  .cxx/
  *.apk
  *.aab
  ```

**A3. Korjaa `settings.gradle.kts`**
- `dependencyResolution` → `dependencyResolutionManagement`

**A4. App-ikonit**
- Lisää `res/mipmap-*/ic_launcher.webp` ja `ic_launcher_round.webp`
- Vaihtoehtoisesti: luo adaptive icon XML:llä (`ic_launcher.xml` + vektori-foreground)
- Vähintään: placeholder-ikoni joka sallii kääntymisen

**A5. Room schema location**
- Lisää `app/build.gradle.kts`:ään:
  ```kotlin
  ksp {
      arg("room.schemaLocation", "$projectDir/schemas")
  }
  ```

**A6. Päivitä riippuvuusversiot**
- Hae uusimmat versiot kaikille riippuvuuksille
- Erityisesti: tarkista Vico-kirjaston tila (beta → stable?)
- Päivitä `gradle/libs.versions.toml`

### Sessio B: Puuttuvat tiedostot

**B1. `di/DomainModule.kt`** (matala prioriteetti)
- Hilt löytää `@Inject`-konstruktorit automaattisesti
- Mutta eksplisiittinen moduuli on selkeämpi ja suunnitelman mukainen
- Sisältää: HealthScoreCalculator singleton -binding

**B2. `ui/theme/Shape.kt`** (matala prioriteetti)
- M3:n default-muodot toimivat, mutta speksi määrittelee:
  - Medium (16dp) korteille
  - Full (999dp) badgeille ja chipeille
- Lisää Shape.kt + integrointi Theme.kt:hen

### Sessio C: Puuttuvat testit

**C1. `CalculateHealthScoreUseCaseTest.kt`**
- Testaa: flow yhdistää kaikki 4 tilaa ja laskee tuloksen
- Käyttää fake-repositoryja

**C2. `DeviceCapabilityManagerTest.kt`**
- Testaa: validoi CURRENT_NOW (plausible range, muuttuva arvo)
- Testaa: yksikön tunnistus (microamps vs milliamps)
- Testaa: signaalikonvention tunnistus
- Testaa: thermal zone -skannaus
- Vaatii mockattua BatteryManageria ja tiedostojärjestelmää

**C3. `GenericBatterySourceTest.kt`**
- Testaa: normalizeCurrent() eri yksiköillä ja signaalikonventioilla
- Testaa: confidence-tason asettaminen DeviceProfilen perusteella
- Testaa: BatteryManager-arvojen mappaus enum-arvoiksi

### Sessio D: Käännöstesti ja korjaukset

**D1. Ensimmäinen kääntäminen**
- Android Studiossa tai komentoriviltä `./gradlew assembleDebug`
- Todennäköisesti tuottaa 10-30 käännösvirhettä (normaalia AI-generoidulle koodille)
- Korjataan iteratiivisesti

**D2. Tyypilliset korjaukset**
- Import-polut jotka eivät täsmää
- Vico-kirjaston API-muutokset
- Room-annotaatiovirheet (KSP paljastaa)
- Hilt-konfiguraatiovirheet
- Puuttuvat string-resurssit

**D3. Ensimmäinen käynnistys emulaattorissa**
- Tarkista: sovellus käynnistyy, dashboard latautuu
- Tarkista: navigointi toimii (kaikki näytöt)
- Tarkista: teema vaihtuu (light/dark/AMOLED)

---

## 3. Phase 2 — Polish & Pro Foundation

Speksin mukaiset ominaisuudet (device-health-monitor-spec.md, Phase 2):

### Sessio E: Dashboard-viimeistely
- Sparkline-kaaviot dashboard-korteissa (pohja SparklineChart.kt:ssä on)
- Gauge-animaatioiden hienosäätö oikeilla laitteilla
- Pull-to-refresh -toiminnallisuuden testaus ja viimeistely

### Sessio F: Mainos-integraatio
- Google AdMob SDK riippuvuus
- Bannerimainos detail-näyttöjen alaosaan (EI dashboardiin)
- Mainoksen piilotus mittauksen aikana
- Consent-hallinta (GDPR — EU-vaatimus)

### Sessio G: In-App Purchase
- Google Play Billing Library 7.x
- ProStatusRepository: todellinen toteutus (nyt stub)
- Osto-flow: Settings → "Upgrade to Pro" → Google Play -dialogi
- Ostojen validointi ja palautus
- Pro-ominaisuuksien avautuminen: laajennettu historia, ei mainoksia

### Sessio H: Laajennettu historia (Pro)
- Muuta CleanupOldReadingsUseCase: Pro ei poista
- Viikko/kuukausi/kaikki-aikajänne TrendChartiin
- Aikajaksonappi detail-näytöille
- Akun terveyden seuranta kuukausitasolla

### Sessio I: Kotinäyttöwidgetit (Pro)
- Battery widget (2x1): taso, lämpö, virta
- Health score widget (2x2): kokonaispisteet + 4 mini-indikaattoria
- Network widget (2x1): signaalivahvuus, tyyppi, latenssi
- Glance API (Compose-pohjainen widget-framework)

---

## 4. Phase 3 — Pro-ominaisuudet

### Sessio J: Laturi/kaapeli-vertailu
- ChargerProfile ja ChargingSession -entityt (Room-skeema on jo speksattu)
- Lataussession automaattinen tallennus
- Vertailunäkymä
- Kaapelin tunnistus suorituskyvyn perusteella

### Sessio K: Per-app-akunkäyttö
- PACKAGE_USAGE_STATS -luvan pyyntö
- Etuala-sovelluksen tunnistus + virrankulutuksen korrelaatio
- Päivittäinen/viikoittainen sovelluskohtainen ranking

### Sessio L: Thermal throttling -loki
- Throttling-tapahtumien tallennus
- Korrelaatio sovelluksen käytön kanssa
- Lämpötila-aikajana tapahtumamarkkereilla

### Sessio M: Enhanced notifications + CSV export
- Reaaliaikaiset akku-tiedot persistent-ilmoituksessa
- Mukautettavat hälytysrajat
- Lataus valmis -yhteenveto
- CSV-vienti historiallisesta datasta

---

## 5. Phase 4 — Julkaisu

### Sessio N: Play Store -valmistelu
- Sovellusikoni (lopullinen design)
- Feature graphic (1024x500)
- Kuvakaappaukset (puhelin + tabletti)
- Lokalisointi: englanti + suomi
- Play Store -listaus (otsikko, kuvaus, ASO-avainsanat)
- Tietosuojakäytäntö-sivu

### Sessio O: Beta-testaus
- Sisäinen testaus (closed beta)
- Laitekohtaisten ongelmien kerääminen
- DeviceProfile-tietokannan laajentaminen käyttäjädatan perusteella
- Crash-raportointi (Firebase Crashlytics tai vastaava)

### Sessio P: Julkaisu
- Production release Google Play -kauppaan
- Play Store -listauksen optimointi ensimmäisten päivien perusteella
- Käyttäjäpalautteen seuranta

---

## 6. Toteutusjärjestys ja riippuvuudet

```
Phase 1 viimeistely (estää kaiken muun):
  A (Build-infra) ──→ D (Käännöstesti) ──→ B + C (Puuttuvat tiedostot/testit)
                                              │
Phase 2:                                      ▼
  E (Dashboard polish) ──→ F (AdMob) ──→ G (IAP) ──→ H (Historia) ──→ I (Widgetit)
                                              │
Phase 3:                                      ▼
  J (Laturi-vertailu) ── K (Per-app) ── L (Thermal log) ── M (Notifications + CSV)
                                              │
Phase 4:                                      ▼
  N (Play Store) ──→ O (Beta) ──→ P (Julkaisu)
```

---

## 7. Ensimmäinen konkreettinen askel

**Sessio A on kriittinen polku.** Ilman toimivaa build-ympäristöä mitään muuta ei voi tehdä.

Vaatimukset:
1. Android SDK asennettuna (API 35)
2. JDK 17
3. Joko Android Studio tai komentorivityökalut

Sessio A:n jälkeen `./gradlew assembleDebug` pitäisi tuottaa joko:
- Onnistunut APK (epätodennäköistä ensi yrittämällä)
- Lista konkreettisia käännösvirheitä joita korjataan Sessio D:ssä

---

## 8. Riskiarvio

| Riski | Todennäköisyys | Vaikutus | Mitigaatio |
|-------|---------------|----------|------------|
| Vico API rikkoutunut | Korkea | Kaaviot eivät käänny | Päivitä stable-versioon tai vaihda kirjasto |
| Room + KSP -yhteensopivuus | Keskitaso | Build kaatuu | Päivitä molemmat yhdessä |
| Hilt WorkManager -integraatio | Keskitaso | Worker ei käynnisty | Tarkista HiltWorkerFactory-konfiguraatio |
| Manufacturer-kohtainen koodi | Matala (MVP) | Väärät arvot tietyillä laitteilla | Confidence-järjestelmä suojaa jo |
| Play Store -hylkäys | Matala | Julkaisu viivästyy | Seuraa ohjeet tarkkaan, ei harhaanjohtavia väitteitä |
