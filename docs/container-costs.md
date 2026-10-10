# Container costs

A container item can carry a cost calculator, which prices each container the packagers use; with costs, the packagers choose the cheapest combination of containers they find, instead of going by the order of the container items.

```java
Container small = Container.newBuilder().withId("small").withSize(10, 10, 10).withMaxLoadWeight(1000).build();
Container large = Container.newBuilder().withId("large").withSize(20, 10, 10).withMaxLoadWeight(1000).build();

List<ContainerItem> containers = ContainerItem.newListBuilder()
    .withContainer(small, 2, new FixedContainerCostCalculator(100, small.getMaxLoadVolume(), "small", 0))
    .withContainer(large, 1, new FixedContainerCostCalculator(250, large.getMaxLoadVolume(), "large", 0))
    .build();

// three boxes of 5 x 10 x 10 fit in two small containers (cost 200) or in one large container (cost 250)
PackagerResult result = packager
    .newResultBuilder()
    .withContainerItems(containers)
    .withBoxItems(boxItems)
    .withMaxContainerCount(2)
    .build();

result.getCost(); // 200: two small containers
```

Without the calculators, the same order is packed into the large container alone, as the packagers then prefer one container which holds all the boxes (see [container packing strategies](container-packing-strategies.md)).

## Attaching a calculator

The cost belongs to a container item (a container type and its count), not to the `Container`, so two items of the same container can be priced differently. Any of these attach it:

 * `ContainerItem.newListBuilder().withContainer(container, count, costCalculator)`
 * `new ContainerItem(container, count, costCalculator)`, or `setCostCalculator(..)` on an existing item
 * `withContainerItem(b -> b.withContainerItem(container, count).withCostCalculator(costCalculator))` on the result builder

Either every container item has a calculator or none: the default strategy factory throws an `IllegalArgumentException` for a mix when packing starts. To price only some container types, give the others a zero-cost calculator, for example `new FixedContainerCostCalculator(0, volume, id, 0)`.

## Calculators

`ContainerCostCalculator` (`com.github.skjolber.packing.api.cost`) prices one use of one container.

 * `calculateCost(weight)` is the cost of a container loaded with the given weight: the total weight of the boxes in it, without the container's own weight (`Container.getLoadWeight()`). Costs are whole numbers in units of your choice, and must not be negative.
 * `getMinimumCost()` is a lower bound on `calculateCost(..)` for any load the calculator supports. The strategies which search for a minimum use it as a bound, so it must not exceed the actual cost.
 * `getMaximumCost()`, `getFixedCost()`, `getCostPerVolume(weight)`, `getCostPerWeight(weight)` and `getId()` are for the caller: the built-in strategies only call `calculateCost(..)` and `getMinimumCost()`.

The calculators of `com.github.skjolber.packing.cost` in `core` (all take the volume of the container, for the per-volume ratios, an optional id and a fixed cost which is added to every price):

| Calculator | Cost |
| --- | --- |
| `FixedContainerCostCalculator` | The same for every load. |
| `BucketContainerCostCalculator` | By load weight ranges: `withBucket(minWeight, maxWeightExclusive, cost)`, contiguous and ascending. Loads below the first range cost as the first range. A load at or above the end of the last range throws an `IllegalArgumentException`. |
| `LinearBucketWeightContainerCostCalculator` | A minimum cost up to a minimum weight, then a fixed cost for every started weight step. A load above the maximum weight throws an `IllegalArgumentException`. |
| `VolumeWeightBucketContainerCostCalculator` | Like the bucket calculator, with a volumetric weight floor: a load is priced at least as the weight `volume / weightToVolumeRatio`, (the volumetric weight which carriers charge for light, bulky parcels). |

```java
ContainerCostCalculator calculator = BucketContainerCostCalculator.newBuilder()
    .withVolume(container.getMaxLoadVolume())
    .withBucket(0, 1_000, 10_000)     // loads below 1 000
    .withBucket(1_000, 2_000, 12_500) // loads from 1 000 to 1 999
    .withFixedCost(250)               // added to both
    .build();                         // calculateCost(500) is 10 250
```

The buckets must cover every load weight the container can carry: a load outside them fails the packing with the exception. Implement `ContainerCostCalculator` yourself for any other rate card. Copies of a container item share its calculator, and packaging operations run concurrently, so use a stateless one.

## What costs change

 * **The default container packing strategy.** When all container items have a calculator, the default is the cost-aware `LowestCostContainerPackingStrategy`; otherwise it is the ordered strategy. Each round, the cost-aware strategy packs the remaining boxes into every container type that can hold some of them, prices each result, plans the cheapest set of containers which would cover the remaining box count, volume and weight if every container of a type loaded what its observed packing did (within the container counts and the container limit), accepts one container of that plan, and plans again. It is a heuristic, which does not prove the globally cheapest packing. See [container packing strategies](container-packing-strategies.md) for it, and for the strategies which search for the lowest cost in other ways.
 * **The reported cost.** `PackagerResult.getCost()` is the sum of the prices of the containers in the result, with every strategy: also when a strategy ignores costs when choosing. It is 0 without costs, and -1 when there is no result (the boxes did not fit, or the packager was interrupted).
 * **Comparing results.** `CompositePackager` picks the better of its baseline results and its improvement with `DefaultPackagerResultComparator` (replace it with `withPackagerResultComparator(..)`): a successful result, then the lower cost, then fewer containers, then less total container volume. Without costs, the improvement is limited to the baseline's container count when the comparator prefers fewer containers; with costs it is not, as more containers can cost less. See [composite packager](heuristics/composite.md).
 * **Virtual-box preprocessing.** [VirtualBoxPackager](heuristics/virtual-box.md) splits counts which do not form a grid into container-sized blocks, and prefers blocks which fit more container types, so that cheaper containers remain usable. It compares its attempts by cost, then container count, then container volume. After a successful packing it refines (splits virtual boxes) when a cheaper result is possible: for a single container, when some container type has a minimum cost below the result's cost; for several containers, always (up to the refinement limit), as there is no container count at which refinement cannot help.

With the default limit of one container, costs pick the cheapest container type which holds all the boxes.

Costs do not change how boxes are packed in a container: the packagers rank the results of the containers they try by their `IntermediatePackagerResultComparator`, which by default prefers the larger load volume. Costs are not a constraint either: there is no budget, only the choice between combinations.

## Count-based or cost-based selection

 * **Without costs**, list the container items in order of preference (for example the smallest first) and set `withMaxContainerCount(..)`. When more than one container may be used, the default strategy takes the most preferred container which holds all the boxes; when none does, it packs the boxes into each candidate, keeps the best result (by default the largest load volume), and repeats. With the default limit of one container, the order does not decide: the smallest container which holds all the boxes wins. The order is the only knob: the strategy does not know that two small containers can cost less than one large.
 * **With costs**, when container types are priced differently, when the price depends on the load weight (rate bands, volumetric weight), when several small containers can be cheaper than one large, or when you need the total cost in the result. The cost-aware default packs every candidate container type in every round, without the single-container search, the hints and the reuse of results of the ordered strategy, so expect more packing work; and as it is a heuristic, check the cost against the alternatives on representative orders.
 * **For small inventories** where the cheapest combination matters most, search for it: `LowestCostFitContainerPackingStrategy` plans with the fit of each box, volume and weight only, `BruteForceContainerPackingStrategy` with `LowestCostControls` packs every sequence of container types (exponential, so keep it to a few container types and a few containers; an interrupt discards the sequences found so far). Both are set with `withContainerPackingStrategyFactory(..)`.

## See also

 * [Container packing strategies](container-packing-strategies.md): which strategy is the default, and the cost-aware strategies.
 * [FEATURES.md](../FEATURES.md#container-selection-and-allocation): the feature summary.

## Tests and examples

 * `ContainerCostCalculatorTest` and `DefaultContainerInventoryTest` (cost estimates) in `core`.
 * `ContainerCostPackingTest` in `core`: the cost-aware strategies with the packagers, sessions and the result builder, with figures.
 * `DefaultContainerPackingStrategyFactoryTest` in `core`: costs switch the default strategy.
 * `ContainerCostVisualizationTest` in `visualizer/packaging`: the example above, written for the [visualizer](visualizer.md) with and without costs.
