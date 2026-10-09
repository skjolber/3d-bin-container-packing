# Developer guide

## Extending core

The `api` module is the stable contract: custom controls and strategies (see below) only need it.
The `core` module deliberately exports all its packages, so that custom algorithms can build on its
internals (packagers, sessions, iterators and point calculators). Those internals may change in any
release, without migration notes; extend them at your own risk.

## Writing your own placement controls

Placement controls choose the next box, permitted orientation and position within
a container. Use them for application-specific placement rules without writing a
new packing algorithm. The interfaces live in
`com.github.skjolber.packing.api.packager.control.placement`.

- Implement `PlacementControls.getPlacement(offset, length)` to select a valid
  placement from the supplied box-item range, or return `null` when none is
  available. Use the supplied `PointControls` to obtain candidate points and
  check that each chosen `BoxStackValue` fits. Respect the requested `Order`;
  do not search outside the current group/range.
- Keep candidate evaluation free of committed state changes. The packager adds
  the chosen placement to its stack and point calculator and updates inventory.
  Do not perform those operations again inside your control.
- Override `accepted(placement)` for state changes after acceptance, such as
  adding supporter/supportee load relationships. Override `undo(placements)`
  to reverse your state changes when a group attempt is rolled back.
- Implement `PlacementControlsBuilder` and supply a
  `PlacementControlsBuilderFactory`. The packager populates the builder with
  its current box-item source, point controls, point calculator, stack, container,
  order and load-constraint flags. Return fresh builders and controls for each
  request from the packager; do not share mutable packing state across attempts
  or parallel workers.

For example, once your application implements `MyPlacementControlsBuilder`:

```java
PlacementControlsBuilderFactory controlsFactory = MyPlacementControlsBuilder::new;

try (PlainPackager packager = PlainPackager.newBuilder()
        .withPlacementControlsBuilderFactory(controlsFactory)
        .build()) {
    PackagerResult result = packager.newResultBuilder()
        .withContainerItems(containerItems)
        .withBoxItems(boxItems)
        .build();
}
```

`PlainPackager` and the LAFF packagers expose this factory. LAFF also has
`withFirstPlacementControlsBuilderFactory(...)` for the first placement of a
level; configure both when a rule must apply throughout the packing. Brute-force
packagers use their own point-filter/comparator extension points instead.

For the common case of ranking boxes and positions differently, the plain and LAFF builders also take a consumer:
`withPlacementControlsBuilderFactory(b -> b.withPlacementComparator(..))` (and
`withFirstPlacementControlsBuilderFactory(b -> ..)` for LAFF) configure the default controls, including
`withCalculateSupport(..)` and `withRequireFullSupport(..)`, which have the same effect as the builder options of the same name
(as long as you do not set your own placement comparator: then rank by support yourself, for example with
`higherSupportIsBetter()`). Rank with a fixed `PlacementComparator` (`withPlacementComparator(..)`), with a
`DefaultPlacementComparatorFactory.Builder` (`withPlacementComparators(r -> r.lowerZIsBetter())`) or with your own
`PlacementComparatorFactory` (`withPlacementComparatorFactory(..)`).

Custom placement controls replace the default ones, so the packager options
which configure the default controls (`withCalculateSupport(..)`,
`withRequireFullSupport(..)`) cannot be combined with them: `build()` throws.
Packagers reject inputs with box load limits unless the factory's
`supportsLoad()` returns true; return true only if the controls respect the
load-constraint flags the packager sets on the builder.

Prefer extending existing controls when possible. `ComparatorPlacementControls`
provides candidate iteration and protected `createPlacement`/`selectPlacement`
hooks; rejecting a candidate in `createPlacement` returns `null`. For ranking
alone, use a custom `PlacementComparator`: a positive `compare(a, b)` means
`a` is preferred. The ranking interfaces (`PlacementComparator`,
`PlacementComparatorFactory`, `PlacementComparatorAttribute`) are in the `api`
module. A comparator which does not read the supported area should return false
from `usesSupportedArea()`, and one which never prefers less support should
return true from `prefersHigherSupportedArea()`: the controls then skip load
validation and support calculation for candidates which cannot be selected.
Results of packing attempts are compared with an
`IntermediatePackagerResultComparator`, also in `api`; it should read the results' load volume, weight and box
count, not their stacks (the brute-force packagers compare results whose stacks are not built). Support-aware and load-aware implementations in
`com.github.skjolber.packing.packer` are examples for physical constraints.
Replacing the default controls does not automatically retain their checks:
implement the requested constraints or reject unsupported configurations in
your builder.

`BoxItemSource` indexes are local and can change as inventory is removed.
Preserve the selected item's identity and use current source/point indexes.
Keep candidate loops allocation-light; never recycle accepted placements or
change their box/coordinates during candidate evaluation. Test no-candidate cases, rotations, ordering,
group rollback, repeated attempts and concurrency, and independently validate
the final layouts when your rules concern load or stability.

Manifest controls (`ManifestControlsBuilderFactory`, set per container item) filter by removing
box items from the shared `BoxItemSource`; point controls (`PointControlsBuilderFactory`) filter the points of each box item,
and placement controls must ask them for the points (`PointControls.getPoints(boxItem)`) for the filter to have any effect.
When a group cannot be fitted, `ManifestListener.undo(..)` is called on the manifest controls and the point controls, then
`PlacementControls.undo(..)`, then `attemptFailure(..)` on the manifest and point controls. `attempt(group, offset, length)`
is delivered to the manifest controls only.

Wire manifest and point controls per container item: with the result builder's
`withContainerItem(b -> b.withContainerItem(container, count).withManifestControlsBuilderFactory(..).withPointControlsBuilderFactory(..))`,
or with `ContainerItem.setManifestControlsBuilderFactory(..)` and `setPointControlsBuilderFactory(..)` on a container item passed to
`withContainerItems(..)` (see the README, "Packager controls", for an example). The plain and LAFF packagers use them; the brute-force packagers
do not support controls and reject such inputs (`getUnsupportedReason(..)`).

Packagers reuse the result of a container for another container when the containers' manifest controls and point controls
factories are equal (containers without controls are equal to each other, but a result which was packed without controls is not
reused for a container with them). Lambdas and method references of different expressions never compare equal, so reuse silently does not
happen for them: implement `equals(..)` and `hashCode()` on the factory (or use one instance for the containers) to enable it.
Packagers run concurrently (for example under `ParallelContainerPackingStrategy` and the parallel brute-force packager), so
factories and comparators you supply (the controls factories, `PlacementComparatorFactory`, `BoxItemComparator`,
`BoxItemGroupComparator` and `IntermediatePackagerResultComparator`) must be safe for concurrent use; stateless ones are.

## Writing your own container packing strategy

A container packing strategy decides which containers to use, and in which order. The interfaces live in
`com.github.skjolber.packing.api.packager.strategy`, so a strategy only needs the `api` module.

- Implement `ContainerPackingStrategy.pack(interrupt, session)`. The `PackagerSession` holds the
  remaining boxes and the available containers (`getContainerInventory()`).
- `attempt(containerIndex, best, abortOnAnyBoxTooBig)` packs as many of the remaining boxes as
  possible into a container type, without changing the session. `peek(containerIndex, existing)`
  reuses an existing result for another container type if the packed boxes fit unchanged, and
  returns `null` otherwise.
- `accept(result)` removes the result's boxes from the remaining boxes, uses up the container and
  returns the packed container.
- Return a `ContainerResult` with the accepted containers and
  `getContainerInventory().getCost()` once no boxes remain, or `null` if the boxes cannot be
  packed.
- Check the interrupt regularly and throw `PackagerInterruptedException` when it returns true (it carries no
  stack trace, so throwing it is cheap); `attempt` throws it too.
- `getMaxContainerCount()` is the number of containers which can still be used for the remaining
  boxes.
- `ContainerInventory` tells what fits: `canLoad(..)`, `getContainers(..)` and `isFeasible(..)`
  filter container types for the remaining boxes; `hasCost()` and `getCost()` report cost.
- To compare alternatives, `fork()` copies the session at its current state and `fresh()`
  restarts the packaging operation. Both are independent of the original session, so alternatives
  can be explored in parallel, one thread per session.

Configure the strategy with a `ContainerPackingStrategyFactory`, which is called once per packaging
operation:

```java
try (PlainPackager packager = PlainPackager.newBuilder()
        .withContainerPackingStrategyFactory((inventory, boxItems, boxItemGroups, comparator, emptyResult) -> new LargestContainerFirstStrategy())
        .build()) {
    // ...
}
```

`create(inventory, boxItems, boxItemGroups, comparator, emptyResultSupplier)` receives the available containers, the
remaining box items (null when packing box item groups) or box item groups (null when packing box items), and the
packager's own `IntermediatePackagerResultComparator` and empty-result supplier. A strategy which compares results, or
returns an empty result, should use these (the built-in `OrderedContainerPackingStrategy`,
`LowestCostContainerPackingStrategy` and `ParallelContainerPackingStrategy` take them as constructor arguments), so that
it follows a custom comparator which the packager was configured with. Return a new strategy for every call (or a
stateless one): the packager uses a strategy for one packaging operation and does not share it. Packagers are thread-safe,
so the factory can be called concurrently; implementations must be safe for concurrent use.

All packager builders have `withContainerPackingStrategyFactory(..)`; for `CompositePackager`, the strategy
also applies to the baseline packagers. `LargestContainerFirstStrategy` and
`BackToFrontPlacementComparator` in the `test` module (package
`com.github.skjolber.packing.test.example`) are complete examples which depend on the `api` module
only. The built-in strategies are in the `core` package
`com.github.skjolber.packing.packer.strategy`; `DefaultContainerPackingStrategyFactory` chooses between
them.

## Packagers in a composite

`CompositePackager` (package `com.github.skjolber.packing.packer.composite`) works with any
`AbstractPackager`. For each packaging operation it creates a session per packager with
`createSession(PackagerInput, interrupt)`, and keeps the sessions in sync: a result from one packager's
session is accepted by all of them.

- A packager implements `newSession(input, interrupt)`. The input's box items and container items are copies which
  belong to the new session; the boxes are shared, as they are never modified.
- `accept(..)` must accept results from other packagers' sessions. Box items are identified by their global
  index (`BoxItem.getGlobalIndex()`), which is the same in all sessions for the same input; local indexes
  change during packing.
- Box item groups are accepted in order: a result must hold a prefix of the remaining groups.
- Return `false` from `supports(PackagerInput)` (by overriding `getUnsupportedReason(..)`) for inputs the
  packager cannot pack; the composite then skips it.
- To let a costly packager skip work, compare results by load volume first and say so with
  `IntermediatePackagerResultComparator.prefersHigherLoadVolume()`: the composite and the ordered container
  packing strategy then pass the best result so far to `attempt(..)`.

