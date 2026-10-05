![Build Status](https://github.com/skjolber/3d-bin-container-packing/actions/workflows/maven.yml/badge.svg) 
[![Maven Central](https://img.shields.io/maven-central/v/com.github.skjolber.3d-bin-container-packing/parent.svg)](https://mvnrepository.com/artifact/com.github.skjolber.3d-bin-container-packing)

# 3d-bin-container-packing

This library does 3D rectangular bin packing; it attempts to match a set of 3D items to one or more in a set of 3D containers. The result can be constrained to a maximum number of containers.

Projects using this library will benefit from:

 * short and predictable calculation time,
 * fairly good use of container space, 
 * brute-force support for low number of boxes (ideal for small orders)

with

 * friendly API
 * customizable packagers
    
Bugs, feature suggestions and help requests can be filed with the [issue-tracker].

See [FEATURES.md](FEATURES.md) for a capability overview, including known
limitations and non-goals.

## Build from source

Use JDK 25 and the included Maven wrapper (Maven 3.9.12). Library sources target Java 17.

```sh
./mvnw -B -ntp -Pdev -pl core -am test
```

This runs the core tests and required modules, with coverage and documentation generation skipped.
Run `./mvnw -B -ntp verify` for full verification. On Windows, use `mvnw.cmd`.
See [AGENTS.md](AGENTS.md) for targeted tests, concurrency options, and failure reports.

## Obtain
The project is implemented in Java and built using [Maven]. The project is available on the central Maven repository.

For the previous version, see the [3.x](https://github.com/skjolber/3d-bin-container-packing/tree/3.x) branch.

<details>
  <summary>Maven coordinates</summary>

Add
 
```xml
<3d-bin-container-packing.version>5.0.x</3d-bin-container-packing.version>
```

and

```xml
<dependency>
    <groupId>com.github.skjolber.3d-bin-container-packing</groupId>
    <artifactId>core</artifactId>
    <version>${3d-bin-container-packing.version}</version>
</dependency>
```

Add the optional result validators separately when needed:

```xml
<dependency>
    <groupId>com.github.skjolber.3d-bin-container-packing</groupId>
    <artifactId>validators</artifactId>
    <version>${3d-bin-container-packing.version}</version>
</dependency>
```

</details>

or

<details>
  <summary>Gradle coordinates</summary>

For

```groovy
ext {
  containerBinPackingVersion = '5.0.x'
}
```

add

```groovy
api("com.github.skjolber.3d-bin-container-packing:core:${containerBinPackingVersion}")
// optional result validators
api("com.github.skjolber.3d-bin-container-packing:validators:${containerBinPackingVersion}")
```

</details>

# Usage
The units of measure is out-of-scope, be they cm, mm or inches.

Obtain a `Packager` instance, then then compose your container and product list:

```java
List<BoxItem> products = new ArrayList<>();

products.add(new BoxItem(Box.newBuilder().withId("Shoes").withSize(6, 10, 2).withRotate3D().withWeight(25).build(), 1));
products.add(new BoxItem(Box.newBuilder().withId("Pants").withSize(4, 10, 1).withRotate3D().withWeight(25).build(), 1));
products.add(new BoxItem(Box.newBuilder().withId("Hat").withSize(4, 10, 2).withRotate3D().withWeight(50).build(), 1));

// add a single container type
Container container = Container.newBuilder()
    .withDescription("1")
    .withSize(10, 10, 3)
    .withEmptyWeight(1)
    .withMaxLoadWeight(100)
    .build();
    
// with unlimited number of containers available
List<ContainerItem> containerItems = ContainerItem
    .newListBuilder()
    .withContainer(container)
    .build();
```

Pack all in a single container:

```java
PackagerResult result = packager
    .newResultBuilder()
    .withContainerItems(containerItems)
    .withBoxItems(products)
    .build();

if(result.isSuccess()) {
    Container match = result.get(0);
    
    // ...
}
```

The placements (`match.getStack().getPlacements()`) refer to copies of the input boxes; identify them by
`placement.getStackValue().getBox().getId()`.

Use a maximum number of containers:

```java
int maxContainers = ...; // maximum number of containers which can be used

PackagerResult result = packager
    .newResultBuilder()
    .withContainerItems(containerItems)
    .withBoxItems(products)
    .withMaxContainerCount(maxContainers)
    .build();
```

Note that all `packager` instances are thread-safe. Packing works on copies of the input boxes and containers, so boxes
can be shared between threads; it only assigns global indexes to box items which have none.

### Plain packager
A simple packager

```java
PlainPackager packager = PlainPackager
    .newBuilder()
    .build();
```

### Largest Area Fit First (LAFF) packager
A packager using the LAFF algorithm

```java
LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager
    .newBuilder()
    .build();
```

### Brute-force packager
For a low number of packages (like <= 6) the brute force packager might be a good fit. 

```java
Packager packager = BruteForcePackager
    .newBuilder()
    .build();
```

See also the `ParallelBoxItemBruteForcePackager` and `FastBruteForcePackager`
packagers. A `BruteForcePointIteratorFilter` can rank fitting points and use a
different point limit at each placement step.

Using a deadline is recommended whenever brute-forcing in a real-time application.

### Virtual-box preprocessing

`VirtualBoxPackager` in `com.github.skjolber.packing.virtualbox` wraps a packager
and replaces suitable inventories with filled rectangular assemblies. Each
virtual item stands for one assembly; equal assemblies share one item with a
count. Its alternative stack values represent different
layouts of the same original boxes. Returned containers are expanded back into
original boxes, orientations and identities. Input counts and indexes are not
changed by the aggregated path or its ungrouped fallback.

```java
try (BruteForcePackager delegate = BruteForcePackager.newBuilder().build();
     VirtualBoxPackager packager = new VirtualBoxPackager(delegate)) {
    PackagerResult result = packager.newResultBuilder()
        .withBoxItems(items)
        .withContainerItems(containers)
        .withMaxContainerCount(3)
        .withMaxLayouts(8)
        .withMaxRefinements(4)
        .withMaxDelegateBoxes(20)
        .withInterruptDuration(1000)
        .build();
}
```

The wrapper does not own or close its delegate. Configure packager-specific
options on the delegate before wrapping it.

Preprocessing first constructs factor grids for entire repeated box items:
`columns * rows * layers == count`. It prefers container axes which are matched
exactly, or which leave a strip narrower than every box, then compact envelopes.
Only permitted original orientations are used. Default limits are 10,000 physical
boxes per grid and eight layouts. Every container type which can hold the grid keeps
at least one layout of its own, even beyond that limit, so a virtual box always has a
layout for each container it fits.

If the whole count forms no fitting grid (prime counts, or more boxes than one
container holds), the item is split into container-sized grids, like a loaded
container: full blocks, then whole layers, whole rows and a line. For example,
40 boxes in containers holding 18 become `18 + 18 + 3 + 1`. Equal blocks are
handed to the delegate as one item with a count, so brute force does not
enumerate orders of interchangeable blocks. The split prefers block sizes which the
available containers can hold (container counts and the maximum container count):
with one large and three small containers, 36 boxes become three small-container
blocks rather than two large-container blocks. When containers have costs, blocks
which fit more container types are preferred, so cheaper containers remain usable.

Distinct item types are not combined, even if their dimensions match. This keeps
preprocessing cheap and preserves original identical-item semantics.

Preprocessing, refinement and delegate attempts share one supplier created
by `PackagerInterruptSupplierBuilder`, using `withInterruptDeadline(...)` or
`withInterruptDuration(...)` and any caller-provided interrupt. There are no
separate preprocessing budgets or reserved time slices. Fallback is
attempted only while the shared operation deadline has not expired. Set
`withAggregation(false)` to disable aggregation.

Aggregation can reduce packing flexibility. The wrapper selectively splits a
large virtual box after failure, or when a successful result might use fewer or
cheaper containers. Without container costs, a result already at the volume and
weight lower bound is not refined if all containers have the same volume. A split
halves the longest axis of the virtual box's grid, so both parts are grids which fit
the containers the original fitted. All equal copies of that virtual box are split
in the same step, or as many as the delegate-item limit allows.
Four refinement steps are allowed by default;
`withMaxRefinements(0)` disables them. Each split preserves original counts,
reuses unchanged assemblies, and retains the best valid packing found so far.
Layouts and unsuccessful grid generations are cached by original inventory and
counts within the operation; repeated child inventories reuse the same layouts.
The delegate's outer packing search is restarted with fresh inventory.

`withMaxDelegateBoxes(...)` limits the number of physical items handed to the
delegate, including refined and ungrouped attempts. The default is unlimited;
set it when wrapping a brute-force packager. If no permissible representation fits,
the operation may fail without attempting every original box individually.

If refinement fails, the wrapper retries the original inventory without
grouping, subject to that count limit and the shared deadline.
Set `withCompareUngrouped(true)` to
also try ungrouped packing after success, retaining lower reported cost, then
fewer containers, then less container volume. This costs another packing
attempt and is disabled by default.

The standalone grid generator checks internal weight, pressure and stack-depth
limits before constructing placements. An envelope cannot safely represent loads
between assemblies, physical stack depth or original box identities. Consequently,
if any input box has a load constraint, the wrapper bypasses aggregation for the
whole operation and forwards the original inventory to the delegate. Use a
load-aware delegate to enforce those constraints during packing. There is no
post-pack load validation, graph rebuilding or load-validation retry; virtual-box
refinement and delegate-item limits do not apply to this bypass path.

Groups, chronological ordering, controlled containers, initial points/obstacles,
motion and existing placements bypass aggregation and retain the delegate's
ordinary behavior. Delegate-specific callbacks must tolerate synthetic boxes
when aggregation is enabled.

The standalone `GridVirtualBoxLayoutGenerator` exposes the same fast generation.
It checks each candidate's internal load limits analytically before creating
placements. Generated grids skip general overlap validation, create their child
placements only when first accessed, and do not allocate contact graphs or
physical search state. `partition(...)` exposes the container-sized split. Refinement reuses
original orientations without cloning boxes or remapping child placements.
`VirtualBoxLayout` retains a `List<Placement>` whose coordinates are relative to
the virtual box origin. The list and its placements are shared and must not be
modified; expansion creates separate placements at container coordinates.
`VirtualBox` turns equivalent filled layouts into a delegate item, with a count for
equal copies. Expansion mappings use
operation-global item indexes and stack-value indexes, not IDs or mutable local
indexes; custom delegate implementations must preserve those indexes.

<details>
  <summary>Algorithm details</summary>

### Largest Area Fit First algorithm
The implementation is based on [this paper][2], and is not a traditional [bin packing problem][1] solver.

The box which covers the largest ground area of the container is placed first; its height becomes the level height. Boxes which fill the full remaining height take priority. Subsequent boxes are stacked in the remaining space in at the same level, the boxes with the greatest volume first. If box height is lower than level height, the algorithm attempts to place some there as well. 

When no more boxes fit in a level, the level is incremented and the process repeated. Boxes are rotated, containers not.

 * `LargestAreaFitFirstPackager` stacks in 3D within each level
 * `FastLargestAreaFitFirstPackager` stacks in 2D within each level

The algorithm runs reasonably fast, usually in milliseconds. Some customization is possible.

### Plain algorithm
This algorithm selects the box with the biggest volume, fitting it where it is best supported.

###  Brute-force algorithm
This algorithm has no logic for selecting the best box or rotation; running through all permutations, for each permutation all rotations:

 * `BruteForcePackager` attempts all box orders, rotations and placement positions.
 * `FastLargestAreaFitFirstPackager` selects all box orders and rotations, selecting the most appropriate placement position.

The complexity of this approach is [exponential], and thus there is a limit to the feasible number of boxes which can be packaged within a reasonable time. However, for real-life applications,  a healthy part of for example online shopping orders are within its grasp.

The worst case complexity can be estimated using the relevant iterators before packaging is attempted.

The algorithm tries to skip combinations which will obviously not yield a (better) result:

 * permutations
   * two or more boxes have the same dimensions
   * permutations which mutated at a previously unreachable index
 * fewer rotations
   * two or more sides have the same length
   * rotations which mutated at a previously unreachable index
 
There is also a parallel version `ParallelBruteForcePackager` of the brute-force packager, for those wishing to use it on a multi-core system.

Do not attempt this with many boxes of different sizes: the number of combinations grows exponentially, so it will likely not complete in time. The search itself is not recursive, so many identical boxes do not exhaust the thread stack.

</details> 

# Packager customizations

## Obstacles within containers
Make the packager account for non-rectangular packaging space, i.e. pillars or other obstacles within the container loading area.

## Load constraints
Boxes can limit what is stacked on top of them:

```java
Box box = Box.newBuilder()
    .withSize(400, 300, 200)
    .withWeight(12)
    .withRotate3D()
    .withMaxLoadWeight(50)          // total weight resting on the box, through all levels above
    .withMaxLoadPressure(0.001)     // weight per area unit
    .withMaxLoadBoxCount(4)         // boxes stacked on top
    .build();
```

Use `withMaxLoadIdenticalBoxCount(count)` to only allow boxes of the same type on top. The plain
and LAFF packagers detect the constraints and enforce them; for brute force, use the load-aware
variants (`LoadBruteForcePackager`, `LoadFastBruteForcePackager`, `LoadParallelBoxItemBruteForcePackager`).

## Insertion order
The placements of each container are in insertion order: the order in which the boxes can be loaded. Each box comes
after the boxes it rests on, and after the boxes it would otherwise have to pass on its way in. Set how boxes get into
a container type with `withAccess(..)`:

```java
Container container = Container.newBuilder()
    .withSize(1200, 240, 260)
    .withMaxLoadWeight(25_000)
    .withAccess(ContainerAccess.FRONT) // a door at x = dx, loading from x = 0; or TOP; default ANY
    .build();
```

Without a box item order (`Order.NONE`), the packagers put the placements of each result in insertion order after
packing (`InsertionSequencer`); the placements themselves are unchanged. With an order, only boxes which can be
inserted after the boxes already there are placed. As boxes are only added on top of, or in front of, the boxes
already there, the loads never decrease while loading: a result within its load limits is within them at every step
of loading and unloading. `InsertionOrderValidator` (part of `DefaultValidator`) checks the order.

Putting the placements in insertion order takes time quadratic in the number of boxes per container. When only the
outcome matters, for example to check whether an order fits during checkout, skip it with
`withInsertionOrder(false)` on the result builder, and calculate the order later if needed with
`InsertionSequencer.sequence(result.getContainers(), Order.NONE)`.

`PackagerResult.isInsertionOrder()` tells whether a result is known to be in insertion order: false when it was
skipped, or when the boxes cannot be loaded in any order (possible through a door, as the packagers place boxes
without regard to the door when there is no box item order).

## Support
Support (the area resting on boxes below) can be calculated, or full support required:

```java
PlainPackager packager = PlainPackager
    .newBuilder()
    .withCalculateSupport(true)     // prefer better supported placements
    .withRequireFullSupport(true)   // or: only place fully supported boxes
    .build();
```

The LAFF packager builders have the same options.

## Container costs
Give container types a cost to prefer cheaper combinations of containers, using
`ContainerItem.newListBuilder().withContainer(container, count, costCalculator)` with an
implementation of `ContainerCostCalculator` (see `com.github.skjolber.packing.cost`).

## Container strategies
A container strategy decides which containers to use, and in which order. By default, containers
are tried in the supplied (preference) order, or the cheapest combination is searched for when the
containers have costs. Supply your own with `withContainerStrategyFactory(..)` on the packager
builders; see [DEVELOPER.md](DEVELOPER.md).

## Combining packagers
`CompositePackager` uses costly packagers only where cheaper packagers fall short:

```java
CompositePackager packager = CompositePackager.newBuilder()
    .withPackager(PlainPackager.newBuilder().build())                // tried first, for every container
    .withPackager(FastBruteForcePackager.newBuilder().build(), 200)  // only where plain does not fit all boxes, for at most 200 ms
    .build();
```

The first packager (or those added with `withBaselinePackager(..)`) first packs the whole order, giving a
baseline result. Then, for each container the container strategy attempts, the packagers are tried in order
until one fits all remaining boxes; a costlier packager only needs to beat the cheaper packagers' result.
The better result is returned (see `PackagerResultComparator`), and the baseline if the deadline passes.

For random orders in the shipping containers of issue #1158, a plain and fast brute force composite (200 ms budget)
packed every order, with 2-6 % less container volume than the plain packager, at 10-60 ms per order; brute force alone
ran out of time for many of the orders. See `CompositeQualityReport` in the `jmh` module.

## Validating results
The optional `validators` artifact checks packing results, for example the load constraints:

```java
LoadValidator validator = new DefaultLoadValidatorBuilder()
    .withPlacements(container.getStack().getPlacements())
    .build(); // null if no load constraints are present

List<ValidatorResultReason> reasons = new ArrayList<>();
boolean valid = validator.isValid(container.getStack().getPlacements(), reasons);
```

`DefaultValidator` validates a whole `PackagerResult` against the input.

## Packager controls
The packagers (excluding brute force) can be extended to handle specialized needs via various `control` (plugins) types. 

In a nutshell, the `controls` are stateful objects which are handed various resources from the packagers during construction, and then notified and/or invoked at certain milestones within the packaging process.

`Controls` must be provided as follows:

 * builder factory
    * builder
       * controls

### Manifest-controls
Determines which boxes go into which containers, i.e. in which combinations. 

A classic example would to be to not package both lighters and dynamite in the same container.

### Point-controls
Determines which points are relevant for a specific box. 

For example, heavy items might be require only points at ground level or flammable items might be required to be stacked in a certain zone.

### Placement-controls
Determines the best placement for a box. 

Can consider a range of options, like stability, stacking height, structural integrity and so on; even randomization is possible. Note that these features are not necessarily implemented in the packagers within this project.

# Visualizer
There is a simple output [visualizer](visualizer) included in this project, based of [three.js](https://threejs.org/). This visualizer is currently intended as a tool for developing better algorithms; not as stacking instructions.

### Setup
```
cd visualizer/viewer
npm install
```

### Run
```
npm start
```

The viewer shows `visualizer/viewer/public/assets/containers.json`, and reloads it when it changes. Write it with
`DefaultPackagingResultVisualizerFactory` (module `visualizer/packaging`), for one result or several results of the same order:

```java
Map<String, PackagerResult> results = new LinkedHashMap<>();
results.put("plain", plainResult);
results.put("composite", compositeResult);
new DefaultPackagingResultVisualizerFactory(true) // true: calculate the free points after each placement
    .visualize(results, validator.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems), file);
```

The viewer shows:

 * a summary of the result: whether it was packed, the time and cost, and the volume and weight used per container
 * a comparison table when there are several results (`r` or click a row to switch)
 * whether the result is valid: the factory validates the boxes' load limits (and the whole result, given the input), logs the reasons, and the viewer outlines the boxes of invalid placements in red
 * colour modes (`c`): box item, group, support, and load relative to the max load weight
 * the container's opening (orange), and boxes which are not in a possible insertion order
 * each container's centre of gravity, and for each box its supported area and load
 * the packing steps (`a` / `d`) and the free points after each placement (`p`, `w` / `s`)

To "hot reload" the visualizer during development, make your unit tests write that file. The `*VisualizationTest`
classes in `visualizer/packaging` are examples (load limits, insertion order, groups and support, container costs,
virtual boxes, and comparing packagers); they are run by hand, for example from the IDE or with
`./mvnw -B -ntp -Pdev -pl visualizer/packaging -am -Dtest=PackagerComparisonVisualizationTest -Dsurefire.failIfNoSpecifiedTests=false test`.

For a tour of the features, run `ShowcaseVisualizationTest` while the viewer is open: it writes one scenario after
another, with a delay between them (`-Dshowcase.delay=20` seconds, `-Dshowcase.rounds=3`), and prints what to look
at. The viewer fits the camera when the containers change size.

![Alt text](visualizer/viewer/images/view.png?raw=true "Demo")

# Get involved
If you have any questions, comments or improvement suggestions, please file an issue or submit a pull-request. 

Note on bugs: Please follow [shuairan's](https://github.com/shuairan) example and [file a test case with a visualization](https://github.com/skjolber/3d-bin-container-packing/issues/574).

# License
[Apache 2.0]. Social media preview by [pch.vector on www.freepik.com](https://www.freepik.com/free-photos-vectors/people).

# Interesting links

 * [The Art of Stacking: Challenges Faced While Developing a Packing Algorithm](https://medium.com/@fayyazawais1412/the-art-of-stacking-challenges-faced-while-developing-a-packing-algorithm-64d869b924ab)

# History
 * 5.0.0: Major release. Breaking changes.
     * Box load constraints: max load weight, pressure, box count and identical boxes only
     * Support calculation + full support for plain and LAFF packagers
     * Container costs and container strategies (ordered, parallel, allocation), and custom container strategies
     * `CompositePackager`: cheap packagers first, costly packagers only where needed
     * Virtual-box preprocessing
     * Substantially faster point calculation, placement search, support calculation and load validation
     * Visualizer: result summaries and comparison of several results, validation reasons on the boxes, colour modes for groups, support and load, and the centre of gravity
     * Behaviour changes:
        * The max load weight of a box limits the total weight resting on it, through all levels and paths of the support graph (previously only direct loads were counted)
        * The full-support fallback no longer skips positions at the edge of a point
        * `MarkResetPointCalculator2D.reset()` restores points which were constrained in place (mutable mode)
        * Supported areas no longer overflow for large dimensions (contact areas above the `int` range, e.g. with 1/10000 inch units)
        * The brute-force placement search is iterative: packing many boxes no longer fails with a stack overflow
        * The load-aware brute-force builders apply `withSkipReversePermutations(..)`, and `LoadParallelBoxItemBruteForcePackager` uses its point filter when boxes have load constraints
        * Brute-force packagers no longer reduce the counts of the box items passed in when packing uses several containers
        * Packing with several container types no longer gives up when each box fits only some of the types (the feasibility check used box indexes which change during packing)
        * Brute-force packing of box item groups over three or more containers no longer fails with a `NullPointerException`
        * Brute-force packagers use the first container type which holds the boxes: when a result was reused for another container type, the copy had no load volume and was never selected, so larger containers were used
        * `FastBruteForcePackager` reports interrupted packings as timeouts, like the other packagers (previously no result, or a `NullPointerException` in the container strategy)
        * Brute-force packing of box item groups no longer fails when a group does not fit some container types (the volume and weight check was inverted). Groups are packed in order: a container takes the remaining groups up to the first which does not fit it
        * Packing works on copies of the boxes and containers: result placements refer to copies of the input boxes (match them by id), and boxes can be shared between threads
        * Brute force skips permutations and containers which cannot load more than the best result so far, when the result comparator compares load volume first (`IntermediatePackagerResultComparator.prefersHigherLoadVolume()`); results are unchanged
        * The load and stability validators find which boxes rest on which from the placements' positions (`SupportGraph`), instead of the support links recorded by the packager. Results from packagers without load limits or support, and hand-made results, are now validated too (previously they passed without being checked)
        * A box placed into a gap under boxes which are already there carries part of their weight: the packagers with load limits now record this when the box is accepted, so later placements are checked against the actual loads (previously the relief for the boxes below was assumed when the box was placed, but not recorded, and boxes could be overloaded)
        * The placements of each container are in insertion order (`InsertionOrder`): each box after the boxes it rests on, and after the boxes in its path from the container's opening (`Container.withAccess(ContainerAccess)`: `ANY`, `TOP` or `FRONT`). Without a box item order, results are reordered after packing (`InsertionSequencer`); with an order, only insertable boxes are placed. `DefaultValidator` checks the order (`InsertionOrderValidator`)
        * `NonIdenticalLoadBoxReason` names the box with the identical-box-only limit as the constrained placement (previously the box directly below the offending box)
     * Breaking changes:
        * `Order.CRONOLOGICAL` and `Order.CRONOLOGICAL_ALLOW_SKIPPING` are renamed to `Order.CHRONOLOGICAL` and `Order.CHRONOLOGICAL_ALLOW_SKIPPING`
        * Validators moved to a separate `validators` artifact (package `com.github.skjolber.packing.validator`)
        * `CenterOfGravityStabilityValidator.isPlacementStable(..)` and `CenterOfGravitySupportStabilityValidator.isPlacementStableSupport(..)` take a `SupportGraph`
        * Interrupts / deadlines moved from `core` (`com.github.skjolber.packing.deadline`) to `api` (`com.github.skjolber.packing.api.interrupt`)
        * `PackagerException` moved from `core` (`com.github.skjolber.packing.packer`) to `api` (`com.github.skjolber.packing.api`); `ParallelBruteForcePackagerException` now extends it
        * `PlacementComparator` now compares two `Placement`s (`compare(a, b)`, positive when `a` is better). Comparators are built by a `PlacementComparatorFactory`, by default `DefaultPlacementComparatorFactory` in `core`
        * Decision-making interfaces are in `api`, so that custom behaviour only needs `api`: `PlacementComparator`, `PlacementComparatorFactory` and `PlacementComparatorAttribute` (`com.github.skjolber.packing.api.packager.control.placement`), and `IntermediatePackagerResult` and `IntermediatePackagerResultComparator`, moved from `core` to `com.github.skjolber.packing.api.packager`
        * Container strategies are in `api` (`com.github.skjolber.packing.api.packager.strategy`): `ContainerStrategy`, `ContainerStrategyFactory`, `ContainerResult` and `ContainerItemsResult`. `PackagerAdapter` is renamed to `PackagerSession` (without `reset()`; use `fresh()`), strategies see the containers as a `ContainerInventory`, and `PackagerInterruptedException` moved to `com.github.skjolber.packing.api.interrupt`
        * Configure a container strategy with the packager builders' `withContainerStrategyFactory(..)`; `AbstractPackager.setContainerPackingStrategyFactory(..)` is removed
        * `PackagerSession.attempt(index, best, ..)` may return an empty result instead of a result with less load volume than `best`; strategies which pass the best result so far must handle this
        * Packagers create sessions with `AbstractPackager.createSession(PackagerInput, ..)`; subclasses implement `newSession(..)`, and each session works on its own copies of the boxes and containers. `DefaultControlsPackagerResultBuilder` is removed
        * `ControlledContainerItem` removed: `ContainerItem` now holds the per-container controls (manifest and point controls, initial points, cost); `PackagerResultBuilder.ControlledContainerItemBuilder` renamed to `ContainerItemBuilder`
        * `clone()` methods renamed to `copy()` (they are copy constructors, not `Object.clone()`), including `Point.copy(maxX, maxY, maxZ)`; `ClonablePackagerInterruptSupplier` renamed to `CopyablePackagerInterruptSupplier`
        * Builder options which had no effect removed: `withPoints(..)` on the brute-force packager builders and `withFirstBoxItemGroupComparator(..)` on the LAFF builders
        * `PlainPlacement*` and `LargestAreaFitFirstPlacementControlsBuilder` removed (use the default placement controls with a placement comparator factory)
        * Points: a single `DefaultPoint3D` / `DefaultPoint2D` implementation replaces the plane- and support-specific point classes
        * The module descriptors export all public packages
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

[1]: 				https://en.wikipedia.org/wiki/Bin_packing_problem
[2]: 				https://www.drupal.org/files/An%20Efficient%20Algorithm%20for%203D%20Rectangular%20Box%20Packing.pdf
[Apache 2.0]: 		http://www.apache.org/licenses/LICENSE-2.0.html
[issue-tracker]:	https://github.com/skjolber/3d-bin-container-packing/issues
[Maven]:			http://maven.apache.org/
[LinkedIn]:			http://lnkd.in/r7PWDz
[Github page]:		https://skjolber.github.io
[NothinRandom]:		https://github.com/NothinRandom
[exponential]:		https://en.wikipedia.org/wiki/Exponential_function
