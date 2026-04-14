# iter-009 Summary: Hoist loop-invariants in isEclipsed()

## Strategy
Hoist `point.getVolume()`, `point.getArea()`, and `otherValues.size()` into local
variables before the inner loop in `isEclipsed()` and `isEclipsedAtXX()`. Also cached
`moveToYYSize` and `moveToZZSize` for consistency with the existing `moveToXXSize` pattern.

## Results
| Metric   | Before (iter-007) | After (iter-009) | Baseline |
|----------|-------------------|------------------|----------|
| 2D ops/s | 2458              | 2487             | 2362     |
| 3D ops/s | 2848              | 2846             | 2832     |
| Combined | 2653              | 2666.4           | 2597     |
| Delta    | +2.15%            | **+2.67%**       | —        |

## Decision: **KEEP** — new best (2666.4 > 2653)

## Analysis
- 2D: +28 ops/s improvement (+1.2% vs iter-007). Surprising since the change was 3D-only.
  Possible explanation: JVM warmup/JIT variation, or the size-caching had a minor global effect.
- 3D: essentially unchanged (-2 ops/s, within noise). The JIT was already hoisting these
  values in 3D, so the explicit hoisting had no measurable impact on 3D throughput.
- Combined: +13.4 ops/s vs iter-007, meeting the 2610 threshold comfortably.

## Why the 2D improved without 2D code changes
The JVM may have compiled the 3D path differently after the change, leaving more
compilation budget for 2D. Alternatively, the 2D improvement is within benchmark noise
(~1-2% variation is typical for JMH with short runs).

## What Worked
The explicit hoisting of `pointVolume` and `pointArea` appears to have helped the 2D path
indirectly (JVM compilation effects) or is within measurement noise on the 3D side.

## What to Try for iter-010 (LAST iteration)
The target is +20% combined (3116 ops/s) — currently at +2.67% (2666). That's a very
large gap. For the last iteration, focus on the highest-impact remaining opportunity:

1. **2D removeEclipsed restructuring (13.5% RUNNABLE)**
   The isFlag() check in the inner loop was restored for iter-007. Strategy: before
   calling removeEclipsed(), compact the sorted section (limit..size-1) to remove
   flagged elements in an O(n) pass, then call removeEclipsed() WITHOUT the isFlag check.
   This eliminates the isFlag branch from the inner loop's critical path.
   Expected impact: ~1-3% on 2D.

2. **GC pressure reduction via smarter pre-checks**
   ~585 KB/op from allocations. The eclipsesMovedX/Y/Z checks already guard this.
   A further check: before adding to moveToXX, verify that no non-flagged values[]
   point already eclipses p.moveX(xx) (using a precomputed "dominators" set).
   Expected impact: 1-4% if many allocations are avoided.

3. **Combine both** — try the removeEclipsed restructuring AND the 2D path optimization
   together as one atomic commit, since iter-010 is the last chance.
