# ADR 0008: Icons as bundled Material Symbols Rounded vector drawables (no icon library)

- Status: Accepted
- Date: 2026-10-22
- Run: `20261022-ux-redesign`

## Context
The redesign uses about 30 icons:
- navigation, factor and action icons
- state icons (offline, cloud off, map off)
- the three rating icons.

The UX pack (`tokens.json` → `icons`) says: "Use Material Symbols Rounded equivalents unless an ADR adds an icon set." The spec NFR says adding an icon library needs an ADR.

ADR 0001 already chose vector drawables in `res/drawable` over `material-icons-extended` for the three MVP rating icons.

AC18 requires the Avoid icon to be **an octagon with a cross**. The MVP `ic_rating_avoid.xml` is a circle with a cross, so it must be replaced.

Icons must add no network request (AC65).

## Options considered
1. **Copy the needed Material Symbols Rounded glyphs into `res/drawable` as Android vector drawables** (chosen)
   - Pros:
     - No new Gradle dependency.
     - Only the glyphs used are shipped, a few KB in total.
     - Material Symbols is the set the pack names.
     - Same mechanism as the existing rating icons.
   - Cons: the files are added by hand. The source and licence are recorded per file.
2. **`androidx.compose.material:material-icons-extended`**
   - Pros: icons available as code.
   - Cons:
     - A large artifact, which ADR 0001 already rejected.
     - It is the older Material Icons set, not Material Symbols Rounded.
     - Google has deprecated it in favour of Material Symbols.
     - **Rejected.**
3. **Material Symbols as a bundled variable icon font**
   - Pros: every glyph in one file.
   - Cons:
     - A multi-megabyte font for about 30 glyphs.
     - Ligature-based icons are awkward for semantics and testing.
     - **Rejected.**
4. **Lucide (the artboards' SVG style)**
   - Pros: matches the artboard strokes exactly.
   - Cons:
     - The pack says to use Material Symbols unless an ADR adds a set.
     - It would need ISC licence notices.
     - **Rejected.**

## Decision
Option 1.

### Who adds the files
The **task 1 developer** (`theme-foundations`) copies the files into the repository. Nothing was downloaded at design time, and no other task adds icons unless it amends the table below in its PR.

### Source and style
- **Source:** https://github.com/google/material-design-icons, at a single commit recorded in the PR:
  - Fill 0 glyphs: `symbols/android/<name>/materialsymbolsrounded/<name>_24px.xml`.
  - Fill 1 glyphs: `symbols/android/<name>/materialsymbolsrounded/<name>_fill1_24px.xml`.
  - If a glyph's Android XML is not at that path, the developer may export the same glyph from https://fonts.google.com/icons with the Android format and the settings below, and say so in the PR.
- **Style:** Rounded, Fill 0, Weight 400, Grade 0, Optical size 24.
- **Exceptions:** the three rating icons and `play_arrow` and `stop` use Fill 1 so they read well at small sizes inside a filled badge or button.
- **Licence:** Apache License 2.0.
  - About already shows the full Apache License 2.0 text (MVP task 5). That meets the licence's requirement to give recipients a copy. The repository has no NOTICE file to reproduce.
  - The About text is not changed.
- **File header:** each copied XML file starts with a one-line comment giving the symbol name, "Material Symbols Rounded, Copyright Google LLC, Apache License 2.0", and the source path.
- **Integrity:** the PR records the source commit and the SHA-256 of each copied file. The only edits allowed are the header comment and the file rename.
- **Tint:** icons are tinted at use from theme roles. The drawable's own fill colour is irrelevant.

### Files (developer adds; renamed to the app's `ic_` convention)
| Use | Symbol | File |
|---|---|---|
| Nav: Today | `sunny` | `ic_nav_today.xml` |
| Nav: Walks | `route` | `ic_nav_walks.xml` |
| Nav: dog; profile photo placeholder | `pets` | `ic_nav_dog.xml` |
| Rating Go (check in a circle) | `check_circle` (fill 1) | `ic_rating_go.xml` (replaces the MVP file) |
| Rating Caution (triangle with !) | `warning` (fill 1) | `ic_rating_caution.xml` (replaces the MVP file) |
| Rating Avoid (octagon with a cross, AC18) | `dangerous` (fill 1) | `ic_rating_avoid.xml` (replaces the MVP file) |
| Factor Air | `thermostat` | `ic_factor_air.xml` |
| Factor Rain | `water_drop` | `ic_factor_rain.xml` |
| Factor Wind | `air` | `ic_factor_wind.xml` |
| Factor Pavement | `road` | `ic_factor_pavement.xml` |
| Refresh, Try again | `refresh` | `ic_refresh.xml` |
| Footnote, About tile | `info` | `ic_info.xml` |
| Row chevron | `chevron_right` | `ic_chevron_right.xml` |
| Back | `arrow_back` | `ic_back.xml` |
| Start walk | `play_arrow` (fill 1) | `ic_play.xml` |
| Stop | `stop` (fill 1) | `ic_stop.xml` |
| Delete buttons and sheets | `delete` | `ic_delete.xml` |
| Privacy tile | `shield` | `ic_shield.xml` |
| Offline banner | `wifi_off` | `ic_wifi_off.xml` |
| Can't check | `cloud_off` | `ic_cloud_off.xml` |
| Meta line (fresh) | `location_on` | `ic_location.xml` |
| Location-needed illustration | `my_location` | `ic_my_location.xml` |
| Meta line (age) | `schedule` | `ic_schedule.xml` |
| Change or choose photo | `image` | `ic_image.xml` |
| Walk detail map-off | `explore_off` | `ic_map_off.xml` |
| History row "More" | `more_vert` | `ic_more.xml` |
| Location-needed bullets | `check` | `ic_check.xml` |
| Privacy "Never leaves this phone" | `smartphone` | `ic_phone.xml` |
| Privacy "Only sent when you use a feature" | `arrow_outward` | `ic_send_out.xml` |
| Recording tip; precise-location illustration | `directions_walk` | `ic_walk.xml` |
| Walks empty state | `route` (same file as the nav icon) | `ic_nav_walks.xml` |

If a named symbol does not exist in Material Symbols Rounded, the developer:
1. picks the closest one
2. names it in the PR
3. keeps the file name above.

The rating icons must keep the AC18 shapes. That is checked by the manual AC61 greyscale check and the AC66 comparison.

The existing notification icon `ic_notification_walk.xml` and the launcher icon are unchanged.

## Consequences
- **Easier:**
  - There is no dependency or build change, and the icons work offline.
  - Each file is reviewable and carries its own provenance.
- **Harder:** a new icon means copying another file with the same header. This is acceptable for an app of this size.
- **Reversal:** replace or delete the files. Nothing else depends on the mechanism.

## For new dependencies
- **Licence:** Apache License 2.0. The licence text is already shown in About.
- **Maintenance:** Google maintains the source repository. The copied XML files are static and need no updates.
- **Known vulnerabilities:** none. They are vector path data, with no code.
- **Size / performance impact:** about 30 small XML vectors, roughly 1 KB each before compilation. There is no runtime cost beyond normal drawable inflation.
