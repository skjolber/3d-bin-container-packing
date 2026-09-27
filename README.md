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
<3d-bin-container-packing.version>4.2.x</3d-bin-container-packing.version>
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
  containerBinPackingVersion = '4.2.x'
}
```

add

```groovy
api("com.github.skjolber.3d-bin-container-packing:core:${containerBinPackingVersion}")
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

Note that all `packager` instances are thread-safe.

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

### Best bounding box

`BruteForceBoundingBox` in `com.github.skjolber.packing.boundingbox` searches for a compact assembly
of **all** supplied boxes inside one empty container's load dimensions and weight
limit. This is useful for constructing virtual boxes before a larger packing operation.

```java
long totalBoxVolume = items.stream()
    .mapToLong(item -> item.getBox().getVolume() * item.getCount()).sum();

try (BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
    BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
        .withBoxItems(items)
        .withContainer(searchLimits)
        .withGoal(bounds -> bounds.getVolume() == totalBoxVolume)
        .withInterruptDuration(1000)
        .build();

    if (result.isSuccess()) {
        BoundingBox bounds = result.getBoundingBox();
        Stack assembly = result.getStack();
    }
}
```

The example stops at a filled rectangular assembly. Without `withGoal`, search
continues to exhaustion or interruption, minimizing bounding volume, then surface
area, then height, depth and width. `withComparator` replaces this ordering (negative
means better). A goal is tested on every complete candidate and returns the first
matching layout, even if another layout ranked better under the comparator.

The result distinguishes `EXHAUSTED`, `GOAL_REACHED` and `INTERRUPTED`. Interruption
retains the best complete layout found; unsuccessful results never contain a
partial assembly. Bounding boxes can contain gaps unless a filled-envelope goal
is satisfied. Returned placements retain the input box identities and orientations.

#### Multiple objectives in one search

Retain additional arrangements for later virtual-box packing without repeating
the permutation/rotation search:

```java
try (BruteForceBoundingBox boundingBox = new BruteForceBoundingBox()) {
    BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
        .withBoxItems(items)
        .withContainer(searchLimits)
        .withMinimumDimensions() // additional objectives named "x", "y", "z"
        .withInterruptDuration(1000)
        .build();

    if (result.isSuccess()) {
        BoundingBoxLayout narrowest = result.getAdditionalResults().get("x");
        BoundingBoxLayout shallowest = result.getAdditionalResults().get("y");
        BoundingBoxLayout lowest = result.getAdditionalResults().get("z");
        List<BoundingBoxLayout> alternatives = result.getResults();
    }
}
```

The primary result still minimizes volume by default. Use `withMinimumX()`,
`withMinimumY()` or `withMinimumZ()` individually, or register a named custom
ordering with `withAdditionalObjective(name, comparator)`. The built-in
`BoundingBox.MIN_X`, `MIN_Y` and `MIN_Z` compare the corresponding dimension
first and use `MIN_VOLUME` to break ties. Reusing a name replaces its objective.

`getAdditionalResults()` maps each registered name to its winner. `getResults()`
returns the primary layout followed by the additional winners, omitting repeated
references to the same saved layout. These are objective winners, not every
candidate or a complete Pareto frontier. Collections are unmodifiable, but stacks
are mutable and can be shared between winning objectives; do not mutate them.

Pruning must rule out improvements for every objective; larger-volume branches
can still improve width, depth or height. Built-in comparisons use primitive
extents, and a candidate improving several objectives is snapshotted only once.
The same API works with `LoadBruteForceBoundingBox`, including valid support
graphs for every retained layout.

Objectives can each supply their own acceptance predicate and comparator:

```java
var result = boundingBox.newResultBuilder()
        .withBoxItems(items)
        .withContainer(limits)
        .withObjective("width", bounds -> bounds.dx() <= targetWidth, BoundingBox.MIN_X)
        .withObjective("height", bounds -> bounds.dz() <= targetHeight, BoundingBox.MIN_Z)
        .build();
var widthLayout = result.getObjectiveResults().get("width");
var reachedGoals = result.getReachedGoals();
```

The first `withObjective` call replaces the implicit primary objective. All
registered objectives are independent: each retains its first goal-satisfying
layout, and the search stops with `GOAL_REACHED` only after **all** goals are met,
possibly by different layouts. Until a goal is met, its comparator selects the
best-so-far layout. Negative comparator results mean better.

A null predicate keeps optimizing until exhaustion or interruption, so it
prevents early goal termination. `withGoal` configures only the implicit primary
objective; `withAdditionalObjective(name, predicate, comparator)` adds an
independent objective without replacing it. The dimension convenience methods
have null predicates. Do not mix `withComparator`/`withGoal` with explicitly
named objectives; configure those through `withObjective` instead.

An interrupted search retains all winners and reports which goals were met.
Only objectives without an acceptance predicate are guaranteed optimal on
`EXHAUSTED`; accepted objectives remain frozen at their goal layouts.
Single-objective operations automatically use a specialized implementation of
`BruteForceBoundingBoxSearch`, preserving the original bounding-box fast path.

Orderings implement `BoundingBoxComparator`. They can override primitive
dimension comparison and a conservative `canImprove(...)` branch bound.
Unknown orderings default to no pruning; search does not identify comparator
singletons or classes. `BoundingBox.of(...)` is the checked factory for callers
outside a search; constructors assume dimensions have already been validated.

This sequential operation explores all permutations, rotations and extreme-point
choices, with safe volume pruning for the default objective. It is independent of
packager configuration and does not enumerate arbitrary coordinates. It is
geometric only: box-item groups, obstacles, existing placements,
per-box load constraints and stability validation are not supported. Ordinary
packing is unchanged. Keep the assembly size small and set a deadline.

For load-constrained assemblies, use `LoadBruteForceBoundingBox` with the same
result-builder API:

```java
try (LoadBruteForceBoundingBox boundingBox = new LoadBruteForceBoundingBox()) {
    BruteForceBoundingBoxResult result = boundingBox.newResultBuilder()
        .withBoxItems(items)
        .withContainer(searchLimits)
        .withGoal(bounds -> bounds.getVolume() == totalBoxVolume)
        .withInterruptDuration(1000)
        .build();
}
```

This variant checks orientation-specific maximum load weight, contact pressure,
stack depth (`maxLoadBoxCount`) and identical-item-only restrictions. Identical
means the same input `BoxItem`, not merely similar dimensions or matching IDs.
Only valid complete assemblies are considered for the objective and goal.
Returned placements include independent supporter/supportee links and distributed
load weights, including fractional weights shared between multiple supports.

Load checks use the complete assembly so that supports placed later can relieve
earlier loads. Reusable O(n) buffers avoid creating support graphs for rejected
candidates; validation costs O(n²) per complete candidate that needs checking.
This does not add group, obstacle, full-support or stability constraints.

### Virtual-box preprocessing

`VirtualBoxPackager` in `com.github.skjolber.packing.virtualbox` wraps a packager
and replaces suitable inventories with filled rectangular assemblies. Each
virtual item has count one; its alternative stack values represent different
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
        .withMaxSearchBoxes(6)
        .withMaxRefinements(4)
        .withMaxDelegateBoxes(20)
        .withInterruptDuration(1000)
        .build();
}
```

The wrapper does not own or close its delegate. Configure packager-specific
options on the delegate before wrapping it.

Preprocessing first constructs factor grids for entire repeated box items:
`columns * rows * layers == count`. It prefers exact matches to container
dimensions, then compact envelopes. Only permitted original orientations are
used. Default limits are 10,000 physical boxes per grid and eight layouts.

Remaining small items with similar sorted dimensions can be assembled by
multi-objective bounding-box search. Defaults are six physical boxes per search,
eight searches, and the operation's shared deadline/interrupt supplier.
`withMaximumDimensionDifference(0.25)` allows up to a 25% difference, relative
to the larger dimension, along each sorted axis. Set `withBruteForce(false)`
to use only direct grids. Every retained assembly must be completely filled;
hollow best-so-far bounding boxes are discarded, even after interruption.

Preprocessing, layout searches and delegate attempts share one supplier created
by `PackagerInterruptSupplierBuilder`, using `withInterruptDeadline(...)` or
`withInterruptDuration(...)` and any caller-provided interrupt. There are no
separate preprocessing/search budgets or reserved time slices. Fallback is
attempted only while the shared operation deadline has not expired. Set
`withMaxGridBoxes(1).withBruteForce(false)` to disable aggregation.

Aggregation can reduce packing flexibility. The wrapper selectively splits a
large virtual box after failure, or when a successful result might use fewer or
cheaper containers. Four refinement steps are allowed by default;
`withMaxRefinements(0)` disables them. Each split preserves original counts,
reuses unchanged assemblies, and retains the best valid packing found so far.
Layouts and unsuccessful layout searches are cached by original inventory and
counts within the operation; repeated child inventories reuse the same layouts.
The delegate's outer packing search is restarted with fresh inventory.

`withMaxDelegateBoxes(...)` limits the number of physical items handed to the
delegate, including refined and ungrouped attempts. The default is unlimited.
An oversized ungrouped item can also be split into smaller rectangular assemblies
to satisfy this limit. If no permissible representation fits, the operation may
fail without attempting every original box individually.

If refinement fails, the wrapper retries the original inventory without
grouping, subject to that count limit and the shared deadline.
Set `withCompareUngrouped(true)` to
also try ungrouped packing after success, retaining lower reported cost, then
fewer containers, then less container volume. This costs another packing
attempt and is disabled by default.

Load-constrained inputs use load-aware bounding-box search. Direct grids check
the maximum weight, pressure and depth of each identical column. After every
delegate attempt, the complete expanded physical packing is validated again,
including loads crossing virtual-box boundaries. Valid results receive rebuilt
support graphs; invalid results trigger refinement or fallback. Use a load-aware
delegate for the best chance of finding valid alternatives. This is validation
and retry, not a guarantee that an arbitrary delegate explores every load-valid
packing. It does not add stability or full-support requirements.

Groups, chronological ordering, controlled containers, initial points/obstacles,
motion and existing placements bypass aggregation and retain the delegate's
ordinary behavior. Delegate-specific callbacks must tolerate synthetic boxes
when aggregation is enabled.

The standalone `GridVirtualBoxLayoutGenerator` and
`BruteForceVirtualBoxLayoutGenerator` expose the same layout-generation steps.
`VirtualBoxLayout` retains fixed relative placements and `VirtualBox` turns
equivalent filled layouts into a count-one item. Expansion mappings use
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

Note that the algorithm is recursive on the number of boxes, so do not attempt this with many boxes (it will likely not complete in time anyhow).

</details> 

# Packager customizations

## Obstacles within containers
Make the packager account for non-rectangular packaging space, i.e. pillars or other obstacles within the container loading area.

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
There is a simple output [visualizer](visualization) included in this project, based of [three.js](https://threejs.org/). This visualizer is currently intended as a tool for developing better algorithms; not as stacking instructions.

### Setup
```
cd visualizer/viewer
npm install
```

### Run
```
npm start
```

Note: To "hot reload" the visualizer during development, make your unit tests write directly to a file in the viewer (see the `VisualizationTest` example).

![Alt text](visualizer/viewer/images/view.png?raw=true "Demo")

# Get involved
If you have any questions, comments or improvement suggestions, please file an issue or submit a pull-request. 

Note on bugs: Please follow [shuairan's](https://github.com/shuairan) example and [file a test case with a visualization](https://github.com/skjolber/3d-bin-container-packing/issues/574).

# License
[Apache 2.0]. Social media preview by [pch.vector on www.freepik.com](https://www.freepik.com/free-photos-vectors/people).

# Interesting links

 * [The Art of Stacking: Challenges Faced While Developing a Packing Algorithm](https://medium.com/@fayyazawais1412/the-art-of-stacking-challenges-faced-while-developing-a-packing-algorithm-64d869b924ab)

# History
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
