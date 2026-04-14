# iter-009 Plan: Hoist loop-invariant values in 3D isEclipsed()

## Chosen Strategy: Hoist volume/area/size in isEclipsed() and isEclipsedAtXX()

### Rationale
The profiler shows 3D add() at 29.7% RUNNABLE. The dominant inner loop is
isEclipsed(), called ~50 times per add() with an inner loop of ~5-15
iterations each. Inside this inner loop, three values are recomputed on every
iteration that are actually loop-invariant:

  1. `point.getVolume()` — virtual call on polymorphic SimplePoint3D
  2. `point.getArea()`   — virtual call on polymorphic SimplePoint3D
  3. `otherValues.size()` — field read, JIT may not hoist from `this.otherValues`

SimplePoint3D has 5-6 concrete subclasses; when the JIT's call site profile shows
multiple receiver types, it may not fully hoist the call result. For otherValues.size():
the field IS modified in the outer loop (via add()), so the JIT may be conservative.
Explicit local variables guarantee hoisting.

Also fixing the inconsistency: `moveToYY.size()` and `moveToZZ.size()` are NOT cached
in their outer loops, unlike `moveToXXSize` which IS cached.

### Change
File: DefaultPointCalculator3D.java

In `isEclipsed(SimplePoint3D point)`:
- Add final long pointVolume = point.getVolume()
- Add final long pointArea = point.getArea()
- Add final int n = otherValues.size()
- Use n, pointVolume, pointArea instead of method calls in loop

In `isEclipsedAtXX(SimplePoint3D point, int xx)`:
- Add final long pointVolume = point.getVolume()
- Add final long pointArea = point.getArea()
- Use local vars in loop body

In `add(int index, Placement placement)`:
- Add int moveToYYSize before YY loop (consistent with moveToXXSize)
- Add int moveToZZSize before ZZ loop (consistent with moveToXXSize)

### Risk: Low (safe local variable hoisting, no logic change)
