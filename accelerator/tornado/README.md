# tornado-accelerator

Experimental [TornadoVM](https://www.tornadovm.org/) accelerator for `FastBruteForcePackager` and `BruteForcePackager` (see
[accelerated brute force](../../docs/heuristics/accelerated-brute-force.md)):
the search of the box orders of a container runs on a GPU (or another OpenCL, CUDA or Metal device of TornadoVM), and the packager places the winning order itself, so
the placements are the same as without the accelerator.

It needs Java 22+ and is part of the default build. To build and test it alone:

```sh
./mvnw -B -ntp -Pdev -pl accelerator/tornado -am test
```

```java
TornadoBruteForceAccelerator accelerator = TornadoBruteForceAccelerator.newBuilder().build();
AcceleratedFastBruteForcePackager packager = AcceleratedFastBruteForcePackager.newBuilder().withAccelerator(accelerator).build();
...
packager.close();
accelerator.close(); // releases the device memory and the compiled kernel
```

The accelerator is only used if it is passed to the packager. Without a TornadoVM runtime and device it gives up, and the packager searches as it does without one; the same
when a search needs more free points than `withMaxPoints(..)` (the layout of the scratch is fixed up front), or when the device fails (`getLastFailure()`). One search runs on the
device at a time; searches from other threads wait, polling their interrupt (a search which loses its race in `AUTO` while it waits never reaches the device).

## How it works

 * `DeviceSelector`, `DeviceInfo` and `TornadoDevices`: find the devices of the TornadoVM runtime and choose one, by default the OpenCL GPU with the most compute throughput
   (compute units times clock, then the most memory: an integrated GPU shares the memory of the system, so it can report more than the discrete card). Without a runtime the list is
   empty. Exclude a device with `withExcludedName(..)`, or choose by name with `withNameContains(..)`.
 * `SubtreeKernel` and `src/main/resources/.../subtree.cl`: the kernel. One work item runs `FlatTree.runTree(..)` for one slot of a window, with its own scratch, for at most
   `withNodeBudget(..)` search nodes, then pauses; `run(..)`, the port of `FlatSubtree`, is the old search of the orders one at a time (`withTree(false)`). It is a hand-written OpenCL C port of the
   flat classes of the `accelerator-core` module, package `com.github.skjolber.packing.accelerator` (`FlatGeometry`, `FlatPointOrder`, `FlatPoints`, `FlatPermutation`, `FlatSearch`, `FlatSubtree`,
   `FlatTree`, `FlatBound`), function by function with the same
   names and statements, run as a TornadoVM *prebuilt* task. See below for why it is not compiled by the TornadoVM JIT. A change to the flat classes is made in the kernel too;
   `KernelParityTest` checks that the two agree.
 * `TornadoBruteForceAccelerator`: the host loop of `FlatEngine` on the device. The permutations are split in items by their first boxes, a prefix of box types (`withTargetItems(..)`
   items at least). Each item is searched as one tree by default (`withTree(true)`), as the packager's `BruteForceTreeSearch` does on the CPU: each prefix of placements is placed once,
   and a node or a box is pruned when the prefix plus the best sub-multiset of the remaining boxes within the load left cannot beat the best. The bound is a table per multiset of remaining
   boxes (`FlatBound`), computed once per problem on the host and left out above 4096 multisets. The items share their best arrangement (volume, weight, count) through the host between
   rounds. Under any best (the packager's default, passed in `FlatProblem.ANY_BEST`) the types are tried largest first, the items with the largest prefixes run first, ties are pruned and the
   first arrangement which places all boxes ends the search; for the exact winner of the loop (`withTreeSearchAnyBest(false)`) the types are tried in their order and only strictly worse
   prefixes are pruned. The item keeps no table of settled orders, which the CPU tree has.
   * The free space bound (`FlatSpace`, the `// FlatSpace` section of the kernel, called from `runTree` as `isHopelessInSpace`) is the packager's one: the free space of the path of the node is
     kept as maximal empty spaces on the scratch (at most 48 per level, `FlatLayout.MAX_SPACES`; more makes the level unknown, in which every box is taken to fit). The box types which fit none of
     them in any rotation can never be placed below the node, so the prefix plus the remaining boxes of the other types within the load left must beat the best. It is checked once per node, when
     its first child is about to be placed, and only where three more of the smallest remaining boxes fit the load left (`SPACE_BOXES`); an item gives it up when fewer than one check in sixteen
     cuts (`SPACE_CHECKS` 2048, `SPACE_CUT_RATE`). A type is looked for first at the free points of the level (a free point which holds it is an empty region), then by the answer kept from the
     level above with its witness space, then in the spaces of the nearest level computed above which miss the boxes placed since, and only then are the spaces of the level computed from the
     parent's. It is on by default for the loop's winner (exact) and for the search of `BruteForcePackager` (every box at every free point, `FlatProblem.ALL_POINTS`, where it cuts a
     third), and off under any best at the best point only, because with ties pruned the bound table leaves it little to cut for its cost (`withFreeSpace(..)`). The scratch per slot grows
     by the spaces, the answers and the witnesses: about 7 KB of ints at 16 boxes and 48 spaces.
   * Splitting (`withSplit(..)`, on by default; `getSplits()` counts them): under any best, a tree item which has run for four rounds (`SPLIT_ROUNDS`) while fewer than a quarter of the slots are
     active is split on the host. Its best so far stays (the item is marked done), and the search below its prefix goes on in new items, one per remaining type (the parent's prefix plus the
     type), which run side by side in the free slots; a child can be split again, down to one box before the last. The work the parent had done below its prefix is redone, bounded by the best so
     far. It is the device's form of work stealing. Exact mode never splits: an earlier item wins ties there, and the order of the children would not be the parent's.
   * One session holds the arrays, sized for a number of boxes and of types (the first problem's, each rounded up to 8; a larger problem makes a larger session once), and the compiled kernel; the
     kernel reads the layout and the sizes from the arrays each run. The per-type arrays are sized by the types, not the boxes: the later containers of a packing keep every type of the packing,
     with no box left of some, so types can exceed boxes (`FlatLayout(n, pMax, types)`, read by the kernel as the last layout int; `FlatTreeTest.sameWinnerWithTypesWithoutBoxes`). The kernel is
     compiled once per accelerator, which takes about 0.45 s.
   * A window of `withWindow(..)` slots runs at a time, each with its own scratch on the device (`withMaxPoints(..)` free points at each level). A tree item keeps its slot (its state is the scratch of
     the slot) until it is done, and the host assigns free slots to the next items each round. The number of slots is capped by the largest allocation of the device.
   * The node budget of a round starts at 32 and doubles each round up to `withNodeBudget(..)`, so that a strong arrangement bounds the others before they search much. When few items are left the
     budget grows up to 64 times, so that the rounds (transfers and launch) do not dominate the tail.
   * After each run the host reads the state of the slots, polls the interrupt, and runs the next round; the winners of the items are reduced as in `FlatEngine`.
 * `ListDevices`: a `main` which prints the devices and the chosen one, as a smoke test of a TornadoVM installation.

The dependency on `tornado-api` is `provided`: the API jar has no native drivers. The runtime is the TornadoVM SDK, which is not on Maven Central.

## Running against a device

Download the SDK for your backend and JDK from the [TornadoVM releases](https://github.com/beehive-lab/TornadoVM/releases), for example `tornadovm-8.0.0-jdk22plus-opencl-linux-amd64.tar.gz`, and unpack it
(for example into `accelerator/tornado/target/tornadovm-sdk`, which `mvn clean` removes). The OpenCL backend has no native library of its own: it calls the system OpenCL loader
(`libOpenCL.so`) and so the OpenCL driver of the GPU (on AMD, the ROCm OpenCL runtime). The SDK contains `tornado-argfile.template`, the JVM options (JVMCI, module path, ...) in which `${TORNADOVM_HOME}`
is the unpacked directory:

```sh
SDK=$PWD/accelerator/tornado/target/tornadovm-sdk/tornadovm-8.0.0-opencl
T=$PWD/accelerator/tornado/target
sed "s|\${TORNADOVM_HOME}|$SDK|g" $SDK/tornado-argfile.template > $T/tornado.args
```

The tests which need a device (`KernelParityTest`, and the device part of `TornadoBruteForceAcceleratorTest`) are skipped unless the JVM options are given, as in CI, which has no device; the other tests run:

```sh
./mvnw -B -ntp -Pdev -pl accelerator/tornado -am test -Dtornado.argfile=$T/tornado.args
```

To run a class by hand:

```sh
CP=$T/classes:$T/test-classes:accelerator/core/target/classes:core/target/classes:api/target/classes
mkdir -p $T/run $T/home/.cache $T/tmp && cd $T/run   # the module path of the SDK contains '.', so run in a directory without jars
HOME=$T/home XDG_CACHE_HOME=$T/home/.cache TMPDIR=$T/tmp java @$T/tornado.args -Djava.io.tmpdir=$T/tmp -cp $CP com.github.skjolber.packing.tornado.ListDevices
```

The environment variables keep the caches of the GPU stack within the build directory (the AMD compiler cache, `comgr`, is the only thing it writes).

 * `GpuSmoke` (in the test sources): chooses the device, runs a vector add, and a kernel with data-dependent loops, `long` arithmetic and scratch memory per work item,
   and compares both with the same code run on the CPU.
 * `PrebuiltProbe`: checks the calling convention of a prebuilt kernel (the order of the parameters, the 16-byte header of a Java array on the device, the global work size).
   Run it after an upgrade of TornadoVM, before the kernel.
 * `KernelParity`: runs the accelerator on random problems and compares the winner with `FlatEngine`, printing the times. Arguments: `firstSeed seeds minBoxes maxBoxes window
   nodeBudget maxPoints targetItems cpuThreads repeat mode`, for example `KernelParity 1 20 8 10`.
   A twelfth argument, `nospace`, runs without the free space bound; the splits are printed.
   The mode is `exact` (default: the loop's winner), `any` (any best, compared by value), `loop` (the old loop kernel), or `allpointsexact` and `allpointsany` (the search of `BruteForcePackager`, all
   points, against the CPU tree engine). Give every run a `timeout`: a hung kernel never returns.
 * `StageProbe` and `ArrayProbe`: probes of the TornadoVM JIT path (below), kept for retrying it with a newer TornadoVM.

Verified: the 8.0.0 `jdk22plus` OpenCL SDK (and the 7.3.1 before it) on JDK 25, with the ROCm 7.1 OpenCL runtime on a Radeon RX 7600 (`gfx1102`, OpenCL 2.0 device); the integrated GPU (`gfx1036`) is listed as a second device.

## Speed

With the packager, the accelerator is by default used in `AcceleratorMode.AUTO`: the packager's own tree search starts at once and the device joins
after a gate of 25 ms, the first result winning (`AcceleratedFastBruteForcePackager.withAcceleratorMode(..)`, `withAcceleratorGate(..)`; `CPU_ONLY` and `GPU_ONLY`
are the other modes). The gate of each box count doubles when the device joins and loses and halves when it wins (`AdaptiveAcceleratorGate`; `withAcceleratorGate(..)` also takes a gate of
your own), so where the device keeps losing it soon joins only the containers which run far longer than the rest. On whole packings of 40 random problems per set with the packager
on 8 threads, any best: 11 to 12 boxes `CPU_ONLY` 0.59 s,
`GPU_ONLY` 1.19 s, `AUTO` 0.64 s; 13 to 14 boxes 2.31 s, 3.09 s, 2.46 s; 15 to 16 boxes 3.97 s, 3.33 s, 3.61 s (after the profiling below; before it `GPU_ONLY` took 4.73 s and
5.15 s). With the packager on one thread (`withThreads(1)`) the same sets take 5.6 s, 10.4 s and 18.3 s in `CPU_ONLY` and 1.0 s, 2.4 s and 3.0 s in
`AUTO`, where the device wins the hardest containers (seed 208 of 13 to 14 boxes: 7.7 s on the thread, 0.87 s in `AUTO`). On 8 threads the device joined 7 to 15 containers per set
and won none: the packager's tree, with its settled orders and free space bound, is faster than this kernel on every container, so `AUTO`
is the packager's own time plus the gate's overhead.

`AcceleratedBruteForcePackager` (every box at every free point where it fits, one thread) searches far longer, and there the device pays: on 12 random problems of 10 to 11
boxes `CPU_ONLY` takes 200 s, `GPU_ONLY` 9.3 s and `AUTO` 9.3 s (the device joined 10 containers and won 4; 25 s and 24 s before the profiling below); the hardest
problem takes 198 s on the packager alone and 8.4 s on the device, the next 1.7 s against 0.45 s. On 30 problems of 7 to 9 boxes, which the packager finishes in under a second, it is
0.90 s, 0.99 s and 0.88 s. The kernel searches with all points under
`FlatProblem.ALL_POINTS` in `runTree` (`KernelParity` modes `allpointsexact` and `allpointsany`: 30 problems of 7 to 9 boxes, the device 6.0 s against the CPU tree engine's 3.1 s in exact mode, 2.9 s
against 1.1 s under any best, no mismatches). The numbers below are the device against the CPU engine `FlatEngine`, the reference for the kernel.

Measured with `KernelParity` on a Radeon RX 7600 (16 compute units, 8 GB, ROCm OpenCL) against `FlatEngine` with 8 threads on 4 cores of a Ryzen 9 9950X, on 42 random problems of 11 to 12
boxes in tight containers (seeds 100 to 141, the first container only), every problem different. The device times include the host loop, the transfers and the one compilation (0.45 s). The CPU
engine runs in a fresh JVM, so its first problems are cold.

| Search | CPU | Device |
|---|---|---|
| any best | tree engine 1.44 s | tree 2.35 s (130 rounds) |
| exact (the loop's winner) | loop engine 19.1 s; tree engine 7.8 s | tree 33.2 s (269 rounds) |
| exact, old loop kernel | loop engine 19.8 s | loop kernel 42.0 s (359 rounds) |
| the packager's own tree search, any best (8 threads / 1 thread) | 0.61 s / 5.5 s | |

The last row is `FastBruteForcePackager.withThreads(8)` on master, with its free space bound and settled orders, measured as a whole packing including the later containers.

Per problem under any best:

 * the medium problems are 1.5 to 3 times slower on the device (seed 122: 0.26 s against 0.09 s; seed 128: 0.20 s against 0.09 s)
 * the problems where all boxes fit are faster on the device (seed 138: 13 to 67 ms against 40 to 160 ms; seed 109: 35 ms to 0.4 s against 20 to 100 ms)
 * the first search pays for the compilation (seed 100: 0.58 s against 14 ms)

In exact mode seed 109 alone takes 278 million nodes in 24 s on the device against 10 s for the CPU loop.

So under any best the device is 1.6 times slower than 8 CPU threads over the set. It lost by 1.6 times to the old loop engine before the tree search, but that engine is 14 times slower than the tree
engine is now. Against the packager's own tree search the device is about 4 times slower. The target of the design (3 times the whole CPU) is not reached.

Before the shared best (volume, weight, count) and the doubling budget, the same set took 35.5 s under any best, of which one problem (seed 109) took 29 s: 192 million nodes against the CPU tree's
2.6 million, because 2048 items each burnt their budget before the arrangements of the others reached them.

Note on measuring: the CPU times must be taken with a warm JVM. A first search in a fresh JVM includes the JIT compilation of the search and is 2 to 6 times slower, which made an earlier
version of this table claim the opposite.

The defaults come from tuning runs (any best, 40 problems of 11 to 12 boxes; window / node budget):

| Window / budget | 256 / 250 | 512 / 1000 | 512 / 250 | 1024 / 500 | 2048 / 1000 | 2048 / 250 | 4096 / 250 | 4096 / 1000 | 8192 / 250 |
|---|---|---|---|---|---|---|---|---|---|
| Time | 15.6 s | 10.1 s | 10.5 s | 6.6 s | 4.6 s | 4.3 s | 2.6 s | 2.9 s | 2.5 s |

 * One work item per work group, which the kernel requires (`reqd_work_group_size(1, 1, 1)`; `withLocalWorkSize(..)` is gone). The search diverges so much that the lanes of a
   wavefront mostly wait for each other: a work group of one was 2.5 times faster than the choice of the runtime, and work groups of 2, 4, 8, 16, 32 and 64 measured 4%, 7%, 15%, 35%,
   65% and 65% slower. Telling the compiler so is worth more than the choice itself: with the size required, every value derived from the slot is uniform within a wave, and the hardest
   14 box problem goes from 1.34 s to 0.97 s, the hardest problem of `AcceleratedBruteForcePackager` from 12.6 s to 8.4 s (28% and 34% less, the same nodes).
 * `withWindow(8192)` (was 2048): smaller windows lose, because fewer items in flight find the best later and the tail runs longer. The memory per slot is the scratch of the layout (about 80 KB of
   ints for 16 boxes and 64 points), so 8192 slots take about 650 MB; the number of slots is capped by the largest allocation of the device.
 *  `withNodeBudget(250)` (was 1000): after each round the best found so far bounds the next, so a small budget means fewer nodes in all; with a window of 4096 or more the budget matters little (2.6
   s at 250, 2.9 s at 1000).
 * `withMaxPoints(64)` (was 128): halves the scratch per slot, so that more slots fit.

 * `withFreeSpace(..)`: on for the loop's winner and for all points, off under any best at the best point only (see "How it works"). A/B on the 42 problems above (8 CPU threads, node
   budget 250):

   | Search | Without | With |
   |---|---|---|
   | CPU tree engine, any best | 0.48 s | 0.67 s |
   | CPU tree engine, exact | 6.5 s | 4.7 s |
   | device, any best | 2.6 s | 3.3 s |
   | device, exact | 33.3 s | 16.6 s |

   Exact halves on the device, and any best takes about a quarter more on the device, which is why it is off there. The search of `AcceleratedBruteForcePackager` (every box at every free
   point) branches far more, and there the bound pays under any best too: its hardest problem of 10 to 11 boxes (seed 208) goes from 23.2 s to 16.0 s on the device, the 12 problems of 10 to 11
   boxes from 25.0 s to 17.4 s, and 30 problems of 7 to 9 boxes from 1.66 s to 1.44 s; the CPU tree engine takes 29 s instead of 95 s on seed 208 (8 threads).
 * `withSplit(..)`: on. On the device under any best, the 42 problems above have 38 splits and no change in the total (2.8 s against 2.6 s, within the run-to-run noise). On 16 problems of 13 to
   14 boxes there are 1360 splits and the time goes from 4.1 s to 3.2 s, the hardest (seed 208) from 2.13 s to 1.79 s. On 12 problems of 15 to 16 boxes there are 268 splits, and the hardest (seed
   309) goes from 1.07 s to 0.66 s, while a few easy ones got slower by tens of milliseconds. Each slot's depth is passed to the kernel in the active array (the depth of its item plus one):
   before that, the children searched the parent's whole subtree again, which the hardest problem of `AcceleratedBruteForcePackager` of 10 to 11 boxes shows (49 s to 23 s on the device with the fix; the sets above
   are unchanged within the noise).

Larger problems, the device against the CPU tree engine with 8 threads on 4 cores, any best, each in a fresh JVM so that the first problems of the CPU are cold:

| Problems | CPU | Device |
|---|---|---|
| 16 problems of 13 to 14 boxes | 2.7 s | 3.2 s |
| 12 problems of 15 to 16 boxes | 1.4 s | 2.0 s |

Per problem the device wins on the hard ones and loses on the many easy ones. Seed 208: 2.37 s on the CPU against 1.79 s on the device. Seed 307: 0.78 s against 0.12 s. Seed 309: 0.50 s against
0.66 s. Seed 205: 0.16 s against 0.09 s. On the easy ones the CPU is far ahead (seed 209: 1 ms against 65 ms; seed 301: 49 ms against 230 ms), because the device has a floor of about 20 to 60 ms
per problem (rounds and transfers) and the 0.45 s of compilation on the first search.

### Profiling the kernel

Where the time of a device search goes, measured with the TornadoVM profiler (`-Dtornado.profiler=True` prints a JSON per execution) and Java Flight Recorder on the 13 to 14 box set, GPU
only: 92% is the kernel, 2% the transfers (5 MB in and 1.9 MB out per round, 0.4 ms), 4% the compilation of the first search, and the rest the host loop over the slots. The CPU twin of the
kernel (`FlatTree`, profiled with JFR on seed 208 of `BruteForcePackager`) spends 59% of its time in `FlatPoints.add` (the free points after a placement), 32% in the search loop itself,
8% in `isEclipsed` and the rest in the sorts and the binary search: the kernel's cost per node is the same code, so these are the places to make the kernel cheaper.

The kernel itself can be compiled offline with the LLVM in Fedora (`clang -x cl -cl-std=CL2.0 -target amdgcn-amd-amdhsa -mcpu=gfx1102 -nogpulib -O2 -S subtree.cl`; `gfx1102` is the
Radeon RX 7600) and its `.vgpr_count`, `.vgpr_spill_count`, `.sgpr_spill_count` and `.private_segment_fixed_size` read from the assembly. The kernel uses all 256 vector registers, so
the device runs about 6 waves per SIMD, and with a work group of one each wave is one search; a lone search makes about 26000 nodes per second (38 µs per node, memory latency), the
device about 8 million per second with every slot busy. What was measured on seed 208 of 14 boxes (any best, 1.7 s) and seed 208 of `BruteForcePackager` (16.0 s):

 * The layout of the scratch (the offsets of `FlatLayout`) was a private struct of 48 ints, read once per work item: it lived in scratch memory (272 bytes per lane), with 195 scalar and 22
   vector register spills. The kernel now reads the offsets from the layout ints in global memory (`L_*`, uniform across the work items, so they come through the scalar cache): 44 and 2
   spills, and the two problems take 1.43 s and 13.6 s (14% and 15% less). The Java side is unchanged.
 * The predicates of a point (`fits3D`, `eclipses`, `eclipsesMoved*`, `supported*`) combine their comparisons with `&` and `|` instead of `&&` and `||`, in the kernel and in the Java
   twin: the loads of the point are issued together instead of one after the other, 1.43 s to 1.34 s and 13.6 s to 12.6 s (6% and 7%). Hoisting the loads of the sort predicates
   (`isAfterX`, `isAfterTied`, `canMove*`) and scanning two points at a time in the all points search measured within the noise and were left out.
 * `reqd_work_group_size(1, 1, 1)` on the kernel: 1.34 s to 0.97 s and 12.6 s to 8.4 s, see "Tuning". With it, `amdgpu_waves_per_eu(16, 16)` (64 vector registers, 4 spills) is the
   same, 12 and 8 waves are slower (9.8 s and 11.9 s), and `amdgpu_num_vgpr(128)` alone is the same as without.
 * Forcing `addPoint` inline (the compiler keeps it a function, called once per node) made both slower (2.0 s and 18.3 s), as did `noinline` on the cold functions (`offerTree`, `isBetter`,
   `isHopelessInSpace`: 1.9 s and 18.7 s): calls are costly, but so is a larger hot function.
 * `restrict` on the buffers changed nothing. `withMaxPoints(32)` or 24 instead of 64: the same time, so the scratch footprint is not what limits the kernel.
 * On the host, a pilot first round (the 256 most promising items alone, with a budget of 128 nodes, so that their best bounds the rest from their first node) did not pay on the 13 to 14
   box set (4.25 s to 4.4 s): the items with the largest prefixes are small subtrees, and their best is not the one which prunes. Splitting after 1 or 2 rounds instead of 4 made the host
   copy the item arrays 60000 times (5.8 s), and a budget factor of 1 or 8 instead of 64 changed nothing.
 * The device searches far more nodes than the CPU tree engine on the easy problems (seed 200: 224000 against 6300; seed 209: 488000 against 9300) and the same on the hard ones (seed 208:
   7.95 million against 7.84 million): the first rounds run every item before the best bounds them, which is the floor of 20 to 60 ms per problem. The rounds with most slots busy are
   three quarters of the device time of the 13 to 14 box set; the tail with few items is the rest.

What could still change the picture: the settled orders of the CPU tree (they matter for the loop's exact winner only; the item search keeps none), a layout of the scratch which coalesces the
memory accesses of a wavefront (not tried), and problems where the CPU needs seconds, where the floor per problem is small against the search.

## Why the kernel is written by hand

The flat classes were written so that the TornadoVM JIT could compile `FlatSubtree.run(..)` from the Java (a `@Parallel` loop over the slots, see `StageProbe` for what is left of it). With
TornadoVM 7.3.1 (OpenCL) that did not work: the JIT compiles the search (36,000 lines of OpenCL, once its inlining limits are raised with `-Dgraal.MaximumInliningSize=20000
-Dgraal.MaximumDesiredSize=2000000`), but the kernel does not run correctly. Found by bisecting on the device, each of which turned a wrong result or a hang into a match with the host:

 * `Integer.compare` and `Long.compare` produce a node which the backend cannot lower (a JIT error); boolean predicates compile.
 * A `return` inside a loop gave wrong results (`-1` or `0` from `FlatPermutation.next` and `FlatSearch.searchPermutation`); a `continue` in a loop gave wrong results too.
 * With those rewritten, every function matched the host when run in a straight line, and a search of one box matched, but the second iteration of the search loop of
   `FlatSearch.searchPermutation` hangs the kernel, whatever the loop does in it; the host thread then blocks in `clEnqueueReadBuffer` for ever.

The JIT does not reject code it cannot lower: it compiles it, and the kernel then returns wrong results or never finishes. TornadoVM 8.0.0 replaced that compiler with its own (TC): it refuses the search outright instead (`TC-E1009 more than 64 distinct callees:
recursion or an unbounded call graph`, from `StageProbe` on 8.0.0), so the prebuilt kernel stays the way to run it. The prebuilt kernel is compiled by the OpenCL compiler of the
driver, takes about 400 ms to compile, and runs the search as written.
