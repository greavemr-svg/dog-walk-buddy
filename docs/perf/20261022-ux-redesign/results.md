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

## Phone confirmation (Galaxy Z Flip3)

Run on 23 Oct 2026, owner decision `perf_breach`: confirm the emulator findings on the reference phone. A fix task is needed only if a breach repeats here.

### Test conditions
- Device: Samsung Galaxy Z Flip3 SM-F711B, Android 14, 1080x2640 at 480 dpi, on USB power. The screen was kept on with `svc power stayon usb` (owner approved), then set back to 0 (checked with `dumpsys power`). No other settings were changed.
- Builds (prebuilt, not rebuilt): base = MVP `a8b4506` debug (md5 `aebb5f94...2109`), redesign = `da65ae2` debug (md5 `d5a5ac9c...445d`, same app code as HEAD). Builds were swapped with `install -r -d`.
- Data: 500 walks from the debug "Seed 500 sample walks" action, seeded once. `install -r` keeps the data, and both builds showed the same totals (this week: 14 walks, 12:52, 38.4 mi). Location permission was not granted, so Today showed the location rationale in both builds.
- Order: interleaved blocks of 5 (cold start A B A B A B A B, history A B A B, PSS A B A B). Each install was followed by one warm-up launch that was not counted.

### Method
- **Cold start:** `am force-stop`, wait 2 s, `am start -W`, TotalTime. n=20 per build.
- **History open (500 walks):** cold launch to Today and wait 4 s. Then `screenrecord` (540x1320, 8 Mbps) and `input tap` on Walk history (base) or the Walks tab (redesign). n=10 per build.
  - Start of clock: the first frame where a small region around the tapped control changes, which is the press ripple onset. The same rule is used for both builds. The ripple shows in that region in both builds (MVP button and redesign nav pill), so this is the frame closest to the tap event that is visible in the video. A screenrecord timeline cannot be matched to `dumpsys input` event times, because the MP4 timestamps start at zero.
  - End of clock: the first frame after which every later frame is within 1.5 grey levels (MAD) of the final settled list frame. The status bar is cropped.
  - Both builds first cross-fade to an empty "0 walks" frame and then fill the list. Both phases are included.
- **PSS:** cold launch, 8 s on Today, `dumpsys meminfo` TOTAL PSS. 4 samples per build.
- No battery test (out of scope for this pass). Screen recordings were analysed and then deleted. The temporary files on the phone were removed.

### Raw numbers (ms)
| Metric | Base | Redesign |
|---|---|---|
| Cold start TotalTime (n=20) | 694 702 695 694 688 / 686 685 692 693 694 / 689 689 686 693 683 / 679 691 685 691 681 | 738 748 741 750 740 / 734 738 740 729 735 / 740 734 733 728 734 / 740 741 747 744 725 |
| History open, ripple onset to settled list (n=10) | 667.3 666.6 666.2 660.0 658.8 666.5 658.7 650.4 659.7 665.9 | 554.0 558.6 565.7 562.0 576.5 562.0 557.1 566.5 567.3 557.4 |
| TOTAL PSS on Today, KB (n=4) | 128,740 128,836 128,797 128,906 | 133,133 132,810 132,759 133,100 |

p95 uses nearest rank. With n=10 that is the largest run, and with n=20 the 19th.

### Results
| Metric | Base (phone) | Redesign (phone) | Delta | Limit | Verdict |
|---|---|---|---|---|---|
| Cold start median | 690.0 | 739.0 | +7.1 % | +10 % (DoD, spec A6) | ok |
| Cold start p95 | 695 | 748 | +7.6 % | +10 % | ok |
| History open median | 663.0 | 562.0 | -15.2 % | +10 %; target <= 1000 ms | ok |
| History open p95 | 667.3 | 576.5 | -13.6 % | +10 % | ok |
| PSS on Today (median) | 128,817 KB | 132,955 KB | +3.2 % | +15 % (peak memory) | ok |

### Conclusions
- **Cold start:** PASS on the phone. The redesign is slower by a small but steady amount: about 45 to 56 ms in every block (block medians 694/741, 692/735, 689/734, 685/741), which is +7.1 % on the median. This is inside the +10 % limit. The emulator's +10.6 % pooled figure was mostly emulator noise, but the direction (redesign slower) is real.
- **History open:** PASS. All 10 redesign runs fall between 554 and 577 ms, well under the 1000 ms target and faster than the base. The 1625 ms outlier from the emulator did not happen again on the phone. The phone results are very tight: base spread 17 ms, redesign spread 23 ms.
- **Memory:** PASS. PSS on Today is +3.2 % (the emulator showed +12.7 % against old MVP figures that were not re-measured).
- No breach on the phone. Under the owner's decision, no fix task is needed.

## Re-check after task 9 (d9a9e13)
Run on 23 Oct 2026 for the stage 4 re-run after fix task 9 (issue #48, PR #49). Branch head `b3dbd78`. Method: code review of `git diff da65ae2..d9a9e13 -- app/src/main` (4 files, +50/-12). No new measurement was run, because no plausible effect on a measured metric was found.

### What changed
- **Recording timer loop** (`RecordingScreen.kt`, new `TimerTick.kt`): the loop used to wait a fixed 1000 ms after each clock read. It now waits until the elapsed time next reaches a whole second (`1000 - (elapsedMs mod 1000)`, always between 1 and 1000 ms).
- **Connectivity** (`ConnectivityStatus.kt`, `CheckViewModel.kt`): `registerDefaultNetworkCallback` is wrapped in try/catch, and the collector has a `.catch` that logs. The happy path still registers and unregisters exactly one callback.

### Impact per metric
| Metric | Impact | Reasoning |
|---|---|---|
| Cold start | none | Neither change runs at startup. `TimerTick` is only used once the Recording screen is composed. The connectivity flow is cold and only collected by the Check screen. A try block and one extra flow operator cost nothing that can be measured. |
| Start walk | none | The Start path and service are unchanged. The first timer tick now comes at most 1 s after the screen opens, the same as before. |
| History open | none | No code on this path changed. |
| Check time | none | One `.catch` operator on the connectivity collector, and a try/catch around one system call. Both cost less than a microsecond. Lookup and network code are unchanged. |
| Memory (PSS) | none | No new allocations that stay alive. Each tick allocates one `Duration`, which is short-lived. One small new class is loaded only on the Recording screen. |
| Battery during recording (timer wake-ups) | none | Both loops wake up about once a second. The old loop's period was 1000 ms plus the loop body time, so it drifted and was slightly slower than 1 Hz. The new loop lines up with whole seconds, so it runs at exactly 1 Hz. Over a 1-hour walk that is at most a few more wake-ups, out of about 3600. A 1 ms wait can only happen once, when the screen first opens and the read lands just before a boundary. After that each wait is about 1000 ms, because Handler-based `delay` never resumes early. A wall-clock jump still gives a wait between 1 and 1000 ms, so the loop cannot spin. As before, the loop only runs while the Recording screen is composed. GPS and the foreground service are unchanged. |

### Verdict
No metric is affected. The phone confirmation above (cold start +7.1 %, history open -15.2 %, PSS +3.2 %) still applies to `d9a9e13`. No regression beyond the thresholds.
