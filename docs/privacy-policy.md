# runcheck Privacy Policy

**Last updated:** March 27, 2026

## What data runcheck collects

runcheck monitors your device's hardware metrics locally on your device:

- **Battery**: level, voltage, temperature, current, health, cycle count
- **Network**: connection type, signal strength, WiFi info, latency
- **Thermal**: battery temperature, Android thermal status, throttling history, and the foreground app label for severe throttling events when Usage Access is granted
- **Storage**: total, used, and available space
- **App usage**: per-app foreground time (requires PACKAGE_USAGE_STATS permission)

## How data is stored

Measurement history, throttling events, and settings are stored **locally on your device** using on-device Room and DataStore storage. The local database is not encrypted at the application layer.

Free users' data is automatically cleaned up after 24 hours. Pro users can choose longer retention windows (3 months, 6 months, 1 year, or forever) and can export CSV files for sharing from the app.

## Network access

runcheck uses network access only for:

- **Latency measurement**: A TCP connection sample to the configured latency endpoint. By default this is `locate.measurementlab.net:443`.
- **In-app purchases**: Google Play Billing for the Pro upgrade
- **Speed testing**: Optional M-Lab NDT7 network throughput testing initiated by the user

No analytics, crash reporting, or telemetry data is ever sent.

## Third-party services

- **Google Play Billing**: Processes in-app purchases. Subject to [Google Play's terms](https://play.google.com/about/play-terms/).
- **M-Lab**: Provides optional speed-test infrastructure and the default latency endpoint. User-initiated speed tests may contribute measurement data to open internet research.

## Permissions

| Permission | Purpose |
|-----------|---------|
| ACCESS_NETWORK_STATE | Monitor network connection type |
| ACCESS_WIFI_STATE | Read WiFi signal strength and details |
| ACCESS_FINE_LOCATION | Allow Android to expose the current WiFi SSID and BSSID |
| FOREGROUND_SERVICE | Run the optional Live Notification foreground service |
| FOREGROUND_SERVICE_SPECIAL_USE | Declare the Live Notification foreground service subtype on supported Android versions |
| POST_NOTIFICATIONS | Send alerts (low battery, high temp, etc.) |
| RECEIVE_BOOT_COMPLETED | Restart monitoring after device reboot |
| INTERNET | Latency measurement, speed testing, and billing |
| READ_MEDIA_IMAGES / READ_MEDIA_VIDEO / READ_MEDIA_AUDIO | Scan media files for storage breakdown and cleanup tools (Android 13+) |
| READ_EXTERNAL_STORAGE | Scan files for storage breakdown and cleanup tools (Android 12 and below) |
| WRITE_EXTERNAL_STORAGE | Legacy external file access for cleanup actions on Android 9 and below |
| PACKAGE_USAGE_STATS | Show per-app foreground time and label severe throttling events with the foreground app when available |

## Data deletion

Uninstalling runcheck removes all locally stored app data. Free users' data is automatically deleted after 24 hours.

## Children's privacy

runcheck is not directed at children under 13 and does not knowingly collect personal information from children.

## Contact

For questions about this privacy policy, contact: [your-email@example.com]

## Changes

We may update this policy from time to time. Changes will be posted here with an updated date.
