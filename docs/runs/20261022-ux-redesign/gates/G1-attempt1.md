GATE G1: FAIL

Recorded by the orchestrator from the gatekeeper report (failing items and notes verbatim; G1.1, G1.2, G1.4-G1.6 met).

```json
{
  "gate": "G1",
  "run_id": "20261022-ux-redesign",
  "result": "FAIL",
  "failing": [
    "G1.3: criteria must be numbered AC1..ACn; planner renumbers UX1-UX66 to AC1-AC66, cites MVP ones as 'MVP ACn', and updates all cross-references, or the owner amends governance via a governance/* PR",
    "G1.7/DoR: A9 factor-value precision (1 dp, half up) is an unconfirmed requirement that departs from the owner's artboards; orchestrator asks the owner, planner records the answer verbatim and updates UX17/A9"
  ],
  "notes": "Spec is untracked and must be committed before Gate A. Non-blocking: dark summary-card caption colour undefined (#CFE0F5 on dark primary #8CC2FF is about 1.4:1, UX6 binds); 'photo errors' pack state has no UX AC (MVP AC43 still applies); the planner's rounding-near-threshold risk is not in the spec's risks list; task estimate is at the 8-task limit."
}
```
