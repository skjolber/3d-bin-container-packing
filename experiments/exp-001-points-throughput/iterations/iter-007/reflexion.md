# iter-007 Reflexion: Merge removeFlagged Pays Off (+2.15%)

## What Happened

Merged two `removeFlagged()` O(n) scan calls in `add()` into a single call.
This is the largest gain so far (+2.15% vs baseline, +0.91% vs iter-006).

## Profiler Data (from iter-006 empirical — keep for future iterations)

### GC Profiler
- 2D: **585 KB/op** allocation rate (~1380 MB/sec)
- 3D: **562 KB/op** allocation rate (~1557 MB/sec)
- ~4.7% GC overhead from `moveX/moveY/moveZ` always allocating new Point subclass objects

### Stack Profiler — 2D hotspots (% RUNNABLE, post iter-006)
| Method | % RUNNABLE |
|--------|-----------|
| removeEclipsed | 13.5% | ← iter-006 improved (isFlag check removed from inner loop — now RESTORED)
| removeFlagged  | 11.5% | ← iter-007 targeted (halved: 1 call instead of 2)
| add (inline)   | 10.2% |
| sort           | 10.1% |

### Stack Profiler — 3D hotspots (% RUNNABLE, post iter-006)
| Method | % RUNNABLE |
|--------|-----------|
| add (inline) | 29.7% | ← biggest 3D opportunity
| sort         |  9.8% |
| isEclipsed   |  3.9% |
| constrainMax |  2.1% |

## Optimization Applied

**File**: `DefaultPointCalculator2D.java`

1. Removed `endIndex -= values.removeFlagged();` (was line 437, the first O(n) scan)
2. Restored `if(values.isFlag(index)) continue;` in `removeEclipsed` inner loop
3. The remaining `endIndex += added - values.removeFlagged();` now handles both
   constraint-flagged originals (F) and eclipse-flagged new items (G) in one pass.

**Correctness proof**:  
All F flagged items (before the old call) are in [0..endIndex_initial-1] because
constrain loops iterate [pointIndex..endIndex] only. Therefore:
`(endIndex_initial - F) + added - G  ==  endIndex_initial + added - (F+G)` ✓

**Trade-off**: Restoring the isFlag check in `removeEclipsed` partially offsets the
iter-006 win. Net: saved ~half of 11.5% RUNNABLE (~5.75%) minus inner-loop isFlag
overhead (small — branch always predicted correctly since flagged items are rare).

**Result: +4.1% in 2D, +0.6% in 3D, combined +2.15%**

## What's Left to Try (data-driven, ranked by expected impact)

### 1. Reduce GC allocation (~585 KB/op) — highest potential gain (5-10%)
Every `moveX/moveY/moveZ` allocates a new Point object. Options:
- **Check-before-allocate**: test eclipse condition BEFORE calling moveX/Y/Z to skip
  allocation for projected points that would be immediately discarded
- **Object pooling**: reuse Point objects across iterations (complex)
- **Mutable projection**: mutate in-place (requires architecture change)

### 2. Reduce removeEclipsed overhead (13.5% RUNNABLE 2D, now with isFlag restored)
We restored the isFlag check which adds overhead. Options:
- **Two-pass removeEclipsed**: first compact flagged originals into a temp, then compare
- **OR**: Re-examine whether we can remove the isFlag check again using a different
  invariant — e.g., if we can guarantee constraint-flagged items are compacted before
  `removeEclipsed`, we get iter-006 + iter-007 gains simultaneously

### 3. Reduce 3D add() overhead (29.7% RUNNABLE 3D) — large opportunity
- **Skip sortThis when list has only 1 element** (common case?)
- **Use insertion sort for small lists** (≤8 items in addXX/addYY)
- **Cache otherValues.size() before isEclipsed loop** (called O(n) times per add)

### 4. Sort optimization in 2D/3D (10.1%/9.8% RUNNABLE)
- `values.sort(Point2D.COMPARATOR_X_THEN_Y, endIndex)` at end of add()
- If endIndex is small (≤ 8), manual insertion sort beats Arrays.sort
- Check: what is the typical value of endIndex at sort time?

### 5. Revisit iter-006 + iter-007 combined (potential +1% addl)
The ideal is: run first removeFlagged BEFORE setAll/move so that removeEclipsed's
inner loop has no flagged items (restoring the iter-006 invariant) AND still only
call removeFlagged once. This seems contradictory, but there may be a way:
- Use a separate compact (copy to temp buffer, clear, copy back) that only touches
  the original items, not the new items, before removeEclipsed.
- Or: use a different data structure (two separate arrays for orig/new).

## Mistakes to Avoid

- iter-001: Binary search micro-opts added overhead (wrong level of abstraction)
- iter-002: Merge pre-sorted segments slower (wrong assumption about sort behavior)
- iter-004: Too many changes at once, disrupted JIT (keep atomic)
- iter-005: isEclipsedAtXX skip — method is not on hot path
- **Key lesson**: Always profile before guessing. The profiler saved 4 bad iterations.

## Next Iteration Recommendation

**Target: 3D add() inline overhead (29.7% RUNNABLE)**

The 3D `add()` method is a monolith (29.7% RUNNABLE). Low-hanging fruit:
1. Cache `otherValues.size()` in a local before the isEclipsed loop — JIT may or may
   not already do this, but it's a 1-line safe change.
2. Skip sort when the relevant list has ≤ 1 element.
3. Check if `constrainMax` (2.1% RUNNABLE) has redundant work like isFlag checks.

Alternatively, target **sort overhead** (~10% RUNNABLE each in 2D and 3D):
- Profile: what's the typical `endIndex` value passed to sort?
- If usually ≤ 8, insertion sort could give 3-5% on both dimensions.
