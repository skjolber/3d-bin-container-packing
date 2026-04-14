# iter-003 Summary: isEclipsed-early-break-minX

## Strategy
Add early-break in `DefaultPointCalculator3D.isEclipsed()` based on the proven
non-decreasing minX order of `otherValues`.

## Change Applied
File: `points/src/main/java/com/github/skjolber/packing/ep/points3d/DefaultPointCalculator3D.java`

Added:
1. Cache `point.getMinX()` into local `pointMinX`
2. `if (otherValue.getMinX() > pointMinX) break;` at top of scan loop

This mirrors the identical early break already present in `removeEclipsed()`.

## Results
| Metric | Baseline | iter-003 | Delta |
|--------|----------|----------|-------|
| 2D ops/s | 2362 | 2426.9 | +2.75% |
| 3D ops/s | 2832 | 2798.8 | -1.17% |
| Combined | 2597 | 2612.9 | +0.61% |

## Decision: **KEEP** ✅
- Combined 2612.9 ≥ threshold 2610 (+0.5%) → KEEP
- Combined 2612.9 < success 3116 (+20%) → success NOT reached

## Analysis
- 2D improved despite no change to 2D code — likely JIT/warmup variance
- 3D dropped slightly despite the optimization targeting 3D — also likely variance
- The change is logically sound and passes all tests
- Net combined is just above threshold; the improvement may be too small to be
  statistically significant with a single 30s benchmark run
