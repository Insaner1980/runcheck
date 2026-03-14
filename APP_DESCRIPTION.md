# runcheck App Description

## What runcheck Is

runcheck is a native Android app for monitoring device health in one place. It combines battery, network, thermal, and storage diagnostics into a single product with a clean dashboard, deeper detail screens, and privacy-first local data handling.

The app is designed for people who want a clearer view of how their phone is behaving day to day:

- battery condition and charging behavior
- network quality and connection details
- thermal state and overheating risk
- storage usage and space pressure

## Product Direction

runcheck is not meant to be a generic “all stats everywhere” utility. The product direction is:

- fast overview on `Home`
- health-focused diagnostics on `Health`
- connection-focused diagnostics and speed tools on `Network`
- secondary tools and settings under `More`

The app should feel modern, readable, and technically credible without becoming cluttered or noisy.

## Core Experience

### Home

Home is a high-level overview. It gives a quick snapshot of overall device condition through larger summary cards for:

- Health
- Battery
- Network
- Thermal

It also exposes locked Pro features and upgrade entry points without overwhelming the main view.

### Health

Health is the main diagnostics dashboard. It combines:

- a large overall health gauge
- compact category summaries for battery, network, thermal, and storage
- deeper category cards that open detail screens

This area is about understanding device condition at a glance and then drilling into the category that needs attention.

### Network

Network is a dedicated top-level area for:

- current connection type
- signal strength and quality
- Wi-Fi or cellular details
- latency
- speed test entry and results

This area should feel more instrument-like than the rest of the app, but still stay within runcheck’s design system.

### More

More contains secondary but important tools:

- Settings
- Chargers
- App Usage
- Storage and thermal utilities when not surfaced directly elsewhere

## Key Product Principles

- Native Android only
- Kotlin + Jetpack Compose
- Privacy-first and offline-first
- No ads
- No accounts
- No cloud sync requirement
- Clear distinction between reliable and estimated readings
- Minimal, readable UI instead of overloaded technical screens

## Privacy and Data Handling

runcheck stores measurement history locally on the device. The app does not depend on analytics, ad SDKs, or user accounts.

Network access is used only when a feature needs it, for example:

- latency checks
- speed testing
- Google Play Billing for Pro purchase handling

## Pro Model

runcheck uses a one-time Pro unlock model. Pro features are gated behind locked UI states instead of ad-supported free screens.

Typical Pro areas include:

- longer history
- charger comparison
- widgets
- export
- advanced insights

## Current Navigation Model

The app currently uses four top-level destinations:

1. Home
2. Health
3. Network
4. More

Detail screens use explicit in-app back navigation.

## Design Intent

The visual direction should stay:

- dark, calm, and technical
- accent-led instead of rainbow-colored
- modern Material 3 / Compose-based
- compact but not cramped
- scalable to larger phones, foldables, and tablets

The goal is not to look like a generic benchmark tool or a gamer dashboard. runcheck should feel premium, practical, and trustworthy.
