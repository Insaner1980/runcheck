# Firebase Crashlytics Setup

DevicePulse now supports Firebase Crashlytics in a privacy-first configuration:

- Firebase Crashlytics SDK is included
- Firebase Analytics is not included
- Crash reporting is off by default
- Users must explicitly enable crash reporting in Settings
- Debug builds never send crash reports

## What This Sends

If the user enables crash reporting, Firebase Crashlytics may send:

- crash stack traces
- ANR diagnostics
- app version and build identifiers
- Android version and device model
- coarse custom metadata if the app adds any in the future

DevicePulse must not send:

- analytics events
- advertising IDs
- account data
- WiFi SSIDs
- carrier names
- IP addresses
- free-form device logs
- user-entered text

## Firebase Console Steps

1. Create a Firebase project in [Firebase Console](https://console.firebase.google.com/).
2. When prompted about Google Analytics, skip it or disable it.
3. Add the Android app with package name `com.devicepulse`.
4. Download the real `google-services.json`.
5. Replace the placeholder file at `app/google-services.json`.
6. In Firebase Console, open Crashlytics and finish the setup flow.
7. Build and install a release variant, then enable `Settings > Privacy > Share crash reports`.
8. Trigger a test crash on a device and relaunch the app so the first report is sent.

## Repository State

The app is configured so that:

- `firebase_crashlytics_collection_enabled` is `false` in the manifest
- user consent is stored in app preferences
- app startup reapplies the saved consent state
- disabling the toggle deletes pending unsent reports
- enabling the toggle sends any pending unsent reports

## Publish Checklist

Before shipping:

1. Replace the placeholder `app/google-services.json`.
2. Verify `Settings > Privacy > Share crash reports` is off on first launch.
3. Verify a debug build does not send Crashlytics reports.
4. Verify a release build sends a test report only after the user opts in.
5. Update privacy text to state that crash diagnostics are sent to Google only when the user enables crash reporting.

Suggested privacy wording:

> If you enable crash reporting, anonymized crash diagnostics and technical app/device metadata are sent to Google Firebase Crashlytics to help fix bugs.

## Notes

- Firebase Console and Crashlytics are generally available at no cost for this use case, but confirm current pricing before launch.
- `app/google-services.json` in this repository is a placeholder and will not send real reports.
- Do not add Firebase Analytics unless product requirements change, because it weakens the current privacy posture.
