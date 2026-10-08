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
  of a later box would fit. 5.0 keeps them (the smallest area over all rotations of the remaining boxes), which is the
  likely reason fast brute force is slower on the small Egy orders.
- Most benchmarks of 5.0 have no 4.2 counterpart: LAFF, support and full support, load limits
  (`constraint.*Benchmark`), box item groups (`GroupBruteForceBenchmark`) and container strategies
  (`ContainerStrategyBenchmark`).
- The gains come from the point calculation, the placement search (point bounds, the support index, comparing
  candidates before validating loads), load validation which visits each placement once, the iterative brute-force
  search with boxes shared instead of copied, and the final optimizations of the 5.0 changes (commits `46e36a07` to
  `c75ac792`). Optimizations which were measured and did not help are listed in [EXPERIMENTS.md](EXPERIMENTS.md).
