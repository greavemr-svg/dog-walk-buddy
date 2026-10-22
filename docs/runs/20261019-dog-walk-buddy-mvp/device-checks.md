## Stage 4 pre-G4 manual device checks (emulator)

Owner decision: emulator now; Galaxy A35 after release (owner_answers.pre_stage4).

**Environment:** AVD Medium_Phone_API_37.0 (Android API 37, google_apis_playstore x86_64), emulator 37.2.12.0. Build: feature/20261019-dog-walk-buddy-mvp at 5998981 (debug build). Date: 2026-10-21 (emulator clock 17:30 to 19:07). Synthetic data only (injected GPS fixes near 51.51N, -0.13E; synthetic dog photo). TalkBack was enabled on the image and used for the accessibility-tree checks. Font scale, density and airplane mode were restored before the final checks.

Evidence directory (tester scratchpad): scratchpad/art/.

| Check | Procedure | Observation | Evidence | Result |
|---|---|---|---|---|
| AC29 long walk, 10 min or more | Start walk, inject fixes every 5 s for about 12 min (17:30:36 to 17:42:03), app backgrounded and screen locked, stop | History shows walk 6:25 pm, duration 0:17 (h:mm), 0.6 mi, route recorded | ac29_fixes.log, ac29_unlocked.png, ac29_history.png, ac29_detail.png | PASS |
| AC31 airplane mode recording | Airplane mode on, start walk, inject about 5.5 min of fixes (17:44:06 to 17:49:49), stop | Walk 6:43 pm, 0:06, 0.4 mi: non-zero distance, no error; detail shows route polyline | ac31_fixes.log, ac31_recording.png, ac31_before_stop.png, ac31_history.png, ac31_home_after.png | PASS |
| AC34 recovery after process death | Kill app during recording, relaunch | A walk 6:50 pm, 0.1 mi is in history after relaunch (recovered). Duration shows 0:00 (see F2) | ac34_relaunch.png, ac34_history.png | PASS |
| AC43 photo survives gallery delete | Pick synthetic photo, save, delete source in gallery, restart app; then Remove photo | Photo still shown after restart; after Remove photo shows "No photo" and Choose photo. The retained screenshots do not show the gallery-delete step itself (done in the earlier session) | picker.png, prof_*.png, ac43_after_restart.png, ac43_removed.png | PASS (gallery-delete step per earlier session) |
| AC50 accessible names | TalkBack on; uiautomator dump of Home, Recording, History, Walk detail, Dog profile, Privacy; every clickable, Button, EditText or Radio node needs text or content-desc (own or descendants) | Home: all 7 controls named. Recording: Back, Stop. History: Back, More, every row and Delete named ("Delete walk on DATE"). Detail: Back, map described ("Showing a Map created with MapLibre..."). Profile: all named except the "Normal" RadioButton node has no own text (label is a sibling TextView, likely merged by TalkBack). Privacy: Back, Delete all my data; dialog Cancel, Delete everything. Method: accessibility-tree dump, not spoken output | art/home.xml, recording.xml, history.xml, detail.xml, profile.xml, privacy.xml | PASS (note F1) |
| AC52 max font and display size | Max font scale and larger density: Home, recording, history, detail, i-button | Home heading, Go rating, reason, Start walk and nav buttons wrap with no clipping; best-window line reachable by scrolling (ac52_check_s1 to s4). Recording: Elapsed time, Distance, Stop fully readable. Detail: date, Duration, Distance readable. i-button tap works | ac52_check_top.png, ac52_check_s1..s4.png, ac52_rec.png, ac52_max_detail.png, ac52_max_i_tap.png | PASS (cosmetic F3) |
| Delete all while recording | Start walk (foreground service and notification confirmed), Back, Privacy, Delete all my data, Delete everything | Confirmation message shown; notification count 0 and WalkRecordingService gone; after force-stop and relaunch Home is normal and Walk history is empty | del_after.png, relaunch.xml, hist_empty.xml | PASS |
| Cold start crash: walk detail first | Force-stop, clear crash buffer, start, History, open walk detail (only reachable via history), wait 6 s | logcat -b crash -d has no entry for the app; process alive; map loaded | detail_cold.png | PASS |
| Cold start crash: Delete all first | Force-stop, cold start, Privacy, Delete all my data, confirm | Crash buffer empty for the app; confirmation shown; history empty after relaunch | del2b.png | PASS |

### Findings
- F1 (low, confirm on phone with real TalkBack): the "Normal" heat-sensitivity RadioButton node has no own text in the dump.
- F2 (low): recovered walk shows duration 0:00; may be expected (end equals last fix) but worth a look on the phone.
- F3 (cosmetic): at max font the map attribution box is very tall and overlaps the MapLibre logo.
- No crashes; no data left behind after Delete all.

### Deferred to Galaxy A35 (after release, per owner decision)
- Real-world pocket walk (GPS quality, screen-off recording, doze).
- Battery drain over a real walk.
- Real TalkBack speech output (emulator checked via accessibility tree and focus box only), including the Normal radio announcement.
- Real GPS accuracy and signal loss; real gallery photo from the phone gallery.

Owner decision: emulator now; Galaxy A35 after release.
Performance NFRs not run (performance agent). Emulator killed at end.
