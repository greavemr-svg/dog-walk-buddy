# Design: UX redesign ("Field Kit", blue) before the first release

- Run: `20261022-ux-redesign`
- Spec: `docs/specs/20261022-ux-redesign.md` (owner-approved at `c0beafb`)
- Design input (read by absolute path, not copied into the repo): `C:\Users\greav\projects\dog-walk-buddy-ux\` (README rules binding, `ux-spec.md`, `tokens.json`, `screens\*.dc.html`)
- ADRs:
  - `docs/adr/0006-bundled-fonts-bricolage-grotesque-and-figtree.md` (Accepted)
  - `docs/adr/0007-field-kit-theme-tokens-and-dark-mode.md` (Accepted)
  - `docs/adr/0008-icons-as-bundled-material-symbols-vector-drawables.md` (Accepted; Amendment 1, 2026-10-22, icon tint attribute)
  - ADR 0001-0005 still apply unchanged.
- Owner decisions on this design: OD1 (test changes E1-E8 approved) and OD2 (row Delete moves into a "More" menu), both 2026-10-22T10:59:00Z. See "Owner decisions (design)".
- Architecture note during task 1 (2026-10-22, PR #40): ADR 0008 Amendment 1 (icon tint) and the task 1 scope note under "Task details". Task 1 also covers `res/values/colors.xml`, the `MainActivity.onConfigurationChanged` system-bar update, and an icon-file guard test. No AC, task order or other task changes.
- Revision: design attempt 3 (G2 fix, `docs/runs/20261022-ux-redesign/gates/G2-attempt2.md`).
  - E3 now says where the `HistoryScreenTest.kt` L270-276 Back check goes (`NavigationTest` AC35 test, system Back), what happens to the old test, and why the new check is at least as strict.
  - E3 now lists every argument-only edit that removing `onBack` from History and Profile forces in `app/src/test` (no assertion changes).
  - Nothing else changed from attempt 2.
- Attempt 2 (G2 fix, `docs/runs/20261022-ux-redesign/gates/G2-attempt1.md`):
  - The Task breakdown AC column now matches the Traceability table for every AC (AC40, AC53, AC64, AC66 and AC67 rows corrected).
  - The task-2 home-link row lists every affected test line, including `CheckScreenIntegrationTest.kt` L245.
  - E3 says where the ProfileScreenTest L321 assertion goes.
  - Task 1 is smaller: icons other than the three rating icons move to the task that first uses each (ADR 0008), `FieldKitBottomSheet` moves to task 4, and the accessibility helper extensions move to task 2.
  - The contrast rounding wording and the ADR 0008 file header are corrected.

## Approach
This run is a presentation change on top of the finished MVP app. The domain rules, the data layer, the recording service, the permissions model and the hosts do not change (spec A1, Out of scope). The work has five layers:

1. **Foundations** (ADR 0006, 0007, 0008).
   - A token-named theme: `FieldKitColors`, `RatingPalettes`, `RecordingPalette`, `FieldKitTypography`, `FieldKitShapes`, `FieldKitSpacing` and `FieldKitSizes`.
   - A Material 3 `ColorScheme` derived from those roles.
   - Two bundled variable fonts and about 30 bundled Material Symbols vector drawables. Each icon is added by the task that first uses it (ADR 0008).
   - Dark mode follows the system theme. The activity handles `uiMode` itself, so a theme switch only recomposes.
2. **App shell.** One `Scaffold` holds the `NavHost` and a custom bottom navigation bar with three top-level destinations: Today, Walks and the dog.
   - The bar appears only on top-level destinations.
   - Recording, Walk detail, Privacy and About are full-screen destinations with a back control.
   - The MVP Home link list is deleted.
3. **Today.** The check screen is rebuilt from components:
   - header
   - cached/offline banner
   - rating hero card with factor grid
   - footnote block
   - best-window card with the 12-hour strip
   - a Start walk button pinned above the nav.

   `CheckViewModel` gains the data the components need. All of it is data the app already has (A1).
4. **Other screens.** Recording, Walks, Walk detail, Dog profile, Privacy and About are restyled to the pack, with the approved copy only.
5. **Tests.**
   - Every new behaviour gets a test named `UX_AC<n>_…`, so it cannot be confused with the MVP's `AC<n>_…` tests.
   - The MVP suite stays in force. Its assertions change only as listed in "Test assertion changes", which repeats the spec list and adds the further changes the ACs force (E1-E8, owner-approved in OD1).

### Precedence applied in this design
In order:
1. The MVP spec.
2. Existing strings.
3. Accessibility floors.
4. This run's spec.
5. The pack.

Every place where this design departs from an artboard is listed in "Differences from the artboards (AC66)".

## Components touched
Paths are under `app/src/main/java/io/github/greavemrsvg/dogwalkbuddy/` and `app/src/main/res/` unless stated. Tests are under `app/src/test/java/io/github/greavemrsvg/dogwalkbuddy/`.

| Component / module | Change | Task(s) |
|---|---|---|
| `ui/theme/` (`Color.kt`, `Theme.kt`, `Type.kt`, new `Dimens.kt` and `RecordingTheme.kt`) | Field Kit token objects, Material 3 mapping, typography with bundled fonts, shapes, spacing, sizes, dark mode, the always-dark recording theme (ADR 0006, 0007) | 1 |
| `res/font/`, `res/raw/licence_ofl_*.txt`, `res/values/themes.xml`, new `res/values-night/themes.xml`, `res/values/colors.xml` | Two font files, two licence texts, light and night window themes, and the two window-background colours they use (ADR 0006, 0007) | 1 |
| `res/drawable/ic_*.xml` | Three rating icons replaced (1). Every other icon is added by the task that first uses it, as planned in ADR 0008, because Lint `UnusedResources` fails on an unreferenced drawable. Tint attribute per ADR 0008 Amendment 1 | 1-8 |
| `AndroidManifest.xml`, `MainActivity.kt` | `android:configChanges="uiMode"` on `MainActivity` only; `MainActivity.onConfigurationChanged` calls `enableEdgeToEdge()` again so the system bar icon style follows the theme (task 1 scope note) | 1 |
| `ui/common/` (new) | Shared components: `FieldKitBottomSheet`, `BackButton`, `PrimaryButton` / `StartWalkButton`, `InfoBanner`, `PermissionExplainer`, `StatCard`, `SpokenDuration`, `ConfirmSheet`, `DestructiveOutlinedButton` | 2-7 (each in the first task that needs it) |
| `ui/a11y/AccessibilityAssertions.kt` (test helper) | Extended helpers: `assertHeadingsMarked(...)`, `assertHiddenFromAccessibility(tag)`, `assertNoTextTruncation(allowEllipsisTags)` | 2 |
| `ui/nav/`, `MainActivity.kt`, `ui/home/` (deleted) | `AppScaffold`, `FieldKitNavBar`, `AppChromeViewModel`, `TOP_LEVEL` routes; Home screen removed | 2 |
| `domain/rating/`, `domain/check/` | Additive only: `pavementFactor`, `FactorRounding`, `driverOf` / `FactorCell`, new `CheckOutcome.Rated` fields. `rate()` unchanged | 3 |
| `ui/check/` | Today rebuilt: header, banner, hero and factor grid, footnote, loading (3); best-window card, 12-hour strip, Can't check, location states (4) | 3, 4 |
| `ui/recording/` | Start section and precise-location sheet (3, 4); Recording screen restyle, timer, stat cards, live route sketch (5) | 3, 4, 5 |
| `domain/walk/` | `LiveWalkFormat` h:mm:ss, new `RouteProjection` (5); `WeeklyTotals.dayDots` (6) | 5, 6 |
| `data/db/WalkDao.kt`, `data/repo/WalkRepository.kt` | One new read-only Flow query, `observePoints(walkId)`. No schema change | 5 |
| `ui/history/` | Walks tab: `WalkDateText`, this-week card and dots, week sections, rows with "More" menu (OD2), delete sheet | 6 |
| `ui/walkdetail/`, new `map/RouteStyle.kt`, `map/maplibre/MapLibreRouteMap.kt` | Edge-to-edge map, route casing and start marker, sheet, delete sheet. `RouteMap` interface unchanged | 7 |
| `ui/privacy/` | Cards, headline, delete-all sheet. Delete logic unchanged | 7 |
| `ui/profile/` | Back removed and tiles added (2); photo row, counters, radio cards, Save restyle (8). Validation unchanged | 2, 8 |
| `ui/about/` | Font licence sections (1); back control (2); headline, intro, thresholds table, panel (8) | 1, 2, 8 |
| `res/values/strings.xml` | New and changed strings per "Approved copy by task"; strings made unused are deleted in the task that stops using them | 1-8 |
| Tests (`ui/**`, `domain/**`, `data/repo/**`, `testutil/`) | New `UX_AC<n>_…` tests; MVP assertions changed only as listed in "Test assertion changes" | 1-8 |
| **Not touched** | Room schema and migrations, `service/` recording logic, location and permission code, `data/weather/` client, network hosts, Gradle build files and dependencies, `.github/`, `scripts/` | — |

## Theme architecture (task 1)

### Colour: token roles (AC1-AC4)
`ui/theme/Color.kt` holds `LightFieldKitColors`, `DarkFieldKitColors`, `LightRatingPalettes`, `DarkRatingPalettes` and `RecordingPalette`. The values are exactly `tokens.json` → `color.light`, `color.dark`, `color.rating`, `color.dark.rating` and `color.recording`.

Two values are derived and kept as separate, named properties (ADR 0007):
- **`summaryCaption`.** Light `#CFE0F5` (pack §4.5). Dark is the **`primaryContainer` role `#1E3A5F`** (AC40).
- **`controlBorder`.** This is the `iconMuted` role: light `#5A6678`, dark `#94A3B8`.

Material 3 mapping (light and dark use the same role-for-role swap; ADR 0007 has the full table):

| Material 3 role | Field Kit role |
|---|---|
| primary / onPrimary | primary / onPrimary |
| primaryContainer / onPrimaryContainer | primaryContainer / onSurface |
| secondaryContainer / onSecondaryContainer | navIndicator / primaryPressed |
| background / onBackground | background / onSurface |
| surface / onSurface, all surfaceContainer* | surface / onSurface |
| surfaceVariant / onSurfaceVariant | surfaceMuted / onSurfaceVariant |
| **outline** | **iconMuted** (accessibility override: the MVP `ContrastTest` needs at least 3:1 for control borders; token `outline` is 1.46:1 light and 2.43:1 dark) |
| outlineVariant | outlineVariant |
| error / onError | error / onError |
| scrim | scrim |

- **Rating colours.** `LocalRatingColors` (MVP API) is kept. Its values become each rating's `onContainer`, for rating-coloured text on plain surfaces such as the About table headers. The MVP `ContrastTest` key-information assertions then pass unchanged.
- **Recording.** `RecordingTheme {}` provides `RecordingPalette` and a dark Material 3 scheme built from it, independent of `isSystemInDarkTheme()` (AC4).

### Typography (AC5)
`ui/theme/Type.kt` defines `FieldKitTypography`, with one `TextStyle` per token in `type.scale`, named as the token:

| Token | Family | Size (sp) / weight | Other | Material 3 slot it also fills |
|---|---|---|---|---|
| ratingWord | Bricolage (opsz 60) | 60 / 800 (52 for "Caution", per token note) | line height 0.95em, letter spacing -0.02em | displayMedium |
| timer | Bricolage (opsz 92) | 92 / 700 | `fontFeatureSettings = "tnum"` | displayLarge |
| screenTitle | Bricolage (opsz 32) | 32 / 800 | line height 1.1em | headlineLarge |
| checkQuestion | Bricolage (opsz 27) | 27 / 700 | line height 1.1em | headlineMedium |
| statLarge | Bricolage (opsz 36) | 36 / 700 | tnum | displaySmall |
| bestWindow | Bricolage (opsz 24) | 24 / 700 | | headlineSmall |
| reason | Figtree | 22 / 700 (21 for the long pavement reasons) | line height 1.25em | titleLarge |
| factorValue | Figtree | 17 / 700 | | titleMedium |
| button | Figtree | 17 / 700 (Start 20 / 700, Stop 21 / 800) | | labelLarge |
| body | Figtree | 15 / 400 | line height 1.5em | bodyMedium (bodyLarge 16 / 400) |
| label | Figtree | 13 / 600 | | labelMedium |
| caption | Figtree | 12 / 500 | line height 1.4em | bodySmall |
| navLabel | Figtree | 12 / 600, active 700 | | (Field Kit only) |
| axisLabel | Figtree | 11 / 600 | | labelSmall |

**Large type at 200 % (AC27, AC38, AC60).** Single-line numerals and the rating word use `BasicText` with `TextAutoSize.StepBased`:
- The maximum is the token size scaled by the font scale.
- The floor is 36 sp for ratingWord and timer and 24 sp for statLarge, so the text is never smaller than the MVP's 200 % headline.
- With `maxLines = 1` they cannot truncate.

Everything else wraps and its container scrolls.

### Shape, spacing and size
`FieldKitShapes`, from `radius`:
- hero 28, card 22, sheet 30 (top corners), startButton 22, button 18, input 16, pill = 50 %, iconTile 14.
- The best-window card uses 24, as the artboard does.

Material 3 `Shapes`: small = input 16, medium = button 18, large = card 22, extraLarge = hero 28.

`FieldKitSpacing` (`space`): gutter 16, headerSide 20, cardPadding 18, heroPadding 22, stackGap 12, sectionGap 18, rowGap 14.

`FieldKitSizes` (`size`):
- minTouchTarget 48 (the existing `MinTouchTarget`)
- startStopHeight 72 (the existing `StartStopMinHeight`)
- stopHeight 76, primaryButtonHeight 56, permissionButtonHeight 60, inputHeight 52, iconButton 48
- ratingBadge 60, bottomNavHeight 78 (a **minimum**, see Navigation), navIndicator 60 x 32
- listRowMinHeight 64, avatar 88
- icons: nav 22, inline 18, small 14.

### Icons
Bundled Material Symbols Rounded vector drawables, listed in ADR 0008. The MVP `ic_rating_avoid.xml` (circle with a cross) is replaced by `dangerous` (octagon with a cross), as AC18 requires.
- Task 1 replaces only the three rating icons, which the MVP code already uses, and records the source commit.
- Every other icon is added by the task that first uses it, from that commit (ADR 0008 "Planned task" column). Lint `UnusedResources` runs with `warningsAsErrors = true`, so a drawable cannot land before its first use.
- **Tint attribute (ADR 0008 Amendment 1).** Each copied file's `android:tint="?attr/colorControlNormal"` becomes `?android:attr/colorControlNormal`, and the header notice says so. The app declares no `colorControlNormal` attribute or theme item, so the icons resolve in any theme, including the `createComposeRule()` test host. A guard test added in task 1 fails on any Material Symbols file that still has an `?attr/` reference.

### Shared components (in `ui/common/`)
Each component is created by the first task that needs it.

| Component | Task | Spec |
|---|---|---|
| `BackButton` | 2 | 48 dp circular icon button, `ic_back`, content description "Back" (the existing `*_back` string). Variant `floating = true`: `surface` fill and a soft shadow, for the map |
| `FieldKitNavBar` | 2 | See Navigation |
| `PrimaryButton` / `StartWalkButton` | 3 | Start: full width in the 16 dp gutter, min height 72, radius 22, `primary` / `onPrimary`, play icon + "Start walk" (20 / 700) |
| `InfoBanner` | 3 | `surfaceMuted`, radius 16, icon + text (13 sp). Optional bold lead span |
| `FieldKitBottomSheet` | 4 | Material 3 `ModalBottomSheet`, `skipPartiallyExpanded = true`, top radius 30, drag handle, Field Kit scrim. Closes on scrim tap, system Back or drag. First used by the precise-location sheet |
| `PermissionExplainer` | 4 | Decorative illustration (hidden from accessibility), heading, body, check-mark bullets, primary button (min 60), optional link |
| `StatCard` | 5 | One accessibility node (`mergeDescendants`): caption + value. Recording uses the recording palette; Walk detail uses `surface` |
| `ConfirmSheet` | 6 | On `FieldKitBottomSheet`: tinted delete-icon circle, title (display 26 / 800), body (15), full-width buttons at least 56 high: destructive (`error` / `onError`), then Cancel (`surfaceMuted` / `onSurface`) |
| `DestructiveOutlinedButton` | 7 | Min height 56, radius 18, `surface`, 1.5 dp `errorOutline` border, `errorText` label + delete icon |

## Navigation (task 2)
- **Routes.** `Routes.HOME` (string `home`) is kept as Today, to avoid churn. `HISTORY` is Walks and `PROFILE` is the dog. `TOP_LEVEL = setOf(HOME, HISTORY, PROFILE)`. Full-screen routes are `RECORDING`, `WALK_DETAIL`, `PRIVACY` and `ABOUT`.
- **Shell.** `AppScaffold` is a `Scaffold`. Its `bottomBar` is `FieldKitNavBar`, shown only when the current back-stack entry's route is in `TOP_LEVEL`. `contentWindowInsets` keeps the edge-to-edge handling.
- **Tab switching.** `navController.navigate(route) { popUpTo(graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }`.
- **System Back (AC11).**
  - From Walks or the dog tab, Back pops to the start destination, Today, whose item then becomes selected.
  - On Today, nothing is left to pop, so the activity finishes.
  - There is no custom `BackHandler`.
- **`FieldKitNavBar`.**
  - A custom `Row`, not Material 3 `NavigationBar`, because that has a fixed height and AC60(a) lets "Today", "Walks" and "Dog" wrap at 200 %.
  - `surface` fill, 1 dp top border `outlineVariant`, `heightIn(min = 78.dp)` plus the navigation-bar inset.
  - Three equal-weight items, each `Modifier.selectable(selected, role = Role.Tab)` with merged descendants. Each is at least 80 x 56, so well over 48 x 48.
  - Item content: a 60 x 32 pill in `navIndicator` behind the 22 dp icon when selected; then the label in navLabel, coloured `primaryPressed` at weight 700 when selected and `onSurfaceVariant` at 600 otherwise.
  - **Test tags.** `NAV_TODAY_TAG`, `NAV_WALKS_TAG` and `NAV_DOG_TAG` on the three items, so tests can tap the dog item whatever its label (the saved name or "Dog").
  - **Dog label (AC9, AC15, AC60).** The saved name, or "Dog" with no profile (`nav_dog`).
    - `AppChromeViewModel` reads it from `ProfileRepository.observeProfile().map { it?.name }`. "Dog" shows until the name has loaded.
    - The name label has `maxLines = 1` and `overflow = TextOverflow.Ellipsis`. The "Today", "Walks" and "Dog" labels have no line limit.
    - The semantics text is always the full string, so TalkBack reads the full name.
- **Back controls (AC12).** `BackButton` on Recording, Privacy and About (top bar), and floating on Walk detail. Tabs have no back control (AC39, AC50).
- **Entry points.**
  - Privacy is reached through the dog tab → "Privacy & data" tile (2 taps, AC13) and through Today's location explainer link "How your data is used".
  - About is reached through Today's footnote link (1 tap) and the dog tab → "About the advice" tile (AC14).
  - The no-profile footnote line navigates to the dog tab using the tab navigation, so the dog item becomes selected.
  - Task 2 adds the two tiles, with their `ic_shield` and `ic_info` icons, to the existing profile screen, because the Home links that reached Privacy and About are deleted in the same task. Task 8 restyles them.
  - Back from Privacy or About returns to the screen they were opened from. Opened from the dog tab, that is the dog tab.
- **Deleted.** `ui/home/HomeScreen.kt` and the strings only it used (`home_title`, `home_history`, `home_dog_profile`, `home_privacy`). `home_about` stays, because the check footnote uses it. `app_name` stays as the launcher label.

## Screen-by-screen changes

### Today (tasks 3 and 4)
Layout:

```
Column(fillMaxSize) {
  Column(weight 1f, verticalScroll) { header; banner?; state content; }
  StartWalkSection
}
```

The nav bar comes from the shell. Start and the nav are never inside the scroll, so they stay visible at 200 % (AC27).

**Order of the scrolling content (binding, AC25).** These orders are chosen so that MVP AC26 and AC27 hold at 411 x 891 dp in every rated state (K1; owner "Yes, move it up").
- **Rated:** header → banner (cached only) → hero card → **footnote block** → best-window card.
- **Loading:** header (title only) → skeleton hero → footnote block.
- **Can't check:** header (title only) → Can't-check card → "You can still record a walk" panel → footnote block.
- **Location needed / Open settings:** header (title only) → explainer → footnote block.

Estimated height at default size for the worst case in AC25 (Caution, offline banner, age label, Sensitive line, two-line title, three-line pavement reason): header about 120 dp, banner about 72, hero about 297, disclaimer and link about 96. That puts the link's bottom at about 585 dp. The scroll viewport is about 717 dp (891 minus Start 96 and nav 78). Placing the best-window card below the footnote is the difference listed for AC66.

**Header (AC20).** Padding 22 / 20.
- **Meta line** (label style, `onSurfaceVariant`):
  - `ic_location` + "Your area · checked just now" (D1) when `ageMinutes == null`.
  - Otherwise `ic_schedule` + the exact `check_age_*` text.
  - No meta line in Loading, Can't check or Location states.
- **Title** (checkQuestion, heading): "Good time to walk {name}?" (C1, new `check_title_named`) or the existing `check_title`.
- **Refresh:** a 48 dp circle, `surface` fill, 1 dp token `outline` ring (decorative; the icon identifies it), `ic_refresh`, content description "Refresh" (`check_refresh`). Tapping calls `viewModel.refresh()`. It is shown only in rated states.

**Banner (AC21, C12).** Shown only when the rated result `isCached`.
- If `!isOnline`: bold "You're offline." followed by "Showing your last check. It's kept for up to 3 hours."
- Otherwise only the second sentence.
- Icon `ic_wifi_off`. A fresh in-session result, even one old enough to show its age, has no banner.

**Hero card (AC17-AC19).**
- `rating.container` fill, radius 28, padding 22.
- Row: a 60 dp `badge` circle with the rating icon in `onBadge` (tag `RATING_ICON_TAG_PREFIX + resId`, kept), then the rating word (ratingWord, `onContainer`).
- Reason (reason style, `onContainer`).
- A 1 dp `rating.divider`.
- A 4-column factor grid. Each cell has:
  - an 18 dp icon
  - the value (factorValue, `onContainer`)
  - the caption (caption, `onContainerMuted`)
  - captions in order Air, Rain, Wind, Pavement.
- **The driver cell** (see "Factor grid data") gets `driverHighlight.bg` fill plus a 2 dp `driverHighlight.ring` inner border. The ring is the non-colour cue.
- **Semantics.** The whole card is one node (`clearAndSetSemantics`) with tag `RATING_BLOCK_TAG` (kept) and `liveRegion = Polite` (kept).
  - Content description: `"%1$s. %2$s."` (existing `check_rating_announcement`) followed by a space and the four cells as "<caption> <value>" joined with ", ".
  - Example: "Caution. Windy. Air 15°C, Rain 0 mm/h, Wind 26 mph, Pavement Not rated".
  - Cells carry a test-only `SemanticsPropertyKey<Boolean> FactorHighlighted` and tags `factor_air` / `factor_rain` / `factor_wind` / `factor_pavement`, checked in the unmerged tree.

**Footnote block (AC24).**
- Row: `ic_info` + the exact disclaimer (13 sp, `onSurfaceVariant`).
- An "About this advice" `TextButton` (bold, `primary`, at least 48 x 48). It is a separate clickable node, not an inline link, so the MVP test helper `hasText("About this advice") and hasClickAction()` still finds exactly one node.
- In rated states only:
  - Sensitive dog: "Heat-sensitive thresholds ({name})" (D2) as its own line.
  - No profile: the exact `check_default_sensitivity` as a `TextButton` (at least 48 dp) that opens the dog tab.
  - The exact pollen notice.
- Then the exact attribution (12 sp, `textTertiary`), in every state.
- Texts are never joined with " · " (K2, K9).

**Best-window card (AC22).**
- `surface`, 1 dp `outlineVariant`, radius 24, padding 16 / 18.
- Label "Best window, next 12 hours" (C2; `check_window_heading` value changes).
- When the window rating is Caution, the "Best available: Caution" label.
- The window text exactly as `BestWindowFormatter` gives it (bestWindow style). The hyphen range format is unchanged (C3 rejected).
- A right-aligned chip (pill, `rating.container` / `onContainer`) with the window rating's word. There is no chip for "No good time in the next 12 hours".
- The texts are merged into one node, as in the MVP.

**12-hour strip (AC23, AC61).** It is drawn from `hours: List<HourRating>`, up to 12, starting with the current hour.
- **Columns.** 12 equal columns with a 4 dp gap. N bars for N hours, and the remaining columns stay empty if fewer than 12 hours exist.
- **Bars.** Each bar is bottom-aligned, radius 6, coloured `rating.bar`, height 44 / 28 / 14 dp.
- **Marker.** A 3 dp `onSurface` line sits above the columns whose `startUtc` is in `[window.startUtc, window.endUtc)`. There is no line for `NoGoodTime`.
- **Hour labels.** One per column, in axisLabel style, from `HourLabelFormat(zone, is24Hour)`: pattern `HH` or `ha` in `Locale.UK`, which gives "14" or "2pm", "12am".
  - Labels are centred on their column and measured unconstrained (`wrapContentWidth(unbounded = true)`, `softWrap = false`), so they never truncate.
  - At the default size the widest pair ("10pm", "11pm", about 27 dp each) fits the 28.9 dp column pitch without overlap.
  - Above font scale 1.3 the strip shows every second or third label, whichever is needed to avoid overlap. The strip is decorative; AC23 requires every label only at the default viewport.
- **Semantics.** The strip is hidden from accessibility with `hideFromAccessibility()`. Bars carry a test-only `BarRating` property and a `strip_bar_<i>` tag. Labels keep their text in the unmerged tree.

**Loading (AC28).** A skeleton hero card in `surfaceMuted`, radius 28, with the visible text "Checking the weather" (`check_loading`) in a polite live region. There is no rating word, icon, reason or bars.

**Can't check (AC29).**
- Card: 2 dp dashed token `outline` border, radius 28.
- Inside it:
  - a 64 dp `surfaceMuted` circle with `ic_cloud_off`
  - "Can't check right now" (screenTitle size)
  - the D4 line
  - a "Try again" pill (C4, `check_retry` value changes): min height 52, `onSurface` fill, `surface` text, `ic_refresh`, which calls `refresh()`.
- Then a `primaryContainer` panel with the C13 text, the bold part as a span.

**Location needed (AC30, AC31).** A `PermissionExplainer` containing:
- An illustration: 220 dp, `primaryContainer`, radius 32. Concentric rings in `primary` at low alpha, drawn on a Canvas from tokens, with `ic_my_location` in a `primary` badge. It is decorative and hidden. No placeholder art is shipped (pack rule 6).
- Heading (D5, `check_needs_location` value changes; screenTitle at 30 sp; heading).
- The D5 body.
- Three `ic_check` bullets.
- The button: "Allow approximate location", or "Open settings" when permission is blocked. Primary style, min height 60.
- A "How your data is used" `TextButton` (at least 48 dp) that opens Privacy.

If the AC30 test shows the button below the fold at the default viewport, the illustration shrinks first, down to 140 dp. That would be a difference listed for AC66.

**Precise location for walks (AC32).**
- When `StartWalkUiState` is `NeedsPreciseLocation` or `PreciseLocationBlocked`, a `FieldKitBottomSheet` shows a `PermissionExplainer` with a `directions_walk` illustration, the exact "Recording a walk needs precise location." and "Allow precise location" or "Open settings" (min 60).
- A sheet keeps Today's layout, so MVP AC26 still holds, and is within thumb reach.
- Dismissing it (scrim, Back or drag) calls the new `StartWalkViewModel.onExplanationDismissed()`, which returns to `Idle`. No walk starts.

**Start walk (AC16, AC26).** `StartWalkButton` with the existing `START_WALK_TAG`. The label is "Start walk", or "Walk in progress" when recording; the second opens Recording. The behaviour is unchanged.

### Recording (task 5)
Wrapped in `RecordingTheme`, so it is always dark (AC4, AC33). There is no bottom nav. Top to bottom:
- **Top row.** `BackButton` (K6). A status pill (`card` fill, radius pill) with an 8 dp `liveDot` circle and "Recording walk" (C5, `recording_title` value changes). The notification keeps `recording_notification_title`.
- **Timer.** "Elapsed time" label (unchanged, D6). The timer is `LiveWalkFormat.elapsed()`, now `h:mm:ss` (for example "0:23:41"), in timer style with autosize, ticking every 1 s as today.
  - The node's content description is the spoken duration from `SpokenDuration`, smallest unit = seconds: "23 minutes 41 seconds", "1 hour 2 minutes 5 seconds", "1 hour 5 seconds", "0 seconds".
- **Two `StatCard`s.** "Distance" with "{x.x} mi" (existing strings), and "Started" (C15) with the start time in `HH:mm` or `h:mm a` per the device format.
- **Route panel (AC35).**
  - `mapPanel` fill, 1 dp `mapPanelBorder`, radius 22, about 200 dp high.
  - A Compose `Canvas` draws the projected fixes: a 4.5 dp `route` line over an 8 dp `routeCasing`, a start ring at the first fix, and a current dot (`onDark` with a halo) at the last fix.
  - Fewer than two fixes: an empty panel and no error.
  - Spoken label "Route so far" (C18). Visible caption "Route so far · saved on this phone" below it, in `onDarkMuted`.
  - The Canvas exposes a test-only `RouteSketchPoints` property with the projected points.
  - There are no map tiles and no network.
- **Tip line.** `ic_walk` + the D6 tip, or the exact `recording_notifications_off` text when notifications are off (existing logic).
- **Stop (AC37).**
  - Full width, min height 76, radius 24, `stopBackground` / `stopForeground`, `ic_stop` + "Stop and save walk" (C6, `recording_stop` value changes), 21 / 800.
  - While saving: disabled, with the exact "Saving your walk".
  - Tag `STOP_WALK_TAG` is kept.

**Route data.**
- New DAO query: `WalkDao.observePoints(walkId): Flow<List<RoutePointEntity>>` (a `SELECT … ORDER BY seq` Flow) and `WalkRepository.observeRoutePoints(walkId): Flow<List<LatLon>>`. This is not a schema change.
- `RecordingViewModel.route` uses `flatMapLatest` on the session's `walkId`.
- Room re-emits after each `appendFix` transaction. The service then publishes the distance, so the line updates at the same rate as distance (every 3 s fix interval, ADR 0005).
- The screen collects with `collectAsStateWithLifecycle`, so nothing is queried while the screen is off or in the background.
- `domain/walk/RouteProjection` (pure Kotlin) does an equirectangular projection with `cos(meanLatitude)` x-scaling and a uniform scale-to-fit with padding. It returns points in panel pixels, with the first point as the start marker and the last as the current marker.

### Walks (task 6)
- **Title.** "Walks" (C7: `history_title` and `nav_walks`; screenTitle; heading). There is no back control. The debug menu, debug builds only, stays at the end of the title row.
- **This-week card (AC40, AC41).**
  - `primary` fill, radius 28, `onPrimary` text.
  - "This week" plus the week range in `summaryCaption`.
  - Three stats in statLarge with autosize: count, `h:mm`, miles to one decimal. Captions in `summaryCaption`: "walks" or "walk" (plural resource), "h:mm", "miles".
  - One node (`clearAndSetSemantics`, tag `THIS_WEEK_TAG` kept), content description "This week: {n} walk(s), {spoken duration, smallest unit minutes}, {x.x} miles". Zero is "0 minutes".
  - **The 7-day dot row** (hidden from accessibility). Each dot is a 26 dp circle, Monday to Sunday, with a test-only `DayDotState` property (WALKED / PAST_NO_WALK / FUTURE, plus `isToday`) and tags `week_dot_0` … `week_dot_6`.

    | Dot | Light | Dark |
    |---|---|---|
    | Walked | `#8CC2FF` fill | `onPrimary` `#0B2747` fill |
    | Past, no walk | `#2E5C94` fill (pack) | `primaryPressed` `#D0E4FF` fill |
    | Future | 1.5 dp dashed `summaryCaption` ring | 1.5 dp dashed `#1E3A5F` ring |
    | Today | a 2 dp `onPrimary` ring around the dot's fill | a 2 dp `onPrimary` ring around the dot's fill |

- **Week sections (AC39).**
  - Heading: the week range from `WalkDateText` (label 13 / 700, heading).
  - Right-aligned totals: `{count} · {h:mm} · {x.x} mi` (D7, new `history_totals_dotted`).
  - Then a `surface` card (radius 22) of rows separated by `divider`.
- **Row (AC39, AC44).**
  - Min height 64. A 44 dp `primaryContainer` tile (radius 14) with `ic_nav_walks` in `primary`.
  - Title = the date ("Mon 19 Oct"). Subtitle = "at {time}" (`history_walk_at`).
  - Right-aligned distance "{x.x} mi" and duration `h:mm` (tnum), then `ic_chevron_right` in `iconMuted`.
  - The row is one clickable node (`onClickLabel` "Open walk details", kept) with merged semantics and content description `"<date> at <time>, <h:mm>, <x.x> mi"`.
  - **"More" (AC44).** A trailing 48 dp icon button (`ic_more`, content description "More" from `history_more`) opens a `DropdownMenu` with one item, "Delete". The item's content description is the existing `history_delete_description` with the year-format date.
  - Choosing it opens `ConfirmSheet` with `history_delete_title`, the exact `history_delete_message`, "Delete walk" and "Cancel". The behaviour is the same as the MVP.
  - The MVP row has a direct "Delete" button, not a menu. AC44 and spec A5 describe a menu. The owner decided "Use a More menu" (OD2), so the row's direct Delete button is removed and Delete moves into the "More" menu as AC44 says.
- **Empty (AC42).** A centred `ic_nav_walks` in `iconMuted` (decorative) plus the exact "No walks yet".
- **Load failure (AC43).** The exact "Couldn't load your walks."
- **Date text (D7, D8, AC39, AC46).** `ui/history/WalkDateText(zoneId, clock, is24Hour)` is plain Kotlin with `Locale.UK`. The current year is `LocalDate.now(clock.withZone(zoneId)).year`.
  - `rowDate`: `EEE d MMM`, plus ` yyyy` when not in the current year.
  - `fullDate`: `EEEE d MMMM`, plus ` yyyy` when not in the current year.
  - `weekRange(start, end)`:
    - same month and year: `EEE d – EEE d MMM[ yyyy]`
    - same year: `EEE d MMM – EEE d MMM[ yyyy]`
    - different years: `EEE d MMM yyyy – EEE d MMM yyyy`
    - The year is added when the range's year is not the current year.
  - `time`: `HH:mm` or `h:mm a`. `timeRange`: `{start} – {end}` (spaced en dash, C14).
  - Delete labels and messages keep the existing `WalkDateFormats` year format (AC44).
- **Data.** `HistoryUiState` gains `thisWeekDays: List<DayDot>` from the new pure `WeeklyTotals.dayDots(walks, zone, now)`. `HistoryViewModel` passes the clock. Grouping and totals are unchanged.

### Walk detail (task 7)
- **Map (AC45).**
  - Edge to edge at the top, 470 dp at the default size and drawn under the status bar.
  - `BackButton` floating at top-start, inset by the status bar, content description "Back".
  - The route keeps `routeMap.Draw` and the `RouteMap` interface is unchanged. The MapLibre adapter adds:
    - a casing `LineLayer` (`#FFFFFF`, 11 dp) under the route line (`#1B4F8F`, 5.5 dp)
    - a start-marker `CircleLayer` (`#1B4F8F` fill, `#FFFFFF` stroke) at the first fix.
  - Constants live in `map/RouteStyle.kt` (ADR 0007: light primary in both themes, because the tiles are not themed).
  - The attribution chip (the existing `map_attribution`, 11 sp, `surface` at 90 %) stays at bottom-end, at most 60 % width.
- **Sheet (AC46).**
  - `background` fill, top radius 28, overlapping the map by 28 dp.
  - Heading: `fullDate` (display 28 / 800, heading). There is **no** "Walk details" title.
  - Time range (C14).
  - Two `StatCard`s on `surface`: "Duration" + `h:mm` and "Distance" + "{x.x} mi" (D8, new caption strings `walk_detail_duration_caption` and `walk_detail_distance_caption`). Each card is one node.
  - `DestructiveOutlinedButton` "Delete this walk" (D8), min height 56.
  - The whole screen scrolls at large font sizes.
- **Offline (AC47).** The map area becomes `surfaceMuted` with `ic_map_off`, the exact "Map not available offline" (16 / 700) and the C17 sub-line.
- **No route, not found, load failed (AC49).** The exact existing texts.
- **Delete (AC48).**
  - `ConfirmSheet` with "Delete this walk?" (`history_delete_title`), the exact `history_delete_message`, "Delete walk" and "Cancel".
  - `WalkDetailViewModel.delete()` calls `WalkRepository.deleteWalk(id)`, then the screen pops back to Walks. The Room Flow updates the totals.
  - Cancel, Back or scrim closes the sheet and changes nothing.
  - A delete failure shows the existing `history_delete_failed` snackbar.

### Dog profile (task 8; tiles and title semantics from task 2)
- **Title.** "Your dog" (C8, `profile_title`; screenTitle; heading). No back control.
- **Photo row (AC50, AC67).**
  - An 88 dp round photo with a 4 dp `surface` ring, content description `profile_photo_description`.
  - The placeholder is a `surfaceMuted` circle with `ic_nav_dog` in `iconMuted`; the paw avatar art is not shipped.
  - A stacked column:
    - With a photo: "Change photo" (C10; outlined pill, `ic_image`, min 48) and "Remove photo" (`TextButton`, `errorText`, min 48 x 48, K4).
    - With no photo: "Choose photo" and the existing visible "No photo" caption (`textTertiary`), kept so the MVP assertions stay valid.
  - Photo error messages (AC67): the exact existing texts in `errorText`, below the row.
- **Inputs (AC51).**
  - Material 3 `OutlinedTextField`, radius 16, min height 52, border `controlBorder` (ADR 0007), focused border `primary`.
  - The label stays inside the field's semantics, so "Breed (optional)" remains one text node. If the pinned Material 3 1.4.0 has `TextFieldLabelPosition.Above`, use it; otherwise use the default label position and list the difference for AC66.
  - In "Breed (optional)" the "(optional)" part is an `AnnotatedString` span in `textTertiary` at weight 400. The string resource is unchanged.
  - `supportingText` holds the existing error text, in `errorText` (start), and the live counter "{n} / 30" or "{n} / 50" (D9, new `profile_counter`, `textTertiary`, end).
- **Sensitivity (AC52).**
  - "Heat sensitivity" heading (kept).
  - Two radio cards in a 2-column row (min height 92, radius 18). The row has `selectableGroup()`. Each card has `Modifier.selectable(selected, role = Role.RadioButton)` with merged descendants: name (17 / 800) and bands (D9).
  - Selected: `primaryContainer` fill, 2 dp `primary` border, and a filled radio dot.
  - Normal is selected by default, and the selected state is exposed on Normal as well (fixes MVP finding F1). The developer confirms the cause of F1 in the PR.
  - Then the C11 hint.
- **Save (AC53).** `PrimaryButton` "Save" (C9), min height 56. The existing "Profile saved" feedback is kept.
- **Tiles.** Two equal tiles (`surface`, radius 22, min 48): "Privacy & data" (`ic_shield`) and "About the advice" (`ic_info`) (D9).

### Privacy (task 7)
- **Top bar.** `BackButton` + "Privacy & your data" (D10, `privacy_title`; heading). Headline "Your walks stay on your phone" (display 30 / 800, heading).
- **Card "Never leaves this phone"** (`ic_phone`, heading): the exact `privacy_local` and `privacy_no_tracking`.
- **Card "Only sent when you use a feature"** (`ic_send_out`, heading), with two entries:
  - "MET Norway · when the check runs" (heading) + the exact `privacy_weather`
  - "OpenFreeMap · when you view a walk map" (heading) + the exact `privacy_map`, including the Cloudflare mention.
- **Bottom.** The existing "Delete your data" heading (`privacy_delete_heading`, kept), the exact `privacy_delete_explanation`, and `DestructiveOutlinedButton` "Delete all my data".
  - It opens `ConfirmSheet` with the exact `privacy_delete_title`, `privacy_delete_message`, "Delete everything" and "Cancel".
  - The deleting, deleted and failed texts and logic are unchanged (AC54, AC55).
- **Not shown.** The MVP section headings "Weather check", "Walk maps" and "Your data stays on your phone" are replaced by the D10 headings. The pack's muted "not backed up" panel is not added as new text, because `privacy_local` already says so.

### About this advice (task 8; font licences in task 1)
- **Top bar.** `BackButton` + "About this advice" (heading).
- **Headline and intro.** Headline "How we rate the weather" (D11, heading). Intro (D11).
- **Thresholds table (AC56, AC61).**
  - A `surface` card headed by the existing "Rating thresholds" (`about_thresholds_heading`, heading). Keeping it near the top lets the MVP test that asserts it is displayed pass unchanged.
  - Columns: Factor, Go, Caution, Avoid. Each rating header shows its rating icon and word in `LocalRatingColors` (`onContainer`).
  - Rows Heat, Pavement, Rain, Wind and Cold, with exactly the D11 cells. The Sensitive values in brackets are in `textTertiary`.
  - **Semantics.**
    - The table node has `collectionInfo = CollectionInfo(rowCount = 6, columnCount = 4)`.
    - Each cell has `collectionItemInfo = CollectionItemInfo(row, 1, column, 1)`.
    - Header cells are marked `heading()`.
    - Each data cell's content description is "<row header>, <column header>, <cell text>", built from the on-screen words only, so TalkBack gives row and column context on every Android version.
- **Existing sentences.** Below the table come the existing threshold sentences (`about_overall` … `about_cold`, word for word).
- **Sources.** "Where this comes from" (D11, replaces the visible `about_sources_heading`) + the existing sources intro and sentences.
- **Data panel.** A muted panel (`surfaceMuted`) with the exact disclaimer (bold), the exact attribution and the map attribution, each a separate unaltered text (K9). The existing "Data" heading (`about_data_heading`) is kept above it.
- **Licences.** The existing "Open-source licences" sections, unchanged, then the two font sections from task 1 (AC8).

## Interfaces and data
- **Database:** no schema change and no migration. A new read-only DAO query, `WalkDao.observePoints(walkId)` (Flow), is added. Room schema JSON is unchanged.
- **Domain (pure Kotlin, additive):**
  - `RatingEngine.pavementFactor(hour, sensitivity, isDaylight): PavementFactor`.
    - `NotRated` when it is night, or cloud is 50 % or more, or cloud is missing.
    - `NoData` when the rule applies but the temperature is missing.
    - Otherwise `Rated(Rating)`, using the same thresholds as `heat()`.
    - `rate()` is unchanged.
  - `FactorRounding.wholeNumber(value: Double): Long`: halves round away from zero (`sign(x) * floor(|x| + 0.5)`), and -0 becomes 0.
    - Do **not** use `kotlin.math.round` (half-even) or `Math.round` (rounds -2.5 to -2).
  - `CheckOutcome.Rated` gains `hours: List<HourRating>`, `conditions: HourlyConditions` (the current hour) and `pavement: PavementFactor`. `CheckRater.rate` fills them in from what it already computes.
  - `FactorCell` mapping: `driverOf(reason)`.
    - WARM, TOO_HOT and FREEZING → Air
    - RAIN and HEAVY_RAIN → Rain
    - WINDY and DANGEROUS_WINDS → Wind
    - PAVEMENT_* → Pavement
    - GOOD and DATA_MISSING → none.
  - `RouteProjection.fit(points, width, height, padding)`.
  - `WeeklyTotals.dayDots(walks, zone, now)`.
- **UI state:**
  - `CheckUiState.Rated` gains `hours`, `factors: FactorGrid` (four `FactorCell(caption, value, highlighted)` built in the ViewModel), `isCached`, `isOffline`, `dogName: String?` and `sensitivity`. Non-rated states take `dogName` for the title.
  - `CheckViewModel` gains constructor parameters with defaults, so existing tests compile: `dogName: Flow<String?> = flowOf(null)` and `isOnline: () -> Boolean = { true }`. The factory wires `profileRepository.observeProfile()` and `container.connectivity::isOnline`.
  - **Offline means `!ConnectivityStatus.isOnline()`**, the existing check of an internet-capable, system-validated network. A captive portal with no validated internet therefore also reads as "You're offline."
- **Recording / history state:**
  - `RecordingViewModel` gains `route: StateFlow<List<LatLon>>`.
  - `HistoryUiState` gains `thisWeekDays`.
  - `WalkDetailViewModel` gains `requestDelete`, `confirmDelete`, `cancelDelete`, `deleteFailed` and `deleted` (it navigates back on `deleted`).
  - `StartWalkViewModel` gains `onExplanationDismissed()`.
- **Navigation:** route strings are unchanged. `TOP_LEVEL` is added. `HomeScreen` is removed. `DogWalkBuddyNavHost` becomes the content of `AppScaffold`. `NAV_TODAY_TAG`, `NAV_WALKS_TAG` and `NAV_DOG_TAG` are added.
- **Manifest:** `MainActivity` adds `android:configChanges="uiMode"`. Nothing else changes: no new permission, component or exported flag.
- **Resources:**
  - `res/font/` (2 files), `res/raw/licence_ofl_*.txt` (2 files), about 30 `res/drawable/ic_*.xml` (spread over tasks 1-8, ADR 0008), and `res/values-night/themes.xml`.
  - `strings.xml` gets new strings for the approved copy and changed values for C2, C4, C5, C6, C7, C8, C9, C10, C11, D5 and D10.
  - Strings the redesign no longer displays are **deleted** in the task that stops using them, because Android Lint `UnusedResources` fails the build under `warningsAsErrors`. Deleting an unused string is not a text change. The same rule is why each icon is added only with its first use.
  - AC-fixed texts are never deleted or changed.
- **Reversal:** every change is UI or resources. Reverting the run's commits restores the MVP. No stored data changes shape.

## Contrast (AC6, AC40, AC67)
Ratios use the WCAG 2.1 relative-luminance formula, the same as `testutil/Wcag.kt`, rounded to the nearest 0.01. `ContrastTest` asserts the floors (4.5, 7 or 3), not these numbers, so rounding cannot move a pair across its floor.

Every pair below is added to `ContrastTest` in task 1. A later task that introduces any other text/fill pair must add it there first.

| Pair (text on fill) | Floor | Light | Dark |
|---|---|---|---|
| onSurface on surface | 4.5 (7 for the reason, MVP) | 17.35 | 14.60 |
| onSurface on background | 4.5 | 15.87 | 16.53 |
| onSurface on surfaceMuted (banner, Cancel, skeleton) | 4.5 | 14.46 | 13.20 |
| onSurface on primaryContainer (selected option, positive panel) | 4.5 | 14.40 | 10.43 |
| onSurfaceVariant on surface | 4.5 | 9.01 | 10.62 |
| onSurfaceVariant on background (meta line, totals) | 4.5 | 8.24 | 12.03 |
| onSurfaceVariant on surfaceMuted | 4.5 | 7.50 | 9.60 |
| onSurfaceVariant on primaryContainer (option bands) | 4.5 | 7.47 | 7.59 |
| textTertiary on surface (counters, brackets, attribution) | 4.5 | 7.44 | 7.82 |
| textTertiary on background ("(optional)", footnote) | 4.5 | 6.81 | 8.85 |
| textTertiary on surfaceMuted | 4.5 | 6.20 | 7.07 |
| primary on surface (links) | 4.5 | 8.20 | 8.65 |
| primary on background | 4.5 | 7.50 | 9.79 |
| primary on primaryContainer (tile labels) | 4.5 | 6.80 | 6.18 |
| onPrimary on primary (Start walk, Save, this-week stats) | **7** | 8.20 | 8.08 |
| summaryCaption on primary (this-week captions and range, AC40) | 4.5 | 6.10 (`#CFE0F5`) | 6.18 (`#1E3A5F`) |
| primaryPressed on surface (active nav label) | 4.5 | 13.56 | 12.45 |
| errorText on surface (destructive outlined buttons, Remove photo) | 4.5 | 8.71 | 9.49 |
| errorText on background (profile and photo errors, AC67) | 4.5 | 7.96 | 10.74 |
| error on surface (MVP pair) | 4.5 | 7.51 | 9.49 |
| onError on error (destructive confirm; MVP Stop pair) | 7 (MVP) | 7.51 | 7.72 |
| surface on onSurface ("Try again" pill) | 4.5 | 17.35 | 14.60 |
| controlBorder (= Material 3 outline) on surface | 3 (non-text, MVP) | 5.82 | 6.28 |
| Go: onContainer / onContainerMuted on container | 7 / 4.5 | 10.50 / 8.05 | 11.14 / 8.45 |
| Caution: onContainer / onContainerMuted on container | 7 / 4.5 | 10.90 / 8.81 | 10.64 / 8.28 |
| Avoid: onContainer / onContainerMuted on container | 7 / 4.5 | 10.87 / 8.74 | 10.76 / 8.02 |
| Caution: onContainer / onContainerMuted on driverHighlight.bg | 4.5 | 11.93 / 9.64 | 8.73 / 6.80 |
| Avoid: onContainer / onContainerMuted on driverHighlight.bg | 4.5 | 12.94 / 10.41 | 9.32 / 6.95 |
| Rating icon (onBadge) on badge, Go / Caution / Avoid | 3 | 5.99 / 6.33 / 7.51 | 4.31 / 4.95 / 5.92 |
| Rating word colour (`LocalRatingColors`) on surface, Go / Caution / Avoid (About headers; MVP test) | 7 | 12.63 / 12.71 / 14.25 | 12.55 / 12.36 / 11.02 |

Recording palette (identical in light and dark):

| Pair | Floor | Ratio |
|---|---|---|
| onDark on background | 4.5 | 18.24 |
| onDark on card (stat values, pill text) | 4.5 | 14.56 |
| onDark on mapPanel | 4.5 | 16.11 |
| onDarkMuted on background (labels, tip, caption) | 4.5 | 12.03 |
| onDarkMuted on card (stat captions) | 4.5 | 9.60 |
| stopForeground on stopBackground (Stop label) | **7** | 16.68 |

Every pair meets its floor. Bars, dots, dividers, the live dot and decorative borders carry no text. They are not text pairs and are not asserted, apart from the MVP 3:1 border check above.

## Risks and mitigations
| Risk | Mitigation |
|---|---|
| **Test churn breaks or quietly weakens MVP tests** (Medium) | Every allowed change is listed in "Test assertion changes", with old and new assertions and, for the home-link row, every affected line. New tests use the `UX_AC` prefix. A changed assertion must stay exact: no substring, regex or existence check. The reviewer checks each diff against the list. E1-E8 are owner-approved (OD1); any other unlisted change still needs the owner's confirmation before it is merged. |
| **The token `outline` fails the MVP 3:1 border test** | The Material 3 `outline` maps to `iconMuted` (ADR 0007). The token `outline` is used only for decorative borders. The difference is listed for AC66. |
| **Today does not fit the disclaimer in the default viewport** (AC25, MVP AC26 and AC27) | The fixed order puts the footnote above the best-window card (estimated link bottom about 585 dp against a 717 dp viewport). A layout test covers the four AC25 cases at `w411dp-h891dp-xxhdpi`. If it fails, spacing is tightened inside the token ranges. Font sizes are never reduced below the tokens. |
| **12-hour strip labels collide** (AC23) | Unconstrained, centred labels. The widest pair is about 27 dp against a 28.9 dp pitch. A test checks label bounds do not intersect in both formats. Above font scale 1.3 the decorative strip thins its labels. |
| **Robolectric text measurement differs from a device** | Layout tests assert relative bounds, not pixel values. The manual AC66 comparison on the emulator is the visual authority. Any layout test that cannot be made deterministic is reported as BLOCKED, never loosened. |
| **Variable fonts fail to load in Robolectric** | The theme builds `FontFamily` objects lazily, and Compose falls back to the system font if loading fails. If Robolectric throws instead, task 1 reports BLOCKED with the stack trace; the architect then amends ADR 0006 (for example, to static instances). |
| **An icon or string lands before its first use and Lint fails the build** | Each icon is added by the task that first uses it (ADR 0008), and strings are added and deleted with their use. Task 1 adds only resources it uses: the fonts, the OFL texts and font headings (About licence sections) and the three replaced rating icons. |
| **A copied icon references a theme attribute the app does not define** (found in task 1) | ADR 0008 Amendment 1: the tint uses the platform `?android:attr/colorControlNormal`, so it resolves in every theme. The icon-file guard test fails the build on any `?attr/` reference in a Material Symbols file. |
| **`clearAndSetSemantics` on the hero and summary cards hides child nodes from tests** | The MVP hero already uses it, and the MVP tests find children with `useUnmergedTree = true`. The same pattern is kept. |
| **The live route line drains the battery** | The Room Flow is collected only while Recording is visible. The Canvas redraws only on a new fix (every 3 s) and the timer ticks every 1 s, as in the MVP. The battery is checked on the reference phone (≤ 8 % per 60 min). |
| **A theme switch recreates the activity** (AC7) | `configChanges="uiMode"`, plus an integration test that the activity instance and entered form text survive a night-mode change. Because the activity is not re-created, `MainActivity.onConfigurationChanged` calls `enableEdgeToEdge()` again so the status and navigation bar icons switch between dark and light with the theme (task 1 scope note). |
| **Copy drift** | `ApprovedCopyTest` (AC63) and `FixedTextsTest` (AC62) assert every approved and AC-fixed string exactly. The code review checks that no other string value changed. |
| **AC44 describes a "More" menu that the MVP does not have** | The owner decided "Use a More menu" (OD2). The menu is built as AC44 says, and the existing delete label and message are reused. |

## Security considerations
Unchanged from `docs/governance/security-standards.md` and the MVP review.
- **Network:**
  - No new host, permission, exported component or network code.
  - Fonts and icons are bundled static assets (ADR 0006, 0008), and `ui-text-google-fonts` must not be added.
  - The route line reads the device's own Room data.
  - A unit test asserts that `androidx.compose.ui.text.googlefonts.GoogleFont` is not on the classpath. The manual proxy check (AC65) repeats MVP AC49.
- **Supply chain:**
  - The developer downloads the font and icon files only from the google/fonts and google/material-design-icons GitHub repositories, at one recorded commit each. Task 1 records both commits; later tasks copy icons from the same icon commit. Each PR that adds files records their SHA-256 checksums.
  - The licences are checked against the actual files. G4 re-checks them.
- **Personal data:**
  - The dog's name now also appears in the nav label, the check title and the Sensitive footnote line. It stays on the device and is never logged. The existing logging rule (class names only) is unchanged.
  - No route, coordinates or name go into test fixtures beyond synthetic values such as "Biscuit" or made-up coordinates.
- **Manifest:** the only change is `configChanges="uiMode"` on the already non-exported-by-default launcher activity. `ManifestSecurityTest` and `SourceManifestHardeningTest` must keep passing unchanged.
- **Data deletion:** "Delete all my data" behaviour is unchanged. The nav label returns to "Dog" afterwards (AC15).

## Performance and size (spec NFR)
The MVP proposed baseline (`docs/perf/20261019-dog-walk-buddy-mvp/proposed-baseline.json`, emulator `Medium_Phone_API_37.0`, debug build) gives these limits for this run's medians (within 10 %, assumption A6). The MVP targets must also still be met.

| Metric | Baseline median | Limit (+10 %) | MVP target | How the design stays inside it |
|---|---|---|---|---|
| App cold start | 913.5 ms | 1004.9 ms | none | No new startup work. The two font files are parsed lazily at first text layout. The nav label reads the profile through the existing Room Flow and shows "Dog" until it arrives, so the first frame never waits for the database. |
| Start walk tap to recording | 351.0 ms | 386.1 ms | ≤ 1000 ms | The start path is unchanged. Recording adds one Room Flow and a Canvas. |
| History with 500 walks | 724.6 ms | 797.1 ms | ≤ 1000 ms | Grouping is unchanged. `dayDots` scans only the current week. `WalkDateText` formatters are built once per screen (`remember`), never per row. `LazyColumn` with stable keys, as now. |
| Check launch to result | 1746.2 ms | 1920.8 ms | p90 ≤ 5000 ms | The network path is unchanged. The factor grid, highlight and strip are O(12) work on data already in memory. |

- **Memory (PSS):** report Today, Recording and Walk detail against 77,563 / 81,071 / 164,935 KB. Memory-mapped font files and about 30 vectors are expected to add well under the 15 % allowed in `definition-of-done.md`.
- **APK size:** report the release APK (`assembleRelease`, unsigned) before (the base of this branch) and after.
  - Fonts are estimated at 300-600 KB uncompressed; icons at about 30 KB. There is no target.
  - The task 1 PR reports the first measurement. The performance stage records the final one.
- **Battery:** the ≤ 8 % per 60-minute walk check with the live route line is measured on the reference phone with the other deferred checks.
- **Results:** all results go in `docs/perf/20261022-ux-redesign/` using the MVP method.

## Test strategy
- **Naming:** this run's tests are `UX_AC<n>_<behaviour>`. MVP tests keep `AC<n>_…`.
- **Unit (JVM):**
  - token values (AC1-AC4)
  - typography and font configuration (AC5)
  - contrast pairs (AC6, AC40, AC67)
  - `FactorRounding` and `pavementFactor` and `driverOf` (AC17)
  - `HourLabelFormat` (AC23)
  - `SpokenDuration` (AC34, AC40)
  - `RouteProjection` (AC35)
  - `WalkDateText` (AC39, AC46)
  - `WeeklyTotals.dayDots` (AC41)
  - `FixedTextsTest` (AC62), `ApprovedCopyTest` (AC63)
  - the no-google-fonts classpath check (AC5, AC65)
  - the Material Symbols icon-file guard (ADR 0008 Amendment 1; added in task 1).
- **Integration (Robolectric Compose, `w411dp-h891dp-xxhdpi` unless stated):**
  - every screen and state in the traceability table, in light **and** with the `+night` qualifier
  - `assertAccessibleScreen()` on each (AC58)
  - an extended `assertHeadingsMarked(...)` and `assertHiddenFromAccessibility(tag)` for decorative graphics (added in task 2)
  - `assertNoTextTruncation()` at 200 % (AC27, AC38, AC60). It gains an optional `allowEllipsisTags` parameter, used only by the AC60 nav-label test. The default behaviour is unchanged.
  - Nav and back behaviour with the real `MainActivity` (AC9-AC16).
  - Room-backed tests for the route Flow (AC35) and delete (AC44, AC48).
  - MockWebServer `requestCount` for "no request" assertions (AC35).
- **Manual (on the emulator during tasks, then on the reference phone at release):**
  - AC4 (Recording identical in both themes)
  - AC7 (system theme switch on all seven screens)
  - AC27 and AC38 (maximum font and display size)
  - AC45 (route, casing, start marker, attribution in light and dark)
  - AC52, AC56 and AC59 (TalkBack walk-through, including both radio options and the About table)
  - AC60 (30-character name, TalkBack reads it in full)
  - AC61 (greyscale)
  - AC65 (proxy host list)
  - AC66 (screenshots of every screen and state, light and dark, compared with the artboards, with each difference from the list below recorded).

  Each screen task's PR (tasks 3-8) includes emulator screenshots in both themes and its list of differences. The reference-phone session at release records AC27, AC38, AC52, AC56, AC59, AC60, AC61 and AC65, the battery check, and the MVP checks still owed (spec "Dependencies and risks").
- **Coverage:** at least 80 % of changed lines (`bash scripts/coverage.sh origin/feature/20261022-ux-redesign`). Logic is kept in pure classes (rounding, projection, dates, spoken duration, label formats, day dots) so it is unit-covered. The MapLibre adapter stays excluded (ADR 0001); its new layers are checked by hand (AC45).

### MVP AC tests stay in force (AC63, AC64)
Every test for MVP AC1-AC53 keeps running in `./gradlew --no-daemon testDebugUnitTest`. Each one changes only as listed in the next section.

| MVP ACs | Main test files | Expected change |
|---|---|---|
| AC1-AC14 (rating rules) | `domain/rating/RatingEngine*Test`, `domain/check/CheckRater*Test`, `domain/sun/SunCalculatorTest` | None. `rate()` is untouched. New tests cover `pavementFactor`. |
| AC15, AC21-AC24 (check flow, cache, offline) | `data/repo/CheckRepository*Test`, `domain/check/CachePolicyTest`, `data/weather/*Test`, `ui/check/CheckViewModel*Test`, `ui/check/CheckScreenIntegrationTest`, `ui/check/CheckCardTest` | "Retry" → "Try again" (C4). New `Rated` fields are filled in test builders. |
| AC16-AC19 (best window) | `domain/window/BestWindowFinder*Test`, `ui/check/BestWindowFormatter*Test`, `ui/check/CheckCardTest` | Heading text (C2) only. The formatter is unchanged. |
| AC20, AC26, AC27, AC44, AC45 | `ui/check/CheckCardTest`, `ui/check/CheckScreenIntegrationTest`, `ui/about/AboutScreenTest` | The rating announcement gains the factor cells (E1). Home-link and Back changes (home-link row, E2, E3, E4, E7). C1 and C9 copy. |
| AC25 (location permission) | `data/location/*Test`, `ui/check/CheckCardTest`, `ui/check/CheckScreenIntegrationTest` | D5 heading without the full stop. |
| AC28-AC34 (recording) | `service/*Test`, `ui/recording/*Test`, `domain/walk/LiveWalkFormatTest`, `domain/walk/RouteDistanceCalculator*Test`, `data/repo/RecoverInterruptedWalkUseCaseTest` | C5 and C6 copy; elapsed time h:mm → h:mm:ss. The notification title test is unchanged. |
| AC35-AC37, AC40 (history) | `domain/walk/WeeklyTotalsTest`, `domain/walk/WalkHistoryEdgeCasesTest`, `domain/walk/WalkFormatsTest`, `data/repo/WalkHistoryRepositoryTest`, `ui/history/*Test` | D7 date and totals formats. Delete is reached through the row's "More" menu (OD2, E6). The History Back check moves to `NavigationTest` and the `onBack` arguments go (E3). Domain totals tests are unchanged. |
| AC38, AC39 (walk detail) | `ui/walkdetail/*Test`, `map/*Test` | D8 and C14 formats; no "Walk details" title (E4). `map/` tests are unchanged. |
| AC41-AC45 (profile) | `ui/profile/*Test`, `data/repo/ProfileRepositoryTest`, `data/repo/PhotoStore*Test`, `domain/profile/ProfileValidatorTest` | C9, C10 and C11 copy; Back on the tab (E3). Validation is unchanged. |
| AC46-AC48 (privacy, delete-all, backup) | `ui/privacy/*Test`, `data/repo/DeleteAllDataUseCaseTest`, `ui/nav/AppFlowIntegrationTest` | Two taps through the dog tab (home-link row). D10 heading (E5). The dialog becomes a sheet with the same texts. |
| AC49 (hosts) | `ManifestSecurityTest`, `SourceManifestHardeningTest`, `map/MapConfigTest`, `data/weather/MetNorwayClientTest` | None. |
| AC50-AC53 (accessibility) | `ui/a11y/*`, `ui/theme/ContrastTest`, `ui/profile/ProfileContrastTest`, plus every screen test | Extended only. Existing assertions are unchanged, because the theme mapping keeps them true (ADR 0007). |

## Test assertion changes
### From the spec's list (owner-approved), with the task that makes each change
| Task | File and lines (at `a8b4506`) | Old → new (exact) | Source |
|---|---|---|---|
| 2 | **Home-link taps**, and the returns from Privacy that ended on Home. Complete list, from a scan of `app/src/test` (there is no `app/src/androidTest`):<br>• `ui/nav/NavigationTest.kt` L23 "Dog profile"; L33 "Walk history"<br>• `ui/nav/AppFlowIntegrationTest.kt` L141, L192, L216, L233, L287 "Walk history"; L144, L290 "Dog profile"; L150, L176, L263, L303, L320 "Privacy"; L152, L180, L285 `back()` from Privacy, after which the next step expects Today<br>• `ui/check/CheckScreenIntegrationTest.kt` L245 "Dog profile"<br>The Home "About this advice" link tap (`CheckScreenIntegrationTest.kt` L143) is E7. | • "Walk history" → `performClick()` on the "Walks" nav item (`NAV_WALKS_TAG`).<br>• "Dog profile" → `performClick()` on the dog nav item (`NAV_DOG_TAG`; its label is the saved name or "Dog").<br>• "Privacy" → the dog nav item, then the "Privacy & data" tile (two taps, AC13).<br>• L152, L180, L285: Back from Privacy now returns to the dog tab, where Privacy was opened. After `back()` the test asserts the dog nav item `assertIsSelected()`, then taps the "Today" nav item (`NAV_TODAY_TAG`) before its unchanged next step. At L180 this keeps "back returns there" exact: it returns to the screen Privacy was opened from.<br>• Nav items are tapped without `performScrollTo()`, because the bar is outside the scroll.<br>• Every assertion that followed each tap stays as it is, apart from changes listed in other rows.<br>These follow the spec's own allowance in AC64 ("a tap on a nav item instead of a home link"), so they are not a new owner decision. | AC10, AC13, AC64 |
| 3 | `ui/check/CheckScreenIntegrationTest.kt` L149 | Unchanged with no profile, plus a new assertion "Good time to walk Biscuit?" with a profile | C1 |
| 4 | `ui/check/CheckCardTest.kt` L168, L265; `ui/check/CheckScreenIntegrationTest.kt` L179 | "Best walking window" → "Best window, next 12 hours" | C2 |
| 4 | `ui/check/CheckCardTest.kt` L158; `ui/check/CheckScreenIntegrationTest.kt` L192, L203 | "Retry" → "Try again" | C4 |
| 4 | `ui/check/CheckCardTest.kt` L176, L186; `ui/check/CheckScreenIntegrationTest.kt` L160, L162 | "The check needs your approximate location." → "The check needs your approximate location" | D5 |
| 5 | `ui/recording/WalkRecordingScreensTest.kt`, all `awaitText("Recording your walk")`; `ui/nav/AppFlowIntegrationTest.kt` L154 | → "Recording walk". `service/WalkRecordingServiceTest.kt` L85 stays "Recording your walk" | C5 |
| 5 | `ui/recording/WalkRecordingScreensTest.kt` L196 | Stop text "Stop" → "Stop and save walk" | C6 |
| 5 | `domain/walk/LiveWalkFormatTest.kt` and Recording screen elapsed assertions | h:mm (for example "0:00") → h:mm:ss ("0:00:00"), same durations | Scope item 5 |
| 6 | `ui/history/HistoryScreenTest.kt` L146-148, L192 | Row lines without the year (D7 list) | D7 |
| 6 | `ui/history/HistoryScreenTest.kt` L162-164 | "Mon 12 – Sun 18 Oct", "2 walks · 0:50 · 1.5 mi", "Mon 19 – Sun 25 Oct" | D7 |
| 6 | `ui/history/HistoryScreenTest.kt` L174, L223 | This-week card texts + spoken "This week: 0 walks, 0 minutes, 0.0 miles" | D7 |
| 6 | `ui/history/HistoryRowOpenTest.kt` L91, L95, L101, L119 | Row texts "Mon 19 Oct" and "at 09:15"; spoken "Mon 19 Oct at 09:15, 1:15, 2.0 mi" | D7 |
| 6 | `ui/nav/AppFlowIntegrationTest.kt` L193-194 | Opens the row "Mon 19 Oct" / "at 09:15" | D7 |
| 7 | `ui/walkdetail/WalkDetailScreenTest.kt` L149-151, L161-163 | Heading "Monday 19 October", "09:15 – 10:30", Duration card "Duration" + "1:15", Distance card "Distance" + "2.0 mi" | D8, C14 |
| 7 | `ui/nav/AppFlowIntegrationTest.kt` L195, L202-203, L217-224, L234-241 | Wait for the heading "Monday 19 October"; Duration and Distance cards as above | D8 |
| 8 | "Save profile" in `ui/profile/ProfileScreenTest.kt` L117, L319; `ui/profile/ProfileScreenStateTest.kt` L76, L86; `ui/check/CheckScreenIntegrationTest.kt` L237, L248; `ui/nav/AppFlowIntegrationTest.kt` L147; `ui/nav/NavigationTest.kt` L24 | → "Save" | C9 |
| 8 | `ui/profile/ProfileScreenTest.kt` L312 | → the C11 text | C11 |
| 8 | Any assertion of "Replace photo" | → "Change photo" | C10 |

`history_delete_description` and `history_delete_message` assertions do not change (AC44).

### Further changes the approved ACs force (not in the spec's list; owner-approved in OD1)
The owner approved E1-E8 exactly as listed below ("Approve E1–E8", OD1, 2026-10-22T10:59:00Z). Each new assertion is exact and at least as strict as the old one. The reviewer checks each diff against this table; a change not in either table is still not allowed without a new owner decision.

| ID | Task | File and lines | Old → new | Forced by |
|---|---|---|---|---|
| E1 | 3 | `ui/check/CheckCardTest.kt` L244-245, L255-257; helper `ui/a11y/AccessibilityAssertions.kt` L110-114; `ui/a11y/AccessibilityAssertionsTest.kt` L70 | The rating node's spoken text equals "<Rating>. <Reason>." → equals "<Rating>. <Reason>. Air …, Rain …, Wind …, Pavement …" in full. The helper gains a `factors` argument and still uses `assertContentDescriptionEquals`, never a prefix. | AC19, K8 |
| E2 | 2 | `SmokeTest.kt` L35; `ui/nav/NavigationTest.kt` L28, L38; `ui/nav/AppFlowIntegrationTest.kt` L181; `ui/recording/WalkRecordingScreensTest.kt` L216 | "Dog Walk Buddy" (the Home title) displayed → the "Today" nav item `assertIsSelected()` **and** the check title displayed. The Home screen and its title no longer exist; `app_name` remains the launcher label. | AC9, AC10 |
| E3 | 2 | `ui/nav/NavigationTest.kt` L26, L36; `ui/check/CheckScreenIntegrationTest.kt` L251; `ui/history/HistoryScreenTest.kt` L273; `ui/nav/AppFlowIntegrationTest.kt` `back()` where it leaves History or Profile (L143, L149, L289); `ui/profile/ProfileScreenTest.kt` L321 | Tap "Back" on History or Profile → system Back (`onBackPressedDispatcher.onBackPressed()`) or a tap on the "Today" nav item.<br>**ProfileScreenTest L321** (`onNodeWithText("Back")` at least 48 x 48). `ProfileScreenTest` hosts the screen alone, with no nav bar, so the assertion moves to where the dog nav item, the control that replaces Back, exists:<br>• `ui/nav/NavigationTest.kt`, in `AC41_home_opens_the_dog_profile_and_back_returns_home` (hosts `MainActivity` with the nav bar), while the dog tab is shown: `onNodeWithTag(NAV_DOG_TAG).assertIsSelected()` and `onNodeWithTag(NAV_DOG_TAG).assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)`. Same helper and threshold as L321.<br>• In `ProfileScreenTest`, L321 is replaced by the same `assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)` on the "Privacy & data" and "About the advice" tiles, the screen's new controls, so that test checks as many controls as before.<br>(Placement clarified at G2 attempt 2; this is the change OD1 approved.)<br>**HistoryScreenTest L270-276** (`back_returns_to_the_previous_screen`: tap "Back" at L273, then `assertEquals(1, backs)` at L275, where `backs` is the `onBack` counter, L57 and L99). `HistoryScreenTest` hosts `HistoryRoute` alone. After task 2 that host has no Back control, no `onBack` parameter, no nav bar and no `BackHandler` (see Navigation), so neither system Back nor the "Today" item can be used there. The check moves to the one host where system Back from Walks exists:<br>• `ui/nav/NavigationTest.kt`, in `AC35_home_opens_the_walk_history_and_back_returns_home` (hosts `MainActivity`). Its L36 step is fixed to **system Back**, not the "Today" tap: `composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }`. After it the test asserts, in this order: `onNodeWithTag(NAV_TODAY_TAG).assertIsSelected()` (AC11(a)); `onNodeWithTag(NAV_WALKS_TAG).assertIsNotSelected()`; the check title displayed (E2, L38); and `assertFalse(composeRule.activity.isFinishing)`.<br>• **What happens to the old test:** it is removed from `HistoryScreenTest`, together with the `backs` field (L57) whose only uses are L99 and L275. It is not rewritten in place, because its subject (the `onBack` callback and the Back control on the Walks screen) no longer exists, and a screen-only host cannot send system Back into a `NavHost`. This is a move of the check, not the loss of one: the behaviour it guarded, "Back from Walks returns to the previous screen, once", is asserted in `NavigationTest` in the real app.<br>• **At least as strict.** The old test proved that one tap called a callback exactly once; it never checked where the app went. The new check proves that one system Back leaves Walks (Walks not selected), lands on Today (Today selected and the check title displayed), and pops exactly one entry (if Back had popped twice, it would have left Today and finished the activity, which `isFinishing == false` rules out). Every fact the old test proved is still proved, and more.<br>• `HistoryScreenTest` still guards the screen itself: task 6's `UX_AC39_*` test (already mapped to AC39) asserts the Walks tab has no back control (`onNodeWithText("Back").assertDoesNotExist()` and `onNodeWithContentDescription("Back").assertDoesNotExist()`).<br>(Placement clarified at G2 attempt 3; this is the change OD1 approved. E3 already names `HistoryScreenTest.kt` L273 and replaces its Back tap with system Back; this note only fixes where that system Back runs, as was done for ProfileScreenTest L321.)<br>**Argument-only edits, no assertion change.** Removing the `onBack` parameter from `HistoryRoute`, `HistoryContent`, `ProfileRoute` and `ProfileScreen` makes these test call sites fail to compile. Task 2 deletes the `onBack = …` argument line (or, at ProfileScreenTest L77, the `, onBack = {}` argument) and changes nothing else in the test:<br>• `ui/history/HistoryScreenTest.kt` L99 (`HistoryRoute`, `onBack = { backs++ }`), L321 (`HistoryContent`), L416 (`HistoryContent`)<br>• `ui/history/HistoryRowOpenTest.kt` L83 (`HistoryRoute`)<br>• `ui/profile/ProfileScreenTest.kt` L77 (`ProfileRoute(viewModel = current, onBack = {})` → `ProfileRoute(viewModel = current)`)<br>• `ui/profile/ProfileScreenStateTest.kt` L39 (`ProfileScreen`)<br>• `ui/history/HistoryScreenTest.kt` L57 (`private var backs = 0`), removed with the moved test above, because nothing else uses it.<br>Complete list, from a grep of `app/src/test` for `onBack =` and for every `HistoryRoute(`, `HistoryContent(`, `ProfileRoute(` and `ProfileScreen(` call (no call passes `onBack` by position). The other test `onBack =` arguments (`ui/about/AboutScreenTest.kt` L34, `ui/walkdetail/WalkDetailScreenTest.kt` L67, `ui/privacy/PrivacyScreenTest.kt` L50) stay, because About, Walk detail and Privacy keep their back control (AC12). The matching production call sites (`ui/nav/DogWalkBuddyNavHost.kt` L94 and L123, `ui/history/HistoryScreen.kt` L104, `ui/profile/ProfileScreen.kt` L82) are task 2 build changes, not test changes. | AC11, AC39, AC50 |
| E4 | 2 (7 for the detail-specific lines) | `ui/walkdetail/WalkDetailScreenTest.kt` L215-216, L224; `ui/recording/WalkRecordingScreensTest.kt` L226; `ui/about/AboutScreenTest.kt` L166; `ui/privacy/PrivacyScreenTest.kt` L120; `ui/check/CheckScreenIntegrationTest.kt` L147; `ui/nav/AppFlowIntegrationTest.kt` L110 | `onNodeWithText("Back")` → `onNodeWithContentDescription("Back")`, the same exact label, because the back control is an icon button. WalkDetailScreenTest L215 "Walk details" displayed → heading "Monday 19 October" displayed (AC46 removes the title). | AC12, AC46 |
| E5 | 7 | `ui/nav/AppFlowIntegrationTest.kt` L178 | "Weather check" displayed → "MET Norway · when the check runs" displayed (exact) | D10 |
| E6 | 6 | `ui/history/HistoryScreenTest.kt` L200, L216, L232, L256-257; `ui/history/HistoryRowOpenTest.kt` L135 | Click the "Delete walk on …" control directly → first open that row's "More" menu, then the item with the same exact content description | AC44, OD2 |
| E7 | 2 | `ui/check/CheckScreenIntegrationTest.kt` L143 | `onAllNodesWithAboutLink()[1]` (the deleted Home link) → `[0]`, the footnote link | AC10 |
| E8 | 3 | `ui/check/CheckCardTest.kt` builders (`ratedState`) and `CheckUiState.Rated(...)` constructor calls | Mechanical: the new fields get the test's existing values (hours = the one rated hour, conditions from the MVP inputs). No assertion changes. | AC17 |

Kept unchanged on purpose:
- "Rating thresholds" (the table heading), "No photo" (the placeholder caption) and "Delete your data" stay visible, so `AboutScreenTest` L44, `CheckScreenIntegrationTest` L144, `ProfileScreenTest` L210 and L214 and any Privacy heading assertion need no change.
- "Breed (optional)" stays one field label.
- The "More" debug-menu assertions (`HistoryScreenTest` L282, L290) still hold, because the row "More" is an icon with a content description, not text.
- Checked in the home-link scan and needing no change:
  - `CheckScreenIntegrationTest` L101, L114, L133 use `onAllNodesWithAboutLink()[0]`. That is the footnote link, which comes before the Home links today and is the only match afterwards.
  - `CheckCardTest` L109 taps the card's own footnote "About this advice", not a Home link.
  - `NavigationTest` L42-44 (`assertAccessibleScreen()` on the first screen) is unchanged. It now also checks the nav items.

## Differences from the artboards (AC66)
Each screen task records its own differences in its PR. The known ones are:
- **Today:**
  - The footnote block sits above the best-window card (K1).
  - "About this advice" is its own button line, not an inline link.
  - The pollen notice, sensitivity line and attribution are separate lines (K2, K9).
  - The Rain cell reads "0 mm/h" with caption "Rain" (AC17), not "Dry".
  - The best-window range keeps the hyphen (C3).
  - Strip labels follow the device time format.
  - Can't check and Location keep the disclaimer (K3).
  - Precise location is a bottom sheet.
- **Borders.** Input and outlined-button borders use `iconMuted`, not `outline` (ADR 0007). The nav top border uses `outlineVariant`, not `#D9E1EB`.
- **Recording:** the back control is kept (K6). There is no "GPS good" (Later). The label reads "Elapsed time" (D6).
- **Walks:** each row has a "More" menu (AC44, OD2). Dark mode uses the dark dot mapping above.
- **Walk detail:** the back label is "Back" (D8). The tiles keep their light style in dark mode (Later), so the route colour is fixed.
- **Profile:** the placeholder is an icon circle, not the paw art (pack rule 6). The "No photo" caption is kept. Remove photo is at least 48 dp (K4). The text-field label position depends on the Material 3 version (see Dog profile).
- **Privacy:** the existing sentences replace the artboard body text (K10). There is no "not backed up" panel (its content is in `privacy_local`).
- **About:**
  - The "Rating thresholds" table heading is kept.
  - The table cells use exact boundaries (K5).
  - The threshold sentences are kept.
  - The "Data" heading is kept.
  - The attributions are separate texts (K9).
  - The font licence sections are appended.

## Approved copy by task
| Task | Items |
|---|---|
| 2 | C7 (`history_title`, `nav_walks`), C16 (`nav_today`, `nav_dog`), D9 tiles (`profile_privacy_tile`, `profile_about_tile`) |
| 3 | C1 (`check_title_named`), C12 (`check_banner_offline_lead`, `check_banner_cached`), D1 (`check_meta_just_now`), D2 (`check_sensitive_thresholds`), D3 (factor captions, value formats, Not rated / Cool / Warm / Hot / No data, `check_factor_spoken`) |
| 4 | C2 (`check_window_heading`), C4 (`check_retry`), C13 (`check_can_still_record_lead`, `check_can_still_record`), D4 (`check_cant_check_explanation`), D5 (`check_needs_location` value, body, three bullets, `check_how_data_used`) |
| 5 | C5 (`recording_title`), C6 (`recording_stop`), C15 (`recording_started_label`), C18 (`recording_route_label`, `recording_route_caption`), D6 (`recording_tip`, plurals `duration_hours`, `duration_minutes`, `duration_seconds`) |
| 6 | D7 (`history_week_range_*`, `history_totals_dotted`, `history_walk_at`, `history_row_spoken`, `history_summary_spoken`, `history_caption_walks` plural, `history_caption_hmm`, `history_caption_miles`) |
| 7 | C14 (`walk_detail_time_range`), C17 (`walk_detail_map_unavailable_sub`), D8 (`walk_detail_duration_caption`, `walk_detail_distance_caption`, `walk_detail_delete`), D10 (`privacy_title` value, `privacy_headline`, `privacy_card_local_heading`, `privacy_card_sent_heading`, `privacy_entry_met_heading`, `privacy_entry_map_heading`) |
| 8 | C8 (`profile_title`), C9 (`profile_save`), C10 (`profile_replace_photo` value), C11 (`profile_sensitivity_hint`), D9 (`profile_counter`, `profile_band_normal`, `profile_band_sensitive`), D11 (`about_headline`, `about_intro`, `about_sources_heading` value, `about_table_*` cells and headers) |

Task 1 adds only the two font-name headings that AC8 requires ("Bricolage Grotesque", "Figtree"). `ApprovedCopyTest` gains the rows for each task's items in that task.

## Owner decisions (design)
Both questions raised by the first version of this design were answered by the owner at 2026-10-22T10:59:00Z (recorded in `docs/runs/20261022-ux-redesign/state.json`, `owner_answers.design_round1`). Both answers match what this design already followed, so no task changes scope. Questions and answers are quoted verbatim.

**OD1. Test changes E1-E8** (`owner_answers.design_round1.test_changes_E1_E8`, 2026-10-22T10:59:00Z)
- Question: "The design needs eight test changes that weren't on the spec's list of test changes (E1–E8). Most follow controls that moved: Home is replaced by the bottom bar, there's no Back button on the main tabs, and Delete is now inside a "More" menu. One follows wording you approved (D10). Only E1 changes what TalkBack reads: the hero card announcement adds the four factor values, as the spec's AC19 requires. Each new check is exact and at least as strict as the old one. Approve these test changes?"
- Answer: "Approve E1–E8"
- Effect: E1-E8 in "Further changes the approved ACs force" are approved test changes under AC64, alongside the spec's own list. Task 2 makes E2, E3, E4 (non-detail lines) and E7; task 3 makes E1 and E8; task 6 makes E6; task 7 makes E4 (detail lines) and E5. Any test change outside the two tables still needs a new owner decision.
- Scope of E3 under OD1 (clarified at G2 attempt 3). The HistoryScreenTest L270-276 move and the argument-only `onBack` edits are inside "Approve E1–E8":
  - E3 as approved already names `HistoryScreenTest.kt` L273 and replaces its Back tap with system Back. The question told the owner "there's no Back button on the main tabs", which is why the test cannot stay on the Walks screen.
  - The clarification only fixes where that system Back runs (the `MainActivity` host, the only one with a `NavHost`), as was done for ProfileScreenTest L321 at attempt 2.
  - The new check is exact and stricter than the old one, which keeps the promise in the question ("Each new check is exact and at least as strict as the old one").
  - The argument edits change no assertion. They are the compile-time result of removing Back from the two tabs, which the owner approved.

**OD2. Row delete** (`owner_answers.design_round1.row_delete`, 2026-10-22T10:59:00Z)
- Question: "The spec says the walk row's "More" menu "stays", but in the MVP each row actually has a Delete button directly on it. Criterion AC44 and the mock-ups need a "More" menu. Should Delete move into a "More" menu on each row?"
- Answer: "Use a More menu"
- Effect: task 6 removes the row's direct Delete button and adds a "More" menu with one "Delete" item, which opens the confirm sheet (see Walks → Row). The delete label, confirm text and result are the same as the MVP. AC44 and the spec are unchanged.

## Task breakdown
Eight tasks, in dependency order. That is the run limit and the spec's estimate. Each is one PR into `feature/20261022-ux-redesign`.

| # | Slug | Title | ACs | Likely files | Depends on | Size |
|---|---|---|---|---|---|---|
| 1 | `theme-foundations` | Field Kit theme, fonts, rating icons, dark mode and contrast | AC1, AC2, AC3, AC4 (palette), AC5, AC6, AC7, AC8, AC40 (contrast pairs), AC62, AC63 (test scaffold), AC64, AC65 (fonts), AC67 (contrast pairs) | `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, `Dimens.kt` (new), `RecordingTheme.kt` (new); `res/font/*.ttf`, `res/raw/licence_ofl_*.txt`, `res/drawable/ic_rating_go.xml`, `ic_rating_caution.xml`, `ic_rating_avoid.xml` (replaced), `res/values/themes.xml`, `res/values-night/themes.xml`, `res/values/colors.xml` (window backgrounds), `res/values/strings.xml`; `AndroidManifest.xml` (`configChanges`); `MainActivity.kt` (`onConfigurationChanged` only, see task 1 scope note); `ui/about/AboutScreen.kt` (font sections only); tests `ui/theme/ContrastTest.kt` (extended), `ui/theme/FieldKitTokensTest.kt`, `ui/theme/FieldKitTypographyTest.kt`, `ui/theme/ThemeSwitchTest.kt`, `ui/theme/MaterialSymbolsIconFilesTest.kt` (new, ADR 0008 Amendment 1), `ui/about/AboutScreenTest.kt`, `ui/copy/FixedTextsTest.kt`, `ui/copy/ApprovedCopyTest.kt` | — | M |
| 2 | `bottom-nav-shell` | Bottom navigation, tabs, back controls and the dog-name label | AC9, AC10, AC11, AC12, AC13, AC14, AC15, AC16, AC53 (tiles), AC58 (helper extensions), AC60 (b, c), AC63 (C7, C16, D9 tiles), AC64 | `ui/nav/DogWalkBuddyNavHost.kt`, `ui/nav/AppScaffold.kt` (new), `ui/nav/FieldKitNavBar.kt` (new), `ui/nav/AppChromeViewModel.kt` (new), `ui/common/BackButton.kt` (new), `ui/home/HomeScreen.kt` (deleted, replaced by `ui/check/TodayScreen.kt` shell), `ui/history/HistoryScreen.kt` (remove back), `ui/profile/ProfileScreen.kt` (remove back, add tiles), `ui/recording/RecordingScreen.kt`, `ui/walkdetail/WalkDetailScreen.kt`, `ui/privacy/PrivacyScreen.kt`, `ui/about/AboutScreen.kt` (back control only), `MainActivity.kt`, `strings.xml`; `res/drawable/ic_nav_today.xml`, `ic_nav_walks.xml`, `ic_nav_dog.xml`, `ic_back.xml`, `ic_shield.xml`, `ic_info.xml`; tests `ui/a11y/AccessibilityAssertions.kt` (helper extensions), `ui/nav/BottomNavTest.kt` (new), `ui/nav/FullScreenDestinationsTest.kt` (new), `ui/nav/NavigationTest.kt`, `ui/nav/AppFlowIntegrationTest.kt`, `ui/check/CheckScreenIntegrationTest.kt`, `ui/profile/ProfileScreenTest.kt` (L321), `SmokeTest.kt`, plus the home-link row and the E2, E3, E4 and E7 lines (OD1) | 1 | M |
| 3 | `today-hero-and-footnote` | Today: header, banner, hero card with factor grid, footnote, pinned Start, loading | AC17, AC18, AC19, AC20, AC21, AC24, AC25, AC26, AC27, AC28, AC58, AC63 (C1, C12, D1-D3), AC64, AC66 | `domain/rating/RatingEngine.kt` (`pavementFactor`), `domain/check/CheckOutcome.kt`, `CheckRater.kt`, `FactorRounding.kt` (new), `FactorCell.kt` (new); `ui/check/CheckUiState.kt`, `CheckViewModel.kt`, `CheckCard.kt` → `TodayScreen.kt`, `TodayHeader.kt`, `RatingHeroCard.kt`, `FactorGrid.kt`, `TodayFootnote.kt` (new); `ui/common/InfoBanner.kt`, `PrimaryButton.kt` (new); `ui/recording/StartWalkSection.kt`; `strings.xml`; `res/drawable/ic_factor_*.xml`, `ic_refresh.xml`, `ic_play.xml`, `ic_wifi_off.xml`, `ic_location.xml`, `ic_schedule.xml`; tests `domain/check/FactorRoundingTest.kt`, `domain/rating/PavementFactorTest.kt`, `ui/check/RatingHeroCardTest.kt`, `TodayHeaderTest.kt`, `TodayFootnoteTest.kt`, `TodayLayoutTest.kt`, `CheckCardTest.kt`, `CheckScreenIntegrationTest.kt`, `CheckViewModelTest.kt`, `ui/recording/WalkRecordingScreensTest.kt` (Start) | 2 | M |
| 4 | `today-window-and-states` | Today: best-window card and 12-hour strip; Can't check, location and precise-location states | AC22, AC23, AC29, AC30, AC31, AC32, AC58, AC61 (strip), AC63 (C2, C4, C13, D4, D5), AC64, AC66 | `ui/check/BestWindowCard.kt`, `HourStrip.kt`, `HourLabelFormat.kt`, `CantCheckCard.kt`, `LocationExplainer.kt` (new); `ui/common/FieldKitBottomSheet.kt`, `PermissionExplainer.kt` (new); `ui/recording/StartWalkSection.kt`, `StartWalkViewModel.kt` (`onExplanationDismissed`); `strings.xml`; `res/drawable/ic_cloud_off.xml`, `ic_my_location.xml`, `ic_check.xml`, `ic_walk.xml`; tests `ui/check/HourLabelFormatTest.kt`, `BestWindowCardTest.kt`, `CantCheckTest.kt`, `LocationNeededTest.kt`, `CheckCardTest.kt`, `CheckScreenIntegrationTest.kt`, `ui/recording/WalkRecordingScreensTest.kt` (precise), `ui/recording/StartWalkViewModelTest.kt` | 3 | M |
| 5 | `recording-restyle-route-line` | Recording: always-dark restyle, h:mm:ss timer, stat cards and live route line | AC4 (integration), AC33, AC34, AC35, AC36, AC37, AC38, AC58, AC63 (C5, C6, C15, C18, D6), AC64, AC65 (route line), AC66 | `ui/recording/RecordingScreen.kt`, `RecordingViewModel.kt`, `RouteSketch.kt` (new); `ui/common/StatCard.kt`, `SpokenDuration.kt` (new); `domain/walk/LiveWalkFormat.kt`, `RouteProjection.kt` (new); `data/db/WalkDao.kt` (`observePoints`), `data/repo/WalkRepository.kt`; `strings.xml`; `res/drawable/ic_stop.xml` (and `ic_walk.xml` if task 4 has not merged); tests `ui/recording/RecordingScreenTest.kt` (new), `WalkRecordingScreensTest.kt`, `domain/walk/LiveWalkFormatTest.kt`, `RouteProjectionTest.kt`, `ui/common/SpokenDurationTest.kt`, `data/repo/WalkRepositoryTest.kt` | 2 | M |
| 6 | `walks-tab` | Walks: this-week card with day dots, week sections, rows with More menu (OD2), empty and error states, delete sheet | AC39, AC40, AC41, AC42, AC43, AC44, AC58, AC60 (a: Walks), AC63 (D7), AC64, AC66 | `ui/history/HistoryScreen.kt`, `HistoryViewModel.kt`, `WalkDateText.kt`, `ThisWeekCard.kt`, `WeekDots.kt` (new); `domain/walk/WeeklyTotals.kt` (`dayDots`); `ui/common/ConfirmSheet.kt` (new); `strings.xml`; `res/drawable/ic_chevron_right.xml`, `ic_more.xml`, `ic_delete.xml`; tests `ui/history/HistoryScreenTest.kt`, `HistoryRowOpenTest.kt`, `HistoryViewModelTest.kt`, `WalkDateTextTest.kt`, `domain/walk/WeekDotsTest.kt`, `ui/nav/AppFlowIntegrationTest.kt` (row lines) | 4, 5 | M |
| 7 | `walk-detail-and-privacy` | Walk detail (edge-to-edge map, sheet, delete) and Privacy (cards, delete-all sheet) | AC45, AC46, AC47, AC48, AC49, AC54, AC55, AC58, AC60 (a: Walk detail, Privacy), AC63 (C14, C17, D8, D10), AC64, AC66 | `ui/walkdetail/WalkDetailScreen.kt`, `WalkDetailViewModel.kt`; `map/RouteStyle.kt` (new), `map/maplibre/MapLibreRouteMap.kt` (casing, start marker); `ui/common/DestructiveOutlinedButton.kt` (new); `ui/privacy/PrivacyScreen.kt`; `strings.xml`; `res/drawable/ic_map_off.xml`, `ic_phone.xml`, `ic_send_out.xml`; tests `ui/walkdetail/WalkDetailScreenTest.kt`, `WalkDetailViewModelTest.kt`, `ui/privacy/PrivacyScreenTest.kt`, `ui/nav/AppFlowIntegrationTest.kt` (detail and privacy lines), `map/RouteStyleTest.kt` | 6 | M |
| 8 | `dog-profile-and-about` | Dog profile (photo row, counters, radio cards, Save, tiles) and About (thresholds table) | AC50, AC51, AC52, AC53, AC56, AC57, AC58, AC59 (checklist), AC60 (a: Dog, About), AC61 (About headers), AC63 (C8-C11, D9, D11), AC64, AC66, AC67 | `ui/profile/ProfileScreen.kt`; `ui/about/AboutScreen.kt`, `ThresholdsTable.kt` (new); `strings.xml`; `res/drawable/ic_image.xml`; tests `ui/profile/ProfileScreenTest.kt`, `ProfileScreenStateTest.kt`, `ui/about/AboutScreenTest.kt`, `ui/check/CheckScreenIntegrationTest.kt` (Save), `ui/nav/AppFlowIntegrationTest.kt`, `ui/nav/NavigationTest.kt` (Save) | 2, 7 (shared buttons) | M |

### Task details
Every task:
- applies only its own approved copy and deletes strings it makes unused
- adds only the icons it is the first to use (ADR 0008)
- runs the full suite (AC64)
- keeps changed-lines coverage at 80 % or more.

Every task from 2 on also extends `assertAccessibleScreen` coverage to its screens in light and night (AC58). Every screen task (3-8) attaches emulator screenshots in both themes with its AC66 difference list.

1. **`theme-foundations`** (M; depends on nothing; the foundation every other task builds on)
   - **Size note.** Re-estimated at G2 attempt 2. The first version was large for an M. It now keeps only what must exist before any screen changes: the icons other than the three rating icons moved to the tasks that first use them (ADR 0008, which Lint also requires), `FieldKitBottomSheet` moved to task 4, and the accessibility helper extensions moved to task 2. The About font licence sections stay here, because they are what uses the bundled OFL texts and font-name strings, and Lint `UnusedResources` would fail without them.
   - **Build:** the token objects and Material 3 mapping (ADR 0007), typography with the two variable fonts (ADR 0006), shapes, spacing, sizes, the three replaced rating icons (ADR 0008), `RecordingTheme`, light and night window themes, and `configChanges="uiMode"`.
   - **About:** the two font licence sections (AC8). The task 1 developer downloads the four font files (two TTFs and two `OFL.txt`) and the three rating icon XMLs, as the ADRs say, and records the source commits. Nothing was downloaded at design time.
   - **Scope note (architecture decision, 2026-10-22, PR #40).** These are in task 1 scope:
     - `res/values/colors.xml`: the two `field_kit_background_*` colours that the light and night window themes use for `android:windowBackground` (ADR 0007).
     - Rating icon tint: the three rating icons use `android:tint="?android:attr/colorControlNormal"` with the header notice from ADR 0008 Amendment 1. Task 1 adds no `res/values/attrs.xml` and no `colorControlNormal` theme item.
     - The guard test `ui/theme/MaterialSymbolsIconFilesTest.kt` (ADR 0008 Amendment 1).
     - `MainActivity.onConfigurationChanged` calls `super` and then `enableEdgeToEdge()`. This fits ADR 0007: dark mode follows the system theme and the activity is not re-created (AC7). `enableEdgeToEdge()` picks the light or dark system-bar icon style from the configuration at the time it is called. Without the second call, the status and navigation bar icons would keep the old theme's style after a switch, for example dark icons on the dark background. This is the only `MainActivity` change in task 1. The shell work on `MainActivity` stays in task 2.
       - Test: `ThemeSwitchTest` (`UX_AC7_*`) also asserts that the window's light-status-bar appearance (`WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars`) is `true` in light and `false` after the switch to night. This also covers the new lines. The manual AC7 check includes the status bar icons.
   - **Tests:**
     - `UX_AC1`-`UX_AC5`.
     - `ContrastTest` extended with every pair in the contrast table (`UX_AC6`), including the summaryCaption pairs (AC40) and the errorText pairs (AC67). The MVP assertions in that file are unchanged.
     - `UX_AC7`: night-mode switch on Today keeps the same activity instance and state, and the system bar icon style follows the theme.
     - `UX_AC8`, `UX_AC62`, the `UX_AC63` scaffold, and `UX_AC65` (no GoogleFont class on the classpath; all `Font`s are `ResourceFont`).
     - The Material Symbols icon-file guard (ADR 0008 Amendment 1).
   - **Test assertion changes:** none. MVP tests must pass unchanged with the new theme (AC64).
   - **PR also reports:** file sizes, SHA-256 checksums and the APK size delta.
2. **`bottom-nav-shell`** (M; depends on 1)
   - **Build:** `AppScaffold`, `FieldKitNavBar` (selected semantics, ellipsised name, full spoken name, `NAV_*_TAG` tags), `AppChromeViewModel`, and the Today route as a shell. The current check content and Start are moved under it unchanged in look. Add `BackButton` on the four full-screen destinations; remove Back from History and Profile; add the two tiles with their icons (AC53 tile behaviour); delete `HomeScreen`. Add the nav, back and tile icons (ADR 0008).
   - **Test helpers:** extend `ui/a11y/AccessibilityAssertions.kt` with `assertHeadingsMarked(...)`, `assertHiddenFromAccessibility(tag)` and the optional `allowEllipsisTags` on `assertNoTextTruncation()` (default unchanged).
   - **Tests:** `UX_AC9`-`UX_AC16`, `UX_AC60` (b, c). `UX_AC13` and `UX_AC14` cover the tiles opening Privacy and About.
   - **Test assertion changes:** the home-link row (every line listed); E2, E3 (including the ProfileScreenTest L321 move, the HistoryScreenTest L270-276 move into `NavigationTest` AC35 with system Back, and the argument-only `onBack` edits listed under E3), E4 (non-detail lines) and E7, all owner-approved in OD1. Copy C7, C16, D9 tiles.
3. **`today-hero-and-footnote`** (M; depends on 2)
   - **Build:** the domain additions (`pavementFactor`, `FactorRounding`, `driverOf`, `CheckOutcome` fields); `CheckViewModel` name and offline; the header with refresh; the banner; the hero with factor grid, highlight and full announcement; the footnote; `StartWalkButton`; the loading skeleton; the fixed scroll order.
   - **Tests:** `UX_AC17`-`UX_AC21`, `UX_AC24`-`UX_AC28`. AC25 uses the four cases at `w411dp-h891dp-xxhdpi`. AC27 runs at 200 %.
   - **Test assertion changes:** C1 row; E1, E8, both owner-approved in OD1.
4. **`today-window-and-states`** (M; depends on 3)
   - **Build:** `BestWindowCard` with chip; `HourStrip` and `HourLabelFormat` (24-hour and 12-hour "2pm"); Can't check card and panel; `FieldKitBottomSheet`; `PermissionExplainer`; location needed and blocked; the precise-location sheet and dismissal.
   - **Tests:**
     - `UX_AC22`, `UX_AC29`-`UX_AC32`.
     - `UX_AC23`: bar count, order, heights 44 / 28 / 14, marker columns, labels in both formats at the test clock in UTC, no label bounds intersect, strip hidden.
     - `UX_AC61`: bar heights differ, and words and icons differ.
   - **Test assertion changes:** C2, C4 and D5 rows.
5. **`recording-restyle-route-line`** (M; depends on 2)
   - **Build:** `RecordingTheme` applied; status pill; h:mm:ss timer with spoken duration; Distance and Started cards; `RouteProjection` and `RouteSketch` from `observePoints`; tip and notifications-off line; Stop restyle.
   - **Tests:**
     - `UX_AC4` (palette identical under `+night`), `UX_AC33`, `UX_AC34`, `UX_AC36`-`UX_AC38`.
     - `UX_AC35`: inserting fix C redraws with the current marker at C; fewer than 2 fixes gives an empty panel; MockWebServer `requestCount` is unchanged while Recording is shown.
   - **Test assertion changes:** C5, C6 and h:mm:ss rows.
6. **`walks-tab`** (M; depends on 4 for `FieldKitBottomSheet`, and on 5 for `SpokenDuration` and `StatCard`)
   - **Build:** `WalkDateText`; the this-week card with `summaryCaption` and spoken summary; `dayDots`; week sections; rows with "More" → Delete → `ConfirmSheet`, replacing the MVP row's direct Delete button (owner decision OD2, "Use a More menu"); empty and error states.
   - **Tests:** `UX_AC39`-`UX_AC44` (AC40 and AC41 in light and night; AC39 also with the clock in 2027 and a year-spanning week), `UX_AC60` (a) for Walks.
   - **Test assertion changes:** the D7 rows; E6 (owner-approved in OD1).
7. **`walk-detail-and-privacy`** (M; depends on 6 for `WalkDateText` and `ConfirmSheet`)
   - **Build:** the edge-to-edge map with floating back; `RouteStyle` and the adapter layers (manual check); the sheet with date heading, time range and two cards; `DestructiveOutlinedButton`; the delete sheet and ViewModel delete; offline, no-route and not-found states; the Privacy cards, headline, delete-all sheet and existing status texts.
   - **Tests:** `UX_AC45` (automated part), `UX_AC46`-`UX_AC49`, `UX_AC54`, `UX_AC55`, `UX_AC60` (a) for Walk detail and Privacy.
   - **Test assertion changes:** the D8 and C14 rows; E4 (detail lines) and E5, both owner-approved in OD1.
8. **`dog-profile-and-about`** (M; depends on 2 for the tiles and 7 for the shared button styles)
   - **Profile:** photo row and errors in `errorText`; fields with counters and the muted "(optional)"; radio cards with selected semantics on both options (F1); Save; tile restyle.
   - **About:** headline and intro, the thresholds table with collection semantics, the sources heading, the muted panel. The existing sentences and licences are kept.
   - **Tests:** `UX_AC50`-`UX_AC53`, `UX_AC67` (light and night), `UX_AC56`, `UX_AC57`, `UX_AC61` (About header icons), `UX_AC60` (a) for Dog and About.
   - **Manual checklist:** the PR adds the AC59 TalkBack walk-through checklist to the run's device-check file, for the release session.
   - **Test assertion changes:** the C9, C10 and C11 rows.

## Traceability check
Every AC maps to at least one task and a test. No task lacks an AC. For every AC, the set of tasks below equals the set of tasks whose row in "Task breakdown" lists that AC (checked line by line at G2 attempt 2).

| AC | Task(s) | Test |
|---|---|---|
| AC1 | 1 | unit `ui/theme/FieldKitTokensTest` `UX_AC1_*` |
| AC2 | 1 | unit `FieldKitTokensTest` `UX_AC2_*` |
| AC3 | 1 | unit `FieldKitTokensTest` `UX_AC3_*` |
| AC4 | 1, 5 | unit `FieldKitTokensTest` (palette); integration `ui/recording/RecordingScreenTest` `UX_AC4_*` (default and `+night` identical); manual |
| AC5 | 1 | unit `ui/theme/FieldKitTypographyTest` `UX_AC5_*` |
| AC6 | 1 | unit `ui/theme/ContrastTest` `UX_AC6_*` (every pair in the contrast table) |
| AC7 | 1 | integration `ui/theme/ThemeSwitchTest` `UX_AC7_*`; manual on the seven screens |
| AC8 | 1 | integration `ui/about/AboutScreenTest` `UX_AC8_*` |
| AC9 | 2 | integration `ui/nav/BottomNavTest` `UX_AC9_*` |
| AC10 | 2 | `BottomNavTest` `UX_AC10_*` |
| AC11 | 2 | `BottomNavTest` `UX_AC11_*` (a, b, c; activity finishing) |
| AC12 | 2 | `ui/nav/FullScreenDestinationsTest` `UX_AC12_*` |
| AC13 | 2 | `BottomNavTest` `UX_AC13_*` |
| AC14 | 2 | `BottomNavTest` `UX_AC14_*` |
| AC15 | 2 | `BottomNavTest` `UX_AC15_*` |
| AC16 | 2 | `ui/recording/WalkRecordingScreensTest` `UX_AC16_*` |
| AC17 | 3 | unit `domain/check/FactorRoundingTest`, `domain/rating/PavementFactorTest`; integration `ui/check/RatingHeroCardTest` `UX_AC17_*` |
| AC18 | 3 | `RatingHeroCardTest` `UX_AC18_*` (light and night) |
| AC19 | 3 | `RatingHeroCardTest` `UX_AC19_*` |
| AC20 | 3 | `ui/check/TodayHeaderTest`, `CheckScreenIntegrationTest` `UX_AC20_*` |
| AC21 | 3 | `CheckScreenIntegrationTest` `UX_AC21_*` (offline and online fakes) |
| AC22 | 4 | `ui/check/BestWindowCardTest` `UX_AC22_*` |
| AC23 | 4 | unit `ui/check/HourLabelFormatTest`; integration `BestWindowCardTest` `UX_AC23_*` |
| AC24 | 3 | `ui/check/TodayFootnoteTest` `UX_AC24_*` |
| AC25 | 3 | `ui/check/TodayLayoutTest` `UX_AC25_*` (four cases) |
| AC26 | 3 | `WalkRecordingScreensTest` `UX_AC26_*` |
| AC27 | 3 | `TodayLayoutTest` `UX_AC27_*` (200 %); manual |
| AC28 | 3 | `ui/check/CheckCardTest` `UX_AC28_*` |
| AC29 | 4 | `ui/check/CantCheckTest` `UX_AC29_*` |
| AC30 | 4 | `ui/check/LocationNeededTest` `UX_AC30_*` |
| AC31 | 4 | `LocationNeededTest` `UX_AC31_*` |
| AC32 | 4 | `WalkRecordingScreensTest` `UX_AC32_*`, `StartWalkViewModelTest` |
| AC33 | 5 | `RecordingScreenTest` `UX_AC33_*` |
| AC34 | 5 | unit `ui/common/SpokenDurationTest`, `domain/walk/LiveWalkFormatTest`; `RecordingScreenTest` `UX_AC34_*` |
| AC35 | 5 | unit `domain/walk/RouteProjectionTest`; `RecordingScreenTest` `UX_AC35_*` |
| AC36 | 5 | `RecordingScreenTest` `UX_AC36_*` |
| AC37 | 5 | `RecordingScreenTest` `UX_AC37_*` |
| AC38 | 5 | `RecordingScreenTest` `UX_AC38_*` (200 %); manual |
| AC39 | 6 | unit `ui/history/WalkDateTextTest`; `HistoryScreenTest` `UX_AC39_*` |
| AC40 | 1, 6 | `ContrastTest` summaryCaption pairs (1); `HistoryScreenTest` `UX_AC40_*` (light and night) (6) |
| AC41 | 6 | unit `domain/walk/WeekDotsTest`; `HistoryScreenTest` `UX_AC41_*` |
| AC42 | 6 | `HistoryScreenTest` `UX_AC42_*` |
| AC43 | 6 | `HistoryScreenTest` `UX_AC43_*` |
| AC44 | 6 (OD2) | `HistoryScreenTest` `UX_AC44_*` |
| AC45 | 7 | `WalkDetailScreenTest` `UX_AC45_*`; unit `map/RouteStyleTest`; manual |
| AC46 | 7 | `WalkDetailScreenTest` `UX_AC46_*`; `WalkDateTextTest` |
| AC47 | 7 | `WalkDetailScreenTest` `UX_AC47_*` |
| AC48 | 7 | `WalkDetailScreenTest` `UX_AC48_*`, `WalkDetailViewModelTest` |
| AC49 | 7 | `WalkDetailScreenTest` `UX_AC49_*` |
| AC50 | 8 | `ProfileScreenTest` `UX_AC50_*` |
| AC51 | 8 | `ProfileScreenTest` `UX_AC51_*` |
| AC52 | 8 | `ProfileScreenTest` `UX_AC52_*`; manual (AC59) |
| AC53 | 2, 8 | `BottomNavTest` `UX_AC13_*` and `UX_AC14_*` (tiles open Privacy and About) (2); `ProfileScreenTest` `UX_AC53_*` (8) |
| AC54 | 7 | `PrivacyScreenTest` `UX_AC54_*` |
| AC55 | 7 | `PrivacyScreenTest` `UX_AC55_*` |
| AC56 | 8 | `AboutScreenTest` `UX_AC56_*`; manual (AC59) |
| AC57 | 8 | `AboutScreenTest` `UX_AC57_*` |
| AC58 | 2-8 (helper extensions in 2) | `assertAccessibleScreen` and heading and hidden checks in each screen's tests, light and night |
| AC59 | 8 | checklist added in task 8, covering every screen; manual at the reference-phone release session |
| AC60 | 2 (b, c), 6, 7, 8 (a) | `BottomNavTest` `UX_AC60_*`; 200 % tests in the Walks, Walk detail, Privacy, Profile and About tests; manual |
| AC61 | 4, 8 | `BestWindowCardTest` and `AboutScreenTest` `UX_AC61_*`; manual greyscale |
| AC62 | 1 | `ui/copy/FixedTextsTest` `UX_AC62_*` |
| AC63 | 1 (scaffold), 2-8 | `ui/copy/ApprovedCopyTest` `UX_AC63_*`, plus the changed assertions listed above |
| AC64 | 1-8 | the full `testDebugUnitTest` suite on every PR, with only the spec-listed (including every home-link line) and OD1-approved (E1-E8) assertion changes |
| AC65 | 1, 5 | unit classpath and `ResourceFont` check (1); `RecordingScreenTest` no-request check (5); manual proxy check at release |
| AC66 | 3-8 | manual screenshot comparison per task PR and at release |
| AC67 | 1, 8 | `ContrastTest` errorText pairs (1); `ProfileScreenTest` `UX_AC67_*` (light and night) (8) |

Approved copy coverage: C1, C2, C4-C18 and D1-D11 each map to exactly one task (see "Approved copy by task"). C3 is rejected and not implemented.
