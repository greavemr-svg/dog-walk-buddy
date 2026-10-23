# Tester report: Task 4, PR #21, head 5bef609 (re-test after developer attempt 2)

Recorded by the orchestrator from the tester's hand-back at 2026-10-20T11:15Z. The tester made no commit and posted no PR comment, because no tests changed. Earlier tester comment at 58b1aa6: https://github.com/greavemr-svg/dog-walk-buddy/pull/21#issuecomment-6288992301

- STATUS: PASSED
- HEAD: 5bef609a2814776215a318da102d7ebd9194e865
- TESTS: 271/271 passed, 0 skipped, 0 failures (Gradle result XMLs after scripts/ci.sh succeeded)
- CHANGED-LINES COVERAGE: 97% (scripts/coverage.sh against origin/feature/20261019-dog-walk-buddy-mvp: 489 changed lines, 10 missed in ProfileScreen.kt and ProfileViewModel.kt); total 98.1%. Threshold 80% (docs/project.md).
- CI at 5bef609: build-test success (run 39411571032); governance success (runs 39411571071, 39412083140)

## Test-change verdict (git diff 58b1aa6 5bef609 -- app/src/test; only 4 lines removed; no test deleted or skipped)
- PhotoStoreTest.AC41_a_truncated_jpeg_does_not_crash_and_writes_nothing: ok, stronger. `!is Unreadable` became `assertEquals(NotAnImage, result)`, and the no-profile-dir assertion is kept.
- PhotoStoreTest.AC43_deleting_a_missing_generated_name_is_harmless: ok, stronger. It now stores a photo first, then asserts the directory listing is unchanged and the file still loads.
- ProfileContrastTest light and dark: ok. They use the theme's LightColors/DarkColors with the same thresholds.

## Fix verification
- F1 (live regions and focus to first field in error): verified by the ProfileScreenTest AC50_* tests, by ProfileScreenStateTest, and by ProfileViewModelTest.AC42_each_refused_save_is_counted (refusedSaves == 2 after two refusals).
- F2 (hint text has no health or breed advice): verified by AC41_the_sensitivity_hint_only_says_what_the_setting_changes.
- F4 (MinTouchTarget): verified by AC51_the_controls_are_at_least_48dp_high_and_save_is_larger.
- F5 (limits in message): verified. The exact text "under 20 MB and 8000 pixels" is asserted.
- F6 (gated until loaded): verified at ViewModel level by AC41_edits_and_saves_are_ignored_until_the_stored_profile_has_loaded_and_are_never_overwritten. The tester says the disabled state is not exercised in the Compose UI. The developer says ProfileScreenStateTest has AC41_the_fields_and_save_are_disabled_until_the_stored_profile_has_loaded; this disagreement is open for the reviewer.
- F7 (photo picked or removed mid-save): verified by AC43_a_photo_picked_while_a_save_runs_is_kept_for_the_next_save and AC43_a_photo_removed_while_a_save_runs_is_removed_by_the_next_save.
- F8 (.partial): verified by AC43_a_store_that_cannot_finish_leaves_no_partial_file_and_no_photo. Directory listings in other store tests show no stray .partial.
- F9 (bitmap recycle): code level only, no direct test. Accepted.
- F10 (strengthen weak tests): the tester did not recognise the label. The test-change verdict above covers it.
- F12 (theme schemes): verified via ProfileContrastTest.

## Findings
None. No app defects found.

## Manual checks pending
- TalkBack on device: polite announcement of "Profile saved" and the photo problem, and focus jumping to the first field in error.
- AC43 gallery delete and restart (from the first test round).
