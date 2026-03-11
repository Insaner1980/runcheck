# Architecture & Layering Review

**Date:** 2026-03-11
**Scope:** data/ -> domain/ -> ui/ layering, Android framework isolation, feature vertical separation, service/monitor independence

---

## 1. Does the code follow data/ -> domain/ -> ui/ layering?

**Verdict: Partially — significant violations exist.**

The directory structure follows Clean Architecture conventions, but the dependency rules are not enforced. There are **two categories of violations**:

### A. Domain layer imports from data layer (CRITICAL)

The domain layer should be the innermost ring with zero outward dependencies. However, **every use case imports concrete data-layer classes**:

| Use Case | Violating Import(s) |
|----------|---------------------|
| `GetBatteryStateUseCase` | `data.battery.BatteryRepository` |
| `GetNetworkStateUseCase` | `data.network.NetworkRepository` |
| `GetThermalStateUseCase` | `data.thermal.ThermalRepository` |
| `GetStorageStateUseCase` | `data.storage.StorageRepository` |
| `GetBatteryHistoryUseCase` | `data.battery.BatteryRepository`, `data.billing.ProStatusRepository`, `data.db.entity.BatteryReadingEntity` |
| `GetSpeedTestHistoryUseCase` | `data.network.SpeedTestRepository` |
| `RunSpeedTestUseCase` | `data.network.SpeedTestRepository` |
| `ExportDataUseCase` | `data.db.dao.BatteryReadingDao`, `NetworkReadingDao`, `StorageReadingDao`, `ThermalReadingDao` |
| `CleanupOldReadingsUseCase` | 5 DAOs + `ProStatusRepository` |
| `GetThrottlingHistoryUseCase` | `data.db.dao.ThrottlingEventDao`, `data.db.entity.ThrottlingEventEntity` |
| `RecordThrottlingEventUseCase` | `data.db.dao.ThrottlingEventDao`, `data.db.entity.ThrottlingEventEntity` |
| `GetAppBatteryUsageUseCase` | `data.db.dao.AppBatteryUsageDao`, `data.db.entity.AppBatteryUsageEntity` |
| `ManageChargingSessionUseCase` | `data.db.dao.ChargerDao`, `data.db.entity.ChargingSessionEntity` |
| `GetChargerComparisonUseCase` | `data.db.dao.ChargerDao` |

**Root cause:** No repository interfaces exist in the domain layer. The data layer defines concrete repository classes and the domain layer imports them directly, inverting the intended dependency direction.

**Recommended fix:** Define repository interfaces in `domain/repository/` (e.g., `BatteryRepository`, `NetworkRepository`) and have the data layer implementations implement those interfaces. Use Hilt `@Binds` to wire concrete implementations to the interfaces. This also applies to DAO access — use cases should never touch DAOs directly; wrap them in repositories.

### B. UI layer imports from data layer (HIGH)

ViewModels and UI state classes bypass the domain layer by importing data-layer types directly:

**ViewModels with data-layer imports:**
- `BatteryViewModel` — `ProStatusRepository`
- `NetworkViewModel` — `ProStatusRepository`, `SpeedTestRepository`, `SpeedTestService`
- `ThermalViewModel` — `ProStatusRepository`, `ThrottlingEventEntity`
- `ChargerViewModel` — `ChargerDao`, `ChargerProfileEntity`
- `HomeViewModel` — `ProStatusRepository`
- `SettingsViewModel` — `ProStatusRepository`, `DeviceProfileRepository`, `UserPreferencesRepository`
- `DashboardViewModel` — `BatteryReadingEntity`

**UI state classes exposing data entities:**
- `BatteryUiState.Success` contains `BatteryReadingEntity`
- `ThermalUiState.Success` contains `ThrottlingEventEntity`
- `ChargerUiState.Success` contains `ChargingSessionEntity`
- `AppUsageUiState.Success` contains `AppBatteryUsageEntity`
- `SettingsUiState` contains `DeviceProfile`

**Root cause:** Room entities are used as display models instead of being mapped to domain/UI models. `ProStatusRepository` has no domain-layer abstraction.

**Recommended fix:**
1. Map data entities to domain models within use cases before returning them to the UI.
2. Define a `ProStatusProvider` interface in domain and implement it in data.
3. Create UI-specific models (or reuse domain models) for state classes — never expose `@Entity`-annotated classes to composables.

---

## 2. Are Android framework dependencies confined to the data/ layer?

**Verdict: Yes, with justified exceptions.**

The domain layer has **zero Android framework imports** — this is correct and well-maintained.

The data layer properly concentrates all system service access:
- `BatteryManager` — `data/battery/` (4 source implementations + factory)
- `ConnectivityManager`, `TelephonyManager`, `WifiManager` — `data/network/NetworkDataSource.kt`
- `StorageStatsManager`, `StorageManager` — `data/storage/StorageDataSource.kt`
- `PowerManager` — `data/thermal/ThermalDataSource.kt`
- `DeviceCapabilityManager` — `data/device/`

**Justified exceptions outside data/:**

| File | Framework Import | Justification |
|------|-----------------|---------------|
| `SettingsViewModel` | `Context`, `MediaStore`, `Environment` | File export to Downloads via MediaStore — legitimate UI-initiated I/O |
| `Theme.kt` | `AccessibilityManager` | Checking `isReducedMotionEnabled` for animation — proper theme concern |
| `RealTimeMonitorService` | `Service`, `NotificationManager` | Inherent to Android Service lifecycle |
| `BootReceiver` | `BroadcastReceiver` | Inherent to boot-complete handling |
| `HealthWidget`, `BatteryWidget` | `BatteryManager` | Glance widgets must access framework directly |

None of these represent architecture violations — they are appropriate framework touchpoints for their respective layers.

---

## 3. Does each feature area have its own data source, domain model, and UI screen?

**Verdict: Yes — excellent vertical separation with no cross-feature contamination.**

Each of the four core features has a complete, independent vertical stack:

```
Battery:   BatteryDataSource -> BatteryRepository -> BatteryState -> GetBatteryStateUseCase -> BatteryViewModel -> BatteryDetailScreen
Network:   NetworkDataSource -> NetworkRepository -> NetworkState -> GetNetworkStateUseCase -> NetworkViewModel -> NetworkDetailScreen
Thermal:   ThermalDataSource -> ThermalRepository -> ThermalState -> GetThermalStateUseCase -> ThermalViewModel -> ThermalDetailScreen
Storage:   StorageDataSource -> StorageRepository -> StorageState -> GetStorageStateUseCase -> StorageViewModel -> StorageDetailScreen
```

Additional verticals (Charger, App Usage) also follow this pattern.

**No cross-feature contamination detected:**
- Battery code imports nothing from network/thermal/storage
- Network code imports nothing from battery/thermal/storage
- Thermal code imports nothing from battery/network/storage
- Storage code imports nothing from battery/network/thermal
- Each ViewModel injects only its own feature's use cases

**Cross-cutting concerns are handled correctly:**
- `CalculateHealthScoreUseCase` in `domain/scoring/` — appropriately shared
- `CleanupOldReadingsUseCase` — shared retention policy, uses all four DAOs (should be routed through repositories)
- `ExportDataUseCase` — shared export logic across all features
- `ProStatusRepository` — billing state used across features (needs domain abstraction)
- Shared enums in `domain/model/Enums.kt` — appropriate

---

## 4. Is service/monitor/ properly separated from UI logic?

**Verdict: Yes — fully independent.**

The `service/monitor/` package contains five classes, all cleanly separated:

### HealthMonitorWorker
- Injects four repositories + `CleanupOldReadingsUseCase` via Hilt
- Collects one reading from each source sequentially, saves, then cleans up
- **No UI imports whatsoever** — runs entirely in the background via WorkManager
- Can run independently of any Activity or Fragment lifecycle

### RealTimeMonitorService
- Pure foreground service for real-time monitoring notification
- Creates its own notification channel and notification
- No ViewModel, no composable, no UI state dependencies
- Started/stopped via `Intent` actions — fully decoupled from UI

### MonitorScheduler
- WorkManager scheduling utility — enqueues `HealthMonitorWorker`
- No UI dependencies

### BootReceiver
- Reschedules monitoring after device reboot
- Delegates to `MonitorScheduler` — no UI coupling

### NotificationHelper
- Centralized notification infrastructure
- Injected via Hilt singleton — no UI dependencies

**The service layer can run entirely independently of the UI.** There is no import of any `ui/` package class from any `service/monitor/` file.

---

## Summary of Findings

| Question | Status | Key Issue |
|----------|--------|-----------|
| data/ -> domain/ -> ui/ layering | **VIOLATED** | Domain imports concrete data classes; no repository interfaces in domain |
| Android framework isolation | **GOOD** | Framework deps properly in data/, justified exceptions elsewhere |
| Feature vertical separation | **EXCELLENT** | Complete stacks per feature, zero cross-contamination |
| Service/monitor independence | **EXCELLENT** | Fully decoupled from UI, runs independently |

### Priority Fixes

1. **HIGH — Introduce domain-layer repository interfaces.** Move repository contracts to `domain/repository/`, have data-layer classes implement them, and bind via Hilt `@Binds`. This is the single change that fixes the majority of violations.

2. **HIGH — Map data entities to domain models.** Use cases should return domain models, not `@Entity` classes. Add mapping functions in repositories or use cases.

3. **MEDIUM — Abstract ProStatusRepository.** Define a `ProStatusProvider` interface in domain. Multiple ViewModels and use cases depend on it.

4. **LOW — Remove direct DAO access from use cases.** `ExportDataUseCase`, `CleanupOldReadingsUseCase`, `GetThrottlingHistoryUseCase`, etc. should go through repository abstractions.

5. **LOW — Move SettingsViewModel file export to a data-layer class.** The `Context`/`MediaStore` usage for export could be extracted to a `FileExportRepository` in the data layer.
