# Spec: Dog Walk Buddy MVP (walk check, walk logging, dog profile)

- Run: `20261019-dog-walk-buddy-mvp`
- Epic: #7
- Status: Draft

## Problem
UK dog owners have no quick, single place to decide whether *right now* is a safe and pleasant time to walk their dog. Today they have to read a general weather app and work out the dog-specific risks themselves: heat, hot pavement, heavy rain, strong wind, freezing conditions. Heat in particular is underestimated. Vets Now warns heat-related illness can start above about 20 °C, and pavements get much hotter than the air. Owners also want a simple, private record of their walks (how often, how long, how far) without signing up for an account or sharing their location history. The first release delivers a one-glance Go / Caution / Avoid check, one-tap walk logging that works offline, and a single dog profile whose heat-sensitivity setting tightens the heat warnings.

## Users and context
- Everyday dog owners in the UK with one dog.
- Use is quick and often one-handed, just before or during heading out of the door. The phone is often outdoors in bright sunlight and goes into a pocket during the walk.
- Android phones only (no tablets, no other platforms). English (UK) only.
- Connectivity may be poor or absent during walks (parks, countryside).
- Some users rely on TalkBack or large system font sizes.

## Scope
**In scope**
1. **"Good time to walk?" check** for the user's approximate current location, using free, keyless, commercial-use weather data (MET Norway Locationforecast, see Dependencies):
   - Factors: air temperature (heat and cold), precipitation rate, wind (mean speed and gusts), and a hot-pavement estimate (from air temperature, daylight and cloud cover).
   - One overall result, **Go / Caution / Avoid**, with one main reason in plain words.
   - The best walking window in the next 12 hours.
   - Rating rules exactly as in "Rating rules" below. Heat and pavement thresholds are stricter for a dog set to "sensitive".
   - The last result is reused for up to 3 hours when a fresh check isn't possible, labelled with its age.
   - "Guidance, not veterinary advice" framing, an "About this advice" screen naming the guidance sources, and data attribution.
   - A notice that pollen is not included.
2. **Walk logging** (fully offline):
   - One-tap Start / Stop. Records start time, duration, distance and GPS route, and keeps recording with the screen off or the app in the background.
   - Walk history list, newest first, grouped by week (Monday to Sunday) with weekly totals (number of walks, total time, total distance) and a "This week" summary.
   - Walk detail showing the route on a street map (free, keyless, commercial-use tiles, see Dependencies).
   - Delete a single walk.
3. **Dog profile** (one dog): name, breed, photo, heat sensitivity (Normal / Sensitive).
4. **Privacy and data controls**: an in-app privacy explanation, "Delete all my data", no accounts, no backend, no analytics, data excluded from cloud backup.

**Rating rules** (binding; "s" = 0 °C for Normal, 4 °C for Sensitive)

| Factor | Go | Caution | Avoid | Main reason text (Caution / Avoid) |
|---|---|---|---|---|
| Heat (air temperature T) | T < 20 − s | 20 − s ≤ T < 25 − s | T ≥ 25 − s | "Warm: keep it short and bring water" / "Too hot to walk safely" |
| Hot pavement: applies only in daylight (between local sunrise and sunset) **and** sunny (forecast cloud cover below 50 %) | T < 20 − s | 20 − s ≤ T < 25 − s | T ≥ 25 − s | "Pavement may be hot: test it with the back of your hand" / "Pavement likely too hot for paws" |
| Rain (precipitation rate P, mm/h) | P < 0.5 | 0.5 ≤ P ≤ 4 | P > 4 | "Rain expected" / "Heavy rain" |
| Wind (mean speed M, gust G, mph) | M < 25 and G < 25 | M ≥ 25 or G ≥ 25 (and G < 39) | G ≥ 39 | "Windy" / "Dangerous wind gusts" |
| Cold (air temperature T) | T > 0 | T ≤ 0 | never | "Freezing: watch for ice and grit" |

Resulting bands: Normal is Go below 20 °C, Caution from 20 °C to below 25 °C, Avoid from 25 °C. Sensitive is Go below 16 °C, Caution from 16 °C to below 21 °C, Avoid from 21 °C.

- The overall rating for an hour is the worst factor rating (Avoid > Caution > Go).
- The main reason is the reason of the worst-rated factor. On a tie it follows this precedence: Hot pavement, Heat, Wind, Rain, Cold. When pavement and heat are both at the worst level, the pavement reason is shown.
- When every factor is Go, the reason is "Good conditions for a walk".
- Sensitivity changes only the heat and pavement thresholds. Rain, wind and cold are the same for both settings.
- Missing data: an hour with any of temperature, precipitation, mean wind or gust missing is never rated Go. The hour is rated from the available factors. If that would be Go, the hour is rated Caution with the reason "Some weather data isn't available". If temperature, precipitation and wind are all missing for the current hour, the check shows "Can't check right now". Missing cloud cover means "not sunny" for the pavement rule. Temperature is still rated.
- **Best window:** look at the 12 hourly forecast periods starting with the one that contains the current time. Find the best overall rating among them. The window is the longest run of consecutive hours at that rating, and the earliest run wins a tie. It is shown as a start time and an end time (the end of the last hour in the run) in the device's time format. If the window starts in the current hour it reads "Now until <end>". If the best rating is Caution, the window is labelled "Best available: Caution". If all 12 hours are Avoid, show "No good time in the next 12 hours".

**Out of scope** (this release)
- Pollen. No free, keyless, commercial-use UK pollen source was found (see Dependencies). Moved to Later.
- Dog-friendly routes nearby, multiple dogs, walk reminders or notifications, sharing or exporting walks, Wear OS.
- iOS, tablets, web, widgets, home-screen shortcuts.
- Accounts, sign-in, cloud sync, any backend or proxy server, analytics, crash reporting to third parties, ads or in-app purchases. Commercial distribution is allowed, but no monetisation is built in this release.
- Weather alerts or background weather checks. The check runs only when the user opens it or taps refresh.
- Pausing or resuming a walk, stopping a walk from the notification, editing a walk, adding a walk manually, discarding very short walks automatically.
- Taking a new photo with the camera from inside the app. MVP only picks an existing photo.
- Downloading maps for offline use. Map search, geocoding, routing or directions.
- Suggested or generated walk routes (see Later).
- Unit settings (units are fixed, see Assumptions), languages other than English (UK).
- Guidance tuned for locations outside the UK. The check still runs elsewhere, but its thresholds are UK guidance.
- Veterinary or medical advice of any kind, and breed-specific thresholds (breed is information only).
- Moving data to a new phone (backup and device transfer are excluded on purpose, see Data and privacy).

**Later** (wider vision, not this release)
- Pollen level in the check, once a free, keyless, commercial-use UK source exists or the owner accepts a different source policy.
- Dog-friendly routes nearby; multiple dogs; walk reminders; sharing walks; Wear OS.
- **Suggested walk route (generated loop)**, planned as the next release: the app plans a new circular walk that starts and ends at the user's location, for a length or duration the user chooses. A future run that specifies it must meet these constraints:
  - Routing or map data must be free, keyless and allowed for commercial use, the same source policy as this release.
  - Privacy impact: the user's approximate location is sent to that routing or map-data provider. This is a new off-device transfer that must be assessed and disclosed in the Privacy screen and Data and privacy section.
  - Relation to "Dog-friendly routes nearby": that item finds existing routes near the user, while this one generates a new loop. They are separate items, and the future run decides how they relate (for example a shared provider or dog-friendly preferences in the generated loop).
- Possible later: pause/resume, camera capture, offline maps, export, unit settings.

**Sizing:** this scope is expected to fit in at most 8 tasks with a walking skeleton as task 1. The functional areas are: the check's data and location; the rating rules and best window; the check screen; the dog profile; walk recording; walk history and totals; the route map; privacy and data controls. The architect owns the actual breakdown.

## Acceptance criteria
Forecast values in ACs mean the values for the hour under test, as supplied by a stubbed weather response. "Normal" and "Sensitive" refer to the dog profile's heat sensitivity. Unless stated otherwise, the hour is at night or overcast, so the pavement rule does not apply. Reason texts are the exact texts in "Rating rules".

| ID | Given | When | Then | Test type |
|---|---|---|---|---|
| AC1 | Normal; current hour T = 15 °C, P = 0 mm/h, M = 10 mph, G = 15 mph, all data present | The check runs | The result is **Go** with reason "Good conditions for a walk" | unit |
| AC2 | Normal; no other factor above Go | The current hour T is 19.9, 20.0, 24.9 and 25.0 °C in turn | The result is Go, Caution ("Warm: keep it short and bring water"), Caution, Avoid ("Too hot to walk safely") in that order | unit |
| AC3 | Sensitive; no other factor above Go | The current hour T is 15.9, 16.0, 20.9 and 21.0 °C in turn | The result is Go, Caution, Caution, Avoid in that order, with heat reasons | unit |
| AC4 | Normal; current hour is between local sunrise and sunset with cloud cover 30 %; no other factor above Go | T is 19.9, 20.0 and 25.0 °C in turn | The result is Go, Caution with "Pavement may be hot: test it with the back of your hand", and Avoid with "Pavement likely too hot for paws" | unit |
| AC5 | Sensitive; daylight, cloud cover 30 % | T is 16.0 and then 21.0 °C | The result is Caution and then Avoid, both with pavement reasons | unit |
| AC6 | Normal; T = 22 °C | The hour is (a) after sunset with cloud cover 0 %, (b) in daylight with cloud cover 50 %, (c) in daylight with cloud cover missing | In all three cases the result is Caution with the heat reason, not the pavement reason | unit |
| AC7 | Normal; T = 15 °C, wind Go | P is 0.4, 0.5, 4.0 and 4.1 mm/h in turn | The result is Go, Caution ("Rain expected"), Caution, Avoid ("Heavy rain") | unit |
| AC8 | Normal; T = 15 °C, P = 0 | (M, G) in mph is (24, 24), (25, 25), (10, 25), (20, 38.9), (20, 39) in turn | The result is Go, Caution ("Windy"), Caution, Caution, Avoid ("Dangerous wind gusts") | unit |
| AC9 | Normal; P = 0, wind Go | T is 0.1, 0.0 and −10 °C in turn | The result is Go, Caution ("Freezing: watch for ice and grit"), Caution. Cold never gives Avoid | unit |
| AC10 | Sensitive | The same rain, wind and cold inputs as AC7-AC9 (with T = 10 °C where AC7/AC8 use 15 °C) are rated | The results are identical to AC7-AC9 | unit |
| AC11 | Normal; T = 22 °C (heat Caution) and P = 5 mm/h (rain Avoid) | The check runs | The result is Avoid with "Heavy rain" | unit |
| AC12 | Normal; daylight, cloud 20 %, T = 21 °C (heat and pavement Caution), M = 26 mph (wind Caution), P = 1 mm/h (rain Caution) | The check runs | The result is Caution with the pavement reason (precedence: pavement, heat, wind, rain, cold) | unit |
| AC13 | Normal; T = 15 °C, P = 0, M = 10 mph, gust missing for the current hour | The check runs | The result is Caution with "Some weather data isn't available", never Go | unit |
| AC14 | Normal; T = 15 °C, gust missing, P = 6 mm/h | The check runs | The result is Avoid with "Heavy rain" (missing data does not hide a worse rating) | unit |
| AC15 | Temperature, precipitation and wind all missing for the current hour | The check runs | "Can't check right now" is shown with a retry control, and no Go / Caution / Avoid result is shown | unit |
| AC16 | 12 hourly ratings starting now: C, C, G, G, G, C, G, G, A, A, C, C (hours starting 10:00 to 21:00) | The best window is computed | It is 12:00-15:00 (longest Go run; ties go to the earliest) | unit |
| AC17 | 12 hourly ratings G, G, C, … (all others C or A), starting now at 10:00 | The best window is computed and shown | It reads "Now until 12:00" | unit |
| AC18 | 12 hourly ratings starting now at 10:00 with no Go hour, where the longest Caution run is the hours starting 15:00 and 16:00 | The best window is shown | It shows 15:00-17:00 labelled "Best available: Caution" | unit |
| AC19 | All 12 hourly ratings are Avoid | The best window is shown | It reads "No good time in the next 12 hours" | unit |
| AC20 | Any successful check | The result is displayed | The screen shows "Pollen isn't included in this check" | integration |
| AC21 | A successful check completed 2 h 59 min ago and the device is now offline | The user opens the check | The previous result (rating, reason, best window) is shown with a visible label giving its age, e.g. "Checked 2 h 59 min ago" | integration |
| AC22 | The last successful check is 3 h 0 min or older, or there is no previous check, and the weather request fails (offline, timeout or error response) | The user opens the check | "Can't check right now" is shown with a retry control, and no rating is shown | integration |
| AC23 | Device location 51.507351, −0.127758 and location permission granted | The check runs against a stub server | The only location sent is latitude 51.51 and longitude −0.13, the request uses HTTPS, and it carries no device, advertising or user identifier (only an identifying app User-Agent, as MET Norway requires) | integration |
| AC24 | The app is installed with permissions granted | The user records a walk, views history, edits the profile, or the app is in the background | No weather request is made. Weather requests happen only when the check screen is opened or refresh is tapped | integration |
| AC25 | Location permission has not been granted | The user opens the check | An explanation that the check needs approximate location is shown, with a control to grant it. No weather request is made | integration |
| AC26 | Any check result, cached or fresh | The check screen is displayed at default font size | The text "Guidance only, not veterinary advice. If you're worried about your dog, contact a vet." is visible without scrolling, and an "About this advice" link opens a screen that lists the rating thresholds and names Vets Now and FOUR PAWS as guidance sources | integration |
| AC27 | Any check result | The check screen or About screen is displayed | The attribution "Weather data: MET Norway (CC BY 4.0)" is visible on the check screen or reachable in one tap from it | integration |
| AC28 | The app is open on its first screen and precise location permission is granted | The user taps Start walk once | Recording starts within 1 s. Elapsed time and distance are shown and update at least every 5 s, and a persistent notification says a walk is being recorded | integration |
| AC29 | A walk is being recorded | The screen is locked or another app is opened for 10 minutes, then the user returns | Elapsed time includes the 10 minutes and the route includes locations from that period. `manual` check: record a 10-minute walk with the phone locked in a pocket, then confirm duration ≥ 10 min and the route covers the path walked | manual + integration |
| AC30 | A walk is being recorded | The user taps Stop | The walk is saved with start date and time, duration, distance and route, recording and the notification end, and the walk appears at the top of the history | integration |
| AC31 | The device has no network connection (airplane mode with GPS on) | The user starts, records and stops a walk | The walk is recorded and saved with duration, distance and route, and no network error is shown during recording. `manual` check: on a real phone in airplane mode with GPS on, walk for at least 5 minutes, stop, and confirm the walk is in history with non-zero distance and no error was shown | integration + manual |
| AC32 | A simulated GPS track of known length 2.000 km with fixes of 10 m accuracy or better | It is recorded as a walk | The saved distance is within ±2 % of 2.000 km | integration |
| AC33 | Precise location permission has not been granted | The user taps Start walk | No walk starts, and an explanation that walk recording needs precise location is shown with a control to grant it | integration |
| AC34 | A walk is being recorded | The app process is killed by the system | On next launch the walk is saved, ending at the last recorded location, with duration and distance up to that point | integration |
| AC35 | Saved walks exist | The user opens the history | Walks are listed newest first, each showing date, start time, duration (h:mm) and distance. With no walks, an empty state reads "No walks yet" | integration |
| AC36 | Walks starting Mon 12 Oct 2026 08:00 (30 min, 1.0 mi), Sun 18 Oct 2026 23:30 (20 min, 0.5 mi) and Mon 19 Oct 2026 00:10 (10 min, 0.3 mi), local time | The history is viewed | The week Mon 12 Oct 2026 to Sun 18 Oct 2026 shows 2 walks, 0:50 and 1.5 mi (the 12 Oct and 18 Oct walks), and the week Mon 19 Oct 2026 to Sun 25 Oct 2026 shows 1 walk, 0:10 and 0.3 mi (the 19 Oct walk). A walk counts in the week it started | unit |
| AC37 | No walk has been recorded in the current Monday-to-Sunday week | The user opens the history | A "This week" summary shows 0 walks, 0:00, 0.0 mi | unit |
| AC38 | A saved walk with a route and a network connection | The user opens the walk detail | The route is drawn on a street map zoomed to fit the whole route, and the map shows "© OpenStreetMap contributors" and "© OpenMapTiles" attribution. `manual` check: open a recorded real walk online and confirm the whole route is visible on street tiles without panning and the attribution is readable | integration + manual |
| AC39 | A saved walk and no network connection with no map tiles available | The user opens the walk detail | Duration, distance and date are shown, and "Map not available offline" is shown where the map would be | integration |
| AC40 | A saved walk | The user chooses Delete on it and confirms | The walk and its route are removed from the device, it no longer appears in history, and the weekly totals no longer include it. If the user cancels instead, nothing changes | integration |
| AC41 | No dog profile exists | The user saves a profile with name "Biscuit", breed "Cockapoo", a photo from the device and heat sensitivity Sensitive | The profile is shown with those values on the next app launch. Heat sensitivity defaults to Normal when not changed | integration |
| AC42 | The profile form | The user tries to save with an empty name, a name over 30 characters, or a breed over 50 characters | Saving is refused with a message naming the field and the limit. Breed and photo may be left empty | unit |
| AC43 | A profile with a photo picked from the gallery | The original photo is deleted from the gallery | The profile still shows the photo. The user can remove or replace the photo. `manual` check: pick a gallery photo, delete it in the gallery app, restart Dog Walk Buddy and confirm the photo still shows; then remove it and confirm it is gone | integration + manual |
| AC44 | A profile set to Normal and a check showing **Go** ("Good conditions for a walk") for current-hour T = 17 °C, with all other factors Go and the pavement rule not applying | The user changes sensitivity to Sensitive and returns to the check | The result is re-rated without a new network request and shows Caution with the heat reason "Warm: keep it short and bring water", because 17 °C is in the Sensitive Caution band (16 °C to below 21 °C). With T = 22 °C instead, the same change turns Caution (Normal) into Avoid with "Too hot to walk safely" (Sensitive Avoid from 21 °C) | integration |
| AC45 | No dog profile exists | The check runs | Normal thresholds are used, and the screen notes "Using normal heat sensitivity. Set up your dog's profile to change this." | integration |
| AC46 | The app is installed | The user opens Privacy (reachable from the main screen in at most 2 taps) | The screen says: approximate location (rounded to about 1 km) is sent to MET Norway only when a check runs, and MET Norway also receives the phone's IP address; when a walk map is viewed the map provider (OpenFreeMap) receives requests for the map area shown and the phone's IP address; walk routes, the dog profile and the walk history never leave the phone; there are no accounts, analytics or tracking | integration |
| AC47 | Walks, a profile with a photo and a cached check exist | The user chooses "Delete all my data" and confirms | All walks, routes, the profile, the photo copy and the cached check are removed. History shows "No walks yet", no profile exists, and the check has no cached result | integration |
| AC48 | The release build | Its backup configuration is inspected, or a cloud backup and device-transfer test is run | App data (walks, routes, profile, photo, cached check) is excluded from Android cloud backup and device-to-device transfer. `manual` check: with data present, trigger a cloud backup, reinstall the app on the same or another phone with restore, and confirm no walks, profile or cached check are restored | integration + manual |
| AC49 | The app is used across all features (check, walk record, history, map, profile) through an intercepting proxy | Network traffic is inspected | The only hosts contacted are the MET Norway API host and the OpenFreeMap tile/style hosts. All traffic is HTTPS and no analytics or advertising hosts are contacted. `manual`: run each feature once with the proxy and record the host list | manual |
| AC50 | TalkBack is on | The user moves through the check, Start/Stop, history, walk detail, profile and privacy screens | Every control has a spoken label. The check result is announced as rating then reason (e.g. "Caution. Windy."), and the best window is announced as text. `manual`: walk through with TalkBack; automated accessibility checks also pass on each screen | manual + integration |
| AC51 | Any screen | Automated accessibility checks run | Every interactive control is at least 48 × 48 dp. Start walk / Stop is at least 72 dp high and full-width. Text contrast is at least 4.5:1, and at least 7:1 for the rating, the reason and Start/Stop | integration |
| AC52 | The system font size is at its largest setting (200 %) | The check screen and the recording screen are shown | The rating, the reason, the best window and Start/Stop are fully readable without truncation or overlap (scrolling allowed). `manual` check: set the phone's font and display size to maximum and visually confirm each listed element on both screens | integration + manual |
| AC53 | Any check result | It is displayed | Go, Caution and Avoid each differ by text and icon as well as colour (understandable in greyscale) | integration |

## Non-functional requirements
- Performance:
  - With a fresh launch and a typical 4G connection, the check result is shown within 5 s (90th percentile over 20 runs, manual on a mid-range reference phone the architect names).
  - Start walk takes ≤ 1 s from tap to recording (AC28).
  - With 500 saved walks, the history opens in ≤ 1 s.
  - Recording a 60-minute walk uses no more than 8 % battery on the reference phone (manual).
- Accessibility: TalkBack support (AC50); 48 dp minimum touch targets and a large Start/Stop (AC51); WCAG 2.1 AA contrast, and 7:1 for the key outdoor information so it reads in bright sunlight (AC51); font scaling to 200 % (AC52); no information conveyed by colour alone (AC53); the primary actions (check result and Start walk) are on the first screen and usable one-handed.
- Offline / error behaviour: walk logging, history, profile and privacy work fully offline. The check reuses a result under 3 hours old, labelled with its age, and otherwise shows "Can't check right now" (AC21, AC22). Never show Go when key data is missing (AC13-AC15). The walk map degrades to a message offline (AC39). A walk survives the app process being killed (AC34). Network calls time out within 10 s rather than hang.
- Security (per `docs/governance/security-standards.md`):
  - HTTPS only; cleartext traffic disabled; certificate validation never disabled.
  - No API keys or secrets exist or are needed.
  - Weather and map responses are treated as untrusted input and validated for type and range. A malformed response is handled as "data missing" or a failed check, never a crash or Go.
  - Profile text input is length-checked (AC42).
  - No exported components beyond the launcher entry.
  - Data is in app-private storage only.
  - Release builds are minified and signed with keys outside the repo.
  - Precise location is used only while a walk is being recorded. Approximate location is used only for the check.
- Data/privacy: see below. No accounts, backend, analytics or tracking (AC49).
- Compliance with provider terms: an identifying User-Agent on MET Norway requests, the provider's caching headers respected, and attribution shown (AC27, AC38).

## Data and privacy
- **Created and stored on the device only** (app-private storage):
  - Walks: start date and time, duration, distance and the GPS route. A route is a trace of precise locations and can reveal home and routine, so it is treated as personal data.
  - Dog profile: name, breed, a copy of the chosen photo, heat sensitivity.
  - The last check result with its time and coarse location.
- **Sent off the device:**
  - To MET Norway (api.met.no), only when a check runs: latitude and longitude rounded to 2 decimal places (about 1 km), over HTTPS. MET Norway, like any server, also sees the phone's IP address and the app's User-Agent. Its terms say these are logged. No other data is sent.
  - To OpenFreeMap (tile/style hosts), only when the user views a walk map: requests for the map tiles covering the area shown. These indicate the approximate area of the route being viewed, along with the IP address. The route coordinates themselves are never sent.
  - Nothing else is sent. No account data, analytics, crash reporting or advertising identifiers.
- **Not created:** no accounts, no user identifiers, no health data about the owner. The dog's heat sensitivity is a preference, not veterinary data.
- **Who can see it:** only the phone's user. Data is excluded from Android cloud backup and device-to-device transfer (AC48), so it does not reach Google backup storage. As a result, history is not carried to a new phone.
- **Retention and deletion:** data is kept until the user deletes a walk (AC40), removes or replaces the photo (AC43), uses "Delete all my data" (AC47), or uninstalls the app. The cached check result is replaced by each successful check and ignored after 3 hours.
- **Explained in the app:** Privacy screen (AC46).
- **Lawful basis note (UK GDPR):** processing happens on the user's own device for the user's own purposes. The only transfers are the coarse location and IP address to the weather provider and the IP address and map area requests to the tile provider, which are needed to provide the features. The in-app privacy text discloses both.

## Assumptions
These are for the owner to review at Gate A.
1. **Weather provider:** MET Norway Locationforecast 2.0 is the weather source. It is free, needs no key, and its data is CC BY 4.0 / NLOD (commercial use allowed). It covers the UK at hourly resolution for at least the next 12 hours, including temperature, precipitation amount, mean wind, wind gust and cloud cover. The walking skeleton confirms gust and cloud cover are present for UK points. If they are not, the work stops for an owner decision before the check is built.
2. **Pollen** is dropped from the MVP (owner's fallback decision) because no free, keyless, commercial-use UK pollen source was found. The check states this (AC20). The owner's "pollen data not available" rule (Q6) therefore has no case to apply to in this release.
3. **"Sunny"** for the pavement rule means forecast cloud cover below 50 %. **"Daylight"** means between local sunrise and sunset at the checked location.
4. **Main-reason precedence** on ties is: hot pavement, heat, wind, rain, cold (safety-first).
5. **Best window** = the longest run of consecutive hours at the best rating in the next 12 hourly periods, with the earliest run winning a tie. It may be a single hour.
6. **Missing gust** counts as missing wind data (never Go), per the owner's "never Go if wind data is missing".
7. **Units:** temperature in °C, wind in mph, rain in mm/h, distance in miles to 1 decimal place, duration as h:mm. There is no unit setting.
8. **Week** = Monday 00:00 to Sunday 23:59 local time. A walk counts in the week it started.
9. **Profile limits:** name required, 1-30 characters; breed optional free text, up to 50 characters; photo optional, picked from the device (no in-app camera); sensitivity defaults to Normal. The check works without a profile, using Normal (AC45).
10. **Location permissions:** approximate location is enough for the check. Precise location is needed only to record walks, and recording continues in the background with a visible notification. Permission is requested in context, when the feature is first used.
11. **When the check runs:** only when the check screen is opened or refresh is tapped. There are no background or scheduled checks.
12. **Storage protection:** walk and profile data rely on Android's app-private storage and device encryption. This spec requires no extra app-level encryption, because the data is not health or employment data under the security standards. Security review may still recommend it.
13. **Map tiles:** OpenFreeMap's public instance is the tile source. It is free, has no key, and its data is OpenStreetMap (ODbL) with OpenMapTiles schema (BSD-3 / CC BY 4.0), which allows commercial use with attribution. The tile source must be easy to swap without app redesign, as the OSMF policy advises. The OpenStreetMap Foundation's own tile servers are **not** used, because their policy forbids distributing heavy-use apps without prior permission.
14. **Guidance sources** shown in "About this advice": Vets Now (heat-related illness risk above about 20 °C) and FOUR PAWS UK (asphalt about 52 °C at 25 °C air). These are named as the basis for the thresholds, framed as guidance, not veterinary advice.
15. Data is excluded from cloud backup and device transfer, so the user accepts that walk history doesn't move to a new phone.

## Dependencies and risks
- **MET Norway Locationforecast 2.0** (api.met.no).
  - Licence: data CC BY 4.0 and NLOD. Free, no key, commercial use allowed.
  - Attribution "MET Norway" is required (AC27).
  - Conditions: an identifying User-Agent (generic agents are throttled or blocked), respecting caching headers and an aggregate limit of 20 requests/s across all installs, and not using "Yr" or implying MET Norway endorsement in the app name or branding.
  - **Risk (Medium):** MET Norway's terms say browsers and mobile apps "should" use a proxy rather than call the API directly. Low-volume apps that identify themselves are tolerated, but heavy traffic "must" go through a caching proxy. A proxy would be a backend, which the owner's constraints rule out. If the app grows, it may be throttled or the owner may have to revisit "no backend". The terms also note that direct calls expose users' IP addresses to MET Norway, which is disclosed in the Privacy screen (AC46).
- **OpenFreeMap public tile instance** (openfreemap.org).
  - Free, no key, no registration, no cookies, no stated request limits. Software is MIT. The project's FAQ ("Is commercial usage allowed?") describes commercial use as allowed, with attribution.
  - Attribution: "OpenFreeMap © OpenMapTiles Data from OpenStreetMap". The OpenFreeMap part is optional; "© OpenStreetMap contributors" and "© OpenMapTiles" are required (AC38).
  - **Risk (Medium):** the instance is donation-funded with no SLA, so it could change or go away. Mitigation: the app degrades gracefully offline or on failure (AC39), and the tile source is swappable (Assumption 13). The architect re-confirms the FAQ and Terms of Service wording on commercial use at design time. If it does not allow commercial use, work stops for an owner decision.
- **Not used:**
  - OpenStreetMap Foundation tile servers: heavy-use app distribution is forbidden without permission.
  - Open-Meteo free API: non-commercial only, for weather and pollen.
  - Met Office DataHub: needs an API key.
  - Google Pollen / BreezoMeter: needs an API key.
  - Polleninformation.at: needs a key, non-commercial, and doesn't cover the UK.
- **Device GPS and Android background-location rules:** recording with the screen off needs a foreground recording notification. Google Play policy reviews background location use, which is a release-time risk for the owner's Play listing.
- **Risk (Low): guidance accuracy.** The thresholds are general guidance, and forecasts can be wrong. Mitigated by the persistent disclaimer (AC26) and the "never Go on missing data" rule.
- **Bootstrap dependency:** no stack exists yet. The architect proposes it (ADR 0001) and the owner approves it at the stack checkpoint. Task 1 is a walking skeleton.

## Sources
- MET Norway API Terms of Service: https://api.met.no/doc/TermsOfService and https://developer.yr.no/doc/TermsOfService (licence CC BY 4.0, User-Agent, proxy advice, 20 req/s, IP logging)
- MET Norway Locationforecast data model: https://api.met.no/doc/locationforecast/datamodel (variables incl. wind_speed_of_gust; gust not available beyond +60 h)
- MET Norway FAQ (generic User-Agents throttled): https://docs.api.met.no/doc/FAQ
- OpenStreetMap Foundation Tile Usage Policy: https://operations.osmfoundation.org/policies/tiles/
- OpenFreeMap: https://openfreemap.org/ (free, no keys, no registration, no cookies, no limits, FAQ on commercial use and attribution, MIT licence)
- OpenFreeMap source: https://github.com/hyperknot/openfreemap
- OpenMapTiles licence and attribution: https://github.com/openmaptiles/openmaptiles and https://openmaptiles.org/about/
- Open-Meteo terms (free API non-commercial only): https://open-meteo.com/en/terms
- Polleninformation.at data interface (key required, non-commercial): https://www.polleninformation.at/en/data-interface
- Vets Now heat-illness guidance (>20 °C), and FOUR PAWS UK asphalt study (25 °C air gives about 52 °C asphalt), as reported by Animal Friends: https://www.animalfriends.co.uk/dog/dog-advice/dog-maintenance-and-safety/the-danger-of-hot-pavements/
- Vets Now: https://www.vets-now.com/
