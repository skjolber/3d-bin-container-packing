# iter-008 Summary: Insertion Sort for Small Lists

## Changes Applied
1. **DefaultPointCalculator3D**: moveToXX and moveToYY changed from `sortThis` to `insertionSortThis`
2. **DefaultPointCalculator2D**: moveToXX and moveToYY changed to `CustomIntArrayList` + `insertionSortThis`
3. **Point2DFlagList.sort()**: Added threshold ≤ 32 → inline insertion sort, else Arrays.sort

## Results
| Metric | iter-007 | iter-008 | Delta |
|--------|----------|----------|-------|
| 2D ops/s | 2458 | 2458 | ±0 |
| 3D ops/s | 2848 | 2755 | **-3.3%** |
| Combined | 2653 | 2606.5 | **-1.75%** |
| vs baseline | +2.15% | +0.37% | regression |

## Decision: **DROP**
Combined 2606.5 < threshold 2610. 3D regressed significantly (-93 ops/s).

## Analysis

### Why 3D got worse
The insertion sort change for `moveToXX.sortThis(xxComparator)` and `moveToYY.sortThis(yyComparator)`
regressed 3D by ~3.3%. Hypothesis:
- Eclipse Collections' `IntArrayList.sortThis()` is highly optimized for int primitives  
  (uses a sort with unboxed primitives, no object allocation)
- The `CustomIntArrayList.insertionSortThis()` calls `comparator.compare()` which goes through
  `CustomIntXComparator.compare()` which does two pointer chases (values.get(i), values.get(j))
  for each comparison — insertion sort does more comparisons than TimSort's merge phase for
  larger inputs
- The moveToXX/moveToYY lists may be larger than assumed (~15-30 elements), making insertion
  sort slower than TimSort
- The JIT may have de-optimized due to type change (plain IntArrayList → CustomIntArrayList)

### Why 2D was unaffected
The 2D change (both moveToXX/YY and values.sort threshold) had zero net effect:
- moveToXX/moveToYY in 2D were contributing very little to the 10.1% sort hotspot
- The main 10.1% hotspot is `values.sort(Point2D.COMPARATOR_X_THEN_Y, endIndex)` at line 463,
  which now uses insertion sort for endIndex ≤ 32, but the improvement was negligible
- Possible reason: JIT already optimizes `Arrays.sort` with a known small-size call pattern,
  or the threshold is too high (should be ≤ 16 not ≤ 32 to avoid regression)

## Key Lesson
- `insertionSortThis` for Eclipse Collections IntArrayList with Object comparators (pointer chases)
  is NOT necessarily faster — the extra comparisons vs merge sort offset any setup savings
- The existing `moveToZZ.insertionSortThis()` may be beneficial only because ZZ items
  are sorted by x-coordinate (already nearly sorted), making insertion sort O(n)
  vs XX/YY which may be less ordered
- Do NOT blindly apply insertion sort — pre-sortedness of input matters greatly

## Next Iteration Hints
1. **Revert this**: Current revert already done
2. **Target GC allocation (5-10% potential)**: moveX/moveY/moveZ always allocate new Point objects
   (~585 KB/op in 2D, ~562 KB/op in 3D). Avoid allocation by checking eclipse condition FIRST
3. **3D add() inline (29.7% RUNNABLE)**: Cache `otherValues.size()` in local variable,
   skip full eclipse check for obviously non-eclipsed points
4. **2D endIndex sort**: Try threshold ≤ 8 (not ≤ 32) — Java's TimSort already uses binary
   insertion sort internally for n ≤ 32; our overhead from calling comparator.compare() via
   virtual dispatch may negate gains for n > 8
