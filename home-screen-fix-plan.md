# Home Screen UI Fix Plan

The home screen currently looks flat, inconsistent, and unpolished. Here's what needs to change.

## 1. Card Surfaces — Add Depth

Right now all cards are flat solid dark gray. They blend into the background and there's no sense of layering.

- The screen background should be the deepest color (#0A0A0F behind everything, #0D0D14 as the base surface).
- Cards should NOT be solid gray. They need a very subtle semi-transparent white overlay (white at ~3% opacity) with a 1dp border (white at ~6% opacity). This creates the glassmorphism layering effect where cards feel like they float above the background.
- The difference between background and card should be subtle but clearly perceptible. If you squint and they look the same, the contrast is too low.

## 2. Typography Hierarchy — Make It Consistent

The hero values on cards (the big numbers like "97", "79%", "5G", "33.7°C") are currently different sizes and different fonts across cards. This looks sloppy.

- ALL hero values on home grid cards must use the same style: 32sp, monospace font (Roboto Mono), regular weight. No exceptions. "97" and "79%" and "5G" and "33.7°C" should all be the same size and font.
- Category names ("Health", "Battery", "Network", "Thermal") should all be 16sp, medium weight, Roboto. Positioned consistently at the top of each card.
- Status labels ("Healthy", "Charging", "Good") should all be 12sp, muted color. Positioned consistently at the bottom of each card.
- The vertical position of each element (name, value, status) should be at the same height across all cards in the same row. If "97" sits at one height in the Health card, "79%" should sit at exactly the same height in the Battery card next to it.

## 3. Gauges — Make Them Thicker

The arc gauges in Health and Battery cards are too thin and wispy. They need to be noticeably thicker — the stroke width should be substantial enough that the gauge feels solid and confident, not like a thin ring. The unfilled portion of the arc (the track) should also be clearly visible as a darker arc, so you can see the full circle shape even where it's not filled.

## 4. Color Usage — Add Variety

Everything is either green or gray right now. The semantic status colors need to actually be used:

- "Healthy" / "Good" status badges should be green with a subtle green-tinted background (not just green text on dark gray).
- The Network card should show signal quality with appropriate color — green for Good, yellow for Fair, etc.
- The Thermal card's status dot is correct but the temperature value itself could subtly reflect the status (green tint when normal, shifting toward yellow/orange when warm).
- The teal accent color (#5EEAD4) should appear somewhere on the home screen — for example in the "DevicePulse" title or as a subtle accent on active/healthy indicators. Right now the only teal is in the Pro card.

## 5. Network Card — Add Signal Bars

The Network card currently shows "5G" as big text with "Good" and dBm below it. The small signal bars at the bottom are barely visible. Make the signal strength bars more prominent — they should be chunky and clearly visible, using green fill for active bars and dark muted fill for inactive bars. This is one of the most recognizable visual indicators and should stand out.

## 6. Locked Cards (Chargers, App Usage) — Don't Look Broken

These two cards currently look empty and broken — just a dim gray icon floating in a gray box. They need to communicate "this is a real feature that's locked" not "this card failed to load."

- Keep the lock icon in the top-right corner.
- Show a brief description of what the feature does (e.g., "Test & compare your chargers" / "See which apps drain battery").
- The card should be visually dimmed compared to active cards but still clearly intentional — slightly lower opacity or a different surface treatment, not just empty.

## 7. Pro Card — More Visual Distinction

The full-width Pro card at the bottom has the right content but needs to stand out more from the regular cards. It should have a subtle teal-tinted border or background gradient so it reads as a distinct call-to-action, not just another card. The star icon is good — keep that.

## 8. Spacing and Padding

Check that the gap between cards is consistent (12dp both horizontally and vertically). Card inner padding should be 16dp on all sides. The content within cards should have breathing room — values shouldn't feel cramped against the card edges.

## Priority Order

Fix these in this order — each step makes the biggest visual improvement relative to effort:

1. Card surfaces (glassmorphism depth)
2. Typography consistency (same sizes, same font, same positions)
3. Gauge thickness
4. Locked card content
5. Color variety and status badges
6. Pro card distinction
7. Spacing polish
