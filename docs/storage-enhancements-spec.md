# Storage Enhancements Spec

Kattava suunnitelma storage-näkymän laajentamiseksi. Yhdistää informatiiviset osiot ja toiminnalliset työkalut.

Perustuu Android 11–16 API-rajoitusten analyysiin (maaliskuu 2026).

---

## Nykytila

StorageDetailScreen näyttää:
- Total / Used / Available tavuina
- Apps-koko (vaatii PACKAGE_USAGE_STATS -lupaa)
- SD-kortti (jos löytyy)
- Täyttövauhtiarvio (placeholder, palauttaa null)

Puutteita:
- `mediaBytes` aina null
- Ei media-erittelyä kategorioittain
- Ei sovelluslistausta
- Ei historiakaavio (Room-data on, UI puuttuu)
- Ei yhtään toiminnallista ominaisuutta (kaikki read-only)

---

## Osa 1: Informatiiviset parannukset

### 1.1 Media-erittely kategorioittain

#### Mitä
Näytetään tallennustilan jakautuminen: kuvat, videot, musiikki, dokumentit, lataukset, muu.

#### Miltä näyttää
```
MEDIA BREAKDOWN

[  Images  ] [  Videos  ] [  Audio   ]
   3.2 GB       8.1 GB      1.4 GB

[  Documents ] [ Downloads ] [  Other  ]
    420 MB        1.8 GB      2.3 GB
```
- Kuusi MetricPill-korttia kahdella rivillä
- Koko formatoitu `Formatter.formatShortFileSize()`

#### Toteutus
- MediaStore-queryt jokaiselle kategorialle:
  - `MediaStore.Images.Media` → `_SIZE` sarakkeen SUM
  - `MediaStore.Video.Media` → `_SIZE` sarakkeen SUM
  - `MediaStore.Audio.Media` → `_SIZE` sarakkeen SUM
  - `MediaStore.Files` filtteröitynä MIME-tyypin perusteella dokumenteille
  - `MediaStore.Downloads` (API 29+) → lataukset
  - Other = used - (apps + images + videos + audio + documents + downloads)
- Ei vaadi erikoislupia — MediaStore on saatavilla kaikille sovelluksille
- Uusi data class `MediaBreakdown`:
  ```kotlin
  data class MediaBreakdown(
      val imagesBytes: Long,
      val videosBytes: Long,
      val audioBytes: Long,
      val documentsBytes: Long,
      val downloadsBytes: Long,
      val otherBytes: Long
  )
  ```
- Lisätään `StorageState`-malliin: `mediaBreakdown: MediaBreakdown?`
- Query suoritetaan `StorageDataSource`ssa IO-dispatcerilla

#### Merkkijonot
```xml
<string name="storage_images">Images</string>
<string name="storage_videos">Videos</string>
<string name="storage_audio">Audio</string>
<string name="storage_documents">Documents</string>
<string name="storage_downloads">Downloads</string>
<string name="storage_other">Other</string>
```
```xml
<string name="storage_images">Kuvat</string>
<string name="storage_videos">Videot</string>
<string name="storage_audio">Musiikki</string>
<string name="storage_documents">Dokumentit</string>
<string name="storage_downloads">Lataukset</string>
<string name="storage_other">Muut</string>
```

---

### 1.2 Top N sovellukset kokojen mukaan

#### Mitä
Näytetään 10 eniten tilaa vievää sovellusta APK + data + cache -erittelyllä.

#### Miltä näyttää
```
TOP APPS BY SIZE                        [PRO]

Chrome                           1.8 GB
  APK 245 MB · Data 1.2 GB · Cache 312 MB
  [Clear cache ›]

WhatsApp                         1.2 GB
  APK 68 MB · Data 980 MB · Cache 187 MB
  [Clear cache ›]

...
```
- Lista suurimmasta pienimpään
- Jokaisella rivillä: sovelluksen nimi, kokonaiskoko, erittely
- "Clear cache ›" -painike avaa sovelluksen tiedot (ACTION_APPLICATION_DETAILS_SETTINGS)
- Pro-feature

#### Toteutus
- `StorageStatsManager.queryStatsForPackage(uuid, packageName, user)` jokaiselle asennetulle paketille
  - Palauttaa `StorageStats`: `.appBytes`, `.dataBytes`, `.cacheBytes`
- `PackageManager.getInstalledApplications()` → lista paketeista
- `PackageManager.getApplicationLabel()` → sovelluksen nimi
- Vaatii `PACKAGE_USAGE_STATS` -luvan (käyttäjä myöntää asetuksista)
- Jos lupaa ei ole → näytetään kehotus myöntää lupa + linkki asetuksiin
- Uusi data class:
  ```kotlin
  data class AppStorageInfo(
      val packageName: String,
      val appName: String,
      val apkBytes: Long,
      val dataBytes: Long,
      val cacheBytes: Long,
      val totalBytes: Long
  )
  ```
- Top 10 suodatus ja lajittelu `totalBytes` mukaan
- Skannaus taustalla — voi kestää muutaman sekunnin, näytetään latausindikaattori

#### Merkkijonot
```xml
<string name="storage_top_apps">Top Apps by Size</string>
<string name="storage_app_apk">APK %1$s</string>
<string name="storage_app_data">Data %1$s</string>
<string name="storage_app_cache">Cache %1$s</string>
<string name="storage_clear_cache">Clear cache</string>
<string name="storage_grant_usage_permission">Grant usage access to see app sizes</string>
<string name="storage_open_usage_settings">Open Settings</string>
```
```xml
<string name="storage_top_apps">Suurimmat sovellukset</string>
<string name="storage_app_apk">APK %1$s</string>
<string name="storage_app_data">Data %1$s</string>
<string name="storage_app_cache">Välimuisti %1$s</string>
<string name="storage_clear_cache">Tyhjennä välimuisti</string>
<string name="storage_grant_usage_permission">Myönnä käyttötilastolupa nähdäksesi sovelluskoot</string>
<string name="storage_open_usage_settings">Avaa asetukset</string>
```

---

### 1.3 Cache-kokonaismäärä

#### Mitä
Näytetään kaikkien sovellusten yhteenlaskettu välimuistikoko.

#### Miltä näyttää
```
Cache Total
2.4 GB across 87 apps
```
- Osa storage-yhteenvetoa (hero-osio tai details-osio)
- Näytetään vain kun PACKAGE_USAGE_STATS -lupa on myönnetty

#### Toteutus
- Sivutuote top-sovellusten skannauksesta — summataan `.cacheBytes` kaikista paketeista
- Lisätään `StorageState`-malliin: `totalCacheBytes: Long?`, `appCount: Int?`

---

### 1.4 Täyttövauhtiarvio (nyt toimiva)

#### Mitä
Lasketaan Room-historian perusteella kuinka nopeasti tallennustila täyttyy ja milloin se on täynnä.

#### Miltä näyttää
```
Fill Rate
+1.2 GB/week · Full in ~4 months
```

#### Toteutus
- Haetaan Room-tietokannasta storage_readings viimeiseltä 30 päivältä
- Lasketaan lineaarinen regressio `usedBytes` vs `timestamp`
- Kulmakerroin = täyttövauhti tavuina per aikayksikkö
- Ennuste: `(totalBytes - usedBytes) / fillRate`
- Jos data laskee (käyttäjä siivonnut) → näytetään "Stable" tai negatiivinen trendi
- Nykyinen `StorageRepositoryImpl.calculateFillRate()` placeholder korvataan oikealla laskennalla

---

### 1.5 Tallennustilan historiakaavio

#### Mitä
Näytetään tallennustilan käyttö ajan funktiona kaaviossa. Data on jo Room-tietokannassa.

#### Miltä näyttää
```
STORAGE HISTORY                         [PRO]

[Day] [Week] [Month] [All]

[kaavio: usedBytes / totalBytes ajan funktiona]
```

#### Toteutus
- Sama `TrendChart`-komponentti kuin akkuhistoriassa
- Data: `StorageReadingDao.getReadingsSince()` → mapataan `usedBytes` float-listaksi
- HistoryPeriod-valinnat (ilman SINCE_UNPLUG)
- Lisätään `StorageRepository`-rajapintaan: `getReadingsSince(since: Long): Flow<List<StorageReadingData>>`
- Pro-feature

---

### 1.6 Roskakori-koko

#### Mitä
Näytetään MediaStoren roskakorissa olevien tiedostojen kokonaiskoko.

#### Miltä näyttää
```
Trash
345 MB (23 items)
[Empty trash]
```

#### Toteutus
- API 30+ (Android 11): `MediaStore.IS_TRASHED = 1` query
- `ContentResolver.query()` aggregoi `_SIZE` ja `COUNT(*)`
- Alempi API-taso → ei näytetä osiota
- "Empty trash" toiminto: `MediaStore.createDeleteRequest()` kaikille trashed-itemeille

---

## Osa 2: Toiminnalliset ominaisuudet

### 2.1 Isojen tiedostojen skanneri

#### Mitä
Etsitään ja listataan suurimmat mediatiedostot. Käyttäjä voi valita ja poistaa.

#### Miltä näyttää
```
LARGE FILES (> 50 MB)

☐ VID_20260102.mp4          1.2 GB   Video
☐ backup_full.zip            890 MB   Download
☐ podcast_ep234.mp3          245 MB   Audio
☐ IMG_20251231.heic           89 MB   Image
☐ app-release.apk             67 MB   Download

[Delete selected (2.1 GB)]
```
- Checkbox-lista suurimmasta pienimpään
- Tiedostotyyppi-ikoni + nimi + koko + kategoria
- Alaosassa "Delete selected" -painike joka käyttää `createDeleteRequest()`
- Skannaus MediaStoren kautta (ei tarvitse MANAGE_EXTERNAL_STORAGE)

#### Toteutus
- `ContentResolver.query()` MediaStore.Files -taulusta
  - `_SIZE > threshold` (oletus 50 MB, käyttäjä voi säätää: 10/50/100/500 MB)
  - ORDER BY `_SIZE DESC`
  - LIMIT 100
  - Palautetaan: URI, display_name, size, mime_type, date_modified
- Poisto: `MediaStore.createDeleteRequest(activity, uris)` (API 30+)
  - Järjestelmä näyttää vahvistusdialogin
  - Käyttäjä hyväksyy → tiedostot poistetaan
  - `ActivityResultLauncher` vastaanottaa tuloksen
- API 29: `ContentResolver.delete(uri)` yksitellen (ei batch-dialogia)
- Uusi data class:
  ```kotlin
  data class LargeFile(
      val uri: Uri,
      val displayName: String,
      val sizeBytes: Long,
      val mimeType: String,
      val dateModified: Long,
      val mediaType: MediaCategory
  )
  ```
- Skannaus omassa coroutinessa, progress-indikaattori

#### Sijainti
Uusi osio StorageDetailScreenissä tai erillinen näkymä johon navigoidaan.

#### Merkkijonot
```xml
<string name="storage_large_files">Large Files</string>
<string name="storage_large_files_threshold">Larger than %1$s</string>
<string name="storage_delete_selected">Delete selected (%1$s)</string>
<string name="storage_no_large_files">No large files found</string>
<string name="storage_scanning">Scanning…</string>
```
```xml
<string name="storage_large_files">Isot tiedostot</string>
<string name="storage_large_files_threshold">Suuremmat kuin %1$s</string>
<string name="storage_delete_selected">Poista valitut (%1$s)</string>
<string name="storage_no_large_files">Isoja tiedostoja ei löytynyt</string>
<string name="storage_scanning">Skannataan…</string>
```

---

### 2.2 Vanhojen latausten siivous

#### Mitä
Etsitään Downloads-kansion tiedostot jotka ovat vanhempia kuin valittu aikaraja.

#### Miltä näyttää
```
OLD DOWNLOADS

[30 days] [60 days] [90 days] [1 year]

Found 34 files · 2.8 GB

☐ manual_v3.pdf              12 MB   93 days ago
☐ setup.exe                 340 MB   187 days ago
☐ photo_backup.zip          1.1 GB   241 days ago

[Delete selected (1.4 GB)]
```

#### Toteutus
- `MediaStore.Downloads` (API 29+) tai `MediaStore.Files` filtteröitynä polun mukaan
- Query: `DATE_MODIFIED < cutoff`
- Sama delete-mekanismi kuin isojen tiedostojen skannerissa
- FilterChip-rivi aikavälille: 30pv / 60pv / 90pv / 1v

---

### 2.3 APK-tiedostojen siivous

#### Mitä
Etsitään .apk-asennuspaketteja jotka jäävät usein latauskansioon asennuksen jälkeen.

#### Miltä näyttää
```
APK FILES

Found 5 APK files · 456 MB

☐ WhatsApp_v2.24.apk         68 MB
☐ chrome_update.apk          245 MB
☐ game_mod.apk               143 MB

[Delete all APKs (456 MB)]
```

#### Toteutus
- MediaStore query: `MIME_TYPE = 'application/vnd.android.package-archive'`
- Tai `DISPLAY_NAME LIKE '%.apk'`
- Sama delete-mekanismi

---

### 2.4 Kuvakaappausten siivous

#### Mitä
Etsitään vanhoja kuvakaappauksia. Kuvakaappauksia kertyy helposti satoja.

#### Miltä näyttää
```
OLD SCREENSHOTS

Older than [30 days] [60 days] [90 days]

Found 234 screenshots · 1.2 GB

[Select all] [Delete selected (1.2 GB)]
```

#### Toteutus
- MediaStore.Images query:
  - `RELATIVE_PATH LIKE '%Screenshots%'` (API 29+)
  - Tai `DATA LIKE '%Screenshots%'` (legacy)
  - `DATE_MODIFIED < cutoff`
- Thumbnailit voi näyttää `ContentResolver.loadThumbnail()` (API 29+)

---

### 2.5 Duplikaattien tunnistus

#### Mitä
Etsitään mahdollisia duplikaattitiedostoja koon ja nimen perusteella.

#### Miltä näyttää
```
POSSIBLE DUPLICATES                     [PRO]

Group 1: IMG_20260115.jpg (4.2 MB × 2)
  ☐ /DCIM/Camera/IMG_20260115.jpg
  ☐ /Pictures/WhatsApp/IMG_20260115.jpg

Group 2: video_2026.mp4 (890 MB × 2)
  ☐ /DCIM/Camera/video_2026.mp4
  ☐ /Downloads/video_2026.mp4

[Delete selected duplicates (894 MB)]
```

#### Toteutus
- MediaStore query: kaikki tiedostot, GROUP BY size
  - Suodata ryhmät joissa COUNT > 1 ja koko on sama
  - Lisäehto: nimen samankaltaisuus (contains tai Levenshtein)
- Näytetään ryhmittäin, käyttäjä valitsee säilytettävän
- Pro-feature (skannaus voi olla raskas)

---

### 2.6 Roskakori-hallinta

#### Mitä
Näytetään roskakoriin siirrettyjen tiedostojen lista ja mahdollisuus tyhjentää.

#### Toteutus
- API 30+: `MediaStore.QUERY_ARG_MATCH_TRASHED` + `MediaStore.IS_TRASHED`
- Query: `Bundle().apply { putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY) }`
- Listaus + "Empty trash" painike
- `MediaStore.createDeleteRequest()` kaikille trashed-itemeille
- Alle API 30: osio piilotetaan

---

### 2.7 Quick Actions -osio

#### Mitä
Nopeat linkit järjestelmäasetuksiin joissa käyttäjä voi tehdä toimenpiteitä joita sovellus ei voi tehdä itse.

#### Miltä näyttää
```
QUICK ACTIONS

[🗑️ Open Storage Settings    ]  ← ACTION_INTERNAL_STORAGE_SETTINGS
[📦 Free Up Space             ]  ← ACTION_MANAGE_STORAGE (Google Files)
[📊 App Usage Access           ]  ← ACTION_USAGE_ACCESS_SETTINGS
```

#### Toteutus
- Kolme painiketta jotka avaavat Intentin:
  - `Settings.ACTION_INTERNAL_STORAGE_SETTINGS`
  - `StorageManager.ACTION_MANAGE_STORAGE` (API 25+) tai `ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION`
  - `Settings.ACTION_USAGE_ACCESS_SETTINGS` (lupien myöntämiseen)
- Tarkistetaan `resolveActivity()` ennen näyttämistä

---

## Osa 3: Tekninen arkkitehtuuri

### Uudet luokat

```
data/storage/
├── StorageDataSource.kt        (laajennetaan)
├── StorageRepositoryImpl.kt    (laajennetaan)
├── MediaStoreScanner.kt        ← UUSI: MediaStore-queryt
├── AppStorageScanner.kt        ← UUSI: per-app koot
└── StorageCleanupHelper.kt     ← UUSI: delete-toiminnot

domain/model/
├── StorageState.kt             (laajennetaan)
├── MediaBreakdown.kt           ← UUSI
├── AppStorageInfo.kt           ← UUSI
├── LargeFile.kt                ← UUSI
└── TrashInfo.kt                ← UUSI

domain/usecase/
├── GetStorageStateUseCase.kt   (olemassa)
├── ScanLargeFilesUseCase.kt    ← UUSI
├── ScanOldDownloadsUseCase.kt  ← UUSI
├── ScanDuplicatesUseCase.kt    ← UUSI
├── GetTrashInfoUseCase.kt      ← UUSI
├── GetAppStorageUseCase.kt     ← UUSI
└── GetStorageHistoryUseCase.kt ← UUSI

ui/storage/
├── StorageDetailScreen.kt      (laajennetaan merkittävästi)
├── StorageViewModel.kt         (laajennetaan)
├── StorageUiState.kt           (laajennetaan)
├── LargeFilesScreen.kt         ← UUSI (erillinen näkymä)
└── OldDownloadsScreen.kt       ← UUSI (erillinen näkymä)
```

### Delete-mekanismi (API 30+)

```kotlin
// StorageCleanupHelper.kt
class StorageCleanupHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Luo PendingIntent joka näyttää järjestelmän vahvistusdialogin.
     * Kutsutaan Activitystä ActivityResultLauncher-kautta.
     */
    fun createDeleteRequest(uris: List<Uri>): PendingIntent {
        return MediaStore.createDeleteRequest(
            context.contentResolver,
            uris
        )
    }

    /**
     * API 29: poistaa yksitellen (ei batch-dialogia)
     */
    suspend fun deleteLegacy(uris: List<Uri>): Int {
        var deleted = 0
        uris.forEach { uri ->
            try {
                if (context.contentResolver.delete(uri, null, null) > 0) deleted++
            } catch (_: SecurityException) { }
        }
        return deleted
    }
}
```

### ActivityResult-integraatio

```kotlin
// StorageDetailScreen.kt tai LargeFilesScreen.kt
val deleteLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartIntentSenderForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        viewModel.onDeleteConfirmed()
    }
}

// Kun käyttäjä painaa "Delete selected":
val pendingIntent = cleanupHelper.createDeleteRequest(selectedUris)
deleteLauncher.launch(
    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
)
```

---

## Osa 4: Android-versiokohtaiset rajoitukset

| Ominaisuus | API 26-28 | API 29 | API 30+ (Android 11+) |
|------------|-----------|--------|----------------------|
| Media-erittely | ✅ MediaStore | ✅ | ✅ |
| App-koot | ✅ SSM | ✅ | ✅ |
| Downloads query | ❌ (ei erillistä) | ✅ MediaStore.Downloads | ✅ |
| Isot tiedostot | ✅ MediaStore | ✅ | ✅ |
| Batch delete | ❌ yksitellen | ❌ yksitellen | ✅ createDeleteRequest |
| Roskakori | ❌ | ❌ | ✅ IS_TRASHED |
| Duplikaatit | ✅ | ✅ | ✅ |
| RELATIVE_PATH | ❌ | ✅ | ✅ |
| Screenshots query | ⚠️ DATA-polku | ✅ RELATIVE_PATH | ✅ |

**Minimistrategia:** API 26+ (sovelluksen min SDK). Batch delete ja roskakori vain API 30+.

---

## Osa 5: Käyttöliittymärakenne

### StorageDetailScreen — uusi layout

```
┌─ Storage Hero Card ─────────────────┐
│  ProgressBar (used / total)          │
│  XX.X GB / YY.Y GB (ZZ%)            │
│  Cache: 2.4 GB across 87 apps       │
│  Fill rate: +1.2 GB/week             │
└──────────────────────────────────────┘

┌─ Media Breakdown ───────────────────┐
│  [Images] [Videos] [Audio]           │
│  [Docs]   [Downloads] [Other]        │
└──────────────────────────────────────┘

┌─ Quick Actions ─────────────────────┐
│  [🗑️ Isot tiedostot (> 50 MB)]     │  → navigoi LargeFilesScreen
│  [📥 Vanhat lataukset]               │  → navigoi OldDownloadsScreen
│  [📦 APK-tiedostot]                  │  → skannaa ja näytä
│  [🗑️ Roskakori (345 MB)]            │  → tyhjennä
│  [📸 Vanhat kuvakaappaukset]         │  → skannaa ja näytä
└──────────────────────────────────────┘

┌─ Top Apps [PRO] ────────────────────┐
│  1. Chrome          1.8 GB  [Info ›] │
│  2. WhatsApp        1.2 GB  [Info ›] │
│  ...                                 │
└──────────────────────────────────────┘

┌─ Duplicates [PRO] ──────────────────┐
│  3 duplicate groups · 1.4 GB         │
│  [Scan for duplicates]               │
└──────────────────────────────────────┘

┌─ Storage History [PRO] ─────────────┐
│  [Day] [Week] [Month] [All]         │
│  [TrendChart]                        │
└──────────────────────────────────────┘

┌─ SD Card ───────────────────────────┐
│  (jos saatavilla)                    │
└──────────────────────────────────────┘

┌─ Quick Actions (System) ────────────┐
│  [Open Storage Settings]             │
│  [Free Up Space]                     │
│  [Grant Usage Access]                │
└──────────────────────────────────────┘
```

---

## Osa 6: Toteutusjärjestys

| Vaihe | Feature | Työmäärä | Riippuvuudet |
|-------|---------|----------|-------------|
| 1 | Media-erittely (#1.1) | Keskitaso | MediaStore query |
| 2 | Cache-koko (#1.3) + hero-kortin parannus | Pieni | SSM query (jo olemassa) |
| 3 | Täyttövauhti (#1.4) | Pieni | Room-data (olemassa) |
| 4 | Quick Actions — system (#2.7) | Pieni | Intent-linkit |
| 5 | Isot tiedostot (#2.1) | Iso | MediaStore + createDeleteRequest |
| 6 | Vanhat lataukset (#2.2) | Keskitaso | Sama mekanismi kuin #5 |
| 7 | APK-siivous (#2.3) | Pieni | Sama mekanismi |
| 8 | Kuvakaappaukset (#2.4) | Pieni | Sama mekanismi |
| 9 | Roskakori (#2.6) | Keskitaso | API 30+ check |
| 10 | Top apps (#1.2) | Keskitaso | SSM per-package + lupa |
| 11 | Historiakaavio (#1.5) | Pieni | Room + TrendChart (olemassa) |
| 12 | Duplikaatit (#2.5) | Iso | Skannauslogiikka |

Vaiheet 1–4 yhdessä sessiossa (informatiiviset + quick links).
Vaihe 5 erikseen (delete-mekanismi on perusta kaikille siivouksille).
Vaiheet 6–9 käyttävät samaa delete-infraa, voidaan tehdä nopeasti peräkkäin.
Vaiheet 10–12 erikseen.

---

## Osa 7: Luvat ja rajoitukset

### Tarvittavat luvat

| Lupa | Pakollinen | Käyttö |
|------|-----------|--------|
| Ei mitään | - | Media-erittely, isot tiedostot, roskakori, lataukset |
| `PACKAGE_USAGE_STATS` | Valinnainen | Top apps, cache-erittely |

**Huom:** `MANAGE_EXTERNAL_STORAGE` EI tarvita. Kaikki tiedostotoiminnot tehdään MediaStore-APIen kautta jotka toimivat scoped storagen kanssa.

### Play Store -yhteensopivuus

- `createDeleteRequest()` on Googlen suositeltu tapa — ei riko Play Store -sääntöjä
- Ei vaadita `MANAGE_EXTERNAL_STORAGE` -lupaa joka rajoittaa Play Store -hyväksyntää
- `PACKAGE_USAGE_STATS` on OK kun käyttötarkoitus on perusteltu (device monitoring app)

---

## Osa 8: Merkkijonojen yhteenveto

### Uudet merkkijonot (EN)
```xml
<!-- Storage Media Breakdown -->
<string name="storage_media_breakdown">Media Breakdown</string>
<string name="storage_images">Images</string>
<string name="storage_videos">Videos</string>
<string name="storage_audio">Audio</string>
<string name="storage_documents">Documents</string>
<string name="storage_downloads">Downloads</string>
<string name="storage_other">Other</string>

<!-- Storage Cache -->
<string name="storage_cache_total">Cache</string>
<string name="storage_cache_summary">%1$s across %2$d apps</string>

<!-- Storage Actions -->
<string name="storage_large_files">Large Files</string>
<string name="storage_large_files_threshold">Larger than %1$s</string>
<string name="storage_old_downloads">Old Downloads</string>
<string name="storage_old_downloads_desc">Older than %1$d days</string>
<string name="storage_apk_files">APK Files</string>
<string name="storage_old_screenshots">Old Screenshots</string>
<string name="storage_trash">Trash</string>
<string name="storage_trash_summary">%1$s (%2$d items)</string>
<string name="storage_empty_trash">Empty trash</string>
<string name="storage_delete_selected">Delete selected (%1$s)</string>
<string name="storage_select_all">Select all</string>
<string name="storage_no_items">No items found</string>
<string name="storage_scanning">Scanning…</string>
<string name="storage_items_found">Found %1$d files · %2$s</string>

<!-- Storage Top Apps -->
<string name="storage_top_apps">Top Apps by Size</string>
<string name="storage_app_cache">Cache %1$s</string>
<string name="storage_clear_cache">Clear cache</string>
<string name="storage_grant_usage_permission">Grant usage access to see app sizes</string>
<string name="storage_open_usage_settings">Open Settings</string>

<!-- Storage Duplicates -->
<string name="storage_duplicates">Possible Duplicates</string>
<string name="storage_duplicate_groups">%1$d groups · %2$s</string>
<string name="storage_scan_duplicates">Scan for duplicates</string>

<!-- Storage History -->
<string name="storage_history">Storage History</string>

<!-- Storage Quick Actions -->
<string name="storage_quick_actions">Quick Actions</string>
<string name="storage_open_settings">Open Storage Settings</string>
<string name="storage_free_up_space">Free Up Space</string>
```

### Uudet merkkijonot (FI)
```xml
<!-- Storage Media Breakdown -->
<string name="storage_media_breakdown">Median erittely</string>
<string name="storage_images">Kuvat</string>
<string name="storage_videos">Videot</string>
<string name="storage_audio">Musiikki</string>
<string name="storage_documents">Dokumentit</string>
<string name="storage_downloads">Lataukset</string>
<string name="storage_other">Muut</string>

<!-- Storage Cache -->
<string name="storage_cache_total">Välimuisti</string>
<string name="storage_cache_summary">%1$s %2$d sovelluksessa</string>

<!-- Storage Actions -->
<string name="storage_large_files">Isot tiedostot</string>
<string name="storage_large_files_threshold">Suuremmat kuin %1$s</string>
<string name="storage_old_downloads">Vanhat lataukset</string>
<string name="storage_old_downloads_desc">Yli %1$d päivää vanhat</string>
<string name="storage_apk_files">APK-tiedostot</string>
<string name="storage_old_screenshots">Vanhat kuvakaappaukset</string>
<string name="storage_trash">Roskakori</string>
<string name="storage_trash_summary">%1$s (%2$d kohdetta)</string>
<string name="storage_empty_trash">Tyhjennä roskakori</string>
<string name="storage_delete_selected">Poista valitut (%1$s)</string>
<string name="storage_select_all">Valitse kaikki</string>
<string name="storage_no_items">Kohteita ei löytynyt</string>
<string name="storage_scanning">Skannataan…</string>
<string name="storage_items_found">Löytyi %1$d tiedostoa · %2$s</string>

<!-- Storage Top Apps -->
<string name="storage_top_apps">Suurimmat sovellukset</string>
<string name="storage_app_cache">Välimuisti %1$s</string>
<string name="storage_clear_cache">Tyhjennä välimuisti</string>
<string name="storage_grant_usage_permission">Myönnä käyttötilastolupa nähdäksesi sovelluskoot</string>
<string name="storage_open_usage_settings">Avaa asetukset</string>

<!-- Storage Duplicates -->
<string name="storage_duplicates">Mahdolliset duplikaatit</string>
<string name="storage_duplicate_groups">%1$d ryhmää · %2$s</string>
<string name="storage_scan_duplicates">Etsi duplikaatteja</string>

<!-- Storage History -->
<string name="storage_history">Tallennustilan historia</string>

<!-- Storage Quick Actions -->
<string name="storage_quick_actions">Pikatoiminnot</string>
<string name="storage_open_settings">Avaa tallennustila-asetukset</string>
<string name="storage_free_up_space">Vapauta tilaa</string>
```
