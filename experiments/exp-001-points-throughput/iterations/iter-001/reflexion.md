# iter-001 Reflexion

## What Was Tried
Removed the `if(p.getIndex() != i)` guard in `updateIndexes()` (both 2D and 3D) and
replaced a linear while-loop with binary search for extending `endIndex` in 2D.

## Results
- 2D: +0.97% (2362 → 2384.953)
- 3D: -0.94% (2832 → 2805.421)
- Combined: -0.07% (2597 → 2595) — BELOW threshold, DROPPED

## What Was Surprising
The 3D benchmark DECREASED despite the same updateIndexes() simplification that
helped 2D. The unconditional `setIndex(i)` write may cause extra cache line dirtying
when the index hasn't changed, negatively impacting the JVM's memory sub-system.
The single-benchmark iteration also means variance (±2-3%) can mask small improvements.

## Key Learnings
1. The `updateIndexes()` change is too small (or neutral in 3D) to cross the 0.5% bar alone.
2. Binary search for endIndex shows promise in 2D (+1%) but alone is insufficient.
3. JIT may already optimize the conditional away, making the guard effectively free.
4. Benchmark variance with -f 1 -wi 1 -i 1 is high (~2-3%), meaning small improvements
   are hard to distinguish from noise without more iterations.
5. The optimization target must be algorithmic (O-class improvement) not micro-optimization.

## Concrete Next Steps for iter-002

### High-priority candidates:
1. **Reduce O(n²) work in isEclipsed() in 3D** — called 10+ times per add(), each O(n).
   Approach: Since the new points being checked often have the SAME minX as the current
   source point (values[i].minX), we could batch-check them at the right position and
   leverage a sorted structure.

2. **Avoid full re-sort in 2D add()** — `values.sort(COMPARATOR_X_THEN_Y, endIndex)` sorts
   [0, endIndex) on every add. The new points (0..added-1) come from move operations that
   keep the same X coordinate as their source (or higher). An insertion-sort over the
   small set of new points would be O(added × endIndex) vs O(endIndex × log(endIndex)).
   However, for small `added` (typically 0-3), insertion would help only if endIndex >> added.

3. **Avoid Point allocation in moveX/moveY** — each `p.moveX(xx, placement)` creates a
   new point object. If we had a fixed pool of points, allocation pressure could be reduced.
   This would require significant refactoring.

4. **Batch-sort the new points FIRST before inserting** — currently new XX and YY points
   are placed at positions 0..added-1 (YY) and added..added+XX-1 (XX). They're both
   small sorted groups. Instead of sorting the full 0..endIndex range, could we merge
   three sorted runs: addYY, addXX, and the sorted tail? A merge is O(endIndex) vs
   O(endIndex log endIndex).

5. **Smarter 3D isEclipsed()** — consider tracking the minimum minX among all points
   in otherValues; if that min > new_point.minX, no eclipser exists. But this requires
   maintaining a separate data structure.

### Recommended next iteration:
Try the sorted merge approach for 2D (replace full sort with merge of sorted segments).
This could cut the sort from O(n log n) to O(n) for the hot path.
