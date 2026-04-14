# iter-002 Summary

## Strategy
merge-sorted-segments-2d

## Scores
- 2D: 2319.365 ops/s (baseline: 2362)
- 3D: 2808.409 ops/s (baseline: 2832)
- Combined avg: 2563.887 ops/s
- Baseline combined: 2597 ops/s
- Delta vs baseline: -1.27%

## Thresholds
- Success threshold (≥+20%): 3116 ops/s — NOT reached
- Keep threshold (≥+0.5%): 2610 ops/s — NOT reached

## Decision: DROP

Combined average 2563.887 < 2610 threshold; the optimization made things slower.

## What Was Changed
Replaced `values.sort(Point2D.COMPARATOR_X_THEN_Y, endIndex)` in 2D `add()` with:
1. `Arrays.sort([0..newAdded-1], COMPARATOR_X_THEN_Y)` — sort the small new prefix
2. Half-buffered merge of the two sorted segments — O(endIndex) merge pass

Added `mergeBuf` field and `mergeWithSortedTail()` private method.

## Why It Failed
Tim Sort (`Arrays.sort`) already handles the "sorted tail + small unsorted prefix" case
efficiently via its internal run-detection and merge. The explicit merge overhead
(extra System.arraycopy into `mergeBuf`, Comparator dispatch in the merge loop) exceeded
the savings from avoiding Tim Sort's internal bookkeeping, especially for small `endIndex`
values (typically 5-20 in the benchmark).

Essentially: Tim Sort is already O(n) for nearly-sorted arrays; my optimization replaced
O(n) with O(n) but with higher constant factors.
