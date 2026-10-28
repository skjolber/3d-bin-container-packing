# Accelerated brute force

`FastBruteForcePackager` can hand the search of all box orders (permutations) of a container to a `BruteForceAccelerator`, such as a GPU. The accelerator is
optional and off by default. Without one, the packager works as described in [brute-force](brute-force.md).

```java
FastBruteForcePackager packager = FastBruteForcePackager
    .newBuilder()
    .withAccelerator(FlatEngine.newBuilder().withThreads(8).build())
    .build();
```

The accelerator only picks the best box order. The packager then places that order itself, with its own search, so the returned placements are the ones which
the packager returns without an accelerator. If the accelerator gives up, or the placement does not confirm the winner (count, load volume and rotations),
the packager searches the permutations itself. Close the accelerator when done: `FlatEngine` owns threads.

## When it is used

A container is searched by the accelerator when all of these hold, otherwise the packager does it itself:

 * no box item order (`Order.NONE`), no box item groups
 * no load limits, no full support (`withRequireFullSupport(true)`), no obstacles or initial points
 * the default point comparator, and the default result comparator (or one which prefers a higher load volume)
 * no extraction orders, no insert-after counts, and no container priorities (the boxes are not permuted in blocks)
 * the accelerator supports the problem (`supports(..)`); `FlatEngine` takes 8 to 64 boxes by default, the search space of fewer boxes is too small to pay for
   threads, see `FlatEngine.Builder.withMinBoxes(..)`

## The flat search

The search of one permutation (each box in each rotation at its best free point, with the extreme-point bookkeeping) is also available on primitive `int` and
`long` arrays in package `com.github.skjolber.packing.packer.bruteforce.flat`: `FlatPoints`, `FlatSearch`, `FlatPermutation`, `FlatSubtree` and `FlatEngine`.
It is a port of the object-based search, so the arrays are what a device kernel can work on: no allocation, no objects, no recursion, no virtual calls.
The differential tests in `core` run it against the object-based search permutation by permutation (count, load volume, rotations), and the packager with
an accelerator against the packager without.

`FlatEngine` splits the permutations in items by their first boxes, searches the items on threads, and reduces the winners of the items in the order of the
iterator: the most load volume, then load weight, then boxes; the first permutation wins a tie, and the first permutation which places all boxes wins outright.
A permutation which cannot be better than an earlier one need not be searched, which is how the packager and the engine skip permutations.

Limits:

 * A search which needs more free points than `withMaxPoints(..)` (default 128) makes the engine give up; the packager then searches itself. The most free
   points seen on the test inputs is 54 (see below), for 10 to 14 boxes.
 * The interrupt is polled between searches of single permutations, so it reacts within the time of one permutation, which can be many milliseconds for
   hard inputs. A box order is never interrupted halfway.
 * With one thread the engine runs on the calling thread.

## Speed

The flat search does the same work per node as the object search: on a single thread it is about as fast (0.98 to 1.09 times), because the extreme-point
bookkeeping dominates and the objects are already cheap. A speed-up comes from running many items at once.

Measured on the engine's reference inputs (10 to 14 boxes, a Ryzen 9 9950X; `FlatStatsReport` in the tests prints these numbers):

 * hard inputs cost 200,000 to 800,000 nodes per permutation, at about 270 to 330 ns per node, evenly spread (the slowest 1% of searches take 3 to 6% of the time)
 * easy inputs finish in a few hundred nodes, with a heavy tail: the slowest 1% of searches can take more than half of the time (up to 80% in the measurements)

On the CPU `ParallelBruteForcePackager` already runs the permutations in parallel, so the engine is not a replacement for it; it is the exact reference for
a device. A device has to beat all the threads of the CPU, which for this branchy search (a depth-first search with data-dependent control flow, a lot of
scratch memory per work item) is not a given. The honest expectation for a recent GPU is between 0.5 and 2 times the speed of 32 CPU threads, and the point of
the SPI is that this can be measured without changing the packager.

## Running on a GPU with TornadoVM

The module [`accelerator/tornado`](../../accelerator/tornado/README.md) (profile `-Ptornado`, Java 22+) is an experimental [TornadoVM](https://www.tornadovm.org/)
accelerator: `TornadoBruteForceAccelerator` chooses the device (by default the OpenCL GPU with the most compute throughput) and runs the search of the items on it, one work
item per item, with the host loop of `FlatEngine` around it. The kernel is a hand-written OpenCL C port of the flat package, run as a TornadoVM prebuilt task: the
TornadoVM 7.3.1 JIT compiles the search from the Java but miscompiles its control flow (the kernel hangs), see the module README. The winner of the device is checked
against `FlatEngine` on random problems by `KernelParityTest` (skipped without a device), and the packager with the accelerator against the packager without.

```java
TornadoBruteForceAccelerator accelerator = TornadoBruteForceAccelerator.newBuilder().build();
FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withAccelerator(accelerator).build();
```

It needs the TornadoVM SDK with an OpenCL, CUDA or Metal backend for the device (AMD cards use OpenCL), which is not on Maven Central; without a runtime the accelerator gives up and
the packager searches itself. The module README shows how to run with the SDK.

On a Radeon RX 7600 against 8 CPU threads (4 cores of a Ryzen 9 9950X), random problems of 11 to 12 boxes: the device is 2 to 8 times faster on most hard problems (5 to 126 million
nodes), and 1.8 times slower on one; that is roughly 0.5 to 2 times the whole CPU with 32 threads, as expected above. On easy problems the compilation of the kernel (about 400 ms,
paid once per packing) dominates. The numbers and the tuning are in the module README.

To write another kernel, port the flat package as the tornado module does and keep both in step, see [DEVELOPER.md](../../DEVELOPER.md#flat-search-and-accelerators).

## Tests

 * `FlatPointsAddTest`, `FlatBestPointTest`, `FlatSearchPermutationTest` and `FlatPermutationTest`: the flat model against the object model
 * `FlatEngineTest`: the engine against a reference loop, with threads, bounds, a tiny point limit, and interrupts
 * `FlatAcceleratedPackagerTest`: placements with and without the accelerator are identical, fallbacks, interrupts, and the inputs which are not accelerated
 * `IteratorSetPermutationTest`: moving the iterator to a permutation
