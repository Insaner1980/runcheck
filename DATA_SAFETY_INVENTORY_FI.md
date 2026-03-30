# runcheck Data Safety -inventaario koodista

Paivitetty: 2026-03-30

Tama tiedosto on tekninen inventaario.
Tama EI ole lopullinen Play Console -lomake.
Taman tarkoitus on auttaa tayttamaan lomake oikein myohemmin.

## 1. Mita tama tiedosto tekee

Tama tiedosto vastaa kysymykseen:

- mita dataa `runcheck` kasittelee
- mika data pysyy vain puhelimessa
- mika data lahtee verkkoon
- mihin verkkoon se lahtee
- milloin se lahtee
- missa koodissa se tapahtuu

## 2. Lyhyt yhteenveto

Tamanhetkisen koodin perusteella:

- suurin osa `runcheck`in mittausdatasta pysyy laitteella
- appi tekee kuitenkin oikeita ulospain menevia verkkotoimintoja
- ne eivat rajoitu vain käyttäjän speed testiin

Tarkeimmat ulospain menevat datavirrat ovat:

1. Google Play Billing
2. latency-mittaus `locate.measurementlab.net:443`
3. M-Lab NDT7 speed test

Debug-buildissa on lisaksi:

4. Sentry debug-diagnostiikka

Mutta release-buildissa Sentry on no-op eika kuulu julkaistavaan artifactiin.

## 3. Menetelma

Inventaario on tehty lukemalla ainakin seuraavat kohdat:

- [app/src/main/AndroidManifest.xml](/home/emma/dev/runcheck/app/src/main/AndroidManifest.xml)
- [app/build.gradle.kts](/home/emma/dev/runcheck/app/build.gradle.kts)
- [app/src/main/java/com/runcheck/data/network/SpeedTestService.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/network/SpeedTestService.kt)
- [app/src/main/java/com/runcheck/data/network/LatencyMeasurer.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/network/LatencyMeasurer.kt)
- [app/src/main/java/com/runcheck/data/network/NetworkRepositoryImpl.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/network/NetworkRepositoryImpl.kt)
- [app/src/main/java/com/runcheck/service/monitor/HealthMonitorWorker.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/service/monitor/HealthMonitorWorker.kt)
- [app/src/main/java/com/runcheck/domain/usecase/GetMeasuredNetworkStateUseCase.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/domain/usecase/GetMeasuredNetworkStateUseCase.kt)
- [app/src/main/java/com/runcheck/data/billing/BillingManager.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/billing/BillingManager.kt)
- [app/src/main/java/com/runcheck/data/billing/ProStatusCache.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/billing/ProStatusCache.kt)
- [app/src/debug/java/com/runcheck/SentryInit.kt](/home/emma/dev/runcheck/app/src/debug/java/com/runcheck/SentryInit.kt)
- [app/src/release/java/com/runcheck/SentryInit.kt](/home/emma/dev/runcheck/app/src/release/java/com/runcheck/SentryInit.kt)

## 4. Data joka pysyy laitteella

Nama asiat nayttavat pysyvan laitteella eivatka tamanhetkisen koodin perusteella laehde appin omana datasiirtona ulos:

- battery readings
- thermal readings
- storage readings
- app usage snapshots
- widgets data
- user preferences DataStoreen
- paikallinen Pro-tilan boole-cache SharedPreferencesiin
- paikallinen Room-historia
- speed test -tulosten paikallinen tallennus Roomiin

Esimerkkeja paikallisesta tallennuksesta:

- [ProStatusCache.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/billing/ProStatusCache.kt)
- [SpeedTestRepositoryImpl.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/network/SpeedTestRepositoryImpl.kt)
- [NetworkRepositoryImpl.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/network/NetworkRepositoryImpl.kt)

Tarkeaa:

- se, että data tallennetaan paikallisesti, ei tee appista automaattisesti "ei dataa ulos koskaan"
- siksi alla olevat verkkovirrat ovat eri lista

## 5. Ulospain menevat datavirrat

## 5.1 Google Play Billing

### Mita tapahtuu

Appi kayttaa Google Play Billingiä Pro-oston tarkistukseen ja ostoon.

### Missa koodissa

- [BillingManager.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/billing/BillingManager.kt#L78)
- [build.gradle.kts](/home/emma/dev/runcheck/app/build.gradle.kts#L260)

### Milloin data voi lahtea ulos

- appin kaynnistyessa, kun billing alustetaan release-buildissa
- kun kysytaan olemassa olevat ostot
- kun kysytaan tuotteen tiedot
- kun kaynnistetaan ostoflow
- kun ostos kuitataan

### Mitka kutsut kertovat taman

- `queryPurchasesAsync`
- `queryProductDetails`
- `launchBillingFlow`
- `acknowledgePurchase`

### Mita dataa tama todennakoisesti koskee

Koodista voidaan varmasti sanoa:

- tuote-ID `runcheck_pro`
- ostotila / ownership-tila
- purchase token ostoksen kuittausta varten

Koodista EI voida yksin varmasti paatella kaikkia Google Playn taustalla kasittelemiä kenttiä.
Niihin voi kuulua myos Google-tiliin, laskutukseen, laitteeseen tai kauppatapahtumaan liittyvaa metatietoa, jota Play kasittelee SDK:n kautta.

### Mita appi itse tallentaa paikallisesti

Appi tallentaa varmasti vain:

- paikallisen `is_pro`-booleanin cacheen

Tasta loytyy koodi:

- [ProStatusCache.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/billing/ProStatusCache.kt#L15)

Appi ei tamanhetkisen koodin perusteella tallenna purchase tokenia omaan pysyvaan varastoonsa.

### Data safety -merkitys

Tama on selva off-device-dataflow.
Se pitaa huomioida Data safety -arviossa.

### Varmuusaste

Korkea.

## 5.2 Automaattinen latency-mittaus taustalla

### Mita tapahtuu

Appi tekee TCP-yhteyden hostiin `locate.measurementlab.net` porttiin `443` mitatakseen latencya.

### Missa koodissa

- [LatencyMeasurer.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/network/LatencyMeasurer.kt#L23)
- [build.gradle.kts](/home/emma/dev/runcheck/app/build.gradle.kts#L42)

### Miten tiedan etta tama ei ole vain speed test

Latency-mittaus kaynnistyy myos taustatyossa:

- [HealthMonitorWorker.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/service/monitor/HealthMonitorWorker.kt#L61)

Se kaynnistyy myos verkon mittausflowssa:

- [GetMeasuredNetworkStateUseCase.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/domain/usecase/GetMeasuredNetworkStateUseCase.kt#L16)

Tama tarkoittaa:

- appi ei tee vain käyttäjän painamaa speed testiä
- appi tekee myos automaattista latency-verkkoliikennetta

### Mita dataa lahtee ulos

Koodista voidaan varmasti sanoa:

- TCP-yhteys M-Labin hostiin
- yhteyden metatiedot, joita verkkoyhteys vaatii toimiakseen

Kaytannon tasolla palvelin nakee ainakin:

- lahettavan IP-osoitteen
- yhteyden ajankohdan
- TCP-yhteyden tekniset tiedot

Koodi ei rakenna omaa sovellustasoista JSON- tai lomakepayloadia tassa kohdassa.

### Mita appi tallentaa paikallisesti

Appi tallentaa vain mitatun latency-arvon paikalliseen historiaan:

- [NetworkRepositoryImpl.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/network/NetworkRepositoryImpl.kt#L58)

### Data safety -merkitys

Tama on selva off-device-dataflow.
Tama kannattaa huomioida eri rivina kuin speed test, koska tama voi tapahtua ilman erillista speed test -napin painamista.

### Varmuusaste

Korkea.

## 5.3 Kayttajan kaynnistama speed test (M-Lab NDT7)

### Mita tapahtuu

Kun käyttäjä aloittaa speed testin, appi kayttaa M-Labin NDT7-palvelua lataus-, lahetys- ja ping-mittaukseen.

### Missa koodissa

- [SpeedTestService.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/network/SpeedTestService.kt#L27)
- [RunSpeedTestUseCase.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/domain/usecase/RunSpeedTestUseCase.kt#L1)
- [NetworkViewModel.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/ui/network/NetworkViewModel.kt#L99)

### Mita dataa lahtee ulos

Koodista voidaan varmasti sanoa:

- appi kaynnistaa verkkotestin M-Labin kautta
- testissa lahtee oikeaa testiliikennetta verkkoon
- palvelin nakee IP-osoitteen ja muut yhteyden toteutumiseen liittyvat verkkotiedot

Koodista ei nay, että appi liittaisi speed testiin omaa kayttaja-ID:ta, nimea tai sähköpostia.

### Mita dataa appi saa takaisin ja tallentaa paikallisesti

Appi tallentaa Roomiin:

- `downloadMbps`
- `uploadMbps`
- `pingMs`
- `jitterMs`
- `serverName`
- `serverLocation`
- `connectionType`
- `networkSubtype`
- `signalDbm`

Tama loytyy taalta:

- [SpeedTestRepositoryImpl.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/network/SpeedTestRepositoryImpl.kt#L28)

### Data safety -merkitys

Tama on selva off-device-dataflow.
Koska testin kaynnistaa käyttäjä itse, taman voi erottaa inventaariossa "user-initiated" -tapaukseksi.

### Varmuusaste

Korkea.

## 5.4 Debug-only Sentry

### Mita tapahtuu

Debug-buildissa Sentry alustetaan.
Release-buildissa Sentry on no-op.

### Missa koodissa

- [app/src/debug/java/com/runcheck/SentryInit.kt](/home/emma/dev/runcheck/app/src/debug/java/com/runcheck/SentryInit.kt)
- [app/src/release/java/com/runcheck/SentryInit.kt](/home/emma/dev/runcheck/app/src/release/java/com/runcheck/SentryInit.kt)
- [build.gradle.kts](/home/emma/dev/runcheck/app/build.gradle.kts#L267)

### Data safety -merkitys

Jos arvioit julkaistavaa release-bundlea:

- tata EI pidä laskea mukaan julkaistun appin data safetyyn, jos release-artifact todella ei sisalla Sentrya

Jos joskus muutat release-buildin sisaltamaan Sentryn:

- inventaario pitaa paivittaa heti

### Varmuusaste

Korkea.

## 6. Dataflowt, joita en laskisi varsinaiseksi "app kerää dataa ulos" -ydinriviksi

## 6.1 CSV-exportin jakaminen toiseen appiin

Koodi:

- [SettingsScreen.kt](/home/emma/dev/runcheck/app/src/main/java/com/runcheck/ui/settings/SettingsScreen.kt#L943)

Tama on kayttajan oma jakotoiminto:

- käyttäjä valitsee jakamisen
- Android chooser avataan
- tiedosto annetaan käyttäjän valitsemaan kohdeappiin

Tama kannattaa muistaa privacy policyssa ja tuotteen kuvauksessa.
Mutta Data safetyssa taman arviointi on eri luonteinen kuin appin automaattinen taustakeruu, koska kyse on käyttäjän omasta nimenomaisesta toiminnasta.

### Varmuusaste

Keskitaso.

## 6.2 Learn-linkkien avaaminen selaimeen

Jos appi avaa linkin ulkoiseen selaimeen, se ei ole sama asia kuin oma analytiikka-SDK tai oma taustalähetys.
Tasta ei loytynyt taman inventaarion aikana sellaista appin omaa analytiikkavirtaa, joka vaatisi erillisen data safety -rivin.

### Varmuusaste

Keskitaso.

## 7. Nykyinen karkea luokittelu

## 7.1 Local-only

- battery
- thermal
- storage
- app usage
- widget data
- user preferences
- paikallinen Pro-cache
- paikallinen mittaushistoria

## 7.2 Off-device

- Google Play Billing
- automaattinen latency-mittaus
- käyttäjän kaynnistama speed test

## 7.3 Debug-only, ei releaseen

- Sentry

## 8. Asiat jotka pitaa tarkistaa uudestaan ennen lopullista Play-lomaketta

1. Lisaantyyko uusia verkkokutsuja?
2. Muuttuuko latency-mittauksen ajotapa?
3. Tuleeko release-buildiin uusia SDK:ita?
4. Lisaatko kirjautumisen, pilvisynkan, palautelomakkeen tai muun first-party-backendin?
5. Muuttuuko export-toiminto niin, etta appi itse upload-aa tiedostoja verkkoon?

Jos vastaus yhteenkään on `kylla`, tama tiedosto pitaa paivittaa.

## 9. Nykyinen paras kaytannon johtopaatos

Tamanhetkisen koodin perusteella `runcheck` ei ole "pelkka offline-tyokalu".

Tama on tarkempi kuva:

- suurin osa datasta pysyy paikallisesti
- mutta appissa on useita aitoja off-device-dataflow'ta
- ainakin billing, latency ja speed test pitaa huomioida ennen lopullista Data safety -vastausta

## 10. Viralliset taustalahteet

- Google Play Data safety: <https://support.google.com/googleplay/android-developer/answer/10787469?hl=en>
- Google Play Billing testing: <https://developer.android.com/google/play/billing/test>
- In-app / one-time product availability: <https://support.google.com/googleplay/android-developer/answer/1153481?hl=en>
