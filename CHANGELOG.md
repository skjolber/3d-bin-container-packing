# Changelog

 * 5.2.0:
     * Brute force tree search, on by default. Returns any arrangement of the best value; `withTreeSearchAnyBest(false)` keeps the 5.1 result.
     * New API: `FastBruteForcePackager.withThreads(..)`.
     * Performance: Brute force packagers 2-3x faster, far more where the first box orders do not place all boxes.
     * Accelerators (experimental): new `accelerator-core` and `tornado-accelerator` modules run the brute force search on threads or a GPU.
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
