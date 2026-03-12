# DevicePulse Navigation Plan

## Goal

Move DevicePulse from a home-hub-only navigation model to a stable bottom-navigation structure that can support a growing amount of functionality without making screens hard to reach or hard to leave.

This plan assumes DevicePulse will continue expanding beyond a small set of detail screens and should therefore treat navigation as a product structure decision, not a temporary UI fix.

## Progress

- [x] Bottom navigation shell introduced for `Home / Health / Network / More`
- [x] Dedicated `More` top-level screen added
- [x] Bottom nav labels aligned to the new top-level structure
- [x] Detail screens get explicit in-app back buttons
- [x] `Speed Test` moved into its own full-screen route under `Network`
- [x] Entry points normalized so cards always navigate into the correct parent area
- [x] Top-level tab titles moved into consistent app bars on `Health / Network / More`
- [x] Home card grid resized to work better with bottom navigation
- [x] Cellular fallback improved when switching from Wi-Fi to mobile data while the app is open
- [x] Health card status labels made more resilient to tight card space

## Latest Implemented Fixes

- `Health`, `Network`, and `More` now use the same top-level app bar pattern instead of pushing titles down inside scroll content.
- `Home` card rows were enlarged and kept scrollable so the overview still feels roomy with the bottom nav visible.
- Network state now falls back to the current cellular radio type when cached subtype information is still stale after a live Wi-Fi-to-mobile switch.
- Status labels on tighter dashboard cards now truncate safely instead of clipping awkwardly.

## Target Navigation Model

DevicePulse should use a bottom navigation bar with four persistent top-level destinations:

1. `Home`
2. `Health`
3. `Network`
4. `More`

These are the long-term primary areas of the app. They should be reachable with one tap at all times from top-level screens.

Bottom navigation solves the current structural issue:

- users should not be forced to rely on the system back gesture to move around the app
- major areas should not be hidden behind the home screen
- future features need a clear place to live before the UI grows further

## Top-Level Destinations

### Home

Purpose:

- fast overview
- glanceable status
- premium first impression
- lightweight entry point into the rest of the app

Home remains a summary screen, not the place where all functionality lives.

Home should contain:

- overall health card
- battery card
- network card
- thermal card
- Pro entry points
- lightweight insights / summary content

Home should not keep growing into a dumping ground for deeper tools.

### Health

Purpose:

- device condition dashboard
- aggregated health scoring
- category-level breakdowns

Health becomes the main destination for:

- health score
- battery health summary
- thermal summary
- storage health summary
- network health summary

Health is a primary destination, not just a child screen of Home.

### Network

Purpose:

- current network status
- Wi-Fi / cellular details
- latency and signal information
- network-focused tools

Network should become a full primary area because it already contains enough content and will continue growing.

Network should contain:

- current connection overview
- signal / SSID / carrier / subtype / latency details
- entry points into deeper network tools
- speed testing
- later, network history if added

### More

Purpose:

- secondary features and settings
- tools that are important but not frequent enough to deserve primary tab space

More should contain:

- Settings
- Chargers
- App Usage
- Storage detail entry
- Thermal detail entry if not surfaced from Health directly
- any later utility pages that do not belong as primary tabs

## Detail Screen Structure

Bottom navigation does not replace back navigation.

Within each top-level destination, deeper screens still need normal top app bar back buttons.

Rules:

- top-level bottom-nav destinations do not show a back button
- pushed detail screens do show a back button
- system back should still work, but it must not be the only way out

This means:

- `Battery detail` should have top app bar back
- `Thermal detail` should have top app bar back
- `Storage detail` should have top app bar back
- `Speed Test` should have top app bar back
- `Settings` should have top app bar back
- `Chargers` should have top app bar back
- `App Usage` should have top app bar back

## Screen Ownership

Each screen should clearly belong to one primary area.

### Under Home

- overview cards only
- lightweight summary / teaser content

### Under Health

- health dashboard
- battery detail
- thermal detail
- storage detail

Battery remains health-adjacent because users read it primarily as device condition and power status, not as a separate app section yet.

### Under Network

- network overview
- speed test
- later network history / test history if added

### Under More

- settings
- chargers
- app usage
- future utility / admin / export style pages

## Speed Test Direction

`Speed Test` should move out of the bottom of the current network detail page and become its own dedicated full-screen experience under the `Network` area.

Reason:

- it is visually and functionally strong enough to stand alone
- it currently competes with metrics above it
- a dedicated screen gives better hierarchy, pacing, and room for results/history later

The `Network` top-level screen should focus on current network state and provide a clear path into `Speed Test`.

## App Usage Direction

`App Usage` should remain under `More` for now.

It should evolve into a screen that shows individual apps and their estimated values when data quality is sufficient.

Important product rule:

- present app-level battery impact as estimated, not as guaranteed exact device-reported drain

The screen should prioritize:

- per-app rows
- app icon
- app name
- estimated drain or usage score
- foreground/use-time context where helpful

This feature should not drive the primary navigation model yet.

## Information Architecture Rules

To keep the app coherent as it grows:

- do not promote every new feature to a bottom-nav destination
- primary tabs should represent enduring product areas, not temporary experiments
- summary belongs to `Home`
- diagnostics and score breakdown belong to `Health`
- connectivity and testing belong to `Network`
- utilities and management belong to `More`

## UI Rules

Bottom navigation should feel intentional and stable.

Rules:

- use four items, not more
- keep labels always visible
- match current dark visual language
- use the same icon family already used in the app
- selected state should be clear but restrained
- avoid oversized nav bars or loud indicator treatments

Top app bars should remain simple:

- top-level screens: title only, no back button
- detail screens: title plus back button

Responsive layout needs to be treated as a product requirement, not a later polish item:

- layouts must scale cleanly to foldables and tablets
- top-level dashboards should not assume a narrow phone portrait width forever
- card grids and detail content should be designed so they can expand into wider multi-column arrangements later without rewriting screen ownership

## Technical Migration Plan

### Phase 1: Establish navigation shell

- create bottom-nav scaffold for the four primary destinations
- move current root navigation into a nested structure
- preserve current screen routes while reorganizing ownership

### Phase 2: Reclassify current screens

- make `Home`, `Health`, `Network`, and `More` top-level destinations
- move existing detail screens under the correct parent area
- add explicit top app bar back navigation to all non-root screens

### Phase 3: Separate speed test

- move speed test out of the current network detail screen
- give it its own full-screen route under `Network`
- keep current test logic and data flow, only change screen structure

### Phase 4: Normalize entry points

- update cards and buttons so they navigate to the correct parent areas and detail routes
- remove any navigation paths that force users through unrelated screens

### Phase 5: Expand secondary areas carefully

- evolve `App Usage`
- evolve `Chargers`
- add history screens only where the information architecture already has a clear home

## Guardrails

- do not rewrite the whole app architecture during navigation work
- keep route names and screen responsibilities stable where possible
- preserve current behavior while reorganizing screen access
- move features into clearer homes without changing their core logic
- do not use bottom nav as a replacement for proper nested navigation

## Definition of Done

This navigation migration is done when:

- users can move between major app areas without relying on system back
- every detail screen has an explicit in-app way back
- `Home`, `Health`, `Network`, and `More` act as clear top-level destinations
- `Speed Test` is no longer buried at the bottom of the network detail page
- the app’s structure feels scalable for future features without needing another navigation redesign soon
