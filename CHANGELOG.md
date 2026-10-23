# Changelog

All notable changes to this project are documented here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning: [SemVer](https://semver.org/).

## [Unreleased]

## [0.1.0] - 2026-10-23

First release of Dog Walk Buddy for Android: the MVP (epic #7) with the "Field Kit" UX redesign (epic #31), shipped together in one release. Release notes: [docs/releases/v0.1.0.md](docs/releases/v0.1.0.md).

### Added
- Go / Caution / Avoid "good time to walk?" check from MET Norway forecasts, with the best window in the next 12 hours and an "About this advice" screen (#9, #10, #12).
- Single dog profile with name, breed, photo and heat-sensitivity setting that makes heat warnings stricter (#11).
- One-tap offline walk recording through a foreground service: duration, distance and GPS route (#13).
- Walk history, weekly totals and walk deletion (#14).
- Walk detail route map on OpenFreeMap tiles, Privacy screen and "Delete all my data" (#15).
- Walking skeleton: hardened manifest, CI and the changed-lines coverage gate (#8).
- "Field Kit" theme: colour tokens, designed light and dark mode, bundled Bricolage Grotesque and Figtree fonts and Material Symbols icons; no runtime font download (#32; ADRs 0006, 0007, 0008).
- Bottom navigation with Today, Walks and Dog tabs; the Dog tab shows the dog's name (#33).
- Today: header, banner, hero rating card with factor grid (whole-number values), footnote and a pinned Start walk button (#34).
- Today: best-window card, 12-hour forecast strip, and designed "Can't check", location and precise-location states (#35).
- Recording: live route line on the recording screen (#36).
- Walks: this-week card with day dots, week sections, a "More" menu on each row and a delete confirmation sheet (#37).
- Walk detail: edge-to-edge map with a detail sheet and delete flow (#38).
- About: heat-sensitivity thresholds table (#39).

### Changed
- Every screen restyled to the owner's Field Kit design; the home screen's column of link buttons is replaced by the bottom navigation (#33-#39).
- Recording is always dark, with an h:mm:ss timer and stat cards (#36).
- Privacy shown as cards with a delete-all confirmation sheet; heading now "Privacy & your data" (#38).
- Dog profile redesigned: photo row, counters, heat-sensitivity radio cards and Save (#39).
- A small set of owner-approved copy changes (spec `docs/specs/20261022-ux-redesign.md`, "Approved copy"). Rating rules, data handling, privacy and offline behaviour are unchanged.

### Fixed
- Keep the MapLibre logo and attribution button visible on the walk detail map (#26).
- Recording timer no longer drifts and skips a second every few minutes; it is aligned to whole seconds (#48).
- A refused network-callback registration no longer crashes the Today screen; it falls back to the on-open connectivity read (#48, security R-02).

### Security
- No Critical or High findings. Open Low findings (MVP S-01, S-02, S-03; redesign R-01, R-08) are listed in the release notes.


[Unreleased]: https://github.com/greavemr-svg/dog-walk-buddy/compare/v0.1.0...HEAD

[0.1.0]: https://github.com/greavemr-svg/dog-walk-buddy/releases/tag/v0.1.0
