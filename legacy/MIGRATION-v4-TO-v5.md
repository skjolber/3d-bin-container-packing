# Migrating from 4.x to 5.0

This guide is for users of 4.2.x who upgrade to 5.0. It was prepared by comparing the public and protected
surface of 4.2.6-SNAPSHOT (4.2.5 plus two fixes) with 5.0.0-SNAPSHOT. 5.0 is a major release with breaking changes:

 * Code which only builds inputs and calls a packager needs a handful of renames (see [Common compile errors](#common-compile-errors)).
 * Code which implements controls, comparators, validators or container strategies, extends a packager, or uses the iterators or point calculators
   directly needs more.
 * Results are not identical to 4.x, see [Behavioral changes](#behavioral-changes).

Reading order: [what stays the same](#what-stays-the-same), the [renames](#renamed-and-replaced), what was [removed](#removed),
[new concepts](#new-concepts-worth-knowing), [behavior](#behavioral-changes), then the compile error [cookbook](#common-compile-errors).
This guide is the complete list of breaking and behavioral changes against 4.x; the History in [README.md](../README.md#history) keeps the feature summary.
See [FEATURES.md](../FEATURES.md) for the capability overview and [DEVELOPER.md](../DEVELOPER.md) for writing controls and strategies.

## What stays the same

The usage flow is the same: build `Box`, `BoxItem`, `Container` and `ContainerItem` objects with their builders, obtain a packager with
`XxxPackager.newBuilder().build()`, then `packager.newResultBuilder()...build()` returns a `PackagerResult` with `isSuccess()`, `get(int)`,
`getContainers()` and `isTimeout()`. The `Packager` interface, the existing members of `PackagerResult` (it only gained members), the packager `newBuilder()` entry points
(`PlainPackager`, `LargestAreaFitFirstPackager`, `FastLargestAreaFitFirstPackager`, `BruteForcePackager`, `FastBruteForcePackager`) and the
result builder methods `withContainerItems(..)`, `withBoxItems(..)` (box items), `withBoxItemGroups(List)`, `withOrder(..)`,
`withMaxContainerCount(..)` and `withContainerItem(Consumer)` are unchanged. Packagers are thread-safe. The Maven `groupId` and the
artifact names `api`, `points` and `core` are unchanged, and so are the JPMS module names (`com.github.skjolber.packing.api`, `.ep`, `.core`, `.test`).

**Java baseline: unchanged, Java 17.** 4.x compiles with `maven.compiler.source` / `target` 17 and 5.0 with `maven.compiler.release` 17.

### Artifacts and modules

 * New artifact `validators` (module `com.github.skjolber.packing.validators`). The validator classes of 4.x lived in `core`, in the same package
   names (`com.github.skjolber.packing.validator.*`); `core` 5.0 does not depend on `validators`, so add it when you use them.
 * The OpenAPI artifacts (`open-api-model`, `open-api-client`, `open-api-server`, `open-api-test`) are removed.
 * The module descriptors export all public packages. In 4.x the `api` module exported only `com.github.skjolber.packing.api`, the `points` module (`com.github.skjolber.packing.ep`) did not
   export `points1d`, and `core` did not export `comparator` or `validator`. The `core` module now `requires transitive` `api` and `ep`.
 * The package `com.github.skjolber.packing.deadline` (in `core`) is gone, see below.

## Renamed and replaced

Each name in the left column exists in 4.2.6-SNAPSHOT and each name in the right column exists in 5.0. Names like `packer.X`, `api.X` or `comparator.X` are relative to
`com.github.skjolber.packing`; an unqualified method or nested class belongs to the class or interface named in the same row.

### Packages and types that moved

<details>
<summary>Show the 8 entries</summary>

| 4.x | 5.0 |
| --- | --- |
| `com.github.skjolber.packing.deadline.*` (`PackagerInterruptSupplier`, `PackagerInterruptSupplierBuilder`, `DefaultPackagerInterrupt`, `DeadlineCheckPackagerInterruptSupplier`, `DelegateDeadlineCheckPackagerInterruptSupplier`, `PositivePackagerInterruptSupplier`, `NegativePackagerInterruptSupplier`) | `com.github.skjolber.packing.api.interrupt.*` (same class names) |
| `packer.PackagerInterruptedException` | `api.interrupt.PackagerInterruptedException` (it carries no stack trace) |
| `packer.PackagerException` | `api.PackagerException` |
| `packer.IntermediatePackagerResult` | `api.packager.IntermediatePackagerResult` |
| `comparator.IntermediatePackagerResultComparator` | `api.packager.IntermediatePackagerResultComparator` |
| `comparator.LargestAreaPlacementComparator` | `comparator.placement.LargestAreaPlacementComparator` |
| `core`: `com.github.skjolber.packing.validator.*` | `validators` artifact, same package names |
| `validator.reasons.TooManyContainerIdsReason` | `validator.reasons.UnknownContainerIdReason` (same reason code) |

</details>

### Request, result builder and interrupts

<details>
<summary>Show the 11 entries</summary>

| 4.x | 5.0 |
| --- | --- |
| `Order.CRONOLOGICAL`, `Order.CRONOLOGICAL_ALLOW_SKIPPING` | `Order.CHRONOLOGICAL`, `Order.CHRONOLOGICAL_ALLOW_SKIPPING` |
| `PackagerResultBuilder.withDeadline(long)` | `withInterruptDeadline(long)` (epoch milliseconds, as before); `withInterruptDuration(long)` (milliseconds from now) is new |
| `PackagerResultBuilder.withInterrupt(BooleanSupplier)` | `withInterrupt(PackagerInterruptSupplier)`; lambdas work, a `BooleanSupplier` variable does not (use `supplier::getAsBoolean`) |
| `PackagerResultBuilder.withBoxItems(BoxItemGroup...)` | `withBoxItemGroups(BoxItemGroup...)` (same on the validator result builder) |
| `PackagerResultBuilder.ControlledContainerItemBuilder` | `PackagerResultBuilder.ContainerItemBuilder` |
| `ControlledContainerItemBuilder.withBoxItemControlsBuilderFactory(..)` | `ContainerItemBuilder.withManifestControlsBuilderFactory(..)` |
| `packer.ControlledContainerItem` | `api.ContainerItem` (it now holds the manifest and point controls factories, initial points and the cost calculator) |
| `ControlledContainerItem.get/set/hasBoxItemControlsBuilderFactory()` | `ContainerItem.get/set/hasManifestControlsBuilderFactory()` |
| `PackagerInterruptSupplierBuilder.builder()` | `PackagerInterruptSupplierBuilder.newBuilder()` |
| `PackagerInterruptSupplierBuilder.withInterrupt(BooleanSupplier)` | `withInterrupt(PackagerInterruptSupplier)` |
| `PackagerInterruptSupplierBuilder.withScheduledThreadPoolExecutor(ScheduledThreadPoolExecutor)` | `withScheduledExecutorService(ScheduledExecutorService)` |

</details>

### Packager builders and comparators

<details>
<summary>Show the 14 entries</summary>

| 4.x | 5.0 |
| --- | --- |
| `ParallelBoxItemBruteForcePackager` | `ParallelBruteForcePackager` |
| `BruteForcePackager.BruteForcePackagerBuilder` | `BruteForcePackager.Builder` |
| `FastBruteForcePackager.FastBruteForcePackagerBuilder` | `FastBruteForcePackager.Builder` |
| `ParallelBoxItemBruteForcePackager.ParallelBruteForcePackagerBuilder` | `ParallelBruteForcePackager.Builder` |
| `PlainPackager.Builder.withPackagerResultComparator(..)` | `withIntermediatePackagerResultComparator(..)` |
| `BruteForcePackager` / `FastBruteForcePackager` builder `withComparator(..)` | `withIntermediatePackagerResultComparator(..)` (the 4.x parallel builder had none) |
| `PlainPackager.Builder.PlacementControlsBuilderFactoryBuilder` | `packer.PlacementControlsBuilderFactoryBuilder` (lambdas passed to `withPlacementControlsBuilderFactory(Consumer)` are unaffected) |
| `AbstractLargestAreaFitFirstPackagerBuilder<R extends Placement, B>` | `AbstractLargestAreaFitFirstPackagerBuilder<B>` |
| `java.util.Comparator<IntermediatePackagerResult>` accepted by the builders | `IntermediatePackagerResultComparator` (no longer extends `java.util.Comparator`) |
| `java.util.Comparator<BoxItemGroup>`, `Comparator<BoxItem>` accepted by the packager builders, the placement controls consumer and the iterator builders | `api.packager.BoxItemGroupComparator`, `api.packager.BoxItemComparator` |
| `java.util.Comparator<Placement>` accepted as placement comparator | `api.packager.control.placement.PlacementComparator` |
| `comparator.VolumeWeightAreaMinZIntermediatePlacementResultComparator` | `comparator.placement.VolumeWeightAreaMinZPlacementComparator` (same ranking) |
| `ParallelBoxItemBruteForcePackager.shutdown()` | `ParallelBruteForcePackager.close()` (shuts down an executor service only if the builder created it) |
| `ParallelBruteForcePackagerException` (thrown by the builder) | `IllegalStateException` from `build()`; the setters still throw `IllegalArgumentException`. A thread count combined with an executor service, which was an `IllegalArgumentException`, is an `IllegalStateException` from `build()` too |

</details>

### Adapters are now sessions

The per-operation workers which pack one container at a time were `PackagerAdapter`s; they are `PackagerSession`s (in `api`, for container packing
strategies). Applications which only call `newResultBuilder()` are not affected. `PackagerSession.attempt(index, best, ..)` may return an empty result instead of a
result with less load volume than `best`.

<details>
<summary>Show the 12 entries</summary>

| 4.x | 5.0 |
| --- | --- |
| `packer.PackagerAdapter` | `api.packager.strategy.PackagerSession` (`getContainers(int)` is `getContainers()`) |
| `packer.AbstractPackagerAdapter` | `packer.AbstractPackagerSession` |
| `packer.AbstractBoxItemAdapter`, `AbstractBoxItemGroupAdapter` | `packer.AbstractBoxItemSession`, `AbstractBoxItemGroupSession` |
| `bruteforce.AbstractBruteForceBoxItemPackagerAdapter` | `bruteforce.AbstractBruteForceBoxItemSession` |
| `bruteforce.AbstractBruteForceBoxItemGroupsPackagerAdapter` | `bruteforce.AbstractBruteForceBoxItemGroupSession` |
| `bruteforce.AbstractSingleThreadedBruteForceBoxItemPackagerAdapter`, `...BoxItemGroupPackagerAdapter` | `bruteforce.AbstractSingleThreadedBruteForceBoxItemSession`, `...BoxItemGroupSession` |
| `PlainPackager.PlainBoxItemAdapter`, `PlainBoxItemGroupAdapter` | `PlainPackager.PlainBoxItemSession`, `PlainBoxItemGroupSession` |
| `AbstractLargestAreaFitFirstPackager.PlainBoxItemAdapter`, `PlainBoxItemGroupAdapter` | `AbstractLargestAreaFitFirstPackager.LargestAreaFitFirstBoxItemSession`, `LargestAreaFitFirstBoxItemGroupSession` |
| `AbstractBruteForcePackager.createBoxItemAdapter(..)`, `createBoxItemGroupAdapter(..)` | `createBoxItemSession(..)`, `createBoxItemGroupSession(..)` |
| `AbstractPackager.packAdapter(..)` | `AbstractPackager.packSession(..)`; sessions come from `createSession(PackagerInput, ..)`, subclasses implement `newSession(..)` |
| `packer.ContainerItemsCalculator` | `packer.DefaultContainerInventory`, an implementation of `api.packager.strategy.ContainerInventory` |
| `BruteForceIntermediatePackagerResult.containsLastStackable()`, `getSize()` | `containsLastBox()`, `getBoxCount()` |

</details>

### Model (`api`)

<details>
<summary>Show the 13 entries</summary>

| 4.x | 5.0 |
| --- | --- |
| `clone()` on `Box`, `BoxItem`, `BoxItemGroup`, `BoxStackValue`, `Container`, `Point` (`clone(int, int, int)`), `Point1D`/`SimplePoint2D`/`SimplePoint3D`, `Point2DFlagList.clone(boolean)`, `Point3DFlagList.clone(boolean)` | `copy()` (`copy(int, int, int)`, `copy(boolean)`) |
| `BoxItem.getIndex()`, `setIndex(int)` | `getLocalIndex()`, `setLocalIndex(int)` (dense, changes during packing); `getGlobalIndex()`, `setGlobalIndex(int)` identifies the box item in the operation. The third argument of `BoxItem(Box, int, int)` is the local index |
| `Box.getBoxItem()`, `Box.Builder.withBoxItem(..)` | `Placement.getBoxItem()`; placements built by hand pass their box item (`new Placement(boxItem, stackValue, point)`, `Placement.setStackValue(BoxItem, BoxStackValue)`), which container priority, extraction order, groups and identical-box checks use |
| `new Box(id, description, volume, weight, stackValues, properties, BoxItem)` | `new Box(id, description, volume, weight, stackValues, properties)`; the stack values must belong to no other box (`IllegalArgumentException` otherwise; `BoxStackValue.copy()` belongs to none) |
| `Box.Builder.newStackValue(int dx, int dy, int dz, List<Surface>, int index)` (protected) | `newStackValue(.., int index, int centerOfGravityX, int centerOfGravityY, int centerOfGravityZ)` |
| `Container.fitsInside(Box)`, `fitsInside(BoxItem)` | `Container.canLoad(Box)`, `canLoad(BoxItem)` |
| `Container.fitsInside(BoxItemGroup)` (true if one box fits) | `Container.canLoadAtLeastOneBox(BoxItemGroup)`; `canLoad(BoxItemGroup)` requires every box to fit |
| `Box.getStackValues(Container)`, `Box.rotations(Container)` | `Box.rotations(int dx, int dy, int dz)` with the container's load size; it returns an empty list, not `null`, when no rotation fits |
| `Container.getWeight()`, `getMaxWeight()`, `getLoadWeight()`, `Stack.getWeight()` return `int` | return `long` |
| `Box.getMinimumPressure()`, `getMaximumPressure()` return `long` | return `double` |
| `Dimension` (`api`) | `DefaultPointCalculator1D.Axis` (`points1d` stays; it is now exported by the module) |
| `Point.X_COMPARATOR`, `COMPARATOR` and the other `java.util.Comparator<Point>` constants | same names, type `Point.PointComparator` |
| `BoxItem`, `Placement`, `Stack` implement `Serializable` | they do not |

</details>

### Controls (`api.packager.control`)

Manifest, point and placement controls already existed in 4.x (the builder factory, builder, controls chain is unchanged). What changed:

<details>
<summary>Show the 9 entries</summary>

| 4.x | 5.0 |
| --- | --- |
| `ManifestControlsBuilderFactory.createBoxItemControlsBuilder()` | `createManifestControlsBuilder()` |
| `PlacementControls<R extends Placement>`, `PlacementControlsBuilder<R>`, `PlacementControlsBuilderFactory<R>`, `AbstractPlacementControls<R>` | the same types without the type parameter |
| `PlacementControlsBuilder.withBoxItems(BoxItemSource, int, int)` | `withBoxItems(BoxItemSource)`; the range is passed to `PlacementControls.getPlacement(offset, length)` |
| `AbstractPlacementControlsBuilder<R>` | removed; implement `PlacementControlsBuilder` |
| `PointControlsBuilder`, `PlacementControlsBuilder` (interfaces to implement) | gain `withMaxLoad(boolean, boolean, boolean)` and `withLoadIdenticalBox(boolean)`, and `PlacementControlsBuilder` also `withStability(boolean, boolean)`: the packagers use them to pass on the load and support options (`AbstractPointControlsBuilder` implements them) |
| `PlacementComparator.compare(Box, BoxStackValue, Point, Box, BoxStackValue, Point)` | `compare(Placement, Placement)` |
| `PlacementControlsBuilderFactoryBuilder.withPlacementComparator(Comparator<Placement>)` | `withPlacementComparator(PlacementComparator)`; `withPlacementComparators(Consumer<DefaultPlacementComparatorFactory.Builder>)` and `withPlacementComparatorFactory(..)` are new |
| `PlainPlacement.getSupportArea()` | `Placement.getSupportedArea()`, and the comparator must declare `usesSupportedArea()` (see [Behavioral changes](#behavioral-changes)) |
| `AbstractPointControlsBuilder.withBoxItemGroups(..)`, `DefaultManifestControls(BoxItemSource)` | removed; `DefaultManifestControls()` |

</details>

### Points (`points`)

<details>
<summary>Show the 6 entries</summary>

| 4.x | 5.0 |
| --- | --- |
| `DefaultPointCalculator1D(.., Dimension)` | `DefaultPointCalculator1D(.., DefaultPointCalculator1D.Axis)` |
| `DefaultPointCalculator2D.cloneOnConstrain`, `constrainFloatingMaxWithClone(..)` (protected) | `copyOnConstrain`, `constrainFloatingMaxWithCopy(..)` |
| `Point2D.clone(int, int)` | `Point2D.copy(int, int)` |
| `DefaultXSupportPoint2D`, `DefaultYSupportPoint2D`, `DefaultXYSupportPoint2D`, `XSupportPoint2D`, `YSupportPoint2D` | `DefaultPoint2D` (one implementation, with `getXSupport()`, `getYSupport()`) |
| `Default3DPlanePoint3D`, `DefaultXYPlanePoint3D`, `DefaultXZPlanePoint3D`, `DefaultYZPlanePoint3D`, `DefaultXYPlaneXZPlanePoint3D`, `DefaultXYPlaneYZPlanePoint3D`, `DefaultXZPlaneYZPlanePoint3D`, `XYPlanePoint3D`, `XZPlanePoint3D`, `YZPlanePoint3D` | `DefaultPoint3D` (one implementation, with `getXYPlane()`, `getXZPlane()`, `getYZPlane()`) |
| `SimplePoint3D.calculateXYSupport(..)`, `calculateXZSupport(..)`, `calculateYZSupport(..)`, `rotate()` | removed (`Point.isSupportedXYPlane(..)` is new) |

</details>

### Iterators (`core`, `iterator`)

<details>
<summary>Show the 3 entries</summary>

| 4.x | 5.0 |
| --- | --- |
| `AnyOrderBoxItemGroupIterator.Builder.withComparator(Comparator<BoxItemGroup>)` | `withComparator(BoxItemGroupComparator)` |
| iterator constructors `(BoxItem[], ..)` | iterator constructors also take the stack values which fit the container (`BoxStackValue[][]`); use the builders, which are unchanged |
| `BinarySearchIterator.reset(int high, int low)` | `reset(int low, int high)`: the signature is the same, so callers compile and silently swap the bounds |

</details>

### Validators and test utilities

<details>
<summary>Show the 6 entries</summary>

| 4.x | 5.0 |
| --- | --- |
| `AbstractValidatorResultBuilder.withBoxItems(BoxItemGroup...)` | `withBoxItemGroups(BoxItemGroup...)` |
| `DefaultPlacementValidator(Container)` | `DefaultPlacementValidator(Container, LoadValidator, StabilityValidator)` |
| `ValidatorContainerItem.createPlacementValidator(Container)` | `createPlacementValidator(Container, List<Placement>)` |
| `test.assertj.AbstractContainerAssert.isStackedWithinContraints()` (so also on `ContainerAssert`) | `isStackedWithinConstraints()` |
| `test.assertj.AbstractStackAssert.isWithinLoadContraints(Container)` (so also on `StackAssert`) | `isWithinLoadConstraints(Container)` |
| a hand-written `assertValid(PackagerResult)` helper | `PackagerResultAssert.assertThat(result).isStackedWithinConstraints()` (`com.github.skjolber.packing.test.assertj`) |

</details>

The `assertValid(..)` of the 4.x tests was a helper in the 4.x test sources, not part of the `test` artifact; `PackagerResultAssert` is new.

## Removed

Removed without a direct replacement, or replaced by a different mechanism:

 * `Dimension`: see the table above. The one-dimensional point calculator is kept (`points1d`).
 * `PointCalculator.remove(Predicate<Point>)` and its three implementations. 4.2.5 had fixed it to remove the points the predicate matches (it removed the
   others); 5.0 removes the method, which had no callers.
 * The plain and LAFF placement controls classes: `PlainPlacement`, `PlainPlacementComparator`, `PlainPlacementControls`, `PlainPlacementControlsBuilder`,
   `PlainPlacementControlsBuilderFactory` and `LargestAreaFitFirstPlacementControlsBuilder`. Configure the default controls instead, with
   `withPlacementControlsBuilderFactory(Consumer<PlacementControlsBuilderFactoryBuilder>)` (also on the LAFF builders, with `withFirstPlacementControlsBuilderFactory(Consumer)`),
   a `PlacementComparator`, or `DefaultPlacementComparatorFactory`.
 * `ListPlacementComparator` and `LowerZDelegatePlacementComparator`: build the ranking with `DefaultPlacementComparatorFactory` (for example `lowerZIsBetter()`).
 * `PointSourceBuilder`, `PointSourceBuilderFactory`, `EmptyPointSource`, `ClonablePackagerInterruptSupplier`, `PermutationBoxItemValue`, `PermutationRotation`,
   `PackagerBoxItemPermutationRotationIterator`, `DefaultControlsPackagerResultBuilder`: unused.
 * Builder options which had no effect: `withPoints(..)` on the `BruteForcePackager` builder, and `withFirstBoxItemGroupComparator(..)` on the LAFF builders.
 * `ContainerItem.MAX_LOAD_VOLUME_COMPARATOR` and `MAX_LOAD_WEIGHT_COMPARATOR`: write the comparator you need.
 * `BoxStackValue.setBox(..)` and `Box(Box, List<BoxStackValue>)`: boxes and their stack values are immutable once built and shared with the packing result.
 * Iterator internals: `BoxItemPermutationRotationIterator.getMinStackableAreaIndex(int)`, `AbstractBoxItemPermutationRotationIterator.getMinStackableArea(int)`,
   `ParallelBoxItemGroupPermutationRotationIterator.preventOptmisation()` (sic) and its padding fields `t0` to `t15`.
 * `AbstractPackager` internals: `packSingle(..)`, `getFitsInside(..)`, `getBoxItemsFitsInside(..)`, `removeEmpty(..)`, `getScheduledThreadPoolExecutor()`.
 * `Rotation` has a private constructor; use `Rotation.newBuilder()`.
 * The protected helpers of `AbstractValidator` and `DefaultValidator` (`validateBoxItemCounts(..)`, `validateLoad(..)`, `validateBoxItemOrder(..)`, `validate(Map, ..)` and so on): the validation
   is split into separate classes in the `validators` artifact (`StackValidator`, `ContainerCountValidator`, `BoxCountValidator`, `BoxItemOrderValidator` and others), which `DefaultValidator` composes.
 * The OpenAPI modules.

## New concepts worth knowing

 * **Controls.** The manifest, point and placement controls of 4.x remain extension points. New: `PlacementComparator` / `PlacementComparatorFactory` /
   `PlacementComparatorAttribute` in `api` (and `DefaultPlacementComparatorFactory` in `core`) to rank placements without writing controls;
   `PlacementControls.accepted(..)` / `undo(..)` callbacks; `PlacementControlsBuilderFactory.supportsLoad()` (default `false`: a packager rejects boxes with load limits
   unless the controls support them). Factories supplied to packagers must be safe for concurrent use. See
   [DEVELOPER.md](../DEVELOPER.md#writing-your-own-placement-controls) and [docs/packager-controls.md](../docs/packager-controls.md).
 * **Sessions.** A `PackagerSession` packs one container at a time on copies of the box items and container items, and is what a container packing strategy drives
   (`attempt`, `peek`, `accept`, `fresh`, `fork`).
 * **Container packing strategies.** A `ContainerPackingStrategy` (created by a `ContainerPackingStrategyFactory`, set with `withContainerPackingStrategyFactory(..)` on the
   packager builders) decides which containers are used and in which order. The defaults are an ordered strategy, or a cost-aware one when containers have a cost
   (`ContainerItem.newListBuilder().withContainer(container, count, costCalculator)`, `ContainerCostCalculator`). 4.x had no such extension point, so there is nothing to
   migrate; the strategy names (`ContainerPackingStrategy`, `ContainerPackingStrategyFactory`, `ContainerResult`, `ContainerInventory`) are those of 5.0. See
   [DEVELOPER.md](../DEVELOPER.md#writing-your-own-container-packing-strategy), [docs/container-packing-strategies.md](../docs/container-packing-strategies.md) and
   [docs/container-costs.md](../docs/container-costs.md).
 * **Box item groups.** `BoxItem` and `BoxItemGroup` keep the packing state (remaining count, local and global index), as before, and now also carry a
   container priority and an extraction order (`withContainerPriority(..)`, `withExtractionOrder(..)`). The group API otherwise has the same shape (`copy()` for `clone()`).
   The boxes of a group are inserted together, and without a box item order the brute-force packagers search the order of the groups. The brute-force packagers also
   support a box item order (4.x threw `IllegalStateException("Order not supported for brute force packager")`).
 * **Load constraints, support, insertion order, deliveries.** See [README.md](../README.md#load-constraints), [docs/insertion-order.md](../docs/insertion-order.md) and
   [docs/deliveries.md](../docs/deliveries.md). `withCalculateSupport(..)` and `withRequireFullSupport(..)` are options of the plain and LAFF builders.
 * **Composite packager and virtual boxes.** `CompositePackager` (cheap packagers first, costly ones only where needed) in `com.github.skjolber.packing.packer.composite`, and
   `VirtualBoxPackager` in `com.github.skjolber.packing.virtualbox` (wraps a packager to pack repeated boxes as assemblies). See [docs/heuristics/composite.md](../docs/heuristics/composite.md) and
   [docs/heuristics/virtual-box.md](../docs/heuristics/virtual-box.md).
 * **Validators.** The `validators` artifact validates results independently of the packager, including load, stability, insertion and extraction order. See
   [README.md](../README.md#validating-results).
 * **Thread priority.** `ParallelBruteForcePackager.newBuilder().withThreadPriority(Thread.MIN_PRIORITY)` searches at a lower thread priority.
 * **Unsupported input is rejected.** Packagers reject an input they cannot honor (`getUnsupportedReason(..)`, thrown as `IllegalStateException` by `build()`) rather than
   ignoring it, see [FEATURES.md](../FEATURES.md#feature-support).
 * **Insertion order of placements.** `PackagerResultBuilder.withInsertionOrder(boolean)`, see [Behavioral changes](#behavioral-changes).

## Behavioral changes

Not all of these are visible to the compiler.

 * **Exact 4.x layout parity ended.** The free points after a placement are processed in a documented canonical total order, which no longer depends on how a
   sort orders equal keys. Where two candidates tie exactly (for example the same box turned 2x8x7 or 8x2x7), an individual order can be packed differently from 4.x. In aggregate
   the results are equal or better: over 10,000 random orders (seeds 300 to 10,299) against 4.2.5, the plain packager wins 229 orders and loses 89 (9,682 tied), the LAFF packager
   wins 824 and loses 11, and the fast LAFF packager wins 750 and loses 9. Do not compare placements with 4.x results one by one. See
   `PlainVersion4ComparisonTest`, `LargestAreaFitFirstVersion4ComparisonTest` and [jmh/PERFORMANCE.md](../jmh/PERFORMANCE.md), which also has the performance
   figures. LAFF packagers also raise a level to the top of the container when no box fits a new level on top of it.
 * **Plain packager default ranking.** Both versions try the box item with the highest volume, then weight, first. To choose the position, 4.x ranked candidates by supported share, then lower z, then
   larger footprint (`PlainPlacementComparator`), and always calculated the support. The 5.0 default ranks by higher volume, higher weight, lower footprint area and lower z; support is only
   calculated, and ranked first, with `withCalculateSupport(true)`.
 * **Placements in insertion order.** The placements of each container are in an order in which the boxes can be loaded (each box after the boxes it rests on and after the boxes
   in its path from the container's opening). Without a box item order the packagers reorder them after packing. Applications which depend on the order of
   `container.getStack().getPlacements()` see a different order; `withInsertionOrder(false)` keeps the order of the packager's search and skips the reordering.
 * **Result comparator contract.** The convention is unchanged: `compare(a, b) > 0` means the first argument is better, for `IntermediatePackagerResultComparator` and for placement comparators (that is how the 4.x
   packagers read them). `IntermediatePackagerResultComparator.prefersHigherLoadVolume()`
   is new (default `false`): return `true` when a lower load volume always compares worse, and the brute-force packagers skip searches that cannot beat the best result. Compare
   results by `getLoadVolume()`, `getLoadWeight()` and `getBoxCount()`, not by their stacks (a brute-force result does not build its stack until it is accepted; the 4.x
   `DefaultIntermediatePackagerResultComparator` read the stacks and chose wrong results when given to a brute-force packager).
 * **Supported area is opt-in for placement comparators.** `PlacementComparator.usesSupportedArea()` is honored and defaults to `false` (also for a lambda): the supported area of a
   candidate is not calculated and `Placement.getSupportedArea()` reads zero, unless the comparator overrides `usesSupportedArea()` to return `true`. A custom comparator that relied on the supported
   area (in 4.x through `PlainPlacement.getSupportArea()`) must declare it. `prefersHigherSupportedArea()` is an optional optimization hint.
 * **Controls factories are matched by id.** For reusing the result of one container for another (`PackagerSession.peek(..)`), 4.x compared the manifest and point controls
   factories of the containers with `equals(..)`. 5.0 reuses a result only when the factories are the same instance, or both return the same non-null `getId()` (default `null`).
   If your factories relied on `equals(..)`, implement `getId()` so that it identifies everything that changes the behavior (for example a weight limit), or wrap a lambda with
   `ManifestControlsBuilderFactory.of(id, factory)` or `PointControlsBuilderFactory.of(id, factory)`. Without an id, results are not shared between factory instances: the
   packing result is the same, only slower. Containers without controls match each other, but a result packed without controls is not reused for a container with controls
   (4.x reused it, so the controls were ignored). `DefaultPointControlsBuilderFactory` returns the id `default`.
 * **Boxes are shared.** Packing works on copies of the box items and container items, while the boxes and stack values are shared with the input and never modified:
   `placement.getBox()` is the input `Box`, and `placement.getBoxItem()` is the session's copy of the box item (same global index). `Box.getBoxItem()` is gone for that reason,
   and one box can be used by several box items. The permutation iterators keep the rotations which fit the container (`getStackValues(int)`, `getBoxItemStackValues()`) instead of
   copies of the boxes.
 * **Absorbed 4.x bugs.** 5.0 contains fixes which were also made on the 4.x line: the inverted z tiebreaks of the plain and LAFF placement comparators (the packagers built towers
   instead of spreading boxes on the floor; fixed in 4.2.5), and, after 4.2.5 (so in 4.2.6), the `int` overflow in the sort tie-break for moved points in containers larger than
   about 1300 per side, and a brute-force peek which could approve a container from another attempt's stack.
 * **`BinarySearchIterator.reset(low, high)`.** The arguments were `(high, low)` in 4.x.
 * **`Box.rotations(dx, dy, dz)`** returns an empty list instead of `null`.

## Common compile errors

<details>
<summary>Show the 30 errors and fixes</summary>

| javac message after the upgrade | Fix |
| --- | --- |
| `package com.github.skjolber.packing.deadline does not exist` | Import `com.github.skjolber.packing.api.interrupt.*` instead. |
| `cannot find symbol ... class PackagerInterruptedException` / `PackagerException` in `com.github.skjolber.packing.packer` | `api.interrupt.PackagerInterruptedException`, `api.PackagerException`. |
| `package com.github.skjolber.packing.validator does not exist` | Add the `validators` artifact. |
| `cannot find symbol ... variable CRONOLOGICAL` | `Order.CHRONOLOGICAL` / `Order.CHRONOLOGICAL_ALLOW_SKIPPING`. |
| `cannot find symbol ... method withDeadline(long)` | `withInterruptDeadline(long)`, or `withInterruptDuration(long)` for milliseconds from now. |
| `incompatible types: BooleanSupplier cannot be converted to PackagerInterruptSupplier` | Pass a lambda, or `supplier::getAsBoolean`. |
| `no suitable method found for withBoxItems(BoxItemGroup)` | `withBoxItemGroups(..)`. |
| `cannot find symbol ... class ControlledContainerItemBuilder` or `ControlledContainerItem` | `PackagerResultBuilder.ContainerItemBuilder`, `ContainerItem`. |
| `cannot find symbol ... method withBoxItemControlsBuilderFactory(..)` / `getBoxItemControlsBuilderFactory()` | `withManifestControlsBuilderFactory(..)` / `getManifestControlsBuilderFactory()`. |
| `... is not abstract and does not override abstract method createManifestControlsBuilder()`, or `method does not override or implement a method from a supertype` on `createBoxItemControlsBuilder()` | Rename the method to `createManifestControlsBuilder()`. |
| `... is not abstract and does not override abstract method withMaxLoad(boolean,boolean,boolean)` on a `PointControlsBuilder` (or `PlacementControlsBuilder`) | Extend `AbstractPointControlsBuilder`, or implement `withMaxLoad(..)` and `withLoadIdenticalBox(..)` (and `withStability(..)` for placement controls). |
| `type PlacementControls does not take parameters` (also `PlacementControlsBuilder`, `PlacementControlsBuilderFactory`) | Remove the type argument. |
| errors on `clone()`, for example `clone() has protected access in Object`, on `Box`, `BoxItem`, `BoxItemGroup`, `BoxStackValue`, `Container`, `Point` | `copy()` (`copy(int, int, int)` for `Point`). |
| `incompatible types: possible lossy conversion from long to int` on `getWeight()`, `getMaxWeight()`, `getLoadWeight()` | Use `long`. |
| `incompatible types: possible lossy conversion from double to long` on `getMinimumPressure()` / `getMaximumPressure()` | Use `double`. |
| `cannot find symbol ... method getIndex()` / `setIndex(int)` on `BoxItem` | `getLocalIndex()` / `getGlobalIndex()`, `setLocalIndex(int)`. |
| `cannot find symbol ... method getBoxItem()` or `withBoxItem(..)` on `Box` | `placement.getBoxItem()`. |
| `constructor Box in class Box cannot be applied to given types` | Drop the trailing `BoxItem` argument; the stack values must belong to no other box (`BoxStackValue.copy()`), see [Model](#model-api). |
| `incompatible types: Box cannot be converted to Stack` (also `BoxItem`, `BoxItemGroup`) on `Container.fitsInside(..)` | `canLoad(..)`; `canLoadAtLeastOneBox(BoxItemGroup)` for a group (`fitsInside(Stack)` is unchanged). |
| `cannot find symbol ... class BruteForcePackagerBuilder` (or `FastBruteForcePackagerBuilder`, `ParallelBruteForcePackagerBuilder`) | `BruteForcePackager.Builder` (and so on). |
| `cannot find symbol ... class ParallelBoxItemBruteForcePackager` | `ParallelBruteForcePackager`. |
| `cannot find symbol ... class ParallelBruteForcePackagerException` | Catch `IllegalStateException`. |
| `cannot find symbol ... method withPackagerResultComparator(..)` or `withComparator(..)` | `withIntermediatePackagerResultComparator(..)`. |
| `incompatible types: Comparator<..> cannot be converted to BoxItemGroupComparator` (or `BoxItemComparator`, `PlacementComparator`, `IntermediatePackagerResultComparator`) | Pass a lambda, or implement the interface; the sign convention is unchanged. |
| `cannot find symbol ... class PlainPlacement` / `PlainPlacementComparator` | Use a `PlacementComparator` and `Placement.getSupportedArea()` with `usesSupportedArea()`, see [Removed](#removed). |
| `cannot find symbol ... class PackagerAdapter` (or an `...Adapter` class) | `PackagerSession` (or the matching `...Session` class), see [Adapters are now sessions](#adapters-are-now-sessions). |
| `cannot find symbol ... class Dimension` | `DefaultPointCalculator1D.Axis`. |
| `cannot find symbol ... method remove(Predicate)` on a `PointCalculator` | No replacement, see [Removed](#removed). |
| `cannot find symbol ... method isStackedWithinContraints()` / `isWithinLoadContraints(..)` | `isStackedWithinConstraints()` / `isWithinLoadConstraints(..)`. |
| `cannot find symbol ... method builder()` on `PackagerInterruptSupplierBuilder` | `newBuilder()`. |

</details>
