# Settings Enhancements Spec

Asetukset-näkymän laajennus. Nykyinen on flat-lista perusasetuksista. Tavoite: ryhmitelty, visuaalinen, kattava.

---

## Nykyiset vs. uudet osiot

| Osio | Nyt | Lisätään |
|------|-----|----------|
| Monitoring | Interval-radio | Kategoriatogglat |
| Notifications | Yksi toggle | Per-alert togglet, master toggle |
| Alert Thresholds | Kovakoodattu | Sliderit (akku/lämpö/tallennustila) |
| Display | — | Lämpötilayksikkö, kotinäkymän kortit |
| Data | Retention + Export | Clear-toiminnot, formattioptio, datatila |
| Pro | Status | Restore purchase |
| Privacy | Crash toggle | Sama |
| Advanced | — | Sign convention, profile refresh |
| About | Version + linkit | What's New, Licenses, Feedback |

---

## UI-rakenne

Jokaiselle osiolle oma kortti (`SettingsCard` = sama pattern kuin BatteryPanel/StoragePanel). Kortit eroteltu `MaterialTheme.spacing.md` väleillä.

### Korttipohja

```
Card(
    surfaceContainer tausta,
    0dp elevation,
    16dp pyöristys,
    ei reunusta
) {
    Column(padding = base, spacing = sm) {
        SectionHeader(title)    ← kortin nimi
        ...rivit...
    }
}
```

Rivityypit kortin sisällä:

### SettingsToggleRow

```
┌──────────────────────────────────────────┐
│ Low Battery                        [ON]  │
│ Alert when battery drops below 20%       │
└──────────────────────────────────────────┘
```

- `Row(SpaceBetween, CenterVertically)`
- Vasen: `Column` { `titleSmall` + `bodySmall onSurfaceVariant` }
- Oikea: `Switch(primary)`
- Disabled: alpha 0.38f koko rivi
- Divider alla: `outlineVariant 0.35f`

### SettingsSliderRow

```
┌──────────────────────────────────────────┐
│ Low Battery Warning              20%     │
│ ━━━━━━━━━━●━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│ Alert when battery drops below           │
└──────────────────────────────────────────┘
```

- Yläosa: `Row(SpaceBetween)` { label `titleSmall` + arvo `bodyMedium numericFont primary` }
- Keskiosa: `Slider(primary track, surfaceVariant background)`
- Alaosa: kuvaus `bodySmall onSurfaceVariant`
- Arvo päivittyy reaaliajassa raahattaessa
- Thumb: `primary`-väri, 20dp

### SettingsNavigationRow

```
┌──────────────────────────────────────────┐
│ Export Format                   CSV   ›  │
└──────────────────────────────────────────┘
```

- `Row(SpaceBetween, clickable)`
- Vasen: label `titleSmall`
- Oikea: `Row` { arvo `bodyMedium onSurfaceVariant` + `ChevronRight 18dp` }
- Koko rivi clickable → avaa dialog tai navigoi

### SettingsRadioGroup

```
┌──────────────────────────────────────────┐
│ Temperature Unit                         │
│   ● Celsius (°C)                         │
│   ○ Fahrenheit (°F)                      │
└──────────────────────────────────────────┘
```

- `Column` + `Row(clickable)` per vaihtoehto
- `RadioButton(primary)` + label `bodyMedium`
- Koko rivi clickable (min 48dp korkeus)

### SettingsDangerRow

```
┌──────────────────────────────────────────┐
│ Clear All Data                        ›  │  ← error-väri
└──────────────────────────────────────────┘
```

- Teksti `error`-värillä
- Kosketus → AlertDialog:
  - Title: `titleMedium onSurface`
  - Body: `bodyMedium onSurfaceVariant`
  - Cancel: `TextButton(primary)`
  - Confirm: `Button(error containerColor, onError contentColor)`

---

## Osiot yksityiskohtaisesti

### 1. MONITORING

```
┌────────────────────────────────────────┐
│ ⬡ MONITORING                          │
│                                        │
│ Interval                    30 min  ›  │  ← dialog: 15/30/60
│ ──────────────────────────────────────  │
│ Battery                          [ON]  │
│ ──────────────────────────────────────  │
│ Network                          [ON]  │
│ ──────────────────────────────────────  │
│ Thermal                          [ON]  │
│ ──────────────────────────────────────  │
│ Storage                          [ON]  │
└────────────────────────────────────────┘
```

- Interval: SettingsNavigationRow → dialog `SingleChoiceDialog`
- Kategoriatogglat: SettingsToggleRow × 4
- Kun kategoria off → HealthMonitorWorker ohittaa sen, kotinäkymässä kortti piilotettu

### 2. NOTIFICATIONS

```
┌────────────────────────────────────────┐
│ ⬡ NOTIFICATIONS                       │
│                                        │
│ Notifications                    [ON]  │  ← master
│ ──────────────────────────────────────  │
│ Low Battery                      [ON]  │
│ Alert when battery is low              │
│ ──────────────────────────────────────  │
│ High Temperature                 [ON]  │
│ Alert when device overheats            │
│ ──────────────────────────────────────  │
│ Low Storage                      [ON]  │
│ Alert when storage is almost full      │
│ ──────────────────────────────────────  │
│ Charge Complete                 [OFF]  │
│ Notify when charging finishes          │
└────────────────────────────────────────┘
```

- Master-toggle ylhäällä
- Kun master OFF → muut rivit alpha 0.38f, Switch disabled
- Kuvaus jokaisessa rivissä `bodySmall onSurfaceVariant`

### 3. ALERT THRESHOLDS

```
┌────────────────────────────────────────┐
│ ⬡ ALERT THRESHOLDS                    │
│                                        │
│ Low Battery                      20%   │
│ ━━━━━━━━━━●━━━━━━━━━━━━━━━━━━━━━━━━━  │
│ ──────────────────────────────────────  │
│ High Temperature                 42°C  │
│ ━━━━━━━━━━━━━━━━●━━━━━━━━━━━━━━━━━━━  │
│ ──────────────────────────────────────  │
│ Low Storage                      90%   │
│ ━━━━━━━━━━━━━━━━━━━━●━━━━━━━━━━━━━━━  │
└────────────────────────────────────────┘
```

Slider-asetukset:

| Hälytys | Range | Steps | Oletus |
|---------|-------|-------|--------|
| Low Battery | 5–50% | 5% | 20% |
| High Temperature | 35–50°C | 1°C | 42°C |
| Low Storage | 70–99% | 5% | 90% |

- Slider track: `surfaceVariant 0.5f` (sama kuin ProgressRing track)
- Active track: `primary`
- Thumb: `primary`, 20dp
- Arvo oikealla: `numericFontFamily`, `primary`-väri

### 4. DISPLAY

```
┌────────────────────────────────────────┐
│ ⬡ DISPLAY                             │
│                                        │
│ Temperature Unit                       │
│   ● Celsius (°C)                       │
│   ○ Fahrenheit (°F)                    │
│ ──────────────────────────────────────  │
│ Home Screen                         ›  │
└────────────────────────────────────────┘
```

- Temperature Unit: SettingsRadioGroup
- Home Screen: navigoi erilliseen näkymään tai avaa bottom sheetin jossa korttien togglet

### 5. DATA

```
┌────────────────────────────────────────┐
│ ⬡ DATA                                │
│                                        │
│ Retention                 3 months  ›  │  ← PRO badge
│ ──────────────────────────────────────  │
│ Export                          CSV ›  │  ← PRO badge
│ ──────────────────────────────────────  │
│ runcheck Data                  12 MB   │  ← read-only
│ ──────────────────────────────────────  │
│ Clear Speed Tests                   ›  │
│ Clear All Monitoring Data           ›  │  ← error-väri
└────────────────────────────────────────┘
```

- Pro-ominaisuuksissa `ProBadgePill` rivin oikeassa reunassa
- "runcheck Data": `bodyMedium numericFont onSurfaceVariant` — ei clickable
- Clear-rivit: SettingsDangerRow

### 6. PRO

```
┌────────────────────────────────────────┐
│ ⬡ PRO                                 │
│                                        │
│ Status                      Active ✓   │  ← healthy-väri
│ ──────────────────────────────────────  │
│ Restore Purchase                    ›  │
└────────────────────────────────────────┘
```

Tai jos ei Pro:

```
┌────────────────────────────────────────┐
│ ⬡ PRO                                 │
│                                        │
│ ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓  │
│ ┃  Unlock Pro — €3.49              ┃  │  ← primary button
│ ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛  │
│ One-time purchase, no subscription     │
└────────────────────────────────────────┘
```

### 7. PRIVACY

```
┌────────────────────────────────────────┐
│ ⬡ PRIVACY                             │
│                                        │
│ Crash Reporting                 [OFF]  │
│ Help improve runcheck by sending       │
│ anonymous crash reports                │
└────────────────────────────────────────┘
```

### 8. ADVANCED (oletuksena piilotettu)

```
  Advanced                            ▸   ← TextButton, napauta avaa

┌────────────────────────────────────────┐  ← AnimatedVisibility
│                                        │
│ Current Sign Convention                │
│   ● Auto-detect                        │
│   ○ Positive = Charging                │
│   ○ Negative = Charging                │
│ ──────────────────────────────────────  │
│ Recalibrate Device Profile          ›  │
│ Last calibrated: Mar 15, 2026          │
└────────────────────────────────────────┘
```

- "Advanced ▸" on `TextButton(onSurfaceVariant)` korttien ulkopuolella
- Napauta → `AnimatedVisibility(expandVertically)` + nuoli ▸ → ▾
- Kuvaus "Recalibrate"-rivin alla: `bodySmall TextMuted`

### 9. DEVICE

```
┌────────────────────────────────────────┐
│ ⬡ DEVICE                              │
│                                        │
│ Model                 Samsung S24 Ultra │
│ ──────────────────────────────────────  │
│ API Level                          35  │
│ ──────────────────────────────────────  │
│ Current Reading               Accurate │  ← healthy/fair/poor väri
│ ──────────────────────────────────────  │
│ Thermal Zones                  4 found │
└────────────────────────────────────────┘
```

- Kaikki read-only `MetricRow`-rivejä
- "Current Reading" arvo saa `statusColor`-värityksen

### 10. ABOUT

```
┌────────────────────────────────────────┐
│ ⬡ ABOUT                               │
│                                        │
│ Version                   1.0.0 (42)   │
│ ──────────────────────────────────────  │
│ What's New                          ›  │
│ ──────────────────────────────────────  │
│ Rate on Play Store                  ›  │
│ ──────────────────────────────────────  │
│ Privacy Policy                      ›  │
│ ──────────────────────────────────────  │
│ Open Source Licenses                ›  │
│ ──────────────────────────────────────  │
│ Send Feedback                       ›  │
└────────────────────────────────────────┘
```

- NavigationRow-rivejä
- "What's New" → `AlertDialog` changelog-tekstillä
- "Rate" → `Intent(ACTION_VIEW, playStoreUri)`
- "Licenses" → `OssLicensesMenuActivity`
- "Feedback" → `Intent(ACTION_SENDTO, mailto:...)`

---

## Tallennus

Kaikki `DataStore<Preferences>`. Uudet avaimet:

| Avain | Tyyppi | Oletus |
|-------|--------|--------|
| `alert_battery_threshold` | Int | 20 |
| `alert_temp_threshold` | Int | 42 |
| `alert_storage_threshold` | Int | 90 |
| `notif_low_battery` | Boolean | true |
| `notif_high_temp` | Boolean | true |
| `notif_low_storage` | Boolean | true |
| `notif_charge_complete` | Boolean | false |
| `temp_unit` | String | "celsius" |
| `monitor_battery` | Boolean | true |
| `monitor_network` | Boolean | true |
| `monitor_thermal` | Boolean | true |
| `monitor_storage` | Boolean | true |
| `export_format` | String | "csv" |
| `current_sign_override` | String | "auto" |

---

## Merkkijonot

### EN
```xml
<!-- Settings Monitoring -->
<string name="settings_monitoring">Monitoring</string>
<string name="settings_categories">Categories</string>

<!-- Settings Notifications -->
<string name="settings_notif_low_battery">Low Battery</string>
<string name="settings_notif_low_battery_desc">Alert when battery is low</string>
<string name="settings_notif_high_temp">High Temperature</string>
<string name="settings_notif_high_temp_desc">Alert when device overheats</string>
<string name="settings_notif_low_storage">Low Storage</string>
<string name="settings_notif_low_storage_desc">Alert when storage is almost full</string>
<string name="settings_notif_charge_complete">Charge Complete</string>
<string name="settings_notif_charge_complete_desc">Notify when charging finishes</string>

<!-- Settings Alert Thresholds -->
<string name="settings_alert_thresholds">Alert Thresholds</string>
<string name="settings_threshold_battery">Low Battery Warning</string>
<string name="settings_threshold_temp">High Temperature Warning</string>
<string name="settings_threshold_storage">Low Storage Warning</string>

<!-- Settings Display -->
<string name="settings_display">Display</string>
<string name="settings_temp_unit">Temperature Unit</string>
<string name="settings_temp_celsius">Celsius (°C)</string>
<string name="settings_temp_fahrenheit">Fahrenheit (°F)</string>
<string name="settings_home_screen">Home Screen</string>

<!-- Settings Data -->
<string name="settings_data">Data</string>
<string name="settings_data_used">runcheck Data</string>
<string name="settings_export_format">Export Format</string>
<string name="settings_clear_speed_tests">Clear Speed Tests</string>
<string name="settings_clear_all_data">Clear All Monitoring Data</string>
<string name="settings_clear_confirm_title">Clear all data?</string>
<string name="settings_clear_confirm_message">This will permanently delete all monitoring history. This cannot be undone.</string>
<string name="settings_clear_confirm_action">Clear</string>

<!-- Settings Advanced -->
<string name="settings_advanced">Advanced</string>
<string name="settings_sign_convention">Current Sign Convention</string>
<string name="settings_sign_auto">Auto-detect</string>
<string name="settings_sign_positive_charging">Positive = Charging</string>
<string name="settings_sign_negative_charging">Negative = Charging</string>
<string name="settings_recalibrate">Recalibrate Device Profile</string>
<string name="settings_last_calibrated">Last calibrated: %1$s</string>

<!-- Settings About -->
<string name="settings_whats_new">What\'s New</string>
<string name="settings_licenses">Open Source Licenses</string>
<string name="settings_feedback">Send Feedback</string>
<string name="settings_restore_purchase">Restore Purchase</string>
```

### FI
```xml
<!-- Settings Monitoring -->
<string name="settings_monitoring">Seuranta</string>
<string name="settings_categories">Kategoriat</string>

<!-- Settings Notifications -->
<string name="settings_notif_low_battery">Akku vähissä</string>
<string name="settings_notif_low_battery_desc">Hälytys kun akku on vähissä</string>
<string name="settings_notif_high_temp">Korkea lämpötila</string>
<string name="settings_notif_high_temp_desc">Hälytys kun laite ylikuumenee</string>
<string name="settings_notif_low_storage">Tallennustila vähissä</string>
<string name="settings_notif_low_storage_desc">Hälytys kun tallennustila on loppumassa</string>
<string name="settings_notif_charge_complete">Lataus valmis</string>
<string name="settings_notif_charge_complete_desc">Ilmoitus kun lataus on valmis</string>

<!-- Settings Alert Thresholds -->
<string name="settings_alert_thresholds">Hälytysrajat</string>
<string name="settings_threshold_battery">Akun varoitusraja</string>
<string name="settings_threshold_temp">Lämpötilaraja</string>
<string name="settings_threshold_storage">Tallennustilaraja</string>

<!-- Settings Display -->
<string name="settings_display">Näyttö</string>
<string name="settings_temp_unit">Lämpötilayksikkö</string>
<string name="settings_temp_celsius">Celsius (°C)</string>
<string name="settings_temp_fahrenheit">Fahrenheit (°F)</string>
<string name="settings_home_screen">Aloitusnäyttö</string>

<!-- Settings Data -->
<string name="settings_data">Data</string>
<string name="settings_data_used">runcheck-data</string>
<string name="settings_export_format">Vientimuoto</string>
<string name="settings_clear_speed_tests">Tyhjennä nopeustestit</string>
<string name="settings_clear_all_data">Tyhjennä kaikki seurantadata</string>
<string name="settings_clear_confirm_title">Tyhjennetäänkö kaikki data?</string>
<string name="settings_clear_confirm_message">Tämä poistaa pysyvästi kaiken seurantahistorian. Tätä ei voi kumota.</string>
<string name="settings_clear_confirm_action">Tyhjennä</string>

<!-- Settings Advanced -->
<string name="settings_advanced">Edistyneet</string>
<string name="settings_sign_convention">Virran etumerkki</string>
<string name="settings_sign_auto">Automaattinen tunnistus</string>
<string name="settings_sign_positive_charging">Positiivinen = Lataus</string>
<string name="settings_sign_negative_charging">Negatiivinen = Lataus</string>
<string name="settings_recalibrate">Kalibroi laiteprofiili</string>
<string name="settings_last_calibrated">Viimeksi kalibroitu: %1$s</string>

<!-- Settings About -->
<string name="settings_whats_new">Uutta</string>
<string name="settings_licenses">Avoimen lähdekoodin lisenssit</string>
<string name="settings_feedback">Lähetä palautetta</string>
<string name="settings_restore_purchase">Palauta ostos</string>
```

---

## Toteutusjärjestys

| Vaihe | Tehtävä | Työmäärä |
|-------|---------|----------|
| 1 | Rivityypit: SettingsToggleRow, SliderRow, NavigationRow, RadioGroup, DangerRow | Pieni |
| 2 | Eritelty notifications + master toggle | Pieni |
| 3 | Alert thresholds + DataStore-avaimet | Keskitaso |
| 4 | Lämpötilayksikkö + UiFormatters-muutos | Keskitaso |
| 5 | Data management: clear-toiminnot + dialogit | Pieni |
| 6 | Advanced-osio (collapsible) | Pieni |
| 7 | About-laajennukset (Licenses, Feedback, What's New) | Pieni |
| 8 | Merkkijonot EN + FI | Pieni |
