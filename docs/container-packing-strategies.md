# Container packing strategies

A container packing strategy decides which containers a packager uses for an order, in which order, and when to stop; the packager itself only packs boxes into the one container it is asked to fill.

```java
ExecutorService executor = Executors.newFixedThreadPool(4); // yours to shut down

PlainPackager packager = PlainPackager.newBuilder()
    .withContainerPackingStrategyFactory((inventory, boxItems, boxItemGroups, comparator, emptyResult) ->
        new ParallelContainerPackingStrategy(executor, comparator))
    .build();
```

All packager builders have `withContainerPackingStrategyFactory(..)`; without it, `DefaultContainerPackingStrategyFactory` chooses (see [the default](#the-default)). `CompositePackager` applies the strategy to its baseline packagers too (see [composite packager](heuristics/composite.md)), and `VirtualBoxPackager` uses the strategy of the packager it wraps.

## What a strategy decides

`ContainerPackingStrategy.pack(interrupt, session)` repeats a round until no boxes remain: choose container types, ask the session to pack the remaining boxes into them, and accept the results it prefers. It stops with a `ContainerResult` (the accepted containers and their total cost) when all boxes are packed, and with `null` when they cannot be: no container can be used, or the container limit (`PackagerSession.getMaxContainerCount()`: the lowest of `withMaxContainerCount(..)`, which is 1 by default, the containers left, and the number of remaining boxes or groups) is reached. It works the same for box items and for box item groups. The packager turns `null` into an unsuccessful, empty result.

A strategy drives a `PackagerSession` (`com.github.skjolber.packing.api.packager.strategy`), which holds the remaining boxes and the available containers:

 * `attempt(containerIndex, best, abortOnAnyBoxTooBig)` packs as many of the remaining boxes as possible into a container of that type, without changing the session. `best` is an optional hint: a session may return an empty result instead of one which loads less volume than `best`. `abortOnAnyBoxTooBig` gives up at once if some remaining box does not fit the container type.
 * `peek(containerIndex, existing)` reuses a result for another container type without packing again, if its boxes fit inside unchanged and the container types have matching controls and initial points (see [packager controls](packager-controls.md)); `null` if not.
 * `accept(result)` removes the result's boxes from the remaining boxes, uses up a container, and adds its cost to the inventory. The result may come from another session of the same operation.
 * `fork()` is an independent session at the current state, `fresh()` one at the start of the operation, to compare alternatives without disturbing the session.
 * `getContainers()` lists the container types which can potentially hold the remaining boxes, and `getContainerInventory()` tells what fits (`canLoad(..)`, `isFeasible(..)`), the counts left, and the cost so far (`hasCost()`, `getCost()`).

A strategy checks its interrupt, and throws `PackagerInterruptedException` when it fires; the packager then returns an empty result with `isTimeout()` set. See [DEVELOPER.md](../DEVELOPER.md#writing-your-own-container-packing-strategy) for writing your own, with an example which needs the `api` module only.

## The default

`DefaultContainerPackingStrategyFactory` creates a strategy for each packaging operation:

 * if the container items have cost calculators (all of them, see [container costs](container-costs.md)): `LowestCostContainerPackingStrategy`;
 * otherwise `OrderedContainerPackingStrategy`, in its variant without the allocation feasibility check when no container could run out: there are at most as many boxes (or groups) as containers, and every available container type has at least that many containers and can load every box item (or group). Accepting a container removes at least one box, so that stays true for the rest of the operation.

The other built-in strategies are opt-in.

## Built-in strategies

| Strategy | Chooses | Default |
| --- | --- | --- |
| `OrderedContainerPackingStrategy` | The most preferred container which holds all boxes; otherwise the container with the best result. | Without costs |
| `LowestCostContainerPackingStrategy` | The cheapest plan of containers, re-planned after each container. | With costs |
| `ParallelContainerPackingStrategy` | The best result of all candidate containers, packed at the same time. | No |
| `FewestContainersFitContainerPackingStrategy` | By an allocation with the fewest containers. | No |
| `LowestCostFitContainerPackingStrategy` | By an allocation with the lowest cost. | No |
| `BruteForceContainerPackingStrategy` | The best of all sequences of container types. | No |

The first three are in `com.github.skjolber.packing.packer.strategy` and its subpackages `ordered` and `cost`, the others in `allocation` and `bruteforce`.

### Ordered

Takes the container items in their order of preference (the order they were added in). Each round:

 1. Unless that check is switched off (see [the default](#the-default)), `ContainerAllocationPlanner.canAllocate(..)` tests that the remaining boxes can be assigned to the remaining containers by fit, volume, weight, counts and the container limit, and fails early if not.
 2. If more than one container may still be used (`getMaxContainerCount()` above one), it looks for the most preferred container which holds all the remaining boxes: the candidates in order if there are two or fewer, otherwise by a binary search over them. If found, it accepts that container and is done.
 3. Otherwise it packs the remaining boxes into each candidate, from the last container item to the first (larger containers are expected last), and accepts the best result by the packager's `IntermediatePackagerResultComparator`; on a tie, the more preferred container wins. When the best result so far already holds all the boxes, other containers first try `peek(..)`.

So the order of preference decides only when more than one container may be used. With the default limit of one container, step 3 compares the containers: the default comparator ranks results by load volume, load weight and box count, then prefers the container with the smaller volume, then the lower empty weight, so the smallest container which holds all the boxes wins, whatever its place in the list.

The comparator's `prefersHigherLoadVolume()` tells the strategy whether a result with less load volume always compares worse. If so (the default comparator, `DefaultIntermediatePackagerResultComparator`), the best result so far is passed to `attempt(..)` as the `best` hint, so that a packager can skip searches which cannot load more. If not (the default, for a custom comparator), no hints are passed. If an interrupt arrives while it compares candidates, it accepts the best result if that holds all the remaining boxes.

The `SingleContainerPacker` constructor argument replaces the search of step 2.

### Lowest cost

Needs a cost calculator on every container item (otherwise an `IllegalStateException`). Each round it packs the remaining boxes into every candidate container type, without hints or `peek(..)`, prices each result, and searches for the cheapest set of results which would cover the remaining box count, volume and weight, with each type used at most as often as it has containers and the whole set within the container limit. It accepts one container of that plan, the one with the lowest container item index, and plans again from the new state. If no plan covers what remains, it takes the best result by the comparator. If an interrupt arrives, it accepts the cheapest result which holds all the remaining boxes, if there is one. It is a heuristic: see [container costs](container-costs.md#what-costs-change).

### Parallel

Each round, it forks the session for every candidate container type, packs them on the `ExecutorService` you pass in, waits for all, and accepts the best result by the comparator in the original session. It has no single-container shortcut, hints or `peek(..)`, and does not use costs. It also checks the allocation feasibility (`withoutAllocationFeasibilityCheck()` skips it). The strategy neither creates nor shuts down the executor; if a round is interrupted or fails, it cancels the tasks which have not finished. It needs sessions which accept results from their forks, as the sessions of the built-in packagers do.

Not to be confused with `ParallelBruteForcePackager`, which splits the search for one container between threads: see [parallel brute-force packager](heuristics/parallel-brute-force.md).

### Allocation

`FewestContainersFitContainerPackingStrategy` and `LowestCostFitContainerPackingStrategy` separate the choice of containers from packing. Each round, `ContainerAllocationPlanner` assigns all the remaining boxes (or groups) to the available containers, considering only the fit of each box, volume, weight, counts and the container limit, not a placement. The objective is the fewest containers, or the lowest cost (fewer containers on a tie; this one needs cost calculators on all container items). The strategy packs the first container of the allocation. If that packs nothing, it leaves that container type out until the next accepted container and plans again; after an accepted container it plans again from the new state. It returns `null` when no allocation exists. Both have a no-argument constructor.

### Brute force

`BruteForceContainerPackingStrategy` tries every sequence of container types, up to the container limit, depth first, with a fork of the session per branch, and prunes the branches which the allocation planner finds infeasible. Which boxes go into each container is still decided by the packager. A `BruteForceContainerPackingStrategy.Controls` implementation decides which branches to try, and which complete packing is the result:

 * `FewestContainersControls`, the default: the fewest containers.
 * `LowestCostControls`: the lowest cost, then the fewest containers; it skips branches which cannot beat the best result, using the minimum cost of the container and an estimate of the remaining cost from a `ContainerItemsCostCalculator` (`EstimatingContainerItemsCostCalculator`, a bound from total volume and weight, by default; `ExactContainerItemsCostCalculator`, a tighter bound from assigning whole boxes and groups to containers, at a higher cost for each estimate).

```java
.withContainerPackingStrategyFactory((inventory, boxItems, boxItemGroups, comparator, emptyResult) ->
    new BruteForceContainerPackingStrategy(new LowestCostControls()))
```

The search is exponential in the number of container types and containers, so keep it to a few; an interrupt throws away the best sequence found so far. The controls keep the result, so a strategy instance serves one packaging operation: create it in the factory, as above.

## Choosing a strategy

 * Keep the default for most orders: with a preference order of the container items, or with costs when container types are priced differently (see [container costs](container-costs.md#count-based-or-cost-based-selection)).
 * Use `ParallelContainerPackingStrategy` when there are several container types, each packing takes long (for example with a brute-force packager) and there are idle cores. Measure first: the overhead of the tasks can outweigh the gain on small inputs.
 * Use `LowestCostFitContainerPackingStrategy` for a cheap cost plan, and `FewestContainersFitContainerPackingStrategy` to plan with the fewest containers by capacity.
 * Use `BruteForceContainerPackingStrategy` for a few container types and a few containers, when the sequence matters more than the time (and set a deadline).
 * Write your own for any other rule, for example a fixed order of container types.

## Concurrency

Packagers are thread-safe, so several packaging operations may run at the same time. `ContainerPackingStrategyFactory.create(..)` is called once for each operation, and so concurrently: a factory must be safe for concurrent use (stateless ones are, like the default factory), and return a new strategy for each call, or a stateless one. The packager does not share a strategy between operations. The `boxItems` and `boxItemGroups` the factory gets belong to the session and change as containers are accepted: do not modify them.

A strategy and its sessions run on the thread of the operation; use one thread for each session, and `fork()` or `fresh()` for another. `ParallelContainerPackingStrategy` does that: its tasks run on the threads of your executor, one fork each. The result comparator, the cost calculators and the controls factories are shared by all operations, and must be safe for concurrent use as well (see [DEVELOPER.md](../DEVELOPER.md)).

## See also

 * [Container costs](container-costs.md): calculators, and what costs change.
 * [DEVELOPER.md](../DEVELOPER.md#writing-your-own-container-packing-strategy): writing a strategy.
 * [FEATURES.md](../FEATURES.md#container-selection-and-allocation): the feature summary.

## Tests and benchmarks

 * `DefaultContainerPackingStrategyFactoryTest` and `ContainerCostPackingTest` in `core`: which strategy is the default, what the factory receives, and the cost-aware strategies.
 * `OrderedContainerPackingStrategyNullResultTest`, `ParallelContainerPackingStrategyTest`, `ContainerAllocationStrategyTest` and `BruteForceContainerPackingStrategyTest` in `core`, in the matching subpackages of `packer.strategy`.
 * `PeekControlsReuseTest` in `core`: when `peek(..)` reuses a result.
 * `LargestContainerFirstStrategy` in the `test` module (package `com.github.skjolber.packing.test.example`): an example strategy which needs the `api` module only.
 * `ContainerPackingStrategyBenchmark` in the `jmh` module: ordered against parallel, with the plain packager on rotated containers and with the brute-force packager on a Bouwkamp code, for 2, 4 and 6 container types. Run with `java -jar jmh/target/benchmark.jar ContainerPackingStrategyBenchmark` after building, see [jmh/README.md](../jmh/README.md).
