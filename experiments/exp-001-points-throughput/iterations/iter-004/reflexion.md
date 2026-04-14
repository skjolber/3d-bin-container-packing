# iter-004 Reflexion

## What Was Tried

Three simultaneous changes targeting eclipse-check overhead:

1. **`isEclipsed()` 3D**: Removed volume/area pre-check, inlined `eclipses()` conditions
   skipping the always-true minX check (guaranteed by iter-003's early break). Cached all
   6 point fields + `otherValues.size()` in locals before the loop.

2. **`isEclipsedAtXX()` 3D**: Added `if (otherMinX > xx) continue;` skip for elements
   with minX > xx (which cannot eclipse since point.minX = xx). Also removed pre-check
   and inlined conditions.

3. **`removeEclipsed()` 2D**: Cached `unsorted` fields outside inner loop. Removed area
   pre-check. Inlined eclipse conditions skipping redundant minX condition.

## Results

- 2D: 2387 ops/s (baseline 2362, iter-003 2427) — within noise, slightly below iter-003
- 3D: 2734 ops/s (baseline 2832, iter-003 2799) — clear regression of -3.4% vs baseline
- Combined: 2560 (-1.42% vs baseline)
- Decision: **DROPPED**

## Root Cause Analysis

### Why the volume/area pre-check removal hurt

The pre-check `point.volume <= other.volume && point.area <= other.area` filters elements
where `other` is too small (volumetrically or area-wise) to contain `point`. This is a
NECESSARY CONDITION for eclipse. When it fails, we skip the full eclipses() check.

Key insight I missed: many elements in `otherValues` come from constrained versions of
original points (with reduced maxX, maxY, or maxZ). These constrained points have SMALLER
volumes/areas than the candidate extreme points being checked. The pre-check effectively
filters them out with just 1-2 fast comparisons (64-bit field loads, likely in the same
cache line).

Without the pre-check, the inlined conditions start with `pointMaxX <= other.maxX`. For
constrained elements with reduced maxX, this fails quickly too — BUT the original pre-check
may have been failing at the volume level earlier in the short-circuit chain, and the
JIT's training data for branch prediction was tuned to the original pattern.

Additionally, removing the pre-check likely CHANGED THE HOT PATH'S JIT PROFILE: the
`eclipses()` call was a very hot, well-devirtualized call that the JIT had deeply optimized.
Replacing it with manually-inlined conditions reset those optimizations.

### Why testing multiple changes simultaneously was a mistake

By combining 3 changes at once, we can't isolate which hurt and which helped. It's
possible the `isEclipsedAtXX` skip was beneficial (algorithmic improvement), but was
overwhelmed by the pre-check removal regression. The combined result is uninterpretable
for per-change attribution.

### Why 3D regressed more than 2D

3D `isEclipsed()` is the most frequently called method (9+ call sites per add()). Even a
small per-element overhead becomes significant at the call volume in the 3D benchmark.
2D's `removeEclipsed()` is called once per placement with a different loop structure,
so the impact was smaller.

## Key Learnings

1. **Never remove volume/area pre-checks without profiling data**: These pre-checks may
   be more effective filters than they appear. The `volume` and `area` fields are cached
   longs at fixed offsets — they're cheap to access after the object is in L1 cache.

2. **Avoid touching the eclipses() JIT hot path**: The JIT spends significant effort
   optimizing frequently-called small methods. Inlining manually disrupts this.

3. **Change one thing at a time**: Three simultaneous changes made failure analysis
   impossible. Next iterations should be atomic.

4. **The isEclipsedAtXX skip (otherMinX > xx → continue) is still valid and untested
   in isolation**: This is a true algorithmic improvement (elements with minX > xx cannot
   possibly eclipse). Its isolated effect is unknown.

## Recommended Next Iteration

**iter-005: isEclipsedAtXX-skip only (isolated single change)**

Add ONLY `if (otherMinX > xx) continue;` to `isEclipsedAtXX()`. Keep all other code
(pre-checks, eclipses() calls) exactly as in iter-003.

```java
private boolean isEclipsedAtXX(SimplePoint3D point, int xx) {
    for (int index = otherValues.size() - 1; index >= 0; index--) {
        SimplePoint3D otherValue = otherValues.get(index);
        final int otherMinX = otherValue.getMinX();
        if (otherMinX < xx) return false;
        if (otherMinX > xx) continue;   // NEW: cannot eclipse (otherMinX > point.minX=xx)
        if(point.getVolume() <= otherValue.getVolume() && point.getArea() <= otherValue.getArea()) {
            if(otherValue.eclipses(point)) return true;
        }
    }
    return false;
}
```

This is:
- Minimal: 2-line change (store minX to local + the continue)
- Reversible: easy to revert
- Logically correct: a necessary condition for eclipse is otherMinX <= pointMinX = xx
- Potentially impactful: if there are often elements with minX > xx in the scan range
  (Phase 2 loop processes values[endIndex..size], many of which have minX > xx)

**Fallback if iter-005 is neutral**: Use async-profiler to measure actual hotspots in
the JMH benchmark and guide future iterations based on empirical data rather than
theoretical analysis.

**Alternative next direction**: Consider reducing object allocation in addXX/addYY/addZZ
creation (moveX/moveY/moveZ create new SimplePoint3D objects). An object pool or struct-
of-arrays approach could reduce GC pressure and improve cache locality. This is a larger
architectural change but could yield significant gains.
