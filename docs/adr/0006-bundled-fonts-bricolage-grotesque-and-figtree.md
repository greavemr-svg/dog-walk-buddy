# ADR 0006: Bundle the Bricolage Grotesque and Figtree fonts as variable-font resources

- Status: Accepted
- Date: 2026-10-22
- Run: `20261022-ux-redesign`

## Context
The owner approved the "Field Kit" type scale (spec `docs/specs/20261022-ux-redesign.md`, Scope item 1, AC5):
- **Bricolage Grotesque** for the display styles (ratingWord, timer, screenTitle, checkQuestion, statLarge, bestWindow), weights 700 and 800 in the scale.
- **Figtree** for the body styles (reason, factorValue, button, body, label, caption, navLabel, axisLabel), weights 400, 500, 600 and 700.

Constraints:
- **No new network host.** MVP AC49 and AC65 allow only the MET Norway API host and the OpenFreeMap hosts. A font must never be fetched at run time.
- **Licence notices in the app.** AC8 says About shows the name and full licence text of each bundled font, next to the existing licence sections, which stay unchanged.
- **Size.** The spec NFR asks for the release APK size before and after (no target). The pack estimated about 300 KB of font files.
- **Robolectric.** Tests run on the JVM (ADR 0001). Font rendering is not checked there, but loading the theme must not crash tests.
- **The owner maintains the app through the pipeline.** The mechanism must be standard Android and Compose, with nothing to update at run time.

## Options considered
1. **Bundled variable fonts from the Google Fonts repository: one file per family in `res/font`** (chosen)
   - Pros:
     - Two files cover every weight in the scale.
     - Bricolage Grotesque's optical-size axis (`opsz` 12-96) lets each display style use the optical size of its own point size, as the artboards do in the browser.
     - Compose supports variation settings on resource fonts from API 26, and minSdk is 29.
     - No network, no provider and no Play Services. The files are just static assets.
   - Cons:
     - The Bricolage variable file is larger than the two static weights we need. The size is measured in task 1.
2. **Bundled static instances** (Bricolage 700 and 800, Figtree 400, 500, 600 and 700: six files)
   - Pros: the simplest `Font(resId, weight)` API.
   - Cons:
     - Six files to source and keep in step.
     - Static Bricolage instances come at fixed optical sizes, so the display styles would not match the artboards' optical sizing.
     - The total size is similar to option 1.
3. **Downloadable Google Fonts** (`androidx.compose.ui:ui-text-google-fonts` with the Google Play Services font provider)
   - Pros: no font bytes in the APK.
   - Cons:
     - The provider downloads fonts from Google servers on first use. That is a host MVP AC49 does not allow.
     - It needs Google Play Services, which the app does not otherwise use (ADR 0005 chose the platform `LocationManager`).
     - It adds a new library.
     - Text falls back to the platform font until the download finishes, and stays that way offline.
     - **Rejected.**
4. **Subsetted fonts** (strip glyphs the UK English UI never uses)
   - Pros: smaller files.
   - Cons:
     - It needs a font tool in the build or a manual step.
     - A subset is a "Modified Version" under the OFL, which adds naming and notice obligations.
     - User text such as a dog's name can contain any character, and a missing glyph would fall back to another font in the middle of a word.
     - **Rejected for now.** It can be revisited if size becomes a problem.
5. **Do nothing (platform sans)**
   - Pros: no size cost.
   - Cons: fails AC5 and the owner's approved design. **Rejected.**

## Decision
Option 1. Bundle one variable TTF per family as Android font resources. Show each font's full `OFL.txt` in About.

### Exact files (added by the task 1 developer; the architect downloads nothing)
| Source path in https://github.com/google/fonts (`main`, commit recorded in the PR) | Destination in this repo | Licence |
|---|---|---|
| `ofl/bricolagegrotesque/BricolageGrotesque[opsz,wdth,wght].ttf` | `app/src/main/res/font/bricolage_grotesque.ttf` | SIL Open Font License 1.1 |
| `ofl/bricolagegrotesque/OFL.txt` | `app/src/main/res/raw/licence_ofl_bricolage_grotesque.txt` | (the licence text itself) |
| `ofl/figtree/Figtree[wght].ttf` | `app/src/main/res/font/figtree.ttf` | SIL Open Font License 1.1 |
| `ofl/figtree/OFL.txt` | `app/src/main/res/raw/licence_ofl_figtree.txt` | (the licence text itself) |

- **Renaming.** Android resource names allow only `a-z`, `0-9` and `_`, so the files are renamed. Their bytes are not changed.
- **Not added:** italic files (`Figtree-Italic[wght].ttf`), the upstream static instances, or any other file.
- **Upstream projects** (for reference, not the download source):
  - Bricolage Grotesque: https://github.com/ateliertriay/bricolage, designed by Mathieu Triay. Copyright line expected in `OFL.txt`: "Copyright 2022 The Bricolage Grotesque Project Authors (https://github.com/ateliertriay/bricolage)".
  - Figtree: https://github.com/erikdkennedy/figtree, designed by Erik Kennedy. Copyright line expected: "Copyright 2022 The Figtree Project Authors (https://github.com/erikdkennedy/figtree)".
- **Developer obligations in task 1:**
  1. Download only from the google/fonts GitHub repository, over HTTPS, at a single commit. Put the commit URL in the PR description.
  2. Record the SHA-256 of each of the four files in the PR description. The security review (G4) re-checks them.
  3. Open each `OFL.txt`. If either file is **not** the SIL Open Font License 1.1, or it declares a Reserved Font Name that the app's use would conflict with, stop and report BLOCKED. The app uses the fonts unmodified, so a Reserved Font Name is not expected to matter.
  4. Do not edit, subset or convert the fonts.

### How the fonts are used
- `ui/theme/Type.kt` builds one `FontFamily` per text style. Each one uses `Font(R.font.<file>, weight, variationSettings = FontVariation.Settings(...))`:
  - **Display styles:** Bricolage Grotesque. `wght` is set to the token weight, and `opsz` is set to the style's token size in sp, clamped to 12-96. For example, ratingWord uses `wght 800, opsz 60` and timer uses `wght 700, opsz 92`. `wdth` stays at its default, 100.
  - **Body styles:** Figtree. `wght` is set to the token weight.
- **Fallback.** If a font cannot be loaded, Compose falls back to the platform sans-serif at the same weight. This is the token file's stated fallback, and it is never a network fetch.
- **No new Gradle dependency.** `FontVariation` and resource fonts are part of `androidx.compose.ui:ui-text`, which the pinned Compose BOM already includes. `ui-text-google-fonts` must not be added.

### Licence notices (AC8)
About keeps every existing licence section unchanged. Two new sections are appended after them:
- One headed "Bricolage Grotesque" with the full text of `licence_ofl_bricolage_grotesque.txt`.
- One headed "Figtree" with the full text of `licence_ofl_figtree.txt`.

The headings are the font names only. No other new text is introduced. The OFL requires the copyright notice and the licence to go with the font, and showing the full `OFL.txt` meets that.

## Consequences
- **Easier:**
  - The design's typography renders identically online and offline, with no host added (AC5, AC65).
  - A font can be swapped later by replacing one file.
- **Harder:**
  - The APK grows by the size of the two files. That is about 300 KB according to the pack; the variable Bricolage file may be larger.
    - The task 1 PR reports the measured size of each file and the release APK size before and after.
    - The performance run records them in `docs/perf/20261022-ux-redesign/`.
    - There is no size target (spec NFR). If the owner later wants a smaller APK, option 4 (subsetting) is the lever and needs an amendment to this ADR.
  - Cold start may parse two font files on first text layout. This is measured against the MVP baseline (within 10 %; see the design's performance section).
  - Robolectric does not render font glyphs, so the font choice is checked by a unit test of the `FontFamily` configuration (AC5) and by the manual visual comparison (AC66).
- **Reversal:** delete the two `res/font` files and their raw licence files, point the families at `FontFamily.SansSerif`, and remove the two About sections. There is no data or migration impact.

## For new dependencies
- **Licence:** SIL Open Font License 1.1 for both fonts. It allows bundling and redistribution with software, including commercial software. The font may not be sold on its own, and the copyright and licence must go with it.
- **Maintenance:**
  - Both families are published in the Google Fonts catalogue (google/fonts repository, actively maintained).
  - Figtree was added to Google Fonts on 2022-07-21. Bricolage Grotesque was added on 2023-06-14.
  - A font file is static data: it has no runtime code and needs no updates.
- **Known vulnerabilities:** none applicable. Font files are parsed by the platform's font engine, which receives Android security updates. The files come only from the google/fonts repository, and their checksums are recorded.
- **Size / performance impact:** two TTF files, estimated at 300-600 KB uncompressed in total. The exact figures go in the task 1 PR and in `docs/perf/20261022-ux-redesign/`. There is no network use and no background work.

## Sources
- [Bricolage Grotesque OFL copyright line (JustFreeFonts, reproducing OFL.txt)](https://justfreefonts.com/fonts/bricolage-grotesque/)
- [@fontsource/bricolage-grotesque (npm)](https://www.npmjs.com/package/@fontsource/bricolage-grotesque)
- [Figtree font details and licence (Fontsource)](https://fontsource.org/fonts/figtree/about)
- [Figtree (Bunny Fonts)](https://fonts.bunny.net/family/figtree)

The developer must confirm these against the actual `OFL.txt` files in google/fonts. This ADR records the expected values, not a substitute for reading the files.
