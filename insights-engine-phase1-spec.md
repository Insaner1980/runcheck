# Insights Engine — Implementation Spec (Phase 1)

Cross-category correlation engine that analyzes existing Room data to generate actionable, personalized insights. Phase 1 establishes the full architecture with one rule and a debug data seeder so the feature is immediately testable without accumulated real data.

---

## Guiding Principles

1. **Confidence-first.** Every insight has a confidence score. If the underlying data is unreliable or insufficient, the insight is suppressed — not shown with a caveat.
2. **On-device only.** All computation happens locally via Room queries + Kotlin logic. No cloud, no ML.
3. **Additive.** New `insights` package reads from existing repositories. Does not modify them.
4. **Pro-gated with free teaser.** Pro users see all insights. Free users see a count and the top insight title, but not the detail.
5. **Immediately testable.** Debug builds include a data seeder so insights can be verified without days of real usage.

---

## Phase 1 Scope

**In scope:**
- Full architecture (entity, DAO, domain model, rule interface, engine, repository, Hilt wiring)
- One rule: BatteryDegradationTrendRule (single-table, no cross-table joins)
- InsightsCard on Home screen (Pro and free views)
- InsightGenerationWorker (standalone, not chained)
- Debug test data seeder
- Room migration

**Explicitly out of scope (phase 2+):**
- Cross-table correlation rules (ThermalDrainCorrelation, ChargerTemperature, NetworkDrainCorrelation)
- StorageTrendRule (linear regression)
- BaselineAnomalyRule (Z-score per category)
- Insight deep-linking to detail screens
- Notification-based insights
- Insight history/archive view

---

## Architecture

```
data/insights/
├── InsightRepository.kt
├── InsightEntity.kt
├── InsightDao.kt
└── debug/
    └── InsightTestDataSeeder.kt     ← DEBUG ONLY

domain/insights/
├── model/
│   ├── Insight.kt
│   ├── InsightType.kt
│   └── InsightPriority.kt
├── engine/
│   ├── InsightEngine.kt
│   └── InsightRule.kt
└── rules/
    └── BatteryDegradationTrendRule.kt

ui/home/insights/
├── InsightsCard.kt
└── InsightRow.kt

worker/
└── InsightGenerationWorker.kt

di/
└── InsightsModule.kt
```

---

## Data Layer

### InsightEntity

```kotlin
@Entity(tableName = "insights")
data class InsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: String,              // e.g. "battery_degradation_trend"
    val type: String,                // InsightType enum name
    val priority: String,            // HIGH / MEDIUM / LOW
    val confidence: Float,           // 0.0–1.0
    val titleResId: String,          // String resource name for the title
    val bodyTemplate: String,        // String resource name for the body template
    val bodyArgs: String,            // JSON array of substitution values: ["25", "7"]
    val generatedAt: Long,           // System.currentTimeMillis()
    val expiresAt: Long,             // When this insight becomes stale
    val dataWindowStart: Long,       // Start of analyzed data range
    val dataWindowEnd: Long,         // End of analyzed data range
    val dismissed: Boolean = false,
    val seen: Boolean = false
)
```

### InsightDao

```kotlin
@Dao
interface InsightDao {
    @Query("""
        SELECT * FROM insights 
        WHERE dismissed = 0 AND expiresAt > :now 
        ORDER BY priority ASC, confidence DESC 
        LIMIT :limit
    """)
    fun getActiveInsights(now: Long, limit: Int = 10): Flow<List<InsightEntity>>

    @Query("""
        SELECT COUNT(*) FROM insights 
        WHERE dismissed = 0 AND seen = 0 AND expiresAt > :now
    """)
    fun getUnseenCount(now: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(insights: List<InsightEntity>)

    @Query("DELETE FROM insights WHERE expiresAt < :now")
    suspend fun deleteExpired(now: Long)

    @Query("DELETE FROM insights WHERE ruleId = :ruleId")
    suspend fun deleteByRule(ruleId: String)

    @Query("UPDATE insights SET dismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: Long)

    @Query("UPDATE insights SET seen = 1 WHERE dismissed = 0 AND seen = 0")
    suspend fun markAllSeen()

    @Query("DELETE FROM insights")
    suspend fun deleteAll()
}
```

Priority sorts ASC because HIGH = 0, MEDIUM = 1, LOW = 2 (enum ordinal).

### InsightRepository

```kotlin
class InsightRepository @Inject constructor(
    private val insightDao: InsightDao
) {
    fun getActiveInsights(limit: Int = 10): Flow<List<Insight>> =
        insightDao.getActiveInsights(now = System.currentTimeMillis(), limit = limit)
            .map { entities -> entities.map { it.toDomain() } }

    fun getUnseenCount(): Flow<Int> =
        insightDao.getUnseenCount(now = System.currentTimeMillis())

    suspend fun dismiss(id: Long) = insightDao.dismiss(id)

    suspend fun markAllSeen() = insightDao.markAllSeen()

    suspend fun deleteAll() = insightDao.deleteAll()
}
```

The `toDomain()` mapper converts entity to domain model, resolving `titleResId` and `bodyTemplate` into `UiText.Resource` with the parsed `bodyArgs`.

---

## Domain Layer

### Insight (domain model)

```kotlin
data class Insight(
    val id: Long,
    val ruleId: String,
    val type: InsightType,
    val priority: InsightPriority,
    val confidence: Float,
    val title: UiText,
    val body: UiText,
    val generatedAt: Long,
    val seen: Boolean
)
```

### InsightType

```kotlin
enum class InsightType {
    BATTERY,
    THERMAL,
    NETWORK,
    STORAGE,
    CROSS_CATEGORY
}
```

### InsightPriority

```kotlin
enum class InsightPriority {
    HIGH,    // ordinal 0 — sorted first
    MEDIUM,  // ordinal 1
    LOW      // ordinal 2
}
```

### InsightRule

```kotlin
interface InsightRule {
    val ruleId: String
    val minimumDataPoints: Int
    val insightTtl: Duration

    /**
     * Evaluate this rule against current data.
     * Returns empty list if data is insufficient or confidence too low.
     * Must never throw — return emptyList on any error.
     */
    suspend fun evaluate(): List<InsightEntity>
}
```

### InsightEngine

```kotlin
class InsightEngine @Inject constructor(
    private val rules: Set<@JvmSuppressWildcards InsightRule>,
    private val insightDao: InsightDao
) {
    suspend fun generateInsights() {
        val now = System.currentTimeMillis()
        insightDao.deleteExpired(now)

        val newInsights = rules.flatMap { rule ->
            try {
                rule.evaluate().filter { it.confidence >= MINIMUM_CONFIDENCE }
            } catch (e: Exception) {
                // Log but never crash — a failing rule must not block others
                emptyList()
            }
        }

        val rulesWithResults = newInsights.map { it.ruleId }.distinct()
        rulesWithResults.forEach { ruleId ->
            insightDao.deleteByRule(ruleId)
        }

        if (newInsights.isNotEmpty()) {
            insightDao.insertAll(newInsights)
        }
    }

    companion object {
        const val MINIMUM_CONFIDENCE = 0.6f
    }
}
```

---

## Phase 1 Rule: BatteryDegradationTrendRule

**What it detects:** Battery drain rate has gotten meaningfully worse this week compared to last week.

**Why this rule first:** It uses data from one table only (battery readings). No cross-table joins, no complex correlation logic. Simple average comparison that validates the entire architecture end-to-end.

**Logic:**
1. Query average drain rate (%/h) during screen-on time for the current 7-day window.
2. Query the same for the previous 7-day window.
3. If current drain is more than 15% higher than previous AND both windows have at least 20 readings each, generate an insight.
4. Confidence = `min(1.0f, min(currentCount, previousCount) / 40f)` — scales with data availability.

**Priority:** HIGH.

**TTL:** 24 hours.

**String resources needed:**
```xml
<string name="insight_battery_degradation_title">Battery draining faster</string>
<string name="insight_battery_degradation_body">Your battery has been draining %1$s%% faster this week compared to last week.</string>
```

**Example output:** "Your battery has been draining 25% faster this week compared to last week."

---

## Debug Test Data Seeder

This is critical for development. Without it, insights cannot be tested because the app is reinstalled frequently and never accumulates enough real data.

### InsightTestDataSeeder

```kotlin
/**
 * DEBUG ONLY. Generates realistic historical readings so insight rules
 * can be tested immediately after a fresh install.
 *
 * Accessible from Settings when BuildConfig.DEBUG is true.
 */
class InsightTestDataSeeder @Inject constructor(
    private val batteryReadingDao: BatteryReadingDao,
    private val insightEngine: InsightEngine
) {
    /**
     * Generates 14 days of battery readings and runs insight generation.
     * Call from a coroutine scope (e.g. viewModelScope in SettingsViewModel).
     */
    suspend fun seedAndGenerate() {
        seedBatteryReadings()
        insightEngine.generateInsights()
    }

    private suspend fun seedBatteryReadings() {
        // Generate readings every 15 minutes for 14 days
        // Week 1 (older): normal drain ~6%/h screen-on
        // Week 2 (recent): elevated drain ~8.5%/h screen-on (~40% increase)
        // This guarantees BatteryDegradationTrendRule fires with high confidence.
        //
        // Each reading should have realistic fields matching BatteryReadingEntity:
        // - timestamp spread across the 14 days
        // - level decreasing then resetting (simulating charge cycles)
        // - temperature varying 25-35°C
        // - screen-on flag alternating in realistic blocks
        //
        // Use a seeded Random (seed = 42) for reproducible test data.
    }
}
```

### Settings UI integration

Add a debug-only section at the bottom of Settings (only visible when `BuildConfig.DEBUG`):

```
Debug Tools
├── "Generate test data" button
│     Calls InsightTestDataSeeder.seedAndGenerate()
│     Shows a Toast/Snackbar on completion: "Test data generated — 14 days, X readings"
├── "Clear insights" button  
│     Calls InsightDao.deleteAll()
└── "Run insight engine" button
      Calls InsightEngine.generateInsights() manually
```

This gives full control during development:
1. Fresh install → open Settings → tap "Generate test data" → go to Home → insights visible.
2. Want to test empty state? → "Clear insights"
3. Want to re-run after tweaking a rule? → "Run insight engine"

---

## Worker

### InsightGenerationWorker

```kotlin
class InsightGenerationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val insightEngine: InsightEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        insightEngine.generateInsights()
        return Result.success()
    }
}
```

**Scheduling:** Register as a standalone periodic worker with a 6-hour repeat interval via the existing `MonitorScheduler`. Do NOT chain with `HealthMonitorWorker` or `HealthMaintenanceWorker` — keep it independent to avoid coupling. A 6-hour interval is sufficient because the rules analyze multi-day windows.

Add scheduling to `MonitorScheduler`:

```kotlin
fun scheduleInsightGeneration() {
    val request = PeriodicWorkRequestBuilder<InsightGenerationWorker>(6, TimeUnit.HOURS)
        .setInitialDelay(1, TimeUnit.MINUTES)
        .build()
    workManager.enqueueUniquePeriodicWork(
        "insight_generation",
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}
```

Call `scheduleInsightGeneration()` from `RuncheckApp` initialization alongside existing worker scheduling.

---

## UI: InsightsCard on Home

### Placement

Between the 2x2 quick status grid and the Quick Tools card.

### Pro user view

- Card background: `surfaceContainer` (BgCard), 16dp corners — matches existing cards.
- Header row: icon (use `AutoAwesome` or `Lightbulb` Material icon, AccentBlue) + "Insights" title (`titleMedium`).
- Unseen count badge: small AccentBlue dot next to title if unseenCount > 0.
- Content: up to 3 InsightRow items, ordered by priority then confidence.
- Empty state (no active insights): single line `bodySmall` in `onSurfaceVariant`: "No insights yet — keep using your device and check back later."
- "Mark all seen" triggers automatically when card becomes visible (LaunchedEffect on card visibility or collectAsState change).

### InsightRow

- Left: colored dot matching InsightType (BATTERY → AccentTeal, THERMAL → AccentOrange, etc.)
- Title: `bodyMedium`, `onSurface`, single line, ellipsize.
- Body: `bodySmall`, `onSurfaceVariant`, max 2 lines, ellipsize.
- Trailing: dismiss icon button (small X, 48dp touch target). Sets dismissed = true.
- No dividers between rows — use 8dp vertical spacing.

### Free user view

- Same card, same position.
- Header shows unseen count: "3 new insights" in `bodySmall` AccentBlue.
- Shows title of the highest-priority insight only (no body text).
- Below the title: `ProFeatureCalloutCard`-style inline prompt. Use the existing `ProFeatureCalloutCard` component — it already exists.
- This is the single strongest Pro conversion teaser in the app.

---

## Hilt Wiring

### InsightsModule

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class InsightsModule {

    @Binds
    @IntoSet
    abstract fun bindBatteryDegradationRule(
        rule: BatteryDegradationTrendRule
    ): InsightRule

    companion object {
        @Provides
        @Singleton
        fun provideInsightDao(database: AppDatabase): InsightDao =
            database.insightDao()

        @Provides
        @Singleton
        fun provideInsightRepository(insightDao: InsightDao): InsightRepository =
            InsightRepository(insightDao)
    }
}
```

Adding future rules = one new `@Binds @IntoSet` line each. No changes to engine or module companion.

---

## Localization

All user-facing text uses string resources via `UiText.Resource`.

The `bodyTemplate` field in `InsightEntity` stores a string resource name (e.g. `"insight_battery_degradation_body"`). The `bodyArgs` field stores a JSON array of computed values (e.g. `["25"]`). At display time, the UI resolves the template and substitutes the args positionally (`%1$s`, `%2$s`, etc.).

This means:
- Templates are translated like any other string resource.
- Numeric values are computed at insight generation time and stored as strings.
- No hardcoded English in generated insights.

---

## Privacy & Data Cleanup

- Insights are derived from data already on-device. No new data collection.
- Insights are never included in crash reports.
- Add `insightDao.deleteAll()` to the existing "Clear all data" flow in Settings.
- The `insights` table is included in Room's standard database clearing.

---

## Room Migration

Purely additive: one `CREATE TABLE insights (...)` statement. No changes to existing tables.

---

## Implementation Order for Claude Code

Execute these as a single implementation session:

1. **InsightEntity + InsightDao** — add to AppDatabase, write the Room migration.
2. **Insight domain model + InsightType + InsightPriority** — pure data classes.
3. **InsightRule interface + InsightEngine** — pure Kotlin, no Android deps.
4. **BatteryDegradationTrendRule** — the one rule. Queries BatteryReadingDao for average drain rates in two 7-day windows.
5. **InsightRepository** — thin wrapper around DAO, exposes Flow.
6. **InsightsModule** — Hilt wiring.
7. **InsightGenerationWorker + MonitorScheduler update** — schedule the 6-hour periodic worker.
8. **InsightTestDataSeeder** — generates 14 days of battery readings with deliberate week-2 degradation.
9. **Settings debug section** — three buttons: seed data, clear insights, run engine.
10. **InsightsCard + InsightRow on Home screen** — both Pro and free views.
11. **Wire "Clear all data" in Settings to also delete insights.**

After this is working end-to-end and verified with the test data seeder, phase 2 rules can be added one at a time as independent tasks.

---

## Phase 2 Rules (future, not in this spec)

For reference, these are the planned follow-up rules. Each is a separate Claude Code task after phase 1 is verified:

- **ThermalDrainCorrelationRule** — drain rate vs thermal state (cross-table: battery + thermal)
- **ChargerTemperatureRule** — temperature differences between chargers (cross-table: charger sessions + thermal)
- **StorageTrendRule** — linear regression on storage fill rate (single table)
- **NetworkDrainCorrelationRule** — signal strength vs drain rate (cross-table: battery + network)
- **BaselineAnomalyRule** — Z-score per category vs user's own historical baseline (per table, but needs rolling stats)
