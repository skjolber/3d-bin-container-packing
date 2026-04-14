# iter-001 Plan

## Strategy: Micro-optimize hot loops in updateIndexes() + binary search for endIndex

### Problem
Two identified hot-path inefficiencies:

1. **updateIndexes()** (both 2D and 3D): Called once per `add()`. For each point, does:
   - `p.getIndex()` virtual call
   - integer comparison `!= i`
   - conditional branch
   - `p.setIndex(i)` only if changed
   Nearly all indices change after a sort, so the conditional almost always evaluates to true.
   Removing the guard eliminates 2 operations per iteration.

2. **while loop in 2D add()**: After removeFlagged() the code scans forward to extend
   `endIndex` past all points with `minX <= xx`. The scanned portion is the already-sorted
   tail of the array, so a binary search gives O(log k) vs O(k).

### Implementation

**Change 1: DefaultPointCalculator2D.updateIndexes()**
```java
// Before:
protected void updateIndexes(Point2DFlagList values) {
    for(int i = 0; i < values.size(); i++) {
        SimplePoint2D p = values.get(i);
        if(p.getIndex() != i) {
            p.setIndex(i);
        }
    }
}

// After:
protected void updateIndexes(Point2DFlagList values) {
    for(int i = 0; i < values.size(); i++) {
        values.get(i).setIndex(i);
    }
}
```

**Change 2: DefaultPointCalculator3D.updateIndexes()**
Same change as above but for SimplePoint3D.

**Change 3: DefaultPointCalculator2D.add() - replace while loop with binary search**
```java
// Before:
while (endIndex < values.size() && values.get(endIndex).getMinX() <= xx) {
    endIndex++;
}

// After:
endIndex = Point2DFlagList.binarySearchPlusMinX(values.getPoints(), endIndex, values.size(), xx);
```

### Safety Analysis
- Change 1 & 2: Purely equivalent - same result, fewer operations per loop iteration.
  No algorithmic change.
- Change 3: Equivalent since values[endIndex..size-1] is the original sorted tail.
  The binary search finds the same first exclusive index where minX > xx.

### Expected Impact
- Changes 1+2 save ~2 ops/element/add-call in updateIndexes
- Change 3 reduces O(k) scan to O(log k) for the endIndex extension
- Combined: ~2-5% throughput improvement over baseline
- Target: combined >= 2610 ops/s (threshold), ideally >= 3116 ops/s (success)
