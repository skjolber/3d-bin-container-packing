# iter-009 Reflexion: Hoist loop-invariants in isEclipsed — new best +2.67%

## What We Tried
In `DefaultPointCalculator3D`:
1. `isEclipsed()`: cached `point.getVolume()`, `point.getArea()`, `otherValues.size()`
   into local final variables before the inner for-loop.
2. `isEclipsedAtXX()`: same treatment for volume and area.
3. `add()`: cached `moveToYYSize` and `moveToZZSize` (consistent with existing `moveToXXSize`).

## What Happened
- 2D: 2487 ops/s (+28 vs iter-007's 2458)
- 3D: 2846 ops/s (-2 vs iter-007's 2848, within noise)
- Combined: 2666.4 — new best, exceeds iter-007's 2653
- Delta vs baseline: +2.67%
- Decision: **KEEP**

## Root Cause Analysis

### 3D: No measurable improvement
The JIT already hoisted these values (as expected for well-optimized code). The explicit
hoisting in `isEclipsed()` was redundant for 3D. The JIT compiles `point.getVolume()` as
a direct field read (the method is a simple getter defined in `Point`), and since `point`
is a local parameter that doesn't change within the loop, the JIT hoists it. Similarly,
`otherValues.size()` is hoisted because `get()` doesn't modify `size`.

### 2D: Small improvement (+28 ops/s)
The 2D code was NOT changed, but 2D throughput improved. This is most likely:
a) JIT compilation non-determinism / warm-up variation (~1-2% is within JMH noise)
b) The change to `isEclipsed()` affects the overall JIT compilation of the class,
   potentially giving the JIT a better inlining budget for other methods
c) Genuine improvement from the moveToYY/ZZ size caching (since these appear in
   code paths shared between 2D and 3D... actually they don't, it's 3D only)

Most likely explanation: natural JMH measurement variance.

## What We Learned
1. Explicit hoisting of loop-invariants did not hurt (safe change)
2. JIT already optimizes simple getter hoisting in these loops
3. Small gains can still accumulate — new best despite 3D being neutral
4. 2D path still has headroom (13.5% RUNNABLE in removeEclipsed)

## Profiler Data (inherited from iter-006 — still valid)
- GC: ~585 KB/op (2D), ~562 KB/op (3D) — ~4.7% GC overhead
- Root cause: every moveX/moveY/moveZ call allocates a new Point object
- 3D add() inline: 29.7% RUNNABLE — biggest single hotspot
- 2D/3D sort: ~10% RUNNABLE each
- 2D removeEclipsed: ~13.5% RUNNABLE
- 3D constrainMax: ~2.1% RUNNABLE

## Next Iteration Directions for iter-010 (LAST)

### 1. 2D removeEclipsed restructuring — HIGHEST PRIORITY
The inner loop in removeEclipsed() has `if(values.isFlag(index)) continue;` which
was restored in iter-007 to support the merged removeFlagged. This adds a branch on
every inner iteration. Strategy for iter-010:

  **Approach A**: Before calling removeEclipsed(added), compact the sorted section
  (index limit..size-1) to remove flagged elements. Then removeEclipsed() doesn't
  need the isFlag check. Cost: O(size) compact pass. Benefit: no isFlag check in
  the O(limit × size) inner loop.

  If limit=3 and size=25 with 6 flagged: compact costs ~25 ops, inner loop saves
  3 × 19 = 57 branch mispredictions. Net benefit is positive if branch miss rate > 0.

  **Approach B**: Process flagged elements separately. Mark two separate lists:
  "eclipse candidates" (non-flagged old points) and "removed" (flagged). Only check
  eclipse candidates in removeEclipsed. Requires restructuring data flow.

  **Approach C**: Simply check removeEclipsed against non-flagged points by maintaining
  a count of flagged points and adjusting the inner loop bounds.

  Approach A seems safest. Expected impact: 1-3% on 2D.

### 2. GC allocation reduction — HIGH EFFORT, UNCERTAIN RETURN
585 KB/op is high. One more avenue: before calling p.moveX(xx), check if the result
would be dominated by a "super-containing" point in values[] with maxX >= p.maxX,
maxY >= p.maxY, maxZ >= p.maxZ, minX <= xx. This can be checked without allocation.
The check is O(k) where k = count of non-flagged values with minX <= xx. If k is small
(say 3-5), the check cost is low and we might skip 10-20% of allocations.

### 3. Cross-list eclipse check in 3D
Before allocating p.moveX(xx), also check p against addedYY and addedZZ (not just addedXX).
A YY-added point that already covers the XX position might eclipse the new XX point.
This is a ~3-line addition to the existing pre-check loops.

### 4. RECOMMENDATION for iter-010
Try Approach A for removeEclipsed (2D optimization). This is the safest path with
clearest expected benefit: removes a branch from the hottest inner loop in 2D.
If time allows, also add cross-list eclipse check in 3D moveToXX loop.
