# iter-005 Summary

## Strategy: isEclipsedAtXX-skip-only

## Change Made
Added `if(otherMinX > xx) continue;` guard in `isEclipsedAtXX()` in
`DefaultPointCalculator3D`. Single atomic change — cache `getMinX()` into
`final int otherMinX`, add one `if/continue` block before volume/area check.

## Results

| Metric | Value |
|--------|-------|
| 2D ops/s | 2406.857 |
| 3D ops/s | 2780.840 |
| Combined | 2593.8 |
| Baseline | 2597.0 |
| Delta | -0.12% |
| Threshold (keep) | 2610.0 |

## Decision: **DROPPED**

Combined 2593.8 < threshold 2610 → revert applied.

## Analysis

The isEclipsedAtXX skip for `otherMinX > xx` is logically valid (such elements
cannot eclipse a point with `minX = xx`). However, the gain did not materialize:

- 3D dropped from iter-003's 2799 to 2781 (-0.6% vs iter-003, -1.8% vs baseline)
- 2D dropped from iter-003's 2427 to 2407 (-0.8% vs iter-003, +1.9% vs baseline)

Possible reasons:
1. **Low hit rate**: `isEclipsedAtXX` may be called only for Phase 2 constrained points
   where the backward scan rarely encounters `otherMinX > xx` elements. If most elements
   already have `otherMinX == xx`, the skip fires rarely and the extra branch adds overhead.
2. **JIT branch prediction**: Adding a new branch (`> xx`) in the hot loop may have
   disrupted the JIT's optimized hot-path profile for this method.
3. **Method is not the bottleneck**: `isEclipsedAtXX` may not be a significant fraction
   of total execution time; the optimization targets the wrong hotspot.
