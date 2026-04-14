# iter-008 Plan: Insertion Sort for Small Lists

## Strategy
Apply insertion sort in three places where Arrays.sort / Eclipse-Collections TimSort
are called on typically-small lists (≤ ~20 elements).

## Changes

### 1. DefaultPointCalculator3D.java (lines 305, 349)
```
// BEFORE
moveToXX.sortThis(xxComparator);
moveToYY.sortThis(yyComparator);

// AFTER
moveToXX.insertionSortThis(xxComparator);
moveToYY.insertionSortThis(yyComparator);
```
moveToXX/moveToYY are already CustomIntArrayList (which has insertionSortThis).
moveToZZ already uses insertionSortThis — this mirrors that pattern.

### 2. DefaultPointCalculator2D.java (fields + lines 335, 355)
```
// BEFORE
protected final IntArrayList moveToYY = new IntArrayList();
protected final IntArrayList moveToXX = new IntArrayList();
...
moveToXX.sortThis(COMPARATOR_MOVE_TO_XX);
moveToYY.sortThis(COMPARATOR_MOVE_TO_YY);

// AFTER
protected final CustomIntArrayList moveToYY = new CustomIntArrayList();
protected final CustomIntArrayList moveToXX = new CustomIntArrayList();
...
moveToXX.insertionSortThis(COMPARATOR_MOVE_TO_XX);
moveToYY.insertionSortThis(COMPARATOR_MOVE_TO_YY);
```
Add import: com.github.skjolber.packing.ep.points3d.CustomIntArrayList

### 3. Point2DFlagList.java (method sort(Comparator<Point2D>, int maxSize))
Add threshold-based insertion sort for the main values sort (likely ~10% 2D hotspot):
```java
public void sort(Comparator<Point2D> comparator, int maxSize) {
    if (maxSize <= 1) return;
    if (maxSize <= 32) {
        for (int i = 1; i < maxSize; i++) {
            SimplePoint2D key = points[i];
            int j = i - 1;
            while (j >= 0 && comparator.compare(points[j], key) > 0) {
                points[j + 1] = points[j];
                j--;
            }
            points[j + 1] = key;
        }
    } else {
        Arrays.sort(points, 0, maxSize, comparator);
    }
}
```

## Rationale
- Insertion sort is 3-5x faster than TimSort for n ≤ 16, and still faster for n ≤ 32
  due to no setup overhead, better cache behavior, and JIT inlineability
- All three sorted containers have small realistic sizes for typical benchmark inputs
- Risk: very low — insertion sort is correct for any input size; fallback to Arrays.sort
  for large inputs preserves correctness
- These three changes are independent and all pass through the same code paths

## Expected gain
- 2D: ~5-8% from values.sort threshold + moveToXX/YY insertionSort
- 3D: ~3-5% from moveToXX/YY insertionSort (moveToZZ already optimal)
- Combined: target +3-5% vs iter-007, bringing combined above 2730 ops/s
