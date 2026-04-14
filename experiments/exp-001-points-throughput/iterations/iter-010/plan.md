# iter-010 Plan: Pre-compact old items before removeEclipsed to eliminate isFlag branch

## Chosen Strategy: removeFlaggedFrom(added) before removeEclipsed

### Rationale
The profiler shows `removeEclipsed` at **13.5% RUNNABLE in 2D** — the biggest remaining
2D hotspot. Its inner loop iterates over old items [limit..size-1] with:

```java
if(values.isFlag(index)) {
    continue;
}
```

This branch exists because `removeFlagged()` is deferred to the end of `add()` (iter-007
optimization). To remove the branch, we pre-compact the old section [added..size-1] before
`removeEclipsed()` is called, using a new `removeFlaggedFrom(int fromIndex)` method.

The combined work is unchanged: instead of one `removeFlagged()` at the end doing O(n),
we do one `removeFlaggedFrom(added)` before removeEclipsed + one `removeFlagged()` after.
Total O(n) work is the same, but the inner O(n²) loop no longer needs the isFlag check.

### Changes

**File: Point2DFlagList.java**
Add `removeFlaggedFrom(int fromIndex)`:
- Compacts [fromIndex..size-1] in-place (same logic as removeFlagged but starting at fromIndex)
- Returns count of items removed

**File: DefaultPointCalculator2D.java**

In `add()`, replace:
```java
removeEclipsed(added);
// Single removeFlagged pass ...
endIndex += added - values.removeFlagged();
```
with:
```java
int oldFlaggedRemoved = values.removeFlaggedFrom(added);
removeEclipsed(added);
endIndex += added - oldFlaggedRemoved - values.removeFlagged();
```

In `removeEclipsed()`, remove inner isFlag block (3 lines):
```java
// REMOVE:
if(values.isFlag(index)) {
    continue;
}
```

### Risk: Low
- Correctness: total items removed is identical (oldFlaggedRemoved + newFlaggedRemoved = original)
- endIndex formula is algebraically equivalent
- removeEclipsed order preserved; early-break still valid (items remain sorted after compaction)
- Tests verify correctness

### Expected impact
2D: +1-3% (remove branch from 13.5% RUNNABLE hotspot)
3D: Neutral (no 3D code changed)

### Fallback (if >15 lines or tests fail)
Cache `values.size()` in the while loop at line 459 of add().
