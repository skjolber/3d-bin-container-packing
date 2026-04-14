# iter-004 Plan

## Strategy: eclipse-check-inline-and-isEclipsedAtXX-skip

### Problem

After iter-003's early-break in `isEclipsed()`, the cost per non-eclipsing element is:
1. 1 int comparison (minX break — kept from iter-003)
2. 1 long comparison (volume pre-check)
3. Sometimes 1 more long comparison (area pre-check)
4. Rarely: 6 int comparisons in eclipses()

Two redundancies remain:
- **Volume/area pre-check**: For addZZ/addYY candidate points (smaller than existing accepted
  points), the pre-check usually PASSES (not filtering), adding 2 long comparisons overhead.
- **Redundant minX in eclipses()**: After the early-break guarantees otherValue.minX <=
  point.minX, the first condition in eclipses() is always true — 1 wasted comparison.
- **isEclipsedAtXX dead work**: Only elements with minX == xx can eclipse (since point.minX
  == xx). Elements with minX > xx cannot eclipse but are scanned and checked with expensive
  volume/area/eclipses conditions.

### Optimization 1: isEclipsed() — 3D

Cache all point fields before the loop. Remove volume/area pre-check. Inline the eclipse
check skipping the redundant minX condition.

Before:
```java
private boolean isEclipsed(SimplePoint3D point) {
    final int pointMinX = point.getMinX();
    for (int index = 0; index < otherValues.size(); index++) {
        SimplePoint3D otherValue = otherValues.get(index);
        if (otherValue.getMinX() > pointMinX) break;
        if(point.getVolume() <= otherValue.getVolume() && point.getArea() <= otherValue.getArea()) {
            if(otherValue.eclipses(point)) return true;
        }
    }
    return false;
}
```

After:
```java
private boolean isEclipsed(SimplePoint3D point) {
    final int pointMinX = point.getMinX();
    final int pointMaxX = point.getMaxX();
    final int pointMinY = point.getMinY();
    final int pointMaxY = point.getMaxY();
    final int pointMinZ = point.getMinZ();
    final int pointMaxZ = point.getMaxZ();
    final int n = otherValues.size();
    for (int index = 0; index < n; index++) {
        SimplePoint3D otherValue = otherValues.get(index);
        if (otherValue.getMinX() > pointMinX) break;
        // otherValue.minX <= pointMinX guaranteed by break — skip redundant minX check in eclipses()
        if (pointMaxX <= otherValue.getMaxX() &&
                otherValue.getMinY() <= pointMinY &&
                pointMaxY <= otherValue.getMaxY() &&
                otherValue.getMinZ() <= pointMinZ &&
                pointMaxZ <= otherValue.getMaxZ()) {
            return true;
        }
    }
    return false;
}
```

Savings per non-eclipsing element (common case):
- Remove 1-2 long comparisons (volume/area pre-check)
- Remove 1 int comparison (minX in eclipses — always true)
- Net: first check is now `pointMaxX <= otherValue.getMaxX()` (int), often fails fast

### Optimization 2: isEclipsedAtXX() — 3D

Add skip for elements with minX > xx (they cannot eclipse point with minX=xx).
Cache point fields. Remove volume/area pre-check. Inline eclipse check.

Before:
```java
private boolean isEclipsedAtXX(SimplePoint3D point, int xx) {
    for (int index = otherValues.size() - 1; index >= 0; index--) {
        SimplePoint3D otherValue = otherValues.get(index);
        if(otherValue.getMinX() < xx) return false;
        if(point.getVolume() <= otherValue.getVolume() && point.getArea() <= otherValue.getArea()) {
            if(otherValue.eclipses(point)) return true;
        }
    }
    return false;
}
```

After:
```java
private boolean isEclipsedAtXX(SimplePoint3D point, int xx) {
    final int pointMaxX = point.getMaxX();
    final int pointMinY = point.getMinY();
    final int pointMaxY = point.getMaxY();
    final int pointMinZ = point.getMinZ();
    final int pointMaxZ = point.getMaxZ();
    for (int index = otherValues.size() - 1; index >= 0; index--) {
        SimplePoint3D otherValue = otherValues.get(index);
        final int otherMinX = otherValue.getMinX();
        if (otherMinX < xx) return false;
        if (otherMinX > xx) continue;   // otherMinX > point.minX(=xx) → cannot eclipse
        // otherMinX == xx: eclipses() minX condition satisfied, inline rest
        if (pointMaxX <= otherValue.getMaxX() &&
                otherValue.getMinY() <= pointMinY &&
                pointMaxY <= otherValue.getMaxY() &&
                otherValue.getMinZ() <= pointMinZ &&
                pointMaxZ <= otherValue.getMaxZ()) {
            return true;
        }
    }
    return false;
}
```

This is the highest-impact change: turns volume/area/eclipses work for minX>xx elements
into a single int comparison (continue). These elements are scanned first (highest minX
first in backward scan) and are often the majority of the scan range.

### Optimization 3: removeEclipsed() — 2D

Cache `unsorted` fields outside inner loop. Remove area pre-check. Inline eclipse check
(skipping redundant minX condition already guaranteed by break).

### Safety Analysis
- No semantic changes: all conditions remain equivalent or trivially simplified
- The break/continue conditions preserve original loop semantics
- The inlined eclipse check is identical to `eclipses()` minus the always-true minX condition
- No test files, jmh/, or experiments/ touched

### Expected Impact
- 3D: +5-15% from eliminating pre-check overhead and isEclipsedAtXX dead work
- 2D: +2-5% from removeEclipsed inline optimization
- Combined threshold 2610 (>baseline 2597): high confidence
