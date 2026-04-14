# iter-002 Reflexion

## What Was Tried
Replaced the O(n log n) `Arrays.sort` in `DefaultPointCalculator2D.add()` with an
explicit half-buffered merge. The insight was that before the sort, the array already
has two sorted segments: [0..newAdded-1] (new points) and [newAdded..endIndex-1] (old
sorted tail). A merge of two sorted segments is O(n), not O(n log n).

## Results
- 2D: 2319.365 ops/s (−1.80% vs baseline 2362)
- 3D: 2808.409 ops/s (−0.83% vs baseline 2832)
- Combined: 2563.887 (−1.27%) — BELOW threshold, DROPPED

## Root Cause of Failure

**Tim Sort already handles this pattern efficiently.**

Java's `Arrays.sort` for object arrays uses Tim Sort. Tim Sort internally:
1. Detects runs (contiguous sorted sequences)
2. For small arrays (< 32 elements), uses binary insertion sort  
3. Merges detected runs with a highly-optimized internal merge

For our case (small unsorted prefix + large sorted tail), Tim Sort:
- Detects the sorted tail as one run: O(n)
- Applies binary insertion sort on the tiny prefix: O(newAdded²) ≈ O(1)
- Merges them: O(n)
- Total: O(n)

My explicit merge replaced Tim Sort's optimized O(n) with my O(n) but with:
- An extra `System.arraycopy` for the merge buffer
- An interface-dispatch `Comparator.compare()` in a tight loop (vs Tim Sort's inlined comparisons)
- The `Arrays.sort` call on the prefix (redundant: Tim Sort already handles it internally)

The constant factor of my implementation was WORSE than Tim Sort's.

## Key Learning
**Do not try to replace Java's Tim Sort for nearly-sorted data.** Tim Sort is specifically
designed for real-world data that tends to be partially sorted. Its O(n) behavior on
nearly-sorted arrays is already optimal. Any explicit merge needs to beat the JVM's
highly-tuned Tim Sort implementation, which is very hard to do.

## What Wasn't Tried But Might Work

1. **3D isEclipsed() early break**: The `isEclipsed()` method scans all of `otherValues`
   for potential eclipsers. Since `otherValues` is built in non-decreasing `minX` order
   (proven: each insertion position is determined by COMPARATOR_X_THEN_Y_THEN_Z, and the
   while loop only advances past elements with smaller sort-order than the inserted point),
   adding `if (otherValue.getMinX() > point.getMinX()) break;` would terminate early.
   For addYY/addZZ/constrain points (minX = source.minX = small), this could skip a
   large fraction of the otherValues list.

2. **Reduce object allocation in moveX/moveY/moveZ**: Each call to `p.moveX(xx, placement)`
   creates a new `SimplePoint3D` object. With dozens of box placements per packing run,
   this creates garbage. A point pool (pre-allocated array of reusable points) could reduce
   GC pressure, especially in 3D.

3. **2D: Avoid full removeFlagged() pass**: Currently called twice per `add()`. The second
   call removes eclipsed new points. If the removeEclipsed() function returned the count
   of removed items and also compacted in-place, we'd save one pass.

4. **Cache-friendly flag check**: The `flag[]` boolean array in `Point2DFlagList` and
   `Point3DFlagList` is accessed separately from `points[]`. Packing them together (a
   struct-of-arrays to array-of-structs transformation) might improve cache locality in
   hot loops.

## Recommended Next Iteration

**iter-003: 3D isEclipsed() early-break based on sorted minX**

The `isEclipsed()` method is called for every new point in 3D. Adding
`if (otherValue.getMinX() > point.getMinX()) break;` (proven safe: otherValues.minX
is non-decreasing) would reduce scan work proportional to the fraction of otherValues
elements with minX > point.minX. For addYY/addZZ points (typically minX = small source
value), many later otherValues elements would be skipped. This is a safe, targeted change
with no algorithmic correctness risk.
