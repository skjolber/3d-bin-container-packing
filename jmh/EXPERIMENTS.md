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
| Lazy clearing of stale references: `resetWithoutFlags` skips `Arrays.fill`, clearing beyond the new size later | Bouwkamp brute force unchanged, Egy plain −15 % | `Arrays.fill` showed 3.4 % in the brute-force profile, but the extra bookkeeping cost more than it saved. Dropping the clearing entirely (no deferred bookkeeping, readers bounded by the size) did win later, see the 2026-10-10 tuning in [PERFORMANCE.md](PERFORMANCE.md) |
| Stable insertion sort of the X/Y moved points (same sort key, comparator-bound loop) instead of the eclipse `IntQuickSort` | Interleaved against the tree without it: Tycho +4.7 % (8 pairs, all positive, but below the 6 % noise bar), fast Bouwkamp +1.9 %, brute-force Bouwkamp +1.6 %, Egy unchanged. The aggregate 4.x tallies over 10,000 orders were unchanged (plain 229 won / 89 lost, largest area fit first 824 / 11 and 750 / 9). Not taken: the free points no longer match 4.x. In 80 random placement sequences of 300 steps 35 % of the steps had a different point set (redundant contained points, and the supports of points with equal coordinates), and in one sequence a small maximal point was missing | Eclipse already sorts up to 9 elements with an insertion sort, so only Tycho-sized lists (about 48 points) gain. The merge does not keep the list minimal, so the order of equal moved points decides which redundant points survive, and exact 4.x point parity cannot be kept with another tie order. Landed later in another form: a canonical total order for the moved points (the tie order is defined, bigger moved points first), with the insertion sort, see the Tycho row in PERFORMANCE.md |
| Compute `Point` extents, area and volume from the coordinates instead of caching them in `dx`, `dy`, `dz`, `area` and `volume` (`DefaultPoint3D` 80 → 56 bytes, allocation per Bouwkamp brute-force search 3.36 → 2.35 GB, −30 %; Egy fast −7 %) | With the volume / area pre-filter of the eclipse scans kept: slower everywhere (Tycho −20.6 %, Egy `packager` −9.3 %, parallel Bouwkamp −6.3 %, fast Bouwkamp −5.6 %, brute-force Bouwkamp −5.3 %, Egy `fastPackager` −6.1 %). With the pre-filter dropped (containment implies it): flat against the tree before it (Tycho +1.9 %, brute-force Bouwkamp +1.1 %, fast Bouwkamp −1.7 % over 10 pairs, Egy within 1 %). Dropping only the pre-filter, with the cached fields kept, measured better: Tycho +6.9 %, brute-force Bouwkamp +3.8 % (3 pairs each) and was landed on its own (see [PERFORMANCE.md](PERFORMANCE.md)) | A computed volume is six loads and two multiplications, more than the containment test it guards (Tycho scans about 48 points per created point); the saved allocation is cheap young-generation bump allocation (GC 10 ms per 45 s) |
| Undo log for brute force (mutable points, undo on pop) | Rejected after measuring: clones are only 17 % of brute-force allocation; 83 % is `moveX/moveY`, which allocate in both modes | Points are shared between stack levels (copy-free stack), so they cannot be mutated |
| Drop the early check of each moved point against the points already moved in the same direction (`addedXX`, `addedYY` and `addedZZ` in `moveX`, `moveY` and `moveZ`), as the early check of the constrained copies was dropped (see [PERFORMANCE.md](PERFORMANCE.md)): the eclipse check of `merge` was expected to discard them anyway | Rejected: `PointCalculatorGoldenMasterTest` fails (the free point sets differ), and Tycho is −14.1 % (2 interleaved pairs: −15.7 %, −12.5 %) | Not analysed to the end: the check evidently is not implied by the eclipse check of `merge` (which only looks at the points merged before a point, and for a point with a yz support only at the points at the new x), so moved points which the early check dropped survive, and every later scan walks them. The early check of the constrained copies was different: `merge` checks a copy against every free point the early check saw, and no moved point can eclipse a copy |
| Add the indices of the points to move (`moveToXX`, `moveToYY`, `moveToZZ` in `classify`) without the capacity check of `IntArrayList.add`, after reserving the room (JFR: `IntArrayList.ensureCapacityForAdd` called from `classify` showed 6.2 % self time on Tycho) | No gain: Tycho −0.6 % (3 interleaved pairs: +0.1 %, +0.5 %, −2.2 %) | The samples are the cost of the loads of the point that are issued around the add, not of the check |

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

## Optimizing the 5.0 changes

Interleaved A/B against the previous commit on 4 cores, 2 rounds (3 for the re-tests). The kept optimizations are commits
`46e36a07` to `c75ac792`.

| Experiment | Result | Likely cause |
|---|---|---|
| Reject a box by its minimum area before trying its rotations (`ComparatorPlacementControls`) | Fast LAFF Tycho 93 −1.3 %, the others within ±0.5 % | `canFitAny` already rejects each rotation cheaply, and the smallest rotation usually fits some point |
| Allocation-free single-container check in `ContainerAllocationPlanner.canAllocate`, without the duplicate global index initialization in brute force | Egy brute force +0.9 % on average (3 rounds) | Too small: the spread between rounds was as large |
| Set up only the levels of the brute-force search frames which changed since the previous permutation | Egy fast brute force −3.4 % (3 rounds); Bouwkamp fast unchanged once the fast point stack swaps its lists | More bookkeeping per permutation than it saves. The suffix minimum areas also change when the search is shorter than the permutation |
| Reusable point buffers for the best arrangements of the fast search | Bouwkamp fast −4.5 % | An extra copy per offered arrangement costs more than the list allocation it saves |
| Skip the supported-area update in `SupportPlacementControls.accepted` when no placement starts above the new box | Tycho support 93 +0.8 % | Once candidates have no load lists, the update is a small part of the cost of support |
