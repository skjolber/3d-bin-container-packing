# Points Module

## Purpose
Manages free-space bookkeeping during packing. Tracks 2D and 3D points that represent candidate placement locations within a container. Provides point calculators that maintain and update the set of available positions as boxes are placed.

## Key Packages
- `com.github.skjolber.packing.ep.points2d` — 2D point types: `Point2D`, `DefaultPoint2D`, `XSupportPoint2D`, `YSupportPoint2D`, `Point2DList`, `Point2DFlagList`
- `com.github.skjolber.packing.ep.points3d` — 3D points: `SimplePoint3D` with a single final implementation `DefaultPoint3D`, whose optional xy/xz/yz plane placements record supporting surfaces; calculators `DefaultPointCalculator3D`, `MarkResetPointCalculator3D`
- `com.github.skjolber.packing.ep` — `PlacementList`, shared with core

## Architecture Notes
- Depends only on **api**; no dependency on **core**.
- Uses **Eclipse Collections** for performance-optimized list operations — prefer these over standard `java.util` collections.
- `Point2DFlagList` uses bitmask flags to avoid object allocation in hot paths.
- The supporting planes of a 3D point enable smarter candidate filtering.

## Performance
- Keep `DefaultPoint3D` the only `SimplePoint3D` implementation: calls on points in the calculator loops must stay monomorphic to be inlined.
- `DefaultPointCalculator3D.add` is split into `classify`, `moveX/Y/Z` and `merge`; changes that grow these hot methods have repeatedly measured slower even when they removed work. Measure every change.
- Behavior is pinned by golden masters (`PointCalculatorGoldenMasterTest` here, `PackagerGoldenMasterTest` in core): optimizations must keep their checksums. Benchmarks: `jmh` module, `PointsBenchmark3D` (including `points3DRecorded`, a large-order replay), `TychoBenchmark`, `EgyPackagerBenchmark`, `BouwkampCodeBruteForcePackagerBenchmark`.

## Testing
- JUnit 5, AssertJ, jQwik (property-based tests verify point calculator invariants)

## Dependencies
| Scope   | Artifact |
|---------|----------|
| compile | api, eclipse-collections |
| test    | test module, junit-jupiter, assertj-core, jqwik |
