# runcheck — UI-uudistuksen yksityiskohtainen toteutussuunnitelma

> Tila: toteutusvalmis suunnitelma  
> Lähdevaatimus: `runcheck-ui-uudistus.md`  
> Kohdehaaran lähtöpiste: `codex/ui-redesign-m3-expressive` @ `88d2609`  
> Suunnitelman laatimispäivä: 2026-07-27  
> Sovellus: `runcheck` (Kotlin, Jetpack Compose, Material 3)

## 1. Tavoite ja onnistumisen määritelmä

Tämän työn tavoite on korjata nykyisen uudistetun käyttöliittymän todetut visuaaliset ja käytettävyysongelmat ilman muutoksia sovelluksen ydintoimintoihin, mittauslogiikkaan, tietokantaan, käyttöoikeuksiin, navigaatiorakenteeseen tai Pro-oikeuksien sääntöihin.

Valmis lopputulos:

1. käyttää Compose BOMin hallitsemaa vakaata Material 3 -versiota eikä Material 3 Expressive -alpha-rajapintoja;
2. säilyttää kohdehaarassa jo toteutetut System-, Light- ja Dark-teemat;
3. korjaa kaikki lähdedokumentin D1–D12-ongelmat niiden todellisen lähdekoodisyyn perusteella;
4. tekee Home-, Insights-, Tools-, Settings-, detail-, speed test- ja cleanup-näkymistä visuaalisesti yhtenäisen kokonaisuuden;
5. säilyttää nykyiset reitit, ViewModelit, use caset, mittauslähteet ja Pro-portit;
6. toimii vähintään 411 × 850 dp:n perusnäkymässä, suurella fontilla, vaaleassa ja tummassa teemassa sekä reduced motion -asetuksella;
7. läpäisee kohdennetut yksikkö-, Compose UI-, käännös-, saavutettavuus- ja visuaaliset tarkistukset;
8. päivittää `AGENTS.md`, `CODEX.md`, `PROJECT.md` ja `UI-SPEC.md` vastaamaan toteutettua visuaalista järjestelmää.

## 2. Lähtötilan lähteet ja niiden etusijajärjestys

Toteutuksen aikana lähteitä käytetään tässä järjestyksessä:

1. toteutushaaran todellinen lähdekoodi;
2. tämän suunnitelman hyväksytyt päätökset ja hyväksymiskriteerit;
3. `runcheck-ui-uudistus.md`;
4. kohdehaaran `PROJECT.md` ja `UI-SPEC.md`;
5. kuvakaappaukset hakemistoissa:
   - `C:\Users\emmah\Desktop\runcheck-screenshots\runcheck-new`
   - `C:\Users\emmah\Desktop\runcheck-screenshots\runcheck-lisäkuvia`
   - `C:\Users\emmah\Desktop\runcheck-screenshots\runcheck-vielä-lisäkuvia`
6. viralliset Android Developers -dokumentit.

Jos vaatimusdokumentin diagnoosi ja lähdekoodi ovat ristiriidassa, korjataan todellinen lähdekoodisyy. Ulkonäöllinen tavoite säilytetään, mutta korjausta ei toteuteta virheellisen oletuksen varaan.

## 3. Varmistetut lähtötilahavainnot

### 3.1 Kohdehaara

- Toteutuksen oikea pohja on `codex/ui-redesign-m3-expressive` commitissa `88d2609`.
- Nykyinen checkout-haara ja kohdehaara ovat eriytyneet. Niiden suora yhdistäminen ennen UI-työtä aiheuttaisi runsaasti ristiriitoja muun muassa teema-, navigaatio-, widget-, asetukset-, kaavio- ja dokumenttitiedostoissa.
- UI-uudistus tehdään siksi erillisessä worktreessä kohdehaaran päälle.
- Nykyisen checkout-haaran myöhempi integrointi on erillinen toimitustehtävä, ei osa visuaalista uudistusta.

### 3.2 Material 3

- Kohdehaarassa on Compose BOM `2026.06.01`.
- `app/build.gradle.kts` ohittaa BOMin Material 3 -version versiolla `1.5.0-alpha24`.
- Koodi käyttää muun muassa:
  - `MaterialExpressiveTheme`;
  - `MotionScheme.expressive()`;
  - `LargeFlexibleTopAppBar`;
  - `LoadingIndicator`;
  - `HorizontalFloatingToolbar`;
  - `ExperimentalMaterial3ExpressiveApi`-opt-iniä.
- Virallisen AndroidX-taulukon mukaan Material 3:n vakaa versio on suunnitelmaa laadittaessa `1.4.0`, ja Expressive-rajapinnat ovat `1.5.0-alpha24`-linjassa.
- Vakaan Material 3:n tulee tulla BOMin kautta. Yksittäiselle `material3`-riippuvuudelle ei jätetä erillistä versiota.

### 3.3 Teemat

- Kohdehaarassa on jo persistoidut System-, Light- ja Dark-teemat.
- `Theme.kt`, DataStore-asetukset, Settings-valinta ja sovelluksen käynnistyksen splash-portti tukevat niitä.
- Uudistus ei poista teematiloja.
- Nykyisen checkoutin vanhempi “dark only” -ohje ei ole kohdehaaran toteutuksen lähtötila.

### 3.4 Kuvakaappauksista vahvistetut ongelmat

| Tunnus | Havainto | Kuvakaappausnäyttö |
|---|---|---|
| D1 | Home-kortin “Accurate” puristuu pystysuuntaiseksi kirjainjonoksi | dark ja light Home |
| D2 | Home on yläosasta tyhjä ja Insights-otsikko jää irralliseksi | Home |
| D3 | Vaalean teeman kortit eivät erotu riittävästi taustasta | light Home, Insights, Tools |
| D4 | Speed test -renkaan sisäinen radial fill näyttää tahralta | Speed Test |
| D5 | Home-mittarikortit ovat keskenään liian samanlaisia | Home |
| D6 | Health-rengas on liian ohut ja visuaalisesti heikko | Home |
| D7 | Tyhjä Insights-näkymä ei tarjoa hyödyllistä sisältöä | Insights |
| D8 | Battery Level- ja Temperature-kaavioiden Y-tekstit limittyvät | Battery charts |
| D9 | 24H/7D/30D/90D/1Y-valitsin leikkautuu ilman vieritysmerkkiä | Battery history |
| D10 | Kaaviot ovat visuaalisesti liian matalia ja viiva jää ohueksi | Battery charts |
| D11 | Sama kaavioalue näyttää yhtä aikaa Pro-lukituksen ja riittämättömän datan | Battery history |
| D12 | Mittarien korostusvärit eivät muodosta selkeää domain-järjestelmää | useita näkymiä |

### 3.5 Lähdedokumentin avoimet kysymykset, jotka voidaan sulkea

| Kysymys | Varmistettu päätös |
|---|---|
| Network detail -yläosan sisältö | Nykyinen dBm- ja latency-hero säilytetään toiminnallisesti ja sovitetaan uuteen visuaaliseen järjestelmään. |
| Thermal detail -yläosan sisältö | Nykyinen lämpötilahero säilytetään toiminnallisesti ja sovitetaan uuteen järjestelmään. |
| App Usage “Not used” | Toiminto on jo toteutettu 30/60/90 päivän suodattimilla. Se säilytetään ja tyylitellään; uutta dataominaisuutta ei tarvita. |
| Weekly report | Näyttö, ViewModel, use case ja reitti ovat olemassa. Työssä muutetaan vain esityspaikka ja visuaalinen esitys. |

## 4. Lähdedokumenttiin tehtävät tekniset täsmennykset

### 4.1 D8: todellinen kaavion juurisyy

`TrendChart.kt` mittaa Y-akselin tekstien leveydet jo `rememberTextMeasurer()`-mekanismilla ja varaa leveyden leveimmän tekstin perusteella. Ongelma ei siis johdu puuttuvasta leveysmittauksesta.

Todellinen syy:

1. `scaleValues` sisältää datan lisäksi kaikki Y-tickit sekä `qualityZones`-alueiden minimi- ja maksimiarvot;
2. Battery Level -laatualuerajat pakottavat asteikon 0–100 alueelle, vaikka näkyvä data olisi esimerkiksi 61–80;
3. Temperature-alueet pakottavat asteikon 0–60 alueelle, vaikka data olisi esimerkiksi 30,7–33,4;
4. todellinen datasarja ja useat tick-tekstit puristuvat pieneen pystysuuntaiseen alueeseen;
5. Current-kaavio näyttää paremmalta, koska sillä ei ole samaa koko domainin pakottavaa quality zone -skaalausta.

Korjausperiaate:

- kaavion näkyvä viewport muodostetaan datasta ja nimenomaisista tick-arvoista;
- quality zone -taustat piirretään näkyvään viewportiin leikattuina;
- quality zone ei saa laajentaa viewportia;
- tickien määrä ja väli valitaan käytettävissä olevan pikselikorkeuden perusteella;
- teksti mitataan edelleen nykyisellä `TextMeasurer`-toteutuksella.

### 4.2 D9: valitsimen todellinen rakenne

`HistoryPeriodFilterChipRow.kt` delegoi geneeriseen `EnumFilterChipRow`-toteutukseen. Yli neljä valintaa käyttävä Expressive-valitsin muuttuu vaakavieritettäväksi segmented row’ksi, jonka:

- aloituskohta on aina ensimmäinen valinta;
- valittua kohtaa ei tuoda automaattisesti näkyviin;
- reunoissa ei ole häivytystä tai muuta vieritysaffordanssia;
- yhden valinnan vähimmäisleveys on suuri.

Historian periodivalitsin erotetaan omaksi komponentikseen. Geneeristä enintään neljän valinnan valitsinta ei monimutkaisteta historian erityistapauksen vuoksi.

### 4.3 Insights “Recently resolved”

Nykyinen `InsightRepository` tarjoaa vain aktiiviset insightit, unseen-määrän ja dismiss/seen-toiminnot. DAO ei tarjoa ratkaistujen insightien historialistaa.

Koska työn guardrail kieltää repository-, Room- ja domain-laajennukset:

- “Recently resolved” toteutetaan vain, jos kohdehaarassa on toteutushetkellä jo esityskerrokselle saatava ratkaistu data;
- muutoin osio jätetään pois tästä UI-uudistuksesta;
- aktiivisia insightteja ei nimetä ratkaistuiksi;
- dismissed-tilaa ei tulkita “resolved”-tilaksi;
- puuttuva data kirjataan erilliseksi tuote-/dataominaisuudeksi.

### 4.4 Mittaustilojen rajaus

Yhteinen `MeasurementState` on esityskerroksen malli, ei uusi domain-protokolla.

- Speed testissä se voidaan mapata olemassa olevasta `SpeedTestPhase`-tilasta.
- Cleanupissa se voidaan mapata olemassa olevasta `CleanupUiState`-tilasta.
- Homen refresh voi näyttää Preparing/Sampling/Computing-tilat vain todellisen `HomeViewModel.refresh()`-elinkaaren yhteydessä.
- Battery current ei saa näyttää keksittyä viiden sekunnin näytteenottoa, jos ViewModel ei tarjoa todellista alkua, etenemää ja päättymistä. Olemassa oleva live-seuranta voidaan visualisoida ilman valheellista progressia.
- Keinotekoinen viive ei saa peittää jo valmistunutta tulosta eikä teeskennellä mittausta.

## 5. Rajaus

### 5.1 Työhön kuuluu

- Material 3 Expressive -riippuvuuden ja API-käytön poisto;
- vakaan Material 3:n komponenttien käyttöönotto;
- väri-, typografia-, muoto-, spacing-, liike- ja kaaviotokenit;
- vaalean ja tumman teeman pintahierarkia;
- komponenttien ja ruutujen visuaalinen uudelleenrakennus;
- Home-, Insights-, Tools- ja Settings-rakenteiden tarkennus;
- kaikkien nykyisten detail-näkymien yhdenmukaistaminen;
- kaavioiden viewport-, periodivalitsin-, state precedence- ja luettavuuskorjaukset;
- olemassa olevien mittaustilojen esityskerrosanimaatiot;
- widgetien värien ja hierarkian yhdenmukaistaminen Glancen sallimissa rajoissa;
- semantiikka-, kosketuskohde-, fonttiskaala-, kontrasti- ja reduced motion -tarkistukset;
- kohdennetut testit ja dokumentaatiopäivitykset.

### 5.2 Työhön ei kuulu

- uudet sensorit tai mittauslähteet;
- uudet verkko-operaatiot;
- NDT7:n toiminnallinen muutos;
- Room-skeeman tai migraatioiden muutos;
- repository- tai use case -rajapintojen laajennus;
- uudet käyttöoikeudet;
- reittien, top-level destinationien tai back stack -politiikan muutos;
- Pro-oikeuksien tai trial-logiikan muutos;
- uudet maksutavat, tilaukset tai mainokset;
- uudet insight-säännöt tai ratkaistujen insightien historiatoiminto;
- screenshot-testikirjaston lisääminen ilman erillistä hyväksyntää;
- nykyisen checkout-haaran yhdistäminen UI-haaraan;
- julkaisutoimet, push tai pull request.

## 6. Haaroitus- ja työskentelystrategia

### 6.1 Esitarkistus

Ennen worktreen luontia:

```powershell
git status --short --branch
git branch --list codex/ui-uudistus-v2
git worktree list
git rev-parse codex/ui-redesign-m3-expressive
```

Edellytykset:

- lähtöcommit on odotetusti `88d2609`;
- `codex/ui-uudistus-v2` ei ole jo käytössä;
- worktree-polku ei sisällä käyttäjän muuta työtä.

### 6.2 Eristetty worktree

Suositeltu komento toteutusvaiheessa:

```powershell
git worktree add C:\Dev\runcheck-ui-uudistus-v2 -b codex/ui-uudistus-v2 codex/ui-redesign-m3-expressive
```

Kaikki toteutuskomennot ajetaan tämän jälkeen polussa:

```text
C:\Dev\runcheck-ui-uudistus-v2
```

### 6.3 Integraatioraja

- UI-haaraan ei yhdistetä nykyistä checkout-haaraa työn aikana.
- UI-uudistus validoidaan ensin omana ehjänä kokonaisuutenaan.
- Mahdollinen myöhempi yhdistäminen tehdään erillisellä merge-base-, diff-, konfliktikartoitus- ja testivaiheella.
- Force pushia ei käytetä.

## 7. Vakaa Material 3 -migraatio

### 7.1 Riippuvuudet

Muokattava:

- `app/build.gradle.kts`

Luettava ja todennettava:

- `gradle/libs.versions.toml`
- Gradlen dependency insight -tulos.

Toimenpiteet:

1. Poista Material 3:n suora `1.5.0-alpha24`-versio-ohitus.
2. Säilytä Compose BOM `platform(...)` yhtenä versionlähteenä.
3. Säilytä `implementation(libs.androidx.compose.material3)` ilman yksittäistä versiota.
4. Poista `ExperimentalMaterial3ExpressiveApi`-opt-in.
5. Älä muuta muuta Compose-riippuvuusjoukkoa ilman käännösvirheen osoittamaa tarvetta.

Todennus:

```powershell
.\gradlew.bat :app:dependencyInsight --dependency androidx.compose.material3:material3 --configuration debugRuntimeClasspath
```

Hyväksyntä:

- resolved Material 3 on vakaa `1.4.0`;
- dependency tree ei sisällä `material3:1.5.0-alpha*`;
- lähdekoodissa ei ole Expressive-opt-iniä.

### 7.2 API-korvaustaulukko

| Nykyinen | Korvaava ratkaisu | Huomio |
|---|---|---|
| `MaterialExpressiveTheme` | `MaterialTheme` | Säilytä light/dark color scheme, typography ja shapes. |
| `MotionScheme.expressive()` | omat `MotionTokens` | Liike ei saa riippua alpha-API:sta. |
| `LargeFlexibleTopAppBar` | `LargeTopAppBar` | Käytä `exitUntilCollapsedScrollBehavior()`-mallia detail-näkymissä. |
| `LoadingIndicator` | `CircularProgressIndicator` tai oma `MeasurementIndicator` | Semantiikka ja reduced motion säilytetään. |
| `HorizontalFloatingToolbar` | vakaa `Surface` + `Row` / `BottomAppBar` | Cleanupin toiminnallisuus ja 48 dp -kohteet säilyvät. |
| `RuncheckWavyProgress` | `HeroGauge` / vakaa Canvas-piirto | Ei aaltoilevaa Expressive-geometriaa. |
| `ExpressiveSingleChoiceSelector` | `RuncheckSingleChoiceSelector` + historian oma chip row | Enintään neljän ja pitkän vaakalistan tarpeet erotetaan. |
| `ExpressiveEmptyState` | `EmptyStateIllustration` | Ei pelkkää tekstiä tyhjässä näkymässä. |
| `ExpressiveDetailScaffold` | `RuncheckDetailScaffold` | Vakaa app bar ja nykyinen navigointi. |

### 7.3 Poistoehto

Vaiheen lopussa seuraavan haun tulee olla tyhjä tuotantokoodissa:

```powershell
rg -n "MaterialExpressiveTheme|MotionScheme|LargeFlexibleTopAppBar|LoadingIndicator|HorizontalFloatingToolbar|ExperimentalMaterial3ExpressiveApi|RuncheckWavyProgress|Expressive[A-Z]" app
```

Testin nimessä oleva vanha termi voidaan nimetä uudelleen samassa muutoksessa. Kuollutta wrapper-koodia ei jätetä varmuuden vuoksi.

## 8. Design token -järjestelmä

### 8.1 Yksi lähde per käsite

| Käsite | Omistava tiedosto |
|---|---|
| Raakavärit ja domain-värit | `ui/theme/Color.kt` |
| Material color schemet ja paikalliset theme extensionit | `ui/theme/Theme.kt` |
| Statusvärit | `ui/theme/StatusColors.kt` |
| Kaaviovärit | `ui/theme/ChartTheme.kt` |
| Typografia | `ui/theme/Type.kt` |
| Muodot | `ui/theme/Shapes.kt` |
| Spacing | `ui/theme/Spacing.kt` |
| Liike | `ui/theme/MotionTokens.kt` |
| Mitat ja yhteiset UI-vakiot | `ui/theme/UiTokens.kt` |

Raakavärejä, korttisäteitä, kosketuskokoja tai animaation kestoja ei määritellä ruuduissa.

### 8.2 Dark-teeman värit

| Token | Arvo | Käyttö |
|---|---:|---|
| `BgPageDark` | `#08171C` | koko sivun pohja |
| `Surface1Dark` | `#0D2229` | pääkortit |
| `Surface2Dark` | `#123039` | sisäkkäiset alueet |
| `Surface3Dark` | `#183D47` | valinta- ja korostuspinnat |
| `TextPrimaryDark` | `#F4FAFC` | pääteksti |
| `TextSecondaryDark` | `#A9BEC6` | seliteteksti |
| `TextMutedDark` | `#789099` | toissijainen metadata |

### 8.3 Light-teeman värit

| Token | Arvo | Käyttö |
|---|---:|---|
| `BgPageLight` | `#DDE6EA` | sivun pohja |
| `Surface1Light` | `#FFFFFF` | pääkortit |
| `Surface2Light` | `#F4F7F8` | sisäkkäiset alueet |
| `Surface3Light` | `#E8EFF2` | valinta- ja korostuspinnat |
| `LightCardBorder` | `#7A939D` | 1 dp vaalean teeman korttiraja |
| `TextPrimaryLight` | tumma, WCAG-tarkistettu | pääteksti |
| `TextSecondaryLight` | tumma harmaansininen | seliteteksti |

Vaalean teeman korteissa käytetään 1 dp rajaa. Tummassa teemassa samaa yleisrajaa ei käytetä. ActionCardin olemassa oleva erityisraja arvioidaan erikseen, jotta kahta sisäkkäistä rajaa ei synny.

### 8.4 Domain-accentit

| Domain | Dark | Light | Sallittu suuri käyttö |
|---|---:|---:|---|
| Battery | `#FFB627` | `#9B5C00` | hero-indikaattori, chart line, pieni tile-korostus |
| Network | `#4EA8F5` | `#0B63B0` | hero-indikaattori, chart line, pieni tile-korostus |
| Thermal | `#FF7A45` | `#C24A12` | hero-indikaattori, chart line, pieni tile-korostus |
| Storage | `#35DDBE` | `#007A66` | segmentit, chart line, pieni tile-korostus |

Accent ei täytä suurta korttipintaa. Health-gaugen kaari pysyy neutraalina; domain- tai statusväri kuuluu indikaattoriin, pisteeseen, viivaan tai pieneen pilliin.

### 8.5 Typografia

| Tyyli | Koko | Fontti | Käyttö |
|---|---:|---|---|
| Hero number | 64 sp | JetBrains Mono | yksittäinen pääarvo |
| Hero unit | 24 sp | Manrope | arvon yksikkö |
| Gauge value | 40–48 sp | JetBrains Mono | hero-gauge |
| Card metric | 24–32 sp | JetBrains Mono | metric tile / stat block |
| Section title | Material title | Manrope | osio-otsikko |
| Body | Material body | Manrope | selitteet |
| Label | Material label | Manrope | status, metadata |

Tekstityylit lisätään `Type.kt`:hon nimettyinä laajennuksina tai sovitetaan olemassa olevaan `Typography`-rakenteeseen. Ruudut eivät rakenna kertakäyttöisiä `TextStyle`-olioita.

### 8.6 Muodot ja mitat

| Token | Arvo |
|---|---:|
| Screen horizontal padding | 20 dp |
| Card internal padding | 20 dp |
| Card gap | 12 dp |
| Section gap | 28 dp |
| Main card radius | 24 dp |
| Hero card radius | 32 dp |
| Small element radius | 12 dp |
| Minimum touch target | 48 dp |
| Chart plot minimum height | 180 dp |
| Hero gauge stroke | 18 dp |

Kaikki spacing-arvot pysyvät 4 dp -ruudukossa. Mahdollinen 20 dp on sallittu osana lähdedokumentin nimenomaista järjestelmää.

### 8.7 Liiketokenit

`MotionTokens.kt` laajennetaan kattamaan:

| Token | Tarkka arvo |
|---|---|
| `durationInstant` | 100 ms |
| `durationFast` | 180 ms |
| `durationMedium` | 320 ms |
| `durationSlow` | 520 ms |
| `durationDeliberate` | 900 ms |
| `easingStandard` | `CubicBezierEasing(0.2f, 0f, 0f, 1f)` |
| `easingEmphasized` | `CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)` |
| `easingDecelerate` | `CubicBezierEasing(0f, 0f, 0f, 1f)` |
| `springGauge` | damping ratio 0.72, stiffness 180 |
| `springChip` | damping ratio 0.55, stiffness 420 |
| `counterTween` | 700 ms, `easingDecelerate` |
| Speed live value spring | damping ratio 0.8, stiffness 300 |
| Result item stagger | 80 ms |
| List entry stagger | ensimmäiset kuusi 40 ms välein |
| Chart fill delay | 200 ms |

Jokainen `tween`, `spring`, `keyframes` ja viive viittaa nimettyyn tokeniin. Paljaita millisekuntiarvoja ei jätetä komponentteihin.

Navigaatioliike:

- top-level tab: 180 ms crossfade, ei vaakasuuntaista slidea;
- detail push: `slideInVertically(initialOffsetY = { it / 12 })` + fade, 320 ms emphasized easing;
- exit peilaa sisääntulon;
- tile-to-detail shared element on valinnainen ja toteutetaan vain, jos se ei muuta navigaatioarkkitehtuuria;
- ensimmäiset kuusi listakohdetta nousevat 8 dp ja ilmestyvät 40 ms porrastuksella;
- yli kuusi kohdetta piirretään ilman porrastusta.

Chart:

- path piirretään vasemmalta oikealle 900 ms decelerate-easingillä;
- fill ilmestyy saman 900 ms ikkunan aikana 200 ms viiveellä;
- animaatioavain johdetaan datasetistä;
- scroll, recomposition tai configuration change ei yksin käynnistä animaatiota uudelleen.

Suorituskyky:

- `draw`-lohko ei allokoi Path-, Brush- tai text layout -olioita;
- käytä `remember`- tai `drawWithCache`-mekanismia datan elinkaareen sopivasti;
- jatkuva animaatio pysähtyy, kun ruutu ei ole resumed-tilassa tai composable poistuu compositionista;
- idle-tilassa ei ole infinite transitionia.

## 9. Komponenttiarkkitehtuuri

### 9.1 Ennen uuden komponentin luontia

Tarkista vähintään:

- `ProgressRing.kt`;
- `AnimatedNumber.kt`;
- `TrendChart.kt`;
- `AreaChart.kt`;
- `LiveChart.kt`;
- `SectionHeader.kt`;
- `SegmentedBar.kt`;
- `SegmentedStatusBar.kt`;
- `ExpressiveComponents.kt`;
- `GridCard.kt`;
- `ActionCard.kt`;
- `StatusStrip.kt`;
- `ConfidenceBadge.kt`.

Jos olemassa oleva komponentti kattaa saman vastuun, sitä refaktoroidaan. Rinnakkaista lähes identtistä toteutusta ei luoda.

### 9.2 Tavoitekomponentit

#### `HeroGauge`

Vastuu:

- neutraali paksu taustakaari;
- erillinen accent-indikaattori;
- 0–100 arvon clamping;
- keskitetty arvo, status ja tarvittaessa confidence;
- reduced motion;
- saavutettava yhdistelmäsemantiikka.

API:n tulee ottaa vähintään arvo, label, status, accent, content description ja animation key. Canvas ei saa olla erillinen TalkBack-kohde, jos sama tieto annetaan tekstinä.

#### `MetricTile`

Vastuu:

- domain-kohtainen visuaalinen identiteetti;
- pääarvo ja yksikkö;
- lyhyt label;
- status/confidence;
- vähintään 48 dp klikattava pinta;
- loading/unavailable-tila ilman layout-hyppyä.

Kaikille neljälle domainille ei käytetä täysin samaa koristegeometriaa. Rakenne pysyy yhdenmukaisena, mutta domain-indikaattori, ikoni ja visualisointityyppi voivat erota.

#### `SegmentedDonut`

Vastuu:

- Storage-kategorioiden osuudet;
- “free”/“used”/kategoriat erillisillä saavutettavilla teksteillä;
- segmenttien minimikulma;
- nolla- ja tuntematon-tilat;
- reduced motion.

#### `StatBlock`

Vastuu:

- yksittäinen label/value/unit/status-yhdistelmä;
- responsiivinen leveys;
- ei kiinteää tekstileveyttä;
- ei värin varaan jäävää tilatietoa.

#### `FullBleedChart`

Toteutustapa:

- ensisijaisesti refaktoroi `TrendChart.kt`; älä jätä kahta chart engineä;
- erottele `ChartViewport`, tick policy, zone clipping ja drawing;
- varaa plotille vähintään 180 dp akselien lisäksi;
- käytä `drawWithCache`-mekanismia muuttumattomille path/brush-laskelmille;
- säilytä valintapiste, scrubbing ja saavutettava yhteenveto;
- viivan paksuus ja fill ovat chart-tokenien hallinnassa.

#### `MeasurementIndicator`

Vastuu:

- yhteinen Idle/Preparing/Sampling/Computing/Settling/Result/Failed-esitys;
- mapataan ruudun olemassa olevasta UI-tilasta;
- ei käynnistä mittausta;
- ei omista domain-tilaa;
- ei näytä keinotekoista edistymistä.

#### `StatusPill`

Vastuu:

- teksti + tarvittaessa ikoni;
- vähimmäissisätila;
- ei koskaan rivitetä yksittäisiksi merkeiksi;
- statusväri vain pienenä pintana;
- `maxLines = 1`, mutta fonttiskaalalla sisältö saa tarvittaessa siirtyä muun layoutin alle.

#### `EmptyStateIllustration`

Vastuu:

- kevyt Canvas-kuvitus tai olemassa olevista Outlined-ikoneista koostuva kuva;
- otsikko, selite ja valinnainen CTA;
- ei uutta kuva-asset-riippuvuutta;
- koristeellinen kuvitus piilotetaan semantiikalta, teksti kertoo merkityksen.

#### `SectionHeader`

Nykyistä `SectionHeader.kt`:ta laajennetaan vain tarvittavilla trailing action/count -ominaisuuksilla. Otsikko säilyttää heading-semanticsin.

#### `AnimatedCounter`

Nykyinen `AnimatedFloatText` laajennetaan tai nimetään hallitusti:

- Int/Float-arvot;
- formatointi;
- prefix/suffix;
- lähtöarvon hallinta;
- reduced motion;
- semantiikalle valmis lopullinen arvo animaation aikana.

### 9.3 Tiedostorakenne

Suositeltu lopputila:

```text
ui/components/
├── AnimatedNumber.kt
├── EmptyStateIllustration.kt
├── HeroGauge.kt
├── MeasurementIndicator.kt
├── MetricTile.kt
├── RuncheckDetailScaffold.kt
├── RuncheckSingleChoiceSelector.kt
├── SectionHeader.kt
├── SegmentedDonut.kt
├── StatBlock.kt
├── StatusPill.kt
└── TrendChart.kt
```

`ExpressiveComponents.kt` poistetaan, kun kaikki kutsujat on siirretty. Jos siirto tehdään useassa commitissa, väliaikainen forwarding-wrapper sallitaan vain yhden vaiheen ajaksi ja poistetaan ennen vaiheen hyväksyntää.

## 10. Kaavioiden yksityiskohtainen korjaussuunnitelma

### 10.1 Viewport-malli

Lisää puhdas, testattava malli:

```text
ChartViewport(
    minValue,
    maxValue,
    ticks,
    visibleZones
)
```

Viewport-laskennan säännöt:

1. poista NaN- ja infinite-arvot;
2. jos data on tyhjä, palauta empty state, ei keinotekoista nollaviivaa;
3. jos min == max, lisää domainiin sopiva symmetrinen padding;
4. muodosta alustava alue datasta;
5. lisää vain eksplisiittisesti näkyväksi tarkoitetut tickit;
6. lisää kohtuullinen 5–10 % visuaalinen padding;
7. leikkaa quality zone -alueet viewportin min/max-arvoihin;
8. älä lisää quality zone -rajoja viewportin laskentaan;
9. valitse enintään neljä Y-labelia;
10. poista label, jos mitattu pystysuuntainen väli alittaa vähimmäisvälin.

### 10.2 Domain-kohtaiset tickit

- Battery level: selkeä kokonaislukuprosentti.
- Temperature: käyttäjän °C/°F-asetus ja järkevä desimaalitarkkuus.
- Current: alle 1000 mA kokonaislukuna; suuret arvot tarvittaessa kompaktisti.
- Voltage: V, enintään kaksi desimaalia.
- Network: dBm tai nopeusyksikkö näytön kontekstin mukaan.
- Thermal headroom/status: domainin nykyinen yksikkö, ei uuden mittarin keksimistä.

### 10.3 State precedence

Kaavioalue näyttää täsmälleen yhden päätilan:

1. loading;
2. error;
3. locked;
4. empty/insufficient;
5. data.

Alempi tila ei saa näkyä ylemmän rinnalla. Esimerkiksi locked-tila ei piirrä taustalle blurred chartia ja “insufficient data” -tekstiä.

### 10.4 Periodivalitsin

Luo historian käyttöön oma komponentti:

- `LazyRow`;
- vakaa `FilterChip`;
- 12 dp väli tai tokenoitu vastaava;
- valittu arvo tuodaan näkyviin `animateScrollToItem`-toiminnolla;
- reduced motion käyttää `scrollToItem`;
- alku- ja loppureunassa kevyt gradient fade vain, jos kyseiseen suuntaan voi vierittää;
- TalkBack kertoo valinnan ja position;
- viimeinen vaihtoehto on saavutettavissa 411 dp leveydellä ja 2.0 font scale -asetuksella;
- ei katkennutta tekstiä.

Valitun periodin tilan omistus säilyy nykyisessä `SavedStateHandle`/ViewModel-polussa.

### 10.5 Testit

Lisättävät puhtaat testit:

- `ChartViewportTest`;
- `ChartTickPolicyTest`;
- `ChartStatePrecedenceTest`;
- historian periodivalitsimen policy-testi.

Vähimmäistapaukset:

- level data 61–80 + quality zones 0–100;
- temperature data 30,7–33,4 + zones 0–60;
- current data ilman zoneja;
- yksi datapiste;
- tasainen datasarja;
- tyhjä sarja;
- erittäin suuri ja negatiivinen arvo;
- Fahrenheit-muunnos;
- 24H, 7D, 30D, 90D ja 1Y;
- free locked, trial/pro data, insufficient data ja error.

## 11. Ruutukohtaiset toteutussopimukset

### 11.1 Home

Muokattavat pääkohteet:

- `ui/home/HomeScreen.kt`;
- `ui/home/HomeSecondarySections.kt`;
- `ui/home/insights/InsightsCard.kt`;
- tarvittaessa `ui/home/HomeUiState.kt` ja `HomeViewModel.kt` vain esitystilan osalta.

Rakenne:

1. kompakti top bar;
2. yksi selkeä Health hero;
3. neljän domainin 2 × 2 metric grid;
4. käyttökelpoinen Insights-preview tai tyhjän tilan CTA;
5. monitoring freshness / trial -kortit vain tarpeen mukaan;
6. ei irrallista otsikkoa ilman sisältöä.

Health hero:

- paksu neutraali gauge;
- tulos keskiössä;
- yksi vaakasuuntainen measurement/status-rivi;
- confidence ei ole oikean reunan kapea pystypalkki;
- refresh käynnistää nykyisen `HomeViewModel.refresh()`-polun;
- tuloksen animaatio seuraa todellista state-muutosta.

Metric grid:

- Battery: prosentti + lataustila;
- Network: yhteystyyppi/signal summary;
- Thermal: lämpötila/status;
- Storage: käyttöaste/vapaa tila;
- jokaisella oma domain-accent;
- kortit eivät ole neljä identtistä minigaugea;
- klikattava alue koko kortti, vähintään 48 dp.

Home-acceptance:

- D1, D2, D5 ja D6 poistuvat;
- 411 × 850 dp:n ensimmäinen viewport font scale 1.0:lla näyttää health score -tuloksen, neljä sub-scorea ja neljä domain-tileä arvoineen ilman vieritystä;
- domain-tilet ovat väreiltään erotettavissa toisistaan, mutta jokainen sisältää myös tekstin/ikonin, jotta merkitys ei riipu väristä;
- 411 dp leveydellä mikään badge ei hajoa merkkijonoksi;
- 1.3 ja 2.0 font scale eivät leikkaa ydintietoja;
- empty Insights ei jätä orpoa otsikkoa.

### 11.2 Insights

Muokattavat:

- `ui/insights/InsightsScreen.kt`;
- `ui/insights/InsightsUiState.kt`;
- `ui/home/insights/InsightsCard.kt`;
- olemassa olevat insight-rivi- ja navigointikomponentit.

Rakenne:

1. top bar;
2. “Needs attention” aktiivisista high/important-insighteista;
3. “This week” olemassa olevan Weekly Report -esityksen tiivistelmänä, jos se voidaan koostaa nykyisestä use case -polusta ilman uutta data-API:a;
4. aktiivisten insightien muu lista;
5. “Recently resolved” vain luvun 4.3 ehdolla.

Tyhjä tila:

- kuvitus;
- otsikko;
- selite siitä, että aktiivisia huomioita ei juuri nyt ole;
- CTA Homeen tai Learn-sisältöön nykyisten navigointikäsittelijöiden kautta;
- ei lupausta jatkuvasta taustamittauksesta, jos monitoring ei ole käytössä.

Suodatus:

- All/Important käyttää vakaata valitsinta;
- valinta säilyy `rememberSaveable`-tilassa;
- aktiivisten insightien seen/dismiss-käytös ei muutu.

### 11.3 Tools

Muokattavat:

- Tools-näyttö ja sen nykyiset entry-komponentit;
- navigoinnin route-map vain, jos Weekly Report -kortin esityspaikka muuttuu mutta reitti säilyy.

Rakenne:

- Speed Test ensisijaisena suurena action-korttina;
- Cleanup vahvana storage-domain actionina;
- Export ja muut utilityt pienempinä;
- Weekly Report poistetaan Toolsin päälistasta, jos se näytetään Insightsissä;
- Pro-lukko näkyy yhtenä selkeänä tilana;
- kortit käyttävät Outlined-ikoneita.

Toiminnalliset reitit eivät muutu.

### 11.4 Settings

Muokattavat:

- `ui/settings/SettingsScreen.kt` tai kohdehaaran vastaavat jaetut Settings-tiedostot;
- olemassa olevat asetusrivit ja dialogit.

Ryhmittely:

1. Appearance;
2. Monitoring;
3. Notifications;
4. Data and privacy;
5. About/support;
6. Pro/trial.

Säännöt:

- System/Light/Dark säilyvät;
- reaaliaikainen foreground monitoring pysyy opt-ininä;
- Sentryyn tai telemetriaan ei lisätä uutta asetusta;
- release pysyy telemetriavapaana;
- Settings-rivit eivät käytä ylisuuria kortteja jokaiselle yhdelle togglelle;
- ryhmät erotetaan spacingillä ja surface-tasoilla.

### 11.5 Battery detail

- nykyinen hero säilyttää kaikki arvot ja confidence-tiedot;
- current-, temperature-, level- ja voltage-livearvot säilyvät;
- historian periodivalitsin korvataan;
- kaikki kaaviot käyttävät uutta viewport/state precedence -mallia;
- Pro-lukko ja insufficient data eivät näy päällekkäin;
- charger comparison -portti säilyy;
- current sampling -animaatio ei väitä viiden sekunnin mittausta ilman todellista statea.

### 11.6 Network detail

- dBm/connection hero säilyy;
- nykyiset SSID/BSSID/cell/network-tiedot säilyvät;
- tap-to-copy-käytös säilyy niissä riveissä, joissa se on toteutettu;
- lukuarvot ja confidence eivät katoa visuaalisessa refaktorissa;
- latency ei käynnisty automaattisesti uutena verkko-operaationa;
- signal chart käyttää samaa chart engineä;
- Speed Test CTA säilyy.

### 11.7 Thermal detail

- nykyinen lämpötila-/statushero säilyy;
- PowerManager-pohjainen data ja API-guardit eivät muutu;
- mitään sysfs-lukua ei lisätä;
- domain-accent on thermal orange, mutta suuri tausta ei muutu oranssiksi;
- history/live chart käyttää uutta chart engineä;
- confidence ja unavailable-tila säilyvät.

### 11.8 Storage detail

- nykyinen storage-data säilyy;
- yläosa muutetaan `SegmentedDonut`-esitykseksi vain, jos kaikki segmentit voidaan johtaa nykyisestä `StorageUiState`-datasta;
- tuntemattomia kategorioita ei arvata;
- cleanup-CTA:t säilyvät;
- projected/current usage -visualisointi käyttää samaa domain-accentia;
- Pro-portit säilyvät.

### 11.9 Speed Test

- nykyinen NDT7-toteutus, lähimmän palvelimen automaattivalinta ja cellular warning säilyvät;
- `SpeedTestPhase` mapataan MeasurementIndicatoriin;
- Ping → Download → Upload → Completed esitetään tekstillä ja visuaalisella tilalla;
- rengas käyttää vain neutraalia kaarta ja yhtä progress/needle-indikaattoria;
- radial glow/fill poistetaan;
- download ja upload eivät näytä yhtä aikaa ristiriitaista aktiivista tilaa;
- Failed säilyttää retry-polun;
- reduced motion näyttää välittömät tilanvaihdot ilman sweep-animaatiota.

### 11.10 Cleanup

- `CleanupUiState` on tilan lähde;
- Idle, Scanning, Results, Deleting, Success, Empty, Unsupported ja Error käsitellään erikseen;
- Expressive floating toolbar korvataan vakaalla bottom action surfacella;
- valittu tiedostomäärä ja koko säilyvät näkyvissä;
- delete confirmation ja Storage Access Framework -polut eivät muutu;
- skannaus ei näytä tekaistua progressia, jos `progress == -1f`;
- success overlay käyttää yhteistä success motion -tokenia;
- thumbnail loaderin kerrosraja säilyy.

### 11.11 App Usage

- nykyiset 30/60/90 päivän “Not used” -suodattimet säilyvät;
- lista käyttää yhteisiä rivejä, stat blockeja ja empty statea;
- Pro-portti säilyy;
- mitään paketin uninstall-toimintoa ei lisätä;
- käyttöaikadataa ei tulkita akunkulutukseksi.

### 11.12 Weekly Report, Learn, Export, Pro ja fullscreen chart

Nämä eivät saa jäädä vanhan järjestelmän visuaalisiksi saarekkeiksi.

- Weekly Report käyttää uusia section-, card- ja stat-tokeneita.
- Learn säilyttää artikkelihierarkian ja luettavan tekstileveyden.
- Export säilyttää nykyiset formaatit, Pro-portit ja URI-polut.
- Pro Upgrade säilyttää kertamaksun, trial-tilat ja olemassa olevan billing-logiikan.
- Fullscreen chart käyttää samaa viewportia, tick policyä ja värejä kuin embedded chart.

## 12. Liike- ja mittaustilajärjestelmä

### 12.1 Esityskerroksen tila

Lisää esimerkiksi:

```kotlin
sealed interface MeasurementState {
    data object Idle
    data object Preparing
    data class Sampling(val progress: Float?)
    data object Computing
    data object Settling
    data object Result
    data class Failed(val message: UiText)
}
```

Lopullinen sijoitus on `ui/common` tai `ui/components` sen mukaan, onko malli vain komponentin API vai usean ViewModel-mapparin yhteinen tyyppi. Domainiin sitä ei sijoiteta.

### 12.2 Tilan mapparit

Jokaiselle virralle luodaan puhdas mappari ja testi:

- `SpeedTestPhase -> MeasurementState`;
- `CleanupUiState -> MeasurementState`;
- Homen loading/refresh/success/error -> measurement presentation;
- Battery live -tila vain saatavilla olevan todellisen datan perusteella.

### 12.3 Animaatiosäännöt

- animaatio reagoi state transitioniin, ei jatkuvaan recompositioniin;
- sama tulos ei käynnistä laskuria uudelleen jokaisella Flow-emissiolla;
- `sample(333L)` säilyy live ViewModeleissa;
- infinite transition sallitaan vain aidosti aktiivisessa rajatussa mittaustilassa;
- idle-tilassa ei ole jatkuvaa hehkua, pulssia tai aaltoa;
- reduced motion nollaa tai lyhentää liikkeen tokenien mukaisesti;
- semantiikka ilmoittaa lopullisen arvon, ei nopeasti muuttuvaa jokaista välilukua.

### 12.4 Operaatiokohtainen rehellisyysmatriisi

| Operaatio | Todellinen nykytila | Sallittu esitys | Kielletty esitys |
|---|---|---|---|
| Home health refresh | `HomeViewModel.refresh()` käynnistää sensorien, historian ja pisteytyksen Flow-polun uudelleen | oikeaan loading/refresh-elinkaareen sidottu Reading sensors / Computing / settle | kiinteä 2400 ms lattia ilman ViewModelin todentamaa vaihetta |
| Speed Test | `SpeedTestPhase`: Idle, Ping, Download, Upload, Completed, Failed | Connecting/latency/download/upload/result oikeista callbackeista; live Mbps | ajastimeen perustuva feikkiprogressi tai uudet laatukynnykset |
| Cleanup | `CleanupUiState.Scanning(progress)` ja muut eksplisiittiset tilat | determinate progress, jos 0–1; muuten indeterminate; kategoriat vasta tuloksissa | tekaistu kategoriakohtainen etenemä ennen dataa |
| Battery current | jatkuva live current -rengaspuskuri ja sampleCount, ei eksplisiittistä 5 s operaatiotilaa | live-arvo, sample count ja confidence sellaisina kuin tila ne tarjoaa | “Averaging over 5s” -progressi ilman todellista 5 s statea |
| Thermal | nykyinen sensoritila virtaa ViewModeliin | arvon muutos ja settle | näytteenottovaiheiden keksiminen |
| Charts | valmis dataset vaihtuu | yksi draw-in per dataset key | re-animation scrollissa tai recompositionissa |

Jos lähdedokumentin tarkka 2400 ms Health- tai 5 s Battery-koreografia halutaan toteuttaa myöhemmin kirjaimellisesti, se vaatii esityskerrokselle todellisen vaiheistetun mittaussignaalin. G2-raja estää tämän työn muuttamasta domain/use case -polkua vain animaation vuoksi.

### 12.5 Arvojen ja kynnysten lähteet

Ennen väri- tai statusmappausta etsi nykyiset lähteet:

```powershell
rg -n "threshold|Threshold|band|Band|Poor|Fair|Good|Excellent|warning|critical" app/src/main/java/com/runcheck
```

- Health-, speed-, thermal- ja storage-kynnyksiä ei kopioida UI-tiedostoihin.
- UI käyttää olemassa olevan domain-tuloksen status-/band-tietoa aina kun sellainen on tarjolla.
- Jos numeerinen kynnys puuttuu, toteutus pysähtyy kyseisen värimappauksen osalta ja havainto raportoidaan.
- Design-dokumentin hex-värejä ei muuteta “paremman näköisiksi”. Jos kontrasti ei läpäise, mitattu pari ja suhde raportoidaan päätettäväksi.

## 13. Widgetit

Muokattavat:

- `widget/BatteryWidget.kt`;
- `widget/HealthWidget.kt`;
- `widget/QuickGlanceWidget.kt`;
- `widget/WidgetCommon.kt`;
- tarvittaessa preview-resurssit.

Toimenpiteet:

- määrittele Glance `ColorProviders(light, dark)` samoista visuaalisista päätöksistä;
- säilytä widgettien nykyinen Room-backed data ja Pro-portti;
- älä yritä jakaa Compose `Color` -olioita suoraan Glancen kanssa;
- varmista vaalea/tumma widgetti;
- säilytä resize- ja empty-state-käytös;
- päivitä preview-kuvat vain, jos niiden värit eivät enää vastaa toteutusta.

## 14. Saavutettavuus

### 14.1 Pakolliset tarkistukset

- jokainen klikattava kohde vähintään 48 × 48 dp;
- chart, ring, segmented donut ja muut visuaalit saavat sisältökuvauksen tai yhdistetyn semantiikan;
- status ei perustu pelkkään väriin;
- heading-semantics kaikilla osio-otsikoilla;
- valitsimet ilmoittavat selected-tilan;
- loading/mittaus ilmoittaa tilan, mutta ei tulvi live region -päivityksiä;
- koristekuvitus ei muodosta turhaa TalkBack-kohdetta;
- font scale 1.0, 1.3 ja 2.0;
- navigointi ja back-toiminto säilyvät;
- TalkBack-järjestys vastaa visuaalista järjestystä;
- reduced motion testataan järjestelmäasetuksella.

### 14.2 Kontrasti

Tarkista vähintään:

- primary text / page;
- secondary text / surface;
- muted text / surface;
- light card border / background;
- domain accent / käyttöpinta;
- status pill text/background;
- chart line/background;
- chart label/background;
- disabled/locked text.

Pelkkä hex-arvon silmämääräinen arvio ei riitä. Tulokset kirjataan teemakohtaiseen tarkistuslistaan.

Vaalean teeman korttirajat tarkistetaan lisäksi fyysisellä laitteella noin 30 % näytön kirkkaudella lähdedokumentin hyväksymiskriteerin mukaisesti.

### 14.3 Automatisointi

Kun testilaite/API mahdollistaa sen, Compose UI -testeissä otetaan käyttöön accessibility checks virallisen Compose-ohjeen mukaisesti. Tämä täydentää, ei korvaa, TalkBack- ja fonttiskaalatestausta.

## 15. Toteutusvaiheet ja riippuvuudet

## Vaihe 0 — Baseline, worktree ja korjausten todistettavuus

**Riippuvuudet:** ei mitään  
**Tavoite:** lukita oikea lähtötila ja tehdä ongelmista toistettavia.

### Read first

- `runcheck-ui-uudistus.md`;
- kohdehaaran `AGENTS.md`, `CODEX.md`, `PROJECT.md`, `UI-SPEC.md`;
- `Theme.kt`, `ExpressiveComponents.kt`, `TrendChart.kt`;
- `HistoryPeriodFilterChipRow.kt`;
- `HomeScreen.kt`, `InsightsScreen.kt`, `SpeedTestScreen.kt`;
- kaikki mainitut kuvakaappaukset.

### Tehtävät

1. Luo erillinen worktree luvun 6 mukaan.
2. Tallenna lähtöcommit ja `git status`.
3. Tee taulukko jokaisesta D1–D12-ongelmasta:
   - lähdekoodipaikka;
   - toistoreitti;
   - teema;
   - Pro/free-tila;
   - kuvakaappaus;
   - korjauksen omistava vaihe.
4. Lisää puhtaat failing-testit chart viewportille ja state precedencelle.
5. Lisää valitsinpolitiikan testi, joka osoittaa viidennen periodin saavutettavuusvaatimuksen.
6. Älä muuta vielä tuotantokoodia muilta osin.

### Hyväksyntä

- jokaisella D1–D12-tunnuksella on lähdekoodiomistaja;
- D8-testi epäonnistuu vanhalla quality-zone-skaalauksella;
- D11-testi epäonnistuu, jos kaksi päätilaa voi olla yhtä aikaa;
- työhakemisto ei sisällä nykyisen checkout-haaran muutoksia.

### Commit

`testit: lukitse UI-uudistuksen kaavio- ja tilaregressiot`

## Vaihe 1 — D8, D9 ja D11 ensimmäisenä toimituskelpoisena korjauksena

**Riippuvuudet:** vaihe 0  
**Tavoite:** korjata käytettävyydeltään vakavimmat ongelmat ennen laajaa visuaalista muutosta.

### Muokattavat

- `ui/components/TrendChart.kt`;
- `ui/chart/HistoryPeriodFilterChipRow.kt`;
- battery/network/thermal/storage chart state -kutsujat;
- vastaavat testit.

### Tehtävät

1. Erota viewport, tick policy ja zone clipping puhtaiksi funktioiksi.
2. Estä quality zoneja laajentamasta viewportia.
3. Rajaa labelit enintään neljään ja varmista minimipikseliväli.
4. Toteuta state precedence.
5. Toteuta historian `LazyRow`-valitsin.
6. Tuo valittu periodi näkyviin.
7. Lisää reuna-affordanssi.
8. Varmista 180 dp:n todellinen plot area.
9. Säilytä fullscreen chartin toiminta.

### Hyväksyntä

- Level-, Temperature-, Current- ja Voltage-kaaviot ovat luettavia;
- quality zone näkyy vain viewportin sisällä;
- kaikki periodit ovat saavutettavissa;
- locked/insufficient/data ovat toisensa poissulkevia;
- yksikkötestit läpäisevät.

### Commit

`korjaus: selkeytä historiakaaviot ja aikavälin valinta`

## Vaihe 2 — Vakaa Material 3

**Riippuvuudet:** vaihe 1  
**Tavoite:** poistaa alpha-Expressive ilman toiminnallista regressiota.

### Muokattavat

- `app/build.gradle.kts`;
- `Theme.kt`;
- `ExpressiveComponents.kt` ja kaikki sen kutsujat;
- cleanup bottom bar;
- app bar -kutsujat;
- komponenttitestit.

### Tehtävät

1. Poista versio-ohitus ja Expressive-opt-in.
2. Vaihda `MaterialExpressiveTheme` vakaaseen `MaterialTheme`en.
3. Korvaa app barit, loading ja floating toolbar.
4. Luo väliaikaiset vakaat Runcheck-wrapperit vain, jos siirtymä vaatii.
5. Päivitä kaikki kutsujat.
6. Poista kuolleet Expressive-wrapperit ja importit.
7. Aja dependency insight ja käännöstarkistus.

### Hyväksyntä

- Material 3 resolved version on 1.4.0;
- Expressive-haku on tyhjä;
- debug Kotlin -käännös onnistuu;
- reitit ja top-level navigation säilyvät.

### Commit

`refaktorointi: siirrä käyttöliittymä vakaaseen Material 3:een`

## Vaihe 3 — Teema, tokenit ja komponenttipohja

**Riippuvuudet:** vaihe 2  
**Tavoite:** luoda yksi yhteinen visuaalinen järjestelmä.

### Muokattavat

- kaikki `ui/theme/`-tiedostot;
- `UiTokens.kt`;
- `ComponentPreviews.kt`;
- uudet/siirretyt signature component -tiedostot;
- `RuncheckThemeTest.kt`.

### Tehtävät

1. Toteuta dark/light surface-hierarkiat.
2. Lisää domain- ja chart-accentit.
3. Päivitä typography ja shapes.
4. Lisää card border -policy vaalealle teemalle.
5. Päivitä motion-tokenit.
6. Toteuta HeroGauge, MetricTile, StatBlock, StatusPill ja EmptyStateIllustration.
7. Refaktoroi AnimatedNumber ja SectionHeader.
8. Luo light/dark previewt kaikista ydinkomponenteista:
   - normaali;
   - pitkä teksti;
   - unavailable;
   - loading;
   - Pro locked;
   - font scale -riskitapaus.
9. Poista duplikaatit.

### Hyväksyntä

- kovakoodattujen visuaalisten arvojen auditointi ei löydä uusia hajautettuja tokeneita;
- vaalean teeman kortti erottuu;
- tumma teema ei saa yleistä tarpeetonta borderia;
- komponentit toimivat molemmissa teemoissa;
- 48 dp ja reduced motion toteutuvat.

### Commit

`ominaisuus: rakenna runcheckin uusi visuaalinen järjestelmä`

## Vaihe 4 — Mittausliike

**Riippuvuudet:** vaihe 3  
**Tavoite:** sitoa liike todellisiin käyttötiloihin.

### Muokattavat

- `MotionTokens.kt`;
- `MeasurementIndicator.kt`;
- speed test-, cleanup-, Home- ja battery-esityksen mapparit;
- mapparitestit.

### Tehtävät

1. Lisää UI-tason MeasurementState.
2. Toteuta puhtaat mapparit.
3. Lisää state transition -kohtaiset animaatiot.
4. Lisää tuloslaskuri ilman semantiikkatulvaa.
5. Tee reduced motion -haara.
6. Estä idle-infinite-animaatiot.
7. Jätä Battery progress pois, jos todellista progress-statea ei ole.

### Hyväksyntä

- mitään mittausta ei simuloida;
- Speed Test ja Cleanup seuraavat oikeaa tilaa;
- reduced motion poistaa sweep/pulse-animaatiot;
- sama state ei käynnistä animaatiota jatkuvasti uudelleen.

### Commit

`ominaisuus: sido mittausanimaatiot todellisiin käyttötiloihin`

## Vaihe 5 — Home ja Tools

**Riippuvuudet:** vaiheet 3–4  
**Tavoite:** korjata pääkokemuksen hierarkia.

### Tehtävät

1. Rakenna Home luvun 11.1 mukaan.
2. Poista pystysuuntaan romahtava confidence-rakenne.
3. Toteuta domain-kohtaiset metric tilet.
4. Tee Insights-preview hyödylliseksi myös tyhjänä.
5. Rakenna Tools-hierarkia.
6. Siirrä Weekly Reportin esityspaikka Insightsiin, jos vaiheessa 7 toteutettava yhteenveto on valmis; muutoin säilytä Tools-linkki siihen asti.
7. Säilytä kaikki navigointikäsittelijät.

### Hyväksyntä

- D1, D2, D5, D6 ja Homen osa D12:sta korjattu;
- Home ei ole tyhjä tai ylisuurten välysten hallitsema;
- Toolsin tärkeimmät toiminnot erottuvat;
- free/pro/trial-polut toimivat.

### Commit

`ominaisuus: uudista Home- ja Tools-näkymien hierarkia`

## Vaihe 6 — Detail-, Speed Test- ja Cleanup-näkymät

**Riippuvuudet:** vaiheet 1, 3 ja 4  
**Tavoite:** yhdenmukaistaa mittausnäkymät.

### Alavaiheet

1. Battery;
2. Network;
3. Thermal;
4. Storage;
5. Speed Test;
6. Cleanup;
7. App Usage;
8. Fullscreen Chart.

### Tehtävät

- vaihda yhteiseen stable detail scaffoldiin;
- ota domain-accentit käyttöön;
- säilytä kaikki confidence-, status- ja Pro-elementit;
- käytä yhteistä chart engineä;
- toteuta SegmentedDonut Storageen vain nykyisen datan perusteella;
- poista Speed Test -renkaan tahramainen fill;
- korvaa Cleanupin toolbar;
- päivitä jokaisen näytön loading/error/empty/locked/data-tilat.

### Hyväksyntä

- D3, D4, D8, D9, D10, D11 ja detail-näkymien D12 korjattu;
- Network/Thermal nykyinen ydinsisältö ei katoa;
- Speed Testin network-käyttäytyminen ei muutu;
- Cleanupin delete-flow ei muutu.

### Commitit

Pidä commitit rajattuina:

- `ominaisuus: uudista Battery- ja Network-näkymät`
- `ominaisuus: uudista Thermal- ja Storage-näkymät`
- `ominaisuus: selkeytä Speed Test- ja Cleanup-mittaustilat`
- `ominaisuus: viimeistele App Usage ja fullscreen-kaaviot`

## Vaihe 7 — Insights, Weekly Report, Settings ja muut näytöt

**Riippuvuudet:** vaiheet 3 ja 5  
**Tavoite:** poistaa tyhjät pinnat ja vanhan järjestelmän saarekkeet.

### Tehtävät

1. Rakenna Insights luvun 11.2 mukaan.
2. Upota Weekly Report -tiivistelmä vain nykyisten APIen kautta.
3. Älä toteuta Recently resolved -historiaa ilman dataa.
4. Siirrä Weekly Report pois Toolsista vasta, kun Insights-linkki/tiivistelmä toimii.
5. Ryhmittele Settings.
6. Päivitä Learn, Export, Pro Upgrade ja Weekly Report.
7. Päivitä kaikki empty/error/locked-tilat.

### Hyväksyntä

- D7 korjattu;
- Insights kertoo käyttäjälle jotain hyödyllistä myös ilman aktiivisia insightteja;
- Weekly Report on edelleen saavutettavissa;
- Settings säilyttää kaikki nykyiset asetukset;
- mikään näyttö ei käytä vanhoja Expressive-komponentteja.

### Commit

`ominaisuus: viimeistele Insights-, Settings- ja raporttinäkymät`

## Vaihe 8 — Widgetit, saavutettavuus ja visuaalinen regressio

**Riippuvuudet:** vaiheet 3, 5–7  
**Tavoite:** validoida koko esityskerros.

### Tehtävät

1. Päivitä widgettien ColorProviders.
2. Tarkista kaikki touch targetit.
3. Tarkista chart/ring/donut-semantics.
4. Lisää kohdennetut Compose UI -testit.
5. Tee manuaalinen TalkBack-kierros.
6. Ota vertailukuvat samoista reiteistä kuin lähtöaineisto:
   - dark;
   - light;
   - free;
   - Pro/trial;
   - data;
   - empty;
   - locked;
   - error mahdollisuuksien mukaan.
7. Vertaa ennen/jälkeen D1–D12-taulukon kautta.

### Hyväksyntä

- kaikki D1–D12 ovat PASS;
- 411 × 850 dp ei leikkaa sisältöä;
- font scale 2.0 säilyttää toiminnallisuuden;
- TalkBack-järjestys on looginen;
- reduced motion toimii;
- widgetit ovat luettavia molemmissa teemoissa.

### Commit

`viimeistely: yhtenäistä widgetit ja saavutettavuus`

## Vaihe 9 — Dokumentaatio ja lopullinen verifiointi

**Riippuvuudet:** kaikki edelliset  
**Tavoite:** varmistaa, että lähdekoodi, dokumentit ja todisteet vastaavat toisiaan.

### Dokumentit

Päivitä samassa muutoksessa:

- `AGENTS.md`;
- `CODEX.md`;
- `PROJECT.md`;
- `UI-SPEC.md`.

Päivitykset:

- poista Material 3 Expressive -maininnat;
- kirjaa vakaa Material 3 ja BOM-periaate;
- kirjaa uusi väri-, surface-, domain-, chart-, shape- ja motion-järjestelmä;
- kirjaa System/Light/Dark edelleen tuetuiksi;
- kirjaa historian chart viewport/state precedence;
- kirjaa uudet signature componentit;
- pidä `AGENTS.md` ja `CODEX.md` linjassa.

Muistijärjestelmää ei päivitetä ilman käyttäjän erillistä nimenomaista pyyntöä.

### Lopputarkistus

1. Git diff vain odotetuille tiedostoille.
2. Ei kuollutta Expressive-koodia.
3. Ei uusia layer- tai API-guard-rikkomuksia.
4. Ei uutta network-, permission-, Room- tai billing-muutosta.
5. Kaikki kohdennetut testit vihreinä.
6. Käännös ja Compose stability vihreinä.
7. Kuvakaappausmatriisi valmis.
8. Dokumentit vastaavat toteutusta.

### Commit

`dokumentaatio: päivitä UI-järjestelmän toteutunut tila`

## 16. Verifiointistrategia

### 16.1 Nopein staattinen tarkistus

```powershell
rg -n "MaterialExpressiveTheme|ExperimentalMaterial3ExpressiveApi|LargeFlexibleTopAppBar|LoadingIndicator|HorizontalFloatingToolbar|MotionScheme" app
rg -n "Color\\(0x|#[0-9A-Fa-f]{6,8}" app/src/main/java/com/runcheck/ui
rg -n "tween\\(|spring\\(|keyframes\\(" app/src/main/java/com/runcheck/ui
rg -n "Icons\\.(Default|Filled|Rounded)" app/src/main/java/com/runcheck
rg -n "Dispatchers\\." app/src/main/java/com/runcheck
```

Osuma ei automaattisesti ole virhe. Jokainen osuma tarkistetaan kontekstissa.

### 16.2 Kohdennetut yksikkötestit

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.runcheck.ui.components.ChartViewportTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.runcheck.ui.components.ChartTickPolicyTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.runcheck.ui.components.ChartStatePrecedenceTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.runcheck.ui.components.HistoryPeriodSelectorPolicyTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.runcheck.ui.theme.RuncheckThemeTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.runcheck.ui.home.HomeViewModelTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.runcheck.ui.insights.InsightsViewModelTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.runcheck.ui.network.NetworkViewModelTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.runcheck.ui.storage.cleanup.CleanupViewModelTest"
```

Testiluokkien lopulliset package-nimet sovitetaan toteutukseen. Komentoa ei saa kopioida sokeasti, jos tiedosto päätyy eri pakettiin.

### 16.3 Käännös ja tyyli

Pienimmästä laajempaan:

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:ktlintMainSourceSetCheck :app:ktlintTestSourceSetCheck
.\gradlew.bat :app:detekt
```

Täyttä `lc`-, `sc`-, Sonar-, Dependency-Check-, MobSF- tai DeepSec-ajoa ei käynnistetä tämän työn oletusverifiointina. Käyttäjä ajaa wrapperit erikseen, jos haluaa niiden raportit.

### 16.4 Compose stability

Koska yhteisiä composableja muutetaan:

```powershell
.\gradlew.bat :app:stabilityCheck
```

Jos stability-baseline muuttuu:

- tarkista jokainen muutos;
- älä hyväksy epävakautta pelkkänä snapshot-päivityksenä;
- päivitä baseline vasta, kun muutos on tarkoituksellinen ja lähdekoodi perustelee sen.

### 16.5 Laite- ja näyttömatriisi

| Ulottuvuus | Vähimmäiskattavuus |
|---|---|
| Teema | Dark, Light, System molempiin suuntiin |
| Koko | 411 × 850 dp, kapea puhelin, suurempi puhelin |
| Font scale | 1.0, 1.3, 2.0 |
| Motion | normaali, reduced motion |
| Oikeus | free, trial active, Pro, trial expired |
| Data | loading, data, empty, insufficient, locked, error |
| API | min SDK 26, API 29/30 guardien ympäristö, API 34+ saavutettavuustarkistus, target API 37 |
| Orientaatio | portrait; fullscreen chartin tukema orientaatio |

Jokaisella ruudulla tarkistetaan lisäksi:

- ruutu ei pääty osio-otsikkoon välittömästi bottom navigationin yläpuolella;
- mikään `Text` ei rivity yhden merkin levyiseksi;
- valittu chip/periodi on näkyvissä tai vierittyy automaattisesti näkyviin;
- tabinvaihdon ja detail-paluun jälkeen nykyinen state restoration toimii;
- sama käyttäjäpolku on edelleen saavutettavissa kuin ennen uudistusta.

### 16.6 Kuvakaappausvertailu

Nykyisessä kohdehaarassa ei ole screenshot-testikirjastoa. Tässä työssä:

- käytetään Compose preview -matriisia;
- otetaan manuaaliset laitekuvat samoista reiteistä ja tiloista kuin lähtöaineistossa;
- nimetään kuvat ruutu–teema–tila-mallilla;
- verrataan D1–D12-taulukkoon;
- automaattisen Compose screenshot testing -infran lisääminen jätetään erilliseksi hyväksyttäväksi tehtäväksi, koska se muuttaisi build toolingia.

## 17. Hyväksymiskriteerien jäljitettävyys

| Vaatimus | Pääkorjaus | Vaihe | Todiste |
|---|---|---:|---|
| D1 Accurate pystyssä | Home hero/status layout | 5 | dark/light + font scale kuvat |
| D2 Home tyhjä | uusi Home-hierarkia | 5 | Home-kuvat ja Compose UI -testi |
| D3 light surfaces | light tokenit + border policy | 3, 6–8 | kontrasti + kuvat |
| D4 speed smudge | HeroGauge ilman radial filliä | 6 | Speed Test -kuvat |
| D5 identtiset tilet | domain-kohtainen MetricTile | 3, 5 | Home-kuvat |
| D6 ohut ring | 18 dp HeroGauge | 3, 5 | preview + Home-kuva |
| D7 empty Insights | EmptyStateIllustration + CTA | 7 | empty-state-kuva/testi |
| D8 Y-label overlap | viewport/tick/zone clipping | 1 | unit test + neljä chart-kuvaa |
| D9 period clipped | HistoryPeriod LazyRow | 1 | 411 dp + font scale 2.0 |
| D10 chart smear | plot min height + line/fill tokenit | 1, 3 | chart-kuvat |
| D11 conflicting states | state precedence | 1 | yksikkö- ja UI-testit |
| D12 accent inconsistency | domain tokenit | 3, 5–8 | theme test + screen matrix |
| Stable Material 3 | BOM, alpha override pois | 2 | dependencyInsight |
| Reduced motion | motion tokenit ja mapparit | 4, 8 | manuaali/UI-testi |
| Accessibility | semantics/touch/contrast | 3–8 | checks + TalkBack |
| Ei toimintoregressiota | route/ViewModel/use case säilytys | kaikki | kohdennetut testit |

## 18. Riskirekisteri

| Riski | Todennäköisyys | Vaikutus | Hallinta |
|---|---|---|---|
| Alpha-API:n poisto rikkoo monta wrapper-kutsujaa | korkea | korkea | vaihe 2 omana commitina, käännös heti |
| Uusi chart engine muuttaa fullscreen-käytöstä | keskitaso | korkea | yksi engine, samat testivektorit embedded/fullscreen |
| Light-teeman border tekee joistakin korteista kaksinkertaisia | keskitaso | keskitaso | keskitetty card policy, ActionCard-erityistapaus |
| Font scale rikkoo metric gridin | korkea | keskitaso | responsiivinen 1/2 sarakkeen policy, 2.0 testi |
| Motion näyttää tekaistua progressia | keskitaso | korkea | mapataan vain todellisesta UI-statesta |
| Insights-suunnitelma vaatii puuttuvaa resolved-dataa | korkea | keskitaso | ehdollinen osio, ei data-layer muutosta |
| Weekly Report katoaa Tools-siirrossa | keskitaso | korkea | poista vanha entry vasta uuden reitin todentamisen jälkeen |
| Widgetit eivät tue samoja Compose-ratkaisuja | keskitaso | keskitaso | oma Glance ColorProviders ja rajattu visuaalinen parity |
| Nykyhaaran yhdistäminen sekoittaa UI-työn | korkea | korkea | eristetty worktree, integraatio myöhemmin |
| Laajat previewt kasvattavat käännösaikaa | matala | matala | rajattu ydinkomponenttimatriisi |
| Stability baseline peittää regressiot | keskitaso | keskitaso | tarkista dump ennen baseline-päivitystä |

## 19. Rollback- ja checkpoint-strategia

Jokainen vaihe on oma palautettava checkpoint:

1. regressiotestit;
2. chart/period korjaus;
3. stable Material;
4. tokenit/komponentit;
5. motion;
6. Home/Tools;
7. detail-ryhmät;
8. Insights/Settings;
9. widget/a11y;
10. docs.

Jos vaihe epäonnistuu:

- älä resetoi käyttäjän muuta worktree-työtä;
- tunnista vaiheen oma commit;
- korjaa seuraavassa commitissa tai palauta vain kyseinen commit hallitusti;
- älä yhdistä puolivalmista wrapper-siirtymää muihin ruutumuutoksiin;
- säilytä aina viimeinen kääntyvä checkpoint.

## 20. Definition of Done

Työ on valmis vasta, kun kaikki seuraavat täyttyvät:

- [ ] Toteutus perustuu haaraan `codex/ui-redesign-m3-expressive` @ `88d2609`.
- [ ] Material 3 tulee BOMista vakaana `1.4.0`.
- [ ] Expressive-alpha-riippuvuutta, opt-iniä tai API-kutsuja ei ole.
- [ ] D1–D12 on merkitty PASS lähdekoodi- ja kuvakaappaustodistein.
- [ ] Dark-, Light- ja System-teemat toimivat.
- [ ] Vaalean teeman surface-hierarkia on selkeä.
- [ ] Kaavioiden quality zone ei muuta viewportia.
- [ ] Periodivalitsimen kaikki vaihtoehdot ovat saavutettavissa.
- [ ] Jokainen kaavio näyttää vain yhden päätilan.
- [ ] Home ei sisällä pystysuuntaan romahtavaa confidence-badgea.
- [ ] Insightsin tyhjä tila on tarkoituksellinen ja hyödyllinen.
- [ ] Recently resolved -osiota ei ole teeskennelty puuttuvalla datalla.
- [ ] Network, Thermal, App Usage ja Weekly Report säilyttävät nykyiset toiminnallisuutensa.
- [ ] Speed Test käyttää edelleen NDT7:ää ja cellular warningia.
- [ ] Cleanup säilyttää nykyiset skannaus-, valinta- ja poistopolut.
- [ ] Pro/free/trial-portit eivät ole muuttuneet.
- [ ] Uusia network-, Room-, permission-, billing- tai telemetry-muutoksia ei ole.
- [ ] Kaikki uudet visuaaliset arvot ovat tokeneissa.
- [ ] Ei uusia duplikaattikomponentteja tai kuollutta koodia.
- [ ] Kaikki ikonit ovat `Icons.Outlined`.
- [ ] Touch targetit ovat vähintään 48 dp.
- [ ] Status ei perustu pelkkään väriin.
- [ ] Reduced motion on testattu.
- [ ] Font scale 2.0 on testattu.
- [ ] Kohdennetut unit- ja UI-testit läpäisevät.
- [ ] `compileDebugKotlin`, ktlint, detekt ja stability check läpäisevät.
- [ ] Widgetit on tarkistettu vaaleina ja tummina.
- [ ] Ennen/jälkeen-kuvakaappausmatriisi on valmis.
- [ ] `AGENTS.md` ja `CODEX.md` ovat keskenään linjassa.
- [ ] `PROJECT.md` ja `UI-SPEC.md` kuvaavat toteutunutta tilaa.
- [ ] Git diff sisältää vain tehtävään kuuluvat muutokset.

## 21. Viralliset tekniset lähteet

- AndroidX versions: https://developer.android.com/jetpack/androidx/versions
- Compose Material 3 release notes: https://developer.android.com/jetpack/androidx/releases/compose-material3
- Compose BOM: https://developer.android.com/develop/ui/compose/bom
- Compose BOM mapping: https://developer.android.com/develop/ui/compose/bom/bom-mapping
- Compose app bars: https://developer.android.com/develop/ui/compose/components/app-bars
- Compose graphics overview: https://developer.android.com/develop/ui/compose/graphics/draw/overview
- Compose graphics modifiers: https://developer.android.com/develop/ui/compose/graphics/draw/modifiers
- Compose accessibility testing: https://developer.android.com/develop/ui/compose/accessibility/testing
- Glance themes: https://developer.android.com/develop/ui/compose/glance/theme
- Compose preview screenshot testing: https://developer.android.com/studio/preview/compose-screenshot-testing

## 22. Toteutuksen aloitusjärjestys

Kun toteutus hyväksytään, ensimmäinen työjakso on:

1. worktree ja baseline;
2. D8:n failing viewport -testi;
3. D11:n state precedence -testi;
4. D9:n periodivalitsin;
5. chart-korjausten laitekuvat;
6. vasta tämän jälkeen stable Material 3 -migraatio.

Tämä järjestys tekee kriittisimmistä regressioista todistettavia ennen laajaa komponenttien vaihtoa ja pitää jokaisen checkpointin arvioitavana erikseen.
