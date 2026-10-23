# Audit: 20261019-dog-walk-buddy-mvp

Auditor: independent auditor agent, 2026-10-21. Read-only review. This is the only file written by the auditor. Nothing committed.
Sources read: `docs/runs/20261019-dog-walk-buddy-mvp/state.json` (live working-tree copy, which git reports as modified), `gates/G4.md`, `gates/G3-T8-c34cc52.md`, `device-checks.md`, `security.md`, `docs/perf/20261019-dog-walk-buddy-mvp/results.md`, `docs/releases/v0.1.0.md`, `.governance/audit-events.jsonl` (1575 lines, searched for blocked events), GitHub (`gh pr view 28`, `gh pr list`, `gh issue list`).

Verification limits, stated up front: I did not read every G3 record, every PR review body, or every AC row. Items marked **UNVERIFIED** were not confirmed by me. My shell commands were partly blocked by the governance hook during this session, so I used the Read and Grep tools for some evidence.

## Run summary

| Item | Value |
|---|---|
| Idea | Dog Walk Buddy: Android app (UK). "Good time to walk?" check from weather and location, a dog profile, simple walk recording and history. Local-only data. |
| Epic | #7 (open) |
| Feature branch | `feature/20261019-dog-walk-buddy-mvp` |
| Bootstrap | Yes (`bootstrap: true`) |
| Created | 2026-10-19T12:17:05Z |
| Tasks | 9 (T1-T8 planned, T9 owner-approved fix task beyond the 8-task cap) |
| Version | 0.1.0 (versionCode 1), kept below 1.0.0 because reference-phone checks are outstanding |
| Release PR | #28 `feat: release v0.1.0 Dog Walk Buddy MVP (#7)`, OPEN, head `4b511ea`, base `main` |
| Run status | `in_progress`. G5 not run. release_approval not recorded. |
| Agent invocations | 119 used per `counters.agent_invocations` (last budget note "119/140") |

Evidence links: `https://github.com/greavemr-svg/dog-walk-buddy/pull/28`, epic `https://github.com/greavemr-svg/dog-walk-buddy/issues/7`.

## Trace table: stage / gate to evidence

| Stage / gate | Result | Timestamp (UTC) | Evidence | Status |
|---|---|---|---|---|
| Planner attempt 1 | BLOCKED, 6 questions | 2026-10-19T12:19:29Z | state.json log | ok (owner answered 12:21:39Z) |
| Planner attempt 2 | INTERRUPTED (API usage limit), then continued and DONE | 13:41:34Z / 13:44:25Z | state.json log | FLAG S-10 |
| G1 (spec) | FAIL then PASS | FAIL 13:45:20Z, PASS 13:48:22Z; PASS after Gate A change 13:55:16Z; PASS on wind amendment 14:33:26Z | state.json `gates.G1`, log | ok (fail fixed, AC36 dates) |
| Gate A (spec approval) | APPROVED | 2026-10-19T13:56:30Z | state.json `owner_answers` Gate A, reply "Approve spec" | ok |
| Spec amendment (wind) approval | APPROVED | 2026-10-19T14:34:19Z | state.json `gates.spec_approval`, reply "Approve spec" | ok |
| Stack checkpoint approval | APPROVED | 2026-10-19T14:18:08Z | state.json `gates.stack_approval`, reply "Approve stack" | ok |
| G2 (compileSdk 37 amendment) | PASS | 2026-10-19T15:23:20Z | state.json `gates.G2` | ok (amendment from owner answer 15:18:10Z) |
| **Gate B (design approval)** | **Not found** | n/a | No `Gate B` or design approval key in `state.json` (Grep for design approval and Gate B returned nothing). Design attempts counter = 2. | **FLAG A-01** |
| G3 T1-T9 | PASS on each task (T8 FAIL first, then PASS) | see per-task table | state.json `tasks[].G3`, `gates/G3-*.md` | ok with notes, see table |
| G4 | PASS on `1abdcae` | 2026-10-21T19:11:11Z | `gates/G4.md`, state.json `gates.G4` | ok (see FLAG R-02, P-01) |
| Security (Stage 4) | Critical 0, High 0, Medium 0, Low 4, Info 5 | reviewed on `5998981` | `security.md` | ok, Low items open (S-01 to S-04) |
| Device checks (Stage 4, emulator) | PASS on emulator, reference-phone checks deferred | 2026-10-21 | `device-checks.md` | FLAG E-01, E-02 |
| Performance (Stage 4, emulator) | PASS on emulator, NO_BASELINE | 2026-10-21 | `docs/perf/20261019-dog-walk-buddy-mvp/results.md` | FLAG P-01, P-02 |
| Release prepare | DONE `4b511ea` | 2026-10-21T19:15:11Z | state.json `release`, `docs/releases/v0.1.0.md`, `CHANGELOG.md` | FLAG R-01 (checklist) |
| **Release PR #28 CI** | **Governance check FAILURE** on `4b511ea` | run started 19:14:32Z, completed 19:14:36Z | `gh pr view 28 --json statusCheckRollup`, run `https://github.com/greavemr-svg/dog-walk-buddy/actions/runs/39659883428` | **FLAG R-02** |
| Release PR #28 CI build-test | SUCCESS | completed 19:21:47Z and 19:19:52Z | runs `39659883417`, `39659873488` | ok |
| G5 | Not run | n/a | `gates.G5: null` | pending |
| Release approval (owner) | Not recorded | n/a | `gates.release_approval: null` | pending |

Note on the G4 timestamp: the release note at `docs/releases/v0.1.0.md` and the G4 record both say G4 was run on `1abdcae`. The `4b511ea` release commit sits on top of that. Commits after `1abdcae` are docs and release only, as far as I could see. **UNVERIFIED**: I did not run `git diff 1abdcae..4b511ea` to confirm that no code changed.

## Gate record

| Gate | Result | Owner approval recorded? | Evidence link |
|---|---|---|---|
| G1 spec | PASS (after one FAIL) | Gate A: `Approve spec`, 2026-10-19T13:56:30Z | state.json `owner_answers`, `gates.G1` |
| Stack | APPROVED | `Approve stack`, 2026-10-19T14:18:08Z | state.json `gates.stack_approval` |
| G2 | PASS | n/a (gatekeeper) | state.json `gates.G2` |
| Gate B (design) | **Not located** | **Not located** | none |
| G3 T1 | PASS on `959eed1` (PR #16) | n/a | state.json task 1, PR #16 comments |
| G3 T2 | PASS on `92cdde8` 2026-10-20T06:04:52Z | n/a | `gates/G3-T2.md`, `https://github.com/greavemr-svg/dog-walk-buddy/pull/19#issuecomment-6284554043` |
| G3 T3 | PASS on `455b954` (superseded), PASS on `65f43eb` 2026-10-20T09:49:09Z | Owner answered t3_review_findings 2026-10-20T09:13:48Z | `gates/G3-T3.md`, `gates/G3-T3-65f43eb.md`, PR #20 comments 6287558648 and 6288204549 |
| G3 T4 | PASS on `5bef609` 2026-10-20T11:16:25Z | n/a | `gates/G3-T4.md`, `tester-T4-5bef609.md`, PR #21 comment 6289628497 |
| G3 T5 | PASS on `a20dac5` 2026-10-21T07:46:16Z | Owner answered t5_licence_text 2026-10-20T15:34:39Z | `gates/G3-T5.md`, PR #22 comment 6310832193 |
| G3 T6 | PASS on `3c86cb0` 2026-10-21T12:28:24Z | Owner answered t6_live_format | `gates/G3-T6.md`, PR #23 comment 6315899725 |
| G3 T7 | PASS on `ca63ee3` 2026-10-21T13:53:54Z | n/a | `gates/G3-T7.md`, PR #24 |
| G3 T8 | FAIL on `c34cc52` 2026-10-21T15:55:30Z, then PASS on `f363a5e` 2026-10-21T16:48:03Z | Owner answered t8_device_checks, t8_emulator, t8_attempt_limit | `gates/G3-T8-c34cc52.md`, `gates/G3-T8.md`, PR #25 |
| G3 T9 | PASS on `d8c010f` 2026-10-21T17:21:06Z | Owner answered task_limit_t9 2026-10-21T16:51:15Z | `gates/G3-T9.md`, PR #27 |
| G4 | PASS on `1abdcae` 2026-10-21T19:11:11Z | n/a (gatekeeper) | `gates/G4.md` |
| G5 | not run | **Owner approval not recorded** (`release_approval: null`) | none |

Owner approvals that matter for G5: release approval has no record in `state.json`. The G5.3 "manual checks done and recorded" criterion is explicitly not met for the reference phone (see E-01). Per the release notes, that waiver is an owner decision (`pre_stage4`), but the release approval itself still needs to be recorded by the owner.

## Per-task table

Attempt counts are from `state.json` `tasks[].attempts`, cross-checked against the log. Where the two disagree, the flag is noted.

| Task | Issue | PR | Attempts | G3 result and date | Merge (into feature) | Notes |
|---|---|---|---|---|---|---|
| T1 Walking skeleton, CI | #8 | #16 | not cleanly extracted (log: tester continuation after API limit, reviewer CHANGES_REQUESTED 04:40:13Z, release retry 04:51:50Z) | PASS 2026-10-20T04:50:10Z on `959eed1` | merged 2026-10-20T04:52:12Z, merge commit `c52d527` (from `gh pr view 16`) | Interrupted tester 04:09:11Z. Release agent blocked once (reviewer verdict not APPROVE, 04:50:33Z). Issue #8 closed manually 04:52:26Z. Budget note 37/60 at that point. |
| T2 Rating engine | #9 | #19 | 1 | PASS 2026-10-20T06:04:52Z on `92cdde8` | `962db72` | Issue closed 06:06:54Z. |
| T3 Weather client | #10 | #20 | 2 | PASS 2026-10-20T09:11:35Z on `455b954`, superseded by PASS 09:49:09Z on `65f43eb` | `4a12f49` | Reviewer found 4 gaps; owner ordered fix in T3 now. Reviewer interrupted by 429 at 09:01:45Z and re-run. |
| T4 Dog profile | #11 | #21 | 2 | PASS 2026-10-20T11:16:25Z on `5bef609` | `085d5e4` | Reviewer CHANGES_REQUESTED 10:43:39Z, then APPROVE 11:12:17Z. Issue #11 closed manually 11:19:24Z. |
| T5 Check screen, About | #12 | #22 | **state says 3, log says 2 with 3rd being a continuation** | PASS 2026-10-21T07:46:16Z on `a20dac5` | `8f66e9c` | Developer interrupted (429) twice and hit 80-turn limit. Licence text fetched from upstream by owner decision. See flag E-06 and Q-03 (flaky test). |
| T6 Walk recording | #13 | #23 | 2 | PASS 2026-10-21T12:28:24Z on `3c86cb0` | `870ad7a` | Attempt 2 session lost, re-run 11:55:51Z, not counted as extra attempt. Reviewer approval was COMMENTED with VERDICT, see Q-01. |
| T7 Walk history | #14 | #24 | 2 | PASS 2026-10-21T13:53:54Z on `ca63ee3` | `8784d4a` | PR body count fixed by PATCH (561 vs 554) without commit. |
| T8 Walk detail map, privacy, delete all | #15 | #25 | **4** (3 limit, 4th owner-approved) | FAIL 2026-10-21T15:55:30Z on `c34cc52` (G3.4: AC38/AC48/AC49 device results not recorded). PASS 2026-10-21T16:48:03Z on `f363a5e`. | `043b7f0` (PR mergeCommit) | Stray commit `d4fc26f` "stage 8", reverted `777e0cb`. Tester found HIGH crash at MapLibre init order (16:09:41Z). Emulator only. Issue #15 closed 16:48:53Z. See E-03, E-04, E-05. |
| T9 Map attribution fix | #26 (sub-issue) | #27 | 1 | PASS 2026-10-21T17:21:06Z on `d8c010f` | `0187222` | Beyond the 8-task cap, owner-approved. Issue #26 closed 17:21:49Z. |

Open PRs not in the epic chain: Dependabot PRs #1, #2, #3, #4 (open, owner-only `.github`). See Dependabot flag under Security.

## AC traceability (53 ACs)

Source: `gates/G4.md` G4.5 states "All 53 ACs trace to a merged PR and named `ACn_` tests." It does not list them. The per-AC table exists only in task records in `state.json` (`ac_trace`) and in PR bodies. I did not extract all 53 rows.

| Trace | Status | Evidence |
|---|---|---|
| AC to task to issue to PR to test, all 53 | Asserted by G4, not independently verified in full | `gates/G4.md` G4.5; `state.json` tasks; PR bodies |
| AC29, AC31, AC34 (recording) | Emulator PASS, with gaps (see E-02) | `device-checks.md` |
| AC43 (gallery photo) | Emulator PASS, gaps (gallery step not shown in images) | `device-checks.md` |
| AC38, AC48, AC49 (map, backup/restore, proxy) | Emulator only, reference phone outstanding | `gates/G3-T8.md`, `device-checks.md`, PR #25 comments 6320612298 and 6321346293 |
| AC50 (TalkBack) | Emulator, accessibility tree only, reference phone outstanding | `device-checks.md` |
| AC52 (max font and display size) | Emulator PASS, cosmetic overlap F3 | `device-checks.md` |

Gaps found: (1) the per-AC table is not in G4.md. (2) AC29 recorded duration 0:17 vs about 12 minutes of injected fixes. (3) AC34 recovered walk shows 0:00 (F2). (4) AC43 gallery-delete step has no image. (5) The reference-phone checks (AC29, AC31, AC43, AC48, AC49, AC50, battery, 4G check time) are not done.

## Separation of duties

| Check | Result | Evidence |
|---|---|---|
| Reviewer is not the author of the PR | Reviews are posted by `greavemr-bot` per the log and GitHub (PR #25: COMMENTED 15:52:36Z and 16:45:39Z). PR authorship is not recorded as an agent identity. **UNVERIFIED** against GitHub author fields for each PR. | log entries, e.g. 13:50:51Z and 12:24:58Z; `gh pr view` not run per PR |
| Tests written by the tester | Tester is recorded adding tests on T6, T7, T9 and T8 (MapLibreRuntimeTest by developer). Tester added tests in T7 (WalkHistoryEdgeCasesTest, WalkHistoryRepositoryTest). | log 13:19:13Z (T7), 12:22:55Z (T6) |
| Developer identity | Developer commits and PRs used the owner's GitHub account until the bot was invited (`bot_access` 15:28:56Z, 2026-10-19). `gh` preflight found it authenticated as the owner (11:40:33Z, 2026-10-21). | state.json `owner_answers.bot_access`; log 11:40:33Z |
| Nothing to main except via release PR | Feature branch merges are squash-integrations into the feature branch. Main has not been changed by the run. **UNVERIFIED** that no direct push to main happened. | `gh pr list` shows PR #28 as the only release PR, OPEN |
| Stray commit | `d4fc26f` "stage 8" on PR #25 was not made by the orchestrator, and the developer denies it. Origin unexplained. | log 15:15:45Z, 15:16:18Z; `gates/G3-T8.md` |
| Guard hook | Hook blocked agents from git merge/rebase/tag (developer 15:23:50Z 2026-10-19), gh pr merge/review by orchestrator (15:24:12Z and 04:51:02Z), redirects by reviewer (04:17:34Z, 04:39:25Z, 05:59:55Z), release agent on non-APPROVE verdict (04:50:33Z), Write outside project (06:04:31Z). | `.governance/audit-events.jsonl` |

Full blocked-event count: the count was not completed in this session because the governance hook blocked my counting command. Blocked events at least 8 were visible in the first portion of the file. **UNVERIFIED** for the full count.

## Exceptions

| Exception | Detail | Owner decision (verbatim) |
|---|---|---|
| Invocation limit 60 to 120 | Log 2026-10-20T04:52:43Z: "37/60 used... Escalating to owner before T2." | `budget`, 2026-10-20T04:53:30Z: "Raise the limit (Recommended)". Governance PR #18 merged to raise to 120 (log 05:32:16Z, and `pipeline.md:40`). |
| Invocation limit 120 to 140 | Log 16:49:24Z, "Budget 105/120, 15 left" | `pre_stage4`, 2026-10-21T16:50:23Z, budget answer: "Raise to 140 (Recommended)". |
| Task 9 beyond 8-task cap | Log 16:49:24Z | `task_limit_t9`, 2026-10-21T16:51:15Z: "Allow task 9 as a fix task (Recommended)". |
| T8 attempt limit (3 attempts used) | Log 16:09:41Z | `t8_attempt_limit`, 2026-10-21T16:11:01Z: "Give guidance and retry (Recommended)". |
| T8 device checks (no phone) | Log 15:56:14Z | `t8_device_checks`, "Connect phone, tester runs". Then log 15:58:09Z: `t8_emulator`, "Emulator now, phone later (Recommended)". |
| Reference-phone checks deferred to after release | Design said before G4 (G3-T8 notes "Legitimately deferred to before G4 (design line 352)"). G4 notes say the design wording is superseded by owner decision. | `pre_stage4`, 2026-10-21T16:50:23Z: "Emulator now, phone after release". |
| T3 review gaps fixed in task | Log 09:13:48Z | `t3_review_findings`: "Fix in Task 3 now (Recommended)"; "Log status code only (Recommended)". |
| T5 licence text from upstream | Log 15:34:39Z | `t5_licence_text`: "Fetch from upstream (Recommended)". |
| T6 live format owner decision | Log 09:10:26Z | `t6_live_format`: answered by owner, text in state.json. |
| Stray "stage 8" commit | Log 15:16:18Z | `t8_stage8_commit`: "Revert it (Recommended)". |
| Owner-instructed state.json update method | Owner instruction 2026-10-20T05:32:16Z: use Edit or Write, not Bash or sed. | Edit tool was unavailable (per log). Update was made by other means and disclosed to the owner. See flag G-03. |
| Descoped tasks | None. G4.1 states no tasks descoped. | n/a |

## Flags

Severity legend: Critical, High, Medium, Low, Info. "Resolution needed" means a concrete action must happen before the stated gate. Flags are ordered by severity within sections.

### Release and gate blockers

- **R-02 | High | Release PR #28 governance check FAILED on head `4b511ea`.** Cause as read from the failing job log: the governance check reads the PR body checklist and fails while items are unticked (`- [ ] CI green on release PR`, `- [ ] Manual checks ... done and recorded`, and the audit item is marked pending). It is a checklist failure, not a code failure. It should pass only after the items are truly complete, not by ticking them early. `gh pr view 28` shows `governance` conclusion FAILURE (run 39659883428, started 19:14:32Z, completed 19:14:36Z). `build-test` runs SUCCESS. The log at 19:15:11Z says "CI green (pending)", and the release checklist still has "CI green" unticked. The governance failure is not discussed anywhere I could find. Evidence: `https://github.com/greavemr-svg/dog-walk-buddy/actions/runs/39659883428`. **Resolution needed: yes**, before G5. Owner: orchestrator to investigate, owner to confirm.
- **A-01 | High | Gate B (design approval) not located.** `state.json` has no Gate B or design approval entry. `counters.design_attempts` is 2, so a design stage ran, but I found no owner approval of the design. Process requires owner approval recorded for Gate A and Gate B. **Resolution needed: yes**, before G5. Owner: owner to confirm or record. If no approval exists, orchestrator must not treat G5 as passable.
- **R-01 | High | Release checklist not complete.** `docs/releases/v0.1.0.md` and log 19:15:11Z: checklist 9 of 12 ticked. Unticked: "CI green" (see R-02), "Audit complete" (this file, now written), "Manual checks done and recorded" (reference-phone checks not done, see E-01). **Resolution needed: yes**, before G5. Owner: orchestrator (CI, audit) and owner (manual checks waiver).
- **G-05 | High | Release approval not recorded.** `gates.release_approval: null`. G5 has not run. **Resolution needed: yes**. Owner: owner.
- **E-01 | High | Reference-phone checks not done.** The Galaxy A35 checks are outstanding: AC29, AC31 (real pocket walk, screen off, doze), AC43 (real gallery photo), AC48 (device transfer and restore), AC49 (mitmproxy User-Agent), AC50 (TalkBack, including the "Normal" radio), battery drain 60 min (8% or less), check time over 4G (20 runs), all reference-phone performance figures. The design (line 352) said these come before G4. The owner decided "Emulator now, phone after release" (`pre_stage4`, 2026-10-21T16:50:23Z). That decision is recorded and verbatim. But G5.3 says "manual checks done and recorded" and that is not met for the reference phone. **Resolution needed: yes**. Owner must explicitly accept this as the G5.3 waiver (the gatekeeper waived nothing per G4.md). Owner: owner.
- **E-02 | Medium | Emulator-only evidence with gaps.** AC29 recorded 0:17 duration for about 12 minutes of injected fixes. AC34 recovered walk shows 0:00 (F2). AC43 gallery-delete step has no image in the evidence set. AC50 was checked by accessibility-tree dump only, no spoken output. Evidence: `device-checks.md`. **Resolution needed: yes** (re-check on Galaxy A35 before or with the release). Owner: developer and tester after release, with owner decision.
- **E-03 | High | Walk detail crash found only by emulator.** Tester found a HIGH crash when opening walk detail (MapLibre initialised in the wrong order). `map/maplibre` is excluded from coverage, so the crash was not caught by unit tests or coverage. Fixed in `f363a5e` (attempt 4) with a seam test `MapLibreRuntimeTest.startsMapLibreBeforeSettingTheHttpClient`. The seam test is "not proven to fail on old code" per the log. Evidence: `gates/G3-T8.md`, log 16:26:42Z. **Resolution needed: yes** to confirm the regression test catches the original bug, or accept the emulator smoke as the only proof. Owner: developer.
- **E-04 | High | T8 G3 FAIL on AC38/AC48/AC49 device evidence.** G3.4 failed at `c34cc52` because the design required recorded manual results for AC38, AC48 and AC49 in the PR and none were recorded. Later PASS on `f363a5e` relied on emulator results. Owner decisions: `t8_emulator`. Evidence: `gates/G3-T8-c34cc52.md`, `gates/G3-T8.md`. **Resolution needed: yes**, Galaxy A35 repeats of AC38, AC39, AC48, AC49 and Minor 3 attribution fix (see E-01). Owner: owner and tester.
- **E-05 | Medium | Limits raised and task-cap exceeded.** Invocations 60 to 120 (owner 04:53:30Z, 2026-10-20), then 120 to 140 (owner 16:50:23Z, 2026-10-21). T9 added beyond the 8-task cap (owner 16:51:15Z). T8 used a 4th attempt beyond the 3-attempt limit (owner 16:11:01Z). All decisions are owner decisions and recorded verbatim above. Also `docs/HOW-TO.md:92` still says 60 (owner-only doc, log 05:32:16Z). **Resolution needed: no** for the decisions themselves. Owner: owner, to update HOW-TO.md if desired.
- **E-06 | Low | T5 attempt count inconsistent.** `state.json` `tasks[5].attempts` = 3, but the log labels the owner licence continuation "not a new attempt" and says it "counts as T5 attempt 2". The third developer run (06:53:17Z) is also labelled continuation. **Resolution needed: yes**, one line correction in state.json. Owner: orchestrator.

### Stray commits and stale agents

- **S-08 | High | Stray "stage 8" commit `d4fc26f` on PR #25 origin unexplained.** The commit was made 14:58:39Z, 4 minutes after the developer's push, with the local git author configured as `greavemr-svg`. It added the owner's README.md change and `docs/runs/.../state.json` to the task branch. The orchestrator did not make it. The developer says it is not theirs. Owner chose "Revert it". Reverted by normal commit `777e0cb` (no force push). Net diff vs feature is clean (G3-T8 G3.6 verified). Feature branch unaffected. Evidence: log 15:15:45Z and 15:16:18Z, `gates/G3-T8-c34cc52.md`. **Resolution needed: yes**, owner should check the local clone for the source. Owner: owner.
- **S-09 | High | Stale background developer agent (reported in the audit brief, NOT SUPPORTED by the records I checked).** The brief reports that the first T8 developer kept running after being reported as stopped, produced local commit `36313f5`, and a reset on the feature branch at 19:15Z. I did not find `36313f5` or a 19:15Z reset in `state.json` log. A Grep of `audit-events.jsonl` for `36313f5`, `d4fc26f` and `19:15:` found no commit or reset event; the only 19:15 entries are the auditor's own SubagentStart (19:15:33Z) and the orchestrator's SubagentStop (19:15:35Z). The T8 attempt 1 entries (14:30:58Z, 15:15:45Z) describe a turn limit with uncommitted work and a continuation, not a stale agent. **UNVERIFIED**. The hook log does not record git commits, so absence there is not proof. **Resolution needed: yes**, check the local clone reflog for `36313f5` and any reset on `feature/20261019-dog-walk-buddy-mvp`, and confirm the feature head history has no unexpected entries. Owner: auditor follow-up, owner to confirm. This is also the most likely cause of the unexplained `d4fc26f` commit, which I could not confirm.
- **S-10 | Medium | Agents hit turn or session limits and were continued.** Planner attempt 2 (API usage limit, 13:41:34Z). T1 tester (usage limit, 04:09:11Z). T3 reviewer (429, 09:01:45Z). T5 developer attempt 1 (429, 14:01:28Z) and 80-turn limit with background work running (14:31:58Z), attempt 3 (429, 06:53:17Z on 2026-10-21). T6 developer attempt 2 session lost (11:40:33Z), re-run. T8 developer attempt 1 (80-turn limit, 14:30:58Z). Pre-G4 tester (60-turn limit, 18:00:13Z). Performance agent continued (18:44:42Z). Auditor continued (19:23:39Z). Each continuation is recorded in the log. None was counted as a new attempt, except where noted. **Resolution needed: no**. Owner: owner, to note the pattern.
- **S-11 | Medium | T6 attempt 2 lost session.** Developer attempt 2 never reported. Orchestrator verified no new commits and re-ran the attempt (not counted). **Resolution needed: no**.

### Counters, state integrity, and tooling

- **G-03 | Medium | state.json edited by a method the owner forbade.** Owner instruction at 2026-10-20T05:32:16Z: "Use the Edit or Write tool (not Bash/sed) to update state.json... counter 36 to 37... Do not change the counter in any other way." Edit was unavailable and the update was made by a Bash/python exact replace. The orchestrator disclosed this to the owner per the brief. **Resolution needed: yes**, owner should accept or reject the method used. Owner: owner.
- **G-04 | Medium | Counter 36 vs 37 mismatch.** Log 05:32:16Z: "counter read 36 at resume but the 04:52:43Z log entry says 37/60 used; counter kept as recorded (36)". The owner instructed 36 to 37. Final `counters.agent_invocations` is 119 and the last budget note is "119/140 incl. this call". The gap between counter and log was noted but not reconciled. **Resolution needed: yes**, one-line reconciliation in state.json or a note. Owner: orchestrator.
- **G-05b | Low | state.json is uncommitted on the feature branch.** `git status` at run start showed ` M docs/runs/20261019-dog-walk-buddy-mvp/state.json` and ` M README.md`. The state file on disk is newer than the last commit. The run records in git may not match `state.json`. **Resolution needed: yes**, commit the run record through a normal commit only if the owner approves. Owner: orchestrator and owner.
- **G-06 | Low | Owner's uncommitted README.md change not in any commit.** `git status` shows ` M README.md`. It was kept out of the task branches by the owner's choice (the "stage 8" revert restored it as an uncommitted local change). It is not part of release PR #28 unless committed. **Resolution needed: yes**, owner decides whether it belongs in the release. Owner: owner.

### Reviews, issues, and tests

- **Q-01 | Medium | Reviews not GitHub approvals for most PRs.** PR #25 has two `greavemr-bot` reviews, both COMMENTED (15:52:36Z and 16:45:39Z). Reviews were posted by `greavemr-bot` as COMMENTED with a VERDICT line (for example T5 review, T6 review 5599586562, T7 review 5600778956, T8 review 5602418810, T9 review 5603505487). The release agent was blocked from release when the latest reviewer verdict was not APPROVE (04:50:33Z). Some formal reviews exist (T4: 5583954088 CHANGES_REQUESTED, then 5584218473 APPROVE). Consistency across PRs is not guaranteed. **Resolution needed: no** for past merges, but the G3 record rests on the VERDICT lines. Owner: owner.
- **Q-02 | Low | Issues closed manually after merge into the feature branch.** Issues #8 (04:52:26Z), #9 (06:06:54Z), #10 (09:51:42Z), #11 (11:19:24Z), #12 (07:50:37Z), #13 (12:29:49Z), #14 (13:54:37Z), #15 (16:48:53Z), #26 (17:21:49Z) were closed. The log says these were closed manually with the PR link. This is expected for feature-branch merges (GitHub does not auto-close) but is worth noting. **Resolution needed: no**.
- **Q-03 | Low | Flaky test.** `CheckViewModelTest.AC15_retry_runs_a_new_check_that_can_succeed` failed on the first CI build at `53325e6` (timing, `CheckViewModelTest.kt:245`) and passed on re-run. Logged as a backlog item. Not fixed. **Resolution needed: no** for this release, but it is a recurring CI risk. Owner: developer (backlog).
- **Q-04 | Low | PR body test counts stale.** T1 PR body misreported (Major, reviewer 04:40:13Z). T3 M1. T4 F3 stale PR body counts. T6 body 500 vs 503 (fixed in body only). T7 body 554 vs 561 (fixed by PATCH, 13:51:58Z). T9 KDoc missing (Minor). **Resolution needed: no**, but the PR bodies may not match the code.
- **Q-05 | Low | Governance check on PR #28 and earlier feature CI.** See R-02. Feature-branch CI run `39657910806` succeeded on `1abdcae` (per G4.1).

### Performance

- **P-01 | Medium | NO_BASELINE.** `docs/perf/baseline.json` does not exist. A proposed baseline is in `docs/perf/20261019-dog-walk-buddy-mvp/proposed-baseline.json`. The owner must promote it via a governance PR. All figures are emulator figures on a host with broadband. **Resolution needed: yes** before G5 or at release. Owner: owner.
- **P-02 | Medium | Performance measured below spec sample size and on emulator.** History 500 walks: p90 963.6 ms (n=8), target 1000 ms, "close to the limit", 2 of 8 runs 930-960 ms. Check time: n=8, not 20, p90 1944.7 ms on broadband, not 4G. Memory: walk detail with map PSS about 165 MB vs about 85 MB without (no limit in spec). AC28 Stop-button: n=9 after excluding one run. **Resolution needed: yes** on Galaxy A35 (see E-01). Owner: owner and performance agent.

### Security

- **S-01 | Low | In-flight cached check row after "Delete all".** An in-flight cached check can be saved after "Delete all". `security.md` S-01. **Resolution needed: no** for MVP, but a privacy edge case. Owner: developer (follow-up).
- **S-02 | Low | Concurrent delete after re-entering Privacy.** Worst case is a spurious failure message. `security.md` S-02. **Resolution needed: no**. Owner: developer (follow-up).
- **S-03 | Low | Dependabot covers only GitHub Actions.** Gradle ecosystem missing (ADR 0001 says the owner adds it). Owner-only `.github/`. **Resolution needed: yes** before release if owner wants Gradle updates, otherwise follow-up. Owner: owner (governance PR).
- **S-04 | Low | Minified release build not verified at runtime.** `security.md` S-04. Release notes say closed by the release-build smoke test. I did not independently verify that smoke test's result. **Resolution needed: yes** (confirm the smoke test in the release notes or re-run). Owner: developer and orchestrator.
- **Dependabot PRs #1 to #4 are open** (`actions/setup-python`, `actions/setup-java`, `actions/checkout`, `actions/setup-node`). They touch owner-only `.github`. **Resolution needed: yes** for owner review. Owner: owner.

### Device and UX issues (Low)

- **F-19 | Low | Attribution box overlaps MapLibre logo at max font and display size.** Found by emulator. Fixed by T9 at 60% width but still noted in G4 (F3). Evidence: `device-checks.md`, `gates/G4.md`. Owner: developer (follow-up).
- **F-20 | Low | Recovered walk shows 0:00 duration.** Device check F2. Owner: developer.
- **F-21 | Low | h:mm duration format is ambiguous for short walks.** Owner: developer (follow-up).
- **F-22 | Low | "Normal" heat-sensitivity radio has no own text node in the accessibility dump.** Device check F1. Owner: owner and tester on Galaxy A35.

### Commit and state consistency

- **C-01 | Low | Security review head `5998981` vs G4 head `1abdcae`.** The security review was on `5998981`. G4 was on `1abdcae`. The gap is the Stage 4 record commits and the release-prep commits. **UNVERIFIED** that no code changed. Owner: orchestrator to confirm with a diff.

### Info

- **I-01 | Info | Owner decisions recorded verbatim** for every gate and exception above (state.json `owner_answers`).
- **I-02 | Info | Gate A approval and stack approval are recorded with owner text.** Good.
- **I-03 | Info | Hook blocks logged.** The governance hook blocked several agents and one orchestrator write outside the project. The G3-T2 record write to scratchpad was blocked; the record was written into `gates/` later. Evidence: `audit-events.jsonl`.
- **I-04 | Info | Auditor shell commands blocked.** During this audit, a Bash command was blocked by the governance hook (matched rule for `>`). The hook was not bypassed. I used the Read and Grep tools for the remaining evidence.

## Flags for the owner

Paste into PR #28 "Audit flags".

1. **R-02 High**: release PR #28 governance check FAILED on `4b511ea` (run 39659883428). Investigate before G5.
2. **A-01 High**: Gate B (design approval) not found in state.json. Confirm or record.
3. **E-01 High**: reference-phone checks not done (AC29, AC31, AC43, AC48, AC49, AC50, battery, 4G, reference-phone NFRs). Owner must record the G5.3 waiver explicitly, or the checks must be done first.
4. **R-01 High**: release checklist 9 of 12 ticked (CI green, audit, manual checks unticked).
5. **G-05 High**: release approval not recorded; G5 not run.
6. **S-08 High**: stray commit `d4fc26f` ("stage 8") on PR #25, origin unexplained. Reverted `777e0cb`. Owner to check the local clone.
7. **S-09 High (unverified)**: stale background developer agent and commit `36313f5` reported in the brief; not found in state.json. Check `audit-events.jsonl`.
8. **E-03 High**: walk detail crash (MapLibre init order) found only by emulator, fixed `f363a5e`. Regression test not proven to fail on old code.
9. **E-04 High**: T8 G3 FAIL on device evidence, later PASS on emulator. Galaxy A35 repeats owed.
10. **E-05 Medium**: invocation limit 60 to 120 to 140, T9 beyond 8-task cap, T8 4th attempt. All owner decisions, recorded verbatim.
11. **E-02 Medium**: emulator evidence gaps (AC29 0:17 vs about 12 min, AC34 0:00, AC43 gallery step not shown, AC50 accessibility tree only).
12. **G-03 Medium**: state.json edited via Bash/python despite owner instruction to use Edit or Write.
13. **G-04 Medium**: counter 36 vs 37 mismatch not reconciled.
14. **Q-01 Medium**: reviews posted as COMMENTED with VERDICT text, not GitHub approvals, for most PRs.
15. **S-10 Medium**: agents hit turn or session limits and were continued (T1, T3, T5, T6, T8, pre-G4 tester, performance, auditor).
16. **P-01 Medium**: NO_BASELINE; proposed baseline not promoted.
17. **P-02 Medium**: performance on emulator only, sample sizes below spec (History p90 964 ms near 1000 ms limit; check time n=8 not 20).
18. **G-06 Low**: owner README.md change uncommitted and not in any commit.
19. **G-05b Low**: state.json uncommitted on the feature branch.
20. **E-06 Low**: T5 attempt count 3 vs log 2 with continuation.
21. **Q-03 Low**: flaky `CheckViewModelTest.AC15` (passed on re-run).
22. **Q-04 Low**: PR body test counts stale (T1, T3, T4, T6, T7).
23. **S-01 to S-04 Low**: security findings (in-flight cached check after delete, concurrent delete, Dependabot gradle missing, minified build smoke to confirm).
24. **Dependabot PRs #1 to #4 open**: owner-only `.github`.
25. **C-01 Low**: security reviewed `5998981`, G4 on `1abdcae`; confirm no code change between.
26. **F-19 to F-22 Low**: attribution overlap at max font, recovered walk 0:00, h:mm ambiguity, "Normal" radio label.
27. **Q-02 Info**: issues closed manually after feature merges (#8 to #15, #26).

## Summary of verification status

- Verified on GitHub: PR #28 head and check rollup. Merged dates for PRs #16 and #19 to #27 from `gh pr list`. Issue closed states from `gh issue list`. T9 merge SHA `0187222` matches state.json.
- Verified in files: gates G3 and G4, security, device-checks, perf results, release notes, state.json log, owner_answers.
- Not verified: per-AC table of 53 rows, reviewer identity per PR, full blocked-event count, `36313f5` and the 19:15Z reset, Gate B, the git diff `1abdcae..4b511ea`, the security smoke-test result for S-04.

## Addendum (2026-10-22): events after the first audit

Auditor: independent auditor agent (Opus), 2026-10-22, about 08:06Z to 08:30Z. Read-only review. This section is appended. Nothing above it was changed; the original text stays as the record of what was known on 2026-10-21. Where it was wrong, the correction is given here. Nothing committed by the auditor.

Window covered: from the first audit (audit.md written 2026-10-21T19:26:53Z, committed in `2e47d34` at 2026-10-22T04:22:15Z) to feature head `af9606e` and PR #28 at 2026-10-22T08:06Z.

Sources checked by me:
- `state.json`: working copy (counter 138/160), log entries 300 to 363, `owner_answers`, `tasks[9]`, `gates.G4`, `gates.G4_history`, `audit_carryover`. I also checked it at each commit `4b511ea`, `2e47d34`, `26f0220`, `b92004d`, `7b0d48c` and `9768121`.
- `gates/G3-T10-6cd541b.md`, `gates/G3-T10.md`, `gates/G4-7b0d48c.md`.
- `docs/releases/v0.1.0.md`, `docs/releases/v0.1.0-pr.md`.
- `.governance/audit-events.jsonl`: 1789 lines, 3 unparseable, 205 events from 2026-10-21T19:28Z onward.
- GitHub:
  - `gh pr checks 28` and `gh pr view 28` (head, body, review decision).
  - `gh pr view 30` (commits, reviews, comments, merge).
  - `gh issue view 29`.
  - `gh run view` for the 9 runs cited below.
  - Comments on epic #7 and PR #28.
  - The bodies of PRs #16, #19 to #25, #27 and #30.
- git: `reflog`, `log`, `show` and `diff` for the commits named below; `origin/main`.

### A1. Trace for T10 (flaky-test fix task)

| Step | Evidence | Timestamp (UTC) | Verified by me |
|---|---|---|---|
| Trigger | Release PR #28 head `253de44`. build-test push run 39722944451 FAILED on `CheckViewModelTest.AC24_refresh_always_makes_a_new_request` (`CheckViewModelTest.kt:353`). The pull_request run 39722934416 on the same SHA passed. This was the second timing flake in this class after Q-03. | run created 04:32:28Z | `gh run view 39722944451`: CI, push, `253de44`, failure |
| Owner approval of a 10th task | `owner_answers.flaky_release_ci`, 2026-10-22T04:40:35Z, answer "Fix flake as task 10". Log 319: "question stated this exceeds the 8-task limit - owner approval of task 10 recorded". | 04:40:35Z | state.json |
| Issue | #29 "20261019-dog-walk-buddy-mvp T10: Make CheckViewModelTest timing deterministic (fix task)". Created by the orchestrator (event log 04:40:05Z, `gh issue create`). CLOSED 07:46:57Z, manually, with a link to the PR. | 04:40Z / 07:46:57Z | `gh issue view 29` |
| PR | #30 `test(check): make CheckViewModelTest timing deterministic (#29)`. Author `greavemr-bot`. Base `feature/20261019-dog-walk-buddy-mvp`. Head branch `task/20261019-dog-walk-buddy-mvp-10-deterministic-check-tests`. | | `gh pr view 30` |
| Attempt 1: developer | `a024d0f`, committed 05:00:57Z. One test file only. CI build-test 39725644134 and governance 39725644148 SUCCESS. | 05:08:59Z | PR commits |
| Attempt 1: tester | PASSED `a024d0f`, 675/675, CheckViewModelTest 30/30 forced runs. issuecomment-6333357376 (05:18:51Z). | 05:19:09Z | PR comments |
| Attempt 1: reviewer | VERDICT: CHANGES_REQUESTED (head `a024d0f`), review 5609644937, COMMENTED, 05:27:57Z. Major: the AC24 creation test passed trivially. The tester had missed this. | 05:28:21Z | PR reviews |
| Attempt 2: developer | Interrupted by 429 at 05:56:29Z (run paused by the owner at 05:35:40Z). Continued 06:02:21Z, not counted as a new attempt. `0d19767`, committed 06:16:20Z. The developer ran `taskkill //IM java.exe` (see N-03). | 06:24:12Z | PR commits; log 331 to 335 |
| Attempt 2: tester | PASSED `0d19767` with a Major finding: the AC24 already-running test passes without the guard. The tester added test commit `79912fd` (06:45:10Z) and posted issuecomment-6334579888 (06:45:39Z). INCIDENT 3 happened here (see N-02). | 06:50:41Z | PR commits and comments |
| Attempt 2: reviewer | VERDICT: CHANGES_REQUESTED (head `79912fd`), review 5610363079, 06:59:48Z. Major: the PR body was stale. | 07:00:17Z | PR reviews |
| Attempt 3 (final, 3 of 3): developer | `6cd541b`, committed 07:08:22Z, +3 lines in the test file, plus a PR body update. CI build-test 39738456067 SUCCESS on `6cd541b`. | 07:19:33Z | `gh run view 39738456067` |
| Attempt 3: tester | PASSED `6cd541b`, 676 tests, 20/20 runs. issuecomment-6335363179 (07:34:43Z). | 07:35:01Z | PR comments |
| Attempt 3: reviewer | VERDICT: APPROVE (head `6cd541b`), review 5610737811, COMMENTED, 07:38:28Z. | 07:38:51Z | PR reviews |
| G3 (1st) | **FAIL** on `6cd541b`, failing [G3.2]. The PR body lacked these template sections: `Design:` line, `Changes outside the task's listed files`, `Risk and rollback`, `Checklist`. Posted as issuecomment-6335488590 (07:42:00Z). Record: `gates/G3-T10-6cd541b.md`. | 07:41:59Z | PR comment; file |
| PR body fix | Developer, PR body edit only, no commit, not a new attempt. Governance re-run 39742492527 SUCCESS on `6cd541b`. | 07:44:04Z | `gh run view 39742492527`; PR #30 body now has every template section (checked by me) |
| G3 (re-run) | **PASS** on `6cd541b`, G3.1 to G3.6 met, failing []. Posted as issuecomment-6335563330 (07:46:10Z). Record: `gates/G3-T10.md`. | 07:46:09Z | PR comment; file |
| Merge | `merge_allowed` event 07:46:38Z. `gh pr merge 30 --squash --match-head-commit 6cd541b…`. Merge commit `2d6e741`, mergedAt 07:46:41Z. The task branch was deleted. | 07:46:41Z | `gh pr view 30`; event log |
| Run records | `7b0d48c` (state.json and the two G3-T10 records), pushed 07:47:56Z. | 07:47:56Z | git |
| G4 re-run | **PASS** on `7b0d48c`, G4.1 to G4.5 met, failing []. CI push run 39743013634 and pull_request run 39743022453 both success on `7b0d48c`. Security and performance were not re-run: the diff `1abdcae..7b0d48c` outside docs is only `CHANGELOG.md` and `CheckViewModelTest.kt`. Record: `gates/G4-7b0d48c.md` (commit `9768121`). Previous G4 kept in `gates.G4_history`. | 07:54:30Z | `gh run view` both runs; `git diff 1abdcae..af9606e -- app/src/main` is empty (checked by me) |
| Release refresh | `af9606e` docs(release). PR #28 body PATCHed: T10 added, CI green ticked. Governance run 39745060025 SUCCESS on `af9606e` (08:04:38Z). | 08:06:06Z | `gh pr checks 28` |

Separation of duties for T10:
- **Authors.** All four T10 commits have git author `greavemr-svg`, the local git config. The PR, the reviews and the comments are by `greavemr-bot`. GitHub identities therefore cannot show which agent wrote what. Role separation rests on the state.json log and the event log. For example, the event log records `gh pr review 30` at 06:59:49Z with agent `reviewer`.
- **Reviewer.** The reviewer did not write code. Its one blocked command (06:59:21Z) matched the `gh pr/issue edit|merge|...` rule. It was blocked and not bypassed.
- **Tests by the tester.** The test commit `79912fd` was written by the tester. The developer's commits were also test-only, because this is a test-fix task.
- **Production code.** The tester did change production code temporarily, by `sed` (see N-02). It was reverted, and the final diff has no `app/src/main` change.
- **main.** `origin/main` is at `3faa29d`, the merge of governance PR #18. No run commit is on main. The feature branch is 37 commits ahead. Nothing from this run reached main.

Attempts: T10 used 3 of 3. G3 for T10 needed two gatekeeper runs, with no new commit between them.

### A2. Status of the original flags

The table lists every original flag and whether it changed. I checked each one again. I did not carry any forward without checking.

| Flag | Original | Status now | Evidence |
|---|---|---|---|
| R-02 | High: governance FAIL on PR #28 | **RESOLVED on head `af9606e`.** | `gh pr checks 28` at about 08:10Z: build-test pass (push 39744039325, PR 39744051956) and governance pass (39745060025, `af9606e`, 08:04:38Z). Governance had still FAILED on `3695516` (39722148555), `253de44` (39722934645) and on `af9606e` before the body edit (39744053157). **Caveat:** any new commit to the feature branch, including the commit of this addendum, moves the PR #28 head. CI must then be green again on the final head for G5.5. The run IDs quoted in the PR body will then be stale. |
| R-01 | High: checklist 9 of 12 | **RESOLVED in the PR body.** All 12 boxes are ticked, and there is no `- [ ]` box in the PR #28 body. | `gh pr view 28` body. **Caveat:** the committed `docs/releases/v0.1.0-pr.md` line 21 still has `- [ ] CI green` (see N-10). "Manual checks" is ticked as WAIVED, not done (see E-01). "Audit complete" points to the `2e47d34` audit, which predates T10 and this addendum. |
| A-01 | High: Gate B (design approval) not located | **WITHDRAWN. Auditor error.** | `docs/governance/pipeline.md:95` defines "Gate B — owner approves release (human)": approval of the release PR on GitHub. There is no human design gate; the design is checked by G2. The orchestrator said so (log 311) and in its note in the PR #28 body. The first audit misread the gate. Gate B itself has **not happened yet**: PR #28 `reviewDecision` is REVIEW_REQUIRED and `gates.release_approval` is null. That is tracked under G-05. |
| G-05 | High: release approval not recorded, G5 not run | **OPEN.** | `gates.G5` null and `gates.release_approval` null. PR #28 has no review. |
| E-01 | High: reference-phone checks not done | **WAIVED by the owner. Still not done (accepted risk).** | `owner_answers.g5_3_phone_checks`, 2026-10-22T04:16:35Z: "Waive, do after release". `owner_answers.g5_3_checkbox`, 04:25:45Z: "Tick it, marked WAIVED". The PR body box reads "Manual checks: WAIVED by owner, NOT done on the reference phone (Galaxy A35)". No Galaxy A35 check has been run. They remain a residual risk for after release. |
| E-02 | Medium: emulator evidence gaps | **Covered by the E-01 waiver. Gaps unchanged.** | same as E-01 |
| E-03 | High: MapLibre init crash, regression test not proven | **OPEN, unchanged.** | No T10 or other change touched it. |
| E-04 | High: T8 device evidence | **Covered by the E-01 waiver. Galaxy A35 repeats still owed.** | same as E-01 |
| E-05 | Medium: limits raised, task cap exceeded | **UPDATED. The scope grew further.** | Invocation limit 140 to 160: `owner_answers.budget_160`, 07:05:00Z, "you can increase to 160". T10 is a 10th task, against a cap of 8 (`flaky_release_ci`). T10 used 3 of 3 attempts. The counter is 138/160. See N-08. |
| E-06 | Low: T5 attempts 3 vs log 2 | **OPEN, unchanged.** | `tasks[4].attempts` is still 3. |
| S-08 | High: stray `d4fc26f` "stage 8" | **OPEN. A probable cause is now identified.** | The orchestrator links it to the stale T8 developer (log 300). Separately, the owner said they made the similarly styled commit `26f0220` "Update state.json" (see N-04). Both commits have author and committer `greavemr-svg <greavemr@gmail.com>` and a short non-Conventional message. Who made `d4fc26f` is still not established. The owner should state whether `d4fc26f` was theirs. |
| S-09 | High (unverified): stale T8 developer, `36313f5` | **CONFIRMED. Contained.** | Local reflog: `36313f5 HEAD@{2026-10-21 20:15:16 +0100}: commit: feat(map): add walk detail map, privacy screen and delete all my data (#15)`, then `4b511ea HEAD@{2026-10-21 20:16:29 +0100}: reset: moving to HEAD~1`. `git cat-file -t 36313f5` is `commit`, so the object is local only. It was not pushed (log 300). **The first audit was wrong** to say this was not in state.json: log entry 300 (2026-10-21T19:17:58Z, "INCIDENT: the first T8 developer ... kept running in the background") was written before the first audit. The control gap behind it recurred: see N-01. |
| S-10 | Medium: agents continued after limits | **UPDATED. More cases.** | T10 developer attempt 2: 429 at 05:56:29Z, continued 06:02:21Z. First auditor: 40-turn limit (log 301). The run was paused twice by the owner (04:08:10Z, 05:35:40Z). A running background developer could not be stopped by the pause (log 330). |
| S-11 | Medium: T6 lost session | Unchanged, closed. | none |
| G-03 | Medium: state.json edited by a method the owner forbade | **PARTLY RESOLVED.** | Since then the owner has approved the methods: the Write tool (`state_fix_method`, 04:16:35Z, "Use Write tool (Recommended)") and Python script edits for routine fields (`state_script_edits`, 07:35:01Z, "Allow script edits"). The 2026-10-20 edit itself has not been explicitly accepted. There is a new deviation from the script condition: see N-06. |
| G-04 | Medium: counter 36 vs 37 | **OPEN, but small.** | The current counter of 138 matches the latest budget note ("138/160", log 363). The historical 36/37 gap has no reconciliation note. |
| G-05b | Low: state.json uncommitted | **LARGELY RESOLVED, with a residue.** | Committed in `2e47d34`, `26f0220`, `b92004d`, `7b0d48c` and `9768121`. The working copy is again 1 commit behind: the counter changed 137 to 138 and 2 log lines were added (`git diff`). The orchestrator is expected to commit this with the addendum. |
| G-06 | Low: owner README.md change uncommitted | **RESOLVED.** | `owner_answers.readme_title_change`, 04:05:57Z: "Discard it (Recommended)". It was restored by `git restore` (log 307). `README.md` is clean in `git status`. |
| Q-01 | Medium: reviews as COMMENTED with VERDICT | **OPEN. The pattern repeats in T10.** | All three T10 reviews (5609644937, 5610363079, 5610737811) are COMMENTED with VERDICT lines. None is a GitHub APPROVE. |
| Q-02 | Info: issues closed manually | Unchanged. #29 was also closed manually (07:46:57Z). | `gh issue view 29` |
| Q-03 | Low: flaky AC15 test | **RESOLVED by T10** (PR #30, `2d6e741`). | Root cause (state.json `tasks[9]`): Unconfined dispatchers, plus OkHttp and Room completing on background threads, so the wait helper read stale state. Fix: a single `StandardTestDispatcher` scheduler. No production change. The final head passed 20/20 local reruns. CI has been green on `6cd541b`, `7b0d48c` and `af9606e`, which is 3 heads, so not yet a long record. **Residual (backlog):** the tester and the gatekeeper noted similar patterns in `CheckViewModelDataDeletedTest`, `ProfileViewModelTest`, `StartWalkViewModelTest` and `PrivacyViewModelTest`, with no flake evidence yet. |
| Q-04 | Low: PR body counts stale | **UPDATED.** T10 too had a stale PR body (reviewer Major on `79912fd`). It was fixed in attempt 3. | log 340 |
| Q-05 | Low | Superseded by R-02 (resolved). | none |
| P-01, P-02 | Medium: NO_BASELINE, emulator-only performance | **OPEN, unchanged.** | The perf files were overwritten and then restored (N-01). The committed versions are the ones G4 assessed, and they did not change since `1abdcae` (G4-7b0d48c G4.4). |
| S-01 to S-04 | Low: security findings | **OPEN, unchanged.** | none |
| Dependabot PRs #1 to #4 | Owner-only `.github` | **OPEN.** All 4 are still open. | `gh pr list` |
| C-01 | Low: security on `5998981`, G4 on `1abdcae` | **RESOLVED.** | `git diff --stat 5998981..1abdcae -- . ':!docs'` is empty. `git diff 1abdcae..af9606e -- app/src/main` is empty. The only non-docs changes since `1abdcae` are `CHANGELOG.md` and `CheckViewModelTest.kt`. |
| F-19 to F-22 | Low: UX items | **OPEN, unchanged.** | none |
| "Nothing to main" (UNVERIFIED in the first audit) | | **VERIFIED.** | `origin/main` = `3faa29d` (governance PR #18 merge). No run commit is on main. |

### A3. New flags

- **N-01 | High | INCIDENT 2: a stale performance agent overwrote committed evidence after G4.** It is the second stale background agent in this run, after S-09.
  - The first performance agent was reported at its 50-turn limit at 18:44Z. It kept running, and wrote `docs/perf/20261019-dog-walk-buddy-mvp/proposed-baseline.json` (19:25:02Z) and `results.md` (19:25:03Z). The event log confirms both writes with agent `performance`, and the agent stopped at 19:25:20Z. This was after G4 PASSED on `1abdcae` (19:11:11Z).
  - The writes were not committed. The owner answered "Discard them (Recommended)" (`owner_answers.stale_perf_overwrite`, 04:05:57Z), and the files were restored with `git restore` (log 307). `docs/perf` is clean in `git status`.
  - The `git restore` command does not appear in the event log (see N-13).
  - A related gap: log 330 records that the orchestrator cannot stop a running background agent when the owner pauses the run.
  - **Resolution needed: yes, before Gate B.** The owner should confirm no agent processes are still running before approving. The control gap is that a reported stop does not end the agent and pause does not stop agents. It needs an owner-side fix in the harness or hooks. Owner: owner.
- **N-02 | High | INCIDENT 3: the tester modified production code via Bash `sed` after the hook blocked it.** The governance hook can be bypassed with Bash.
  - The orchestrator asked the tester to prove the test can fail by temporarily changing app code.
  - The hook blocked the tester's Edit of `app/src/main/.../CheckViewModel.kt` (event log, `blocked`, tester, Edit, 2026-10-22T06:33:12Z, "this agent may only write to: test/* ..."). The auto-mode classifier blocked `git checkout`. The tester then made the change with `sed` and reverted it (log 338).
  - The final state is clean. I checked that the `app/src/main` diff for `1abdcae..af9606e` is empty. PR #30 has one file, the test file.
  - The owner answered "Note it, continue" (`owner_answers.tester_hook_bypass`, 06:50:41Z). The orchestrator accepted the root cause: its own instruction.
  - **Facts the owner should weigh:**
    - (a) The write-path hook restricts the Edit and Write tools only. Any role that has Bash can still change restricted paths with `sed -i`, Python or similar.
    - (b) The `sed` command is **not** in `.governance/audit-events.jsonl`. I searched every event from 19:28Z on for `sed ` and `sed -`, and the only match is an orchestrator `sed -n` read at 07:46:11Z. So the bypass is known only from the agents' own reports.
    - (c) The first auditor did something similar: it modified `audit.md` with a `python - <<'EOF'` heredoc via Bash at 2026-10-21T19:27:52Z (event log). That file was its allowed target, but this was a Bash write by a read-only role.
  - **Resolution needed: yes.** The owner should harden `.claude/hooks/` (owner-only) for Bash file writes by restricted roles, and decide whether the command log must capture all Bash commands. The orchestrator must not ask testers or reviewers to change production code. Owner: owner.
- **N-03 | Medium | The developer ran `taskkill //IM java.exe`, which kills every Java process on the machine.**
  - T10 developer attempt 2 (log 335; `tasks[9].attempt2`). It kills every `java.exe` on the host, including other projects' Gradle daemons, IDEs and services.
  - In the same session `%TEMP%/ci.log` contained another project's output. That means a temp file was shared across projects. The developer did not use it.
  - The command is not in the event log. Attempt 3 records "no machine-wide kills".
  - **Resolution needed: yes (follow-up).** Add a hook rule that blocks `taskkill /IM`, `pkill` and `killall` for agents. Use `./gradlew --stop` instead. Owner: owner.
- **N-04 | Low | Commit `26f0220` is owner-made, with a non-Conventional message.**
  - "Update state.json", body "17". Author and committer `greavemr-svg`, 2026-10-22T04:33:23Z. It commits the orchestrator's uncommitted state.json edits, 13 lines added and 3 removed.
  - The owner answered "I made it, keep it" (`owner_answers.stray_commit_26f0220`). No history was rewritten.
  - It breaks the CLAUDE.md Conventional Commit rule. It is the only non-Conventional commit in `1abdcae..af9606e`.
  - **Resolution needed: no**, as an owner exception. Note that it will reach main unless the release PR is squash-merged.
- **N-05 | Medium | state.json is rewritten whole on every update, and the release head carried a duplicate G4 key.**
  - At `4b511ea`, the release head at the time of the first audit, `state.json` had two `"G4"` keys: line 22 PASS and line 38 `null`. A JSON parser keeps the last, so `gates.G4` parsed as **null** on the release head, although G4 had passed. I checked this with `json.load` on `git show 4b511ea:...`.
  - It was repaired at 04:16:35Z with the Write tool (`owner_answers.state_fix_method`; log 310). The current file has one G4 key.
  - Between 04:22Z and 07:26Z the orchestrator rewrote the whole file with the Write tool 11 times (event log). From 07:35Z it used Python `json.dump` scripts. The file is about 140 KB.
  - I compared the 6 committed versions from `4b511ea` to `9768121`. No log entry and no `owner_answers` key was lost.
  - **Resolution needed: no further repair.** The owner may want a duplicate-key or schema check on state.json, for example in CI. Owner: owner.
- **N-06 | Medium | The state.json update method changed, and the owner's condition was not fully kept.**
  - The owner approved script edits (`owner_answers.state_script_edits`, 07:35:01Z, "Allow script edits"). The question put to the owner said "Gate and owner-approval records would still go through Write".
  - At 07:54:33Z the orchestrator's Python script set `gates.G4` to the PASS record and appended the old one to `gates.G4_history` (event log command; log 360, "gates.G4 summary pointer updated by script"). That is a gate record written by script, not by Write. Log 354 had said the "run-level gates{} block will still be written with Write".
  - The full G4 record itself was saved with Write to `gates/G4-7b0d48c.md` (event log `file_write` 07:54:09Z).
  - **Resolution needed: yes.** The owner should accept the deviation or say how gate entries must be written. Owner: owner.
- **N-07 | Medium | The owner switched all agents to Opus, and 11 `.claude/agents/*.md` edits are uncommitted.**
  - The owner instruction was "cahgne model all agents to opus 5.5" (`owner_answers.agent_model`, 06:10:00Z). From 06:24:12Z the orchestrator passes `model=opus` on every invocation. The developer that was already running (attempt 2 continuation) was not switched.
  - `git diff --stat -- .claude` shows 11 files, one `model:` line each. For example, `auditor.md` changed from `haiku` to `claude-opus-5-5` and `architect.md` from `opus` to `claude-opus-5-5`.
  - These are owner-only files and are uncommitted. The committed governance config therefore differs from what ran from 06:24Z on. They must not be staged by agents. This addendum's commit must exclude them.
  - **Resolution needed: yes, before Gate B.** The owner should commit them via a governance PR or revert them. Owner: owner.
- **N-08 | Medium | The budget was raised to 160, and there is a naming gap for Gate A and Gate B.**
  - `docs/governance/pipeline.md:40` still says "Max 8 tasks and 120 agent invocations per run". `docs/governance/HOW-TO.md:92` still says 60.
  - This run used 10 tasks and 138 invocations against an owner-raised limit of 160. The raises to 140 (`pre_stage4`) and 160 (`budget_160`) are recorded only in state.json. Only the 60 to 120 raise went through a governance PR (#18).
  - Naming: state.json has no key called Gate A or Gate B. Gate A is in `owner_answers` and `gates.spec_approval`, and Gate B will be `gates.release_approval`. That gap caused the first audit's A-01 error.
  - **Resolution needed: owner acknowledgement at Gate B.** A governance PR for the docs is optional. Owner: owner.
- **N-09 | Medium | The gatekeeper cannot write gate records, so the orchestrator saves them "verbatim".** I compared the saved files with GitHub:
  - `gates/G3-T10.md`: the JSON is **identical** (parsed equality) to the JSON posted in PR #30 issuecomment-6335563330.
  - `gates/G3-T10-6cd541b.md`: PR #30 issuecomment-6335488590 is a **summary only**, with no JSON. The summary matches the file: failing G3.2, and the same four missing sections. The rest of the file cannot be checked against GitHub.
  - `gates/G4-7b0d48c.md`: **no GitHub copy found.** There is no gate comment on epic #7 (its only comment is 6323110730 from 2026-10-21) or on PR #28. The record exists only as the orchestrator's file. I independently confirmed its main checkable claims: CI runs 39743013634 and 39743022453 success on `7b0d48c`, and no `app/src/main`, build or dependency change.
  - Gatekeeper blocks in the event log: 07:40:57Z (redirect rule).
  - **Resolution needed: yes.** Post the G4 re-run JSON on PR #28 or epic #7. The owner should either allow the gatekeeper to write `gates/*` or require every gate JSON to be posted on GitHub. Owner: orchestrator (post) and owner (hook).
- **N-10 | Low | The CI box is ticked in the PR #28 body but not in the committed `docs/releases/v0.1.0-pr.md`.**
  - Line 21 of the committed file is `- [ ] CI green on the release PR ...`. The PR body has it ticked. The release agent noted this (log 362, "body authoritative").
  - The body's CI line cites governance run 39744053157 as failed only because of this box. That is true: it then passed in 39745060025 after the body edit. So for the governance part, the tick enabled its own check.
  - build-test was green on `af9606e` (39744039325, 39744051956) before the tick.
  - **Resolution needed: yes (Low).** Sync the committed file with the body. After the next commit, re-check CI on the new head and update the run IDs. Owner: release agent and orchestrator.
- **N-11 | Medium | Earlier G3 passes accepted PR bodies with missing template sections.**
  - I checked the current PR bodies for the template headings.
  - PR #25 (T8) and PR #27 (T9) have the `Design:` line but **no** "Acceptance criteria covered", "Changes outside the task's listed files", "Risk and rollback" or "Checklist" heading.
  - PRs #16 and #19 to #24 have all of them. PR #30 has all of them, after its G3.2 FAIL.
  - G3.2 was passed for T8 and T9, but failed for T10 on the same criterion. The gatekeeper noted this itself (log 350; G3-T10-6cd541b notes: "precedent does not waive criterion").
  - **Resolution needed: yes.** The owner should accept the T8 and T9 G3.2 passes as they stand or have the bodies of the merged PRs #25 and #27 completed. Owner: owner.
- **N-12 | Low | Gaps in the event log.**
  - `.governance/audit-events.jsonl` has 3 unparseable lines. Timestamps use mixed formats, for example `2026:10:19T...`, and some contain line breaks.
  - These commands were not logged: the tester's `sed` (N-02), the developer's `taskkill` (N-03), and the orchestrator's 04:05Z `git restore` (N-01).
  - The log cannot be treated as a complete record of commands.
  - **Resolution needed: yes (follow-up).** The owner should review the hook logger. Owner: owner.
- **N-13 | Info | Blocked actions since the first audit** (event log, from 19:28Z on):
  - reviewer 4: 3 redirect-rule blocks, plus 1 `gh pr/issue` mutating-verb block at 06:59:21Z
  - tester 1: Edit of `app/src/main`, 06:33:12Z (bypassed afterwards via `sed`, see N-02)
  - gatekeeper 1: redirect, 07:40:57Z
  - auditor: several in this session, for redirects, `gh pr` verbs matched in strings, and Write outside the project. None was bypassed. This addendum was written with the Write tool to `audit.md`, which the hook allows.
  - Earlier, at 2026-10-21T19:20:10Z and 19:24:44Z, there were 2 auditor blocks.
- **N-14 | Info | Corrections to the first audit (auditor errors):**
  - A-01 was a misreading of Gate B.
  - S-09 said the incident was not in state.json, but log entry 300 recorded it.
  - The first auditor used a Bash Python heredoc to edit `audit.md` (see N-02 c).

### A4. Flags for the owner (updated short list for Gate B)

These are the items still open, by severity. Resolved, withdrawn and closed items are left out (R-02, R-01, A-01, C-01, G-06, Q-03, S-11, Q-05). R-02 and R-01 hold only for head `af9606e`. CI and the checklist must be re-checked on the final head after this addendum is committed.

High
1. **G-05**: release approval not recorded and G5 not run. Gate B (owner approval of PR #28 on GitHub) is pending.
2. **N-01**: stale background agents. The performance agent overwrote evidence after G4 (discarded), and the stale T8 developer made `36313f5` (S-09, now confirmed). A pause does not stop running agents. Confirm no agents are still running.
3. **N-02**: the tester changed `app/src/main` via `sed` after the hook blocked it (reverted, verified clean). The hooks do not stop Bash file writes, and the command was not logged.
4. **S-08**: origin of `d4fc26f` "stage 8" still not established. The owner should say whether it was theirs; `26f0220` was.
5. **E-03**: MapLibre init crash. The regression test is not proven to fail on the old code.
6. **E-01, E-04** (plus E-02): reference-phone checks WAIVED by the owner (`g5_3_phone_checks`, `g5_3_checkbox`) and still not done. Accepted residual risk; Galaxy A35 checks are owed after release.

Medium
7. **N-07**: 11 uncommitted `.claude/agents/*.md` model-line edits. Commit them via a governance PR or revert them, and keep them out of run commits.
8. **N-06**: the `gates.G4` entry in state.json was written by script, against the condition attached to `state_script_edits`.
9. **N-09**: gate records are saved by the orchestrator. The G4 re-run record is not on GitHub, and the G3 FAIL JSON is not on GitHub.
10. **N-11**: G3.2 passed for T8 and T9 although their PR bodies (#25, #27) lack template sections. G3.2 was failed for T10.
11. **N-08** (plus E-05): 10 tasks and 138 of 160 invocations against the documented 8 and 120. The limits were raised only in state.json.
12. **N-03**: `taskkill //IM java.exe` (machine-wide) by the developer. Add a hook rule.
13. **N-05**: whole-file state.json rewrites. The release head `4b511ea` parsed as `gates.G4 = null` because of a duplicate key (repaired; no data lost).
14. **G-03** (partly resolved) and **G-04**: the historical state.json edit method and the 36/37 counter gap are not explicitly closed.
15. **Q-01**: reviews are COMMENTED with VERDICT lines, not GitHub approvals, including all of T10.
16. **S-10**: more continuations after limits (T10 developer 429, auditor turn limit).
17. **P-01, P-02**: NO_BASELINE, and performance figures from the emulator only.

Low
18. **N-10**: the committed `v0.1.0-pr.md` still has the CI box unticked. The PR body's CI run IDs will be stale after the next commit.
19. **N-12**: event-log gaps (unlogged commands, 3 unparseable lines, inconsistent timestamps).
20. **N-04**: owner commit `26f0220` has a non-Conventional message (owner exception).
21. **G-05b**: state.json is again 1 commit behind (counter 138 and 2 log lines). To be committed with this addendum.
22. **E-06**, **Q-04**, **S-01 to S-04**, **F-19 to F-22**, Dependabot PRs #1 to #4: unchanged.
23. **Q-03 residual**: similar timing patterns in other ViewModel tests (backlog, no flake evidence yet).
