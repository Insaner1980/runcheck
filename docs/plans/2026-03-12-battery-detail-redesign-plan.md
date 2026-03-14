## Battery Detail Redesign Plan

Goal: move the Battery screen from a stacked metric list to a more intentional "hero + grouped panels" layout inspired by the reference, while preserving runcheck's existing dark teal visual language and current data model.

### Layout changes

- Replace the first battery level tile with a large hero section built around a circular progress ring.
- Group core battery specs into a single panel instead of separate metric cards.
- Elevate charging current and charge level into a dedicated live-status panel.
- Keep history on the page, but present it inside a stronger chart panel with the existing pro gating.

### Data usage

- Keep using the current `BatteryUiState.Success` data as-is.
- Do not add new repository, ViewModel, or domain requirements for this redesign.
- Reuse existing confidence handling for charging current.

### Component strategy

- Reuse `ProgressRing`, `ConfidenceBadge`, `TrendChart`, `DetailTopBar`, and `ProFeatureCalloutCard`.
- Add Battery-specific private composables inside `BatteryDetailScreen.kt` for the new visual structure.
- Avoid introducing generic shared components until there is proven reuse outside the Battery screen.

### Visual direction

- Preserve current runcheck color tokens and spacing.
- Use fewer cards, larger sections, and more hierarchy.
- Keep important status values color-coded with the existing semantic/status colors.

### Constraints

- No architecture changes.
- No fake battery data.
- No redesign spillover into other screens.
