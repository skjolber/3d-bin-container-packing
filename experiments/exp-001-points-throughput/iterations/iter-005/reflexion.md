# iter-005 Reflexion

## What Was Tried

Single atomic change: added `if(otherMinX > xx) continue;` guard in `isEclipsedAtXX()`
in `DefaultPointCalculator3D`. The `otherMinX` value was cached from the existing
`otherValue.getMinX()` call. All other code left untouched.

## Results

- 2D: 2407 ops/s (baseline 2362, iter-003 best 2427)
- 3D: 2781 ops/s (baseline 2832, iter-003 best 2799)
- Combined: 2594 (-0.12% vs baseline 2597)
- Threshold: 2610 — **not reached**
- Decision: **DROPPED**

## Root Cause Analysis

### Why the guard didn't help

The change is logically correct: elements with `otherMinX > xx` cannot satisfy `eclipses()`
when `point.minX = xx`. However, the empirical results show it didn't improve performance.

**Hypothesis 1: isEclipsedAtXX() is called rarely**
Looking at the call sites (lines 563, 590), `isEclipsedAtXX` is only called during Phase 2
(adding constrained points from addYY/addZZ). These are minority cases in the benchmark.
If the method is called infrequently, any per-call improvement is negligible to the total.

**Hypothesis 2: Low `otherMinX > xx` hit rate**
When `isEclipsedAtXX` is called, the backward scan starts at the highest minX elements.
But the `otherValues` list may have been already filtered/sorted such that most elements
already have `otherMinX == xx`. If elements with `otherMinX > xx` are rare in the scan,
the `continue` guard fires rarely and the new branch just adds overhead.

**Hypothesis 3: JIT disruption**
Adding a second int comparison branch (`> xx`) in the same loop as the existing `< xx`
check may cause the JIT to produce slightly less optimal code (e.g., merging conditions,
changed register allocation), adding marginal overhead.

**Hypothesis 4: isEclipsedAtXX() not a hotspot**
The major hotspot is likely `isEclipsed()` (called at many sites in add()) not
`isEclipsedAtXX()`. Optimizing a non-hotspot yields no measurable gain.

## Key Learnings

1. **`isEclipsedAtXX()` is not a bottleneck**: Two iterations (iter-004, iter-005) have
   targeted this method without gain. It should not be revisited.

2. **The main loop in `isEclipsed()` is where gains lie**: iter-003 (+0.61%) succeeded
   by adding an early-break there. That's the true hotspot.

3. **Next priority: profile-driven approach**: Without actual profiler data, we're guessing
   at hotspots. The most valuable next step is to use async-profiler or JMH -prof stack
   to measure where time is actually spent.

4. **Theoretical "skip" optimizations have run out**: We've tried isEclipsed early-break
   (kept), isEclipsed inline + pre-check removal (dropped), isEclipsedAtXX skip (dropped
   twice). The remaining micro-optimizations in this class are likely below noise level.

## Recommended Next Iteration

**iter-006: Explore different strategy area**

Options ranked by expected impact:

**A. In `isEclipsed()` main loop: cache `otherValues.size()` before the loop**
```java
final int n = otherValues.size();
for (int index = 0; index < n; index++) {
```
This is truly minimal (1 line change), avoids repeated field access on the ArrayList.
Risk: JIT likely already hoists this — may be neutral.

**B. In `add()` 3D: Reduce object creation in addXX/addYY/addZZ**
The `moveX`, `moveY`, `moveZ` methods create new `SimplePoint3D` objects. An object pool
could reduce GC pressure. High complexity, high risk of breaking things.

**C. Use async-profiler to identify actual hotspots**
Run `java -jar benchmark.jar ... -prof async:libPath=/path/to/async-profiler.so` to
get flame graph data. This removes guesswork entirely.

**D. In `removeEclipsed()` 2D: cache `values.get(i)` to avoid double lookup**
The 2D side showed +1.9% vs baseline in iter-005 (may be noise), suggesting the 2D
path has room. Look for redundant list accesses in the 2D eclipsed-removal loop.

**Recommended: Try option A first (size caching in isEclipsed) — truly minimal risk.**
If neutral, proceed to option D (2D path optimization).
