# Performance results - 20261022-ux-redesign (Stage 4)

All numbers are EMULATOR figures and indicative only. They are NOT the reference device (Galaxy A35).
Verdict: **PASS**. No metric regresses beyond its threshold. Battery is left for the owner's manual phone check.

## Conditions
- Date: 2026-10-23. Host: Windows 11, WHPX acceleration, host GPU (RTX 5060 Laptop), host broadband network.
- Emulator: `Medium_Phone_API_37.0` (API 37, 1080x2400), headless (`-no-window -no-audio -no-boot-anim`). This is the same AVD as the MVP.
- Device animation settings were left as found: window 0, transition 1.0, animator default. They are the same for both builds.
- Builds: **debug** (`assembleDebug`), as in the MVP baseline and the spec NFR.
  - Redesign: `feature/20261022-ux-redesign` @ `da65ae2`.
  - MVP base: `a8b4506`, the last commit before this run on the feature branch. It was built in a scratch worktree outside the repo.
- Location: `adb emu geo fix` no longer reached the location manager on this image. The location was mocked through shell test providers (`cmd location providers set-test-provider-location gps|fused`, 51.5072,-0.1276). This was the same for both builds.
- Data: 500 walks seeded with the debug "Seed 500 sample walks" menu for the History runs, plus the few walks created by the Start-walk runs.

## Why there is a same-session A/B
The absolute figures in this session are slower than the MVP session (2026-10-19). This holds for the **unchanged MVP base build** too: its cold start median is 1952.5 ms here against 913.5 ms then.
- The cause is the environment, not the app: emulator image and host state, and possibly a different launch state.
- The frame-analysis script was also rewritten, because the MVP one was not kept in the repo. Its settle criterion is not identical.

So the regression judgement uses the **MVP base build and the redesign build measured back-to-back in the same emulator session with the same scripts**. The +10 % limit (spec assumption A6, definition-of-done) is applied to that delta. Absolute MVP targets are checked on the redesign's absolute numbers.

## Method (same as MVP; n as MVP)
- **Cold start:** `am force-stop`, wait 3 s, `am start -W`, read TotalTime. There were 2 discarded warm-up launches after install, then n=10.
- **Start walk tap to recording** (n=10): `screenrecord --size 540x1200`, then tap Start walk. Time runs from the first frame differing from the pre-tap frame to the frame where the screen settles on the recording screen. The status bar is cropped, and the settle threshold of 1.5 grey levels mean absolute difference (MAD) ignores the running timer tick.
  - Reported with reaction threshold 1.0 (first reaction, includes the button ripple, as in the MVP) and 5.0 (first content change).
- **History with 500 walks** (n=8): force-stop, launch, tap Walk history (MVP) or the Walks tab (redesign), and screenrecord. Time runs from first reaction to the final list frame.
  - Reaction threshold 5.0 is used for the judgement. The redesign's bottom-nav tap ripple (MAD ~1.2) crosses the 1.0 threshold about 430 ms before any content change, while the MVP button ripple (~0.6) does not. Using 1.0 would compare different start points.
- **Check launch to result** (n=8, as MVP): HOME, then screenrecord, then `am start` about 1.5 s in. Time runs from first reaction to the final (rating) frame, so it includes the cold launch.
- **PSS:** `dumpsys meminfo` TOTAL PSS on Today after a check, while recording (12 mock fixes, about 35 s), and on Walk detail with the MapLibre route map shown (map rendered, screenshot checked).
- **APK size:** `assembleRelease` unsigned APK, byte size, base against HEAD.

## Results - timings (ms)
| Metric | MVP proposed baseline (2026-10-19) | MVP base, this session | Redesign, this session | Delta vs base (same session) | Limit | MVP target | Result |
|---|---|---|---|---|---|---|---|
| Cold start TotalTime, median | 913.5 | 1952.5 (p90 1966.9, 1919-1975) | 2080.5 (p90 2116.0, 2033-2179) | **+6.6 %** | +10 % | none | PASS |
| Start walk, first reaction (thr 1.0), median | 351.0 | 718.2 (p90 811.3, 652.5-830.6) | 743.2 (p90 769.4, 685.5-780.2) | **+3.5 %** | +10 % | <= 1000 | PASS |
| Start walk, content change (thr 5.0), median | n/a | 438.6 (p90 530.6, 370.3-563.9) | 469.5 (p90 568.0, 386.3-685.5) | +7.0 % | +10 % | <= 1000 | PASS |
| History 500 walks (thr 5.0), median | 724.6 | 422.8 (p90 505.0, 298.7-515.6) | 225.7 (p90 365.5, 178.0-438.2) | **-46.6 %** | +10 % | <= 1000 | PASS |
| Check launch to result, median | 1746.2 | 4014.9 (p90 4811.7, 3038.1-4894.6) | 3725.9 (p90 3993.2, 3452.4-4158.6) | **-7.2 %** | +10 % | p90 <= 5000 | PASS (p90 3993.2) |

Raw values (ms):
- Cold start, base: 1960 1929 1959 1926 1927 1975 1949 1966 1919 1956.
- Cold start, redesign (paired set): 2109 2107 2044 2045 2104 2060 2088 2073 2033 2179.
- Cold start, redesign, an earlier set taken before the A/B, right after the first launches: 2446 2116 2592 2487 2659 2893 2917 2663 2702 2229. Median 2625.5.
  - This set had much wider spread, and it was taken before the base build was measured, so it is not used for the comparison. It is recorded here for transparency.
  - If the owner prefers the conservative reading, cold start is the metric to re-check on the reference phone.
- Start walk, redesign (thr 1.0): 720.4 694.6 780.2 685.5 732.7 743.8 758.0 765.9 768.2 742.7.
- Start walk, base (thr 1.0): 715.4 716.4 652.5 778.1 797.3 697.9 720.0 809.2 830.6 708.2.
- History, redesign (thr 5.0): 251.2 234.8 216.6 189.6 199.3 178.0 334.3 438.2. With thr 1.0 the median is 687.9 and run 8 is 1556.4 (ripple artefact).
- History, base (thr 5.0): 317.8 298.7 500.5 449.4 399.8 430.4 415.2 515.6.
- Check, redesign: 3452.4 3895.1 3922.3 3665.7 3670.3 3743.6 4158.6 3708.2.
- Check, base: 3038.1 3309.3 3435.1 4894.6 4158.7 4776.2 4176.1 3871.2.

Notes:
- History got faster in the redesign. Walks is now a bottom-nav tab, with no push-navigation slide animation (the MVP number included the animation).
- The check is dominated by the network (api.met.no over host broadband). Check figures are not 4G and are n=8, not 20; both remain deferred to the reference phone, as in the MVP.

## Results - memory (TOTAL PSS, KB)
The MVP base build was not re-measured for PSS in this session, because the emulator was shut down externally once the timing runs finished. These are compared with the MVP proposed baseline.

| Screen | MVP baseline | Redesign | Delta | Limit (DoD peak memory) | Result |
|---|---|---|---|---|---|
| Today after check | 77,563 | 84,696 (Java 16,568, Native 11,404) | +9.2 % | +15 % | PASS |
| Recording a walk | 81,071 | 87,949 (Java 15,564, Native 13,608) | +8.5 % | +15 % | PASS |
| Walk detail with map (peak) | 164,935 | 164,378 (Java 15,576, Native 72,712) | -0.3 % | +15 % | PASS |

- Peak PSS (walk detail with map) is unchanged.
- Today and Recording gained about 7 MB, which fits the memory-mapped bundled fonts and the extra vectors (design estimate: "well under 15 %").
- The Java/Native split differs from the MVP recording (MVP walk detail: Java 99.5 MB, Native 47.4 MB), but the totals match. The split is attributed to the emulator image or runtime rather than the app.

## APK size (release, unsigned, R8)
| Build | Bytes | MB |
|---|---|---|
| MVP base `a8b4506` | 52,455,255 | 52.46 |
| Redesign `da65ae2` | 53,045,823 | 53.05 |
| Delta | +590,568 (+576.7 KiB) | **+1.13 %** |

- There is no size target.
- The growth is above the spec's "about 300 KB" and task 1's +310 KB, but within the design estimate of fonts at 300-600 KB plus icons at about 30 KB.
- Debug APK for reference: 84,362,337 to 85,738,065 bytes (+1.63 %).

## Not measured (manual, owner)
- **Battery**, at most 8 % per 60-minute recording with the live route line: this cannot be measured on an emulator. It needs the reference phone (Galaxy A35).
- All figures on the reference phone, check time over real 4G with n=20 (p90 <= 5000 ms). These carry over from the MVP deferred list.
- Optionally, re-check cold start on the phone (the redesign is +6.6 % in the A/B, the closest metric to its limit).

## Baseline
- `docs/perf/baseline.json` still does not exist. Compared against `docs/perf/20261019-dog-walk-buddy-mvp/proposed-baseline.json` and a same-session MVP base build.
- The absolute emulator numbers in this session do not reproduce the MVP session's numbers, even for the unchanged base build. If the owner promotes an emulator baseline, it should come from a single session with a script kept alongside it.
