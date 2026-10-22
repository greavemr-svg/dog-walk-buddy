# Spec: UX redesign ("Field Kit", blue) before the first release

- Run: `20261022-ux-redesign`
- Epic: #31
- Status: Draft (G1 fix attempt 1: criteria renumbered AC1-AC67 and the factor-rounding owner decision recorded. Follow-ups: the negative-rounding owner decision replaced former assumption A11, the plan round 2 owner decisions (system Back, strip hour labels, long dog names) replaced former assumptions A3, A4 and A8, and the 12-hour strip-label owner decision ("2pm") replaced the planner's proposed label form. Ready for G1 re-run)
- Base: `feature/20261022-ux-redesign`, branched from `feature/20261019-dog-walk-buddy-mvp` at `a8b4506`, which holds the full MVP app. It has not been released.
- Design input (outside the repo, read by absolute path):
  - `C:\Users\greav\projects\dog-walk-buddy-ux\README.md`: rules for agents, binding.
  - `C:\Users\greav\projects\dog-walk-buddy-ux\ux-spec.md`: the screen spec, cited below as "pack §n".
  - `C:\Users\greav\projects\dog-walk-buddy-ux\tokens.json`: colour, type, spacing, radius and size tokens.
  - `C:\Users\greav\projects\dog-walk-buddy-ux\screens\*.dc.html`: 14 artboards. Use their exact values only.

## Problem
The MVP app comes from run `20261019-dog-walk-buddy-mvp`. Its release, v0.1.0, is on hold in PR #28. The app meets its 53 acceptance criteria, but it looks like a plain list of controls:
- The home screen is the check card followed by a column of link buttons.
- It uses default Material styling.
- Its dark theme is a minimal colour swap.

The owner has made a full visual design ("Field Kit", blue). They want the app to follow it **before** the first release, so users never see the interim look. The redesign:
- makes the one-glance answer bigger
- puts Start walk within thumb reach
- replaces the link list with a bottom navigation bar
- adds a designed dark mode.

It must not change what the app does or weaken any MVP guarantee. The rating rules, data handling, privacy, offline behaviour and accessibility floors all stay exactly as the MVP spec states them.

## Users and context
These are unchanged from the MVP spec:
- Everyday UK dog owners with one dog.
- Android phones only, in English (UK).
- Often used one-handed outdoors in bright sun, and sometimes offline.
- Some users rely on TalkBack or a 200 % font size.

Dark mode matters because people use the app early in the morning and late in the evening.

## Precedence (binding for this run)
1. `docs/specs/20261019-dog-walk-buddy-mvp.md` and its criteria MVP AC1-AC53 win over the design pack. Where the pack conflicts with an MVP criterion, follow the MVP criterion (see "Conflicts between the pack and the MVP spec").
2. Existing strings in `app/src/main/res/values/strings.xml` win. The only exceptions are the copy changes the owner approved on 2026-10-22, listed in "Approved copy".
3. Accessibility floors are unchanged (MVP AC50-AC53).
4. Where this spec and the pack differ, this spec wins.

## Owner decisions (2026-10-22)
The owner answered the plan questions, two questions raised at Gate G1, and four follow-up questions. The answers are recorded verbatim in `docs/runs/20261022-ux-redesign/state.json`: `owner_answers.plan_round1` (timestamp `2026-10-22T09:48:57Z`), `owner_answers.factor_rounding` (timestamp `2026-10-22T10:01:59Z`), `owner_answers.negative_rounding` (timestamp `2026-10-22T10:07:10Z`), `owner_answers.plan_round2` (timestamp `2026-10-22T10:11:34Z`) and `owner_answers.hour_label_12h` (timestamp `2026-10-22T10:16:31Z`).

| Topic | Owner answer (verbatim) | What it means in this spec |
|---|---|---|
| Copy changes (C1-C18, D1-D11) | "Approve all but C3" | C1, C2 and C4-C18 are approved. D1-D11 are approved with the conditions the planner wrote, which apply to C12, D6, D7, D8 and D11. C3 (en dash in the best-window range) is rejected: the range stays "12:00-15:00" as in MVP AC16 and MVP AC18. C12 says "You're offline." only when the device really has no connection. Final texts are in "Approved copy" |
| Disclaimer position (MVP AC26 against the pack layout) | "Yes, move it up" | MVP AC26 holds in every rated state at 411 x 891 dp at the default font. The disclaimer may sit higher on Today than the artboard shows (K1, AC25) |
| Factor grid content | "Accept the rule" | The factor grid is in scope, with the content rule in AC17 and D3. Value precision is set by the two rounding decisions below |
| About thresholds table and scope cuts | "Yes to both" | The About table uses exact boundary wording, and the existing threshold sentences stay on the screen (D11, K5, AC56). The GPS indicator, opening the app straight onto Recording, and a dark map style move to Later |
| Factor-value rounding. Question (verbatim): "Gate G1 needs your choice on how the Today card's factor values are rounded. Your mock-ups show whole numbers ("14°C", "0 mm/h", "9 mph"), but the planner chose 1 decimal place ("15.0°C", "26.3 mph") on its own. Which should the app show?" | "Whole numbers" | Air, Rain and Wind values in the factor grid show whole numbers, rounded half up, as in the artboards, for example "14°C", "0 mm/h", "9 mph" (AC17, AC19, D3). For negative temperatures, halves round away from zero (next row). Wind is converted to mph as the rating does. The displayed number is informational: the rating and reason come from the unrounded values, as in the MVP. The planner's 1-decimal choice (former assumption A9) is withdrawn. The owner accepts that a value near a threshold can display as the threshold value while the rating is on the other side of it (see "Dependencies and risks") |
| Negative-value rounding. Question (verbatim): "One small follow-up on whole-number rounding: how should negative temperatures that end in .5 round? The planner proposes rounding away from zero, so -2.5°C shows "-3°C", and a value that rounds to zero shows "0°C" with no minus sign." | "Away from zero" | In the Air cell, negative temperatures round halves away from zero: -2.5 °C shows "-3°C" and -0.5 °C shows "-1°C". A value that rounds to zero shows "0°C" with no minus sign, so -0.4 °C shows "0°C", never "-0°C". Rain and wind are never negative, so this affects only Air (AC17, AC19, D3). The former planner assumption A11 is replaced by this decision |
| System Back on the tabs. Question (verbatim): "Back button: on the Walks or Dog tab, should pressing the phone's Back button go to Today, and on Today leave the app? This is the usual Android bottom-navigation behaviour." | "Yes, standard" | On the Walks tab or the dog tab, system Back shows Today. On Today, system Back leaves the app (AC11). The former planner assumption A3 is replaced by this decision |
| Strip hour labels. Question (verbatim): "Hour labels on the 12-hour forecast strip: your mock-ups show 2-digit 24-hour labels ("09", "14"). Should they stay 24-hour even when the phone is set to 12-hour time?" | "Follow phone setting" | The strip's hour labels follow the device's time format, like the best-window text. In 24-hour mode they are 2-digit hours ("09", "14"), as in the artboards. In 12-hour mode their exact form is set by the owner's next answer ("2pm"; see the next row and AC23). The planner's former reading (always 24-hour, former assumption A4) is withdrawn |
| Strip hour-label form in 12-hour mode. Question (verbatim): "Last one before G1: when the phone uses 12-hour time, how should the forecast strip's hour labels look? The planner proposes "2 pm", "12 am" (lowercase, space, no :00), matching the app's best-time text "3:00 pm"." | `"2pm"` (the answer text, including its quotation marks) | In 12-hour mode each strip label is the hour with no leading zero and no minutes, followed directly by lowercase "am" or "pm" with no space: "2pm", "12am", "1am", "11pm" (AC23). The planner's proposed form with a space ("2 pm") is withdrawn. This deliberately differs from the best-window text, which keeps its MVP form with a space ("3:00 pm"; MVP AC16, MVP AC18) and is not changed (see "Dependencies and risks") |
| Long dog name in the nav bar. Question (verbatim): "A long dog name in the bottom navigation bar (the Dog tab label): may it wrap to two lines or be shortened with "…" on screen, as long as TalkBack always reads the full name?" | "Yes, shorten with …" | The dog-name nav label stays on one line. A name that does not fit is shortened on screen with "…" at the end. TalkBack always reads the full saved name (AC60). The former planner assumption A8 is replaced by this decision |

## Scope
**In scope**
1. **Foundations:**
   - The pack's colour tokens for light, dark, the always-dark recording palette and the three rating palettes.
   - The type scale with Bricolage Grotesque (display) and Figtree (body). The owner approved these fonts.
   - Spacing, radius and size tokens.
   - The icon set in `tokens.json` → `icons`.
2. **Dark mode** on every screen. It follows the system theme. There is no in-app setting.
3. **Navigation:**
   - A bottom navigation bar with three destinations: Today, Walks and the dog (pack §2). It replaces the home link list.
   - Recording, Walk detail, Privacy and About become full-screen destinations with a back control and no bottom nav.
4. **Today (the check screen):**
   - Parts: header, rating hero card with factor grid, best-window card with the 12-hour strip, cached/offline banner, footnote block, and a Start walk button pinned at the bottom.
   - States: Loading, Go, Caution, Avoid, data missing, cached, Can't check, Location needed (and its Settings variant), no profile, and precise location needed for walks (pack §4.1-§4.3, §5).
5. **Recording:**
   - An always-dark restyle with an h:mm:ss timer and Distance and Started stat cards.
   - A live route line drawn from the recorded fixes, with no map and no network.
   - The tip line, or the notifications-off line.
   - A restyled Stop button (pack §4.4).
6. **Walks (history):** a this-week summary card with a 7-day dot row, week sections, list rows, and empty and error states (pack §4.5).
7. **Walk detail:** an edge-to-edge map with a floating back button, a content sheet, an outlined destructive delete button, a bottom-sheet confirm, and offline, no-route and not-found states (pack §4.6).
8. **Dog profile:** photo row, photo errors, inputs with live counters, sensitivity radio cards, Save, and Privacy and About tiles (pack §4.7, §5).
9. **Privacy** and **About this advice** restyles, including the About thresholds table (pack §4.8, §4.9).
10. The approved copy changes in "Approved copy", and no others.

**Out of scope**
- Any change to the rating rules, best-window rules, cache policy, weather or map sources, data model, recording service behaviour, permissions model or privacy behaviour. The MVP spec's Scope, Rating rules, Data and privacy and Out of scope sections still apply unchanged.
- New features that the design does not need. This means no place name or geocoding (pack §4.1 says so), no in-app theme switch and no settings screen.
- The GPS signal indicator on Recording ("GPS good"). Moved to Later by owner decision.
- Opening the app straight onto Recording while a walk is being recorded. The app does not do this today. Moved to Later by owner decision (see K7).
- A dark map style for the walk-detail map tiles. Moved to Later by owner decision. Tiles keep their current style in both themes.
- The placeholder illustrations and sample data in the artboards are not shipped as content (pack README rule 6). This covers the map art, route sketch, paw avatar, "Biscuit", dates and distances.
- Screenshot-testing tools such as Paparazzi or Roborazzi. Visual fidelity is checked by hand (AC66).
- Languages other than English (UK).
- Copy item C3 (rejected).

## Acceptance criteria
Conventions:
- **Numbering:** this spec's acceptance criteria are AC1-AC67. A bare "ACn" always means a criterion of this spec. "MVP ACn" means criterion ACn of `docs/specs/20261019-dog-walk-buddy-mvp.md`, and a range written "MVP ACa-ACb" means MVP criteria a to b. AC67 was added at G1 fix attempt 1 and is listed with the Dog profile criteria.
- **Test types:**
  - `unit` is a JVM test.
  - `integration` is a Robolectric Compose or Room test, run by `./gradlew --no-daemon testDebugUnitTest`.
  - `manual` is checked by a person on the emulator or the reference phone. Each manual check says how to do it.
- **Viewport:** "default viewport" means `w411dp-h891dp-xxhdpi` at font scale 1.0. The MVP tests already use this qualifier for "without scrolling". "200 %" means font scale 2.0.
- **Dark** means system night mode is on: the `night` qualifier in Robolectric, or the system dark theme on a device.
- **AC-fixed texts** must appear exactly as the MVP criteria give them:
  - the reason texts, and the rating words Go / Caution / Avoid
  - "Now until …", "Best available: Caution", "No good time in the next 12 hours" and "Checked … ago"
  - the disclaimer, "About this advice", "Weather data: MET Norway (CC BY 4.0)" and "Pollen isn't included in this check"
  - "Using normal heat sensitivity. Set up your dog's profile to change this."
  - "No walks yet", "Can't check right now" and "Map not available offline"
  - "© OpenStreetMap contributors" and "© OpenMapTiles"
  - "Delete all my data"
  - the best-window range written with a hyphen, as in "12:00-15:00".
- **MVP ACs** column: the MVP criteria that each AC touches. Every touched MVP criterion stays fully in force. The AC adds to it and never relaxes it.
- **Test examples:** examples use the existing test clock, `2026-10-19T14:20:00Z` (UTC, 24-hour), unless a row says otherwise.

### Foundations and dark mode

| ID | Given | When | Then | Test type | MVP ACs |
|---|---|---|---|---|---|
| AC1 | The app theme in light mode | The theme's colour roles are read | Each of these roles has exactly the value of its token in `tokens.json` → `color.light`: background, surface, surfaceMuted, primaryContainer, navIndicator, onSurface, onSurfaceVariant, textTertiary, iconMuted, outline, outlineVariant, divider, primary, onPrimary, primaryPressed, error, onError, errorText, errorOutline and scrim | unit | MVP AC51 |
| AC2 | The app theme in dark mode | The theme's colour roles are read | Every role in `color.dark` has exactly its token's value, for the same roles as AC1 | unit | MVP AC51 |
| AC3 | The rating palettes | They are read in light and in dark | For Go, Caution and Avoid, these roles equal `color.rating` in light and `color.dark.rating` in dark: container, onContainer, onContainerMuted, badge, onBadge, divider and bar, plus driverHighlight for Caution and Avoid | unit | MVP AC51, MVP AC53 |
| AC4 | The recording palette | The Recording screen is shown in light mode and in dark mode | Both modes use `color.recording`: background #0B1622, card #1A2A3D, map panel #132233 with border #22364D, route #8CC2FF with casing #2D4A6B, live dot #FF6B57, and Stop #F2F5F9 with text #0B1622. `manual`: open Recording with the system theme set to light and then dark, and confirm the two look identical | integration + manual | MVP AC51 |
| AC5 | The type scale | The theme typography is read | Display styles (ratingWord, timer, screenTitle, checkQuestion, statLarge, bestWindow) use Bricolage Grotesque. Body styles (reason, factorValue, button, body, label, caption, navLabel, axisLabel) use Figtree. Sizes and weights match `tokens.json` → `type.scale`. The font files ship inside the app, and no font is fetched over the network | unit | MVP AC49 |
| AC6 | Every foreground/background colour pair used for text, in the light, dark and recording palettes | The existing `ContrastTest`, extended to these pairs, runs | Every text pair is at least 4.5:1. On each rating container, in light and dark, the rating word and the reason are at least 7:1. The Start walk label on `primary` (light and dark) and the Stop label on the Stop colour are at least 7:1. The rating icon on its badge is at least 3:1 | unit | MVP AC51 |
| AC7 | The app is open on any screen | The system theme changes between light and dark | The screen redraws in the other theme without a restart or data loss. There is no in-app theme setting. `manual`: switch the system dark theme on and off on each of the seven main screens | integration + manual | none |
| AC8 | Fonts are bundled in the app | The About screen is opened | It shows the name and full licence text of each bundled font, alongside the existing licence sections. The existing sections are unchanged | integration | MVP AC26 |

### Navigation

| ID | Given | When | Then | Test type | MVP ACs |
|---|---|---|---|---|---|
| AC9 | The app is launched | The first screen shows | It is Today, with a bottom navigation bar of exactly three items in this order: "Today", "Walks" and the dog's saved name. The third item reads "Dog" when no profile exists (C16). Each item has an icon and a visible text label and is at least 48 x 48 dp (the pack shows 80 x 56). The current item is exposed to accessibility services as selected | integration | MVP AC28, MVP AC50, MVP AC51 |
| AC10 | Any tab is shown | The user taps another nav item | That destination shows and its item becomes selected. The home link list ("Walk history", "Dog profile", "Privacy" and "About this advice" buttons) no longer exists | integration | none |
| AC11 | (a) The Walks tab is shown; (b) the dog tab is shown; (c) Today is shown | The user presses system Back | In (a) and (b), Today shows and its nav item becomes selected. In (c), the app is left: its activity finishes and no app screen remains shown (owner decision "Yes, standard") | integration | none |
| AC12 | Recording, Walk detail, Privacy or About is shown | The screen is inspected and its back control is tapped | There is no bottom nav. The back control has the spoken label "Back" and is at least 48 x 48 dp. Tapping it returns to the screen it was opened from | integration | MVP AC50, MVP AC51 |
| AC13 | The app is on Today | The user taps the dog tab, then "Privacy & data" (D9) | Privacy shows, 2 taps from the first screen | integration | MVP AC46 |
| AC14 | The app is on Today | The user taps "About this advice" in the footnote, or taps the dog tab and then "About the advice" (D9) | About this advice shows. From the check it is 1 tap away | integration | MVP AC26, MVP AC27 |
| AC15 | No profile exists | The user saves a profile named "Biscuit", then later uses "Delete all my data" | The third nav label reads "Biscuit" after saving and "Dog" after the deletion | integration | MVP AC41, MVP AC47 |
| AC16 | A walk is being recorded and the user has left Recording | Today is shown | The Start control reads "Walk in progress" and opens Recording when tapped. This is unchanged MVP behaviour | integration | MVP AC28 |

### Today: rated states

| ID | Given | When | Then | Test type | MVP ACs |
|---|---|---|---|---|---|
| AC17 | A rated result | Today shows | The hero card shows a factor grid of four cells in this order, with these captions: "Air", "Rain", "Wind", "Pavement". Each value is for the current (rated) hour. **Air** is the temperature as "{T}°C". **Rain** is the precipitation rate as "{P} mm/h". **Wind** is the mean wind speed as "{M} mph". T, P and M are each shown as a whole number, rounded half up (owner decision "Whole numbers"), for example "14°C", "0 mm/h", "9 mph". So 14.5 °C shows "15°C", 0.4 mm/h shows "0 mm/h" and 24.6 mph shows "25 mph". Negative temperatures round halves away from zero (owner decision "Away from zero"): -2.5 °C shows "-3°C", -2.4 °C shows "-2°C" and -0.5 °C shows "-1°C". A temperature that rounds to zero shows "0°C" with no minus sign: -0.4 °C shows "0°C", never "-0°C". **Pavement** reads "Not rated" when the pavement rule does not apply: at night, or when cloud cover is 50 % or more, or when cloud cover is missing. When the rule does apply, it shows the pavement factor's own rating as "Cool" (Go), "Warm" (Caution) or "Hot" (Avoid). If a cell's input value is missing, the cell reads "No data". Exactly one cell is highlighted when the reason names a factor: a heat or freezing reason highlights Air, rain highlights Rain, wind highlights Wind, and pavement highlights Pavement. No cell is highlighted for "Good conditions for a walk" or "Some weather data isn't available". The highlight is a ring as well as a fill, not colour alone. Example: Normal, daylight, cloud 20 %, T = 21 °C, P = 1 mm/h, M = 26 mph (MVP AC12 inputs) shows "21°C", "1 mm/h", "26 mph" and "Warm", with Pavement highlighted. Negative example: night, T = -2.5 °C, P = 0.0 mm/h, M = 5 mph shows "-3°C", "0 mm/h", "5 mph" and "Not rated", with Air highlighted for the freezing reason; with T = -0.4 °C instead, Air shows "0°C" | integration | MVP AC1-AC14, MVP AC53 |
| AC18 | A rated result with rating Go, Caution or Avoid | Today shows, in light and in dark | The hero card uses that rating's container colour. It shows a badge with that rating's icon (Go: a check in a circle; Caution: a triangle with an exclamation mark; Avoid: an octagon with a cross), the rating word and the exact reason text. The three icons are different drawables | integration | MVP AC1-AC14, MVP AC51, MVP AC53 |
| AC19 | A rated result | TalkBack focuses the hero card | The card is one accessibility node. Its spoken text starts with "<Rating>. <Reason>." exactly as in the MVP (for example "Caution. Windy."). That is followed by the four factor cells as "<caption> <value>", in grid order and separated by ", ". The values are the same whole numbers the grid shows (AC17). Example, for T = 15.0 °C, P = 0.0 mm/h, M = 26.3 mph: "Caution. Windy. Air 15°C, Rain 0 mm/h, Wind 26 mph, Pavement Not rated" | integration | MVP AC50 |
| AC20 | A rated result | Today shows | The header shows the check title, a meta line and a refresh icon button. The title is "Good time to walk {name}?" with the saved dog name (for example "Good time to walk Biscuit?"), or "Good time to walk?" with no profile (C1). For a fresh result under 1 minute old, the meta line reads "Your area · checked just now" (D1). Otherwise it shows a clock icon and the exact `check_age_*` text, for example "Checked 12 min ago" or "Checked 2 h 59 min ago". The refresh button's spoken label is "Refresh". It is at least 48 x 48 dp, and tapping it runs a check | integration | MVP AC21, MVP AC24, MVP AC50, MVP AC51 |
| AC21 | A cached result under 3 h old is shown because a fresh check was not possible | Today shows (a) with the device reporting no network connection, and (b) with a connection but a failed weather request or no location fix | An info banner shows above the hero card (C12). In (a) it reads "You're offline." in bold, then "Showing your last check. It's kept for up to 3 hours." In (b) it reads only "Showing your last check. It's kept for up to 3 hours." With a fresh result there is no banner | integration | MVP AC21 |
| AC22 | A rated result with a best window | Today shows | The best-window card shows the heading "Best window, next 12 hours" (C2). The window text appears exactly as the MVP formatter produces it, for example "Now until 15:00", "12:00-15:00" (a hyphen with no spaces; C3 rejected) or "No good time in the next 12 hours". When the best rating is Caution, the card shows the "Best available: Caution" label. A chip shows the window's rating word, except with "No good time in the next 12 hours", where there is no chip | integration | MVP AC16-AC19 |
| AC23 | A rated result with 12 hourly ratings, at the test clock `2026-10-19T14:20:00Z` with the device time zone UTC, and the device time format set (a) to 24-hour and (b) to 12-hour | The 12-hour strip is drawn | There are 12 bars in hour order, starting with the current hour. Bar height is 44 dp for Go, 28 dp for Caution and 14 dp for Avoid, in that rating's bar colour. A marker line sits above exactly the bars in the best window, and there is none when there is no good time. Each column has a label for the start of its hour, in the device's time zone and time format (owner decision "Follow phone setting"). (a) 24-hour: the label is the 2-digit hour, as in the artboards. The 12 labels read "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "00", "01". (b) 12-hour: the label is the hour with no leading zero and no minutes, followed directly by lowercase "am" or "pm" with no space (owner decision `"2pm"`). Midnight is "12am" and noon is "12pm". The 12 labels read "2pm", "3pm", "4pm", "5pm", "6pm", "7pm", "8pm", "9pm", "10pm", "11pm", "12am", "1am". These labels deliberately differ from the best-window text, which keeps its MVP form with a space ("3:00 pm") and is not changed by this criterion (MVP AC16, MVP AC18). In both modes, every label is shown in full, with no truncation or overlap with a neighbouring label, at the default viewport. The strip, bars and labels, is hidden from accessibility services, because the window text carries the meaning | integration | MVP AC16-AC19, MVP AC50, MVP AC53 |
| AC24 | A rated result | Today shows | The footnote shows the exact disclaimer, an "About this advice" link with a touch target of at least 48 x 48 dp, the exact "Pollen isn't included in this check" and the exact "Weather data: MET Norway (CC BY 4.0)". With no profile, it shows the exact "Using normal heat sensitivity. Set up your dog's profile to change this.", and tapping that opens the dog tab. With a Sensitive profile, it shows "Heat-sensitive thresholds ({name})" as its own line (D2), for example "Heat-sensitive thresholds (Biscuit)" | integration | MVP AC20, MVP AC26, MVP AC27, MVP AC45 |
| AC25 | Four cases, each at the default viewport and font: Go; Caution with the offline banner, an age label, the Sensitive line and the factor grid; Avoid; and no profile | Today shows, without scrolling | The disclaimer text and the "About this advice" link are fully displayed in all four cases. The disclaimer may sit higher on the screen than the artboard shows (owner decision) | integration | MVP AC26 |
| AC26 | Any Today state with a Start control | Today shows | Start walk is full width inside a 16 dp gutter and at least 72 dp high, with a `primary` fill and `onPrimary` text. Its label is "Start walk" with a play icon, and it sits directly above the bottom nav. Tapping it once starts recording within 1 s, exactly as in the MVP | integration | MVP AC28, MVP AC51 |
| AC27 | Font scale 200 % | Today shows each rated state | The rating, the reason, the best-window text and Start walk are fully readable without truncation or overlap. The content above Start scrolls. Start walk and the bottom nav stay visible without scrolling. `manual`: set the phone's font and display size to maximum and check each element | integration + manual | MVP AC52 |
| AC28 | The check is running and there is nothing to show yet | Today shows | A placeholder card in `surfaceMuted` is shown, and the exact "Checking the weather" text is available to TalkBack. No rating word, rating icon, reason or bars are shown | integration | MVP AC15, MVP AC50 |

### Today: no-result states

| ID | Given | When | Then | Test type | MVP ACs |
|---|---|---|---|---|---|
| AC29 | The check cannot produce a result (the MVP AC15 or MVP AC22 conditions) | Today shows | A card shows the exact "Can't check right now" and the line "We couldn't get the weather, and there's no recent check to fall back on. Check your connection and try again." (D4). Below them is a "Try again" control (C4), at least 48 dp high, that runs a check when tapped. A panel reads "**You can still record a walk.** Recording works without a connection and saves to your phone." (C13). The exact disclaimer, the attribution and the "About this advice" link are shown. There is no refresh button, age, rating word or icon, reason, best window or bars. Start walk and the nav show as usual | integration | MVP AC15, MVP AC22, MVP AC26, MVP AC27, MVP AC51, MVP AC53 |
| AC30 | Approximate location has not been granted | Today shows | The screen shows a decorative illustration, hidden from accessibility. Then the heading "The check needs your approximate location" and the body "So we can get the weather where you are. Your approximate location is enough for this." (D5). Then three bullets (D5): "Rounded to about 1 km before it leaves your phone", "Only sent when you open the check or tap refresh" and "Sent only to the weather service, MET Norway". Then an "Allow approximate location" primary button, at least 48 dp high (the pack shows 60). Then a "How your data is used" link (D5) that opens Privacy. No weather request is made. The attribution is still shown | integration | MVP AC25, MVP AC27, MVP AC46, MVP AC51 |
| AC31 | Approximate location was refused and the system will not ask again | Today shows | Same as AC30, but the button reads "Open settings" and opens the app's system settings page | integration | MVP AC25 |
| AC32 | Precise location has not been granted | The user taps Start walk | No walk starts. An explanation in the same pattern as AC30 shows the exact "Recording a walk needs precise location." with an "Allow precise location" control, or "Open settings" if permission was permanently refused. Each control is at least 48 dp high. The illustration is decorative | integration | MVP AC33, MVP AC50, MVP AC51 |

### Recording

| ID | Given | When | Then | Test type | MVP ACs |
|---|---|---|---|---|---|
| AC33 | A walk is being recorded | Recording shows, in light and in dark | It uses the recording palette (AC4), has no bottom nav and keeps a back control (K6). A status pill shows a live dot and "Recording walk" (C5). The notification title stays exactly "Recording your walk" | integration | MVP AC28, MVP AC50 |
| AC34 | A walk started 23 min 41 s ago | Recording shows | The label reads "Elapsed time", unchanged (D6). The timer reads "0:23:41" (h:mm:ss) and updates at least once a second. Its spoken text is the duration in words (D6): "23 minutes 41 seconds". For 1 h 2 min 5 s it is "1 hour 2 minutes 5 seconds". For 1 h 0 min 5 s it is "1 hour 5 seconds". Units whose value is 0 are left out, except that a zero duration is "0 seconds". A value of 1 uses the singular. The Distance card shows "{x.x} mi". A "Started" card (C15) shows the start time in the device's 12-hour or 24-hour format | integration | MVP AC28, MVP AC50 |
| AC35 | A walk is being recorded with fixes A, B and C | Recording shows | The route panel draws the fixes as one line scaled to fit the panel, with a start marker at A and a current marker at C. It redraws when a new fix arrives, at least as often as the distance updates. With fewer than two fixes it shows the empty panel and no error. Its spoken label is "Route so far", and its caption is "Route so far · saved on this phone" (C18). No network request is made while Recording is shown | integration | MVP AC28, MVP AC31, MVP AC49, MVP AC50 |
| AC36 | A walk is being recorded | Notifications are on, then off | With notifications on, the tip reads "Pop your phone in your pocket. Recording carries on with the screen off, and a notification shows while it's on." (D6). With notifications off, the exact "Notifications are off. Your walk is still being recorded." shows instead | integration | MVP AC28 |
| AC37 | A walk is being recorded | Recording shows and the user taps Stop | Stop is full width and at least 72 dp high (the pack shows 76), with a stop icon and the label "Stop and save walk" (C6). While the walk saves, Stop is disabled and reads exactly "Saving your walk". The walk is then saved as in the MVP | integration | MVP AC30, MVP AC51 |
| AC38 | Font scale 200 % | Recording shows | The timer, the distance, the Started time and Stop are fully readable without truncation or overlap. Wrapping and scrolling are allowed. `manual`: check on the phone at maximum font and display size | integration + manual | MVP AC52 |

### Walks (history)
Date rules for this section and for Walk detail (D7, D8):
- A date or week range in the current calendar year has no year.
- A date or week range in an earlier year shows the year.
- A week that spans two years always shows both years.
- "Current year" is the year of the device clock's local date.

| ID | Given | When | Then | Test type | MVP ACs |
|---|---|---|---|---|---|
| AC39 | The MVP AC36 walks: Mon 12 Oct 2026 08:00 (30 min, 1.0 mi), Sun 18 Oct 2026 23:30 (20 min, 0.5 mi) and Mon 19 Oct 2026 00:10 (10 min, 0.3 mi), with the clock in 2026 | The Walks tab shows | The title "Walks" (C7) is a heading, and there is no back control. Week sections are listed newest first. The newest section is headed "Mon 19 – Sun 25 Oct" with totals "1 walk · 0:10 · 0.3 mi". The older section is headed "Mon 12 – Sun 18 Oct" with totals "2 walks · 0:50 · 1.5 mi" (D7). Each row shows the date, the start time with "at", the distance and the duration, for example "Mon 19 Oct", "at 00:10", "0.3 mi" and "0:10". Each row is at least 64 dp high and is one clickable target that opens that walk's detail. Its spoken text is "<date> at <time>, <h:mm>, <x.x> mi", for example "Mon 19 Oct at 00:10, 0:10, 0.3 mi". Week ranges across months read like "Mon 26 Oct – Sun 1 Nov". With the clock in 2027, the same walks read "Mon 12 – Sun 18 Oct 2026" and "Mon 19 Oct 2026". A week spanning two years reads like "Mon 28 Dec 2026 – Sun 3 Jan 2027". The counts, h:mm and miles are the MVP values | integration | MVP AC35, MVP AC36, MVP AC50, MVP AC51 |
| AC40 | No walk has been recorded in the current week and the clock is Mon 19 Oct 2026 | The Walks tab shows, in light and in dark | The this-week summary card shows "This week" and "Mon 19 – Sun 25 Oct". It has three stats with captions: "0" "walks", "0:00" "h:mm" and "0.0" "miles". The caption reads "walk" when the count is 1. The card's spoken text is "This week: 0 walks, 0 minutes, 0.0 miles" (D7). With 3 walks totalling 1:25 and 4.2 mi, it is "This week: 3 walks, 1 hour 25 minutes, 4.2 miles". Spoken durations follow the AC34 unit rules, and a zero duration is "0 minutes". Every text on the card, including the captions and the week range, has at least 4.5:1 contrast against the card fill in light and in dark (7:1 wherever MVP AC51 requires it), and is covered by the AC6 contrast test. The pack's light-blue caption colour #CFE0F5 is not used on the dark `primary` fill #8CC2FF (about 1.4:1). The architect chooses the caption colour from the token roles | integration | MVP AC37, MVP AC50, MVP AC51 |
| AC41 | The current week has walks on Monday and Wednesday, and today is Thursday | The summary card shows | It has 7 day dots, Monday to Sunday. Monday and Wednesday show as "walked" and Tuesday as "past, no walk". Thursday is marked as today, and Friday to Sunday show as "future". The dots are hidden from accessibility services | integration | none |
| AC42 | No walks exist | The Walks tab shows | The exact "No walks yet" shows with a decorative muted icon | integration | MVP AC35, MVP AC47 |
| AC43 | Walks cannot be loaded | The Walks tab shows | The exact "Couldn't load your walks." shows | integration | none |
| AC44 | A walk row in Walks | The user opens the row's "More" menu and deletes the walk, or cancels | Row delete works as in the MVP, with the same confirm and the same result. Cancelling changes nothing. The delete control's spoken label and the confirm message keep their current date format, with the year: "Delete walk on Mon 19 Oct 2026 at 08:00" and the exact `history_delete_message` | integration | MVP AC40 |

### Walk detail

| ID | Given | When | Then | Test type | MVP ACs |
|---|---|---|---|---|---|
| AC45 | A saved walk with a route, online | Walk detail opens | The map fills the top of the screen edge to edge, with the route fitted. There is a floating back button, at least 48 x 48 dp, spoken "Back" (D8). The map attribution shows readable "© OpenStreetMap contributors" and "© OpenMapTiles". `manual`: confirm the route is drawn in `primary` with a light casing and a start marker, the whole route is visible without panning, and the attribution is readable in light and dark | integration + manual | MVP AC38, MVP AC50, MVP AC51 |
| AC46 | A saved walk that started Mon 19 Oct 2026 09:15 and lasted 1:15 for 2.0 mi, with the clock in 2026 and a 24-hour device format | Walk detail opens | A sheet below the map shows the full date as a heading, "Monday 19 October" (D8), or "Monday 19 October 2026" when the clock is in another year. Below it is the time range "09:15 – 10:30" (C14), in the device's 12-hour or 24-hour format. Then a Duration card and a Distance card (D8). Each card is one accessibility node containing its caption and value: "Duration" with "1:15", and "Distance" with "2.0 mi". Then an outlined destructive "Delete this walk" button (D8), at least 48 dp high. There is no visible "Walk details" title | integration | MVP AC39, MVP AC51 |
| AC47 | A saved walk, offline, with no tiles | Walk detail opens | The map area shows a map-off icon, the exact "Map not available offline" and "Your route is saved. It'll show here next time you're online." (C17). The date, duration and distance still show | integration | MVP AC39 |
| AC48 | A saved walk | The user taps "Delete this walk", then "Delete walk" in the bottom sheet | The sheet has the title "Delete this walk?", the exact `history_delete_message` body, and "Delete walk" and "Cancel" buttons, each at least 48 dp high (the pack shows 56). Confirming removes the walk and its route, returns to Walks and updates the totals. Cancel, system Back or a tap on the scrim closes the sheet and changes nothing | integration | MVP AC40, MVP AC51 |
| AC49 | A walk with no route, a deleted walk id, or a load failure | Walk detail opens | The exact existing texts show: "No route was recorded for this walk", "This walk can't be found. It may have been deleted." and "Couldn't load this walk." | integration | MVP AC39 |

### Dog profile

| ID | Given | When | Then | Test type | MVP ACs |
|---|---|---|---|---|---|
| AC50 | The dog tab | It shows with a photo and without one | The title "Your dog" (C8) is a heading, and there is no back control. With a photo, it shows an 88 dp round photo, "Change photo" (C10) and "Remove photo". With no photo, it shows a placeholder and "Choose photo", and no remove control. Every photo control is at least 48 x 48 dp; the pack shows 44 (see K4) | integration | MVP AC41, MVP AC43, MVP AC51 |
| AC51 | The profile form | The user types a 7-character name and an 8-character breed | Live counters read "7 / 30" and "8 / 50" (D9) and update on every change. "(optional)" shows muted next to Breed. Saving is refused with an empty name, a 31-character name or a 51-character breed. The exact existing error text shows below the field it names | integration | MVP AC42 |
| AC52 | The profile form | The heat-sensitivity options show | There are two options. "Normal" shows "Caution from 20°C / Avoid from 25°C", and "Sensitive" shows "Caution from 16°C / Avoid from 21°C" (D9). They form a radio group. Each option is announced with its name, its bands and whether it is selected, including Normal, which fixes MVP finding F1. The selected option is marked by a filled radio dot and a border, not by colour alone. Normal is selected by default for a new profile. The hint reads "Sensitive makes the heat and hot-pavement warnings start 4°C sooner. Rain, wind and cold stay the same." (C11). `manual`: covered by the AC59 TalkBack walk-through | integration + manual | MVP AC41, MVP AC44, MVP AC50, MVP AC53 |
| AC53 | The profile form | The user taps "Save" (C9) | The profile is saved as in the MVP, and the existing "Profile saved" feedback shows. Below Save, a "Privacy & data" tile opens Privacy and an "About the advice" tile opens About (D9) | integration | MVP AC41, MVP AC46 |
| AC67 | The dog tab, in light and in dark | The user picks (a) a photo that is too large, (b) a file that is not a usable image, or (c) a photo that cannot be read | The matching existing text shows, word for word and with the same values as the current app: (a) `profile_photo_too_large` ("That photo is too large. Choose one under %1$s MB and %2$s pixels on each side."), (b) `profile_photo_not_an_image` ("That file isn't a photo the app can use. Choose a different one.") or (c) `profile_photo_unreadable` ("Couldn't read that photo. Try another one."). The message is styled per the pack's error text (`errorText`, as for the profile validation errors in pack §4.7) and has at least 4.5:1 contrast against its background (AC6). These texts are not changed | integration | MVP AC41, MVP AC43, MVP AC51 |

### Privacy and About

| ID | Given | When | Then | Test type | MVP ACs |
|---|---|---|---|---|---|
| AC54 | Privacy is opened | It shows | It has a back control, the title "Privacy & your data" and the headline "Your walks stay on your phone" (D10). The existing `privacy_weather`, `privacy_map` (including "served through its CDN provider, Cloudflare"), `privacy_local` and `privacy_no_tracking` sentences show word for word. They are laid out in cards with the headings "Never leaves this phone" and "Only sent when you use a feature". The entry headings are "MET Norway · when the check runs" and "OpenFreeMap · when you view a walk map" (D10). At the bottom is an outlined destructive "Delete all my data" button, at least 48 dp high, with the existing explanation | integration | MVP AC46, MVP AC48, MVP AC51 |
| AC55 | Privacy is shown | The user taps "Delete all my data" and confirms, or cancels | A bottom sheet shows the exact `privacy_delete_title`, `privacy_delete_message`, "Delete everything" and "Cancel". Confirming deletes everything as in the MVP and shows the existing deleting, deleted and failed texts. Cancelling changes nothing | integration | MVP AC47 |
| AC56 | About is opened | It shows | It has a back control and the title "About this advice". The headline is "How we rate the weather", and the intro is "The overall rating is the worst of these. Heat-sensitive dogs use the values in brackets." (D11). The thresholds table has the column headers Factor, Go, Caution and Avoid, with each rating header showing its icon. Its rows hold exactly the cells in D11, in the order Heat, Pavement, Rain, Wind, Cold. TalkBack reads it as a table with row and column headers. Every existing threshold sentence (`about_overall`, `about_heat`, `about_pavement`, `about_rain`, `about_wind`, `about_cold`) is still on the screen, word for word. `manual`: TalkBack table reading is checked in AC59 | integration + manual | MVP AC26, MVP AC50 |
| AC57 | About is shown | The user scrolls | The sources heading reads "Where this comes from" (D11). The existing guidance-source sentences (Vets Now, FOUR PAWS UK, the Beaufort scale), the disclaimer, "Weather data: MET Norway (CC BY 4.0)", the map attribution and every existing licence section are present and unchanged | integration | MVP AC26, MVP AC27 |

### Cross-cutting

| ID | Given | When | Then | Test type | MVP ACs |
|---|---|---|---|---|---|
| AC58 | Each screen and state in this spec, in light and dark | The existing `AccessibilityAssertions`, extended to every screen and state listed here, run | Every interactive control is at least 48 x 48 dp and has a spoken label. Start walk and Stop are at least 72 dp high and full width. Headings are marked as headings. Decorative images (illustrations, dots, strip) are hidden | integration | MVP AC50, MVP AC51 |
| AC59 | TalkBack is on, on a real phone | The user moves through Today (Go, Caution, Avoid, Can't check, Location needed), Recording, Walks, Walk detail and its delete sheet, Dog, Privacy and its delete sheet, and About, in light and in dark | Every control is spoken with a label. The check is announced as rating, then reason. The best window is announced as text. The selected nav item and the selected sensitivity option (Normal and Sensitive) are announced as selected. The About table is read with its row and column headers. `manual`: record the walk-through result for each screen | manual | MVP AC50 |
| AC60 | (a) Font scale 200 %; (b) a saved dog name of 30 characters, at font scale 1.0 and at 2.0; (c) the saved name "Biscuit" at font scale 1.0 | Walks, Walk detail, Dog, Privacy and About show, and the bottom nav is inspected | (a) All text on those screens is readable without truncation or overlap, and the screens scroll. The "Today" and "Walks" nav labels, and "Dog" with no profile, are never shortened; they may wrap. (b) The dog-name nav label stays on one line. When the name does not fit, it is shortened on screen with "…" at the end (owner decision "Yes, shorten with …"). (c) A name that fits shows in full with no "…". In every case the nav item's spoken label contains the full saved name. `manual`: check each screen and the nav with a 30-character name on the phone at maximum font and display size, and confirm with TalkBack that the full name is read | integration + manual | MVP AC52 |
| AC61 | Any check result | The screen is viewed in greyscale and the 12-hour strip is inspected | Go, Caution and Avoid differ by word, icon and bar height. `manual`: turn on the phone's greyscale (colour correction) setting and confirm each rating can be told apart on Today and in the About table header | integration + manual | MVP AC53 |
| AC62 | The string resources | A test reads every AC-fixed text | Each AC-fixed text is present exactly as in the MVP, including the hyphen range format `%1$s-%2$s` (C3 rejected). The MVP tests that assert those texts still pass, and none of their matchers is loosened | unit + integration | MVP AC15-AC22, MVP AC26, MVP AC27, MVP AC35, MVP AC38, MVP AC39, MVP AC45, MVP AC47 |
| AC63 | The approved copy list | The string resources and screens are inspected | Each item in "Approved copy" appears exactly as listed there. Every string not on that list keeps its current text. Each existing test assertion in "Test assertions that change" asserts the new exact text with the same values. These are owner-approved text changes, not weakening: no such assertion is removed, turned into a substring, regex or existence check, or moved to a looser matcher | unit + integration | MVP AC35-AC37, MVP AC39 |
| AC64 | The full MVP test suite | `./gradlew --no-daemon testDebugUnitTest` runs on the redesigned app | Every test for MVP AC1-AC53 passes. A test may change only to follow a moved control (for example, a tap on a nav item instead of a home link) or an approved copy change in "Test assertions that change". The changed test must assert the same behaviour at least as strictly | integration | MVP AC1-AC53 |
| AC65 | The redesigned app | It is used across all features through an intercepting proxy | The only hosts contacted are still the MET Norway API host and the OpenFreeMap hosts. Fonts, icons and the recording route line add no requests. `manual`: as MVP AC49 | manual | MVP AC49 |
| AC66 | The artboards in `screens/*.dc.html` (light) and `MainDark.dc.html` (dark) | Each redesigned screen and state is compared with its artboard on the emulator, in light and dark | Layout, colours, type, radii and spacing match the artboard, except where an MVP criterion, this spec, a rejected copy item or a Later item requires a difference. Each difference is listed. Dark screens apply the swap from the AC1 roles to the AC2 roles. `manual`: record screenshots of every screen in both themes with the run's device checks | manual | none |

## Approved copy
The owner approved every item below on 2026-10-22 ("Approve all but C3"). This list replaces the current string or adds a new one. Any text not on this list does not change, and AC-fixed texts (see Conventions) do not change.

**C. Pack §6 "Copy changes" suggestions**

| # | Where | Current | Approved text | Notes |
|---|---|---|---|---|
| C1 | Check title | Good time to walk? | Good time to walk {name}? | Stays "Good time to walk?" with no profile |
| C2 | Window heading | Best walking window | Best window, next 12 hours | |
| C3 | Window range | %1$s-%2$s | **Rejected.** Stays %1$s-%2$s ("12:00-15:00") | MVP AC16 and MVP AC18 |
| C4 | Retry | Retry | Try again | MVP AC15 and MVP AC22 require a retry control, not this word |
| C5 | Recording title (status pill) | Recording your walk | Recording walk | The notification title stays "Recording your walk" |
| C6 | Stop | Stop | Stop and save walk | The saving state stays "Saving your walk" |
| C7 | History title and nav label | Walk history | Walks | |
| C8 | Profile title | Dog profile | Your dog | |
| C9 | Profile save | Save profile | Save | |
| C10 | Photo button | Replace photo | Change photo | |
| C11 | Sensitivity hint | Sensitive uses stricter heat and pavement limits. | Sensitive makes the heat and hot-pavement warnings start 4°C sooner. Rain, wind and cold stay the same. | |
| C12 | New: cached banner | (none) | Offline: "**You're offline.** Showing your last check. It's kept for up to 3 hours." Online but no fresh result: "Showing your last check. It's kept for up to 3 hours." | "You're offline." shows only when the device reports no network connection (AC21) |
| C13 | New: Can't check panel | (none) | **You can still record a walk.** Recording works without a connection and saves to your phone. | |
| C14 | New: walk detail time range | (none) | {start} – {end}, for example "07:42 – 08:06" | Spaced en dash; device 12-hour or 24-hour format. C3's rejection applies only to the best-window range |
| C15 | New: Recording stat label | (none) | Started | |
| C16 | New: nav labels | (none) | Today / Walks / {name}, or "Dog" with no profile | |
| C17 | New: map-offline sub-line | (none) | Your route is saved. It'll show here next time you're online. | |
| C18 | New: route caption | (none) | Route so far · saved on this phone | Spoken label "Route so far" |

**D. Further text from the pack's screens (approved with the conditions shown)**

| # | Where | Approved text | Condition / note |
|---|---|---|---|
| D1 | Today meta line, fresh result under 1 minute old | Your area · checked just now | Older or cached results show the exact `check_age_*` text |
| D2 | Footnote, Sensitive dog | Heat-sensitive thresholds ({name}) | Its own line. "Pollen isn't included in this check" stays in full (K2) |
| D3 | Factor grid | Captions Air, Rain, Wind, Pavement. Values "{T}°C", "{P} mm/h", "{M} mph" as whole numbers, for example "14°C", "0 mm/h", "9 mph", "-3°C". Pavement "Not rated" / "Cool" / "Warm" / "Hot". Missing value "No data" | The rule is in AC17 ("Accept the rule"). Whole numbers, rounded half up, per the owner's "Whole numbers". Negative temperatures round halves away from zero, and a value that rounds to zero shows "0°C" with no minus sign, per the owner's "Away from zero" |
| D4 | Can't check explanation | We couldn't get the weather, and there's no recent check to fall back on. Check your connection and try again. | |
| D5 | Location needed | Heading "The check needs your approximate location" (the current `check_needs_location` without its full stop). Body "So we can get the weather where you are. Your approximate location is enough for this." Bullets "Rounded to about 1 km before it leaves your phone", "Only sent when you open the check or tap refresh", "Sent only to the weather service, MET Norway". Link "How your data is used" | |
| D6 | Recording | Spoken timer, for example "23 minutes 41 seconds" or "1 hour 2 minutes 5 seconds". Tip "Pop your phone in your pocket. Recording carries on with the screen off, and a notification shows while it's on." | Condition: the label stays "Elapsed time". The pack's "Time" is not used |
| D7 | Walks | Week range "Mon 19 – Sun 25 Oct". Week totals "5 walks · 3:10 · 9.6 mi". Summary captions "walks" (or "walk" for 1), "h:mm" and "miles". Summary spoken "This week: 3 walks, 1 hour 25 minutes, 4.2 miles". Row "Wed 21 Oct" + "at 07:42" | Condition: ranges and dates outside the current year keep the year (date rules above AC39) |
| D8 | Walk detail | Full date heading "Wednesday 21 October". Card captions "Duration" and "Distance" with values "{h:mm}" and "{x.x} mi". Button "Delete this walk" | Conditions: the year is kept when the walk is not in the current year. The back label stays "Back" (existing string). The pack's "Back to walks" is not used |
| D9 | Dog profile | Counters "{n} / 30", "{n} / 50". "(optional)" shown muted. Option bands "Caution from 20°C / Avoid from 25°C" and "Caution from 16°C / Avoid from 21°C". Tiles "Privacy & data" and "About the advice" | |
| D10 | Privacy | Title "Privacy & your data". Headline "Your walks stay on your phone". Card headings "Never leaves this phone" and "Only sent when you use a feature". Entry headings "MET Norway · when the check runs" and "OpenFreeMap · when you view a walk map" | The card bodies stay the existing `privacy_*` sentences (pack §4.8) |
| D11 | About | Headline "How we rate the weather". Intro "The overall rating is the worst of these. Heat-sensitive dogs use the values in brackets." Sources heading "Where this comes from" | Conditions: the table cells use exact boundaries (below), and the existing source and threshold sentences stay. The pack's shortened paragraph is not used |

**D11 thresholds table (exact cells)**

| Factor | Go | Caution | Avoid |
|---|---|---|---|
| Heat | below 20 °C (16 °C) | 20 to below 25 °C (16 to below 21 °C) | 25 °C or above (21 °C or above) |
| Pavement | below 20 °C (16 °C) | 20 to below 25 °C (16 to below 21 °C) | 25 °C or above (21 °C or above) |
| Rain | below 0.5 | 0.5 to 4 | above 4 |
| Wind | below 25 | 25 to below 39 | 39 or above |
| Cold | above 0 °C | 0 °C or below | never |

These cells agree with the MVP Rating rules, with s = 4 °C for Sensitive. The units for Rain (mm/h) and Wind (mph), and the daylight-and-sunny condition for Pavement, come from the existing threshold sentences on the same screen.

### Test assertions that change (owner-approved text changes, not weakening)
These existing assertions change only because the owner approved the new text. Each must assert the new exact text with the same values (AC63, AC64). Line numbers are as at `a8b4506`.

| Test file (under `app/src/test/java/io/github/greavemrsvg/dogwalkbuddy/`) | Current assertion | New assertion | Source |
|---|---|---|---|
| `ui/history/HistoryScreenTest.kt` L146-148, L192 | Row lines "Mon 19 Oct 2026 at 09:15, 1:15, 2.0 mi", "Thu 15 Oct 2026 at 17:30, 0:20, 0.5 mi", "Mon 12 Oct 2026 at 08:00, 0:30, 1.0 mi", "Mon 19 Oct 2026 at 14:20, 0:25, 0.0 mi" | "Mon 19 Oct at 09:15, 1:15, 2.0 mi", "Thu 15 Oct at 17:30, 0:20, 0.5 mi", "Mon 12 Oct at 08:00, 0:30, 1.0 mi", "Mon 19 Oct at 14:20, 0:25, 0.0 mi" | D7 |
| `ui/history/HistoryScreenTest.kt` L162-164 | "Mon 12 Oct 2026 to Sun 18 Oct 2026", "2 walks, 0:50, 1.5 mi", "Mon 19 Oct 2026 to Sun 25 Oct 2026" | "Mon 12 – Sun 18 Oct", "2 walks · 0:50 · 1.5 mi", "Mon 19 – Sun 25 Oct" | D7 |
| `ui/history/HistoryScreenTest.kt` L174, L223 | This-week node text equals "This week", "0 walks, 0:00, 0.0 mi" | This-week card shows "This week", "Mon 19 – Sun 25 Oct", "0", "walks", "0:00", "h:mm", "0.0", "miles", and its spoken text equals "This week: 0 walks, 0 minutes, 0.0 miles" | D7 |
| `ui/history/HistoryRowOpenTest.kt` L91, L95, L101, L119 | Row text "Mon 19 Oct 2026 at 09:15"; spoken "Mon 19 Oct 2026 at 09:15, 1:15, 2.0 mi" | Row texts "Mon 19 Oct" and "at 09:15"; spoken "Mon 19 Oct at 09:15, 1:15, 2.0 mi" | D7 |
| `ui/walkdetail/WalkDetailScreenTest.kt` L149-151, L161-163 | "Mon 19 Oct 2026 at 09:15", "Duration 1:15", "Distance 2.0 mi" | Heading "Monday 19 October", time range "09:15 – 10:30", Duration card "Duration" + "1:15", Distance card "Distance" + "2.0 mi" | D8, C14 |
| `ui/nav/AppFlowIntegrationTest.kt` L193-195, L202-203, L217-224, L234-241 | Opens the row "Mon 19 Oct 2026 at 09:15"; waits for "Walk details"; "Duration 1:15", "Distance 2.0 mi" | Opens the row "Mon 19 Oct" / "at 09:15"; waits for the heading "Monday 19 October"; Duration and Distance cards as above | D7, D8 |
| `ui/check/CheckCardTest.kt` L168, L265; `ui/check/CheckScreenIntegrationTest.kt` L179 | "Best walking window" | "Best window, next 12 hours" | C2 |
| `ui/check/CheckCardTest.kt` L158; `ui/check/CheckScreenIntegrationTest.kt` L192, L203 | "Retry" | "Try again" | C4 |
| `ui/check/CheckScreenIntegrationTest.kt` L149 | "Good time to walk?" | Unchanged with no profile. With a profile, "Good time to walk {name}?" | C1 |
| `ui/recording/WalkRecordingScreensTest.kt` (all `awaitText("Recording your walk")`) | "Recording your walk" on screen | "Recording walk". `service/WalkRecordingServiceTest.kt` L85 (notification title) stays "Recording your walk" | C5 |
| `ui/recording/WalkRecordingScreensTest.kt` L196 | Stop text equals "Stop" | Stop text equals "Stop and save walk" | C6 |
| `ui/profile/ProfileScreenTest.kt` L117, L319; `ui/profile/ProfileScreenStateTest.kt` L76, L86; `ui/check/CheckScreenIntegrationTest.kt` L237, L248; `ui/nav/AppFlowIntegrationTest.kt` L147; `ui/nav/NavigationTest.kt` L24 | "Save profile" | "Save" | C9 |
| `ui/profile/ProfileScreenTest.kt` L312 | "Sensitive uses stricter heat and pavement limits." | The C11 text | C11 |
| Any test asserting "Replace photo" or "The check needs your approximate location." | Current text | "Change photo"; "The check needs your approximate location" | C10, D5 |
| Tests of the Recording elapsed value (`LiveWalkFormat` and the Recording screen) | h:mm, for example "0:00" | h:mm:ss, for example "0:00:00", with the same durations | Scope item 5 (pack §4.4) |
| `ui/nav/AppFlowIntegrationTest.kt` and `ui/nav/NavigationTest.kt` taps on "Walk history", "Dog profile", "Privacy" and "About this advice" home links | Home link taps | Taps on the "Walks" or dog nav item, or on the "Privacy & data" or "About the advice" tiles | Navigation change (AC10), not a copy change |

Delete labels and messages that contain dates (`history_delete_description`, `history_delete_message`) keep their current date format with the year. Their assertions do not change (AC44).

## Conflicts between the pack and the MVP spec
| # | Conflict | Resolution (MVP wins) |
|---|---|---|
| K1 | Pack §4.1 puts the disclaimer below the best-window card. With the cached banner, age label and sensitivity line, it would not fit in the default viewport without scrolling. That breaks MVP AC26 and its existing test at 411 x 891 dp | MVP AC26 holds in every rated state at the default viewport (AC25). The disclaimer sits higher than the artboard where needed (owner: "Yes, move it up") |
| K2 | The CheckCaution artboard shortens the pollen notice to "Pollen isn't included" and joins it with other texts on one line | The exact "Pollen isn't included in this check" stays (MVP AC20, AC24, AC62) |
| K3 | The CheckUnavailable artboard has no disclaimer | Can't check keeps the disclaimer (AC29). The MVP test `AC26_the_disclaimer_is_also_shown_with_a_cached_result_and_with_no_result` asserts it and may not be weakened |
| K4 | The Profile artboard's "Remove photo" is 44 px high | At least 48 dp (MVP AC51, AC50) |
| K5 | The About table cells ("20–25°", "25–38", "(16°)") blur the boundaries in the Rating rules: 25 °C is Avoid, and 38.5 mph is Caution | The table uses exact boundary wording, and the existing threshold sentences stay on the screen (owner: "Yes to both"; D11, AC56) |
| K6 | The Recording artboard has no back control, but pack §2 lists Recording as a full-screen destination with one, and the MVP has one | Keep the back control (AC33) |
| K7 | Pack §2 says opening the app while recording goes straight to Recording "(unchanged behaviour)". The MVP app does not do that: Today shows "Walk in progress", which opens Recording | Keep the current behaviour (AC16). Auto-open moves to Later (owner decision) |
| K8 | Pack §3 says the hero card is one node announced with rating and reason only, but the card also shows factor values | The announcement starts "<Rating>. <Reason>." exactly as MVP AC50 requires, and the factor values follow (AC19) |
| K9 | The artboards and the About muted panel join attribution texts with " · " or add a full stop ("Weather data: MET Norway (CC BY 4.0). Maps: …") | Each AC-fixed text stays complete and unaltered (AC62) |
| K10 | The Privacy artboard's body text differs from the `privacy_*` strings and drops the Cloudflare mention | The existing sentences stay, word for word (pack §4.8 agrees; AC54) |
| K11 | The WalkDelete artboard's sheet body differs from `history_delete_message` | The existing `history_delete_message` stays (pack §4.6 agrees; AC48) |
| K12 | The pack's best-window range uses a spaced en dash | Rejected by the owner (C3). The range stays "12:00-15:00" (MVP AC16, MVP AC18, AC22) |

## Non-functional requirements
- **Performance:** no regression against the MVP proposed baseline in `docs/perf/20261019-dog-walk-buddy-mvp/proposed-baseline.json`. Measure with the same method and the same emulator configuration (Medium_Phone_API_37.0, debug build).
  - Four timings are measured: app cold start, Start walk tap to recording, History open with 500 walks, and check launch to result. Each median may be no more than 10 % above the baseline median (assumption A6). Every MVP target must also still be met: Start walk ≤ 1 s, History with 500 walks ≤ 1 s, check p90 ≤ 5 s.
  - Report PSS memory on Today, Recording and Walk detail against the baseline.
  - Report release APK size before and after. The fonts add about 300 KB. There is no size target.
  - Recording with the live route line must stay within the MVP battery budget of ≤ 8 % for a 60-minute walk. This is measured on the reference phone with the other deferred checks.
  - Results go in `docs/perf/20261022-ux-redesign/`.
- **Accessibility:** all MVP floors are unchanged (MVP AC50-AC53), and they now apply in the light, dark and recording palettes:
  - 48 dp touch targets.
  - Full-width Start and Stop, at least 72 dp high.
  - Text contrast of 4.5:1, and 7:1 for the rating, the reason and Start/Stop. Where a pack colour pair falls below these floors (for example the dark summary-card captions, AC40), the floor wins and the architect picks a compliant colour from the token roles.
  - 200 % font scaling that scrolls instead of truncating. The one owner-approved exception is the dog-name nav label, which is shortened with "…" on screen while its spoken label keeps the full name (AC60).
  - No meaning carried by colour alone.
  - TalkBack labels on every control, with decorative graphics hidden from accessibility services.
- **Offline / error behaviour:** unchanged from the MVP. The recording route line works with no network, and fonts and icons are on the device. The offline wording in the cached banner depends on the system's network state. The app already holds the network-state permission (`ACCESS_NETWORK_STATE`).
- **Security:** unchanged from the MVP security standards. There are no new permissions, exported components or hosts. The bundled font files are static assets.
- **Dependencies:** no new library without an ADR in `docs/adr/`. The fonts are a new, owner-approved dependency. They need an ADR covering how they ship and their licence; SIL Open Font License 1.1 is expected, to be confirmed in the ADR. Icons use the Material Symbols equivalents in `tokens.json` → `icons`. Adding an icon library needs an ADR.
- **Test coverage:** changed-lines coverage must be at least 80 %, per `docs/project.md` (`bash scripts/coverage.sh <base-ref>`). The ADR 0001 exclusions apply: the MapLibre adapter stays excluded and is checked by hand.
- **Build and lint:** the commands in `docs/project.md` pass unchanged.

## Data and privacy
No change. No personal data is created, stored or sent that the MVP did not already handle:
- The dog's name is already stored on the device. It is now also shown in the nav label, the check title and the Sensitive footnote line, and it stays on the device.
- The recording route line is drawn from route fixes already stored on the device for the walk being recorded. Nothing is sent.
- The factor grid shows forecast values the app already receives and caches. Nothing new is requested.
- Fonts and icons are bundled, so no font or icon service is contacted.
- The Privacy screen keeps every MVP statement word for word (MVP AC46).

## Assumptions
Assumption IDs are kept stable so that earlier references stay valid. The former assumptions A3 (system Back), A4 (strip hour labels), A8 (long dog names), A9 (factor-value rounding) and A11 (negative rounding) are now owner decisions, recorded in "Owner decisions" above, as is the 12-hour strip-label form ("2pm"). None of the assumptions below is a user-visible product choice that the owner has not confirmed.

- **A1** The redesign is a presentation change. The domain layer (rating, best window, cache, weekly totals) and the data layer keep their behaviour. The UI may use more of the data that already exists: hourly ratings for the strip, current-hour conditions and the pavement factor's rating for the factor grid, and route fixes for the line.
- **A2** Dark mode follows the system setting only, from Android 10 (minSdk 29). There is no in-app override.
- **A5** The history row's "More" menu with Delete stays (pack §4.5 allows it), so a walk can be deleted from both the row and walk detail. This keeps the current MVP behaviour.
- **A6** "No performance regression" means each median is within 10 % of the MVP proposed baseline on the same emulator configuration, and every MVP target is still met. The baseline is emulator-based and only proposed.
- **A7** "Screenshot" checks are manual comparisons on the emulator. The project has no screenshot-test tool, and adding one needs an ADR.
- **A10** The week-range and date formats for dates outside the current year (AC39, AC46) apply the owner's D7/D8 condition. The pack shows only current-year examples.

## Dependencies and risks
- **Fonts ADR** (architect): Bricolage Grotesque and Figtree, bundled, with licence notices shown in About (AC8).
- **Release implication:** this run's release PR goes from `feature/20261022-ux-redesign` to `main`. It carries the MVP **and** the redesign together, as v0.1.0 or whatever version the release agent derives. It replaces release PR #28, which the owner closes when this run reaches release. Run `20261019-dog-walk-buddy-mvp` stays on hold and does not release on its own.
- **Reference-phone manual checks are owed at this run's release.** They are done once, on the redesigned app, on the reference phone (Samsung Galaxy A35 5G). The MVP-run checks still owed are:
  - the real pocket walk with the screen off (MVP AC29)
  - recording in airplane mode (MVP AC31)
  - a photo deleted from the gallery (MVP AC43)
  - backup and device transfer (MVP AC48)
  - the proxy host list (MVP AC49)
  - real TalkBack speech, including the Normal radio option (MVP AC50, MVP finding F1)
  - maximum font and display size (MVP AC52)
  - battery use over 60 minutes, and the 4G check timing.

  Sources: `docs/runs/20261019-dog-walk-buddy-mvp/device-checks.md` (section "Deferred to Galaxy A35") and `docs/releases/v0.1.0.md`. The MVP run's phone-checks file is not on this branch; it exists only in an unpushed local commit on the MVP branch, so this spec cites the two files above. This run adds AC27, AC38, AC52, AC56, AC59, AC60, AC61 and AC65 to the same phone session, plus the battery check with the live route line.
- **Risk (Medium): test churn.** Moving controls into tabs and the approved copy changes mean many existing UI tests change. "Test assertions that change", AC63 and AC64 limit what may change. The tester and reviewer check each changed assertion against that list.
- **Risk (Low): layout pressure on Today.** The factor grid and the cached banner add height. AC25 requires the disclaimer to stay visible without scrolling in all four cases, and the owner allows it to sit higher than the artboard.
- **Risk (Low): 12-hour strip labels are wider.** In 12-hour mode the labels ("10pm", "12am") are wider than the 2-digit labels in the artboards. AC23 requires all 12 to show in full without overlap at the default viewport.
- **Risk (Low, accepted by the owner): 12-hour time styles differ on Today.** The strip labels have no space before am/pm ("3pm"; owner decision `"2pm"`), while the best-window text on the same card keeps its MVP form with a space ("3:00 pm"; MVP AC16, MVP AC18). This difference is deliberate. The best-window text must not be changed to match the strip, nor the strip to match the best-window text, and testers must not report the difference as a defect.
- **Risk (Low, accepted by the owner): rounded factor values at thresholds.** The rating uses the unrounded value, but the factor grid shows whole numbers (owner decision "Whole numbers"). So a value just under a threshold can display as the threshold: 24.6 mph shows "25 mph" while the wind rating is still Go (Caution starts at 25 mph), and 19.6 °C shows "20°C" while heat is still Go for a Normal dog. The reverse also happens: 0.5 mm/h (Caution) shows "1 mm/h", and 4.4 mm/h (Avoid, above 4) shows "4 mm/h". The rating word and reason stay correct. The owner accepted this as the trade-off of whole numbers. Testers must not report it as a defect.
- **Risk (Low): scope.** About 8 tasks (see below). Adding any Later item would go over 8.
- **Sizing** (the architect owns the breakdown):
  1. Theme foundations: tokens, dark mode, fonts and their ADR, contrast test.
  2. Bottom navigation and destinations.
  3. Today: hero card with factor grid, header, banner, footnote, pinned Start, loading.
  4. Best-window card and strip, plus the Can't check, location-needed and precise-location states.
  5. Recording restyle and route line.
  6. Walks and walk detail, with the delete sheet.
  7. Dog profile, including the photo error messages (AC67).
  8. Privacy and About, with the table and font licences.

  Each task applies the approved copy and updates the matching test assertions for the screens it owns.

## Later (in the pack, not in this run)
- GPS signal indicator on Recording ("GPS good"). It needs a defined signal-quality rule.
- Opening the app straight onto Recording while a walk is being recorded.
- A dark map style for the walk-detail map tiles in dark mode.
- Real illustrations to replace the placeholder art (map art, route sketch, paw avatar).
- Screenshot-test tooling for visual regression (needs an ADR).
- Everything in the MVP spec's Later list stays there.
