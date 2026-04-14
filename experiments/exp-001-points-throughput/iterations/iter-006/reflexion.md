# iter-006 Reflexion: Profile-First Approach Pays Off

## What Happened

Changed approach from guessing hotspots to running JMH profilers first.
This yielded two key findings that led to a +1.22% win (best so far).

## Profiler Findings (empirical — use for future iterations)

### GC Profiler
- 2D: **585 KB/op** allocation rate
- 3D: **562 KB/op** allocation rate
- ~54 GC collections in 30s; ~29ms GC pause total = ~4.7% overhead
- Root cause: every `moveX/moveY/moveZ` call creates a new Point subclass object

### Stack Profiler — 2D hotspots (% of all samples)
1. removeEclipsed: **9.0%** (13.5% RUNNABLE) ← optimized this iteration
2. removeFlagged: **7.7%** (11.5% RUNNABLE) ← next target
3. add (inline): **6.8%** (10.2% RUNNABLE)
4. sort: **6.7%** (10.1% RUNNABLE)

### Stack Profiler — 3D hotspots (% of all samples)
1. add (inline): **19.8%** (29.7% RUNNABLE)
2. sort: **6.6%** (9.8% RUNNABLE)
3. isEclipsed: **2.6%** (3.9% RUNNABLE)
4. constrainMax: **1.4%** (2.1% RUNNABLE)

## Optimization Applied

Removed `if(values.isFlag(index)) continue;` from inner loop of `removeEclipsed()`.
Also cached `unsortedMinX` and `unsortedArea` as locals.

This is provably correct: `values.removeFlagged()` runs before insertion, so
all existing items [limit..size-1] are guaranteed unflagged.

**Result: +2.9% in 2D, 3D unchanged, combined +1.22%**

## What's Left to Try (data-driven, ranked by expected impact)

### 1. Reduce GC allocation (~585 KB/op) — highest potential gain
The massive GC churn (4.7% overhead + allocation bandwidth) is the biggest opportunity.
Every moveX/moveY/moveZ allocates a new object. Options:
- **Object pooling**: reuse Point objects across iterations (complex but could give 5-10%)
- **Check-before-allocate**: verify eclipse BEFORE calling moveX to skip allocation for
  discarded points. Need `eclipsesMovedX(p, xx)` against existing values, not just addXX.
- **Mutable points**: change moveX to mutate in-place (requires architecture change)

### 2. Reduce removeFlagged calls in 2D (7.7% all samples)
Currently called twice per add(). Merging to one pass would save ~3-4%.
Challenge: need binary search to recompute endIndex after combined removal.
Plan: skip first removeFlagged, keep isFlag check in removeEclipsed inner loop,
do single removeFlagged at end + `binarySearchPlusMinX(0, xx)` to find new endIndex.

### 3. Reduce 3D add() overhead (19.8% all samples)
The add() method is a monolith. Sort is 9.8% of 3D RUNNABLE.
- Skip sortThis when list has only 1 element (common case?)
- Use insertion sort for moveToXX/YY when list is small (≤8 items)
- Cache `otherValues.size()` before isEclipsed loop (called O(n) times)

### 4. Reduce isEclipsed overhead in 3D (2.6% all samples)
Cache `pointVolume` and `pointArea` outside loop in `isEclipsed()`.
Expected gain: ~0.5% in 3D (modest).

## Mistakes to Avoid

- iter-001: Binary search micro-opts added overhead (wrong level of abstraction)
- iter-002: Merge pre-sorted segments slower (wrong assumption about sort behavior)
- iter-004: Too many changes at once, disrupted JIT (keep atomic)
- iter-005: isEclipsedAtXX skip — method is not on hot path (profiler would have shown this!)
- **Key lesson**: Always profile before guessing. The profiler saved 4 bad iterations.

## Next Iteration Recommendation

**Target: removeFlagged (7.7% of all samples in 2D)**

Approach: Merge two removeFlagged calls into one by deferring the first.
1. Remove line 437: `endIndex -= values.removeFlagged()`
2. Keep isFlag check in removeEclipsed inner loop (needed for now)
3. Do single removeFlagged at end of add() (line 455)
4. Replace endIndex tracking with: `endIndex = values.binarySearchPlusMinX(0, xx + 1)`
   (or `binarySearchPlusMinX(0, xx)` adjusted for exclusive return)
5. Remove the while-loop adjustment (replaced by binary search)

This trades one O(n) scan for a O(log n) binary search, potentially saving ~3-4%.
But needs careful validation that endIndex semantics are preserved.
