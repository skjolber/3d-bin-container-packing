# Parallel brute-force packager

`ParallelBruteForcePackager` is a parallel version of the brute-force packager, for those wishing to use it on a multi-core system.

## Algorithm

It runs the same search as [`BruteForcePackager`](brute-force.md#algorithm), split into tasks on several threads. With the [tree search](brute-force.md#tree-search) (the default), the box orders of a container are split into units by their first boxes (the shortest prefixes of box types which number at least a quarter of the parallelization count), which the threads take in turn; each unit is searched as a tree restricted to the orders with its prefix, on the thread's own point calculator. The units share their best arrangement for pruning, the units after a unit with a complete result stop, and the units' results compare in the order of the orders, so the result is exactly the single-threaded one. With the loop (`withTreeSearch(false)`, or reverse-filtered permutations), the permutations of the boxes are split into different tasks, and for box item groups without a box item order, the orders of the groups (by their first groups). The rotations and point placements of a permutation are not split.

The parallel packager searches a box item order, or box items with container priorities, on one thread, as the permutations
cannot be split between threads. For box item groups without a box item order, it splits the orders of the groups between
its threads (or, for a few groups with many boxes, the permutations of each order), with the same result as on one thread.

## Options

 * `withThreads(..)` and `withAvailableProcessors(..)`: the number of threads (the latter as the available processors divided by a factor).
 * `withParallelizationCount(..)`: the number of units to split the work into, which should be an order of magnitude larger than the number of threads.
 * `withExecutorService(..)`: run the tasks on an executor service of your own.
 * `withPointFilter(..)`, `withSkipReversePermutations(..)`, `withRequireFullSupport(..)`, `withTreeSearch(..)`, `withTreeSearchMinBoxes(..)`: as for the [brute-force packager](brute-force.md#variants).

## Thread priority

To leave CPU capacity to other work, search at a lower thread priority: `ParallelBruteForcePackager.newBuilder().withThreadPriority(Thread.MIN_PRIORITY)`. The priority is a hint to the operating system's scheduler. Each packing task sets it on the thread which runs it (a pool thread of an executor service passed in with `withExecutorService(..)`, or the calling thread) and restores the thread's own priority afterwards; an executor service created by the builder also creates its threads at the priority. By default, the thread priority is left alone.

## Complexity and limits

The complexity is that of the [brute-force packager](brute-force.md#complexity-and-limits): exponential, so use a deadline.
For box item groups it is also exponential in the number of groups. Benchmark representative inputs before choosing
parallelism; task setup and candidate-selection overhead can outweigh parallel work on small inputs (see
[FEATURES.md](../../FEATURES.md#choosing-an-approach)).

## Tests and benchmarks

 * `ParallelBruteForcePackagerTest`, `ParallelBruteForcePackagerThreadPriorityTest`, `ParallelBruteForcePackagerWorkUnitsTest`, `ParallelGroupOrderSplitTest` and `ParallelBruteForceTreeSearchTest` in `core`, and the `ParallelBoxItem*PermutationRotationIterator*Test` classes in package `iterator`.
 * `ParallelBruteForceEquivalenceIT` in `core` (package `packer.bruteforce.reference`): the parallel search against the 4.x brute force on two and four threads, run with the `slow-tests` profile.
 * `ParallelIteratorBenchmark` in the `jmh` module, see [jmh/README.md](../../jmh/README.md).
