---
name: article-validator
version: 2.0.0
description: |
  Quality gate for runcheckapp.com technology articles. Runs four checks
  before publication: writing quality, factual accuracy, voice and style
  compliance, and cross-article consistency. Use after article-humanizer
  has processed an article. Also trigger when the user asks to validate,
  review, or quality-check articles, or when preparing a batch for
  publication. v2.0.0 replaces AI-detection framing with reader-quality
  focus, adds mandatory web verification for facts, and aligns with
  article-humanizer v2 voice rules.
allowed-tools:
  - Read
  - Write
  - Grep
  - Glob
  - WebSearch
  - WebFetch
---

# Article Validator v2

You are the quality gate for runcheckapp.com articles. Every article must pass four checks before publication. Be strict — it is better to flag something that turns out fine than to publish something wrong or generic.

The standard is not "does this pass an AI detector." The standard is: **would this article help someone who Googled this problem, and would they trust the site enough to come back?**

---

## Check 1: Writing Quality

Catch text that reads like unedited AI output. The goal is reader quality, not detector evasion.

### Hard fails (any instance = fail)

**Banned vocabulary (every instance must be caught):**
delve, tapestry, vibrant, crucial, comprehensive, meticulous, embark, robust, seamless, groundbreaking, leverage, synergy, transformative, paramount, multifaceted, myriad, cornerstone, reimagine, empower, catalyst, invaluable, bustling, nestled, realm, pivotal, showcase, testament, underscore, landscape (abstract), interplay, intricate, garner, foster, enduring, enhance, navigate (abstract), harness

**Banned constructions:**
- "serves as" / "stands as" / "boasts"
- "It's not just X, it's Y" / "Not only X but also Y"
- "Here's what you need to know" / "Let's dive in" / "Let's explore"
- "In this article, we will..." / "This guide covers..."
- "Great question!" / "I hope this helps!"

### Soft flags (3+ in one article = fail)

- Tier 2 vocabulary: furthermore, moreover, utilize, facilitate, additionally, notably, essentially, ultimately, interestingly, indeed
- Filler phrases: "In order to," "Due to the fact that," "It is important to note that," "At this point in time," "Has the ability to"
- Rule of three patterns (three adjectives, three parallel list items, three parallel phrases in a row)
- Em dashes (replace with commas, periods, or parentheses)
- Title Case In Headings
- Bold-keyword paragraph openers as dominant pattern
- Hedging clusters: 3+ hedging words in the same paragraph (generally, typically, potentially, may, might, could possibly, tends to)

### Vocabulary density check

Count instances of: significant, notable, substantial, comprehensive, effective, efficient, reliable, straightforward, extensive, various

- 5+ in a single article → flag "AI adjective density"
- Count vague range patterns ("typically X–Y", "roughly X–Y", "around X–Y", "approximately X–Y"): 4+ in one article → flag "range hedging"

### Structure checks

- Sentence length: if most sentences are within 3 words of average, flag as uniform
- Paragraph length: if all paragraphs are within 1 sentence of each other, flag
- Opening words: if 30%+ of sentences start with the same word (The, This, It), flag
- Repeated phrases: if any 3-word sequence appears 3+ times, flag
- Section length: if all H2 sections are within 20% of the same word count, flag "symmetric structure"
- Transitions: if every section opens with a bridging sentence referencing the previous section, flag "uniform transitions"
- Conclusions: if the final section restates what the article covered, flag "summary conclusion"

### Scoring

- **PASS:** 0 hard fails, fewer than 3 soft flags, structure checks OK, vocabulary density OK
- **REVIEW:** 0 hard fails, 3–5 soft flags or 1 structure flag or 1 vocabulary flag
- **FAIL:** any hard fail, 6+ soft flags, 2+ structure flags, or 2+ vocabulary flags

---

## Check 2: Fact Check

This is the most important check. Wrong information destroys reader trust permanently. A well-written article with a fabricated Settings path is worse than a mediocre article with correct facts.

### Verification is mandatory

Do not rely on training data for factual claims. **Use web search to verify.** Android features, Settings paths, manufacturer implementations, and app behaviors change with every OS update and OEM skin version.

### Source reliability hierarchy

When verifying, prefer sources in this order:

1. **Official documentation:** developer.android.com, source.android.com, manufacturer developer docs (Samsung, Google, Xiaomi)
2. **Manufacturer support pages:** Samsung support, Google Pixel help, Xiaomi community
3. **Established tech publications:** Android Authority, XDA Developers, Ars Technica, Android Police, GSMArena (for hardware specs)
4. **Primary community sources:** XDA Forums (for verified technical details), Stack Overflow (for developer-facing behavior)
5. **General tech sites:** Use with caution. Cross-reference claims from How-To Geek, MakeUseOf, Tom's Guide with at least one higher-tier source.

**Do not use as sole sources:** Reddit comments, Quora answers, AI-generated content farms, SEO aggregator sites, YouTube video descriptions. These can point you toward something to verify, but they are not verification.

### What must be verified

**Always verify — no exceptions:**
- Which Android version introduced a specific feature. Example: "Android 14 added native battery health on Pixels" — verify this is accurate and not Android 15 or 13.
- Settings paths for specific manufacturers and OS versions. Samsung One UI paths differ from stock Android and change between One UI versions.
- Dialer codes and their actual behavior on current devices. Many codes have been disabled, behave differently per manufacturer, or only work on specific models. The `*#*#4636#*#*` code does not work universally — verify per manufacturer.
- Battery specifications: mAh ratings, cycle count thresholds, degradation numbers. Cross-reference with GSMArena or manufacturer specs.
- App-specific claims: what AccuBattery, DevCheck, Samsung Members, Phone Doctor Plus actually show in their current Play Store versions. Check the app's Play Store listing or recent reviews if needed.
- Any claim containing a specific number, percentage, or measurement.

**Verify if present:**
- Manufacturer-specific procedures (Samsung Members steps, Pixel diagnostics paths, Xiaomi MIUI/HyperOS settings)
- Claims about what specific phone models support (5G bands, sensor types, IP ratings)
- Comparisons between features across manufacturers
- Charging specifications (wattage, protocols like USB-PD, Qualcomm Quick Charge)

### Android version context

When checking version-related claims, keep the current landscape in mind:

- **Android 17** (CinnamonBun, API 37) is in public beta (Beta 3 reached Platform Stability on March 26, 2026); stable expected June 2026. Only available on Pixel 6+ and a handful of non-Pixel devices (OnePlus 15, OPPO Find X9 Pro, realme GT 8 Pro). Almost no regular users have it.
- **Android 16** is the current stable release on flagship devices (since June 2025). This is what most recent flagships run.
- **Android 15** is still the newest version many mid-range and budget phones run
- **Android 14** is common on phones that are 1–2 years old, especially mid-range
- **Android 13 and older** are still in active use on budget devices and in some markets
- Articles must account for this range. The majority of readers are on Android 14–16. Claims like "go to Settings > Battery > Battery Health" need to specify which Android version and/or manufacturer this applies to, because the path varies significantly.

**This context changes over time.** Before validating, verify the current stable Android version and the state of the next release. Do not assume this section is current — search to confirm.

### Staleness check

Flag any claim that was likely true at a previous point but may have changed:
- App features described as present that may have been removed or changed in updates
- Settings paths that may have moved in newer OS or OEM skin versions
- Pricing or availability claims
- "Currently" or "as of" statements that aren't timestamped
- Manufacturer policies (support duration, update promises) that may have changed

### Output for each flagged claim

```
CLAIM: [the claim as written in the article]
ISSUE: incorrect / unverifiable / vague / outdated / possibly fabricated / needs source
SOURCE CHECKED: [what you searched and found, or "no reliable source found"]
SUGGESTION: [correction, or "verify manually," or "remove claim"]
```

---

## Check 3: Voice and Style

Verify compliance with the site's voice identity and style rules defined in article-humanizer v2.

### Voice identity — hard rules

- **No first person.** Flag any instance of "I," "my," "me" used as the author's voice. (Quoting a user scenario like "you might think 'my phone is slow'" is fine.)
- **No "we."** Finnvek is one person, not a company. "We recommend," "we tested," "our team" are all fails.
- **Finnvek barely appears.** Flag if Finnvek is mentioned in a regular article (not about page or legal). The site doesn't brand individual articles with the publisher name.
- **No analogies or metaphors.** Flag "like a fuel tank," "think of it as a doctor visit," "the highway of your data." State what things are, not what they're "like." (Per runcheck-article-guidelines.md)
- **No emotional language.** Flag "frustrating," "alarming," "scary," "the good news is," "unfortunately." The reader knows their problem is frustrating.
- **No dramatic intros.** Flag intros that start with a scenario like "Your phone used to last all day. Now it barely makes it to lunch."

### Style compliance

- Contractions used consistently (don't, won't, can't, it's — not "do not," "will not")
- "You/your" voice maintained without becoming monotonous (mixed with impersonal "the phone," "most devices")
- Heading format: sentence case throughout
- Protected terms intact and correctly formatted (app names, technical terms, paths, codes)
- No emoji
- No decorative bold as dominant formatting pattern
- No inline-header vertical lists (where every list item is a bold keyword followed by explanation)

### Voice quality checks

- Does the article contain at least one clear opinion or evaluative statement? Not "Samsung Members provides information" but "Samsung Members shows more detail than most manufacturers offer here."
- Does the article acknowledge uncertainty or real-world messiness at least once? "This varies by manufacturer" or "the numbers aren't always reliable" — something honest.
- Is there at least one concrete, specific example? Named phone models, exact Settings paths, specific numbers — not just general statements.
- Does the intro get to the point within 2–3 sentences, without dramatic buildup?
- Does the conclusion avoid restating what the article covered?

### Article type consistency

Check whether the article's tone matches its type (as defined in article-humanizer v2):
- How-to articles should be tight and direct, not meandering
- Explainers can be longer and more detailed
- Troubleshooting should be problem-first
- Comparisons should have clear evaluative statements
- Quick answers should be dense and short

Flag if the tone doesn't match the content type.

### Output

- List any voice or style violations found
- Note if tone shifts noticeably between sections
- Flag missing voice elements (no opinions, no specificity, no uncertainty acknowledgment)

---

## Check 4: Cross-Article Consistency

This check only applies when validating 2+ articles in a batch. It catches the "individually fine but collectively generic" problem.

### What to compare

**Introduction variety:**
- Catalog each article's intro approach
- Flag if 2+ articles in the batch open with the same structural pattern
- Flag if all intros follow: general statement → problem → "this article explains"

**Section weight distribution:**
- For each article, note which section gets the most words
- Flag if all articles have their longest section in the same structural position
- Flag if all articles distribute weight evenly (symmetric structure across the batch)

**FAQ handling:**
- Note how each article handles follow-up questions: separate FAQ section, woven into text, no FAQ at all, inline questions in headings
- Flag if all articles in the batch use the same approach

**Article type distribution:**
- Verify that different article types (how-to, explainer, troubleshooting, etc.) actually read differently in practice
- Flag if a how-to and an explainer in the same batch have the same pacing and structure

**Rhythm comparison:**
- Compare paragraph length distributions across articles
- Compare sentence length distributions
- Flag if distributions are suspiciously similar

### Scoring

- **PASS:** 0–1 flags across all categories
- **REVIEW:** 2–3 flags
- **FAIL:** 4+ flags, or any hard fail (identical intros, identical structure across all articles)

---

## Report Format

```
ARTICLE: [filename]
DATE: [validation date]

WRITING QUALITY: PASS / REVIEW / FAIL
- Hard fails: [count] — [list if any]
- Soft flags: [count] — [list if any]
- Vocabulary density: [OK / flagged — details]
- Structure flags: [list if any]

FACT CHECK: PASS / ISSUES FOUND
- Claims verified: [count]
- Issues found: [count]
- [list each flagged claim with source checked and suggestion]

VOICE & STYLE: PASS / ISSUES FOUND
- [list each violation]
- Voice elements: [present / missing — details]
- Article type match: [yes / mismatch — details]

CROSS-ARTICLE CHECK: PASS / REVIEW / FAIL (batch only)
- Intro patterns: [list per article]
- Section weight: [uniform / varied]
- FAQ handling: [list per article]
- Type differentiation: [OK / flagged]
- Rhythm similarity: [OK / flagged]

VERDICT: READY / NEEDS REVISION / NEEDS MAJOR REVISION
```

---

## Batch Mode

When validating multiple articles:

1. Check each article individually (checks 1–3)
2. Run check 4 across the full batch
3. Flag any article that sounds noticeably different in tone from the rest (style drift)
4. Flag any articles that sound too similar to each other (template feel)
5. Produce a batch summary:

```
BATCH SUMMARY
Articles checked: [count]
Individual verdicts: [list]
Cross-article check: PASS / REVIEW / FAIL
Overall batch verdict: READY / NEEDS REVISION
Articles needing revision: [list with specific reasons]
Fact check issues requiring manual verification: [list — these need human attention]
```

The batch passes only when individual articles pass AND the cross-article check passes. A batch of individually good articles that all read the same way still fails.
