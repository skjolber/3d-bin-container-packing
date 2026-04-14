# iter-010 Summary: Pre-compact before removeEclipsed — DROPPED

## Strategy
Add `removeFlaggedFrom(added)` to compact [added..size-1] before `removeEclipsed()`,
eliminating the `isFlag(index)` branch from the inner loop (2D's 13.5% RUNNABLE hotspot).

## Results
| Metric      | Baseline | iter-009 (best) | iter-010  | Delta vs baseline |
|-------------|----------|-----------------|-----------|-------------------|
| 2D (ops/s)  | 2362     | 2487            | 2336      | -1.10%            |
| 3D (ops/s)  | 2832     | 2846            | 2780      | -1.84%            |
| Combined    | 2597     | 2666            | **2558**  | **-1.50%**        |

## Decision: ❌ DROPPED (combined 2558 < threshold 2610)

## Why it failed
The strategy added an extra O(n) compaction pass (`removeFlaggedFrom`) BEFORE
`removeEclipsed()`, while keeping the existing `removeFlagged()` AFTER. This means:
1. [added..size-1] is scanned TWICE — once to compact, once by `removeFlagged()` again
2. Net result: ~2× O(n) scanning work vs the original ~1× O(n)
3. The branch savings in the inner loop (skipping flagged items) were NOT large enough
   to offset the doubled scanning cost

The original design (single deferred `removeFlagged` from iter-007) was specifically
optimized to avoid double scanning. This iteration inadvertently undid that benefit.

Additionally, the `isFlag` branch in the inner loop is highly predictable
(most items are NOT flagged → branch predictor correctly predicts "not taken").
So the actual branch mispredict penalty is small — not worth extra O(n) work.

## Code reverted
`git revert HEAD` applied. Repository is back to iter-009 state (best: 2666 ops/s).
