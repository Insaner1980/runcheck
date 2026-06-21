Käyn promptin läpi PROJECT.md:n pohjalta. Tiivistän — jätän pois kohdat joissa kaikki on kunnossa, ja keskityn löydöksiin.

## 1. Kymmenen tärkeintä (tärkeysjärjestyksessä)

**1. Target SDK on Android 17 beta — v1.0-blokkeri.** Play Store ei hyväksy production-julkaisua preview-SDK:ta vasten. Release-build pitää kääntää vakaata API-tasoa vasten (API 36 / Android 16). CinnamonBun voi jäädä kehityshaaraan, mutta julkaisuartefakti tarvitsee stable compileSdk/targetSdk. Tämä on isoin yksittäinen asia koko dokumentissa. Työmäärä: pieni–keskisuuri (riippuu siitä, käytetäänkö API 37 -only -rajapintoja). Riski jos ei tehdä: julkaisu ei mene läpi ollenkaan.

**2. Ensikäynnistyksen tyhjät tilat.** Lähes kaikki arvo (historiakaaviot, insightit, charger comparison, health score -trendit) vaatii kertynyttä dataa. Uusi käyttäjä näkee ensimmäiset tunnit tyhjiä kaavioita ja nolla insightia — juuri silloin kun hän päättää pitääkö appin. PROJECT.md ei kuvaa "collecting data" -tiloja. Jokainen historiapinta tarvitsee selkeän "Keräämme dataa, palaa parin tunnin päästä" -tilan ja mieluiten arvion milloin dataa on tarpeeksi. Blokkeri, koska tämä on ensivaikutelma ja arvostelut tulevat siitä.

**3. Thermal score rankaisee puuttuvasta CPU-lämpötilasta.** CPU temp on aina `null` (ei sysfs-lukuja), ja silti scoring "penalizes missing/known CPU temperature". Eli käytännössä jokainen laite saa pysyvän miinuksen sensorista jota ei ole. Health score näyttää huonompaa kuin todellisuus — ristiriidassa koko "rehellinen mittausluotettavuus" -differentiaattorin kanssa. Puuttuva data ei saa laskea pisteitä; sen pitää siirtää painoa muille komponenteille. Blokkeri.

**4. Network 25 % painolla, ja disconnected = 0.** Lentotilassa tai ilman SIM:iä oleva täysin terve puhelin saa health scoreen -25 pistettä. Käyttäjä tulkitsee "laitteeni on huonossa kunnossa". Disconnected pitäisi käsitellä "ei arvioitavissa" -tilana ja normalisoida painot, ei nollana. Sama logiikkavirhe kuin kohdassa 3. Blokkeri.

**5. Tuore speed test vaikuttaa health scoreen tunnin.** Heikolla kuuluvuudella ajettu cellular-testi laskee koko laitteen kuntopisteitä tunniksi. Vähintään cellular-tulokset kannattaa jättää scoresta pois, tai painottaa vain Wi-Fi-tuloksia.

**6. Insights free vs. Pro on epäselvä — strateginen päätös puuttuu.** PROJECT.md:n mukaan Home-yhteenveto *ja* koko Insights-näyttö ovat kaikille. Jos pääerottautumistekijä on kokonaan ilmainen, mikä myy Pron? Looginen paketti: 1–3 kuratoitua insightia ilmaiseksi (maistiainen), koko lista + historia + insightien deep-link-kohteet Prohon. Päätettävä ennen julkaisua, koska gaten lisääminen jälkikäteen suututtaa käyttäjät.

**7. Trial nollautuu uudelleenasennuksella.** Trial-tila on DataStoressa, `allowBackup="false"`, ei server-anchoria → ikuinen ilmainen trial reinstallaamalla. Solo-devinä tämä voi olla hyväksyttävä riski (suurin osa ei viitsi), mutta päätä se tietoisesti. Kevyt parannus ilman serveriä ei oikein ole olemassa; Play Billingin kautta voisi tarkistaa aiemman "oston" vain jos trial olisi Billing-pohjainen.

**8. Pro myy ominaisuuksia jotka eivät toimi kaikilla laitteilla.** minSdk 26, mutta Old Downloads- ja APK-cleanup vaativat API 30+. Android 8–10 -käyttäjä ostaa Pron ja saa vajaan cleanupin. Pro Upgrade -näytön (kun se tehdään) pitää näyttää vain laitteella toimivat ominaisuudet, tai vähintään merkitä rajoitukset.

**9. Widget Pro-gatauksen free-kokemus.** Widgetit näkyvät launcherin widget-pickerissä kaikille. Mitä free-käyttäjä näkee lisätessään widgetin? Tarvitaan siisti locked-tila widgetissä itsessään (CTA appiin), ei rikkinäistä/tyhjää widgetiä.

**10. Play-deklaraatiot sensitiivisille pinnoille.** `PACKAGE_USAGE_STATS`, location (SSID), `FOREGROUND_SERVICE_SPECIAL_USE` — kaikki kolme vaativat Play Consolessa perustelut, ja specialUse erityisesti on tarkassa syynissä. Valmistele perustelutekstit ja data safety -lomake etukäteen; nämä ovat yleisin hylkäyssyy tämän tyyppisille apeille.

## 2. Differentiaatio ilman AI:ta

11 sääntöä on uskottava pohja. Luotettavuuden tunne syntyy todisteista: jokaisen insightin pitäisi näyttää *mihin dataan se perustuu* — "lämpötila nousi 8 °C ja drain kasvoi 40 % samoissa ikkunoissa viim. 5 päivänä" + linkki kaavioon jossa korrelaatio näkyy. Insight ilman evidenssiä tuntuu horoskoopilta; insight + kaavio tuntuu diagnostiikalta. Tämä on v1.1-tason parannus mutta isoin yksittäinen keino erottua. Confidence-suodatus 0.6:ssa on järkevä; harkitse confidence-badgen näyttämistä myös insighteissa (sama Accurate/Estimated-kieli kuin mittauksissa — yhtenäinen rehellisyysteema).

## 3. UX ja onboarding

Trial-UI:ssa on paljon kerroksia (welcome sheet, day-5 banner, expiration modal, post-expiration card, pacing). Rakenne on hyvä, mutta varmista ettei mikään näistä keskeytä käyttäjää ensimmäisen session aikana — welcome sheet riittää, loput myöhemmin. Wi-Fi-permission-help-card ja media-permission-card ovat oikein. Suurin aukko on kohdan 2 tyhjät tilat. Lisäksi: "monitoring stale" -tila on hyvä, mutta varmista että se kertoo käyttäjälle *miksi* (OEM-akkuoptimointi tappaa workerit — tämä on Suomessa ja muualla yleisin "appi ei toimi" -valitus, etenkin Xiaomi/Samsung).

## 4. Mittausluotettavuus

Tämä osa-alue on projektin vahvin: currentNow-validointi, yksikkönormalisointi, sign-konventio, vendor-lähteet, confidence-badget — kaikki kunnossa. Korjattavat asiat ovat scoring-puolella (kohdat 3–5 yllä). Lisäksi: "Remaining time" -estimaatit live-notifikaatiossa ja battery-näytöllä pitäisi aina merkitä estimaateiksi samalla badge-kielellä.

## 5. Arkkitehtuuri

Ei merkittävää velkaa. Yksi moduuli on oikea ratkaisu. `ProFeature`-enum jota ei käytetä per-feature-gateen on hyväksyttävää tulevaisuusvarausta. Gson toimii; kotlinx.serialization-vaihto ei ole v1.0-asia. `pro_status_cache` SharedPreferencesissa cold start -flashin estoon on pragmaattinen ja oikein.

## 6. Akkuvaikutus

333 ms UI-throttle, 5 s opt-in foreground, 15/30/60 min workerit, 6 h insight-generointi — kaikki turvallisia. Kaksi tarkistettavaa: (a) thermal headroom -pollaus 3 s välein — varmista että se pyörii vain Thermal-näytön ollessa aktiivinen, ei Homessa; (b) `GetMeasuredNetworkStateUseCase` tekee TCP-latencyn 30 s välein — jos Home käyttää samaa use casea, Home tekee verkkokutsuja aina auki ollessaan. Jos näin on, latency-mittaus vain Network-näytölle.

## 7. Yksityisyys

Posture on poikkeuksellisen hyvä (ei backupia, ei cleartextia, kolme hyväksyttyä outbound-pintaa, release-Sentry no-op). Yksi konkreettinen parannus: API 33+ voi saada SSID:n `NEARBY_WIFI_DEVICES` + `neverForLocation` -lipulla ilman location-permissionia. Se poistaisi location-deklaraation data safety -lomakkeesta valtaosalla käyttäjistä — iso voitto sekä Play-reviewissä että käyttäjien luottamuksessa. Location jäisi vain ≤API 32 -fallbackiksi. Lisäksi: varmista että `exports/`-cachen vanhat CSV:t siivotaan.

## 8. Monetisaatio

Reilut gatet: Charger Comparison, App Usage, Extended History, Thermal logs, Remaining time. Kyseenalaiset: **CSV export** (käyttäjän oma data — panttivankifiilis, ja "anna datani ulos vain maksusta" näyttää huonolta myös privacy-näkökulmasta; suosittelen freehen) ja **koko cleanup-reitti** (Files by Google tekee saman ilmaiseksi — tämä gate generoi "paywall scam" -yhden tähden arvosteluja; vaihtoehto: skannaus ja näkymä ilmaiseksi, poisto Prossa, jolloin arvo näytetään ennen maksua). Widgetit Prossa on ok jos locked-tila on siisti.

## 9. Testaus

Tärkeimmät lisäykset ennen v1.0: (1) migraatioketju 1→10 yhtenä instrumentoituna testinä — schema-assetit on jo olemassa, tämä on halpa; (2) Billing-tilasiirtymät release-variantin logiikalla (pending → purchased → acknowledged, retry-polut) — debug-pakotettu Pro tarkoittaa että release-billing on käytännössä testaamaton arjessa; (3) WorkManager TestDriver -testit kaikille kolmelle workerille constraint-käyttäytymisineen; (4) insight-sääntöjen raja-arvotestit (juuri alle / juuri yli kynnysten, tyhjä historia, yhden datapisteen historia); (5) yksi instrumentoitu smoke-testi joka kävelee koko navigaatiograafin + deep-linkit läpi. Instrumentoituja testejä on nyt 1 kpl — se on ohuin kohta koko verifioinnissa.

## 10. Alusta- ja riippuvuusstrategia

Konservatiivinen linja v1.0:lle: stable SDK (kohta 1), AGP 9.1.0 ja Kotlin-plugin 2.3.0 paikallaan (CodeQL- ja Qodana-rajoitteet on jo dokumentoitu oikein), Compose BOM -bump 2026.04.01:een vasta v1.0:n jälkeen. Kaikki nostot kerralla yhdessä "tooling bump" -PR:ssä julkaisun jälkeen, ei julkaisuviikolla.

## 11. AI-jäänteet

PROJECT.md on puhdas — ei AI-sanastoa. Ainoa varauksellinen termi on "anomaly detection", joka on tilastotermi mutta voi *markkinointitekstissä* kuulostaa ML:ltä. Sovelluksen sisällä ja Play-listauksessa kannattaa käyttää muotoa "rule-based analysis, runs entirely on your device, no AI, no cloud" — se on nykyilmapiirissä jopa myyntivaltti. Tarkista Play-listaus ja Learn-artikkelit samalla linssillä kun ne kirjoitetaan.

## 12. Toteutusjärjestys

**v1.0-blokkerit:** stable targetSdk -release-build; tyhjät/keräystilat; CPU-temp- ja disconnected-scoring-korjaukset; insights free/Pro -päätös; Play-deklaraatiot; migraatio- ja billing-testit; widgetin locked-tila.
**v1.0 nice-to-have:** cellular-speedtestin poisto scoresta; CSV free-tieriin; cleanup-gaten kevennys; NEARBY_WIFI_DEVICES.
**v1.1:** insight-evidenssilinkit kaavioihin; OEM-akkuoptimointiohjeistus stale-tilaan; insight-confidence-badget.
**Myöhemmin:** Learn read-state; per-feature-gatet jos tarvetta; kotlinx.serialization.

## 13. Suora rehellisyys

Ylirakennettua: security-tooling-pino (5 CI-workflowta + 15 PowerShell-wrapperia) on enterprise-tasoa solo-deville jolla on nolla käyttäjää — se ei ole väärin, mutta jokainen AGP/Kotlin-nosto maksaa nyt moninkertaisesti, ja se aika on pois julkaisusta. Samoin chart-animaatioiden hiominen ("Instrument Sweep", glow pulse) on tehty ennen kuin ensikäyttökokemus tyhjillä datoilla on edes suunniteltu — prioriteetti on ollut väärinpäin. Alirakennettua: ensikäynnistys, release-billingin todellinen testaus ja instrumentoitu testaus ylipäätään. Riskialtteinta: beta-SDK:n varaan rakennettu julkaisupolku ja se, ettei kukaan ole vielä päättänyt mikä Prossa oikeasti myy, jos insightit ovat ilmaisia. Appin tekninen laatu on selvästi keskimääräistä indie-appia parempi — ongelmat eivät ole koodissa vaan julkaisukuntoon liittyvissä päätöksissä joita ei ole vielä tehty.

Mainitsemiesi puuttuvien osien (onboarding, Pro Upgrade -näyttö, kynnysarvot) osalta en spekuloinut — ne näkyvät yllä vain siltä osin kuin ne ovat riippuvuuksia muille päätöksille (kohta 6 ja 8).