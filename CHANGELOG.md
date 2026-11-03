# Changelog

 * 5.2.0:
     * Brute force: the box orders of a container are searched as one tree instead of one order at a time (`withTreeSearch(..)`, on by default from 6 boxes, `withTreeSearchMinBoxes(..)`), with the loop's result: each prefix of placements is placed once, the orders are settled per prefix, and a node is pruned by the best sub-multiset of the remaining boxes within the load left and by its free space (maximal empty spaces). See [tree search](docs/heuristics/brute-force.md#tree-search).
     * Brute force: `withTreeSearchAnyBest(..)`, on by default: any arrangement of the best value (load volume, weight, boxes) is returned, and the search ends at the first arrangement which places all boxes. Behaviour change: among equally good arrangements the one returned can differ from 5.1; `withTreeSearchAnyBest(false)` gives exactly the arrangement of 5.1.
     * Brute force: `FastBruteForcePackager.withThreads(..)` and `withExecutorService(..)` search a container with many orders on threads; `ParallelBruteForcePackager` searches a container as one tree on all its threads, with work stealing (`withTreeSearchStealing(..)`, on by default).
     * Brute force: box load limits are restored exactly when a placement is removed, so a box at a load limit is accepted the same on every search path.
     * Performance: brute force on problems whose boxes do not all fit, 8 boxes: `FastBruteForcePackager` 264 ms to 75 ms, `BruteForcePackager` 5.7 s to 2.3 s, `ParallelBruteForcePackager` on 4 cores 0.6 s. Squared squares (Bouwkamp): `BruteForcePackager` 0.27 to 136 ops/s, `FastBruteForcePackager` 42 to 764 ops/s. `ParallelBruteForcePackager` on small orders 5.8x, box item groups up to 30%. See [jmh/PERFORMANCE.md](jmh/PERFORMANCE.md).
     * New artifact `accelerator-core` (module `com.github.skjolber.packing.accelerator`): `AcceleratedFastBruteForcePackager` and `AcceleratedBruteForcePackager` can hand the search of the box orders of a container to a `BruteForceAccelerator`: `FlatEngine` on CPU threads, or a device. The accelerator only picks the best order; the packager places and confirms it, so the placements are the same as without. Modes `CPU_ONLY`, `GPU_ONLY` and `AUTO` (the default: the packager and the accelerator race, the accelerator joining after an adaptive gate). The search itself is also available on primitive arrays (`FlatPoints`, `FlatSearch`, `FlatTree`, `FlatBound`, `FlatSpace`, ..). See [accelerated brute force](docs/heuristics/accelerated-brute-force.md).
     * New artifact `tornado-accelerator` (experimental; Java 22+ and a [TornadoVM](https://www.tornadovm.org/) 8.0.0 SDK at runtime): `TornadoBruteForceAccelerator` runs the search as an OpenCL kernel on a GPU. Measured on a Radeon RX 7600: `BruteForcePackager` on 12 problems of 10 to 11 boxes 200 s to 2.5 s (the hardest 198 s to 1.7 s); `FastBruteForcePackager` on one thread 5.6 s to 1.0 s (11 to 12 boxes) and 18.3 s to 3.0 s (15 to 16 boxes); with 8 CPU threads the device does not pay. See the [module README](accelerator/tornado/README.md).
     * Benchmarks: `TightBruteForceBenchmark` (random problems whose boxes do not all fit) in the `jmh` module.
     * Documentation: the version history moved from `README.md` to `CHANGELOG.md`.
 * 5.1.0:
     * New API: `PointCalculator.addLast(..)` places the final box without updating the free points.
     * Performance: Packagers are up to 6% faster, validation up to 16%, and point calculation up to 17%.
 * 5.0.0: Major release. Breaking changes. A ton of new features. Assisted by AI. 
     * Box load constraints: 
        * max load weight
        * pressure
        * box count
        * identical boxes only
     * Support calculation + full support for plain and LAFF packagers; full support for the brute-force packagers
     * Container costs and container packing strategies (ordered, parallel, allocation, brute force), and custom container packing strategies
     * Deliveries: 
        * extraction order (for example the stops of a route) 
        * container priority (for example urgent boxes in the first containers) of box items and groups
     * Visualizer: result summaries and comparison of several results, validation reasons on the boxes, colour modes for groups, support, load and extraction order, and the centre of gravity
     * Composite packager: Cheap packagers first, costly packagers only where needed
     * Virtual box: Combine multiple boxes into one to simplify the packaging.
     * Substantially faster point calculation, placement search, support calculation and load validation (see [jmh/PERFORMANCE.md](jmh/PERFORMANCE.md))
     * The 4.x implementation is [retained for testing](legacy/v4/README.md): 5.0 is verified against it with equivalence and golden-master tests and 10,000-order comparisons
     * Breaking changes and behaviour changes against 4.x: see [legacy/MIGRATION-v4-TO-v5.md](legacy/MIGRATION-v4-TO-v5.md)
 * 4.2.1: `Placement` can now be added anywhere within a `Point` (not only at the point origin).
 * 4.2.0: Obstacles.
 * 4.1.x: Validator.
 * 4.0.x: Major rewrite. Breaking changes.
     * Support for packaging groups
     * Various ways to control packaging:
        * Manifest controls (box vs box, box vs container)
        * Point controls (points per box)
        * Placement controls (select best box + point)
     * Minor visualizer improvements
 * 3.0.11: Use `BigInteger` to sanity-check max volume / max weight, calculate real remaining max volume.
 * 3.0.10: Fix module info, bump dependencies.
 * 3.0.9: Fix point support bug which resulted in invalid packaging result
 * 3.0.8: Visualization fix
 * 3.0.4-3.0.6: Fix issue #689
 * 3.0.3: Fix module info
 * 3.0.2: Make Plain Packager prefer low z coordinate over supported area.
 * 3.0.1: Various performance improvements.
 * 3.0.0: Support max number of containers (i.e. per container type). Use builders from now on. Various optimizations.
 * 2.1.4: Fix issue #574
 * 2.1.3: Fix null-pointer
 * 2.1.2: Tidy up, i.e. remove warnings, nuke some dependencies.
 * 2.1.1: Improve free space calculation performance
 * 2.1.0: Improve brute force iterators, respect deadlines in brute for packagers.
