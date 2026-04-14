# iter-003 Plan

## Strategy: isEclipsed-early-break-minX

### Problem

In `DefaultPointCalculator3D.add()`, `isEclipsed(point)` is called for each candidate point
to check whether it is dominated by an already-added output point. The method scans ALL of
`otherValues` linearly (O(n)):

```java
private boolean isEclipsed(SimplePoint3D point) {
    for (int index = 0; index < otherValues.size(); index++) {
        SimplePoint3D otherValue = otherValues.get(index);
        if(point.getVolume() <= otherValue.getVolume() && point.getArea() <= otherValue.getArea()) {
            if(otherValue.eclipses(point)) {
                return true;
            }
        }
    }
    return false;
}
```

### Key Insight

`otherValues` is populated in non-decreasing minX order (proven in profile-snapshot.txt):
- First loop (i=0..endIndex-1) emits points with minX = values[i].minX (non-decreasing)
- Second loop (i=endIndex..size-1) interleaves addXX[i] (minX=xx) and values[i] (minX≥xx)
  at their COMPARATOR_X_THEN_Y_THEN_Z sorted position → still non-decreasing minX

`Point.eclipses(point)` requires `this.minX ≤ point.getMinX()`. Therefore, once
`otherValue.getMinX() > point.getMinX()`, **no subsequent element** in otherValues can eclipse
`point` (sorted order guarantees all later elements have the same or larger minX).

### Early Break Is Valid

- Condition `if (otherValue.getMinX() > point.getMinX()) break;` is correct
- otherValues sorted in non-decreasing minX → all subsequent elements can also not eclipse
- `removeEclipsed()` (line 882) uses the identical break for the same reason ✓

### When Does It Help?

The break fires when scanning reaches an element with minX > point.minX.

**For isEclipsed calls in Phase 1 (first loop):** point.minX = values[i].minX. At call time all
existing otherValues elements have minX ≤ values[i].minX. Break never fires. Extra comparison
per iteration is a well-predicted false branch → near-zero overhead.

**For isEclipsed calls in Phase 2 (second loop for non-supportedYZPlane addXX points):**
point.minX = xx. Once second-loop values[j] (minX > xx) appear in otherValues, the break
fires. In practice values[endIndex].minX ≥ xx, often > xx → break fires after the Phase 1
tail, skipping all Phase 2 elements with minX > xx. Savings typically 25-40%.

### File to Change

`/home/thomas/git/3d-bin-container-packing2/points/src/main/java/com/github/skjolber/packing/ep/points3d/DefaultPointCalculator3D.java`

Method: `isEclipsed(SimplePoint3D point)` at line 655.

### Before

```java
private boolean isEclipsed(SimplePoint3D point) {
    // check if one of the existing values contains the new value
    for (int index = 0; index < otherValues.size(); index++) {
        SimplePoint3D otherValue = otherValues.get(index);

        if(point.getVolume() <= otherValue.getVolume() && point.getArea() <= otherValue.getArea()) {
            if(otherValue.eclipses(point)) {
                // discard 
                return true;
            }
        }
    }
    return false;
}
```

### After

```java
private boolean isEclipsed(SimplePoint3D point) {
    // check if one of the existing values contains the new value
    // otherValues is built in non-decreasing minX order; once minX exceeds point.minX
    // no subsequent element can eclipse point (eclipses() requires minX <= point.minX)
    final int pointMinX = point.getMinX();
    for (int index = 0; index < otherValues.size(); index++) {
        SimplePoint3D otherValue = otherValues.get(index);
        if (otherValue.getMinX() > pointMinX) {
            break;
        }
        if(point.getVolume() <= otherValue.getVolume() && point.getArea() <= otherValue.getArea()) {
            if(otherValue.eclipses(point)) {
                // discard 
                return true;
            }
        }
    }
    return false;
}
```

### Safety Analysis

- Only `isEclipsed()` is modified, no structural changes
- The break is guarded by the proven sorted-order invariant
- `isEclipsedAtXX()` is unchanged (already has its own backward-scan early exit)
- `removeEclipsed()` is unchanged (already has this break)
- No test files touched; no jmh/ or experiments/ files touched

### Expected Impact

- 3D throughput: +5-15% (second-loop addXX isEclipsed calls scan 25-40% fewer elements)
- 2D throughput: unchanged (2D has no isEclipsed() method)
- Combined improvement vs baseline 2597: estimated +2.5-7.5%
- Target threshold 2610 (combined avg +0.5%): high confidence of clearing
- Success target 3116: unlikely from this single change alone
