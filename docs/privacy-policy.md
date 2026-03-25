# runcheck Privacy Policy

**Last updated:** March 10, 2026

## What data runcheck collects

runcheck monitors your device's hardware metrics locally on your device:

- **Battery**: level, voltage, temperature, current, health, cycle count
- **Network**: connection type, signal strength, WiFi info, latency
- **Thermal**: battery and CPU temperature, thermal status
- **Storage**: total, used, and available space
- **App usage**: per-app foreground time and estimated battery drain (requires PACKAGE_USAGE_STATS permission)

## How data is stored

Measurement history and settings are stored **locally on your device** using on-device Room and DataStore storage. The local database is not encrypted at the application layer.

Free users' data is automatically cleaned up after 24 hours. Pro users can retain data indefinitely and export it as CSV files to their device's Downloads folder.

## Network access

runcheck uses network access only for:

- **Latency measurement**: A TCP connection sample to the configured latency endpoint. By default this is `locate.measurementlab.net:443`.
- **In-app purchases**: Google Play Billing for the Pro upgrade
- **Speed testing**: Optional M-Lab NDT7 network throughput testing initiated by the user

No analytics, crash reporting, or telemetry data is ever sent.

## Third-party services

- **Google Play Billing**: Processes in-app purchases. Subject to [Google Play's terms](https://play.google.com/about/play-terms/).
- **M-Lab**: Provides optional speed-test infrastructure and the default latency endpoint.

## Permissions

| Permission | Purpose |
|-----------|---------|
| ACCESS_NETWORK_STATE | Monitor network connection type |
| ACCESS_WIFI_STATE | Read WiFi signal strength and details |
| ACCESS_FINE_LOCATION | Allow Android to expose current WiFi SSID and related network details |
| FOREGROUND_SERVICE | Run background monitoring service |
| FOREGROUND_SERVICE_SPECIAL_USE | Support the real-time monitoring foreground service while actively viewing live metrics |
| POST_NOTIFICATIONS | Send alerts (low battery, high temp, etc.) |
| RECEIVE_BOOT_COMPLETED | Restart monitoring after device reboot |
| INTERNET | Latency measurement, speed testing, and billing |
| PACKAGE_USAGE_STATS | Per-app battery usage tracking |

## Data deletion

Uninstalling runcheck removes all locally stored app data. Free users' data is automatically deleted after 24 hours.

## Children's privacy

runcheck is not directed at children under 13 and does not knowingly collect personal information from children.

## Contact

For questions about this privacy policy, contact: [your-email@example.com]

## Changes

We may update this policy from time to time. Changes will be posted here with an updated date.
