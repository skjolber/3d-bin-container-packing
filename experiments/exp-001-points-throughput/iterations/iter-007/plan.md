# iter-007 Plan: Merge Two removeFlagged() Calls into One

## Target
**File**: `DefaultPointCalculator2D.java`  
**Method**: `add()` (lines 437, 455) + `removeEclipsed()` (inner loop)

## Profiler Motivation
- `removeFlagged` is **11.5% of RUNNABLE** time in 2D
- Currently called **twice** per `add()` — each is a full O(n) scan
- Merging to one call saves ~half of 11.5% ≈ **5.75% RUNNABLE** gain
- Cost: restoring isFlag check in removeEclipsed inner loop (small fraction of 13.5%)
- Expected net gain: **+2-4% in 2D**

## Current Code Structure

```
add():
  line 226: endIndex = binarySearchPlusMinX(index+1, absoluteEndX)
  lines 228-332: main processing loop — flags items [pointIndex..endIndex)
  lines 334-435: constrain ops — flags more items
  line 437: endIndex -= values.removeFlagged()   ← CALL #1 (O(n) scan)
  line 439: placements.add(placement)
  line 441: added = addXX.size() + addYY.size()
  line 446: values.ensureAdditionalCapacity(added)
  line 449: values.move(added)
  line 450: values.setAll(addYY, 0)
  line 451: values.setAll(addXX, addYY.size())
  line 453: removeEclipsed(added)
  line 455: endIndex += added - values.removeFlagged()  ← CALL #2 (O(n) scan)
  lines 458-460: while loop extends endIndex to include minX == xx
  line 462: values.sort(...)
```

```
removeEclipsed(int limit):
  outer loop i in [0..limit):  checks isFlag(i) → continue
  inner loop index in [limit..size):  NO isFlag check (removed in iter-006)
```

## Correctness Proof: Deferring First removeFlagged

**Key invariant**: All items flagged before line 437 have minX in [0..endIndex_initial-1].
- Proof: constrain loops operate on [pointIndex..endIndex], source flag at `index` where
  minX = absoluteX < xx. Therefore all F flagged items are within [0..endIndex_initial-1].

**Formula equivalence**:
| Step | Current (2 calls) | Deferred (1 call) |
|------|-------------------|-------------------|
| After #1 removeFlagged | endIndex = initial - F | endIndex = initial (unchanged) |
| After move(added) | shift conceptually +added | same |
| After #2 removeFlagged | endIndex += added - G | endIndex += added - (F+G) |
| **Final** | initial + added - F - G | initial + added - F - G |

**Both yield identical endIndex.** ✓

**Trade-off**: We add back `if(values.isFlag(index)) continue;` in removeEclipsed inner
loop because original items at [added..size-1] may still be flagged during removeEclipsed.
Without this guard, a new item could be wrongly eclipsed by a doomed original item.

## Proposed Changes

### Change 1: Remove first removeFlagged call (line 437)

**BEFORE:**
```java
		endIndex -= values.removeFlagged();

		placements.add(placement);
```

**AFTER:**
```java
		placements.add(placement);
```

### Change 2: Update line 455 comment/formula (formula unchanged, reasoning updated)

The formula `endIndex += added - values.removeFlagged()` at line 455 is correct as-is.
When combined: removeFlagged() now removes F (old flagged) + G (new flagged) items.
endIndex = endIndex_initial + added - (F+G) = endIndex_initial + added - F - G ✓

No code change needed at line 455.

### Change 3: Restore isFlag check in removeEclipsed inner loop

**BEFORE (iter-006 removed this check):**
```java
		for (int index = limit; index < size; index++) {
			Point2D sorted = values.get(index);
			if(sorted.getMinX() > unsortedMinX) {
				break;
			}
```

**AFTER:**
```java
		for (int index = limit; index < size; index++) {
			if(values.isFlag(index)) {
				continue;
			}
			Point2D sorted = values.get(index);
			if(sorted.getMinX() > unsortedMinX) {
				break;
			}
```

**Reason**: Without first removeFlagged, items in [limit..size-1] may be flagged.
Must skip them to avoid false eclipse checks against doomed items.

**NOTE on break optimization**: After restoring isFlag, if we hit a flagged item we
`continue` rather than `break`. This is safe: a flagged item's minX might be < xx
while the next unflagged item has minX > unsortedMinX. The break condition is still
checked for each unflagged item.

## Edge Cases

| Case | Behavior |
|------|----------|
| F=0 (no constrain flags) | removeFlagged at 455 same result, isFlag in inner loop always false (branch-predicted) |
| G=0 (no eclipse flags) | Combined removeFlagged returns only F, formula still correct |
| F=N (all items constrained away) | Combined removeFlagged removes all N+G items, endIndex = initial + added - N - G |
| values empty | loops don't execute, no issue |
| all added items eclipsed (G=added) | endIndex = initial - F, then while loop adjusts |

## Expected Impact

- Save one O(n) scan per add() call = eliminate ~half of 11.5% RUNNABLE = **+5.75%** 2D gain
- Restore inner isFlag check in removeEclipsed: small overhead (< 2% of 13.5% RUNNABLE)
  because flagged items are rare and CPU branch predictor handles this well
- Net expected gain: **+3-4%** in 2D → combined **+1.5-2%** vs baseline
- 3D: no change (removeEclipsed and removeFlagged are not 3D hotspots)
- Target: combined ≥ 2610 ops/s (threshold) → 2680+ ops/s expected
