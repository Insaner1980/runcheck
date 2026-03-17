# RunCheck Pro: Implementation Prompt for Claude Code

## Context

RunCheck is an Android-native device health monitoring app (Kotlin, Jetpack Compose, Material Design 3). It combines battery, network, thermal, and storage diagnostics into a single dashboard with a health score. The app is ad-free in Pro, privacy-first (no analytics, no accounts, no cloud sync).

This task implements the full Pro monetization system: a 7-day visible reverse trial, a single one-time in-app purchase via Google Play Billing, ad integration for the free tier, and all related UI components.

## Architecture Requirements

- All Pro state must be stored locally (no server, no accounts)
- Use DataStore (Preferences) for trial state and purchase status
- Use Google Play Billing Library 7.x for IAP
- All dates/times must use device clock with tampering protection (compare against last known timestamp — if clock goes backward, freeze trial)
- Pro features must degrade gracefully (locked features show a consistent "Pro" badge and upgrade prompt, never crash)

---

## 1. Pro State Management

Create a `ProManager` singleton (or Hilt-injected class) that serves as the single source of truth for Pro status across the entire app.

### State model

```
enum class ProStatus {
    TRIAL_ACTIVE,    // Within 7-day trial period
    TRIAL_EXPIRED,   // Trial ended, not purchased
    PRO_PURCHASED    // Purchased
}

data class ProState(
    val status: ProStatus,
    val trialDaysRemaining: Int,      // 7 down to 0
    val trialStartTimestamp: Long,     // epoch millis, set on first app launch
    val purchaseTimestamp: Long?       // epoch millis of purchase
)
```

### Storage (DataStore keys)

- `trial_start_timestamp` — Long, set once on first ever app launch, never overwritten
- `last_known_timestamp` — Long, updated every app launch for clock tampering detection
- `pro_purchased` — Boolean
- `purchase_timestamp` — Long (nullable)

### Clock tampering protection

On every app launch:
1. Read `last_known_timestamp`
2. If current time < last_known_timestamp by more than 1 hour, freeze trial (treat as expired)
3. Otherwise, update `last_known_timestamp` to current time

### ProManager API

```kotlin
// Expose as StateFlow for Compose observation
val proState: StateFlow<ProState>

// Check specific feature access
fun hasFeature(feature: ProFeature): Boolean
fun isTrialActive(): Boolean
fun isPro(): Boolean

// Called on first launch
suspend fun initializeTrial()

// Called after successful purchase
suspend fun activatePro()

// Called on app launch to verify purchase with Play Store
suspend fun verifyPurchase()
```

---

## 2. Pro Feature Gating

### Feature enum

```kotlin
enum class ProFeature {
    EXTENDED_HISTORY,       // Beyond 24-hour history
    CHARGER_COMPARISON,     // Compare charging speeds across chargers
    PER_APP_BATTERY,        // Per-app battery usage breakdown
    WIDGETS,                // Home screen widgets
    CSV_EXPORT,             // Export data to CSV
    THERMAL_LOGS,           // Thermal throttling log history
    AD_FREE                 // Remove banner ads
}
```

### Gating rules

- During TRIAL_ACTIVE: ALL ProFeatures are unlocked
- During TRIAL_EXPIRED: ALL ProFeatures are locked
- During PRO_PURCHASED: ALL ProFeatures are unlocked
- Free tier always includes: full diagnostics dashboard, health score, 24-hour history, all real-time monitoring

### UI gating pattern

Create a reusable composable wrapper:

```kotlin
@Composable
fun ProGated(
    feature: ProFeature,
    proState: ProState,
    lockedContent: @Composable () -> Unit = { ProLockedOverlay(feature) },
    content: @Composable () -> Unit
)
```

When locked, show the feature area with a frosted/blurred overlay and a lock icon + "Unlock with Pro" button that navigates to the upgrade screen.

---

## 3. Seven-Day Visible Reverse Trial

### Trial initialization

- On first app launch ever, set `trial_start_timestamp` to System.currentTimeMillis()
- Show a welcome bottom sheet (not a blocking dialog) explaining the trial

### Trial touchpoints (these are critical for conversion — implement all of them)

**Day 0 — Welcome bottom sheet (on first launch):**
- Headline: "Welcome to RunCheck Pro Trial"
- Body: "You have 7 days of full Pro access. All premium features are unlocked."
- Show a checklist of Pro features with checkmark icons:
  - ✓ Extended battery history (beyond 24 hours)
  - ✓ Charger speed comparison
  - ✓ Per-app battery usage
  - ✓ Home screen widgets
  - ✓ CSV data export
  - ✓ Thermal throttling logs
  - ✓ Ad-free experience
- Single dismiss button: "Start Exploring"
- Do NOT show a purchase button here — too early

**Days 1–5 — Home screen trial card:**
- Persistent card at the top of the home/dashboard screen
- Show: "Pro Trial — X days remaining" with a subtle progress bar (7 segments, filled segments = days elapsed)
- Tapping the card opens the upgrade screen
- Card uses a distinct but non-aggressive color (e.g., the app's accent color at low opacity)

**Day 5 — In-app highlight notification:**
- When the user opens the app on day 5 or later (if not yet shown):
- Show a dismissible banner/snackbar at the top: "Your battery history now has 5 days of data. Keep tracking with Pro."
- This is shown ONCE, tracked via DataStore boolean `day5_prompt_shown`

**Day 6 — Urgency card:**
- The home screen trial card changes appearance: border becomes warning color (amber/orange)
- Text changes to: "Pro Trial ends tomorrow"
- Add a "Keep Pro" button directly on the card

**Day 7 (expiration day) — Full-screen modal:**
- When the user opens the app after trial expiration:
- Show a full-screen dismissible modal (NOT a dialog — a full Composable screen with a close button)
- Headline: "Your Pro Trial Has Ended"
- Show what they're losing with concrete data if available:
  - "X days of battery history will be locked"
  - "Charger comparison data will be locked"
  - "Ads will appear on detail screens"
- Show the purchase button with price (see Section 4)
- Dismiss button at the bottom: "Continue with Free"
- This modal is shown ONCE per app session until purchased, tracked via in-memory flag (not persisted — show again next session)

**Post-expiration — Persistent home card:**
- The trial card transforms into a permanent (dismissible) upgrade card:
- "Your Pro trial ended. Unlock Pro for €2.99"
- If dismissed, re-show after 7 days (track last_dismiss_timestamp)
- After 3 dismissals, stop showing the card permanently

### Scheduled notifications (optional but recommended)

If the user has granted notification permission:
- Day 5: "Your RunCheck Pro trial ends in 2 days. You've tracked X charge cycles so far."
- Day 7: "Your Pro trial ended today. Upgrade to keep your extended history and ad-free experience."

Use WorkManager for scheduling. Do NOT send more than these two notifications total.

---

## 4. Google Play Billing — One-Time IAP

### Product ID (configure in Google Play Console)

| Product ID | Display Name | Price (base) | Description |
|---|---|---|---|
| `runcheck_pro` | RunCheck Pro | €2.99 | Unlock all Pro features |

### Pricing roadmap (all changes made in Play Console, not in code)

The app must NEVER hardcode price strings. Always fetch the current price dynamically from BillingClient and display whatever Google returns (already localized to the user's currency).

**Stage 1 — Launch (first 3 weeks):** Set price to **€1.99** in Play Console. Communicate "Launch price" in Play Store description and changelog to set expectations that price will rise.

**Stage 2 — Post-launch (after ~3 weeks):** Raise to **€2.99** in Play Console. This is the conservative base price while the app builds reviews and social proof.

**Stage 3 — Optimization (after accumulating 100+ reviews):** Use Google Play Console's built-in price experiments to A/B test €2.99 vs €3.99 vs €4.99. Research data shows €4.99 may generate more total revenue (higher price × more motivated buyers), but only works with enough reviews to justify premium positioning. Price experiments require no code changes — Play Console handles the randomization and statistical significance tracking.

### Implementation

Use Google Play Billing Library 7.x with the following flow:

1. **BillingClient setup:** Initialize in Application class or a Hilt module. Use `BillingClient.newBuilder()` with `enablePendingPurchases()`.

2. **Product query:** On upgrade screen load, query `runcheck_pro` using `queryProductDetailsAsync()` for `ProductType.INAPP`. Display the price returned by Google (already localized to the user's currency).

3. **Purchase flow:** Launch `billingClient.launchBillingFlow()` with the ProductDetails. Handle the result in `onPurchasesUpdated()`.

4. **Purchase verification:** After successful purchase:
   - Call `billingClient.acknowledgePurchase()` (required within 3 days or the purchase is refunded)
   - Update ProManager: `proManager.activatePro()`
   - Show a thank-you bottom sheet with confetti or a subtle animation

5. **Purchase restoration:** On every app launch, call `billingClient.queryPurchasesAsync(ProductType.INAPP)` to verify existing purchases. If a purchase exists but local state shows `pro_purchased = false`, restore it. This handles reinstalls and device switches.

6. **Error handling:** Handle all BillingResponseCode values gracefully. Show user-friendly messages for common cases:
   - `USER_CANCELED` — no message needed
   - `ITEM_ALREADY_OWNED` — restore purchase and show Pro
   - `SERVICE_UNAVAILABLE` / `BILLING_UNAVAILABLE` — "Google Play is unavailable. Please try again later."
   - `ERROR` / `DEVELOPER_ERROR` — "Something went wrong. Please try again."

### Upgrade screen UI

Create a dedicated full-screen Composable (`ProUpgradeScreen`) accessible from:
- Trial home card tap
- Settings → "Upgrade to Pro"
- Any ProGated locked overlay tap
- Day 7 expiration modal

Layout:
- Header: "Unlock RunCheck Pro"
- Feature list with icons (same as trial welcome, but with brief descriptions)
- Single prominent purchase button showing the price fetched from BillingClient
- Below the button: "One-time purchase. No subscription. Pay once, own it forever."
- If on launch pricing period: the Play Store description and changelog should mention "Launch price: €1.99 (regular €2.99)" but the app itself just shows whatever price BillingClient returns

---

## 5. Banner Ad Integration (Free Tier Only)

### Ad placement rules

- Show a standard AdMob banner (320×50 or adaptive banner) ONLY on detail/secondary screens:
  - Battery detail screen
  - Network detail screen
  - Thermal detail screen
  - Storage detail screen
- NEVER show ads on:
  - The main dashboard/home screen
  - The Settings screen
  - The upgrade/purchase screen
  - Any modal, bottom sheet, or dialog
- Ads are hidden when `proState.status` is `TRIAL_ACTIVE` or `PRO_PURCHASED`

### Implementation

- Use Google Mobile Ads SDK (AdMob)
- Initialize in Application class: `MobileAds.initialize(this)`
- Create a reusable `@Composable AdBanner()` component using `AndroidView` wrapping `AdView`
- Use adaptive banner size for best fill rate: `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize()`
- Place the ad at the bottom of detail screens, above any bottom navigation
- Handle ad loading failures silently (show empty space, do not show error messages)
- Use test ad unit IDs during development: `ca-app-pub-3940256099942544/6300978111`

### Ad unit ID configuration

Store the production ad unit ID in `local.properties` or a BuildConfig field (not hardcoded in source). Use the test ID for debug builds:

```kotlin
val adUnitId = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/6300978111" // Google test banner
} else {
    BuildConfig.ADMOB_BANNER_ID
}
```

---

## 6. File Structure

Expected new/modified files:

```
app/src/main/java/com/runcheck/
├── pro/
│   ├── ProManager.kt              // Single source of truth for Pro state
│   ├── ProState.kt                // Data classes and enums
│   ├── ProFeature.kt              // Feature enum
│   ├── TrialManager.kt            // Trial countdown logic, clock protection
│   └── BillingManager.kt          // Google Play Billing wrapper
├── ui/
│   ├── pro/
│   │   ├── ProUpgradeScreen.kt    // Full upgrade screen
│   │   ├── ProGated.kt            // Reusable gating composable
│   │   ├── ProLockedOverlay.kt    // Locked feature overlay
│   │   ├── TrialWelcomeSheet.kt   // Day 0 bottom sheet
│   │   ├── TrialHomeCard.kt       // Home screen trial/upgrade card
│   │   ├── TrialExpirationModal.kt // Day 7 full-screen modal
│   │   └── PurchaseThankYou.kt    // Post-purchase confirmation
│   └── ads/
│       └── AdBanner.kt            // Reusable AdMob banner composable
├── worker/
│   └── TrialNotificationWorker.kt // WorkManager for day 5 and day 7 notifications
```

---

## 7. Testing Checklist

Before considering this task complete, verify:

- [ ] First launch shows trial welcome sheet and starts 7-day countdown
- [ ] ProManager correctly reports TRIAL_ACTIVE for 7 days
- [ ] All Pro features are accessible during trial
- [ ] Home card shows correct days remaining and updates daily
- [ ] Day 5 banner appears once
- [ ] Day 6 card shows urgency styling
- [ ] After 7 days, ProManager reports TRIAL_EXPIRED
- [ ] Expiration modal appears on first post-expiration launch
- [ ] All Pro features are locked after trial (graceful degradation, no crashes)
- [ ] Ads appear on detail screens when trial expired and not purchased
- [ ] Ads do NOT appear during trial or after purchase
- [ ] Upgrade screen loads product details from Google Play (use test products in debug)
- [ ] Purchase flow completes and ProManager updates to PRO_PURCHASED
- [ ] Purchase persists across app restarts
- [ ] Purchase restores on fresh install (queryPurchasesAsync)
- [ ] Clock set backward freezes trial
- [ ] Post-expiration home card dismissal logic works (re-show after 7 days, stop after 3 dismissals)
- [ ] All navigation paths to upgrade screen work (card tap, settings, locked overlay, expiration modal)

---

## 8. Dependencies to Add

```kotlin
// build.gradle.kts (app)

// Google Play Billing
implementation("com.android.billingclient:billing-ktx:7.1.1")

// Google Mobile Ads (AdMob)
implementation("com.google.android.gms:play-services-ads:24.1.0")

// WorkManager (for trial notifications)
implementation("androidx.work:work-runtime-ktx:2.10.0")
```

Check for latest versions before adding.

---

## Important Notes

- Never hardcode prices — always fetch from BillingClient. The price will change over time (€1.99 → €2.99 → potentially €4.99) without any code changes. See "Pricing roadmap" in Section 4.
- Never use interstitial or video ads — banners only on detail screens
- Privacy: no analytics, no tracking, no network calls except Google Play Billing and AdMob
- All trial/purchase state is local-only via DataStore
