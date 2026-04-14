# iter-006 Plan: Remove Redundant isFlag Check in removeEclipsed Inner Loop

## Profiler Findings Summary

### GC Allocation (dominant issue)
- 2D: **585 KB/op** allocation rate (~1380 MB/sec at 2414 ops/s)
- 3D: **562 KB/op** allocation rate (~1557 MB/sec at 2835 ops/s)
- 48-54 GC collections in 30 seconds; ~25-29 ms total pause = ~4.7% overhead
- Root cause: `moveX/moveY/moveZ` always allocate new Point subclass objects

### CPU Hotspots (2D)
| Method | % all samples | % RUNNABLE |
|--------|--------------|------------|
| removeEclipsed | 9.0% | 13.5% | ← PRIMARY TARGET |
| removeFlagged | 7.7% | 11.5% |
| add (inline) | 6.8% | 10.2% |
| sort | 6.7% | 10.1% |

### CPU Hotspots (3D)
| Method | % all samples | % RUNNABLE |
|--------|--------------|------------|
| add (inline) | 19.8% | 29.7% |
| sort | 6.6% | 9.8% |
| isEclipsed | 2.6% | 3.9% |
| constrainMax | 1.4% | 2.1% |

## Chosen Optimization

**File**: `DefaultPointCalculator2D.removeEclipsed()`

**What**: Remove `if(values.isFlag(index)) continue;` from the inner loop (index >= limit)
         and cache `unsortedMinX` and `unsortedArea` as locals outside the inner loop.

**Why it's correct**: When `removeEclipsed(added)` is called at line 453:
1. Line 437: `values.removeFlagged()` was called — ALL remaining items are unflagged
2. Line 449: `values.move(added)` shifts items right, copying flags (all false)
3. Lines 450-451: `setAll()` explicitly sets flags=false for new items [0..added-1]
4. Items [added..size-1] = original items, all unflagged after step 1
5. The inner loop iterates index = limit (=added) to size-1 — ALWAYS unflagged

The `isFlag(index)` check is **always false** in the inner loop. Removing it saves:
- One boolean array access per inner iteration
- One conditional branch per inner iteration
- Potential branch predictor improvement

Caching `unsortedMinX` and `unsortedArea` removes repeated pointer chasing
through `unsorted` object on each inner iteration.

## Before Code

```java
protected void removeEclipsed(int limit) {
    Point2DFlagList values = this.values;
    int size = values.size();

    added: for (int i = 0; i < limit; i++) {
        if(values.isFlag(i)) {
            continue;
        }
        Point2D unsorted = values.get(i);

        for (int index = limit; index < size; index++) {
            if(values.isFlag(index)) {
                continue;
            }
            Point2D sorted = values.get(index);
            if(sorted.getMinX() > unsorted.getMinX()) {
                break;
            }
            if(unsorted.getArea() <= sorted.getArea()) {
                if(sorted.eclipses(unsorted)) {
                    values.flag(i);
                    continue added;
                }
            }
        }
    }
}
```

## After Code

```java
protected void removeEclipsed(int limit) {
    Point2DFlagList values = this.values;
    int size = values.size();

    added: for (int i = 0; i < limit; i++) {
        if(values.isFlag(i)) {
            continue;
        }
        Point2D unsorted = values.get(i);
        final int unsortedMinX = unsorted.getMinX();
        final long unsortedArea = unsorted.getArea();

        for (int index = limit; index < size; index++) {
            Point2D sorted = values.get(index);
            if(sorted.getMinX() > unsortedMinX) {
                break;
            }
            if(unsortedArea <= sorted.getArea()) {
                if(sorted.eclipses(unsorted)) {
                    values.flag(i);
                    continue added;
                }
            }
        }
    }
}
```

## Expected Impact

- `removeEclipsed` is 9.0% of all samples in 2D (13.5% of RUNNABLE)
- Removing one branch + array access from tight inner loop: ~10-15% method speedup
- Expected throughput gain: ~1-2% in 2D
- 3D unaffected (uses different, inline eclipse checks)
- Combined gain estimate: +0.5% to +1.0% vs baseline (2597 ops/s)
- Expected combined: ~2610-2623 ops/s (above keep threshold of 2610)
