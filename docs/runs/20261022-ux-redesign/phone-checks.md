# Phone checks: 20261022-ux-redesign (release session)

Device: the owner's Samsung Galaxy Z Flip3 (SM-F711B, Android 14), which replaces the Galaxy A35 named in the spec (owner_answers.test_device).
Build: debug APK from feature/20261022-ux-redesign at 394f5a2 (md5 bbd488d6d0568aca02d42814181c833c). The app code is the same as G4 head f6d629d.
The owner does the checks (owner preference: manual checks on the phone are done by the owner). The orchestrator records the owner's results here verbatim.

| # | Check | Spec ref | How | Result | Owner notes |
|---|---|---|---|---|---|
| P1 | Battery, 60-minute recorded walk | NFR (≤ 8 %), MVP deferred | Unplug and charge to a known level. Start a walk and record for 60 min with the phone in a pocket, screen off. Note battery % at the start and end | | |
| P2 | Real-world walk quality | MVP deferred | Same walk: the route on Walk detail follows your path; distance and time are sensible; recording continued with the screen off | | |
| P3 | Check over 4G | NFR (p90 ≤ 5 s) | Wi-Fi off. Open the app ~5 times; the rating should appear within about 5 s each time | | |
| P4 | TalkBack walk-through | AC59 / MVP AC50 | Fill in `docs/ux-evidence/20261022-ux-redesign/task-8/AC59-talkback-checklist.md` (17 rows, light and dark), or report the rows that fail | | |
| P5 | Largest font and display size | AC27, AC38, AC60(a), MVP AC52 | Settings > Display > Font size and style: max; Screen zoom: max. Today, Recording, Walks, Walk detail, Dog, Privacy, About: nothing cut off or overlapping; Start walk and the bottom bar stay visible | | |
| P6 | 30-character dog name | AC60(b) | Save a 30-character name: the Dog tab label is shortened with "…"; TalkBack reads the full name | | |
| P7 | Greyscale | AC61 | Accessibility > Visibility > Colour adjustment / Greyscale on: Go, Caution, Avoid are told apart by word, icon and bar height | | |
| P8 | Heat-sensitivity cards and About table | AC52, AC56 | Dog tab: both cards show their bands; tapping selects one. About: the thresholds table reads correctly | | |
| P9 | Light and dark | AC66 | Switch the phone between light and dark mode: every screen follows and looks right | | |
| P10 | Walks load failure | AC66 (owed screenshot) | Optional; skip if it can't be reproduced. Note if you ever see the Walks error state | | |
| P11 | Real gallery photo | MVP deferred | Dog tab > Choose photo from your gallery: shows correctly; Remove photo works | | |
| P12 | Delete all my data | MVP | Privacy > Delete all my data: Walks and the dog profile are empty afterwards | | |
| P13 | Network hosts | AC65 / MVP AC49 | Optional, needs a proxy. Otherwise record "not run" and it carries forward as before | | |
| P14 | No cloud backup of app data | MVP AC48 | Optional (needs a Google backup and a reinstall with restore). With walks and a dog profile present: Settings > Google > Backup > Back up now; uninstall the app; reinstall the same build; open it: no walks, profile or photo come back. Otherwise record "not run" | | |
