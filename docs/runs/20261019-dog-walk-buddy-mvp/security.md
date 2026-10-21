# Security review: 20261019-dog-walk-buddy-mvp

Feature branch head reviewed: `5998981edade999a74ecb4017643a3bf9ea991cd`. Reviewer: security agent, Stage 4, 2026-10-21. Read-only review. Recorded here by the orchestrator, verbatim in substance.

**Result: G4.3 MET.** Counts: critical=0, high=0, medium=0, low=4, info=5.

## Summary
There are no Critical or High findings. Manifest, network, storage and delete-all hardening match the spec and design:
- **Permissions:** exactly the 7 expected, with no background location.
- **Components:** only MainActivity is exported, and the service has `exported=false`.
- **Backup:** `allowBackup=false`, and both backup rule files exclude everything.
- **Network config:** cleartext traffic is off, and debug-overrides apply to debug builds only.
- **Requests:** weather requests are HTTPS-only with 2-decimal coordinates. Tile requests carry only the app User-Agent.
- **Photos:** re-encoded without EXIF, saved under generated names, with a path-traversal guard.
- **Secrets:** none found in the tree or in the branch's git history.
- **Known vulnerabilities:** OSV.dev returned no advisories for OkHttp 5.1.0, MapLibre 13.6.1, Room 2.8.4, kotlinx-serialization-json 1.10.0 or navigation-compose 2.9.7.
- **Stray commit:** the "stage 8" commit `d4fc26f` did not reach the feature branch.

## Findings
| ID | Severity | Location | Description | Recommended fix | Owner |
|---|---|---|---|---|---|
| S-01 | Low | `ui/check/CheckViewModel.kt` (check save path), `data/repo/DeleteAllDataUseCase.kt:46-48` | A check that is in flight during "Delete all my data" could call `dao.save()` after `clearAllTables()`. That would leave one cached_check row (lat/lon to 2 decimals, about 1 km, plus weather JSON) after the screen reports success. Coarse location only, no walk data, overwritten by the next check, and it stays on the device. Same issue as reviewer Minor 1 on PR #25. | Cancel or join the in-flight check before `clearAllTables`, or use a deletion generation counter. A follow-up issue is enough. | developer |
| S-02 | Low | `ui/privacy/PrivacyViewModel.kt:37-48` | The in-progress guard lives in the ViewModel, so a new ViewModel after leaving and re-entering Privacy can start a second, concurrent delete. The steps are idempotent; the worst case is a spurious failure message. Same issue as reviewer Minor 2 on PR #25. | Hold the in-progress flag (Mutex or AtomicBoolean) in DeleteAllDataUseCase. | developer |
| S-03 | Low | `.github/dependabot.yml` | Dependabot covers only github-actions. ADR 0001 line 140 says the owner adds the gradle ecosystem after the stack checkpoint, and that hasn't been done. | Owner adds a gradle entry via a `governance/*` PR. Until then, re-run an OSV audit before each release. | owner |
| S-04 | Low | `app/proguard-rules.pro` | R8 minify and shrink are on but weren't verified at runtime for kotlinx.serialization, Room and MapLibre. A failure would be functional, not a security issue. | Smoke-test the minified release APK (check, record, map, delete) before the release PR. | developer |
| S-05 | Info | `res/xml/network_security_config.xml` | debug-overrides adds user CA trust in debuggable builds only, for the AC49 proxy check. Acceptable. | None | n/a |
| S-06 | Info | `map/MapConfig.kt:19-23` | The map client has `callTimeout` set to `ZERO`; connect, read and write timeouts still apply. The tile server sees the IP address and the viewed tile coordinates. This is already covered in the spec and ADR 0003. | Confirm the Privacy screen mentions it. | n/a |
| S-07 | Info | `WalkRecordingService.kt`, manifest | The service isn't exported and has no intent filter; it uses START_NOT_STICKY and an immutable PendingIntent. The notification text is generic, and foreground-start failures are handled. | None | n/a |
| S-08 | Info | local working tree | Uncommitted README.md and run state exist locally but are not in HEAD. | Orchestrator handles them deliberately. | owner |
| S-09 | Info | git history | `d4fc26f` exists only on the remote task branch for Task 8. Its revert is `777e0cb`, and the feature branch is unaffected. | Optionally investigate where it came from. | owner |

## Dependencies (all pinned exactly in `gradle/libs.versions.toml`)
| Name | Version | ADR | Licence | CVE status |
|---|---|---|---|---|
| Android Gradle Plugin | 9.2.1 | 0001 | Apache-2.0 | none known |
| Kotlin / compose / serialization plugins | 2.3.20 | 0001 | Apache-2.0 | none known |
| KSP | 2.3.10 | 0001 | Apache-2.0 | none known |
| Compose BOM | 2026.08.00 | 0001 | Apache-2.0 | none known |
| activity-compose | 1.11.0 | 0001 | Apache-2.0 | none known |
| core-ktx | 1.17.0 | 0001 | Apache-2.0 | none known |
| lifecycle (viewmodel-compose, runtime-compose, service) | 2.9.4 | 0001, 0005 | Apache-2.0 | none known |
| navigation-compose | 2.9.7 | 0001 | Apache-2.0 | OSV: none |
| Room | 2.8.4 | 0001, 0004 | Apache-2.0 | OSV: none |
| kotlinx-coroutines | 1.10.2 | 0001 | Apache-2.0 | none known |
| kotlinx-serialization-json | 1.10.0 | 0001, 0002 | Apache-2.0 | OSV: none |
| OkHttp | 5.1.0 | 0002 | Apache-2.0 (+MPL-2.0 PSL data) | OSV: none |
| MapLibre android-sdk | 13.6.1 | 0003 | BSD-2-Clause | OSV: none |
| Test-only: mockwebserver3, okhttp-tls, junit, robolectric, androidx.test | various | 0001, 0002 | various | not shipped |
| ktlint | 1.7.1 | 0001 | MIT | tooling only |

The OSV queries covered 5 direct libraries. Transitive dependencies were not scanned, and there is no dependency-audit tool configured.
