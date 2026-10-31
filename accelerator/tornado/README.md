# tornado-accelerator

Experimental [TornadoVM](https://www.tornadovm.org/) accelerator for `FastBruteForcePackager` (see [accelerated brute force](../../docs/heuristics/accelerated-brute-force.md)):
the search of the box orders of a container runs on a GPU (or another OpenCL, CUDA or Metal device of TornadoVM), and the packager places the winning order itself, so
the placements are the same as without the accelerator.

It needs Java 22+, and is only built with the `tornado` profile; the default build does not contain it:

```sh
./mvnw -B -ntp -Ptornado,dev -pl accelerator/tornado -am test
```

```java
TornadoBruteForceAccelerator accelerator = TornadoBruteForceAccelerator.newBuilder().build();
FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withAccelerator(accelerator).build();
...
packager.close();
accelerator.close(); // releases the device memory and the compiled kernel
```

The accelerator is only used if it is passed to the packager. Without a TornadoVM runtime and device it gives up, and the packager searches as it does without one; the same
when a search needs more free points than `withMaxPoints(..)` (the layout of the scratch is fixed up front), or when the device fails (`getLastFailure()`). One search runs on the
device at a time; searches from other threads wait.

## How it works

 * `DeviceSelector`, `DeviceInfo` and `TornadoDevices`: find the devices of the TornadoVM runtime and choose one, by default the OpenCL GPU with the most compute throughput
   (compute units times clock, then the most memory: an integrated GPU shares the memory of the system, so it can report more than the discrete card). Without a runtime the list is
   empty. Exclude a device with `withExcludedName(..)`, or choose by name with `withNameContains(..)`.
 * `SubtreeKernel` and `src/main/resources/.../subtree.cl`: the kernel. One work item runs `FlatTree.runTree(..)` for one slot of a window, with its own scratch, for at most
   `withNodeBudget(..)` search nodes, then pauses; `run(..)`, the port of `FlatSubtree`, is the old search of the orders one at a time (`withTree(false)`). It is a hand-written OpenCL C port of the
   flat package of the core module (`FlatGeometry`, `FlatPointOrder`, `FlatPoints`, `FlatPermutation`, `FlatSearch`, `FlatSubtree`, `FlatTree`, `FlatBound`), function by function with the same
   names and statements, run as a TornadoVM *prebuilt* task. See below for why it is not compiled by the TornadoVM JIT. A change to the flat package is made in the kernel too;
   `KernelParityTest` checks that the two agree.
 * `TornadoBruteForceAccelerator`: the host loop of `FlatEngine` on the device. The permutations are split in items by their first boxes, a prefix of box types (`withTargetItems(..)`
   items at least). Each item is searched as one tree by default (`withTree(true)`), as the packager's `BruteForceTreeSearch` does on the CPU: each prefix of placements is placed once,
   and a node or a box is pruned when the prefix plus the best sub-multiset of the remaining boxes within the load left cannot beat the best. The bound is a table per multiset of remaining
   boxes (`FlatBound`), computed once per problem on the host and left out above 4096 multisets. The items share their best arrangement (volume, weight, count) through the host between
   rounds. Under any best (the packager's default, passed in `FlatProblem.ANY_BEST`) the types are tried largest first, the items with the largest prefixes run first, ties are pruned and the
   first arrangement which places all boxes ends the search; for the exact winner of the loop (`withTreeSearchAnyBest(false)`) the types are tried in their order and only strictly worse
   prefixes are pruned. The item keeps no table of settled orders and has no free space bound, which the CPU tree has.
   * One session holds the arrays, sized for a number of boxes (the first problem's, rounded up to 8; a larger problem makes a larger session once), and the compiled kernel; the kernel reads the
     layout and the sizes from the arrays each run. The kernel is compiled once per accelerator, which takes about 0.45 s.
   * A window of `withWindow(..)` slots runs at a time, each with its own scratch on the device (`withMaxPoints(..)` free points at each level). A tree item keeps its slot (its state is the scratch of
     the slot) until it is done, and the host assigns free slots to the next items each round. The number of slots is capped by the largest allocation of the device.
   * The node budget of a round starts at 32 and doubles each round up to `withNodeBudget(..)`, so that a strong arrangement bounds the others before they search much. When few items are left the
     budget grows up to 64 times, so that the rounds (transfers and launch) do not dominate the tail.
   * After each run the host reads the state of the slots, polls the interrupt, and runs the next round; the winners of the items are reduced as in `FlatEngine`.
 * `ListDevices`: a `main` which prints the devices and the chosen one, as a smoke test of a TornadoVM installation.

The dependency on `tornado-api` is `provided`: the API jar has no native drivers. The runtime is the TornadoVM SDK, which is not on Maven Central.

## Running against a device

Download the SDK for your backend and JDK from the [TornadoVM releases](https://github.com/beehive-lab/TornadoVM/releases), for example `tornadovm-7.3.1-jdk22plus-opencl-linux-amd64.tar.gz`, and unpack it
(for example into `accelerator/tornado/target/tornadovm-sdk`, which `mvn clean` removes). The OpenCL backend has no native library of its own: it calls the system OpenCL loader
(`libOpenCL.so`) and so the OpenCL driver of the GPU (on AMD, the ROCm OpenCL runtime). The SDK contains `tornado-argfile.template`, the JVM options (JVMCI, module path, ...) in which `${TORNADOVM_HOME}`
is the unpacked directory:

```sh
SDK=$PWD/accelerator/tornado/target/tornadovm-sdk/tornadovm-7.3.1-opencl
T=$PWD/accelerator/tornado/target
sed "s|\${TORNADOVM_HOME}|$SDK|g" $SDK/tornado-argfile.template > $T/tornado.args
```

The tests which need a device (`KernelParityTest`, and the device part of `TornadoBruteForceAcceleratorTest`) are skipped unless the JVM options are given:

```sh
./mvnw -B -ntp -Ptornado,dev -pl accelerator/tornado -am test -Dtornado.argfile=$T/tornado.args
```

To run a class by hand:

```sh
CP=$T/classes:$T/test-classes:core/target/classes:api/target/classes
mkdir -p $T/run $T/home/.cache $T/tmp && cd $T/run   # the module path of the SDK contains '.', so run in a directory without jars
HOME=$T/home XDG_CACHE_HOME=$T/home/.cache TMPDIR=$T/tmp java @$T/tornado.args -Djava.io.tmpdir=$T/tmp -cp $CP com.github.skjolber.packing.tornado.ListDevices
```

The environment variables keep the caches of the GPU stack within the build directory (the AMD compiler cache, `comgr`, is the only thing it writes).

 * `GpuSmoke` (in the test sources): chooses the device, runs a vector add, and a kernel with data-dependent loops, `long` arithmetic and scratch memory per work item,
   and compares both with the same code run on the CPU.
 * `PrebuiltProbe`: checks the calling convention of a prebuilt kernel (the order of the parameters, the 16-byte header of a Java array on the device, the global work size).
   Run it after an upgrade of TornadoVM, before the kernel.
 * `KernelParity`: runs the accelerator on random problems and compares the winner with `FlatEngine`, printing the times. Arguments: `firstSeed seeds minBoxes maxBoxes window
   nodeBudget maxPoints targetItems cpuThreads repeat localWorkSize mode`, for example `KernelParity 1 20 8 10`.
   The mode is `exact` (default: the loop's winner), `any` (any best, compared by value) or `loop` (the old loop kernel). Give every run a `timeout`: a hung kernel never returns.
 * `StageProbe` and `ArrayProbe`: probes of the TornadoVM JIT path (below), kept for retrying it with a newer TornadoVM.

Verified: the 7.3.1 `jdk22plus` OpenCL SDK on JDK 25, with the ROCm 7.1 OpenCL runtime on a Radeon RX 7600 (`gfx1102`, OpenCL 2.0 device); the integrated GPU (`gfx1036`) is listed as a second device.

## Speed

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

 * `withLocalWorkSize(1)`: one work item per work group. The search diverges so much that the lanes of a wavefront mostly wait for each other; a work group of one
   was 2.5 times faster than the choice of the runtime (a work group of 32 was no better than that).
 * `withWindow(8192)` (was 2048): smaller windows lose, because fewer items in flight find the best later and the tail runs longer. The memory per slot is the scratch of the layout (about 80 KB of
   ints for 16 boxes and 64 points), so 8192 slots take about 650 MB; the number of slots is capped by the largest allocation of the device.
 *  `withNodeBudget(250)` (was 1000): after each round the best found so far bounds the next, so a small budget means fewer nodes in all; with a window of 4096 or more the budget matters little (2.6
   s at 250, 2.9 s at 1000).
 * `withMaxPoints(64)` (was 128): halves the scratch per slot, so that more slots fit.

What could still change the picture: the free space bound and the settled orders in the kernel (the CPU tree has both, and does less work for it), splitting a long item into its children on the
host (the device's form of work stealing), and larger problems (14 boxes and more), where a search takes seconds on the CPU. A layout of the scratch which coalesces the memory accesses of a
wavefront has not been tried.

## Why the kernel is written by hand

The flat package was written so that the TornadoVM JIT could compile `FlatSubtree.run(..)` from the Java (a `@Parallel` loop over the slots, see `StageProbe` for what is left of it). With
TornadoVM 7.3.1 (OpenCL) that did not work: the JIT compiles the search (36,000 lines of OpenCL, once its inlining limits are raised with `-Dgraal.MaximumInliningSize=20000
-Dgraal.MaximumDesiredSize=2000000`), but the kernel does not run correctly. Found by bisecting on the device, each of which turned a wrong result or a hang into a match with the host:

 * `Integer.compare` and `Long.compare` produce a node which the backend cannot lower (a JIT error); boolean predicates compile.
 * A `return` inside a loop gave wrong results (`-1` or `0` from `FlatPermutation.next` and `FlatSearch.searchPermutation`); a `continue` in a loop gave wrong results too.
 * With those rewritten, every function matched the host when run in a straight line, and a search of one box matched, but the second iteration of the search loop of
   `FlatSearch.searchPermutation` hangs the kernel, whatever the loop does in it; the host thread then blocks in `clEnqueueReadBuffer` for ever.

The JIT does not reject code it cannot lower: it compiles it, and the kernel then returns wrong results or never finishes. The prebuilt kernel is compiled by the OpenCL compiler of the
driver, takes about 400 ms to compile, and runs the search as written.
