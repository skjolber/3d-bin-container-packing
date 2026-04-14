# iter-001 Summary

## Results
| Metric | Value |
|--------|-------|
| 2D score | 2384.953 ops/s |
| 3D score | 2805.421 ops/s |
| combined avg | 2595.19 ops/s |
| baseline combined | 2597 ops/s |
| delta vs baseline | -0.07% |
| threshold (keep) | 2610 ops/s (+0.5%) |

## Strategy Applied
1. Removed `if(p.getIndex() != i)` conditional guard in `updateIndexes()` for both 2D and 3D
2. Replaced O(k) while-loop with O(log k) binary search in `DefaultPointCalculator2D.add()` for extending endIndex past `xx`

## Decision: DROP
Combined (2595) < threshold (2610). Reverting changes.

## Analysis
- 2D: +22.953 ops/s (+0.97%) — binary search + updateIndexes change helped slightly
- 3D: -26.579 ops/s (-0.94%) — slight regression, likely measurement noise or
  the unconditional setIndex() writes causing extra cache line invalidation
- Net: essentially neutral / within measurement variance

The optimization was too small to cross the threshold even with favorable 3D noise:
2D alone with +23 ops/s and 3D stable would give ~(2384 + 2832)/2 = 2608, still <2610.
