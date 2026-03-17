# Battery & Thermal Enhancements Spec

Uudet mittarit ja parannukset olemassa oleviin näkymiin. Perustuu Battery Guru -sovelluksen analyysiin.

---

## 1. mAh jäljellä hero-kortissa

### Mitä
Näytetään arvioitu jäljellä oleva kapasiteetti milliampeeritunteina akkuprosentin rinnalla.

### Miltä näyttää
```
60 %
Discharging · ~2700 mAh remaining
```
- Sama rivi kuin nykyinen "Discharging"-teksti, laajennettu
- Tyyli: `bodyMedium`, `TextSecondary`
- `~` etuliite koska arvio (kapasiteetti × taso / 100)

### Sijainti
`BatteryDetailScreen.kt` → `BatteryHeroCard` (ja `HomeScreen.kt` → `BatteryHeroCard`)

### Toteutus
- Akun suunnittelukapasiteetti: `BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER` (µAh) tai fallback laiteprofiilin perusteella
- Laskenta: `chargeCounter / 1000` = mAh jäljellä
- Jos `CHARGE_COUNTER` ei saatavilla → ei näytetä mAh-arvoa
- Uusi kenttä `BatteryState`: `remainingMah: Int?`
- `GenericBatterySource`: luetaan `BATTERY_PROPERTY_CHARGE_COUNTER`

### Merkkijonot
```xml
<string name="battery_remaining_mah">%1$s · ~%2$d mAh remaining</string>
<string name="battery_remaining_mah">%1$s · ~%2$d mAh jäljellä</string>
```

---

## 2. W + mV virta-arvon alla

### Mitä
Näytetään teho watteina ja jännite millivoltteina virta-arvon (mA) alla kompaktina rivinä.

### Miltä näyttää
```
Current
-219  mA                    [Accurate]
0.9 W · 4125 mV
```
- Uusi rivi ison mA-luvun alla
- Tyyli: `bodySmall`, `onSurfaceVariant`
- Muoto: `X.X W · XXXX mV`

### Sijainti
`BatteryDetailScreen.kt` → Charging/Battery Current -osio, mA-näytön ja confidence badgen alapuolelle.

### Toteutus
- Arvot ovat jo olemassa: `battery.voltageMv`, `powerW` (lasketaan jo `remember`-lohkossa)
- Pelkkä UI-muutos: lisätään `Text`-composable mA-rivin alle
- Poistetaan power ja voltage erillisistä MetricPill-esityksistä jos ne siirretään tähän

### Merkkijonot
```xml
<string name="battery_power_voltage">%1$s W · %2$d mV</string>
```

---

## 3. Virran avg/min/max

### Mitä
Näytetään virran tilastot (keskiarvo, minimi, maksimi) nykyisen lataus- tai purkaussession aikana.

### Miltä näyttää
```
[  Average   ] [  Minimum  ] [  Maximum  ]
   -185 mA       -33 mA       -423 mA
```
- Kolme `MetricPill`-komponenttia rivissä, `Modifier.weight(1f)` kullakin
- Nollautuu kun lataus/purkaus -tila vaihtuu

### Sijainti
`BatteryDetailScreen.kt` → Charging/Battery Current -osio, Status/Type -pillien jälkeen.

### Toteutus
- Uusi data class:
  ```kotlin
  data class CurrentStats(
      val avg: Int,
      val min: Int,
      val max: Int,
      val sampleCount: Int
  )
  ```
- `BatteryViewModel`: kerää statistiikkaa `currentMa`-flowsta
  - Pidetään `sum`, `count`, `min`, `max` muuttujina
  - Nollautuu kun `chargingStatus` muuttuu
- Ei tarvita tietokantaa — vain muistissa nykyisen session ajan

### Merkkijonot
```xml
<string name="battery_current_average">Average</string>
<string name="battery_current_minimum">Minimum</string>
<string name="battery_current_maximum">Maximum</string>
```
```xml
<string name="battery_current_average">Keskiarvo</string>
<string name="battery_current_minimum">Minimi</string>
<string name="battery_current_maximum">Maksimi</string>
```

---

## 4. Lämpötilan min/max sessionissa

### Mitä
Näytetään lämpötilan vaihteluväli nykyisen session (viimeisimmän irrotuksen jälkeen) aikana.

### Miltä näyttää
```
29.8 °C
Cool
↓ 27.3°C · ↑ 35.3°C
```
- Uusi rivi hero-kortin alaosassa, "Cool"-tekstin alla
- Tyyli: `bodySmall`, `onSurfaceVariant`
- `↓` väri: `statusColorForTemperature(minTemp)` (yleensä vihreä)
- `↑` väri: `statusColorForTemperature(maxTemp)` (vaihtelee)

### Sijainti
`ThermalDetailScreen.kt` → `ThermalHeroCard`, bandLabel-tekstin alla.

### Toteutus
- `ThermalViewModel`: seuraa min/max arvoja
  - `private var sessionMinTemp` ja `sessionMaxTemp`
  - Päivitetään joka kerta kun uusi lämpötila saadaan
  - Nollautuu sovelluksen käynnistyessä (ei tietokantaa)
- Uudet kentät `ThermalUiState.Success`: `sessionMinTemp: Float?`, `sessionMaxTemp: Float?`
- Näytetään vain kun molemmat ovat saatavilla ja eroavat toisistaan

### Merkkijonot
```xml
<string name="thermal_range">↓ %1$s · ↑ %2$s</string>
```

---

## 5. Screen On / Screen Off -erottelu

### Mitä
Erillinen kulutustieto näytön ollessa päällä vs. pois päältä nykyisen purkaussession aikana.

### Miltä näyttää
```
BATTERY USAGE

Screen On                 Screen Off
4.6 %/h                   2.1 %/h
~587 mA · 12 min          ~174 mA · 23 min
```
- Kaksi `MetricPill`-korttia vierekkäin (`weight(1f)`)
- Ensisijainen arvo: purkausnopeus %/h
- Toissijainen: arvioitu virta + aika

### Sijainti
`BatteryDetailScreen.kt` → uusi osio Drain Rate -osion tilalle tai sen laajennuksena. Näytetään vain purkautuessa.

### Toteutus

**Screen state -seuranta:**
- `ScreenStateTracker` (uusi luokka `service/monitor/`-kansioon)
- Rekisteröi `BroadcastReceiver`: `ACTION_SCREEN_ON`, `ACTION_SCREEN_OFF`
- Seuraa: screen on -aika, screen off -aika, akkutaso kullakin hetkellä
- Data class:
  ```kotlin
  data class ScreenUsageStats(
      val screenOnDuration: Long,    // ms
      val screenOffDuration: Long,   // ms
      val screenOnDrainPct: Float,   // % kulutettu näyttö päällä
      val screenOffDrainPct: Float,  // % kulutettu näyttö pois
      val screenOnDrainRate: Float,  // %/h
      val screenOffDrainRate: Float  // %/h
  )
  ```
- Nollautuu kun laturista irrotetaan (`ACTION_POWER_DISCONNECTED`)

**ViewModel-integraatio:**
- `BatteryViewModel` saa `ScreenStateTracker`-injektion Hiltin kautta
- Uusi kenttä `BatteryUiState.Success`: `screenUsage: ScreenUsageStats?`

**Huom:** Tämä on isompi muutos kuin muut — vaatii uuden luokan ja broadcastin.

### Merkkijonot
```xml
<string name="battery_screen_on">Screen On</string>
<string name="battery_screen_off">Screen Off</string>
<string name="battery_usage_section">Battery Usage</string>
```
```xml
<string name="battery_screen_on">Näyttö päällä</string>
<string name="battery_screen_off">Näyttö pois</string>
<string name="battery_usage_section">Akun käyttö</string>
```

---

## 6. "Since unplug" historianäkymä

### Mitä
Uusi aikaväli-vaihtoehto akkuhistoriakaavioon: näyttää datan viimeisimmästä laturin irrotuksesta lähtien.

### Miltä näyttää
```
[Since unplug] [Day] [Week] [Month] [All]
```
- Uusi `FilterChip` olemassa olevan rivin alkuun
- Sama `TrendChart`-komponentti, eri aikaväli

### Sijainti
`BatteryDetailScreen.kt` → History-osion FilterChip-rivi.

### Toteutus
- Uusi arvo `HistoryPeriod`-enumiin: `SINCE_UNPLUG`
- `BatteryRepository`/`GetBatteryTrendUseCase`: tunnistaa viimeisimmän lataustapahtuman (status = CHARGING → NOT_CHARGING siirtymä) ja hakee datan siitä eteenpäin
- Logiikka: etsi tietokannasta viimeisin rivi jossa `status = "CHARGING"`, käytä sen aikaleimaa alkupisteenä
- Jos ei löydy lataustapahtumaa → näytetään kaikki saatavilla oleva data

### Merkkijonot
```xml
<string name="history_period_since_unplug">Since unplug</string>
```
```xml
<string name="history_period_since_unplug">Irrotuksesta</string>
```

---

## 7. Akun kapasiteetti mAh:na

### Mitä
Näytetään akun todellinen kapasiteetti suhteessa suunnittelukapasiteettiin. Konkreettisempi kuin pelkkä "Good".

### Miltä näyttää
```
Battery Health
Good
4149 / 4500 mAh
```
- Nykyisen "Good"-tekstin alle uusi rivi
- Tyyli: `bodySmall`, `onSurfaceVariant`, numericFontFamily
- Tai vaihtoehtoisesti MetricPill: `Health` → `92% · 4149/4500 mAh`

### Sijainti
`BatteryDetailScreen.kt` → Battery Info -osion Health-rivi.

### Toteutus
- Suunnittelukapasiteetti: `PowerProfile`-luokan `battery.capacity` (mAh) — Android sisäinen API, luetaan reflektiolla
  ```kotlin
  val powerProfile = Class.forName("com.android.internal.os.PowerProfile")
      .getConstructor(Context::class.java)
      .newInstance(context)
  val capacity = powerProfile.javaClass
      .getMethod("getBatteryCapacity")
      .invoke(powerProfile) as Double
  ```
- Todellinen kapasiteetti: `suunnittelukapasiteetti × healthPercent / 100`
- Tai jos `CHARGE_COUNTER` on saatavilla 100% tasolla → käytä sitä
- Uudet kentät `BatteryState`: `designCapacityMah: Int?`, `estimatedCapacityMah: Int?`

### Merkkijonot
```xml
<string name="battery_capacity_mah">%1$d / %2$d mAh</string>
```

---

## 8. Deep Sleep vs Held Awake

### Mitä
Näytetään kuinka paljon aikaa puhelin viettää syvässä unessa vs. hereillä näytön ollessa pois päältä. Kertoo käyttäjälle onko jokin sovellus estämässä puhelinta nukkumasta.

### Miltä näyttää
```
SLEEP ANALYSIS

[  Deep Sleep  ] [  Held Awake  ]
    3m 11s           28m 17s
  4 mAh (0.1%)    87 mAh (2.6%)
```
- Kaksi MetricPill-korttia vierekkäin
- Ensisijainen arvo: kesto
- Toissijainen: mAh ja prosenttiosuus

### Sijainti
`BatteryDetailScreen.kt` → uusi osio Screen On/Off -osion (#5) jälkeen. Näytetään vain purkautuessa.

### Toteutus
- Vaatii `WakeLock`-tietojen lukemista: `PowerManager` + `UsageStatsManager`
- `ACTION_SCREEN_OFF` + `PowerManager.isDeviceIdleMode` erottaa deep sleep vs held awake
- Tämä on käyttöjärjestelmätason tieto joka vaatii `PACKAGE_USAGE_STATS` -luvan (käyttäjä myöntää asetuksista)
- Vaihtoehtoinen yksinkertaisempi toteutus: seuraa vain `isInteractive`-tilaa ilman WakeLock-erittelyä

### Merkkijonot
```xml
<string name="battery_deep_sleep">Deep Sleep</string>
<string name="battery_held_awake">Held Awake</string>
<string name="battery_sleep_analysis">Sleep Analysis</string>
```
```xml
<string name="battery_deep_sleep">Syvä uni</string>
<string name="battery_held_awake">Hereillä</string>
<string name="battery_sleep_analysis">Unitilat</string>
```

---

## 9. Pitkän ajan tilastot

### Mitä
Yhteenveto akkukäytöstä pidemmältä aikaväliltä: montako prosenttia ladattu/purettu, montako latauskertaa, keskimääräinen kulutus.

### Miltä näyttää
```
STATISTICS (last 10 days)

[Charged] [Discharged] [Sessions]
  680%       657%         45

Average Usage
Screen On     Screen Off
~10.3%/h      ~3.3%/h

Full Charge Estimate
Screen On     Screen Off
9h 56m        1d 12h
```
- Ylärivillä kolme MetricPill-korttia
- Alla kaksi osiota kahdella kortilla kummassakin

### Sijainti
`BatteryDetailScreen.kt` → uusi osio History-kaavion jälkeen. Pro-feature.

### Toteutus
- Data on jo Room-tietokannassa (battery readings taulut)
- Uusi `GetBatteryStatisticsUseCase`:
  - Query: laske summa charged/discharged prosenteista
  - Tunnista lataussessiot (status-siirtymät)
  - Laske keskimääräiset purkausnopeudet screen on/off -tiloissa (vaatii #5 screen state dataa)
- Full charge estimate = 100% / keskimääräinen purkausnopeus

### Merkkijonot
```xml
<string name="battery_stats_section">Statistics</string>
<string name="battery_stats_charged">Charged</string>
<string name="battery_stats_discharged">Discharged</string>
<string name="battery_stats_sessions">Sessions</string>
<string name="battery_stats_avg_usage">Average Usage</string>
<string name="battery_stats_full_charge_est">Full Charge Estimate</string>
<string name="battery_stats_last_n_days">Last %1$d days</string>
```
```xml
<string name="battery_stats_section">Tilastot</string>
<string name="battery_stats_charged">Ladattu</string>
<string name="battery_stats_discharged">Purettu</string>
<string name="battery_stats_sessions">Sessiot</string>
<string name="battery_stats_avg_usage">Keskimääräinen käyttö</string>
<string name="battery_stats_full_charge_est">Täyden latauksen arvio</string>
<string name="battery_stats_last_n_days">Viimeiset %1$d päivää</string>
```

---

## 10. Virran reaaliaikakaavio lyhyillä aikaväleillä

### Mitä
Virran (mA) reaaliaikainen kaavio jossa valittavissa lyhyet aikavälit: 1m, 10m, 1h, 6h. Nykyinen sessiokaavio näyttää vain koko session.

### Miltä näyttää
```
SESSION GRAPH

[Current] [Power]

[kaavio]

[1m] [10m] [1h] [6h]
```
- Samat FilterChip-valinnat kuin nyt (Current/Power)
- Uusi FilterChip-rivi aikaväleille kaavion alla
- Oletusvalinta: 10m

### Sijainti
`BatteryDetailScreen.kt` → olemassa oleva Session Graph -osio. Laajennetaan FilterChipillä.

### Toteutus
- Virrasta kerätään jo dataa session aikana (muistissa oleva lista)
- Lisätään aikavälisuodatus: näytetään vain viimeiset N minuuttia
- Ei tietokantamuutoksia — in-memory ringbuffer riittää
- `BatteryViewModel`: uusi `sessionChartPeriod` state
- Suodatus: `sessionReadings.filter { it.timestamp > now - periodMs }`

### Merkkijonot
```xml
<string name="chart_period_1m">1m</string>
<string name="chart_period_10m">10m</string>
<string name="chart_period_1h">1h</string>
<string name="chart_period_6h">6h</string>
```

---

## 11. Lataus/purkaus -erillinen historia

### Mitä
Erilliset tilastot ja kaaviot lataus- ja purkaustiloille. Battery Gurussa tämä on Charging/Discharging -tabien kautta.

### Miltä näyttää
Virta-arvon yläpuolella:
```
[Charging] [Discharging]
```
- FilterChip-pari joka suodattaa kaikki session-tiedot ja kaaviot valitun tilan mukaan

### Sijainti
`BatteryDetailScreen.kt` → Charging/Battery Current -osion yläosaan.

### Toteutus
- UI-tason suodatus: näytetään eri data riippuen valinnasta
- Charging-näkymä: viimeisimmän lataussession data
- Discharging-näkymä: viimeisimmän purkaussession data
- Tilastot (avg/min/max) pidetään erikseen kummallekin tilalle
- `BatteryViewModel`: `selectedCurrentTab: ChargingStatus`

### Merkkijonot
Käytetään olemassa olevia `battery_status_charging` / `battery_status_discharging` merkkijonoja.

---

## Toteutusjärjestys (päivitetty)

| Vaihe | Feature | Työmäärä | Riippuvuudet |
|-------|---------|----------|-------------|
| 1 | W + mV virta-arvon alla (#2) | Pieni | Ei |
| 2 | Lämpötilan min/max (#4) | Pieni | Ei |
| 3 | Virran avg/min/max (#3) | Pieni | Ei |
| 4 | mAh jäljellä (#1) | Keskitaso | CHARGE_COUNTER |
| 5 | Akun kapasiteetti mAh (#7) | Keskitaso | PowerProfile-reflektio |
| 6 | Since unplug (#6) | Keskitaso | HistoryPeriod-laajennus |
| 7 | Virtakaavion aikavälit (#10) | Keskitaso | Ringbuffer |
| 8 | Lataus/purkaus-tabit (#11) | Keskitaso | ViewModel state |
| 9 | Screen On/Off (#5) | Iso | BroadcastReceiver |
| 10 | Deep sleep / Held awake (#8) | Iso | PowerManager, luvat |
| 11 | Pitkän ajan tilastot (#9) | Iso | UseCase, riippuu #5 |

Vaiheet 1–3 yhdessä sessionissa. Vaiheet 4–8 kukin erikseen. Vaiheet 9–11 ovat isoja ja riippuvat toisistaan.

| Vaihe | Feature | Työmäärä | Riippuvuudet |
|-------|---------|----------|-------------|
| 1 | W + mV virta-arvon alla (#2) | Pieni | Ei — pelkkä UI |
| 2 | Lämpötilan min/max (#4) | Pieni | Ei — ViewModel-laajennus |
| 3 | Virran avg/min/max (#3) | Pieni | Ei — ViewModel-laajennus |
| 4 | mAh jäljellä (#1) | Keskitaso | CHARGE_COUNTER lukeminen |
| 5 | Since unplug (#6) | Keskitaso | HistoryPeriod-laajennus |
| 6 | Screen On/Off (#5) | Iso | ScreenStateTracker, BroadcastReceiver |

Vaiheet 1–3 voi toteuttaa yhdessä sessionissa. Vaihe 4 erikseen. Vaiheet 5–6 ovat isompia ja voi tehdä myöhemmin.
