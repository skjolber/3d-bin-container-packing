# Final Optimization Report: exp-001-points-throughput

## Summary

| | Value |
|---|---|
| Experiment | exp-001-points-throughput |
| Iterations run | 10 |
| Iterations kept | 4 |
| Baseline combined avg | 2597 ops/s |
| **Best combined avg** | **2666 ops/s (+2.67%)** |
| 2D: baseline → best | 2362 → 2487 ops/s (+5.3%) |
| 3D: baseline → best | 2832 → 2846 ops/s (+0.5%) |
| Target (20%) | 3116 ops/s — **not reached** |

---

## Best Configuration (iter-009)

Four changes are cumulatively active in the best build. Applied in order:

### 1. `DefaultPointCalculator3D.isEclipsed()` — early-break on minX (iter-003)
`otherValues` is maintained in non-decreasing minX order. Once `otherValue.getMinX() > point.getMinX()`, no further element can eclipse the point. Added a `break` that exits the scan early, eliminating redundant comparisons in Phase 2 add paths.

### 2. `DefaultPointCalculator2D.removeEclipsed()` — local variable caching (iter-006)
Cached `unsorted.getMinX()` and `unsorted.getArea()` as `final int`/`final long` locals before the inner loop. Eliminated repeated virtual-dispatch field reads on the hot path (removeEclipsed was 13.5% RUNNABLE).

### 3. `DefaultPointCalculator2D.add()` — merge two `removeFlagged()` into one (iter-007)
Removed the first `removeFlagged()` call (after eclipse flagging) and deferred it to a single combined call at the end of `add()`. This halves the number of O(n) array scans per `add()` invocation. `removeFlagged` was measured at 11.5% RUNNABLE; merging saved approximately one full scan per call.

### 4. `DefaultPointCalculator3D.isEclipsed()` + `isEclipsedAtXX()` — hoist volume/area/size (iter-009)
Hoisted `point.getVolume()`, `point.getArea()`, and `otherValues.size()` into `final` locals before the loop. Also cached `moveToYY.size()` and `moveToZZ.size()` before their respective loops. Eliminated repeated method calls inside tight iteration.

---

## Full Diff (vs master)

```diff
DefaultPointCalculator2D.java:
  - Remove first removeFlagged() call at ~line 437 (now done once at end)
  + Cache unsortedMinX and unsortedArea as locals in removeEclipsed inner loop
  + isFlag branch retained (needed since removeFlagged is deferred)

DefaultPointCalculator3D.java:
  + Early-break in isEclipsed() when otherValue.getMinX() > pointMinX
  + Hoist pointVolume, pointArea, n=otherValues.size() before isEclipsed loop
  + Hoist pointVolume, pointArea before isEclipsedAtXX loop
  + Cache moveToYYSize and moveToZZSize before their loops
```

---

## All Iterations

| Rank | Iteration | 2D (ops/s) | 3D (ops/s) | Combined | Delta | Strategy | Result |
|------|-----------|------------|------------|---------|-------|----------|--------|
| 1 | **iter-009** | **2487** | **2846** | **2666** | **+2.67%** | hoist-volume-area-size-in-isEclipsed | ✅ |
| 2 | iter-007 | 2458 | 2848 | 2653 | +2.15% | merge-removeflagged-calls | ✅ |
| 3 | iter-006 | 2431 | 2827 | 2629 | +1.22% | remove-redundant-isflag-in-removeeclipsed | ✅ |
| 4 | iter-003 | 2427 | 2799 | 2613 | +0.61% | isEclipsed-early-break-minX | ✅ |
| — | baseline | 2362 | 2832 | 2597 | — | — | — |
| — | iter-008 | 2458 | 2755 | 2607 | +0.37% | insertion-sort-small-lists | ❌ |
| — | iter-001 | 2385 | 2805 | 2595 | -0.07% | remove-updateindexes-conditional-plus-binary-search | ❌ |
| — | iter-005 | 2407 | 2781 | 2594 | -0.12% | isEclipsedAtXX-skip-only | ❌ |
| — | iter-002 | 2319 | 2808 | 2564 | -1.27% | merge-sorted-segments-2d | ❌ |
| — | iter-004 | 2387 | 2734 | 2560 | -1.42% | eclipse-check-inline-and-isEclipsedAtXX-skip | ❌ |
| — | iter-010 | 2336 | 2780 | 2558 | -1.50% | pre-compact-before-removeeclipsed | ❌ |

---

## Performance Analysis

The 20% target was not reached. Final improvement is **+2.67%** (combined avg).

**2D improved more than 3D** (+5.3% vs +0.5%). This is expected: iter-006, iter-007, and iter-009 targeted methods that are proportionally heavier in the 2D path.

**The gains are cumulative but sub-linear**: each successive kept change added less than the previous (+0.61% → +1.22% → +2.15% → +2.67%). The gap between each step reflects that each fix targeted a progressively smaller slice of total runtime.

**Benchmark noise (±2%)** was a significant constraint. The iteration threshold of 0.5% meant that real improvements near that level were hard to confirm reliably with a single 30s measurement window.

---

## What Worked and Why

| Strategy | Why It Worked |
|---|---|
| Early-break on minX in `isEclipsed()` | `otherValues` sorted order guarantees correctness; skips tail of scan |
| Local variable caching in `removeEclipsed()` | Eliminates repeated virtual dispatch reads on a method called per-point |
| Merge `removeFlagged()` calls | Halves O(n) compaction scans — the single clearest algorithmic saving |
| Hoist volume/area/size before loops | Prevents re-evaluation of unchanging values inside tight loops |

## What Didn't Work and Why

| Strategy | Why It Failed |
|---|---|
| Binary-search micro-opts (iter-001) | JIT already handles simple field access well; added branch overhead |
| Merge pre-sorted segments (iter-002) | Arrays.sort (TimSort) is already optimal for small n in Java |
| isEclipsedAtXX skip (iter-004, iter-005) | Method called infrequently; not on hot path |
| Insertion sort (iter-008) | Arrays.sort uses binary insertion sort for n≤32 internally — can't outperform |
| Pre-compact before removeEclipsed (iter-010) | Split the deferred `removeFlagged` back into two O(n) passes, undoing iter-007's win |

**Critical lesson (iter-001 through iter-005)**: Five iterations wasted guessing hotspots. Running the JMH GC + stack profiler in iter-006 immediately identified the real targets and produced three consecutive keeps (iter-006, 007, 009). **Always profile first.**

---

## Recommended Next Steps

Highest-potential untapped opportunities (in order):

1. **GC allocation reduction** (~5-10% potential)
   The profiler measured ~585 KB/op in 2D from `moveX()`/`moveY()` allocating new `SimplePoint` objects. Options:
   - Pre-check eclipse condition **before** allocating: if coordinates would be immediately eclipsed, skip `new`
   - Object pool: reuse `SimplePoint` instances across `add()` calls (requires `cloneOnConstrain=false` path)

2. **3D `add()` inline (29.7% RUNNABLE)**
   The 3D `add()` is a monolith. Sort is 9.8% of 3D RUNNABLE; the inner eclipse-check loops have more hoisting opportunities that weren't fully exploited in the scoped iterations.

3. **2D `removeEclipsed` branch elimination**
   Remove the `isFlag` guard from the inner loop by doing a partial `removeFlagged(limit, size)` on only the newly-inserted items before calling `removeEclipsed`. Cost: O(added_count) instead of O(n). This was attempted in iter-010 but incorrectly extended to all items, doubling scan work.

---

## Also Fixed (non-optimization)

**`PointsBenchmark2D` reset bug** (pre-experiment): The 2D JMH benchmark was calling `redo()` between invocations instead of `clear()`. `redo()` clears the values list without restoring the origin point, causing a `NullPointerException` on the second invocation. Fixed to `clear()` before establishing baseline.

---

*Generated: 2026-04-15*
