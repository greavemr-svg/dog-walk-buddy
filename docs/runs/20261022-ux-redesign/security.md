# Security review: 20261022-ux-redesign (Stage 4, feature verification)

Recorded verbatim by the orchestrator from the security agent's report (the security agent is read-only). Agent run 2026-10-23, model opus.

- **Epic:** #31
- **Branch:** `feature/20261022-ux-redesign` at `da65ae2`. The fetched branch matches origin.
- **Delta reviewed:** `a8b4506..HEAD`. That is 249 files: 8 task merges, PRs #40 to #47.
- **Baseline:** `docs/runs/20261019-dog-walk-buddy-mvp/security.md`.
- **Mode:** read-only. No files were changed and nothing was committed.

## Summary
There are **no Critical or High findings**, so nothing blocks release.

The redesign adds no Gradle dependency, no new host, no new permission and no new exported component. The bundled fonts and icons are byte-for-byte traceable to their upstream sources. Delete-all still removes everything, and the dog form now forgets the deleted profile. There are 2 Low findings, both hardening or process items, and 5 Info notes.

Counts: critical=0 high=0 medium=0 low=2 info=5

## Findings

| ID | Severity | Location | Issue and evidence | Fix | Owner |
|---|---|---|---|---|---|
| R-01 | Low | `.github/dependabot.yml`; repository settings | **No dependency alerting.** The Dependabot alerts API returns 403 "Dependabot alerts are disabled for this repository". `dependabot.yml` still covers only `github-actions`, which carries forward MVP S-03. No audit tool is available locally either: osv-scanner, pip-audit and Gradle dependencyCheck are all absent. The redesign changes no dependency, so its own risk is unchanged, but the gap remains. | The owner enables Dependabot alerts and adds a `gradle` entry through a `governance/*` PR. Until then, run an OSV scan of `gradle/libs.versions.toml` before the release PR. | owner |
| R-02 | Low | `data/net/ConnectivityStatus.kt:54`, collected at `ui/check/CheckViewModel.kt:89` | **The new network callback can crash the app if registration fails.** `registerDefaultNetworkCallback` can throw a `RuntimeException`: a `TooManyRequestsException` above the per-app callback limit, or a `SecurityException` if `ACCESS_NETWORK_STATE` is ever removed. The flow is collected with a bare `viewModelScope.launch`, so an exception there would crash the app. Today `ACCESS_NETWORK_STATE` is declared (manifest line 7) and only one callback is held per CheckViewModel, so the risk is low. The impact is availability only. | Wrap the registration (for example in `runCatching`) and close the flow quietly, or add `.catch { }` where `CheckViewModel` collects it. The offline lead then falls back to the on-open `isOnline()` read. | developer |
| R-03 | Info | `ConnectivityStatus.kt:36-56`, `CheckViewModel.kt:89` | **The callback lifecycle is correct.** It is registered when collection starts and unregistered in `awaitClose`; it uses `.conflate()` and emits only `Unit`, with no network details. It is collected in `viewModelScope`, and Today is the start destination that stays in the back stack, so the callback stays registered for the life of the UI, including in the background. It is released in `onCleared`, so there is no leak. | Optional: collect only while the screen is started (`WhileSubscribed` or a lifecycle-aware collection). | developer |
| R-04 | Info | `ui/profile/ProfileViewModel.kt:90`, `:117-138`, `:140-190` | **The forget-after-delete-all change works, with one narrow race.** `forgetProfile()` clears the pending photo, the photo flags and the form. A `save()` or `onPhotoPicked()` coroutine already running when the delete happens could still finish afterwards and write or show the profile again. Same class of race as MVP S-01; practically unreachable. | Optional: track a deletion generation, or cancel in-flight jobs in `forgetProfile()`. Can be bundled with the MVP S-01 follow-up. | developer |
| R-05 | Info | `res/values/strings.xml:267-273`; `res/drawable/ic_*.xml` line 1 | **Licence attribution for the icons.** The Apache-2.0 modification notice is in each icon's XML header comment, as ADR 0008 requires; aapt strips these from the APK. About shows the full Apache 2.0 text (meets §4(a)), but no About entry names "Material Symbols". ADR 0008 accepted this. Licence hygiene, not security. | Optional: add "Material Symbols (Google)" to the Apache list on About in a later copy change. | architect |
| R-06 | Info | `docs/ux-evidence/20261022-ux-redesign/**` | **The screenshots contain only test data.** Synthetic Hyde Park loop (1.3 mi in 0:01), abstract recording sketch, "Your area", fake dog "Biscuit" with a plain colour photo, seeded walks 8 hours apart. None of the 61 PNGs has tEXt/iTXt/zTXt/eXIf metadata; no screenshot was replaced in history; the AC59 checklist has no personal data. | None | n/a |
| R-07 | Info | `DeleteAllDataUseCase.kt`, `PrivacyViewModel.kt`, `proguard-rules.pro` | **MVP S-01, S-02 and S-04 still apply.** These files are untouched by the redesign. | Keep the existing follow-ups. | developer |

## Verification detail

### 1. Dependencies and supply chain
- No changes to `gradle/`, `*.gradle.kts`, `gradle.properties`, `version.properties`, `proguard-rules.pro` or `res/xml`: no new dependency, no version-catalog change.
- `ui-text-google-fonts` is not on the classpath; no `GoogleFont`, `fonts.googleapis` or `gstatic` in main; `FieldKitTypographyTest.kt:156` asserts the class is absent.
- Fonts: SHA-256 match PR #40 (`bricolage_grotesque.ttf` 413e7357…ac682, 408,496 B; `figtree.ttf` 26ad3db9…fb714, 62,712 B; `licence_ofl_bricolage_grotesque.txt` 4b5a7d8f…e07ba; `licence_ofl_figtree.txt` 140d3723…a268). Git blob SHAs equal upstream `google/fonts@45e3d52` (byte-identical). Both are valid TrueType (`00 01 00 00`), 20 standard glyf-based variable-font tables, no SVG/CFF/DSIG/unknown table. SIL OFL 1.1, full texts shown on About (`AboutScreen.kt:144-146`).
- Icons: 30 Material Symbols XML files (plus 2 MVP drawables), each with the one-line source/licence/"modified:" header. Committed SHA-256 match PRs #40-#47. Independent upstream check: header removed and tint edit reversed, git blob SHA equals upstream `google/material-design-icons@49d4db35` for all 30. No external references, scripts or extra attributes.

### 2. Network
- No new host (only the `schemas.android.com` namespace and URLs inside the bundled OFL texts, shown as plain text, never fetched). MVP AC49 and spec AC65 hold (proxy run remains a manual release check).
- About renders licence text as plain `Text`; no `fromHtml`, `LinkAnnotation`, `Linkify` or WebView in main.
- `MapConfig.STYLE_URL` unchanged (`https://tiles.openfreemap.org/styles/liberty`).
- Recording route line: Compose Canvas from Room fixes; nothing in `ui/recording` imports MapLibre, OkHttp or `MapConfig`.
- Walk detail casing and start marker built locally; `RouteStyle.startMarkerGeoJson` uses `Locale.ROOT`.

### 3. Secrets and personal data
- `git log -p a8b4506..HEAD` scanned for keys, tokens, passwords, private keys, AWS/GitHub/Google/OpenAI patterns, keystores and emails: only design-token wording. No `.jks/.keystore/.p12/.pem/.env` added; nothing added-then-deleted.
- Test coordinates synthetic (~51.50 / -0.12 to -0.13); debug seed data synthetic.
- No new `Log`/`println`/`printStackTrace`; existing logs only status codes and exception class names.

### 4. Android surface
- Manifest: only `android:configChanges="uiMode"` on `MainActivity`. No new permissions, components, intent filters or deep links; `usesCleartextTraffic="false"` and backup settings unchanged.
- Status-bar code only toggles `isAppearanceLight*Bars` and restores with `enableEdgeToEdge()`; no `FLAG_SECURE` removed, no screen-on flags.
- Debug "Seed 500 sample walks" exists only in `app/src/debug`; `app/src/release/.../DebugTools.kt` returns `emptyList()`; `HistoryScreen.kt:257` shows the menu only when non-empty; release resources lack `R.string.debug_seed_walks`; release is `isDebuggable = false` with minify and shrink. Neither source set changed.

### 5. Data deletion and privacy
- `DeleteAllDataUseCase.kt` unchanged: stops an active recording first (fails if not stopped within 5 s), clears all tables, photos, HTTP cache and map cache, emits `deletions`; `CheckViewModel` and `ProfileViewModel` (`:203`) subscribe.
- Walk detail delete (AC48): confirm sheet, `if (!confirming) return` guard, parameterised Room `DELETE ... WHERE id = :walkId`, nothing logged on failure.
- Privacy texts (K10): `privacy_weather`, `privacy_map` (with Cloudflare), `privacy_local`, `privacy_no_tracking`, all `privacy_delete_*` unchanged; only headings changed (AC54).

### 6. MASVS-style checks
- Profile input capped at 200 chars in the ViewModel; name 30, breed 50; `ProfileValidator` unchanged; dog name rendered only through Compose `Text`.
- Photo Picker (`PickVisualMedia.ImageOnly`), no storage permission; `PhotoStore.prepare` unchanged (bounded read, size cap, bounds-decode, edge cap); errors map to Unreadable/TooLarge/NotAnImage.
- No new file I/O, persisted data or Room schema change; `observePoints` is read-only.

## Tools run
- `sha256sum` on fonts, licences and 32 drawables vs PR #40-#47 bodies: all match.
- `git hash-object` vs GitHub contents-API blob SHAs for `google/fonts@45e3d52` (4 files) and `google/material-design-icons@49d4db35` (30 icons, after reversing header and tint edit): all match.
- `xxd` on TTF headers and table directories: valid, standard tables only.
- PNG metadata-chunk scan of 61 screenshots: none.
- grep/ripgrep scans of delta and history for secrets, URLs, logging, intents, WebView, HTML: clean.
- `gh api .../dependabot/alerts`: 403, alerts disabled (R-01).
- osv-scanner, pip-audit, dependencyCheck not installed; no dependency changed.

STATUS: DONE
BLOCKING: 0
