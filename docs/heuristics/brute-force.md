# Brute-force packagers

`BruteForcePackager` and `FastBruteForcePackager` search the box orders and rotations of each container, which is feasible for a low number of boxes.

## Algorithm

This algorithm has no logic for selecting the best box or rotation; running through all permutations, for each permutation all rotations:

 * `BruteForcePackager` attempts all box orders, rotations and placement positions.
 * `FastBruteForcePackager` attempts all box orders and rotations, placing each box at the best free point for its rotation.

The brute-force packagers use one search for every order of the boxes: each box in the order is tried in each rotation,
at each free point (the fast brute-force packager tries each rotation at its best point only), and a container gets the
best arrangement by the result comparator. Without a box item order (`Order.NONE`), every permutation of the boxes is
searched this way; with a box item order (`Order.CHRONOLOGICAL`), only that order, each box insertable after the boxes
before it. With skipping (`Order.CHRONOLOGICAL_ALLOW_SKIPPING`), each box (or box item group) can also be skipped, and
waits for a later container. With container priorities, they only permute the boxes within each priority.

For box item groups without a box item order, the packagers also search the orders of the groups, see
[deliveries](../deliveries.md#packager-support) and [FEATURES.md](../../FEATURES.md#packing-algorithms).

## Pruning

The algorithm tries to skip combinations which will obviously not yield a (better) result:

 * permutations
   * two or more boxes have the same dimensions
   * permutations which mutated at a previously unreachable index
 * fewer rotations
   * two or more sides have the same length
   * rotations which mutated at a previously unreachable index

`BruteForcePackager` and `ParallelBruteForcePackager` can also skip reverse-equivalent permutations
(`withSkipReversePermutations(..)`); see [FEATURES.md](../../FEATURES.md#packing-algorithms) for when the skip applies.

## Variants

 * `BruteForcePackager`: all box orders, rotations and placement positions. A `BruteForcePointIteratorFilter` can rank fitting points and use a different point limit at each placement step. Set it with `withPointFilter(..)`.
 * `FastBruteForcePackager`: each rotation at its best point only. The point is chosen by a `FastBruteForceBoxStackValuePointComparator` (`withPointComparator(..)`).
 * `ParallelBruteForcePackager`: the same search on several threads, see [parallel brute-force](parallel-brute-force.md).

All of them can require full support (`withRequireFullSupport(true)`, see [support](../support.md)), enforce box load limits, and reject manifest and point controls (see [FEATURES.md](../../FEATURES.md#constraints-and-controls)).

## Complexity and limits

The complexity of this approach is [exponential](https://en.wikipedia.org/wiki/Exponential_function), and thus there is a limit to the feasible number of boxes which can be packaged within a reasonable time. However, for real-life applications,  a healthy part of for example online shopping orders are within its grasp.

The worst case complexity can be estimated using the relevant iterators before packaging is attempted: the permutation and rotation iterators in `com.github.skjolber.packing.iterator` count them (`countPermutations()`, `countRotations()`).

Do not attempt this with many boxes of different sizes: the number of combinations grows exponentially, so it will likely not complete in time. The search itself is not recursive, so many identical boxes do not exhaust the thread stack.

Use a deadline whenever brute-forcing in a real-time application, with `withInterruptDuration(..)` or `withInterruptDeadline(..)` on the result builder.

## Tests and benchmarks

 * `BruteForcePackagerTest`, `FastBruteForcePackagerTest`, `BruteForceReversePermutationsTest`, `BruteForceInterruptTest` and `BruteForceBoxItemGroupsTest` in `core`, and the permutation and rotation iterator tests in `core` (package `iterator`).
 * The differential tests in `core` (package `packer.bruteforce.reference`) compare the searches and iterators with the 4.x brute force, see [legacy/v4/README.md](../../legacy/v4/README.md).
 * `EgyPackagerBenchmark` (small orders), `TychoBenchmark`, `BouwkampCodeBruteForcePackagerBenchmark`, `GroupBruteForceBenchmark` (box item groups) and `DeadlineBenchmark` in the `jmh` module; see [jmh/README.md](../../jmh/README.md) and [jmh/PERFORMANCE.md](../../jmh/PERFORMANCE.md).
