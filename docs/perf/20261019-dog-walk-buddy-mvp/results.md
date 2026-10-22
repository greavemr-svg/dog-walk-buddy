# Performance results - 20261019-dog-walk-buddy-mvp (Stage 4)

Owner decision: emulator figures now, reference-phone (Galaxy A35) measurements after release.
All numbers below are EMULATOR figures (Medium_Phone_API_37.0, API 37, 1080x2400, debug build, commit 5998981,
host machine network). They are indicative only and are NOT the reference device. No baseline exists
(first release); these are recorded as a proposed baseline in proposed-baseline.json (owner promotes).

## Method and raw numbers (ms)
- Cold start: `am force-stop`, then `am start -W -n <appId>/.MainActivity`, TotalTime. n=10:
  930 986 990 833 887 983 853 868 970 897. Median 913.5, p90 986. No target in spec. (Debug build, not minified.)
- Start walk (AC28, target <= 1000): screenrecord (540x1200) frame analysis, first screen reaction to Stop-button visible. n=10 runs:
  381.6 336.2 319.1 383.4 351.0 319.0 240.7 1783.4 352.1 366.3. Run 8 excluded: its tap was detected at 33.9 ms, before the
  recording was stable, so the reaction frame was mis-detected (artefact); the other nine are tightly clustered. n=9 median 351.0, p90 383.4.
  A cruder screencap-poll method gave about 1.5 s but includes screencap overhead of several hundred ms per poll, so it is not representative.
  Verdict: PASS (emulator).
- History with 500 seeded walks (debug "Seed 500 sample walks" menu; target <= 1000): force-stop, launch, tap Walk history, screenrecord;
  time from first reaction to final list frame. n=8: 722.1 717.2 931.8 727.1 963.6 742.5 700.7 701.9. Median 724.6, p90 963.6.
  Includes navigation animation. Verdict: PASS (emulator) but p90 is close to the limit; 2 of 8 runs were 930-960 ms. Launch itself adds
  about 0.9 s when counted from process start (not part of the spec wording "history opens"). Seeded count shown on screen was not independently verified as exactly 500.
- Check time (launch to rating shown, target p90 <= 5000 over 20 runs): screenrecord started, app launched about 1.5 s later; time from first
  reaction to final (rating) frame. n=8 (not 20): 1739.5 1752.8 1881 (run 3 reaction adjusted, spurious early frame) 1824.7 1944.7 1625.5 1679.0 1661.7.
  Median 1746.2, p90 1944.7. Host broadband, not typical 4G; method error about +-200 ms. Verdict: PASS (emulator); 4G/20-run figure deferred.
- Memory (dumpsys meminfo TOTAL PSS): Home after check 77.6 MB; recording a walk 81.1 MB; walk detail with MapLibre map 164.9 MB
  (Java heap 99.5 MB, Native 47.4 MB). Spec sets no memory limit. The map screen roughly doubles PSS (note for owner; no target to judge against).
  Note: walk-detail PSS on non-route walks was 84-87 MB, so the map accounts for about 80 MB.

## Release build smoke (S-04)
PASS. `./gradlew --no-daemon assembleRelease` produced app/build/outputs/apk/release/app-release-unsigned.apk (52.5 MB, R8 minified;
mapping.txt present) because DWB_KEYSTORE_* were absent. Signed with a throwaway key created OUTSIDE the repo (keytool + apksigner, in the temp perfwork folder),
debug app uninstalled, release installed on the emulator (cold start TotalTime 621 ms). Exercised: Home check (rating "Go" shown), start walk with mock
GPS fixes, stop, History, walk detail (MapLibre map displayed), Privacy, Delete all my data (confirmed; History then shows "0 walks"). `adb logcat -b crash -d`
empty; no FATAL EXCEPTION or ANR. The release APK was not built from the repo-signed config; release signing itself is not exercised.
The debug app was uninstalled, so the emulator now holds the release build only.

## Deferred
- Battery: 60-minute recording <= 8 % (real device only).
- All figures on the reference phone (Galaxy A35), the check over a real 4G connection, and 20 runs for check time.
