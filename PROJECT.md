# DevicePulse — Projektin kuvaus

## Yleiskatsaus

DevicePulse on natiivi Android-sovellus, joka seuraa laitteen terveyttä neljällä osa-alueella: akku, verkko, lämpötila ja tallennustila. Sovellus tarjoaa reaaliaikaisen diagnostiikan, yhdistetyn terveyspistemäärän ja pitkäaikaisen trendihistorian — kaikki yksityisyys edellä, täysin paikallisesti laitteella.

Sovellus on suunniteltu käyttäjille, jotka haluavat selkeän näkymän puhelimensa päivittäiseen toimintaan: akun kuntoon ja latauksen käyttäytymiseen, verkon laatuun, lämpötilaan ja ylikuumenemisriskiin sekä tallennustilan käyttöön.

---

## Tuotteen visio

DevicePulse ei ole yleinen "kaikki tilastot kaikkialla" -työkalu. Tuotesuunta on:

- Nopea yleiskatsaus kotinäkymässä
- Terveyspainotteinen diagnostiikka detaljinäkymissä
- Yhteyspainotteiset työkalut verkkonäkymässä (nopeustesti, signaalinlaatu)
- Toissijaiset mutta tärkeät työkalut (laturivertailu, sovelluskohtainen akunkäyttö, vienti)

Sovelluksen tulee tuntua modernilta, luettavalta ja teknisesti uskottavalta olematta sekava tai meluisa. Tavoite ei ole näyttää geneeriseltä benchmarktyökalilta tai pelipaneelilta — DevicePulsen tulee tuntua premium-tuotteelta: käytännölliseltä ja luotettavalta.

---

## Tekninen pino

| Komponentti | Teknologia | Versio |
|-------------|-----------|--------|
| Kieli | Kotlin | 2.3.0 |
| UI-kehys | Jetpack Compose + Material 3 | BOM 2026.02.01 |
| Arkkitehtuuri | MVVM + Clean Architecture (data → domain → ui) | — |
| Tietokanta | Room | 2.8.4 |
| Asynkronisuus | Kotlin Coroutines + Flow | 1.10.2 |
| Riippuvuusinjektio | Hilt (Dagger) | 2.59.2 |
| Kaaviot | Vico (Compose-natiivi) | 3.0.3 |
| Navigaatio | Navigation Compose | 2.9.0 |
| Taustatehtävät | WorkManager | 2.11.1 |
| Asetukset | DataStore Preferences | 1.2.0 |
| Laskutus | Google Play Billing | 8.3.0 |
| Nopeustesti | M-Lab NDT7 + OkHttp | 1.0.0 / 4.12.0 |
| Widgetit | Glance AppWidget + Material 3 | 1.1.1 |
| Sarjallistaminen | Gson | 2.11.0 |
| Build-järjestelmä | Gradle (Kotlin DSL) + AGP + KSP | 9.4.0 / 9.1.0 / 2.3.1 |
| Testaus | JUnit 4 + MockK + Coroutines Test + Compose UI Test | — |

### Android-versiovaatimukset

- **Min SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 35 (Android 15)
- **Compile SDK:** 36 (vaaditaan Vico 3.x:lle)

---

## Sovelluksen rakenne

### Arkkitehtuurikerrokset

Sovellus noudattaa Clean Architecture -periaatetta kolmella kerroksella:

**Data-kerros** — Laiterajapinnat, tietokannat ja repositoriot:
- `battery/` — BatteryManager-käärimet, sysfs-lukijat, valmistajakohtaiset tietolähteet
- `network/` — ConnectivityManager, TelephonyManager, nopeustestirepositoriot
- `thermal/` — ThermalManager, CPU-lämpötilan sysfs-lukijat
- `storage/` — StorageStatsManager, tallennustilatiedot
- `device/` — Laitetunnistus, DeviceProfile, kapasiteettien kartoitus
- `appusage/` — Sovelluskohtainen akunkäyttödata
- `billing/` — Pro-tilan hallinta ja välimuisti
- `preferences/` — Käyttäjäasetusten DataStore-toteutus
- `export/` — CSV-vientitoiminnallisuus
- `db/` — Room-tietokanta, DAO:t, entiteetit, migraatiot

**Domain-kerros** — Liiketoimintalogiikka ja mallit:
- `model/` — Puhtaat data-luokat (BatteryState, NetworkState, HealthScore jne.)
- `usecase/` — Liiketoimintasäännöt (terveyspisteytys, historiahaku, vienti jne.)
- `scoring/` — Terveyspistelaskenta-algoritmi
- `repository/` — Rajapinnat data-kerrokseen (abstraktiot)

**UI-kerros** — Näkymät, ViewModelit ja navigaatio:
- `home/` — Kotinäkymä (sovelluksen keskiö)
- `battery/` — Akkudetaljinäkymä + ViewModel
- `network/` — Verkkodetaljinäkymä + ViewModel
- `thermal/` — Lämpötiladetaljinäkymä + ViewModel
- `storage/` — Tallennustiladetaljinäkymä + ViewModel
- `charger/` — Laturivertailunäkymä + ViewModel
- `appusage/` — Sovelluskohtainen akunkäyttönäkymä + ViewModel
- `settings/` — Asetukset + ViewModel
- `theme/` — Väriteema, typografia, välistykset
- `components/` — Jaetut komponentit (25 kpl)
- `common/` — Muotoiluapufunktiot
- `navigation/` — Navigaatiograafi + reittiluokka

**Palvelut:**
- `service/monitor/` — WorkManager-taustatyöt, reaaliaikaseuranta, ilmoitukset

---

## Näkymät ja navigaatio

### Navigaatiorakenne

Sovellus käyttää push-pohjaista navigaatiota yhdestä kotinäkymästä. Pohjanavigointi (bottom nav) on poistettu. Kaikki detaljinäkymät avataan kotinäkymästä painamalla korttia tai listarivejä, ja paluunavigaatio tapahtuu takaisin-nuolella.

### Näkymähierarkia

```
Kotinäkymä (Home)
├── Akku (Battery Detail)
│   └── Laturivertailu (Charger Comparison)
├── Verkko (Network Detail)
│   └── Nopeustesti (Speed Test)
├── Lämpötila (Thermal Detail)
├── Tallennustila (Storage Detail)
├── Sovellusten akunkäyttö (App Usage) — PRO
├── Asetukset (Settings)
└── Pro-päivitys (Pro Upgrade)
```

### Näkymien kuvaukset

| Näkymä | Reitti | Kuvaus |
|--------|--------|--------|
| **Home** | `home` | Scrollattava päänäkymä: Health Score -hero, Battery-hero, 2×2 ominaisuusruudukko, pikatyökalut, Pro-banneri |
| **Battery** | `battery` | Hero-rengas, ryhmitellyt akkudetaljit, latausvirta ja istuntokooste, latausajan arvio, istuntograafi, historiapaneeli |
| **Network** | `network` | Yhteystyyppi, signaalinvoimakkuus, WiFi/mobiili-detaljit, latenssi, nopeustestipainike |
| **Speed Test** | `speed_test` | NDT7-nopeustesti: latenssi, lataus, lähetys — reaaliaikainen mittarianimaatio |
| **Thermal** | `thermal` | Akun ja CPU:n lämpötilat, lämpötilaheadroom, throttling-tapahtumat, HeatStrip-visualisointi |
| **Storage** | `storage` | Kokonaistila/käytetty/vapaa, sovellusten ja median jaottelu, käyttöaste |
| **Charger** | `charger` | Laturiprofiilien hallinta, latausistuntojen vertailu, keskimääräinen latausnopeus — PRO |
| **App Usage** | `app_usage` | Sovelluskohtainen akkukulutus, etuala-aika, arvioitu kulutus mAh — PRO |
| **Settings** | `settings` | Seurantaväli, ilmoitukset, Pro-osto, CSV-vienti, tietoja |

---

## Domain-mallit

### Päämallit

| Malli | Kuvaus | Keskeiset kentät |
|-------|--------|-----------------|
| **BatteryState** | Akun nykyinen tila | level, voltageMv, temperatureC, currentMa (MeasuredValue), chargingStatus, plugType, health, technology, cycleCount?, healthPercent? |
| **NetworkState** | Verkon nykyinen tila | connectionType, signalDbm?, signalQuality, wifiSsid?, wifiSpeedMbps?, carrier?, networkSubtype?, latencyMs? |
| **ThermalState** | Lämpötilojen tila | batteryTempC, cpuTempC?, thermalHeadroom?, thermalStatus, isThrottling |
| **StorageState** | Tallennustilan tila | totalBytes, availableBytes, usedBytes, usagePercent, appsBytes?, mediaBytes?, sdCardAvailable |
| **HealthScore** | Yhdistetty terveyspistemäärä | overallScore, batteryScore, networkScore, thermalScore, storageScore, status |
| **MeasuredValue\<T\>** | Arvo luotettavuustiedolla | value, confidence (HIGH / LOW / UNAVAILABLE) |
| **UserPreferences** | Käyttäjän asetukset | themeMode, amoledBlack, dynamicColors, monitoringInterval, notificationsEnabled |

### Historiayksellä

| Malli | Kuvaus |
|-------|--------|
| **BatteryReading** | Yksittäinen akkulukema aikaleimattu tietokantaan |
| **SpeedTestResult** | Nopeustestin tulos: download/upload Mbps, ping, jitter, palvelin, yhteystyyppi |
| **ChargerProfile** | Nimetty laturiprofiili |
| **ChargingSession** | Latausistunto: aloitus/lopetus, tasot, virta, jännite, teho |
| **ChargerSummary** | Laturin yhteenvetostatistiikka (istuntojen lukumäärä, keskimääräinen nopeus jne.) |
| **AppBatteryUsage** | Sovelluskohtainen akunkäyttö: paketti, etuala-aika, arvioitu kulutus |
| **ThrottlingEvent** | Lämpötilan throttling-tapahtuma: status, lämpötilat, etuala-sovellus, kesto |
| **DeviceProfileInfo** | Laitteen kapasiteettitiedot: valmistaja, malli, API-taso, mittauskyky |

### Enumeraatiot

| Enum | Arvot |
|------|-------|
| ChargingStatus | CHARGING, DISCHARGING, FULL, NOT_CHARGING |
| PlugType | AC, USB, WIRELESS, NONE |
| BatteryHealth | GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, COLD, UNKNOWN |
| ConnectionType | WIFI, CELLULAR, NONE |
| SignalQuality | EXCELLENT, GOOD, FAIR, POOR, NO_SIGNAL |
| ThermalStatus | NONE, LIGHT, MODERATE, SEVERE, CRITICAL, EMERGENCY, SHUTDOWN |
| HealthStatus | HEALTHY, FAIR, POOR, CRITICAL |
| Confidence | HIGH, LOW, UNAVAILABLE |
| ThemeMode | LIGHT, DARK, SYSTEM |
| MonitoringInterval | FIFTEEN (15min), THIRTY (30min), SIXTY (60min) |
| HistoryPeriod | DAY, WEEK, MONTH, ALL |

---

## Käyttötapaukset (Use Cases)

Sovellus sisältää 15 käyttötapausta, jotka toteuttavat liiketoimintalogiikan:

| Käyttötapaus | Kuvaus |
|-------------|--------|
| **GetBatteryStateUseCase** | Hakee akun nykyisen tilan (Flow) |
| **GetNetworkStateUseCase** | Hakee verkon nykyisen tilan (Flow) |
| **GetThermalStateUseCase** | Hakee lämpötilan nykyisen tilan (Flow) |
| **GetStorageStateUseCase** | Hakee tallennustilan nykyisen tilan (Flow) |
| **CalculateHealthScoreUseCase** | Yhdistää neljän osa-alueen tilat kokonaisterveyspisteeksi |
| **GetBatteryHistoryUseCase** | Hakee akkulukemat aikaleiman jälkeen; kunnioittaa Pro-rajoitusta (vapaa = 24h) |
| **GetChargerComparisonUseCase** | Kokoaa laturiprofiilit ja istunnot, laskee keskimääräiset nopeudet |
| **GetAppBatteryUsageUseCase** | Hakee sovelluskohtaisen akunkäytön |
| **RunSpeedTestUseCase** | Suorittaa nopeustestin (Flow\<SpeedTestProgress\>) |
| **ExportDataUseCase** | Vie akku-, verkko-, lämpötila- ja tallennustiladatan CSV:ksi |
| **GetThrottlingHistoryUseCase** | Hakee lämpötilan throttling-tapahtumat |
| **GetSpeedTestHistoryUseCase** | Hakee aikaisemmat nopeustestin tulokset |
| **ManageChargingSessionUseCase** | Käynnistää ja päättää latausistuntoja mittaustiedoilla |
| **RecordThrottlingEventUseCase** | Kirjaa lämpötilan throttling-tapahtumat |
| **CleanupOldReadingsUseCase** | Poistaa yli 24h vanhat lukemat (Pro-käyttäjillä pidempi säilytys) |

---

## Tietokanta

### Room-entiteetit

| Taulu | Tarkoitus | Indeksointi |
|-------|----------|-------------|
| `battery_readings` | Historialliset akkulukemat | timestamp |
| `network_readings` | Historialliset verkkomittaukset | timestamp |
| `thermal_readings` | Historialliset lämpötilamittaukset | timestamp |
| `storage_readings` | Historialliset tallennustilaluvut | timestamp |
| `charger_profiles` | Nimetyt laturiprofiilit | — |
| `charging_sessions` | Latausistunnot (viittaus charger_profiles) | — |
| `app_battery_usage` | Sovelluskohtaiset akunkäyttötiedot | timestamp, package_name |
| `throttling_events` | Lämpötilan throttling-tapahtumat | timestamp |
| `speed_test_results` | Nopeustestien tulokset | timestamp |
| `devices` | Laiteprofiilitiedot (valmistaja, malli, kyvyt) | — |

### Tiedon säilytys

- **Vapaa taso:** Säilyttää vain 24 tunnin lukemat (poistetaan automaattisesti jokaisella kirjoituksella)
- **Pro-taso:** Konfiguroitava säilytys (3kk / 6kk / 1v / ikuisesti)
- Auto-migraatiot käytössä

---

## Taustapalvelut

| Palvelu | Tyyppi | Kuvaus |
|---------|--------|--------|
| **HealthMonitorWorker** | WorkManager (HiltWorker) | Kerää lukemat kaikista repositorioista ajoitetusti (oletus 30min, käyttäjän valittavissa 15/30/60min). Suorittaa vanhojen tietojen siivouksen. |
| **RealTimeMonitorService** | Foreground Service | Käynnissä vain kun käyttäjä aktiivisesti katsoo reaaliaikaista dataa. specialUse-tyyppinen etualanpalvelu. |
| **BootReceiver** | BroadcastReceiver | Varmistaa WorkManager-tehtävien jatkumisen laitteen uudelleenkäynnistyksen jälkeen. |
| **MonitorScheduler** | Apuluokka | Hallitsee WorkManager-tehtävien ajoitusta ja peruutusta. |
| **NotificationHelper** | Apuluokka | Luo ja hallitsee ilmoituskanavat ja hälytykset. |

---

## Widgetit

Sovellus sisältää kaksi Glance-pohjaista kotinäyttöwidgettiä, molemmat vaativat Pro-tilauksen:

| Widget | Kuvaus |
|--------|--------|
| **BatteryWidget** | Näyttää akun tason (%), lämpötilan (°C) ja virrankulutuksen (mA) |
| **HealthWidget** | Näyttää kokonaisterveyspisteemäärän ja akun tason mini-indikaattorina |

Molemmat näyttävät lukitun tilan ei-Pro-käyttäjille.

---

## Suunnittelujärjestelmä (Design System)

### Teema

Sovellus käyttää tällä hetkellä yhtä tummaa premium-teemaa. Muotokieli nojaa tummiin paneeleihin, kirkkaaseen teal/siniseen aksenttiin ja vahvaan korttihierarkiaan.

Typografia on päivitetty:
- **Manrope** käyttöliittymän pääfonttina
- **JetBrains Mono** valituissa numeerisissa mittareissa (esim. prosentit, virta, scoret)

### Väripaletti

| Rooli | Väri | Käyttö |

### Nykyinen UI-suunta

- Koti- ja Battery-näkymät on viimeistelty dashboard-henkisiksi, ei asetussivumaisiksi listoiksi
- Pohjanavigaatio on poistettu; liikkuminen tapahtuu kotinäkymästä korttien ja toimintorivien kautta
- Headerit käyttävät safe area -paddingia, jotta otsikot eivät jää statusbarin alle
- Battery-näkymässä free- ja pro-history-tilat käyttävät samaa paneelikieltä kuin muut kortit
|-------|------|--------|
| BgPage | #0B1E24 | Sivun tausta |
| BgCard | #133040 | Korttien tausta |
| BgCardAlt | #0F2A35 | Vaihtoehtoinen kortti / track-väri |
| BgIconCircle | #1A3A48 | Ikonien kehykset, erottimet, track-täytöt |
| AccentTeal | #5DE4C7 | Terve/aktiivinen/hyvä tila, ensisijainen aksentti |
| AccentBlue | #4A9EDE | Akku/lataus, informatiivinen |
| AccentOrange | #F5963A | Varoitus: lämmin lämpötila, välimuisti |
| AccentRed | #F06040 | Virhe: kuuma, kriittinen |
| AccentLime | #C8E636 | CTA-painikkeet AINOASTAAN — ei muuhun |
| AccentYellow | #F5D03A | PRO-merkit, toissijaiset korostukset |
| TextPrimary | #E8E8ED | Pääasiallinen teksti, otsikot, hero-numerot |
| TextSecondary | #90A8B0 | Alaotsikot, kuvaukset, toissijaiset arvot |
| TextMuted | #506068 | Pois käytöstä, paikkamerkit, osion otsikot |
| TextOnLime | #1A2E0A | Tumma teksti lime CTA-painikkeilla |

### Typografia

Järjestelmä Roboto (ei mukautettuja fontteja). Typografiaskaala:

| Rooli | Koko | Paino | Lisämääritys |
|-------|------|-------|-------------|
| Hero-numero | 48sp | Bold | letterSpacing -0.04em |
| Hero-yksikkö | 20sp | Regular | textSecondary-väri |
| Sivun otsikko | 20sp | SemiBold | — |
| Osion otsikko | 12sp | SemiBold | UPPERCASE, letterSpacing 0.08em, textMuted |
| Kortin otsikko | 16sp | SemiBold | — |
| Kortin alaotsikko | 13sp | Regular | textSecondary tai aksenttiväri |
| Leipäteksti | 14–15sp | Regular | — |
| Merkki (badge) | 10sp | SemiBold | — |

### Korttityylit

- Muoto: `RoundedCornerShape(16.dp)`
- Taustaväri: `BgCard` (#133040)
- Ei reunoja, ei varjoja, ei korkotasoa (elevation)
- Ei glassmorfismia, sumennusta tai läpinäkyvyyttä
- Hero-kortit: padding 24dp vaakasuunnassa, 28dp pystysuunnassa
- Normaalikortit: padding 20dp

### Komponenttikirjasto

Sovellus sisältää 25 jaettua komponenttia:

| Komponentti | Kuvaus |
|------------|--------|
| **ProgressRing** | Animoitu rengasmittari (Canvas). 1200ms ease-out. Konfiguroitava koko/viiva/väri. |
| **MiniBar** | Vaakasuuntainen edistymispalkki. 800ms ease-out. Pyöristetyt päät. |
| **GridCard** | Keskitetty kortti: ikonikehys → otsikko → aksenttialaotsikko. 2×2 ruudukkoon. |
| **ListRow** | Rivi: [ikoni] — [teksti] — [arvo] — [chevron]. Kortin sisälle. |
| **SectionHeader** | UPPERCASE muted-teksti osion otsikoille. |
| **IconCircle** | 44dp harmaa kehys keskitetyllä harmaalla ikonilla. |
| **StatusDot** | 8dp väripiste tilaindkaattoreille. |
| **ProBadgePill** | Keltainen PRO-merkki 12% läpinäkyvyydellä. |
| **PrimaryButton** | Lime CTA-painike, 56dp, 28dp pyöristys. |
| **SecondaryButton** | Outline-painike, 48dp, TextMuted-reuna. |
| **PrimaryTopBar** | Hamburger + otsikko + toimintopainikkeet. |
| **DetailTopBar** | Takaisin-nuoli + keskitetty otsikko. |
| **MetricTile** | Label-arvo-yksikkö-kortti metriikoille. |
| **ConfidenceBadge** | Luotettavuusmerkki: Tarkka / Arvioitu / Ei saatavilla. |
| **SpeedGauge** | Puolikaarimittari nopeustestille. |
| **HeatStrip** | Lämpötilavisualisointi värigradientilla. |
| **TrendChart** | Animoitu viivakaavio (Vico). |
| **AreaChart** | Täytetty aluekaaviov (Vico). |
| **SparklineChart** | Kompakti sparkline-kaavio kortteihin. |
| **AnimatedNumber** | Animoitu lukuarvo sujuvilla siirtymillä. |
| **StatusIndicator** | Väripiste + tekstiselite statukselle. |
| **PullToRefreshWrapper** | Material 3 pull-to-refresh PullToRefreshBox. |
| **ProFeatureCalloutCard** | Pro-ominaisuuden mainoskortti. |
| **ProFeatureLockedState** | Lukittu tila Pro-ominaisuuksille. |

### Animaatiot

- ProgressRing: 1200ms ease-out (`FastOutSlowInEasing`) nollasta tavoitteeseen
- MiniBar: 800ms ease-out nollasta tavoitteeseen
- Näkymäsiirtymät: slide + fade 300ms (Material 3 shared axis)
- Ei korttien sisääntulo-animaatioita
- Molemmat kunnioittavat `reducedMotion`-asetusta (välitön animaatio kun true)

---

## Laitetunnistus

`DeviceCapabilityManager` määrittää käynnistyksen yhteydessä, mitä dataa laite pystyy luotettavasti tuottamaan. Tulokset tallennetaan `DeviceProfile`-olioon ja Room-tietokantaan.

### Valmistajakohtainen käsittely

| Valmistaja | Erityiskäsittely |
|-----------|-----------------|
| **Google Pixel** | Luotettavin baseline, käytetään referenssinä |
| **Samsung** | Maksimiteoreettisen virran lukemien käsittely |
| **OnePlus** | SUPERVOOC-etumerkkikonventioiden käsittely |
| **API 34+** | Uudet `BATTERY_PROPERTY_CHARGING_CYCLE_COUNT` ja `STATE_OF_HEALTH` |
| **Muut** | `GenericBatterySource` luotettavuusvaroituksilla |

### Luotettavuusjärjestelmä

Jokainen mittausarvo voi saada luotettavuustason:
- **HIGH** (Tarkka) — Vihreä merkki, luotettava lukema
- **LOW** (Arvioitu) — Keltainen merkki, arvioitu arvo
- **UNAVAILABLE** (Ei saatavilla) — Harmaa merkki, ei dataa saatavilla

Sovellus ei koskaan näytä epätarkkaa dataa ilman varoitusta. Jos mittaus ei ole saatavilla, se piilotetaan tai näytetään "Ei tuettu tällä laitteella" -viesti.

---

## Monetisaatio

### Pro-malli

DevicePulse käyttää kertaostoa (arvioitu hinta ~€3.49) Google Play Billing -kirjaston kautta.

### Vapaan version ominaisuudet

- Kotinäkymän kokonaisyleiskatsaus
- Akkudetaljit (vain nykyinen tila, ei historiaa)
- Verkkodetaljit ja nopeustesti
- Lämpötilanseuranta (vain nykyinen, ei tapahtumahistoriaa)
- Tallennustilan käyttö
- Asetukset (ei vientiä)
- Taustamonitorointi

### Pro-ominaisuudet

| Ominaisuus | Kuvaus |
|-----------|--------|
| **Pidennetty historia** | Vapaan 24h → Pro 3kk/6kk/1v/ikuisesti |
| **Akkuhistoriakaaviot** | Trendi- ja aluekaaviolliset |
| **Lämpötilan throttling-loki** | Yksityiskohtainen tapahtumahistoria |
| **Laturivertailu** | Vertaile latureiden nopeutta ja tehokkuutta |
| **Sovellusten akunkäyttö** | Sovelluskohtainen akkukulutusanalyysi |
| **Kotinäyttöwidgetit** | Akku- ja terveyswidgetit |
| **CSV-vienti** | Kaiken datan vienti CSV-muodossa |

### Osto-ohjaus

- `ProPurchaseManager` — Käynnistää ostoprosessin ja päivittää ostotilan
- `ProStatusRepository` — Hallitsee ostotilan Flow:na
- `ProStatusCache` — Paikallinen välimuisti nopeaan Pro-tilan tarkistukseen

---

## Android-käyttöoikeudet

| Oikeus | Tarkoitus | Pakollinen |
|--------|----------|-----------|
| `ACCESS_NETWORK_STATE` | Verkon tilan seuranta | Kyllä |
| `ACCESS_WIFI_STATE` | WiFi-yhteyden tiedot | Kyllä |
| `FOREGROUND_SERVICE` | Reaaliaikaseuranta | Kyllä |
| `FOREGROUND_SERVICE_SPECIAL_USE` | specialUse-palvelutyyppi | Kyllä |
| `POST_NOTIFICATIONS` | Ilmoitukset (Android 13+) | Kyllä |
| `RECEIVE_BOOT_COMPLETED` | Taustatyöt uudelleenkäynnistyksen jälkeen | Kyllä |
| `INTERNET` | Latenssin mittaus ja nopeustesti | Valinnainen |
| `ACCESS_FINE_LOCATION` | WiFi SSID:n lukeminen (Android 8+) | Valinnainen |
| `PACKAGE_USAGE_STATS` | Sovelluskohtainen tallennustilajaottelu | Valinnainen |

---

## Lokalisointi

Sovellus tukee kahta kieltä:
- **Englanti** (en) — oletuskieli
- **Suomi** (fi) — täysi käännös

Merkkijonoresursseja on ~260–296 kappaletta kieliversiota kohden.

---

## Yksityisyys ja tietoturva

- **Ei analytiikkaa** — ei Firebase Analyticsia, ei kolmannen osapuolen seurantaa
- **Ei käyttäjätilejä** — ei rekisteröitymistä, ei kirjautumista
- **Ei pilvisynkronointia** — kaikki data pysyy laitteella
- **Ei mainoksia** — liiketoimintamalli perustuu kertaostoon
- **Verkkoliikenne** — vain latenssin mittaus, nopeustesti ja Google Play Billing

---

## Build ja julkaisu

- Yksittäinen `app`-moduuli (ei multi-moduulijakoa)
- ProGuard/R8 minifiointi ja resurssien kutistus release-buildissa
- Signing-konfiguraatio ympäristömuuttujista: `DEVICEPULSE_KEYSTORE_PATH`, `DEVICEPULSE_KEYSTORE_PASSWORD`, `DEVICEPULSE_KEY_ALIAS`, `DEVICEPULSE_KEY_PASSWORD`
- Versionumerointi: semver (1.0.0), automaattinen version code

### Build-huomiot

- AGP 9.1.0 sisäänrakennettu Kotlin on pois käytöstä (`android.builtInKotlin=false`) koska KSP 2.3.1 vaatii erillisen Kotlin-pluginin
- `android.disallowKotlinSourceSets=false` tarvitaan KSP:n generoimille lähteille AGP 9:llä
- `kotlin.compose`-plugin lisätään erikseen Compose-kääntäjälle
- Vico 3.x poisti `core`-moduulin (yhdistetty `views`-moduuliin); tarvitaan vain `compose` ja `compose-m3`
- `BatteryManager.BATTERY_PROPERTY_CHARGING_CYCLE_COUNT` ja `STATE_OF_HEALTH` eivät ole julkisessa SDK:ssa — käytetään raaka-integer-vakioita (8 ja 12)

---

## CI/CD

- GitHub Actions -työnkulku: `dependency-vulnerability-scan.yml` riippuvuuksien haavoittuvuustarkistukselle
- Riippuvuuslukitus (`dependencyLocking`) käytössä kaikille konfiguraatioille

---

*Tämä dokumentti kuvaa DevicePulse-projektin tilaa maaliskuussa 2026.*
