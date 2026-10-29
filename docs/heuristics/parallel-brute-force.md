# Parallel brute-force packager

`ParallelBruteForcePackager` is a parallel version of the brute-force packager, for those wishing to use it on a multi-core system.

## Algorithm

It runs the same search as [`BruteForcePackager`](brute-force.md#algorithm) on several threads. By default (`withTreeSearchStealing(true)`) a container's orders are searched as one [tree](brute-force.md#tree-search) on all the threads, with work stealing: the first thread searches the whole tree, and a thread without work steals the shallowest subtree another thread has still to search (the types it has still to try at a node within the first six levels), as a part with that node's prefix of placements, which it replays on its own point calculator. The tree's order of search is kept, so the first boxes tried last are pruned with what was found under the first ones, and the threads stay busy. The threads share their best arrangement for pruning, a complete result stops the others, and the results compare by the result comparator, then by their orders: the result is the single-threaded one, except that among several complete arrangements the threads' timing decides, and among equal arrangements of the same order the first found. Containers with few permutations are searched on one thread, with the tree, as are, with `withTreeSearchAnyBest(true)`, containers whose boxes all fit: the tree then tries the largest boxes first and ends at the first complete arrangement, which one thread finds sooner than several.

With `withTreeSearchStealing(false)`, the permutations of the boxes are split into different tasks (the loop), and for box item groups without a box item order, the orders of the groups (by their first groups). The rotations and point placements of a permutation are not split. With `withTreeSearchUnits(true)` instead, the box orders are split into units by their first boxes, each searched as a tree restricted to its prefix; it is off by default, since units searching the first boxes side by side cannot prune the later first boxes with what the tree finds under the earlier ones.

The parallel packager searches a box item order, or box items with container priorities, on one thread, as the permutations
cannot be split between threads. For box item groups without a box item order, it splits the orders of the groups between
its threads (or, for a few groups with many boxes, the permutations of each order), with the same result as on one thread.

## Options

 * `withThreads(..)` and `withAvailableProcessors(..)`: the number of threads (the latter as the available processors divided by a factor).
 * `withParallelizationCount(..)`: the number of units to split the work into, which should be an order of magnitude larger than the number of threads.
 * `withExecutorService(..)`: run the tasks on an executor service of your own.
 * `withPointFilter(..)`, `withSkipReversePermutations(..)`, `withRequireFullSupport(..)`, `withTreeSearch(..)`, `withTreeSearchMinBoxes(..)`: as for the [brute-force packager](brute-force.md#variants); `withTreeSearchStealing(..)` and `withTreeSearchUnits(..)`: see above.

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
