# Project profile

- Status: APPROVED
<!-- The first pipeline run (bootstrap) fills this in: the architect proposes it,
     the owner approves it at the stack checkpoint. After that it changes only
     through a pipeline run or a governance/* PR. -->

## Product
- Name: Dog Walk Buddy
- One-line description: a one-glance Go / Caution / Avoid "good time to walk?" check for UK dog owners, with private, offline walk logging and a single dog profile.
- Users: everyday UK dog owners with one dog, using the app quickly and often one-handed, outdoors. Some rely on TalkBack or large fonts.
- Platform(s): Android phones only (minSdk 29 / Android 10, targetSdk 36 / Android 16). English (UK) only. No backend.
- Application ID / package: `io.github.greavemrsvg.dogwalkbuddy`. The owner confirmed this at the stack checkpoint on 2026-10-19. It cannot change after the first Play release.

## Stack
- Language / framework: Kotlin 2.3.20 (AGP built-in Kotlin) with Jetpack Compose (BOM 2026.08.00, Material 3). Gradle 9.4.1, Android Gradle Plugin 9.2.1, JDK 17.
- Key libraries:
  - App: Room 2.8.4 (KSP 2.3.10), OkHttp 5.1.0, kotlinx.serialization-json 1.10.0, kotlinx-coroutines 1.10.2, Navigation Compose 2.9.7, Lifecycle 2.9.4, Activity Compose 1.11.0, Core KTX 1.17.0, MapLibre Native Android 13.6.1.
  - Tests: JUnit 4.13.2, Robolectric 4.16.1 (`sdk=34`), MockWebServer3 / okhttp-tls 5.1.0, AndroidX Test 1.7.0 / ext-junit 1.3.0, Compose ui-test.
  - Tooling: ktlint-cli 1.7.1, Android Lint, JaCoCo 0.8.13, diff-cover 10.2.0 (CI and local tool only).
- External services (no keys): MET Norway Locationforecast 2.0 `complete` (`api.met.no`) and OpenFreeMap tiles and style (`tiles.openfreemap.org`).
- Decision records:
  - `docs/adr/0001-technology-stack.md`
  - `docs/adr/0002-weather-source-and-client.md`
  - `docs/adr/0003-map-renderer-and-tile-source.md`
  - `docs/adr/0004-local-data-model-and-persistence.md`
  - `docs/adr/0005-walk-recording-foreground-service.md`

## Commands (every agent uses exactly these)
| Purpose | Command |
|---|---|
| Build | `./gradlew --no-daemon assembleDebug assembleRelease` |
| Lint | `./gradlew --no-daemon ktlintCheck lintDebug` |
| Unit tests | `./gradlew --no-daemon testDebugUnitTest` |
| Coverage | `bash scripts/coverage.sh <base-ref>` (for example `bash scripts/coverage.sh origin/feature/20261019-dog-walk-buddy-mvp`) |
| Performance | none (justified below) |
| Full CI locally | `bash scripts/ci.sh` |

The commands work as follows:
- **Unit tests** cover both plain JVM unit tests and Robolectric integration tests (Compose UI, Room, service, location). No emulator is needed.
- **Coverage:**
  - It runs `createDebugUnitTestCoverageReport` (JaCoCo XML at `app/build/reports/coverage/test/debug/report.xml`).
  - It then runs `diff-cover` against `<base-ref>` with `--fail-under=80`.
  - With no argument it uses `origin/$GITHUB_BASE_REF` when set. Otherwise it reports total coverage and skips the changed-lines gate.
- **Performance is none** because the performance NFRs need a real phone and network. They are run manually on the reference phone, a **Samsung Galaxy A35 5G**, using the procedures in `docs/design/20261019-dog-walk-buddy-mvp.md` (Test strategy, Manual checks). The results are recorded in `docs/perf/<run-id>/`.
- **`bash scripts/ci.sh`** runs Lint, Unit tests, Build and Coverage in that order. When `GITHUB_BASE_REF` is set (pull requests), it passes `origin/$GITHUB_BASE_REF` to the coverage script. GitHub Actions (`.github/workflows/ci.yml`) calls `bash scripts/ci.sh` with JDK 17 on `ubuntu-latest`, which has the Android SDK preinstalled.
- On Windows, run the commands from Git Bash.

- Version file(s): `version.properties` (`versionName`, `versionCode`), read by `app/build.gradle.kts`.
- Changed-lines coverage threshold: 80%

## Codebase notes
- Single module `:app`. Package root `io.github.greavemrsvg.dogwalkbuddy`.
  - `domain/`: pure Kotlin, no Android types. Rating engine, best window, sun times, distance, weekly totals, profile validation.
  - `data/`: weather client and validation, location adapters, Room database (schema in `app/schemas/`), repositories, photo store.
  - `service/`: the walk recording foreground service.
  - `ui/`: Compose screens and ViewModels, theme, navigation.
  - `map/`: the `RouteMap` slot, `MapConfig` (single tile-source constant) and the MapLibre adapter in `map/maplibre/`, which is excluded from coverage and checked manually.
- Manual DI through `AppContainer`. `Clock`, `ZoneId`, the weather base URL, the OkHttp client, location sources and connectivity are injected, so tests are deterministic.
- All user-facing text lives in `res/values/strings.xml`.
- Release signing comes from environment variables `DWB_KEYSTORE_PATH`, `DWB_KEYSTORE_PASSWORD`, `DWB_KEY_ALIAS` and `DWB_KEY_PASSWORD`. The release build is unsigned when they are absent. Keys are never stored in the repo.
