# Core Module

## Purpose
The main algorithmic engine. Implements all packager strategies (LAFF, brute-force, plain), box permutation/rotation iterators, placement controls (including load constraints and support) and container packing strategies.

## Key Packages
- `com.github.skjolber.packing.packer.laff` — Largest Area Fit First: `LargestAreaFitFirstPackager`, `FastLargestAreaFitFirstPackager`
- `com.github.skjolber.packing.packer.bruteforce` — `BruteForcePackager`, `FastBruteForcePackager`, `ParallelBruteForcePackager`
- `com.github.skjolber.packing.packer.plain` — `PlainPackager` (simple greedy)
- `com.github.skjolber.packing.iterator` — `BoxItemPermutationRotationIterator`, `BoxItemGroupPermutationRotationIterator`, `FilteredBoxItemsPermutationRotationIterator`
- `com.github.skjolber.packing.comparator` — Result comparator implementations; `comparator.placement` — placement comparator implementations (`DefaultPlacementComparatorFactory`). The comparator interfaces are in **api**, so users can supply their own decision-making.
- `com.github.skjolber.packing.packer.strategy` — Container packing strategy implementations (ordered, parallel, allocation, cost) and `DefaultContainerPackingStrategyFactory`. The strategy interfaces (`ContainerPackingStrategy`, `PackagerSession`, `ContainerInventory`) are in **api**; packager builders take a factory via `withContainerPackingStrategyFactory(..)`.
- `com.github.skjolber.packing.packer.composite` — `CompositePackager`: baseline from cheap packagers, then per-container escalation to costlier packagers through `CompositePackagerSession` (sessions of several packagers kept in sync by accepting each result in all of them)
- `com.github.skjolber.packing.packer.util` — Load-constraint utilities used by the load-aware placement controls
- `com.github.skjolber.packing.virtualbox` — Virtual-box preprocessing
- Interrupts and deadlines live in **api** (`com.github.skjolber.packing.api.interrupt`).

## Architecture Notes
- All packagers implement `Packager<B>` from **api**.
- Depends on **points** for free-space tracking during placement.
- Strategy pattern: swap packager implementations at construction time; the calling code interacts only via the `Packager` interface.
- Deadline/interrupt pattern: callers supply a `Supplier<Boolean>` that the packager polls; return `true` to abort early.
- `ParallelBruteForcePackager` uses a `ForkJoinPool`; avoid shared mutable state in iterators.
- `Fast*` variants trade flexibility for reduced allocation and faster iteration.

## Modules
- The module descriptor is `src/main/java9/module-info.java` (added by Moditect when packaging). Export every package with public classes; `ModuleDescriptorTest` in **jmh** checks this.

## Testing
- JUnit 5, AssertJ, jQwik, junit-quickcheck
- Property-based tests verify packing correctness across random inputs

## Dependencies
| Scope   | Artifact |
|---------|----------|
| compile | api, points |
| test    | test module, validators, junit-jupiter, assertj-core, jqwik, junit-quickcheck |
