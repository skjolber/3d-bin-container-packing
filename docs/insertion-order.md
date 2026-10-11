# Insertion order

The placements of each container are in insertion order: the order in which the boxes can be loaded.

Each box comes after the boxes it rests on, and after the boxes it would otherwise have to pass on its way in. Set how boxes get into a container type with `withAccess(..)`:

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

## Skipping the ordering

Putting the placements in insertion order takes time quadratic in the number of boxes per container. When only the
outcome matters, for example to check whether an order fits during checkout, skip it with
`withInsertionOrder(false)` on the result builder, and calculate the order later if needed with
`InsertionSequencer.sequence(result.getContainers(), Order.NONE)`.

`PackagerResult.isInsertionOrder()` tells whether a result is known to be in insertion order: false when it was
skipped, or when the boxes cannot be loaded in any order (possible through a door, as the packagers place boxes
without regard to the door when there is no box item order).

## Obstacles

Boxes which are already in a container (obstacles, see `withObstacles(..)` on the container item builder) are
inserted first: the packagers only place boxes where no obstacle rests on them or is in their path, and the result
containers keep the obstacles (`Container.getObstacles()`), which the validator includes.

## Box item groups

The boxes of a box item group are inserted together: no box of another group is inserted between them, so a group
(for example the parts of one product or order) can be loaded, and unloaded, as a unit. The packagers only place a box
where no box of an earlier group rests on it or is in its path; `GroupInsertionValidator` (part of `DefaultValidator`)
checks the order.

## See also

 * [Deliveries](deliveries.md): extraction order and container priority, which also follow the container's access.
 * [FEATURES.md](../FEATURES.md#constraints-and-controls): the feature summary.
