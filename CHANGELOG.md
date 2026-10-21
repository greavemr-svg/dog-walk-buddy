# Changelog

All notable changes to this project are documented here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning: [SemVer](https://semver.org/).

## [Unreleased]

## [0.1.0] - 2026-10-21

First release (MVP) of Dog Walk Buddy for Android. Release notes: [docs/releases/v0.1.0.md](docs/releases/v0.1.0.md).

### Added
- Go / Caution / Avoid "good time to walk?" check from MET Norway forecasts, with the best window in the next 12 hours and an "About this advice" screen (#9, #10, #12).
- Single dog profile with name, breed, photo and heat-sensitivity setting that makes heat warnings stricter (#11).
- One-tap offline walk recording through a foreground service: duration, distance and GPS route (#13).
- Walk history, weekly totals and walk deletion (#14).
- Walk detail route map on OpenFreeMap tiles, Privacy screen and "Delete all my data" (#15).
- Walking skeleton: hardened manifest, CI and the changed-lines coverage gate (#8).

### Fixed
- Keep the MapLibre logo and attribution button visible on the walk detail map (#26).

### Security
- No Critical or High findings. Open Low findings S-01, S-02, S-03 are listed in the release notes.

[Unreleased]: https://github.com/greavemr-svg/dog-walk-buddy/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/greavemr-svg/dog-walk-buddy/releases/tag/v0.1.0
