# runcheckin täydellinen Material 3 Expressive -uudistus

## Yhteenveto

runcheck uudistetaan visuaalisesti ja rakenteellisesti koko sovelluksen laajuisesti. Lopputulos käyttää Material 3 Expressive -komponentteja, selvästi nykyisestä poikkeavaa valoisaa ilmettä, System/Light/Dark-teemavalintaa ja neljän päätason navigaatiota: Home, Insights, Tools ja Settings.

Uudistus säilyttää nykyisen Clean Architecture -rakenteen, mittausten luotettavuusmallin, Pro-portit, NDT7-nopeustestin, englanninkielisyyden ja debug-only Sentry -rajauksen. Muutoksia ei toteuteta dokumentin oletusten perusteella silloin, kun nykyinen koodi osoittaa toisenlaisen toiminnan.

Toteutus tehdään erillisessä `codex/ui-redesign-m3-expressive`-worktreessä, jotta nykyisen laajasti muokatun `main`-checkoutin muutokset säilyvät koskemattomina.

## Vahvistetut korjaukset lähtödokumenttiin

- Projektin nykyinen Compose BOM on `2026.06.01`, ei dokumentissa mainittu `2026.03.00`. Nykyinen BOM säilytetään ja vain `androidx.compose.material3:material3` ylikirjoitetaan versioon `1.5.0-alpha24`.
- `MaterialExpressiveTheme`, expressive motion scheme ja uudet expressive-komponentit ovat edelleen kokeellisia 1.5.0-alpha24-versiossa. Käyttö keskitetään runcheckin omiin komponentteihin ja opt-in asetetaan build-konfiguraatiossa. [Material 3 -julkaisut](https://developer.android.com/jetpack/androidx/releases/compose-material3), [Compose BOM](https://developer.android.com/develop/ui/compose/bom).
- Teema-asetusta ei lueta DataStoresta synkronisesti UI-säikeellä eikä sille rakenneta rinnakkaista SharedPreferences-välimuistia. Splash pidetään näkyvissä ensimmäiseen DataStore-arvoon saakka.
- Charger-toiminnossa ei ole manuaalista “Start session” -toimintoa. Sitä ei lisätä vain FAB-valikkoa varten. Käytetään yhtä Extended FAB -painiketta “Add charger”; FAB menu otetaan käyttöön vasta, jos koodissa on vähintään kaksi todellista ensisijaista toimintoa.
- Split buttonia ei käytetä cleanup-riveillä ilman aitoa vaihtoehtoista toimintoa. Nykyiset cleanup-tyypit saavat tavallisen toimintopainikkeen.
- App Usage osaa jo hakea sovellusten nimet. Korjaus kohdistuu vain labelin puuttuessa näkyvään raakaa package-nimeä parempaan fallbackiin.
- Nykyisestä datasta ei voida laskea luotettavaa sovelluskohtaista mAh-kulutusta eikä akun viikoittaista ladattua energiaa. Raportti käyttää foreground-aikaa ja arvioitua akun varaustason muutosta prosenttiyksikköinä.
- WorkManagerin ajoitus ei ole tarkka seinäkellonaika. Viikkoraportti toteutetaan kalenteri- ja DST-tietoisena uniikkien one-time-töiden ketjuna, ei väitetysti täsmällisenä periodic-ajona. [WorkManagerin ajoitusmalli](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).
- Android-ilmoitukselle ei rakenneta omaa vaaleaa/tummaa RemoteViews-ulkoasua. Järjestelmä vastaa ilmoituksen teemasta; sovellus määrittää vain kanavan, tekstin, monochrome-ikonin ja avautuvan reitin.
- Glance pidetään vakaassa `1.1.1`-versiossa. Expressive Material 3 -alpha ei edellytä Glance-alphan käyttöönottoa. [Glance-julkaisut](https://developer.android.com/jetpack/androidx/releases/glance).

## Julkiset rajapinnat ja tietomallit

### Teema

Lisätään yksi domain-tason teema-arvo:

```kotlin
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
```

- `UserPreferences` saa `themeMode`-kentän ja repository `setThemeMode()`-operaation.
- Tuntematon tai poistunut tallennettu arvo palautuu deterministisesti `SYSTEM`-tilaan.
- `RuncheckTheme(themeMode)` valitsee vaalean tai tumman color schemen ja käyttää `MaterialExpressiveTheme`-juurta sekä `MotionScheme.expressive()`-asetusta.
- MainActivity pitää splashin näkyvissä, kunnes ensimmäinen teema-arvo on saatavilla. Prosessin aikana ei näytetä väärän teeman välähdystä.

### Navigointi

Lisätään:

- `TopLevelDestination.Home`
- `TopLevelDestination.Insights`
- `TopLevelDestination.Tools`
- `TopLevelDestination.Settings`
- `Screen.Tools`
- `Screen.WeeklyReport`
- `Screen.Export`

App Usage -ruudun “Usage / Not used” on ruudun sisäinen tila eikä erillinen navigointireitti.

Kylmäkäynnistyksen parent-mäppäys:

- Battery, Network, Thermal, Storage ja Fullscreen Chart → Home.
- Speed Test, Cleanup, Charger Comparison, App Usage, Weekly Report, Learn, Learn Article ja Export → Tools.
- Pro Upgrade → Settings.
- Sovelluksen sisältä avattu globaali detail säilyttää todellisen kutsuvan ruudun back stackissa.

Päätason vaihto käyttää `saveState`, `restoreState`, `launchSingleTop` ja start destinationiin kohdistuvaa `popUpTo`-mallia. Vaihto palauttaa päätason oman selaus- ja navigointitilan; nykyisen välilehden uudelleenvalinta palauttaa sen juureen. Detail-ruuduilla alapalkki piilotetaan. Järjestelmän Back palaa detailistä parentiin, päätasolta Homeen ja Homesta ulos. Toteutus seuraa Androidin virallista usean back stackin mallia. [Useat back stackit](https://developer.android.com/guide/navigation/backstack/multi-back-stacks), [NavigationBar](https://developer.android.com/develop/ui/compose/components/navigation-bar).

### Viikkoraportti

Uudet domain-tyypit:

- `WeeklyReportPeriod(startInclusive, endExclusive, zoneId)`
- `WeeklyReport`
- `WeeklyReportCoverage`
- `WeeklyReportMetric<T>`
- `WeeklyReportAvailability`
- `GenerateWeeklyReportUseCase`

Raportti ei lisää Room-taulua. Se muodostetaan nykyisistä Room-historioista valmiiksi päättyneelle viikolle.

DataStoreen lisätään:

- `weeklyReportEnabled`, oletus `false`
- `weeklyReportLastProcessedPeriodStart`
- `weeklyReportLastProcessedPeriodEnd`

### Käyttämättömät sovellukset

Uudet domain-tyypit:

- `UnusedAppsPeriod` arvoilla 30, 60 ja 90 päivää
- `UnusedAppCandidate`
- `UnusedAppsResult`, joka sisältää käyttöoikeuden, havaintoikkunan ja mahdolliset osittaiset virheet
- `GetUnusedAppsUseCase`

Sovellusikoneita tai Androidin `Drawable`-olioita ei tuoda domain-malliin. UI käyttää yhteistä sovellusikonikomponenttia package-nimen perusteella.

## Design-järjestelmä

### Värit ja teemat

Tumma teema säilyttää runcheckin identiteetin mutta mapataan Material-rooleihin:

- `background #0B1E24`
- `surfaceContainerLow #0F2A35`
- `surfaceContainer #133040`
- `surfaceContainerHigh #1A3A48`
- `primary #4A9EDE`
- `secondary #5DE4C7`
- `onSurface #F4F7F8`
- `onSurfaceVariant #B5C7CE`

Vaalea teema:

- `background #F4F7F8`
- `surface #FFFFFF`
- `surfaceContainerLow #F0F4F5`
- `surfaceContainer #E9EFF1`
- `surfaceContainerHigh #DEE7EA`
- `surfaceContainerHighest #D4E0E4`
- `primary #246A9F`
- `secondary #006B5A`
- `tertiary #795F00`
- `error #B3261E`
- `onSurface #16262C`
- `onSurfaceVariant #4E6570`
- `outline #647A83`
- `outlineVariant #C0CDD1`

Statusvärit erotetaan varsinaisesta Material-schemestä:

- Healthy `#006B57`
- Fair `#795F00`
- Poor `#9C4E00`
- Critical `#B3261E`
- Neutral `#4E6570`
- Unavailable `#647A83`

Tekstille käytetään aina erikseen määritettyä foreground/container-paria, ei pelkkää statusvärin alpha-versiota. Jos kontrastitesti hylkää parin, foregroundiksi valitaan deterministisesti joko `#0B1E24` tai `#FFFFFF` sen mukaan, kumpi tuottaa suuremman kontrastin.

Karttojen ja kaavioiden värit keskitetään `ChartColors`-rakenteeseen: grid, axis, line, fill, selected point ja glow. Vaalea teema käyttää hillittyä täyttöä ja poistaa voimakkaan hehkun.

### Muodot, typografia ja liike

- Tavallinen kortti 16 dp.
- Hero-kortti ja suuret sheetit 28 dp.
- Pienet elementit 8 dp.
- Navigointi-indikaattori ja badge-elementit pill-muotoisia.
- Ei varjoja eikä elevationia; rajauksia käytetään vain nykyisen ActionCard-säännön mukaisesti.
- Manrope säilyy tekstifonttina ja JetBrains Mono numero-, mittaus- ja kaavioarvoissa.
- Hero-numeroille lisätään selvästi nykyistä vahvempi expressive-typografiahierarkia.
- Generic-komponenttien liike tulee `MaterialTheme.motionScheme`-arvoista. Nykyiset `MotionTokens` säilyvät anturi- ja kaavioanimaatioiden lähteenä.
- Reduced motion pysäyttää toistuvat aaltoliikkeet, korvaa shape-morphit välittömällä tilanvaihdolla ja lyhentää navigaatiotransitiot.
- Status ei koskaan välity pelkällä värillä.

### Yhteiset expressive-komponentit

Rakennetaan ja käytetään koko sovelluksessa:

- `ExpressiveDetailScaffold`
- `ExpressiveSingleChoiceSelector<T>`
- `RuncheckActionCard`
- `InfoBanner`
- `StatusPill`
- `SectionHeader`
- `LearnTopicLink`
- `ExpressiveEmptyState`
- `RuncheckLoadingIndicator`
- `RuncheckWavyProgress`
- `AppDisplayName`
- `ChartTheme`

Kokeelliset M3E-kutsut pidetään näiden komponenttien sisällä. Ruudut eivät rakenna omia väripalettaja, connected button groupeja tai expressive-opt-in-ratkaisuja.

## Informaatioarkkitehtuuri ja ruudut

### App shell

- Root-tasolle tulee edge-to-edge `Scaffold` ja neljän kohteen `NavigationBar`.
- Järjestys on Home, Insights, Tools, Settings.
- Insights näyttää badge-indikaattorin, kun Roomissa on uusia näkyviä insight-rivejä.
- Bottom bar käyttää lyhyitä englanninkielisiä label-arvoja ja Outline-ikoneita.
- Alapalkki näkyy vain neljällä päätason ruudulla.
- NavigationBarin korkeus, gesture-insetit ja kontrasti varmistetaan sekä vaaleassa että tummassa teemassa.

### Home

Ensimmäinen viewport muuttuu selvästi:

1. Kevyt top bar: runcheck-logo/nimi ja mahdollinen monitoring-status; Settings-kuvake poistuu.
2. Stale monitoring -banneri vain silloin, kun data on oikeasti vanhentunut.
3. Suuri Health hero:
   - kokonaispiste
   - determinate wavy ring
   - sanallinen status
   - mittauksen ajankohta ja luottamus
   - ei neljän kategorian toistuvaa breakdown-listaa
4. 2×2-kategoriaruudukko: Battery, Network, Thermal, Storage.
5. Yksi tärkein aktiivinen insight ja linkki Insightsiin.
6. Vain aktiivisen trialin ohut banneri.

Home-sivulta poistetaan erilliset charger-, quick tools-, Pro status- ja moninkertaiset insight-kortit. Niiden toiminnot siirtyvät Tools- tai Settings-tasolle.

### Insights

- Ei back-painiketta.
- Yläosassa otsikko, aktiivisten insightien määrä ja suodatus.
- Insight-rivit säilyttävät nykyiset näkyvyys- ja Pro-säännöt.
- Rivit käyttävät kompaktia statuspintaa, selkeää otsikkoa, aikaleimaa ja yhtä toimintoa.
- Luettu-/nähty-tila ohjaa NavigationBar-badgea.
- Top-level Home Insights -kortti pysyy ilmaisena.

### Tools

Tools muodostaa keskitetyn toimintohubin:

1. Hallitseva Speed Test -ActionCard ja “Run speed test” -CTA.
2. Bento-ruudukko:
   - Storage cleanup, nykyinen Pro-portti säilyttäen
   - Charger comparison, Pro
   - App usage / Not used, Pro
   - Weekly report, Pro
3. Toissijaiset listatoiminnot:
   - Learn
   - Export data, Pro
4. Pro-lukitut kohteet ovat näkyviä, käyttävät samaa lock-indikaattoria ja avaavat standardin Pro-locked-tilan.

### Settings

Uusi järjestys:

- Display: System/Light/Dark.
- Monitoring: automaattinen monitorointi ja intervallit.
- Notifications: master toggle, alert-asetukset, live notification ja Weekly report.
- Data: retention ja Export data -navigointi.
- Widgets.
- Pro: trial/purchase/restore.
- About ja debug-osiot nykyisten build-rajojen mukaan.

ThemeMode-valinta vaikuttaa välittömästi ilman Activityn uudelleenkäynnistystä. Weekly report -toggle näkyy myös Free-käyttäjälle, mutta on lukittu ja selittää Pro-vaatimuksen.

### Detail-ruutujen yhteinen rakenne

Battery, Network, Thermal ja Storage siirtyvät rakenteeseen:

1. `LargeFlexibleTopAppBar` ja nested scroll.
2. Hero `surfaceContainerLow`-pinnalla.
3. Yksi ensisijainen toimintopinta.
4. Mittausryhmät tavallisilla `surfaceContainer`-pinnoilla.
5. Connected button group metriikka- ja periodivalinnoille.
6. Korkeintaan yksi ratkaistu `InfoBanner`.
7. Yksi aihekohtainen Learn-linkki.
8. Mahdolliset Pro-historiat nykyisillä porteilla.

InfoBanner valitaan deterministisesti: ensin vakavin nykyiseen mittaustilaan liittyvä tieto, sitten catalog-järjestys. Banneri avautuu paikoillaan, voidaan sulkea ja käyttää nykyisiä dismissal-ID-arvoja.

### Ruutukohtaiset muutokset

- Battery: health/current hero, charging/session/history omiin osioihinsa; Charger Comparison näkyy vain sekundaarisena linkkinä.
- Network: yhteystiedot, signaali ja historia ryhmitellään; Speed Test on ainoa hallitseva CTA; tap-to-copy säilyy.
- Thermal: current status hero ennen selityksiä; chartit, throttling ja historia seuraavat; info ei peitä ensimmäistä viewportia.
- Storage: kokonaiskäyttö hero, kategoriat ja cleanup-CTA; cache-arvo selitetään read-only-mittaukseksi.
- Speed Test: aloituspainike morffaa ympyrästä pyöristetyksi stop-tilaksi testin aikana; vaiheissa käytetään expressive LoadingIndicatoria; cellular-varoitus ja NDT7-logiikka eivät muutu.
- Cleanup: scan/results/selection-tilat selkeytetään; monivalinnassa käytetään alareunan `HorizontalFloatingToolbar`-toimintoja. MediaStore-delete request säilyy ainoana poistomekanismina.
- Charger Comparison: tyhjä tila saa illustration-tyylisen Outline-ikoniryhmän ja inline “Add charger” -painikkeen. Profiilien kanssa käytetään yhtä Extended FABia; automaattiseen session trackingiin ei lisätä manuaalista käynnistystä.
- App Usage: connected valinta “Usage / Not used”; package-nimi näkyy vain teknisenä lisätietona, ei ensisijaisena nimenä.
- Weekly Report: period hero, coverage, battery, storage, thermal, speed test ja app usage -bento-osiot.
- Learn: aihetason suodatus Battery/Network/Thermal/Storage/Privacy; detail-ruutujen Learn-linkit avaavat valmiiksi suodatetun listan.
- Export: oma Pro-gated-ruutu ja ViewModel; nykyinen export use case säilyy yhtenä totuuden lähteenä.
- Pro Upgrade: visuaalinen uudistus, mutta purchase/restore/trial-käytös ei muutu.
- Fullscreen Chart: säilyy kompaktina data-alueena ilman suurta flexible app baria; valitsimet vaihtuvat yhteiseen connected-komponenttiin.

## Viikkoraportin toteutus

- Raporttijakso on edellinen paikallinen maanantai 00.00 – seuraava maanantai 00.00, loppuhetki exclusive.
- Ilmoitus suunnitellaan maanantaille noin klo 09 laitteen nykyisessä aikavyöhykkeessä.
- `WeeklyReportScheduler` on erillinen nykyisestä `MonitorScheduler`-järjestelmästä.
- Uniikki work name on `weekly_report`.
- Scheduler laskee seuraavan paikallisen maanantain ja luo one-time-työn. Worker ajastaa seuraavan työn vain onnistuneen tai lopullisesti ohitetun suorituksen jälkeen; retry ei luo rinnakkaista ketjua.
- Aikataulu varmistetaan sovelluksen käynnistyessä, boot/package replace -tapahtumissa, aikavyöhykkeen vaihtuessa sekä toggle- ja Pro-tilan muuttuessa.
- Weekly toggle pois päältä peruuttaa uniikin työn.
- Trial lasketaan nykyisen `ProState.isPro`-käytännön mukaisesti Proksi.
- Jos Pro päättyy, käyttäjän valinta säilyy mutta ilmoituksia ei lähetetä; ostamisen jälkeen aikataulu voi jatkua.
- Master notifications pois, POST_NOTIFICATIONS estetty tai kanava estetty: periodi merkitään käsitellyksi eikä vanhaa raporttia lähetetä myöhemmin catch-upina.
- Uusi `CHANNEL_REPORTS` ei käytä hälytyskanavan kiireellisyystasoa.
- Ilmoitus avaa `weekly_report`-reitin Tools-parentin päälle.

Raportin sisältö:

- Battery: keskimääräinen purkautumisnopeus ja arvioidut positiiviset/negatiiviset varaustason muutokset vain kelvollisten vierekkäisten näytteiden perusteella.
- Battery health: kapasiteetti-healthin muutos vain, jos `healthPct` on saatavilla.
- Storage: ensimmäisen ja viimeisen mittauksen erotus.
- Thermal: tallennettujen thermal/throttling-tapahtumien määrä ja vakavin tila.
- Speed: käyttäjän käynnistämien testien määrä sekä mediaani download/upload/latency.
- Apps: foreground-ajaltaan suurimmat sovellukset; ei mAh-väitteitä.
- Coverage: valvottujen päivien määrä, näytemäärä ja unavailable/estimated-tila.

Free-käyttäjälle ruutu näyttää ProFeatureLockedState-preview’n ja nykytilan esimerkkirakenteen, mutta ei lue eikä paljasta seitsemän päivän aggregaatteja.

## “Not used” -sovelluslista

- Lähtöjoukko haetaan launcher-aktiviteeteista `MAIN + LAUNCHER`.
- Mukaan otetaan vain käyttäjän asentamat, ei-system/updated-system-sovellukset.
- runcheck itse, system appit, package managerin näkymättömät sovellukset ja tarkastelujakson jälkeen asennetut sovellukset suljetaan pois.
- `QUERY_ALL_PACKAGES`-oikeutta ei lisätä; nykyistä package visibility -queries-ratkaisua käytetään. [Package visibility](https://developer.android.com/training/package-visibility).
- UsageStats kertoo viimeisen tallennetun käytön. Puuttuva tapahtuma esitetään tekstinä “No recorded use in 30/60/90 days”, ei varmana väitteenä käyttämättömyydestä.
- Koko haetaan `StorageStatsManager.queryStatsForPackage()`-kutsulla IO-dispatcherilla ja virheet käsitellään sovelluskohtaisesti. Koko saa olla unavailable. [StorageStatsManager](https://developer.android.com/reference/android/app/usage/StorageStatsManager).
- Haku suoritetaan rajatulla rinnakkaisuudella ja tulokset välimuistitetaan yhden refreshin ajaksi.
- Uninstall käyttää `ACTION_DELETE package:` -intentiä ja järjestelmän vahvistusta.
- Ruudun tulokset päivitetään takaisin palattaessa.
- Usage permissionin puuttuessa näytetään olemassa oleva käyttöoikeusohjaus, ei tyhjää “unused”-listaa.
- Ominaisuus pysyy App Usage -Pro-portin sisällä.

## Widgetit

- Nykyiset Battery- ja Health-widgetit mapataan samaan day/night-värijärjestelmään Glancen `ColorProvider`-arvoilla.
- Lisätään Quick Glance 4×2:
  - health score
  - battery
  - free storage
  - temperature
- Jokainen solu avaa oikean reitin: Home, Battery, Storage tai Thermal.
- Quick Glance käyttää nykyisen Health-widgetin snapshot-lähteitä laajennettuna storage- ja thermal-arvoilla; rinnakkaista laskentalogiikkaa ei rakenneta.
- Widgetin teema seuraa launcher-/järjestelmäteemaa, ei sovelluksen manuaalista ThemeMode-valintaa.
- Toteutetaan Pro-locked, loading, empty, stale ja fresh -tilat.
- Päivitetään receiverit, appwidget-info XML:t, previewt, `RuncheckWidgets.updateAll()` ja widget deep-linkit.
- Minimikoot, ellipsis ja sisältö testataan useilla launcher-kokoluokilla.

## Cleanupin rehellisyys ja sisältö

- Kaikki cache-, RAM-, booster-, cleaner- ja “speed up” -väitteet auditoidaan strings-resursseista, Learn-sisällöstä ja sovelluksen omista metadata-/listing-teksteistä.
- UI kertoo, että runcheck voi mitata cache-tilaa mutta ei tyhjentää muiden sovellusten cachea.
- Tarjotaan linkki Androidin storage-/App info -asetuksiin.
- Lisätään Learn-artikkeli siitä, miksi cleaner-sovellus ei voi turvallisesti tyhjentää muiden sovellusten cacheja.
- Storage-ruudun cache-info linkittää artikkeliin.
- Nykyiset cleanup-kategoriat ja MediaStore-poistot säilyvät ennallaan.
- Mitään Accessibility Service-, root-, hidden API- tai muuta kiertotietä ei lisätä.

## Toteutusvaiheet

### 1. Turvallinen lähtötilanne

- Luo erillinen worktree ja haara nykyisestä sovitusta commit-pisteestä.
- Tallenna lähtötilan reitti-, Pro-portti-, screenshot- ja komponenttimatriisi.
- Varmista nykyiset theme-, navigation-, preferences-, widget- ja notification-kutsujat ennen siirtoja.
- Älä kopioi nykyisen dirty checkoutin irrallisia muutoksia automaattisesti worktreehen.

### 2. Dependency- ja teemaperusta

- Lisää Material 3 alpha24 -ylikirjoitus, dependency verification -metatiedot ja keskitetty experimental opt-in.
- Toteuta ThemeMode/DataStore/splash-ketju.
- Rakenna light/dark color schemes, status- ja chart-tokenit, shapes, typography ja motion integration.
- Muunna previews valon ja pimeän matriisiksi.
- Poista korvatut väri- ja korttivakiot samassa vaiheessa.

### 3. App shell ja navigointi

- Lisää Tools sekä uudet route-tyypit.
- Rakenna root Scaffold, NavigationBar, saved top-level state ja detail-bar visibility.
- Siirrä Home Settings -toiminto varsinaiseen Settings-välilehteen.
- Toteuta deep-link-parentit, Pro-uudelleenohjaus ja notification-route.
- Säilytä fullscreen chartin SavedStateHandle-tulospolku.

### 4. Yhteiset expressive-komponentit ja Home

- Toteuta yhteiset scaffold-, selector-, banner-, status-, empty-state-, loading- ja chart-komponentit.
- Uudista Home kokonaan.
- Siirrä työkalut Tools-sivulle ja Pro-hallinta Settingsiin.
- Poista vanhat komponentit vasta, kun kaikki kutsujat on siirretty.

### 5. Detail- ja tukiruudut

- Uudista Battery, Network, Thermal ja Storage yhteisellä rakenteella.
- Uudista Speed Test, Cleanup, Charger Comparison, App Usage, Learn, Export, Pro Upgrade ja Fullscreen Chart.
- Korvaa toistuvat FilterChip-rivit ja InfoCard-pinot yhteisillä komponenteilla.
- Varmista, ettei ruuduilla ole rinnakkaisia väri-, padding-, motion- tai CTA-toteutuksia.

### 6. Viikkoraportti ja käyttämättömät sovellukset

- Lisää domain-mallit, repository-rajapinnat ja use caset.
- Lisää puuttuvat periodikohtaiset DAO-kyselyt, erityisesti speed test -historialle.
- Toteuta scheduler, worker, notification channel, asetukset ja raporttiruutu.
- Toteuta installed-app/usage/storage-yhdistely ja App Usage -ruudun uusi tila.
- Roomin versiota ei nosteta, ellei toteutuksen aikana löydy pakottavaa pysyvän datan tarvetta.

### 7. Widgetit, sisältö ja dokumentaatio

- Lisää Quick Glance ja uudista nykyiset widgetit.
- Tee cleanup-väiteauditointi ja Learn-artikkeli.
- Päivitä `AGENTS.md` ja `CODEX.md` samassa muutoksessa.
- Päivitä `PROJECT.md`: navigointi, teemat, viikkoraportti, workerit, widgetit ja Pro-ominaisuudet.
- Päivitä `UI-SPEC.md` vasta toteutuneen koodin pohjalta; suunnitelmatekstiä ei kopioida sinne tulevaisuuden totuutena.
- Olematonta repositoryn `memory/MEMORY.md`-tiedostoa ei luoda. Globaalia Codex-muistia muutetaan vain erillisellä nimenomaisella pyynnöllä.
- Commit-viestit kirjoitetaan suomeksi.

## Testisuunnitelma

### Teema ja design system

- ThemeMode DataStore round-trip ja tuntemattoman arvon SYSTEM-fallback.
- System-teeman reagointi runtime day/night -muutokseen.
- Ei light/dark-välähdystä kylmäkäynnistyksessä.
- WCAG-kontrastit jokaiselle tekstille, statukselle, containerille ja disabled-tilalle molemmissa teemoissa.
- Kaaviot, statusbadge-elementit ja hero-pinnat molemmissa teemoissa.
- Reduced motion: ei jatkuvaa aalto-/glow-liikettä eikä tarpeetonta shape-morphia.
- 48 dp touch target ja outline-icon-sääntö.

### Navigointi

- Jokaisen tabin state restore ja scroll restore.
- Tab → detail → tab switch → restore.
- Current-tab reselect → kyseisen tabin juuri.
- Detail Back → oikea parent.
- Settings/Insights/Tools Back → Home → exit.
- Bottom bar näkyy vain top-level-ruuduilla.
- Kaikki cold-start deep linkit ja notification route.
- Pro-route ennen ja jälkeen Pro-staten valmistumisen.
- Fullscreen chart -argumentit ja paluutulos.

### Viikkoraportti

- Jakson rajat tavallisena viikkona, DST-siirtymässä ja aikavyöhykkeen vaihdossa.
- Ensimmäinen ajoitus, uudelleenajoitus, boot, package replace ja toggle off.
- Duplicate work- ja duplicate notification -esto.
- Pro, trial, expired trial, master notifications, POST_NOTIFICATIONS ja blocked channel.
- Nolla dataa, yksi näyte, osittainen viikko ja täysi viikko.
- Akun kasvavat/laskevat segmentit ja liian suuret näytevälit.
- Ei mAh- tai app battery drain -väitteitä.
- Notification tap avaa oikean raportin.

### Käyttämättömät sovellukset

- 30/60/90 päivän rajat.
- System-, updated-system-, self- ja liian uusi app suljetaan pois.
- Usage permission puuttuu.
- Ei UsageStats-riviä.
- Package label puuttuu.
- StorageStats permission/security/IO-virhe.
- Poiston peruutus, onnistuminen ja lifecycle-refresh.
- Ei `QUERY_ALL_PACKAGES`-oikeutta.

### Widgetit ja sisältö

- Day/night, Pro/free, fresh/stale/empty.
- Battery/Health/Quick Glance eri widget-koot.
- Jokaisen Quick Glance -solun deep link.
- Widget previewt ja receiver-rekisteröinnit.
- Automaattinen testi tai grep varmistaa, ettei käyttäjäteksteihin jää väitteitä muiden sovellusten cachen tyhjentämisestä.

### Visuaalinen ja saavutettavuustarkastus

- Compact phone, landscape ja vähintään 600 dp leveys.
- Font scale 1.0, 1.3 ja 2.0.
- Gesture navigation ja 3-button navigation.
- TalkBack-järjestys, chart-kuvaukset ja status ilman väririippuvuutta.
- Home ensimmäinen viewport, kaikki päätasot ja jokainen detail hero molemmissa teemoissa.
- Loading-, error-, unavailable-, locked-, empty- ja stale-tilat.
- Lopputuloksen on oltava ensisilmäyksellä selvästi eri näköinen kuin nykyinen tumma korttipino; pelkkä tokenien hienosäätö ei täytä hyväksymiskriteeriä.

### Matalan CPU-kuorman verifiointi

- Jokaisessa vaiheessa vain kyseisen alueen compile tai rajatut unit/UI-testit.
- Lopussa `:app:compileDebugKotlin`, rajatut unit-testit, `assembleDebug` ja tarvittavat navigation/widget-instrumentaatiot.
- Compose stability -baseline päivitetään vain, jos julkiset composable-signatuurit todella muuttuvat ja uusi raportti todistaa muutoksen.
- Raskaita `lc`, `sc`, Sonar-, Dependency-Check- tai full verification -ajoja ei käynnistetä automaattisesti.
- Julkaisukandidaatissa käyttäjä ajaa `lc`; security-kokonaisuus ajetaan vain erillisestä pyynnöstä.

## Hyväksymiskriteerit ja oletukset

- Kaikki nykyiset käyttäjäpolut ovat edelleen saavutettavissa.
- Home, Insights, Tools ja Settings ovat neljä yhtäläistä päätason kohdetta.
- System/Light/Dark toimii ilman dynaamisia värejä.
- English-only, min SDK 26, one-time Pro, NDT7 ja debug-only Sentry säilyvät.
- Pro-portteja ei heikennetä: Charger Comparison, App Usage/Not used, Weekly Report, Cleanupin nykyinen Pro-polku, Extended History, Export ja Widgets tarkistavat Pro-tilan myös domain-tasolla.
- UI ei ohita use case- tai repository-kerroksia.
- Uusia verkkokutsuja, telemetriaa tai Android-oikeuksia ei lisätä.
- Käyttämättömyys, raporttiarviot ja cache-rajat kuvataan käyttäjälle täsmällisesti.
- Material 3 -alpha on tietoinen, rajattu poikkeus; muu Compose-pino pysyy nykyisessä vakaassa BOMissa.
- Uuden expressive-version vaihto myöhemmin edellyttää kaikkien projektin expressive-wrapperien compile- ja UI-testien läpäisyä.
