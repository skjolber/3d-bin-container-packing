# Accelerated brute force

`AcceleratedFastBruteForcePackager` (a `FastBruteForcePackager` in the `accelerator-core` artifact, JPMS module `com.github.skjolber.packing.accelerator`) and `AcceleratedBruteForcePackager` (a
`BruteForcePackager` in the same artifact, see [below](#the-brute-force-packager)) can hand the search of all box orders
(permutations) of a container to a `BruteForceAccelerator`, such as a GPU. The accelerator is
optional and off by default. Without one, the packagers work as described in [brute-force](brute-force.md), as do `FastBruteForcePackager` and `BruteForcePackager` in `core`, which has no
accelerator code.

```xml
<dependency>
    <groupId>com.github.skjolber.3d-bin-container-packing</groupId>
    <artifactId>accelerator-core</artifactId>
    <version>${3d-bin-container-packing.version}</version>
</dependency>
```

```java
AcceleratedFastBruteForcePackager packager = AcceleratedFastBruteForcePackager
    .newBuilder()
    .withAccelerator(FlatEngine.newBuilder().withThreads(8).build())
    .build();
```

The accelerator only picks the best box order (and by default searches alongside the packager, see [modes](#modes)). The packager then places that order itself, with its own search, so the returned
placements are the ones which
the packager returns without an accelerator (in exact mode; under any best the value is the same, the arrangement can differ). If the accelerator gives up, or the placement does not confirm the
winner (count, load volume and rotations),
the packager searches the permutations itself (`getAcceleratedCount()`, `getAcceleratorFallbackCount()` and `getAcceleratorMismatchCount()` of the packager count these). Close the accelerator when
done: `FlatEngine` owns threads.

## Modes

`withAcceleratorMode(..)` says how the packager uses the accelerator for the containers it applies to:

 * `CPU_ONLY`: the packager searches every container itself; the accelerator is not used.
 * `GPU_ONLY`: the accelerator searches every container it applies to; the packager searches itself only where the accelerator gives up.
 * `AUTO` (the default): the packager starts its own search at once and, after a gate (`withAcceleratorGate(..)`, 25 ms by default, about the least
   any search takes on a device), the accelerator searches the same container alongside on its own thread; the first result wins and stops the
   other. The easy containers never reach the accelerator; the hard ones take the faster of the two. The accelerator's winner is placed and confirmed
   as in `GPU_ONLY`. `getAcceleratedCount()`, `getAcceleratorRaceCpuCount()` and `getAcceleratorFallbackCount()` count who finished which container.
   The gate adapts to the two sides, per box count (`withAcceleratorAdaptiveGate(..)`, on by default): the gate of a box count starts at the
   configured gate, doubles after a race which the accelerator joined and lost, and halves after one it won, between the configured gate and
   1024 times it (`getAcceleratorGateMillis(boxes)` tells the current one). Where the accelerator keeps losing it soon joins only the containers which
   run far longer than the rest, where it keeps winning it joins at the configured gate; a container which the packager finishes before the gate
   teaches nothing.
   A packager hands its searches to the accelerator on one thread, one after the other: when the packager is used from several threads at once, a
   search whose race is over before its turn is dropped from the queue, and never reaches the accelerator.

Measured with `FastBruteForcePackager.withThreads(8)` and the TornadoVM accelerator (a Radeon RX 7600) on whole packings of 40 random problems per
set, 8 containers each, any best:

| Boxes | `CPU_ONLY` | `GPU_ONLY` | `AUTO` | containers the device joined / won in `AUTO` |
|---|---|---|---|---|
| 11 to 12 | 0.59 s | 1.75 s | 0.70 s | 15 / 0 |
| 13 to 14 | 2.31 s | 4.73 s | 2.50 s | 16 / 0 |
| 15 to 16 | 3.97 s | 5.15 s | 3.84 s | 20 / 0 |

The packager's own tree on 8 threads, with its settled orders and free space bound, finishes first on every container the device joined, so `AUTO`
costs the gate's thread and the interrupt checks (a few percent, within the noise on the largest set) and gains nothing with this kernel; it pays
when the device wins some containers, as it does against the CPU engine `FlatEngine` (see [the module README](../../accelerator/tornado/README.md)) and
for [`BruteForcePackager`](#the-brute-force-packager), whose search is much longer.

## The brute-force packager

`AcceleratedBruteForcePackager` is `BruteForcePackager` with the same accelerator options, modes and counters. `BruteForcePackager` tries a box at every free point where it fits, in the
order of the points, where the fast packager takes the best point of each rotation; so its search of a container is larger by a factor which grows with the boxes, and it runs on one
thread. The accelerator searches the same way under `FlatProblem.ALL_POINTS`, which only the tree search takes (`FlatTree`, the kernel's `runTree`): a `FlatEngine` needs `withTree(true)`
for it, an engine without does not support the problems of this packager, which then searches itself. The accelerator picks the box order, the packager places it with its own point
calculator and confirms it, so the placements are those of `BruteForcePackager` in exact mode (`withTreeSearchAnyBest(false)`), and of the same value under any best. The accelerator applies
under the conditions [below](#when-it-is-used), plus no point filter (`withPointFilter(..)`) and no reverse permutation skip (`withSkipReversePermutations(..)`).

```java
AcceleratedBruteForcePackager packager = AcceleratedBruteForcePackager
    .newBuilder()
    .withAccelerator(FlatEngine.newBuilder().withThreads(8).withTree(true).build())
    .build();
```

Measured with the TornadoVM accelerator on the same device, whole packings of random problems, 8 containers each, any best, `BruteForcePackager` on its one thread:

| Boxes | Problems | `CPU_ONLY` | `GPU_ONLY` | `AUTO` | containers the device joined / won in `AUTO` |
|---|---|---|---|---|---|
| 7 to 9 | 30 | 0.90 s | 1.51 s | 0.87 s | 7 / 0 |
| 10 to 11 | 12 | 200 s | 25 s | 24 s | 10 / 4 |

The search of `BruteForcePackager` is where the device pays: on the hardest problem of the second set (seed 208) the packager takes 198 s alone and the device 23 s, on the next (seed 205) 1.7 s
against 0.8 s, and the easy ones stay the packager's in `AUTO`. The device loses the problems which the packager finishes in less than about 0.3 s, to its floor of tens of milliseconds per
search, which is what the gate is for. On the CPU, `ParallelBruteForcePackager` (not accelerated) runs the same search on threads.

## When it is used

A container is searched by the accelerator when all of these hold (for `AcceleratedBruteForcePackager` also no point filter and no reverse permutation skip), otherwise the packager does it itself:

 * no box item order (`Order.NONE`), no box item groups
 * no load limits, no full support (`withRequireFullSupport(true)`), no obstacles or initial points
 * the default point comparator, and the default result comparator (or one which prefers a higher load volume)
 * no extraction orders, no insert-after counts, and no container priorities (the boxes are not permuted in blocks)
 * the accelerator supports the problem (`supports(..)`); `FlatEngine` takes 8 to 64 boxes by default, the search space of fewer boxes is too small to pay for
   threads, see `FlatEngine.Builder.withMinBoxes(..)`

## The flat search

The search of one permutation (each box in each rotation at its best free point, with the extreme-point bookkeeping) is also available on primitive `int` and
`long` arrays in package `com.github.skjolber.packing.accelerator` (the `accelerator-core` module): `FlatPoints`, `FlatSearch`, `FlatPermutation`, `FlatSubtree`, `FlatTree`, `FlatBound`, `FlatSpace`
and `FlatEngine`.
It is a port of the object-based search, so the arrays are what a device kernel can work on: no allocation, no objects, no recursion, no virtual calls.
The differential tests in `accelerator-core` run it against the object-based search permutation by permutation (count, load volume, rotations), and the packager with
an accelerator against the packager without.

`FlatEngine` splits the permutations in items by their first boxes (a prefix of box types), searches the items on threads, and reduces the winners of the items
in the order of the iterator: the most load volume, then load weight, then boxes; the first permutation wins a tie, and the first permutation which places all boxes
wins outright. A permutation which cannot be better than an earlier one need not be searched, which is how the packager and the engine skip permutations.

An item is searched as one tree by default (`FlatTree`, the same search as the packager's `BruteForceTreeSearch`, see [tree search](brute-force.md#tree-search)):
each prefix of placements is placed once, where `FlatSubtree` places it again for every order which starts with it. A node, or a box, is pruned when the prefix
plus the best sub-multiset of the remaining boxes within the load left cannot beat the best. That bound is a table with one entry per multiset of remaining
boxes (`FlatBound`), computed once per problem on the host and left out above 4096 multisets, where the bound is the whole of the remaining boxes within the load
left, which is looser. The items share their best arrangement (volume, weight, count) between rounds, so a strong arrangement of one item prunes the others.
The item keeps no table of settled orders (the CPU tree has one).

A node is also bounded by its free space, as the CPU tree does (`FlatSpace`, a port of `BruteForceTreeFreeSpace`): the free space of the path of the node is kept as maximal empty spaces on the
scratch (at most 48 per level, `FlatLayout.MAX_SPACES`; more makes the level unknown, in which every box is taken to fit). The box types which fit none of them in any rotation can never be placed
below the node, so the prefix plus the remaining boxes of the other types within the load left must beat the best. It is checked once per node, when its first child is about to be placed, and
only where three more of the smallest remaining boxes fit the load left; an item gives it up when fewer than one check in sixteen cuts. The switch is `FlatEngine.Builder.withFreeSpace(..)`, by
default on for the loop's winner (exact) and off under any best, because with ties pruned the bound table leaves it little to cut for its cost.

What counts as the best depends on `FlatProblem.ANY_BEST`, which the packager sets from `withTreeSearchAnyBest(..)`:

 * any best (the packager's default, `withTreeSearchAnyBest(true)`): any arrangement of the best value will do. The types are tried largest first, the items with the
   largest prefixes run first, ties are pruned, and the first arrangement which places all boxes ends the search. The arrangement can differ from the one of the
   loop, with the same value.
 * exact (`withTreeSearchAnyBest(false)`): the winner of the loop. The types are tried in their order, and only strictly worse prefixes are pruned.

On a device, a tree item which runs on while most slots are idle can be split into its children (`TornadoBruteForceAccelerator.Builder.withSplit(..)`, on by default, any best only): see the
module README.

`FlatEngine.Builder.withTree(false)` selects the old search, of the orders one at a time with `FlatSubtree`, which is the reference for the loop's winner.

Limits:

 * A search which needs more free points than `withMaxPoints(..)` (default 128 for the engine, 64 for the Tornado accelerator) makes the engine give up; the
   packager then searches itself. The most free points seen on the test inputs is 54 (see below), for 10 to 14 boxes.
 * The interrupt is polled between the rounds of a tree item (every node budget of nodes) and between the orders of a loop item, so it reacts within the time
   of a round or of one order, which can be many milliseconds for hard inputs.
 * With one thread the engine runs on the calling thread.

## Speed

The flat search does the same work per node as the object search: on a single thread it is about as fast (0.98 to 1.09 times), because the extreme-point
bookkeeping dominates and the objects are already cheap. A speed-up comes from running many items at once.

Measured on the engine's reference inputs (10 to 14 boxes, a Ryzen 9 9950X; `FlatStatsReport` in the tests prints these numbers), for the search of the orders one at a time:

 * hard inputs cost 200,000 to 800,000 nodes per permutation, at about 270 to 330 ns per node, evenly spread (the slowest 1% of searches take 3 to 6% of the time)
 * easy inputs finish in a few hundred nodes, with a heavy tail: the slowest 1% of searches can take more than half of the time (up to 80% in the measurements)

The tree search of the items is much faster than that on the same inputs. On 42 random problems of 11 to 12 boxes (8 threads on 4 cores), the tree engine takes 1.44 s under
any best and 7.8 s in exact mode, where the loop engine takes 19.1 s in exact mode. The packager's own tree search (with its free space bound and settled orders) takes 0.61 s
for the same problems under any best, and 5.5 s on one thread. The free space bound takes the exact mode of the tree engine from 6.5 s to 4.7 s on the same set (measured later, 11 to 12 boxes,
node budget 250), and costs under any best (0.48 s to 0.67 s), so it is off there by default.

On the CPU `ParallelBruteForcePackager` already runs the permutations in parallel, so the engine is not a replacement for it; it is the exact reference for
a device. A device has to beat all the threads of the CPU, which for this branchy search (a depth-first search with data-dependent control flow, a lot of
scratch memory per work item) is not a given. The honest expectation for a recent GPU is between 0.5 and 2 times the speed of 32 CPU threads, and the point of
the SPI is that this can be measured without changing the packager.

## Running on a GPU with TornadoVM

The module [`accelerator/tornado`](../../accelerator/tornado/README.md) (Java 22+) is an experimental [TornadoVM](https://www.tornadovm.org/)
accelerator: `TornadoBruteForceAccelerator` chooses the device (by default the OpenCL GPU with the most compute throughput) and runs the search of the items on it, one work
item per item, with the host loop of `FlatEngine` around it. The kernel is a hand-written OpenCL C port of the flat classes of `accelerator-core`, run as a TornadoVM prebuilt task: the
TornadoVM 7.3.1 JIT compiles the search from the Java but miscompiles its control flow (the kernel hangs), and the compiler of 8.0.0 refuses it (too many
callees), see the module README. The winner of the device is checked
against `FlatEngine` on random problems by `KernelParityTest` (skipped without a device), and the packager with the accelerator against the packager without.

```java
TornadoBruteForceAccelerator accelerator = TornadoBruteForceAccelerator.newBuilder().build();
AcceleratedFastBruteForcePackager packager = AcceleratedFastBruteForcePackager.newBuilder().withAccelerator(accelerator).build();
```

It needs the TornadoVM SDK with an OpenCL, CUDA or Metal backend for the device (AMD cards use OpenCL), which is not on Maven Central; without a runtime the accelerator gives up and
the packager searches itself. The module README shows how to run with the SDK.

Each item is searched as one tree on the device too (`FlatTree`, the kernel function `runTree`, a port of it), the kernel is compiled once per accelerator (about 0.45 s),
and the items share their best arrangement through the host between rounds.

On a Radeon RX 7600 against `FlatEngine` with 8 threads (4 cores of a Ryzen 9 9950X), 42 random problems of 11 to 12 boxes, every problem different:

 * any best: the CPU tree engine takes 1.44 s and the device 2.35 s (130 rounds), so the device is 1.6 times slower. The medium problems are 1.5 to 3 times slower on the
   device, the problems where all boxes fit are faster on the device, and the first search pays the 0.45 s of compilation.
 * exact (the loop's winner): the CPU loop engine takes 19.1 s and the device tree 33.2 s (269 rounds). The CPU tree engine in exact mode takes 7.8 s.

With the free space bound the device takes 16.6 s instead of 33.3 s in exact mode, and 3.3 s instead of 2.6 s under any best (off by default there). Splitting a long item into its children, under
any best, changes nothing on these problems (38 splits) but helps on larger ones: on 12 problems of 15 to 16 boxes the hardest goes from 1.07 s to 0.66 s, and on the hardest problem of
`BruteForcePackager` of 10 to 11 boxes the device goes from 49 s to 23 s once each child's depth reaches the kernel (before that fix the children searched the parent's whole subtree again). Against the CPU
tree engine on larger problems the device takes 3.2 s against 2.7 s (16 problems of 13 to 14 boxes) and 2.0 s against 1.9 s (12 problems of 15 to 16 boxes): it wins single hard problems (seed
307: 0.12 s against 0.75 s) and loses the many easy ones, where its floor of 20 to 60 ms per problem and the 0.45 s of compilation decide.

The device is about 4 times slower than the packager's own tree search on the CPU (0.61 s for the same problems). It lost by 1.6 times to the loop engine before the tree
search too, but that engine is 14 times slower than the tree engine, so the device is no closer. The accelerator is not worth using as it is; the numbers, the tuning and what
could change the picture (the settled orders in the kernel, a coalesced layout of the scratch, problems where the CPU needs seconds) are in the module README.

To write another kernel, port the flat classes of `accelerator-core` as the tornado module does and keep both in step, see [DEVELOPER.md](../../DEVELOPER.md#flat-search-and-accelerators).

## Tests

These are in `accelerator/core/src/test/java/com/github/skjolber/packing/accelerator/` (and `IteratorSetPermutationTest` in `core`).

 * `FlatPointsAddTest`, `FlatBestPointTest`, `FlatSearchPermutationTest` and `FlatPermutationTest`: the flat model against the object model
 * `FlatEngineTest`: the engine against a reference loop, with threads, bounds, a tiny point limit, and interrupts
 * `FlatTreeTest`: the tree item against the loop over the orders: the same winner in exact mode with one and three threads, with a bound and a limit, and with a node
   budget of one, and with more types than boxes (`sameWinnerWithTypesWithoutBoxes`: the later containers of a packing keep every type, with no box left of some); the same value under any best,
   with the arrangement confirmed by the packager's search of its order
 * `KernelParityTest` and `TornadoBruteForceAcceleratorTest` (with a device): the device against the CPU engine (`loopKernelWinnerIsTheWinnerOfTheCpuEngine`,
   `anyBestHasTheValueOfTheCpuEngine`), and the packager with the accelerator against the packager without
   (`sameValuesAsThePackagerWithoutAcceleratorWhenAnyBestWillDo`)
 * `FlatAcceleratedPackagerTest`: placements with and without the accelerator are identical (the packager runs in exact mode for this, because under any best the tree may pick
   another arrangement of the same value), fallbacks, interrupts, and the inputs which are not accelerated
 * `FlatBruteForcePackagerTest`: `BruteForcePackager` with the CPU engine as its accelerator against `BruteForcePackager` without: the same placements in exact mode (`GPU_ONLY` and `AUTO`), the
   same values under any best, and an engine without the tree search does not apply; `TornadoBruteForceAcceleratorTest.bruteForcePackagerAgreesWithTheDevice` on a device
 * `IteratorSetPermutationTest` (in `core`): moving the iterator to a permutation
