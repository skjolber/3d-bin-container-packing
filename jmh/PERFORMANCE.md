# Performance: 5.0 against 4.2

End-to-end packing throughput of 5.0.0 (branch `boxItemConstraints`, commit `c75ac792`) against 4.2
(`master`, commit `0206e88f`, version 4.2.3-SNAPSHOT), measured on 2026-10-08.

| Benchmark | Input | 4.2 (ops/s) | 5.0 (ops/s) | Change |
|---|---|---|---|---|
| `TychoBenchmark.plainPackager` | 93 boxes of different sizes, one container | 56 | 675 | 12× |
| `BouwkampCodeBruteForcePackagerBenchmark.fastPackager` | squared squares (2D), fast brute force | 11.0 | 28.2 | 2.6× |
| `TychoBenchmark.fastPackager` | 22 boxes, fast brute force | 7,040 * | 16,140 | 2.3× |
| `EgyPackagerBenchmark.plainPackager` | small orders (about 10 boxes) | 60,190 | 114,120 | +90 % |
| `EgyPackagerBenchmark.parallelPackager` | small orders, parallel brute force | 1,790 | 2,480 | +38 % |
| `EgyPackagerBenchmark.packager` | small orders, brute force | 158,330 | 161,170 | +2 % |
| `EgyPackagerBenchmark.fastPackager` | small orders, fast brute force | 250,790 | 230,050 | −8 % |

\* 4.2 varied by ±4,000 ops/s between iterations.

## Tuning on 2026-10-10 (commits `da7a4132`, `eb1b9fc0`, `2b8e3d6f`)

A benchmark-driven tuning pass (G1 pinned, interleaved A/B runs, results gated on the version-4 oracle suites)
moved the brute-force family further; measured against the pre-tuning 5.0 on the same machine:

| Benchmark | Before (ops/s) | After (ops/s) | Change | What |
|---|---|---|---|---|
| `EgyPackagerBenchmark.parallelPackager` | 2,490 | 27,000 | 10.9× | lazy work units; stop submitting once a unit holds all boxes; allocation 2.46 MB/op → 79 KB/op, a session's live heap 461 KB → 4 KB |
| `BouwkampCodeBruteForcePackagerBenchmark.fastPackager` | 28.1 | 31.4 | +12 % | skip materializing results below the best; lazy, right-sized point lists; reset without nulling |
| `BouwkampCodeBruteForcePackagerBenchmark.packager` | 0.182 | 0.187 | +3-5 % | the point list changes; run to run spread is ±2-4 %, so the exact figure is uncertain |
| `TychoBenchmark.fastPackager` (`boxes=22`) | 15,622 | 16,589 | +6.2 % | canonical total order of the moved points (see below) and an insertion sort for the x and y moves; measured against the tree before it, 4 interleaved pairs, all positive |
| `TychoBenchmark.fastPackager` (`boxes=22`), on top of the above | | | +6.9 % | the volume / area pre-filter of the eclipse scans dropped: containment implies it, and it measured as more expensive than the early-exit containment test it guards (brute-force Bouwkamp +3.8 %, the rest within noise) |

Search results are unchanged: the oracle suites pass unweakened, and differential fuzzes of exact placements
(fast, 576 instances) and of parallel quality (1,600 comparisons) found no differences.

The last row comes with a definition instead of a library accident: the 3D point calculator now processes the points it
moves past a placement in a canonical total order (`CustomIntXComparator` and its y and z counterparts), which is
monotone with eclipsing and breaks ties by the richest supports, so that the free points no longer depend on how a
quicksort happens to order equal keys. Exact 4.x point parity ended with it (tied redundant points are suppressed by
construction, and the same free space may be tiled into different points: over 1.3 million random checks the union of
the 5.0 points covers every 4.x free point which meets the minimum area and volume limit in force, and only 4.x points
below those limits, which hold no remaining box, are sometimes not covered; in 0.07 % of 118,000 random placements with
boxes as large as the free point 5.0 keeps a free point which 4.x lost); the quality contracts against 4.x are unchanged. The same change on the
other benchmarks, against the tree before it: fast Bouwkamp +1.9 %, parallel Bouwkamp +3.4 %, parallel Egy +1.7 %,
Egy `packager` +2.1 %, Egy `fastPackager` −1.5 %, brute-force Bouwkamp −0.4 %: all inside their run to run spread.

Measured dead ends, so they are not retried:
- JIT shaping: no huge methods, no compile skips, no deopt churn, all hot sites monomorphic; the only flag effect
  (`-XX:InlineSmallCode=4000`) caps the whole avenue at about 2 % on the exhaustive search.
- Structure-of-arrays for the point scans: maintaining shadow bounds arrays costs more than the scans save,
  because `merge` rewrites the whole free-point list on every placement while the scans only cover its 3-6
  (Tycho: ~48) points; measured −13 to −23 %.
- `jdk.incubator.vector` for the eclipse scan: kernel-level speedup only from about 32 free points upward
  (2-4× at 64-512, a loss at 8 with 512-bit lanes), so it cannot help these list sizes; C2 already
  auto-vectorizes the branch-free form of the loop.

## How it was measured

- Both versions ran their own `jmh` module, so each used its own benchmark code. 4.2's `TychoBenchmark` packed
  `set.getProducts()`, which is always null; its copy was changed to pack the selected products, as 5.0 does. This
  changes the benchmark harness only.
- JDK 25, 4 cores (`taskset -c 0-3`), 1 fork, 3 × 3 s warmup and 5 × 3 s measurement. CPU frequency boost was not
  controlled, so the versions were interleaved for two rounds (4.2, 5.0, 4.2, 5.0) and averaged.
- To reproduce: build `jmh` for both commits (`./mvnw -Pdev -DskipTests package -pl jmh -am`), then
  run `java -jar jmh/target/benchmark.jar '<regex>' -p boxes=93 -f 1 -wi 3 -w 3s -i 5 -r 3s` alternately on each jar
  (`-p boxes=22` for `TychoBenchmark.fastPackager`).

## Reading the numbers

- The packagers do not always do the same work in both versions. 5.0 fixes several cases where 4.2 placed fewer boxes
  than it could, for example fast brute force with 3D rotations: 4.2 discarded free points which only another rotation
  of a later box would fit. 5.0 keeps them (the smallest area over all rotations of the remaining boxes).
- The 4.2 pruning (by the areas of the current rotations) was reimplemented and measured, and it does NOT explain the
  −8 % of fast brute force on the small Egy orders: with it, the Egy orders run at 225,900 against the default's
  224,000 ops/s (within noise, JMH, 4 cores), and the 22-box Tycho order is 17 % slower (12,900 against 15,600 ops/s,
  with the same number of search steps), as the 4.2 limit stays at the smallest area at the start of the descent, also
  after the smallest box has been placed, and so keeps small free points longer. It also reproduced 4.2's lower box
  counts (13 orders of 220,000 packed worse), so it was dropped rather than offered as an option.
- Most benchmarks of 5.0 have no 4.2 counterpart: LAFF, support and full support, load limits
  (`constraint.*Benchmark`), box item groups (`GroupBruteForceBenchmark`) and container packing strategies
  (`ContainerPackingStrategyBenchmark`).
- The gains come from the point calculation, the placement search (point bounds, the support index, comparing
  candidates before validating loads), load validation which visits each placement once, the iterative brute-force
  search with boxes shared instead of copied, and the final optimizations of the 5.0 changes (commits `46e36a07` to
  `c75ac792`). Optimizations which were measured and did not help are listed in [EXPERIMENTS.md](EXPERIMENTS.md).
