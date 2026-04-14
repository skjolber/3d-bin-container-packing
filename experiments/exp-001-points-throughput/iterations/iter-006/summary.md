# iter-006 Summary: Remove Redundant isFlag Check in removeEclipsed

## Result

| Metric | Value |
|--------|-------|
| 2D score | 2431.073 ops/s |
| 3D score | 2826.523 ops/s |
| Combined | 2628.798 ops/s |
| Delta vs baseline (2597) | **+1.22%** |
| Decision | **KEEP** |
| New best | Yes (previous best iter-003: 2613 ops/s / +0.61%) |

## What Changed

Optimized `DefaultPointCalculator2D.removeEclipsed()`:

1. **Removed `if(values.isFlag(index)) continue;`** from the inner loop (lines over `index = limit..size-1`)
2. **Cached `unsortedMinX` and `unsortedArea`** as `final` locals outside the inner loop

## Why it Worked

The profiler showed `removeEclipsed` at 9.0% of all samples (13.5% of RUNNABLE CPU) in 2D — the hottest method after accounting for `add()` inline code.

Code analysis proved the `isFlag(index)` check is always false when the inner loop runs:
- `values.removeFlagged()` is called at line 437 before new points are inserted
- All existing items [limit..size-1] are therefore guaranteed unflagged
- The `isFlag` check added one boolean array access + branch per inner iteration for zero benefit

Removing it gives the JIT a tighter inner loop with:
- No unnecessary memory access (flag array)
- No branch that always goes the same way
- Cached locals reduce pointer chasing

## Breakdown

- 2D improved: +2362→2431 = **+2.9%** vs 2D baseline
- 3D approximately unchanged: 2826 vs 2832 baseline (−0.2%, noise)
- The optimization is 2D-only (3D uses different inline eclipse checks, no removeEclipsed call)

## Profiler Context

The change is data-driven from profiler output:
- GC: ~585 KB/op (2D), ~562 KB/op (3D) — massive allocation from moveX/moveY/moveZ
- CPU 2D: removeEclipsed=13.5%, removeFlagged=11.5%, add=10.2%, sort=10.1%
- CPU 3D: add=29.7%, sort=9.8%, isEclipsed=3.9%
