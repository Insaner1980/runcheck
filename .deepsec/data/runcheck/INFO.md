# runcheck security context

runcheck is an Android/Kotlin device health diagnostics app. It reads battery, thermal, storage, network, app-usage, billing, and widget state through Android APIs, Room, DataStore, WorkManager, Glance, M-Lab NDT7, and Google Play Billing.

High-value user data:
- Device health history persisted in Room.
- Network identifiers and connection details such as SSID, signal, IP, and DNS data.
- App usage, storage breakdown, charging sessions, widget summaries, Pro and trial state.
- Exported CSV files and user-controlled cleanup selections.

Important trust boundaries:
- `MainActivity` and app widget receivers are exported Android entry points and should not gain unvalidated external action handling.
- `RealTimeMonitorService` is an opt-in foreground service controlled from Settings.
- `FileExportRepositoryImpl` and the FileProvider path should only expose generated export files from app-owned cache paths.
- Speed tests and latency measurement may use outbound network access; current connection detail reads should stay on-device.
- Google Play Billing is allowed for purchase state. Release builds must not add analytics or crash reporting beyond the current release-safe Sentry no-op.

Security expectations:
- `android:allowBackup` should stay false unless a backup design is explicitly reviewed.
- `android:usesCleartextTraffic` should stay false.
- Logs must not expose network identifiers, billing state, raw device measurements, file paths, export URIs, or persisted health history.
- New outbound network calls outside NDT7 speed test, latency measurement, or billing require review.
- Dependency CVE findings are handled by OWASP Dependency-Check, OSV, and Dependabot, not DeepSec.
