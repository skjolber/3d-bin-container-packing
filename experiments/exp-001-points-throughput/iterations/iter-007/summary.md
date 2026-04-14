# iter-007 Summary: Merge Two removeFlagged() Calls

## Strategy
Deferred the first `removeFlagged()` call (previously at line 437 of `add()`) so that
both constraint-flagged originals (F items) and eclipse-flagged new items (G items) are
removed in a single O(n) scan at the end of `add()`.

Restored `if(values.isFlag(index)) continue;` in the inner loop of `removeEclipsed()` since
original items may still be flagged when `removeEclipsed()` runs.

## Math Correctness
endIndex formula `+= added - removeFlagged()` is identical whether applied once (combined)
or twice (split): `(endIndex_initial - F) + added - G == endIndex_initial + added - (F+G)`.
All flagged items are guaranteed to be in [0..endIndex_initial-1] (confirmed by loop ranges).

## Results

| Metric     | Baseline | iter-006 | iter-007 | Δ vs baseline | Δ vs prev |
|------------|----------|----------|----------|---------------|-----------|
| 2D (ops/s) | 2362     | 2431     | 2458     | +4.1%         | +1.1%     |
| 3D (ops/s) | 2832     | 2827     | 2848     | +0.6%         | +0.7%     |
| Combined   | 2597     | 2629     | 2653     | +2.15%        | +0.91%    |

## Decision: **KEEP** (combined 2653 ≥ threshold 2610)

## Files Changed
- `points/src/main/java/com/github/skjolber/packing/ep/points2d/DefaultPointCalculator2D.java`
  - Removed `endIndex -= values.removeFlagged();` (was line 437)
  - Restored `if(values.isFlag(index)) continue;` in `removeEclipsed` inner loop
  - Added comment explaining the combined pass formula
