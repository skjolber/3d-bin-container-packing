# iter-004 Summary

## Strategy
eclipse-check-inline-and-isEclipsedAtXX-skip

Three changes applied simultaneously in DefaultPointCalculator3D and DefaultPointCalculator2D:
1. `isEclipsed()` 3D: cached all 6 point fields, cached `otherValues.size()`, removed
   volume/area pre-check, inlined `eclipses()` skipping the always-true minX condition.
2. `isEclipsedAtXX()` 3D: added `continue` for elements with minX > xx (they can never
   eclipse), cached point fields, removed volume/area pre-check, inlined eclipse check.
3. `removeEclipsed()` 2D: cached unsorted point fields outside inner loop, removed area
   pre-check, inlined eclipse check skipping redundant minX condition.

## Results

| Metric | Value |
|--------|-------|
| 2D ops/s | 2387 |
| 3D ops/s | 2734 |
| Combined | 2560 |
| Baseline combined | 2597 |
| Delta vs baseline | -1.42% |
| Threshold (2610) met? | NO |

## Decision: DROPPED

Combined (2560) < threshold (2610). Reverted via `git revert HEAD --no-edit`.

## Analysis

The optimization **hurt** 3D by -3.4% vs baseline (2734 vs 2832), despite the theoretical
arguments for removing redundant checks. Key failure reason: the volume/area pre-check in
`isEclipsed()` was more effective as a filter than assumed. Most elements in otherValues
that do NOT eclipse the candidate were being efficiently dismissed by the pre-check in
1-2 comparisons. Without the pre-check, the inlined conditions see more elements, and
the JIT's optimization of the original eclipses() call pattern was disrupted.

The 2D regression (2387 vs 2362 baseline — within noise, but slightly below iter-003's
2427) suggests the area pre-check removal in removeEclipsed() was also not beneficial.
