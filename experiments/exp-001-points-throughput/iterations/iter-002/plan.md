# iter-002 Plan

## Strategy: merge-sorted-segments-2d

### Problem

In `DefaultPointCalculator2D.add()`, after inserting new points at positions [0..newAdded-1]
and keeping the old sorted tail at [newAdded..endIndex-1], the code calls:

```java
values.sort(Point2D.COMPARATOR_X_THEN_Y, endIndex);  // O(endIndex × log(endIndex))
```

This sort covers the ENTIRE active range every time. For endIndex=20 this means ~80-100
comparisons; for endIndex=30 it's ~145. Since `add()` is called hundreds of times per
packing, this adds up.

### Key Insight

The layout before the sort has TWO sorted segments:
- **[0..newAdded-1]**: new points (addYY then addXX). Proven sorted by COMPARATOR_X_THEN_Y
  because addYY.minX < xx = addXX.minX, and each sub-group is sorted correctly.
- **[newAdded..endIndex-1]**: old sorted tail (sorted from previous iteration).

A **merge** of two sorted segments is O(n), not O(n log n).

### Implementation

**Change 1**: Add a pre-allocated merge buffer field to `DefaultPointCalculator2D`:
```java
protected SimplePoint2D[] mergeBuf = new SimplePoint2D[16];
```

**Change 2**: Add `import java.util.Arrays;`

**Change 3**: Replace the full sort with sort-small-prefix + merge:

```java
// Before (line 455-462):
endIndex += added - values.removeFlagged();
while (endIndex < values.size() && values.get(endIndex).getMinX() <= xx) {
    endIndex++;
}
values.sort(Point2D.COMPARATOR_X_THEN_Y, endIndex);

// After:
int newAdded = added - values.removeFlagged();
endIndex += newAdded;
while (endIndex < values.size() && values.get(endIndex).getMinX() <= xx) {
    endIndex++;
}
if (newAdded <= 1 || endIndex <= newAdded) {
    // trivially sorted or no tail to merge with
    values.sort(Point2D.COMPARATOR_X_THEN_Y, endIndex);
} else {
    Arrays.sort(values.getPoints(), 0, newAdded, Point2D.COMPARATOR_X_THEN_Y);
    mergeWithSortedTail(newAdded, endIndex);
}
```

**Change 4**: Add `mergeWithSortedTail(int splitPoint, int endIndex)` private method:

```java
private void mergeWithSortedTail(int splitPoint, int endIndex) {
    SimplePoint2D[] pts = values.getPoints();
    if (mergeBuf.length < splitPoint) {
        mergeBuf = new SimplePoint2D[splitPoint * 2];
    }
    System.arraycopy(pts, 0, mergeBuf, 0, splitPoint);
    int i = 0, j = splitPoint, k = 0;
    Comparator<Point2D> cmp = Point2D.COMPARATOR_X_THEN_Y;
    while (i < splitPoint && j < endIndex) {
        if (cmp.compare(mergeBuf[i], pts[j]) <= 0) {
            pts[k++] = mergeBuf[i++];
        } else {
            pts[k++] = pts[j++];
        }
    }
    // copy any remaining left elements; right elements are already in place
    System.arraycopy(mergeBuf, i, pts, k, splitPoint - i);
}
```

### Safety Analysis

- The two-sorted-segment invariant is proven in profile-snapshot.txt
- The merge algorithm is a classic stable merge: reads left half from buffer, right half in-place
- Correctness guaranteed: left half copied to buffer first, no overlap with output writes
- Falls back to `Arrays.sort` for edge cases (newAdded <= 1 or no old tail)
- 3D path is unchanged

### Expected Impact

- For typical endIndex=15-25: ~3-5× fewer sort comparisons
- If the sort accounts for ~15-25% of 2D add() time → 10-20% improvement in 2D throughput
- 3D unchanged → combined avg improvement estimated +5-10%
- Target: combined >= 2610 ops/s (threshold), hoping for >= 3116 ops/s (success)
