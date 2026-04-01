# Core Module

## Purpose
The main algorithmic engine. Implements all packager strategies (LAFF, brute-force, plain), box permutation/rotation iterators, deadline management for time-bounded packing, and placement validation.

## Key Packages
- `com.github.skjolber.packing.packer.laff` — Largest Area Fit First: `LargestAreaFitFirstPackager`, `FastLargestAreaFitFirstPackager`
- `com.github.skjolber.packing.packer.bruteforce` — `BruteForcePackager`, `FastBruteForcePackager`, `ParallelBruteForcePackager`
- `com.github.skjolber.packing.packer.plain` — `PlainPackager` (simple greedy)
- `com.github.skjolber.packing.iterator` — `BoxItemPermutationRotationIterator`, `BoxItemGroupPermutationRotationIterator`, `FilteredBoxItemsPermutationRotationIterator`
- `com.github.skjolber.packing.deadline` — `DeadlineCheckPackagerInterruptSupplier`, `PackagerInterruptSupplierBuilder`
- `com.github.skjolber.packing.comparator` — Result comparators for selecting the best packing
- `com.github.skjolber.packing.validator` — Runtime placement correctness checks

## Architecture Notes
- All packagers implement `Packager<B>` from **api**.
- Depends on **points** for free-space tracking during placement.
- Strategy pattern: swap packager implementations at construction time; the calling code interacts only via the `Packager` interface.
- Deadline/interrupt pattern: callers supply a `Supplier<Boolean>` that the packager polls; return `true` to abort early.
- `ParallelBruteForcePackager` uses a `ForkJoinPool`; avoid shared mutable state in iterators.
- `Fast*` variants trade flexibility for reduced allocation and faster iteration.

## Testing
- JUnit 5, AssertJ, jQwik, junit-quickcheck
- Property-based tests verify packing correctness across random inputs
- Run: `mvn test -pl core`

## Dependencies
| Scope   | Artifact |
|---------|----------|
| compile | api, points |
| test    | test module, junit-jupiter, assertj-core, jqwik, junit-quickcheck |

## Build
```
mvn test -pl core
```
