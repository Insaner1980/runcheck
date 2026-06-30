# runcheck Release Checklist

## Before First Release

### Billing
- [ ] Create in-app product `runcheck_pro` in Google Play Console
- [ ] Set price for Pro upgrade
- [ ] Keep `runcheck_pro` as a one-time `INAPP` product with one backward-compatible buy option, or update app offer-token selection before adding multiple one-time purchase offers
- [ ] Test purchase flow with license testers

### Signing
- [ ] Generate upload keystore outside the repository:
  ```bash
  keytool -genkey -v -keystore "$HOME/.android/runcheck-upload.jks" \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias runcheck
  ```
- [ ] Set environment variables:
  ```bash
  export RUNCHECK_KEYSTORE_PATH=$HOME/.android/runcheck-upload.jks
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
- [x] Design final app icon (512x512 for Play Store, adaptive icon for app). Verify or regenerate exports with `tools\export-launcher-icons.ps1`.
- [ ] Create feature graphic (1024x500)
- [ ] Take screenshots on phone (minimum 2, recommended 4-8)
- [ ] Take screenshots on tablet (if targeting tablets)

## Release Build

```powershell
# Set signing environment variables first, then:
.\gradlew.bat --no-configuration-cache --project-prop=runcheck.releaseVersionCodeFloor=<latest-published-versionCode> copyReleaseArtifacts
```

Use `0` for `runcheck.releaseVersionCodeFloor` before the first Play upload. The versioned AAB file will be at `app/build/outputs/release-upload/runcheck-<versionName>-code<versionCode>-release.aab`.

## Version Bumping

Update `app/build.gradle.kts`:
- `currentReleaseVersionCode`: Increment by 1 for each upload (1, 2, 3...)
- `currentReleaseVersionName`: Follow semantic versioning (1.0.0, 1.1.0, 1.2.0...)

## Google Play Console Upload

1. Go to Production > Create new release
2. Upload the AAB file
3. Add release notes (Finnish + English)
4. Submit for review

## Post-Release

- [ ] Monitor Play Console vitals (ANRs, crashes)
- [ ] Respond to user reviews
- [ ] Track conversion rate for Pro upgrade
