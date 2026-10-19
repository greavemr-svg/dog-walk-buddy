# ADR 0001: Technology stack for Dog Walk Buddy (Android, Kotlin, Jetpack Compose)

- Status: Accepted
- Date: 2026-10-19
- Run: `20261019-dog-walk-buddy-mvp`

## Context
The spec (`docs/specs/20261019-dog-walk-buddy-mvp.md`) asks for an Android phone app, English (UK) only, with:
- A weather "walk check" (MET Norway, HTTPS, no key, no backend).
- Walk recording that keeps running with the screen off and with no network (foreground location).
- A route map that renders OpenFreeMap **vector** tiles.
- Local-only storage (walks, routes, profile, photo, cached check), excluded from backup.
- TalkBack support, 200 % font scaling and strict contrast.
- No accounts, analytics or crash reporting.

The owner builds through an agent pipeline and reviews on GitHub. The stack must be mainstream and well documented, and it must build, lint and test on the existing `ubuntu-latest` CI job (`.github/workflows/ci.yml`). That job installs JDK 17 when `gradlew` exists and then runs `bash scripts/ci.sh`. There is no emulator in CI, so UI and integration tests must run on the JVM. Cost must be zero: no paid services or SDKs.

`docs/governance/coding-standards.md` already assumes Kotlin, ktlint, coroutines, ViewModels and repositories on Android.

## Options considered
1. **Kotlin + Jetpack Compose, native Android (Gradle/AGP)**
   - Pros:
     - Google's recommended stack and matches the coding standards.
     - First-class foreground services, location, photo picker and backup rules.
     - Compose semantics give testable TalkBack labels, touch sizes and font scaling.
     - Robolectric runs UI, Room and service tests on the JVM in CI.
     - MapLibre Native has a maintained Android SDK for vector tiles.
   - Cons:
     - Android only. Acceptable, because the spec is Android-only.
2. **Flutter (Dart)**
   - Pros: a good UI toolkit, and `maplibre_gl` exists.
   - Cons:
     - Background location needs third-party plugins with mixed maintenance.
     - Backup rules, foreground-service types and manifest hardening still need native Android code, so there are two languages to maintain.
     - The coding standards are written for Kotlin.
     - JVM-side integration testing of services is weaker.
3. **React Native (TypeScript)**
   - Pros: a large ecosystem.
   - Cons:
     - Background location and MapLibre need native modules.
     - Two toolchains (Node and Gradle) in CI.
     - A bigger dependency surface to vet against the "no analytics" rule.
     - Accessibility checks for font scaling are harder to automate.
4. **Kotlin + Android Views (XML)**
   - Pros: mature.
   - Cons: the legacy UI toolkit, more boilerplate, and weaker semantics-based testing than Compose. There is no benefit for a new app.

## Decision
Option 1: **native Android in Kotlin with Jetpack Compose**, built with Gradle and AGP.

It is the only option where every hard requirement can be met with first-party APIs:
- foreground location service
- `dataExtractionRules`
- network security config
- Photo Picker
- semantics-based accessibility tests

It also runs fully on the JVM in the existing CI job.

### Platform levels
| Setting | Value | Why |
|---|---|---|
| `minSdk` | **29** (Android 10) | `foregroundServiceType="location"` exists from 29. Covers the large majority of UK phones in use in 2026. |
| `targetSdk` | **36** (Android 16) | The Google Play requirement for new apps and updates from 31 Aug 2026. |
| `compileSdk` | **36** | Matches target. AGP 9.2 supports up to 36.1. |
| JVM target / toolchain | **17** | The minimum for AGP 9.2 and Gradle 9.4.1. Matches the JDK 17 that CI installs. |

### Architecture choices without a new library
- **Dependency injection: manual constructor injection.** A single `AppContainer` is created in `DogWalkBuddyApp`.
  - Alternatives: Hilt (adds KAPT/KSP processing and Gradle plugin coupling) and Koin (runtime service locator).
  - The app has about ten collaborators, so manual DI is simpler and keeps test wiring explicit (fake clock, fake location, MockWebServer base URL).
- **Location: platform `LocationManager`** through `androidx.core` `LocationManagerCompat`. Google Play Services is not used. See ADR 0005.
- **Sunrise and sunset: computed on the device** using the NOAA solar-position equations in pure Kotlin domain code.
  - Alternative: the MET Norway Sunrise 3.0 API, which would be a second network call, would need caching, and would not work when the forecast is cached.
  - Computing locally is deterministic and offline, and adds no host.
- **Images: platform `BitmapFactory` / `ImageDecoder`** for decoding, downscaling and re-encoding the profile photo. Re-encoding also strips EXIF, including any GPS tag.
  - Alternative: Coil, which is not needed for one image.
- **Icons: three vector drawables in `res/drawable`** for the Go, Caution and Avoid icons.
  - Alternative: `material-icons-extended`, which is large and not needed.
- **Changed-lines coverage: JaCoCo, bundled with Gradle and AGP**, plus `diff-cover`.
  - Alternative: Kover. Its AGP 9 built-in-Kotlin support could not be confirmed, and AGP's built-in `enableUnitTestCoverage` already produces a JaCoCo XML report.
- **ktlint: run as the official `ktlint-cli` jar** through a Gradle `JavaExec` task (`ktlintCheck`, `ktlintFormat`). This is the method ktlint documents for use without a plugin.
  - Alternatives: `org.jlleitschuh.gradle.ktlint` and Spotless. Neither has confirmed support for AGP 9 built-in Kotlin, and the CLI jar avoids coupling the build to a third-party plugin.

### Pinned versions
All versions live in `gradle/libs.versions.toml` and are referenced only through the catalog. No `+`, ranges or snapshots are allowed.

If a pinned version fails to resolve or is incompatible during task 1, the developer stops and reports BLOCKED. The architect then amends this ADR. There is no silent substitution.

Gradle's conflict resolution may raise a transitive AndroidX version, for example Lifecycle pulled in by the Compose BOM. Task 1 records the resolved versions in its PR, using `./gradlew :app:dependencies --configuration releaseRuntimeClasspath`.

#### Build tooling
| Component | Version | Licence | Maintenance | Alternatives considered | Why |
|---|---|---|---|---|---|
| Gradle (wrapper) | 9.4.1 | Apache-2.0 | Gradle Inc., monthly releases | Maven/Bazel (no AGP support or too heavy) | Required by AGP 9.2 (minimum and default 9.4.1) |
| Android Gradle Plugin | 9.2.1 | Apache-2.0 | Google, released with Android Studio (9.2.0 Apr 2026, 9.2.1 patch) | AGP 8.13 (previous major, end of feature work) | Current stable. Built-in Kotlin support, so the `kotlin-android` plugin is not applied. |
| Kotlin (compiler, via AGP built-in Kotlin; Compose and serialization compiler plugins) | 2.3.20 | Apache-2.0 | JetBrains, regular releases (2.3.20 in 2026) | Java (verbose, against the standards) | Language mandated by the coding standards |
| `org.jetbrains.kotlin.plugin.compose` | 2.3.20 | Apache-2.0 | JetBrains, ships with Kotlin | none (required for Compose) | Compose compiler |
| `org.jetbrains.kotlin.plugin.serialization` | 2.3.20 | Apache-2.0 | JetBrains, ships with Kotlin | Moshi codegen, Gson (reflection, no null-safety) | Compile-time JSON adapters |
| KSP (`com.google.devtools.ksp`) | 2.3.10 | Apache-2.0 | Google; 2.3.6+ required for AGP 9 built-in Kotlin | KAPT (deprecated path, slower) | Room annotation processing |

#### App runtime
| Library | Version | Licence | Maintenance | Alternatives considered | Why |
|---|---|---|---|---|---|
| Compose BOM `androidx.compose:compose-bom` (ui, foundation, material3, ui-tooling-preview; ui-tooling debug only) | 2026.08.00 | Apache-2.0 | Google, monthly BOMs | Android Views | Declarative UI with a semantics tree for accessibility tests |
| `androidx.activity:activity-compose` | 1.11.0 | Apache-2.0 | Google | none | `setContent`, Photo Picker (`PickVisualMedia`) and permission result contracts |
| `androidx.core:core-ktx` | 1.17.0 | Apache-2.0 | Google | none | `LocationManagerCompat`, `NotificationCompat`, `ServiceCompat` |
| `androidx.lifecycle:lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`, `lifecycle-service` | 2.9.4 | Apache-2.0 | Google | none | ViewModels, `collectAsStateWithLifecycle`, `LifecycleService` |
| `androidx.navigation:navigation-compose` | 2.9.7 (Jan 2026 stable) | Apache-2.0 | Google | Navigation 3 (newer API, less documentation); hand-rolled state navigation | Mainstream type-safe navigation for about 8 screens |
| `androidx.room:room-runtime`, `room-ktx`, `room-compiler` (KSP) | 2.8.4 | Apache-2.0 | Google; 2.8 gets patch releases. Room 3.0 was only RC in June 2026. | SQLDelight; DataStore + JSON files (no queries or transactions for routes) | Typed SQLite with Flows and transactions. See ADR 0004. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.10.2 | Apache-2.0 | JetBrains | RxJava | Async work as the standards require (no `GlobalScope`) |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.10.0 (Jan 2026, Kotlin 2.3 based) | Apache-2.0 | JetBrains | Moshi, Gson, `org.json` | Strict, null-safe decoding of the untrusted MET response. Also used to store the cached forecast. |
| `com.squareup.okhttp3:okhttp` | 5.1.0 | Apache-2.0 | Square/Block, active (5.x stable since Jul 2025) | `HttpURLConnection` (no HTTP cache or conditional requests to honour MET caching headers); Ktor client (more modules); Retrofit (not needed for one endpoint) | HTTP disk cache that honours `Expires` and `Last-Modified`, call timeouts, and MockWebServer for tests. See ADR 0002. |
| `org.maplibre.gl:android-sdk` | 13.6.1 | BSD-2-Clause (bundles OSS notices) | MapLibre organisation, monthly releases, Linux Foundation project | osmdroid (raster only, cannot render OpenFreeMap vector tiles); Google Maps SDK (needs an API key, so not allowed); Mapbox (key and licence terms) | The only maintained, keyless Android renderer for OpenFreeMap vector styles. No telemetry. See ADR 0003. |

#### Test (test source sets only)
| Library | Version | Licence | Maintenance | Alternatives considered | Why |
|---|---|---|---|---|---|
| `junit:junit` | 4.13.2 | EPL-1.0 | Stable, maintenance mode | JUnit 5 (Robolectric and Compose rules need JUnit 4) | Test runner |
| `org.robolectric:robolectric` | 4.16.1 | MIT | Google and community, active | Emulator instrumented tests (no emulator in CI) | Compose UI, Room, Service and `LocationManager` tests on the JVM |
| `androidx.test:core-ktx` | 1.7.0 | Apache-2.0 | Google | none | `ApplicationProvider`, scenarios |
| `androidx.test.ext:junit-ktx` | 1.3.0 | Apache-2.0 | Google | none | `AndroidJUnit4` runner |
| `androidx.compose.ui:ui-test-junit4` and `ui-test-manifest` (debug) | from the Compose BOM | Apache-2.0 | Google | Espresso alone | Semantics assertions: labels, `assertTouchHeightIsAtLeast` and font-scale tests |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.10.2 | Apache-2.0 | JetBrains | none | Virtual time, `runTest` |
| `androidx.room:room-testing` | 2.8.4 | Apache-2.0 | Google | none | Schema and migration tests |
| `com.squareup.okhttp3:mockwebserver3`, `mockwebserver3-junit4` | 5.1.0 | Apache-2.0 | Square | WireMock (heavier) | Stub MET server; records requests for AC23 and AC24 |
| `com.squareup.okhttp3:okhttp-tls` | 5.1.0 | Apache-2.0 | Square | none | HTTPS on the stub server, so AC23 can assert HTTPS |

#### Lint, format and coverage tooling
| Tool | Version | Licence | Maintenance | Alternatives considered | Why |
|---|---|---|---|---|---|
| Android Lint | bundled with AGP 9.2.1 | Apache-2.0 | Google | none | Android correctness, security and accessibility lint (`lintDebug`) with `warningsAsErrors` for security categories |
| `com.pinterest.ktlint:ktlint-cli` | 1.7.1 | MIT | Pinterest and community, active | ktlint-gradle, Spotless, detekt formatting | Kotlin official style, as required by the standards |
| JaCoCo | 0.8.13 (set through `android.testCoverage.jacocoVersion`) | EPL-2.0 | Eclipse, active | Kover | Bundled. AGP's `enableUnitTestCoverage` produces XML. |
| `diff-cover` (Python, CI and local only, not shipped) | 10.2.0 | Apache-2.0 | Bachmann1234 and community, active (10.2.0 Jan 2026) | A custom script | Mainstream changed-lines coverage gate against the base branch |

None of these libraries contains analytics, advertising or crash-reporting SDKs. Task 1 verifies this on the merged release manifest and the dependency tree.

There were no known High or Critical CVEs for these versions at design time. The security review re-checks this at G4 with Dependabot and the dependency tree. After the stack checkpoint, the owner adds the `gradle` ecosystem to `.github/dependabot.yml` through a `governance/*` PR.

### Commands (also in `docs/project.md`)
| Purpose | Command |
|---|---|
| Build | `./gradlew --no-daemon assembleDebug assembleRelease` |
| Lint | `./gradlew --no-daemon ktlintCheck lintDebug` |
| Unit tests (unit and Robolectric integration) | `./gradlew --no-daemon testDebugUnitTest` |
| Coverage | `bash scripts/coverage.sh [<base-ref>]` |
| Performance | none (manual on the reference phone, see the design) |
| Full CI | `bash scripts/ci.sh` |

`scripts/coverage.sh` does three things:
1. Runs `./gradlew --no-daemon createDebugUnitTestCoverageReport`.
2. Installs `diff-cover==10.2.0` into a git-ignored `.venv-ci`.
3. Runs `diff-cover app/build/reports/coverage/test/debug/report.xml --compare-branch=<base-ref> --src-roots app/src/main/java --fail-under=80`.

When `<base-ref>` is omitted, it uses `origin/$GITHUB_BASE_REF` if that is set. Otherwise it prints the total coverage only and skips the gate. On a shallow CI checkout it runs `git fetch --no-tags --unshallow origin` first.

### Robolectric SDK level
Robolectric 4.16 needs JDK 21 to run tests against SDK 36, but CI installs JDK 17. `app/src/test/resources/robolectric.properties` therefore sets `sdk=34`. All APIs the app uses at runtime exist at level 34, apart from the SDK 35 and 36 behaviours listed below, and those are checked manually.

If the owner changes `java-version` in `ci.yml` to `"21"` later, this can move to `sdk=36` with no code change.

Behaviours checked manually on the reference phone:
- the edge-to-edge default
- the foreground-service timeout rules

### Coverage exclusions (exhaustive)
These are excluded from the JaCoCo report:
- Generated code: `**/R.class`, `**/R$*.class`, `**/BuildConfig.*`, `**/*_Impl*.*` (Room), `**/*ComposableSingletons*.*`, `**/*$serializer.*`.
- `app/src/main/java/**/map/maplibre/**`: the thin MapLibre `AndroidView` adapter, which needs native libraries that Robolectric cannot load. It is verified by the AC38 manual check.

No other exclusions are allowed without an ADR amendment.

## Consequences
- Easier:
  - One language and toolchain.
  - Every integration test runs in CI without an emulator.
  - Accessibility (AC50-AC53) is asserted through Compose semantics.
- Harder:
  - The MapLibre native renderer cannot be exercised in CI. That logic is kept in a small adapter and checked manually (AC38).
  - The SDK 35 and 36 runtime differences are not covered by Robolectric at `sdk=34`.
  - AGP 9 is a recent major, so third-party Gradle plugins are avoided on purpose: no ktlint-gradle, Kover or Hilt plugins.
- Follow-up for the owner:
  - Add `gradle` to Dependabot (governance PR).
  - Optionally move CI to JDK 21.
  - Provide release signing keys outside the repo (`DWB_KEYSTORE_PATH`, `DWB_KEYSTORE_PASSWORD`, `DWB_KEY_ALIAS`, `DWB_KEY_PASSWORD` as environment variables or GitHub secrets). Release signing is skipped when they are absent, which gives an unsigned `assembleRelease` in CI.
- Reversal: the UI and platform layer would need a rewrite. Domain logic (rating, best window, sunrise, weekly totals, distance) is pure Kotlin with no Android types, so it could move to Kotlin Multiplatform if iOS is ever in scope.

## For new dependencies
- Licence: all permissive (Apache-2.0, MIT, BSD-2-Clause, EPL-1.0/2.0 for test and tooling only). The About screen (task 5) has an "Open-source licences" section listing these licences and the MapLibre notices, as BSD-2 attribution requires.
- Maintenance: every runtime library is maintained by Google, JetBrains, Square/Block or the MapLibre organisation, with releases in the last 12 months.
- Known vulnerabilities: none known at High or Critical for the pinned versions at design time. They are re-checked at G4.
- Size / performance impact:
  - MapLibre native libraries add roughly 10-15 MB per ABI. Mitigated by publishing an Android App Bundle (per-ABI splits) and R8 minification.
  - The other libraries are small.
