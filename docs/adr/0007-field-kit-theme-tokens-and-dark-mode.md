# ADR 0007: Field Kit theme: token-named colour roles, Material 3 mapping, dark mode and accessibility overrides

- Status: Accepted
- Date: 2026-10-22
- Run: `20261022-ux-redesign`

## Context
The redesign (spec `docs/specs/20261022-ux-redesign.md`) replaces the MVP's minimal theme with the "Field Kit" tokens in the UX pack's `tokens.json`:
- colour roles for light and dark
- an always-dark recording palette
- three rating palettes per theme
- type, spacing, radius and size tokens.

Several forces shape how they map onto Compose:
- **Roles Material 3 lacks.** Some token roles have no Material 3 `ColorScheme` slot: textTertiary, iconMuted, divider, primaryPressed, errorText, errorOutline, navIndicator, scrim, and the rating and recording palettes.
- **Exact values.** AC1-AC3 require the theme's roles to equal the token values exactly. AC4 requires the recording palette in both system themes.
- **Contrast floors.** Every text pair must meet the AC6 floors in light, dark and recording. Where the pack's pair fails, AC40 asks for a colour from the token roles.
- **The MVP contrast tests.** `ContrastTest` and `ProfileContrastTest` are MVP AC51 tests that AC64 keeps in force, unloosened.
  - One assertion checks that `ColorScheme.outline` has at least 3:1 against `ColorScheme.surface` ("border").
  - The token `outline` fails it: `#CDD7E3` on `#FFFFFF` is 1.46:1, and the dark `#4A5E78` on `#132233` is 2.43:1.
- **System theme only.** Dark mode follows the system theme, with no in-app setting (A2). A theme change must redraw without a restart or data loss (AC7).

## Options considered
1. **Token-named theme objects plus a derived Material 3 `ColorScheme`** (chosen)
   - Pros:
     - AC1-AC4 can be tested against roles named exactly as in `tokens.json`.
     - Material 3 components still get sensible colours.
     - Accessibility overrides are explicit and confined to the Material 3 mapping.
   - Cons: there are two views of the colours to keep consistent. A test covers this.
2. **Material 3 `ColorScheme` only, with the extra roles squeezed into unused slots** (for example `tertiary` = textTertiary)
   - Pros: one object.
   - Cons:
     - The role names would mislead.
     - It cannot hold the rating and recording palettes.
     - AC1 could not be tested by token name.
3. **Do nothing (keep the MVP theme)**
   - Fails AC1-AC6. **Rejected.**

## Decision
Option 1.

### Theme objects (`ui/theme/`)
- **`FieldKitColors`** (immutable).
  - One property per token in `color.light` / `color.dark`, named exactly as the token and holding exactly its value: background, surface, surfaceMuted, primaryContainer, navIndicator, onSurface, onSurfaceVariant, textTertiary, iconMuted, outline, outlineVariant, divider, primary, onPrimary, primaryPressed, error, onError, errorText, errorOutline and scrim.
  - The scrim is stored with its alpha: light `rgba(8,16,28,0.55)` is alpha 140/255, and dark `rgba(0,0,0,0.6)` is alpha 153/255.
  - The instances are `LightFieldKitColors` and `DarkFieldKitColors`.
  - Two derived roles are extra properties, kept separate and named for their use:
    - **`summaryCaption`** (AC40). The caption and week-range colour on the Walks this-week card, whose fill is `primary`.
      - **Light** uses the pack's `#CFE0F5` (pack §4.5), 6.10:1 on `#1B4F8F`.
      - **Dark** uses the **`primaryContainer` role `#1E3A5F`**, 6.18:1 on `#8CC2FF`.
      - The pack's `#CFE0F5` on dark `#8CC2FF` is about 1.4:1 and is never used.
      - `onPrimary` (8.08:1) would also pass. `primaryContainer` was chosen because it keeps the captions visibly secondary to the stat numbers, as the light artboard does.
    - **`controlBorder`**: the border of text inputs and of outlined, non-destructive buttons. It equals the **`iconMuted`** role in each theme: `#5A6678` light (5.82:1 on surface) and `#94A3B8` dark (6.28:1 on surface).
- **`RatingPalette`** for each of Go, Caution and Avoid.
  - Roles: container, onContainer, onContainerMuted, badge, onBadge, divider, bar, and driverHighlight (bg, ring) for Caution and Avoid.
  - Values: exactly `color.rating` (light) and `color.dark.rating` (dark).
  - Go has no driverHighlight, because a Go result never highlights a factor cell (AC17).
  - Bar heights are 44, 28 and 14 dp.
- **`RecordingPalette`**: background, card, mapPanel, mapPanelBorder, onDark, onDarkMuted, route, routeCasing, liveDot, stopBackground and stopForeground. The values are exactly `color.recording`. There is one instance, and it does not depend on the system theme (AC4).
- **Other token objects.** `FieldKitTypography`, `FieldKitShapes`, `FieldKitSpacing` and `FieldKitSizes` have one value per token in `type.scale`, `radius`, `space` and `size`, named as the token. Fonts are covered in ADR 0006.
- **Composition locals.** `LocalFieldKitColors`, `LocalRatingPalettes`, `LocalFieldKitTypography` and `LocalRecordingPalette`, read through an accessor object `FieldKit`, for example `FieldKit.colors.textTertiary`.
- **`LocalRatingColors` / `RatingColors`.** The existing MVP API, which the MVP `ContrastTest` reads, is kept.
  - Its three colours become each rating's `onContainer` value. That is the colour for rating-coloured text on a plain surface, for example the About table's column headers.
  - On `surface` they are about 12.6, 12.7 and 14.3:1 in light and 12.6, 12.4 and 11.0:1 in dark. So the MVP 7:1 assertions keep passing unchanged.

### Material 3 `ColorScheme` mapping (derived from `FieldKitColors`)
| Material 3 role | Field Kit role (same swap in light and dark) | Note |
|---|---|---|
| primary / onPrimary | primary / onPrimary | |
| primaryContainer / onPrimaryContainer | primaryContainer / onSurface | |
| secondaryContainer / onSecondaryContainer | navIndicator / primaryPressed | |
| background / onBackground | background / onSurface | |
| surface / onSurface | surface / onSurface | |
| surfaceVariant / onSurfaceVariant | surfaceMuted / onSurfaceVariant | |
| surfaceContainerLowest … surfaceContainerHighest, surfaceBright, surfaceDim | surface | Sheets, menus and cards use the token `surface` |
| **outline** | **iconMuted** (= `controlBorder`) | **Accessibility override.** Material 3 draws text-field and outlined-button borders with `outline`, and those borders identify a control. The MVP `ContrastTest` requires at least 3:1 on surface. The token `outline` (1.46:1 light, 2.43:1 dark) stays available as `FieldKitColors.outline` for decorative borders that do not identify a control: the dashed Can't-check card, and the refresh-button ring, where the icon identifies the button. |
| outlineVariant | outlineVariant | Card borders (decorative) |
| error / onError | error / onError | |
| scrim | scrim (alpha applied by the sheet) | |
| secondary, tertiary, inverse* | primary / onPrimary / onSurface family | Not used by the app's components. Set so that no Material default purple appears. |

Dynamic colour (Material You) is **not** used.

### Dark mode and theme switching (AC7)
- `DogWalkBuddyTheme` reads `isSystemInDarkTheme()` and provides the light or dark objects.
- `MainActivity` declares `android:configChanges="uiMode"`. A system theme change then reaches Compose as a configuration change, which recomposes the UI without re-creating the activity. Nothing restarts and no in-flight state is lost.
- `res/values/themes.xml` gets a light window theme and `res/values-night/themes.xml` a dark one. Both set `android:windowBackground` to the token `background`, so there is no flash of the wrong colour at cold start.

### Recording palette
`RecordingTheme { }` wraps the Recording screen. It provides `RecordingPalette` and a Material 3 `ColorScheme` built from it, so any Material component inside is also always dark, whatever the system theme.

### Map route on Walk detail
The tiles keep their single light style in both themes, because a dark map style is in Later. The route line therefore uses the **light** `primary` `#1B4F8F` with a `#FFFFFF` casing and start marker in both themes.

The constants live in `map/RouteStyle.kt`, outside the MapLibre adapter, so they stay covered by tests.

## Consequences
- **Easier:**
  - AC1-AC4 are plain unit tests against named roles.
  - Every text pair the screens use can be listed and asserted in `ContrastTest` (AC6). The design lists the pairs and their computed ratios.
- **Harder:**
  - Input and outlined-button borders are darker than the artboard (`iconMuted` instead of `outline`). This is a deliberate difference, listed for the AC66 comparison, required by the MVP AC51 contrast test that AC64 keeps in force.
  - Developers must use `FieldKit.colors` roles in screens, never literal hex values. Code review checks this.
- **Reversal:** the theme objects are self-contained in `ui/theme/`. Restoring the previous `Color.kt` and `Theme.kt` reverts the change. No data is affected.

## For new dependencies
None. Everything uses Compose Material 3 from the pinned BOM.
