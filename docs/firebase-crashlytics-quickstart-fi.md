# Firebase Crashlytics käyttöönotto myöhemmin

Tämä ohje on runchecka varten. Sitä ei tarvitse tehdä vielä kehitysvaiheessa.

Nykyinen koodi on jo valmisteltu näin:

- Firebase Crashlytics SDK on lisätty projektiin
- Firebase Analyticsia ei käytetä
- crash-raportointi on oletuksena pois päältä
- käyttäjä voi ottaa crash-raportoinnin käyttöön sovelluksen asetuksista
- debug-buildit eivät lähetä raportteja

Kun haluat ottaa Crashlyticsin oikeasti käyttöön, tee vain nämä vaiheet.

## 1. Luo Firebase-projekti

1. Mene osoitteeseen `https://console.firebase.google.com/`
2. Luo uusi projekti, esimerkiksi `runcheck`
3. Kun Firebase kysyy Google Analyticsista:
   - ohita se
   - tai poista se käytöstä

Analyticsia ei tarvita Crashlyticsiä varten tässä sovelluksessa.

## 2. Lisää Android-sovellus Firebaseen

1. Valitse Firebase-projektissa `Add app`
2. Valitse `Android`
3. Anna package name:

```text
com.runcheck
```

4. App nickname on vapaaehtoinen
5. SHA-1 ei ole Crashlyticsin peruskäyttöön pakollinen, joten sen voi jättää myöhemmäksi

## 3. Lataa oikea google-services.json

Firebase antaa ladattavaksi tiedoston:

```text
google-services.json
```

Korvaa tällä tiedostolla repoossa oleva placeholder:

[`app/google-services.json`](/home/emma/dev/runcheck/app/google-services.json)

## 4. Ota Crashlytics käyttöön Firebase Consolessa

1. Avaa Firebase-projektissa `Crashlytics`
2. Viimeistele mahdollinen onboarding
3. Tässä vaiheessa konsoli voi näyttää, että se odottaa ensimmäistä raporttia

Se on normaalia.

## 5. Testaa release-buildillä

Crashlytics kannattaa testata release-variantilla, koska debug-buildit eivät lähetä raportteja.

Esimerkki:

```bash
./gradlew assembleRelease
```

Jos teet Play-jakelua varten AAB:n:

```bash
./gradlew bundleRelease
```

Muista signing-ympäristömuuttujat ennen release-buildiä.

## 6. Testaa opt-in-polku

Varmista nämä asiat:

1. Sovellus käynnistyy ja crash-raportointi on oletuksena pois päältä
2. Asetuksissa näkyy:

```text
Settings > Privacy > Share crash reports
```

3. Debug-build ei lähetä raportteja
4. Release-build alkaa lähettää raportteja vasta kun käyttäjä kytkee asetuksen päälle

## 7. Lähetä ensimmäinen testiraportti

Kun haluat varmistaa, että integraatio toimii:

1. Asenna release-build laitteelle
2. Ota crash-raportointi käyttöön asetuksista
3. Aiheuta testikaatuminen
4. Avaa sovellus uudelleen
5. Tarkista Firebase Consolesta, että raportti ilmestyy Crashlyticsiin

## 8. Mitä pitää muistaa ennen julkaisua

Ennen kuin julkaiset version, jossa Crashlytics on mukana:

1. Päivitä privacy policy kertomaan, että käyttäjän suostumuksella crash-diagnostiikkaa voidaan lähettää Firebase Crashlyticsiin
2. Varmista, ettei Firebase Analyticsia ole lisätty projektiin
3. Varmista, ettei sovellus lähetä custom-logeihin:
   - WiFi SSID:tä
   - carrier-nimiä
   - IP-osoitteita
   - vapaamuotoista käyttäjädataa
4. Varmista, että placeholder-`google-services.json` on korvattu oikealla tiedostolla

## Mitä sinun ei tarvitse tehdä vielä

Näitä ei tarvitse tehdä nyt, jos sovellus on vielä aktiivisessa kehityksessä:

- Firebase-projektin luonti
- oikean `google-services.json`-tiedoston lisääminen
- Crashlyticsin tuotantotestaus

Ne voi tehdä myöhemmin juuri ennen sisäistä testijakelua tai ensimmäistä julkaisua.
