# runcheck Release Checklist

## Before First Release

### Firebase
- [ ] Create Firebase project at https://console.firebase.google.com/
- [ ] Skip or disable Google Analytics during setup
- [ ] Download `google-services.json` and place in `app/`
- [ ] Enable Crashlytics in Firebase Console
- [ ] Verify crash reporting is off by default on first launch
- [ ] Remove placeholder `google-services.json` from version control if repo is public

### Billing
- [ ] Create in-app product `runcheck_pro` in Google Play Console
- [ ] Set price for Pro upgrade
- [ ] Test purchase flow with license testers

### Signing
- [ ] Generate upload keystore:
  ```bash
  keytool -genkey -v -keystore runcheck-upload.jks \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias runcheck
  ```
- [ ] Set environment variables:
  ```bash
  export RUNCHECK_KEYSTORE_PATH=/path/to/runcheck-upload.jks
  export RUNCHECK_KEYSTORE_PASSWORD=your_password
  export RUNCHECK_KEY_ALIAS=runcheck
  export RUNCHECK_KEY_PASSWORD=your_password
  ```
- [ ] Enable Play App Signing in Google Play Console
- [ ] **NEVER** commit your keystore or passwords to version control

### Privacy Policy
- [ ] Update email address in `docs/privacy-policy.md`
- [ ] Host privacy policy at a public URL (e.g., GitHub Pages)
- [ ] Add URL to Google Play Console

### Assets
- [ ] Design final app icon (512x512 for Play Store, adaptive icon for app)
- [ ] Create feature graphic (1024x500)
- [ ] Take screenshots on phone (minimum 2, recommended 4-8)
- [ ] Take screenshots on tablet (if targeting tablets)

## Release Build

```bash
# Set signing environment variables first, then:
./gradlew bundleRelease
```

The AAB file will be at `app/build/outputs/bundle/release/app-release.aab`.

## Version Bumping

Update `app/build.gradle.kts`:
- `versionCode`: Increment by 1 for each upload (1, 2, 3...)
- `versionName`: Follow semantic versioning (1.0.0, 1.1.0, 1.2.0...)

## Google Play Console Upload

1. Go to Production > Create new release
2. Upload the AAB file
3. Add release notes (Finnish + English)
4. Submit for review

## Post-Release

- [ ] Monitor Crashlytics dashboard for crash reports
- [ ] Monitor Play Console vitals (ANRs, crashes)
- [ ] Respond to user reviews
- [ ] Track conversion rate for Pro upgrade
