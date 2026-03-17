# Storage Cleanup Feature Spec

Yksityiskohtainen suunnitelma tiedostojen siivoustoimintojen toteutukselle. Kattaa UI:n, UX-flown, teknisen arkkitehtuurin ja visuaaliset elementit.

Viittaa: [storage-enhancements-spec.md](storage-enhancements-spec.md), [storage-ui-design.md](storage-ui-design.md)

---

## 1. Arkkitehtuuri

### Yksi uudelleenkäytettävä näkymä

Kaikki siivoustyypit (Large Files, Old Downloads, APK Files) noudattavat samaa kaavaa: skannaa → näytä lista → valitse → poista. Yksi `CleanupScreen` + `CleanupViewModel` + tyyppiparametri.

```kotlin
enum class CleanupType {
    LARGE_FILES,
    OLD_DOWNLOADS,
    APK_FILES
}
```

Jokainen tyyppi määrittää:
- Otsikon (string resource)
- Skannaustoiminnon (MediaStoreScanner-metodi)
- Filtterivaihtoehtojen listan
- Oletusfiltterit
- Oletusvalinnan (APK = kaikki valittu, muut = ei mitään)

### Tiedostorakenne

```
ui/storage/
├── StorageDetailScreen.kt         (olemassa)
├── StorageViewModel.kt            (olemassa, trash-empty lisätään)
├── StorageUiState.kt              (olemassa)
├── cleanup/
│   ├── CleanupScreen.kt           ← uudelleenkäytettävä päänäkymä
│   ├── CleanupViewModel.kt        ← skannaus + valinta + poisto
│   ├── CleanupUiState.kt          ← Idle/Scanning/Results/Deleting/Success
│   ├── CleanupType.kt             ← enum + filtterioptiot
│   ├── FileListItem.kt            ← thumbnail + info + MiniBar
│   ├── CategoryGroup.kt           ← collapsible ryhmäotsikko
│   ├── CleanupBottomBar.kt        ← sticky: ennuste + delete-painike
│   └── CleanupSuccessOverlay.kt   ← "X GB freed" -animaatio

data/storage/
├── MediaStoreScanner.kt           (olemassa)
├── StorageCleanupHelper.kt        ← createDeleteRequest wrapper
└── ThumbnailLoader.kt             ← pikkukuvien lataus + LRU-cache
```

---

## 2. Filtterioptiot per tyyppi

### LARGE_FILES

```
[10 MB] [50 MB] [100 MB] [500 MB]
```
- Oletus: 50 MB
- Skannaa: `MediaStoreScanner.scanLargeFiles(thresholdBytes)`
- Oletusvalinta: ei mitään (käyttäjä valitsee tietoisesti)

### OLD_DOWNLOADS

```
[30 days] [60 days] [90 days] [1 year]
```
- Oletus: 30 days
- Skannaa: `MediaStoreScanner.scanOldDownloads(olderThanMs)`
- Oletusvalinta: ei mitään

### APK_FILES

- Ei filttereitä
- Skannaa: `MediaStoreScanner.scanApkFiles()`
- Oletusvalinta: kaikki valittu (APK:t ovat lähes aina turvallista poistaa)

---

## 3. Tilat (CleanupUiState)

```kotlin
sealed interface CleanupUiState {
    data object Idle : CleanupUiState

    data class Scanning(val progress: Float) : CleanupUiState

    data class Results(
        val groups: List<FileGroup>,
        val selectedUris: Set<Uri>,
        val selectedSize: Long,
        val totalSize: Long,
        val totalCount: Int,
        val currentUsagePercent: Float,
        val projectedUsagePercent: Float
    ) : CleanupUiState

    data class Deleting(val count: Int) : CleanupUiState

    data class Success(val freedBytes: Long) : CleanupUiState
}

data class FileGroup(
    val category: MediaCategory,
    val files: List<ScannedFile>,
    val totalBytes: Long,
    val expanded: Boolean = false
)
```

### Tilasiirtymät

```
Idle → (automaattinen skannaus) → Scanning
Scanning → Results
Results → (käyttäjä painaa delete) → Deleting
Deleting → Success
Success → (2s timeout) → Results (päivitetty lista)

Filtterinvaihto missä tahansa → Scanning → Results
```

---

## 4. UI-näkymä

### 4.1 Skannausvaihe

```
┌──────────────────────────────────────────┐
│  ← Large Files                           │
│                                          │
│  [10 MB] [50 MB] [100 MB] [500 MB]      │
│                                          │
│                                          │
│              ◌ Scanning…                 │  ← CircularProgressIndicator
│                                          │
│                                          │
└──────────────────────────────────────────┘
```

### 4.2 Tulokset — ryhmitelty lista

```
┌──────────────────────────────────────────┐
│  ← Large Files                           │
│                                          │
│  [10 MB] [50 MB] [100 MB] [500 MB]      │
│                                          │
│  Found 14 files · 4.2 GB                │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │ ▾ Videos (5) · 3.1 GB    [All ☐] │  │  ← Blue dot
│  │ ────────────────────────────────── │  │
│  │ ☑ ┌─────┐ VID_20260102    1.2 GB │  │
│  │   │thumb│ Video · Jan 2           │  │
│  │   └─────┘ ████████████████░░░░░░  │  │
│  │ ────────────────────────────────── │  │
│  │ ☐ ┌─────┐ recording      890 MB  │  │
│  │   │thumb│ Video · Mar 1           │  │
│  │   └─────┘ ██████████████░░░░░░░░  │  │
│  │ ────────────────────────────────── │  │
│  │ ...                               │  │
│  │                                    │  │
│  │ ▸ Downloads (4) · 820 MB [All ☐] │  │  ← Yellow dot
│  │ ▸ Images (3) · 270 MB    [All ☐] │  │  ← Teal dot
│  │ ▸ Audio (2) · 180 MB     [All ☐] │  │  ← Orange dot
│  └────────────────────────────────────┘  │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │  67% → 59%                        │  │  ← CleanupBottomBar
│  │  ██████████████░░░  → ██████████░ │  │
│  │                                    │  │
│  │ ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓  │  │
│  │ ┃   Free 2.1 GB · 3 items     ┃  │  │
│  │ ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛  │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

### 4.3 Onnistumistila

```
┌──────────────────────────────────────────┐
│                                          │
│                                          │
│                 ✓                        │  ← AccentTeal, 48dp
│            2.1 GB freed                  │  ← titleLarge, numericFont
│                                          │
│                                          │
└──────────────────────────────────────────┘
```

- Overlay koko näkymän päällä
- Fade-in 200ms, pysyy 1.5s, fade-out 300ms
- Sitten lista päivittyy (poistetut häviävät)

### 4.4 Tyhjä tila

```
┌──────────────────────────────────────────┐
│  ← Large Files                           │
│                                          │
│  [10 MB] [50 MB] [100 MB] [500 MB]      │
│                                          │
│                                          │
│        No large files found              │  ← bodyMedium, TextMuted
│     Your storage is looking clean!       │  ← bodySmall, onSurfaceVariant
│                                          │
│                                          │
└──────────────────────────────────────────┘
```

---

## 5. Komponentit

### 5.1 FileListItem

```
Row(48dp+ korkeus, clickable = toggle selection) {
    Checkbox(primary-väri)
    Spacer(8dp)
    // Thumbnail tai IconCircle
    Box(48dp × 48dp, RoundedCornerShape(8dp)) {
        if (hasThumbnail) Image(thumbnail)
        else IconCircle(categoryIcon, categoryColor, size=48dp)
    }
    Spacer(12dp)
    Column(weight(1f)) {
        Text(displayName, titleSmall, onSurface, maxLines=1, ellipsis)
        Text("$category · $date", bodySmall, onSurfaceVariant)
        MiniBar(progress = fileSize / maxFileSize, height=3dp)
    }
    Text(formattedSize, bodyMedium, numericFontFamily, onSurface)
}
```

**Thumbnailit:**
- Kuvat ja videot: `ContentResolver.loadThumbnail(uri, Size(96,96), null)` (API 29+)
- `ThumbnailLoader` luokka jossa LRU-cache (50 kuvaa)
- Ladataan `LaunchedEffect`-coroutinessa
- Fallback: `IconCircle` kategoriavärillä

**MiniBar per rivi:**
- Korkeus 3dp (pienempi kuin normaali)
- Pienin tiedosto = lyhyin palkki, suurin = täysi leveys
- Accent-väri tiedostotyypin mukaan

**Kategoria-ikonit ja -värit (IconCircle-fallback):**

| Kategoria | Icon | Väri |
|-----------|------|------|
| Video | `Videocam` | AccentBlue |
| Image | `Image` | AccentTeal |
| Audio | `MusicNote` | AccentOrange |
| Document | `Description` | AccentLime |
| Download | `Download` | AccentYellow |
| APK | `Android` | AccentLime |

**Swipe-to-delete:**
- `SwipeToDismissBox` Material 3
- Pyyhkäisy vasemmalle → punainen tausta + Delete-ikoni
- Vahvistus: `createDeleteRequest` yksittäiselle URI:lle (API 30+)
- API 29: suora `contentResolver.delete()`

### 5.2 CategoryGroup

```
Column {
    // Header
    Row(clickable = toggle expanded, height=48dp) {
        StatusDot(categoryColor)
        Spacer(8dp)
        Icon(if (expanded) ExpandMore else ChevronRight)
        Text("Videos", titleSmall, onSurface)
        Text("(5)", bodySmall, TextMuted)
        Spacer(weight)
        Text("3.1 GB", bodySmall, numericFontFamily, onSurfaceVariant)
        Spacer(8dp)
        Checkbox("All", onToggleAll)
    }
    // Content (AnimatedVisibility)
    if (expanded) {
        files.forEach { file -> FileListItem(file) }
    }
}
```

- Isoin ryhmä oletuksena auki
- Muut kiinni (collapsible)
- `AnimatedVisibility(expandVertically)` avautumiselle
- "All" checkbox valitsee/poistaa kaikki ryhmän tiedostot

### 5.3 CleanupBottomBar

```
Surface(
    surfaceContainer tausta,
    yläreuna: divider outlineVariant 0.35f
) {
    Column(padding = base) {
        // Ennuste
        Row {
            Text("67%", bodySmall, numericFont, onSurfaceVariant)
            Text(" → ", bodySmall, TextMuted)
            Text("59%", bodySmall, numericFont, AccentTeal)
        }
        MiniBar(
            progress = projectedPercent / 100f,
            height = 4dp,
            progressColor = statusColorForStoragePercent(projected)
        )
        Spacer(8dp)
        // Delete-painike
        Button(
            onClick = onDelete,
            colors = error/onError,
            fillMaxWidth
        ) {
            Text("Free $formattedSize · $count items")
        }
    }
}
```

- `AnimatedVisibility(slideInVertically)` kun valittu > 0
- `slideOutVertically` kun valittu = 0
- Painikkeen väri: `error` (punainen) koska tuhoaa dataa
- Teksti: "Free X GB · N items" — positiivinen kehystys ("free" ei "delete")

### 5.4 CleanupSuccessOverlay

```
AnimatedVisibility(
    visible = showSuccess,
    enter = fadeIn(200ms),
    exit = fadeOut(300ms)
) {
    Box(fillMaxSize, surfaceContainer.copy(alpha=0.95f), center) {
        Column(center) {
            Icon(
                Icons.Outlined.CheckCircle,
                tint = AccentTeal,
                size = 64dp
            )
            Spacer(16dp)
            Text(
                "$formattedSize freed",
                titleLarge, numericFontFamily, onSurface
            )
        }
    }
}
```

- Koko näkymän päällä oleva overlay
- LaunchedEffect: `delay(1800)` → hide → re-scan

---

## 6. Delete-mekanismi

### StorageCleanupHelper

```kotlin
@Singleton
class StorageCleanupHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // API 30+: batch-poisto järjestelmädialogin kautta
    fun createDeleteRequest(uris: List<Uri>): PendingIntent {
        return MediaStore.createDeleteRequest(context.contentResolver, uris)
    }

    // API 29: yksitellen, ei järjestelmädialogia
    suspend fun deleteLegacy(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        uris.forEach { uri ->
            try {
                if (context.contentResolver.delete(uri, null, null) > 0) deleted++
            } catch (_: SecurityException) { }
        }
        deleted
    }
}
```

### Flow composablessa

```kotlin
// CleanupScreen.kt
val deleteLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartIntentSenderForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        viewModel.onDeleteConfirmed()
    } else {
        viewModel.onDeleteCancelled()
    }
}

// Kun ViewModel emittoi delete-requestin:
LaunchedEffect(pendingDeleteIntent) {
    pendingDeleteIntent?.let { intent ->
        deleteLauncher.launch(
            IntentSenderRequest.Builder(intent.intentSender).build()
        )
    }
}
```

### Trash-tyhjennys (StorageDetailScreen, ei navigaatiota)

```
Käyttäjä painaa "Empty trash" ActionCardissa
→ StorageViewModel.emptyTrash()
→ MediaStoreScanner hae trashed URIt
→ StorageCleanupHelper.createDeleteRequest(uris)
→ Side-effect → StorageDetailScreen catches it
→ ActivityResultLauncher → järjestelmädialogi
→ Onnistuminen → refresh storage state
```

---

## 7. ThumbnailLoader

```kotlin
@Singleton
class ThumbnailLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cache = LruCache<Uri, ImageBitmap>(50)

    suspend fun loadThumbnail(uri: Uri, sizePx: Int = 96): ImageBitmap? =
        withContext(Dispatchers.IO) {
            cache.get(uri)?.let { return@withContext it }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
            try {
                val bitmap = context.contentResolver.loadThumbnail(
                    uri, android.util.Size(sizePx, sizePx), null
                )
                val imageBitmap = bitmap.asImageBitmap()
                cache.put(uri, imageBitmap)
                imageBitmap
            } catch (_: Exception) { null }
        }
}
```

- LRU cache 50 pikkukuvaa (riittää näkyviin riveihin + puskuri)
- `LaunchedEffect(file.uri)` lataa per tiedosto
- Alle API 29: ei pikkukuvia, käytetään aina IconCircle-fallbackia

---

## 8. Navigaatio

### Screen-luokka

```kotlin
// Screen.kt
data class Cleanup(val type: String) : Screen("cleanup/{type}") {
    companion object {
        fun route(type: CleanupType) = "cleanup/${type.name}"
    }
}
```

### NavGraph

```kotlin
composable(
    route = "cleanup/{type}",
    arguments = listOf(navArgument("type") { type = NavType.StringType })
) { backStackEntry ->
    val typeName = backStackEntry.arguments?.getString("type") ?: return@composable
    val type = try { CleanupType.valueOf(typeName) } catch (_: Exception) { return@composable }
    CleanupScreen(
        cleanupType = type,
        onBack = { navController.popBackStack() }
    )
}
```

### StorageDetailScreen ActionCardien onClick

```kotlin
ActionCard(
    ...
    onAction = { onNavigateToCleanup(CleanupType.LARGE_FILES) }
)
```

`StorageDetailScreen` saa uuden parametrin: `onNavigateToCleanup: (CleanupType) -> Unit`

---

## 9. Merkkijonot

### EN

```xml
<string name="cleanup_large_files_title">Large Files</string>
<string name="cleanup_old_downloads_title">Old Downloads</string>
<string name="cleanup_apk_files_title">APK Files</string>
<string name="cleanup_found_files">Found %1$d files · %2$s</string>
<string name="cleanup_no_files">No files found</string>
<string name="cleanup_no_files_desc">Your storage is looking clean!</string>
<string name="cleanup_free_action">Free %1$s · %2$d items</string>
<string name="cleanup_freed">%1$s freed</string>
<string name="cleanup_select_all">All</string>
<string name="cleanup_filter_10mb">10 MB</string>
<string name="cleanup_filter_50mb">50 MB</string>
<string name="cleanup_filter_100mb">100 MB</string>
<string name="cleanup_filter_500mb">500 MB</string>
<string name="cleanup_filter_30d">30 days</string>
<string name="cleanup_filter_60d">60 days</string>
<string name="cleanup_filter_90d">90 days</string>
<string name="cleanup_filter_1y">1 year</string>
<string name="cleanup_confirm_delete">Delete %1$d files?</string>
<string name="cleanup_confirm_delete_desc">This cannot be undone</string>
```

### FI

```xml
<string name="cleanup_large_files_title">Isot tiedostot</string>
<string name="cleanup_old_downloads_title">Vanhat lataukset</string>
<string name="cleanup_apk_files_title">APK-tiedostot</string>
<string name="cleanup_found_files">Löytyi %1$d tiedostoa · %2$s</string>
<string name="cleanup_no_files">Tiedostoja ei löytynyt</string>
<string name="cleanup_no_files_desc">Tallennustilasi näyttää siistiltä!</string>
<string name="cleanup_free_action">Vapauta %1$s · %2$d kohdetta</string>
<string name="cleanup_freed">%1$s vapautettu</string>
<string name="cleanup_select_all">Kaikki</string>
<string name="cleanup_filter_10mb">10 MB</string>
<string name="cleanup_filter_50mb">50 MB</string>
<string name="cleanup_filter_100mb">100 MB</string>
<string name="cleanup_filter_500mb">500 MB</string>
<string name="cleanup_filter_30d">30 päivää</string>
<string name="cleanup_filter_60d">60 päivää</string>
<string name="cleanup_filter_90d">90 päivää</string>
<string name="cleanup_filter_1y">1 vuosi</string>
<string name="cleanup_confirm_delete">Poistetaanko %1$d tiedostoa?</string>
<string name="cleanup_confirm_delete_desc">Tätä ei voi kumota</string>
```

---

## 10. Toteutusjärjestys

| Vaihe | Tehtävä | Riippuvuudet |
|-------|---------|--------------|
| 1 | `CleanupType.kt` — enum + filtterioptiot | Ei |
| 2 | `ThumbnailLoader.kt` — pikkukuvalataus + cache | Ei |
| 3 | `StorageCleanupHelper.kt` — delete wrapper | Ei |
| 4 | `CleanupUiState.kt` — tilamallit | Vaihe 1 |
| 5 | `CleanupViewModel.kt` — skannaus + valinta + poisto | Vaiheet 1-4 |
| 6 | `FileListItem.kt` — tiedostorivi thumbnail + MiniBar | Vaihe 2 |
| 7 | `CategoryGroup.kt` — collapsible ryhmä | Vaihe 6 |
| 8 | `CleanupBottomBar.kt` — sticky ennuste + delete | Ei |
| 9 | `CleanupSuccessOverlay.kt` — onnistumisanimaatio | Ei |
| 10 | `CleanupScreen.kt` — kokoa kaikki yhteen | Vaiheet 5-9 |
| 11 | NavGraph + Screen -päivitys | Vaihe 10 |
| 12 | StorageDetailScreen — ActionCard navigaatio | Vaihe 11 |
| 13 | StorageViewModel — trash empty -toiminto | Vaihe 3 |
| 14 | Merkkijonot EN + FI | Ei |

Vaiheet 1-4 + 14 ensin (data + tilat + stringit). Sitten 5-9 rinnakkain. Lopuksi 10-13 integraatio.
