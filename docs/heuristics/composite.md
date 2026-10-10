# Composite packager

`CompositePackager` combines other packagers, using costly packagers only where cheaper packagers fall short.

## Algorithm

The first packager (or those added with `withBaselinePackager(..)`) first packs the whole order, giving a
baseline result. Then, for each container the container packing strategy attempts, the packagers are tried in order
until one fits all remaining boxes; a costlier packager only needs to beat the cheaper packagers' result.
The better result is returned (see `PackagerResultComparator`), and the baseline if the deadline passes.
A container packing strategy set with `withContainerPackingStrategyFactory(..)` applies to the baseline too.

```
baseline:     plain ────────────────────────────────▶ result A
improvement:  container 1: plain ✔ (all boxes)
              container 2: plain ✘ ──▶ brute force ✔
              ...                                    ▶ result B
return the better of A and B
```

A costly packager therefore runs only where the cheaper packagers did not pack all remaining boxes. Without container
costs, the improvement uses at most as many containers as the baseline (if the result comparator prefers fewer containers).

## Budgets

Each packager after the first can have a budget, in milliseconds: the time it may run for, counted from the start of the
improvement. Once the budget is used up, the packager is not used any more. The first packager which supports the input
has no budget, as its results bound the other packagers. A packager which rejects the input is skipped
(`getUnsupportedReason(..)`, see [FEATURES.md](../../FEATURES.md#feature-support)).

```java
CompositePackager packager = CompositePackager.newBuilder()
    .withPackager(PlainPackager.newBuilder().build())                // tried first, for every container
    .withPackager(FastBruteForcePackager.newBuilder().build(), 200)  // only where plain does not fit all boxes, for at most 200 ms
    .build();
```

## Quality

For random orders in the shipping containers of issue #1158, a plain and fast brute force composite (200 ms budget)
packed every order, with 2-6 % less container volume than the plain packager, at 10-60 ms per order; brute force alone
ran out of time for many of the orders. See `CompositeQualityReport` in the `jmh` module.

## Customization

 * `withIntermediatePackagerResultComparator(..)` selects the best result for each container, and `withPackagerResultComparator(..)` the better of the baseline and the improvement.
 * Custom packagers take part if they implement the session contract, see [Packagers in a composite](../../DEVELOPER.md#packagers-in-a-composite) in DEVELOPER.md.

## Tests and benchmarks

 * `CompositePackagerTest` in `core`; `PackagerConformanceTest` includes a composite with a plain stage and a fast brute-force stage.
 * `CompositeQualityReport` in the `jmh` module: quality and time of the packagers and of composite packagers on random orders in the shipping containers of issue #1158. Run with `java -cp jmh/target/benchmark.jar com.github.skjolber.packing.jmh.composite.CompositeQualityReport [orders] [deadline ms]`.
