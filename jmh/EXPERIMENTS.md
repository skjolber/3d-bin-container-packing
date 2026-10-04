# Performance experiments that did not work

Optimizations which were implemented, measured and reverted (or rejected after analysis) during
the 5.0.0 performance work. They are recorded so that they are not retried without a new reason.
Each entry lists the idea, the measurement, and the likely cause.

All changes were required to keep the golden masters (`PointCalculatorGoldenMasterTest`,
`PackagerGoldenMasterTest`) identical, so these entries are about speed only.

## How changes were measured

- CPU frequency boost was off, using the `performance` governor, on JDK 25.
- The quick suite ran 1 fork, 3 × 3 s warmup and 5 × 3 s measurement, with `-prof gc`. It covered:
  - `BouwkampCodeBruteForcePackagerBenchmark`;
  - `EgyPackagerBenchmark` (plain and brute force);
  - `TychoBenchmark` (plain, fast LAFF and support);
  - `PointsBenchmark2D` and `PointsBenchmark3D`, including the recorded replays;
  - `constraint.LoadPalletBenchmark`.
- On a shared or busy machine, use an interleaved A/B: build both variants, then alternate them
  for two rounds (`head, current, head, current`). Differences between separate runs of a few
  percent were often noise, and once 15 %.
- Profiles came from JFR (`-prof jfr` with `configName=profile`), reading inclusive time per method.

## Points module (`DefaultPointCalculator3D`, `Point3DFlagList`)

`DefaultPointCalculator3D.add` is at the edge of the JIT's inlining and compilation budget.
Several changes which removed work still measured slower, because they added code to the hot
methods. Splitting `add` into `classify`, `moveX/Y/Z` and `merge` helped later changes. The
lesson: measure every change, and prefer smaller methods.

| Experiment | Result | Likely cause |
|---|---|---|
| Struct-of-arrays coordinate mirror (`int[] minX…maxZ`, `long[] area, volume`) in `Point3DFlagList`, so that the eclipse scans read contiguous arrays | Every benchmark slower: Bouwkamp brute force −10 %, Egy brute force −12 %, Tycho −3 % | The array writes on every append and copy cost more than the faster scan saved, and `add` grew past good JIT compilation |
| Flat merge buffers: one list of (target index, point), sorted by target, instead of the per-index `Point3DListArray`/`Point3DArray` | Allocation fell (Tycho 375 KB → 265 KB per op), but brute force and the replay were 2–4 % slower. Tried twice: before and after splitting `add` | More instructions per point in the merge, and a sort per `add` |
| Indexed loops over `PointSource` (`size()`/`get(i)`) in the placement controls instead of the iterator | Tycho 93 593 → 505 ops/s | `size()` and `get(i)` are megamorphic interface calls in the loop; the iterator makes one call per point |
| Lazily allocated inner lists in `Point3DListArray` | No gain, Tycho about −4 % | An extra null check on every access |
| Lazy clearing of stale references: `resetWithoutFlags` skips `Arrays.fill`, clearing beyond the new size later | Bouwkamp brute force unchanged, Egy plain −15 % | `Arrays.fill` showed 3.4 % in the brute-force profile, but the extra bookkeeping cost more than it saved |
| Undo log for brute force (mutable points, undo on pop) | Rejected after measuring: clones are only 17 % of brute-force allocation; 83 % is `moveX/moveY`, which allocate in both modes | Points are shared between stack levels (copy-free stack), so they cannot be mutated |

## Point calculator, 2D (`DefaultPointCalculator2D`)

| Experiment | Result | Likely cause |
|---|---|---|
| Backward scan in `removeEclipsed`, starting from a binary search (as for the 3D eclipse check, which gained 10–24 %) | 2D replay 62.8k → 61.2k ops/s, fast LAFF unchanged | 2D point lists hold about 10 points (23 at most), too few for scan order to matter |
| JDK `Arrays.sort` with a lambda instead of the custom insertion and merge sort | 55.7k ops/s, against 61k for the custom sort | The custom sort only sorts the new points and merges them into the sorted tail |

## Placement controls

| Experiment | Result | Likely cause |
|---|---|---|
| Point-bound rotation skip (`canFitAny`) in the load-aware controls, as a box-level skip | Constraint benchmarks 2–6 % slower | Tiny workloads (about 6 µs per packing): computing the bounds costs more than it saves |
| `canFitAny` in `SupportPlacementControls` / `FullSupportPlacementControls` | Neutral (+3 % / −2 %) | 3D points are large, so the bound rarely skips anything (104 of 1,875 rotations on Tycho). It pays only with small 2D points (fast LAFF +41 %) |
| End coordinates cached in `Placement` (`endX/endY/endZ`, updated in `setStackValue`/`setPoint`) | Egy plain −7 %, Tycho plain −4 %, pallet −5 % (interleaved A/B) | Candidates are created tens of thousands of times per packing, each setting the position and stack value. That is more updates than reads saved |
| Synchronizing the load stack index once per search | Not done: the sync is about 2.5 %, mostly real index maintenance | The brute-force packagers call the utility without a search boundary, so the index could go stale |

## Load support graph (A/B against the previous version)

Visiting each placement once instead of every path through the support graph is required: tall
staggered stacks were exponential, and a 40-row brick wall hung. The first version cost 10 % on the
shallow `LoadPalletBenchmark` (LAFF, weight + pressure + count). Measured parts:

| Variant | Result |
|---|---|
| Per-query memo for box counts on every visited placement | About 5 % of the 10 % |
| `propagateLoad` queueing every supporter (list allocation plus selection per call) | About 7 % (36.3k old vs 33.9k) |

Kept: update placements with a single supportee immediately, queue only converging placements,
and memoize only placements with several supportees (or supporters). The remaining cost is 4 %.

## Brute-force setup

| Experiment | Result | Likely cause |
|---|---|---|
| Reusing stacks and placement arrays across packing calls (pool) | Rejected after analysis | Results keep references to the stack's point lists (`setState(pointCalculator.getPoints(), …)`) and to the placement arrays until they are materialized, so reuse could corrupt results. Packagers are also shared between threads |
| Avoiding the throwaway stack in `resetState()` | Not needed; `reset()` was later removed from the session | No strategy called the session's `reset()` |
