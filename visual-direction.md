# runcheck — Visual Direction Notes

Based on UI explorations with Google Stitch. These capture the visual feel we're going for beyond what the main spec defines.

## Shape Language

- **Thick, solid forms over thin and delicate.** Gauges, bars, and indicators should feel substantial and confident. This is a diagnostics app — clarity and readability come first.
- **Battery gauge:** Circular gauge with a visible inner ring and an outer progress arc — creates a layered, dimensional look. The circle-within-a-circle motif.
- **Signal strength bars:** Chunky, wide bars rather than thin lines. Should feel bold and easy to read at a glance.
- **Bar charts (e.g. usage trends):** Thick columns with generous width. Not narrow slivers — the data should feel tangible.

## Screen Structure Pattern

Each detail screen should follow a consistent top-down hierarchy:

1. **Hero summary card** — the most important fact, immediately visible. Example: storage screen's "128.5 GB of 256 GB used" card with a progress bar. Big, clear, no ambiguity.
2. **Contextual alert card** (when applicable) — a visually distinct card highlighting something the user should know. Example: "Storage full soon — approx. 45 days." Only appears when there's something worth flagging.
3. **Detail content** — metric cards, charts, breakdowns below.

## Charts

- **Line charts use a gradient fill** under the line, fading from the accent color (~20% opacity at the top) to transparent at the bottom. A bare line feels too dry, a solid fill is too heavy — the gradient is the right middle ground.
- **Bar charts** can use solid fills with slightly rounded tops for a softer feel.

## General Feel

- Rounded, approachable, not clinical
- Data should feel glanceable — the eye lands on the important number first
- Visual weight where it matters, subtlety everywhere else
