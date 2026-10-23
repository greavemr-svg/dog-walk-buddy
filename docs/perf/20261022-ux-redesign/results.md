# Performance results - 20261022-ux-redesign (Stage 4)

All figures are EMULATOR figures and are indicative only. They are not reference-device figures.
Raw data: `docs/perf/20261022-ux-redesign/results.json`. `docs/perf/baseline.json` does not exist. The comparison
baseline is the MVP proposed baseline (`docs/perf/20261019-dog-walk-buddy-mvp/`), which is still only proposed.

## Test conditions
- Emulator: Medium_Phone_API_37.0 (API 37, 1080x2400), serial emulator-5554, host broadband network. This is the same AVD as the MVP baseline.
- Builds:
  - Base: MVP debug build, commit a8b4506.
  - Redesign: debug build of `feature/20261022-ux-redesign` at HEAD da65ae2. `assembleDebug` was up to date. Installed APK md5 d5a5ac9c6ef5a9991fa5853ac502445d.
- Same-session A/B: the base build was re-measured on 2026-10-23 with the same scripts, minutes apart from the redesign build.
  - The emulator was much slower today than in the MVP session. Base cold start was 2322 ms today against 913.5 ms then. Base check was 4015 ms against 1746 ms.
  - So the absolute MVP numbers cannot be compared with today's numbers. **Regression is judged against today's base re-measure.** The MVP figures are shown for reference only.
- Interaction timing data: for the start-walk, history and check timings, both builds used the walk data already on the emulator (one shared app database; the base build was installed over the redesign build).
  - This is the 500-walk set seeded earlier in the session. The count was not re-verified as 500 today.
  - The later cold-start runs reinstall the app, so the emulator now holds the redesign build with empty data.

## Method
- **Screenrecord timings** (start walk, history, check):
  - Each run records 540x1200 with `adb shell screenrecord`. The action happens about 1.5 s after recording starts.
  - Frames are compared with OpenCV in greyscale, with the status bar excluded.
  - Each run gives a pair [a, b]:
    - a = time from the first frame that differs from frame 0 (mean absolute difference > 1.0) to the first frame of the final settled screen (difference from the last frame < 1.5).
    - b = time of that first reaction within the video.
- Re-running the analysis on the saved base videos gives exactly the numbers in the earlier `res_base.json`, so the pairs are read consistently.
- **Measure used for start walk and history: time from the tap to the final frame**, computed as a + b - 1500 (in practice, final-frame time minus 1500 ms). This is the same formula for both builds.
  - The "a" value alone is not comparable between the builds. The redesign's navigation-bar touch ripple (difference about 1.0 to 1.2) crosses the reaction threshold at the tap. The base build's ripple (about 0.6) does not, so for base the reaction is first detected at the screen change, about 450 ms after the tap.
  - Frame timelines of `hist0`/`hist7` were checked to confirm this.
- **Check**: `am force-stop`, then Home, then screenrecord, then `am start` about 1.5 s later. The measure is a (first reaction to rating shown), as in the MVP method. Both builds start from the launcher, so a is comparable.
- **Cold start**: `cold.sh` reinstalls the build, grants permissions, sets a mock location and runs 2 discarded warm-ups. Then it runs `am force-stop` + `am start -W` and records TotalTime.
  - Order of runs: one sequential pair (base x10, then redesign x10), then an interleaved round (B5, R5, B5, R5).
  - A third round (R, B, B, R) returned no TotalTime. The adb daemon restarted, the emulator load average was about 17 and LaunchState was UNKNOWN. That round was discarded.
- **Memory**: `dumpsys meminfo` TOTAL PSS on the redesign build, in three states:
  - Today 12 s after launch, after the check.
  - Recording, after 12 mock GPS fixes at 2.5 s intervals.
  - Walk detail of that walk with the MapLibre map, 10 to 12 s after opening, measured twice.
  - Base PSS was **not** re-measured today. The comparison uses the MVP figures.

## Raw numbers and statistics (ms)
p90 uses the nearest-rank method. With n=8 to 10, p90 and p95 are the largest or second-largest run.

| Metric | Base runs (today) | Redesign runs |
|---|---|---|
| Start walk, tap to Recording (n=10) | 771 764 690 849 855 751 777 926 869 780 | 627 696 780 736 777 767 777 708 790 748 |
| History open, tap to final list (n=8) | 799 836 1046 873 1053 917 770 1038 | 897 762 795 767 616 625 848 1625 |
| Check, launch reaction to rating (n=8) | 3038 3309 3435 4895 4159 4776 4176 3871 | 3452 3895 3922 3666 3670 3744 4159 3708 |
| Cold start TotalTime, sequential (n=10) | 2240 2141 2114 1988 2493 2053 1944 1996 2025 1949 | 2174 2128 2136 2206 2645 2627 2716 2579 2506 2524 |
| Cold start TotalTime, interleaved (n=10) | 2325 2366 2539 2319 2559 / 2542 2412 2490 2485 2339 | 2508 3088 2662 2599 2530 / 2836 3350 2558 2550 3053 |

| Metric | Base median / p90 | Redesign median / p90 | Median delta | p90 (p95) delta |
|---|---|---|---|---|
| Start walk | 778.3 / 869 | 757.2 / 780 | -2.7 % | -10.2 % |
| History open | 895.2 / 1053 | 781.2 / 1625 | -12.7 % | +54.3 % (one run of 8) |
| Check | 4014.9 / 4894.6 | 3725.9 / 4158.6 | -7.2 % | -15.0 % |
| Cold start, all 20 vs 20 | 2322.0 / 2539 | 2568.5 / 3053 | **+10.6 %** | +20.2 % |
| Cold start, interleaved only | 2448.5 / 2542 | 2630.5 / 3088 | +7.4 % | +21.5 % |
| Cold start, sequential only | 2039.0 / 2240 | 2515.0 / 2645 | +23.3 % | +18.1 % |

Per block, the redesign's cold-start median was higher every time: sequential +23.3 %, interleaved block 1 +9.8 % (2366 against 2599), and interleaved block 2 +14.1 % (2485 against 2836).

### Memory, TOTAL PSS (KB)
| State | Base (MVP results, not re-measured) | Redesign (today) | Delta | Threshold (peak memory) |
|---|---|---|---|---|
| Today after check | 77,563 | 87,404 (Java 17,388, Native 11,884) | +12.7 % | +15 %, ok |
| Recording | 81,071 | 89,810 (Java 15,796, Native 14,160) | +10.8 % | +15 %, ok |
| Walk detail with map | 164,935 | 166,329 / 184,342 (two openings) | +0.8 % / +11.8 % | +15 %, ok |

The map screen is still the peak: PSS rises by about 80 to 95 MB, mostly native (MapLibre), which is the same pattern as the MVP.

## Verdicts
Thresholds come from `definition-of-done.md`: p95 interaction time +10 %, cold start +10 %, peak memory +15 %. Spec A6 judges regression by medians within +10 %. The spec targets are start walk <= 1 s, history (500 walks) <= 1 s and check p90 <= 5 s.

- **Start walk:** PASS. Median 757.2 ms, at most 790 ms, so the <= 1000 ms target is met. Median -2.7 % and p90 -10.2 % against base.
- **History open:** median PASS, p95 BREACH (low confidence).
  - The median is 781.2 ms (target <= 1000) and is -12.7 % against base.
  - One run in 8 took 1625 ms. That run had a real gap of about 400 ms before the list frame appeared, so it is not an analysis artefact.
  - Under the DoD p95 rule this is +54 %, a breach. Under spec A6 (median) it passes.
  - The base build also had 3 of 8 runs just over 1000 ms (1038 to 1053). Neither build meets "<= 1 s" on every run on this emulator.
- **Check:** PASS. p90 4158.6 ms (target <= 5000). Median -7.2 % and p90 -15 % against base. n=8, not 20, on host broadband rather than 4G.
- **Cold start:** BREACH (marginal).
  - The pooled 20-against-20 median is +10.6 %, over the +10 % threshold. The interleaved-only median is +7.4 %.
  - The redesign was slower in every pairing, which goes against the design's expectation of "no new startup work".
  - The emulator was heavily loaded, so the noise is large compared with the margin.
- **Memory:** PASS. Every state is within +15 % of the MVP figures. Base was not re-measured on the same day.

## Deferred (owner's phone session, Samsung Galaxy Z Flip3)
- Battery: a 60-minute recording with the live route line must use <= 8 % (spec NFR). Not measurable on the emulator.
- All timings on the reference phone, in particular cold start and history open, to confirm or clear the marginal emulator findings above.
- Check time over real 4G with 20 runs.
- PSS on the device, with a same-day base re-measure.
