# iter-010 Reflexion: isFlag branch removal via pre-compaction — negative result

## What We Tried
Added `removeFlaggedFrom(int fromIndex)` to compact [added..size-1] before
`removeEclipsed()`, removing the `if(values.isFlag(index)) continue;` branch from the
inner loop. Combined O(n) work was meant to stay the same.

## What Happened
- 2D: **2336 ops/s** (−6.1% vs iter-009's 2487, −1.10% vs baseline)
- 3D: **2780 ops/s** (−2.3% vs iter-009's 2846, −1.84% vs baseline)
- Combined: **2558** — well below threshold (2610) and baseline (2597)
- Decision: **DROPPED**

## Root Cause Analysis

### Double-scanning of old items
The original iter-007 optimization explicitly merged two `removeFlagged()` calls into
ONE deferred pass. This iter-010 split it back into two passes:
1. `removeFlaggedFrom(added)` before removeEclipsed — scans [added..size-1]
2. `removeFlagged()` after removeEclipsed — scans ALL [0..size-1] again

So the old section [added..size-1] is scanned TWICE. This doubles the O(n) work for
the most expensive part of the list (which tends to be larger than the new items section).

### Branch predictor efficiency
The `isFlag(index)` branch in the inner loop is highly predictable:
- Most iterations of the inner loop hit NON-flagged items (the list is sparsely flagged)
- Modern CPUs predict "not taken" with >95% accuracy for sparse flags
- Branch mispredict cost is ~15 cycles; 5% miss × 15 = 0.75 cycles/iteration overhead
- Not worth an extra O(n) pass

### 3D regression
3D code was NOT changed. The 3D regression (−2.3%) is likely JMH measurement noise
(±1-2% is normal) but could also reflect that the JIT's overall compilation of the
class changed slightly due to the method additions.

## What We Learned
1. Iter-007's single-deferred-removeFlagged was a key optimization — do not split it
2. Branch prediction is efficient for sparse flag patterns — `isFlag` check is cheap
3. The `removeEclipsed` inner loop branch is NOT a real bottleneck despite profiler showing 13.5% RUNNABLE — the profiler likely attributes time to the surrounding loop body (eclipses call), not the isFlag check itself
4. Adding extra O(n) pre-passes is expensive in tight loops

## What Would Be Most Promising Next (if there were an iter-011)

### 1. GC allocation reduction — the TRUE remaining opportunity
Combined GC: ~585 KB/op (2D), ~562 KB/op (3D). Every `p.moveX()`, `p.moveY()` allocates.
Approach: Add a pre-check before allocation — if a point would be eclipsed by an
existing point in the new-items list (addXX/addYY), skip the allocation entirely.
This is O(k) check before an allocation, where k = size of addXX/addYY so far.
Some allocations could be avoided without any structural changes.

### 2. 3D add() inline loop (29.7% RUNNABLE)
The largest single hotspot. The inner `for (int k = 0; k < addXX.size(); k++)` loop
grows as more points are added. Adding early termination or spatial indexing could help.

### 3. Accept current state (iter-009 at +2.67%)
The experiment has reached diminishing returns. The remaining 10% RUNNABLE is spread
across branches and data-structure overhead that require larger architectural changes
(e.g., switching to array-of-struct layout, reducing method dispatch overhead).

## Final State
Best result: **iter-009 at combined 2666.4 ops/s (+2.67% vs baseline)**
Threshold achieved: YES (2666 > 2610)
Success target: NOT reached (2666 << 3116)
