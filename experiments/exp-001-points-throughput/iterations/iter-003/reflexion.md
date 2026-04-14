# iter-003 Reflexion

## What Was Tried
Added early-break in `DefaultPointCalculator3D.isEclipsed()` exploiting the proven
non-decreasing minX order of `otherValues`. When `otherValue.getMinX() > point.getMinX()`,
no further element can eclipse `point` (since `eclipses()` requires `otherValue.minX ≤
point.minX`). The same early-break already exists in `removeEclipsed()` at line 882.

## Results
- 2D: 2426.914 ops/s (+2.75% vs baseline 2362) — no 2D code changed, variance
- 3D: 2798.834 ops/s (−1.17% vs baseline 2832) — counterintuitive
- Combined: 2612.874 (+0.61%) — just above threshold, KEPT

## Analysis of Surprise: 3D Dropped Despite Targeted Optimization

The 3D degradation is likely dominated by **benchmark noise** at the current
measurement precision (single 30s benchmark run). The change is logically sound and
provably correct, but the actual runtime savings may be too small to outweigh noise:

**Why the benefit might be modest:**
1. `isEclipsed()` is called primarily in Phase 1 (first loop, i=0..endIndex-1) for
   addZZ/addYY/constrain points. For ALL of these calls, all current otherValues elements
   have minX ≤ point.minX → break NEVER fires → 0 savings, just overhead of one extra
   comparison per iteration.
2. The break only fires for Phase 2 (second loop, non-supportedYZPlane addXX points).
   These are a smaller fraction of total isEclipsed calls.
3. Even where the break fires, it only skips the second-loop portion of otherValues —
   the first-loop portion (often 40-60% of otherValues) must still be fully scanned.

**Benchmark noise:** With only 1 measurement iteration and 30s total, variance of ±2-3%
is typical. The 3D drop of 1.17% is within noise. The 2D gain of 2.75% (no code change)
also confirms high noise in this session.

## Key Learning
The early-break optimization is theoretically valid but targets only a narrow subset of
`isEclipsed()` calls (Phase 2, non-supportedYZPlane addXX). Phase 1 calls (the majority)
get zero benefit. A larger speedup requires reducing cost across ALL isEclipsed calls.

## What Wasn't Tried But Might Work

1. **Track otherValues count of elements with minX ≤ lastInsertedMinX**: Since Phase 1
   builds otherValues monotonically in minX order, we could maintain a high-water mark
   `int eclipseCheckLimit` that tracks how many elements have minX ≤ current threshold.
   Phase 2 checks only scan up to that limit. But this requires extra bookkeeping.

2. **Reduce isEclipsed call count via pre-filter**: Before calling isEclipsed(p), check
   if p is dominated by the MOST RECENTLY ADDED point to otherValues (index=size-1).
   If the last point eclipses p, skip. This is O(1) pre-filter before O(n) scan.

3. **Object allocation reduction in moveX/Y/Z**: Each addXX point creates a new
   SimplePoint3D object. A pre-allocated pool or `setMinX()` mutation on reusable objects
   would reduce GC pressure significantly for large packings. Risk: immutablePoints flag.

4. **Cache-line optimization in Point3DFlagList**: The `flag[]` boolean array and
   `points[]` array are accessed separately. For `isEclipsed()` which only reads points
   (not flags), having a single `get()` accessing a compact struct would reduce
   cache misses in tight loops.

5. **Profile-guided approach**: Run with async-profiler to measure actual hot paths
   before guessing. The JMH result variance suggests the hot method may not be isEclipsed.

## Recommended Next Iteration

**iter-004: Add O(1) most-recently-added pre-filter in isEclipsed()**

Before the full O(n) scan, check if the last-added element to otherValues eclipses point.
This targets ALL isEclipsed calls (not just Phase 2). Logic:
```java
private boolean isEclipsed(SimplePoint3D point) {
    final int n = otherValues.size();
    if (n == 0) return false;
    // Fast pre-check: if the most recently added point eclipses this one, skip full scan
    // (Most recently added has highest minX, so also acts as a minX guard for break)
    final int pointMinX = point.getMinX();
    for (int index = 0; index < n; index++) {
        SimplePoint3D otherValue = otherValues.get(index);
        if (otherValue.getMinX() > pointMinX) break;
        if(point.getVolume() <= otherValue.getVolume() && point.getArea() <= otherValue.getArea()) {
            if(otherValue.eclipses(point)) return true;
        }
    }
    return false;
}
```
Alternative: Focus on 2D improvements since 2D is further from baseline and more
malleable. The 2D `removeEclipsed()` inner loop already has the early break; look
at `updateIndexes()` or the hot `sort()` path with a smarter strategy.

**OR:** Run the benchmark multiple times (2-3 forks) to get stable measurements before
making decisions. The current noise level makes it impossible to reliably distinguish
small optimizations.
