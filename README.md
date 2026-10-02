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

Distinct item types are not combined, even if their dimensions match. This keeps
preprocessing cheap and preserves original identical-item semantics. Grids that
cannot fit remain ordinary inventory; refinement can try smaller grids.

Preprocessing, refinement and delegate attempts share one supplier created
by `PackagerInterruptSupplierBuilder`, using `withInterruptDeadline(...)` or
`withInterruptDuration(...)` and any caller-provided interrupt. There are no
separate preprocessing budgets or reserved time slices. Fallback is
attempted only while the shared operation deadline has not expired. Set
`withAggregation(false)` to disable aggregation.

Aggregation can reduce packing flexibility. The wrapper selectively splits a
large virtual box after failure, or when a successful result might use fewer or
cheaper containers. Four refinement steps are allowed by default;
`withMaxRefinements(0)` disables them. Each split preserves original counts,
reuses unchanged assemblies, and retains the best valid packing found so far.
Layouts and unsuccessful grid generations are cached by original inventory and
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
placements. Generated grids skip general overlap validation and do not allocate
contact graphs or physical search state. Refinement reuses
original orientations without cloning boxes or remapping child placements.
`VirtualBoxLayout` retains a `List<Placement>` whose coordinates are relative to
the virtual box origin. The list and its placements are shared and must not be
modified; expansion creates separate placements at container coordinates.
`VirtualBox` turns equivalent filled layouts into a count-one item. Expansion mappings use
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
