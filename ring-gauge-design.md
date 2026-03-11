# Ring Gauge Design

The ring gauges on the home screen cards (Health score, Battery level) should have a distinctive layered look that sets them apart from typical diagnostic apps.

## Structure

Two concentric circles:

1. **Outer ring (track):** A visible light ring that forms the full circle. This acts as a "frame" and is always fully visible regardless of the progress value. Use white at around 15-25% opacity — bright enough to clearly stand out against the dark card surface, but not pure white which would be too harsh.

2. **Inner ring (progress):** The colored progress arc sits inside the track ring. It's slightly narrower than the white track, so the track peeks out on both the inner and outer edges of the progress arc. The color follows the semantic status colors (green for healthy, yellow for fair, red for poor).

## Key Detail

The track ring should be noticeably wider than the progress ring — not just 1px difference but enough that the white "frame" is clearly visible even where the progress arc overlaps it. This creates a layered, dimensional effect: white frame behind, color on top.

## Why This Works

- The white track creates strong contrast against the dark glassmorphism card surface — the gauge immediately pops
- When the value is low (e.g. battery at 5%), the mostly-white ring with a tiny color sliver clearly communicates "almost empty"
- The layered rings create visual depth without any shadow or blur tricks
- It's a distinctive look — most apps use a dim gray track that disappears into dark backgrounds
