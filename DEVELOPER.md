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
`a` is preferred. Support-aware and load-aware implementations in
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
