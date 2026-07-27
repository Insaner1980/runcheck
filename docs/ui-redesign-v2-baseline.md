# UI-uudistus v2 — vaihe 0 baseline

Tämä dokumentti lukitsee `codex/ui-uudistus-v2`-haaran lähtötilan ennen
tuotantokoodimuutoksia. Lähdekoodi on ensisijainen totuus; kuvakaappaukset
todentavat näkyvän oireen.

## Git- ja compile-lähtötila

- Lähtöcommit: `88d2609d9b6962309c7b5b9379037f61682285cf`
  (`Korjaa viimeisen katselmoinnin löydökset`).
- Haara: `codex/ui-uudistus-v2`.
- Ennen vaiheen muutoksia `git status --short --branch` tulosti vain
  `## codex/ui-uudistus-v2`.
- Komento:
  `.\gradlew.bat :app:compileDebugKotlin --no-parallel --max-workers=1 --console=plain`
- Tulos: `BUILD SUCCESSFUL in 5s`; 8 taskia olivat ajan tasalla.
- Lähtötilassa ei ollut nykyisen pää-checkoutin muutoksia.

## Kuvakaappausjuuret

- `N/` = `C:\Users\emmah\Desktop\runcheck-screenshots\runcheck-new`
- `L/` = `C:\Users\emmah\Desktop\runcheck-screenshots\runcheck-lisäkuvia`
- `V/` = `C:\Users\emmah\Desktop\runcheck-screenshots\runcheck-vielä-lisäkuvia`

## D1–D12

| ID | Lähdekoodiomistaja lähtötilassa | Toistoreitti | Teema | Pro/free-tila | Kuvakaappaustodiste | Korjauksen omistava vaihe |
|---|---|---|---|---|---|---|
| D1 | `HomeScreen.kt:278-401`: `HomeHealthHero` jakaa tilan ringin ja `HealthHeroSummary`n kesken; confidence-rivi ei varaa badge-tekstille omaa turvallista leveyttä. | Home, ensimmäinen viewport. | Dark + Light | Riippumaton Prosta. | `N/Screenshot_20260727-171314.png`, `N/Screenshot_20260727-171533.png` | 5 |
| D2 | `HomeScreen.kt:218-259` renderöi aina `InsightsCard`in; `home/insights/InsightsCard.kt:27-62` säilyttää otsikko- ja empty-state-rakenteen myös ilman insightteja. | Home ilman aktiivisia insightteja. | Dark + Light | Riippumaton Prosta. | `N/Screenshot_20260727-171314.png`, `N/Screenshot_20260727-171533.png` | 5 |
| D3 | `Color.kt:26-31` käyttää vaaleita `#F4F7F8` / `#F0F4F5` / `#E9EFF1` -pintoja; `Theme.kt:48-59` lisää borderin vain erikseen outline-komponentille, ei kaikille korteille. | Home, Insights, Tools tai Settings vaalealla teemalla. | Light | Riippumaton Prosta. | `N/Screenshot_20260727-171445.png`, `N/Screenshot_20260727-171452.png`, `N/Screenshot_20260727-171500.png`, `N/Screenshot_20260727-171518.png`, `N/Screenshot_20260727-171533.png` | 3, 6–8 |
| D4 | `SpeedTestScreen.kt:310-512`: `SpeedTestHero` piirtää ringin sisään `Brush.radialGradient`-ympyrän riveillä 465–475. | Tools → Speed Test, idle. | Dark + Light | Riippumaton Prosta. | `N/Screenshot_20260727-171341.png`, `N/Screenshot_20260727-171349.png`, `N/Screenshot_20260727-171508.png` | 6 |
| D5 | `HomeSecondarySections.kt:39-188`: kaikki neljä domainia käyttävät samaa `GridCard`-rakennetta ja `surfaceContainerHighest`-ikonitaustaa; vain pieni ikonitintti vaihtelee. | Home, 2×2 domain-grid. | Dark + Light | Riippumaton Prosta. | `N/Screenshot_20260727-171314.png`, `N/Screenshot_20260727-171533.png` | 3, 5 |
| D6 | `HomeScreen.kt:338-361` käyttää 148 dp `RuncheckWavyProgress`ia; `ExpressiveComponents.kt:495-535` delegoi ohuen expressive-indikaattorin oletusgeometriaan ilman 18 dp hero-strokea tai tickejä. | Home, Health Score -hero. | Dark + Light | Riippumaton Prosta. | `N/Screenshot_20260727-171314.png`, `N/Screenshot_20260727-171533.png` | 3, 5 |
| D7 | `InsightsScreen.kt:89-139` näyttää nollatuloksella koko näkymän `ExpressiveEmptyState`n; aktiivisen insight-repositoryn ulkopuolista hyötysisältöä ei ole. | Insights, 0 active insights. | Dark + Light | Molemmat; näkyvä tyhjätila ei vaadi Prota. | `N/Screenshot_20260727-171322.png`, `N/Screenshot_20260727-171518.png` | 7 |
| D8 | `TrendChart.kt:284-297` lisää dataan ja tickeihin kaikkien quality zonejen min/max-arvot ennen viewportin laskentaa. Tekstit mitataan jo riveillä 348–371, joten juurisyy on koko domainin pakottava skaalaus ja pystysuuntainen pakkautuminen. | Home → Battery → History → Level tai Temp → 24h. | Dark (aineisto) | Trial/Pro history. | `V/Screenshot_20260727-180801.png`, `V/Screenshot_20260727-180809.png`; vertailu ilman zoneja: `V/Screenshot_20260727-180816.png` | 1 |
| D9 | `HistoryPeriodFilterChipRow.kt:8-21` delegoi geneeriseen `EnumFilterChipRow`iin; `ExpressiveComponents.kt:70-89,156-200` käyttää yli neljälle valinnalle 104 dp segmented-optioneita vaakavierityksessä, mutta ei tuo valittua itemiä näkyviin, lisää fade-affordanssia tai pidä labelia yksirivisenä. Battery käyttää lisäksi samaa geneeristä riviä suoraan `BatteryDetailScreen.kt:1031-1045`. | Battery/Network/Thermal/Storage → History; valitse viides tai myöhempi periodi 411 dp leveydellä, font scale 2.0. | Dark (aineisto) | Trial/Pro history. | `L/Screenshot_20260727-173100.png`, `L/Screenshot_20260727-173141.png`, `V/Screenshot_20260727-180801.png`, `V/Screenshot_20260727-180809.png`, `V/Screenshot_20260727-180816.png`, `V/Screenshot_20260727-180824.png`, `V/Screenshot_20260727-180832.png` | 1 |
| D10 | `TrendChart.kt:143-148,309-336` tarjoaa 200 dp ulkomitan, mutta embedded-plot vähentää paddingit ja akselit sekä käyttää 2 dp viivaa; `BatteryDetailScreen.kt:1107-1131` ei anna suurempaa full-bleed-minimikorkeutta. | Home → Battery → History → Level/Temp/Current/Voltage. | Dark (aineisto) | Trial/Pro history. | `V/Screenshot_20260727-180801.png`, `V/Screenshot_20260727-180809.png`, `V/Screenshot_20260727-180816.png`, `V/Screenshot_20260727-180824.png` | 1, 3 |
| D11 | `BatteryDetailScreen.kt:1136-1242`: locked ja insufficient käyttävät samaa `BatteryHistoryPreviewPlaceholder`ia; placeholder lisää aina `ProBadgePill`in, joten Pro-käyttäjän insufficient-tila näyttää samalla Pro-lukitulta. | Home → Battery → History → Voltage, valitse periodi jolla on alle 2 datapistettä. | Dark (aineisto) | Trial/Pro insufficient; sama placeholder myös Free locked -tilassa. | `L/Screenshot_20260727-173100.png`, `V/Screenshot_20260727-180832.png` | 1 |
| D12 | `BatteryDetailScreen.kt:1066-1118` välittää metric-kohtaiset quality zonet; `TrendChart.kt:121-139,382-384,663-677` muodostaa viivan värit zoneista, kun taas zoneton Current käyttää teeman primarya. Väri kuvaa siten metric/status-yhdistelmää eikä Battery-domainia. | Home → Battery → History; vaihda Level → Temp → Current → Voltage. | Dark (aineisto) | Trial/Pro history. | `V/Screenshot_20260727-180801.png`, `V/Screenshot_20260727-180809.png`, `V/Screenshot_20260727-180816.png`, `V/Screenshot_20260727-180824.png` | 3, 5–8 |

## Koko kuvakaappausinventaario

Kaikki briefin kolmen hakemiston kuvat tarkistettiin. D-taulukko yllä nimeää
suorat regressiotodisteet; muut kuvat täydentävät ruutu- ja reittibaselinea.

- `N/`: `Screenshot_20260727-171314.png`,
  `Screenshot_20260727-171322.png`, `Screenshot_20260727-171330.png`,
  `Screenshot_20260727-171341.png`, `Screenshot_20260727-171349.png`,
  `Screenshot_20260727-171353.png`, `Screenshot_20260727-171403.png`,
  `Screenshot_20260727-171411.png`, `Screenshot_20260727-171423.png`,
  `Screenshot_20260727-171432.png`, `Screenshot_20260727-171445.png`,
  `Screenshot_20260727-171452.png`, `Screenshot_20260727-171500.png`,
  `Screenshot_20260727-171508.png`, `Screenshot_20260727-171518.png`,
  `Screenshot_20260727-171533.png`.
- `L/`: `Screenshot_20260727-173028.png`,
  `Screenshot_20260727-173042.png`, `Screenshot_20260727-173050.png`,
  `Screenshot_20260727-173100.png`, `Screenshot_20260727-173105.png`,
  `Screenshot_20260727-173114.png`, `Screenshot_20260727-173124.png`,
  `Screenshot_20260727-173134.png`, `Screenshot_20260727-173141.png`.
- `V/`: `Screenshot_20260727-180801.png`,
  `Screenshot_20260727-180809.png`, `Screenshot_20260727-180816.png`,
  `Screenshot_20260727-180824.png`, `Screenshot_20260727-180832.png`.

## TDD RED -sopimus

Vaihe 0 lisää seuraavat puhtaat wished-for-API-testit:

- `ChartViewportTest`: quality zonet eivät saa laajentaa kapeaa data-viewportia.
- `ChartStatePrecedenceTest`: `loading > error > locked > insufficient > data`;
  vain yksi päätila valitaan.
- `HistoryPeriodSelectorPolicyTest`: viides periodi tuodaan näkyviin 411 dp
  leveydellä ja font scale 2.0:ssa, TalkBackille välitetään valinta ja positio,
  reduced motion käyttää animaatiotonta scrollia eikä labelia katkaista.

RED-komento:

```powershell
.\gradlew.bat :app:testDebugUnitTest `
  --tests "com.runcheck.ui.chart.ChartViewportTest" `
  --tests "com.runcheck.ui.chart.ChartStatePrecedenceTest" `
  --tests "com.runcheck.ui.chart.HistoryPeriodSelectorPolicyTest" `
  --no-parallel --max-workers=1 --console=plain
```

Odotettu ja havaittu RED: `:app:compileDebugUnitTestKotlin FAILED`, koska
`calculateChartViewport`, `ChartPrimaryState` /
`resolveChartPrimaryState` ja `historyPeriodSelectorPolicy` eivät vielä ole
olemassa. Tämä on vaihe 0:n tarkoituksellinen checkpoint, ei typo eikä
tuotantokoodin käännösvirhe. Testeissä ei käytetä `@Ignore`a. Vaiheen 1
ensimmäinen portti on toteuttaa nämä presentation-API:t ja saada samat testit
vihreiksi.
