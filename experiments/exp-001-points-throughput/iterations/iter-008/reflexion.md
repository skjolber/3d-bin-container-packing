# iter-008 Reflexion: Insertion Sort Regressed 3D (-1.75% combined)

## What We Tried
Applied insertion sort in three places:
1. 3D moveToXX and moveToYY: `sortThis()` → `insertionSortThis()`
2. 2D moveToXX and moveToYY: IntArrayList → CustomIntArrayList + insertionSortThis  
3. 2D Point2DFlagList.sort(): threshold ≤ 32 → inline insertion sort

## What Happened
- 2D: **no change** (2458 ops/s, same as iter-007)
- 3D: **regression** (2755 vs 2848 = -3.3%)
- Combined: 2606.5 — below threshold 2610 → **DROPPED**

## Root Cause Analysis

### 3D regression
`moveToXX` and `moveToYY` in 3D are sorted by a comparator that does object lookups:
`CustomIntXComparator.compare(a, b)` = `values.get(a).getMinY()` vs `values.get(b).getMinY()`.

Each comparison involves two pointer chases. Insertion sort is O(n²) comparisons in worst case,
TimSort is O(n log n). For n ≈ 10-25, insertion sort does ~50-300 comparisons vs TimSort's
~40-120. The pointer-chase cost of each comparison erases the setup overhead savings.

Note: `moveToZZ.insertionSortThis()` was already there and presumably was already benchmarked
and kept — it may work because ZZ items tend to be already-sorted (x/y coordinates in order),
making insertion sort O(n) in practice. XX/YY items may not share this property.

### 2D unaffected
The `values.sort()` threshold change had no measurable effect:
- Java's `Arrays.sort` internally uses binary insertion sort for n ≤ 32 already
- Our inline insertion sort uses linear insertion (not binary), so it may actually be slower
  for n=20-32 due to more element shifts
- TimSort with n ≤ 32 in Java is already highly optimized — overhead is nearly zero

## Mistakes Made
- Assumed insertion sort always beats TimSort for small arrays — wrong
- Did not consider that Java's Arrays.sort already uses insertion sort for small n internally
- Did not profile the actual input distributions (n distribution of moveToXX etc.)
- Applied change to XX/YY without understanding why ZZ was kept with insertionSort

## What We Learned
1. Eclipse Collections `IntArrayList.sortThis()` with object-lookup comparators is already fast
2. Java `Arrays.sort` is already adaptive (binary insertion sort for n ≤ 32)
3. The "10% sort hotspot" in 2D may NOT be addressable with simple insertion sort tricks
4. Pre-sortedness matters: moveToZZ insertionSort works because ZZ items arrive nearly sorted

## Next Iteration Directions (ranked by expected impact)

### 1. GC Reduction (highest potential: 5-10%)
Every `moveX/moveY/moveZ` allocates a new SimplePoint object (~585 KB/op).
Strategy: before calling `p.moveX(xx)`, check if the resulting point would be eclipsed
(pre-check eclipse condition without allocating). If eclipsed, skip allocation.
Risk: medium (requires understanding eclipse conditions)

### 2. 3D add() inline (29.7% RUNNABLE)
Cache `otherValues.size()` before the `isEclipsed()` inner loop.
The `isEclipsed()` method calls `otherValues.size()` in the loop condition repeatedly.
This is a 1-line safe change with potentially measurable impact.

### 3. Reduce 2D removeEclipsed (13.5% RUNNABLE)
The isFlag() check inside removeEclipsed was restored in iter-007 to enable the
single-pass removeFlagged. Explore if there's a way to have both:
- No isFlag check in removeEclipsed inner loop (iter-006 win)  
- Single removeFlagged pass (iter-007 win)
This requires restructuring constraint flags to not co-exist with remove flags.

### 4. Short-circuit sort for size ≤ 1 only
Instead of full insertion sort, just skip sort when list has 0 or 1 element.
This is the safest zero-cost optimization (no regression possible).
