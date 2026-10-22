# ADR 0008: Icons as bundled Material Symbols Rounded vector drawables (no icon library)

- Status: Accepted (amended 2026-10-22, see "Amendment 1")
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
- Nothing was downloaded at design time.
- The **task 1 developer** (`theme-foundations`) copies the three rating icons, which replace MVP files that are already in use, and records the source commit in the PR. That commit is the one every later icon is copied from.
- Every other icon is added by the task that first uses it (the "Planned task" column below), from the same recorded commit, with the same header, checksums and PR record.
  - Reason: the build runs Android Lint with `warningsAsErrors = true` (`app/build.gradle.kts`), and Lint's `UnusedResources` check fails on a drawable that no code references yet. An icon therefore cannot be added before the code that uses it.
  - If the tasks merge in a different order, the first task that uses an icon adds it, and later tasks reuse the file.
- No task adds an icon that is not in the table unless it amends the table in its PR.

### Source and style
- **Source:** https://github.com/google/material-design-icons, at a single commit recorded in the task 1 PR:
  - Fill 0 glyphs: `symbols/android/<name>/materialsymbolsrounded/<name>_24px.xml`.
  - Fill 1 glyphs: `symbols/android/<name>/materialsymbolsrounded/<name>_fill1_24px.xml`.
  - If a glyph's Android XML is not at that path, the developer may export the same glyph from https://fonts.google.com/icons with the Android format and the settings below, and say so in the PR.
- **Style:** Rounded, Fill 0, Weight 400, Grade 0, Optical size 24.
- **Exceptions:** the three rating icons and `play_arrow` and `stop` use Fill 1 so they read well at small sizes inside a filled badge or button.
- **Licence:** Apache License 2.0.
  - About already shows the full Apache License 2.0 text (MVP task 5). That meets the licence's requirement to give recipients a copy. The repository has no NOTICE file to reproduce.
  - The About text is not changed.
- **File header:** each copied XML file starts with a one-line comment giving:
  - the symbol name
  - "Material Symbols Rounded, Copyright Google LLC, Apache License 2.0"
  - the source path
  - the statement "modified: header comment added". Apache License 2.0 §4(b) requires a modified file to carry a prominent notice that it was changed, and adding this comment is a change.
- **Integrity:** the PR records the source commit and, for each file, the SHA-256 of the upstream file as downloaded (before the header is added) and the SHA-256 of the committed file. The only edits allowed are the header comment and the file rename. The PR lists each rename.
- **Tint:** icons are tinted at use from theme roles. The drawable's own fill colour is irrelevant.

(The header statement, the allowed edits and the tint attribute are amended by "Amendment 1" below.)

### Files (renamed to the app's `ic_` convention)
| Use | Symbol | File | Planned task |
|---|---|---|---|
| Nav: Today | `sunny` | `ic_nav_today.xml` | 2 |
| Nav: Walks | `route` | `ic_nav_walks.xml` | 2 |
| Nav: dog; profile photo placeholder | `pets` | `ic_nav_dog.xml` | 2 |
| Rating Go (check in a circle) | `check_circle` (fill 1) | `ic_rating_go.xml` (replaces the MVP file) | 1 |
| Rating Caution (triangle with !) | `warning` (fill 1) | `ic_rating_caution.xml` (replaces the MVP file) | 1 |
| Rating Avoid (octagon with a cross, AC18) | `dangerous` (fill 1) | `ic_rating_avoid.xml` (replaces the MVP file) | 1 |
| Factor Air | `thermostat` | `ic_factor_air.xml` | 3 |
| Factor Rain | `water_drop` | `ic_factor_rain.xml` | 3 |
| Factor Wind | `air` | `ic_factor_wind.xml` | 3 |
| Factor Pavement | `road` | `ic_factor_pavement.xml` | 3 |
| Refresh, Try again | `refresh` | `ic_refresh.xml` | 3 |
| Footnote, About tile | `info` | `ic_info.xml` | 2 |
| Row chevron | `chevron_right` | `ic_chevron_right.xml` | 6 |
| Back | `arrow_back` | `ic_back.xml` | 2 |
| Start walk | `play_arrow` (fill 1) | `ic_play.xml` | 3 |
| Stop | `stop` (fill 1) | `ic_stop.xml` | 5 |
| Delete buttons and sheets | `delete` | `ic_delete.xml` | 6 |
| Privacy tile | `shield` | `ic_shield.xml` | 2 |
| Offline banner | `wifi_off` | `ic_wifi_off.xml` | 3 |
| Can't check | `cloud_off` | `ic_cloud_off.xml` | 4 |
| Meta line (fresh) | `location_on` | `ic_location.xml` | 3 |
| Location-needed illustration | `my_location` | `ic_my_location.xml` | 4 |
| Meta line (age) | `schedule` | `ic_schedule.xml` | 3 |
| Change or choose photo | `image` | `ic_image.xml` | 8 |
| Walk detail map-off | `explore_off` | `ic_map_off.xml` | 7 |
| History row "More" | `more_vert` | `ic_more.xml` | 6 |
| Location-needed bullets | `check` | `ic_check.xml` | 4 |
| Privacy "Never leaves this phone" | `smartphone` | `ic_phone.xml` | 7 |
| Privacy "Only sent when you use a feature" | `arrow_outward` | `ic_send_out.xml` | 7 |
| Recording tip; precise-location illustration | `directions_walk` | `ic_walk.xml` | 4 (or 5, if task 5 merges first) |
| Walks empty state | `route` (same file as the nav icon) | `ic_nav_walks.xml` | 2 |

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
- **Harder:**
  - A new icon means copying another file with the same header. This is acceptable for an app of this size.
  - Icons arrive over several PRs, so each of those PRs repeats the provenance record (commit, checksums, renames) for the files it adds.
- **Reversal:** replace or delete the files. Nothing else depends on the mechanism.

## For new dependencies
- **Licence:** Apache License 2.0. The licence text is already shown in About. Each copied file states that it was modified (§4(b)).
- **Maintenance:** Google maintains the source repository. The copied XML files are static and need no updates.
- **Known vulnerabilities:** none. They are vector path data, with no code.
- **Size / performance impact:** about 30 small XML vectors, roughly 1 KB each before compilation. There is no runtime cost beyond normal drawable inflation.

## Amendment 1 (2026-10-22): the tint attribute
Raised by the task 1 developer on PR #40 (task #32).

### Problem
- Upstream Material Symbols Android XML sets `android:tint="?attr/colorControlNormal"` on the `<vector>`.
- `colorControlNormal` without the `android:` prefix is an attribute declared by AppCompat or Material Components. This app has neither, so aapt fails with "resource attr/colorControlNormal not found".
- This ADR allowed only the header comment and the rename, so the developer could not change the glyph file.

### Options considered
1. **Declare an app attribute `colorControlNormal` in `res/values/attrs.xml` and set it in both window themes to `?android:attr/colorControlNormal`.** The icon files stay byte-identical to upstream below the header.
   - The reference only resolves when the drawable is inflated with a theme that sets the app attribute, which means `Theme.DogWalkBuddy` or a child of it.
   - Compose `painterResource` parses the vector itself and reads `android:tint` through the `LocalContext` theme. If that theme does not set the attribute, the value stays an unresolved theme reference. androidx.core's `TypedArrayUtils.getNamedColorStateList` then throws `UnsupportedOperationException("Failed to resolve attribute ...")`. A tint passed in code (`Icon(tint = ...)`) does not help, because the error happens while the XML is parsed, before any colour filter is applied.
   - Such themes already exist in this project. `createComposeRule()` hosts content in `androidx.activity.ComponentActivity` from `ui-test-manifest`, and that library's manifest (1.12.x, the version in use) gives the activity `@android:style/Theme.Material.Light.NoActionBar`, not the app theme. `CheckCardTest` renders all three rating icons in that host. Compose previews and any later host activity or dialog window with its own theme are in the same position.
   - It also adds an app attribute that shadows an AppCompat/MDC name. If either library is ever added, its `colorControlNormal` (format `color`) would clash with this one (format `color|reference`).
   - **Rejected:** whether it works depends on the theme of whoever inflates the icon, and that is easy to break without noticing.
2. **Change one token per icon file: `?attr/colorControlNormal` to `?android:attr/colorControlNormal`** (chosen).
   - The platform attribute exists in every platform theme from API 21 (minSdk is 29), so the file resolves in any context: the app, test host activities, previews.
   - At run time it makes no visible difference. Screens tint icons in code from `FieldKit` roles (the "Tint" rule above). In Compose, a colour filter passed in code replaces the drawable's own tint.
   - No attribute, theme item or resource is added. Lint has nothing new to check and `UnusedResources` is not affected.
   - Apache §4(b): the file changes in one more place, and the header's modification notice says so (below).

### Decision
Option 2. For every Material Symbols file, in task 1 and every later task:
- **Allowed edits** (this replaces the list under "Integrity"):
  1. the header comment
  2. the file rename
  3. exactly one token on the `<vector>` element: `android:tint="?attr/colorControlNormal"` becomes `android:tint="?android:attr/colorControlNormal"`.
  Nothing else changes. Path data, size, viewport and `android:fillColor` stay as they are upstream.
- **Header statement** (this replaces "modified: header comment added"): end the one-line header with
  `modified: header comment added; tint changed from ?attr/colorControlNormal to ?android:attr/colorControlNormal`.
  If an upstream file has no `?attr/` tint, it gets no tint edit and its header keeps "modified: header comment added".
- **Integrity record:** unchanged in form. The PR records the SHA-256 of the upstream file as downloaded and the SHA-256 of the committed file. A reviewer can check a file by removing the header line and reversing the one tint edit; the result must match the upstream checksum.
- **No theme support:** do not add `res/values/attrs.xml` or a `colorControlNormal` theme item. The window themes keep only what ADR 0007 gives them (`android:windowBackground`).
- **Guard test (task 1):** add a JVM unit test that reads every `src/main/res/drawable/ic_*.xml` whose first line contains "Material Symbols Rounded" (the same file-reading approach as `SourceManifestHardeningTest`). It asserts that:
  - the file contains no `?attr/` reference
  - the header contains `modified:`.
  Later tasks get this check for free. They must not change it.

### What later tasks (2-8) must do
- Copy each icon from the recorded commit, apply only the three allowed edits, use the header statement above, and record both checksums in the PR.
- Run the full suite. The guard test fails if a copied file still has `?attr/colorControlNormal`.

### What PR #40 must change
- `app/src/main/res/drawable/ic_rating_go.xml`, `ic_rating_caution.xml`, `ic_rating_avoid.xml`: apply the tint edit and the new header statement. Update the committed-file SHA-256 values in the PR description. The upstream values stay the same.
- Delete `app/src/main/res/values/attrs.xml`.
- Remove `<item name="colorControlNormal">?android:attr/colorControlNormal</item>` from `app/src/main/res/values/themes.xml` and `app/src/main/res/values-night/themes.xml`.
- Add the guard test above.
- `app/src/main/res/values/colors.xml` (the two `field_kit_background_*` window colours) stays. It is part of the ADR 0007 window themes and in scope for task 1 (see the design's task 1 scope note).
