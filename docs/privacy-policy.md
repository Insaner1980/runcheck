# DevicePulse Privacy Policy

**Last updated:** March 10, 2026

## What data DevicePulse collects

DevicePulse monitors your device's hardware metrics locally on your device:

- **Battery**: level, voltage, temperature, current, health, cycle count
- **Network**: connection type, signal strength, WiFi info, latency
- **Thermal**: battery and CPU temperature, thermal status
- **Storage**: total, used, and available space
- **App usage**: per-app foreground time and estimated battery drain (requires PACKAGE_USAGE_STATS permission)

## How data is stored

All measurement data is stored **locally on your device** in an encrypted database. DevicePulse does **not** transmit your device data to any external server.

Free users' data is automatically cleaned up after 24 hours. Pro users can retain data indefinitely and export it as CSV files to their device's Downloads folder.

## Network access

DevicePulse uses network access only for:

- **Latency measurement**: A single HTTPS request to measure network response time
- **Ads**: Google AdMob displays banner advertisements to free users
- **In-app purchases**: Google Play Billing for the Pro upgrade

## Third-party services

- **Google AdMob**: Displays ads to free users. AdMob may collect device identifiers and usage data per [Google's privacy policy](https://policies.google.com/privacy).
- **Google Play Billing**: Processes in-app purchases. Subject to [Google Play's terms](https://play.google.com/about/play-terms/).

## Permissions

| Permission | Purpose |
|-----------|---------|
| BATTERY_STATS | Read detailed battery statistics |
| ACCESS_NETWORK_STATE | Monitor network connection type |
| ACCESS_WIFI_STATE | Read WiFi signal strength and details |
| FOREGROUND_SERVICE | Run background monitoring service |
| POST_NOTIFICATIONS | Send alerts (low battery, high temp, etc.) |
| RECEIVE_BOOT_COMPLETED | Restart monitoring after device reboot |
| INTERNET | Latency measurement and ad delivery |
| READ_PHONE_STATE | Read cellular network info |
| PACKAGE_USAGE_STATS | Per-app battery usage tracking |

## Data deletion

Uninstalling DevicePulse removes all locally stored data. Free users' data is automatically deleted after 24 hours.

## Children's privacy

DevicePulse is not directed at children under 13 and does not knowingly collect personal information from children.

## Contact

For questions about this privacy policy, contact: [your-email@example.com]

## Changes

We may update this policy from time to time. Changes will be posted here with an updated date.
