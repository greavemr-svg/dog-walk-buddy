GATE G2: FAIL

Re-check on 7b99867. Recorded by the orchestrator from the gatekeeper report (gatekeeper writes are hook-blocked). G2.1-G2.6, task-set comparison (0 mismatches), dependency order, Lint claim, licences and home-link scan met. Result, failing, blocking and notes verbatim:

```json
{
  "gate": "G2",
  "run_id": "20261022-ux-redesign",
  "head": "7b99867",
  "result": "FAIL",
  "failing": [
    "test_assertion_changes_listed"
  ],
  "blocking": [
    "test_assertion_changes_listed: HistoryScreenTest.kt L271-275 (back_returns_to_the_previous_screen, assertEquals(1, backs)) cannot follow E3 as written in a screen-only host with no nav bar and no BackHandler (design L179), so task 2 would have to delete, weaken or improvise an unlisted change. Architect: give this test an explicit E3 placement, as was done for ProfileScreenTest L321 (for example, move the check into the MainActivity-hosted NavigationTest AC35 test: system Back from Walks, then NAV_TODAY_TAG assertIsSelected), and state whether that is within OD1 or needs a new owner answer."
  ],
  "notes": "Non-blocking: removing onBack from History and Profile forces argument-only compile edits that are in no table (HistoryScreenTest L99, L321, L416; HistoryRowOpenTest L83; ProfileScreenTest L77; ProfileScreenStateTest L39). No assertions change; consider saying so in the design so the reviewer does not flag them. Earlier notes fixed: contrast rounding wording (L487), ADR 0008 §4(b) header, E3 placement for ProfileScreenTest L321, task 1 trimmed."
}
```
