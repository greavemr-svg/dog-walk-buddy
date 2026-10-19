# Design: Dog Walk Buddy MVP (walk check, walk logging, dog profile)

- Run: `20261019-dog-walk-buddy-mvp`
- Spec: `docs/specs/20261019-dog-walk-buddy-mvp.md`
- Amended 2026-10-19: wind is rated on mean speed only, following the re-approved spec (commit 0d6637f) and the 2026-10-19 amendment in ADR 0002. Tasks, sizes, dependencies and the AC mapping are unchanged.
- Amended 2026-10-19: compileSdk raised from 36 to 37 (T1 finding, owner decision; ADR 0001 amendment). targetSdk 36 and minSdk 29 are unchanged. Tasks, sizes, dependencies and the AC mapping are unchanged.
- ADRs (all Accepted):
  - `docs/adr/0001-technology-stack.md`
  - `docs/adr/0002-weather-source-and-client.md`
  - `docs/adr/0003-map-renderer-and-tile-source.md`
  - `docs/adr/0004-local-data-model-and-persistence.md`
  - `docs/adr/0005-walk-recording-foreground-service.md`

## Approach
This is a single-module native Android app (`:app`), written in Kotlin with Jetpack Compose (ADR 0001). It is layered as the coding standards require:

- **Domain (pure Kotlin, no Android types).**
  - The rating engine, best-window finder and sunrise/sunset calculator.
  - Route distance, weekly totals and profile validation.
  - Nearly all business rules live here and are unit-tested with injected `Clock` and `ZoneId`.
- **Data.**
  - The MET Norway client and response validator (ADR 0002).
  - Location adapters (ADR 0005).
  - The Room database and repositories (ADR 0004).
  - The photo store and the delete-all use case.
- **Service.** `WalkRecordingService`: a non-exported foreground service of type `location` (ADR 0005).
- **UI.**
  - Compose screens, each with a ViewModel holding UI state.
  - Navigation Compose with type-safe routes.
  - The map is behind a `RouteMap` slot with a MapLibre implementation (ADR 0003).
- **Wiring.** `AppContainer` is created in `DogWalkBuddyApp`. It is manual DI that provides `Clock`, `ZoneId`, the OkHttp client, the base URL, location sources and the database, so tests swap in fakes.

### Screens and navigation
| Screen | Route | Reached from |
|---|---|---|
| Home | `home` (start) | Launch. Holds the check card at the top and the full-width **Start walk** button below it, so the check and Start walk are on the first screen and usable one-handed (NFR). Shows links to History, Dog profile, Privacy and About. |
| About this advice | `about` | Link on the check card and on Home. Lists thresholds, Vets Now, FOUR PAWS UK, the MET Norway attribution and open-source licences. |
| Recording | `recording` | Opened automatically by Start walk. Shows elapsed time, distance and **Stop**. If a walk is recording, Home shows "Walk in progress" and returns here. |
| History | `history` | Home link. |
| Walk detail | `walk/{id}` | Tap a history item. |
| Dog profile | `profile` | Home link and the check screen's "Set up your dog's profile" note. |
| Privacy | `privacy` | Home link, 1 tap (AC46 allows 2 or fewer). Holds "Delete all my data". |

### The check flow (single source of truth: `CheckViewModel`)
1. **The screen is opened, or refresh is tapped.** These are the only triggers (AC24). There is no `init` fetch in repositories, no WorkManager and no lifecycle observers that fetch.
2. **Permission check.** If `ACCESS_COARSE_LOCATION` is not granted, the state is `NeedsLocationPermission`, with an explanation and a Grant button. No request is made (AC25).
3. **Location.** `ApproximateLocationSource.current()` is called with a 10 s timeout. If it fails, go to step 6 (cache fallback).
4. **Fetch.** `CheckRepository.fetch(lat, lon)`:
   - rounds the coordinates to 2 decimals
   - calls MET `complete` through OkHttp with the HTTP cache, the identifying User-Agent and a 10 s call timeout
   - validates the response into `List<HourlyConditions>`
5. **Success.** The validated hours and their location are saved to `cached_check` with `fetchedAt = clock.now`. Then go to step 7.
6. **Failure** (offline, timeout, non-2xx, malformed). Load `cached_check`.
   - If it exists and `clock.now − fetchedAt < 3 h`, use it, labelled "Checked X h Y min ago" (AC21).
   - Otherwise the state is `CantCheck`, with retry and no rating (AC22).
7. **Rate.** Take the 12 hourly periods starting with the one that contains `clock.now`. For each hour:
   - compute daylight with `SunCalculator` (local sunrise and sunset at the rounded location)
   - call `RatingEngine.rate(hour, sensitivity, isDaylight)`
   - The current hour gives the headline result, and `BestWindowFinder` runs over the 12 results.
   - **Sensitivity** comes from `ProfileRepository.observeSensitivity()`. With no profile it is `NORMAL`, and the screen shows the "Using normal heat sensitivity…" note (AC45). The ViewModel **combines** the sensitivity Flow with the loaded hours, so a profile change re-rates with no network request (AC44).
   - If temperature, precipitation and mean wind are all missing for the current hour, the state is `CantCheck` (AC15).

### Rating engine (binding rules from the spec)
- `s = 0` for Normal and `s = 4` for Sensitive.
- The heat and pavement factors use `T < 20−s` for Go, `T < 25−s` for Caution, and Avoid otherwise.
- **Pavement** applies only when `isDaylight && cloud != null && cloud < 50`.
- **Rain:** `P < 0.5` is Go, `P ≤ 4` is Caution, and `P > 4` is Avoid.
- **Wind (mean speed M, mph):** `M ≥ 39` is Avoid (`DANGEROUS_WINDS`, "Dangerous winds"). `M ≥ 25` is Caution (`WINDY`, "Windy"). Otherwise Go. If M is missing, wind is not rated and the hour is flagged missing.
- **Cold:** `T ≤ 0` is Caution.
- **Overall:** the worst factor. Ties are broken by the precedence list `[PAVEMENT, HEAT, WIND, RAIN, COLD]`.
- **Missing data:** if any of T, P or M is missing and the result would be Go, the result is Caution with reason `DATA_MISSING`. Missing cloud cover only means "not sunny" for the pavement rule and does not count as missing data.
- **Reasons:** the domain returns a `Reason` enum. `strings.xml` holds the exact spec texts, and a Robolectric test pins each mapping.
- **Best window:**
  - Find the best rating among the 12 hours.
  - Find the longest consecutive run at that rating, and break ties by the earliest run.
  - The window runs from the start of the run to the end of its last hour.
  - The model is `Window(start, end, rating, startsNow)` or `NoGoodTime` (all Avoid).
  - `BestWindowFormatter` uses the device time format to give "Now until <end>", "<start>-<end>" and the "Best available: Caution" label.

### Walk flow
The recording service, location source and recovery are described in ADR 0005 and ADR 0004.

- **Distance** is stored in metres. It is shown in miles to 1 decimal place (1 mi = 1609.344 m).
- **Duration** is shown as `h:mm`, computed by flooring total seconds to minutes.
- **Weekly totals** (`WeeklyTotals.group(walks, zoneId)`):
  - Weeks run Monday 00:00 to Sunday 23:59:59 in the device zone.
  - A walk belongs to the week of its start time.
  - Totals sum the raw durations and metres, then format.
  - The "This week" summary is always present, showing 0 walks, 0:00 and 0.0 mi when the week is empty (AC37).

## Components touched
Everything is new. The package root is `io.github.greavemrsvg.dogwalkbuddy` (the applicationId is the same, and the owner confirmed it at the stack checkpoint on 2026-10-19).

| Component / module | Change |
|---|---|
| Root build: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `gradlew`, `gradlew.bat`, `gradle.properties`, `version.properties` | New Gradle 9.4.1 / AGP 9.2.1 project, version catalog, `ktlintCheck` and `ktlintFormat` tasks |
| `app/build.gradle.kts`, `app/proguard-rules.pro` | Android app module (minSdk 29, targetSdk 36, compileSdk 37 per the ADR 0001 amendment), Compose, KSP/Room, JaCoCo unit-test coverage, R8 for release, optional signing from environment variables |
| `app/src/main/AndroidManifest.xml`, `res/xml/network_security_config.xml`, `res/xml/data_extraction_rules.xml`, `res/xml/backup_rules.xml` | Hardened manifest: no cleartext, backup excluded, only `MainActivity` exported, profileinstaller receiver removed |
| `.../domain/rating/*`, `.../domain/window/*`, `.../domain/sun/*` | Rating engine, best window, sunrise and sunset |
| `.../domain/walk/*`, `.../domain/profile/*` | Distance calculator, weekly totals, profile validation |
| `.../data/weather/*` | `MetNorwayClient`, `MetResponseParser`, `WeatherConfig`, `HttpsOnlyInterceptor`, `UserAgentInterceptor` |
| `.../data/location/*` | `ApproximateLocationSource`, `WalkLocationSource` and their `LocationManager` adapters |
| `.../data/db/*` | `AppDatabase`, entities, DAOs, `app/schemas/1.json` |
| `.../data/repo/*` | `CheckRepository`, `WalkRepository`, `ProfileRepository`, `PhotoStore`, `DeleteAllDataUseCase`, `RecoverInterruptedWalkUseCase` |
| `.../service/WalkRecordingService.kt`, `RecordingStateHolder.kt` | Foreground recording |
| `.../ui/{theme,nav,home,check,about,recording,history,walkdetail,profile,privacy}/*` | Screens, ViewModels and theme (high-contrast tokens) |
| `.../map/RouteMap.kt`, `MapConfig.kt`, `RouteBounds.kt`, `.../map/maplibre/MapLibreRouteMap.kt` | Map slot and MapLibre adapter |
| `app/src/main/res/values/strings.xml`, `res/drawable/ic_rating_*.xml` | All user text and the rating icons |
| `app/src/test/**`, `app/src/test/resources/robolectric.properties`, `app/src/test/resources/met/*.json` | Unit and Robolectric integration tests, MET fixtures |
| `scripts/ci.sh`, `scripts/coverage.sh` (new) | Exact CI commands and the changed-lines coverage gate |
| `.gitignore`, `.gitattributes` | Android build outputs, `.venv-ci/`, `local.properties`; `*.webp binary` |
| `docs/project.md` | Filled in by this design (Status: PROPOSED) |

## Interfaces and data
- **Data model:** see ADR 0004.
  - Room v1 with the tables `walk`, `route_point`, `dog_profile` and `cached_check`.
  - Times are UTC epoch ms. Distances are in metres.
  - There is no migration in v1. Destructive migration is forbidden.
- **Domain types** (pure Kotlin):
  - `HourlyConditions(startUtc: Instant, temperatureC: Double?, precipitationMmPerHour: Double?, windMeanMph: Double?, cloudCoverPercent: Double?)`
  - `HeatSensitivity { NORMAL, SENSITIVE }`
  - `Rating { GO, CAUTION, AVOID }` (ordered)
  - `Reason { GOOD, WARM, TOO_HOT, PAVEMENT_WARM, PAVEMENT_TOO_HOT, RAIN, HEAVY_RAIN, WINDY, DANGEROUS_WINDS, FREEZING, DATA_MISSING }`
  - `HourRating(startUtc, rating, reason)`
  - `CheckOutcome = Rated(current: HourRating, window: BestWindow, fetchedAt: Instant, isCached: Boolean) | CantCheck`
  - `BestWindow = Window(startUtc, endUtc, rating, startsNow) | NoGoodTime`
  - `LocationFix(lat, lon, accuracyM, timeUtc)`
  - `WalkSummary(id, startUtc, durationSeconds, distanceM)`
  - `WeekGroup(weekStart: LocalDate, walks, count, totalSeconds, totalM)`
  - `ProfileInput(name, breed?)` → `ValidationResult` (`NAME_REQUIRED`, `NAME_TOO_LONG(30)`, `BREED_TOO_LONG(50)`)
- **Injection seams** (all in `AppContainer`):
  - `java.time.Clock`
  - `ZoneId`
  - `is24HourFormat: () -> Boolean`
  - `weatherBaseUrl: HttpUrl`
  - `OkHttpClient`
  - `ApproximateLocationSource`, `WalkLocationSource`
  - `ConnectivityStatus` (for AC39)
  - `RouteMap` composable
  - `AppDatabase` (in-memory in tests)
  - `filesDir` root for `PhotoStore`
- **External API:** MET Locationforecast 2.0 `complete` (ADR 0002) and OpenFreeMap style and tiles at `https://tiles.openfreemap.org/styles/liberty` (ADR 0003). These are the only hosts.
- **Manifest permissions:**
  - `INTERNET`, `ACCESS_NETWORK_STATE`
  - `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`
  - `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`
  - `POST_NOTIFICATIONS`
  - **No** `ACCESS_BACKGROUND_LOCATION` and **no** storage or media permissions. The Photo Picker needs none.
- **Version file:** `version.properties` (`versionName=0.1.0`, `versionCode=1`), read by `app/build.gradle.kts`. The User-Agent uses `BuildConfig.VERSION_NAME`.

### Provider terms and attribution (summary)
- **MET Norway:**
  - Identifying User-Agent `DogWalkBuddy/<version> (+https://github.com/greavemr-svg/dog-walk-buddy)`.
  - Coordinates of 4 decimals or fewer (2 are used).
  - `Expires` and `Last-Modified` honoured through the OkHttp HTTP cache, which sends conditional requests.
  - No polling or background requests. Requests are on demand only, which keeps volume far below 20 req/s in total.
  - Attribution "Weather data: MET Norway (CC BY 4.0)" on the check screen (AC27).
  - No use of "Yr" or implied endorsement.
- **OpenFreeMap:**
  - Commercial use is allowed ("Is commercial usage allowed? Yes." in the FAQ, re-confirmed 2026-10-19, see ADR 0003).
  - A unique User-Agent is sent.
  - No bulk download.
  - The visible attribution "© OpenStreetMap contributors · © OpenMapTiles" is on the map (AC38).
- **Guidance sources:** Vets Now and FOUR PAWS UK, named on About (AC26).
- **Open-source licences:** listed on the About screen (BSD-2 notice for MapLibre, Apache-2.0 and others).

## Risks and mitigations
| Risk | Mitigation |
|---|---|
| MET `complete` lacks temperature, precipitation, mean wind or cloud cover for UK points, or is not hourly for 12 h (Assumption 1) | Task 1 captures a live London response as a fixture, and a test asserts the fields. If they are missing, task 1 reports BLOCKED for an owner decision before task 2 starts (ADR 0002). The 2026-10-19 finding and owner decision are recorded in the ADR 0002 amendment. |
| MET throttles direct mobile calls ("should use a proxy") | Identifying UA, 2-decimal rounding (shared cache cells), HTTP cache, on-demand only. Graceful `CantCheck` and the 3 h cache. The owner revisits "no backend" if usage grows (spec risk). |
| OpenFreeMap changes or disappears (no SLA) | AC39 fallback message. Single `MapConfig` constant to swap the host (ADR 0003). |
| OpenFreeMap requests pass through Cloudflare (ToS) | Disclosed as additive wording in the Privacy text (task 8). The owner approved this wording at the stack checkpoint on 2026-10-19. |
| AGP 9 / Kotlin 2.3 / pinned versions fail to resolve or interoperate | Third-party Gradle plugins avoided (ktlint CLI, JaCoCo built in). Task 1 proves the whole dependency set, including MapLibre packaging and R8. If a version fails, BLOCKED and an ADR amendment, with no silent substitution. The 2026-10-19 finding (Compose 1.12.0 needs compileSdk 37) and owner decision are recorded in the ADR 0001 amendment. |
| CI runner must provide Android platform 37; verified on the T1 PR | compileSdk 37 was verified locally only. The T1 PR's CI run on `ubuntu-latest` is the evidence that the runner's preinstalled SDK has `platforms;android-37*` (or that AGP can install it). If that run fails because platform 37 is missing, the developer reports BLOCKED with the CI log; the agents do not edit `.github/`. **Owner action in that case:** add an SDK platform install step (for example `sdkmanager "platforms;android-37.0"` after the JDK setup) to `.github/workflows/ci.yml` through a `governance/*` PR, then re-run the T1 PR's CI. |
| Robolectric cannot run SDK 36 on CI's JDK 17 | `robolectric.properties` `sdk=34`. SDK 35/36 behaviour (edge-to-edge, foreground-service rules) is checked manually on the reference phone. The owner may move CI to JDK 21. |
| MapLibre native renderer cannot be tested in CI, and Vulkan issues on some devices | Thin adapter excluded from coverage and verified manually (AC38). Fallback to the OpenGL artifact through an ADR 0003 amendment. |
| Battery over 8 %/h | 3 s GPS interval, no wake locks beyond the foreground service, UI ticker only while visible. If the manual result is over 8 %, raise the interval to 5 s. |
| Process death mid-walk loses the route | Every accepted fix is written in a transaction. Recovery runs on next launch (AC34). |
| Data leaks through backup or device transfer | `allowBackup=false` plus both rule files exclude all domains. Test plus manual (AC48). |
| Merged dependency adds an exported component | Robolectric test asserts that the only exported component is `MainActivity`. The profileinstaller receiver is removed with `tools:node="remove"`. |
| Windows authoring loses the `gradlew` executable bit or uses CRLF | `.gitattributes` already forces LF. Task 1 commits `gradlew` with `git update-index --chmod=+x gradlew`, and `ci.sh` invokes `bash ./gradlew` for robustness. |
| Photo is a huge or hostile image | Bounds-decode first, refuse over 20 MB or 8000 px, downscale, re-encode to JPEG (EXIF stripped), app-generated file name. |
| Disclaimer pushed below the fold | Layout puts the disclaimer directly under the rating and reason. A test asserts it is displayed without scrolling at default font on a 411×891 dp screen. |

## Security considerations
Per `docs/governance/security-standards.md`:

- **Transport:**
  - `res/xml/network_security_config.xml` sets `<base-config cleartextTrafficPermitted="false">` with system trust anchors only. The manifest also sets `android:usesCleartextTraffic="false"`.
  - A `<debug-overrides>` block trusts user CAs **in debuggable builds only**, so the AC49 proxy check can be run on a debug build. Release builds never trust user CAs.
  - Certificate validation is never disabled. There is no custom `TrustManager` or `HostnameVerifier`.
  - `HttpsOnlyInterceptor` rejects non-HTTPS URLs on the shared OkHttp client, which MapLibre also uses.
- **No exported components** beyond the launcher `MainActivity`:
  - `WalkRecordingService` is `exported="false"`.
  - Room and AndroidX Startup providers are not exported.
  - `androidx.profileinstaller.ProfileInstallReceiver` (exported, DUMP-protected) is removed through manifest merger, so the spec's stricter rule holds.
  - A test checks the merged manifest through `PackageManager`.
  - There are no deep links and no intent filters other than `MAIN`/`LAUNCHER`.
- **Backup exclusion:** `allowBackup=false`, plus `fullBackupContent` and `dataExtractionRules` excluding every domain for cloud backup and device transfer (ADR 0004, AC48).
- **Data at rest:** app-private internal storage only (`filesDir`, `cacheDir`, the database). Nothing goes to external or shared storage. There is no extra encryption (Assumption 12). The security review may recommend more.
- **Input validation:**
  - MET responses: size cap of 2 MiB, strict nullable schema, range checks per field. Malformed values are "missing", and a malformed document is a failed check. Never a crash, never Go (ADR 0002).
  - Map style and tiles are parsed by MapLibre and treated as untrusted. A load failure gives the AC39 message.
  - Profile text is trimmed and length-checked (name 1-30, breed 0-50, AC42). The domain validator rejects control characters.
  - Photo: content URIs come from the system Photo Picker only. The image is bounds-checked, re-encoded and stored under an app-generated name, never a path from input.
  - No SQL is built from input. Room uses parameterised queries.
- **Least privilege:**
  - Coarse location is used only for the check, and fine location only for recording.
  - No background-location permission and no storage permissions.
  - Permissions are requested in context.
- **Secrets:** none exist or are needed. Release signing comes from environment variables or GitHub secrets (`DWB_KEYSTORE_*`). `*.jks` and `*.keystore` are already git-ignored, and `local.properties` is added to `.gitignore`.
- **Release hardening:** `isMinifyEnabled = true` and `isShrinkResources = true` for release. `debuggable` is false.
- **Logging:** no coordinates, profile values, routes or response bodies are logged. Logs carry only status codes and error classes.
- **Third parties:** no analytics, crash reporting or ads SDK. MapLibre has no telemetry. AC49 is checked manually with a proxy.
- **Personal data in tests:** fixtures use public forecast data and synthetic routes and profiles. "Biscuit" and "Cockapoo" are fictional values from the spec. The photo fixture is a generated solid-colour image.

## Test strategy
All automated tests run on the JVM with `./gradlew testDebugUnitTest`: plain JUnit for the domain, and Robolectric for Android, Room, service and Compose. CI needs no emulator. Test names reference ACs, for example `AC8_wind_thresholds_in_mph`.

### Determinism (injection)
- **Clock and time zone:**
  - `java.time.Clock` and `ZoneId` are injected everywhere.
  - Tests use `Clock.fixed` or a small `MutableClock` that implements `Clock`, for AC21 (2 h 59 min), AC22 (3 h 0 min) and AC29 (advance 10 min).
  - `ZoneId.of("Europe/London")` is used for AC36 week boundaries.
  - `is24HourFormat` is injected so the AC16-AC18 texts are fixed.
- **Network:**
  - `weatherBaseUrl` and `OkHttpClient` are injected.
  - Integration tests use `mockwebserver3` with HTTPS (`okhttp-tls` `HeldCertificate`) and a client that trusts only that test certificate.
  - `RecordedRequest` lets tests assert the path, query (`lat=51.51&lon=-0.13`), scheme, headers (only `User-Agent` identifies the app, no other identifiers) and request count (AC23, AC24, AC44).
  - Failure modes are simulated with `SocketPolicy`/`MockResponse` (disconnect, 500, 429, malformed body, timeout).
  - The skeleton's real MET fixture is reused as the success body.
- **Location:**
  - Domain and ViewModel tests use `FakeApproximateLocationSource` and `FakeWalkLocationSource` (backed by a `MutableSharedFlow`).
  - Adapter and service tests use Robolectric `ShadowLocationManager.simulateLocation`, so they exercise the real `LocationManager` path.
  - AC32 generates a synthetic 2.000 km track (points about 5 m apart, accuracy 5-10 m, small seeded deterministic noise) and asserts the result is within ±2 %.
- **Connectivity:** a `ConnectivityStatus` fake for AC39.
- **Map:** a fake `RouteMap` records the route and bounds it was given.
- **Database:** `Room.inMemoryDatabaseBuilder` with `allowMainThreadQueries` only in tests. AC34 uses a file-backed database that is closed and reopened to simulate process death.
- **Coroutines:** `StandardTestDispatcher` and `runTest` for virtual time. Dispatchers are injected, never hard-coded.

### Unit tests (domain, pure JVM)
- `RatingEngine`: AC1-AC15, every boundary in the spec table, the tie precedence and the missing-data rules.
- `BestWindowFinder`: AC16-AC19. `BestWindowFormatter`: AC17 and AC18 texts (Robolectric for string resources).
- `SunCalculator`: against published sunrise and sunset times for London on fixed dates (±2 min). Also the edge where an hour straddles sunrise. The rule is evaluated at the hour's start time and documented in code.
- `MetResponseParser`: valid fixture, missing fields, out-of-range values, wrong types, truncated JSON, oversize body, m/s to mph conversion.
- `CoordinateRounding`: AC23 values and negative halves.
- `RouteDistanceCalculator`: accuracy filter, jitter suppression, haversine.
- `WeeklyTotals`: AC36, AC37, plus the DST change week.
- `ProfileValidator`: AC42.
- `CachePolicy`: AC21 and AC22 boundaries at 2:59:59 and 3:00:00.
- `ContrastTest`: computes the WCAG 2.1 contrast ratio for every theme token pair in light and dark themes. It requires 4.5:1 for all text and 7:1 for rating, reason and Start/Stop (AC51).

### Integration tests (Robolectric, JVM)
- **Manifest and security:**
  - only `MainActivity` exported
  - `FLAG_ALLOW_BACKUP` not set
  - the `data_extraction_rules` and `backup_rules` XML exclude all domains (AC48)
  - the cleartext-disabled config is referenced
- **`CheckRepository` and `CheckViewModel` with MockWebServer:** AC21-AC25, AC44, AC45, plus a malformed body giving `CantCheck` or "data missing" and never Go.
- **Compose screen tests** (`createAndroidComposeRule`) with injected fakes: every screen AC (AC20, AC26, AC27, AC35, AC38 attribution, AC39-AC47, AC50-AC53).
  - **Accessibility helper `assertAccessibleScreen()`**, run on every screen:
    - every node with a click action has a non-empty `contentDescription` or text
    - every clickable node is at least 48×48 dp (`assertTouchHeightIsAtLeast` and `assertTouchWidthIsAtLeast`)
    - Start and Stop are at least 72 dp high and fill the max width
    - no `Text` has visual overflow at `fontScale = 2.0` (`getTextLayoutResult` and `hasVisualOverflow`, AC52)
    - the rating node's merged semantics announce "<Rating>. <Reason>." (AC50)
  - **AC53:** each rating renders a distinct icon resource and label text, asserted through semantics. The colour is not the only difference.
- **Service tests** (`Robolectric.buildService`):
  - `startForeground` called with type location and an ongoing notification (AC28)
  - fixes recorded while the clock advances 10 min with no activity (AC29)
  - stop saves the walk and removes the notification (AC30)
  - no network calls during recording, with the MockWebServer request count at 0 (AC31, AC24)
  - the 2 km track (AC32)
  - no fine permission means no start (AC33)
- **Recovery:** a `RECORDING` row plus points in a reopened database, then `RecoverInterruptedWalkUseCase`, gives a walk saved at the last fix (AC34).
- **Delete:** single delete and cancel (AC40). Delete-all clears the tables, the photo directory, the weather HTTP cache and the cached check, and calls the map-cache clearer through a fake (AC47).

### Changed-lines coverage (80 %)
- `app/build.gradle.kts` sets `debug { enableUnitTestCoverage = true }`.
- JaCoCo is configured with `isIncludeNoLocationClasses = true` and `excludes = ["jdk.internal.*"]`, so Robolectric-loaded classes are counted.
- `scripts/coverage.sh <base-ref>`:
  1. runs `createDebugUnitTestCoverageReport`
  2. installs `diff-cover==10.2.0` in `.venv-ci`
  3. runs `diff-cover app/build/reports/coverage/test/debug/report.xml --compare-branch=<base-ref> --src-roots app/src/main/java --fail-under=80`
- In CI on a pull request, `ci.sh` passes `origin/$GITHUB_BASE_REF`. The script unshallows the checkout first. Locally the tester runs `bash scripts/coverage.sh origin/feature/20261019-dog-walk-buddy-mvp`.
- The only exclusions are the generated-code patterns and the `map/maplibre` adapter listed in ADR 0001.

### Manual checks (reference phone: Samsung Galaxy A35 5G, Android 14 or later, release-config build signed with a local test key)
The owner confirmed the Samsung Galaxy A35 5G as the reference phone at the stack checkpoint on 2026-10-19. The performance agent records the device model and OS build with every result.

| Check | Procedure | Pass |
|---|---|---|
| AC29 | Start a walk, lock the phone, pocket it, walk 10 min, unlock | Duration of 10 min or more. The route covers the path walked. |
| AC31 | Airplane mode with GPS on. Walk at least 5 min. Stop. | Walk in history with non-zero distance. No error shown. |
| AC38 | Online, open a real recorded walk | Whole route visible without panning. Attribution readable. |
| AC43 | Pick a gallery photo, delete it in the gallery, restart the app; then remove the photo | Photo still shown; then gone |
| AC48 | With data present, run `adb shell bmgr backupnow <pkg>` (or the cloud backup settings), reinstall with restore, and try device-to-device transfer | No walks, profile or cached check restored |
| AC49 | Debug build with mitmproxy or Charles CA (user CA trusted only in debug). Use check, record, history, map, profile once each. | Hosts are only `api.met.no` and `tiles.openfreemap.org`, all HTTPS |
| AC50 | TalkBack through check, Start/Stop, history, detail, profile, privacy | Every control spoken. "Caution. Windy." style announcements. |
| AC52 | Font size and display size at maximum | Rating, reason, window and Start/Stop fully readable on the check and recording screens |
| NFR check time | Fresh launch on typical 4G, 20 runs, stopwatch or screen recording from launch to result | 90th percentile of 5 s or less |
| NFR Start walk | Screen recording at 60 fps, tap to "Recording" state | 1 s or less |
| NFR history | Debug build: History overflow, then "Seed 500 sample walks" (debug source set only, synthetic data), then cold-open History 10 times | 1 s or less each |
| NFR battery | 100 % charge, screen off, record 60 min outdoors, read the battery % | Drop of 8 % or less |

**Performance command: none.** The performance NFRs need a real device and a real network, so they are run manually on the reference phone with the procedures above. The performance agent records the results in `docs/perf/20261019-dog-walk-buddy-mvp/` as a proposed baseline. A Macrobenchmark module can be added in a later run through an ADR.

## Task breakdown
| # | Title | ACs | Likely files | Depends on | Size |
|---|---|---|---|---|---|
| 1 | Walking skeleton: Gradle/AGP project, hardened manifest, CI commands, coverage gate, MET field verification | AC48 | `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `gradlew(.bat)` (exec bit), `gradle.properties`, `version.properties`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `AndroidManifest.xml`, `res/xml/{network_security_config,data_extraction_rules,backup_rules}.xml`, `DogWalkBuddyApp.kt`, `AppContainer.kt`, `MainActivity.kt`, `ui/theme/*`, `ui/home/HomeScreen.kt` (placeholder title only), `app/src/test/.../ManifestSecurityTest.kt`, `.../SmokeTest.kt`, `app/src/test/resources/robolectric.properties`, `app/src/test/resources/met/complete_london_51.51_-0.13.json`, `.../data/weather/MetFixtureContractTest.kt`, `scripts/ci.sh`, `scripts/coverage.sh`, `.gitignore`, `.gitattributes` | — | M |
| 2 | Rating engine, sunrise/sunset and best window (domain) | AC1-AC19 | `domain/rating/{RatingEngine,Rating,Reason,HeatSensitivity,HourlyConditions}.kt`, `domain/sun/SunCalculator.kt`, `domain/window/{BestWindowFinder,BestWindow}.kt`, `ui/check/BestWindowFormatter.kt`, `res/values/strings.xml` (reason texts), tests | 1 | M |
| 3 | Weather client, approximate location and cached check | AC21, AC22, AC23, AC24 | `data/weather/{MetNorwayClient,MetResponseParser,WeatherConfig,HttpsOnlyInterceptor,UserAgentInterceptor,CoordinateRounding}.kt`, `data/location/{ApproximateLocationSource,LocationManagerApproximateSource}.kt`, `data/db/{AppDatabase,CachedCheckEntity,CachedCheckDao}.kt`, `app/schemas/*`, `data/repo/CheckRepository.kt`, `domain/check/CachePolicy.kt`, `AppContainer.kt`, tests | 1 | M |
| 4 | Dog profile with photo and validation | AC41, AC42, AC43, AC50, AC51 | `data/db/{DogProfileEntity,DogProfileDao}.kt`, `data/repo/{ProfileRepository,PhotoStore}.kt`, `domain/profile/ProfileValidator.kt`, `ui/profile/{ProfileScreen,ProfileViewModel}.kt`, `ui/nav/*`, `ui/a11y/AccessibilityAssertions.kt` (test helper in `src/test`), strings, tests | 3 (database exists) | M |
| 5 | Check screen and About this advice | AC15, AC20, AC21, AC22, AC25, AC26, AC27, AC44, AC45, AC50, AC51, AC52, AC53 | `ui/check/{CheckCard,CheckViewModel,CheckUiState}.kt`, `ui/about/AboutScreen.kt` (thresholds, sources, attribution, OSS licences), `ui/home/HomeScreen.kt`, `res/drawable/ic_rating_{go,caution,avoid}.xml`, `ui/theme/Color.kt`, strings, `ContrastTest.kt`, tests | 2, 3, 4 | M |
| 6 | Walk recording foreground service and recording screen | AC28, AC29, AC30, AC31, AC32, AC33, AC34, AC50, AC51, AC52 | `service/{WalkRecordingService,RecordingStateHolder,RecordingNotification}.kt`, `data/location/{WalkLocationSource,GpsWalkLocationSource}.kt`, `domain/walk/RouteDistanceCalculator.kt`, `data/db/{WalkEntity,RoutePointEntity,WalkDao}.kt`, `data/repo/{WalkRepository,RecoverInterruptedWalkUseCase}.kt`, `ui/recording/*`, `ui/home/HomeScreen.kt` (Start walk), `AndroidManifest.xml` (service, permissions), `DogWalkBuddyApp.kt` (recovery on start), tests | 5 | M |
| 7 | Walk history, weekly totals and delete | AC30, AC35, AC36, AC37, AC40, AC50, AC51 | `domain/walk/{WeeklyTotals,DurationFormat,DistanceFormat}.kt`, `ui/history/{HistoryScreen,HistoryViewModel}.kt`, `data/repo/WalkRepository.kt` (delete, observe summaries), `app/src/debug/.../DebugSeedWalks.kt` (synthetic 500-walk seeding for the NFR), strings, tests | 6 | M |
| 8 | Walk detail map, Privacy and "Delete all my data", cross-feature checks | AC24, AC38, AC39, AC46, AC47, AC48, AC49, AC50, AC51 | `map/{RouteMap,MapConfig,RouteBounds}.kt`, `map/maplibre/MapLibreRouteMap.kt`, `ui/walkdetail/{WalkDetailScreen,WalkDetailViewModel}.kt`, `data/net/ConnectivityStatus.kt`, `ui/privacy/{PrivacyScreen,PrivacyViewModel}.kt`, `data/repo/DeleteAllDataUseCase.kt`, `AppContainer.kt` (shared OkHttp client for MapLibre), strings, tests, manual-check record in the PR body | 5, 7 | M |

### Task notes (what "done" means beyond the ACs)
- **Task 1 (walking skeleton).** It must merge before any other task starts.
  - **Scaffold and build:**
    - The full ADR 0001 catalog is declared, and every runtime library is on the app classpath, so resolution, R8 and MapLibre packaging are proven once.
    - `assembleDebug` and an unsigned `assembleRelease` (minified) both pass.
  - **Code:**
    - One Compose `MainActivity` showing the app title on a placeholder Home screen.
    - The `AppContainer` skeleton holding `Clock` and `ZoneId`.
    - One passing `SmokeTest`.
    - `ManifestSecurityTest` covering AC48 config, only-launcher-exported and the cleartext config.
  - **Tooling:**
    - `ktlintCheck`, `ktlintFormat` and `lintDebug` pass.
    - `scripts/coverage.sh` produces the JaCoCo XML and the diff-cover gate.
    - `scripts/ci.sh` is rewritten to run exactly: Lint, Unit tests, Build, then Coverage with the base ref when `GITHUB_BASE_REF` is set.
    - `bash scripts/ci.sh` passes in GitHub Actions on the task PR.
  - **Assumption 1 check:**
    - Capture the MET `complete` response for 51.51, −0.13 with the app User-Agent and commit it as a fixture.
    - Add `Assumption1_met_complete_has_hourly_temp_precip_wind_cloud_for_uk_point` in `MetFixtureContractTest`. It asserts that the first 12 timeseries entries are 1 hour apart and each has `air_temperature`, `wind_speed` and `cloud_area_fraction` in `instant.details`, and `precipitation_amount` in `next_1_hours.details` (ADR 0002).
    - Record the capture UTC time and the HTTP status in the PR.
    - **If temperature, precipitation, mean wind or cloud cover is absent, or the data is not hourly for 12 h, stop and report BLOCKED for the owner** (spec Assumption 1).
  - **Repository housekeeping:**
    - `gradlew` is committed executable.
    - `.gitignore` adds `build/`, `.gradle/`, `local.properties`, `.venv-ci/` and `*.apk`/`*.aab`.
  - **Not included:** the manual part of AC48 is done in task 8, once real data exists.
- **Task 3** adds the Room database with only `cached_check`. Later tasks add tables, and because the app is unreleased they stay on schema version 1. Each task re-exports `app/schemas/1.json`. A migration is required only after the first release (ADR 0004).
- **Task 4.** With no profile, the check uses Normal. Task 5 wires the sensitivity Flow into the check.
- **Task 5** owns the shared high-contrast theme tokens and `ContrastTest`. Later screens must use those tokens.
- **Task 8**:
  - Adds the Cloudflare disclosure wording to the Privacy text (ADR 0003).
  - Runs an end-to-end Robolectric test: open history, profile, record and background with the MockWebServer request count at 0, then open the check and see exactly 1 request (AC24).
  - Records the manual results for AC38, AC48 and AC49 in the PR.
  - The tester or performance agent runs the remaining manual checks (AC29, AC31, AC43, AC50, AC52 and the NFRs) on the reference phone before G4.

## Traceability check
| AC | Task(s) |
|---|---|
| AC1 | 2 |
| AC2 | 2 |
| AC3 | 2 |
| AC4 | 2 |
| AC5 | 2 |
| AC6 | 2 |
| AC7 | 2 |
| AC8 | 2 |
| AC9 | 2 |
| AC10 | 2 |
| AC11 | 2 |
| AC12 | 2 |
| AC13 | 2 |
| AC14 | 2 |
| AC15 | 2, 5 |
| AC16 | 2 |
| AC17 | 2 |
| AC18 | 2 |
| AC19 | 2 |
| AC20 | 5 |
| AC21 | 3, 5 |
| AC22 | 3, 5 |
| AC23 | 3 |
| AC24 | 3, 8 |
| AC25 | 5 |
| AC26 | 5 |
| AC27 | 5 |
| AC28 | 6 |
| AC29 | 6 |
| AC30 | 6, 7 |
| AC31 | 6 |
| AC32 | 6 |
| AC33 | 6 |
| AC34 | 6 |
| AC35 | 7 |
| AC36 | 7 |
| AC37 | 7 |
| AC38 | 8 |
| AC39 | 8 |
| AC40 | 7 |
| AC41 | 4 |
| AC42 | 4 |
| AC43 | 4 |
| AC44 | 5 |
| AC45 | 5 |
| AC46 | 8 |
| AC47 | 8 |
| AC48 | 1, 8 |
| AC49 | 8 |
| AC50 | 4, 5, 6, 7, 8 |
| AC51 | 4, 5, 6, 7, 8 |
| AC52 | 5, 6 |
| AC53 | 5 |

All of AC1-AC53 are mapped. Every task has at least one AC. There are 8 tasks.
