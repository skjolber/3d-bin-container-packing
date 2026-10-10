# Plain packager

`PlainPackager` is a simple packager which fills each container by repeatedly selecting a box and placing it at a free point.

## Algorithm

This algorithm selects the box with the biggest volume, fitting it where it is best supported.

## Ranking

By default, the box with the highest volume, then the highest weight, is placed first, at the position with the lowest
area, then the lowest z. Support only takes part in the ranking when it is calculated: with `withCalculateSupport(true)`,
better supported positions rank first, and with `withRequireFullSupport(true)` boxes are only placed where they have full
support (see [support](../support.md)).

Each part can be replaced:

 * the box item comparator and the placement ranking, with `withPlacementControlsBuilderFactory(..)` on the builder (see [packager controls](../packager-controls.md));
 * the order of box item groups when there is no box item order, with `withBoxItemGroupComparator(..)` (by default by volume, then weight; see [deliveries](../deliveries.md) for how container priority and extraction order come first);
 * the comparison of the results of different containers, with `withIntermediatePackagerResultComparator(..)`.

## Variants

None. The LAFF packagers use the same placement controls, but fill the container level by level: see
[Largest Area Fit First](largest-area-fit-first.md).

## Limits

The plain packager is greedy, so the result depends on the ranking. Use it, or a LAFF packager, for normal,
latency-sensitive requests, and a [brute-force](brute-force.md) packager for a small number of boxes (see
[FEATURES.md](../../FEATURES.md#choosing-an-approach)). Which features it supports is listed in
[FEATURES.md](../../FEATURES.md#feature-support).

## Tests and benchmarks

 * `PlainPackagerTest` and the `PlainPackager*ConstraintTest` classes in `core`.
 * `PlainHeuristicStudyIT` in `core`: a measurement study of the placement ranking on random orders, against a
   comparator which emulates that of 4.x and against 4.x itself (never runs by default; run with `-Dtest=PlainHeuristicStudyIT`).
 * `EgyPackagerBenchmark` and `TychoBenchmark` in the `jmh` module, and [jmh/PERFORMANCE.md](../../jmh/PERFORMANCE.md) for 5.0 against 4.2.
