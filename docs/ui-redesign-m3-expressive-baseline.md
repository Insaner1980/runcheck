# Material 3 Expressive - turvallinen UI-lahtotilanne

> Lahtotila: `acc802d923a394742b3be985e456e4473885f2f7` (`codex/ui-redesign-m3-expressive`).
> Tämä on koodista johdettu suunnittelun turvaverkko, ei tavoitetilan
> spesifikaatio. Kaikki polut ovat repository-juuresta ja viittaavat kyseisen
> commitin HEADiin.

## Rajaus ja tapa lukea matriiseja

- Navigation Compose -graafissa ei ole sisakkaisia `navigation {}`-graafeja:
  alla oleva **parent** tarkoittaa nykyista sisääntulokohtaa ja suunniteltua
  back-stack-suhdetta, ei NavGraphin omaa parent-destinaatiota.
- `Screen.directRoutes` on ainoa sovelluksen koodissa oleva ulkoisen
  reittiargumentin sallintalista. `MainActivity` lukee vain
  `NotificationHelper.EXTRA_NAVIGATE_TO`-extran ja validoi sen tällä listalla
  ([Screen.kt](../app/src/main/java/com/runcheck/ui/navigation/Screen.kt#L55),
  [MainActivity.kt](../app/src/main/java/com/runcheck/MainActivity.kt#L82)).
  Siksi taulukon "ulkoisesti avattava" ei tarkoita Android URI deep linkia.
- Kaikkien destinationien back-kutsu on `popBackStack()`, ellei rivillä
  mainita poikkeusta. `navigateSingleTop` asettaa vain `launchSingleTop`; se ei
  aseta yleista `popUpTo`-sääntöä
  ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L377)).

## 1. Reittimatriisi

| Ruutu / `Screen` | Route | Nykyiset sisääntulot ja parent | Back / ulkoinen avaus |
|---|---|---|---|
| Home | `home` | `NavHost.startDestination`; sovelluksen launcher avaa `MainActivity`n, joka luo `RuncheckNavHost`in ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L64), [MainActivity.kt](../app/src/main/java/com/runcheck/MainActivity.kt#L39)). | Juurikohde; `home` on direct-route. Ilmoitusreitti tyhjentaa stackin aloituskohteeseen ennen navigointia ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L382)). |
| Insights | `insights` | Home `onNavigateToInsights`; parent on Home (tai nykyinen stack, jos se avataan ulkoisesti) ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L127)). | `popBackStack`; direct-route. |
| Battery Detail | `battery` | Home ja Insights; Home navigoi suoraan, Chargerille mennessa `navigateNested(battery, charger)`. Lisäksi nelja Learn-artikkelia cross-linkkaa tahan (alla) ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L110), [NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L144)). | `popBackStack`; direct-route. Vastaanottaa Fullscreen Chart -tuloksen `SavedStateHandle`lla ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L160)). |
| Network Detail | `network` | Home ja Insights; Speed Test -sisääntulo rakentaa `network -> speed_test` -stackin. Lisäksi kolme Learn-artikkelia cross-linkkaa tahan (alla) ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L111), [NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L146)). | `popBackStack`; direct-route. Vastaanottaa Fullscreen Chart -valinnan ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L190)). |
| Speed Test | `speed_test` | Homesta `navigateNested(network, speed_test)`; Network Detailista suora single-top ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L120), [NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L201)). | `popBackStack`; direct-route. Jakaa Network ViewModelin, jos `network`-entry on stackissa; muuten luo oman ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L279)). |
| Thermal Detail | `thermal` | Home ja Insights; kolme Learn-artikkelia cross-linkkaa tahan (alla) ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L112), [NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L147)). | `popBackStack`; direct-route. |
| Storage Detail | `storage` | Home ja Insights; kaksi Learn-artikkelia cross-linkkaa tahan (alla) ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L113), [NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L148)). | `popBackStack`; direct-route. |
| Cleanup | `cleanup/{type}` | Vain Storage Detailin `onNavigateToCleanup`, joka antaa `CleanupType.name`-argumentin ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L227), [Screen.kt](../app/src/main/java/com/runcheck/ui/navigation/Screen.kt#L28)). | `popBackStack`; **ei** direct-route. `type` on `NavType.StringType`; CleanupViewModel palauttaa tuntemattoman arvon `LARGE_FILES`-oletukseen ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L239), [CleanupViewModel.kt](../app/src/main/java/com/runcheck/ui/storage/cleanup/CleanupViewModel.kt#L50)). |
| Charger Comparison | `charger` | Home ja Insights rakentavat Battery-parentin, Battery Detail avaa suoraan ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L115), [NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L169)). | `popBackStack`; direct-route. Upgrade korvaa `charger`-entryn `pro_upgrade`lla (`popUpTo(..., inclusive = true)`) ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L245)). |
| App Usage | `app_usage` | Home ja Insights ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L126), [NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L151)). | `popBackStack`; direct-route. Upgrade korvaa `app_usage`-entryn `pro_upgrade`lla ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L258)). |
| Settings | `settings` | Home; yksi Learn-artikkeli cross-linkkaa tahan (alla) ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L128)). | `popBackStack`; direct-route. |
| Pro Upgrade | `pro_upgrade` | Home, Insights, Battery, Network, Thermal, Storage, Charger ja App Usage välittävät upgrade-callbackin ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L124), [NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L295)). | `popBackStack`; direct-route. |
| Learn | `learn` | Home ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L129)). | `popBackStack`; direct-route. |
| Learn Article | `learn/{articleId}` | Home, Battery, Network, Thermal, Storage, Settings ja Learn; argumentti on `NavType.StringType` ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L130), [NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L306)). | `popBackStack`; **ei** direct-route. Catalog validoi cross-linkin `Screen.isValidLearnCrossLinkRoute`-tarkistuksella, joka palauttaa direct-route-tarkistuksen ([Screen.kt](../app/src/main/java/com/runcheck/ui/navigation/Screen.kt#L81), [LearnArticleCatalog.kt](../app/src/main/java/com/runcheck/ui/learn/LearnArticleCatalog.kt#L166)). |
| Fullscreen Chart | `fullscreen_chart/{source}/{metric}/{period}` | Battery ja Network antavat kaikki kolme argumenttia ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L173), [NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L203)). | `popBackStack`; **ei** direct-route. Palauttaa valinnan edelliselle entrylle `SavedStateHandle`lla ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L365)). |

### Learn Article -cross-linkit

`LearnArticleDetailScreen` renderoi cross-linkin vain, kun catalogin
`crossLinkRoute` ei ole null, ja välittää sen NavGraphin
`onNavigateToRoute = { route -> navController.navigateSingleTop(route) }`
-callbackille ([LearnArticleDetailScreen.kt](../app/src/main/java/com/runcheck/ui/learn/LearnArticleDetailScreen.kt#L76),
[NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L312)).
Siksi jokaisessa seuraavassa rivissä parent on nykyinen `learn/{articleId}`
-entry: cross-link lisää kohdereitin sen päälle, ja kohderuudun `popBackStack`
palauttaa artikkeliin. `navigateSingleTop` estää vain jo ylimpänä olevan saman
reitin kaksoiskohteen ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L377)).

| Kohderuutu | Artikkelien `crossLinkRoute`-sisääntulot | Parent / back |
|---|---|---|
| Battery Detail (`battery`) | `BATTERY_HEALTH`, `BATTERY_DRAIN`, `BATTERY_CHARGING`, `BATTERY_CURRENT_POWER` ([LearnArticleCatalog.kt](../app/src/main/java/com/runcheck/ui/learn/LearnArticleCatalog.kt#L19)). | Parent: kyseinen Learn Article; back: Battery Detail -> kyseinen artikkeli. |
| Thermal Detail (`thermal`) | `THERMAL_NORMAL_TEMPS`, `THERMAL_THROTTLING`, `THERMAL_FEEDBACK` ([LearnArticleCatalog.kt](../app/src/main/java/com/runcheck/ui/learn/LearnArticleCatalog.kt#L55)). | Parent: kyseinen Learn Article; back: Thermal Detail -> kyseinen artikkeli. |
| Network Detail (`network`) | `NETWORK_SIGNAL`, `NETWORK_WIFI_BANDS`, `NETWORK_SPEED_TESTS` ([LearnArticleCatalog.kt](../app/src/main/java/com/runcheck/ui/learn/LearnArticleCatalog.kt#L82)). | Parent: kyseinen Learn Article; back: Network Detail -> kyseinen artikkeli. |
| Storage Detail (`storage`) | `STORAGE_SLOWDOWN`, `STORAGE_BREAKDOWN` ([LearnArticleCatalog.kt](../app/src/main/java/com/runcheck/ui/learn/LearnArticleCatalog.kt#L109)). | Parent: kyseinen Learn Article; back: Storage Detail -> kyseinen artikkeli. |
| Settings (`settings`) | `BACKGROUND_MONITORING` ([LearnArticleCatalog.kt](../app/src/main/java/com/runcheck/ui/learn/LearnArticleCatalog.kt#L145)). | Parent: kyseinen Learn Article; back: Settings -> kyseinen artikkeli. |

`Screen` ei maarittele URI-deep-linkkia, ja `NavGraph.kt`n destinationit eivät
maarittele `deepLinks = ...` -argumenttia. Nykyinen sovellustason
"deep-link" on siis vain ilmoituksen Intent-extra, jota `NotificationHelper`
tuottaa Battery-, Thermal-, Storage- ja Pro Upgrade -kohteille
([NotificationHelper.kt](../app/src/main/java/com/runcheck/service/monitor/NotificationHelper.kt#L39),
[NotificationHelper.kt](../app/src/main/java/com/runcheck/service/monitor/NotificationHelper.kt#L143)).

## 2. Pro-porttimatriisi

`ProStatusProvider` on domain-sopimus (`isProUser: Flow<Boolean>`,
`isPro(): Boolean`) ([ProStatusProvider.kt](../app/src/main/java/com/runcheck/domain/repository/ProStatusProvider.kt#L5)).
Alla "repository" tarkoittaa nimenomaan repository-sopimusta tai sen
käyttöreittiä; se ei väitä, että jokaiseen repository-metodiin olisi lisätty
erillinen portti.

| Ominaisuus | UI-portti | ViewModel / use case -portti | Domain / repository -taso |
|---|---|---|---|
| Charger Comparison | Home-työkalu lukitaan ja ohjaa upgradeen; destinationin `ChargerComparisonScreen` näyttää `ProFeatureLockedState`n ([HomeSecondarySections.kt](../app/src/main/java/com/runcheck/ui/home/HomeSecondarySections.kt#L214), [ChargerComparisonScreen.kt](../app/src/main/java/com/runcheck/ui/charger/ChargerComparisonScreen.kt#L121)). | `ChargerViewModel.refresh`, tilan seuranta ja kaikki mutaatiot tarkistavat Pro-tilan ([ChargerViewModel.kt](../app/src/main/java/com/runcheck/ui/charger/ChargerViewModel.kt#L41), [ChargerViewModel.kt](../app/src/main/java/com/runcheck/ui/charger/ChargerViewModel.kt#L69)). `GetChargerComparisonUseCase` palauttaa tyhjan Flow'n ilman Prota ([GetChargerComparisonUseCase.kt](../app/src/main/java/com/runcheck/domain/usecase/GetChargerComparisonUseCase.kt#L22)). | `ChargerRepository` on raakadatakontrakti ilman Pro-parametria; portti on sitä ennen use casessa ([ChargerRepository.kt](../app/src/main/java/com/runcheck/domain/repository/ChargerRepository.kt#L8)). |
| App Usage | Home-rivi ohjaa ei-Pro-käyttäjän upgradeen; ruutu näyttää `ProFeatureLockedState`n ([HomeSecondarySections.kt](../app/src/main/java/com/runcheck/ui/home/HomeSecondarySections.kt#L306), [AppUsageScreen.kt](../app/src/main/java/com/runcheck/ui/appusage/AppUsageScreen.kt#L126)). | ViewModel ei käynnistä latausta ilman Prota ja reagoi Pro-tilan poistumiseen ([AppUsageViewModel.kt](../app/src/main/java/com/runcheck/ui/appusage/AppUsageViewModel.kt#L58), [AppUsageViewModel.kt](../app/src/main/java/com/runcheck/ui/appusage/AppUsageViewModel.kt#L78)). `GetAppBatteryUsageUseCase` palauttaa tyhjan `PagingData`n ilman Prota ([GetAppBatteryUsageUseCase.kt](../app/src/main/java/com/runcheck/domain/usecase/GetAppBatteryUsageUseCase.kt#L20)). | `AppBatteryUsageRepository` tarjoaa datan ilman Pro-ehtoa; use case on portti ([AppBatteryUsageRepository.kt](../app/src/main/java/com/runcheck/domain/repository/AppBatteryUsageRepository.kt#L8)). |
| Extended History | Battery-, Network-, Thermal- ja Storage-ruudut käyttävät `state.isPro`-haaroja/history calloutteja; esimerkiksi Battery näyttää lukitun history-tilan, Thermal ja Storage Pro-calloutin ([BatteryDetailScreen.kt](../app/src/main/java/com/runcheck/ui/battery/BatteryDetailScreen.kt#L1066), [ThermalDetailScreen.kt](../app/src/main/java/com/runcheck/ui/thermal/ThermalDetailScreen.kt#L337), [StorageDetailScreen.kt](../app/src/main/java/com/runcheck/ui/storage/StorageDetailScreen.kt#L817)). | ViewModelit välittävät Pro-tilan UI-stateen; Network rajaa speed-test historian `5`/`100` tulokseen ([BatteryViewModel.kt](../app/src/main/java/com/runcheck/ui/battery/BatteryViewModel.kt#L133), [NetworkViewModel.kt](../app/src/main/java/com/runcheck/ui/network/NetworkViewModel.kt#L42), [ThermalViewModel.kt](../app/src/main/java/com/runcheck/ui/thermal/ThermalViewModel.kt#L158)). | Nelja history use casea portittaa repository-kyselyn: Battery/Network rajaavat ei-Prolle viimeiseen paivaan, Thermal/Storage palauttavat tyhjan ([GetBatteryHistoryUseCase.kt](../app/src/main/java/com/runcheck/domain/usecase/GetBatteryHistoryUseCase.kt#L22), [GetNetworkHistoryUseCase.kt](../app/src/main/java/com/runcheck/domain/usecase/GetNetworkHistoryUseCase.kt#L20), [GetThermalHistoryUseCase.kt](../app/src/main/java/com/runcheck/domain/usecase/GetThermalHistoryUseCase.kt#L20), [GetStorageHistoryUseCase.kt](../app/src/main/java/com/runcheck/domain/usecase/GetStorageHistoryUseCase.kt#L20)). |
| Cleanup | Storage Tools naytetaan vain Prolle; muuten `ProFeatureCalloutCard` ([StorageDetailScreen.kt](../app/src/main/java/com/runcheck/ui/storage/StorageDetailScreen.kt#L473)). Cleanup-destination on silti olemassa, joten sen oma UI ei toimi navigointivartijana ([NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L239)). | `CleanupViewModel` estaa skannauksen, poiston ja vahvistukset, ja Pro-tilan menetys mitatoi tilan ([CleanupViewModel.kt](../app/src/main/java/com/runcheck/ui/storage/cleanup/CleanupViewModel.kt#L172), [CleanupViewModel.kt](../app/src/main/java/com/runcheck/ui/storage/cleanup/CleanupViewModel.kt#L324), [CleanupViewModel.kt](../app/src/main/java/com/runcheck/ui/storage/cleanup/CleanupViewModel.kt#L498)). | `StorageCleanupUseCase` ja `StorageCleanupRepository` eivat injektoi Pro-tilaa; portti on ViewModelissa ([StorageCleanupUseCase.kt](../app/src/main/java/com/runcheck/domain/usecase/StorageCleanupUseCase.kt#L14), [StorageCleanupRepository.kt](../app/src/main/java/com/runcheck/domain/repository/StorageCleanupRepository.kt#L10)). |
| Export | Settingsin Data Section piilottaa/poistaa export-actionin ei-Prolta ([SettingsSections.kt](../app/src/main/java/com/runcheck/ui/settings/SettingsSections.kt#L352)). | `SettingsViewModel.exportData()` palaa heti ilman Prota ([SettingsViewModel.kt](../app/src/main/java/com/runcheck/ui/settings/SettingsViewModel.kt#L207)). | `ExportDataUseCase.requirePro()` tekee `check(proStatusProvider.isPro())` ennen CSV-vienteja; jako menee `FileExportRepository`n kautta ([ExportDataUseCase.kt](../app/src/main/java/com/runcheck/domain/usecase/ExportDataUseCase.kt#L51), [FileExportRepository.kt](../app/src/main/java/com/runcheck/domain/repository/FileExportRepository.kt#L3)). |
| Widgets | Ei Compose-asetusruutua widgetin lisaamiseen: widgetin oma Glance UI valitsee `WidgetLockedContent`n, kun data on lukittu ([BatteryWidget.kt](../app/src/main/java/com/runcheck/widget/BatteryWidget.kt#L47), [HealthWidget.kt](../app/src/main/java/com/runcheck/widget/HealthWidget.kt#L53), [WidgetCommon.kt](../app/src/main/java/com/runcheck/widget/WidgetCommon.kt#L33)). | Ei ViewModelia; `WidgetDataProvider` yhdistaa Pro-Flow'n dataan ja palauttaa `Locked`-tilan ([WidgetDataProvider.kt](../app/src/main/java/com/runcheck/widget/WidgetDataProvider.kt#L69)). | `WidgetDataProvider` lukee `ProStatusProvider`n Hilt entry pointin kautta. Molemmat Glance-widgetit rekisteroidaan manifestissa ([WidgetDataProvider.kt](../app/src/main/java/com/runcheck/widget/WidgetDataProvider.kt#L129), [AndroidManifest.xml](../app/src/main/AndroidManifest.xml#L90)). |

## 3. Screenshot- ja preview-matriisi

| Artefakti | Nykyinen katettu kohde / tila | Lähde |
|---|---|---|
| `ComponentStackPreview` | 412 dp levea komponenttipino: top barit, `GridCard`, `ListRow`, confidence badge, progress ring ja Pro lock. | [ComponentPreviews.kt](../app/src/main/java/com/runcheck/ui/components/ComponentPreviews.kt#L20) |
| `LearnScreenPreview` | Learn-katalogin normaali tila, 412 x 915 dp. | [LearnScreen.kt](../app/src/main/java/com/runcheck/ui/learn/LearnScreen.kt#L84) |
| `LearnArticleDetailScreenPreview` | Yksi tunnettu Battery Health -artikkeli, 412 x 915 dp. | [LearnArticleDetailScreen.kt](../app/src/main/java/com/runcheck/ui/learn/LearnArticleDetailScreen.kt#L171) |
| `SpeedTestContentPreview` | Valmis Wi-Fi-speed-test tuloksineen, 412 x 915 dp. | [SpeedTestScreen.kt](../app/src/main/java/com/runcheck/ui/network/SpeedTestScreen.kt#L1099) |
| Screenshot-testit | Repositoryn Kotlin/Gradle/TOML-lahteista ei loytynyt screenshot-testitehtavaa, Paparazzi-/Roborazzi-konfiguraatiota eika screenshot-testiluokkaa. | Tarkistus: `rg -n -i 'screenshot|paparazzi|roborazzi|previewScreenshot|validateDebugScreenshotTest'` project-lahteista; ainoa osuma on debug-Sentryn `isAttachScreenshot = false` ([SentryInit.kt](../app/src/debug/java/com/runcheck/SentryInit.kt#L22)). |

### Puuttuva visuaalinen kattavuus

Nykyiset nelja previewta ovat kaikki `RuncheckTheme`n sisalla; teema maarittelee
vain `darkColorScheme`n ([Theme.kt](../app/src/main/java/com/runcheck/ui/theme/Theme.kt#L46)).
Yhdessakaan neljasta `@Preview`-annotaatiosta ei ole `uiMode`, `fontScale`,
`device` tai layout-suunnan parametria (edellisen taulukon tarkat
annotaatiot). Siksi lahtotilassa puuttuvat:

- vaalea/kontrastivaihtoehto (nykyinen tuote on tarkoituksella vain tumma, ei
  vaalean teeman regressiokohde),
- kapea/leveys- tai tablet-layout, RTL- ja font-scale-previewt,
- ruututason previewt Home-, Insights-, Battery-, Network-, Thermal-, Storage-,
  Cleanup-, Charger-, App Usage-, Settings-, Pro Upgrade- ja Fullscreen Chart
  -kohteille,
- lataus-, virhe-, lukittu-, tyhja- ja permission-tilojen kuvavertailut;
  vain Speed Testin valmis tila ja Learnin normaalitilat ovat previewssa.

Tama on puuttuvan testin/previewn kartoitus, ei vaitos siita, etteiko kyseisilla
ruuduilla olisi runtime-tiloja: esimerkiksi Home, Battery ja Cleanup mallintavat
`Loading`-tilan omissa UI-state-tiedostoissaan
([HomeUiState.kt](../app/src/main/java/com/runcheck/ui/home/HomeUiState.kt#L14),
[BatteryUiState.kt](../app/src/main/java/com/runcheck/ui/battery/BatteryUiState.kt#L11),
[CleanupUiState.kt](../app/src/main/java/com/runcheck/ui/storage/cleanup/CleanupUiState.kt#L17)).

## 4. Komponentti- ja kutsujamatriisi

Tama osa on taydellinen nykyisen HEADin **suorien** kutsu- ja
integraatiotiedostojen inventaario pyydetyille perheille. Määrittelytiedosto
on lueteltu ensin; sen jalkeiset tiedostot ovat kutsujia, ellei rivillä erikseen
mainita Android-manifestin rekisterointia. Näin esimerkiksi ViewModelin
`InfoCard`-tilamalliviittaus ei virheellisesti näy UI-komponentin kutsuna.

| Perhe | Määrittely / omistaja | Kaikki nykyiset kutsujat / integraatiot |
|---|---|---|
| Theme ja tokenit | `ui/theme/Theme.kt`, `Color.kt`, `Type.kt`, `Spacing.kt`, `UiTokens.kt`, `MotionTokens.kt`, `StatusColors.kt`; `RuncheckTheme` tarjoaa tumman `MaterialTheme`n ([Theme.kt](../app/src/main/java/com/runcheck/ui/theme/Theme.kt#L79)). | `RuncheckTheme`: `MainActivity.kt`, `ui/components/ComponentPreviews.kt`, `ui/learn/LearnScreen.kt`, `ui/learn/LearnArticleDetailScreen.kt`, `ui/network/SpeedTestScreen.kt`. Root-ketju on `MainActivity` -> `RuncheckTheme` -> `RuncheckNavHost` ([MainActivity.kt](../app/src/main/java/com/runcheck/MainActivity.kt#L39)). |
| Navigation | `ui/navigation/Screen.kt`, `NavGraph.kt`; `RuncheckNavHost` omistaa ainoan NavHostin. | `RuncheckNavHost`: `MainActivity.kt`. `Screen`-reitit: `NavGraph.kt`, `LearnArticleCatalog.kt` (cross-linkit) ja `MainActivity.kt` (notification-reitin validointi). Notification-polku on `MainActivity.consumeNotificationRoute` -> `RuncheckNavHost` -> `navigateNotificationRoute` ([MainActivity.kt](../app/src/main/java/com/runcheck/MainActivity.kt#L82), [NavGraph.kt](../app/src/main/java/com/runcheck/ui/navigation/NavGraph.kt#L384)). |
| Preferences | `domain/repository/UserPreferencesRepository.kt`, `domain/usecase/ManageUserPreferencesUseCase.kt`, `ObserveSettingsUseCase.kt`; toteutus `data/preferences/UserPreferencesRepositoryImpl.kt`. | Repository-sopimuksen kuluttajat: `data/appusage/AppBatteryUsageRepositoryImpl.kt`; `domain/usecase/ChargerSessionTracker.kt`, `CleanupOldReadingsUseCase.kt`, `DeleteChargerUseCase.kt`, `ExportDataUseCase.kt`, `ManageUserPreferencesUseCase.kt`, `ObserveSettingsUseCase.kt`, `SetDataRetentionUseCase.kt`, `SetMonitoringIntervalUseCase.kt`, `SetNotificationsEnabledUseCase.kt`; `service/monitor/BootReceiver.kt`, `HealthMonitorWorker.kt`, `MonitorScheduler.kt`, `RealTimeMonitorService.kt`; `widget/WidgetDataProvider.kt`; DI-sidonta `di/RepositoryModule.kt`. `ManageUserPreferencesUseCase`-kutsujat: `ui/battery/BatteryViewModel.kt`, `ui/charger/ChargerViewModel.kt`, `ui/fullscreen/FullscreenChartViewModel.kt`, `ui/home/HomeViewModel.kt`, `ui/network/NetworkViewModel.kt`, `ui/settings/SettingsViewModel.kt`, `ui/storage/StorageViewModel.kt`, `ui/thermal/ThermalViewModel.kt`. |
| Widgets | `widget/BatteryWidget.kt`, `HealthWidget.kt`, `WidgetCommon.kt`, `WidgetDataProvider.kt`; receiverit `AndroidManifest.xml`:ssa. | `RuncheckWidgets.updateAll`: `RuncheckApp.kt` ja `service/monitor/HealthMaintenanceWorker.kt`. `BatteryWidget` ja `HealthWidget` instansioidaan `WidgetDataProvider.kt`:ssa paivitysta varten ja omissa receiver-tiedostoissaan `glanceAppWidget`-propertyssa; manifest rekisteroi molemmat receiverit ([WidgetDataProvider.kt](../app/src/main/java/com/runcheck/widget/WidgetDataProvider.kt#L120), [AndroidManifest.xml](../app/src/main/AndroidManifest.xml#L90)). |
| Notifications | `service/monitor/NotificationHelper.kt`; reaaliaikaisen foreground-ilmoituksen omistaa `RealTimeMonitorService.kt`. | `NotificationHelper`-integraatiot: `MainActivity.kt` (extra), `RuncheckApp.kt` (kanavat), `service/monitor/HealthMonitorWorker.kt` (alertit), `RealTimeMonitorService.kt` (kanava ja content intent), `ui/settings/SettingsScreen.kt` (alert-kanavan tila), `worker/TrialNotificationWorker.kt` (trial-viestit). |
| Cards | Jaetut `ActionCard`, `CardSectionTitle`, `ContentContainer`, `GridCard`, `InfoCard`, `InfoSheetHost`, `ListRow`, `ProFeatureCalloutCard`, `ProFeatureLockedState`, `ProBadgePill`; feature-cardit `TrialHomeCard`, `PostExpirationUpgradeCard`, `InsightsCard`, `LearnArticleCard`, `RelatedArticlesSection`. | `ActionCard`: `ui/storage/StorageDetailSupport.kt`. `CardSectionTitle`: `ui/battery/BatteryDetailScreen.kt`, `ui/learn/LearnScreen.kt`, `ui/network/NetworkDetailScreen.kt`, `NetworkDetailSupport.kt`, `ui/settings/SettingsScreen.kt`, `SettingsSections.kt`, `ui/storage/StorageDetailScreen.kt`, `StorageDetailSupport.kt`, `ui/thermal/ThermalDetailScreen.kt`. `ContentContainer`: `ui/insights/InsightsScreen.kt`, `ui/storage/cleanup/CleanupScreen.kt`. `GridCard`: ComponentPreviews, HomeSecondarySections. `InfoCard`: Battery, Network, Storage ja Thermal Detail Screen; `InfoSheetHost`: Battery, Network, SpeedTest, Settings, Storage ja Thermal Screen (ja se kutsuu `InfoBottomSheet`ia). `ListRow`: ComponentPreviews, HomeSecondarySections, StorageDetailSupport. `ProFeatureCalloutCard`: Network, Storage ja Thermal Detail Screen. `ProFeatureLockedState`: AppUsage, ChargerComparison, ComponentPreviews ja FullscreenChart Screen. `ProBadgePill`: Battery Detail, `GridCard` ja HomeSecondarySections. `TrialHomeCard`, `PostExpirationUpgradeCard` ja `InsightsCard`: HomeScreen. `LearnArticleCard`: LearnScreen. `RelatedArticlesSection`: Battery, Network, Storage ja Thermal Detail Screen. |
| Selectorit | `ui/chart/HistoryPeriodFilterChipRow.kt`, `ui/components/SegmentedBar.kt`, `ui/common/ChartSelection.kt` (Material `FilterChip`), Cleanupin paikallinen suodatin. | `HistoryPeriodFilterChipRow`: `ui/network/NetworkDetailScreen.kt`, `ui/storage/StorageDetailScreen.kt`, `ui/thermal/ThermalDetailScreen.kt`. `SegmentedBar`: `ui/storage/StorageDetailScreen.kt`. `FilterChip`/`ChartSelection`: `ui/fullscreen/FullscreenChartScreen.kt` ja `ui/storage/cleanup/CleanupScreen.kt`. |
| Status | `ui/components/ConfidenceBadge.kt`, `StatusDot.kt`, `SegmentedStatusBar.kt`, `SignalBars.kt`, `ProBadgePill.kt`; vari- ja confidence-tokenit `ui/theme/StatusColors.kt`:ssa. | `ConfidenceBadge`: `ui/battery/BatteryDetailScreen.kt`, ComponentPreviews. `StatusDot`: `ui/components/SegmentedBar.kt`, `ui/home/HomeScreen.kt`, `ui/storage/cleanup/CategoryGroup.kt`, `ui/thermal/ThermalDetailScreen.kt`. `SegmentedStatusBar`: Thermal Detail Screen. `SignalBars`: Network Detail Screen. `ProBadgePill`: `ui/home/HomeSecondarySections.kt`. |
| Bannerit | Ei jaettua `*Banner*`-komponenttia. Ainoa nimetty banner-tila on `HomeUiState.Success.showDay5Banner`; näkyvä stale-data-varoitus on HomeScreenin yksityinen `MonitoringStaleWarning`. | `showDay5Banner`-kuluttaja ja `MonitoringStaleWarning`-kutsuja ovat `ui/home/HomeScreen.kt`; tilan tuottaa/poistaa `ui/home/HomeViewModel.kt` ([HomeScreen.kt](../app/src/main/java/com/runcheck/ui/home/HomeScreen.kt#L223), [HomeScreen.kt](../app/src/main/java/com/runcheck/ui/home/HomeScreen.kt#L317), [HomeViewModel.kt](../app/src/main/java/com/runcheck/ui/home/HomeViewModel.kt#L204)). |
| Loading | Ei jaettua loading-composablea; käytössä on ruutukohtainen Material `CircularProgressIndicator` ja UI-state-haarat. | `CircularProgressIndicator`-kutsujat: `ui/appusage/AppUsageScreen.kt`, `ui/battery/BatteryDetailScreen.kt`, `ui/charger/ChargerComparisonScreen.kt`, `ui/fullscreen/FullscreenChartScreen.kt`, `ui/home/HomeScreen.kt`, `ui/insights/InsightsScreen.kt`, `ui/network/NetworkDetailScreen.kt`, `SpeedTestScreen.kt`, `ui/settings/SettingsSections.kt`, `ui/storage/StorageDetailScreen.kt`, `ui/storage/cleanup/CleanupScreen.kt`, `ui/thermal/ThermalDetailScreen.kt`. |
| Charts | `ui/components/AreaChart.kt`, `LiveChart.kt`, `TrendChart.kt`; `ui/chart/ChartStatsRow.kt`, `ChartAccessibility.kt`, `ChartRenderModel.kt`. | `AreaChart`: `ui/battery/BatteryDetailScreen.kt`. `LiveChart`: Battery, Network, Storage ja Thermal Detail Screen. `TrendChart`: Battery, Network, Storage, Thermal ja `ui/fullscreen/FullscreenChartScreen.kt`. `ChartStatsRow`: Battery, Network, Storage ja Thermal Detail Screen. Chart-accessibility/render-modelia kayttavat samat Battery-, Network-, Storage-, Thermal- ja Fullscreen Chart -polut komponenttiensa kautta. |

## Read-only varmennus

Suoritettu HEADista ilman tuotantokoodin muokkausta:

1. Luettiin `Screen.kt`, `NavGraph.kt`, `MainActivity.kt` ja
   `NotificationHelper.kt`; reitit, notification-entry ja back-stack-poikkeukset
   vastaavat reittimatriisia.
2. Haettiin `isPro`, `ProFeatureLockedState`, feature-use caset ja repositoryt;
   jokainen Pro-matriisin rivi on sidottu UI-, ViewModel/use case- ja
   domain/repository-lahteeseen tai eksplisiittiseen puuttuvaan porttiin.
3. Haettiin kaikki `@Preview`-annotaatiot sekä screenshot-testityokalut ja
   -tehtavat; loytyi nelja previewta, ei screenshot-testikonfiguraatiota.
4. Haettiin ja luettiin theme-, navigation-, preferences-, widget- ja
   notification-kutsujat; tulokset on koottu komponenttimatriisiin.

Lahtotason `:app:compileDebugKotlin --no-parallel --max-workers=1` on briefin
mukaan jo lapaissyt, joten Gradle-ajokertaa ei toistettu.
