# runcheck — Speed Test Feature Spec

## Overview

Add a built-in internet speed test to the Network section using M-Lab's NDT7 protocol. M-Lab (Measurement Lab) is an open-source internet measurement consortium founded by Google, with servers worldwide that auto-select the nearest location to the user.

## Library

- **ndt7-client-android** — Official Kotlin implementation by M-Lab
- Repository: `https://github.com/m-lab/ndt7-client-android`
- License: Apache 2.0
- Auto-selects nearest M-Lab server via `https://locate.measurementlab.net/`
- Measures: download speed, upload speed, latency
- No API key required, no usage limits, free

## Integration Notes

- The library provides an `NDTTest` base class to subclass
- Override `onMeasurementDownloadProgress()` and `onMeasurementUploadProgress()` for real-time updates
- Override `onDownloadProgress()` and `onUploadProgress()` for raw byte counts
- Override `onFinished()` for final results
- Test runs on a background thread, progress callbacks for UI updates
- Data is streamed — nothing is saved to disk, no storage impact

## Updated Network Detail Screen

Add a **Speed Test** section below the existing network info.

### Speed Test UI

- Large circular **Start** button (centered, prominent) — tap to begin test
- During test, button transforms into animated progress indicator
- Three result gauges displayed during/after test:
  - **Download** (Mbps) — large, primary metric
  - **Upload** (Mbps) — medium
  - **Ping** (ms) — small
- Real-time needle/arc animation as speeds are measured (same gauge style as battery current)
- Results card after completion: download, upload, ping, server location, connection type (WiFi/cellular)
- **Data usage warning** displayed before starting test on cellular: "This test will use approximately 20–50 MB of mobile data"

### Test Flow

1. User taps Start
2. Latency test (ping) — ~3 seconds
3. Download test — ~10 seconds, real-time speed updates
4. Upload test — ~10 seconds, real-time speed updates
5. Results displayed with option to save

## Data Model

```sql
speed_test_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER,
    download_mbps REAL,
    upload_mbps REAL,
    ping_ms INTEGER,
    jitter_ms INTEGER,           -- nullable, if available from NDT7
    server_name TEXT,             -- M-Lab server identifier
    server_location TEXT,         -- City/country of test server
    connection_type TEXT,         -- WIFI / CELLULAR
    network_subtype TEXT,         -- LTE / NR / WiFi-5GHz / etc.
    signal_dbm INTEGER           -- Signal strength at time of test
)
```

## Free vs Pro

| Feature | Free | Pro |
|---------|------|-----|
| Run speed test | Yes | Yes |
| View latest result | Yes | Yes |
| Speed test history | Last 5 results | Unlimited |
| WiFi vs Cellular comparison | — | Yes |
| Speed trends over time | — | Yes |
| Export speed test data | — | Yes (CSV) |
| Scheduled automatic tests | — | Yes |

## Network Health Score Update

Add speed test results as optional input to the network health score (20% weight in overall score):

| Metric | Weight (within Network) | Scoring |
|--------|------------------------|---------|
| Signal strength (dBm) | 40% | Based on excellent/good/fair/poor thresholds |
| Latency (ping) | 30% | < 30ms excellent, 30–100ms good, > 100ms poor |
| Download speed | 20% | Relative to connection type expectations |
| Connection stability | 10% | Based on signal variance over time |

Speed test metrics only included when a recent test exists (< 1 hour old). Otherwise signal + latency only.

## Permissions

No additional permissions needed — `INTERNET` is already in the spec.

## Navigation

Speed test lives within the Network screen, either as a scrollable section below connection info or as a tab within Network.

## Animations

- **Start button:** Ripple effect on tap, morphs into circular progress ring over 300ms
- **During test:** Progress ring fills as test progresses. Speed value in center counts up in real-time using rolling counter animation (same as battery metrics)
- **Phase transitions** (ping → download → upload): Cross-fade between gauge labels over 200ms
- **Completion:** Results cards stagger in with slide-up animation (same as dashboard cards, 60ms delay between cards)
- **Gauge needles:** Spring physics tracking real-time speed fluctuations (`spring(dampingRatio = 0.8)`)

## Privacy

M-Lab is an open research platform:

- M-Lab collects the user's IP address along with test results
- Test data is published as open data for internet research
- No personal information beyond IP address is collected
- The app's privacy policy must disclose this
- Add notice in speed test UI: "Speed test powered by M-Lab. Test data contributes to open internet research. [Learn more]"

## Development Phases

**Phase 1 (MVP):** Basic speed test — start, measure, show results, save latest result

**Phase 2 (Polish):** History (limited in free), result cards with animations, cellular data warning

**Phase 3 (Pro):** Unlimited history, WiFi vs cellular comparison, speed trends, scheduled tests, CSV export
