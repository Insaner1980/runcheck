# Suunnitelma: runcheck-projektin Android-versioiden kokonaispäivitys

## 1. Tavoite ja valmis-määritelmä

Päivitetään projektin kaikki käytössä olevat Android-, Kotlin-, Gradle-, AndroidX-, runtime-, testaus- ja analysointiriippuvuudet 3.8.2026 varmennettuihin versioihin [stable-versiodokumentin](<C:/Users/emmah/Downloads/android-stable-version-baseline-2026-08-03.md>) perusteella.

Päivitys katsotaan valmiiksi vasta, kun:

- kaikki projektissa käytetyt versiot on joko päivitetty tavoiteversioon tai merkitty perustelluksi yhteensopivuuspoikkeukseksi;
- debug- ja release-lähdejoukot kääntyvät ilman uusia virheitä;
- yksikkötestit, riippuvuustarkistukset, lintit ja projektin omat sopimustestit läpäisevät;
- Billing 9- ja OkHttp 5 -muutokset on tarkastettu myös käyttäytymisen tasolla;
- Gradlen riippuvuuksien varmennusmetadata vastaa uusia artefakteja;
- vanhoja versioita ei jää aktiivisiin konfiguraatioihin vahingossa;
- nykytiladokumentaatio vastaa toteutusta;
- mitään tarkistinta, baselinea, tietoturvasääntöä tai release-suojausta ei ole heikennetty päivityksen läpiviemiseksi.

Päivitys koskee vain projektin oikeasti käyttämiä komponentteja. Dokumentissa mainittuja mutta projektista puuttuvia kirjastoja ei lisätä.

## 2. Lukittu tavoiteversiomatriisi

### Päivitettävä ydintyökaluketju

| Komponentti | Nykyinen | Tavoite | Toteutusperiaate |
|---|---:|---:|---|
| Gradle Wrapper | 9.4.1 | 9.6.1 | Käyttäjän valitsema tavoite; päivitetään wrapper JAR, properties ja virallinen SHA-256 |
| Android Gradle Plugin | 9.2.1 | 9.3.1 | Vakaa API 37 -yhteensopiva versio |
| Kotlin / Compose-plugin | 2.3.0 | 2.4.10 | Yksi yhteinen Kotlin-version lähde |
| erillinen `kotlinRuntime` | 2.3.20 | poistetaan | stdlib-rajoitteet viittaamaan samaan Kotlin 2.4.10 -versioon |
| KSP | 2.3.9 | 2.3.10 | Vakaa tavoite |
| Hilt | 2.59.2 | 2.60.1 | Vakaa tavoite |

AGP 9.3 edellyttää vähintään Gradle 9.5.0:aa ja JDK 17:ää; projektin JDK 17 sekä compile/target API 37 säilyvät. Gradle 9.6.1 hyväksytään vasta projektin omilla tarkistuksilla, koska AGP:n oletusversio on 9.5.0. [AGP 9.3 -julkaisutiedot](https://developer.android.com/build/releases/agp-9-3-0-release-notes), [Kotlin-julkaisut](https://kotlinlang.org/docs/releases.html).

### Päivitettävät sovellus- ja testiriippuvuudet

| Komponentti | Nykyinen | Tavoite |
|---|---:|---:|
| Paging | 3.3.6 | 3.5.0 |
| Activity Compose | 1.12.3 | 1.13.0 |
| Google Play Billing KTX | 8.3.0 | 9.1.0 |
| OkHttp | 4.12.0 | 5.4.0 |
| Gson | 2.11.0 | 2.14.0 |
| MockK | 1.13.16 | 1.14.11 |
| Dependency Analysis Gradle plugin | 3.16.1 | 3.17.0 |

### Nykyiseen vakaaseen versioon jätettävät

Näitä ei muuteta pelkän versionumeron vaihtamisen vuoksi:

- Compose BOM 2026.06.01
- Room 2.8.4
- Navigation Compose 2.9.8
- Lifecycle 2.11.0
- WorkManager 2.11.2
- DataStore 1.2.1
- AndroidX Core 1.19.0
- Profile Installer 1.4.1
- AndroidX Hilt 1.4.0
- coroutines 1.11.0
- kotlinx.serialization 1.11.0
- Glance 1.1.1
- JUnit 4.13.2
- AndroidX Test Runner 1.7.0
- AndroidX Test Ext JUnit 1.3.0
- JaCoCo 0.8.14
- ktlint Gradle plugin 14.2.0
- ktlint-moottori 1.8.0
- Compose Rules ktlint 0.5.9
- OWASP Dependency-Check 12.2.2
- Sonar Gradle plugin 7.3.1.8318
- Android Security Lints 1.0.4
- Foojay resolver 1.0.0
- Sentry Android Core 8.49.0, ellei virallisesta lähteestä löydy toteutushetkellä uudempaa vakaata versiota
- compileSdk 37, targetSdk 37, minSdk 26 ja Java 17

Room 3:een tai Navigation 3:een ei siirrytä. Ne olisivat arkkitehtuuri- ja API-migraatioita, eivät tavallisia versiopäivityksiä.

### Sallitut työkalupoikkeukset

Projektissa on analysointityökaluja, joille ei ole täysin vakaata Kotlin 2.4 / Gradle 9.6 -yhdistelmää:

- Detektin ensisijainen tavoite on `2.0.0-alpha.5`, koska se on virallisen yhteensopivuustaulukon mukaan rakennettu Gradle 9.5.1:llä, Kotlin 2.4.0:lla ja AGP 9.2.1:llä. Plugin-ID säilyy `dev.detekt`. [Detekt-yhteensopivuus](https://detekt.dev/docs/introduction/compatibility/), [Detekt 2 -migraatio](https://detekt.dev/docs/introduction/migration/).
- Compose Rules Detekt säilyy versiossa 0.5.9.
- Compose Stability Analyzerin ensisijainen tavoite on 0.8.0. Sen julkaistu Kotlin-yhteensopivuus ei vielä suoraan kata Kotlin 2.4.10:tä, joten hyväksyntä perustuu projektin debug- ja release-analyysien todelliseen toimintaan. [Compose Stability Analyzer](https://github.com/skydoves/compose-stability-analyzer).
- Jos uusi analysointiversio ei toimi, testataan nykyinen versio uuden ydintyökaluketjun kanssa. Toimiva vanhempi versio voidaan jättää määräaikaiseksi poikkeukseksi.
- Jos uuden ydintyökaluketjun kanssa ei toimi mikään analysointiversio, tarkistinta ei poisteta eikä ohiteta. Kyseinen päivityserä palautetaan kohdennetuilla vastamuutoksilla viimeiseen ehjään yhdistelmään ja yhteensopivuus kirjataan estäväksi ongelmaksi.

Versioluetteloon lisätään sopimustesti, joka hylkää `alpha`, `beta`, `rc`, `preview`, `eap`, `milestone`, `dev` ja `snapshot` -versiot. Detekt on tämän säännön ainoa nimetty semanttinen poikkeus. NDT7:n commit-hash ei ole esiversiotunnus.

## 3. Toteutusvaiheet

### Vaihe A — lähtötilan ja lähteiden jäädyttäminen

Ennen ensimmäistä muutosta:

1. Varmistetaan nykyinen haara, HEAD ja työpuun tila. Lähtötilaksi kirjataan nykyinen commit `cf1cea8c025ab3a978e1877c6a8af45bfee6e2ea`.
2. Varmistetaan, ettei käyttäjän työpuuhun ole tullut uusia muutoksia suunnittelun jälkeen. Uusia tai asiaan liittyviä muutoksia ei ylikirjoiteta.
3. Tallennetaan nykyisten `debugRuntimeClasspath`- ja `releaseRuntimeClasspath`-konfiguraatioiden riippuvuuspuut ignored `reports/`-alueelle vertailua varten.
4. Tarkistetaan JDK-versio, wrapperin nykyinen toiminta, konfiguraatiovälimuistin tila ja aktiiviset Gradle-konfiguraatiot.
5. Tarkistetaan tavoiteversiot uudelleen vain virallisista julkaisu- ja plugin-lähteistä. Jos jokin vakaa tavoite on ehtinyt muuttua 3.8.2026 jälkeen, muutosta ei omaksuta hiljaisesti, vaan suunnitelman tavoitematriisi päivitetään ensin.
6. Nykyisiä `reports/`-tuloksia ei käytetä puhtauden todisteena, koska ne ovat nykyistä HEAD-committia vanhempia.

### Vaihe B — Gradle Wrapper

1. Päivitetään wrapper 9.6.1:een Gradlen omalla wrapper-tehtävällä.
2. Käytetään `bin`-jakelua.
3. Lisätään Gradlen julkaisema 9.6.1-jakelun SHA-256 `distributionSha256Sum`-arvoksi.
4. Tarkastetaan wrapper JARin ja properties-tiedoston diffi; muita wrapper-asetuksia ei muuteta.
5. Varmennetaan:
   - `gradlew --version`
   - `gradlew help`
   - Gradlen asetusten konfiguroituminen
   - konfiguraatiovälimuistin ensimmäinen ja toinen ajo
6. Jos AGP 9.2.1 ei hyväksy Gradle 9.6.1:tä, siirrytään suoraan seuraavaan ydintyökaluvaiheeseen. Tätä välitilaa ei käsitellä epäonnistuneena lopputuloksena.

### Vaihe C — AGP, Kotlin, KSP ja Hilt

Päivitetään keskitetysti [libs.versions.toml](C:/Dev/runcheck/gradle/libs.versions.toml):

- AGP 9.3.1
- Kotlin ja Compose-plugin 2.4.10
- KSP 2.3.10
- Hilt 2.60.1

Samalla:

- poistetaan erillinen `kotlinRuntime`-versioavain;
- säilytetään tarvittavat stdlib-jdk7/jdk8-rajoitteet, mutta sidotaan ne yhteiseen Kotlin-versioon;
- varmistetaan, ettei `org.jetbrains.kotlin.android`- tai kapt-pluginia palauteta;
- säilytetään AGP 9:n sisäänrakennetun Kotlin-tuen nykyinen rakenne;
- tarkistetaan KSP:n generoimat Room- ja Hilt-lähteet;
- korjataan vain todelliset Kotlin 2.4 -käännösvirheet, deprekaatiot tai DSL-muutokset;
- ei tehdä rinnakkaisia tyylirefaktorointeja.

Välitarkistukset:

- Gradle-konfigurointi
- `:app:kspDebugKotlin`
- `:app:compileDebugKotlin`
- `:app:compileReleaseKotlin`
- Hiltin generoitu komponenttipuu
- Room-skeeman generointi ilman skeemadiffiä

Tietokannan version tulee pysyä 10:ssä. Jos Room-skeemaan syntyy muutos, päivitys pysäytetään ja syy tutkitaan; riippuvuuspäivitys ei itsessään oikeuta migraatioon.

### Vaihe D — vakaat AndroidX-, runtime- ja testikirjastot

Päivitetään Paging, Activity Compose, Gson, MockK ja Dependency Analysis -plugin.

Kullekin päivitykselle tarkistetaan ennen koodimuutoksia:

- käytössä olevat importit ja kutsupaikat;
- poistetut tai muuttuneet API:t;
- mahdolliset uudet Kotlin-nullability- tai generics-vaatimukset;
- debug- ja release-konfiguraatioiden todellinen ratkaistu versio;
- ettei vanha versio tule takaisin toisen kirjaston transitiivisena valintana.

Erityistarkistukset:

- Paging 3.5.0: Cleanup-listojen, PagingSource-toteutusten, ViewModelien ja testien lataus-, refresh-, virhe- ja tyhjätilat.
- Activity Compose 1.13.0: Activity- ja Compose-elinkaarikytkennät, back handling sekä state restoration.
- Gson 2.14.0: Roomiin tai asetuksiin tallennettujen JSON-rakenteiden vanhojen arvojen lukeminen. Tallennusmuotoa ei muuteta.
- MockK 1.14.11: testit korjataan vain, jos mockkauksen API tai tiukentunut validointi sitä vaatii.
- Open source -lisenssi- ja notice-tiedot päivitetään vastaamaan uusia versioita, ja niiden sopimustesti ajetaan.

### Vaihe E — Google Play Billing 9.1.0

Billing päivitetään omana eränään, koska kyseessä on käyttäytymiseen vaikuttava pääversiomuutos. [Virallinen Billing 9 -migraatio](https://developer.android.com/google/play/billing/migrate-gpblv9?hl=en).

Nykyinen oikea rakenne säilytetään:

- `ProductDetails` ja `ProductType`
- parametrillinen pending purchases -käyttöönotto
- automaattinen palveluyhteyden palautus
- `queryPurchasesAsync`
- kertamaksullinen tuote
- pending-, acknowledgement-, restore- ja retry-polut
- nykyinen Pro-tilan lähde ja trial-käyttäytyminen

Tarvittavat muutokset:

1. Korjataan kaikki Billing 9:ssä poistuneet symbolit tai allekirjoitukset.
2. Laajennetaan nykyistä virhetulkintaa BillingResultin alavastauskoodeille:
   - maksun hylkäys riittämättömien varojen vuoksi;
   - käyttäjä ei ole oikeutettu ostoon;
   - tuntematon tai puuttuva alavastauskoodi käyttää turvallista yleisviestiä.
3. Lisätään englanninkieliset resurssitekstit; osittaista suomennosta ei lisätä.
4. Poistetaan testien riippuvuus vanhentuneesta `SERVICE_TIMEOUT`-vakiosta ja siihen liittyvä deprekaation ohitus.
5. Varmistetaan, ettei `SERVICE_UNAVAILABLE`, `BILLING_UNAVAILABLE`, verkkovirhe tai käyttäjän peruutus sekoitu onnistuneeseen ostoon.
6. Varmistetaan, ettei ostosta merkitä Proksi ennen oikeaa purchase state- ja acknowledgement-käsittelyä.
7. Release-suojauksia, tuotetunnusta, kertamaksumallia tai trialin nykyistä Pro-käyttäytymistä ei muuteta.

Automaattiset testit kattavat vähintään yhteyden muodostuksen, katkeamisen, restore-polun, pending-oston, acknowledgement-retryn, käyttäjän peruutuksen, tunnetut alavastauskoodit ja tuntemattoman fallbackin.

Laitehyväksyntä tehdään Google Playn lisenssitestaajalla. Jos testitiliä tai testijakelua ei ole saatavilla, päivitystä ei kuvata täysin runtime-varmennetuksi.

### Vaihe F — OkHttp 5.4.0 ja NDT7

NDT7-riippuvuus säilytetään nykyisessä commitissa `e0cb663613eb252a7793216ad28cf54a35677b8f`, joka vastasi tarkistushetkellä käytössä olevan upstream-repositorion HEADia. Sitä ei vaihdeta toisen samannimisen repositorion koordinaattiin.

OkHttp-päivityksessä:

1. Päivitetään eksplisiittinen OkHttp-riippuvuus 5.4.0:aan.
2. Tarkistetaan `SpeedTestService`-toteutuksen `Dns`, `OkHttpClient`, socket factory ja NDT7 `HttpClientFactory` -kytkentä.
3. Säilytetään aktiiviseen Android-verkkoyhteyteen sidottu liikenne.
4. Säilytetään M-Labin automaattinen lähimmän palvelimen valinta; kiinteää palvelin-URLia ei lisätä.
5. Säilytetään peruutus, verkkoyhteyden vaihtuminen, timeoutit ja virhetilojen siivous.
6. Varmistetaan release-R8:lla, ettei OkHttp 5 tai NDT7 tarvitse uusia keep-sääntöjä. Keep-sääntö lisätään vain todistetun minifiointivirheen perusteella.

Laitekokeet:

- Wi-Fi-download ja upload;
- mobiiliverkon varoitus ennen testin käynnistymistä;
- käyttäjän peruutus kesken testin;
- verkkoyhteyden vaihtuminen;
- epäonnistunut palvelinyhteys;
- toisen samanaikaisen nopeustestin esto;
- varmistus, ettei yhteystietojen pelkkä näyttäminen käynnistä verkko- tai ping-kutsua.

### Vaihe G — analysointityökalut ja dokumentoitu poikkeuspolitiikka

1. Päivitetään Detekt ensisijaisesti versioon 2.0.0-alpha.5.
2. Varmistetaan:
   - `dev.detekt`-plugin;
   - nykyinen config;
   - baselinejen latautuminen;
   - `:app:detekt`;
   - `cr`- ja `lc`-reitityksen raporttimuoto;
   - shared Android-checkin kyky tulkita raportit.
3. Päivitetään Compose Stability Analyzer 0.8.0:aan ja ajetaan debug- sekä release-analyysi.
4. Nykyisiä Compose Stability -baselineja ei kirjoiteta automaattisesti uusiksi. Mahdollinen diffi tarkastetaan luokka- ja syytasolla.
5. Jos 0.8.0 ei toimi mutta nykyinen 0.7.0 toimii Kotlin 2.4.10:n kanssa, 0.7.0 jää nimetyksi poikkeukseksi.
6. Jos Detekt alpha.5 ei toimi, nykyinen alpha.3 testataan samalla työkaluketjulla ennen ydintyökalujen palauttamista.
7. Tarkistinta ei poisteta, tehtävää ei muuteta no-opiksi eikä virhettä muunnetta varoitukseksi.

Nykyinen ristiriita korjataan lopuksi: projektidokumenteissa ei saa yhtä aikaa lukea Detekt 1.23.8 / Compose Rules 0.4.x ja toteutuksessa Detekt 2.x / Compose Rules 0.5.9.

### Vaihe H — Gradlen varmennusmetadata ja transitiiviset riippuvuudet

`gradle/verification-metadata.xml` päivitetään hallitusti:

1. Uudet riippuvuudet ratkaistaan ensin tavallisesti, jotta puuttuvat artefaktit ja avaimet näkyvät täsmällisesti.
2. SHA-256- ja PGP-metadata tuotetaan samoista rajatuista konfiguraatioista.
3. Jokainen metadata-diffi tarkastetaan; laajoja trusted artifact-, trusted group- tai key-ohituksia ei lisätä.
4. `verification-keyring.keys` muuttuu vain, jos uusi allekirjoitettu artefakti sitä oikeasti edellyttää.
5. Vanhat komponenttimerkinnät poistetaan vain, jos `dependencyInsight` ja kaikki aktiiviset debug-, release-, build-tool-, lint-, ktlint-, Detekt- ja testikonfiguraatiot osoittavat, ettei niitä enää ratkaista.
6. Metadataa ei tyhjennetä eikä generoida sokkona kokonaan uudelleen.
7. OSV:n metadatahavainnot käsitellään erillään runtime-riippuvuuksista, koska vanha build-tool-artefakti voi näkyä löydöksenä, vaikka sovelluksen runtime-puu olisi puhdas.
8. Uusia dependency lockfileja ei luoda. Projektin tarkoituksella poistettua dependency locking -käytäntöä ei palauteta tämän päivityksen yhteydessä.

`gradle.properties`-tiedoston build-tool-turvaversiot säilyvät, ellei tuore tarkistus osoita tiettyä uudempaa vakaata ja yhteensopivaa versiota. Pakettitason laajoja pakotuksia tai OSV-ohituksia ei lisätä.

Kotlin 2.4.10:n jälkeen arvioidaan nykyinen `org.gradle.caching=false`:

- build cache voidaan ottaa käyttöön vain, jos sitä estäneen Kotlin-haavoittuvuuden korjaus varmistuu virallisesta lähteestä;
- kaksi peräkkäistä muuttumatonta rakennusta tuottaa hyväksyttävän cache-käyttäytymisen;
- testit, generoitu koodi ja artefaktit säilyvät samoina;
- muussa tapauksessa asetus jätetään pois käytöstä ja kommentti päivitetään vastaamaan todellista syytä.

## 4. Julkiset rajapinnat ja pysyvät yhteensopivuudet

Suunnitelma ei muuta:

- tietokantaskeemaa tai Room-migraatioita;
- navigaatioreittejä;
- domain-malleja;
- minSdk-, compileSdk- tai targetSdk-arvoja;
- sovelluksen pakettinimeä;
- kertamaksullista Pro-tuotemallia;
- englanninkielistä lokalisaatiolinjaa;
- release-version telemetriattomuutta;
- debug-only Sentry -rajausta;
- NDT7-palvelua tai verkkoliikenteen sallittuja käyttötapauksia.

Ainoa tarkoituksellinen käyttäytymismuutos on Billing 9:n tarkempi virheiden esittäminen. Mahdolliset Kotlin-, Paging-, Gson- tai OkHttp-yhteensopivuuskorjaukset pidetään sisäisinä.

## 5. Testaus- ja hyväksyntäjärjestys

### Jokaisen erän jälkeen

- Gradle-konfigurointi
- riippuvuuksien varmennus
- `:app:compileDebugKotlin`
- kyseiseen erään liittyvät rajatut yksikkötestit
- Git-diffin tarkistus
- varmistus, ettei muutos koske erän ulkopuolisia ominaisuuksia

### Kohdistetut sopimus- ja regressiotestit

Ajetaan vähintään:

- `DependencyVersionCatalogContractTest`
- BillingManagerin API-, helper-, purchase state-, ProManager- ja Pro UI -testit
- SpeedTest connection precheck-, response parsing-, network identity lock- ja active session -testit
- PagingSource- ja cleanup-ViewModel-testit
- Gsonia käyttävien repositoryjen vanhan datan lukutestit
- Room migration- ja DAO-testit
- `OpenSourceNoticesContractTest`
- `ReleaseBuildContractTest`
- `ComposeStabilityBaselineContractTest`

Version catalog -sopimustesti päivitetään tarkistamaan:

- Gradle 9.6.1:n URL ja checksum;
- AGP 9.3.1;
- Kotlin 2.4.10 yhtenä version lähteenä;
- KSP vähintään 2.3.10;
- Dependency Analysis 3.17.0;
- Detektin täsmällinen hyväksytty poikkeus;
- ettei luetteloon jää luvattomia esiversioita tai vanhoja rinnakkaisia Kotlin-versioita.

### Projektitason automaattiset portit

Kun kohdistetut tarkistukset ovat puhtaita:

1. koko debug-yksikkötestisarja;
2. debug-assemble;
3. release-Kotlin-käännös;
4. release-lint ja release Compose Stability;
5. release-R8/minifiointi sellaisella tehtävällä, joka ei kierrä signing-suojauksia;
6. ktlint, Detekt ja Compose Rules;
7. `dc` riippuvuusvarmennus-, OSV- ja OWASP-polku;
8. `cs` Compose Stability;
9. `sentry`, jonka tulee todistaa debug-Sentryn olemassaolo ja release-Sentryn puuttuminen.

Repositoryn ohjeen mukaisesti toteuttaja ei aja itse `lc`- tai `sc`-wrapperia. Käyttäjä ajaa lopullisesta tilasta:

```powershell
lc
sc -Full
```

Sen jälkeen luetaan tuoreet run-ID:t, `reports/latest.json`, manifestit, hashit ja analyysikohtaiset raportit. Vanhoja raportteja ei hyväksytä.

Tulos raportoidaan eriteltynä:

- aktiiviset löydökset;
- baselineen kuuluvat löydökset;
- sallitut tarkat poikkeukset;
- työkaluketjun varoitukset;
- tekniset virheet;
- debug- ja release-kattavuus.

### Release- ja laiteportit

- Signed release bundle rakennetaan vain, jos tarvittavat signing-ympäristömuuttujat ovat saatavilla. Salaisuuksia ei tulosteta.
- Signing-suojausta tai versionCode-lattiaa ei ohiteta.
- Jos signing-tietoja ei ole, signed release jää nimetyksi ulkoiseksi hyväksyntäportiksi.
- Billing 9 testataan Play-lisenssitestaajalla.
- NDT7 testataan oikealla laitteella Wi-Fi- ja mobiiliverkossa.
- Room-migraatiotesti ajetaan emulaattorilla tai laitteella.
- Sovellusta ei poisteta laitteelta allekirjoitusristiriidan ratkaisemiseksi; käytetään vain turvallista päivitysasennusta tai raportoidaan este.

## 6. Dokumentaatio ja lopputulos

Onnistuneen teknisen hyväksynnän jälkeen päivitetään nykytilaa kuvaavat dokumentit:

- `PROJECT.md`: todelliset versiot, tarkistuspäivä, poikkeukset ja build cache -tila;
- `AGENTS.md` ja `CODEX.md`: yhtenevä Detekt-, Compose Rules- ja työkaluketjukuvaus;
- `CLAUDE.md`, jos se säilyy aktiivisena projektiohjeena: sama nykytilakuvaus;
- lisenssi-/notice-aineisto: todelliset käytössä olevat riippuvuudet.

Historiallisia suunnitelmia tai changelog-merkintöjä ei kirjoiteta jälkikäteen uusiksi. Stable-versiodokumenttia ei muokata.

Muistiin ei kirjoiteta, koska käyttäjä ei pyytänyt muistipäivitystä eikä tämä päivitys lähtökohtaisesti muuta arkkitehtuuria.

Loppuraportti sisältää:

- jokaisen käytössä olevan komponentin nykyinen ja lopullinen versio;
- päivitetyt, ennallaan jätetyt ja poikkeukseksi jääneet komponentit;
- poikkeusten tekninen syy ja poistamisehto;
- suoritetut testit ja niiden tulokset;
- mahdolliset käyttäjän ajettaviksi jääneet portit;
- vahvistus siitä, ettei tietokanta-, navigaatio-, Pro-, telemetria- tai verkkokäyttäytymistä muutettu tahattomasti.

## 7. Virhe- ja palautusperiaatteet

- Muutokset tehdään pieninä, toisistaan eroteltuina erinä.
- Epäonnistunut erä palautetaan kohdennetuilla vastamuutoksilla; `git reset --hard`- tai muita työpuuta hävittäviä komentoja ei käytetä.
- Baselineja ei päivitetä vain siksi, että tarkistus muuttuisi vihreäksi.
- Riippuvuuden versiota ei pakoteta sovelluksen koko runtime-puuhun build-tool-haavoittuvuuden korjaamiseksi.
- Jos vakaa versio rikkoo dokumentoidun toiminnallisuuden eikä turvallista migraatiota ole, päivitystä ei merkitä valmiiksi.
- Tekninen tarkistusvirhe ei ole CLEAN-tulos.
- Työpuun ulkopuolisia muutoksia, committeja, brancheja, pushausta tai pull requestia ei tehdä ilman erillistä lupaa.
- Jos commitit myöhemmin pyydetään, ne tehdään eräkohtaisesti suomenkielisin viestein.

## 8. Lukitut oletukset

- Tavoitetila perustuu 3.8.2026 varmennettuihin vakaisiin versioihin.
- Gradlen tavoite on käyttäjän valitsema 9.6.1, ei AGP:n konservatiivinen 9.5.0.
- Detekt ja tarvittaessa Compose Stability Analyzer saavat jäädä dokumentoiduksi yhteensopivuuspoikkeukseksi, mutta tarkistimia ei poisteta.
- Room 3-, Navigation 3-, Detekt 1 -paluu, Detekt-tarkistuksen poisto, dependency locking ja Qodana-linterituotteen vaihto eivät kuulu tähän työhön.
- Päivitys ei ole valmis ennen tuoreita lint-, tietoturva-, riippuvuus-, debug-, release- ja laitetuloksia tai selvästi raportoitua ulkoista hyväksyntäestettä.
