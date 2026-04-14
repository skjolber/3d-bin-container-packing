# iter-005 Plan

## Strategy: isEclipsedAtXX-skip-only

### Problem
`isEclipsedAtXX()` in `DefaultPointCalculator3D` iterates *backward* through `otherValues`.
When `point.minX == xx`, any `otherValue` with `otherMinX > xx` cannot satisfy the eclipse
condition (`other.minX <= point.minX` is required). Yet currently those elements go through
the volume/area pre-check and potentially `eclipses()` before being rejected. Adding a
`continue` for `otherMinX > xx` avoids this waste.

This was identified as the isolated high-value change in iter-004's reflexion. iter-004
combined it with three other changes; this iteration tests it ALONE.

### Change: ONE atomic logical addition

**File:** `points/src/main/java/com/github/skjolber/packing/ep/points3d/DefaultPointCalculator3D.java`  
**Method:** `isEclipsedAtXX()` (line 675)

**Before:**
```java
private boolean isEclipsedAtXX(SimplePoint3D point, int xx) {
    // check if one of the existing values contains the new value
    for (int index = otherValues.size() - 1; index >= 0; index--) {
        SimplePoint3D otherValue = otherValues.get(index);
        if(otherValue.getMinX() < xx) {
            return false;
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

**After:**
```java
private boolean isEclipsedAtXX(SimplePoint3D point, int xx) {
    // check if one of the existing values contains the new value
    for (int index = otherValues.size() - 1; index >= 0; index--) {
        SimplePoint3D otherValue = otherValues.get(index);
        final int otherMinX = otherValue.getMinX();
        if(otherMinX < xx) {
            return false;
        }
        if(otherMinX > xx) {
            continue;
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

### Diff summary
- Cache `getMinX()` call into `final int otherMinX` (local variable, replaces inline call)
- Add `if(otherMinX > xx) { continue; }` guard before volume/area pre-check
- ALL other code unchanged

### Correctness
`eclipses(point)` requires `other.minX <= point.minX = xx`. When `otherMinX > xx`, this
necessary condition fails, so skipping is safe.

### Expected impact
- 3D: moderate gain — elements with minX > xx are scanned first in the backward pass.
  Skipping their volume/area/eclipses work reduces per-element cost.
- 2D: no change (no equivalent method)
- Combined: aim >= 2610 (threshold) for KEEP
