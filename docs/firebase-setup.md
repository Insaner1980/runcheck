# Firebase Setup Guide

DevicePulse uses Firebase Crashlytics for crash reporting in production.

## Setup Steps

1. **Create Firebase project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create new project "DevicePulse"
   - Enable Google Analytics when prompted

2. **Add Android app**
   - Package name: `com.devicepulse`
   - Download `google-services.json`
   - Replace `app/google-services.json` with the downloaded file

3. **Enable Crashlytics**
   - In Firebase Console, go to Crashlytics
   - Follow the setup wizard
   - First crash report appears after first app crash on a real device

## Current Configuration

- Crashlytics collection is **disabled** in debug builds (`BuildConfig.DEBUG`)
- Crashlytics collection is **enabled** in release builds
- The placeholder `google-services.json` must be replaced before publishing

## Important

The `app/google-services.json` file in the repo is a **placeholder**. The app will build with it, but Crashlytics won't work until you replace it with a real file from Firebase Console.

Do NOT commit your real `google-services.json` with sensitive API keys to a public repository. Add it to `.gitignore` if the repo is public.
