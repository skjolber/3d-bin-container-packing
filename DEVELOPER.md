# Developer guide

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
Results of packing attempts are compared with a
`Comparator<IntermediatePackagerResult>`, also in `api`. Support-aware and load-aware implementations in
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

## Writing your own container strategy

A container strategy decides which containers to use, and in which order. The interfaces live in
`com.github.skjolber.packing.api.packager.strategy`, so a strategy only needs the `api` module.

- Implement `ContainerStrategy.pack(interrupt, session)`. The `PackagerSession` holds the
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
- Check the interrupt regularly and throw `PackagerInterruptedException` when it returns true;
  `attempt` throws it too.
- `getMaxContainerCount()` is the number of containers which can still be used for the remaining
  boxes.
- `ContainerInventory` tells what fits: `canLoad(..)`, `getContainers(..)` and `isFeasible(..)`
  filter container types for the remaining boxes; `hasCost()` and `getCost()` report cost.
- To compare alternatives, `fork()` copies the session at its current state and `fresh()`
  restarts the packaging operation. Both are independent of the original session, so alternatives
  can be explored in parallel, one thread per session.

Configure the strategy with a `ContainerStrategyFactory`, which is called once per packaging
operation:

```java
try (PlainPackager packager = PlainPackager.newBuilder()
        .withContainerStrategyFactory((inventory, boxItems, boxItemGroups) -> new LargestContainerFirstStrategy())
        .build()) {
    // ...
}
```

All packager builders have `withContainerStrategyFactory(..)`. `LargestContainerFirstStrategy` and
`BackToFrontPlacementComparator` in the `test` module (package
`com.github.skjolber.packing.test.example`) are complete examples which depend on the `api` module
only. The built-in strategies are in the `core` package
`com.github.skjolber.packing.packer.strategy`; `DefaultContainerStrategyFactory` chooses between
them.

## Packagers in a composite

`CompositePackager` (package `com.github.skjolber.packing.packer.composite`) works with any
`AbstractPackager`. For each packaging operation it creates a session per packager with
`createSession(PackagerInput, interrupt)`, and keeps the sessions in sync: a result from one packager's
session is accepted by all of them.

- A packager implements `newSession(input, interrupt)`. The input's boxes and containers are copies which
  belong to the new session.
- `accept(..)` must accept results from other packagers' sessions. Box items are identified by their global
  index (`BoxItem.getGlobalIndex()`), which is the same in all sessions for the same input; local indexes
  change during packing.
- Box item groups are accepted in order: a result must hold a prefix of the remaining groups.
- Return `false` from `supports(PackagerInput)` (by overriding `getUnsupportedReason(..)`) for inputs the
  packager cannot pack; the composite then skips it.
- To let a costly packager skip work, compare results by load volume first and say so with
  `IntermediatePackagerResultComparator.prefersHigherLoadVolume()`: the composite and the ordered container
  strategy then pass the best result so far to `attempt(..)`.

